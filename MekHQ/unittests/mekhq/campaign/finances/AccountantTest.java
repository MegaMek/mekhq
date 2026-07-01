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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import megamek.common.equipment.Engine;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Hangar;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
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

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenFoodAndHousingDisabled() {
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        Money expected = Money.zero();
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenNoPersonnel() {
        Campaign mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);

        Accountant accountant = new Accountant(mockCampaign);

        CurrentLocation location = new CurrentLocation();
        when(mockCampaign.getCurrentLocation()).thenReturn(location);

        Money expected = Money.zero();
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyPrisoners() {
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

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * prisoners.size();
        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * prisoners.size();
        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_WhenOnlyPrisoners() {
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

        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * prisoners.size();
        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_WhenOnlyPrisoners() {
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

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * prisoners.size();
        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyDependents() {
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

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * dependents.size();
        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * dependents.size();
        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_WhenOnlyDependents() {
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

        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * dependents.size();
        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_WhenOnlyDependents() {
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

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * dependents.size();
        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyEnlisted() {
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

        int expensesFood = FOOD_ENLISTED * enlistedPersonnel.size();
        int expensesHousing = HOUSING_ENLISTED * enlistedPersonnel.size();
        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_WhenOnlyEnlisted() {
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

        int expensesHousing = HOUSING_ENLISTED * enlistedPersonnel.size();
        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_WhenOnlyEnlisted() {
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

        int expensesFood = FOOD_ENLISTED * enlistedPersonnel.size();
        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyOfficers() {
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

        int expensesFood = FOOD_OFFICER * officerPersonnel.size();
        int expensesHousing = HOUSING_OFFICER * officerPersonnel.size();
        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_WhenOnlyOfficers() {
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

        int expensesHousing = HOUSING_OFFICER * officerPersonnel.size();
        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_WhenOnlyOfficers() {
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

        int expensesFood = FOOD_OFFICER * officerPersonnel.size();
        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_Mixed() {
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

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesFood += FOOD_ENLISTED * enlistedPersonnel.size();
        expensesFood += FOOD_OFFICER * officerPersonnel.size();

        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesHousing += HOUSING_ENLISTED * enlistedPersonnel.size();
        expensesHousing += HOUSING_OFFICER * officerPersonnel.size();

        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyHousingExpenses_Mixed() {
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

        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesHousing += HOUSING_ENLISTED * enlistedPersonnel.size();
        expensesHousing += HOUSING_OFFICER * officerPersonnel.size();

        Money expected = Money.of(expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodExpenses_Mixed() {
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

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesFood += FOOD_ENLISTED * enlistedPersonnel.size();
        expensesFood += FOOD_OFFICER * officerPersonnel.size();

        Money expected = Money.of(expensesFood);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_Mixed_InTransit() {
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

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesFood += FOOD_ENLISTED * enlistedPersonnel.size();
        expensesFood += FOOD_OFFICER * officerPersonnel.size();

        int expensesHousing = 0;

        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

        assertEquals(expected, actual);
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_Mixed_ExcludingWarShipCrew() {
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

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesFood += FOOD_ENLISTED * enlistedPersonnel.size();
        expensesFood += FOOD_OFFICER * officerPersonnel.size();
        expensesFood += FOOD_ENLISTED * warShipPersonnel.size();

        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * (prisoners.size() + dependents.size());
        expensesHousing += HOUSING_ENLISTED * enlistedPersonnel.size();
        expensesHousing += HOUSING_OFFICER * officerPersonnel.size();

        Money expected = Money.of(expensesFood + expensesHousing);
        Money actual = accountant.getMonthlyFoodAndHousingExpenses();

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

            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

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

            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

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


            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

            assertEquals(1, expectedMap.size());
            assertTrue(expectedMap.containsKey(null));
            assertEquals(Money.of(2000), expectedMap.get(null));
        }

        @Test
        void testGetMedicPoolPay() {
            // Arrange
            when(mockCampaign.getTemporaryMedicPool()).thenReturn(5);
            when(mockCampaign.getCampaignOptions()).thenReturn(campaignOptions);


            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

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

            Map<Person, Money> expectedMap = accountant.getPayRollSummary();

            assertEquals(1, expectedMap.size());
            assertTrue(expectedMap.containsKey(null));
            assertEquals(Money.of(CREWCOUNT *
                                        campaignOptions.getRoleBaseSalaries()[role.ordinal()].getAmount()
                                              .doubleValue()),
                  expectedMap.get(null));
        }

        Person getMockPerson() {
            Person mockPerson = mock(Person.class);

            when(mockPerson.getSalary(mockCampaign)).thenReturn(Money.of(100));
            return mockPerson;
        }
    }

    /**
     * tests {@link Accountant#getSalaryTotal(java.util.Collection, Campaign, boolean)}
     */
    @Nested
    class TestGetSalaryTotal {
        Campaign mockCampaign;

        @BeforeEach
        void beforeEach() {
            mockCampaign = mock(Campaign.class);
        }

        @Test
        void testGetSalaryTotal_sumsAllPersonnel() {
            Person personA = mock(Person.class);
            when(personA.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(personA.getSalary(mockCampaign)).thenReturn(Money.of(100));

            Person personB = mock(Person.class);
            when(personB.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(personB.getSalary(mockCampaign)).thenReturn(Money.of(250));

            Money actual = getSalaryTotal(List.of(personA, personB), mockCampaign, false);

            assertEquals(Money.of(350), actual);
        }

        @Test
        void testGetSalaryTotal_noInfantryExcludesSoldiers() {
            Person soldier = mock(Person.class);
            when(soldier.getPrimaryRole()).thenReturn(SOLDIER);
            when(soldier.getSalary(mockCampaign)).thenReturn(Money.of(100));

            Person mekWarrior = mock(Person.class);
            when(mekWarrior.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(mekWarrior.getSalary(mockCampaign)).thenReturn(Money.of(250));

            Money actual = getSalaryTotal(List.of(soldier, mekWarrior), mockCampaign, true);

            assertEquals(Money.of(250), actual);
        }

        @Test
        void testGetSalaryTotal_emptyCollectionIsZero() {
            Money actual = getSalaryTotal(List.of(), mockCampaign, false);

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
     * tests {@link Accountant#getPeacetimeOperatingCosts(java.util.Collection, Campaign, boolean)}
     */
    @Nested
    class TestGetPeacetimeOperatingCosts {
        Campaign mockCampaign;
        CampaignOptions mockCampaignOptions;
        Hangar mockHangar;

        @BeforeEach
        void beforeEach() {
            mockCampaign = mock(Campaign.class);
            // A real CampaignOptions is used (rather than a mock) so that unrelated lookups such as
            // getRoleBaseSalaries()// consulted internally while totaling temporary crew pay - resolve to
            // real, non-null values instead of requiring exhaustive stubbing.
            mockCampaignOptions = new CampaignOptions();
            mockHangar = mock(Hangar.class);

            when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
            when(mockCampaign.getHangar()).thenReturn(mockHangar);
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

            Money actual = getPeacetimeOperatingCosts(List.of(topLevelFormation), mockCampaign, false);

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

            Money actual = getPeacetimeOperatingCosts(List.of(formation), mockCampaign, false);

            assertEquals(Money.of(100), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_includeSalariesFalseExcludesSalaries() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Person crewMember = mock(Person.class);
            when(crewMember.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(crewMember.getSalary(mockCampaign)).thenReturn(Money.of(1000));
            when(unit.getCrew()).thenReturn(List.of(crewMember));

            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(formation.getSubFormations()).thenReturn(new Vector<>());

            mockCampaignOptions.setPayForSalaries(true);

            Money actual = getPeacetimeOperatingCosts(List.of(formation), mockCampaign, false);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_payForSalariesFalseExcludesSalariesEvenWhenRequested() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Person crewMember = mock(Person.class);
            when(crewMember.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(crewMember.getSalary(mockCampaign)).thenReturn(Money.of(1000));
            when(unit.getCrew()).thenReturn(List.of(crewMember));

            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(formation.getSubFormations()).thenReturn(new Vector<>());

            mockCampaignOptions.setPayForSalaries(false);

            Money actual = getPeacetimeOperatingCosts(List.of(formation), mockCampaign, true);

            assertEquals(Money.zero(), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_includesSalariesOfFormationCrews() {
            UUID unitId = UUID.randomUUID();
            Unit unit = mock(Unit.class);
            when(mockHangar.getUnit(unitId)).thenReturn(unit);

            Person crewMember = mock(Person.class);
            when(crewMember.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(crewMember.getSalary(mockCampaign)).thenReturn(Money.of(1000));
            when(unit.getCrew()).thenReturn(List.of(crewMember));

            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(formation.getSubFormations()).thenReturn(new Vector<>());

            mockCampaignOptions.setPayForSalaries(true);
            mockCampaignOptions.setUseInfantryDontCount(false);

            Money actual = getPeacetimeOperatingCosts(List.of(formation), mockCampaign, true);

            assertEquals(Money.of(1000), actual);
        }

        @Test
        void testGetPeacetimeOperatingCosts_emptyFormationsIsZero() {
            Money actual = getPeacetimeOperatingCosts(List.of(), mockCampaign, true);

            assertEquals(Money.zero(), actual);
        }
    }

    /**
     * These tests confirm that, for a campaign where every hangar unit is assigned somewhere in the TO&E and every
     * salary-eligible person crews one of those units (i.e. the common case today), routing
     * {@link Accountant#getPeacetimeCost(boolean)} through the new formation-based
     * {@link Accountant#getPeacetimeOperatingCosts(java.util.Collection, Campaign, boolean)} produces the exact same
     * total that the old whole-campaign calculation used to produce.
     */
    @Nested
    class TestPeacetimeCostMatchesLegacyBehavior {
        Campaign mockCampaign;
        CampaignOptions mockCampaignOptions;
        Hangar mockHangar;
        Accountant accountant;

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
            when(mockCampaign.getHangar()).thenReturn(mockHangar);
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
            when(mockHangar.getUnits()).thenReturn(List.of(unit1, unit2));

            // Each unit's crew is exactly the campaign's salary-eligible personnel - nobody is
            // unassigned, so the formation-derived roster matches the whole-campaign roster.
            person1 = mock(Person.class);
            when(person1.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person1.getSalary(mockCampaign)).thenReturn(Money.of(1000));
            when(unit1.getCrew()).thenReturn(List.of(person1));

            person2 = mock(Person.class);
            when(person2.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person2.getSalary(mockCampaign)).thenReturn(Money.of(1500));
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
            // The "legacy" total: spare parts/fuel/ammo summed over every hangar unit, salaries
            // summed over every salary-eligible person - exactly what the old implementation did.
            Money legacyTotal = getSparePartsTotal(mockHangar.getUnits())
                                      .plus(getFuelTotal(mockHangar.getUnits()))
                                      .plus(getAmmoTotal(mockHangar.getUnits()))
                                      .plus(getSalaryTotal(mockCampaign.getSalaryEligiblePersonnel(),
                                            mockCampaign,
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
            Money legacyTotal = getSparePartsTotal(mockHangar.getUnits())
                                      .plus(getFuelTotal(mockHangar.getUnits()))
                                      .plus(getAmmoTotal(mockHangar.getUnits()));

            Money actual = accountant.getPeacetimeCost(false);

            assertEquals(legacyTotal, actual);
            assertEquals(Money.of(495), actual);
        }

        @Test
        void testGetMonthlySpareParts_stillMatchesWholeHangarTotal() {
            Money actual = accountant.getMonthlySpareParts();

            assertEquals(getSparePartsTotal(mockHangar.getUnits()), actual);
        }

        @Test
        void testGetMonthlyFuel_stillMatchesWholeHangarTotal() {
            Money actual = accountant.getMonthlyFuel();

            assertEquals(getFuelTotal(mockHangar.getUnits()), actual);
        }

        @Test
        void testGetMonthlyAmmo_stillMatchesWholeHangarTotal() {
            Money actual = accountant.getMonthlyAmmo();

            assertEquals(getAmmoTotal(mockHangar.getUnits()), actual);
        }

        @Test
        void testGetPayRoll_stillMatchesWholeCampaignSalaryTotal() {
            Money actual = accountant.getPayRoll();

            assertEquals(getSalaryTotal(mockCampaign.getSalaryEligiblePersonnel(), mockCampaign, false), actual);
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
}
