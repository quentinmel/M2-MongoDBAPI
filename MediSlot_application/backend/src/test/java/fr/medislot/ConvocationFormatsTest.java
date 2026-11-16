package fr.medislot;

import fr.medislot.application.ConvocationFormats;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConvocationFormatsTest {
 @Test void formatsTheAppointmentInParisTimeWithFrenchAccents() {
  assertEquals("jeudi 15 janvier 2026 à 11:00", ConvocationFormats.dateTime(Instant.parse("2026-01-15T10:00:00Z")));
 }
}
