package fr.medislot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import fr.medislot.application.AbService;

class AbServiceTest {
    private final AbService service = new AbService(null, true);

    @Test
    void hashesTheExperimentAndVisitorIntoAnEvenSplit() {
        assertEquals("A", service.variantFor(
                AbService.EXPERIMENT_KEY, "00000000-0000-0000-0000-000000000001"));
        assertEquals("B", service.variantFor(AbService.EXPERIMENT_KEY, "visitor-1"));
        assertEquals("B", service.variantFor(AbService.EXPERIMENT_KEY, "visitor-1"));
    }
}
