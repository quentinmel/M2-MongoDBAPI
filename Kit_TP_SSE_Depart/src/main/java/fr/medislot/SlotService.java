package fr.medislot;

import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SlotService {
  private final MongoTemplate mongo;
  private final SlotEvents events;

  public SlotService(MongoTemplate mongo, SlotEvents events) {
    this.mongo = mongo;
    this.events = events;
  }

  public List<Slot> all() { 
    return mongo.findAll(Slot.class); 
  }

  // Méthode pour créer un créneau de test (appelée par le profil lab)
  public Slot createTestSlot() {
    Slot slot = new Slot();
    slot.status = "AVAILABLE";
    slot.version = 1L;
    slot.startsAt = java.time.Instant.now().plusSeconds(3600).toString();
    
    Slot saved = mongo.save(slot);
    
    // Notification SSE indispensable
    events.changed(saved);
    return saved;
  }

  public Slot reserve(String id, String customerId) {
    var filter = Query.query(Criteria.where("_id").is(id)
      .and("status").is("AVAILABLE"));
    var change = new Update().set("status", "BOOKED")
      .set("bookedBy", customerId)
      .set("bookingId", UUID.randomUUID().toString())
      .inc("version", 1).inc("claimCount", 1);
    Slot winner = mongo.findAndModify(filter, change,
      FindAndModifyOptions.options().returnNew(true), Slot.class);
    if (winner == null) {
      boolean exists = mongo.exists(
        Query.query(Criteria.where("_id").is(id)), Slot.class);
      throw new ResponseStatusException(exists ? HttpStatus.CONFLICT
        : HttpStatus.NOT_FOUND, exists ? "Créneau déjà réservé" : "Créneau inconnu");
    }
    events.changed(winner);
    return winner;
  }
}