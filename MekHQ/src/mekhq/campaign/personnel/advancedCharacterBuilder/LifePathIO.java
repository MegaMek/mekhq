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

import static mekhq.MHQConstants.CONFIRMATION_UPGRADE_LIFE_PATHS;
import static mekhq.MHQConstants.LIFE_PATHS_DEFAULT_DIRECTORY_PATH;
import static mekhq.MHQConstants.LIFE_PATHS_USER_DIRECTORY_PATH;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.GUI;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.io.FileType;

/**
 * Reads and writes Life Path definitions as JSON files.
 *
 * <p>A Life Path file is ordinary JSON that may be preceded by a licence header written as {@code #} comment lines.
 * JSON has no comment syntax, so every read here skips forward to the first line beginning with <code>{</code>
 * before handing the text to Jackson.</p>
 *
 * <p>Files are read from two places: the shipped {@code data} directory, and a directory under the user's own
 * directory. Both are scanned recursively.</p>
 *
 * @since 0.50.11
 */
public class LifePathIO {
    static final String PRIMARY_RESOURCE_BUNDLE = "mekhq.resources.LifePathIO";
    static final String LEGAL_NOTICE_RESOURCE_BUNDLE = "mekhq.resources.Legal";

    /**
     * The file chooser titles live with the wizard's other strings rather than in this class's own bundle, so they
     * have to be looked up there.
     */
    static final String BUILDER_DIALOG_RESOURCE_BUNDLE = "mekhq.resources.LifePathBuilderDialog";

    private static final MMLogger LOGGER = MMLogger.create(LifePathIO.class);

    /**
     * One mapper, shared, and deliberately tolerant of properties it does not recognise.
     *
     * <p>Jackson fails on an unknown property by default. That would mean the day a component is removed from
     * {@link LifePath}, every file still carrying it stops loading. Ignoring unknown properties lets a removed field
     * simply fall away.</p>
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(
          DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Name given to a file whose Life Path has no usable name of its own. */
    private static final String FALLBACK_FILE_NAME = "unnamed_life_path";

    /** Text that marks a file as already carrying the MegaMek Data licence header. */
    private static final String LEGAL_STATEMENT_MARKER = "MegaMek Data (C)";

    /** How far into a file to look for the licence header before giving up. */
    private static final int LEGAL_STATEMENT_SCAN_LINES = 10;

    /**
     * Loads every Life Path available to the campaign, from both the shipped data directory and the user directory.
     *
     * <p>A user Life Path whose id matches a shipped one replaces it, matching how camos, portraits and units
     * already behave.</p>
     *
     * @param campaign the campaign being loaded, used only to parent any dialog the load raises
     *
     * @return every Life Path found, keyed by id
     *
     * @since 0.50.11
     */
    public static Map<UUID, LifePath> loadAllLifePaths(Campaign campaign) {
        LOGGER.info("Loading all LifePaths");
        Map<UUID, LifePath> lifePathMap =
              new HashMap<>(loadAllLifePathsFromDirectory(campaign, LIFE_PATHS_DEFAULT_DIRECTORY_PATH, true));

        Path userDirectory = resolveUserLifePathDirectory();
        if (userDirectory != null) {
            LOGGER.info("Loading LifePaths from user directory {}", userDirectory);
            Map<UUID, LifePath> userLifePathMap = new HashMap<>(loadAllLifePathsFromDirectory(campaign,
                  userDirectory.toString(),
                  false));

            for (Map.Entry<UUID, LifePath> entry : userLifePathMap.entrySet()) {
                LifePath shippedLifePath = lifePathMap.get(entry.getKey());

                if (shippedLifePath != null) {
                    LOGGER.warn("Overriding shipped Life Path [{}] with the user directory copy [{}].",
                          shippedLifePath.name(), entry.getValue().name());
                }

                lifePathMap.put(entry.getKey(), entry.getValue());
            }
        }

        validateLifePaths(lifePathMap);

        return lifePathMap;
    }

    /**
     * Returns the user's Life Path directory as a resolved path, or {@code null} when no user directory is set.
     *
     * <p>Resolved rather than concatenated: {@link mekhq.MHQConstants#LIFE_PATHS_USER_DIRECTORY_PATH} begins with a
     * separator, so joining it to a user directory that already ends in one produces a path that does not exist.</p>
     *
     * @return the directory Life Paths are read from and written to, or {@code null} when unset
     *
     * @since 0.50.11
     */
    private static @Nullable Path resolveUserLifePathDirectory() {
        String userDirectory = PreferenceManager.getClientPreferences().getUserDir();

        if (StringUtility.isNullOrBlank(userDirectory)) {
            return null;
        }

        // Leading separators have to go, or resolve() treats the suffix as an absolute path and discards the
        // user directory entirely.
        String relativeSuffix = LIFE_PATHS_USER_DIRECTORY_PATH.replaceAll("^[/\\\\]+", "");

        return Paths.get(userDirectory).resolve(relativeSuffix);
    }

    private static Map<UUID, LifePath> loadAllLifePathsFromDirectory(Campaign campaign, String directoryPath,
          boolean silentlyUpgrade) {
        Map<UUID, LifePath> lifePathMap = new HashMap<>();
        Map<UUID, File> outOfDateLifePaths = new HashMap<>();
        Set<UUID> outOfDateLifePathsWithLegalStatements = new HashSet<>();

        try {
            Path startPath = Paths.get(directoryPath);
            if (Files.exists(startPath)) {
                try (Stream<Path> paths = Files.walk(startPath)) {
                    List<File> jsonFiles = paths
                                                 .filter(Files::isRegularFile)
                                                 .filter(path -> path.getFileName()
                                                                       .toString()
                                                                       .toLowerCase()
                                                                       .endsWith(".json"))
                                                 .map(Path::toFile)
                                                 .toList();

                    for (File file : jsonFiles) {
                        try {
                            LifePath record = readLifePathFromFile(file);
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
                                boolean overrideLegalStatementRequirements = overrideUpgradeRequirements && false;

                                if (record.version().isLowerThan(MHQConstants.VERSION) || overrideUpgradeRequirements) {
                                    // The file itself, not a path rebuilt from the record's name. A file whose name
                                    // differs from the Life Path's name would otherwise be written out a second
                                    // time under the derived name, leaving the original behind as a duplicate id.
                                    outOfDateLifePaths.put(id, file);
                                    LOGGER.info("LifePath [{}] is out of date.", record.name());

                                    if (!fileHasLegalStatement(file) || overrideLegalStatementRequirements) {
                                        outOfDateLifePathsWithLegalStatements.add(id);
                                    }
                                }

                                LOGGER.debug("Loaded LifePath [{}] from {}", record.name(), file.getPath());
                            } else {
                                LOGGER.warn("File {} missing valid LifePath id. Skipping.", file.getPath());
                            }
                        } catch (Exception exception) {
                            LOGGER.error(exception, "Failed to load LifePath from {}", file.getPath());
                        }
                    }
                }
            } else {
                LOGGER.warn("Life Path directory does not exist: {}", startPath);
            }
        } catch (Exception exception) {
            LOGGER.error(exception, "Failed to load LifePaths from directory {}", directoryPath);
        }

        if (!outOfDateLifePaths.isEmpty()) {
            boolean isUpgrade = silentlyUpgrade ||
                                      triggerConfirmationDialog(campaign, directoryPath, outOfDateLifePaths);
            if (isUpgrade) {
                for (Map.Entry<UUID, File> entry : outOfDateLifePaths.entrySet()) {
                    boolean includeLegalStatement = outOfDateLifePathsWithLegalStatements.contains(entry.getKey());
                    LifePath record = lifePathMap.get(entry.getKey());
                    saveAction(record, entry.getValue(), includeLegalStatement);
                }
            }
        }

        return lifePathMap;
    }

    /**
     * Reports whether a Life Path file already carries the MegaMek Data licence header.
     *
     * @param file the file to inspect
     *
     * @return {@code true} when the header is present in the first few lines
     *
     * @since 0.50.11
     */
    static boolean fileHasLegalStatement(File file) {
        try (BufferedReader reader = new BufferedReader(
              new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            for (int lineNumber = 0; lineNumber < LEGAL_STATEMENT_SCAN_LINES; lineNumber++) {
                String line = reader.readLine();

                if (line == null) {
                    break;
                }

                if (line.contains(LEGAL_STATEMENT_MARKER)) {
                    return true;
                }
            }
        } catch (Exception exception) {
            LOGGER.error(exception, "Error reading file: {}", file.getPath());
        }

        return false;
    }

    /**
     * Reads one Life Path file.
     *
     * <p>This is the single read path: the licence header is skipped, unknown properties are tolerated, and the XP
     * cost is recomputed from the file's own contents.</p>
     *
     * @param file the file to read
     *
     * @return the Life Path it describes
     *
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the file describes a Life Path the record will not accept
     *
     * @since 0.50.11
     */
    static LifePath readLifePathFromFile(File file) throws IOException {
        LifePath record = OBJECT_MAPPER.readValue(readJsonBody(file), LifePath.class);

        return withRecalculatedXPCost(record, file.getPath());
    }

    /**
     * Returns the Life Path with its XP cost recomputed from its own contents.
     *
     * <p>The cost is stored in the file, but it is derived from everything else in the path, so a file written before
     * a change to the cost rules carries a stale figure. Recomputing on load means the campaign always uses the
     * current rules, and the warning says which file needs resaving.</p>
     *
     * @param record   the Life Path as it was read
     * @param filePath the file it came from, named in the warning
     *
     * @return the same Life Path when the stored cost was right, otherwise a copy carrying the recomputed cost
     *
     * @since 0.50.11
     */
    private static LifePath withRecalculatedXPCost(LifePath record, String filePath) {
        int calculatedCost = LifePathXPCostCalculator.calculateXPCost(record);

        if (record.xpCost() != null && record.xpCost() == calculatedCost) {
            return record;
        }

        LOGGER.warn("LifePath [{}] in {} stores an XP cost of {} but its contents come to {}. Using {}. Resave the "
                          + "file to bring it up to date.", record.name(), filePath, record.xpCost(), calculatedCost,
              calculatedCost);

        return LifePathBuilder.from(record).xpCost(calculatedCost).build();
    }

    /**
     * Reads a Life Path file and returns just its JSON, discarding any licence header above it.
     *
     * <p>The header is written as {@code #} comment lines, which JSON does not allow, so everything before the
     * first line starting with <code>{</code> is dropped.</p>
     *
     * @param file the file to read
     *
     * @return the file's JSON body, empty when the file contains none
     *
     * @throws IOException if the file cannot be read
     *
     * @since 0.50.11
     */
    private static String readJsonBody(File file) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
              new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean inJson = false;

            while ((line = reader.readLine()) != null) {
                if (!inJson && !line.trim().startsWith("{")) {
                    continue;
                }

                inJson = true;
                jsonBuilder.append(line).append('\n');
            }
        }

        return jsonBuilder.toString();
    }

    /**
     * Turns a Life Path name into a file name that every supported platform accepts.
     *
     * @param lifePathName the Life Path's display name
     *
     * @return a safe base file name, without extension
     *
     * @since 0.50.11
     */
    private static String toSafeFileName(String lifePathName) {
        if (StringUtility.isNullOrBlank(lifePathName)) {
            return FALLBACK_FILE_NAME;
        }

        String safeName = lifePathName.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "");
        safeName = safeName.replaceAll("[. ]+$", "");
        // Quantified, unlike the original: a name reduced to several underscores kept all but one of them.
        safeName = safeName.replaceAll("^_+|_+$", "");

        return safeName.isBlank() ? FALLBACK_FILE_NAME : safeName;
    }

    /**
     * Asks the user whether out-of-date Life Path files in a directory should be rewritten to the current version.
     *
     * @param campaign           the campaign, used to parent the dialog
     * @param directoryPath       the directory being offered for upgrade, shown in the message
     * @param outOfDateLifePaths the files that would be rewritten, keyed by Life Path id
     *
     * @return {@code true} when the user agreed to the upgrade
     *
     * @since 0.50.11
     */
    private static boolean triggerConfirmationDialog(Campaign campaign, String directoryPath,
          Map<UUID, File> outOfDateLifePaths) {
        String message = getFormattedTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.notice",
              outOfDateLifePaths.size(), directoryPath);
        String cancelOption = getTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.button.cancel");
        String confirmOption = getTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.button.confirm");
        String warning = getTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathIO.upgradeDialog.warning");

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

            if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_UPGRADE_LIFE_PATHS)) {
                ImmersiveDialogConfirmation confirmDialog = new ImmersiveDialogConfirmation(campaign,
                      CONFIRMATION_UPGRADE_LIFE_PATHS);
                dialogConfirmed = confirmDialog.wasConfirmed();
            } else {
                dialogConfirmed = true;
            }
        }
        return isUpgrade;
    }

    /**
     * Logs every problem found in the loaded Life Path library.
     *
     * <p>Two kinds of problem are reported. A dangling link is one Life Path requiring or excluding an id that no
     * file provides, which can only be found with the whole library in hand. The rest come from
     * {@link LifePathValidator}, which checks each path on its own.</p>
     *
     * @param lifePathMap every Life Path that was loaded, keyed by id
     *
     * @since 0.50.11
     */
    private static void validateLifePaths(Map<UUID, LifePath> lifePathMap) {
        LOGGER.info("Starting Life Path Validation");
        for (LifePath lifePath : lifePathMap.values()) {
            for (InvalidLifePathReason reason : LifePathValidator.validate(lifePath)) {
                LOGGER.warn("LifePath [{}] ({}) is invalid: {}", lifePath.name(), lifePath.id(),
                      reason.getDisplayName());
            }

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
        LOGGER.info("{} Life Paths Validated.", lifePathMap.size());
    }

    /**
     * The outcome of asking the user to open a Life Path file.
     *
     * <p>A failed read used to be swallowed, so picking a bad file did nothing visible. Returning the reason lets
     * the caller tell the user what went wrong.</p>
     *
     * @param lifePath               the Life Path that was read, or {@code null} when nothing was read
     * @param errorMessage           why the read failed, or {@code null} when it did not fail
     * @param fileHasLegalStatement  whether the file carried the MegaMek Data licence header, so the wizard can show
     *                               the checkbox the way the file actually is rather than however it was last left
     *
     * @since 0.50.11
     */
    public record LifePathLoadResult(@Nullable LifePath lifePath, @Nullable String errorMessage,
                                     boolean fileHasLegalStatement) {
        /**
         * @return {@code true} when a Life Path was read
         */
        public boolean isLoaded() {
            return lifePath != null;
        }

        /**
         * @return {@code true} when the user picked a file that could not be read
         */
        public boolean isFailed() {
            return errorMessage != null;
        }
    }

    /**
     * Asks the user to pick a Life Path file, then reads it.
     *
     * @return the Life Path, or the reason it could not be read, or neither when the user cancelled
     *
     * @since 0.50.11
     */
    public static LifePathLoadResult loadFromJSONWithDialog() {
        Optional<File> selectedFile = GUI.fileDialogOpen(
              null,
              getTextAt(BUILDER_DIALOG_RESOURCE_BUNDLE, "LifePathBuilderDialog.io.load"),
              FileType.JSON,
              resolveStartingDirectory()
        );

        if (selectedFile.isEmpty()) {
            LOGGER.info("Load operation cancelled by user.");
            return new LifePathLoadResult(null, null, false);
        }

        File file = selectedFile.get();
        try {
            LifePath record = readLifePathFromFile(file);
            LOGGER.info("Loaded Life Path from: {}", file.getAbsolutePath());
            return new LifePathLoadResult(record, null, fileHasLegalStatement(file));
        } catch (Exception exception) {
            LOGGER.error(exception, "Failed to load Life Path from {}", file.getAbsolutePath());
            return new LifePathLoadResult(null,
                  getFormattedTextAt(PRIMARY_RESOURCE_BUNDLE, "LifePathIO.load.failure",
                        file.getName(), exception.getMessage()),
                  false);
        }
    }

    /**
     * Returns the directory a file chooser should open in: the user's Life Path directory when there is one, and the
     * shipped data directory otherwise.
     *
     * @return the directory to start browsing from
     *
     * @since 0.50.11
     */
    private static String resolveStartingDirectory() {
        Path userDirectory = resolveUserLifePathDirectory();

        return userDirectory == null ? LIFE_PATHS_DEFAULT_DIRECTORY_PATH : userDirectory.toString();
    }

    /**
     * Writes a Life Path into the given directory, under a file name derived from its own name.
     *
     * @param record               the Life Path to write
     * @param directory            the directory to write into, created if absent
     * @param includeLegalStatement whether to prepend the MegaMek Data licence header
     *
     * @since 0.50.11
     */
    public static void writeToJSONWithoutDialog(LifePath record, String directory, boolean includeLegalStatement) {
        File targetDirectory = new File(directory);
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            LOGGER.error("Could not create directory: {}", directory);
            return;
        }

        File file = new File(targetDirectory, toSafeFileName(record.name()) + ".json");
        saveAction(record, file, includeLegalStatement);
    }

    /**
     * Writes a Life Path to an exact file, stamping it with the current version as it goes.
     *
     * @param originalRecord        the Life Path to write
     * @param file                  the file to overwrite
     * @param includeLegalStatement whether to prepend the MegaMek Data licence header
     *
     * @since 0.50.11
     */
    public static void saveAction(LifePath originalRecord, File file, boolean includeLegalStatement) {
        String legalStatement = getLegalStatement();
        LifePath newRecord = originalRecord.resaveWithUpdatedVersion();

        try {
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            printer.indentObjectsWith(new DefaultIndenter("    ", DefaultIndenter.SYS_LF));
            printer.indentArraysWith(new DefaultIndenter("    ", DefaultIndenter.SYS_LF));
            printer = printer.withArrayIndenter(new DefaultIndenter("    ", DefaultIndenter.SYS_LF));
            printer = printer.withObjectIndenter(new DefaultIndenter("    ", DefaultIndenter.SYS_LF));
            String jsonContent = OBJECT_MAPPER.writer(printer).writeValueAsString(newRecord);

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
                LOGGER.info("Wrote LifePathRecord JSON to: {}", file.getAbsolutePath());
            }
        } catch (Exception exception) {
            LOGGER.error(exception, "Failed to write Life Path JSON to {}", file.getAbsolutePath());
        }
    }

    /**
     * Returns the MegaMek Data licence header, stripped of the HTML comment markers the shared string carries.
     *
     * @return the licence header as plain text
     *
     * @since 0.50.11
     */
    private static String getLegalStatement() {
        // Passed as a String so the formatter does not group it as a number: "3,025" is not a year.
        String year = String.valueOf(LocalDate.now().getYear());
        // "Legal.legalStatement" is the key Legal.properties actually carries, and it is shared with the rank
        // system exporter. Asking for "Legal.mmData.legalStatement" silently wrote the header
        // "!Legal.mmData.legalStatement!" into every saved Life Path.
        String legalStatement = getFormattedTextAt(LEGAL_NOTICE_RESOURCE_BUNDLE, "Legal.legalStatement", year);
        legalStatement = legalStatement.replaceAll("-->", "");
        legalStatement = legalStatement.replaceAll("<!--", "");
        legalStatement = legalStatement.trim();
        return legalStatement;
    }

    /**
     * Asks the user where to save a Life Path, then writes it there.
     *
     * <p>The file is returned so the caller knows whether the save actually happened. Anything the caller commits
     * only on a successful save, such as a newly generated id, can then wait for that.</p>
     *
     * @param record                the Life Path to write
     * @param includeLegalStatement whether to prepend the MegaMek Data licence header
     *
     * @return the file written, or empty when the user cancelled
     *
     * @since 0.50.11
     */
    public static Optional<File> writeToJSONWithDialog(LifePath record, boolean includeLegalStatement) {
        Optional<File> dialogFile = GUI.fileDialogSave(
              null,
              getTextAt(BUILDER_DIALOG_RESOURCE_BUNDLE, "LifePathBuilderDialog.io.save"),
              FileType.JSON,
              resolveStartingDirectory(),
              toSafeFileName(record.name())
        );

        if (dialogFile.isEmpty()) {
            LOGGER.info("Save operation cancelled by user.");
            return Optional.empty();
        }

        File file = dialogFile.get();
        String fileName = file.getName();
        if (!fileName.toLowerCase().endsWith(".json")) {
            file = new File(file.getParent(), fileName + ".json");
        }

        saveAction(record, file, includeLegalStatement);

        return Optional.of(file);
    }
}
