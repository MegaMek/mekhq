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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Phase 3.3: verifies the model's format-aware file IO. {@code Serialize(File)} now writes JSON, and
 * {@code Deserialize(File)} sniffs content to read either JSON or (legacy) XML.
 */
class ScenarioTemplateFormatIOTest {

    private static final Path CORPUS_DIRECTORY = Path.of("testresources", "data", "scenariotemplates");

    @Test
    void serializeFileWritesJsonThatReloads(@TempDir Path tempDir) throws Exception {
        ScenarioTemplate original = ScenarioTemplate.Deserialize(CORPUS_DIRECTORY.resolve("Assassination.xml")
                                                                       .toFile());
        assertNotNull(original);

        Path out = tempDir.resolve("Assassination.json");
        original.Serialize(out.toFile());

        // The written file leads with the '#' license header, then the JSON object (not XML).
        String written = Files.readString(out);
        assertTrue(written.startsWith("# MegaMek Data (C)"), () -> "expected leading license header, got: " + written);
        assertTrue(written.contains("\n{"), () -> "expected a JSON object after the header, got: " + written);

        // And it reloads through the format-detecting deserializer to an equivalent template (compared via canonical
        // XML, since that is the format-neutral reference).
        ScenarioTemplate reloaded = ScenarioTemplate.Deserialize(out.toFile());
        assertNotNull(reloaded);
        assertEquals(toXml(original), toXml(reloaded));
    }

    @Test
    void deserializeStillReadsLegacyXml() {
        // A .xml file (and, since detection is by content, the name does not matter) still loads.
        ScenarioTemplate fromXml = ScenarioTemplate.Deserialize(CORPUS_DIRECTORY.resolve("Breakout.xml").toFile());
        assertNotNull(fromXml);
        assertEquals("Breakout", fromXml.name);
    }

    @Test
    void detectionIsByContentNotExtension(@TempDir Path tempDir) throws Exception {
        // Write JSON but name it .xml; content sniffing must still parse it as JSON.
        ScenarioTemplate original = ScenarioTemplate.Deserialize(CORPUS_DIRECTORY.resolve("Breakout.xml").toFile());
        String json = ScenarioTemplateJson.toJson(original);
        Path misnamed = tempDir.resolve("actually-json.xml");
        Files.writeString(misnamed, json);

        ScenarioTemplate reloaded = ScenarioTemplate.Deserialize(misnamed.toFile());
        assertNotNull(reloaded);
        assertEquals(toXml(original), toXml(reloaded));
    }

    @Test
    void embeddingWritePathStaysXml() {
        // The PrintWriter serializer is what AtBDynamicScenario embeds into campaign saves. It must remain XML even
        // though standalone files are now written as JSON, since a template is nested inside the XML campaign save.
        ScenarioTemplate template = ScenarioTemplate.Deserialize(CORPUS_DIRECTORY.resolve("Breakout.xml").toFile());
        String embedded = toXml(template);

        assertTrue(embedded.stripLeading().startsWith("<"), () -> "embedding must stay XML, got: " + embedded);
        assertTrue(embedded.contains("<ScenarioTemplate>"), () -> "expected XML root element, got: " + embedded);
    }

    private static String toXml(ScenarioTemplate template) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            template.Serialize(printWriter);
        }
        return stringWriter.toString();
    }
}
