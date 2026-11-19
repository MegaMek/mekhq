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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

import megamek.Version;

/**
 * Copies a campaign save before it is upgraded, so the player can return to it if the upgrade goes wrong.
 *
 * <p>The copy sits next to the save and keeps its extension, so it still appears in the list of campaigns to load.
 * Its name records the version it came from, for example {@code Kestrel's Lancers (backup from 0.50.11).cpnx.gz}.
 * An existing file is never overwritten; a number is added instead.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class CampaignSaveBackup {
    /** Save extensions, longest first so {@code .cpnx.gz} wins over {@code .gz}. */
    private static final List<String> EXTENSIONS = List.of(".cpnx.gz", ".xml.gz", ".cpnx", ".xml", ".gz");
    private static final int MAXIMUM_ATTEMPTS = 1000;

    private CampaignSaveBackup() {}

    /**
     * Picks a free file name for a backup of a save.
     *
     * @param save        the save to back up
     * @param fromVersion the version the save was made in
     *
     * @return a file next to the save that does not exist yet
     *
     * @throws IOException if no free name could be found
     */
    public static File backupFileFor(File save, Version fromVersion) throws IOException {
        String name = save.getName();
        String extension = extensionOf(name);
        String base = name.substring(0, name.length() - extension.length());
        String stem = base + " (backup from " + fromVersion + ")";

        File parent = save.getAbsoluteFile().getParentFile();
        File candidate = new File(parent, stem + extension);
        for (int number = 2; candidate.exists(); number++) {
            if (number > MAXIMUM_ATTEMPTS) {
                throw new IOException("No free backup file name for " + save);
            }
            candidate = new File(parent, stem + " " + number + extension);
        }
        return candidate;
    }

    /**
     * Copies a save to a new backup file next to it.
     *
     * @param save        the save to back up
     * @param fromVersion the version the save was made in
     *
     * @return the backup file
     *
     * @throws IOException if the save can't be read or the copy can't be written
     */
    public static File createBackup(File save, Version fromVersion) throws IOException {
        if (!save.isFile()) {
            throw new IOException("Campaign save not found: " + save);
        }
        File backup = backupFileFor(save, fromVersion);
        Files.copy(save.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        return backup;
    }

    private static String extensionOf(String name) {
        String lowerCase = name.toLowerCase(Locale.ROOT);
        for (String extension : EXTENSIONS) {
            if (lowerCase.endsWith(extension) && (lowerCase.length() > extension.length())) {
                return name.substring(name.length() - extension.length());
            }
        }
        return "";
    }
}
