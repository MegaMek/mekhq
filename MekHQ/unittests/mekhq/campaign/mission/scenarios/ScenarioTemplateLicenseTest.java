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
package mekhq.campaign.mission.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScenarioTemplateLicenseTest {

    @Test
    void savedTemplateBeginsWithLicenseAndReloadsCleanly(@TempDir Path tempDir) throws IOException {
        ScenarioTemplate template = new ScenarioTemplate();
        template.name = "Licensed";

        File out = tempDir.resolve("licensed.json").toFile();
        template.Serialize(out);

        String content = Files.readString(out.toPath());
        assertTrue(content.startsWith("# MegaMek Data (C)"), "saved JSON should begin with the '#' license header");
        assertTrue(content.contains("CC BY-NC-SA 4.0"), "the license text should be the MegaMek Data notice");
        assertTrue(content.contains("affiliated with Microsoft."), "the license header should be complete");
        int objectStart = content.indexOf('{');
        assertTrue(objectStart > 0 && content.substring(0, objectStart).contains("# MegaMek Data"),
              "the license header must precede the JSON object");

        ScenarioTemplate reloaded = ScenarioTemplate.Deserialize(out);
        assertNotNull(reloaded, "a template carrying a _license block should still deserialize");
        assertEquals("Licensed", reloaded.name, "the license block must not disturb the model on read");
    }
}
