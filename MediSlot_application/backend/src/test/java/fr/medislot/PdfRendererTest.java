package fr.medislot;

import fr.medislot.application.ConvocationDocument;
import fr.medislot.application.PdfRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.spring6.SpringTemplateEngine;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfRendererTest {
 @Test void rendersAnAccessibleUnicodePdfWithTheEmbeddedFont() throws Exception {
  var resolver = new ClassLoaderTemplateResolver();
  resolver.setPrefix("templates/");
  resolver.setSuffix(".html");
  resolver.setTemplateMode(TemplateMode.HTML);
  resolver.setCharacterEncoding("UTF-8");
  var templates = new SpringTemplateEngine();
  templates.setTemplateResolver(resolver);
  var renderer = new PdfRenderer(templates);
  byte[] result = renderer.render(new ConvocationDocument(
   42L,
   "Élodie Exemple",
   "Dr Aline Martin",
   "Médecine générale",
   "1 rue de l'École",
   "Paris",
   "jeudi 15 janvier 2026 à 11:00"
  ));

  assertTrue(new String(result, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"));
  assertTrue(result.length > 10_000);
  try (var pdf = PDDocument.load(result)) {
   String text = new PDFTextStripper().getText(pdf);
   assertTrue(text.contains("Élodie Exemple"));
   assertTrue(text.contains("jeudi 15 janvier 2026 à 11:00"));
  }
 }
}
