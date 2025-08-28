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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.gui.GUI;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.io.FileType;

public class LifePathIO {
    final static String RESOURCE_BUNDLE = "mekhq.resources.LifePathIO";
    private static final MMLogger LOGGER = MMLogger.create(LifePathIO.class);

    public static Map<UUID, LifePath> loadAllLifePaths(Campaign campaign) {
        LOGGER.info("Loading all LifePaths");
        Map<UUID, LifePath> lifePathMap =
              new HashMap<>(loadAllLifePathsFromDirectory(campaign, LIFE_PATHS_DEFAULT_DIRECTORY_PATH, true));

        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (userDirectory == null || userDirectory.isBlank()) {
            userDirectory = LIFE_PATHS_DEFAULT_DIRECTORY_PATH;
        } else {
            userDirectory = userDirectory + LIFE_PATHS_USER_DIRECTORY_PATH;
        }

        LOGGER.info("Loading LifePaths from user directory {}", LIFE_PATHS_USER_DIRECTORY_PATH);
        Map<UUID, LifePath> tempLifePathMap = new HashMap<>(loadAllLifePathsFromDirectory(campaign, userDirectory,
              false));
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

    private static Map<UUID, LifePath> loadAllLifePathsFromDirectory(Campaign campaign, String directoryPath,
          boolean silentlyUpgrade) {
        Map<UUID, LifePath> lifePathMap = new HashMap<>();
        Map<UUID, String> outOfDateLifePaths = new HashMap<>();

        try {
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
                                    LOGGER.warn("Duplicate LifePath id found ({}). Overwriting {} with {}.",
                                          id, lifePathMap.get(id).name(), record.name());
                                }
                                lifePathMap.put(id, record);

                                if (record.version().isLowerThan(MHQConstants.VERSION)) {
                                    outOfDateLifePaths.put(id, file.getParent());
                                    LOGGER.info("LifePath [{}] is out of date.", record.name());
                                }

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
                LOGGER.warn("Directory does not exist.");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load LifePaths from directory {}", e.getMessage());
        }

        if (!outOfDateLifePaths.isEmpty()) {
            boolean isUpgrade = silentlyUpgrade || triggerConfirmationDialog(campaign, directoryPath,
                  outOfDateLifePaths);

            if (isUpgrade) {
                for (Map.Entry<UUID, String> entry : outOfDateLifePaths.entrySet()) {
                    LifePath record = lifePathMap.get(entry.getKey());
                    writeToJSONWithoutDialog(record, entry.getValue());
                }
            }
        }

        return lifePathMap;
    }

    private static boolean triggerConfirmationDialog(Campaign campaign, String directoryPath,
          Map<UUID, String> outOfDateLifePaths) {
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.notice",
              outOfDateLifePaths.size(), directoryPath);
        String cancelOption = getTextAt(RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.button.cancel");
        String confirmOption = getTextAt(RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.button.confirm");
        String warning = getFormattedTextAt(RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.warning");

        boolean isUpgrade = false;
        boolean dialogConfirmed = false;
        while (!dialogConfirmed) {
            ImmersiveDialogSimple decisionDialog = new ImmersiveDialogSimple(campaign,
                  null,
                  null,
                  message,
                  List.of(cancelOption, confirmOption),
                  warning,
                  null,
                  false);
            isUpgrade = decisionDialog.getDialogChoice() == 1;

            ImmersiveDialogConfirmation confirmDialog = new ImmersiveDialogConfirmation(campaign);
            dialogConfirmed = confirmDialog.wasConfirmed();
        }
        return isUpgrade;
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

    public static Optional<LifePath> loadFromJSONWithDialog() {
        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (userDirectory == null || userDirectory.isBlank()) {
            userDirectory = LIFE_PATHS_DEFAULT_DIRECTORY_PATH;
        } else {
            userDirectory = Paths.get(userDirectory, LIFE_PATHS_USER_DIRECTORY_PATH).toString();
        }

        Optional<File> fileOpt = GUI.fileDialogOpen(
              null,
              getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.io.load"),
              FileType.JSON,
              userDirectory
        );

        if (fileOpt.isPresent()) {
            File file = fileOpt.get();
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                LifePath record = objectMapper.readValue(file, LifePath.class);
                LOGGER.info("Loaded LifePathRecord from: {}", file.getAbsolutePath());
                return Optional.of(record);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        } else {
            LOGGER.info("Load operation cancelled by user.");
        }

        return Optional.empty();
    }

    public static void writeToJSONWithoutDialog(LifePath record, String directory) {
        String baseName = record.name();
        if (baseName == null || baseName.isBlank()) {
            baseName = "unnamed_life_path";
        } else {
            baseName = baseName.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "");
            baseName = baseName.replaceAll("[. ]+$", "");
            baseName = baseName.replaceAll("^_|_$", "");
        }

        // Ensure the directory exists
        File dir = new File(directory);
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.error("Could not create directory: {}", directory);
            return;
        }

        // File path
        File file = new File(dir, baseName + ".json");
        saveAction(record, file);
    }

    public static void saveAction(LifePath record, File file) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            DefaultIndenter indenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF); // 4 spaces
            printer.indentObjectsWith(indenter);
            printer.indentArraysWith(indenter);
            objectMapper.writer(printer).writeValue(file, record);
            LOGGER.info("Wrote LifePathRecord JSON to: {}", file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to write LifePathRecord JSON: {}", e.getMessage());
        }
    }

    public static void writeToJSONWithDialog(LifePath record) {
        String baseName = record.name();
        if (baseName.isBlank()) {
            baseName = "unnamed_life_path";
        } else {
            baseName = baseName.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "");
            baseName = baseName.replaceAll("[. ]+$", "");
            baseName = baseName.replaceAll("^_|_$", "");
        }

        // Pick an initial directory (preferably the user directory or fallback)
        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();
        if (userDirectory == null || userDirectory.isBlank()) {
            userDirectory = LIFE_PATHS_DEFAULT_DIRECTORY_PATH;
        } else {
            userDirectory = userDirectory + LIFE_PATHS_USER_DIRECTORY_PATH;
        }

        Optional<File> dialogFile = GUI.fileDialogSave(
              null,
              getTextAt(RESOURCE_BUNDLE, "LifePathBuilderDialog.io.save"),
              FileType.JSON,
              userDirectory,
              baseName
        );

        if (dialogFile.isPresent()) {
            File file = dialogFile.get();
            // Ensure it ends with ".json"
            String name = file.getName();
            if (!name.toLowerCase().endsWith(".json")) {
                file = new File(file.getParent(), name + ".json");
            }
            // Write the record
            LifePathIO.saveAction(record, file);
        } else {
            LOGGER.info("Save operation cancelled by user.");
        }
    }
}
