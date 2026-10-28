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
 * pay their average count, and an Objective Raid's target intelligence is capped by Scale.
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
        campaignOptions.set(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE, true);
        campaignOptions.set(CampaignOption.MINIMUM_ONE_TRACK_PER_ROLL, false);
        campaignOptions.set(CampaignOption.ROLL_TRACKS_WEEKLY, false);
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
        return contract;
    }

    @Test
    void essentialScenariosPayFromTheirSchedule() {
        StratConCombatBonusProjection projection = StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.GARRISON_DUTY));

        assertEquals(5.0, projection.bonusCount(), DELTA);
        assertFalse(projection.isFromNavPoint());
    }

    @Test
    void navPointsReplacingEssentialScenariosPayTheirAverageCount() {
        // Espionage's data caches replace its Essential scenarios: 4 rolls x 3 tracks
        StratConCombatBonusProjection projection = StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.ESPIONAGE));

        assertEquals(12.0, projection.bonusCount(), DELTA);
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
              contract(ContractObjectiveType.GARRISON_DUTY)).bonusCount(), DELTA);
    }

    @Test
    void withoutStratConNoCombatBonusIsPaid() {
        campaignOptions.set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.DISABLED);

        assertEquals(0.0, StratConCombatBonusProjection.project(campaign,
              contract(ContractObjectiveType.GARRISON_DUTY)).bonusCount(), DELTA);
    }
}
