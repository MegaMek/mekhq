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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Phase 3.2 - the cross-format equivalence harness (the deferred "0.5" safety net). For every shipped template it
 * proves the JSON mapper preserves exactly what XML did, by re-serializing both the XML-loaded object and the
 * JSON-round-tripped object back through the <em>same</em> JAXB writer and asserting the results are byte-identical.
 *
 * <p>Unlike a JSON self round-trip (3.1), this catches a field the JSON layer drops or reshapes <em>consistently</em>:
 * such a field would be missing from the JSON-derived object and therefore from its re-serialized XML, producing a diff
 * against the XML-derived canonical form. This is the machine-proof that gates the rest of the migration.
 */
class ScenarioTemplateCrossFormatTest {

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
    void jsonPreservesEverythingXmlDid(Path templateFile) throws Exception {
        ScenarioTemplate fromXml = ScenarioTemplate.Deserialize(templateFile.toFile());
        assertNotNull(fromXml, "Failed to deserialize " + templateFile);

        // Canonical form: the XML-loaded object serialized back to XML.
        String canonicalXml = toXml(fromXml);

        // Round-trip the same object through JSON, then serialize back to XML through the same writer.
        String json = ScenarioTemplateJson.toJson(fromXml);
        ScenarioTemplate fromJson = ScenarioTemplateJson.fromJson(json, ScenarioTemplate.class);
        assertNotNull(fromJson, "Failed to deserialize JSON for " + templateFile);
        String crossFormatXml = toXml(fromJson);

        assertEquals(canonicalXml, crossFormatXml,
              "JSON round-trip lost or reshaped a field relative to XML: " + templateFile);
    }

    private static String toXml(ScenarioTemplate template) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            template.Serialize(printWriter);
        }
        return stringWriter.toString();
    }
}
