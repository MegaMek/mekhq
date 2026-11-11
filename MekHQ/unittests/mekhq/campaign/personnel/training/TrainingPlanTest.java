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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.training.TrainingPlan.Climb;
import mekhq.campaign.personnel.training.TrainingPlan.StageResult;
import mekhq.campaign.personnel.training.TrainingPlan.StagedPurchase;
import mekhq.campaign.personnel.training.TrainingPlan.Step;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase.NaturalAptitude;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TrainingPlan}: purchases priced in the order they are staged, all-or-nothing training fees, and undoing,
 * removing and previewing steps.
 */
class TrainingPlanTest {
    private static final int PLENTY_OF_XP = 100_000;
    private static final int START_LEVEL = 3;

    private TrainingFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new TrainingFixture();
    }

    private Person gunner(int xp) {
        Person person = fixture.person(xp);
        TrainingFixture.giveSkill(person, S_GUN_MEK, START_LEVEL);
        return person;
    }

    private static SkillImprovement improve(Person person) {
        return new SkillImprovement(person, S_GUN_MEK);
    }

    @Nested
    class Staging {
        @Test
        void eachPurchaseIsPricedAfterTheOnesBeforeIt() {
            Person person = gunner(PLENTY_OF_XP);
            TrainingPlan plan = fixture.plan();

            StageResult result = plan.stage("two levels", List.of(improve(person), improve(person)));

            Step step = result.step();
            assertNotNull(step);
            assertEquals(2, step.purchases().size());
            Quote first = step.purchases().get(0).quote();
            Quote second = step.purchases().get(1).quote();
            assertEquals(TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL), first.xp());
            // The second level is priced from where the first left the skill
            assertEquals(TrainingFixture.improvementCost(S_GUN_MEK, first.projectedSkill().getLevel()), second.xp());
            assertEquals(second.projectedSkill().getLevel(), plan.projectionOf(person).skillLevel(S_GUN_MEK));
            assertEquals(first.xp() + second.xp(), plan.totalXp());
            assertEquals(PLENTY_OF_XP - plan.totalXp(), plan.availableXpOf(person));
            assertEquals(2, plan.purchaseCountOf(person));
        }

        @Test
        void xpIsNeverCountedTwice() {
            Person person = gunner(TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL));
            TrainingPlan plan = fixture.plan();

            StageResult result = plan.stage("two levels", List.of(improve(person), improve(person)));

            assertEquals(1, result.stagedCount());
            assertEquals(1, result.refused().get(TrainingRefusal.NOT_ENOUGH_XP));
            assertEquals(0, plan.availableXpOf(person));
        }

        @Test
        void aBatchWithNothingAllowedChangesNothing() {
            Person person = gunner(0);
            TrainingPlan plan = fixture.plan();
            int revision = plan.revision();

            StageResult result = plan.stage("nothing", List.of(improve(person)));

            assertNull(result.step());
            assertEquals(1, result.refusedCount());
            assertTrue(plan.isEmpty());
            assertNull(plan.peek(person));
            assertEquals(revision, plan.revision());
        }

        @Test
        void stagingChangesTheRevision() {
            TrainingPlan plan = fixture.plan();
            int revision = plan.revision();

            plan.stage("level", List.of(improve(gunner(PLENTY_OF_XP))));

            assertNotEquals(revision, plan.revision());
        }

        @Test
        void countsEveryoneWithSomethingStaged() {
            TrainingPlan plan = fixture.plan();
            plan.stage("levels", List.of(improve(gunner(PLENTY_OF_XP)), improve(gunner(PLENTY_OF_XP))));

            assertEquals(2, plan.changedPeopleCount());
            assertEquals(2, plan.steps().get(0).peopleCount());
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

        private Person pricedGunner() {
            Person person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, pricedLevel);
            return person;
        }

        @Test
        void aBatchTheCampaignCantPayForIsRefusedWhole() {
            Person person = pricedGunner();
            TrainingPlan plan = fixture.plan(fee.minus(Money.of(1)));

            StageResult result = plan.stage("too dear", List.of(improve(person)));

            assertNull(result.step());
            assertEquals(1, result.refused().get(TrainingRefusal.NOT_ENOUGH_FUNDS));
            assertEquals(Money.of(1), result.shortfall());
            assertTrue(plan.isEmpty());
            assertNull(plan.peek(person));
        }

        @Test
        void feesCountAcrossSteps() {
            TrainingPlan plan = fixture.plan(fee);

            assertEquals(1, plan.stage("first", List.of(improve(pricedGunner()))).stagedCount());
            StageResult second = plan.stage("second", List.of(improve(pricedGunner())));

            assertEquals(0, second.stagedCount());
            assertEquals(fee, second.shortfall());
            assertEquals(1, plan.steps().size());
        }

        @Test
        void feesAreTotalledByCategory() {
            TrainingPlan plan = fixture.plan();
            plan.stage("level", List.of(improve(pricedGunner())));

            assertEquals(fee, plan.totalFees());
            assertEquals(fee, plan.feesByCategory().get(TrainingFeeCategory.PROFESSION));
            assertEquals(TrainingFixture.PLENTY.minus(fee), plan.fundsLeft());
        }

        @Test
        void preStagedPurchasesAreRefusedWhenTheyCantBePaidFor() {
            Person person = pricedGunner();
            TrainingPlan plan = fixture.plan(fee.minus(Money.of(1)));
            Quote quote = plan.quote(improve(person));

            assertFalse(plan.stagePriced("too dear", List.of(new StagedPurchase(improve(person), quote))));
            assertTrue(plan.isEmpty());
        }
    }

    @Nested
    class UndoingAndRemoving {
        @Test
        void undoRemovesTheLastStep() {
            Person person = gunner(PLENTY_OF_XP);
            TrainingPlan plan = fixture.plan();
            plan.stage("first", List.of(improve(person)));
            int afterFirst = plan.projectionOf(person).skillLevel(S_GUN_MEK);
            int xpAfterFirst = plan.totalXp();
            plan.stage("second", List.of(improve(person)));

            assertTrue(plan.undo());

            assertEquals(1, plan.steps().size());
            assertEquals(afterFirst, plan.projectionOf(person).skillLevel(S_GUN_MEK));
            assertEquals(xpAfterFirst, plan.totalXp());
        }

        @Test
        void undoOnAnEmptyPlanDoesNothing() {
            assertFalse(fixture.plan().undo());
        }

        @Test
        void removingAStepPricesTheLaterOnesAgain() {
            Person person = gunner(PLENTY_OF_XP);
            TrainingPlan plan = fixture.plan();
            plan.stage("first", List.of(improve(person)));
            plan.stage("second", List.of(improve(person)));

            assertEquals(0, plan.removeStep(0));

            // What was the second level is now the first, priced from the starting level
            assertEquals(1, plan.steps().size());
            assertEquals(TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL),
                  plan.steps().get(0).purchases().get(0).quote().xp());
        }

        @Test
        void removingAStepDropsWhatDependedOnIt() {
            Person person = fixture.person(PLENTY_OF_XP);
            when(person.getCostToGainNaturalAptitude(eq(S_GUN_MEK), anyBoolean())).thenReturn(100);
            TrainingPlan plan = fixture.plan();
            plan.stage("learn", List.of(improve(person)));
            plan.stage("aptitude", List.of(new NaturalAptitude(person, S_GUN_MEK)));

            // Without the skill, the aptitude can't be bought, and its step is left empty
            assertEquals(1, plan.removeStep(0));

            assertTrue(plan.isEmpty());
            assertEquals(0, plan.totalXp());
        }

        @Test
        void clearRemovesEverything() {
            Person person = gunner(PLENTY_OF_XP);
            TrainingPlan plan = fixture.plan();
            plan.stage("level", List.of(improve(person)));

            plan.clear();

            assertTrue(plan.isEmpty());
            assertEquals(0, plan.totalXp());
            assertEquals(Money.zero(), plan.totalFees());
            assertEquals(0, plan.changedPeopleCount());
            assertEquals(PLENTY_OF_XP, plan.availableXpOf(person));
        }
    }

    @Nested
    class CopiesAndPreviews {
        @Test
        void aCopyCanChangeWithoutChangingTheOriginal() {
            Person person = gunner(PLENTY_OF_XP);
            TrainingPlan plan = fixture.plan();
            plan.stage("level", List.of(improve(person)));
            int level = plan.projectionOf(person).skillLevel(S_GUN_MEK);

            TrainingPlan copy = plan.copy();
            copy.stage("another", List.of(improve(person)));

            assertEquals(1, plan.steps().size());
            assertEquals(level, plan.projectionOf(person).skillLevel(S_GUN_MEK));
            assertEquals(2, copy.steps().size());
        }

        @Test
        void aClimbReachesTheTargetWithoutStagingAnything() {
            Person person = gunner(PLENTY_OF_XP);
            TrainingPlan plan = fixture.plan();
            int target = START_LEVEL + 3;

            Climb climb = plan.previewClimb(List.of(person), TrainingPlanTest::improve,
                  projection -> projection.skillLevel(S_GUN_MEK), target);

            assertTrue(plan.isEmpty());
            assertEquals(1, climb.people());
            assertEquals(0, climb.stopShort());
            assertEquals(climb.purchases().size(), climb.steps());
            // Staging what it found reaches the target
            plan.stage("climb", climb.purchases());
            assertTrue(plan.projectionOf(person).skillLevel(S_GUN_MEK) >= target);
            assertEquals(climb.xp(), plan.totalXp());
        }

        @Test
        void aClimbStopsShortWhenXpRunsOut() {
            Person person = gunner(TrainingFixture.improvementCost(S_GUN_MEK, START_LEVEL));

            Climb climb = fixture.plan().previewClimb(List.of(person), TrainingPlanTest::improve,
                  projection -> projection.skillLevel(S_GUN_MEK), START_LEVEL + 3);

            assertEquals(1, climb.people());
            assertEquals(1, climb.steps());
            assertEquals(1, climb.stopShort());
        }
    }
}
