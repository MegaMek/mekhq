/*
 * UnitPersonTest.java
 *
 * Copyright (C) 2020 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

public class UnitPersonTest {
    @Test
    public void testGetTechReturnsTech() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);

        // Units do not start with a tech
        assertNull(unit.getTech());

        UUID id = UUID.randomUUID();
        Person mockTech = mock(Person.class);
        when(mockTech.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockTech);

        // Set the tech
        unit.setTech(mockTech);

        // Ensure we were added to the unit
        verify(mockTech, times(1)).addTechUnit(eq(unit));

        // Ensure when getting the tech that it is the same tech
        assertEquals(mockTech, unit.getTech());
    }

    @Test
    public void testGetTechReturnsEngineerIfPresent() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);

        // Units do not start with a tech
        assertNull(unit.getTech());

        UUID id = UUID.randomUUID();
        Person mockTech = mock(Person.class);
        when(mockTech.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockTech);

        // Set the tech
        unit.setTech(mockTech);

        // Ensure we were added to the unit
        verify(mockTech, times(1)).addTechUnit(eq(unit));

        // Add an engineer to the unit
        Person mockEngineer = mock(Person.class);
        when(mockEngineer.getId()).thenReturn(UUID.randomUUID());

        when(unit.getEngineer()).thenReturn(mockEngineer);

        // Ensure it is the engineer this time and not the tech
        assertEquals(mockEngineer, unit.getTech());
    }

    @Test
    public void testNoTechRemoveTech() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);

        // Units do not start with a tech
        assertNull(unit.getTech());

        // Remove the tech
        unit.removeTech();

        assertNull(unit.getTech());
    }

    @Test
    public void testRemoveTech() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);

        UUID id = UUID.randomUUID();
        Person mockTech = mock(Person.class);
        when(mockTech.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockTech);

        // Set the tech
        unit.setTech(mockTech);

        // Ensure we were added to the unit
        verify(mockTech, times(1)).addTechUnit(eq(unit));

        // Remove the tech
        unit.removeTech();

        // Ensure we were removed from the unit
        verify(mockTech, times(1)).removeTechUnit(eq(unit));
    }

    @Test
    public void testUnitIsUnmaintained() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);

        // Does not require maintenance?
        when(unit.requiresMaintenance()).thenReturn(false);

        // ...not 'unmaintained'
        assertFalse(unit.isUnmaintained());

        // But if the unit does require maintenance...
        when(unit.requiresMaintenance()).thenReturn(true);

        // ...then if there is no tech...
        assertNull(unit.getTech());

        // ...it is unmaintained.
        assertTrue(unit.isUnmaintained());

        // And thus by assigning a tech...
        UUID id = UUID.randomUUID();
        Person mockTech = mock(Person.class);
        when(mockTech.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockTech);
        unit.setTech(mockTech);

        // ...then it is no longer unmaintained.
        assertFalse(unit.isUnmaintained());
    }

    @Test
    public void testUnitCompleteActivationRemovesTech() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);

        UUID id = UUID.randomUUID();
        Person mockTech = mock(Person.class);
        when(mockTech.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockTech);

        // Set the tech
        unit.setTech(mockTech);

        // Complete activation of a mothballed unit
        unit.completeActivation();

        // Ensure we were removed from the unit after activation
        verify(mockTech, times(1)).removeTechUnit(eq(unit));
    }

    @Test
    public void testUnitIsUnmanned() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);

        // If a unit has no commander...
        when(unit.getCommander()).thenReturn(null);

        // ...then it is unmanned.
        assertTrue(unit.isUnmanned());

        // But if the unit has a commander...
        when(unit.getCommander()).thenReturn(mock(Person.class));

        // ...then it is manned.
        assertFalse(unit.isUnmanned());
    }

    @Test
    public void testDriver() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);
        doNothing().when(unit).resetPilotAndEntity();

        // Units do not start with a driver
        assertTrue(unit.getDrivers().isEmpty());

        // Create the driver
        UUID id = UUID.randomUUID();
        Person mockDriver = mock(Person.class);
        when(mockDriver.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockDriver);

        // This person is NOT a driver (yet)
        assertFalse(unit.isDriver(mockDriver));

        // Add the driver
        unit.addDriver(mockDriver);

        // Ensure we were added to the unit
        verify(mockDriver, times(1)).setUnit(eq(unit));
        verify(unit, times(1)).resetPilotAndEntity();

        // Ensure when getting the driver that it is the same driver
        List<Person> drivers = unit.getDrivers();
        assertTrue(drivers.contains(mockDriver));
        assertTrue(unit.isDriver(mockDriver));

        // Make sure we're part of the crew!
        List<Person> crew = unit.getCrew();
        assertFalse(crew.isEmpty());
        assertEquals(1, crew.size());
        assertTrue(crew.contains(mockDriver));

        Person randomPerson = mock(Person.class);
        when(randomPerson.getId()).thenReturn(UUID.randomUUID());

        // Ensure some rando isn't our driver
        assertFalse(unit.isDriver(randomPerson));

        // Now remove the driver
        unit.remove(mockDriver, false);

        // Make sure we are removed from the person
        verify(mockDriver, times(1)).setUnit(eq(null));
        verify(unit, times(2)).resetPilotAndEntity();

        // Make sure we were removed from the unit
        assertTrue(unit.getDrivers().isEmpty());
        assertFalse(unit.isDriver(mockDriver));
        assertTrue(unit.getCrew().isEmpty());
    }

    @Test
    public void testGunner() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);
        doNothing().when(unit).resetPilotAndEntity();

        // Units do not start with a gunner
        assertTrue(unit.getGunners().isEmpty());

        // Create the gunner
        UUID id = UUID.randomUUID();
        Person mockGunner = mock(Person.class);
        when(mockGunner.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockGunner);

        // This person is NOT a gunner (yet)
        assertFalse(unit.isGunner(mockGunner));

        // Add the gunner
        unit.addGunner(mockGunner);

        // Ensure we were added to the unit
        verify(mockGunner, times(1)).setUnit(eq(unit));
        verify(unit, times(1)).resetPilotAndEntity();

        // Ensure when getting the gunner that it is the same gunner
        List<Person> gunners = unit.getGunners();
        assertTrue(gunners.contains(mockGunner));
        assertTrue(unit.isGunner(mockGunner));

        // Make sure we're part of the crew!
        List<Person> crew = unit.getCrew();
        assertFalse(crew.isEmpty());
        assertEquals(1, crew.size());
        assertTrue(crew.contains(mockGunner));

        Person randomPerson = mock(Person.class);
        when(randomPerson.getId()).thenReturn(UUID.randomUUID());

        // Ensure some rando isn't our gunner
        assertFalse(unit.isGunner(randomPerson));

        // Now remove the gunner
        unit.remove(mockGunner, false);

        // Make sure we are removed from the person
        verify(mockGunner, times(1)).setUnit(eq(null));
        verify(unit, times(2)).resetPilotAndEntity();

        // Make sure we were removed from the unit
        assertTrue(unit.getGunners().isEmpty());
        assertFalse(unit.isGunner(mockGunner));
        assertTrue(unit.getCrew().isEmpty());
    }

    @Test
    public void testVesselCrew() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);
        doNothing().when(unit).resetPilotAndEntity();

        // Units do not start with a vessel crew
        assertTrue(unit.getVesselCrew().isEmpty());

        // Create the vessel crew
        UUID id = UUID.randomUUID();
        Person mockVesselCrew = mock(Person.class);
        when(mockVesselCrew.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockVesselCrew);

        // Add the vessel crew
        unit.addVesselCrew(mockVesselCrew);

        // Ensure we were added to the unit
        verify(mockVesselCrew, times(1)).setUnit(eq(unit));
        verify(unit, times(1)).resetPilotAndEntity();

        // Ensure when getting the vessel crew that it is the same vessel crew
        List<Person> vesselCrew = unit.getVesselCrew();
        assertTrue(vesselCrew.contains(mockVesselCrew));

        // Make sure we're part of the crew!
        List<Person> crew = unit.getCrew();
        assertFalse(crew.isEmpty());
        assertEquals(1, crew.size());
        assertTrue(crew.contains(mockVesselCrew));

        // Now remove the vessel crew
        unit.remove(mockVesselCrew, false);

        // Make sure we are removed from the person
        verify(mockVesselCrew, times(1)).setUnit(eq(null));
        verify(unit, times(2)).resetPilotAndEntity();

        // Make sure we were removed from the unit
        assertTrue(unit.getVesselCrew().isEmpty());
        assertTrue(unit.getCrew().isEmpty());
    }

    @Test
    public void testTechOfficer() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);
        doNothing().when(unit).resetPilotAndEntity();

        // Units do not start with a tech officer
        assertNull(unit.getTechOfficer());

        // Create the tech officer
        UUID id = UUID.randomUUID();
        Person mockTechOfficer = mock(Person.class);
        when(mockTechOfficer.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockTechOfficer);

        // This person is NOT a tech officer (yet)
        assertFalse(unit.isTechOfficer(mockTechOfficer));

        // Set the tech officer
        unit.setTechOfficer(mockTechOfficer);

        // Ensure we were added to the unit
        verify(mockTechOfficer, times(1)).setUnit(eq(unit));
        verify(unit, times(1)).resetPilotAndEntity();

        // Ensure when getting the tech officer that it is the same tech officer
        assertEquals(mockTechOfficer, unit.getTechOfficer());
        assertTrue(unit.isTechOfficer(mockTechOfficer));

        // Make sure we're part of the crew!
        List<Person> crew = unit.getCrew();
        assertFalse(crew.isEmpty());
        assertEquals(1, crew.size());
        assertTrue(crew.contains(mockTechOfficer));

        Person randomPerson = mock(Person.class);
        when(randomPerson.getId()).thenReturn(UUID.randomUUID());

        // Ensure some rando isn't our tech officer
        assertFalse(unit.isTechOfficer(randomPerson));

        // Now remove the tech officer
        unit.remove(mockTechOfficer, false);

        // Make sure we are removed from the person
        verify(mockTechOfficer, times(1)).setUnit(eq(null));
        verify(unit, times(2)).resetPilotAndEntity();

        // Make sure we were removed from the unit
        assertNull(unit.getTechOfficer());
        assertFalse(unit.isTechOfficer(mockTechOfficer));
        assertTrue(unit.getCrew().isEmpty());
    }

    @Test
    public void testNavigator() {
        Entity mockEntity = mock(Entity.class);
        Campaign mockCampaign = mock(Campaign.class);
        Unit unit = spy(new Unit(mockEntity, mockCampaign));
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);
        doNothing().when(unit).resetPilotAndEntity();

        // Units do not start with a tech
        assertNull(unit.getTech());

        // Create the navigator
        UUID id = UUID.randomUUID();
        Person mockNavigator = mock(Person.class);
        when(mockNavigator.getId()).thenReturn(id);
        when(mockCampaign.getPerson(eq(id))).thenReturn(mockNavigator);

        // This person is NOT a navigator (yet)
        assertFalse(unit.isNavigator(mockNavigator));

        // Set the navigator
        unit.setNavigator(mockNavigator);

        // Ensure we were added to the unit
        verify(mockNavigator, times(1)).setUnit(eq(unit));
        verify(unit, times(1)).resetPilotAndEntity();

        // Make sure we're part of the crew!
        List<Person> crew = unit.getCrew();
        assertFalse(crew.isEmpty());
        assertEquals(1, crew.size());
        assertTrue(crew.contains(mockNavigator));

        // Ensure when getting the tech that it is the same tech
        assertEquals(mockNavigator, unit.getNavigator());
        assertTrue(unit.isNavigator(mockNavigator));

        Person randomPerson = mock(Person.class);
        when(randomPerson.getId()).thenReturn(UUID.randomUUID());

        // Ensure some rando isn't our navigator
        assertFalse(unit.isNavigator(randomPerson));

        // Now remove the navigator
        unit.remove(mockNavigator, false);

        // Make sure we are removed from the person
        verify(mockNavigator, times(1)).setUnit(eq(null));
        verify(unit, times(2)).resetPilotAndEntity();

        // Make sure we were removed from the unit
        assertNull(unit.getNavigator());
        assertFalse(unit.isNavigator(mockNavigator));
        assertTrue(unit.getCrew().isEmpty());
    }
}
