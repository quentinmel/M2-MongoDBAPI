package fr.medislot.application;

public record ConvocationDocument(
 Long appointmentId,
 String patientName,
 String professionalName,
 String specialty,
 String address,
 String city,
 String appointmentDateTime
) {}
