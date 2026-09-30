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
package mekhq.campaign.universe.factionStanding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;

/**
 * A manifest listing the file names of the Faction Standing ultimatums the game loads. Each ultimatum is its own JSON
 * file in the same directory as the manifest.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class FactionStandingUltimatumManifest {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandingUltimatumManifest.class);

    /** The name of the manifest file inside the ultimatums directory. */
    public static final String MANIFEST_FILE_NAME = "ultimatummanifest.json";

    public List<String> ultimatumFileNames = new ArrayList<>();

    /**
     * Reads an ultimatum manifest from the given file.
     *
     * @param manifestFile the manifest file to read
     *
     * @return the manifest, or {@code null} if the file does not exist or could not be read (logged)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable FactionStandingUltimatumManifest deserialize(File manifestFile) {
        if (!manifestFile.exists()) {
            LOGGER.warn("Specified file {} does not exist", manifestFile.getPath());
            return null;
        }

        try {
            FactionStandingUltimatumManifest manifest = FactionStandingUltimatumJson.fromFile(manifestFile,
                  FactionStandingUltimatumManifest.class);
            if (manifest.ultimatumFileNames == null) {
                manifest.ultimatumFileNames = new ArrayList<>();
            }
            return manifest;
        } catch (Exception exception) {
            LOGGER.error("Error deserializing ultimatum manifest {}", manifestFile.getPath(), exception);
            return null;
        }
    }

    /**
     * Writes this manifest to the given JSON file.
     *
     * @param outputFile the destination file
     *
     * @return {@code true} if the file was written, {@code false} if an error occurred (logged)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean serialize(File outputFile) {
        try {
            FactionStandingUltimatumJson.toFile(this, outputFile);
            return true;
        } catch (Exception exception) {
            LOGGER.error("Error serializing ultimatum manifest {}", outputFile.getPath(), exception);
            return false;
        }
    }
}
