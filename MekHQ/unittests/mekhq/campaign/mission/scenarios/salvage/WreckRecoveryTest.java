/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
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
