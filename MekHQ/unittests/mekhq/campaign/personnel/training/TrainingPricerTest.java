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

import static mekhq.campaign.personnel.ATOWTraits.TRAIT_MODIFICATION_COST;
import static mekhq.campaign.personnel.skills.SkillType.S_ARTILLERY;
import static mekhq.campaign.personnel.skills.SkillType.S_GUN_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.ATOWTraits;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillTrainingCategory;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.training.TrainingPricer.Quote;
import mekhq.campaign.personnel.training.TrainingPurchase.AttributeIncrease;
import mekhq.campaign.personnel.training.TrainingPurchase.EdgeRefill;
import mekhq.campaign.personnel.training.TrainingPurchase.NaturalAptitude;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import mekhq.campaign.personnel.training.TrainingPurchase.TraitChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link TrainingPricer}: that each purchase is priced the way the Spend XP menu priced it, and refused for the
 * right reason. Abilities are tested in {@link TrainingAbilityCostsTest}.
 */
class TrainingPricerTest {
    private static final int PLENTY_OF_XP = 100_000;

    private TrainingFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new TrainingFixture();
    }

    @Nested
    class Skills {
        @Test
        void learningAnUnknownSkillCostsItsFirstLevel() {
            Person person = fixture.person(PLENTY_OF_XP);

            Quote quote = fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK));

            assertTrue(quote.isAllowed());
            assertEquals(SkillType.getType(S_GUN_MEK).getCost(0), quote.xp());
            assertNotNull(quote.projectedSkill());
            assertEquals(0, quote.projectedSkill().getLevel());
        }

        @ParameterizedTest
        @CsvSource({ "0", "2", "5" })
        void improvingCostsWhatTheGameChargesForTheNextLevel(int level) {
            Person person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, level);

            Quote quote = fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK));

            assertTrue(quote.isAllowed());
            assertEquals(TrainingFixture.improvementCost(S_GUN_MEK, level), quote.xp());
            assertTrue(quote.projectedSkill().getLevel() > level);
        }

        @Test
        void xpAlreadyPutTowardsTheSkillComesOffThePrice() {
            Person person = fixture.person(PLENTY_OF_XP);
            Skill skill = TrainingFixture.giveSkill(person, S_GUN_MEK, 3);
            int cost = TrainingFixture.improvementCost(S_GUN_MEK, 3);
            skill.changeXpProgress(cost - 1);

            Quote quote = fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK));

            assertEquals(1, quote.xp());
            // The progress is used up by the improvement
            assertEquals(0, quote.projectedSkill().getXpProgress());
        }

        @Test
        void theCampaignXpMultiplierScalesThePrice() {
            fixture.options.set(CampaignOption.XP_COST_MULTIPLIER, 2.0);
            Person person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, 3);

            Quote quote = fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK));

            assertEquals(2 * TrainingFixture.improvementCost(S_GUN_MEK, 3), quote.xp());
        }

        @Test
        void thePersonsTalentScalesThePrice() {
            Person person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, 3);
            when(person.getTalentBasedXpCostMultiplier(anyBoolean(), eq(SkillType.getType(S_GUN_MEK))))
                  .thenReturn(1.5);

            Quote quote = fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK));

            assertEquals((int) Math.round(TrainingFixture.improvementCost(S_GUN_MEK, 3) * 1.5), quote.xp());
        }

        @Test
        void refusedWithoutEnoughXpButStillPriced() {
            Person person = fixture.person(0);
            TrainingFixture.giveSkill(person, S_GUN_MEK, 3);

            Quote quote = fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK));

            assertEquals(TrainingRefusal.NOT_ENOUGH_XP, quote.refusal());
            assertEquals(TrainingFixture.improvementCost(S_GUN_MEK, 3), quote.xp());
        }

        @Test
        void refusedAtTheHighestLevel() {
            Person person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, SkillType.NUM_LEVELS - 1);

            assertEquals(TrainingRefusal.AT_LIMIT,
                  fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK)).refusal());
        }

        @Test
        void artilleryIsRefusedWhenTheCampaignDoesntUseIt() {
            fixture.options.set(CampaignOption.USE_ARTILLERY, false);
            Person person = fixture.person(PLENTY_OF_XP);

            assertEquals(TrainingRefusal.DISABLED,
                  fixture.plan().quote(new SkillImprovement(person, S_ARTILLERY)).refusal());
        }

        @Test
        void unknownSkillsAreRefused() {
            Person person = fixture.person(PLENTY_OF_XP);

            assertEquals(TrainingRefusal.DISABLED,
                  fixture.plan().quote(new SkillImprovement(person, "No Such Skill")).refusal());
        }

        @Test
        void inactivePeopleCantTrain() {
            Person person = fixture.person(PLENTY_OF_XP);
            when(person.getStatus()).thenReturn(PersonnelStatus.RETIRED);

            assertEquals(TrainingRefusal.NOT_ACTIVE,
                  fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK)).refusal());
        }
    }

    @Nested
    class SkillFees {
        @BeforeEach
        void turnFeesOn() {
            fixture.options.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        }

        /** @return the first level whose improvement is charged a fee, priced as a role skill or not */
        private int firstPricedLevel(String skillName, boolean isRoleSkill) {
            SkillType type = SkillType.getType(skillName);
            for (int level = 0; level < SkillType.NUM_LEVELS - 1; level++) {
                if (!SkillTrainingCosts.getSkillImprovementCost(fixture.options, type, isRoleSkill, level).isZero()) {
                    return level;
                }
            }
            return fail("No level of " + skillName + " is charged a training fee");
        }

        @Test
        void roleSkillsAreChargedTheProfessionFee() {
            int level = firstPricedLevel(S_GUN_MEK, true);
            Person person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, level);
            SkillType type = SkillType.getType(S_GUN_MEK);

            Quote quote = fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK));

            assertEquals(SkillTrainingCosts.getSkillImprovementCost(fixture.options, type, true, level), quote.fee());
            assertEquals(TrainingFeeCategory.PROFESSION, quote.feeCategory());
            assertEquals(type.getExperienceLevel(level + 1), quote.reachedLevel());
        }

        @Test
        void otherSkillsAreChargedByTheirOwnCategory() {
            int level = firstPricedLevel(S_LEADER, false);
            Person person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_LEADER, level);
            SkillType type = SkillType.getType(S_LEADER);

            Quote quote = fixture.plan().quote(new SkillImprovement(person, S_LEADER));

            assertEquals(SkillTrainingCosts.getSkillImprovementCost(fixture.options, type, false, level), quote.fee());
            assertEquals(TrainingFeeCategory.of(SkillTrainingCategory.getCategory(false, type)), quote.feeCategory());
        }

        @Test
        void learningASkillIsNeverCharged() {
            Person person = fixture.person(PLENTY_OF_XP);

            // Learning a skill only reaches its lowest experience level, which is never priced
            Quote quote = fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK));

            assertEquals(Money.zero(), quote.fee());
            assertNull(quote.feeCategory());
            assertEquals(-1, quote.reachedLevel());
        }

        @Test
        void noFeeWhenTheOptionIsOff() {
            int level = firstPricedLevel(S_GUN_MEK, true);
            fixture.options.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, false);
            Person person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, level);

            assertEquals(Money.zero(), fixture.plan().quote(new SkillImprovement(person, S_GUN_MEK)).fee());
        }
    }

    @Nested
    class Aptitudes {
        private static final int APTITUDE_COST = 300;

        private Person personWithSkill() {
            Person person = fixture.person(PLENTY_OF_XP);
            TrainingFixture.giveSkill(person, S_GUN_MEK, 3);
            when(person.getCostToGainNaturalAptitude(eq(S_GUN_MEK), anyBoolean())).thenReturn(APTITUDE_COST);
            return person;
        }

        @Test
        void costsWhatThePersonWouldPay() {
            Quote quote = fixture.plan().quote(new NaturalAptitude(personWithSkill(), S_GUN_MEK));

            assertTrue(quote.isAllowed());
            assertEquals(APTITUDE_COST, quote.xp());
            assertEquals(Money.zero(), quote.fee());
        }

        @Test
        void xpAlreadyPutTowardsItComesOffAfterTheMultiplier() {
            fixture.options.set(CampaignOption.XP_COST_MULTIPLIER, 2.0);
            Person person = personWithSkill();
            person.getSkill(S_GUN_MEK).changeNaturalAptitudeXpProgress(50);

            assertEquals((2 * APTITUDE_COST) - 50, fixture.plan().quote(new NaturalAptitude(person, S_GUN_MEK)).xp());
        }

        @Test
        void needsTheSkill() {
            Person person = fixture.person(PLENTY_OF_XP);

            assertEquals(TrainingRefusal.NOT_HELD,
                  fixture.plan().quote(new NaturalAptitude(person, S_GUN_MEK)).refusal());
        }

        @Test
        void aStagedSkillCountsAsKnown() {
            Person person = fixture.person(PLENTY_OF_XP);
            when(person.getCostToGainNaturalAptitude(eq(S_GUN_MEK), anyBoolean())).thenReturn(APTITUDE_COST);
            TrainingPlan plan = fixture.plan();
            plan.stage("learn", List.of(new SkillImprovement(person, S_GUN_MEK)));

            assertTrue(plan.quote(new NaturalAptitude(person, S_GUN_MEK)).isAllowed());
        }

        @Test
        void refusedWhenAlreadyHeld() {
            Person person = personWithSkill();
            person.getSkill(S_GUN_MEK).setHasNaturalAptitude(true);

            assertEquals(TrainingRefusal.ALREADY_HELD,
                  fixture.plan().quote(new NaturalAptitude(person, S_GUN_MEK)).refusal());
        }

        @Test
        void refusedOnceStaged() {
            Person person = personWithSkill();
            TrainingPlan plan = fixture.plan();
            plan.stage("aptitude", List.of(new NaturalAptitude(person, S_GUN_MEK)));

            assertEquals(TrainingRefusal.ALREADY_HELD, plan.quote(new NaturalAptitude(person, S_GUN_MEK)).refusal());
        }

        @Test
        void refusedWhenTheSkillOffersNone() {
            Person person = personWithSkill();
            when(person.getCostToGainNaturalAptitude(eq(S_GUN_MEK), anyBoolean()))
                  .thenReturn(SkillType.DISABLED_SKILL_LEVEL);

            assertEquals(TrainingRefusal.DISABLED,
                  fixture.plan().quote(new NaturalAptitude(person, S_GUN_MEK)).refusal());
        }
    }

    @Nested
    class Attributes {
        private Person person;

        @BeforeEach
        void setUpPerson() {
            person = fixture.person(PLENTY_OF_XP);
            when(person.getAttributeScore(SkillAttribute.STRENGTH)).thenReturn(5);
            when(person.getAttributeCap(SkillAttribute.STRENGTH)).thenReturn(6);
            when(person.getEdge()).thenReturn(2);
            when(person.getAttributeCap(SkillAttribute.EDGE)).thenReturn(7);
        }

        @Test
        void costsTheAttributeCostScaledByTheMultipliers() {
            fixture.options.set(CampaignOption.XP_COST_MULTIPLIER, 2.0);
            when(person.getReasoningXpCostMultiplier(anyBoolean())).thenReturn(1.5);
            int attributeCost = fixture.options.get(CampaignOption.ATTRIBUTE_COST);

            Quote quote = fixture.plan().quote(new AttributeIncrease(person, SkillAttribute.STRENGTH));

            assertEquals((int) Math.round(Math.round(attributeCost * 2.0) * 1.5), quote.xp());
        }

        @Test
        void refusedPastTheCapIncludingStagedRaises() {
            TrainingPlan plan = fixture.plan();
            assertEquals(1, plan.stage("raise", List.of(new AttributeIncrease(person, SkillAttribute.STRENGTH)))
                                  .stagedCount());

            assertEquals(TrainingRefusal.AT_LIMIT,
                  plan.quote(new AttributeIncrease(person, SkillAttribute.STRENGTH)).refusal());
            assertEquals(6, plan.projectionOf(person).attributeScore(SkillAttribute.STRENGTH));
        }

        @Test
        void edgeIsCappedByTheCampaignsMaximum() {
            fixture.options.set(CampaignOption.MAXIMUM_EDGE, 2);

            assertEquals(TrainingRefusal.AT_LIMIT,
                  fixture.plan().quote(new AttributeIncrease(person, SkillAttribute.EDGE)).refusal());
        }

        @Test
        void edgeIsRefusedWhenTheCampaignDoesntUseIt() {
            fixture.options.set(CampaignOption.USE_EDGE, false);

            assertEquals(TrainingRefusal.DISABLED,
                  fixture.plan().quote(new AttributeIncrease(person, SkillAttribute.EDGE)).refusal());
        }

        @Test
        void noAttributeIsRefused() {
            assertEquals(TrainingRefusal.DISABLED,
                  fixture.plan().quote(new AttributeIncrease(person, SkillAttribute.NO_ATTRIBUTE)).refusal());
        }
    }

    @Nested
    class Traits {
        @ParameterizedTest
        @CsvSource({
              "CONNECTIONS, 1, true", "FAME, 1, true", "FAME, -1, false", "WEALTH, 1, true", "WEALTH, -1, false",
              "EXTRA_INCOME, 1, true", "EXTRA_INCOME, -1, false", "UNLUCKY, -1, true", "UNLUCKY, 1, false",
              "BLOODMARK, -1, true", "BLOODMARK, 1, false" })
        void costlyDirections(ATOWTraits trait, int delta, boolean isCostly) {
            assertEquals(isCostly, TrainingPricer.isCostlyDirection(trait, delta));
        }

        @Test
        void raisingFameCostsTheTraitCost() {
            Person person = fixture.person(PLENTY_OF_XP);

            Quote quote = fixture.plan().quote(new TraitChange(person, ATOWTraits.FAME, 1));

            assertEquals(TRAIT_MODIFICATION_COST, quote.xp());
        }

        @Test
        void loweringFameGivesXpBackEvenWithNone() {
            Person person = fixture.person(0);

            Quote quote = fixture.plan().quote(new TraitChange(person, ATOWTraits.FAME, -1));

            assertTrue(quote.isAllowed());
            assertEquals(-TRAIT_MODIFICATION_COST, quote.xp());
        }

        @Test
        void refusedPastTheTraitsRangeIncludingStagedSteps() {
            Person person = fixture.person(PLENTY_OF_XP);
            when(person.getFame()).thenReturn(ATOWTraits.FAME.getMaximum() - 1);
            TrainingPlan plan = fixture.plan();
            plan.stage("fame", List.of(new TraitChange(person, ATOWTraits.FAME, 1)));

            assertEquals(TrainingRefusal.AT_LIMIT, plan.quote(new TraitChange(person, ATOWTraits.FAME, 1)).refusal());
        }

        @Test
        void connectionsCantBeSoldBack() {
            Person person = fixture.person(PLENTY_OF_XP);
            when(person.getConnections()).thenReturn(3);

            assertEquals(TrainingRefusal.DISABLED,
                  fixture.plan().quote(new TraitChange(person, ATOWTraits.CONNECTIONS, -1)).refusal());
        }

        @Test
        void traitsSetAtCharacterCreationAreRefused() {
            Person person = fixture.person(PLENTY_OF_XP);

            assertEquals(TrainingRefusal.DISABLED,
                  fixture.plan().quote(new TraitChange(person, ATOWTraits.ENEMY, 1)).refusal());
        }

        @Test
        void onlySingleStepsAreAllowed() {
            Person person = fixture.person(PLENTY_OF_XP);

            assertEquals(TrainingRefusal.DISABLED,
                  fixture.plan().quote(new TraitChange(person, ATOWTraits.FAME, 2)).refusal());
        }
    }

    @Nested
    class EdgeRefills {
        @Test
        void costsTheRefreshCostScaledByTalent() {
            Person person = fixture.person(PLENTY_OF_XP);
            when(person.getUsedEdge()).thenReturn(2);
            when(person.getTalentBasedXpCostMultiplier(anyBoolean(), isNull())).thenReturn(2.0);
            int refreshCost = fixture.options.get(CampaignOption.EDGE_REFRESH_COST);

            assertEquals((int) Math.round(refreshCost * 2.0), fixture.plan().quote(new EdgeRefill(person)).xp());
        }

        @Test
        void notNeededWithNothingSpent() {
            Person person = fixture.person(PLENTY_OF_XP);

            assertEquals(TrainingRefusal.NOT_NEEDED, fixture.plan().quote(new EdgeRefill(person)).refusal());
        }

        @Test
        void notNeededOnceStaged() {
            Person person = fixture.person(PLENTY_OF_XP);
            when(person.getUsedEdge()).thenReturn(1);
            TrainingPlan plan = fixture.plan();
            plan.stage("refill", List.of(new EdgeRefill(person)));

            assertEquals(TrainingRefusal.NOT_NEEDED, plan.quote(new EdgeRefill(person)).refusal());
        }

        @Test
        void refusedWhenTheCampaignDoesntUseEdge() {
            fixture.options.set(CampaignOption.USE_EDGE, false);
            Person person = fixture.person(PLENTY_OF_XP);
            when(person.getUsedEdge()).thenReturn(1);

            assertEquals(TrainingRefusal.DISABLED, fixture.plan().quote(new EdgeRefill(person)).refusal());
        }
    }
}
