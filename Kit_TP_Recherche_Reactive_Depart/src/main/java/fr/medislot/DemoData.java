package fr.medislot;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.MongoTemplate;
@Component
@Profile("lab")
public class DemoData implements CommandLineRunner {
  private final MongoTemplate mongo;
  public DemoData(MongoTemplate mongo) { this.mongo = mongo; }
  public void run(String... args) {
    String[][] rows = {
      {"p01","Claire Martin","Généraliste","Paris"},
      {"p02","Hugo Marchand","Dermatologue","Lyon"},
      {"p03","Léa Moreau","Cardiologue","Paris"},
      {"p04","Nadia Mercier","Pédiatre","Lille"},
      {"p05","Thomas Martin","Kinésithérapeute","Bordeaux"},
      {"p06","Inès Bernard","Ophtalmologue","Nantes"},
      {"p07","Sami Robert","Dentiste","Paris"},
      {"p08","Julien Dubois","Généraliste","Rennes"},
      {"p09","Sarah Petit","Dermatologue","Toulouse"},
      {"p10","Emma Durand","Cardiologue","Lyon"},
      {"p11","Théo Leroy","Pédiatre","Strasbourg"},
      {"p12","Nora Simon","Généraliste","Dijon"}
    };
    for (String[] r : rows) {
      if (mongo.findById(r[0], Professional.class) == null)
        mongo.insert(new Professional(r[0],r[1],r[2],r[3],
          SearchText.normalize(r[1]),SearchText.normalize(r[2])));
    }
  }
}
