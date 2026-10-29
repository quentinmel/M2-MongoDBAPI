package fr.medislot.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AbService {
    public static final String EXPERIMENT_KEY = "booking-cta";

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public AbService(JdbcTemplate jdbc, @Value("${medislot.ab-enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    public record Assignment(Long id, String variant, boolean enabled) {}
    public record VariantMetrics(String variant, long assigned, long exposed, long converted) {}

    public boolean isEnabled() {
        return enabled;
    }

    public Assignment assignmentFor(String visitorId) {
        if (!enabled) {
            return new Assignment(null, "A", false);
        }

        String variant = variantFor(EXPERIMENT_KEY, visitorId);
        jdbc.update("""
                INSERT INTO ab_assignment (experiment_key, visitor_id, variant)
                VALUES (?, ?, ?)
                ON CONFLICT (experiment_key, visitor_id) DO NOTHING
                """, EXPERIMENT_KEY, visitorId, variant);

        return jdbc.queryForObject("""
                SELECT id, variant
                FROM ab_assignment
                WHERE experiment_key = ? AND visitor_id = ?
                """, (rs, row) -> new Assignment(rs.getLong("id"), rs.getString("variant"), true),
                EXPERIMENT_KEY, visitorId);
    }

    public String variantFor(String experimentKey, String visitorId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((experimentKey + ":" + visitorId).getBytes(StandardCharsets.UTF_8));
            return (digest[0] & 0xFF) < 128 ? "A" : "B";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public boolean expose(long id, String visitorId) {
        if (!enabled || visitorId == null) {
            return false;
        }
        return jdbc.update("""
                UPDATE ab_assignment
                SET exposed = TRUE, exposed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND experiment_key = ? AND visitor_id = ? AND exposed = FALSE
                """, id, EXPERIMENT_KEY, visitorId) == 1;
    }

    public void recordConversion(String visitorId) {
        if (!enabled || visitorId == null) {
            return;
        }
        jdbc.update("""
                UPDATE ab_assignment
                SET converted = TRUE
                WHERE experiment_key = ? AND visitor_id = ? AND exposed = TRUE AND converted = FALSE
                """, EXPERIMENT_KEY, visitorId);
    }

    public List<VariantMetrics> metrics() {
        return jdbc.query("""
                SELECT variants.variant,
                       COUNT(a.id) AS assigned,
                       COUNT(a.id) FILTER (WHERE a.exposed) AS exposed,
                       COUNT(a.id) FILTER (WHERE a.converted) AS converted
                FROM (VALUES ('A'), ('B')) AS variants(variant)
                LEFT JOIN ab_assignment a
                    ON a.experiment_key = ? AND a.variant = variants.variant
                GROUP BY variants.variant
                ORDER BY variants.variant
                """, (rs, row) -> new VariantMetrics(
                rs.getString("variant"),
                rs.getLong("assigned"),
                rs.getLong("exposed"),
                rs.getLong("converted")), EXPERIMENT_KEY);
    }
}
