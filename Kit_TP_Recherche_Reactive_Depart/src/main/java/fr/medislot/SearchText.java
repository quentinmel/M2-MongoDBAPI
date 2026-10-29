package fr.medislot;
import java.text.Normalizer;
import java.util.Locale;
public final class SearchText {
  private SearchText() {}
  public static String normalize(String text) {
    return Normalizer.normalize(text.strip(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
  }
}
