/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.skills.enums;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.stream.Stream;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Aging;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class AgingMilestoneTest {
    private static final LocalDate today = LocalDate.of(3150, 1, 1);
    private static Campaign mockCampaign;

    @BeforeAll
    static void beforeAll() {
        mockCampaign = mock(Campaign.class);
        when(mockCampaign.getFaction()).thenReturn(Factions.getInstance().getDefaultFaction());
    }

    record milestoneRecord(int age, AgingMilestone expected) {
    }

    static Stream<milestoneRecord> milestoneProvider() {
        return Stream.of(new milestoneRecord(24, AgingMilestone.NONE),
              new milestoneRecord(25, AgingMilestone.TWENTY_FIVE),
              new milestoneRecord(30, AgingMilestone.TWENTY_FIVE),
              new milestoneRecord(31, AgingMilestone.THIRTY_ONE),
              new milestoneRecord(40, AgingMilestone.THIRTY_ONE),
              new milestoneRecord(41, AgingMilestone.FORTY_ONE),
              new milestoneRecord(50, AgingMilestone.FORTY_ONE),
              new milestoneRecord(51, AgingMilestone.FIFTY_ONE),
              new milestoneRecord(60, AgingMilestone.FIFTY_ONE),
              new milestoneRecord(61, AgingMilestone.SIXTY_ONE),
              new milestoneRecord(70, AgingMilestone.SIXTY_ONE),
              new milestoneRecord(71, AgingMilestone.SEVENTY_ONE),
              new milestoneRecord(80, AgingMilestone.SEVENTY_ONE),
              new milestoneRecord(81, AgingMilestone.EIGHTY_ONE),
              new milestoneRecord(90, AgingMilestone.EIGHTY_ONE),
              new milestoneRecord(91, AgingMilestone.NINETY_ONE),
              new milestoneRecord(100, AgingMilestone.NINETY_ONE),
              new milestoneRecord(101, AgingMilestone.ONE_HUNDRED_ONE),
              new milestoneRecord(101, AgingMilestone.ONE_HUNDRED_ONE));
    }

    @ParameterizedTest
    @MethodSource(value = "milestoneProvider")
    void testGetMilestone(milestoneRecord testCase) {
        // Setup
        Person person = new Person(mockCampaign);
        person.setDateOfBirth(today.minusYears(testCase.age));
        int age = person.getAge(today);

        // Act
        AgingMilestone actual = Aging.getMilestone(age);

        // Assert
        assertEquals(testCase.expected, actual, "Invalid milestone for age: " + age);
    }

    @Test
    void testGetAgeModifier_strength() {
        SkillAttribute attribute = SkillAttribute.STRENGTH;

        for (int i = 0; i < 110; i++) {
            // Setup
            Person person = new Person(mockCampaign);
            person.setDateOfBirth(today.minusYears(i));
            int age = person.getAge(today);

            AgingMilestone milestone = Aging.getMilestone(age);

            // Act
            int actual = Aging.getAgeModifier(milestone, attribute, SkillAttribute.NONE);

            int expected = 0;
            for (AgingMilestone validMilestone : AgingMilestone.values()) {
                if (validMilestone.getMinimumAge() <= age) {
                    expected += validMilestone.getAttribute(attribute);
                }
            }

            expected = (int) Math.round((double) expected / Aging.AGING_SKILL_MODIFIER_DIVIDER);

            // Assert
            assertEquals(expected,
                  actual,
                  "Invalid age modifier for milestone: " + milestone.name() + " for " + attribute);
        }
    }

    static Stream<Arguments> ageAndAttributeProvider() {
        // Test all ages 0-109 for each desired attribute
        return Stream.of(SkillAttribute.STRENGTH,
                    SkillAttribute.BODY,
                    SkillAttribute.DEXTERITY,
                    SkillAttribute.REFLEXES,
                    SkillAttribute.INTELLIGENCE,
                    SkillAttribute.WILLPOWER,
                    SkillAttribute.CHARISMA)
                     .flatMap(attribute -> Stream.iterate(0, i -> i + 1)
                                                 .limit(110)
                                                 .map(age -> Arguments.of(age, attribute)));
    }

    @ParameterizedTest
    @MethodSource(value = "ageAndAttributeProvider")
    void testGetAgeModifier_allAttributes(int age, SkillAttribute attribute) {
        Person person = new Person(mockCampaign);
        person.setDateOfBirth(today.minusYears(age));
        int personAge = person.getAge(today);

        AgingMilestone milestone = Aging.getMilestone(personAge);

        int actual = Aging.getAgeModifier(milestone, attribute, SkillAttribute.NONE);

        int expected = 0;
        for (AgingMilestone validMilestone : AgingMilestone.values()) {
            if (validMilestone.getMinimumAge() <= personAge) {
                expected += validMilestone.getAttribute(attribute);
            }
        }
        expected = (int) Math.round((double) expected / Aging.AGING_SKILL_MODIFIER_DIVIDER);

        assertEquals(expected,
              actual,
              "Invalid age modifier for milestone: " +
                    milestone.name() +
                    " for attribute: " +
                    attribute.name() +
                    " and age: " +
                    age);
    }

    @ParameterizedTest
    @EnumSource(AgingMilestone.class)
    void testGetLabel(AgingMilestone milestone) {
        String label = milestone.getLabel();
        assertTrue(isResourceKeyValid(label), "Invalid resource key for milestone: " + label);
    }
}
