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
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.skills.Attributes.DEFAULT_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.UNTRAINED_SKILL_MODIFIER;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.UNTRAINED_TARGET_NUMBER_ONE_LINKED_ATTRIBUTE;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.UNTRAINED_TARGET_NUMBER_TWO_LINKED_ATTRIBUTES;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.determineTargetNumber;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.getTotalAttributeScoreForSkill;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.performQuickSkillCheck;
import static mekhq.campaign.personnel.skills.SkillType.S_GUN_MEK;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.DISASTROUS;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginValue;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.DEXTERITY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NONE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.REFLEXES;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * The {@link SkillCheckUtilityTest} class is a test suite designed to validate the behavior and functionality of the
 * {@link SkillCheckUtility} class. It contains unit tests and parameterized tests to ensure proper handling of various
 * edge cases and scenarios in skill check calculations, attribute modifiers, and target number determination.
 *
 * <p>Methods in this class include tests for scenarios involving:</p>
 * <ul>
 *     <li>Null checks for person objects in skill checks.</li>
 *     <li>Calculations of total attribute modifiers with different numbers of linked attributes.</li>
 *     <li>Computation of individual attribute modifiers based on attribute scores.</li>
 *     <li>Verification of total attribute scores for skills, given a range of linked attribute configurations.</li>
 *     <li>Determination of target numbers for skill checks, considering trained and untrained skills, single and
 *     multiple attributes, invalid attributes, edge cases, and negative modifiers.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.5
 */
class SkillCheckUtilityTest {
    private static final LocalDate CURRENT_DATE = LocalDate.of(3151, 1, 1);

    @Test
    void testIsPersonNull_EdgeDisallowed() {
        SkillCheckUtility checkUtility = new SkillCheckUtility(null,
              S_GUN_MEK,
              null,
              0,
              false,
              false,
              false,
              false,
              CURRENT_DATE);

        int expectedMarginOfSuccess = getMarginValue(DISASTROUS);
        assertEquals(expectedMarginOfSuccess, checkUtility.getMarginOfSuccess());

        String RESOURCE_BUNDLE = "mekhq.resources.SkillCheckUtility";
        String expectedResultsText = getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.nullPerson");
        assertEquals(expectedResultsText, checkUtility.getResultsText());

        int expectedTargetNumber = Integer.MAX_VALUE;
        assertEquals(expectedTargetNumber, checkUtility.getTargetNumber().getValue());

        int expectedRoll = Integer.MIN_VALUE;
        assertEquals(expectedRoll, checkUtility.getRoll());
    }

    @Test
    void testIsPersonNull_EdgeAllowed() {
        SkillCheckUtility checkUtility = new SkillCheckUtility(null,
              S_GUN_MEK,
              null,
              0,
              true,
              false,
              false,
              false,
              CURRENT_DATE);

        int expectedMarginOfSuccess = getMarginValue(DISASTROUS);
        assertEquals(expectedMarginOfSuccess, checkUtility.getMarginOfSuccess());

        String RESOURCE_BUNDLE = "mekhq.resources.SkillCheckUtility";
        String expectedResultsText = getFormattedTextAt(RESOURCE_BUNDLE, "skillCheck.nullPerson");
        assertEquals(expectedResultsText, checkUtility.getResultsText());

        int expectedTargetNumber = Integer.MAX_VALUE;
        assertEquals(expectedTargetNumber, checkUtility.getTargetNumber().getValue());

        int expectedRoll = Integer.MIN_VALUE;
        assertEquals(expectedRoll, checkUtility.getRoll());
    }

    @Test
    void testIsPersonNull_PerformQuickSkillCheck() {
        boolean results = performQuickSkillCheck(null, S_GUN_MEK, null, 0, false, false, CURRENT_DATE);
        assertFalse(results);
    }

    @Test
    void testGetTotalAttributeScoreForSkill_SingleLinkedAttribute() {
        // Setup
        SkillType testSkillType = new SkillType();
        testSkillType.setFirstAttribute(REFLEXES);
        testSkillType.setSecondAttribute(NONE);

        Attributes attributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              7,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE);

        TargetRoll targetNumber = new TargetRoll();

        // Act
        int totalScore = SkillCheckUtility.getTotalAttributeScoreForSkill(targetNumber, attributes, testSkillType);

        // Assert
        assertEquals(7, totalScore, targetNumber.toString());
    }

    @Test
    void testGetTotalAttributeScoreForSkill_TwoLinkedAttributes() {
        // Setup
        SkillType testSkillType = new SkillType();
        testSkillType.setFirstAttribute(REFLEXES);
        testSkillType.setSecondAttribute(DEXTERITY);

        Attributes attributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              6,
              8,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE);

        TargetRoll targetNumber = new TargetRoll();

        // Act
        SkillCheckUtility.getTotalAttributeScoreForSkill(targetNumber, attributes, testSkillType);

        // Assert
        assertEquals(-14, targetNumber.getValue(), targetNumber.toString());
    }

    @Test
    void testGetTotalAttributeScoreForSkill_NoLinkedAttributes() {
        // Setup
        SkillType testSkillType = new SkillType();
        testSkillType.setFirstAttribute(NONE);
        testSkillType.setSecondAttribute(NONE);

        Attributes attributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              7,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE);

        TargetRoll targetNumber = new TargetRoll();

        // Act
        int totalScore = SkillCheckUtility.getTotalAttributeScoreForSkill(targetNumber, attributes, testSkillType);

        // Assert
        assertEquals(0, totalScore);
    }

    @Test
    void testGetTotalAttributeScoreForSkill_SingleLinkedAttribute_None() {
        // Setup
        SkillType testSkillType = new SkillType();
        testSkillType.setFirstAttribute(NONE);
        testSkillType.setSecondAttribute(REFLEXES);

        Attributes attributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              7,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE);

        TargetRoll targetNumber = new TargetRoll();

        // Act
        SkillCheckUtility.getTotalAttributeScoreForSkill(targetNumber, attributes, testSkillType);

        // Assert
        assertEquals(-7, targetNumber.getValue(), targetNumber.toString());
    }

    @Test
    void testDetermineTargetNumber_UntrainedWithOneLinkedAttribute() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");
        Person person = new Person(mockCampaign);

        try (MockedStatic<SkillType> mockSkillType = Mockito.mockStatic(SkillType.class)) {
            SkillType testSkillType = new SkillType();
            testSkillType.setSecondAttribute(NONE);

            mockSkillType.when(() -> SkillType.getType("MISSING_NAME")).thenReturn(testSkillType);

            // Act
            TargetRoll targetNumber = determineTargetNumber(person, testSkillType, 0, false, false, CURRENT_DATE);

            // Assert
            int expectedTargetNumber = UNTRAINED_TARGET_NUMBER_ONE_LINKED_ATTRIBUTE + UNTRAINED_SKILL_MODIFIER -
                                             person.getAttributeScore(REFLEXES);
            assertEquals(expectedTargetNumber, targetNumber.getValue(), targetNumber.toString());
        }
    }

    @Test
    void testDetermineTargetNumber_UntrainedWithTwoLinkedAttributes() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);

        try (MockedStatic<SkillType> mockSkillType = Mockito.mockStatic(SkillType.class)) {
            SkillType testSkillType = new SkillType();

            mockSkillType.when(() -> SkillType.getType("MISSING_NAME")).thenReturn(testSkillType);

            // Act
            TargetRoll targetNumber = determineTargetNumber(person, testSkillType, 0, false, false, CURRENT_DATE);

            // Assert
            int expectedTargetNumber = UNTRAINED_TARGET_NUMBER_TWO_LINKED_ATTRIBUTES -
                                             person.getAttributeScore(REFLEXES) -
                                             person.getAttributeScore(DEXTERITY) + UNTRAINED_SKILL_MODIFIER;
            assertEquals(expectedTargetNumber, targetNumber.getValue(), targetNumber.toString());
        }
    }

    @Test
    void testDetermineTargetNumber_TrainedWithOneLinkedAttribute() {
        for (int attributeScore = MINIMUM_ATTRIBUTE_SCORE; attributeScore < MAXIMUM_ATTRIBUTE_SCORE; attributeScore++) {
            // Setup
            SkillType testSkillType = new SkillType();
            testSkillType.setSecondAttribute(NONE);

            Skill skill = new Skill(testSkillType, 0, 0);

            Attributes characterAttributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
                  DEFAULT_ATTRIBUTE_SCORE,
                  attributeScore,
                  DEFAULT_ATTRIBUTE_SCORE,
                  DEFAULT_ATTRIBUTE_SCORE,
                  DEFAULT_ATTRIBUTE_SCORE,
                  DEFAULT_ATTRIBUTE_SCORE);

            SkillModifierData skillModifierData = new SkillModifierData(new PersonnelOptions(), characterAttributes,
                  0, false);

            Person mockPerson = mock(Person.class);
            when(mockPerson.hasSkill("MISSING_NAME")).thenReturn(true);
            when(mockPerson.getSkill("MISSING_NAME")).thenReturn(skill);
            when(mockPerson.getATOWAttributes()).thenReturn(characterAttributes);
            when(mockPerson.getOptions()).thenReturn(new PersonnelOptions());
            when(mockPerson.getReputation()).thenReturn(0);
            when(mockPerson.getSkillModifierData(anyBoolean(), anyBoolean(), any(LocalDate.class))).thenReturn(
                  skillModifierData);


            try (MockedStatic<SkillType> mockSkillType = Mockito.mockStatic(SkillType.class)) {
                mockSkillType.when(() -> SkillType.getType("MISSING_NAME")).thenReturn(testSkillType);

                // Act
                TargetRoll targetNumber = determineTargetNumber(mockPerson,
                      testSkillType,
                      0,
                      false,
                      false,
                      CURRENT_DATE);

                // Assert
                int skillTargetNumber = skill.getFinalSkillValue(skillModifierData);

                assertEquals(skillTargetNumber, targetNumber.getValue(), "Attribute Score: " + attributeScore);
            }
        }
    }

    @Test
    void testDetermineTargetNumber_TrainedWithOneLinkedAttribute_AboveNormalAttributeScore() {
        // Setup
        SkillType testSkillType = new SkillType();
        testSkillType.setSecondAttribute(NONE);

        Skill skill = new Skill(testSkillType, 0, 0);

        Attributes characterAttributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              300,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE);

        SkillModifierData skillModifierData = new SkillModifierData(new PersonnelOptions(), characterAttributes,
              0, false);

        Person mockPerson = mock(Person.class);
        when(mockPerson.hasSkill("MISSING_NAME")).thenReturn(true);
        when(mockPerson.getSkill("MISSING_NAME")).thenReturn(skill);
        when(mockPerson.getATOWAttributes()).thenReturn(characterAttributes);
        when(mockPerson.getOptions()).thenReturn(new PersonnelOptions());
        when(mockPerson.getReputation()).thenReturn(0);
        when(mockPerson.getSkillModifierData(anyBoolean(), anyBoolean(), any(LocalDate.class))).thenReturn(
              skillModifierData);

        try (MockedStatic<SkillType> mockSkillType = Mockito.mockStatic(SkillType.class)) {
            mockSkillType.when(() -> SkillType.getType("MISSING_NAME")).thenReturn(testSkillType);

            // Act
            TargetRoll targetNumber = determineTargetNumber(mockPerson, testSkillType, 0, false, false, CURRENT_DATE);

            // Assert
            int skillTargetNumber = skill.getFinalSkillValue(skillModifierData);
            assertEquals(skillTargetNumber, targetNumber.getValue(), targetNumber.toString());
        }
    }

    @Test
    void testDetermineTargetNumber_TrainedWithTwoLinkedAttributes() {
        for (int attributeScore = MINIMUM_ATTRIBUTE_SCORE; attributeScore < MAXIMUM_ATTRIBUTE_SCORE; attributeScore++) {
            // Setup
            SkillType testSkillType = new SkillType();

            Skill skill = new Skill(testSkillType, 0, 0);

            Attributes characterAttributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
                  DEFAULT_ATTRIBUTE_SCORE,
                  attributeScore,
                  attributeScore,
                  DEFAULT_ATTRIBUTE_SCORE,
                  DEFAULT_ATTRIBUTE_SCORE,
                  DEFAULT_ATTRIBUTE_SCORE);

            SkillModifierData skillModifierData = new SkillModifierData(new PersonnelOptions(), characterAttributes,
                  0, false);

            Person mockPerson = mock(Person.class);
            when(mockPerson.hasSkill("MISSING_NAME")).thenReturn(true);
            when(mockPerson.getSkill("MISSING_NAME")).thenReturn(skill);
            when(mockPerson.getATOWAttributes()).thenReturn(characterAttributes);
            when(mockPerson.getOptions()).thenReturn(new PersonnelOptions());
            when(mockPerson.getReputation()).thenReturn(0);
            when(mockPerson.getSkillModifierData(anyBoolean(), anyBoolean(), any(LocalDate.class))).thenReturn(
                  skillModifierData);

            try (MockedStatic<SkillType> mockSkillType = Mockito.mockStatic(SkillType.class)) {
                mockSkillType.when(() -> SkillType.getType("MISSING_NAME")).thenReturn(testSkillType);

                // Act
                TargetRoll targetNumber = determineTargetNumber(mockPerson,
                      testSkillType,
                      0,
                      false,
                      false,
                      CURRENT_DATE);

                // Assert
                int skillTargetNumber = skill.getFinalSkillValue(skillModifierData);

                assertEquals(skillTargetNumber, targetNumber.getValue(),
                      targetNumber + " [Attribute Score: " + attributeScore + ']');
            }
        }
    }

    @Test
    void testDetermineTargetNumber_InvalidAttributes() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");
        Person person = new Person(mockCampaign);

        Attributes invalidAttributes = new Attributes(-5, -5, -5, -5, -5, -5, -5); // Invalid attribute scores
        person.setATOWAttributes(invalidAttributes);

        SkillType testSkillType = new SkillType();
        testSkillType.setFirstAttribute(REFLEXES);
        testSkillType.setSecondAttribute(DEXTERITY);

        try (MockedStatic<SkillType> mockSkillType = Mockito.mockStatic(SkillType.class)) {
            mockSkillType.when(() -> SkillType.getType("MISSING_NAME")).thenReturn(testSkillType);

            // Act
            TargetRoll targetNumber = determineTargetNumber(person, testSkillType, 0, false, false, CURRENT_DATE);

            // Assert
            int expectedTargetNumber = UNTRAINED_TARGET_NUMBER_TWO_LINKED_ATTRIBUTES + UNTRAINED_SKILL_MODIFIER -
                                             SkillCheckUtility.getTotalAttributeScoreForSkill(new TargetRoll(),
                                                   invalidAttributes,
                                                   testSkillType);
            assertEquals(expectedTargetNumber, targetNumber.getValue(), targetNumber.toString());
        }
    }

    @Test
    void testDetermineTargetNumber_EdgeCaseSkillType() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");
        Person person = new Person(mockCampaign);

        // Using default attributes.
        SkillType edgeCaseSkillType = new SkillType();
        edgeCaseSkillType.setFirstAttribute(NONE); // No attributes linked
        edgeCaseSkillType.setSecondAttribute(NONE);
        person.setATOWAttributes(new Attributes());

        try (MockedStatic<SkillType> mockSkillType = Mockito.mockStatic(SkillType.class)) {
            mockSkillType.when(() -> SkillType.getType("MISSING_NAME")).thenReturn(edgeCaseSkillType);

            // Act
            TargetRoll targetNumber = determineTargetNumber(person, edgeCaseSkillType, 0, false, false, CURRENT_DATE);

            // Assert
            int expectedTargetNumber = UNTRAINED_TARGET_NUMBER_ONE_LINKED_ATTRIBUTE + UNTRAINED_SKILL_MODIFIER;
            assertEquals(expectedTargetNumber, targetNumber.getValue(), targetNumber.toString());
        }
    }

    @Test
    void testDetermineTargetNumber_NegativeAttributeModifier() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");
        Person person = new Person(mockCampaign);

        Attributes attributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              1,
              1,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE);

        person.setATOWAttributes(attributes);

        SkillType testSkillType = new SkillType();
        testSkillType.setFirstAttribute(DEXTERITY);
        testSkillType.setSecondAttribute(REFLEXES);

        try (MockedStatic<SkillType> mockSkillType = Mockito.mockStatic(SkillType.class)) {
            mockSkillType.when(() -> SkillType.getType("MISSING_NAME")).thenReturn(testSkillType);

            // Act
            TargetRoll targetNumber = determineTargetNumber(person, testSkillType, 0, false, false, CURRENT_DATE);

            // Assert
            int expectedTargetNumber = UNTRAINED_TARGET_NUMBER_TWO_LINKED_ATTRIBUTES + UNTRAINED_SKILL_MODIFIER -
                                             getTotalAttributeScoreForSkill(new TargetRoll(),
                                                   attributes,
                                                   testSkillType);
            assertEquals(expectedTargetNumber, targetNumber.getValue(), targetNumber.toString());
        }
    }
}
