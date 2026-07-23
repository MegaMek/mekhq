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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Phase 3.1: exercises the full {@link ScenarioTemplateJson} mapper against every shipped template by loading the XML,
 * serializing to JSON, reloading, and asserting the JSON is stable across a second round-trip. This surfaces mapper
 * fidelity issues (enum handling, the getter-only role collection, the forces map) on real data. The stronger
 * XML-vs-JSON equivalence check is Phase 3.2.
 */
class ScenarioTemplateJsonRoundTripTest {

    private static final Path CORPUS_DIRECTORY = Path.of("testresources", "data", "scenariotemplates");

    private static Stream<Path> shippedScenarioTemplateFiles() throws IOException {
        try (Stream<Path> paths = Files.list(CORPUS_DIRECTORY)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".xml"))
                         .filter(path -> !path.getFileName().toString().equals("ScenarioManifest.xml"))
                         .sorted(Comparator.comparing(Path::toString))
                         .toList()
                         .stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shippedScenarioTemplateFiles")
    void templateRoundTripsThroughJson(Path templateFile) throws Exception {
        ScenarioTemplate fromXml = ScenarioTemplate.Deserialize(templateFile.toFile());
        assertNotNull(fromXml, "Failed to deserialize " + templateFile);

        String firstJson = ScenarioTemplateJson.toJson(fromXml);
        ScenarioTemplate fromJson = ScenarioTemplateJson.fromJson(firstJson, ScenarioTemplate.class);
        assertNotNull(fromJson, "Failed to deserialize JSON for " + templateFile);

        String secondJson = ScenarioTemplateJson.toJson(fromJson);

        assertEquals(firstJson, secondJson, "JSON serialization is not stable for " + templateFile);
    }
}
