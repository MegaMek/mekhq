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

import static mekhq.MHQConstants.LIFE_PATHS_DEFAULT_DIRECTORY_PATH;
import static mekhq.MHQConstants.LIFE_PATHS_USER_DIRECTORY_PATH;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

public class LifePathIO {
    private static final MMLogger LOGGER = MMLogger.create(LifePathIO.class);

    public static Map<UUID, LifePath> loadAllLifePaths() {
        LOGGER.info("Loading all LifePaths");
        Map<UUID, LifePath> lifePathMap = new HashMap<>(loadAllLifePathsFromDirectory(LIFE_PATHS_DEFAULT_DIRECTORY_PATH));

        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (userDirectory == null || userDirectory.isBlank()) {
            userDirectory = LIFE_PATHS_DEFAULT_DIRECTORY_PATH;
        } else {
            userDirectory = userDirectory + LIFE_PATHS_USER_DIRECTORY_PATH;
        }

        LOGGER.info("Loading LifePaths from user directory {}", LIFE_PATHS_USER_DIRECTORY_PATH);
        Map<UUID, LifePath> tempLifePathMap = new HashMap<>(loadAllLifePathsFromDirectory(userDirectory));
        for (UUID id : tempLifePathMap.keySet()) {
            if (lifePathMap.containsKey(id)) {
                LOGGER.warn("Overriding {} with {}.", lifePathMap.get(id).name(),
                      tempLifePathMap.get(id).name());
                continue;
            }

            lifePathMap.put(id, tempLifePathMap.get(id));
        }

        validateLifePath(lifePathMap);

        return lifePathMap;
    }

    private static Map<UUID, LifePath> loadAllLifePathsFromDirectory(String directoryPath) {
        Map<UUID, LifePath> lifePathMap = new HashMap<>();
        try {
            LOGGER.info("Loading LifePaths from directory and its subdirectories: {}", directoryPath);

            ObjectMapper objectMapper = new ObjectMapper();

            Path startPath = Paths.get(directoryPath);
            if (Files.exists(startPath)) {
                try (Stream<Path> paths = Files.walk(startPath)) {
                    List<File> jsonFiles = paths
                                                 .filter(Files::isRegularFile)
                                                 .filter(p -> p.getFileName()
                                                                    .toString()
                                                                    .toLowerCase()
                                                                    .endsWith(".json"))
                                                 .map(Path::toFile)
                                                 .toList();

                    for (File file : jsonFiles) {
                        try {
                            LifePath record = objectMapper.readValue(file, LifePath.class);
                            UUID id = record.id();
                            if (id != null) {
                                if (lifePathMap.containsKey(id)) {
                                    LOGGER.warn("Duplicate LifePath id found. Overwriting {} with {}.",
                                          lifePathMap.get(id).name(), record.name());
                                }
                                lifePathMap.put(id, record);
                                LOGGER.debug("Loaded LifePath [{}] from {}", record.name(), file.getPath());
                            } else {
                                LOGGER.warn("File {} missing valid LifePath id. Skipping.", file.getPath());
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to load LifePath from {}: {}", file.getPath(), e.getMessage());
                        }
                    }
                }
            } else {
                LOGGER.warn("Directory {} does not exist.", directoryPath);
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
