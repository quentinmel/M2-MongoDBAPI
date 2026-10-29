package fr.medislot;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Document("professionals")
public record Professional(@Id String id, String name, String specialty,
    String city, String searchName, String searchSpecialty) {}
