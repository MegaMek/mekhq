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
 */
package mekhq.campaign.personnel;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static mekhq.campaign.personnel.RandomDependents.DEPENDENT_CAPACITY_MULTIPLIER;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RandomDependentsTest {
    @Test
    void testPrepareData() {
        final int NUMBER_OF_NON_DEPENDENTS = 20;
        final int NUMBER_OF_DEPENDENTS = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate currentDay = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(currentDay);

        List<Person> activeDependents = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_DEPENDENTS; i++) {
            Person dependent = new Person(mockCampaign);
            dependent.setPrimaryRole(mockCampaign, DEPENDENT);

            activeDependents.add(dependent);
        }
        when(mockCampaign.getActiveDependents()).thenReturn(activeDependents);

        List<Person> activeNonDependent = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_NON_DEPENDENTS; i++) {
            Person nonDependent = new Person(mockCampaign);
            nonDependent.setPrimaryRole(mockCampaign, MEKWARRIOR);

            activeNonDependent.add(nonDependent);
        }
        activeNonDependent.addAll(activeDependents);
        when(mockCampaign.getActivePersonnel(false)).thenReturn(activeNonDependent);

        // Act
        RandomDependents randomDependents = new RandomDependents(mockCampaign);
        int actualValue = randomDependents.prepareData();
        int expectedValue = NUMBER_OF_NON_DEPENDENTS;

        // Assert
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void testCalculateDependentCapacity() {
        final int NUMBER_OF_NON_DEPENDENTS = 20;
        final int DEPENDENT_CAPACITY = max(1, (int) round(NUMBER_OF_NON_DEPENDENTS * DEPENDENT_CAPACITY_MULTIPLIER));

        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate currentDay = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(currentDay);

        List<Person> activeNonDependent = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_NON_DEPENDENTS; i++) {
            Person nonDependent = new Person(mockCampaign);
            nonDependent.setPrimaryRole(mockCampaign, MEKWARRIOR);

            activeNonDependent.add(nonDependent);
        }
        when(mockCampaign.getActivePersonnel()).thenReturn(activeNonDependent);

        // Act
        RandomDependents randomDependents = new RandomDependents(mockCampaign);
        int actualValue = randomDependents.calculateDependentCapacity();
        int expectedValue = DEPENDENT_CAPACITY;

        // Assert
        assertEquals(expectedValue, actualValue);
    }
}
