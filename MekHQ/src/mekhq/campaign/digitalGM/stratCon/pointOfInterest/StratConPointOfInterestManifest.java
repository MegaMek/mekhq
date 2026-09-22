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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * A manifest listing the file names of StratCon point of interest definitions, mirroring the facility manifest.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConPointOfInterestManifest {
    private static final MMLogger LOGGER = MMLogger.create(StratConPointOfInterestManifest.class);

    public List<String> pointOfInterestFileNames = new ArrayList<>();

    /**
     * Reads a point of interest manifest from the given file.
     *
     * @param fileName the manifest file to read
     *
     * @return the manifest, or {@code null} if the file does not exist or could not be read (logged)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConPointOfInterestManifest deserialize(String fileName) {
        File inputFile = new File(fileName);
        if (!inputFile.exists()) {
            LOGGER.warn("Specified file {} does not exist", fileName);
            return null;
        }

        try {
            return StratConPointOfInterestJson.fromFile(inputFile, StratConPointOfInterestManifest.class);
        } catch (Exception exception) {
            LOGGER.error("Error deserializing point of interest manifest {}", fileName, exception);
            return null;
        }
    }
}
