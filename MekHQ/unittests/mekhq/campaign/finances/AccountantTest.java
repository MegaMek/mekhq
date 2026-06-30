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
import static mekhq.campaign.personnel.ranks.Rank.RWO_MIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.common.units.Entity;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AccountantTest {

    private Campaign mockCampaign;
    private CampaignOptions mockCampaignOptions;
    private AbstractLocation mockLocation;
    private List<Unit> emptyUnits;
    private List<Formation> emptyFormations;
    private List<Part> emptyParts;
    private Map<PersonnelRole, Integer> emptyRoleMap;

    @BeforeEach
    void setUp() {
        mockCampaign = mock(Campaign.class);
        mockCampaignOptions = mock(CampaignOptions.class);
        mockLocation = mock(AbstractLocation.class);

        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaign.getCurrentLocation()).thenReturn(mockLocation);

        emptyUnits = Collections.emptyList();
        emptyFormations = Collections.emptyList();
        emptyParts = Collections.emptyList();
        emptyRoleMap = new HashMap<>();
    }

    private Accountant createAccountant(List<Person> personnel) {
        return new Accountant(
              mockCampaign,
              emptyUnits,
              personnel,
              emptyFormations,
              emptyParts,
              0,
              0,
              emptyRoleMap
        );
    }

    private Person createMockPrisoner() {
        Person prisoner = mock(Person.class);
        UUID uniqueId = UUID.randomUUID();
        when(prisoner.getId()).thenReturn(uniqueId);
        when(prisoner.getStatus()).thenReturn(PersonnelStatus.ACTIVE);

        PrisonerStatus prisonerStatus = mock(PrisonerStatus.class);
        when(prisonerStatus.isCurrentPrisoner()).thenReturn(true);
        when(prisoner.getPrisonerStatus()).thenReturn(prisonerStatus);
        return prisoner;
    }

    private Person createMockDependent() {
        Person dependent = mock(Person.class);
        UUID uniqueId = UUID.randomUUID();
        when(dependent.getId()).thenReturn(uniqueId);
        when(dependent.getStatus()).thenReturn(PersonnelStatus.ACTIVE);

        PrisonerStatus prisonerStatus = mock(PrisonerStatus.class);
        when(prisonerStatus.isCurrentPrisoner()).thenReturn(false);
        when(dependent.getPrisonerStatus()).thenReturn(prisonerStatus);
        when(dependent.getPrimaryRole()).thenReturn(DEPENDENT);
        return dependent;
    }

    private Person createMockEnlisted() {
        Person enlisted = mock(Person.class);
        UUID uniqueId = UUID.randomUUID();
        when(enlisted.getId()).thenReturn(uniqueId);
        when(enlisted.getStatus()).thenReturn(PersonnelStatus.ACTIVE);

        PrisonerStatus prisonerStatus = mock(PrisonerStatus.class);
        when(prisonerStatus.isCurrentPrisoner()).thenReturn(false);
        when(enlisted.getPrisonerStatus()).thenReturn(prisonerStatus);
        when(enlisted.getPrimaryRole()).thenReturn(MEKWARRIOR);
        when(enlisted.getRankNumeric()).thenReturn(RWO_MIN - 1);
        return enlisted;
    }

    private Person createMockOfficer() {
        Person officer = mock(Person.class);
        UUID uniqueId = UUID.randomUUID();
        when(officer.getId()).thenReturn(uniqueId);
        when(officer.getStatus()).thenReturn(PersonnelStatus.ACTIVE);

        PrisonerStatus prisonerStatus = mock(PrisonerStatus.class);
        when(prisonerStatus.isCurrentPrisoner()).thenReturn(false);
        when(officer.getPrisonerStatus()).thenReturn(prisonerStatus);
        when(officer.getPrimaryRole()).thenReturn(MEKWARRIOR);
        when(officer.getRankNumeric()).thenReturn(RWO_MIN + 1);
        return officer;
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenFoodAndHousingDisabled() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);
        when(mockLocation.isOnPlanet()).thenReturn(true);

        Accountant accountant = createAccountant(Collections.emptyList());

        assertEquals(Money.zero(), accountant.getMonthlyFoodAndHousingExpenses());
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenNoPersonnel() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockLocation.isOnPlanet()).thenReturn(true);

        Accountant accountant = createAccountant(Collections.emptyList());

        assertEquals(Money.zero(), accountant.getMonthlyFoodAndHousingExpenses());
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyPrisoners() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockLocation.isOnPlanet()).thenReturn(true);

        List<Person> prisoners = List.of(createMockPrisoner(), createMockPrisoner(), createMockPrisoner());
        Accountant accountant = createAccountant(prisoners);

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * prisoners.size();
        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * prisoners.size();
        Money expected = Money.of(expensesFood + expensesHousing);

        assertEquals(expected, accountant.getMonthlyFoodAndHousingExpenses());
    }

    @Test
    void testGetMonthlyHousingExpenses_WhenOnlyPrisoners() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(false);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockLocation.isOnPlanet()).thenReturn(true);

        List<Person> prisoners = List.of(createMockPrisoner(), createMockPrisoner(), createMockPrisoner());
        Accountant accountant = createAccountant(prisoners);

        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * prisoners.size();
        Money expected = Money.of(expensesHousing);

        assertEquals(expected, accountant.getMonthlyFoodAndHousingExpenses());
    }

    @Test
    void testGetMonthlyFoodExpenses_WhenOnlyPrisoners() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(false);
        when(mockLocation.isOnPlanet()).thenReturn(true);

        List<Person> prisoners = List.of(createMockPrisoner(), createMockPrisoner(), createMockPrisoner());
        Accountant accountant = createAccountant(prisoners);

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * prisoners.size();
        Money expected = Money.of(expensesFood);

        assertEquals(expected, accountant.getMonthlyFoodAndHousingExpenses());
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyDependents() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockLocation.isOnPlanet()).thenReturn(true);

        List<Person> dependents = List.of(createMockDependent(), createMockDependent(), createMockDependent());
        Accountant accountant = createAccountant(dependents);

        int expensesFood = FOOD_PRISONER_OR_DEPENDENT * dependents.size();
        int expensesHousing = HOUSING_PRISONER_OR_DEPENDENT * dependents.size();
        Money expected = Money.of(expensesFood + expensesHousing);

        assertEquals(expected, accountant.getMonthlyFoodAndHousingExpenses());
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyEnlisted() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockLocation.isOnPlanet()).thenReturn(true);

        List<Person> enlistedPersonnel = List.of(createMockEnlisted(), createMockEnlisted(), createMockEnlisted());
        Accountant accountant = createAccountant(enlistedPersonnel);

        int expensesFood = FOOD_ENLISTED * enlistedPersonnel.size();
        int expensesHousing = HOUSING_ENLISTED * enlistedPersonnel.size();
        Money expected = Money.of(expensesFood + expensesHousing);

        assertEquals(expected, accountant.getMonthlyFoodAndHousingExpenses());
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_WhenOnlyOfficers() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockLocation.isOnPlanet()).thenReturn(true);

        List<Person> officerPersonnel = List.of(createMockOfficer(), createMockOfficer(), createMockOfficer());
        Accountant accountant = createAccountant(officerPersonnel);

        int expensesFood = FOOD_OFFICER * officerPersonnel.size();
        int expensesHousing = HOUSING_OFFICER * officerPersonnel.size();
        Money expected = Money.of(expensesFood + expensesHousing);

        assertEquals(expected, accountant.getMonthlyFoodAndHousingExpenses());
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_Mixed_InTransit() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockLocation.isOnPlanet()).thenReturn(false);

        List<Person> personnel = List.of(createMockEnlisted(), createMockEnlisted());
        Accountant accountant = createAccountant(personnel);

        int expensesFood = FOOD_ENLISTED * personnel.size();
        Money expected = Money.of(expensesFood);

        assertEquals(expected, accountant.getMonthlyFoodAndHousingExpenses());
    }

    @Test
    void testGetMonthlyFoodAndHousingExpenses_Mixed_ExcludingWarShipCrew() {
        when(mockCampaignOptions.isPayForFood()).thenReturn(true);
        when(mockCampaignOptions.isPayForHousing()).thenReturn(true);
        when(mockLocation.isOnPlanet()).thenReturn(true);

        Unit warShip = mock(Unit.class);
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.isLargeCraft()).thenReturn(true);
        when(mockEntity.isDropShip()).thenReturn(false);
        when(warShip.getEntity()).thenReturn(mockEntity);

        Person crew = mock(Person.class);
        when(crew.getId()).thenReturn(UUID.randomUUID());
        when(crew.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        PrisonerStatus prisonerStatus = mock(PrisonerStatus.class);
        when(crew.getPrisonerStatus()).thenReturn(prisonerStatus);
        when(crew.getPrimaryRole()).thenReturn(PersonnelRole.VESSEL_GUNNER);
        when(crew.getRankNumeric()).thenReturn(RWO_MIN - 1);
        when(crew.getUnit()).thenReturn(warShip);

        List<Person> personnel = List.of(crew);
        Accountant accountant = createAccountant(personnel);

        int expensesFood = FOOD_ENLISTED * personnel.size();
        Money expected = Money.of(expensesFood);

        assertEquals(expected, accountant.getMonthlyFoodAndHousingExpenses());
    }
}
