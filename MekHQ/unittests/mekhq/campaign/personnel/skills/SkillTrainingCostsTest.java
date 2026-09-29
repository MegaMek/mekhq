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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
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
    private SkillModifierData skillModifierData;

    @BeforeEach
    void setUp() {
        skillType = ladderSkillType();
        skillTypes = mockStatic(SkillType.class);
        skillTypes.when(() -> SkillType.getType(anyString())).thenReturn(skillType);

        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS, true);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);

        skillModifierData = mock(SkillModifierData.class);
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

    private Person personWithSkillAt(int totalLevel) {
        Skill skill = mock(Skill.class);
        when(skill.getTotalSkillLevel(any())).thenReturn(totalLevel);
        Person person = mock(Person.class);
        when(person.getSkill(SKILL_NAME)).thenReturn(skill);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.NONE);
        when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        return person;
    }

    private Money cost(Person person, boolean isRoleSkill) {
        return SkillTrainingCosts.getSkillImprovementCost(campaign, person, SKILL_NAME, isRoleSkill,
              skillModifierData);
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
        @CsvSource({ "2, 100", "3, 300", "4, 700", "5, 1200", "6, 2200" })
        void otherSkillsUseTheNonProfessionTable(int currentLevel, int expectedSupportPoints) {
            assertEquals(supportPoints(expectedSupportPoints), cost(personWithSkillAt(currentLevel), false));
        }

        @Test
        void utilitySkillsUseTheProfessionTableOutsideTheRole() {
            when(skillType.isUtilitySkill()).thenReturn(true);

            // Heroic to Legendary: 1200 on the profession table rather than 2200
            assertEquals(supportPoints(1200), cost(personWithSkillAt(6), false));
        }

        @Test
        void roleplaySkillsUseTheProfessionTableOutsideTheRole() {
            when(skillType.isRoleplaySkill()).thenReturn(true);

            assertEquals(supportPoints(200), cost(personWithSkillAt(3), false));
        }

        @Test
        void combatSkillsOutsideTheRoleUseTheNonProfessionTable() {
            when(skillType.isUtilitySkill()).thenReturn(false);
            when(skillType.isRoleplaySkill()).thenReturn(false);

            assertEquals(supportPoints(300), cost(personWithSkillAt(3), false));
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
        /** The experience level shown to the player includes modifiers, so pricing must use the total level. */
        @Test
        void totalSkillLevelIsPricedNotTheBaseLevel() {
            Skill skill = mock(Skill.class);
            when(skill.getLevel()).thenReturn(1);
            when(skill.getTotalSkillLevel(skillModifierData)).thenReturn(3);
            Person person = mock(Person.class);
            when(person.getSkill(SKILL_NAME)).thenReturn(skill);

            // Total 3 -> 4 is Regular to Veteran
            assertEquals(supportPoints(300), cost(person, false));
            verify(skill).getTotalSkillLevel(skillModifierData);
        }
    }

    @Nested
    class RoleDetection {
        private Person mekWarrior(PersonnelRole primaryRole, PersonnelRole secondaryRole, String skillName) {
            Skill skill = mock(Skill.class);
            when(skill.getTotalSkillLevel(any())).thenReturn(3);
            Person person = mock(Person.class);
            when(person.getSkill(skillName)).thenReturn(skill);
            when(person.getPrimaryRole()).thenReturn(primaryRole);
            when(person.getSecondaryRole()).thenReturn(secondaryRole);
            return person;
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
        void skillOutsideBothRolesIsNotAProfessionSkill() {
            Person person = mekWarrior(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE, SkillType.S_TACTICS);

            assertEquals(supportPoints(300),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, person, SkillType.S_TACTICS));
        }

        @Test
        void artilleryOnlyCountsForMekWarriorsWhenArtilleryIsInUse() {
            Person person = mekWarrior(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE, SkillType.S_ARTILLERY);

            campaignOptions.set(CampaignOption.USE_ARTILLERY, false);
            assertEquals(supportPoints(300),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, person, SkillType.S_ARTILLERY));

            campaignOptions.set(CampaignOption.USE_ARTILLERY, true);
            assertEquals(supportPoints(200),
                  SkillTrainingCosts.getSkillImprovementCost(campaign, person, SkillType.S_ARTILLERY));
        }
    }
}
