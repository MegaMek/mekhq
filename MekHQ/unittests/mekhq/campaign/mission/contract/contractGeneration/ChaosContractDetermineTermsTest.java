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
package mekhq.campaign.mission.contract.contractGeneration;

import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_EIGHT;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_ELEVEN;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_FIVE;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_FOUR;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_NINE;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_SEVEN;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_SIX;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_THREE;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import megamek.common.compute.Compute;
import mekhq.campaign.mission.contract.contractData.ContractTermsData;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests the roll-to-step tables in {@link ChaosContractDetermineTerms}.
 *
 * <p>Every clause rolls the same stubbed 2d6, then shifts the base step by (objective modifier + employer modifier).
 * The cases below pair {@link ChaosObjectiveType#RAID} with {@link ChaosEmployerType#NOBLE}: NOBLE is neutral on every
 * clause and RAID is neutral on all but salvage (-1), so the pay/support/transport/command results read the switch
 * tables directly and only salvage carries a one-step drop. That keeps the roll tables - the typo-prone part - pinned
 * without re-deriving the modifier arithmetic.</p>
 */
class ChaosContractDetermineTermsTest {

    private static ContractTermsData termsForRoll(final int roll) {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(roll);
            return ChaosContractDetermineTerms.determineInitialTerms(ChaosObjectiveType.RAID, ChaosEmployerType.NOBLE,
                  null, false);
        }
    }

    @Test
    void lowestRollReadsTheBottomOfEachTable() {
        // Salvage: base STEP_THREE dropped one step by RAID's -1 salvage modifier -> STEP_TWO.
        assertEquals(new ContractTermsData(STEP_THREE, STEP_THREE, STEP_FIVE, STEP_TWO, STEP_THREE), termsForRoll(2));
    }

    @Test
    void midRollReadsTheMiddleOfEachTable() {
        // Salvage: base STEP_FOUR dropped one step -> STEP_THREE.
        assertEquals(new ContractTermsData(STEP_FIVE, STEP_FOUR, STEP_SIX, STEP_THREE, STEP_SEVEN), termsForRoll(7));
    }

    @Test
    void highestRollReadsTheTopOfEachTable() {
        // Salvage: base STEP_SEVEN dropped one step -> STEP_SIX.
        assertEquals(new ContractTermsData(STEP_EIGHT, STEP_SEVEN, STEP_NINE, STEP_SIX, STEP_ELEVEN), termsForRoll(12));
    }

    @Test
    void theEmployerModifierShiftsTheStep() {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(2);
            // CORPORATION carries a +2 pay-rate modifier; RAID adds nothing to pay, so base STEP_THREE climbs to
            // STEP_FIVE. This proves the employer modifier reaches influenceStep rather than being dropped.
            ContractTermsData terms = ChaosContractDetermineTerms.determineInitialTerms(ChaosObjectiveType.RAID,
                  ChaosEmployerType.CORPORATION, null, false);

            assertEquals(STEP_FIVE, terms.payRate());
        }
    }
}
