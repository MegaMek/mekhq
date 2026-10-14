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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import megamek.common.enums.Faction;
import megamek.common.loaders.MekSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnitTechProgressionTest {
    @TempDir
    Path tempDir;

    /**
     * MekFileParser rethrows a unit missing from its zip as a bare NullPointerException. Before this was caught, one
     * such unit failed the whole faction's progression map and the unit market showed no units at all.
     */
    @Test
    void unitMissingFromItsZipYieldsNoProgressionInsteadOfThrowing() throws Exception {
        File zip = tempDir.resolve("units.zip").toFile();
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("Other Unit.mtf"));
            output.write("chassis:Other".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        MekSummary missingUnit = mock(MekSummary.class);
        when(missingUnit.getName()).thenReturn("Marauder MAD-3J");
        when(missingUnit.getSourceFile()).thenReturn(zip);
        when(missingUnit.getEntryName()).thenReturn("Marauder MAD-3J.mtf");

        assertNull(UnitTechProgression.calcTechProgression(missingUnit, Faction.FS));
    }

    @Test
    void sourceOfAZippedUnitNamesTheZipAndTheEntry() {
        File zip = new File("data" + File.separator + "mekfiles" + File.separator + "customs.zip");
        MekSummary unit = mock(MekSummary.class);
        when(unit.getSourceFile()).thenReturn(zip);
        when(unit.getEntryName()).thenReturn("Marauder MAD-3J.mtf");

        assertEquals(zip.getPath() + " (Marauder MAD-3J.mtf)", UnitTechProgression.describeSource(unit));
    }

    @Test
    void sourceOfAUnitInTheHomeDirectoryRedactsTheHomeDirectory() {
        File file = new File(System.getProperty("user.home"), "Marauder MAD-3J.mtf");
        MekSummary unit = mock(MekSummary.class);
        when(unit.getSourceFile()).thenReturn(file);

        String source = UnitTechProgression.describeSource(unit);

        assertEquals("<redacted>" + File.separator + "Marauder MAD-3J.mtf", source);
    }
}
