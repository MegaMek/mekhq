package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecoveryTimeDataTest {
    private final RecoveryTimeData data = new RecoveryTimeData(0.25, 0.5, 0.25, 0.5, 0.5, 0.25, 60, 195);

    @Test
    void breakdownCanBeWrappedInHtml() {
        String breakdown = data.getRecoveryTimeBreakdownString(true);

        assertTrue(breakdown.startsWith("<html>"), breakdown);
        assertTrue(breakdown.endsWith("</html>"), breakdown);
    }

    @Test
    void plainBreakdownHasNoHtmlWrapper() {
        String breakdown = data.getRecoveryTimeBreakdownString(false);

        assertFalse(breakdown.startsWith("<html>"), breakdown);
        assertFalse(breakdown.endsWith("</html>"), breakdown);
    }

    @Test
    void breakdownIsFullyLocalized() {
        String breakdown = data.getRecoveryTimeBreakdownString(false);

        // A missing resource key comes back wrapped in '!'
        assertFalse(breakdown.contains("!RecoveryTimeData"), breakdown);
    }

    @Test
    void breakdownShowsTheTotals() {
        String breakdown = data.getRecoveryTimeBreakdownString(false);

        assertTrue(breakdown.contains("195"), breakdown);
        // 1.0 + 0.25 + 0.5 + 0.25 + 0.5 + 0.5 + 0.25
        assertTrue(breakdown.contains("3.25") || breakdown.contains("3,25"), breakdown);
    }
}
