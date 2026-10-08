package fr.medislot;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
@RestController
@RequestMapping("/api")
public class SlotController {
  private final SlotService service;
  private final SlotEvents events;
  public SlotController(SlotService service, SlotEvents events) {
    this.service = service; this.events = events;
  }
  public record SlotView(String id, String startsAt, String status, long version) {
    static SlotView from(Slot s) {
      return new SlotView(s.id, s.startsAt, s.status, s.version);
    }
  }
  public record BookingRequest(@NotBlank @Size(max=50) String customerId) {}
  public record BookingView(String bookingId, String slotId, String status) {}
  @GetMapping("/slots")
  public List<SlotView> list() {
    return service.all().stream().map(SlotView::from).toList();
  }
  @PostMapping("/slots/{id}/reservations")
  @ResponseStatus(HttpStatus.CREATED)
  public BookingView reserve(@PathVariable String id,
      @Valid @RequestBody BookingRequest body) {
    Slot s = service.reserve(id, body.customerId());
    return new BookingView(s.bookingId, s.id, s.status);
  }
  @GetMapping(value="/slots/events", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream() { return events.subscribe(); }
}
