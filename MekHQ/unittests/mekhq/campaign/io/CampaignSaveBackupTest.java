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
package mekhq.campaign.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import megamek.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CampaignSaveBackupTest {
    private static final Version FROM = new Version("0.50.11");

    @TempDir
    Path folder;

    @Test
    void keepsTheCompressedExtension() throws IOException {
        File save = folder.resolve("Kestrel's Lancers.cpnx.gz").toFile();
        assertEquals("Kestrel's Lancers (backup from 0.50.11).cpnx.gz",
              CampaignSaveBackup.backupFileFor(save, FROM).getName());
    }

    @Test
    void handlesPlainAndUnknownExtensions() throws IOException {
        assertEquals("Lancers (backup from 0.50.11).cpnx",
              CampaignSaveBackup.backupFileFor(folder.resolve("Lancers.cpnx").toFile(), FROM).getName());
        assertEquals("Lancers.sav (backup from 0.50.11)",
              CampaignSaveBackup.backupFileFor(folder.resolve("Lancers.sav").toFile(), FROM).getName());
    }

    @Test
    void leavesOutTheBuildExtra() {
        Version nightly = new Version("0.50.11");
        nightly.setExtra("nightly-abc1234");
        assertEquals("0.50.11", CampaignSaveBackup.versionLabel(nightly));
    }

    @Test
    void keepsThePointReleaseRevision() {
        assertEquals("0.49.19.1", CampaignSaveBackup.versionLabel(new Version("0.49.19.1")));
    }

    @Test
    void copiesTheSaveWithoutOverwriting() throws IOException {
        Path save = folder.resolve("Lancers.cpnx.gz");
        byte[] contents = { 1, 2, 3, 4 };
        Files.write(save, contents);

        File first = CampaignSaveBackup.createBackup(save.toFile(), FROM);
        File second = CampaignSaveBackup.createBackup(save.toFile(), FROM);

        assertEquals("Lancers (backup from 0.50.11).cpnx.gz", first.getName());
        assertEquals("Lancers (backup from 0.50.11) 2.cpnx.gz", second.getName());
        assertArrayEquals(contents, Files.readAllBytes(first.toPath()));
        assertArrayEquals(contents, Files.readAllBytes(second.toPath()));
    }

    @Test
    void refusesAMissingSave() {
        assertThrows(IOException.class,
              () -> CampaignSaveBackup.createBackup(folder.resolve("missing.cpnx").toFile(), FROM));
    }
}
