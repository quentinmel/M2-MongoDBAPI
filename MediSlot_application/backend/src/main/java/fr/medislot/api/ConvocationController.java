package fr.medislot.api;

import fr.medislot.application.ConvocationService;
import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class ConvocationController {
 private final ConvocationService convocations;

 public ConvocationController(ConvocationService convocations) {
  this.convocations = convocations;
 }

 @GetMapping("/{id}/convocation.pdf")
 public ResponseEntity<byte[]> get(@PathVariable Long id, Principal principal) {
  byte[] pdf = convocations.build(id, principal.getName());
  return ResponseEntity.ok()
   .contentType(MediaType.APPLICATION_PDF)
   .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(ConvocationService.filename(id)).build().toString())
   .cacheControl(CacheControl.noStore())
   .header("X-Content-Type-Options", "nosniff")
   .contentLength(pdf.length)
   .body(pdf);
 }
}
