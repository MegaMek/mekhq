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

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.personnel.ATOWTraits.TRAIT_MODIFICATION_COST;
import static mekhq.campaign.personnel.skills.SkillType.S_GUN_MEK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Vector;

import megamek.common.options.IOption;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.training.TrainingPurchase.AttributeIncrease;
import mekhq.campaign.personnel.training.TrainingPurchase.EdgeRefill;
import mekhq.campaign.personnel.training.TrainingPurchase.FlawBuyOff;
import mekhq.campaign.personnel.training.TrainingPurchase.NaturalAptitude;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import mekhq.campaign.personnel.training.TrainingPurchase.TraitChange;
import mekhq.campaign.personnel.training.TrainingService.CommitReport;
import mekhq.utilities.spaUtilities.SpaUtilities;
import mekhq.utilities.spaUtilities.enums.AbilityCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests {@link TrainingService}: that committing a plan applies each purchase the way the Spend XP menu did, pays the
 * training fees, and skips what no longer fits.
 */
class TrainingServiceTest {
    private static final int START_XP = 10_000;

    private TrainingFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new TrainingFixture();
    }

    private void verifyUpdated(Person person) {
        verify(fixture.campaign.getPlayerForce().getHumanResources()).personUpdated(fixture.campaign, person);
    }

    @Nested
    class Skills {
        @Test
        void improvesTheSkillAndSpendsItsXp() {
            Person person = fixture.livePerson(START_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, 3);
            TrainingPlan plan = fixture.plan();
            plan.stage("two levels", List.of(new SkillImprovement(person, S_GUN_MEK),
                  new SkillImprovement(person, S_GUN_MEK)));
            int stagedLevel = plan.projectionOf(person).skillLevel(S_GUN_MEK);

            CommitReport report = TrainingService.commit(fixture.campaign, plan);

            assertFalse(report.isRefused());
            assertEquals(2, report.applied());
            assertEquals(0, report.skipped());
            assertEquals(plan.totalXp(), report.xpSpent());
            assertEquals(stagedLevel, person.getSkill(S_GUN_MEK).getLevel());
            assertEquals(START_XP - plan.totalXp(), person.getXP());
            verifyUpdated(person);
            verify(fixture.campaign, atLeastOnce()).addReport(eq(PERSONNEL), anyString());
        }

        @Test
        void skipsWhatNoLongerFits() {
            Person person = fixture.livePerson(START_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, 3);
            TrainingPlan plan = fixture.plan();
            plan.stage("level", List.of(new SkillImprovement(person, S_GUN_MEK)));
            // The person spent their XP elsewhere before the commit
            when(person.getXP()).thenReturn(0);

            CommitReport report = TrainingService.commit(fixture.campaign, plan);

            assertEquals(0, report.applied());
            assertEquals(1, report.skipped());
            verify(person, never()).improveSkill(anyString());
        }

        @Test
        void naturalAptitudesAreGained() {
            Person person = fixture.livePerson(START_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, 3);
            when(person.getCostToGainNaturalAptitude(eq(S_GUN_MEK), anyBoolean())).thenReturn(200);
            TrainingPlan plan = fixture.plan();
            plan.stage("aptitude", List.of(new NaturalAptitude(person, S_GUN_MEK)));

            TrainingService.commit(fixture.campaign, plan);

            assertTrue(person.getSkill(S_GUN_MEK).getHasNaturalAptitude());
            assertEquals(START_XP - 200, person.getXP());
        }
    }

    @Nested
    class Fees {
        private Money fee;
        private int pricedLevel;

        @BeforeEach
        void turnFeesOn() {
            fixture.options.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
            SkillType type = SkillType.getType(S_GUN_MEK);
            for (int level = 0; level < SkillType.NUM_LEVELS - 1; level++) {
                Money cost = SkillTrainingCosts.getSkillImprovementCost(fixture.options, type, true, level);
                if (!cost.isZero()) {
                    fee = cost;
                    pricedLevel = level;
                    return;
                }
            }
            fail("No level of " + S_GUN_MEK + " is charged a training fee");
        }

        private Person pricedGunner() {
            Person person = fixture.livePerson(START_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, pricedLevel);
            return person;
        }

        @Test
        void aStepsFeesArePaidTogetherAsEducation() {
            TrainingPlan plan = fixture.plan();
            plan.stage("levels", List.of(new SkillImprovement(pricedGunner(), S_GUN_MEK),
                  new SkillImprovement(pricedGunner(), S_GUN_MEK)));

            CommitReport report = TrainingService.commit(fixture.campaign, plan);

            assertEquals(fee.plus(fee), report.feesPaid());
            verify(fixture.finances, times(1)).debit(eq(TransactionType.EDUCATION), eq(TrainingFixture.TODAY),
                  eq(fee.plus(fee)), anyString());
        }

        @Test
        void nothingIsAppliedWhenTheBalanceCantCoverTheFees() {
            Person person = pricedGunner();
            TrainingPlan plan = fixture.plan();
            plan.stage("level", List.of(new SkillImprovement(person, S_GUN_MEK)));
            when(fixture.finances.getBalance()).thenReturn(fee.minus(Money.of(1)));

            CommitReport report = TrainingService.commit(fixture.campaign, plan);

            assertTrue(report.isRefused());
            verify(person, never()).improveSkill(anyString());
            verify(fixture.finances, never()).debit(any(), any(LocalDate.class), any(), anyString());
        }
    }

    @Nested
    class AttributesTraitsAndEdge {
        @Test
        void attributesAreRaisedByOne() {
            Person person = fixture.livePerson(START_XP);
            when(person.getAttributeScore(SkillAttribute.STRENGTH)).thenReturn(4);
            when(person.getAttributeCap(SkillAttribute.STRENGTH)).thenReturn(8);
            TrainingPlan plan = fixture.plan();
            plan.stage("strength", List.of(new AttributeIncrease(person, SkillAttribute.STRENGTH)));

            TrainingService.commit(fixture.campaign, plan);

            verify(person).changeAttributeScore(SkillAttribute.STRENGTH, 1);
            assertEquals(START_XP - (int) fixture.options.get(CampaignOption.ATTRIBUTE_COST), person.getXP());
        }

        @Test
        void edgeThatCantBeGainedIsSkippedAndNotPaidFor() {
            fixture.options.set(CampaignOption.MAXIMUM_EDGE, 7);
            Person person = fixture.livePerson(START_XP);
            when(person.getEdge()).thenReturn(1);
            when(person.getAttributeCap(SkillAttribute.EDGE)).thenReturn(7);
            TrainingPlan plan = fixture.plan();
            plan.stage("edge", List.of(new AttributeIncrease(person, SkillAttribute.EDGE)));
            when(person.gainEdge(1, 7)).thenReturn(0);

            CommitReport report = TrainingService.commit(fixture.campaign, plan);

            assertEquals(1, report.skipped());
            assertEquals(START_XP, person.getXP());
        }

        @Test
        void traitsMoveOneStep() {
            Person person = fixture.livePerson(START_XP);
            TrainingPlan plan = fixture.plan();
            plan.stage("fame", List.of(new TraitChange(person, ATOWTraits.FAME, 1)));

            TrainingService.commit(fixture.campaign, plan);

            verify(person).setFame(1);
            assertEquals(START_XP - TRAIT_MODIFICATION_COST, person.getXP());
        }

        @Test
        void spentEdgeIsRefilled() {
            Person person = fixture.livePerson(START_XP);
            when(person.getUsedEdge()).thenReturn(2);
            TrainingPlan plan = fixture.plan();
            plan.stage("refill", List.of(new EdgeRefill(person)));

            TrainingService.commit(fixture.campaign, plan);

            verify(person).resetCurrentEdge();
            verifyUpdated(person);
        }
    }

    @Nested
    class Flaws {
        private static final String FLAW = "test_flaw";

        @Test
        void aFlawBoughtOffIsReturnedToItsDefault() {
            SpecialAbility flaw = mock(SpecialAbility.class);
            when(flaw.getName()).thenReturn(FLAW);
            when(flaw.getCost()).thenReturn(-100);
            when(flaw.getRemovedAbilities()).thenReturn(new Vector<>());
            Person person = fixture.livePerson(START_XP);
            when(person.getOptions().booleanOption(FLAW)).thenReturn(true);
            IOption option = mock(IOption.class);
            when(option.getDefault()).thenReturn(Boolean.FALSE);
            when(person.getOptions().getOption(FLAW)).thenReturn(option);

            try (MockedStatic<SpecialAbility> abilities = mockStatic(SpecialAbility.class);
                  MockedStatic<SpaUtilities> spaUtilities = mockStatic(SpaUtilities.class, CALLS_REAL_METHODS)) {
                abilities.when(() -> SpecialAbility.getAbility(FLAW)).thenReturn(flaw);
                abilities.when(() -> SpecialAbility.getDisplayName(FLAW)).thenReturn("Test Flaw");
                spaUtilities.when(() -> SpaUtilities.getSpaCategory(any())).thenReturn(AbilityCategory.CHARACTER_FLAW);
                TrainingPlan plan = fixture.plan();
                plan.stage("buy off", List.of(new FlawBuyOff(person, FLAW)));

                CommitReport report = TrainingService.commit(fixture.campaign, plan);

                assertEquals(1, report.applied());
                verify(option).setValue(Boolean.FALSE);
                assertEquals(START_XP - 100, person.getXP());
            }
        }
    }

    @Nested
    class DisplayNames {
        @Test
        void aChoiceFollowsTheAbilitysName() {
            try (MockedStatic<SpecialAbility> abilities = mockStatic(SpecialAbility.class)) {
                abilities.when(() -> SpecialAbility.getDisplayName("weapon_specialist"))
                      .thenReturn("Weapon Specialist");

                assertEquals("Weapon Specialist Medium Laser",
                      TrainingService.abilityDisplayName("weapon_specialist", "Medium Laser"));
                assertEquals("Weapon Specialist", TrainingService.abilityDisplayName("weapon_specialist", null));
                assertEquals("Weapon Specialist", TrainingService.abilityDisplayName("weapon_specialist", " "));
            }
        }
    }
}
