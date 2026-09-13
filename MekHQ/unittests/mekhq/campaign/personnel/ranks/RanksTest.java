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
package mekhq.campaign.personnel.ranks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the rank system export in {@link Ranks}.
 *
 * <p>The export writes the user's custom rank systems to {@code userdata/data/universe/ranks.xml} on launch, so a
 * failure part-way through it destroys the user's data rather than just logging.</p>
 *
 * @since 0.51.01
 */
class RanksTest {
    @Test
    void testExportRankSystemsToFile_WritesTheLicenceHeaderAndTheRootElement(@TempDir Path tempDir)
          throws IOException {
        File file = tempDir.resolve("ranks.xml").toFile();

        Ranks.exportRankSystemsToFile(file, List.of());

        String contents = Files.readString(file.toPath());

        assertTrue(contents.startsWith("<?xml"), "The export should begin with the XML declaration.");
        // The header comes from a resource bundle. When that bundle was renamed and the exporter kept asking for the
        // old name, the lookup threw after the declaration was written, and the user's file was left holding
        // nothing else: every launch wiped their rank systems.
        assertTrue(contents.contains("MegaMek Data (C)"),
              "The MegaMek Data licence header should follow the declaration.");
        assertFalse(contents.contains("!Legal."),
              "A missing resource key should not be written into the file as its marker text.");
        assertTrue(contents.contains("<rankSystems"),
              "The root element should be written even when there are no rank systems to export.");
        assertTrue(contents.contains("</rankSystems>"), "The root element should be closed.");
    }
}
