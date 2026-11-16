package fr.medislot.application;

import fr.medislot.domain.Appointment;
import fr.medislot.repository.AppointmentRepository;
import fr.medislot.repository.ProfessionalRepository;
import fr.medislot.repository.SlotRepository;
import fr.medislot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConvocationService {
 private final AppointmentRepository appointments;
 private final UserRepository users;
 private final SlotRepository slots;
 private final ProfessionalRepository professionals;
 private final PdfRenderer renderer;

 public ConvocationService(
  AppointmentRepository appointments,
  UserRepository users,
  SlotRepository slots,
  ProfessionalRepository professionals,
  PdfRenderer renderer
 ) {
  this.appointments = appointments;
  this.users = users;
  this.slots = slots;
  this.professionals = professionals;
  this.renderer = renderer;
 }

 public byte[] build(Long appointmentId, String email) {
  var user = users.findByEmail(email)
   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable."));
  Appointment appointment = appointments.findById(appointmentId)
   .filter(candidate -> candidate.patientId.equals(user.id))
   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable."));
  if ("CANCELLED".equals(appointment.status)) {
   throw new ResponseStatusException(HttpStatus.CONFLICT, "Une convocation ne peut pas être générée pour un rendez-vous annulé.");
  }
  var slot = slots.findById(appointment.slotId)
   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable."));
  var professional = professionals.findById(slot.professionalId)
   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable."));

  return renderer.render(new ConvocationDocument(
   appointment.id,
   user.name,
   professional.name,
   professional.specialty,
   professional.address,
   professional.city,
   ConvocationFormats.dateTime(slot.startsAt)
  ));
 }

 public static String filename(Long appointmentId) {
  return "convocation-rdv-" + appointmentId + ".pdf";
 }
}
