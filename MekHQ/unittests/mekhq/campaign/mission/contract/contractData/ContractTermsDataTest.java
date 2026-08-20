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

import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_FIVE;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_NINE;
import static mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable.STEP_SEVEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ContractTermsData}'s per-clause withers, which the negotiation table uses to raise one clause at a time.
 * Each wither must leave every other clause on its negotiated step, so that raising one clause cannot silently undo an
 * earlier round of negotiation.
 */
class ContractTermsDataTest {
    private static final ContractTermsData BASELINE = new ContractTermsData(STEP_SEVEN,
          STEP_SEVEN,
          STEP_SEVEN,
          STEP_SEVEN,
          STEP_SEVEN);

    @Test
    void raisingOneClauseLeavesTheOthersUntouched() {
        ContractTermsData raised = BASELINE.withPayRate(STEP_NINE);

        assertEquals(STEP_NINE, raised.payRate(), "the named clause moves");
        assertEquals(STEP_SEVEN, raised.support(), "an untouched clause must keep its negotiated step");
        assertEquals(STEP_SEVEN, raised.transport());
        assertEquals(STEP_SEVEN, raised.salvageRights());
        assertEquals(STEP_SEVEN, raised.commandRights());
    }

    @Test
    void everyClauseCanBeReplacedIndependently() {
        assertEquals(STEP_NINE, BASELINE.withPayRate(STEP_NINE).payRate());
        assertEquals(STEP_NINE, BASELINE.withSupport(STEP_NINE).support());
        assertEquals(STEP_NINE, BASELINE.withTransport(STEP_NINE).transport());
        assertEquals(STEP_NINE, BASELINE.withSalvageRights(STEP_NINE).salvageRights());
        assertEquals(STEP_NINE, BASELINE.withCommandRights(STEP_NINE).commandRights());
    }

    @Test
    void chainingWithersAppliesAllOfThem() {
        ContractTermsData replaced = BASELINE.withPayRate(STEP_NINE)
                                             .withSupport(STEP_FIVE)
                                             .withTransport(STEP_NINE)
                                             .withSalvageRights(STEP_FIVE)
                                             .withCommandRights(STEP_NINE);

        assertEquals(new ContractTermsData(STEP_NINE, STEP_FIVE, STEP_NINE, STEP_FIVE, STEP_NINE), replaced);
    }
}
