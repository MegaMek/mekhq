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
