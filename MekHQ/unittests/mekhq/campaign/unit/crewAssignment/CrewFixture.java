/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.crewAssignment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.unit.Unit;

/**
 * Mock people and units for the crew assignment tests. Each {@link CrewUnit} keeps real crew lists behind its mock, so
 * assigning someone through the {@link Unit} methods changes what the unit reports, and each person's unit is tracked
 * here so {@link Person#getUnit()} follows those changes. Every mock is at the same (empty) location.
 */
class CrewFixture {
    private final Map<Person, Unit> unitOf = new HashMap<>();

    /** A unit's crew lists, behind a mock {@link Unit}. */
    final class CrewUnit {
        final Unit unit = mock(Unit.class);
        final List<Person> drivers = new ArrayList<>();
        final Set<Person> gunners = new LinkedHashSet<>();
        final List<Person> vesselCrew = new ArrayList<>();
        Person navigator;
        Person techOfficer;
        /** Whoever is named here is refused by every add method, as if the unit turned them away. */
        Person refuse;

        private CrewUnit(Entity entity, String name, int drivers, int gunners, int crew, boolean soloPilot,
              boolean soldiers) {
            when(unit.getId()).thenReturn(UUID.randomUUID());
            when(unit.getName()).thenReturn(name);
            when(unit.getEntity()).thenReturn(entity);
            when(unit.isAvailable()).thenReturn(true);
            when(unit.getTotalDriverNeeds()).thenReturn(drivers);
            when(unit.getTotalGunnerNeeds()).thenReturn(gunners);
            when(unit.getTotalCrewNeeds()).thenReturn(crew);
            when(unit.usesSoloPilot()).thenReturn(soloPilot);
            when(unit.usesSoldiers()).thenReturn(soldiers);
            when(unit.getDrivers()).thenAnswer(invocation -> List.copyOf(this.drivers));
            when(unit.getGunners()).thenAnswer(invocation -> Set.copyOf(this.gunners));
            when(unit.getVesselCrew()).thenAnswer(invocation -> List.copyOf(vesselCrew));
            when(unit.getNavigator()).thenAnswer(invocation -> navigator);
            when(unit.getTechOfficer()).thenAnswer(invocation -> techOfficer);

            doAnswer(invocation -> {
                Person person = invocation.getArgument(0);
                if (person != refuse) {
                    this.drivers.add(person);
                    if (soloPilot || soldiers) {
                        this.gunners.add(person);
                    }
                    unitOf.put(person, unit);
                }
                return null;
            }).when(unit).addPilotOrSoldier(any(), any(), anyBoolean());
            doAnswer(invocation -> add(invocation.getArgument(0), this.drivers))
                  .when(unit).addDriver(any(), anyBoolean());
            doAnswer(invocation -> add(invocation.getArgument(0), this.gunners))
                  .when(unit).addGunner(any(), anyBoolean());
            doAnswer(invocation -> add(invocation.getArgument(0), vesselCrew))
                  .when(unit).addVesselCrew(any(), anyBoolean());
            doAnswer(invocation -> {
                navigator = invocation.getArgument(0);
                unitOf.put(navigator, unit);
                return null;
            }).when(unit).setNavigator(any(), anyBoolean());
            doAnswer(invocation -> {
                techOfficer = invocation.getArgument(0);
                unitOf.put(techOfficer, unit);
                return null;
            }).when(unit).setTechOfficer(any(), anyBoolean());
            doAnswer(invocation -> {
                Person person = invocation.getArgument(0);
                this.drivers.remove(person);
                this.gunners.remove(person);
                vesselCrew.remove(person);
                if (person == navigator) {
                    navigator = null;
                }
                if (person == techOfficer) {
                    techOfficer = null;
                }
                if (unitOf.get(person) == unit) {
                    unitOf.remove(person);
                }
                return null;
            }).when(unit).remove(any(), anyBoolean());
        }

        private Object add(Person person, java.util.Collection<Person> seat) {
            if (person != refuse) {
                seat.add(person);
                unitOf.put(person, unit);
            }
            return null;
        }

        /** Puts someone in a seat directly, as if they were already crewing the unit before the test. */
        CrewUnit with(Person person, SeatType type) {
            switch (type) {
                case PILOT, SOLDIER -> {
                    drivers.add(person);
                    gunners.add(person);
                }
                case DRIVER -> drivers.add(person);
                case GUNNER -> gunners.add(person);
                case CREW -> vesselCrew.add(person);
                case NAVIGATOR -> navigator = person;
                case TECH_OFFICER, CONSOLE_COMMANDER -> techOfficer = person;
            }
            unitOf.put(person, unit);
            return this;
        }
    }

    /** A solo-pilot unit, such as a 'Mek or aerospace fighter, with one pilot seat. */
    CrewUnit soloPilotUnit(Entity entity, String name) {
        return new CrewUnit(entity, name, 1, 1, 0, true, false);
    }

    /** A vehicle with one driver, the given number of gunners and no other crew. */
    CrewUnit vehicle(Entity entity, String name, int gunners) {
        return new CrewUnit(entity, name, 1, gunners, 0, false, false);
    }

    /** A vessel with the given pilot, gunner and crew seats. */
    CrewUnit vessel(Entity entity, String name, int pilots, int gunners, int crew) {
        return new CrewUnit(entity, name, pilots, gunners, crew, false, false);
    }

    /** An infantry or battle armor unit with the given number of soldier seats. */
    CrewUnit infantry(Entity entity, String name, boolean conventional, int soldiers) {
        CrewUnit crewUnit = new CrewUnit(entity, name, soldiers, soldiers, 0, false, true);
        when(crewUnit.unit.isConventionalInfantry()).thenReturn(conventional);
        return crewUnit;
    }

    /** An active, employed, free person with the given primary role and no secondary role. */
    Person person(String name, PersonnelRole primaryRole) {
        return person(name, primaryRole, PersonnelRole.NONE);
    }

    /** An active, employed, free person with the given roles. */
    Person person(String name, PersonnelRole primaryRole, PersonnelRole secondaryRole) {
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        when(person.getFullName()).thenReturn(name);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isEmployed()).thenReturn(true);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getPrimaryRole()).thenReturn(primaryRole);
        when(person.getSecondaryRole()).thenReturn(secondaryRole);
        when(person.hasRole(any())).thenCallRealMethod();
        when(person.getUnit()).thenAnswer(invocation -> unitOf.get(person));
        when(person.getSkillLevel(any(), anyBoolean(), any(), anyBoolean(), anyBoolean()))
              .thenReturn(SkillLevel.REGULAR);
        return person;
    }
}
