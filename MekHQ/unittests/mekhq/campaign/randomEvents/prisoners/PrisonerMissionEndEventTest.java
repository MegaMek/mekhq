/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import static mekhq.campaign.personnel.skills.SkillType.S_GUN_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_PILOT_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_SMALL_ARMS;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.MAX_CRIME_PENALTY;
import static mekhq.campaign.randomEvents.prisoners.PrisonerMissionEndEvent.GOOD_EVENT_CHANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
        Campaign mockCampaign = mockCampaign();
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getPlayerForce().getDateOfLastCrime()).thenReturn(null);

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
        Campaign mockCampaign = mockCampaign();
        when(mockCampaign.getPlayerForce().getAdjustedCrimeRating()).thenReturn(CRIME_RATING);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getPlayerForce().getDateOfLastCrime()).thenReturn(today);

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
        Campaign mockCampaign = mockCampaign();
        when(mockCampaign.getPlayerForce().getAdjustedCrimeRating()).thenReturn(CRIME_RATING);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getPlayerForce().getDateOfLastCrime()).thenReturn(today);

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
        Campaign mockCampaign = mockCampaign();
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.isAlternativeQualityAveraging()).thenReturn(false);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        AtBContract contract = new AtBContract("TEST");

        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        prisoner.addSkill(S_GUN_MEK, SKILL_LEVEL, 0);
        prisoner.addSkill(S_PILOT_MEK, SKILL_LEVEL, 0);
        prisoner.setPrimaryRoleDirect(MEKWARRIOR);

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
        Campaign mockCampaign = mockCampaign();
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.isAlternativeQualityAveraging()).thenReturn(false);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        AtBContract contract = new AtBContract("TEST");

        SkillType.initializeTypes();

        Person prisoner = new Person(mockCampaign);
        prisoner.addSkill(S_SMALL_ARMS, SKILL_LEVEL, 0);
        prisoner.setPrimaryRoleDirect(SOLDIER);

        // Act
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);

        Money actualValue = endEvent.getRansom(List.of(prisoner));
        Money expectedValue = OTHER_RANSOM_VALUES.get(SKILL_LEVEL - 1);

        // Assert
        assertEquals(expectedValue, actualValue);
    }

    // The following tests cover the financial, prisoner-removal, and execution outcome helpers. The "GREGification"
    // merge rerouted these through getPlayerForce().getFinances() / getHumanResources(); the tests pin the direction
    // of each transaction (credit vs debit) and the sign/cap of the crime penalty so a reroute regression is caught.

    @Test
    void testPerformRansom_credit_creditsFinances() {
        // Setup
        Campaign mockCampaign = mockCampaign();
        LocalDate today = LocalDate.of(3151, 1, 1);
        AtBContract contract = new AtBContract("TEST");
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);
        Money ransom = Money.of(1000);

        // Act
        endEvent.performRansom(true, ransom, today);

        // Assert
        verify(mockCampaign.getPlayerForce().getFinances())
              .credit(eq(TransactionType.RANSOM), eq(today), eq(ransom), anyString());
        verify(mockCampaign.getPlayerForce().getFinances(), never())
              .debit(eq(TransactionType.RANSOM), eq(today), eq(ransom), anyString());
    }

    @Test
    void testPerformRansom_debit_debitsFinances() {
        // Setup
        Campaign mockCampaign = mockCampaign();
        LocalDate today = LocalDate.of(3151, 1, 1);
        AtBContract contract = new AtBContract("TEST");
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);
        Money ransom = Money.of(1000);

        // Act
        endEvent.performRansom(false, ransom, today);

        // Assert
        verify(mockCampaign.getPlayerForce().getFinances())
              .debit(eq(TransactionType.RANSOM), eq(today), eq(ransom), anyString());
        verify(mockCampaign.getPlayerForce().getFinances(), never())
              .credit(eq(TransactionType.RANSOM), eq(today), eq(ransom), anyString());
    }

    @Test
    void testRemoveAllPrisoners_removesEachPrisoner() {
        // Setup
        Campaign mockCampaign = mockCampaign();
        AtBContract contract = new AtBContract("TEST");
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);
        Person first = mock(Person.class);
        Person second = mock(Person.class);

        // Act
        endEvent.removeAllPrisoners(List.of(first, second));

        // Assert
        verify(mockCampaign.getPlayerForce().getHumanResources()).removePerson(mockCampaign, first);
        verify(mockCampaign.getPlayerForce().getHumanResources()).removePerson(mockCampaign, second);
    }

    @Test
    void testExecutePrisoners_crimeNoticed_appliesNegativePenaltyAndRecordsDate() {
        // Setup
        Campaign mockCampaign = mockCampaign();
        LocalDate today = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);
        AtBContract contract = new AtBContract("TEST");
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);
        List<Person> prisoners = List.of(mock(Person.class), mock(Person.class), mock(Person.class));

        // Act
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(100)).thenReturn(0); // 0 < 3 -> crime noticed

            endEvent.executePrisoners(prisoners);
        }

        // Assert — penalty = min(MAX_CRIME_PENALTY, 3 * 2) = 6
        verify(mockCampaign.getPlayerForce()).changeCrimeRating(-6);
        verify(mockCampaign.getPlayerForce()).setDateOfLastCrime(today);
    }

    @Test
    void testExecutePrisoners_crimeNoticed_penaltyCappedAtMaximum() {
        // Setup
        Campaign mockCampaign = mockCampaign();
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
        AtBContract contract = new AtBContract("TEST");
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);
        List<Person> prisoners = new ArrayList<>();
        for (int i = 0; i < 100; i++) { // 100 * 2 = 200, which exceeds MAX_CRIME_PENALTY
            prisoners.add(mock(Person.class));
        }

        // Act
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(100)).thenReturn(0);

            endEvent.executePrisoners(prisoners);
        }

        // Assert
        verify(mockCampaign.getPlayerForce()).changeCrimeRating(-MAX_CRIME_PENALTY);
    }

    @Test
    void testExecutePrisoners_crimeUnnoticed_doesNotChangeCrimeRating() {
        // Setup
        Campaign mockCampaign = mockCampaign();
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
        AtBContract contract = new AtBContract("TEST");
        PrisonerMissionEndEvent endEvent = new PrisonerMissionEndEvent(mockCampaign, contract);
        List<Person> prisoners = List.of(mock(Person.class), mock(Person.class), mock(Person.class));

        // Act
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(100)).thenReturn(99); // 99 < 3 is false -> unnoticed

            endEvent.executePrisoners(prisoners);
        }

        // Assert
        verify(mockCampaign.getPlayerForce(), never()).changeCrimeRating(anyInt());
    }
}
