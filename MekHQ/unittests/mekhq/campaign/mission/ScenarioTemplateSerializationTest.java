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
package mekhq.campaign.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Serialization safety net for {@link ScenarioTemplate}. This class establishes the shared test harness (Phase 0.1): it
 * locates the shipped scenario template corpus copied into {@code testresources} and confirms every file deserializes.
 * The round-trip idempotence and cross-format equivalence assertions build on this plumbing in later sub-phases.
 */
class ScenarioTemplateSerializationTest {

    /**
     * Location of the scenario template corpus within the module. Mirrors the {@code testresources}-relative resolution
     * used by other data-file tests (e.g. {@code PlanetarySystemYamlIOTest}); Gradle runs tests from the module
     * directory, so this relative path resolves against {@code mekhq/MekHQ/}.
     */
    private static final Path CORPUS_DIRECTORY = Path.of("testresources", "data", "scenariotemplates");

    /**
     * The manifest file is not a scenario template and must never be part of the corpus. It is excluded when the
     * fixtures are staged, but is guarded here as well so an accidental copy fails loudly.
     */
    private static final String SCENARIO_MANIFEST_FILE_NAME = "ScenarioManifest.xml";

    /**
     * Provides every shipped scenario template file in the corpus, sorted for stable ordering.
     *
     * @return a stream of paths to the {@code .xml} template fixtures
     */
    private static Stream<Path> shippedScenarioTemplateFiles() throws IOException {
        try (Stream<Path> paths = Files.list(CORPUS_DIRECTORY)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".xml"))
                         .filter(path -> !path.getFileName().toString().equals(SCENARIO_MANIFEST_FILE_NAME))
                         .sorted(Comparator.comparing(Path::toString))
                         .toList()
                         .stream();
        }
    }

    @Test
    void corpusDirectoryExistsAndIsPopulated() throws IOException {
        assertTrue(Files.isDirectory(CORPUS_DIRECTORY),
              "Scenario template corpus directory is missing: " + CORPUS_DIRECTORY.toAbsolutePath());
        assertFalse(shippedScenarioTemplateFiles().findAny().isEmpty(),
              "Scenario template corpus is empty: " + CORPUS_DIRECTORY.toAbsolutePath());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shippedScenarioTemplateFiles")
    void shippedTemplateDeserializes(Path templateFile) {
        ScenarioTemplate template = ScenarioTemplate.Deserialize(templateFile.toFile());

        assertNotNull(template, "Failed to deserialize scenario template: " + templateFile);
        assertNotNull(template.name, "Deserialized template has a null name: " + templateFile);
        assertFalse(template.name.isBlank(), "Deserialized template has a blank name: " + templateFile);
    }

    /**
     * Round-trip idempotence guard (Phase 0.2). The first marshal normalizes any legacy or formatting quirks present in
     * the shipped file, so we compare the serialized form of one round-trip against the serialized form of a second
     * round-trip. Any drift means serialization is not stable: a dropped field, a re-ordered element, or a value that
     * does not survive a load/save cycle. This is the baseline that later phases (correctness fixes, the XML to JSON
     * migration) must not regress.
     *
     * @param templateFile a shipped template fixture
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("shippedScenarioTemplateFiles")
    void shippedTemplateSurvivesRoundTripUnchanged(Path templateFile, @TempDir Path tempDir) throws IOException {
        ScenarioTemplate firstLoad = ScenarioTemplate.Deserialize(templateFile.toFile());
        assertNotNull(firstLoad, "Failed to deserialize scenario template: " + templateFile);

        String firstSerialization = serialize(firstLoad);

        ScenarioTemplate secondLoad = deserialize(firstSerialization, tempDir.resolve("round-trip.xml"));
        assertNotNull(secondLoad, "Failed to re-deserialize round-tripped template: " + templateFile);

        String secondSerialization = serialize(secondLoad);

        assertEquals(firstSerialization, secondSerialization,
              "Scenario template serialization is not idempotent: " + templateFile);
    }

    /**
     * Serializes a template to its XML string form using the fragment writer used by the editor.
     */
    private static String serialize(ScenarioTemplate template) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            template.Serialize(printWriter);
        }
        return stringWriter.toString();
    }

    /**
     * Writes the given serialized XML to a scratch file and deserializes it back through the production file path.
     */
    private static ScenarioTemplate deserialize(String serializedXml, Path scratchFile) throws IOException {
        Files.writeString(scratchFile, serializedXml);
        return ScenarioTemplate.Deserialize(scratchFile.toFile());
    }
}
