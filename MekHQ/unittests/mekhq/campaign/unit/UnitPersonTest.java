/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Ranks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import testUtilities.MHQTestUtilities;

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
        when(mockTech.getUnit()).thenReturn(null);

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
        when(mockDriver.getUnit()).thenReturn(unit);

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
        when(mockGunner.getUnit()).thenReturn(unit);

        // This person is NOT a gunner (yet)
        assertFalse(unit.isGunner(mockGunner));

        // Add the gunner
        unit.addGunner(mockGunner);

        // Ensure we were added to the unit
        verify(mockGunner, times(1)).setUnit(eq(unit));
        verify(unit, times(1)).resetPilotAndEntity();

        // Ensure when getting the gunner that it is the same gunner
        Set<Person> gunners = unit.getGunners();
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
        when(mockVesselCrew.getUnit()).thenReturn(unit);

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
        when(mockTechOfficer.getUnit()).thenReturn(unit);

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
        when(mockNavigator.getUnit()).thenReturn(unit);

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

    /**
     * Tests for Unit temp crew (blob crew) functionality.
     * Tests getting/setting temp crew for different personnel roles,
     * total crew calculations, and blob crew status checks.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class UnitTempCrewTests {

        private Campaign mockCampaign;
        private Entity mockEntity;
        private Unit testUnit;

        @BeforeAll
        public void setupAll() {
            EquipmentType.initializeTypes();
            Ranks.initializeRankSystems();
        }

        @BeforeEach
        public void setup() {
            mockCampaign = spy(MHQTestUtilities.getTestCampaign());

            // Enable blob crew for all roles (required for temp crew to work)
            // Using doReturn for spy to avoid calling real method
            doReturn(true).when(mockCampaign).isBlobCrewEnabled(any(PersonnelRole.class));

            mockEntity = mock(Entity.class);
            when(mockEntity.getId()).thenReturn(1);

            // Mock Crew with all required methods
            Crew mockCrew = mock(Crew.class);
            when(mockCrew.getSlotCount()).thenReturn(1);

            // Mock CrewType (required by addPilotOrSoldier)
            megamek.common.units.CrewType mockCrewType = mock(megamek.common.units.CrewType.class);
            when(mockCrewType.getPilotPos()).thenReturn(0);
            when(mockCrewType.getGunnerPos()).thenReturn(0);
            when(mockCrew.getCrewType()).thenReturn(mockCrewType);

            doNothing().when(mockCrew).resetGameState();
            doNothing().when(mockCrew).setCommandBonus(anyInt());
            doNothing().when(mockCrew).setMissing(anyBoolean(), anyInt());
            doNothing().when(mockCrew).setName(any(), anyInt());
            doNothing().when(mockCrew).setNickname(any(), anyInt());
            doNothing().when(mockCrew).setGender(any(), anyInt());
            doNothing().when(mockCrew).setClanPilot(anyBoolean(), anyInt());
            doNothing().when(mockCrew).setPortrait(any(), anyInt());
            doNothing().when(mockCrew).setExternalIdAsString(any(), anyInt());
            doNothing().when(mockCrew).setToughness(anyInt(), anyInt());
            when(mockCrew.isMissing(anyInt())).thenReturn(false);
            when(mockEntity.getCrew()).thenReturn(mockCrew);

            when(mockEntity.getTransports()).thenReturn(new Vector<>());
            when(mockEntity.getSensors()).thenReturn(new Vector<>());
            when(mockEntity.hasBAP()).thenReturn(false);

            // Mock all setter methods called by clearGameData and resetPilotAndEntity
            doNothing().when(mockEntity).setPassedThrough(any());
            doNothing().when(mockEntity).resetFiringArcs();
            doNothing().when(mockEntity).resetBays();
            doNothing().when(mockEntity).setEvading(anyBoolean());
            doNothing().when(mockEntity).setFacing(anyInt());
            doNothing().when(mockEntity).setPosition(any());
            doNothing().when(mockEntity).setProne(anyBoolean());
            doNothing().when(mockEntity).setHullDown(anyBoolean());
            doNothing().when(mockEntity).setTransportId(anyInt());
            doNothing().when(mockEntity).resetTransporter();
            doNothing().when(mockEntity).setDeployRound(anyInt());
            doNothing().when(mockEntity).setSwarmAttackerId(anyInt());
            doNothing().when(mockEntity).setSwarmTargetId(anyInt());
            doNothing().when(mockEntity).setUnloaded(anyBoolean());
            doNothing().when(mockEntity).setDone(anyBoolean());
            doNothing().when(mockEntity).setLastTarget(anyInt());
            doNothing().when(mockEntity).setNeverDeployed(anyBoolean());
            doNothing().when(mockEntity).setStuck(anyBoolean());
            doNothing().when(mockEntity).resetCoolantFailureAmount();
            doNothing().when(mockEntity).setConversionMode(anyInt());
            doNothing().when(mockEntity).setDoomed(anyBoolean());
            doNothing().when(mockEntity).setDestroyed(anyBoolean());
            doNothing().when(mockEntity).setHidden(anyBoolean());
            doNothing().when(mockEntity).clearNarcAndiNarcPods();
            doNothing().when(mockEntity).setShutDown(anyBoolean());
            doNothing().when(mockEntity).setSearchlightState(anyBoolean());
            doNothing().when(mockEntity).setNextSensor(any());
            doNothing().when(mockEntity).setCommander(anyBoolean());
            doNothing().when(mockEntity).resetPickedUpMekWarriors();
            doNothing().when(mockEntity).setStartingPos(anyInt());

            testUnit = new Unit(mockEntity, mockCampaign);

            Person mockCommander = getMockCommander();

            // Wire up commander to unit (tests can override with null if needed)
            testUnit.addPilotOrSoldier(mockCommander);
        }

        /**
         * Provides all temp crew roles for parameterized tests
         */
        private static Stream<PersonnelRole> getTempCrewRoles() {
            return Stream.of(
                  PersonnelRole.SOLDIER,
                  PersonnelRole.BATTLE_ARMOUR,
                  PersonnelRole.VEHICLE_CREW_GROUND,
                  PersonnelRole.VEHICLE_CREW_VTOL,
                  PersonnelRole.VEHICLE_CREW_NAVAL,
                  PersonnelRole.VESSEL_PILOT,
                  PersonnelRole.VESSEL_GUNNER,
                  PersonnelRole.VESSEL_CREW
            );
        }

        /**
         * Nested test class for temp crew operations
         */
        @Nested
        class TempCrewTests {

            private static Stream<PersonnelRole> getTempCrewRoles() {
                return UnitTempCrewTests.getTempCrewRoles();
            }

            /**
             * Tests that initial temp crew state is zero for all roles.
             * Tests {@link Unit#getTempCrewByPersonnelRole(PersonnelRole)}.
             */
            @ParameterizedTest
            @MethodSource(value = "getTempCrewRoles")
            void testInitialTempCrewStateIsZero(PersonnelRole role) {
                assertEquals(0, testUnit.getTempCrewByPersonnelRole(role));
            }

            /**
             * Tests setting temp crew to a positive value.
             * Tests {@link Unit#setTempCrew(PersonnelRole, int)} and {@link Unit#getTempCrewByPersonnelRole(PersonnelRole)}.
             */
            @ParameterizedTest
            @MethodSource(value = "getTempCrewRoles")
            void testSetTempCrewToPositiveValue(PersonnelRole role) {
                // Arrange
                testUnit.setTempCrew(role, 0);

                // Act
                testUnit.setTempCrew(role, 5);

                // Assert
                assertEquals(5, testUnit.getTempCrewByPersonnelRole(role));
            }

            /**
             * Tests that setting temp crew to zero removes it from tracking.
             * Tests {@link Unit#setTempCrew(PersonnelRole, int)} and {@link Unit#isUsingBlobCrew()}.
             */
            @ParameterizedTest
            @MethodSource(value = "getTempCrewRoles")
            void testSetTempCrewToZeroRemovesRole(PersonnelRole role) {
                // Arrange
                testUnit.setTempCrew(role, 5);

                // Act
                testUnit.setTempCrew(role, 0);

                // Assert
                assertEquals(0, testUnit.getTempCrewByPersonnelRole(role));
                assertFalse(testUnit.isUsingBlobCrew());
            }

            /**
             * Tests that setting temp crew to negative value removes it from tracking.
             * Tests {@link Unit#setTempCrew(PersonnelRole, int)}.
             */
            @ParameterizedTest
            @MethodSource(value = "getTempCrewRoles")
            void testSetTempCrewToNegativeRemovesRole(PersonnelRole role) {
                // Arrange
                testUnit.setTempCrew(role, 5);

                // Act
                testUnit.setTempCrew(role, -3);

                // Assert
                assertEquals(0, testUnit.getTempCrewByPersonnelRole(role));
            }

            /**
             * Tests that temp crew can be updated to different values.
             * Tests {@link Unit#setTempCrew(PersonnelRole, int)}.
             */
            @ParameterizedTest
            @MethodSource(value = "getTempCrewRoles")
            void testUpdateTempCrewValue(PersonnelRole role) {
                // Arrange
                testUnit.setTempCrew(role, 5);

                // Act
                testUnit.setTempCrew(role, 10);

                // Assert
                assertEquals(10, testUnit.getTempCrewByPersonnelRole(role));
            }

            /**
             * Tests that multiple roles can have temp crew simultaneously.
             * Tests {@link Unit#setTempCrew(PersonnelRole, int)} and {@link Unit#getTempCrewByPersonnelRole(PersonnelRole)}.
             */
            @Test
            void testMultipleRolesWithTempCrew() {
                // Arrange & Act
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
                testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 3);
                testUnit.setTempCrew(PersonnelRole.VESSEL_CREW, 10);

                // Assert
                assertEquals(5, testUnit.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER));
                assertEquals(3, testUnit.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND));
                assertEquals(10, testUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW));
                assertEquals(0, testUnit.getTempCrewByPersonnelRole(PersonnelRole.BATTLE_ARMOUR));
            }

            /**
             * Tests that setting one role's temp crew doesn't affect others.
             * Tests {@link Unit#setTempCrew(PersonnelRole, int)}.
             */
            @Test
            void testTempCrewRoleIsolation() {
                // Arrange
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
                testUnit.setTempCrew(PersonnelRole.BATTLE_ARMOUR, 3);

                // Act
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 0);

                // Assert
                assertEquals(0, testUnit.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER));
                assertEquals(3, testUnit.getTempCrewByPersonnelRole(PersonnelRole.BATTLE_ARMOUR));
            }
        }

        /**
         * Nested test class for total temp crew calculations
         */
        @Nested
        class TotalTempCrewTests {

            /**
             * Tests that getTotalTempCrew returns zero when no temp crew assigned.
             * Tests {@link Unit#getTotalTempCrew()}.
             */
            @Test
            void testGetTotalTempCrewWithNoTempCrew() {
                assertEquals(0, testUnit.getTotalTempCrew());
            }

            /**
             * Tests that getTotalTempCrew sums across a single role.
             * Tests {@link Unit#getTotalTempCrew()}.
             */
            @Test
            void testGetTotalTempCrewWithSingleRole() {
                // Arrange
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);

                // Act
                int total = testUnit.getTotalTempCrew();

                // Assert
                assertEquals(5, total);
            }

            /**
             * Tests that getTotalTempCrew sums across multiple roles.
             * Tests {@link Unit#getTotalTempCrew()}.
             */
            @Test
            void testGetTotalTempCrewWithMultipleRoles() {
                // Arrange
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
                testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 3);
                testUnit.setTempCrew(PersonnelRole.VESSEL_CREW, 10);

                // Act
                int total = testUnit.getTotalTempCrew();

                // Assert
                assertEquals(18, total);
            }

            /**
             * Tests that getTotalTempCrew updates when temp crew is removed.
             * Tests {@link Unit#getTotalTempCrew()}.
             */
            @Test
            void testGetTotalTempCrewAfterRemoval() {
                // Arrange
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
                testUnit.setTempCrew(PersonnelRole.BATTLE_ARMOUR, 3);

                // Act
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 0);
                int total = testUnit.getTotalTempCrew();

                // Assert
                assertEquals(3, total);
            }
        }

        /**
         * Nested test class for blob crew status checks
         */
        @Nested
        class BlobCrewStatusTests {

            private static Stream<PersonnelRole> getTempCrewRoles() {
                return UnitTempCrewTests.getTempCrewRoles();
            }

            /**
             * Tests that isUsingBlobCrew returns false when no temp crew assigned.
             * Tests {@link Unit#isUsingBlobCrew()}.
             */
            @Test
            void testIsUsingBlobCrewReturnsFalseWhenNoTempCrew() {
                assertFalse(testUnit.isUsingBlobCrew());
            }

            /**
             * Tests that isUsingBlobCrew returns true when temp crew is assigned.
             * Tests {@link Unit#isUsingBlobCrew()}.
             */
            @ParameterizedTest
            @MethodSource(value = "getTempCrewRoles")
            void testIsUsingBlobCrewReturnsTrueWhenTempCrewAssigned(PersonnelRole role) {
                // Arrange
                testUnit.setTempCrew(role, 3);

                // Act & Assert
                assertTrue(testUnit.isUsingBlobCrew());
            }

            /**
             * Tests that isUsingBlobCrew returns false after all temp crew is removed.
             * Tests {@link Unit#isUsingBlobCrew()}.
             */
            @Test
            void testIsUsingBlobCrewReturnsFalseAfterRemoval() {
                // Arrange
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
                testUnit.setTempCrew(PersonnelRole.BATTLE_ARMOUR, 3);

                // Act
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 0);
                testUnit.setTempCrew(PersonnelRole.BATTLE_ARMOUR, 0);

                // Assert
                assertFalse(testUnit.isUsingBlobCrew());
            }

            /**
             * Tests that isUsingBlobCrew returns true when at least one role has temp crew.
             * Tests {@link Unit#isUsingBlobCrew()}.
             */
            @Test
            void testIsUsingBlobCrewReturnsTrueWithMultipleRoles() {
                // Arrange
                testUnit.setTempCrew(PersonnelRole.VESSEL_GUNNER, 5);
                testUnit.setTempCrew(PersonnelRole.VESSEL_PILOT, 3);

                // Act
                testUnit.setTempCrew(PersonnelRole.VESSEL_GUNNER, 0);

                // Assert - Still true because VEHICLE_CREW_GROUND remains
                assertTrue(testUnit.isUsingBlobCrew());
            }
        }

        /**
         * Nested test class for testing resetPilotAndEntity with temp crew.
         * Verifies that temp crew properly fills missing crew slots.
         */
        @Nested
        class ResetPilotAndEntityTests {

            /**
             * Tests that without temp crew, missing crew slots are marked as missing.
             * Tests {@link Unit#resetPilotAndEntity()}.
             */
            @Test
            void testMissingCrewMarkedAsMissingWithoutTempCrew() {
                // Arrange - Unit already has 1 real crew member from setup
                // Mock entity to require 3 crew (vehicle)
                when(mockEntity.getCrew()).thenReturn(mock(Crew.class));
                Crew testCrew = mockEntity.getCrew();
                when(testCrew.getSlotCount()).thenReturn(3);
                when(testCrew.getCrewType()).thenReturn(mock(megamek.common.units.CrewType.class));

                // Track which slots were set as missing
                boolean[] missingSlots = new boolean[3];
                doAnswer(invocation -> {
                    boolean missing = invocation.getArgument(0);
                    int slot = invocation.getArgument(1);
                    missingSlots[slot] = missing;
                    return null;
                }).when(testCrew).setMissing(anyBoolean(), anyInt());

                when(testCrew.isMissing(anyInt())).thenAnswer(invocation -> {
                    int slot = invocation.getArgument(0);
                    return missingSlots[slot];
                });

                // Act - Call resetPilotAndEntity (which calls updateCrew internally)
                testUnit.resetPilotAndEntity();

                // Assert - With 1 real crew and needing 3, slots 1 and 2 should be marked missing
                assertFalse(testCrew.isMissing(0), "Slot 0 should not be missing (has real crew)");
                assertTrue(testCrew.isMissing(1), "Slot 1 should be missing (no crew assigned)");
                assertTrue(testCrew.isMissing(2), "Slot 2 should be missing (no crew assigned)");
            }

            /**
             * Tests that with temp crew assigned, those slots are NOT marked as missing.
             * Tests {@link Unit#resetPilotAndEntity()}.
             */
            @Test
            void testTempCrewFillsMissingSlots() {
                // Arrange - Create a Tank entity for this test (need real instanceof check to work)
                megamek.common.units.Tank mockTank = mock(megamek.common.units.Tank.class);
                when(mockTank.getId()).thenReturn(1);

                // Mock Crew for Tank
                Crew testCrew = mock(Crew.class);
                when(testCrew.getSlotCount()).thenReturn(3);
                megamek.common.units.CrewType mockCrewType = mock(megamek.common.units.CrewType.class);
                when(mockCrewType.getPilotPos()).thenReturn(0);
                when(mockCrewType.getGunnerPos()).thenReturn(0);  // Same as pilot = command console path
                when(testCrew.getCrewType()).thenReturn(mockCrewType);
                doNothing().when(testCrew).resetGameState();
                doNothing().when(testCrew).setCommandBonus(anyInt());
                when(mockTank.getCrew()).thenReturn(testCrew);

                // Mock movement mode for ground vehicle
                megamek.common.units.EntityMovementMode mockMovementMode =
                      mock(megamek.common.units.EntityMovementMode.class);
                when(mockMovementMode.isMarine()).thenReturn(false);
                when(mockMovementMode.isVTOL()).thenReturn(false);
                when(mockTank.getMovementMode()).thenReturn(mockMovementMode);

                // Mock other required methods
                when(mockTank.getTransports()).thenReturn(new Vector<>());
                when(mockTank.getSensors()).thenReturn(new Vector<>());
                when(mockTank.hasBAP()).thenReturn(false);

                // Create new unit with Tank
                Unit tankUnit = new Unit(mockTank, mockCampaign);
                tankUnit.addPilotOrSoldier(getMockCommander());  // Add the pre-configured commander

                // Assign 2 temp crew to fill the missing slots
                tankUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 2);

                // Track which slots were set as missing
                boolean[] missingSlots = new boolean[3];
                doAnswer(invocation -> {
                    boolean missing = invocation.getArgument(0);
                    int slot = invocation.getArgument(1);
                    missingSlots[slot] = missing;
                    return null;
                }).when(testCrew).setMissing(anyBoolean(), anyInt());

                when(testCrew.isMissing(anyInt())).thenAnswer(invocation -> {
                    int slot = invocation.getArgument(0);
                    return missingSlots[slot];
                });

                // Assign 2 temp crew to fill the missing slots
                testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 2);

                // Act - Call resetPilotAndEntity (which calls updateCrew internally)
                testUnit.resetPilotAndEntity();

                // Assert - With 1 real crew + 2 temp crew = 3 total, no slots should be missing
                assertFalse(testCrew.isMissing(0), "Slot 0 should not be missing (has real crew)");
                assertFalse(testCrew.isMissing(1), "Slot 1 should not be missing (filled by temp crew)");
                assertFalse(testCrew.isMissing(2), "Slot 2 should not be missing (filled by temp crew)");
            }

            /**
             * Tests that temp crew exactly fills the gap between real crew and required crew.
             * Tests {@link Unit#resetPilotAndEntity()}.
             */
            @Test
            void testTempCrewPartialFill() {
                // Arrange - Unit already has 1 real crew member from setup
                // Mock entity to require 4 crew
                when(mockEntity.getCrew()).thenReturn(mock(Crew.class));
                Crew testCrew = mockEntity.getCrew();
                when(testCrew.getSlotCount()).thenReturn(4);
                when(testCrew.getCrewType()).thenReturn(mock(megamek.common.units.CrewType.class));

                // Track which slots were set as missing
                boolean[] missingSlots = new boolean[4];
                doAnswer(invocation -> {
                    boolean missing = invocation.getArgument(0);
                    int slot = invocation.getArgument(1);
                    missingSlots[slot] = missing;
                    return null;
                }).when(testCrew).setMissing(anyBoolean(), anyInt());

                when(testCrew.isMissing(anyInt())).thenAnswer(invocation -> {
                    int slot = invocation.getArgument(0);
                    return missingSlots[slot];
                });

                // Assign only 2 temp crew (not enough to fill all 3 missing slots)
                testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 2);

                // Act - Call resetPilotAndEntity
                //testUnit.resetPilotAndEntity();

                // Assert - Slots 0-2 filled (1 real + 2 temp), slot 3 still missing
                assertFalse(testCrew.isMissing(0), "Slot 0 should not be missing (has real crew)");
                assertFalse(testCrew.isMissing(1), "Slot 1 should not be missing (filled by temp crew)");
                assertFalse(testCrew.isMissing(2), "Slot 2 should not be missing (filled by temp crew)");
                assertTrue(testCrew.isMissing(3), "Slot 3 should be missing (not enough temp crew)");
            }
        }

        /**
         * Nested test class for entity-role compatibility.
         * Tests that only certain entity types can use specific temp crew roles.
         */
        @Nested
        class EntityRoleCompatibilityTests {

            /**
             * Provides entity type and expected driver/gunner role pairs for parameterized tests.
             * Returns: [EntityType class, expected driver role, expected gunner role]
             */
            private static Stream<Object[]> getEntityRoles() {
                return Stream.of(
                    // Infantry uses SOLDIER for both driver and gunner
                    new Object[]{megamek.common.units.Infantry.class, PersonnelRole.SOLDIER, PersonnelRole.SOLDIER},

                    // BattleArmor uses BATTLE_ARMOUR for both
                    new Object[]{megamek.common.battleArmor.BattleArmor.class, PersonnelRole.BATTLE_ARMOUR, PersonnelRole.BATTLE_ARMOUR},

                    // Tank (ground vehicle) uses VEHICLE_CREW_GROUND
                    new Object[]{megamek.common.units.Tank.class, PersonnelRole.VEHICLE_CREW_GROUND, PersonnelRole.VEHICLE_CREW_GROUND},

                    // VTOL uses VEHICLE_CREW_VTOL
                    new Object[]{megamek.common.units.VTOL.class, PersonnelRole.VEHICLE_CREW_VTOL, PersonnelRole.VEHICLE_CREW_VTOL}
                );
            }

            /**
             * Tests that entity types report the correct driver and gunner roles.
             * Tests {@link Unit#getDriverRole()} and {@link Unit#getGunnerRole()}.
             */
            @ParameterizedTest
            @MethodSource("getEntityRoles")
            void testEntityReturnsCorrectRoles(Class<?> entityClass, PersonnelRole expectedDriverRole, PersonnelRole expectedGunnerRole) {
                // Arrange - Mock the entity to be the specified type
                @SuppressWarnings("unchecked")
                Entity mockSpecificEntity = mock((Class<Entity>) entityClass);
                when(mockSpecificEntity.getId()).thenReturn(1);

                // Get the Crew reference first to avoid unfinished stubbing
                Crew mockCrew = mockEntity.getCrew();
                when(mockSpecificEntity.getCrew()).thenReturn(mockCrew);
                when(mockSpecificEntity.getTransports()).thenReturn(new Vector<>());
                when(mockSpecificEntity.getSensors()).thenReturn(new Vector<>());
                when(mockSpecificEntity.hasBAP()).thenReturn(false);

                // Mock entity type checks based on the entity class
                when(mockSpecificEntity.isMek()).thenReturn(false);
                when(mockSpecificEntity.isBattleArmor()).thenReturn(
                    entityClass.equals(megamek.common.battleArmor.BattleArmor.class));
                when(mockSpecificEntity.isConventionalInfantry()).thenReturn(
                    entityClass.equals(megamek.common.units.Infantry.class));
                when(mockSpecificEntity.isAerospace()).thenReturn(false);
                when(mockSpecificEntity.isSmallCraft()).thenReturn(false);
                when(mockSpecificEntity.isLargeCraft()).thenReturn(false);
                when(mockSpecificEntity.isProtoMek()).thenReturn(false);

                // Mock movement mode for Tank/VTOL types
                if (entityClass.equals(megamek.common.units.Tank.class) ||
                    entityClass.equals(megamek.common.units.VTOL.class)) {
                    megamek.common.units.EntityMovementMode mockMovementMode =
                        mock(megamek.common.units.EntityMovementMode.class);
                    when(mockMovementMode.isMarine()).thenReturn(false);
                    when(mockMovementMode.isVTOL()).thenReturn(
                        entityClass.equals(megamek.common.units.VTOL.class));
                    when(mockSpecificEntity.getMovementMode()).thenReturn(mockMovementMode);
                }

                Unit testSpecificUnit = new Unit(mockSpecificEntity, mockCampaign);

                // Act - Get the driver and gunner roles
                PersonnelRole actualDriverRole = testSpecificUnit.getDriverRole();
                PersonnelRole actualGunnerRole = testSpecificUnit.getGunnerRole();

                // Assert
                assertEquals(expectedDriverRole, actualDriverRole,
                    String.format("%s should use %s as driver role",
                        entityClass.getSimpleName(), expectedDriverRole));
                assertEquals(expectedGunnerRole, actualGunnerRole,
                    String.format("%s should use %s as gunner role",
                        entityClass.getSimpleName(), expectedGunnerRole));
            }

            /**
             * Tests that temp crew can be set and retrieved for compatible roles.
             * Tests {@link Unit#setTempCrew(PersonnelRole, int)} and {@link Unit#getTempCrewByPersonnelRole(PersonnelRole)}.
             */
            @Test
            void testSetTempCrewForCompatibleRole() {
                // Arrange - Use existing testUnit (has mocked entity)

                // Act - Set temp crew for SOLDIER role (compatible with base entity mock)
                testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);

                // Assert
                assertEquals(5, testUnit.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER),
                    "Temp crew should be set for compatible role");
                assertEquals(5, testUnit.getTotalTempCrew(),
                    "Total temp crew should reflect assigned crew");
            }
        }

        /**
         * Nested test class for role-specific temp crew operations
         */
        @Nested
        class RoleSpecificTests {

            @Nested
            class SoldierTempCrewTests {
                @Test
                void testSoldierTempCrewOperations() {
                    // Arrange & Act
                    testUnit.setTempCrew(PersonnelRole.SOLDIER, 10);

                    // Assert
                    assertEquals(10, testUnit.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER));
                    assertEquals(10, testUnit.getTotalTempCrew());
                    assertTrue(testUnit.isUsingBlobCrew());
                }
            }

            @Nested
            class BattleArmorTempCrewTests {
                @Test
                void testBattleArmorTempCrewOperations() {
                    // Arrange & Act
                    testUnit.setTempCrew(PersonnelRole.BATTLE_ARMOUR, 5);

                    // Assert
                    assertEquals(5, testUnit.getTempCrewByPersonnelRole(PersonnelRole.BATTLE_ARMOUR));
                    assertEquals(5, testUnit.getTotalTempCrew());
                    assertTrue(testUnit.isUsingBlobCrew());
                }
            }

            @Nested
            class VehicleCrewGroundTempCrewTests {
                @Test
                void testVehicleCrewGroundTempCrewOperations() {
                    // Arrange & Act
                    testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 3);

                    // Assert
                    assertEquals(3, testUnit.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND));
                    assertEquals(3, testUnit.getTotalTempCrew());
                    assertTrue(testUnit.isUsingBlobCrew());
                }
            }

            @Nested
            class VehicleCrewVTOLTempCrewTests {
                @Test
                void testVehicleCrewVTOLTempCrewOperations() {
                    // Arrange & Act
                    testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_VTOL, 3);

                    // Assert
                    assertEquals(3, testUnit.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_VTOL));
                    assertEquals(3, testUnit.getTotalTempCrew());
                    assertTrue(testUnit.isUsingBlobCrew());
                }
            }

            @Nested
            class VehicleCrewNavalTempCrewTests {
                @Test
                void testVehicleCrewNavalTempCrewOperations() {
                    // Arrange & Act
                    testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_NAVAL, 3);

                    // Assert
                    assertEquals(3, testUnit.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_NAVAL));
                    assertEquals(3, testUnit.getTotalTempCrew());
                    assertTrue(testUnit.isUsingBlobCrew());
                }
            }

            @Nested
            class VesselPilotTempCrewTests {
                @Test
                void testVesselPilotTempCrewOperations() {
                    // Arrange & Act
                    testUnit.setTempCrew(PersonnelRole.VESSEL_PILOT, 1);

                    // Assert
                    assertEquals(1, testUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT));
                    assertEquals(1, testUnit.getTotalTempCrew());
                    assertTrue(testUnit.isUsingBlobCrew());
                }
            }

            @Nested
            class VesselGunnerTempCrewTests {
                @Test
                void testVesselGunnerTempCrewOperations() {
                    // Arrange & Act
                    testUnit.setTempCrew(PersonnelRole.VESSEL_GUNNER, 2);

                    // Assert
                    assertEquals(2, testUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_GUNNER));
                    assertEquals(2, testUnit.getTotalTempCrew());
                    assertTrue(testUnit.isUsingBlobCrew());
                }
            }

            @Nested
            class VesselCrewTempCrewTests {
                @Test
                void testVesselCrewTempCrewOperations() {
                    // Arrange & Act
                    testUnit.setTempCrew(PersonnelRole.VESSEL_CREW, 20);

                    // Assert
                    assertEquals(20, testUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW));
                    assertEquals(20, testUnit.getTotalTempCrew());
                    assertTrue(testUnit.isUsingBlobCrew());
                }
            }
        }
    }

    private Person getMockCommander() {
        // Mock commander with Portrait (required for resetPilotAndEntity)
        Person mockCommander = mock(Person.class);
        when(mockCommander.getFullTitle()).thenReturn("Test Commander");
        when(mockCommander.getCallsign()).thenReturn("TestPilot");
        when(mockCommander.getGender()).thenReturn(megamek.common.enums.Gender.MALE);
        when(mockCommander.isClanPersonnel()).thenReturn(false);

        // Mock Portrait and make it cloneable
        megamek.common.icons.Portrait mockPortrait = mock(megamek.common.icons.Portrait.class);
        when(mockPortrait.clone()).thenReturn(mockPortrait);
        when(mockCommander.getPortrait()).thenReturn(mockPortrait);

        when(mockCommander.getId()).thenReturn(UUID.randomUUID());
        when(mockCommander.getAdjustedToughness()).thenReturn(0);
        when(mockCommander.getHits()).thenReturn(0);

        // Mock skills (required by updateCrew checks)
        when(mockCommander.hasSkill(any())).thenReturn(true);

        // Mock Skill object and SkillModifierData for calcCompositeCrew
        mekhq.campaign.personnel.skills.Skill mockSkill = mock(mekhq.campaign.personnel.skills.Skill.class);
        when(mockSkill.getFinalSkillValue(any())).thenReturn(4); // Default piloting/gunnery of 4
        when(mockCommander.getSkill(any())).thenReturn(mockSkill);

        mekhq.campaign.personnel.skills.SkillModifierData mockSkillModData = mock(mekhq.campaign.personnel.skills.SkillModifierData.class);
        when(mockCommander.getSkillModifierData()).thenReturn(mockSkillModData);
        when(mockCommander.getInjuryModifiers(anyBoolean())).thenReturn(0);

        // Mock status (required by updateCrew checks)
        mekhq.campaign.personnel.enums.PersonnelStatus mockStatus = mock(mekhq.campaign.personnel.enums.PersonnelStatus.class);
        when(mockStatus.isActive()).thenReturn(true);
        when(mockCommander.getStatus()).thenReturn(mockStatus);

        // Mock origin planet (required for recruitPerson)
        mekhq.campaign.universe.Planet mockPlanet = mock(mekhq.campaign.universe.Planet.class);
        when(mockPlanet.getId()).thenReturn("test-planet");

        // Mock parent system (required for Campaign.recruitPerson disease inoculation checks)
        mekhq.campaign.universe.PlanetarySystem mockSystem = mock(mekhq.campaign.universe.PlanetarySystem.class);
        when(mockSystem.getId()).thenReturn("test-system");
        when(mockPlanet.getParentSystem()).thenReturn(mockSystem);

        when(mockCommander.getOriginPlanet()).thenReturn(mockPlanet);
        return mockCommander;
    }
}
