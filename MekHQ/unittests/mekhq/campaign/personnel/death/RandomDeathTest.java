/*
 * Copyright (c) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.death;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link RandomDeath} class.
 *
 * <p>This class contains a suite of tests that validate the functionality of various methods
 * in the {@code RandomDeath} class, including determining if a person can die, simulating
 * random deaths, processing weekly death events, and evaluating campaign-specific configurations.</p>
 *
 * <p>These tests use mocked dependencies, such as {@link Person}, {@link Campaign}, and
 * {@link CampaignOptions}, to isolate the functionality of individual methods and ensure
 * accurate testing without requiring an entire campaign environment.</p>
 *
 * <p>Key Testing Scenarios:</p>
 * <ul>
 *     <li>Validating random death behavior based on age, gender, and configurations.</li>
 *     <li>Ensuring the correct reasons are returned when a person cannot die.</li>
 *     <li>Simulating weekly death processing and ensuring outcomes are consistent.</li>
 *     <li>Validating edge cases, such as no deaths when chances are zero or disabled configurations.</li>
 * </ul>
 */
public class RandomDeathTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RandomDeath";

    @Test
    public void testCanDie_PersonAlreadyDead() {
        Person mockedPerson = mock(Person.class);
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.KIA);

        CampaignOptions mockedOptions = mock(CampaignOptions.class);
        RandomDeath randomDeath = new RandomDeath(mockedOptions);

        String result = randomDeath.canDie(mockedPerson, AgeGroup.ELDER, true);

        assertEquals(getFormattedTextAt(RESOURCE_BUNDLE, "cannotDie.Dead.text"), result);
    }

    @Test
    public void testCanDie_PersonImmortal() {
        Person mockedPerson = mock(Person.class);
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockedPerson.isImmortal()).thenReturn(true);

        CampaignOptions mockedOptions = mock(CampaignOptions.class);
        RandomDeath randomDeath = new RandomDeath(mockedOptions);

        String result = randomDeath.canDie(mockedPerson, AgeGroup.ADULT, true);

        assertEquals(getFormattedTextAt(RESOURCE_BUNDLE, "cannotDie.Immortal.text"), result);
    }

    @Test
    public void testCanDie_AgeGroupDisabled() {
        Person mockedPerson = mock(Person.class);
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockedPerson.isImmortal()).thenReturn(false);

        CampaignOptions mockedOptions = mock(CampaignOptions.class);
        Map<AgeGroup, Boolean> ageGroupMap = Map.of(AgeGroup.ADULT, false);
        when(mockedOptions.getEnabledRandomDeathAgeGroups()).thenReturn(ageGroupMap);

        RandomDeath randomDeath = new RandomDeath(mockedOptions);

        String result = randomDeath.canDie(mockedPerson, AgeGroup.ADULT, true);

        assertEquals(getFormattedTextAt(RESOURCE_BUNDLE, "cannotDie.AgeGroupDisabled.text"), result);
    }

    @Test
    public void testCanDie_RandomDeathFalse() {
        Person mockedPerson = mock(Person.class);
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);

        CampaignOptions mockedOptions = mock(CampaignOptions.class);
        RandomDeath randomDeath = new RandomDeath(mockedOptions);

        String result = randomDeath.canDie(mockedPerson, AgeGroup.ADULT, false);

        assertNull(result);
    }

    @Test
    public void testCanDie_CanDieNullMessage() {
        Person mockedPerson = mock(Person.class);
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockedPerson.isImmortal()).thenReturn(false);

        CampaignOptions mockedOptions = mock(CampaignOptions.class);
        Map<AgeGroup, Boolean> ageGroupMap = Map.of(AgeGroup.ELDER, true);
        when(mockedOptions.getEnabledRandomDeathAgeGroups()).thenReturn(ageGroupMap);

        RandomDeath randomDeath = new RandomDeath(mockedOptions);

        String result = randomDeath.canDie(mockedPerson, AgeGroup.ELDER, true);

        assertNull(result);
    }

    @Test
    public void testRandomlyDies_BaseChanceZero() {
        CampaignOptions mockedOptions = mock(CampaignOptions.class);
        when(mockedOptions.getRandomDeathChance()).thenReturn(0);

        RandomDeath randomDeath = new RandomDeath(mockedOptions);

        assertFalse(randomDeath.randomlyDies(30, Gender.MALE));
    }

    @Test
    public void testRandomlyDies_AgeThresholdAbove() {
        CampaignOptions mockedOptions = mock(CampaignOptions.class);
        when(mockedOptions.getRandomDeathChance()).thenReturn(10);

        RandomDeath randomDeath = new RandomDeath(mockedOptions);

        assertFalse(randomDeath.randomlyDies(95, Gender.MALE));
    }

    @Test
    public void testRandomlyDies_GenderFemaleMultiplier() {
        CampaignOptions mockedOptions = mock(CampaignOptions.class);
        when(mockedOptions.getRandomDeathChance()).thenReturn(10);

        RandomDeath randomDeath = new RandomDeath(mockedOptions);

        assertFalse(randomDeath.randomlyDies(30, Gender.FEMALE));
    }

    @Test
    public void testProcessNewWeek_PersonCannotDie() {
        // Mock setup
        Person mockedPerson = mock(Person.class);
        Campaign mockedCampaign = mock(Campaign.class);
        LocalDate today = LocalDate.now();
        when(mockedPerson.getAge(today)).thenReturn(30);
        when(mockedPerson.getGender()).thenReturn(Gender.MALE);
        RandomDeath mockedRandomDeath = spy(new RandomDeath(mock(CampaignOptions.class)));
        doReturn("Cannot die message").when(mockedRandomDeath).canDie(eq(mockedPerson), any(AgeGroup.class), eq(true));

        // Call processNewWeek
        boolean result = mockedRandomDeath.processNewWeek(mockedCampaign, today, mockedPerson);

        // Assertions
        assertFalse(result);
    }

    @Test
    public void testProcessNewWeek_RandomlyDies() {
        // Mock setup
        Person mockedPerson = mock(Person.class);
        Campaign mockedCampaign = mock(Campaign.class);
        LocalDate today = LocalDate.now();
        when(mockedPerson.getAge(today)).thenReturn(70);
        when(mockedPerson.getGender()).thenReturn(Gender.MALE);

        RandomDeath mockedRandomDeath = spy(new RandomDeath(mock(CampaignOptions.class)));
        doReturn(null).when(mockedRandomDeath).canDie(eq(mockedPerson), any(AgeGroup.class), eq(true));
        doReturn(true).when(mockedRandomDeath).randomlyDies(eq(70), eq(Gender.MALE));
        doReturn(PersonnelStatus.NATURAL_CAUSES).when(mockedRandomDeath).getCause(eq(mockedPerson), any(AgeGroup.class), eq(70));

        // Call processNewWeek
        boolean result = mockedRandomDeath.processNewWeek(mockedCampaign, today, mockedPerson);

        // Assertions
        assertTrue(result);
    }

    @Test
    public void testProcessNewWeek_RandomlySurvives() {
        // Mock setup
        Person mockedPerson = mock(Person.class);
        Campaign mockedCampaign = mock(Campaign.class);
        LocalDate today = LocalDate.now();
        when(mockedPerson.getAge(today)).thenReturn(50);
        when(mockedPerson.getGender()).thenReturn(Gender.FEMALE);

        RandomDeath mockedRandomDeath = spy(new RandomDeath(mock(CampaignOptions.class)));
        doReturn(null).when(mockedRandomDeath).canDie(eq(mockedPerson), any(AgeGroup.class), eq(true));
        doReturn(false).when(mockedRandomDeath).randomlyDies(eq(50), eq(Gender.FEMALE));

        // Call processNewWeek
        boolean result = mockedRandomDeath.processNewWeek(mockedCampaign, today, mockedPerson);

        // Assertions
        assertFalse(result);
    }
}
