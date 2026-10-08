package fr.medislot;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@Profile("lab")
@RestController
@RequestMapping("/api/lab")
public class LabController {
  private final MongoTemplate mongo;
  private final SlotEvents events;
  public LabController(MongoTemplate mongo, SlotEvents events) {
    this.mongo = mongo; this.events = events;
  }
  @PostMapping("/slots")
  @ResponseStatus(HttpStatus.CREATED)
  public SlotController.SlotView create() {
    Slot slot = mongo.insert(new Slot(UUID.randomUUID().toString(),
      Instant.now().plusSeconds(86400).toString()));
    events.changed(slot);
    return SlotController.SlotView.from(slot);
  }
  @GetMapping("/slots/{id}/proof")
  public Map<String, Object> proof(@PathVariable String id) {
    Slot s = mongo.findById(id, Slot.class);
    if (s == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return Map.of("id", s.id, "status", s.status, "claimCount", s.claimCount,
      "version", s.version, "bookingId", s.bookingId == null ? "" : s.bookingId);
  }
}
