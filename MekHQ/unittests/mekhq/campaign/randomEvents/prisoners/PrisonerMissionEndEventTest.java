/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.randomEvents.prisoners;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static mekhq.campaign.personnel.Person.MEKWARRIOR_AERO_RANSOM_VALUES;
import static mekhq.campaign.personnel.Person.OTHER_RANSOM_VALUES;
import static mekhq.campaign.personnel.SkillType.S_GUN_MEK;
import static mekhq.campaign.personnel.SkillType.S_PILOT_MEK;
import static mekhq.campaign.personnel.SkillType.S_SMALL_ARMS;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.randomEvents.prisoners.PrisonerMissionEndEvent.GOOD_EVENT_CHANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class contains unit tests for the {@link PrisonerMissionEndEvent} class, focusing on the
 * functionality and correctness of methods such as {@code determineGoodEventChance} and {@code getRansom}.
 *
 * <p>The tests validate different scenarios such as determining the chance for a good event based
 * on crime history and calculation of ransom values for prisoners with different roles and skills.
 * Mocked objects are used to isolate dependencies and ensure the test logic is independent of
 * external factors.</p>
 */
class PrisonerMissionEndEventTest {
    @Test
    void testDetermineGoodEventChance_NoCrime() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getDateOfLastCrime()).thenReturn(null);

        AtBContract contract = new AtBContract("TEST");
        contract.setStartDate(today.minusYears(1));

        // Act
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);

        int actualValue = endEvent.determineGoodEventChance(true);
        int expectedValue = GOOD_EVENT_CHANCE;

        // Assert
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void testDetermineGoodEventChance_SomeCrime() {
        final int CRIME_RATING = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getAdjustedCrimeRating()).thenReturn(CRIME_RATING);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getDateOfLastCrime()).thenReturn(today);

        AtBContract contract = new AtBContract("TEST");
        contract.setStartDate(today.minusYears(1));

        // Act
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);

        int actualValue = endEvent.determineGoodEventChance(true);
        int expectedValue = GOOD_EVENT_CHANCE - CRIME_RATING;

        // Assert
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void testDetermineGoodEventChance_AllTheCrime() {
        final int CRIME_RATING = GOOD_EVENT_CHANCE * 2;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getAdjustedCrimeRating()).thenReturn(CRIME_RATING);

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getDateOfLastCrime()).thenReturn(today);

        AtBContract contract = new AtBContract("TEST");
        contract.setStartDate(today.minusYears(1));

        // Act
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);

        int actualValue = endEvent.determineGoodEventChance(true);
        int expectedValue = 1; // this is the minimum value accepted by randomInt

        // Assert
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void testGetRansom_MekWarrior() {
        final int SKILL_LEVEL = 3;
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.isAlternativeQualityAveraging()).thenReturn(false);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        AtBContract contract = new AtBContract("TEST");

        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        prisoner.addSkill(S_GUN_MEK, SKILL_LEVEL, 0);
        prisoner.addSkill(S_PILOT_MEK, SKILL_LEVEL, 0);
        prisoner.setPrimaryRole(mockCampaign, MEKWARRIOR);

        // Act
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);

        Money actualValue = endEvent.getRansom(List.of(prisoner));
        Money expectedValue = MEKWARRIOR_AERO_RANSOM_VALUES.get(SKILL_LEVEL - 1);

        // Assert
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void testGetRansom_Other() {
        final int SKILL_LEVEL = 3;
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.isAlternativeQualityAveraging()).thenReturn(false);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        AtBContract contract = new AtBContract("TEST");

        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        prisoner.addSkill(S_SMALL_ARMS, SKILL_LEVEL, 0);
        prisoner.setPrimaryRole(mockCampaign, SOLDIER);

        // Act
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);

        Money actualValue = endEvent.getRansom(List.of(prisoner));
        Money expectedValue = OTHER_RANSOM_VALUES.get(SKILL_LEVEL - 1);

        // Assert
        assertEquals(expectedValue, actualValue);
    }
}
