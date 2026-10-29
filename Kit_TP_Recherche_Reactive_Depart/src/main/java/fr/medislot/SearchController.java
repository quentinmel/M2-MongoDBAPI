package fr.medislot;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/api")
public class SearchController {
  private final SearchService service;
  public SearchController(SearchService service) { this.service = service; }
  public record ProfessionalView(String id, String name, String specialty, String city) {
    static ProfessionalView from(Professional p) {
      return new ProfessionalView(p.id(),p.name(),p.specialty(),p.city());
    }
  }
  public record SearchResponse(String query, List<ProfessionalView> items) {}
  @GetMapping("/professionals")
  public ResponseEntity<SearchResponse> search(@RequestParam String q) {
    var items = service.search(q).stream().map(ProfessionalView::from).toList();
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(new SearchResponse(q.strip(), items));
  }
}
