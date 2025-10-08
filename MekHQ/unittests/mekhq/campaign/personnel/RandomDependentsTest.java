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
package mekhq.campaign.personnel;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static mekhq.campaign.personnel.RandomDependents.DEPENDENT_CAPACITY_MULTIPLIER;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

class RandomDependentsTest {
    @Test
    void testPrepareData() {
        final int NUMBER_OF_NON_DEPENDENTS = 20;
        final int NUMBER_OF_DEPENDENTS = 5;

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        LocalDate currentDay = LocalDate.of(3151, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(currentDay);

        List<Person> activeDependents = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_DEPENDENTS; i++) {
            Person dependent = new Person(mockCampaign);
            dependent.setPrimaryRole(mockCampaign, DEPENDENT);
            dependent.changeStatus(mockCampaign, currentDay, PersonnelStatus.CAMP_FOLLOWER);

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
        when(mockCampaign.getActivePersonnel(false, false)).thenReturn(activeNonDependent);

        // Act
        RandomDependents randomDependents = new RandomDependents(mockCampaign);
        int actualValue = randomDependents.prepareData();

        // Assert
        assertEquals(NUMBER_OF_NON_DEPENDENTS, actualValue);
    }

    @Test
    void testCalculateDependentCapacity() {
        final int NUMBER_OF_NON_DEPENDENTS = 20;
        final int DEPENDENT_CAPACITY = max(1, (int) round(NUMBER_OF_NON_DEPENDENTS * DEPENDENT_CAPACITY_MULTIPLIER));

        // Setup
        Campaign mockCampaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isMercenary()).thenReturn(true);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

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
        when(mockCampaign.getActivePersonnel(true, true)).thenReturn(activeNonDependent);

        // Act
        RandomDependents randomDependents = new RandomDependents(mockCampaign);
        int actualValue = randomDependents.calculateDependentCapacity();

        // Assert
        assertEquals(DEPENDENT_CAPACITY, actualValue);
    }
}
