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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
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

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ACTIVE;
import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.MHQOptions;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class QuickTrainTest {

    @Test
    void detectsDirectTrainingFormation() {
        Person person = activePerson();
        Formation formation = formation(CombatRole.TRAINING, List.of());

        assertTrue(QuickTrain.isInTrainingFormation(person, campaign(person, formation)));
    }

    @Test
    void detectsParentTrainingFormation() {
        Person person = activePerson();
        Formation parent = formation(CombatRole.TRAINING, List.of());
        Formation formation = formation(CombatRole.FRONTLINE, List.of(parent));

        assertTrue(QuickTrain.isInTrainingFormation(person, campaign(person, formation)));
    }

    @Test
    void doesNotTreatNormalFormationAsTraining() {
        Person person = activePerson();
        Formation formation = formation(CombatRole.FRONTLINE, List.of());

        assertFalse(QuickTrain.isInTrainingFormation(person, campaign(person, formation)));
    }

    @Test
    void doesNotTreatUnassignedPersonAsTraining() {
        Person person = activePerson();
        when(person.getUnit()).thenReturn(null);

        assertFalse(QuickTrain.isInTrainingFormation(person, campaign(person, null)));
    }

    @Test
    void doesNotTreatFormationTechnicianAsTrainee() {
        Person person = activePerson();
        when(person.getUnit()).thenReturn(null);
        Campaign campaign = campaign(person, null);
        PlayerForce playerForce = campaign.getPlayerForce();
        Formation trainingFormation = formation(CombatRole.TRAINING, List.of());
        when(playerForce.getFormationFor(person)).thenReturn(trainingFormation);

        assertFalse(QuickTrain.isInTrainingFormation(person, campaign));
        verify(playerForce, never()).getFormationFor(person);
    }

    @Test
    void monthlyOptionsIncludeTrainingFormationPreference() {
        MHQOptions options = mock(MHQOptions.class);
        when(options.getQuickTrainIgnoreTrainingFormations()).thenReturn(true);

        QuickTrain.QuickTrainOptions quickTrainOptions =
              QuickTrain.QuickTrainOptions.getQuickTrainOptionsForNewDay(options);

        assertTrue(quickTrainOptions.ignoreTrainingFormations());
    }

    @Test
    void manualOptionsDoNotIgnoreTrainingFormations() {
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        lenient().when(campaignOptions.get(CampaignOption.USE_ARTILLERY)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.ADMINS_HAVE_NEGOTIATION)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.USE_ADVANCED_SCOUTING)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.USE_FUNCTIONAL_ESCAPE_ARTIST)).thenReturn(false);
        QuickTrain.QuickTrainOptions quickTrainOptions =
              QuickTrain.QuickTrainOptions.buildQuickTrainOptions(campaignOptions);

        assertFalse(quickTrainOptions.ignoreTrainingFormations());
    }

    @Test
    void enabledOptionSkipsSkillProcessingForTrainee() {
        Person person = processablePerson();
        Campaign campaign = processCampaign(person, formation(CombatRole.TRAINING, List.of()));

        QuickTrain.processQuickTraining(List.of(person), 5, campaign, options(true), true);

        verify(person, never()).getSkills();
    }

    @Test
    void disabledOptionRetainsSkillProcessingForTrainee() {
        Person person = processablePerson();
        Campaign campaign = processCampaign(person, formation(CombatRole.TRAINING, List.of()));

        QuickTrain.processQuickTraining(List.of(person), 5, campaign, options(false), true);

        verify(person).getSkills();
    }

    @Test
    void unaffordableImprovementsAreSkippedAndReported() {
        Person person = processablePerson();
        holdsTrainableLeadership(person);
        Campaign campaign = processCampaign(person, null);

        try (MockedStatic<SkillTrainingCosts> skillTrainingCosts = mockStatic(SkillTrainingCosts.class)) {
            skillTrainingCosts.when(() -> SkillTrainingCosts.payForSkillImprovement(campaign, person, S_LEADER))
                  .thenReturn(false);

            QuickTrain.processQuickTraining(List.of(person), 5, campaign, leadershipOnly(), true);
        }

        verify(person, never()).improveSkill(any());
        verify(campaign).addReport(eq(PERSONNEL), anyString());
    }

    @Test
    void affordableImprovementsArePaidForBeforeTraining() {
        Person person = processablePerson();
        holdsTrainableLeadership(person);
        Campaign campaign = processCampaign(person, null);

        try (MockedStatic<SkillTrainingCosts> skillTrainingCosts = mockStatic(SkillTrainingCosts.class)) {
            skillTrainingCosts.when(() -> SkillTrainingCosts.payForSkillImprovement(campaign, person, S_LEADER))
                  .thenReturn(true);

            // Singular, as the mocked skill never actually rises
            QuickTrain.processQuickTraining(List.of(person), 5, campaign, leadershipOnly(), false);

            skillTrainingCosts.verify(() -> SkillTrainingCosts.payForSkillImprovement(campaign, person, S_LEADER));
        }

        verify(person).improveSkill(S_LEADER);
    }

    private static Person activePerson() {
        Person person = mock(Person.class);
        when(person.getStatus()).thenReturn(ACTIVE);
        when(person.getUnit()).thenReturn(mock(Unit.class));
        return person;
    }

    private static Person processablePerson() {
        Person person = activePerson();
        LocalDate today = LocalDate.of(3151, 1, 1);
        when(person.getSkillModifierData(false, false, today, true)).thenReturn(mock(SkillModifierData.class));
        when(person.getSkills()).thenReturn(mock(Skills.class));
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.NONE);
        when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        return person;
    }

    /** Gives the person a Leadership skill that Quick Train can raise for free in XP. */
    private static void holdsTrainableLeadership(Person person) {
        Skill leadership = mock(Skill.class);
        when(leadership.getLevel()).thenReturn(1);
        when(leadership.isImprovementLegal()).thenReturn(true);
        when(person.getSkill(S_LEADER)).thenReturn(leadership);
        when(person.getSkills().hasSkill(S_LEADER)).thenReturn(true);
    }

    private static Formation formation(CombatRole role, List<Formation> parents) {
        Formation formation = mock(Formation.class);
        when(formation.getCombatRoleInMemory()).thenReturn(role);
        when(formation.getAllParents()).thenReturn(parents);
        return formation;
    }

    private static Campaign campaign(Person person, Formation formation) {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        Unit unit = person.getUnit();
        if (unit != null) {
            when(playerForce.getFormationFor(unit)).thenReturn(formation);
        }
        return campaign;
    }

    private static Campaign processCampaign(Person person, Formation formation) {
        Campaign campaign = campaign(person, formation);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        lenient().when(campaignOptions.get(CampaignOption.USE_ARTILLERY)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.ADMINS_HAVE_NEGOTIATION)).thenReturn(false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        lenient().when(campaignOptions.get(CampaignOption.USE_AGE_EFFECTS)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.PERSONNEL_LOG_SKILL_GAIN)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.XP_COST_MULTIPLIER)).thenReturn(0.0);
        lenient().when(campaignOptions.get(CampaignOption.USE_REASONING_XP_MULTIPLIER)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.USE_FUNCTIONAL_APPRAISAL)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.USE_SMALL_ARMS_ONLY)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.USE_FUNCTIONAL_ESCAPE_ARTIST)).thenReturn(false);
        lenient().when(campaignOptions.get(CampaignOption.DOCTORS_USE_ADMINISTRATION)).thenReturn(false);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
        lenient().when(campaign.getPlayerForce().getHumanResources()).thenReturn(mock(ForceHumanResources.class));
        return campaign;
    }

    private static QuickTrain.QuickTrainOptions leadershipOnly() {
        return new QuickTrain.QuickTrainOptions(false, false, false, true, false, false, false);
    }

    private static QuickTrain.QuickTrainOptions options(boolean ignoreTrainingFormations) {
        return new QuickTrain.QuickTrainOptions(false, false, false, false, false, false,
              ignoreTrainingFormations);
    }
}
