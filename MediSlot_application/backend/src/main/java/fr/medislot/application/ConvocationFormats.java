package fr.medislot.application;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ConvocationFormats {
 public static final ZoneId PARIS = ZoneId.of("Europe/Paris");
 private static final DateTimeFormatter DATE_TIME =
  DateTimeFormatter.ofPattern("EEEE d MMMM uuuu 'à' HH:mm", Locale.FRENCH).withZone(PARIS);

 private ConvocationFormats() {}

 public static String dateTime(Instant instant) {
  return DATE_TIME.format(instant);
 }
}
