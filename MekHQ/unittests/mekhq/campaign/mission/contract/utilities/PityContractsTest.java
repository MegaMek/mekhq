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
package mekhq.campaign.mission.contract.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.ContractMarket;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractMarketAvailability;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import testUtilities.MHQTestUtilities;

/**
 * Tests {@link PityContracts#generatePityContracts}, which tops a struggling force's market up with easy offers.
 *
 * <p>The shortfall is the configured pity count less the successful contracts already earned, so only successes
 * count against it - a failed or breached contract must not use up an offer the player has not benefited from. The
 * offers also have to land in the bucket the campaign's faction actually browses, or they are generated into a tab the
 * player never opens.</p>
 */
class PityContractsTest {
    /**
     * A campaign whose faction is described by the supplied predicates, holding {@code completed} contracts and
     * configured to guarantee {@code pityCount} successes.
     */
    private static Campaign campaign(int pityCount, List<AbstractContract> completed, boolean isPirate,
          boolean isMercenary, ContractMarket market) {
        Campaign campaign = MHQTestUtilities.mockCampaign();

        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(options.get(CampaignOption.PITY_CONTRACTS)).thenReturn(pityCount);
        when(campaign.getCompletedContracts()).thenReturn(completed);

        Faction faction = mock(Faction.class);
        when(faction.isPirate()).thenReturn(isPirate);
        when(faction.isMercenary()).thenReturn(isMercenary);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(campaign.getPlayerForce().getContractMarket()).thenReturn(market);

        return campaign;
    }

    private static AbstractContract completedContract(MissionStatus status) {
        AbstractContract contract = new ChaosContract();
        contract.setContractId(UUID.randomUUID());
        contract.setStatus(status);
        return contract;
    }

    /** Generation is stubbed out; what is under test is how many offers are asked for and where they are filed. */
    private static int generateWithStubbedOffers(Campaign campaign) {
        try (MockedStatic<ChaosContractMarketAvailability> availability =
                   mockStatic(ChaosContractMarketAvailability.class)) {
            availability.when(() -> ChaosContractMarketAvailability.generateProvingGroundOffer(any(), any()))
                  .thenAnswer(invocation -> completedContract(MissionStatus.ACTIVE));

            return PityContracts.generatePityContracts(campaign);
        }
    }

    @ParameterizedTest
    @CsvSource({
          "3, 0, 3",   // nothing earned yet: the full guarantee
          "3, 1, 2",
          "3, 2, 1",
          "3, 3, 0",   // the guarantee is met
          "3, 5, 0",   // and cannot go negative
          "0, 0, 0"    // the feature disabled
    })
    void theShortfallIsTheGuaranteeLessTheSuccessesAlreadyEarned(int pityCount, int successes, int expected) {
        ContractMarket market = new ContractMarket();
        List<AbstractContract> completed = java.util.stream.IntStream.range(0, successes)
                                                 .mapToObj(index -> completedContract(MissionStatus.SUCCESS))
                                                 .toList();

        int generated = generateWithStubbedOffers(campaign(pityCount, completed, false, true, market));

        assertEquals(expected, generated);
        assertEquals(expected, market.getMercenaryWork().size(), "every offer generated must reach the market");
    }

    @Test
    void onlySuccessfulContractsCountAgainstTheGuarantee() {
        ContractMarket market = new ContractMarket();
        List<AbstractContract> completed = List.of(completedContract(MissionStatus.FAILED),
              completedContract(MissionStatus.BREACH),
              completedContract(MissionStatus.PARTIAL),
              completedContract(MissionStatus.SUCCESS));

        int generated = generateWithStubbedOffers(campaign(3, completed, false, true, market));

        assertEquals(2, generated,
              "a contract the player did not succeed at must not use up an offer they never benefited from");
    }

    @ParameterizedTest
    @CsvSource({ "true, false, PIRATE", "false, true, MERCENARY", "false, false, GOVERNMENT" })
    void offersLandInTheBucketTheCampaignsFactionActuallyBrowses(boolean isPirate, boolean isMercenary,
          ContractSearchType expectedBucket) {
        ContractMarket market = new ContractMarket();

        generateWithStubbedOffers(campaign(2, List.of(), isPirate, isMercenary, market));

        assertEquals(2, market.getContracts(expectedBucket).size());
        for (ContractSearchType other : ContractSearchType.values()) {
            if (other != expectedBucket) {
                assertTrue(market.isEmpty(other), "offers must not be filed under " + other);
            }
        }
    }

    @Test
    void offersThatFailToGenerateAreSkippedRatherThanFilingNulls() {
        ContractMarket market = new ContractMarket();
        Campaign campaign = campaign(3, List.of(), false, true, market);

        int generated;
        try (MockedStatic<ChaosContractMarketAvailability> availability =
                   mockStatic(ChaosContractMarketAvailability.class)) {
            availability.when(() -> ChaosContractMarketAvailability.generateProvingGroundOffer(any(), any()))
                  .thenReturn(null);

            generated = PityContracts.generatePityContracts(campaign);
        }

        assertEquals(3, generated, "the shortfall is reported as attempted regardless of generation success");
        assertTrue(market.isEmpty(ContractSearchType.MERCENARY), "a failed generation must not file a null offer");
    }
}
