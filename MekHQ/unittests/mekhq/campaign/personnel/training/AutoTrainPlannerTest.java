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
package mekhq.campaign.personnel.training;

import static mekhq.campaign.personnel.skills.SkillType.S_GUN_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_PILOT_MEK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.QuickTrain;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.training.AutoTrainPlanner.Plan;
import mekhq.campaign.personnel.training.AutoTrainPlanner.Skip;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests {@link AutoTrainPlanner}: which improvements auto-train picks, in what order, and who it leaves out. Prices
 * come from the game's own skill math, so the tests check the planner's choices rather than any particular cost.
 */
class AutoTrainPlannerTest {
    private static final int PLENTY_OF_XP = 100_000;
    private static final int START_LEVEL = 2;
    private static final int TARGET_LEVEL = 5;

    private TrainingFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new TrainingFixture();
    }

    /** @return settings that train only Gunnery/Mek, one level per skill, up to {@link #TARGET_LEVEL} */
    private static AutoTrainSettings.Builder gunnery() {
        return new AutoTrainSettings.Builder().setName("Gunnery").setTargetLevel(TARGET_LEVEL)
                     .setSpecificSkill(S_GUN_MEK);
    }

    private Person gunner(int xp, int level) {
        Person person = fixture.person(xp);
        TrainingFixture.giveSkill(person, S_GUN_MEK, level);
        return person;
    }

    private Plan plan(List<Person> people, AutoTrainSettings.Builder settings) {
        return AutoTrainPlanner.plan(fixture.campaign, fixture.plan(), people, settings.build());
    }

    private static List<String> skillsOf(Plan plan) {
        return plan.purchases().stream()
                     .map(purchase -> ((SkillImprovement) purchase).skillName())
                     .toList();
    }

    @Nested
    class Levels {
        @Test
        void oneLevelPerSkillUnlessContinuous() {
            Person person = gunner(PLENTY_OF_XP, START_LEVEL);

            Plan plan = plan(List.of(person), gunnery());

            assertEquals(List.of(S_GUN_MEK), skillsOf(plan));
            assertEquals(TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL), plan.xp());
            assertEquals(1, plan.peopleCount());
        }

        @Test
        void continuousTrainingStopsAtTheTarget() {
            Person person = gunner(PLENTY_OF_XP, START_LEVEL);

            Plan plan = plan(List.of(person), gunnery().setContinuous(true));

            int expectedXp = 0;
            for (int level = START_LEVEL; level < TARGET_LEVEL; level++) {
                expectedXp += TrainingFixture.improvementCost(S_GUN_MEK, level);
            }
            assertEquals(TARGET_LEVEL - START_LEVEL, plan.purchases().size());
            assertEquals(expectedXp, plan.xp());
        }

        @Test
        void skillsAtTheTargetAreLeftAlone() {
            Person person = gunner(PLENTY_OF_XP, TARGET_LEVEL);

            Plan plan = plan(List.of(person), gunnery().setContinuous(true));

            assertTrue(plan.staged().isEmpty());
        }

        @Test
        void skillsNotHeldAreNeverLearned() {
            Person person = gunner(PLENTY_OF_XP, START_LEVEL);

            Plan plan = plan(List.of(person), gunnery().setSpecificSkill(S_PILOT_MEK));

            assertTrue(plan.staged().isEmpty());
        }

        @Test
        void continuousTrainingStopsWhenTheXpRunsOut() {
            int firstCost = TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL);
            int secondCost = TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL + 1);
            Person person = gunner(firstCost + secondCost - 1, START_LEVEL);

            Plan plan = plan(List.of(person), gunnery().setContinuous(true));

            assertEquals(1, plan.purchases().size());
            assertEquals(firstCost, plan.xp());
        }

        @Test
        void reservedXpIsNotSpent() {
            int firstCost = TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL);
            int secondCost = TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL + 1);
            Person person = gunner(firstCost + secondCost, START_LEVEL);

            Plan plan = plan(List.of(person), gunnery().setContinuous(true).setReserveXp(1));

            assertEquals(1, plan.purchases().size());
        }
    }

    @Nested
    class Ordering {
        private Person person;

        @BeforeEach
        void giveAWeakButDearSkill() {
            person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, 1);
            TrainingFixture.giveSkill(person, S_PILOT_MEK, 3);
            // Gunnery is the weaker skill, but this person's talent makes it the dearer one to train
            when(person.getTalentBasedXpCostMultiplier(anyBoolean(), eq(SkillType.getType(S_GUN_MEK))))
                  .thenReturn(10.0);
            assertTrue(TrainingFixture.improvementCost(S_GUN_MEK, 1) * 10
                             > TrainingFixture.improvementCost(S_PILOT_MEK, 3),
                  "the weaker skill should cost more to train");
        }

        private Plan planBoth(AutoTrainSettings.Order order) {
            try (MockedStatic<QuickTrain> quickTrain = mockStatic(QuickTrain.class, CALLS_REAL_METHODS)) {
                quickTrain.when(() -> QuickTrain.getTargetSkills(any(), any(), any()))
                      .thenReturn(List.of(S_PILOT_MEK, S_GUN_MEK));
                return plan(List.of(person),
                      new AutoTrainSettings.Builder().setTargetLevel(TARGET_LEVEL).setOrder(order));
            }
        }

        @Test
        void weakestFirstTrainsTheLowestSkillFirst() {
            assertEquals(List.of(S_GUN_MEK, S_PILOT_MEK), skillsOf(planBoth(AutoTrainSettings.Order.WEAKEST_FIRST)));
        }

        @Test
        void cheapestFirstTrainsTheCheapestImprovementFirst() {
            assertEquals(List.of(S_PILOT_MEK, S_GUN_MEK),
                  skillsOf(planBoth(AutoTrainSettings.Order.CHEAPEST_FIRST)));
        }
    }

    @Nested
    class RoleSkills {
        @Test
        void quickTrainsSkillsAreTrainedAndCountedBySkill() {
            Person both = gunner(PLENTY_OF_XP, START_LEVEL);
            TrainingFixture.giveSkill(both, S_PILOT_MEK, START_LEVEL);
            Person gunnerOnly = gunner(PLENTY_OF_XP, START_LEVEL);

            Plan plan;
            try (MockedStatic<QuickTrain> quickTrain = mockStatic(QuickTrain.class, CALLS_REAL_METHODS)) {
                quickTrain.when(() -> QuickTrain.getTargetSkills(any(), any(), any()))
                      .thenReturn(List.of(S_PILOT_MEK, S_GUN_MEK));
                plan = plan(List.of(both, gunnerOnly), new AutoTrainSettings.Builder().setTargetLevel(TARGET_LEVEL));
            }

            assertEquals(3, plan.purchases().size());
            assertEquals(2, plan.peopleCount());
            assertEquals(List.of(S_GUN_MEK, S_PILOT_MEK), List.copyOf(plan.bySkill().keySet()),
                  "the skill trained most comes first");
            assertEquals(2, plan.bySkill().get(S_GUN_MEK));
            assertEquals(1, plan.bySkill().get(S_PILOT_MEK));
        }
    }

    @Nested
    class Skips {
        @Test
        void excludedPeopleAreLeftOutOnlyWhenExclusionsAreRespected() {
            Person person = gunner(PLENTY_OF_XP, START_LEVEL);
            when(person.isQuickTrainIgnore()).thenReturn(true);

            Plan respected = plan(List.of(person), gunnery().setRespectExclusions(true));
            Plan ignored = plan(List.of(person), gunnery());

            assertTrue(respected.staged().isEmpty());
            assertEquals(1, respected.skipped().get(Skip.EXCLUDED));
            assertEquals(1, ignored.purchases().size());
            assertTrue(ignored.skipped().isEmpty());
        }

        @Test
        void inactivePeopleAndStudentsAreLeftOut() {
            Person dead = gunner(PLENTY_OF_XP, START_LEVEL);
            when(dead.getStatus()).thenReturn(PersonnelStatus.KIA);
            Person student = gunner(PLENTY_OF_XP, START_LEVEL);
            when(student.getStatus()).thenReturn(PersonnelStatus.STUDENT);

            Plan plan = plan(List.of(dead, student), gunnery());

            assertTrue(plan.staged().isEmpty());
            assertEquals(2, plan.skipped().get(Skip.INACTIVE));
        }

        @Test
        void peopleInTrainingFormationsAreLeftOutWhenAsked() {
            Person person = gunner(PLENTY_OF_XP, START_LEVEL);

            Plan skipped;
            Plan trained;
            try (MockedStatic<QuickTrain> quickTrain = mockStatic(QuickTrain.class, CALLS_REAL_METHODS)) {
                quickTrain.when(() -> QuickTrain.isInTrainingFormation(person, fixture.campaign)).thenReturn(true);
                skipped = plan(List.of(person), gunnery().setSkipTrainingFormations(true));
                trained = plan(List.of(person), gunnery());
            }

            assertEquals(1, skipped.skipped().get(Skip.TRAINING_FORMATION));
            assertTrue(skipped.staged().isEmpty());
            assertEquals(1, trained.purchases().size());
        }
    }

    @Nested
    class Fees {
        private int pricedLevel;
        private Money fee;

        @BeforeEach
        void turnFeesOn() {
            fixture.options.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
            SkillType type = SkillType.getType(S_GUN_MEK);
            for (int level = 0; level < SkillType.NUM_LEVELS - 1; level++) {
                Money cost = SkillTrainingCosts.getSkillImprovementCost(fixture.options, type, true, level);
                if (!cost.isZero()) {
                    pricedLevel = level;
                    fee = cost;
                    return;
                }
            }
            fail("No level of " + S_GUN_MEK + " is charged a training fee");
        }

        private AutoTrainSettings.Builder oneLevelUp() {
            return gunnery().setTargetLevel(pricedLevel + 1);
        }

        @Test
        void feesAreCounted() {
            Plan plan = plan(List.of(gunner(PLENTY_OF_XP, pricedLevel)), oneLevelUp());

            assertEquals(1, plan.purchases().size());
            assertEquals(fee, plan.fees());
        }

        @Test
        void improvementsWithFeesCanBeLeftOut() {
            Plan plan = plan(List.of(gunner(PLENTY_OF_XP, pricedLevel)), oneLevelUp().setSkipFeeImprovements(true));

            assertTrue(plan.staged().isEmpty());
        }

        @Test
        void improvementsTheCampaignCantPayForAreLeftOut() {
            TrainingPlan staged = fixture.plan(fee.minus(Money.of(1)));

            Plan plan = AutoTrainPlanner.plan(fixture.campaign, staged,
                  List.of(gunner(PLENTY_OF_XP, pricedLevel)), oneLevelUp().build());

            assertTrue(plan.staged().isEmpty());
        }
    }

    @Nested
    class TheGivenPlan {
        @Test
        void isNotChanged() {
            TrainingPlan staged = fixture.plan();
            int revision = staged.revision();

            Plan plan = AutoTrainPlanner.plan(fixture.campaign, staged,
                  List.of(gunner(PLENTY_OF_XP, START_LEVEL)), gunnery().build());

            assertEquals(1, plan.purchases().size());
            assertTrue(staged.isEmpty());
            assertEquals(revision, staged.revision());
        }

        @Test
        void trainingAlreadyStagedIsBuiltOn() {
            Person person = gunner(PLENTY_OF_XP, TARGET_LEVEL - 1);
            TrainingPlan staged = fixture.plan();
            staged.stage("by hand", List.of(new SkillImprovement(person, S_GUN_MEK)));

            Plan plan = AutoTrainPlanner.plan(fixture.campaign, staged, List.of(person), gunnery().build());

            assertTrue(plan.staged().isEmpty(), "the staged improvement already reaches the target");
        }

        @Test
        void thePlanCanBeStagedAsOneStep() {
            TrainingPlan staged = fixture.plan();
            Plan plan = AutoTrainPlanner.plan(fixture.campaign, staged,
                  List.of(gunner(PLENTY_OF_XP, START_LEVEL), gunner(PLENTY_OF_XP, START_LEVEL)),
                  gunnery().setContinuous(true).build());

            assertTrue(staged.stagePriced("auto-train", plan.staged()));

            assertEquals(1, staged.steps().size());
            assertEquals(plan.xp(), staged.totalXp());
            assertEquals(plan.purchases().size(), staged.purchaseCount());
        }
    }
}
