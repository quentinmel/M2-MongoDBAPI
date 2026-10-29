package fr.medislot.application;
import fr.medislot.domain.*;
import fr.medislot.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
@Service
public class BookingService {
 private final UserRepository users; private final SlotRepository slots; private final AppointmentRepository appointments;
 private final AuditRepository audit; private final NotificationRepository notifications; private final ApplicationEventPublisher events; private final AbService ab; private final JdbcTemplate jdbc;
 public BookingService(UserRepository u,SlotRepository s,AppointmentRepository a,AuditRepository h,NotificationRepository n,ApplicationEventPublisher e,AbService ab,JdbcTemplate jdbc){users=u;slots=s;appointments=a;audit=h;notifications=n;events=e;this.ab=ab;this.jdbc=jdbc;}
 public Long patient(String email){return users.findByEmail(email).orElseThrow().id;}
 private ResponseStatusException conflict(){return new SlotTakenException();}
 private Slot lock(Long id){return slots.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Créneau introuvable."));}
 private void check(Slot s){if(!s.startsAt.isAfter(Instant.now())||appointments.existsBySlotIdAndStatus(s.id,"CONFIRMED"))throw conflict();}
 private Appointment owned(Long id,Long patient){var a=appointments.lockById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));if(!a.patientId.equals(patient))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable.");return a;}
 @Transactional public Appointment book(String email,Long slotId){return book(email,slotId,null);}
 @Transactional public Appointment book(String email,Long slotId,String visitorId){return book(email,slotId,visitorId,null);}
 @Transactional public Appointment book(String email,Long slotId,String visitorId,String idemKey){
  Long patient=patient(email);
  if(idemKey!=null){
   if(!idemKey.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Clé d’idempotence invalide.");
   int inserted=jdbc.update("INSERT INTO booking_idempotency(idem_key,patient_id,slot_id) VALUES (?::uuid,?,?) ON CONFLICT (idem_key) DO NOTHING",idemKey,patient,slotId);
   if(inserted==0){
    var previous=jdbc.queryForObject("SELECT patient_id,slot_id,appointment_id FROM booking_idempotency WHERE idem_key=?::uuid FOR UPDATE",(rs,row)->new Object[]{rs.getLong("patient_id"),rs.getLong("slot_id"),(Long)rs.getObject("appointment_id")},idemKey);
    if(!patient.equals(previous[0])||!slotId.equals(previous[1]))throw new ResponseStatusException(HttpStatus.CONFLICT,"Cette clé d’idempotence a déjà été utilisée pour une autre demande.");
    if(previous[2]==null)throw new IllegalStateException("La clé d’idempotence existe sans réservation associée.");
    return appointments.findById((Long)previous[2]).orElseThrow(()->new IllegalStateException("La réservation idempotente est introuvable."));
   }
  }
  Slot s=lock(slotId);check(s);
  Appointment a=new Appointment();a.patientId=patient;a.slotId=s.id;a.status="CONFIRMED";a.createdAt=Instant.now();appointments.saveAndFlush(a);
  if(idemKey!=null)jdbc.update("UPDATE booking_idempotency SET appointment_id=? WHERE idem_key=?::uuid",a.id,idemKey);
  record(a,"CREATED","Votre rendez-vous est confirmé.");ab.recordConversion(visitorId);events.publishEvent(new BookingChanged(patient,List.of(s.professionalId)));return a;
 }
 @Transactional public Appointment cancel(String email,Long id){
  Appointment a=owned(id,patient(email));Slot s=lock(a.slotId);
  if(a.status.equals("CANCELLED"))return a;
  a.status="CANCELLED";appointments.saveAndFlush(a);record(a,"CANCELLED","Votre rendez-vous a été annulé.");events.publishEvent(new BookingChanged(a.patientId,List.of(s.professionalId)));return a;
 }
 @Transactional public Appointment reschedule(String email,Long id,Long slotId){
  Appointment a=owned(id,patient(email));if(!a.status.equals("CONFIRMED"))throw conflict();
  if(a.slotId.equals(slotId))return a;
  Long oldId=a.slotId; Slot first=lock(Math.min(oldId,slotId)),second=lock(Math.max(oldId,slotId));
  Slot target=first.id.equals(slotId)?first:second;Slot old=first.id.equals(oldId)?first:second;check(target);
  a.slotId=target.id;appointments.saveAndFlush(a);record(a,"RESCHEDULED","Votre rendez-vous a été déplacé.");
  events.publishEvent(new BookingChanged(a.patientId,List.of(old.professionalId,target.professionalId)));return a;
 }
 private void record(Appointment a,String action,String message){AuditEvent h=new AuditEvent();h.appointmentId=a.id;h.actorId=a.patientId;h.action=action;h.createdAt=Instant.now();audit.save(h);Notification n=new Notification();n.patientId=a.patientId;n.message=message;n.createdAt=h.createdAt;notifications.save(n);}
}
