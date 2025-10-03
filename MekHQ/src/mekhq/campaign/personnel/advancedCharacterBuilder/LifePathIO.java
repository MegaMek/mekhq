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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    final static String PRIMARY_RESOURCE_BUNDLE = "mekhq.resources.LifePathIO";
    final static String LEGAL_NOTICE_RESOURCE_BUNDLE = "mekhq.resources.Legal";
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
        Map<UUID, String> outOfDateLifePathsWithLegalStatements = new HashMap<>();

        try {
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
                            // PREPROCESS: Skip legal comment lines
                            StringBuilder jsonBuilder = new StringBuilder();
                            try (BufferedReader reader = new BufferedReader(
                                  new InputStreamReader(
                                        new FileInputStream(file), StandardCharsets.UTF_8))) {
                                String line;
                                boolean inJson = false;
                                while ((line = reader.readLine()) != null) {
                                    if (!inJson) {
                                        if (line.trim().startsWith("{")) {
                                            inJson = true;
                                            jsonBuilder.append(line).append('\n');
                                        }
                                    } else {
                                        jsonBuilder.append(line).append('\n');
                                    }
                                }
                            }
                            String json = jsonBuilder.toString();
                            ObjectMapper objectMapper = new ObjectMapper();
                            LifePath record = objectMapper.readValue(json, LifePath.class);
                            UUID id = record.id();
                            if (id != null) {
                                if (lifePathMap.containsKey(id)) {
                                    LOGGER.warn("Duplicate LifePath id found ({}). Overwriting {} with {}.",
                                          id, lifePathMap.get(id).name(), record.name());
                                }
                                lifePathMap.put(id, record);

                                // Set these booleans to 'true' to resave every Life Path in the data directory.
                                // Useful for when we make a change and don't want to manually resave everything. The
                                // second boolean bypasses the 'check for legal statement' conditional and causes all
                                // Life Paths in the data directory to be resaved with a legal statement. Note that
                                // both booleans need to be true for this statement inclusion to occur.
                                boolean overrideUpgradeRequirements = false;
                                boolean overrideLegalStatementRequirements = false;

                                if (record.version().isLowerThan(MHQConstants.VERSION) || overrideUpgradeRequirements) {
                                    outOfDateLifePaths.put(id, file.getParent());
                                    LOGGER.info("LifePath [{}] is out of date.", record.name());

                                    if (fileHasLegalStatement(file) || overrideLegalStatementRequirements) {
                                        outOfDateLifePathsWithLegalStatements.put(id, file.getPath());
                                    }

                                    outOfDateLifePathsWithLegalStatements.put(id, file.getPath());
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
            boolean isUpgrade = silentlyUpgrade ||
                                      triggerConfirmationDialog(campaign, directoryPath, outOfDateLifePaths);
            if (isUpgrade) {
                for (Map.Entry<UUID, String> entry : outOfDateLifePaths.entrySet()) {
                    boolean includeLegalStatement = outOfDateLifePathsWithLegalStatements.containsKey(entry.getKey());
                    LifePath record = lifePathMap.get(entry.getKey());
                    writeToJSONWithoutDialog(record, entry.getValue(), includeLegalStatement);
                }
            }
        }

        return lifePathMap;
    }

    private static boolean fileHasLegalStatement(File file) {
        try (BufferedReader reader = new BufferedReader(
              new InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            for (int i = 0; i < 10; i++) { // Only scan first 10 lines for performance
                String line = reader.readLine();
                if (line == null) {break;}
                if (line.contains("MegaMek Data (C)")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Optionally log error
        }
        return false;
    }

    private static boolean triggerConfirmationDialog(Campaign campaign, String directoryPath,
          Map<UUID, String> outOfDateLifePaths) {
        String message = getFormattedTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.notice",
              outOfDateLifePaths.size(), directoryPath);
        String cancelOption = getTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.button.cancel");
        String confirmOption = getTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.button.confirm");
        String warning = getFormattedTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.warning");

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
            Collection<Set<UUID>> requirementsLifePaths = lifePath.requirementsLifePath().values();
            for (Set<UUID> allIDs : requirementsLifePaths) {
                for (UUID id : allIDs) {
                    if (!lifePathMap.containsKey(id)) {
                        LOGGER.warn("LifePath {} requires non-existent LifePath {}", lifePath.name(), id);
                    }
                }
            }

            Collection<Set<UUID>> exclusionsLifePaths = lifePath.exclusionsLifePath().values();
            for (Set<UUID> allIDs : exclusionsLifePaths) {
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
              getTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathBuilderDialog.io.load"),
              FileType.JSON,
              userDirectory
        );

        if (fileOpt.isPresent()) {
            File file = fileOpt.get();
            try {
                // PREPROCESS: Skip legal notice lines starting with '#'
                StringBuilder jsonBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                      new InputStreamReader(
                            new FileInputStream(file), StandardCharsets.UTF_8))) {
                    String line;
                    boolean inJson = false;
                    while ((line = reader.readLine()) != null) {
                        // Skip comment and blank lines until JSON starts
                        if (!inJson) {
                            if (line.trim().startsWith("{")) {
                                inJson = true;
                                jsonBuilder.append(line).append('\n');
                            }
                        } else {
                            jsonBuilder.append(line).append('\n');
                        }
                    }
                }

                String json = jsonBuilder.toString();
                ObjectMapper objectMapper = new ObjectMapper();
                LifePath record = objectMapper.readValue(json, LifePath.class);

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

    public static void writeToJSONWithoutDialog(LifePath record, String directory, boolean includeLegalStatement) {
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
        saveAction(record, file, includeLegalStatement);
    }

    public static void saveAction(LifePath record, File file, boolean includeLegalStatement) {
        String legalStatement = getLegalStatement();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            printer.indentObjectsWith(new DefaultIndenter("    ", DefaultIndenter.SYS_LF));
            printer.indentArraysWith(new DefaultIndenter("    ", DefaultIndenter.SYS_LF));
            printer = printer.withArrayIndenter(new DefaultIndenter("    ", DefaultIndenter.SYS_LF));
            printer = printer.withObjectIndenter(new DefaultIndenter("    ", DefaultIndenter.SYS_LF));
            String jsonContent = objectMapper.writer(printer).writeValueAsString(record);

            // Always open the file in OVERWRITE mode (default):
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                  new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
                if (includeLegalStatement) {
                    writer.write(legalStatement);
                    writer.write(System.lineSeparator()); // Twinned line seps ensures a clean start to the file
                    writer.write(System.lineSeparator());
                }

                writer.write(jsonContent);
                writer.flush();
            }

            LOGGER.info("Wrote LifePathRecord JSON to: {}", file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to write LifePathRecord JSON: {}", e.getMessage());
        }
    }

    private static String getLegalStatement() {
        String year = String.valueOf(LocalDate.now().getYear()).replace(",", "");
        String legalStatement = getFormattedTextAt(LEGAL_NOTICE_RESOURCE_BUNDLE, "Legal.mmData.legalStatement", year);
        legalStatement = legalStatement.replaceAll("-->", "");
        legalStatement = legalStatement.replaceAll("<!--", "");
        legalStatement = legalStatement.trim();
        return legalStatement;
    }

    public static void writeToJSONWithDialog(LifePath record, boolean includeLegalStatement) {
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
              getTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathBuilderDialog.io.save"),
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
            saveAction(record, file, includeLegalStatement);
        } else {
            LOGGER.info("Save operation cancelled by user.");
        }
    }
}
