package fr.medislot.api;

import fr.medislot.application.AbService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ab")
public class AbController {
    private final AbService service;
    private final boolean secureCookie;

    public AbController(
            AbService service,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookie) {
        this.service = service;
        this.secureCookie = secureCookie;
    }

    public record AssignmentResponse(Long id, String variant, boolean enabled) {}
    public record ExposureRequest(@NotNull @Positive Long id) {}

    @GetMapping("/assignment")
    public ResponseEntity<AssignmentResponse> assignment(
            @CookieValue(name = "ms_uid", required = false) String cookie,
            HttpServletResponse response) {
        String visitorId = validVisitorId(cookie);
        boolean newCookie = visitorId == null && service.isEnabled();
        if (newCookie) {
            visitorId = UUID.randomUUID().toString();
        }

        AbService.Assignment assignment = service.assignmentFor(visitorId);
        if (newCookie) {
            response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("ms_uid", visitorId)
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofDays(365))
                    .build()
                    .toString());
        }
        return ResponseEntity.ok(new AssignmentResponse(
                assignment.id(), assignment.variant(), assignment.enabled()));
    }

    @PostMapping("/exposure")
    public ResponseEntity<Void> expose(
            @CookieValue(name = "ms_uid", required = false) String cookie,
            @Valid @RequestBody ExposureRequest request) {
        service.expose(request.id(), validVisitorId(cookie));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/metrics")
    public List<AbService.VariantMetrics> metrics() {
        return service.metrics();
    }

    private String validVisitorId(String value) {
        if (value == null || !value.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return null;
        }
        return UUID.fromString(value).toString();
    }
}
