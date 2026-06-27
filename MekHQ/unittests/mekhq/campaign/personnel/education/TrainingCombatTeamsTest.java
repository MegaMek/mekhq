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

import static mekhq.campaign.personnel.education.TrainingCombatTeams.XP_RATE_BASE_LINE;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.ALMOST;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.BARELY_MADE_IT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

class TrainingCombatTeamsTest {
    private Campaign campaign;
    private Person trainee;
    private Skill gunneryMek;

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        campaign = MHQTestUtilities.getTestCampaign();

        trainee = new Person(campaign);
        campaign.importPerson(trainee);

        trainee.addSkill(SkillType.S_GUN_MEK, 0, 0);
        gunneryMek = trainee.getSkill(SkillType.S_GUN_MEK);
    }

    @Test
    void test_ImproveSkill_ProgressChanged_SomeXpProgressRemains() {
        int startingXpProgress = 10;
        gunneryMek.changeXpProgress(startingXpProgress);

        int baseCostToImprove = 5;
        TrainingCombatTeams.improveSkill(campaign, trainee, gunneryMek, baseCostToImprove);

        int expectedXPProgress = startingXpProgress - baseCostToImprove;
        int newXPProgress = gunneryMek.getXpProgress();

        assertEquals(expectedXPProgress, newXPProgress);
    }

    @Test
    void test_ImproveSkill_ProgressChanged_NoXpProgressRemains() {
        int startingXpProgress = 5;
        gunneryMek.changeXpProgress(startingXpProgress);

        int baseCostToImprove = 5;
        TrainingCombatTeams.improveSkill(campaign, trainee, gunneryMek, baseCostToImprove);

        int expectedXPProgress = 0;
        int newXPProgress = gunneryMek.getXpProgress();

        assertEquals(expectedXPProgress, newXPProgress);
    }

    @Test
    void test_ImproveSkill_ProgressChanged_NegativeXpProgress() {
        int startingXpProgress = 0;
        gunneryMek.changeXpProgress(startingXpProgress);

        int baseCostToImprove = 5;
        TrainingCombatTeams.improveSkill(campaign, trainee, gunneryMek, baseCostToImprove);

        int expectedXPProgress = 0;
        int newXPProgress = gunneryMek.getXpProgress();

        assertEquals(expectedXPProgress, newXPProgress);
    }

    @Test
    void test_ImproveSkill_SkillImproved() {
        int baseCostToImprove = 0;
        TrainingCombatTeams.improveSkill(campaign, trainee, gunneryMek, baseCostToImprove);

        int expectedLevel = 1;
        int actualLevel = gunneryMek.getLevel();

        assertEquals(expectedLevel, actualLevel);
    }

    @Test
    void test_IsWasTrainingCompleted_NotEnoughXP() {
        int baseCostToImprove = 10;
        int finalXpProgress = 5;
        boolean wasTrainingCompleted = TrainingCombatTeams.isWasTrainingCompleted(baseCostToImprove, finalXpProgress);

        assertFalse(wasTrainingCompleted);
    }

    @Test
    void test_IsWasTrainingCompleted_EnoughXP() {
        int baseCostToImprove = 5;
        int finalXpProgress = 5;
        boolean wasTrainingCompleted = TrainingCombatTeams.isWasTrainingCompleted(baseCostToImprove, finalXpProgress);

        assertTrue(wasTrainingCompleted);
    }

    @Test
    void test_IsWasTrainingCompleted_TooMuchXP() {
        int baseCostToImprove = 5;
        int finalXpProgress = 10;
        boolean wasTrainingCompleted = TrainingCombatTeams.isWasTrainingCompleted(baseCostToImprove, finalXpProgress);

        assertTrue(wasTrainingCompleted);
    }

    @Test
    void test_GetFinalXPProgress_ZeroMarginOfSuccess() {
        int marginOfSuccess = 0;
        TrainingCombatTeams.getFinalXPProgress(marginOfSuccess, gunneryMek);

        int expectedProgress = XP_RATE_BASE_LINE;
        int actualXPProgress = gunneryMek.getXpProgress();

        assertEquals(expectedProgress, actualXPProgress);
    }

    @Test
    void test_GetFinalXPProgress_NonZeroMarginOfSuccess() {
        int marginOfSuccess = 2;
        TrainingCombatTeams.getFinalXPProgress(marginOfSuccess, gunneryMek);

        int expectedProgress = XP_RATE_BASE_LINE * marginOfSuccess;
        int actualXPProgress = gunneryMek.getXpProgress();

        assertEquals(expectedProgress, actualXPProgress);
    }

    @Test
    void test_SortSkillsLowestLevelToHighest_GunneryMekFirst() {
        trainee.addSkill(SkillType.S_PILOT_MEK, 10, 0);
        Skill pilotingMek = trainee.getSkill(SkillType.S_PILOT_MEK);

        List<Skill> skillList = new ArrayList<>();
        skillList.add(gunneryMek);
        skillList.add(pilotingMek);


        TrainingCombatTeams.sortSkillsLowestLevelToHighest(skillList);

        assertEquals(skillList.getFirst(), gunneryMek);
    }

    @Test
    void test_SortSkillsLowestLevelToHighest_NotGunneryMekFirst() {
        trainee.addSkill(SkillType.S_PILOT_MEK, 0, 0);
        Skill pilotingMek = trainee.getSkill(SkillType.S_PILOT_MEK);

        gunneryMek.setLevel(10);

        List<Skill> skillList = new ArrayList<>();
        skillList.add(gunneryMek);
        skillList.add(pilotingMek);

        TrainingCombatTeams.sortSkillsLowestLevelToHighest(skillList);

        assertEquals(skillList.getFirst(), pilotingMek);
    }

    @Test
    void test_IsTrainingImpossible_NoSkills_CheckSuccessful() {
        boolean isTrainingImpossible = TrainingCombatTeams.isTrainingImpossible(List.of(), BARELY_MADE_IT.getValue());
        assertTrue(isTrainingImpossible);
    }

    @Test
    void test_IsTrainingImpossible_YesSkills_CheckFailed() {
        boolean isTrainingImpossible = TrainingCombatTeams.isTrainingImpossible(List.of(gunneryMek),
              ALMOST.getValue());
        assertTrue(isTrainingImpossible);
    }

    @Test
    void test_IsTrainingImpossible_NoSkills_CheckFailed() {
        boolean isTrainingImpossible = TrainingCombatTeams.isTrainingImpossible(List.of(), ALMOST.getValue());
        assertTrue(isTrainingImpossible);
    }

    @Test
    void test_IsTrainingImpossible_YesSkills_CheckSuccessful() {
        boolean isTrainingImpossible = TrainingCombatTeams.isTrainingImpossible(List.of(gunneryMek),
              BARELY_MADE_IT.getValue());
        assertFalse(isTrainingImpossible);
    }
}
