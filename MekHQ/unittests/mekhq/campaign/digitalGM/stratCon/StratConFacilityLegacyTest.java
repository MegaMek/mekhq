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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityDefinition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityFactory;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that facilities from before 0.51.01 still load: facilities in old saves, which held a full copy of their
 * one-sided definition, and old-format definition files, which the factory merges back into one definition per type.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilityLegacyTest {

    private static final String TEST_FACILITY_DIRECTORY = "testresources/data/stratconfacilities";

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConFacility unmarshal(String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(StratConFacility.class);
        return context.createUnmarshaller()
                     .unmarshal(new StreamSource(new StringReader(xml)), StratConFacility.class)
                     .getValue();
    }

    private static String marshal(StratConFacility facility) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(StratConFacility.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter writer = new StringWriter();
        marshaller.marshal(new JAXBElement<>(new QName("facility"), StratConFacility.class, facility), writer);
        return writer.toString();
    }

    /** A hostile Mek Base as a save from before 0.51.01 held it, with one objective modifier added at placement. */
    private static final String OLD_SAVE_HOSTILE_MEK_BASE = """
          <facility>
              <owner>Opposing</owner>
              <displayableName>Mek Base</displayableName>
              <facilityType>MekBase</facilityType>
              <userDescription>Hostile Meks will participate in scenarios in this sector.</userDescription>
              <visible>false</visible>
              <isAvailable>false</isAvailable>
              <sharedModifiers>EnemyMekReinforcements.json</sharedModifiers>
              <localModifiers>EnemyMekGarrison.json</localModifiers>
              <localModifiers>FacilityHostileDestroy.json</localModifiers>
              <capturedDefinition>AlliedMekBase.json</capturedDefinition>
              <revealTrack>false</revealTrack>
              <increaseScanRange>false</increaseScanRange>
              <scenarioOddsModifier>0</scenarioOddsModifier>
              <monthlySPModifier>0</monthlySPModifier>
              <aggroRating>0</aggroRating>
              <strategicObjective>true</strategicObjective>
          </facility>
          """;

    @Nested
    class OldSaves {
        @Test
        void anOldFacilityIsMatchedToTheDefinitionOfItsType() throws JAXBException {
            StratConFacility facility = unmarshal(OLD_SAVE_HOSTILE_MEK_BASE);
            facility.resolveLegacyData();

            assertEquals("MekBase", facility.getDefinitionId());
            assertEquals(ForceAlignment.Opposing, facility.getOwner());
            assertFalse(facility.getVisible());
            assertFalse(facility.isAvailable());
            assertTrue(facility.isStrategicObjective());
            assertEquals(FacilityType.MekBase, facility.getFacilityType());
            assertEquals(List.of("EnemyMekReinforcements.json"), facility.getSharedModifiers());
        }

        @Test
        void onlyTheModifiersAddedAtPlacementAreKeptAsTheFacilitysOwn() throws JAXBException {
            StratConFacility facility = unmarshal(OLD_SAVE_HOSTILE_MEK_BASE);
            facility.resolveLegacyData();

            assertEquals(List.of("FacilityHostileDestroy.json"), facility.getAdditionalLocalModifiers());
            // An old save's facility is a Base at full garrison, whose second step adds the turrets.
            assertEquals(List.of("EnemyMekGarrison.json", "EnemyTurrets.json", "FacilityHostileDestroy.json"),
                  facility.getLocalModifiers());
        }

        @Test
        void aMatchedFacilityIsSavedInTheNewFormatOnly() throws JAXBException {
            StratConFacility facility = unmarshal(OLD_SAVE_HOSTILE_MEK_BASE);
            facility.resolveLegacyData();

            String xml = marshal(facility);

            assertTrue(xml.contains("<definitionId>MekBase</definitionId>"));
            assertTrue(xml.contains("<additionalLocalModifier>FacilityHostileDestroy.json</additionalLocalModifier>"));
            assertFalse(xml.contains("facilityType"), "the old definition copy should not be written back");
            assertFalse(xml.contains("<localModifiers>"));
        }

        @Test
        void resolvingTwiceChangesNothing() throws JAXBException {
            StratConFacility facility = unmarshal(OLD_SAVE_HOSTILE_MEK_BASE);
            facility.resolveLegacyData();
            facility.resolveLegacyData();

            assertEquals(List.of("FacilityHostileDestroy.json"), facility.getAdditionalLocalModifiers());
        }

        @Test
        void theTrackResolvesEveryFacilityOnLoad() throws JAXBException {
            StratConTrackState track = new StratConTrackState();
            StratConCoords coords = new StratConCoords(2, 3);
            track.addFacility(coords, unmarshal(OLD_SAVE_HOSTILE_MEK_BASE));

            track.restoreFacilityDefinitions();

            assertEquals("MekBase", track.getFacility(coords).getDefinitionId());
        }
    }

    @Nested
    class NewSaves {
        @Test
        void aFacilityRoundTripsThroughTheSave() throws JAXBException {
            StratConFacilityDefinition definition = StratConFacilityFactory.getDefinition("DataCenter");
            assertNotNull(definition);
            StratConFacility original = new StratConFacility(definition, ForceAlignment.Allied);
            original.setVisible(true);
            original.setIsAvailable(false);
            original.setStrategicObjective(true);
            original.addAdditionalLocalModifiers(List.of("FacilityAlliedDefend.json"));

            StratConFacility reloaded = unmarshal(marshal(original));
            reloaded.resolveLegacyData();

            assertEquals("DataCenter", reloaded.getDefinitionId());
            assertSame(definition, reloaded.getDefinition(), "a saved facility looks its definition up again");
            assertEquals(ForceAlignment.Allied, reloaded.getOwner());
            assertTrue(reloaded.getVisible());
            assertFalse(reloaded.isAvailable());
            assertTrue(reloaded.isStrategicObjective());
            assertEquals(List.of("FacilityAlliedDefend.json"), reloaded.getAdditionalLocalModifiers());
            assertEquals(1, reloaded.getScanRangeIncrease());
        }

        @Test
        void aFacilityWhoseDefinitionIsMissingDoesNothing() throws JAXBException {
            StratConFacility facility = unmarshal("""
                  <facility>
                      <definitionId>NoSuchFacility</definitionId>
                      <owner>Opposing</owner>
                  </facility>
                  """);

            assertEquals("NoSuchFacility", facility.getDisplayableName());
            assertTrue(facility.getSharedModifiers().isEmpty());
            assertEquals(0, facility.getScenarioOddsModifier());
            assertEquals("NoSuchFacility", facility.getDefinitionId(), "the ID is kept, so the data can come back");
        }
    }

    @Nested
    class OldFormatFiles {
        @AfterEach
        void restoreTestData() {
            StratConFacilityFactory.loadForTest(new File(TEST_FACILITY_DIRECTORY, "facilitymanifest.json").getPath(),
                  TEST_FACILITY_DIRECTORY);
        }

        private static void write(Path directory, String fileName, String json) throws IOException {
            Files.writeString(directory.resolve(fileName), json, StandardCharsets.UTF_8);
        }

        private void loadOldFormatFiles(Path directory) throws IOException {
            write(directory, "AlliedTestBase.json", """
                  { "owner": "Allied", "displayableName": "Test Base", "facilityType": "TankBase",
                    "userDescription": "Allied tanks.", "localModifiers": ["AlliedTankGarrison.json"],
                    "capturedDefinition": "HostileTestBase.json" }
                  """);
            write(directory, "HostileTestBase.json", """
                  { "owner": "Opposing", "displayableName": "Test Base", "facilityType": "TankBase",
                    "userDescription": "Hostile tanks.", "localModifiers": ["EnemyTankGarrison.json"],
                    "capturedDefinition": "AlliedTestBase.json" }
                  """);
            write(directory, "HostileLoneBase.json", """
                  { "owner": "Opposing", "displayableName": "Lone Base", "facilityType": "AirBase",
                    "scenarioOddsModifier": 10 }
                  """);
            write(directory, "facilitymanifest.json", """
                  { "facilityFileNames": ["AlliedTestBase.json", "HostileTestBase.json", "HostileLoneBase.json"] }
                  """);
            StratConFacilityFactory.loadForTest(directory.resolve("facilitymanifest.json").toString(),
                  directory.toString());
        }

        @Test
        void theTwoSidesOfATypeAreMergedIntoOneDefinition(@TempDir Path directory) throws IOException {
            loadOldFormatFiles(directory);

            StratConFacilityDefinition merged = StratConFacilityFactory.getDefinition("AlliedTestBase");
            assertNotNull(merged);
            assertNotNull(merged.getAlliedProfile());
            assertNotNull(merged.getHostileProfile());
            assertEquals("Allied tanks.", merged.getAlliedProfile().getDescription());
            assertEquals("Hostile tanks.", merged.getHostileProfile().getDescription());
            assertEquals(2, StratConFacilityFactory.getDefinitions().size());
        }

        @Test
        void eitherOldFileNameFindsTheMergedDefinition(@TempDir Path directory) throws IOException {
            loadOldFormatFiles(directory);

            StratConFacilityDefinition merged = StratConFacilityFactory.getDefinition("AlliedTestBase");
            assertSame(merged, StratConFacilityFactory.getDefinition("HostileTestBase.json"));
            assertSame(merged, StratConFacilityFactory.getDefinition("HostileTestBase"));
            assertSame(merged, StratConFacilityFactory.getDefinition("AlliedTestBase.json"));
        }

        @Test
        void aFileWithNoPartnerStaysOneSided(@TempDir Path directory) throws IOException {
            loadOldFormatFiles(directory);

            StratConFacilityDefinition lone = StratConFacilityFactory.getDefinition("HostileLoneBase");
            assertNotNull(lone);
            assertNull(lone.getAlliedProfile());
            assertEquals(10, lone.getProfileFor(ForceAlignment.Opposing).getScenarioOddsModifier());
            assertEquals(0, lone.getProfileFor(ForceAlignment.Allied).getScenarioOddsModifier());
        }

        @Test
        void aOneSidedTypeIsOnlyPlacedForItsOwnSide(@TempDir Path directory) throws IOException {
            loadOldFormatFiles(directory);

            List<StratConFacilityDefinition> alliedTypes =
                  StratConFacilityFactory.getDefinitionsFor(ForceAlignment.Allied);
            assertEquals(1, alliedTypes.size());
            assertEquals("AlliedTestBase", alliedTypes.get(0).getId());
            assertEquals(2, StratConFacilityFactory.getDefinitionsFor(ForceAlignment.Opposing).size());
        }

        @Test
        void aTypeWithBothProfilesIsPreferredWhenMatchingOldSaves(@TempDir Path directory) throws IOException {
            loadOldFormatFiles(directory);

            StratConFacilityDefinition tankBase = StratConFacilityFactory.getDefinitionForType(FacilityType.TankBase);
            assertNotNull(tankBase);
            assertEquals("AlliedTestBase", tankBase.getId());
            assertNull(StratConFacilityFactory.getDefinitionForType(FacilityType.MekBase));
        }
    }
}
