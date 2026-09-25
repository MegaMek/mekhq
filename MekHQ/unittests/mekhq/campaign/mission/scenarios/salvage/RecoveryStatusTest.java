package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RecoveryStatusTest {
    private static final Set<RecoveryStatus> RECOVERED_STATUSES = EnumSet.of(RecoveryStatus.RECOVERED_AUTOMATICALLY,
          RecoveryStatus.RECOVERED, RecoveryStatus.CARRIED_IN_CARGO, RecoveryStatus.CARRIED_IN_BAY,
          RecoveryStatus.COMMITTED);

    @ParameterizedTest
    @EnumSource(RecoveryStatus.class)
    void recoveredStatuses(RecoveryStatus status) {
        assertEquals(RECOVERED_STATUSES.contains(status), status.isRecovered());
    }

    @ParameterizedTest
    @EnumSource(RecoveryStatus.class)
    void everyUnrecoveredStatusButUnassignedIsAProblem(RecoveryStatus status) {
        boolean isExpectedProblem = !RECOVERED_STATUSES.contains(status) && (status != RecoveryStatus.UNASSIGNED);
        assertEquals(isExpectedProblem, status.isProblem());
    }

    @Test
    void unassignedHasNoLabel() {
        assertEquals("", RecoveryStatus.UNASSIGNED.getLabel());
    }

    @ParameterizedTest
    @EnumSource(value = RecoveryStatus.class, names = "UNASSIGNED", mode = EnumSource.Mode.EXCLUDE)
    void everyOtherStatusHasALocalizedLabel(RecoveryStatus status) {
        String label = status.getLabel();
        assertFalse(label.isBlank(), status.name());
        // A missing resource key comes back wrapped in '!'
        assertFalse(label.startsWith("!"), label);
    }

    @ParameterizedTest
    @EnumSource(RecoveryMethod.class)
    void recoveryMethodsAreLocalized(RecoveryMethod recoveryMethod) {
        String label = recoveryMethod.toString();
        assertFalse(label.isBlank(), recoveryMethod.name());
        assertFalse(label.startsWith("!"), label);
    }

    @Test
    void recoveryMethodLabelsDiffer() {
        assertTrue(!RecoveryMethod.CARRY.toString().equals(RecoveryMethod.DRAG.toString()));
    }
}
