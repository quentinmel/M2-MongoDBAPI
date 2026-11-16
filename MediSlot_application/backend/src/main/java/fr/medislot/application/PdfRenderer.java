package fr.medislot.application;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRRuntimeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
public class PdfRenderer {
 private final SpringTemplateEngine templates;

 public PdfRenderer(SpringTemplateEngine templates) {
  this.templates = templates;
 }

 public byte[] render(ConvocationDocument document) {
  Context context = new Context(Locale.FRENCH);
  context.setVariable("document", document);
  String html = templates.process("convocation", context);

  try (var output = new ByteArrayOutputStream()) {
   PdfRendererBuilder builder = new PdfRendererBuilder();
   builder.useFastMode();
   builder.useFont(font("/fonts/DejaVuSans.ttf"), "DejaVu Sans");
   builder.useFont(font("/fonts/DejaVuSans-Bold.ttf"), "DejaVu Sans", 700, PdfRendererBuilder.FontStyle.NORMAL, true);
   builder.withHtmlContent(html, "classpath:/templates/");
   builder.toStream(output);
   builder.run();
   return output.toByteArray();
  } catch (IOException | XRRuntimeException e) {
   throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La convocation n’a pas pu être générée.", e);
  }
 }

 private static FSSupplier<InputStream> font(String path) {
  return () -> {
   try {
    return new ClassPathResource(path).getInputStream();
   } catch (IOException e) {
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La police nécessaire à la convocation est indisponible.", e);
   }
  };
 }
}
