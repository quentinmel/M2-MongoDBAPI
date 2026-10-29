package fr.medislot.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SlotTakenException extends ResponseStatusException {
    public SlotTakenException() {
        super(HttpStatus.CONFLICT, "Ce créneau n’est plus disponible. Choisissez-en un autre.");
    }
}
