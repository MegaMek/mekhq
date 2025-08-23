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
package mekhq.campaign.market.personnelMarket.markets;

import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR_COMMAND;
import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR_HR;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.DOCTOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.LAM_PILOT;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.PROTOMEK_PILOT;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.personnelMarket.records.PersonnelMarketEntry;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

class NewPersonnelMarketTest {
    static PersonnelMarketEntry marketEntryAdminHR = entry(ADMINISTRATOR_HR, 1, 1);
    static PersonnelMarketEntry marketEntryDoctor = entry(DOCTOR, 2, 2);
    static PersonnelMarketEntry marketEntryMekWarrior = entry(MEKWARRIOR, 3, 3);

    private static PersonnelMarketEntry entry(PersonnelRole role, int weight, int count) {
        return new PersonnelMarketEntry(weight, role, count, 3050, 3100, DEPENDENT);
    }

    @Test
    void testGetMarketEntriesAsList_EmptyCase() {
        NewPersonnelMarket personnelMarket = new NewPersonnelMarket(null);
        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = Map.of();

        List<PersonnelMarketEntry> sortedList = personnelMarket.getMarketEntriesAsList(marketEntries);

        assertEquals(0, sortedList.size());
    }

    @Test
    void testGetMarketEntriesAsList_SingleEntry() {
        NewPersonnelMarket personnelMarket = new NewPersonnelMarket(null);
        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = Map.of(DOCTOR, marketEntryDoctor);
        List<PersonnelRole> expectedOrder = List.of(DOCTOR);

        List<PersonnelMarketEntry> sortedList = personnelMarket.getMarketEntriesAsList(marketEntries);

        assertEquals(expectedOrder.size(), sortedList.size());
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertEquals(expectedOrder.get(i), sortedList.get(i).profession());
        }
    }

    @Test
    void testGetMarketEntriesAsList_NormalMixedCase() {
        NewPersonnelMarket personnelMarket = new NewPersonnelMarket(null);
        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = Map.of(DOCTOR,
              marketEntryDoctor,
              MEKWARRIOR,
              marketEntryMekWarrior,
              ADMINISTRATOR_HR,
              marketEntryAdminHR);
        List<PersonnelRole> expectedOrder = List.of(ADMINISTRATOR_HR, DOCTOR, MEKWARRIOR);

        List<PersonnelMarketEntry> sortedList = personnelMarket.getMarketEntriesAsList(marketEntries);

        assertEquals(expectedOrder.size(), sortedList.size());
        for (int i = 0; i < expectedOrder.size(); i++) {
            assertEquals(expectedOrder.get(i), sortedList.get(i).profession());
        }
    }

    @Test
    void testSanitizeMarketEntries_removesEntriesWithNonPositiveWeight() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = new HashMap<>();
        marketEntries.put(PersonnelRole.DOCTOR, entry(PersonnelRole.DOCTOR, -2, 4));
        marketEntries.put(PersonnelRole.MEKWARRIOR, entry(PersonnelRole.MEKWARRIOR, 0, 1));

        // Act
        Map<PersonnelRole, PersonnelMarketEntry> sanitizedEntries = market.sanitizeMarketEntries(new HashMap<>(
              marketEntries));

        // Assert
        assertEquals(0, sanitizedEntries.size());
        assertFalse(sanitizedEntries.containsKey(DOCTOR));
        assertFalse(sanitizedEntries.containsKey(MEKWARRIOR));
    }

    @Test
    void testSanitizeMarketEntries_removesEntriesWithNonPositiveCount() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = new HashMap<>();
        marketEntries.put(PersonnelRole.DOCTOR, entry(PersonnelRole.DOCTOR, 2, -4));
        marketEntries.put(PersonnelRole.MEKWARRIOR, entry(PersonnelRole.MEKWARRIOR, 1, 0));

        // Act
        Map<PersonnelRole, PersonnelMarketEntry> sanitizedEntries = market.sanitizeMarketEntries(new HashMap<>(
              marketEntries));

        // Assert
        assertEquals(0, sanitizedEntries.size());
        assertFalse(sanitizedEntries.containsKey(DOCTOR));
        assertFalse(sanitizedEntries.containsKey(MEKWARRIOR));
    }

    @Test
    void testSanitizeMarketEntries_removesEntryWithBothWeightAndCountZeroOrNegative() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = new HashMap<>();
        marketEntries.put(PersonnelRole.DOCTOR, entry(PersonnelRole.DOCTOR, -2, -4));
        marketEntries.put(PersonnelRole.MEKWARRIOR, entry(PersonnelRole.MEKWARRIOR, 0, 0));

        // Act
        Map<PersonnelRole, PersonnelMarketEntry> sanitizedEntries = market.sanitizeMarketEntries(new HashMap<>(
              marketEntries));

        // Assert
        assertEquals(0, sanitizedEntries.size());
        assertFalse(sanitizedEntries.containsKey(DOCTOR));
        assertFalse(sanitizedEntries.containsKey(MEKWARRIOR));
    }

    @Test
    void testSanitizeMarketEntries_preservesAllValidEntries() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = new HashMap<>();
        marketEntries.put(DOCTOR, entry(DOCTOR, 2, 4));
        marketEntries.put(MEKWARRIOR, entry(MEKWARRIOR, 1, 1));

        // Act
        Map<PersonnelRole, PersonnelMarketEntry> sanitizedEntries = market.sanitizeMarketEntries(new HashMap<>(
              marketEntries));

        // Assert
        assertEquals(marketEntries.keySet(), sanitizedEntries.keySet());
    }

    @Test
    void testSanitizeMarketEntries_removesOnlyInvalidEntriesMixed() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = new HashMap<>();
        marketEntries.put(DOCTOR, entry(DOCTOR, -2, 4));
        marketEntries.put(MEKWARRIOR, entry(MEKWARRIOR, 0, 1));
        marketEntries.put(ADMINISTRATOR_HR, entry(ADMINISTRATOR_HR, 1, -1));
        marketEntries.put(ADMINISTRATOR_COMMAND, entry(ADMINISTRATOR_COMMAND, 1, 0));
        marketEntries.put(LAM_PILOT, entry(LAM_PILOT, -1, -1));
        marketEntries.put(PROTOMEK_PILOT, entry(PROTOMEK_PILOT, 1, 1));

        // Act
        Map<PersonnelRole, PersonnelMarketEntry> sanitizedEntries = market.sanitizeMarketEntries(new HashMap<>(
              marketEntries));

        // Assert
        assertEquals(1, sanitizedEntries.size());
        assertTrue(sanitizedEntries.containsKey(PROTOMEK_PILOT));
        assertFalse(sanitizedEntries.containsKey(DOCTOR));
        assertFalse(sanitizedEntries.containsKey(MEKWARRIOR));
        assertFalse(sanitizedEntries.containsKey(ADMINISTRATOR_HR));
        assertFalse(sanitizedEntries.containsKey(ADMINISTRATOR_COMMAND));
        assertFalse(sanitizedEntries.containsKey(LAM_PILOT));
    }

    @Test
    void testSanitizeMarketEntries_handlesEmptyMap() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = new HashMap<>();

        // Act
        Map<PersonnelRole, PersonnelMarketEntry> sanitizedEntries = market.sanitizeMarketEntries(new HashMap<>(
              marketEntries));

        // Assert
        assertEquals(0, sanitizedEntries.size());
    }

    @Test
    void testPickEntry_selectsCorrectEntryBasedOnRandom() {
        // Setup
        List<PersonnelMarketEntry> marketEntries = List.of(marketEntryDoctor, marketEntryMekWarrior);

        // Act
        try (MockedStatic<Compute> mockedRandom = mockStatic(Compute.class)) {
            mockedRandom.when(() -> Compute.randomInt(anyInt())).thenReturn(0);
            NewPersonnelMarket market = spy(new NewPersonnelMarket(null));
            PersonnelMarketEntry result = market.pickEntry(marketEntries);

            // Assert
            assertEquals(marketEntryDoctor, result);
        }
    }

    @Test
    void testPickEntry_emptyListReturnsNull() {
        NewPersonnelMarket market = new NewPersonnelMarket(null);
        assertNull(market.pickEntry(List.of()));
    }

    @Test
    void testPickEntry_allZeroOrNegativeWeightsReturnsNull() {
        // Setup
        PersonnelMarketEntry zeroDoctor = entry(DOCTOR, 0, 1);
        PersonnelMarketEntry negativeMekWarrior = entry(MEKWARRIOR, -1, 1);
        NewPersonnelMarket market = new NewPersonnelMarket(null);

        // Act
        PersonnelMarketEntry pick = market.pickEntry(List.of(zeroDoctor, negativeMekWarrior));

        // Assert
        assertNull(pick);
    }

    @Test
    void testPickEntry_singlePositiveEntryAlwaysPicked() {
        // Setup
        try (MockedStatic<Compute> mockedRandom = mockStatic(Compute.class)) {
            NewPersonnelMarket market = new NewPersonnelMarket(null);
            mockedRandom.when(() -> Compute.randomInt(1)).thenReturn(0);

            PersonnelMarketEntry negativeMekWarrior = entry(MEKWARRIOR, -1, 1);

            // Act
            PersonnelMarketEntry pick = market.pickEntry(List.of(negativeMekWarrior, marketEntryDoctor));

            assertEquals(marketEntryDoctor, pick);
        }
    }

    @ParameterizedTest
    @CsvSource(value = { "0, ADMINISTRATOR_HR", "1, DOCTOR", "2, DOCTOR", "3, MEKWARRIOR", "4, MEKWARRIOR",
                         "5, MEKWARRIOR" })
    void testPickEntry_multiplePositiveEntriesEachPickable(int randomValue, String expectedEntryKey) {
        // Setup
        List<PersonnelMarketEntry> entries = List.of(marketEntryAdminHR, marketEntryDoctor, marketEntryMekWarrior);
        NewPersonnelMarket market = new NewPersonnelMarket(null);

        // Act
        try (MockedStatic<Compute> mockedRandom = mockStatic(Compute.class)) {
            mockedRandom.when(() -> Compute.randomInt(6)).thenReturn(randomValue);

            PersonnelRole expectedProfession = PersonnelRole.fromString(expectedEntryKey);
            PersonnelRole actualProfession = market.pickEntry(entries).profession();

            // Assert
            assertEquals(expectedProfession, actualProfession);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    void testPickEntry_zeroWeightAmongPositivesNeverPicked(int randomValue) {
        // Setup
        PersonnelMarketEntry zeroDoctor = entry(DOCTOR, 0, 1);
        PersonnelMarketEntry weightedMekWarrior = entry(MEKWARRIOR, 10, 1);
        List<PersonnelMarketEntry> entries = List.of(weightedMekWarrior, zeroDoctor);

        NewPersonnelMarket market = new NewPersonnelMarket(null);

        // Act
        try (MockedStatic<Compute> mockedRandom = mockStatic(Compute.class)) {
            mockedRandom.when(() -> Compute.randomInt(10)).thenReturn(randomValue);

            // Assert
            assertEquals(weightedMekWarrior, market.pickEntry(entries));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    void testPickEntry_zeroCountAmongPositivesNeverPicked(int randomValue) {
        // Setup
        PersonnelMarketEntry weightedMekWarrior = entry(MEKWARRIOR, 10, 1);
        PersonnelMarketEntry negativeCountDoctor = entry(DOCTOR, 100, -1);
        PersonnelMarketEntry zeroCountLAMPilot = entry(LAM_PILOT, 100, 0);

        List<PersonnelMarketEntry> entries = List.of(weightedMekWarrior, negativeCountDoctor, zeroCountLAMPilot);

        NewPersonnelMarket market = new NewPersonnelMarket(null);

        // Act
        try (MockedStatic<Compute> mockedRandom = mockStatic(Compute.class)) {
            mockedRandom.when(() -> Compute.randomInt(10)).thenReturn(randomValue);

            // Assert
            assertEquals(weightedMekWarrior, market.pickEntry(entries));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    void testPickEntry_zeroCountAndZeroWeightAmongPositivesNeverPicked(int randomValue) {
        // Setup
        PersonnelMarketEntry weightedMekWarrior = entry(MEKWARRIOR, 10, 1);
        PersonnelMarketEntry negativeCountDoctor = entry(DOCTOR, 100, -1);
        PersonnelMarketEntry zeroCountLAMPilot = entry(LAM_PILOT, 100, 0);
        PersonnelMarketEntry negativeWeightProtoMekPilot = entry(PROTOMEK_PILOT, -1, 10);
        PersonnelMarketEntry zeroWeightAdmin = entry(ADMINISTRATOR_COMMAND, 0, 10);

        List<PersonnelMarketEntry> entries = List.of(weightedMekWarrior,
              negativeCountDoctor,
              zeroCountLAMPilot,
              negativeWeightProtoMekPilot,
              zeroWeightAdmin);

        NewPersonnelMarket market = new NewPersonnelMarket(null);

        // Act
        try (MockedStatic<Compute> mockedRandom = mockStatic(Compute.class)) {
            mockedRandom.when(() -> Compute.randomInt(10)).thenReturn(randomValue);

            // Assert
            assertEquals(weightedMekWarrior, market.pickEntry(entries));
        }
    }

    @Test
    void generateSingleApplicant_returnNullForNoPick() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);

        // Act
        Person applicant = market.generateSingleApplicant(new HashMap<>(), List.of());

        // Assert
        assertNull(applicant);
    }

    @Test
    void vetEntryForIntroductionAndExtinctionYears_returnNullIfOutOfIterations() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);
        market.setGameYear(9999);

        PersonnelMarketEntry impossibleSoldier = new PersonnelMarketEntry(1, SOLDIER, 1, 3050, 3100, SOLDIER);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = Map.of(SOLDIER, impossibleSoldier);

        // Act
        PersonnelMarketEntry returnedEntry = market.vetEntryForIntroductionAndExtinctionYears(marketEntries,
              impossibleSoldier);

        // Assert
        assertNull(returnedEntry);
    }

    @Test
    void vetEntryForIntroductionAndExtinctionYears_introductionYearMatchesCurrentYear() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);
        market.setGameYear(3050);

        PersonnelMarketEntry soldier = new PersonnelMarketEntry(1, SOLDIER, 1, 3050, 3100, DEPENDENT);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = Map.of(SOLDIER, soldier);

        // Act
        PersonnelMarketEntry returnedEntry = market.vetEntryForIntroductionAndExtinctionYears(marketEntries, soldier);

        // Assert
        assertNotNull(returnedEntry);
    }

    @Test
    void vetEntryForIntroductionAndExtinctionYears_introductionYearBeforeCurrentYearExtinctionYearAfterCurrentYear() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);
        market.setGameYear(3051);

        PersonnelMarketEntry soldier = new PersonnelMarketEntry(1, SOLDIER, 1, 3050, 3100, DEPENDENT);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = Map.of(SOLDIER, soldier);

        // Act
        PersonnelMarketEntry returnedEntry = market.vetEntryForIntroductionAndExtinctionYears(marketEntries, soldier);

        // Assert
        assertNotNull(returnedEntry);
    }

    @Test
    void vetEntryForIntroductionAndExtinctionYears_introductionYearAfterCurrentYear() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);
        market.setGameYear(3049);

        PersonnelMarketEntry soldier = new PersonnelMarketEntry(1, SOLDIER, 1, 3050, 3100, DEPENDENT);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = Map.of(SOLDIER, soldier);

        // Act
        PersonnelMarketEntry returnedEntry = market.vetEntryForIntroductionAndExtinctionYears(marketEntries, soldier);

        // Assert
        assertNull(returnedEntry);
    }

    @Test
    void vetEntryForIntroductionAndExtinctionYears_currentYearEqualsExtinctionYear() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);
        market.setGameYear(3100);

        PersonnelMarketEntry soldier = new PersonnelMarketEntry(1, SOLDIER, 1, 3050, 3100, DEPENDENT);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = Map.of(SOLDIER, soldier);

        // Act
        PersonnelMarketEntry returnedEntry = market.vetEntryForIntroductionAndExtinctionYears(marketEntries, soldier);

        // Assert
        assertNull(returnedEntry);
    }

    @Test
    void vetEntryForIntroductionAndExtinctionYears_currentYearAfterExtinctionYear() {
        // Setup
        NewPersonnelMarket market = new NewPersonnelMarket(null);
        market.setGameYear(3101);

        PersonnelMarketEntry soldier = new PersonnelMarketEntry(1, SOLDIER, 1, 3050, 3100, DEPENDENT);

        Map<PersonnelRole, PersonnelMarketEntry> marketEntries = Map.of(SOLDIER, soldier);

        // Act
        PersonnelMarketEntry returnedEntry = market.vetEntryForIntroductionAndExtinctionYears(marketEntries, soldier);

        // Assert
        assertNull(returnedEntry);
    }

    @Test
    void generateSingleApplicant_returnNullIfApplicantIsNull() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        NewPersonnelMarket market = new NewPersonnelMarket(mockCampaign);
        market.setGameYear(3050);

        Faction faction = new Faction();
        market.setApplicantOriginFactions(List.of(faction));

        when(mockCampaign.newPerson(any(PersonnelRole.class),
              eq(faction.getShortName()),
              eq(Gender.RANDOMIZE))).thenReturn(null);

        PersonnelMarketEntry soldier = new PersonnelMarketEntry(1, SOLDIER, 1, 3050, 3100, SOLDIER);

        Map<PersonnelRole, PersonnelMarketEntry> unorderedMarketEntries = Map.of(SOLDIER, soldier);
        List<PersonnelMarketEntry> orderedMarketEntries = market.getMarketEntriesAsList(unorderedMarketEntries);

        // Act
        try (MockedStatic<Compute> mockedRandom = mockStatic(Compute.class)) {
            mockedRandom.when(() -> Compute.randomInt(4)).thenReturn(0);

            Person applicant = market.generateSingleApplicant(unorderedMarketEntries, orderedMarketEntries);

            // Assert
            assertNull(applicant);
        }
    }

    @Test
    void generateSingleApplicant_returnApplicantIfApplicantIsNotNull() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        NewPersonnelMarket market = new NewPersonnelMarket(mockCampaign);
        market.setGameYear(3050);

        Faction faction = new Faction();
        market.setApplicantOriginFactions(List.of(faction));
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person person = new Person(mockCampaign);
        when(mockCampaign.newPerson(any(PersonnelRole.class),
              eq(faction.getShortName()),
              eq(Gender.RANDOMIZE))).thenReturn(person);

        PersonnelMarketEntry soldier = new PersonnelMarketEntry(1, SOLDIER, 1, 3050, 3100, SOLDIER);

        Map<PersonnelRole, PersonnelMarketEntry> unorderedMarketEntries = Map.of(SOLDIER, soldier);
        List<PersonnelMarketEntry> orderedMarketEntries = market.getMarketEntriesAsList(unorderedMarketEntries);

        // Act
        try (MockedStatic<Compute> mockedRandom = mockStatic(Compute.class)) {
            mockedRandom.when(() -> Compute.randomInt(4)).thenReturn(0);

            Person applicant = market.generateSingleApplicant(unorderedMarketEntries, orderedMarketEntries);

            // Assert
            assertNotNull(applicant);
        }
    }
}
