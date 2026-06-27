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

package mekhq.campaign.personnel.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import megamek.common.compute.Compute;

class ActionCheckRollTest {

    @Test
    void performNormalRoll() {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(Compute::d6).thenReturn(3, 4);

            ActionCheckRoll checkRoll = ActionCheckRoll.perform(ActionCheckRoll.RollType.NORMAL);

            assertEquals(7, checkRoll.result());
            assertEquals(Arrays.asList(3, 4), checkRoll.individualDice());
        }
    }

    @Test
    void performAdvantageRoll() {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(Compute::d6).thenReturn(2, 5, 4);

            ActionCheckRoll checkRoll = ActionCheckRoll.perform(ActionCheckRoll.RollType.ADVANTAGE);

            assertEquals(9, checkRoll.result());
            assertEquals(Arrays.asList(2, 5, 4), checkRoll.individualDice());
        }
    }

    @Test
    void performDisadvantageRoll() {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(Compute::d6).thenReturn(6, 1, 4);

            ActionCheckRoll checkRoll = ActionCheckRoll.perform(ActionCheckRoll.RollType.DISADVANTAGE);

            assertEquals(5, checkRoll.result());
            assertEquals(Arrays.asList(6, 1, 4), checkRoll.individualDice());
        }
    }
}
