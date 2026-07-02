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

import static mekhq.campaign.finances.Accountant.*;
import static mekhq.campaign.force.Formation.FORMATION_NONE;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.personnel.enums.PersonnelRole.VESSEL_GUNNER;
import static mekhq.campaign.personnel.ranks.Rank.RWO_MIN;
import static mekhq.campaign.randomEvents.prisoners.PrisonerStatus.PRISONER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import megamek.common.equipment.Engine;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Hangar;
import mekhq.campaign.HumanResources;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.market.contractMarket.AlternatePaymentModelValues;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRoleDirect(DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRoleDirect(DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRoleDirect(DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRoleDirect(MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRoleDirect(MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRoleDirect(MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);
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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRoleDirect(DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRoleDirect(MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRoleDirect(DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRoleDirect(MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRoleDirect(DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRoleDirect(MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRoleDirect(DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRoleDirect(MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

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

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        Person prisoner = new Person(mockCampaign);
        prisoner.setPrisonerStatus(mockCampaign, PRISONER, false);
        List<Person> prisoners = List.of(prisoner, prisoner, prisoner);

        Person dependent = new Person(mockCampaign);
        dependent.setPrimaryRoleDirect(DEPENDENT);
        List<Person> dependents = List.of(dependent, dependent, dependent);

        Person enlisted = new Person(mockCampaign);
        enlisted.setPrimaryRoleDirect(MEKWARRIOR);
        enlisted.setRank(RWO_MIN - 1);
        List<Person> enlistedPersonnel = List.of(enlisted, enlisted, enlisted);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        List<Person> officerPersonnel = List.of(officer, officer, officer);

        Unit warShip = new Unit();
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.isLargeCraft()).thenReturn(true);
        when(mockEntity.isDropShip()).thenReturn(false);
        warShip.setEntity(mockEntity);

        Person warShipCrew = new Person(mockCampaign);
        warShipCrew.setPrimaryRoleDirect(VESSEL_GUNNER);
        warShipCrew.setRank(RWO_MIN - 1);
        warShipCrew.setUnit(warShip);
        List<Person> warShipPersonnel = List.of(warShipCrew, warShipCrew, warShipCrew);

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

    @Test
    void testGetMonthlyFoodAndHousingExpenses_UsesContractBasedFactionStandingBarrackMultiplier() {
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockCampaignOptions.isUseFactionStandingBarracksCostsSafe()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        PlanetarySystem mockSystem = mock(PlanetarySystem.class);
        CurrentLocation location = new CurrentLocation(mockSystem, 0);
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        LocalDate today = LocalDate.of(3025, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        // A contract active in the current system takes precedence over local factions when
        // determining whose regard sets the barrack cost multiplier.
        AtBContract contract = mock(AtBContract.class);
        when(contract.getSystem()).thenReturn(mockSystem);
        when(contract.getEmployerCode()).thenReturn("EMPLOYER");
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        FactionStandings factionStandings = mock(FactionStandings.class);
        when(factionStandings.getRegardForFaction("EMPLOYER", true)).thenReturn(20.0);
        when(mockCampaign.getFactionStandings()).thenReturn(factionStandings);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        when(mockCampaign.getAllPersonnel()).thenReturn(List.of(officer));

        double expectedMultiplier = FactionStandingUtilities.getBarrackCostsMultiplier(20.0);
        Money expected = Money.of(HOUSING_OFFICER).multipliedBy(expectedMultiplier);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_FallsBackToLocalFactionsWhenNoMatchingContract() {
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockCampaignOptions.isUseFactionStandingBarracksCostsSafe()).thenReturn(true);

        Accountant accountant = new Accountant(mockCampaign);

        PlanetarySystem mockSystem = mock(PlanetarySystem.class);
        CurrentLocation location = new CurrentLocation(mockSystem, 0);
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        Faction faction = new Faction();
        when(mockCampaign.getFaction()).thenReturn(faction);

        LocalDate today = LocalDate.of(3025, 1, 1);
        when(mockCampaign.getLocalDate()).thenReturn(today);

        // No active contracts in the current system, so the barrack multiplier falls back to the
        // highest regard among the system's local factions.
        when(mockCampaign.getActiveAtBContracts()).thenReturn(List.of());

        Faction localFaction = mock(Faction.class);
        when(localFaction.getShortName()).thenReturn("LOCAL");
        when(mockSystem.getFactionSet(today)).thenReturn(Set.of(localFaction));

        FactionStandings factionStandings = mock(FactionStandings.class);
        when(factionStandings.getRegardForFaction("LOCAL", true)).thenReturn(15.0);
        when(mockCampaign.getFactionStandings()).thenReturn(factionStandings);

        Person officer = new Person(mockCampaign);
        officer.setPrimaryRoleDirect(MEKWARRIOR);
        officer.setRank(RWO_MIN + 1);
        when(mockCampaign.getAllPersonnel()).thenReturn(List.of(officer));

        double expectedMultiplier = FactionStandingUtilities.getBarrackCostsMultiplier(15.0);
        Money expected = Money.of(HOUSING_OFFICER).multipliedBy(expectedMultiplier);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    /**
     * tests {@link Accountant#getPayRollSummary()}
     */
    @Nested
    class TestGetPayrollSummary {
        Campaign mockCampaign;
        HumanResources mockHumanResources;
        CampaignOptions campaignOptions;
        Accountant accountant;
        final int EXPECTEDPAY = 100;
        final int CREWCOUNT = 5;
        final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        @BeforeEach
        void beforeEach() {
            mockCampaign = mock(Campaign.class);
            mockHumanResources = mock(HumanResources.class);
            campaignOptions = new CampaignOptions();
            accountant = new Accountant(mockCampaign);
            when(mockCampaign.isClanCampaign()).thenReturn(false);
            when(mockCampaign.getLocalDate()).thenReturn(TODAY);
            when(mockCampaign.getHumanResources()).thenReturn(mockHumanResources);
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
            Person mockPerson = getMockPerson();

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
            when(mockHumanResources.getTempPersonnelRoleMap()).thenReturn(Map.of(role, CREWCOUNT));
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

        Person getMockPerson() {
            Person mockPerson = mock(Person.class);

            when(mockPerson.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(EXPECTEDPAY));
            return mockPerson;
        }
    }

    /**
     * tests {@link Accountant#getSalaryTotal(java.util.Collection, CampaignOptions, boolean, LocalDate, boolean)}
     */
    @Nested
    class TestGetSalaryTotal {
        CampaignOptions campaignOptions;
        final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        @BeforeEach
        void beforeEach() {
            campaignOptions = new CampaignOptions();
        }

        @Test
        void testGetSalaryTotal_sumsAllPersonnel() {
            Person personA = mock(Person.class);
            when(personA.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(personA.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(100));

            Person personB = mock(Person.class);
            when(personB.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(personB.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(250));

            Money actual = getSalaryTotal(List.of(personA, personB), campaignOptions, false, TODAY, false);

            assertEquals(Money.of(350), actual);
        }

        @Test
        void testGetSalaryTotal_noInfantryExcludesSoldiers() {
            Person soldier = mock(Person.class);
            when(soldier.getPrimaryRole()).thenReturn(SOLDIER);
            when(soldier.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(100));

            Person mekWarrior = mock(Person.class);
            when(mekWarrior.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(mekWarrior.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(250));

            Money actual = getSalaryTotal(List.of(soldier, mekWarrior), campaignOptions, false, TODAY, true);

            assertEquals(Money.of(250), actual);
        }

        @Test
        void testGetSalaryTotal_emptyCollectionIsZero() {
            Money actual = getSalaryTotal(List.of(), campaignOptions, false, TODAY, false);

            assertEquals(Money.zero(), actual);
        }
    }

    /**
     * tests {@link Accountant#getSparePartsTotal(java.util.Collection)}
     */
    @Nested
    class TestGetSparePartsTotal {
        @Test
        void testGetSparePartsTotal_excludesMothballedUnits() {
            Unit activeUnit = mock(Unit.class);
            when(activeUnit.isMothballed()).thenReturn(false);
            when(activeUnit.getSparePartsCost()).thenReturn(Money.of(100));

            Unit mothballedUnit = mock(Unit.class);
            when(mothballedUnit.isMothballed()).thenReturn(true);
            when(mothballedUnit.getSparePartsCost()).thenReturn(Money.of(500));

            Money actual = getSparePartsTotal(List.of(activeUnit, mothballedUnit));

            assertEquals(Money.of(100), actual);
        }

        @Test
        void testGetSparePartsTotal_emptyCollectionIsZero() {
            Money actual = getSparePartsTotal(List.of());

            assertEquals(Money.zero(), actual);
        }
    }

    /**
     * tests {@link Accountant#getAmmoTotal(java.util.Collection)}
     */
    @Nested
    class TestGetAmmoTotal {
        @Test
        void testGetAmmoTotal_excludesMothballedUnits() {
            Unit activeUnit = mock(Unit.class);
            when(activeUnit.isMothballed()).thenReturn(false);
            when(activeUnit.getAmmoCost()).thenReturn(Money.of(50));

            Unit mothballedUnit = mock(Unit.class);
            when(mothballedUnit.isMothballed()).thenReturn(true);
            when(mothballedUnit.getAmmoCost()).thenReturn(Money.of(999));

            Money actual = getAmmoTotal(List.of(activeUnit, mothballedUnit));

            assertEquals(Money.of(50), actual);
        }

        @Test
        void testGetAmmoTotal_emptyCollectionIsZero() {
            Money actual = getAmmoTotal(List.of());

            assertEquals(Money.zero(), actual);
        }
    }

    /**
     * tests {@link Accountant#getFuelTotal(java.util.Collection)}
     */
    @Nested
    class TestGetFuelTotal {
        @Test
        void testGetFuelTotal_onlyChargesUnitsInTheTOE() {
            Unit unitInToe = newFusionUnit(FORMATION_NONE + 1);
            when(unitInToe.getFuelCost(anyInt())).thenReturn(Money.of(30));

            Unit unitNotInToe = newFusionUnit(FORMATION_NONE);
            when(unitNotInToe.getFuelCost(anyInt())).thenReturn(Money.of(30));

            Money actual = getFuelTotal(List.of(unitInToe, unitNotInToe));
            // only the unit in the TO&E is charged, even though both units burn fuel
            assertEquals(Money.of(30), actual);
        }

        @Test
        void testGetFuelTotal_unitsOutsideTheToeStillContributeToProduction() {
            // a fusion-engined unit outside the TO&E still adds to the hydrogen pool, which is
            // then used to price fuel for whichever units are actually in the TO&E.
            Unit unitInToe = newFusionUnit(FORMATION_NONE + 1);
            Unit unitNotInToe = newFusionUnit(FORMATION_NONE);

            getFuelTotal(List.of(unitInToe, unitNotInToe));
            // both units were consulted for their engine when pricing fuel
            verifyEngineWasChecked(unitInToe);
            verifyEngineWasChecked(unitNotInToe);
        }

        @Test
        void testGetFuelTotal_skipsUnitsWithNullEntityOrEngine() {
            Unit unitWithNoEntity = mock(Unit.class);
            when(unitWithNoEntity.isMothballed()).thenReturn(false);
            when(unitWithNoEntity.getEntity()).thenReturn(null);
            when(unitWithNoEntity.getFormationId()).thenReturn(FORMATION_NONE + 1);
            when(unitWithNoEntity.getFuelCost(anyInt())).thenReturn(Money.zero());

            Entity entityWithNoEngine = mock(Entity.class);
            when(entityWithNoEngine.getEngine()).thenReturn(null);

            Unit unitWithNoEngine = mock(Unit.class);
            when(unitWithNoEngine.isMothballed()).thenReturn(false);
            when(unitWithNoEngine.getEntity()).thenReturn(entityWithNoEngine);
            when(unitWithNoEngine.getFormationId()).thenReturn(FORMATION_NONE + 1);
            when(unitWithNoEngine.getFuelCost(anyInt())).thenReturn(Money.zero());
            // no exception thrown, and both units are still priced at 0 hydrogen production
            Money actual = getFuelTotal(List.of(unitWithNoEntity, unitWithNoEngine));
            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetFuelTotal_emptyCollectionIsZero() {
            Money actual = getFuelTotal(List.of());

            assertEquals(Money.zero(), actual);
        }

        Unit newFusionUnit(int formationId) {
            Engine mockEngine = mock(Engine.class);
            when(mockEngine.isFusion()).thenReturn(true);

            Entity mockEntity = mock(Entity.class);
            when(mockEntity.getEngine()).thenReturn(mockEngine);

            Unit mockUnit = mock(Unit.class);
            when(mockUnit.isMothballed()).thenReturn(false);
            when(mockUnit.getEntity()).thenReturn(mockEntity);
            when(mockUnit.getFormationId()).thenReturn(formationId);

            return mockUnit;
        }

        void verifyEngineWasChecked(Unit unit) {
            assertTrue(unit.getEntity().getEngine().isFusion());
        }
    }

    /**
     * tests
     * {@link Accountant#getPeacetimeOperatingCosts(java.util.Collection, Hangar, CampaignOptions, boolean, LocalDate,
     * int, int, java.util.Map, boolean)}
     */
    @Nested
    class TestGetPeacetimeOperatingCosts {
        CampaignOptions campaignOptions;
        Hangar mockHangar;
        final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        @BeforeEach
        void beforeEach() {
            // A real CampaignOptions is used (rather than a mock) so that unrelated lookups such as
            // getRoleBaseSalaries() - consulted internally while totaling temporary crew pay - resolve to
            // real, non-null values instead of requiring exhaustive stubbing.
            campaignOptions = new CampaignOptions();
            mockHangar = mock(Hangar.class);
        }

        @Test
        void testGetPeacetimeOperatingCosts_walksSubFormationsRecursively() {
            // a top-level formation with no units of its own, and a two-level-deep sub-formation that owns the only
            // unit. The recursive walk should still find it.
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            when(unit.getSparePartsCost()).thenReturn(Money.of(100));
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation grandchildFormation = mock(Formation.class);
            when(grandchildFormation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(grandchildFormation.getSubFormations()).thenReturn(new Vector<>());

            Formation childFormation = mock(Formation.class);
            when(childFormation.getUnits()).thenReturn(new Vector<>());
            when(childFormation.getSubFormations()).thenReturn(new Vector<>(List.of(grandchildFormation)));

            Formation topLevelFormation = mock(Formation.class);
            when(topLevelFormation.getUnits()).thenReturn(new Vector<>());
            when(topLevelFormation.getSubFormations()).thenReturn(new Vector<>(List.of(childFormation)));

            Money actual = getPeacetimeOperatingCosts(List.of(topLevelFormation), mockHangar, campaignOptions, false,
                  TODAY, 0, 0, Map.of(), false);

            assertEquals(Money.of(100), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_unitsOutsideGivenFormationsAreExcluded() {
            // the hangar has a unit that isn't reachable from the given formations. Unlike the
            // old whole-hangar calculation, that unit should not be charged for.
            UUID includedId = UUID.randomUUID();
            Unit includedUnit = mock(Unit.class);
            when(includedUnit.getSparePartsCost()).thenReturn(Money.of(100));
            when(mockHangar.getUnit(includedId)).thenReturn(includedUnit);

            UUID excludedId = UUID.randomUUID();
            Unit excludedUnit = mock(Unit.class);
            when(excludedUnit.getSparePartsCost()).thenReturn(Money.of(500));
            when(mockHangar.getUnit(excludedId)).thenReturn(excludedUnit);
            // excludedUnit is in the hangar, but not referenced by any formation passed in
            when(mockHangar.getUnits()).thenReturn(List.of(includedUnit, excludedUnit));

            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(includedId)));
            when(formation.getSubFormations()).thenReturn(new Vector<>());

            Money actual = getPeacetimeOperatingCosts(List.of(formation), mockHangar, campaignOptions, false, TODAY,
                  0, 0, Map.of(), false);

            assertEquals(Money.of(100), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_includeSalariesFalseExcludesSalaries() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Person crewMember = mock(Person.class);
            when(crewMember.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(crewMember.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));
            when(unit.getCrew()).thenReturn(List.of(crewMember));

            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(formation.getSubFormations()).thenReturn(new Vector<>());

            campaignOptions.setPayForSalaries(true);

            Money actual = getPeacetimeOperatingCosts(List.of(formation), mockHangar, campaignOptions, false, TODAY,
                  0, 0, Map.of(), false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_payForSalariesFalseExcludesSalariesEvenWhenRequested() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Person crewMember = mock(Person.class);
            when(crewMember.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(crewMember.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));
            when(unit.getCrew()).thenReturn(List.of(crewMember));

            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(formation.getSubFormations()).thenReturn(new Vector<>());

            campaignOptions.setPayForSalaries(false);

            Money actual = getPeacetimeOperatingCosts(List.of(formation), mockHangar, campaignOptions, false, TODAY,
                  0, 0, Map.of(), true);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_includesSalariesOfFormationCrews() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Person crewMember = mock(Person.class);
            when(crewMember.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(crewMember.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));
            when(unit.getCrew()).thenReturn(List.of(crewMember));

            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(formation.getSubFormations()).thenReturn(new Vector<>());

            campaignOptions.setPayForSalaries(true);
            campaignOptions.setUseInfantryDontCount(false);

            Money actual = getPeacetimeOperatingCosts(List.of(formation), mockHangar, campaignOptions, false, TODAY,
                  0, 0, Map.of(), true);

            assertEquals(Money.of(1000), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_emptyFormationsIsZero() {
            Money actual = getPeacetimeOperatingCosts(List.of(), mockHangar, campaignOptions, false, TODAY, 0, 0,
                  Map.of(), true);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_includesTemporaryCrewPay() {
            // With no formations there are no units or crews at all, so any nonzero total here can only
            // have come from the astech pool - isolating the temporary-crew-pay contribution.
            campaignOptions.setPayForSalaries(true);

            double expectedTempCrewPay = campaignOptions.getRoleBaseSalaries()[PersonnelRole.ASTECH.ordinal()]
                                               .getAmount()
                                               .doubleValue() * 5;

            Money actual = getPeacetimeOperatingCosts(List.of(), mockHangar, campaignOptions, false, TODAY, 5, 0,
                  Map.of(), true);

            assertEquals(Money.of(expectedTempCrewPay), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_skipsUnitIdsMissingFromHangar() {
            UUID missingUnitId = UUID.randomUUID();
            // mockHangar.getUnit(missingUnitId) is left unstubbed, so it returns null - simulating a unit
            // that was removed from the hangar but is still referenced by a formation.

            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(missingUnitId)));
            when(formation.getSubFormations()).thenReturn(new Vector<>());

            Money actual = getPeacetimeOperatingCosts(List.of(formation), mockHangar, campaignOptions, false, TODAY,
                  0, 0, Map.of(), false);

            assertEquals(Money.zero(), actual);
        }
    }

    /**
     * These tests confirm that, for a campaign where every hangar unit is assigned somewhere in the TO&E and every
     * salary-eligible person crews one of those units (i.e. the common case today), routing
     * {@link Accountant#getPeacetimeCost(boolean)} through the new formation-based
     * {@link Accountant#getPeacetimeOperatingCosts(java.util.Collection, Hangar, CampaignOptions, boolean, LocalDate,
     * int, int, java.util.Map, boolean)} produces the exact same total that the old whole-campaign calculation used to
     * produce.
     */
    @Nested
    class TestPeacetimeCostMatchesLegacyBehavior {
        Campaign mockCampaign;
        CampaignOptions mockCampaignOptions;
        Hangar mockHangar;
        Accountant accountant;
        final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        Unit unit1;
        Unit unit2;
        Person person1;
        Person person2;

        @BeforeEach
        void beforeEach() {
            mockCampaign = mock(Campaign.class);
            // A real CampaignOptions is used (rather than a mock) so that unrelated lookups such as
            // getRoleBaseSalaries() - consulted internally while totaling temporary crew pay - resolve to
            // real, non-null values instead of requiring exhaustive stubbing.
            mockCampaignOptions = new CampaignOptions();
            mockHangar = mock(Hangar.class);
            accountant = new Accountant(mockCampaign);

            when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
            when(mockCampaign.getAllHangar()).thenReturn(mockHangar);
            when(mockCampaign.isClanCampaign()).thenReturn(false);
            when(mockCampaign.getLocalDate()).thenReturn(TODAY);
            when(mockCampaign.getHumanResources()).thenReturn(mock(HumanResources.class));
            mockCampaignOptions.setPayForSalaries(true);
            mockCampaignOptions.setUseInfantryDontCount(false);

            // Two units, each with a fusion engine, each in the TO&E, each fully paid for
            UUID unitId1 = UUID.randomUUID();
            unit1 = newFusionUnit(1);
            when(unit1.getSparePartsCost()).thenReturn(Money.of(100));
            when(unit1.getAmmoCost()).thenReturn(Money.of(50));
            when(unit1.getFuelCost(anyInt())).thenReturn(Money.of(30));

            UUID unitId2 = UUID.randomUUID();
            unit2 = newFusionUnit(2);
            when(unit2.getSparePartsCost()).thenReturn(Money.of(200));
            when(unit2.getAmmoCost()).thenReturn(Money.of(75));
            when(unit2.getFuelCost(anyInt())).thenReturn(Money.of(40));

            when(mockHangar.getUnit(unitId1)).thenReturn(unit1);
            when(mockHangar.getUnit(unitId2)).thenReturn(unit2);
            when(mockCampaign.getAllUnits()).thenReturn(List.of(unit1, unit2));

            // Each unit's crew is exactly the campaign's salary-eligible personnel - nobody is
            // unassigned, so the formation-derived roster matches the whole-campaign roster.
            person1 = mock(Person.class);
            when(person1.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person1.getSalary(mockCampaignOptions, false, TODAY)).thenReturn(Money.of(1000));
            when(unit1.getCrew()).thenReturn(List.of(person1));

            person2 = mock(Person.class);
            when(person2.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person2.getSalary(mockCampaignOptions, false, TODAY)).thenReturn(Money.of(1500));
            when(unit2.getCrew()).thenReturn(List.of(person2));

            when(mockCampaign.getSalaryEligiblePersonnel()).thenReturn(List.of(person1, person2));

            // Every unit is reachable from the top-level formations Accountant now passes along
            Formation formationA = mock(Formation.class);
            when(formationA.getUnits()).thenReturn(new Vector<>(List.of(unitId1)));
            when(formationA.getSubFormations()).thenReturn(new Vector<>());

            Formation formationB = mock(Formation.class);
            when(formationB.getUnits()).thenReturn(new Vector<>(List.of(unitId2)));
            when(formationB.getSubFormations()).thenReturn(new Vector<>());

            Formation rootFormation = mock(Formation.class);
            when(rootFormation.getSubFormations()).thenReturn(new Vector<>(List.of(formationA, formationB)));

            when(mockCampaign.getFormations()).thenReturn(rootFormation);
        }

        @Test
        void testGetPeacetimeCost_includingSalaries_matchesLegacyTotal() {
            // The "legacy" total: spare parts/fuel/ammo summed over every campaign unit, salaries
            // summed over every salary-eligible person - exactly what the old implementation did.
            Money legacyTotal = getSparePartsTotal(mockCampaign.getAllUnits())
                                      .plus(getFuelTotal(mockCampaign.getAllUnits()))
                                      .plus(getAmmoTotal(mockCampaign.getAllUnits()))
                                      .plus(getSalaryTotal(mockCampaign.getSalaryEligiblePersonnel(),
                                            mockCampaignOptions,
                                            false,
                                            TODAY,
                                            false));

            Money actual = accountant.getPeacetimeCost(true);

            assertEquals(legacyTotal, actual);
            assertEquals(Money.of(2995), actual);
        }

        @Test
        void testGetPeacetimeCost_noArgDefaultsToIncludingSalaries() {
            Money withArg = accountant.getPeacetimeCost(true);
            Money noArg = accountant.getPeacetimeCost();

            assertEquals(withArg, noArg);
        }

        @Test
        void testGetPeacetimeCost_excludingSalaries_matchesLegacyTotal() {
            // The "legacy" total without salaries: just spare parts, fuel, and ammo.
            Money legacyTotal = getSparePartsTotal(mockCampaign.getAllUnits())
                                      .plus(getFuelTotal(mockCampaign.getAllUnits()))
                                      .plus(getAmmoTotal(mockCampaign.getAllUnits()));

            Money actual = accountant.getPeacetimeCost(false);

            assertEquals(legacyTotal, actual);
            assertEquals(Money.of(495), actual);
        }

        @Test
        void testGetMonthlySpareParts_stillMatchesWholeCampaignTotal() {
            Money actual = accountant.getMonthlySpareParts();

            assertEquals(getSparePartsTotal(mockCampaign.getAllUnits()), actual);
        }

        @Test
        void testGetMonthlyFuel_stillMatchesWholeCampaignTotal() {
            Money actual = accountant.getMonthlyFuel();

            assertEquals(getFuelTotal(mockCampaign.getAllUnits()), actual);
        }

        @Test
        void testGetMonthlyAmmo_stillMatchesWholeCampaignTotal() {
            Money actual = accountant.getMonthlyAmmo();

            assertEquals(getAmmoTotal(mockCampaign.getAllUnits()), actual);
        }

        @Test
        void testGetPayRoll_stillMatchesWholeCampaignSalaryTotal() {
            Money actual = accountant.getPayRoll();

            assertEquals(getSalaryTotal(mockCampaign.getSalaryEligiblePersonnel(), mockCampaignOptions, false, TODAY,
                  false), actual);
        }

        Unit newFusionUnit(int formationId) {
            Engine mockEngine = mock(Engine.class);
            when(mockEngine.isFusion()).thenReturn(true);

            Entity mockEntity = mock(Entity.class);
            when(mockEntity.getEngine()).thenReturn(mockEngine);

            Unit mockUnit = mock(Unit.class);
            when(mockUnit.isMothballed()).thenReturn(false);
            when(mockUnit.getEntity()).thenReturn(mockEntity);
            when(mockUnit.getFormationId()).thenReturn(formationId);

            return mockUnit;
        }
    }

    /**
     * tests
     * {@link Accountant#getPayRollTotal(java.util.Collection, CampaignOptions, boolean, LocalDate, int, int,
     * java.util.Map, boolean, boolean)}
     */
    @Nested
    class TestGetPayRollTotal {
        CampaignOptions campaignOptions;
        final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        @BeforeEach
        void beforeEach() {
            campaignOptions = new CampaignOptions();
        }

        @Test
        void testGetPayRollTotal_payForSalariesFalseIsZero() {
            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(500));

            Money actual = getPayRollTotal(List.of(person), campaignOptions, false, TODAY, 0, 0, Map.of(), false,
                  false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetPayRollTotal_sumsSalariesWhenPayForSalariesTrue() {
            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(500));

            Money actual = getPayRollTotal(List.of(person), campaignOptions, false, TODAY, 0, 0, Map.of(), false,
                  true);

            assertEquals(Money.of(500), actual);
        }

        @Test
        void testGetPayRollTotal_noInfantryExcludesSoldiers() {
            Person soldier = mock(Person.class);
            when(soldier.getPrimaryRole()).thenReturn(SOLDIER);
            when(soldier.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(500));

            Money actual = getPayRollTotal(List.of(soldier), campaignOptions, false, TODAY, 0, 0, Map.of(), true,
                  true);

            assertEquals(Money.zero(), actual);
        }
    }

    /**
     * tests {@link Accountant#getMaintenanceTotal(java.util.Collection, boolean)}
     */
    @Nested
    class TestGetMaintenanceTotal {
        @Test
        void testGetMaintenanceTotal_payForMaintainFalseIsZero() {
            Unit unit = mock(Unit.class);
            when(unit.requiresMaintenance()).thenReturn(true);
            when(unit.getTech()).thenReturn(mock(Person.class));
            when(unit.getMaintenanceCost()).thenReturn(Money.of(100));

            Money actual = getMaintenanceTotal(List.of(unit), false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetMaintenanceTotal_excludesUnitsWithoutTech() {
            Unit unitWithTech = mock(Unit.class);
            when(unitWithTech.requiresMaintenance()).thenReturn(true);
            when(unitWithTech.getTech()).thenReturn(mock(Person.class));
            when(unitWithTech.getMaintenanceCost()).thenReturn(Money.of(100));

            Unit unitWithoutTech = mock(Unit.class);
            when(unitWithoutTech.requiresMaintenance()).thenReturn(true);
            when(unitWithoutTech.getTech()).thenReturn(null);
            when(unitWithoutTech.getMaintenanceCost()).thenReturn(Money.of(500));

            Money actual = getMaintenanceTotal(List.of(unitWithTech, unitWithoutTech), true);

            assertEquals(Money.of(100), actual);
        }

        @Test
        void testGetMaintenanceTotal_excludesUnitsThatDontRequireMaintenance() {
            Unit unit = mock(Unit.class);
            when(unit.requiresMaintenance()).thenReturn(false);
            when(unit.getTech()).thenReturn(mock(Person.class));
            when(unit.getMaintenanceCost()).thenReturn(Money.of(100));

            Money actual = getMaintenanceTotal(List.of(unit), true);

            assertEquals(Money.zero(), actual);
        }
    }

    /**
     * tests {@link Accountant#getWeeklyMaintenanceTotal(java.util.Collection)}
     */
    @Nested
    class TestGetWeeklyMaintenanceTotal {
        @Test
        void testGetWeeklyMaintenanceTotal_sumsAllUnitsRegardlessOfMaintenanceSettings() {
            Unit unitA = mock(Unit.class);
            when(unitA.getWeeklyMaintenanceCost()).thenReturn(Money.of(10));

            Unit unitB = mock(Unit.class);
            when(unitB.getWeeklyMaintenanceCost()).thenReturn(Money.of(20));

            Money actual = getWeeklyMaintenanceTotal(List.of(unitA, unitB));

            assertEquals(Money.of(30), actual);
        }

        @Test
        void testGetWeeklyMaintenanceTotal_emptyCollectionIsZero() {
            Money actual = getWeeklyMaintenanceTotal(List.of());

            assertEquals(Money.zero(), actual);
        }
    }

    /**
     * tests
     * {@link Accountant#getOverheadTotal(java.util.Collection, CampaignOptions, boolean, LocalDate, int, int,
     * java.util.Map, boolean)}
     */
    @Nested
    class TestGetOverheadTotal {
        CampaignOptions campaignOptions;
        final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        @BeforeEach
        void beforeEach() {
            campaignOptions = new CampaignOptions();
        }

        @Test
        void testGetOverheadTotal_payForOverheadFalseIsZero() {
            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));

            Money actual = getOverheadTotal(List.of(person), campaignOptions, false, TODAY, 0, 0, Map.of(), false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetOverheadTotal_fivePercentOfTheoreticalPayroll() {
            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));

            Money actual = getOverheadTotal(List.of(person), campaignOptions, false, TODAY, 0, 0, Map.of(), true);

            assertEquals(Money.of(50), actual);
        }
    }

    /**
     * tests {@link Accountant#getFoodAndHousingTotal(java.util.Collection, boolean, boolean, double)}
     */
    @Nested
    class TestGetFoodAndHousingTotal {
        Campaign mockCampaign;

        @BeforeEach
        void beforeEach() {
            mockCampaign = mock(Campaign.class);
            when(mockCampaign.getFaction()).thenReturn(new Faction());
        }

        @Test
        void testGetFoodAndHousingTotal_bothDisabledIsZero() {
            Money actual = getFoodAndHousingTotal(List.of(), false, false, 1.0);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetFoodAndHousingTotal_officerFoodAndHousing() {
            Person officer = new Person(mockCampaign);
            officer.setPrimaryRoleDirect(MEKWARRIOR);
            officer.setRank(RWO_MIN + 1);

            Money actual = getFoodAndHousingTotal(List.of(officer, officer, officer), true, true, 1.0);

            int expected = (FOOD_OFFICER + HOUSING_OFFICER) * 3;
            assertEquals(Money.of(expected), actual);
        }

        @Test
        void testGetFoodAndHousingTotal_appliesBarrackCostMultiplier() {
            Person officer = new Person(mockCampaign);
            officer.setPrimaryRoleDirect(MEKWARRIOR);
            officer.setRank(RWO_MIN + 1);

            Money actual = getFoodAndHousingTotal(List.of(officer), true, true, 2.0);

            int expected = (FOOD_OFFICER + HOUSING_OFFICER) * 2;
            assertEquals(Money.of(expected), actual);
        }
    }

    /**
     * tests {@link Accountant#isNonDropShipLargeVessel(Unit)}
     */
    @Nested
    class TestIsNonDropShipLargeVessel {
        @Test
        void testIsNonDropShipLargeVessel_nullUnitIsFalse() {
            assertFalse(Accountant.isNonDropShipLargeVessel(null));
        }

        @Test
        void testIsNonDropShipLargeVessel_largeCraftNonDropShipIsTrue() {
            Unit unit = new Unit();
            Entity mockEntity = mock(Entity.class);
            when(mockEntity.isLargeCraft()).thenReturn(true);
            when(mockEntity.isDropShip()).thenReturn(false);
            unit.setEntity(mockEntity);

            assertTrue(Accountant.isNonDropShipLargeVessel(unit));
        }

        @Test
        void testIsNonDropShipLargeVessel_dropShipIsFalse() {
            Unit unit = new Unit();
            Entity mockEntity = mock(Entity.class);
            when(mockEntity.isLargeCraft()).thenReturn(true);
            when(mockEntity.isDropShip()).thenReturn(true);
            unit.setEntity(mockEntity);

            assertFalse(Accountant.isNonDropShipLargeVessel(unit));
        }

        @Test
        void testIsNonDropShipLargeVessel_notLargeCraftIsFalse() {
            Unit unit = new Unit();
            Entity mockEntity = mock(Entity.class);
            when(mockEntity.isLargeCraft()).thenReturn(false);
            unit.setEntity(mockEntity);

            assertFalse(Accountant.isNonDropShipLargeVessel(unit));
        }
    }

    /**
     * tests
     * {@link Accountant#getForceValue(java.util.Collection, Hangar, Faction, CampaignOptions, boolean, boolean, double,
     * double, double, boolean)}
     */
    @Nested
    class TestGetForceValueStatic {
        Hangar mockHangar;
        CampaignOptions campaignOptions;
        Faction faction;

        @BeforeEach
        void beforeEach() {
            mockHangar = mock(Hangar.class);
            // A mock is used (rather than a real CampaignOptions) because the real setters clamp contract
            // percentages to small real-game maximums (e.g. 5% for combat equipment), which would get in the
            // way of asserting simple round-number totals here.
            campaignOptions = mock(CampaignOptions.class);
            when(campaignOptions.getEquipmentContractPercent()).thenReturn(100.0);
            faction = new Faction();
        }

        @Test
        void testGetForceValue_sumsRegularUnitAtEquipmentContractPercent() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));
            when(unit.isConventionalInfantry()).thenReturn(false);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));

            Money actual = getForceValue(List.of(formation), mockHangar, faction, campaignOptions, false, false, 10,
                  10, 10, false);

            assertEquals(Money.of(1000), actual);
        }

        @Test
        void testGetForceValue_excludesNonStandardFormations() {
            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.SUPPORT);

            Money actual = getForceValue(List.of(formation), mockHangar, faction, campaignOptions, false, false, 10,
                  10, 10, false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetForceValue_excludesInfantryWhenRequested() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.isConventionalInfantry()).thenReturn(true);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));

            Money actual = getForceValue(List.of(formation), mockHangar, faction, campaignOptions, false, true, 10,
                  10, 10, false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetForceValue_emptyFormationsIsZero() {
            Money actual = getForceValue(List.of(), mockHangar, faction, campaignOptions, false, false, 10, 10, 10,
                  false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetForceValue_excludesNonCombatRoleFormations() {
            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.RESERVE);

            Money actual = getForceValue(List.of(formation), mockHangar, faction, campaignOptions, false, false, 10,
                  10, 10, false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetForceValue_warShipIncludedWhenPercentNonZero() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(entity.isWarShip()).thenReturn(true);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));

            Money actual = getForceValue(List.of(formation), mockHangar, faction, campaignOptions, false, false, 10,
                  30, 10, false);

            assertEquals(Money.of(1000), actual);
        }

        @Test
        void testGetForceValue_warShipExcludedWhenPercentZero() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(entity.isWarShip()).thenReturn(true);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));

            Money actual = getForceValue(List.of(formation), mockHangar, faction, campaignOptions, false, false, 10,
                  0, 10, false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetForceValue_jumpShipIncludedWhenPercentNonZero() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(entity.isJumpShip()).thenReturn(true);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));

            Money actual = getForceValue(List.of(formation), mockHangar, faction, campaignOptions, false, false, 10,
                  10, 40, false);

            assertEquals(Money.of(1000), actual);
        }

        @Test
        void testGetForceValue_jumpShipExcludedWhenPercentZero() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(entity.isJumpShip()).thenReturn(true);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));

            Money actual = getForceValue(List.of(formation), mockHangar, faction, campaignOptions, false, false, 10,
                  10, 0, false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetForceValue_appliesDiminishingReturnsWhenThresholdExceeded() {
            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);

            Vector<UUID> unitIds = new Vector<>();
            for (int i = 0; i < 100; i++) {
                UUID unitId = UUID.randomUUID();
                Unit unit = mock(Unit.class);
                Entity entity = mock(Entity.class);
                when(unit.getEntity()).thenReturn(entity);
                when(unit.getBuyCost()).thenReturn(Money.of(1000));
                when(mockHangar.getUnit(unitId)).thenReturn(unit);
                unitIds.add(unitId);
            }
            when(formation.getUnits()).thenReturn(unitIds);

            Money straightSum = getForceValue(List.of(formation), mockHangar, faction, campaignOptions, false, false,
                  10, 10, 10, false);
            Money withDiminishingReturns = getForceValue(List.of(formation), mockHangar, faction, campaignOptions,
                  true, false, 10, 10, 10, false);

            // 100 units comfortably exceeds any faction's diminishing-returns threshold, so the
            // discounted total must come out strictly lower than the undiscounted straight sum.
            assertTrue(withDiminishingReturns.isLessThan(straightSum));
        }
    }

    /**
     * tests {@link Accountant#getTotalEquipmentValue(java.util.Collection, java.util.Collection)}
     */
    @Nested
    class TestGetTotalEquipmentValueStatic {
        @Test
        void testGetTotalEquipmentValue_sumsUnitSellValueAndSpareParts() {
            Unit unit = mock(Unit.class);
            when(unit.getSellValue()).thenReturn(Money.of(1000));

            Part sparePart = mock(Part.class);
            when(sparePart.isSpare()).thenReturn(true);
            when(sparePart.getActualValue()).thenReturn(Money.of(200));

            Money actual = getTotalEquipmentValue(List.of(unit), List.of(sparePart));

            assertEquals(Money.of(1200), actual);
        }

        @Test
        void testGetTotalEquipmentValue_excludesNonSpareParts() {
            Part installedPart = mock(Part.class);
            when(installedPart.isSpare()).thenReturn(false);
            when(installedPart.getActualValue()).thenReturn(Money.of(500));

            Money actual = getTotalEquipmentValue(List.of(), List.of(installedPart));

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetTotalEquipmentValue_noUnitsOrPartsIsZero() {
            Money actual = getTotalEquipmentValue(List.of(), List.of());

            assertEquals(Money.zero(), actual);
        }
    }

    /**
     * tests {@link Accountant#getEquipmentContractValue(CampaignOptions, Unit, boolean)}
     */
    @Nested
    class TestGetEquipmentContractValueStatic {
        CampaignOptions campaignOptions;

        @BeforeEach
        void beforeEach() {
            // A mock is used (rather than a real CampaignOptions) because the real setters clamp contract
            // percentages to small real-game maximums (e.g. 5% for combat equipment, 1% for DropShips), which
            // would get in the way of asserting simple round-number totals here.
            campaignOptions = mock(CampaignOptions.class);
        }

        @Test
        void testGetEquipmentContractValue_regularUnitUsesEquipmentContractPercent() {
            when(campaignOptions.getEquipmentContractPercent()).thenReturn(50.0);

            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));

            Money actual = getEquipmentContractValue(campaignOptions, unit, false);

            assertEquals(Money.of(500), actual);
        }

        @Test
        void testGetEquipmentContractValue_dropShipUsesDropShipContractPercent() {
            when(campaignOptions.getDropShipContractPercent()).thenReturn(20.0);

            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(entity.hasETypeFlag(Entity.ETYPE_DROPSHIP)).thenReturn(true);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));

            Money actual = getEquipmentContractValue(campaignOptions, unit, false);

            assertEquals(Money.of(200), actual);
        }

        @Test
        void testGetEquipmentContractValue_useSaleValueUsesSellValue() {
            when(campaignOptions.getEquipmentContractPercent()).thenReturn(100.0);

            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getSellValue()).thenReturn(Money.of(750));

            Money actual = getEquipmentContractValue(campaignOptions, unit, true);

            assertEquals(Money.of(750), actual);
        }

        @Test
        void testGetEquipmentContractValue_warShipUsesWarShipContractPercent() {
            when(campaignOptions.getWarShipContractPercent()).thenReturn(30.0);

            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(entity.hasETypeFlag(Entity.ETYPE_WARSHIP)).thenReturn(true);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));

            Money actual = getEquipmentContractValue(campaignOptions, unit, false);

            assertEquals(Money.of(300), actual);
        }

        @Test
        void testGetEquipmentContractValue_jumpShipUsesJumpShipContractPercent() {
            when(campaignOptions.getJumpShipContractPercent()).thenReturn(40.0);

            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)).thenReturn(true);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));

            Money actual = getEquipmentContractValue(campaignOptions, unit, false);

            assertEquals(Money.of(400), actual);
        }

        @Test
        void testGetEquipmentContractValue_spaceStationUsesJumpShipContractPercent() {
            when(campaignOptions.getJumpShipContractPercent()).thenReturn(40.0);

            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(entity.hasETypeFlag(Entity.ETYPE_SPACE_STATION)).thenReturn(true);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));

            Money actual = getEquipmentContractValue(campaignOptions, unit, false);

            assertEquals(Money.of(400), actual);
        }
    }

    /**
     * tests
     * {@link Accountant#getContractBase(CampaignOptions, Faction, LocalDate, Hangar, java.util.List, int, int,
     * java.util.Map, java.util.List)}
     */
    @Nested
    class TestGetContractBaseStatic {
        CampaignOptions campaignOptions;
        Faction faction;
        final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        @BeforeEach
        void beforeEach() {
            campaignOptions = new CampaignOptions();
            faction = new Faction();
        }

        @Test
        void testGetContractBase_defaultFallsBackToTheoreticalPayroll() {
            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));

            Hangar mockHangar = mock(Hangar.class);

            Money actual = getContractBase(campaignOptions, faction, TODAY, mockHangar, List.of(person), 0, 0,
                  Map.of(), List.of());

            assertEquals(Money.of(1000), actual);
        }

        @Test
        void testGetContractBase_equipmentContractBaseUsesForceValue() {
            campaignOptions.setEquipmentContractBase(true);
            // Left at its real default (5%) rather than set explicitly - CampaignOptions clamps
            // setEquipmentContractPercent() to MAXIMUM_COMBAT_EQUIPMENT_PERCENT (5.0), so the expected total below
            // is computed against that real-game cap rather than an artificial round number.

            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(2000));
            when(unit.isConventionalInfantry()).thenReturn(false);

            Hangar mockHangar = mock(Hangar.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));

            Money actual = getContractBase(campaignOptions, faction, TODAY, mockHangar, List.of(), 0, 0, Map.of(),
                  List.of(formation));

            assertEquals(Money.of(100), actual);
        }

        @Test
        void testGetContractBase_alternatePaymentModeSkipsTheoreticalPayrollFallback() {
            campaignOptions.setUseAlternatePaymentMode(true);

            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));

            Hangar mockHangar = mock(Hangar.class);

            Money actual = getContractBase(campaignOptions, faction, TODAY, mockHangar, List.of(person), 0, 0,
                  Map.of(), List.of());

            // Alternate payment mode short-circuits before ever falling through to the theoretical-payroll
            // branch, so the salary stubbed above must not show up in the result.
            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetContractBase_alternatePaymentModeUsesAlternatePaymentModelValues() {
            campaignOptions.setUseAlternatePaymentMode(true);

            UUID unitId = UUID.randomUUID();
            // isProtoMek() is the simplest single-condition branch in
            // AlternatePaymentModelValues#getUnitContractValue, making it an easy way to get a
            // predictable, nonzero force value out of the real production logic.
            Entity entity = mock(Entity.class);
            when(entity.isProtoMek()).thenReturn(true);

            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(entity);

            Hangar mockHangar = mock(Hangar.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnitsAsUnits(mockHangar)).thenReturn(List.of(unit));

            Money actual = getContractBase(campaignOptions, faction, TODAY, mockHangar, List.of(), 0, 0, Map.of(),
                  List.of(formation));

            // Real default equipmentContractPercent (5%) applied to the PROTOMEK base value.
            Money expected = AlternatePaymentModelValues.PROTOMEK.getValue().multipliedBy(0.05);
            assertEquals(expected, actual);
        }

        @Test
        void testGetContractBase_alternatePaymentModeHalvesResultWhenUsingSaleValue() {
            campaignOptions.setUseAlternatePaymentMode(true);
            campaignOptions.setEquipmentContractSaleValue(true);

            UUID unitId = UUID.randomUUID();
            Entity entity = mock(Entity.class);
            when(entity.isProtoMek()).thenReturn(true);

            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(entity);

            Hangar mockHangar = mock(Hangar.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnitsAsUnits(mockHangar)).thenReturn(List.of(unit));

            Money actual = getContractBase(campaignOptions, faction, TODAY, mockHangar, List.of(), 0, 0, Map.of(),
                  List.of(formation));

            Money expected = AlternatePaymentModelValues.PROTOMEK.getValue().multipliedBy(0.05).multipliedBy(0.5);
            assertEquals(expected, actual);
        }

        @Test
        void testGetContractBase_usePeacetimeCostCombinesForceValueAndPeacetimeCost() {
            campaignOptions.setUsePeacetimeCost(true);

            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));
            when(unit.getSparePartsCost()).thenReturn(Money.of(10));
            when(unit.getAmmoCost()).thenReturn(Money.of(5));
            when(unit.getFuelCost(anyInt())).thenReturn(Money.zero());

            Hangar mockHangar = mock(Hangar.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(formation.getSubFormations()).thenReturn(new Vector<>());

            Money expectedPeacetimeCost = getSparePartsTotal(List.of(unit)).plus(getFuelTotal(List.of(unit)))
                                                .plus(getAmmoTotal(List.of(unit)));
            Money expectedForceValue = getEquipmentContractValue(campaignOptions, unit, false);
            Money expected = expectedPeacetimeCost.multipliedBy(0.75).plus(expectedForceValue);

            Money actual = getContractBase(campaignOptions, faction, TODAY, mockHangar, List.of(), 0, 0, Map.of(),
                  List.of(formation));

            assertEquals(expected, actual);
        }
    }

    /**
     * tests
     * {@link Accountant#getPayRollSummary(java.util.Collection, CampaignOptions, boolean, LocalDate, int, int,
     * java.util.Map)}
     */
    @Nested
    class TestGetPayRollSummaryStatic {
        CampaignOptions campaignOptions;
        final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        @BeforeEach
        void beforeEach() {
            campaignOptions = new CampaignOptions();
        }

        @Test
        void testGetPayRollSummary_mapsEachPersonToTheirSalary() {
            Person person = mock(Person.class);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(300));

            Map<Person, Money> actual = getPayRollSummary(List.of(person), campaignOptions, false, TODAY, 0, 0,
                  Map.of());

            assertEquals(2, actual.size());
            assertTrue(actual.containsKey(person));
            assertEquals(Money.of(300), actual.get(person));
            assertTrue(actual.containsKey(null));
            assertEquals(Money.zero(), actual.get(null));
        }

        @Test
        void testGetPayRollSummary_emptyPersonnelStillHasPoolKey() {
            Map<Person, Money> actual = getPayRollSummary(List.of(), campaignOptions, false, TODAY, 0, 0, Map.of());

            assertEquals(1, actual.size());
            assertTrue(actual.containsKey(null));
            assertEquals(Money.zero(), actual.get(null));
        }
    }

    /**
     * These tests confirm that the thin instance-method wrappers correctly extract values from the wrapped
     * {@link Campaign} (formations, hangar, campaign options, salary-eligible personnel, etc.) and forward them to
     * their static counterparts, since those counterparts are otherwise only ever exercised directly with hand-built
     * inputs.
     */
    @Nested
    class TestInstanceMethodDelegation {
        Campaign mockCampaign;
        CampaignOptions campaignOptions;
        Hangar mockHangar;
        Accountant accountant;
        final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        @BeforeEach
        void beforeEach() {
            mockCampaign = mock(Campaign.class);
            campaignOptions = new CampaignOptions();
            mockHangar = mock(Hangar.class);
            accountant = new Accountant(mockCampaign);

            when(mockCampaign.getCampaignOptions()).thenReturn(campaignOptions);
            when(mockCampaign.getAllHangar()).thenReturn(mockHangar);
            when(mockCampaign.isClanCampaign()).thenReturn(false);
            when(mockCampaign.getLocalDate()).thenReturn(TODAY);
            when(mockCampaign.getHumanResources()).thenReturn(mock(HumanResources.class));
        }

        @Test
        void testGetMaintenanceCosts_delegatesToStaticTotal() {
            campaignOptions.setPayForMaintain(true);

            Unit unit = mock(Unit.class);
            when(unit.requiresMaintenance()).thenReturn(true);
            when(unit.getTech()).thenReturn(mock(Person.class));
            when(unit.getMaintenanceCost()).thenReturn(Money.of(100));
            when(mockCampaign.getAllUnits()).thenReturn(List.of(unit));

            Money actual = accountant.getMaintenanceCosts();

            assertEquals(Money.of(100), actual);
        }

        @Test
        void testGetMaintenanceCosts_payForMaintainFalseIsZero() {
            campaignOptions.setPayForMaintain(false);

            Unit unit = mock(Unit.class);
            when(unit.requiresMaintenance()).thenReturn(true);
            when(unit.getTech()).thenReturn(mock(Person.class));
            when(unit.getMaintenanceCost()).thenReturn(Money.of(100));
            when(mockCampaign.getAllUnits()).thenReturn(List.of(unit));

            Money actual = accountant.getMaintenanceCosts();

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetWeeklyMaintenanceCosts_delegatesToStaticTotal() {
            Unit unit = mock(Unit.class);
            when(unit.getWeeklyMaintenanceCost()).thenReturn(Money.of(20));
            when(mockCampaign.getAllUnits()).thenReturn(List.of(unit));

            Money actual = accountant.getWeeklyMaintenanceCosts();

            assertEquals(Money.of(20), actual);
        }

        @Test
        void testGetOverheadExpenses_delegatesToStaticTotal() {
            campaignOptions.setPayForOverhead(true);

            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));
            when(mockCampaign.getSalaryEligiblePersonnel()).thenReturn(List.of(person));

            Money actual = accountant.getOverheadExpenses();

            assertEquals(Money.of(50), actual);
        }

        @Test
        void testGetOverheadExpenses_payForOverheadFalseIsZero() {
            campaignOptions.setPayForOverhead(false);

            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));
            when(mockCampaign.getSalaryEligiblePersonnel()).thenReturn(List.of(person));

            Money actual = accountant.getOverheadExpenses();

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetForceValue_instanceDelegatesWithCampaignFormationsAndHangar() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));
            when(unit.isConventionalInfantry()).thenReturn(false);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Formation formation = mock(Formation.class);
            when(formation.getFormationType()).thenReturn(FormationType.STANDARD);
            when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(mockCampaign.getAllFormations()).thenReturn(List.of(formation));

            Faction faction = new Faction();
            when(mockCampaign.getFaction()).thenReturn(faction);

            Money actual = accountant.getForceValue(false, false, 10, 10, 10, false);

            // Real default equipmentContractPercent (5%) applied to the unit's buy cost.
            assertEquals(Money.of(1000).multipliedBy(0.05), actual);
        }

        @Test
        void testGetTotalEquipmentValue_delegatesToStaticTotal() {
            Unit unit = mock(Unit.class);
            when(unit.getSellValue()).thenReturn(Money.of(1000));
            when(mockCampaign.getAllUnits()).thenReturn(List.of(unit));
            when(mockCampaign.getAllParts()).thenReturn(List.of());

            Money actual = accountant.getTotalEquipmentValue();

            assertEquals(Money.of(1000), actual);
        }

        @Test
        void testGetEquipmentContractValue_instanceDelegatesWithCampaignOptions() {
            Unit unit = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.getBuyCost()).thenReturn(Money.of(1000));

            Money actual = accountant.getEquipmentContractValue(unit, false);

            // Real default equipmentContractPercent (5%).
            assertEquals(Money.of(1000).multipliedBy(0.05), actual);
        }

        @Test
        void testGetContractBase_instanceDelegatesWithCampaign() {
            when(mockCampaign.getFaction()).thenReturn(new Faction());

            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1000));
            when(mockCampaign.getSalaryEligiblePersonnel()).thenReturn(List.of(person));

            Money actual = accountant.getContractBase();

            assertEquals(Money.of(1000), actual);
        }
    }
}
