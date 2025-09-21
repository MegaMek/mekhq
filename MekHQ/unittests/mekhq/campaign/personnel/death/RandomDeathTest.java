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
package mekhq.campaign.personnel.death;

import static megamek.common.eras.EraFlag.STAR_LEAGUE;
import static mekhq.campaign.personnel.enums.AgeGroup.ADULT;
import static mekhq.campaign.personnel.enums.AgeGroup.BABY;
import static mekhq.campaign.personnel.enums.AgeGroup.CHILD;
import static mekhq.campaign.personnel.enums.AgeGroup.ELDER;
import static mekhq.campaign.personnel.enums.AgeGroup.PRETEEN;
import static mekhq.campaign.personnel.enums.AgeGroup.TEENAGER;
import static mekhq.campaign.personnel.enums.AgeGroup.TODDLER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.eras.Era;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link RandomDeath} class.
 *
 * <p>This class contains a suite of tests that validate the functionality of various methods
 * in the {@code RandomDeath} class, including determining if a person can die, simulating random deaths, processing
 * weekly death events, and evaluating campaign-specific configurations.</p>
 *
 * <p>These tests use mocked dependencies, such as {@link Person}, {@link Campaign}, and
 * {@link CampaignOptions}, to isolate the functionality of individual methods and ensure accurate testing without
 * requiring an entire campaign environment.</p>
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

    private static Campaign mockedCampaign;
    private static CampaignOptions mockedCampaignOptions;
    private static LocalDate mockedToday;
    private static Person mockedPerson;
    private static RandomDeath randomDeath;

    private static Map<AgeGroup, Boolean> ageGroups;

    @BeforeEach
    public void beforeAll() {
        // Prep Age Groups
        ageGroups = Map.of(
              ELDER, true,
              ADULT, false,
              TEENAGER, true,
              PRETEEN, true,
              CHILD, true,
              TODDLER, true,
              BABY, true
        );

        mockedCampaign = mock(Campaign.class);
        mockedCampaignOptions = mock(CampaignOptions.class);
        mockedToday = LocalDate.of(3025, 1, 1);
        mockedPerson = mock(Person.class);

        when(mockedCampaign.getCampaignOptions()).thenReturn(mockedCampaignOptions);
        when(mockedCampaignOptions.getEnabledRandomDeathAgeGroups()).thenReturn(ageGroups);
        when(mockedCampaignOptions.isUseRandomDeathSuicideCause()).thenReturn(false);
        when(mockedCampaignOptions.getRandomDeathMultiplier()).thenReturn(1.0);
        when(mockedCampaign.getLocalDate()).thenReturn(mockedToday);

        randomDeath = new RandomDeath(mockedCampaign);
    }

    @Test
    public void testCanDie_PersonAlreadyDead() {
        when(mockedPerson.getAge(any(LocalDate.class))).thenReturn(1);
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.KIA);

        String result = randomDeath.canDie(mockedPerson, true);

        assertEquals(getFormattedTextAt(RESOURCE_BUNDLE, "cannotDie.Dead.text"), result);
    }

    @Test
    public void testCanDie_PersonImmortal() {
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockedPerson.isImmortal()).thenReturn(true);

        String result = randomDeath.canDie(mockedPerson, true);

        assertEquals(getFormattedTextAt(RESOURCE_BUNDLE, "cannotDie.Immortal.text"), result);
    }

    @Test
    public void testCanDie_AgeGroupDisabled() {
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockedPerson.isImmortal()).thenReturn(false);
        when(mockedPerson.getAge(mockedToday)).thenReturn(21);

        String result = randomDeath.canDie(mockedPerson, true);

        assertEquals(getFormattedTextAt(RESOURCE_BUNDLE, "cannotDie.AgeGroupDisabled.text"), result);
    }

    @Test
    public void testCanDie_RandomDeathFalse() {
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);

        String result = randomDeath.canDie(mockedPerson, false);

        assertNull(result);
    }

    @Test
    public void testCanDie_RandomDeathTrue() {
        when(mockedPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockedPerson.isImmortal()).thenReturn(false);
        when(mockedPerson.getAge(mockedToday)).thenReturn(106);

        String result = randomDeath.canDie(mockedPerson, true);

        assertNull(result);
    }

    @Test
    void testRandomlyDies_DeathChanceZero() {
        when(mockedPerson.getAge(any())).thenReturn(25);
        when(mockedPerson.getGender()).thenReturn(Gender.MALE);

        randomDeath = spy(randomDeath);
        doReturn(0.0).when(randomDeath).getBaseDeathChance(mockedPerson);
        doReturn(null).when(randomDeath).canDie(mockedPerson, true);

        boolean result = randomDeath.randomlyDies(mockedPerson);

        assertFalse(result);
    }

    @Test
    void testRandomlyDies_NotDeath() {
        // Mocking the Person object
        when(mockedPerson.getAge(any())).thenReturn(30);
        when(mockedPerson.getGender()).thenReturn(Gender.MALE);

        // Mocking the Era object
        Era mockedEra = mock(Era.class);
        when(mockedEra.getFlags()).thenReturn(Set.of(STAR_LEAGUE));

        // Mocking the Faction object
        Faction mockedFaction = mock(Faction.class);
        when(mockedFaction.isClan()).thenReturn(false);

        // Mocking the Campaign object
        when(mockedCampaign.getEra()).thenReturn(mockedEra);
        when(mockedCampaign.getFaction()).thenReturn(mockedFaction);
        when(mockedCampaign.getLocalDate()).thenReturn(LocalDate.now());

        // Create the RandomDeath object normally, then spy on it
        RandomDeath realRandomDeath = new RandomDeath(mockedCampaign) {
            @Override
            protected int randomInt(int bound) {
                return 1000; // Simulate rolling a 1000
            }
        };

        RandomDeath randomDeath = spy(realRandomDeath);

        // Ensure mocked methods return valid results
        doReturn(1000.0).when(randomDeath).getBaseDeathChance(mockedPerson);
        doReturn(1.0).when(randomDeath).getEraMultiplier(mockedEra);
        doReturn(1.0).when(randomDeath).getFactionMultiplier(mockedFaction);
        doReturn(1.0).when(randomDeath).getHealthModifier(mockedPerson);
        doReturn(null).when(randomDeath).canDie(mockedPerson, true);

        // Use mocked CampaignOptions
        when(mockedCampaignOptions.getRandomDeathMultiplier()).thenReturn(1.0);
        when(mockedCampaign.getCampaignOptions()).thenReturn(mockedCampaignOptions);

        // Act
        boolean result = randomDeath.randomlyDies(mockedPerson);

        // Assert
        assertFalse(result);
    }

    @Test
    void testRandomlyDies_Dies() {
        // Mocking the Person object
        when(mockedPerson.getAge(any())).thenReturn(30);
        when(mockedPerson.getGender()).thenReturn(Gender.MALE);

        // Mocking the Era object
        Era mockedEra = mock(Era.class);
        when(mockedEra.getFlags()).thenReturn(Set.of(STAR_LEAGUE));

        // Mocking the Faction object
        Faction mockedFaction = mock(Faction.class);
        when(mockedFaction.isClan()).thenReturn(false);

        // Mocking the Campaign object
        when(mockedCampaign.getEra()).thenReturn(mockedEra);
        when(mockedCampaign.getFaction()).thenReturn(mockedFaction);
        when(mockedCampaign.getLocalDate()).thenReturn(LocalDate.now());

        // Create the RandomDeath object normally, then spy on it
        RandomDeath realRandomDeath = new RandomDeath(mockedCampaign) {
            @Override
            protected int randomInt(int bound) {
                return 1; // Simulate rolling a 1
            }
        };

        RandomDeath randomDeath = spy(realRandomDeath); // Spy on the real object

        // Ensure mocked methods return valid results
        doReturn(1000.0).when(randomDeath).getBaseDeathChance(mockedPerson);
        doReturn(1.0).when(randomDeath).getEraMultiplier(mockedEra);
        doReturn(1.0).when(randomDeath).getFactionMultiplier(mockedFaction);
        doReturn(1.0).when(randomDeath).getHealthModifier(mockedPerson);
        doReturn(null).when(randomDeath).canDie(mockedPerson, true);

        // Use mocked CampaignOptions
        when(mockedCampaignOptions.getRandomDeathMultiplier()).thenReturn(1.0);
        when(mockedCampaign.getCampaignOptions()).thenReturn(mockedCampaignOptions);

        // Act
        boolean result = randomDeath.randomlyDies(mockedPerson);

        // Assert
        assertTrue(result);
    }
}
