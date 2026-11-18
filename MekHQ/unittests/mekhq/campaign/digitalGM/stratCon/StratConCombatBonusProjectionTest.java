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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.gm.StratConPlayType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link StratConCombatBonusProjection}: Essential scenarios pay from their known schedule, replacing Nav Points
 * pay three a month per point of Scale, an Objective Raid's target intelligence is capped by Scale, and a contract type
 * with no Essential scenarios of its own pays nothing.
 */
class StratConCombatBonusProjectionTest {
    private static final double DELTA = 0.0001;

    private Campaign campaign;
    private CampaignOptions campaignOptions;

    @BeforeEach
    void setUp() {
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.NORMAL);
        campaignOptions.set(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS, true);
        campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
    }

    /** A six-month, three-track, Scale 4 contract whose Essential schedule totals 5. */
    private static AbstractContract contract(ContractObjectiveType objectiveType) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        when(contract.getScenarioSchedule()).thenReturn(List.of(0, 1, 2, 0, 1, 1));
        when(contract.getLengthInMonths()).thenReturn(6);
        when(contract.getTrackCount()).thenReturn(3);
        when(contract.getScale()).thenReturn(4);
        when(contract.getExactScale()).thenReturn(4.0);
        return contract;
    }

    @Test
    void essentialScenariosPayFromTheirSchedule() {
        // Planetary Assault keeps its Essential scenarios alongside its strategic positions
        StratConCombatBonusProjection projection = StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.PLANETARY_ASSAULT));

        assertEquals(5.0, projection.bonusCount(), DELTA);
        assertFalse(projection.isFromNavPoint());
    }

    @Test
    void aContractTypeWithoutEssentialScenariosPaysNothing() {
        // Garrison Duty's definition has no Essential scenarios, and its shows of force do not replace them
        StratConCombatBonusProjection projection = StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.GARRISON_DUTY));

        assertEquals(0.0, projection.bonusCount(), DELTA);
        assertFalse(projection.isFromNavPoint());
    }

    @Test
    void navPointsReplacingEssentialScenariosPayThreeAMonthPerPointOfScale() {
        // Espionage's data caches replace its Essential scenarios: 3 x Scale 4 x 6 months
        StratConCombatBonusProjection projection = StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.ESPIONAGE));

        assertEquals(72.0, projection.bonusCount(), DELTA);
        assertTrue(projection.isFromNavPoint());
    }

    @Test
    void withoutSpecialMechanicsEssentialScenariosStillPay() {
        campaignOptions.set(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS, false);

        StratConCombatBonusProjection projection = StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.ESPIONAGE));

        assertEquals(5.0, projection.bonusCount(), DELTA);
        assertFalse(projection.isFromNavPoint());
    }

    @Test
    void objectiveRaidIsCappedAtOneFacilityPerPointOfScale() {
        StratConCombatBonusProjection projection = StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.OBJECTIVE_RAID));

        assertEquals(4.0, projection.bonusCount(), DELTA);
        assertTrue(projection.isFromNavPoint());
    }

    @Test
    void maplessPlayPaysNoCombatBonus() {
        campaignOptions.set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.MAPLESS);

        assertEquals(0.0, StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.PLANETARY_ASSAULT)).bonusCount(), DELTA);
    }

    @Test
    void withoutStratConNoCombatBonusIsPaid() {
        campaignOptions.set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.DISABLED);

        assertEquals(0.0, StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.PLANETARY_ASSAULT)).bonusCount(), DELTA);
    }

    // Combat bonuses a month, which combat pay is spread across

    @Test
    void essentialScenarioContractsSpreadCombatPayOverOneBonusAMonthPerPointOfScale() {
        assertEquals(4, StratConCombatBonusProjection.getMonthlyCombatBonusCount(campaignOptions,
              contract(ContractObjectiveType.PLANETARY_ASSAULT)));
    }

    @Test
    void navPointContractsSpreadCombatPayOverThreeBonusesAMonthPerPointOfScale() {
        assertEquals(12, StratConCombatBonusProjection.getMonthlyCombatBonusCount(campaignOptions,
              contract(ContractObjectiveType.ESPIONAGE)));
    }

    @Test
    void withoutSpecialMechanicsEveryContractIsPaidThroughEssentialScenarios() {
        campaignOptions.set(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS, false);

        assertEquals(4, StratConCombatBonusProjection.getMonthlyCombatBonusCount(campaignOptions,
              contract(ContractObjectiveType.ESPIONAGE)));
    }

    @Test
    void targetIntelligenceIsPaidAsItsFacilityFightsAre() {
        assertEquals(4, StratConCombatBonusProjection.getMonthlyCombatBonusCount(campaignOptions,
              contract(ContractObjectiveType.OBJECTIVE_RAID)));
    }

    /**
     * Nav Points and Essential scenarios are rounded separately, so at a fractional Scale the Nav Points are not always
     * exactly three times as many. Combat pay is spread across the bonuses a contract actually brings, so each contract
     * still pays its whole monthly combat pay.
     */
    @Test
    void combatPayIsSpreadAcrossTheBonusesTheScheduleBringsAtAFractionalScale() {
        AbstractContract navPointContract = contract(ContractObjectiveType.ESPIONAGE);
        when(navPointContract.getExactScale()).thenReturn(1.4);
        AbstractContract essentialContract = contract(ContractObjectiveType.PLANETARY_ASSAULT);
        when(essentialContract.getExactScale()).thenReturn(1.4);

        // 3 x 1.4 = 4.2 Nav Points a month, rounded to 4; 1.4 Essential scenarios, rounded to 1
        double navPointsPerMonth = StratConCombatBonusProjection.project(campaign, navPointContract).bonusCount() / 6;
        assertEquals(4, navPointsPerMonth, DELTA);
        assertEquals(4, StratConCombatBonusProjection.getMonthlyCombatBonusCount(campaignOptions, navPointContract));
        assertEquals(1, StratConCombatBonusProjection.getMonthlyCombatBonusCount(campaignOptions, essentialContract));
    }

    @Test
    void withoutSupportPointsInScaleEveryThreePointsOfScaleCountAsOne() {
        campaignOptions.set(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION, false);
        AbstractContract contract = contract(ContractObjectiveType.ESPIONAGE);
        when(contract.getExactScale()).thenReturn(12.0);

        // Scale 12 counts as 4: 3 x 4 Nav Points a month, over 6 months
        assertEquals(12, StratConCombatBonusProjection.getMonthlyCombatBonusCount(campaignOptions, contract));
        assertEquals(72.0, StratConCombatBonusProjection.project(campaign, contract).bonusCount(), DELTA);
    }

    @Test
    void anAcceptedContractKeepsHowItsScaleWasCounted() {
        AbstractContract contract = contract(ContractObjectiveType.PLANETARY_ASSAULT);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.setSupportPointsFactoredIntoScale(false);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);

        // The option is on now, but the contract's Scale 4 was counted without support points: it counts as 4 / 3
        assertEquals(1, StratConCombatBonusProjection.getMonthlyCombatBonusCount(campaignOptions, contract));
    }
}
