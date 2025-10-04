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
package mekhq.campaign.randomEvents.prisoners;

import static mekhq.campaign.personnel.Person.MEKWARRIOR_AERO_RANSOM_VALUES;
import static mekhq.campaign.personnel.Person.OTHER_RANSOM_VALUES;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.randomEvents.prisoners.PrisonerMissionEndEvent.GOOD_EVENT_CHANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

/**
 * This class contains unit tests for the {@link PrisonerMissionEndEvent} class, focusing on the functionality and
 * correctness of methods such as {@code determineGoodEventChance} and {@code getRansom}.
 *
 * <p>The tests validate different scenarios such as determining the chance for a good event based
 * on crime history and calculation of ransom values for prisoners with different roles and skills. Mocked objects are
 * used to isolate dependencies and ensure the test logic is independent of external factors.</p>
 */
class PrisonerMissionEndEventTest {
    @Test
    void testDetermineGoodEventChance_NoCrime() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getDateOfLastCrime()).thenReturn(null);

        AtBContract contract = new AtBContract("TEST");
        contract.setStartDate(today.minusYears(1));

        // Act
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);

        int actualValue = endEvent.determineGoodEventChance(true);

        // Assert
        assertEquals(GOOD_EVENT_CHANCE, actualValue);
    }

    @Test
    void testDetermineGoodEventChance_SomeCrime() {
        final int CRIME_RATING = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getAdjustedCrimeRating()).thenReturn(CRIME_RATING);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

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
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

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
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.isAlternativeQualityAveraging()).thenReturn(false);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        AtBContract contract = new AtBContract("TEST");


        Person prisoner = new Person(mockCampaign);
        prisoner.addSkill(SkillTypeNew.S_GUN_MEK.name(), SKILL_LEVEL, 0);
        prisoner.addSkill(SkillTypeNew.S_PILOT_MEK.name(), SKILL_LEVEL, 0);
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
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.isAlternativeQualityAveraging()).thenReturn(false);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        AtBContract contract = new AtBContract("TEST");


        Person prisoner = new Person(mockCampaign);
        prisoner.addSkill(SkillTypeNew.S_SMALL_ARMS.name(), SKILL_LEVEL, 0);
        prisoner.setPrimaryRole(mockCampaign, SOLDIER);

        // Act
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);

        Money actualValue = endEvent.getRansom(List.of(prisoner));
        Money expectedValue = OTHER_RANSOM_VALUES.get(SKILL_LEVEL - 1);

        // Assert
        assertEquals(expectedValue, actualValue);
    }
}
