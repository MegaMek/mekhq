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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

public class AccountantTest {

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
        when(mockCampaign.getLocation()).thenReturn(location);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        when(mockCampaign.getPersonnel()).thenReturn(new ArrayList<>());

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        when(mockCampaign.getPersonnel()).thenReturn(prisoners);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        when(mockCampaign.getPersonnel()).thenReturn(prisoners);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        when(mockCampaign.getPersonnel()).thenReturn(prisoners);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRole(mockCampaign, DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        when(mockCampaign.getPersonnel()).thenReturn(dependents);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRole(mockCampaign, DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        when(mockCampaign.getPersonnel()).thenReturn(dependents);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRole(mockCampaign, DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        when(mockCampaign.getPersonnel()).thenReturn(dependents);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRole(mockCampaign, MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        when(mockCampaign.getPersonnel()).thenReturn(enlistedPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRole(mockCampaign, MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        when(mockCampaign.getPersonnel()).thenReturn(enlistedPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRole(mockCampaign, MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        when(mockCampaign.getPersonnel()).thenReturn(enlistedPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRole(mockCampaign, MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

        when(mockCampaign.getPersonnel()).thenReturn(officerPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRole(mockCampaign, MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

        when(mockCampaign.getPersonnel()).thenReturn(officerPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRole(mockCampaign, MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

        when(mockCampaign.getPersonnel()).thenReturn(officerPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRole(mockCampaign, DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRole(mockCampaign, MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRole(mockCampaign, MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);

        when(mockCampaign.getPersonnel()).thenReturn(allPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRole(mockCampaign, DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRole(mockCampaign, MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRole(mockCampaign, MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);

        when(mockCampaign.getPersonnel()).thenReturn(allPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRole(mockCampaign, DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRole(mockCampaign, MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRole(mockCampaign, MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);

        when(mockCampaign.getPersonnel()).thenReturn(allPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRole(mockCampaign, DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRole(mockCampaign, MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRole(mockCampaign, MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);

        when(mockCampaign.getPersonnel()).thenReturn(allPersonnel);

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
        when(mockCampaign.getLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRole(mockCampaign, DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRole(mockCampaign, MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRole(mockCampaign, MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

        Unit warShip = new Unit();
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.isLargeCraft()).thenReturn(true);
        when(mockEntity.isDropShip()).thenReturn(false);
        warShip.setEntity(mockEntity);

        Person warShipCrew = new Person(mockCampaign);
        warShipCrew.setPrimaryRole(mockCampaign, VESSEL_GUNNER);
        warShipCrew.setRank(RWO_MIN - 1);
        warShipCrew.setUnit(warShip);
        List<Person> warShipPersonnel = List.of(warShipCrew, warShipCrew, warShipCrew);

        List<Person> allPersonnel = new ArrayList<>();
        allPersonnel.addAll(prisoners);
        allPersonnel.addAll(dependents);
        allPersonnel.addAll(enlistedPersonnel);
        allPersonnel.addAll(officerPersonnel);
        allPersonnel.addAll(warShipPersonnel);

        when(mockCampaign.getPersonnel()).thenReturn(allPersonnel);

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
}
