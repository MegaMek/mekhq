/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import static mekhq.MHQConstants.LIFE_PATHS_DIRECTORY_PATH;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.Utilities;

public class LifePathIO {
    private static final MMLogger LOGGER = MMLogger.create(LifePathIO.class);

    public static Map<UUID, LifePath> loadAllLifePaths() {
        Map<UUID, LifePath> lifePathMap = new HashMap<>(loadAllLifePathsFromDirectory(LIFE_PATHS_DIRECTORY_PATH));

        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (userDirectory != null && !userDirectory.isBlank()) {
            LOGGER.info("Loading LifePaths from user directory {}", userDirectory);
            Map<UUID, LifePath> tempLifePathMap = new HashMap<>(loadAllLifePathsFromDirectory(userDirectory));
            for (UUID id : tempLifePathMap.keySet()) {
                if (lifePathMap.containsKey(id)) {
                    LOGGER.warn("Duplicate LifePath id found in both user directory and main directory. Overwriting.");
                    continue;
                }

                lifePathMap.put(id, tempLifePathMap.get(id));
            }
        }

        validateLifePath(lifePathMap);

        return lifePathMap;
    }

    private static Map<UUID, LifePath> loadAllLifePathsFromDirectory(String directoryPath) {
        Map<UUID, LifePath> lifePathMap = new HashMap<>();
        try {
            FilenameFilter filter = (dir, name) -> name.toLowerCase().endsWith(".json");

            File[] files = Utilities.getAllFiles(directoryPath, filter);

            ObjectMapper objectMapper = new ObjectMapper();

            if (files != null) {
                for (File file : files) {
                    try {
                        LifePath record = objectMapper.readValue(file, LifePath.class);
                        UUID id = record.id();
                        if (id != null) {
                            lifePathMap.put(id, record);
                            LOGGER.info("Loaded LifePath [{}] from {}", id, file.getAbsolutePath());
                        } else {
                            LOGGER.warn("File {} missing valid LifePath id. Skipping.", file.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to load LifePath from {}: {}", file.getAbsolutePath(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load LifePaths from directory {}: {}", directoryPath, e.getMessage());
        }
        return lifePathMap;
    }

    private static void validateLifePath(Map<UUID, LifePath> lifePathMap) {
        for (LifePath lifePath : lifePathMap.values()) {
            Collection<List<UUID>> requirementsLifePaths = lifePath.requirementsLifePath().values();
            for (List<UUID> allIDs : requirementsLifePaths) {
                for (UUID id : allIDs) {
                    if (!lifePathMap.containsKey(id)) {
                        LOGGER.warn("LifePath {} requires non-existent LifePath {}", lifePath.name(), id);
                    }
                }
            }

            Collection<List<UUID>> exclusionsLifePaths = lifePath.exclusionsLifePath().values();
            for (List<UUID> allIDs : exclusionsLifePaths) {
                for (UUID id : allIDs) {
                    if (!lifePathMap.containsKey(id)) {
                        LOGGER.warn("LifePath {} excludes non-existent LifePath {}", lifePath.name(), id);
                    }
                }
            }
        }
    }

}
