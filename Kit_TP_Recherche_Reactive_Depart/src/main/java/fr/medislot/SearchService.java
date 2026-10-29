package fr.medislot;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class SearchService {
  private final MongoTemplate mongo;
  private final boolean lab;
  public SearchService(MongoTemplate mongo, Environment env) {
    this.mongo = mongo; this.lab = env.acceptsProfiles(Profiles.of("lab"));
  }
  public List<Professional> search(String raw) {
    String q = raw.strip();
    if (q.length() < 2 || q.length() > 60)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "q doit contenir entre 2 et 60 caractères");
    String normalized = SearchText.normalize(q);
    if (lab) simulate(normalized);
    var literal = Pattern.compile(Pattern.quote(normalized));
    var filter = new Criteria().orOperator(
        Criteria.where("searchName").regex(literal),
        Criteria.where("searchSpecialty").regex(literal));
    var query = Query.query(filter)
        .with(Sort.by("searchName", "_id")).limit(20);
    return mongo.find(query, Professional.class);
  }
  private void simulate(String q) {
    if (q.equals("erreur"))
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Erreur simulée dans le laboratoire");
    try { Thread.sleep(q.equals("ma") ? 1200 : q.equals("mar") ? 100 : 180); }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    }
  }
}
