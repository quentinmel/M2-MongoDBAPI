package fr.medislot;

import fr.medislot.api.ConvocationController;
import fr.medislot.application.ConvocationService;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConvocationControllerTest {
 @Test void returnsThePdfWithPrivateInlineHeadersAndAnIdOnlyFilename() {
  var service = mock(ConvocationService.class);
  byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  when(service.build(42L, "patient@example.test")).thenReturn(pdf);
  var controller = new ConvocationController(service);
  Principal principal = () -> "patient@example.test";

  var response = controller.get(42L, principal);

  assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
  assertEquals("inline; filename=\"convocation-rdv-42.pdf\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
  assertEquals("no-store", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
  assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
  assertEquals(pdf.length, response.getHeaders().getContentLength());
 }
}
