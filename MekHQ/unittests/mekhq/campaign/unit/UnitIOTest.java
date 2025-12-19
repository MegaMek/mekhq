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

package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Ranks;
import testUtilities.MHQTestUtilities;

/**
 * Tests for Unit I/O operations (XML serialization and deserialization).
 */
public class UnitIOTest {

    /**
     * Nested test class for writeToXML method.
     * Tests XML serialization of Unit objects, with focus on temp crew (blob crew) functionality.
     */
    @Nested
    public class WriteToXMLTests {

        private Campaign mockCampaign;
        private Entity mockEntity;

        @BeforeAll
        public static void setupAll() {
            EquipmentType.initializeTypes();
            Ranks.initializeRankSystems();
        }

        @BeforeEach
        public void setup() {
            mockCampaign = spy(MHQTestUtilities.getTestCampaign());

            // Enable blob crew for all roles
            doReturn(true).when(mockCampaign).isBlobCrewEnabled(any(PersonnelRole.class));
            doReturn(true).when(mockCampaign).isBlobVesselCrewEnabled();
            doReturn(true).when(mockCampaign).isBlobVesselPilotEnabled();
            doReturn(true).when(mockCampaign).isBlobVesselGunnerEnabled();

            // Mock getEntities() for XML writing
            doReturn(new Vector<>()).when(mockCampaign).getEntities();

            mockEntity = mock(Entity.class);
            when(mockEntity.getId()).thenReturn(1);

            // Mock Crew
            Crew mockCrew = mock(Crew.class);
            when(mockCrew.getSlotCount()).thenReturn(1);

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

            // Mock Camouflage (required for writeEntityToXmlString)
            megamek.common.icons.Camouflage mockCamouflage = mock(megamek.common.icons.Camouflage.class);
            when(mockCamouflage.hasDefaultCategory()).thenReturn(true);
            when(mockEntity.getCamouflage()).thenReturn(mockCamouflage);

            // Mock entity setter methods
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
        }

        /**
         * Provides all temp crew roles for parameterized tests.
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
         * Helper method to write unit to XML and parse the result.
         */
        private Document writeUnitToXMLDocument(Unit unit) throws Exception {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            unit.writeToXML(printWriter, 0);
            printWriter.flush();

            String xmlString = stringWriter.toString();

            // Parse XML string into Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new java.io.ByteArrayInputStream(xmlString.getBytes()));
        }

        /**
         * Tests that basic unit XML structure is created correctly.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)}.
         */
        @Test
        void testBasicXMLStructure() throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);

            // Act - Write to string to verify structure
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            testUnit.writeToXML(printWriter, 0);
            printWriter.flush();
            String xmlString = stringWriter.toString();

            // Assert - Verify basic XML structure exists
            assertFalse(xmlString.isEmpty(), "XML should not be empty");
            assertTrue(xmlString.contains("<unit"), "XML should contain unit opening tag");
            assertTrue(xmlString.contains("</unit>"), "XML should contain unit closing tag");
        }

        /**
         * Tests that unit with no temp crew does not write tempCrewMap.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)}.
         */
        @Test
        void testNoTempCrewMapWhenEmpty() throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);

            // Act
            Document doc = writeUnitToXMLDocument(testUnit);

            // Assert
            NodeList tempCrewMapNodes = doc.getElementsByTagName("tempCrewMap");
            assertEquals(0, tempCrewMapNodes.getLength(),
                "tempCrewMap element should not exist when no temp crew assigned");
        }

        /**
         * Tests that tempCrewMap is written when temp crew is assigned.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)}.
         */
        @Test
        void testTempCrewMapWrittenWhenPresent() throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);
            testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);

            // Act
            Document doc = writeUnitToXMLDocument(testUnit);

            // Assert
            NodeList tempCrewMapNodes = doc.getElementsByTagName("tempCrewMap");
            assertEquals(1, tempCrewMapNodes.getLength(),
                "tempCrewMap element should exist when temp crew is assigned");
        }

        /**
         * Tests that a single temp crew role is serialized correctly to XML.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)} for individual PersonnelRole values.
         *
         * @param role the personnel role to test
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testSingleTempCrewRoleSerialization(PersonnelRole role) throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);
            int expectedCount = 3;
            testUnit.setTempCrew(role, expectedCount);

            // Act
            Document doc = writeUnitToXMLDocument(testUnit);

            // Assert
            NodeList tempCrewMapNodes = doc.getElementsByTagName("tempCrewMap");
            assertEquals(1, tempCrewMapNodes.getLength(), "Should have one tempCrewMap element");

            Element tempCrewMap = (Element) tempCrewMapNodes.item(0);
            NodeList tempCrewNodes = tempCrewMap.getElementsByTagName("tempCrew");
            assertEquals(1, tempCrewNodes.getLength(),
                String.format("Should have exactly one tempCrew entry for %s", role));

            Element tempCrew = (Element) tempCrewNodes.item(0);
            String actualRole = tempCrew.getElementsByTagName("role").item(0).getTextContent();
            String actualCount = tempCrew.getElementsByTagName("count").item(0).getTextContent();

            assertEquals(role.name(), actualRole,
                String.format("Role should be %s", role.name()));
            assertEquals(String.valueOf(expectedCount), actualCount,
                String.format("Count should be %d for role %s", expectedCount, role));
        }

        /**
         * Tests that multiple temp crew roles are serialized correctly to XML.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)} with multiple PersonnelRole entries.
         */
        @Test
        void testMultipleTempCrewRolesSerialization() throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);
            testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
            testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 3);
            testUnit.setTempCrew(PersonnelRole.VESSEL_CREW, 10);

            // Act
            Document doc = writeUnitToXMLDocument(testUnit);

            // Assert
            NodeList tempCrewMapNodes = doc.getElementsByTagName("tempCrewMap");
            assertEquals(1, tempCrewMapNodes.getLength(), "Should have one tempCrewMap element");

            Element tempCrewMap = (Element) tempCrewMapNodes.item(0);
            NodeList tempCrewNodes = tempCrewMap.getElementsByTagName("tempCrew");
            assertEquals(3, tempCrewNodes.getLength(),
                "Should have exactly three tempCrew entries");

            // Verify each role is present with correct count
            boolean foundSoldier = false;
            boolean foundVehicleCrewGround = false;
            boolean foundVesselCrew = false;

            for (int i = 0; i < tempCrewNodes.getLength(); i++) {
                Element tempCrew = (Element) tempCrewNodes.item(i);
                String role = tempCrew.getElementsByTagName("role").item(0).getTextContent();
                int count = Integer.parseInt(tempCrew.getElementsByTagName("count").item(0).getTextContent());

                if (role.equals(PersonnelRole.SOLDIER.name())) {
                    assertEquals(5, count, "SOLDIER count should be 5");
                    foundSoldier = true;
                } else if (role.equals(PersonnelRole.VEHICLE_CREW_GROUND.name())) {
                    assertEquals(3, count, "VEHICLE_CREW_GROUND count should be 3");
                    foundVehicleCrewGround = true;
                } else if (role.equals(PersonnelRole.VESSEL_CREW.name())) {
                    assertEquals(10, count, "VESSEL_CREW count should be 10");
                    foundVesselCrew = true;
                }
            }

            assertTrue(foundSoldier, "Should find SOLDIER role in XML");
            assertTrue(foundVehicleCrewGround, "Should find VEHICLE_CREW_GROUND role in XML");
            assertTrue(foundVesselCrew, "Should find VESSEL_CREW role in XML");
        }

        /**
         * Tests that temp crew with zero count is not written to XML.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)}.
         */
        @Test
        void testZeroTempCrewNotWritten() throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);
            testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
            testUnit.setTempCrew(PersonnelRole.SOLDIER, 0); // Remove it

            // Act
            Document doc = writeUnitToXMLDocument(testUnit);

            // Assert
            NodeList tempCrewMapNodes = doc.getElementsByTagName("tempCrewMap");
            assertEquals(0, tempCrewMapNodes.getLength(),
                "tempCrewMap should not exist after removing all temp crew");
        }

        /**
         * Tests that updating temp crew count updates XML correctly.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)}.
         */
        @Test
        void testUpdatedTempCrewCount() throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);
            testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
            testUnit.setTempCrew(PersonnelRole.SOLDIER, 10); // Update count

            // Act
            Document doc = writeUnitToXMLDocument(testUnit);

            // Assert
            NodeList tempCrewMapNodes = doc.getElementsByTagName("tempCrewMap");
            assertEquals(1, tempCrewMapNodes.getLength(), "Should have one tempCrewMap element");

            Element tempCrewMap = (Element) tempCrewMapNodes.item(0);
            NodeList tempCrewNodes = tempCrewMap.getElementsByTagName("tempCrew");
            assertEquals(1, tempCrewNodes.getLength(), "Should have exactly one tempCrew entry");

            Element tempCrew = (Element) tempCrewNodes.item(0);
            String actualCount = tempCrew.getElementsByTagName("count").item(0).getTextContent();
            assertEquals("10", actualCount, "Count should be updated to 10");
        }

        /**
         * Tests that removing one role from multiple temp crews only affects that role.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)}.
         */
        @Test
        void testRemovingOneRoleFromMultiple() throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);
            testUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
            testUnit.setTempCrew(PersonnelRole.BATTLE_ARMOUR, 3);
            testUnit.setTempCrew(PersonnelRole.SOLDIER, 0); // Remove SOLDIER

            // Act
            Document doc = writeUnitToXMLDocument(testUnit);

            // Assert
            NodeList tempCrewMapNodes = doc.getElementsByTagName("tempCrewMap");
            assertEquals(1, tempCrewMapNodes.getLength(), "Should still have tempCrewMap element");

            Element tempCrewMap = (Element) tempCrewMapNodes.item(0);
            NodeList tempCrewNodes = tempCrewMap.getElementsByTagName("tempCrew");
            assertEquals(1, tempCrewNodes.getLength(),
                "Should have exactly one tempCrew entry after removing SOLDIER");

            Element tempCrew = (Element) tempCrewNodes.item(0);
            String role = tempCrew.getElementsByTagName("role").item(0).getTextContent();
            assertEquals(PersonnelRole.BATTLE_ARMOUR.name(), role,
                "Remaining role should be BATTLE_ARMOUR");
        }

        /**
         * Tests that large temp crew counts are serialized correctly.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)}.
         */
        @Test
        void testLargeTempCrewCount() throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);
            int largeCount = 1000;
            testUnit.setTempCrew(PersonnelRole.VESSEL_CREW, largeCount);

            // Act
            Document doc = writeUnitToXMLDocument(testUnit);

            // Assert
            Element tempCrewMap = (Element) doc.getElementsByTagName("tempCrewMap").item(0);
            Element tempCrew = (Element) tempCrewMap.getElementsByTagName("tempCrew").item(0);
            String actualCount = tempCrew.getElementsByTagName("count").item(0).getTextContent();

            assertEquals(String.valueOf(largeCount), actualCount,
                "Large count should be serialized correctly");
        }

        /**
         * Tests that all supported temp crew roles can be serialized together.
         * Verifies {@link Unit#writeToXML(PrintWriter, int)}.
         */
        @Test
        void testAllTempCrewRolesTogether() throws Exception {
            // Arrange
            Unit testUnit = new Unit(mockEntity, mockCampaign);
            testUnit.setTempCrew(PersonnelRole.SOLDIER, 1);
            testUnit.setTempCrew(PersonnelRole.BATTLE_ARMOUR, 2);
            testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 3);
            testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_VTOL, 4);
            testUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_NAVAL, 5);
            testUnit.setTempCrew(PersonnelRole.VESSEL_PILOT, 6);
            testUnit.setTempCrew(PersonnelRole.VESSEL_GUNNER, 7);
            testUnit.setTempCrew(PersonnelRole.VESSEL_CREW, 8);

            // Act
            Document doc = writeUnitToXMLDocument(testUnit);

            // Assert
            Element tempCrewMap = (Element) doc.getElementsByTagName("tempCrewMap").item(0);
            NodeList tempCrewNodes = tempCrewMap.getElementsByTagName("tempCrew");
            assertEquals(8, tempCrewNodes.getLength(),
                "Should have all 8 temp crew roles serialized");

            // Verify each role is present
            for (int i = 0; i < tempCrewNodes.getLength(); i++) {
                Element tempCrew = (Element) tempCrewNodes.item(i);
                String role = tempCrew.getElementsByTagName("role").item(0).getTextContent();
                int count = Integer.parseInt(tempCrew.getElementsByTagName("count").item(0).getTextContent());

                assertTrue(count >= 1 && count <= 8,
                    String.format("Count for role %s should be between 1 and 8", role));
            }
        }
    }

    /**
     * Nested test class for temp crew deserialization from XML.
     * Tests {@link Unit#generateInstanceFromXML(Node, Version, Campaign)}.
     */
    @Nested
    @Disabled // TODO: Fix test / Entity mocking
    public class GenerateInstanceFromXMLTests {

        private Campaign mockCampaign;
        private Entity mockEntity;

        @BeforeAll
        public static void setupAll() {
            EquipmentType.initializeTypes();
            Ranks.initializeRankSystems();
        }

        @BeforeEach
        public void setup() throws Exception {
            mockCampaign = spy(MHQTestUtilities.getTestCampaign());

            // Enable blob crew for all roles
            doReturn(true).when(mockCampaign).isBlobCrewEnabled(any(PersonnelRole.class));
            doReturn(true).when(mockCampaign).isBlobVesselCrewEnabled();
            doReturn(true).when(mockCampaign).isBlobVesselPilotEnabled();
            doReturn(true).when(mockCampaign).isBlobVesselGunnerEnabled();

            // Mock getEntities() for XML operations
            doReturn(new Vector<>()).when(mockCampaign).getEntities();

            // Use a real Entity instead of mock for proper serialization/deserialization
            MekSummary mekSummary = MekSummaryCache.getInstance()
                .getMek("Atlas AS7-D");
            if (mekSummary != null) {
                mockEntity = mekSummary.loadEntity();
            } else {
                // Fallback: create a simple Mek if Atlas not found
                mockEntity = new BipedMek();
            }
            mockEntity.setGame(mockCampaign.getGame());
        }

        /**
         * Provides all temp crew roles for parameterized tests.
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
         * Helper method to serialize a unit to XML, then deserialize it back.
         */
        private Unit serializeAndDeserialize(Unit originalUnit) throws Exception {
            // Ensure the unit has an ID for serialization
            if (originalUnit.getId() == null) {
                originalUnit.setId(java.util.UUID.randomUUID());
            }

            // Serialize to XML
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            originalUnit.writeToXML(printWriter, 0);
            printWriter.flush();
            String xmlString = stringWriter.toString();

            // Parse XML into Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(
                xmlString.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

            // Get the unit element
            Element unitElement = doc.getDocumentElement();
            assertEquals("unit", unitElement.getNodeName(), "Root element should be 'unit'");

            // Deserialize using generateInstanceFromXML
            return Unit.generateInstanceFromXML(unitElement, new Version(), mockCampaign);
        }

        /**
         * Tests that unit with no temp crew deserializes correctly without tempCrewMap.
         * Verifies {@link Unit#generateInstanceFromXML(Node, Version, Campaign)}.
         */
        @Test
        void testNoTempCrewDeserialization() throws Exception {
            // Arrange
            Unit originalUnit = new Unit(mockEntity, mockCampaign);

            // Act
            Unit deserializedUnit = serializeAndDeserialize(originalUnit);

            // Assert
            assertNotNull(deserializedUnit, "Deserialized unit should not be null");
            // Verify all temp crew roles are 0
            assertEquals(0, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER));
            assertEquals(0, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.BATTLE_ARMOUR));
            assertEquals(0, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW));
        }

        /**
         * Tests that a single temp crew role deserializes correctly.
         * Verifies {@link Unit#generateInstanceFromXML(Node, Version, Campaign)} for each PersonnelRole.
         *
         * @param role the personnel role to test
         */
        @ParameterizedTest
        @MethodSource("getTempCrewRoles")
        void testSingleTempCrewRoleDeserialization(PersonnelRole role) throws Exception {
            // Arrange
            Unit originalUnit = new Unit(mockEntity, mockCampaign);
            int expectedCount = 3;
            originalUnit.setTempCrew(role, expectedCount);

            // Act
            Unit deserializedUnit = serializeAndDeserialize(originalUnit);

            // Assert
            assertNotNull(deserializedUnit, "Deserialized unit should not be null");
            assertEquals(expectedCount, deserializedUnit.getTempCrewByPersonnelRole(role),
                String.format("Count for %s should be %d", role, expectedCount));
        }

        /**
         * Tests that multiple temp crew roles deserialize correctly.
         * Verifies {@link Unit#generateInstanceFromXML(Node, Version, Campaign)}.
         */
        @Test
        void testMultipleTempCrewRolesDeserialization() throws Exception {
            // Arrange
            Unit originalUnit = new Unit(mockEntity, mockCampaign);
            originalUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
            originalUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 3);
            originalUnit.setTempCrew(PersonnelRole.VESSEL_CREW, 10);

            // Act
            Unit deserializedUnit = serializeAndDeserialize(originalUnit);

            // Assert
            assertNotNull(deserializedUnit, "Deserialized unit should not be null");
            assertEquals(5, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER),
                "SOLDIER count should be 5");
            assertEquals(3, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND),
                "VEHICLE_CREW_GROUND count should be 3");
            assertEquals(10, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW),
                "VESSEL_CREW count should be 10");
        }

        /**
         * Tests that large temp crew counts deserialize correctly.
         * Verifies {@link Unit#generateInstanceFromXML(Node, Version, Campaign)}.
         */
        @Test
        void testLargeTempCrewCountDeserialization() throws Exception {
            // Arrange
            Unit originalUnit = new Unit(mockEntity, mockCampaign);
            int largeCount = 1000;
            originalUnit.setTempCrew(PersonnelRole.VESSEL_CREW, largeCount);

            // Act
            Unit deserializedUnit = serializeAndDeserialize(originalUnit);

            // Assert
            assertNotNull(deserializedUnit, "Deserialized unit should not be null");
            assertEquals(largeCount, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW),
                "Large count should be deserialized correctly");
        }

        /**
         * Tests that all supported temp crew roles deserialize correctly together.
         * Verifies {@link Unit#generateInstanceFromXML(Node, Version, Campaign)}.
         */
        @Test
        void testAllTempCrewRolesDeserialization() throws Exception {
            // Arrange
            Unit originalUnit = new Unit(mockEntity, mockCampaign);
            originalUnit.setTempCrew(PersonnelRole.SOLDIER, 1);
            originalUnit.setTempCrew(PersonnelRole.BATTLE_ARMOUR, 2);
            originalUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, 3);
            originalUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_VTOL, 4);
            originalUnit.setTempCrew(PersonnelRole.VEHICLE_CREW_NAVAL, 5);
            originalUnit.setTempCrew(PersonnelRole.VESSEL_PILOT, 6);
            originalUnit.setTempCrew(PersonnelRole.VESSEL_GUNNER, 7);
            originalUnit.setTempCrew(PersonnelRole.VESSEL_CREW, 8);

            // Act
            Unit deserializedUnit = serializeAndDeserialize(originalUnit);

            // Assert
            assertNotNull(deserializedUnit, "Deserialized unit should not be null");
            assertEquals(1, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER));
            assertEquals(2, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.BATTLE_ARMOUR));
            assertEquals(3, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND));
            assertEquals(4, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_VTOL));
            assertEquals(5, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_NAVAL));
            assertEquals(6, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT));
            assertEquals(7, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_GUNNER));
            assertEquals(8, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW));
        }

        /**
         * Tests that zero count temp crew is not deserialized.
         * Verifies {@link Unit#generateInstanceFromXML(Node, Version, Campaign)}.
         */
        @Test
        void testZeroTempCrewNotDeserialized() throws Exception {
            // Arrange
            Unit originalUnit = new Unit(mockEntity, mockCampaign);
            originalUnit.setTempCrew(PersonnelRole.SOLDIER, 5);
            originalUnit.setTempCrew(PersonnelRole.SOLDIER, 0); // Remove it

            // Act
            Unit deserializedUnit = serializeAndDeserialize(originalUnit);

            // Assert
            assertNotNull(deserializedUnit, "Deserialized unit should not be null");
            assertEquals(0, deserializedUnit.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER),
                "SOLDIER count should be 0 after removal");
        }
    }
}
