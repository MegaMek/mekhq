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
package mekhq.campaign.finances;

import static mekhq.campaign.finances.Accountant.FOOD_ENLISTED;
import static mekhq.campaign.finances.Accountant.FOOD_OFFICER;
import static mekhq.campaign.finances.Accountant.FOOD_PRISONER_OR_DEPENDENT;
import static mekhq.campaign.finances.Accountant.HOUSING_ENLISTED;
import static mekhq.campaign.finances.Accountant.HOUSING_OFFICER;
import static mekhq.campaign.finances.Accountant.HOUSING_PRISONER_OR_DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_GUNNER;
import static mekhq.campaign.personnel.ranks.Rank.RWO_MIN;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.PRISONER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class AccountantTest {

    /** Creates {@code count} distinct {@link Person} objects, each configured by {@code configure}. */
    private static List<Person> createPersons(int count, Campaign campaign, Consumer<Person> configure) {
        List<Person> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Person p = new Person(campaign);
            configure.accept(p);
            result.add(p);
        }
        return result;
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenFoodAndHousingDisabled() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        // Act
        Money expected = Money.zero();
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenNoPersonnel() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        // Act
        Money expected = Money.zero();
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyPrisoners() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> prisoners = createPersons(3, mockCampaign,
              p -> p.setPrisonerStatus(mockCampaign, PRISONER, false));
        when(mockCampaign.getAllPersonnel()).thenReturn(prisoners);

        // Act
        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * prisoners.size();
        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * prisoners.size();
        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_WhenOnlyPrisoners() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> prisoners = createPersons(3, mockCampaign,
              p -> p.setPrisonerStatus(mockCampaign, PRISONER, false));
        when(mockCampaign.getAllPersonnel()).thenReturn(prisoners);

        // Act
        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * prisoners.size();
        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_WhenOnlyPrisoners() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> prisoners = createPersons(3, mockCampaign,
              p -> p.setPrisonerStatus(mockCampaign, PRISONER, false));
        when(mockCampaign.getAllPersonnel()).thenReturn(prisoners);

        // Act
        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * prisoners.size();
        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyDependents() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> dependents = createPersons(3, mockCampaign,
              p -> p.setPrimaryRole(mockCampaign, DEPENDENT));
        when(mockCampaign.getAllPersonnel()).thenReturn(dependents);

        // Act
        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * dependents.size();
        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * dependents.size();
        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_WhenOnlyDependents() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> dependents = createPersons(3, mockCampaign,
              p -> p.setPrimaryRole(mockCampaign, DEPENDENT));
        when(mockCampaign.getAllPersonnel()).thenReturn(dependents);

        // Act
        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * dependents.size();
        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_WhenOnlyDependents() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> dependents = createPersons(3, mockCampaign,
              p -> p.setPrimaryRole(mockCampaign, DEPENDENT));
        when(mockCampaign.getAllPersonnel()).thenReturn(dependents);

        // Act
        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * dependents.size();
        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyEnlisted() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> enlistedPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN - 1);
        });
        when(mockCampaign.getAllPersonnel()).thenReturn(enlistedPersonnel);

        // Act
        int expensesFood = FOOD_ENLISTED * enlistedPersonnel.size();
        int expensesHousing = HOUSING_ENLISTED * enlistedPersonnel.size();
        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_WhenOnlyEnlisted() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> enlistedPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN - 1);
        });
        when(mockCampaign.getAllPersonnel()).thenReturn(enlistedPersonnel);

        // Act
        int expensesHousing = HOUSING_ENLISTED * enlistedPersonnel.size();
        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_WhenOnlyEnlisted() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> enlistedPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN - 1);
        });
        when(mockCampaign.getAllPersonnel()).thenReturn(enlistedPersonnel);

        // Act
        int expensesFood = FOOD_ENLISTED * enlistedPersonnel.size();
        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyOfficers() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> officerPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN + 1);
        });
        when(mockCampaign.getAllPersonnel()).thenReturn(officerPersonnel);

        // Act
        int expensesFood = FOOD_OFFICER * officerPersonnel.size();
        int expensesHousing = HOUSING_OFFICER * officerPersonnel.size();
        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_WhenOnlyOfficers() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> officerPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN + 1);
        });
        when(mockCampaign.getAllPersonnel()).thenReturn(officerPersonnel);

        // Act
        int expensesHousing = HOUSING_OFFICER * officerPersonnel.size();
        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_WhenOnlyOfficers() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> officerPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN + 1);
        });
        when(mockCampaign.getAllPersonnel()).thenReturn(officerPersonnel);

        // Act
        int expensesFood = FOOD_OFFICER * officerPersonnel.size();
        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_Mixed() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> prisoners = createPersons(3, mockCampaign,
              p -> p.setPrisonerStatus(mockCampaign, PRISONER, false));
        List<Person> dependents = createPersons(3, mockCampaign,
              p -> p.setPrimaryRole(mockCampaign, DEPENDENT));
        List<Person> enlistedPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN - 1);
        });
        List<Person> officerPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN + 1);
        });

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);
        when(mockCampaign.getAllPersonnel()).thenReturn(allPersonnel);

        // Act
        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesFood += FOOD_ENLISTED * enlistedPersonnel.size();
        expensesFood += FOOD_OFFICER * officerPersonnel.size();

        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesHousing += HOUSING_ENLISTED * enlistedPersonnel.size();
        expensesHousing += HOUSING_OFFICER * officerPersonnel.size();

        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_Mixed() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> prisoners = createPersons(3, mockCampaign,
              p -> p.setPrisonerStatus(mockCampaign, PRISONER, false));
        List<Person> dependents = createPersons(3, mockCampaign,
              p -> p.setPrimaryRole(mockCampaign, DEPENDENT));
        List<Person> enlistedPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN - 1);
        });
        List<Person> officerPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN + 1);
        });

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);
        when(mockCampaign.getAllPersonnel()).thenReturn(allPersonnel);

        // Act
        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesHousing += HOUSING_ENLISTED * enlistedPersonnel.size();
        expensesHousing += HOUSING_OFFICER * officerPersonnel.size();

        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_Mixed() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> prisoners = createPersons(3, mockCampaign,
              p -> p.setPrisonerStatus(mockCampaign, PRISONER, false));
        List<Person> dependents = createPersons(3, mockCampaign,
              p -> p.setPrimaryRole(mockCampaign, DEPENDENT));
        List<Person> enlistedPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN - 1);
        });
        List<Person> officerPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN + 1);
        });

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);
        when(mockCampaign.getAllPersonnel()).thenReturn(allPersonnel);

        // Act
        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesFood += FOOD_ENLISTED * enlistedPersonnel.size();
        expensesFood += FOOD_OFFICER * officerPersonnel.size();

        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_Mixed_InTransit() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        location.setTransitTime(1);
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> prisoners = createPersons(3, mockCampaign,
              p -> p.setPrisonerStatus(mockCampaign, PRISONER, false));
        List<Person> dependents = createPersons(3, mockCampaign,
              p -> p.setPrimaryRole(mockCampaign, DEPENDENT));
        List<Person> enlistedPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN - 1);
        });
        List<Person> officerPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN + 1);
        });

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);
        when(mockCampaign.getAllPersonnel()).thenReturn(allPersonnel);

        // Act
        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesFood += FOOD_ENLISTED * enlistedPersonnel.size();
        expensesFood += FOOD_OFFICER * officerPersonnel.size();

        int expensesHousing = 0;

        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_Mixed_ExcludingWarShipCrew() {
        // Setup
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        when(mockCampaign.getFaction()).thenReturn(new Faction());

        List<Person> prisoners = createPersons(3, mockCampaign,
              p -> p.setPrisonerStatus(mockCampaign, PRISONER, false));
        List<Person> dependents = createPersons(3, mockCampaign,
              p -> p.setPrimaryRole(mockCampaign, DEPENDENT));
        List<Person> enlistedPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN - 1);
        });
        List<Person> officerPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, MEKWARRIOR);
            p.setRank(RWO_MIN + 1);
        });

        Unit warShip = new Unit();
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.isLargeCraft()).thenReturn(true);
        when(mockEntity.isDropShip()).thenReturn(false);
        warShip.setEntity(mockEntity);

        Unit finalWarShip = warShip;
        List<Person> warShipPersonnel = createPersons(3, mockCampaign, p -> {
            p.setPrimaryRole(mockCampaign, VESSEL_GUNNER);
            p.setRank(RWO_MIN - 1);
            p.setUnit(finalWarShip);
        });

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);
        allPersonnel.addAll(warShipPersonnel);
        when(mockCampaign.getAllPersonnel()).thenReturn(allPersonnel);

        // Act
        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesFood += FOOD_ENLISTED * enlistedPersonnel.size();
        expensesFood += FOOD_OFFICER * officerPersonnel.size();
        expensesFood += FOOD_ENLISTED * warShipPersonnel.size();

        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesHousing += HOUSING_ENLISTED * enlistedPersonnel.size();
        expensesHousing += HOUSING_OFFICER * officerPersonnel.size();

        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        // Assert
        assertEquals(expected, actual);
    }

    /**
     * tests {@link Accountant#getPayRollSummary()}
     */
    @Nested
    class TestGetPayrollSummary {
        Campaign mockCampaign;
        CampaignOptions campaignOptions;
        Accountant accountant;
        final int EXPECTEDPAY = 100;
        final int CREWCOUNT = 5;

        @BeforeEach
        void beforeEach() {
            mockCampaign = mock(Campaign.class);
            campaignOptions = new CampaignOptions();
            accountant = new Accountant(mockCampaign);
        }


        @Test
        void testGetPayrollSummary_emptyCampaign() {
            // Arrange
            when(mockCampaign.getCampaignOptions()).thenReturn(campaignOptions);

            // Act
            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

            // Assert
            assertEquals(1, expectedMap.size());
            assertTrue(expectedMap.containsKey(null));
            assertEquals(Money.zero(), expectedMap.get(null));
        }

        @Test
        void testGetPayrollSummary_onePerson() {
            // Arrange
            Person mockPerson = getMockPerson(EXPECTEDPAY);

            when(mockCampaign.getCampaignOptions()).thenReturn(campaignOptions);
            when(mockCampaign.getSalaryEligiblePersonnel()).thenReturn(List.of(mockPerson));

            // Act
            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

            // Assert
            assertEquals(2, expectedMap.size());
            assertTrue(expectedMap.containsKey(null));
            assertEquals(Money.zero(), expectedMap.get(null));
            assertTrue(expectedMap.containsKey(mockPerson));
            assertEquals(Money.of(EXPECTEDPAY), expectedMap.get(mockPerson));
        }

        @Test
        void testGetAstechPoolPay() {
            // Arrange
            when(mockCampaign.getTemporaryAsTechPool()).thenReturn(5);
            when(mockCampaign.getCampaignOptions()).thenReturn(campaignOptions);

            // Act

            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

            // Assert
            assertEquals(1, expectedMap.size());
            assertTrue(expectedMap.containsKey(null));
            assertEquals(Money.of(2000), expectedMap.get(null));
        }

        @Test
        void testGetMedicPoolPay() {
            // Arrange
            when(mockCampaign.getTemporaryMedicPool()).thenReturn(5);
            when(mockCampaign.getCampaignOptions()).thenReturn(campaignOptions);

            // Act

            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

            // Assert
            assertEquals(1, expectedMap.size());
            assertTrue(expectedMap.containsKey(null));
            assertEquals(Money.of(2000), expectedMap.get(null));
        }

        @ParameterizedTest
        @EnumSource(names = { "SOLDIER", "BATTLE_ARMOUR", "VEHICLE_CREW_GROUND", "VEHICLE_CREW_VTOL",
                              "VEHICLE_CREW_NAVAL", "VESSEL_PILOT", "VESSEL_GUNNER", "VESSEL_CREW" })
        void testGetAllTempCrewPay(PersonnelRole role) {
            // Arrange
            when(mockCampaign.getTempCrewRoleKeys())
                  .thenReturn(new HashSet<>(Collections.singleton(role)));
            when(mockCampaign.getTempCrewPool(any())).thenReturn(CREWCOUNT);
            when(mockCampaign.getCampaignOptions()).thenReturn(campaignOptions);

            // Act
            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

            // Assert
            assertEquals(1, expectedMap.size());
            assertTrue(expectedMap.containsKey(null));
            assertEquals(Money.of(CREWCOUNT *
                                        campaignOptions.getRoleBaseSalaries()[role.ordinal()].getAmount()
                                              .doubleValue()),
                  expectedMap.get(null));
        }

        Person getMockPerson(int pay) {
            Person mockPerson = mock(Person.class);

            when(mockPerson.getSalary(mockCampaign)).thenReturn(Money.of(pay));
            return mockPerson;
        }
    }
}
