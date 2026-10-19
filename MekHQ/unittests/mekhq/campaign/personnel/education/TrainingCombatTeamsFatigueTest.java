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
package mekhq.campaign.personnel.education;

import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_GLASS_JAW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import megamek.common.options.PilotOptions;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Coverage for the Fatigue gained by members of a Training Combat Team during
 * {@code TrainingCombatTeams#performTraining}.
 */
class TrainingCombatTeamsFatigueTest {
    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Formation formation;
    private Person commander;
    private Person trainee;

    @BeforeEach
    void beforeEach() {
        campaign = mockCampaign();
        Faction faction = mock(Faction.class);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(faction.getShortName()).thenReturn("MERC");

        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.USE_FATIGUE, true);
        campaignOptions.set(CampaignOption.FATIGUE_RATE, 1);
        campaignOptions.set(CampaignOption.CHASSIS_FAMILIARITY_MODE, Familiarity.DISABLED);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);

        commander = mock(Person.class);
        Unit commanderUnit = mock(Unit.class);
        when(commanderUnit.getActiveCrew()).thenReturn(List.of(commander));
        when(commander.getUnit()).thenReturn(commanderUnit);

        trainee = new Person(campaign);
        Unit traineeUnit = mock(Unit.class);
        when(traineeUnit.getActiveCrew()).thenReturn(List.of(trainee));

        UUID traineeUnitId = UUID.randomUUID();
        when(campaign.getUnit(traineeUnitId)).thenReturn(traineeUnit);

        formation = mock(Formation.class);
        when(formation.getUnits()).thenReturn(new Vector<>(List.of(traineeUnitId)));
    }

    private void performTraining() throws Exception {
        Method method = TrainingCombatTeams.class.getDeclaredMethod("performTraining", Campaign.class,
              Formation.class, Person.class, Map.class, ActionCheckResult.class);
        method.setAccessible(true);

        try (MockedStatic<Fatigue> fatigue = mockStatic(Fatigue.class)) {
            // No educator skills, so the trainee learns nothing and only the Fatigue is applied
            method.invoke(null, campaign, formation, commander, Map.of(), null);

            if (campaignOptions.get(CampaignOption.USE_FATIGUE)) {
                fatigue.verify(() -> Fatigue.processFatigueActions(campaign, trainee));
            } else {
                fatigue.verifyNoInteractions();
            }
        }
    }

    @Test
    void traineeGainsFatigueRate() throws Exception {
        campaignOptions.set(CampaignOption.FATIGUE_RATE, 2);

        performTraining();

        assertEquals(2, trainee.getFatigueDirect());
    }

    /**
     * Regression: Glass Jaw was applied once by training and again by {@link Person#changeFatigue(int)}, quadrupling
     * the Fatigue gained.
     */
    @Test
    void glassJawIsOnlyAppliedOnce() throws Exception {
        trainee.getOptions().acquireAbility(PilotOptions.LVL3_ADVANTAGES, FLAW_GLASS_JAW, true);

        performTraining();

        assertEquals(2, trainee.getFatigueDirect());
    }

    @Test
    void toughnessStillGainsAtLeastOne() throws Exception {
        trainee.getOptions().acquireAbility(PilotOptions.LVL3_ADVANTAGES, ATOW_TOUGHNESS, true);

        performTraining();

        assertEquals(1, trainee.getFatigueDirect());
    }

    @Test
    void noFatigueWhenFatigueDisabled() throws Exception {
        campaignOptions.set(CampaignOption.USE_FATIGUE, false);

        performTraining();

        assertEquals(0, trainee.getFatigueDirect());
    }
}
