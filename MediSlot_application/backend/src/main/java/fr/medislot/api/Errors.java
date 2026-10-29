package fr.medislot.api;
import org.springframework.web.bind.annotation.*;
import fr.medislot.application.SlotTakenException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
public class Errors {
 @ExceptionHandler(SlotTakenException.class) public ResponseEntity<ProblemDetail> slotTaken(SlotTakenException e){var p=ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason());p.setProperty("code","SLOT_TAKEN");return ResponseEntity.status(e.getStatusCode()).body(p);}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ProblemDetail> status(ResponseStatusException e){var p=ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()==null?"Demande refusée":e.getReason());return ResponseEntity.status(e.getStatusCode()).body(p);}
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e){var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Vérifiez les données saisies.");p.setProperty("errors",e.getBindingResult().getFieldErrors().stream().map(f->java.util.Map.of("field",f.getField(),"message",validationMessage(f))).toList());return ResponseEntity.badRequest().body(p);}
 @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<ProblemDetail> conflict(){return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"La demande entre en conflit avec les données actuelles."));}
 private String validationMessage(org.springframework.validation.FieldError error){
  String field=error.getField();
  String code=error.getCode();
  if("NotBlank".equals(code))return switch(field){case "email"->"L’adresse e-mail est obligatoire.";case "password"->"Le mot de passe est obligatoire.";case "name"->"Le nom est obligatoire.";default->"Ce champ est obligatoire.";};
  if("Email".equals(code))return "Saisissez une adresse e-mail valide.";
  if("Size".equals(code))return switch(field){case "email"->"L’adresse e-mail ne doit pas dépasser 254 caractères.";case "password"->error.getRejectedValue() instanceof String value&&value.length()<12?"Le mot de passe doit contenir au moins 12 caractères.":"Le mot de passe ne doit pas dépasser 72 caractères.";case "name"->"Le nom ne doit pas dépasser 120 caractères.";default->"La valeur saisie a une longueur invalide.";};
  return "La valeur saisie est invalide.";
 }
}
