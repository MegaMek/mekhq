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
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ULTRA_GREEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.codeUtilities.MathUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/**
 * Tests {@link SkillTrainingCosts}, the unofficial skill improvement costs from the Hot Spots: Draconis Reach first
 * printing pg 34 table.
 *
 * <p>Skill types are mocked with a simple experience ladder so each step can be priced in isolation: skill level 0-1
 * is Ultra-Green, 2 Green, 3 Regular, 4 Veteran, 5 Elite, 6 Heroic, and 7+ Legendary.</p>
 */
class SkillTrainingCostsTest {
    private static final int SUPPORT_POINTS_TO_C_BILLS = 10_000;
    private static final String SKILL_NAME = "Test Skill";
    private static final LocalDate TODAY = LocalDate.of(3052, 6, 1);

    private MockedStatic<SkillType> skillTypes;
    private SkillType skillType;
    private Campaign campaign;
    private CampaignOptions campaignOptions;

    @BeforeEach
    void setUp() {
        skillType = ladderSkillType();
        skillTypes = mockStatic(SkillType.class);
        skillTypes.when(() -> SkillType.getType(anyString())).thenReturn(skillType);

        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        // Price Utility and Roleplay skills alike so their category-specific calculations are easy to compare.
        campaignOptions.set(CampaignOption.ROLEPLAY_SKILL_TRAINING_COST_MULTIPLIER, 1.0);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);
    }

    @AfterEach
    void tearDown() {
        skillTypes.close();
    }

    /** 0-1 Ultra-Green, 2 Green, 3 Regular, 4 Veteran, 5 Elite, 6 Heroic, 7+ Legendary. */
    private static SkillType ladderSkillType() {
        SkillType ladder = mock(SkillType.class);
        when(ladder.getExperienceLevel(anyInt())).thenAnswer(invocation -> {
            int level = invocation.getArgument(0);
            return Math.max(EXP_ULTRA_GREEN, Math.min(EXP_LEGENDARY, level - 1));
        });
        return ladder;
    }

    /** @return a person with the skill trained to this level; improvement costs follow the trained level alone */
    private Person personWithSkillAt(int trainedLevel) {
        Skill skill = mock(Skill.class);
        when(skill.getLevel()).thenReturn(trainedLevel);
        Person person = mock(Person.class);
        when(person.getSkill(SKILL_NAME)).thenReturn(skill);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.NONE);
        when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        return person;
    }

    private Money cost(Person person, boolean isRoleSkill) {
        return SkillTrainingCosts.getSkillImprovementCost(campaign, person, SKILL_NAME, isRoleSkill);
    }

    private static Money supportPoints(int supportPoints) {
        return Money.of(supportPoints * SUPPORT_POINTS_TO_C_BILLS);
    }

    @Nested
    class Pricing {
        /** Each row improves the skill from the given level into the next experience level. */
        @ParameterizedTest
        @CsvSource({ "2, 100", "3, 200", "4, 700", "5, 1200", "6, 1200" })
        void professionSkillsUseTheProfessionTable(int currentLevel, int expectedSupportPoints) {
            assertEquals(supportPoints(expectedSupportPoints), cost(personWithSkillAt(currentLevel), true));
        }

        @ParameterizedTest
        @CsvSource({ "2, 100", "3, 200", "4, 700", "5, 1200", "6, 1200" })
        void skillsOutsideTheRoleUseTheProfessionTable(int currentLevel, int expectedSupportPoints) {
            assertEquals(supportPoints(expectedSupportPoints), cost(personWithSkillAt(currentLevel), false));
        }

        @Test
        void utilitySkillsUseTheProfessionTableOutsideTheRole() {
            when(skillType.isUtilitySkill()).thenReturn(true);

            // Heroic to Legendary: 1200 on the profession table
            assertEquals(supportPoints(1200), cost(personWithSkillAt(6), false));
        }

        @Test
        void roleplaySkillsUseTheProfessionTableOutsideTheRole() {
            when(skillType.isRoleplaySkill()).thenReturn(true);

            assertEquals(supportPoints(200), cost(personWithSkillAt(3), false));
        }

        /** A combat or support skill outside the person's roles is priced as a Utility skill. */
        @Test
        void combatSkillsOutsideTheRoleArePricedAsUtilitySkills() {
            when(skillType.isUtilitySkill()).thenReturn(false);
            when(skillType.isRoleplaySkill()).thenReturn(false);
            campaignOptions.set(CampaignOption.PROFESSION_SKILL_TRAINING_COST_MULTIPLIER, 5.0);
            campaignOptions.set(CampaignOption.UTILITY_SKILL_TRAINING_COST_MULTIPLIER, 0.5);
            campaignOptions.set(CampaignOption.ROLEPLAY_SKILL_TRAINING_COST_MULTIPLIER, 0.25);

            // Regular to Veteran: 200 x 0.5
            assertEquals(supportPoints(100), cost(personWithSkillAt(3), false));
        }

        /** The price is set by the level reached, not the level left. */
        @Test
        void priceRisesWithTheLevelReached() {
            Money previous = Money.zero();
            for (int currentLevel = 2; currentLevel <= 6; currentLevel++) {
                Money current = cost(personWithSkillAt(currentLevel), false);
                assertEquals(false, current.isLessThan(previous), "level " + currentLevel);
                previous = current;
            }
        }

        @Test
        void professionMultiplierScalesProfessionSkills() {
            campaignOptions.set(CampaignOption.PROFESSION_SKILL_TRAINING_COST_MULTIPLIER, 2.0);

            // Regular to Veteran: 200 x 2
            assertEquals(supportPoints(400), cost(personWithSkillAt(3), true));
        }

        @Test
        void utilityMultiplierScalesUtilitySkills() {
            when(skillType.isUtilitySkill()).thenReturn(true);
            campaignOptions.set(CampaignOption.UTILITY_SKILL_TRAINING_COST_MULTIPLIER, 0.5);

            assertEquals(supportPoints(100), cost(personWithSkillAt(3), false));
        }

        @Test
        void roleplayMultiplierScalesRoleplaySkills() {
            when(skillType.isRoleplaySkill()).thenReturn(true);
            campaignOptions.set(CampaignOption.ROLEPLAY_SKILL_TRAINING_COST_MULTIPLIER, 0.5);

            assertEquals(supportPoints(100), cost(personWithSkillAt(3), false));
        }

        /** A Utility skill belonging to the person's role is priced as a profession skill. */
        @Test
        void roleUtilitySkillsUseTheProfessionMultiplier() {
            when(skillType.isUtilitySkill()).thenReturn(true);
            campaignOptions.set(CampaignOption.PROFESSION_SKILL_TRAINING_COST_MULTIPLIER, 3.0);
            campaignOptions.set(CampaignOption.UTILITY_SKILL_TRAINING_COST_MULTIPLIER, 0.0);

            assertEquals(supportPoints(600), cost(personWithSkillAt(3), true));
        }

        @Test
        void roleplaySkillsOutsideTheRoleAreFreeWhenRoleplayCostsAreZero() {
            when(skillType.isRoleplaySkill()).thenReturn(true);
            campaignOptions.set(CampaignOption.ROLEPLAY_SKILL_TRAINING_COST_MULTIPLIER, 0.0);

            assertEquals(Money.zero(), cost(personWithSkillAt(3), false));
        }

        /** Costs are always charged in C-bills, even if contract pay is kept in raw support points. */
        @Test
        void costsAreInCBillsWhateverTheSupportPointConversionOption() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, false);

            assertEquals(supportPoints(100), cost(personWithSkillAt(2), false));
        }
    }

    @Nested
    class FreeImprovements {
        @Test
        void freeWhenTheOptionIsOff() {
            campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, false);

            assertEquals(Money.zero(), cost(personWithSkillAt(6), false));
            assertEquals(Money.zero(),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, personWithSkillAt(6), SKILL_NAME));
        }

        @Test
        void freeForAnUnknownSkill() {
            skillTypes.when(() -> SkillType.getType(anyString())).thenReturn(null);

            assertEquals(Money.zero(), cost(personWithSkillAt(6), false));
            assertEquals(Money.zero(),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, personWithSkillAt(6), SKILL_NAME));
        }

        @ParameterizedTest
        @CsvSource({ "0", "1" })
        void freeBelowRegular(int currentLevel) {
            // 0 -> 1 stays Ultra-Green; 1 -> 2 reaches Green
            assertEquals(Money.zero(), cost(personWithSkillAt(currentLevel), false));
        }

        @Test
        void freeWhenTheImprovementStaysInTheSameExperienceLevel() {
            // 7 -> 8 is Legendary to Legendary
            assertEquals(Money.zero(), cost(personWithSkillAt(7), false));
        }

        @Test
        void freeBeyondLegendary() {
            assertEquals(Money.zero(), cost(personWithSkillAt(20), true));
        }

        @Test
        void gainingANewSkillAtUltraGreenIsFree() {
            Person person = mock(Person.class);
            when(person.getSkill(SKILL_NAME)).thenReturn(null);

            assertEquals(Money.zero(), cost(person, false));
        }

        /** A new skill whose first level is already Regular is charged as reaching Regular. */
        @Test
        void gainingANewSkillThatStartsAtRegularIsCharged() {
            SkillType startsAtRegular = mock(SkillType.class);
            when(startsAtRegular.getExperienceLevel(anyInt())).thenReturn(EXP_REGULAR);
            skillTypes.when(() -> SkillType.getType(anyString())).thenReturn(startsAtRegular);
            Person person = mock(Person.class);
            when(person.getSkill(SKILL_NAME)).thenReturn(null);

            assertEquals(supportPoints(100), cost(person, false));
        }
    }

    @Nested
    class SkillModifiers {
        /**
         * Attributes, abilities, injuries and other modifiers change how well the person uses the skill, not what
         * training it costs, so pricing follows the trained level alone.
         */
        @Test
        void trainedLevelIsPricedNotTheTotalLevel() {
            Skill skill = mock(Skill.class);
            when(skill.getLevel()).thenReturn(3);
            when(skill.getTotalSkillLevel(any())).thenReturn(1);
            Person person = mock(Person.class);
            when(person.getSkill(SKILL_NAME)).thenReturn(skill);

            // Trained 3 -> 4 is Regular to Veteran; the total level would have been free
            assertEquals(supportPoints(200), cost(person, false));
            verify(skill, never()).getTotalSkillLevel(any());
        }
    }

    /** The whole-career price of a recruit's skills, used for training-based recruitment costs. */
    @Nested
    class AccumulatedCost {
        private Person person;
        private Skills skills;

        @BeforeEach
        void setUpPerson() {
            person = mock(Person.class);
            skills = mock(Skills.class);
            when(person.getSkills()).thenReturn(skills);
            when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
            when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
            when(skills.getSkillNames()).thenReturn(List.of());
        }

        private void holdsSkills(String... skillNamesAndLevels) {
            List<String> skillNames = new ArrayList<>();
            for (int index = 0; index + 1 < skillNamesAndLevels.length; index += 2) {
                String skillName = skillNamesAndLevels[index];
                Skill skill = mock(Skill.class);
                when(skill.getTotalSkillLevel(any())).thenReturn(MathUtility.parseInt(skillNamesAndLevels[index + 1]));
                when(person.getSkill(skillName)).thenReturn(skill);
                skillNames.add(skillName);
            }
            when(skills.getSkillNames()).thenReturn(skillNames);
        }

        /** The combined cost across every category. */
        private Money accumulated() {
            Money total = Money.zero();
            for (Money categoryCost : SkillTrainingCosts.getAccumulatedSkillTrainingCosts(campaign, person).values()) {
                total = total.plus(categoryCost);
            }
            return total;
        }

        private Money accumulated(SkillTrainingCategory category) {
            return SkillTrainingCosts.getAccumulatedSkillTrainingCosts(campaign, person).get(category);
        }

        @Test
        void noSkillsCostNothingInEveryCategory() {
            Map<SkillTrainingCategory, Money> costs = SkillTrainingCosts.getAccumulatedSkillTrainingCosts(campaign,
                  person);

            for (SkillTrainingCategory category : SkillTrainingCategory.values()) {
                assertEquals(Money.zero(), costs.get(category), category.name());
            }
        }

        @ParameterizedTest
        @CsvSource({ "0, 0", "2, 0", "3, 100", "4, 300", "5, 1000", "6, 2200", "7, 3400", "12, 3400" })
        void skillsOutsideTheRoleAddUpEveryLevelReachedAsRoleplaySkills(int level, int expectedSupportPoints) {
            when(skillType.isRoleplaySkill()).thenReturn(true);
            holdsSkills(SkillType.S_TACTICS, String.valueOf(level));

            assertEquals(supportPoints(expectedSupportPoints), accumulated(SkillTrainingCategory.ROLEPLAY));
        }

        @ParameterizedTest
        @CsvSource({ "2, 0", "3, 100", "4, 300", "5, 1000", "6, 2200", "7, 3400" })
        void roleSkillsUseTheProfessionTable(int level, int expectedSupportPoints) {
            holdsSkills(SkillType.S_GUN_MEK, String.valueOf(level));

            assertEquals(supportPoints(expectedSupportPoints), accumulated(SkillTrainingCategory.PROFESSION));
        }

        @Test
        void secondaryRoleSkillsUseTheProfessionTable() {
            when(person.getPrimaryRole()).thenReturn(PersonnelRole.NONE);
            when(person.getSecondaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
            holdsSkills(SkillType.S_PILOT_MEK, "7");

            assertEquals(supportPoints(3400), accumulated(SkillTrainingCategory.PROFESSION));
        }

        @Test
        void utilitySkillsUseTheProfessionTable() {
            when(skillType.isUtilitySkill()).thenReturn(true);
            holdsSkills(SkillType.S_TACTICS, "7");

            assertEquals(supportPoints(3400), accumulated(SkillTrainingCategory.UTILITY));
            assertEquals(supportPoints(3400), accumulated());
        }

        @Test
        void roleplaySkillsUseTheProfessionTable() {
            when(skillType.isRoleplaySkill()).thenReturn(true);
            holdsSkills(SkillType.S_TACTICS, "7");

            assertEquals(supportPoints(3400), accumulated(SkillTrainingCategory.ROLEPLAY));
            assertEquals(supportPoints(3400), accumulated());
        }

        @Test
        void everySkillIsSummedIntoItsCategory() {
            // Veteran role skill (300) + Elite off-role skill (1000) + Green skill (0)
            when(skillType.isRoleplaySkill()).thenReturn(true);
            holdsSkills(SkillType.S_GUN_MEK, "4", SkillType.S_TACTICS, "5", SkillType.S_LEADER, "2");

            assertEquals(supportPoints(300), accumulated(SkillTrainingCategory.PROFESSION));
            assertEquals(supportPoints(1000), accumulated(SkillTrainingCategory.ROLEPLAY));
            assertEquals(supportPoints(1300), accumulated());
        }

        /** Recruitment applies its own multipliers, so the improvement multipliers are left out. */
        @Test
        void improvementMultipliersAreNotApplied() {
            campaignOptions.set(CampaignOption.PROFESSION_SKILL_TRAINING_COST_MULTIPLIER, 3.0);
            holdsSkills(SkillType.S_GUN_MEK, "4");

            assertEquals(supportPoints(300), accumulated(SkillTrainingCategory.PROFESSION));
        }

        @Test
        void unknownSkillsAreSkipped() {
            holdsSkills("Removed Skill", "7", SkillType.S_TACTICS, "3");
            skillTypes.when(() -> SkillType.getType("Removed Skill")).thenReturn(null);

            assertEquals(supportPoints(100), accumulated());
        }

        /** Priced whatever the skill improvement option says; recruitment decides whether it applies. */
        @Test
        void pricedEvenWithTheImprovementOptionOff() {
            campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, false);
            holdsSkills(SkillType.S_TACTICS, "3");

            assertEquals(supportPoints(100), accumulated());
        }

        /** A recruit with every skill Legendary must not overflow. */
        @Test
        void largeTotalsDoNotOverflow() {
            String[] manySkills = new String[2_000];
            for (int index = 0; index < manySkills.length; index += 2) {
                manySkills[index] = "Skill " + index;
                manySkills[index + 1] = "7";
            }
            holdsSkills(manySkills);

            // 1,000 skills x 3,400 SP x 10,000 C-bills
            assertEquals(Money.of(34_000_000_000.0), accumulated());
        }
    }

    @Nested
    class RoleDetection {
        private Person mekWarrior(PersonnelRole primaryRole, PersonnelRole secondaryRole, String skillName) {
            Skill skill = mock(Skill.class);
            when(skill.getLevel()).thenReturn(3);
            Person person = mock(Person.class);
            when(person.getSkill(skillName)).thenReturn(skill);
            when(person.getPrimaryRole()).thenReturn(primaryRole);
            when(person.getSecondaryRole()).thenReturn(secondaryRole);
            return person;
        }

        /** Price Utility and Roleplay skills below profession skills, so the categories can be told apart. */
        @BeforeEach
        void lowerNonProfessionCosts() {
            campaignOptions.set(CampaignOption.UTILITY_SKILL_TRAINING_COST_MULTIPLIER, 0.5);
            campaignOptions.set(CampaignOption.ROLEPLAY_SKILL_TRAINING_COST_MULTIPLIER, 0.25);
        }

        @Test
        void primaryRoleSkillIsAProfessionSkill() {
            Person person = mekWarrior(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE, SkillType.S_GUN_MEK);

            assertEquals(supportPoints(200),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, person, SkillType.S_GUN_MEK));
        }

        @Test
        void secondaryRoleSkillIsAProfessionSkill() {
            Person person = mekWarrior(PersonnelRole.NONE, PersonnelRole.MEKWARRIOR, SkillType.S_PILOT_MEK);

            assertEquals(supportPoints(200),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, person, SkillType.S_PILOT_MEK));
        }

        @Test
        void skillOutsideBothRolesUsesUtilityPricing() {
            Person person = mekWarrior(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE, SkillType.S_TACTICS);

            assertEquals(supportPoints(100),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, person, SkillType.S_TACTICS));
        }

        @Test
        void artilleryOnlyCountsForMekWarriorsWhenArtilleryIsInUse() {
            Person person = mekWarrior(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE, SkillType.S_ARTILLERY);

            campaignOptions.set(CampaignOption.USE_ARTILLERY, false);
            assertEquals(supportPoints(100),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, person, SkillType.S_ARTILLERY));

            campaignOptions.set(CampaignOption.USE_ARTILLERY, true);
            assertEquals(supportPoints(200),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, person, SkillType.S_ARTILLERY));
        }
    }

    /** Quick Train and Mass Training charge through this as well as the personnel menu. */
    @Nested
    class Payment {
        private Finances finances;
        private Person person;

        @BeforeEach
        void setUpFinances() {
            finances = mock(Finances.class);
            when(campaign.getPlayerForce().getFinances()).thenReturn(finances);
            // Regular to Veteran in a skill outside the role: 200 SP
            person = personWithSkillAt(3);
            when(person.getFullTitle()).thenReturn("MekWarrior Test");
        }

        private void debitSucceeds(boolean isSuccessful) {
            when(finances.debit(any(TransactionType.class), any(LocalDate.class), any(Money.class), anyString()))
                  .thenReturn(isSuccessful);
        }

        @Test
        void freeImprovementsAreNotCharged() {
            campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, false);

            assertTrue(SkillTrainingCosts.payForSkillImprovement(campaign, person, SKILL_NAME));
            verify(finances, never()).debit(any(), any(), any(), anyString());
        }

        @Test
        void pricedImprovementsAreChargedAsEducation() {
            debitSucceeds(true);

            assertTrue(SkillTrainingCosts.payForSkillImprovement(campaign, person, SKILL_NAME));
            verify(finances).debit(eq(TransactionType.EDUCATION), eq(TODAY), eq(supportPoints(200)), anyString());
        }

        @Test
        void refusedWhenTheForceCannotPay() {
            debitSucceeds(false);

            assertFalse(SkillTrainingCosts.payForSkillImprovement(campaign, person, SKILL_NAME));
        }

        @Test
        void aPrePricedImprovementIsChargedAtThatPrice() {
            debitSucceeds(true);

            assertTrue(SkillTrainingCosts.payForSkillImprovement(campaign, person, SKILL_NAME, Money.of(1_234)));
            verify(finances).debit(eq(TransactionType.EDUCATION), eq(TODAY), eq(Money.of(1_234)), anyString());
        }
    }

    /**
     * The level-based overload, which the Training Console uses to price improvements that are staged but not yet
     * applied, must price exactly as the person-based one does.
     */
    @Nested
    class LevelBasedPricing {
        @ParameterizedTest
        @CsvSource({ "2, true", "3, true", "4, true", "5, false", "6, false" })
        void matchesThePersonBasedPrice(int currentLevel, boolean isRoleSkill) {
            assertEquals(cost(personWithSkillAt(currentLevel), isRoleSkill),
                  SkillTrainingCosts.getSkillImprovementCost(campaignOptions, skillType, isRoleSkill, currentLevel));
        }

        @Test
        void learningASkillIsFree() {
            assertEquals(Money.zero(), SkillTrainingCosts.getSkillImprovementCost(campaignOptions, skillType, true,
                  -1));
        }

        @Test
        void freeWhenTheOptionIsOff() {
            campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, false);
            assertEquals(Money.zero(), SkillTrainingCosts.getSkillImprovementCost(campaignOptions, skillType, true,
                  3));
        }
    }
}
