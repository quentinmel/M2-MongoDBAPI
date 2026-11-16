package fr.medislot;

import fr.medislot.application.ConvocationDocument;
import fr.medislot.application.ConvocationService;
import fr.medislot.application.PdfRenderer;
import fr.medislot.domain.AppUser;
import fr.medislot.domain.Appointment;
import fr.medislot.domain.Professional;
import fr.medislot.domain.Slot;
import fr.medislot.repository.AppointmentRepository;
import fr.medislot.repository.ProfessionalRepository;
import fr.medislot.repository.SlotRepository;
import fr.medislot.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvocationServiceTest {
 private final AppointmentRepository appointments = mock(AppointmentRepository.class);
 private final UserRepository users = mock(UserRepository.class);
 private final SlotRepository slots = mock(SlotRepository.class);
 private final ProfessionalRepository professionals = mock(ProfessionalRepository.class);
 private final PdfRenderer renderer = mock(PdfRenderer.class);
 private final ConvocationService service = new ConvocationService(appointments, users, slots, professionals, renderer);
 private AppUser patient;
 private Appointment appointment;
 private Slot slot;
 private Professional professional;
 private final byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

 @BeforeEach void setUp() {
  patient = new AppUser();
  patient.id = 7L;
  patient.name = "Élodie Exemple";
  appointment = new Appointment();
  appointment.id = 42L;
  appointment.patientId = patient.id;
  appointment.slotId = 9L;
  appointment.status = "CONFIRMED";
  slot = new Slot();
  slot.id = appointment.slotId;
  slot.professionalId = 11L;
  slot.startsAt = Instant.parse("2026-01-15T10:00:00Z");
  professional = new Professional();
  professional.id = slot.professionalId;
  professional.name = "Dr Aline Martin";
  professional.specialty = "general";
  professional.address = "1 rue de l'École";
  professional.city = "Paris";
 }

 @Test void buildsAnOwnedConfirmedAppointmentUsingOnlyAppointmentIdForTheFilename() {
  when(users.findByEmail("elodie@example.test")).thenReturn(Optional.of(patient));
  when(appointments.findById(42L)).thenReturn(Optional.of(appointment));
  when(slots.findById(9L)).thenReturn(Optional.of(slot));
  when(professionals.findById(11L)).thenReturn(Optional.of(professional));
  when(renderer.render(any(ConvocationDocument.class))).thenReturn(pdf);

  assertArrayEquals(pdf, service.build(42L, "elodie@example.test"));
  assertEquals("convocation-rdv-42.pdf", ConvocationService.filename(42L));
  verify(renderer).render(new ConvocationDocument(
   42L, "Élodie Exemple", "Dr Aline Martin", "general", "1 rue de l'École", "Paris",
   "jeudi 15 janvier 2026 à 11:00"
  ));
 }

 @Test void hidesAppointmentsThatDoNotBelongToTheAuthenticatedPatient() {
  var anotherPatient = new AppUser();
  anotherPatient.id = 8L;
  when(users.findByEmail("other@example.test")).thenReturn(Optional.of(anotherPatient));
  when(appointments.findById(42L)).thenReturn(Optional.of(appointment));

  var error = assertThrows(ResponseStatusException.class, () -> service.build(42L, "other@example.test"));

  assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
  verify(renderer, never()).render(any());
 }

 @Test void rejectsCancelledAppointmentsWithConflict() {
  when(users.findByEmail("elodie@example.test")).thenReturn(Optional.of(patient));
  appointment.status = "CANCELLED";
  when(appointments.findById(42L)).thenReturn(Optional.of(appointment));

  var error = assertThrows(ResponseStatusException.class, () -> service.build(42L, "elodie@example.test"));

  assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
  verify(renderer, never()).render(any());
 }
}
