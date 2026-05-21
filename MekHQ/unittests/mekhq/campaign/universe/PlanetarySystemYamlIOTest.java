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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.preference.PreferenceManager;
import mekhq.MHQConstants;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

class PlanetarySystemYamlIOTest {

    private static final LocalDate EVENT_DATE = LocalDate.of(3000, 1, 1);

    private static final String VERSIONED_SYSTEM = """
          id: Version Test
          sucsId: 42
          xcood: 1.25
          ycood: -2.5
          spectralType:
            source: Source Book, pg. 1
            version: 2026.03.02
            value: G2V
          primarySlot: 1
          event:
            - date: '3000-01-01'
              zenithCharge: yes
          planet:
            - name:
                source: Source Book, pg. 2
                version: 2026.03.02
                value: Version Test Prime
              type: TERRESTRIAL
              orbitalDist: 1.0
              sysPos: 1
              pressure: STANDARD
              atmosphere: BREATHABLE
              gravity: 1.0
              diameter: 12000
              density: 5.5
              dayLength: 24
              yearLength: 1.0
              temperature: 20
              water: 70
              event:
                - date: '3000-01-01'
                  faction:
                    source: Source Book, pg. 3
                    version: 2026.03.02
                    value:
                      - FS
                  socioIndustrial:
                    source: Source Book, pg. 4
                    version: 2026.03.02
                    value: C-C-C-C-C
          """;

    @Test
    void sourceableValueVersionSurvivesRoundTrip() throws Exception {
        PlanetarySystem system = readSystem(VERSIONED_SYSTEM);

        assertEquals("2026.03.02", system.getSourcedStar().getVersion());

        Planet planet = system.getPrimaryPlanet();
        assertEquals("2026.03.02", planet.getSourcedName(EVENT_DATE).getVersion());
        assertEquals(List.of("FS"), planet.getSourcedFactions(EVENT_DATE).getValue());
        assertEquals("2026.03.02", planet.getSourcedFactions(EVENT_DATE).getVersion());

        String savedYaml = writeSystem(system);
        assertTrue(savedYaml.contains("version:"));
        assertTrue(savedYaml.contains("2026.03.02"));
        assertTrue(savedYaml.contains("sucsId: 42"));
        assertFalse(savedYaml.contains("factions:"));
        assertFalse(savedYaml.contains("currentEvents:"));

        PlanetarySystem reloaded = readSystem(savedYaml);
        Planet reloadedPlanet = reloaded.getPrimaryPlanet();
        assertEquals("2026.03.02", reloaded.getSourcedStar().getVersion());
        assertEquals("Version Test Prime", reloadedPlanet.getName(EVENT_DATE));
        assertEquals(List.of("FS"), reloadedPlanet.getFactions(EVENT_DATE));
        assertEquals("C-C-C-C-C", reloadedPlanet.getSocioIndustrial(EVENT_DATE).toString());
        assertTrue(reloaded.isZenithCharge(EVENT_DATE));
    }

    @Test
    void savedYamlDoesNotIncludeDerivedGetterProperties() throws Exception {
        PlanetarySystem system = readSystem(VERSIONED_SYSTEM);

        String savedYaml = writeSystem(system);

        assertFalse(savedYaml.contains("\nplanets:"));
        assertFalse(savedYaml.contains("\nevents:"));
        assertFalse(savedYaml.contains("\nstar:"));
        assertFalse(savedYaml.contains("\nrechargeStationsText:"));
        assertFalse(savedYaml.contains("\nparentSystem:"));
        assertFalse(savedYaml.contains("event: []"));
    }

    @Test
    void writeAndCopyDoNotPopulateSerializationListFields() throws Exception {
        PlanetarySystem system = readSystem(VERSIONED_SYSTEM);
        Planet planet = system.getPrimaryPlanet();

        assertNull(getPrivateField(system, "planetList"));
        assertNull(getPrivateField(system, "eventList"));
        assertNull(getPrivateField(planet, "eventList"));

        String savedYaml = writeSystem(system);
        assertTrue(savedYaml.contains("\nplanet:"));
        assertTrue(savedYaml.contains("\nevent:"));
        assertNull(getPrivateField(system, "planetList"));
        assertNull(getPrivateField(system, "eventList"));
        assertNull(getPrivateField(planet, "eventList"));

        assertNotNull(PlanetarySystemYamlIO.copy(system));
        assertNull(getPrivateField(system, "planetList"));
        assertNull(getPrivateField(system, "eventList"));
        assertNull(getPrivateField(planet, "eventList"));
    }

    @Test
    void sucsIdNaSentinelLoadsAsNull() throws Exception {
        PlanetarySystem system = readSystem(VERSIONED_SYSTEM.replace("sucsId: 42", "sucsId: .na"));

        assertNull(system.getSucsId());
        assertFalse(writeSystem(system).contains("sucsId:"));
    }

    @Test
    void editedPlanetaryEventRoundTripsThroughYaml() throws Exception {
        PlanetarySystem system = readSystem(VERSIONED_SYSTEM);
        Planet planet = system.getPrimaryPlanet();

        Planet.PlanetaryEvent event = new Planet.PlanetaryEvent();
        event.date = LocalDate.of(3050, 1, 1);
        event.message = "Edited by Planetary Data Editor";
        event.faction = SourceableValue.of("MekHQ GM", null, List.of("LC"));
        event.population = SourceableValue.of("MekHQ GM", null, 123456789L);
        event.hpg = SourceableValue.of("MekHQ GM", null, HPGRating.A);
        event.socioIndustrial = SourceableValue.of("MekHQ GM", null, SocioIndustrialData.parse("B-C-D-F-A"));
        planet.putEvent(event);

        PlanetarySystem reloadedSystem = readSystem(writeSystem(system));
        Planet reloadedPlanet = reloadedSystem.getPrimaryPlanet();

        assertEquals(List.of("LC"), reloadedPlanet.getFactions(event.date));
        assertEquals(123456789L, reloadedPlanet.getPopulation(event.date));
        assertEquals(HPGRating.A, reloadedPlanet.getHPG(event.date));
        assertEquals("B-C-D-F-A", reloadedPlanet.getSocioIndustrial(event.date).toString());
        assertEquals("MekHQ GM", reloadedPlanet.getSourcedFactions(event.date).getSource());
    }

    @Test
    void socioIndustrialDataRoundTripsDisplayNamesThroughYaml() throws Exception {
        PlanetarySystem system = readSystem(VERSIONED_SYSTEM);
        Planet planet = system.getPrimaryPlanet();

        Planet.PlanetaryEvent event = new Planet.PlanetaryEvent();
        event.date = LocalDate.of(3050, 1, 1);
        event.socioIndustrial = SourceableValue.of(new SocioIndustrialData(PlanetarySophistication.ADVANCED,
              PlanetaryRating.A, PlanetaryRating.B, PlanetaryRating.C, PlanetaryRating.D));
        planet.putEvent(event);

                Planet.PlanetaryEvent regressedEvent = new Planet.PlanetaryEvent();
                regressedEvent.date = LocalDate.of(3060, 1, 1);
                regressedEvent.socioIndustrial = SourceableValue.of(new SocioIndustrialData(PlanetarySophistication.REGRESSED,
              PlanetaryRating.F, PlanetaryRating.F, PlanetaryRating.F, PlanetaryRating.F));
                planet.putEvent(regressedEvent);

        PlanetarySystem reloadedSystem = readSystem(writeSystem(system));
                Planet reloadedPlanet = reloadedSystem.getPrimaryPlanet();

                assertEquals("Advanced-A-B-C-D", reloadedPlanet.getSocioIndustrial(event.date).toString());
                assertEquals("Regressed-F-F-F-F", reloadedPlanet.getSocioIndustrial(regressedEvent.date).toString());
    }

    @Test
    void planetEventMutationUpdatesDateDependentValues() throws Exception {
        PlanetarySystem system = readSystem(VERSIONED_SYSTEM);
        Planet planet = system.getPrimaryPlanet();
        LocalDate editDate = LocalDate.of(3050, 1, 1);

        Planet.PlanetaryEvent event = new Planet.PlanetaryEvent();
        event.date = editDate;
        event.faction = SourceableValue.of(List.of("LC"));
        planet.putEvent(event);

        assertEquals(List.of("LC"), planet.getFactions(editDate));

        assertTrue(planet.removeEvent(editDate));
        assertEquals(List.of("FS"), planet.getFactions(editDate));

        planet.replaceEvents(List.of(event));
        assertEquals(1, count(planet.getEvents()));
        assertEquals(List.of("LC"), planet.getFactions(editDate));
    }

    @Test
    void campaignXmlOverridesRoundTripEmbeddedYaml() throws Exception {
        PlanetarySystem system = readSystem(VERSIONED_SYSTEM);

        String xml = writeCampaignXmlOverrides(List.of(system));

        assertTrue(xml.contains("<planetarySystemOverrides>"));
        assertTrue(xml.contains("<planetarySystemOverride id=\"Version Test\"><![CDATA["));
        assertTrue(xml.contains("sucsId: 42"));

        Document document = MHQXMLUtility.newSafeDocumentBuilder()
                                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        List<PlanetarySystem> overrides = PlanetarySystemCampaignXmlIO.parse(document.getDocumentElement());

        assertEquals(1, overrides.size());
        PlanetarySystem reloaded = overrides.getFirst();
        assertEquals("Version Test", reloaded.getId());
        assertEquals("2026.03.02", reloaded.getSourcedStar().getVersion());
        assertEquals(List.of("FS"), reloaded.getPrimaryPlanet().getFactions(EVENT_DATE));
    }

    @Test
    void campaignXmlOverrideIdMustMatchEmbeddedYamlId() {
        StringBuilder xml = new StringBuilder();
        xml.append("<planetarySystemOverrides>\n");
        xml.append("  <planetarySystemOverride id=\"XML Id\"><![CDATA[\n");
        xml.append(VERSIONED_SYSTEM);
        xml.append("  ]]></planetarySystemOverride>\n");
        xml.append("</planetarySystemOverrides>\n");

        IOException exception = assertThrows(IOException.class, () -> parseCampaignXmlOverrides(xml.toString()));

        assertTrue(exception.getMessage().contains("does not match YAML id"));
    }

    @Test
    void campaignXmlOverridesSkipEmptyCollections() {
        assertEquals("", writeCampaignXmlOverrides(List.of()));
    }

    @Test
    void loadDefaultIncludesUserDirectorySystems(final @TempDir Path userDirectory) throws Exception {
        Path customSystemsDirectory = userDirectory.resolve(MHQConstants.PLANETARY_SYSTEM_DIRECTORY_PATH);
        Files.createDirectories(customSystemsDirectory);
        Files.writeString(customSystemsDirectory.resolve("User Custom.yml"), """
              id: User Custom
              xcood: 9999.0
              ycood: 9999.0
              spectralType: G2V
              primarySlot: 1
              planet:
                - name: User Custom Prime
                  type: TERRESTRIAL
                  orbitalDist: 1.0
                  sysPos: 1
              """, StandardCharsets.UTF_8);

        String originalUserDir = PreferenceManager.getClientPreferences().getUserDir();
        try {
            PreferenceManager.getClientPreferences().setUserDir(userDirectory.toString());

            Systems systems = Systems.loadDefault();

            assertNotNull(systems.getSystemById("User Custom"));
        } finally {
            PreferenceManager.getClientPreferences().setUserDir(originalUserDir == null ? "" : originalUserDir);
        }
    }

    @ParameterizedTest
    @MethodSource("realPlanetarySystemFiles")
    void realPlanetarySystemFilesReloadAfterRoundTrip(Path sourcePath) throws Exception {
        PlanetarySystem originalSystem;
        try (var inputStream = Files.newInputStream(sourcePath)) {
            originalSystem = PlanetarySystemYamlIO.read(inputStream);
        }

        PlanetarySystem reloadedSystem = readSystem(writeSystem(originalSystem));

        assertSystemsEquivalent(originalSystem, reloadedSystem);
    }

    private static Stream<Path> realPlanetarySystemFiles() throws IOException {
        Path testSystemsDirectory = Path.of("testresources", "data", "universe", "planetary_systems",
              "canon_systems");
        try (Stream<Path> paths = Files.list(testSystemsDirectory)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".yml"))
                         .sorted(Comparator.comparing(Path::toString))
                         .toList()
                         .stream();
        }
    }

    private static void assertSystemsEquivalent(PlanetarySystem expected, PlanetarySystem actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getSucsId(), actual.getSucsId());
        assertEquals(expected.getX(), actual.getX());
        assertEquals(expected.getY(), actual.getY());
        assertEquals(expected.getPrimaryPlanetPosition(), actual.getPrimaryPlanetPosition());
        assertEquals(count(expected.getEvents()), count(actual.getEvents()));
        assertEquals(expected.getPlanets().size(), actual.getPlanets().size());

        for (Planet expectedPlanet : expected.getPlanets()) {
            Planet actualPlanet = actual.getPlanet(expectedPlanet.getSystemPosition());
            assertNotNull(actualPlanet);
            assertEquals(expectedPlanet.getId(), actualPlanet.getId());
            assertEquals(expectedPlanet.getSystemPosition(), actualPlanet.getSystemPosition());
            assertEquals(expectedPlanet.getSourcedPlanetType().getValue(), actualPlanet.getSourcedPlanetType()
                      .getValue());
            assertEquals(count(expectedPlanet.getEvents()), count(actualPlanet.getEvents()));
        }
    }

    private static int count(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    private static Object getPrivateField(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static PlanetarySystem readSystem(String yaml) throws IOException {
        return PlanetarySystemYamlIO.read(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String writeSystem(PlanetarySystem system) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PlanetarySystemYamlIO.write(system, outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private static List<PlanetarySystem> parseCampaignXmlOverrides(String xml) throws Exception {
        Document document = MHQXMLUtility.newSafeDocumentBuilder()
                                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return PlanetarySystemCampaignXmlIO.parse(document.getDocumentElement());
    }

    private static String writeCampaignXmlOverrides(Collection<PlanetarySystem> systems) {
        StringWriter output = new StringWriter();
        try (PrintWriter writer = new PrintWriter(output)) {
            PlanetarySystemCampaignXmlIO.writeToXML(writer, 0, systems);
        }
        return output.toString();
    }
}

