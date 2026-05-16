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
package mekhq.campaign.market.contractMarket;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.MissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import testUtilities.MHQTestUtilities;

import static mekhq.campaign.market.contractMarket.PityContracts.PITY_CONTRACT_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PityContractTest {
    Campaign campaign;
    AbstractContractMarket contractMarket;

    @BeforeEach
    void setUp() {
        campaign = MHQTestUtilities.getTestCampaign();

        contractMarket = new AtbMonthlyContractMarket();
        campaign.setContractMarket(contractMarket);
    }

    @ParameterizedTest
    @CsvSource({
          "0, 4",
          "1, 3",
          "2, 2",
          "3, 1",
          "4, 0",
          "5, 0"
    })
    void testGeneratePityContracts_allContractSuccesses(int successfulContractCount, int expectedRequestedContractCount) {
        campaign.getAtBContracts().clear();

        for (int i = 0; i < successfulContractCount; i++) {
            AtBContract contract = new AtBContract("Test Contract");
            contract.setStatus(MissionStatus.SUCCESS);
            campaign.addMission(contract);

            campaign.getAtBContracts().add(contract);
        }

        int requestedContractCount = PityContracts.generatePityContracts(campaign);

        assertEquals(expectedRequestedContractCount, requestedContractCount,
              "Invalid number of requested contracts for [" + successfulContractCount + "] successful contracts");
    }

    @ParameterizedTest
    @CsvSource({
          "0",
          "1",
          "2",
          "3",
          "4",
          "5"
    })
    void testGeneratePityContracts_allContractFailures(int failedContractCount) {
        campaign.getAtBContracts().clear();

        for (int i = 0; i < failedContractCount; i++) {
            AtBContract contract = new AtBContract("Test Contract");
            contract.setStatus(MissionStatus.FAILED);
            campaign.addMission(contract);

            campaign.getAtBContracts().add(contract);
        }

        int requestedContractCount = PityContracts.generatePityContracts(campaign);

        assertEquals(PITY_CONTRACT_COUNT, requestedContractCount,
              "Invalid number of requested contracts for [" + failedContractCount + "] successful contracts");
    }
}
