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
package mekhq.campaign.mission.contract.contractData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Exercises the step arithmetic on {@link ChaosContractStepsTable}, in particular
 * {@link ChaosContractStepsTable#influenceStep(int)}: applying a delta, clamping to the 1..17 range, and always
 * resolving to a real step.
 */
class ChaosContractStepsTableTest {

    @ParameterizedTest
    @CsvSource({ "STEP_ONE, 1", "STEP_TWO, 2", "STEP_THREE, 3", "STEP_FOUR, 4", "STEP_FIVE, 5", "STEP_SIX, 6",
                 "STEP_SEVEN, 7", "STEP_EIGHT, 8", "STEP_NINE, 9", "STEP_TEN, 10", "STEP_ELEVEN, 11", "STEP_TWELVE, 12",
                 "STEP_THIRTEEN, 13", "STEP_FOURTEEN, 14", "STEP_FIFTEEN, 15", "STEP_SIXTEEN, 16",
                 "STEP_SEVENTEEN, 17" })
    void stepValueMatchesOrdinalPosition(final ChaosContractStepsTable step, final int expectedValue) {
        assertEquals(expectedValue, step.stepValue());
    }

    @ParameterizedTest
    @EnumSource(ChaosContractStepsTable.class)
    void influenceStepWithZeroDeltaReturnsSameStep(final ChaosContractStepsTable step) {
        assertSame(step, step.influenceStep(0));
    }

    @ParameterizedTest
    @CsvSource({ "STEP_THREE, 2, STEP_FIVE", "STEP_FIVE, -4, STEP_ONE", "STEP_TEN, 3, STEP_THIRTEEN",
                 "STEP_ONE, 16, STEP_SEVENTEEN", "STEP_SEVEN, -6, STEP_ONE", "STEP_EIGHT, 1, STEP_NINE" })
    void influenceStepAppliesTheDelta(final ChaosContractStepsTable step, final int delta,
          final ChaosContractStepsTable expected) {
        assertSame(expected, step.influenceStep(delta));
    }

    @ParameterizedTest
    @EnumSource(ChaosContractStepsTable.class)
    void influenceStepClampsBelowTheBottomStep(final ChaosContractStepsTable step) {
        assertSame(ChaosContractStepsTable.STEP_ONE, step.influenceStep(-100),
              "any step reduced far below the minimum must clamp to STEP_ONE");
    }

    @ParameterizedTest
    @EnumSource(ChaosContractStepsTable.class)
    void influenceStepClampsAboveTheTopStep(final ChaosContractStepsTable step) {
        assertSame(ChaosContractStepsTable.STEP_SEVENTEEN, step.influenceStep(100),
              "any step raised far above the maximum must clamp to STEP_SEVENTEEN");
    }

    @ParameterizedTest
    @EnumSource(ChaosContractStepsTable.class)
    void influenceStepNeverReturnsNullAcrossAWideDeltaRange(final ChaosContractStepsTable step) {
        for (int delta = -30; delta <= 30; delta++) {
            assertNotNull(step.influenceStep(delta),
                  "influenceStep must always resolve to a real step (" + step + " + " + delta + ")");
        }
    }
}
