package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

class WreckRecoveryTest {
    private final TestUnit wreck = mock(TestUnit.class);
    private final WreckRecovery recovery = new WreckRecovery(wreck);

    @Test
    void newRecoveryIsUnassigned() {
        assertSame(wreck, recovery.getWreck());
        assertEquals(RecoveryStatus.UNASSIGNED, recovery.getStatus());
        assertFalse(recovery.isRecovered());
        assertFalse(recovery.hasRecoveryUnits());
        assertTrue(recovery.getRecoveryUnits().isEmpty());
        assertNull(recovery.getFirstUnit());
        assertNull(recovery.getSecondUnit());
        assertNull(recovery.getRecoveryMethod());
        assertFalse(recovery.isRecoveryMethodChoosable());
        assertNull(recovery.getPreferredRecoveryMethod());
    }

    @Test
    void recoveryUnitsAreListedInSlotOrder() {
        Unit first = mock(Unit.class);
        Unit second = mock(Unit.class);
        recovery.setRecoveryUnits(first, second);

        assertTrue(recovery.hasRecoveryUnits());
        assertEquals(List.of(first, second), recovery.getRecoveryUnits());
        assertSame(first, recovery.getFirstUnit());
        assertSame(second, recovery.getSecondUnit());
    }

    @Test
    void eitherSlotCountsAsAssigned() {
        Unit second = mock(Unit.class);
        recovery.setRecoveryUnits(null, second);

        assertTrue(recovery.hasRecoveryUnits());
        assertEquals(List.of(second), recovery.getRecoveryUnits());
    }

    @Test
    void clearingTheSlotsUnassignsTheWreck() {
        recovery.setRecoveryUnits(mock(Unit.class), mock(Unit.class));
        recovery.setRecoveryUnits(null, null);

        assertFalse(recovery.hasRecoveryUnits());
    }

    @Test
    void recoveredFollowsStatus() {
        recovery.status = RecoveryStatus.COMMITTED;
        assertTrue(recovery.isRecovered());

        recovery.status = RecoveryStatus.UNIT_IN_USE;
        assertFalse(recovery.isRecovered());
    }

    @Test
    void preferredRecoveryMethodIsStored() {
        recovery.setPreferredRecoveryMethod(RecoveryMethod.DRAG);
        assertEquals(RecoveryMethod.DRAG, recovery.getPreferredRecoveryMethod());
    }
}
