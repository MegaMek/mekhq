/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.utilities;

import static mekhq.MHQConstants.LOGS_PATH;
import static mekhq.gui.CampaignGUI.saveCampaign;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import jakarta.annotation.Nullable;
import megamek.MMConstants;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.Configuration;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import org.apache.commons.io.output.CountingOutputStream;

/**
 * Utility methods for preparing and packaging campaign data for bug reports.
 *
 * <p>This helper class centralizes the logic used to:</p>
 * <ul>
 *     <li>Save a campaign in a "bug report" configuration</li>
 *     <li>Prompt the user for a destination archive file</li>
 *     <li>Create a ZIP file containing the saved campaign and relevant log files</li>
 * </ul>
 *
 * <p>The resulting archive is intended to be attached directly to issue reports for easier debugging and
 * reproduction.</p>
 *
 * @author Illiani
 * @since 0.50.11
 */
public class EasyBugReport {
    private static final MMLogger LOGGER = MMLogger.create(EasyBugReport.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.EasyBugReport";

    /**
     * Maximum target size (in bytes) for each generated bug-report ZIP part.
     *
     * <p>If the combined inputs exceed this limit, the bug report is written as multiple independent ZIP files (e.g.,
     * {@code name-part1.zip}, {@code name-part2.zip}, ...).</p>
     *
     * <p><b>Note:</b> This is a best-effort cap because ZIP container metadata and compression behavior can affect
     * the final on-disk size.</p>
     */
    private static final long MAX_ARCHIVE_BYTES = 24L * 1_000_000L; // 24 MB

    /**
     * Conservative per-entry overhead estimate (in bytes) used when deciding whether adding another file would exceed
     * {@link #MAX_ARCHIVE_BYTES}.
     *
     * <p>ZIP files add metadata per entry (headers, file name bytes, optional data descriptors, etc.), and the exact
     * overhead depends on the entry name length and ZIP features used. This constant intentionally overestimates
     * typical overhead to keep the resulting part sizes under the limit in practice.</p>
     */
    private static final long ZIP_ENTRY_OVERHEAD_BYTES = 2_048L;

    /** Archive sub-directory holding the collected logs and reports. */
    private static final String LOG_ENTRY_PREFIX = "logs/";

    /**
     * Basenames of the text logs written by {@code mmconf/log4j2.xml}, most important first. The live {@code .log}
     * files are collected in this order, and only rolled archives of these names are collected.
     */
    static final List<String> LOG_BASE_NAMES = List.of("mekhq",
          "megamek",
          "princess",
          "unified_log",
          "bot_path_ranker",
          "megameklab");

    private static final String LOG_EXTENSION = ".log";
    private static final String ROLLED_LOG_EXTENSION = ".log.gz";

    /**
     * The game dataset log MegaMek's server writes per game when dataset logging is on, as
     * {@code game_actions_<counter>.tsv} or with a date stamp. See {@code megamek.common.game.GameDatasetLogger}.
     */
    /**
     * Archive sub-directory for MegaMek's newest in-battle autosave, kept apart from the campaign save at the root so
     * the two cannot be confused. Kept the same as MegaMek's own bug report bundle.
     */
    private static final String BATTLE_AUTOSAVE_ENTRY_PREFIX = "battle-autosave/";

    /**
     * MegaMek autosaves are written to {@code savegames/} as {@code autosave.sav.gz}, {@code autosave<stamp>.sav.gz}
     * or {@code Round-<n>-autosave<stamp>.sav.gz}. See {@code megamek.server.AbstractGameManager#autoSave()} and
     * {@code megamek.server.AutosaveService}.
     */
    private static final String BATTLE_AUTOSAVE_MARKER = "autosave";
    private static final String BATTLE_AUTOSAVE_EXTENSION = ".sav.gz";

    private static final String GAME_DATASET_PREFIX = "game_actions_";
    private static final String GAME_DATASET_EXTENSION = ".tsv";

    /**
     * Archive sub-directory for a game's minimap images, matching their layout on disk so the folder name still
     * carries the game's UUID. Kept the same as MegaMek's own bug report bundle.
     */
    private static final String GAME_SUMMARY_ENTRY_PREFIX = LOG_ENTRY_PREFIX + "gameSummaries/minimap/";
    private static final String GAME_SUMMARY_FRAME_EXTENSION = ".png";
    private static final String GAME_SUMMARY_RECORDING_EXTENSION = ".gif";

    /**
     * How a file is treated when the bug report runs out of room. Files that are not {@link #REQUIRED} never start a
     * new archive part. Within each group the files are newest first, and once one does not fit, it and the rest of
     * its group are left out, so the oldest go first. A later group can still use the space that is left.
     */
    enum DropGroup {
        /** The save, live logs and newest reports; these start a new part when they do not fit. */
        REQUIRED,
        /** The newest game dataset log. */
        GAME_DATASET,
        /** Rolled {@code .log.gz} archives. */
        ROLLED_LOG,
        /** Named minimap PNGs of the newest game, one per phase. */
        MINIMAP_FRAME,
        /** The minimap GIF, which repeats the PNG frames without phase names. */
        MINIMAP_RECORDING
    }

    /**
     * One file destined for the bug report.
     *
     * @param source    the file to copy
     * @param entryName the path the file occupies inside the archive
     * @param dropGroup how the file is treated when the archive runs out of room
     */
    record BugReportCandidate(File source, String entryName, DropGroup dropGroup) {}

    /**
     * The outcome of writing a bug report.
     *
     * @param parts           the archive parts written, in order
     * @param includedEntries archive paths of every file written, in the order written
     * @param droppedEntries  names of the optional files left out to avoid another part; empty when everything
     *                        fitted
     */
    record BugReportArchiveResult(List<File> parts, List<String> includedEntries, List<String> droppedEntries) {}

    /**
     * Saves the given campaign and packages it, along with log files, into a ZIP archive chosen by the user.
     *
     * <p>The workflow is:</p>
     * <ol>
     *     <li>Derive a default base name using campaign name, in-game date, and a random callsign</li>
     *     <li>Ensure the campaigns directory exists</li>
     *     <li>Show a file chooser so the user can confirm or modify the archive name and location</li>
     *     <li>Save a temporary campaign file tailored for bug reporting</li>
     *     <li>Create an archive that includes the campaign file and log files</li>
     *     <li>Delete the temporary campaign file on success</li>
     * </ol>
     * <p>If archive creation fails, the temporary campaign file is deliberately left in place so the user still has
     * something they can attach to a bug report.</p>
     *
     * @param frame    the parent frame used as the owner for the file-chooser dialog
     * @param campaign the campaign whose state will be saved and packaged
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static void saveCampaignForBugReport(JFrame frame, Campaign campaign) {
        LOGGER.info("Saving campaign for bug report...");

        String campaignName = campaign.getPlayerForce().getName();
        LocalDate today = campaign.getLocalDate();

        // Random call sign so multiple bug report builds on the same in-game date don't overwrite each other
        String randomName = RandomCallsignGenerator.getInstance().generate();

        String dateString = today.format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                               .withLocale(MekHQ.getMHQOptions().getDateLocale()));

        // This will be used as the default base name
        String defaultBaseName = String.format("%s%s_%s", campaignName, dateString, randomName);

        // Base campaigns directory
        String rawDirectory = MekHQ.getCampaignsDirectory().getValue();
        File directory = new File(rawDirectory);

        // Ensure directory exists
        if (!directory.exists() && !directory.mkdirs()) {
            LOGGER.error("Failed to create campaign directory: {}", rawDirectory);
            return;
        }

        // Let the user pick the archive name / location
        JFileChooser fileChooser = new JFileChooser(directory);
        fileChooser.setDialogTitle(getTextAt(RESOURCE_BUNDLE, "EasyBugReport.fileChooser"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Bug Report Archives (*.zip)", "zip"));

        // Default file name (with .zip)
        fileChooser.setSelectedFile(new File(directory, defaultBaseName + ".zip"));

        int result = fileChooser.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            LOGGER.info("User cancelled bug report save dialog.");
            return;
        }

        File chosenArchiveFile = fileChooser.getSelectedFile();
        // Ensure .zip extension
        String archiveName = chosenArchiveFile.getName();
        if (!archiveName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            chosenArchiveFile = new File(chosenArchiveFile.getParentFile(), archiveName + ".zip");
        }

        // Temporary campaign file used only for building the archive, placed next to the archive so everything stays
        // together.
        File campaignFile = new File(chosenArchiveFile.getParentFile(), defaultBaseName + ".cpnx.gz");

        LOGGER.info("Bug report campaign temporary save target: {}", campaignFile.getName());
        // Save campaign with bug report prep flag enabled
        boolean saveSuccess = saveCampaign(frame, campaign, campaignFile, true);
        if (!saveSuccess) {
            LOGGER.error("Failed to save campaign for bug report");
            return;
        }

        // Now package campaign + logs into the user-chosen archive
        try {
            // Text logs always go to LOGS_PATH (mmconf/log4j2.xml); the combat and auto-resolve reports follow the
            // MegaMek log directory preference, which the player can relocate.
            ClientPreferences clientPreferences = PreferenceManager.getClientPreferences();
            List<BugReportCandidate> candidates = gatherCandidates(campaignFile,
                  new File(LOGS_PATH),
                  new File(clientPreferences.getLogDirectory()),
                  clientPreferences.getGameLogFilename(),
                  clientPreferences.getAutoResolveGameLogFilename(),
                  Configuration.gameSummaryImagesMMDir(),
                  new File(MMConstants.SAVEGAME_DIR));
            BugReportArchiveResult archiveResult = createBugReportArchive(candidates,
                  chosenArchiveFile,
                  MAX_ARCHIVE_BYTES);
            if (archiveResult.parts().size() > 1) {
                showTooMuchDataDialog();
            }

            // We only want the archive, so delete the loose campaign file
            if (!campaignFile.delete()) {
                LOGGER.warn("Unable to delete temporary bug report campaign file: {}", campaignFile.getName());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create bug report archive", e);
            // In this failure case, we intentionally leave the campaignFile so the user still has something to
            // attach if needed.
        }
    }

    /**
     * Builds the ordered list of files for a bug report. Order is load-bearing: the archive is filled front to back,
     * so the most useful files come first.
     *
     * <ol>
     *     <li>The campaign save, at the archive root</li>
     *     <li>MegaMek's newest in-battle autosave, under {@code battle-autosave/}, so a maintainer can reload the
     *     battle itself</li>
     *     <li>The live text logs, in {@link #LOG_BASE_NAMES} order; empty files are skipped</li>
     *     <li>The newest combat report ({@code gamelog*.html}) and the newest auto-resolve report
     *     ({@code simulation*.html})</li>
     *     <li>The newest game dataset log ({@code game_actions_*.tsv})</li>
     *     <li>Rolled {@code <name>_N.log.gz} archives of those logs, newest first</li>
     *     <li>The minimap PNGs of the newest game, newest first, then that game's GIF</li>
     * </ol>
     *
     * <p>Everything after the newest reports is optional: see {@link DropGroup} for how it is left out when space
     * runs short.</p>
     *
     * <p>The log directory also holds a {@code Bot_*.mul} unit list per bot per game and one combat report per
     * game, which together can run to hundreds of megabytes. Those are never collected beyond the newest report.</p>
     *
     * @param campaignFile           the saved campaign
     * @param logDirectory           the directory the text logs are written to
     * @param gameReportDirectory    the directory the combat and auto-resolve reports and the dataset log are written
     *                               to
     * @param gameLogFileName        the configured combat report name, normally {@code gamelog.html}, or
     *                               {@code null} to collect none
     * @param autoResolveLogFileName the configured auto-resolve report name, normally {@code simulation.html}, or
     *                               {@code null} to collect none
     * @param minimapDirectory       the folder holding one minimap image folder per game, normally
     *                               {@code gameSummaries/minimap} under the report directory
     * @param savegameDirectory      the folder MegaMek writes its autosaves to, normally {@code savegames} under the
     *                               working directory
     *
     * @return the files to archive, in priority order
     */
    static List<BugReportCandidate> gatherCandidates(File campaignFile, File logDirectory, File gameReportDirectory,
          @Nullable String gameLogFileName, @Nullable String autoResolveLogFileName, File minimapDirectory,
          File savegameDirectory) {
        List<BugReportCandidate> candidates = new ArrayList<>();
        candidates.add(new BugReportCandidate(campaignFile, campaignFile.getName(), DropGroup.REQUIRED));

        File battleAutosave = findNewestBattleAutosave(savegameDirectory);
        if (battleAutosave != null) {
            candidates.add(new BugReportCandidate(battleAutosave,
                  BATTLE_AUTOSAVE_ENTRY_PREFIX + battleAutosave.getName(),
                  DropGroup.REQUIRED));
        }

        boolean hasLogDirectory = logDirectory.isDirectory();
        if (hasLogDirectory) {
            for (String baseName : LOG_BASE_NAMES) {
                File logFile = new File(logDirectory, baseName + LOG_EXTENSION);
                if (logFile.isFile() && (logFile.length() > 0)) {
                    candidates.add(new BugReportCandidate(logFile,
                          LOG_ENTRY_PREFIX + logFile.getName(),
                          DropGroup.REQUIRED));
                }
            }
        } else {
            LOGGER.warn("Logs directory does not exist or is not a directory: {}", logDirectory.getPath());
        }

        for (String reportFileName : new String[] { gameLogFileName, autoResolveLogFileName }) {
            File newestReport = findNewestReport(gameReportDirectory, reportFileName);
            if (newestReport != null) {
                candidates.add(new BugReportCandidate(newestReport,
                      LOG_ENTRY_PREFIX + newestReport.getName(),
                      DropGroup.REQUIRED));
            }
        }

        File newestDataset = findNewestFile(gameReportDirectory, GAME_DATASET_PREFIX, GAME_DATASET_EXTENSION);
        if (newestDataset != null) {
            candidates.add(new BugReportCandidate(newestDataset,
                  LOG_ENTRY_PREFIX + newestDataset.getName(),
                  DropGroup.GAME_DATASET));
        }

        if (hasLogDirectory) {
            for (File rolledLog : findRolledLogsNewestFirst(logDirectory)) {
                candidates.add(new BugReportCandidate(rolledLog,
                      LOG_ENTRY_PREFIX + rolledLog.getName(),
                      DropGroup.ROLLED_LOG));
            }
        }

        File gameSummaryDirectory = findNewestGameSummaryDirectory(minimapDirectory);
        if (gameSummaryDirectory != null) {
            String entryPrefix = GAME_SUMMARY_ENTRY_PREFIX + gameSummaryDirectory.getName() + "/";
            for (File frame : listNewestFirst(gameSummaryDirectory, GAME_SUMMARY_FRAME_EXTENSION)) {
                candidates.add(new BugReportCandidate(frame,
                      entryPrefix + frame.getName(),
                      DropGroup.MINIMAP_FRAME));
            }
            for (File recording : listNewestFirst(gameSummaryDirectory, GAME_SUMMARY_RECORDING_EXTENSION)) {
                candidates.add(new BugReportCandidate(recording,
                      entryPrefix + recording.getName(),
                      DropGroup.MINIMAP_RECORDING));
            }
        }

        LOGGER.info("Bug report: collecting logs from {} and reports from {}: {} candidate file(s)",
              logDirectory.getPath(),
              gameReportDirectory.getPath(),
              candidates.size());
        return candidates;
    }

    /**
     * Finds the most recently modified, non-empty report matching a configured report name.
     *
     * <p>With MegaMek's filename stamping preference on, {@code gamelog.html} is written as
     * {@code gamelog_2026-07-21_11-16-49.html}, so a match is the name's basename as a prefix plus its extension as a
     * suffix.</p>
     *
     * @param reportDirectory the directory to search
     * @param reportFileName  the configured report name, or {@code null}
     *
     * @return the newest match, or {@code null} when there is none
     */
    private static @Nullable File findNewestReport(File reportDirectory, @Nullable String reportFileName) {
        if ((reportFileName == null) || reportFileName.isBlank()) {
            return null;
        }

        int extensionIndex = reportFileName.lastIndexOf('.');
        String prefix = (extensionIndex > 0) ? reportFileName.substring(0, extensionIndex) : reportFileName;
        String suffix = (extensionIndex > 0) ? reportFileName.substring(extensionIndex) : "";
        return findNewestFile(reportDirectory, prefix, suffix);
    }

    /**
     * Finds the most recently modified, non-empty file whose name has the given prefix and suffix.
     *
     * @param directory the directory to search
     * @param prefix    the name the file must start with
     * @param suffix    the name the file must end with, or an empty string for any
     *
     * @return the newest match, or {@code null} when there is none
     */
    private static @Nullable File findNewestFile(File directory, String prefix, String suffix) {
        File[] matches = directory.listFiles(file -> file.isFile()
                                                          && (file.length() > 0)
                                                          && file.getName().startsWith(prefix)
                                                          && file.getName().endsWith(suffix));
        if (matches == null) {
            return null;
        }

        File newestMatch = null;
        for (File match : matches) {
            if ((newestMatch == null) || (match.lastModified() > newestMatch.lastModified())) {
                newestMatch = match;
            }
        }
        return newestMatch;
    }

    /**
     * Finds MegaMek's most recent in-battle autosave, whichever of its naming forms it took.
     *
     * @param savegameDirectory the folder MegaMek writes its autosaves to
     *
     * @return the newest autosave, or {@code null} when there is none
     */
    private static @Nullable File findNewestBattleAutosave(File savegameDirectory) {
        File[] autosaves = savegameDirectory.listFiles(EasyBugReport::isBattleAutosave);
        if (autosaves == null) {
            return null;
        }

        File newestAutosave = null;
        for (File autosave : autosaves) {
            if ((newestAutosave == null) || (autosave.lastModified() > newestAutosave.lastModified())) {
                newestAutosave = autosave;
            }
        }
        return newestAutosave;
    }

    /**
     * Whether a file is a non-empty MegaMek autosave rather than a save the player named.
     *
     * @param file the file to test
     *
     * @return {@code true} if the file is an autosave
     */
    private static boolean isBattleAutosave(File file) {
        String fileName = file.getName();
        return file.isFile()
                     && (file.length() > 0)
                     && fileName.contains(BATTLE_AUTOSAVE_MARKER)
                     && fileName.endsWith(BATTLE_AUTOSAVE_EXTENSION);
    }

    /**
     * Finds the most recently changed per-game minimap folder. From the campaign there is no current game to name
     * one, and the newest folder is the battle most likely behind the report.
     *
     * @param minimapDirectory the folder holding one sub-folder per game, named by the game's UUID
     *
     * @return the newest game folder, or {@code null} when there is none
     */
    private static @Nullable File findNewestGameSummaryDirectory(File minimapDirectory) {
        File[] gameDirectories = minimapDirectory.listFiles(File::isDirectory);
        if (gameDirectories == null) {
            return null;
        }

        File newestDirectory = null;
        for (File gameDirectory : gameDirectories) {
            if ((newestDirectory == null) || (gameDirectory.lastModified() > newestDirectory.lastModified())) {
                newestDirectory = gameDirectory;
            }
        }
        return newestDirectory;
    }

    /**
     * Lists the non-empty files in a directory with the given extension, newest first. A minimap GIF has no end
     * trailer until its game ends, but a copy taken mid-battle is still readable.
     *
     * @param directory the directory to list
     * @param extension the extension to match, such as {@code .png}
     *
     * @return the matching files, newest first, ties broken by name
     */
    private static List<File> listNewestFirst(File directory, String extension) {
        File[] files = directory.listFiles(file -> file.isFile()
                                                        && (file.length() > 0)
                                                        && file.getName().toLowerCase(Locale.ROOT).endsWith(extension));
        if (files == null) {
            return List.of();
        }

        List<File> sortedFiles = new ArrayList<>(List.of(files));
        sortedFiles.sort(Comparator.comparingLong(File::lastModified).reversed().thenComparing(File::getName));
        return sortedFiles;
    }

    /**
     * Collects the rolled archives ({@code <name>_N.log.gz}) of the logs in {@link #LOG_BASE_NAMES}, newest first.
     * Other compressed files in the log directory are ignored.
     *
     * @param logDirectory the directory the text logs are written to
     *
     * @return the rolled logs, newest first, ties broken by name
     */
    private static List<File> findRolledLogsNewestFirst(File logDirectory) {
        File[] rolledLogs = logDirectory.listFiles(EasyBugReport::isCollectableRolledLog);
        if (rolledLogs == null) {
            return List.of();
        }

        List<File> sortedRolledLogs = new ArrayList<>(List.of(rolledLogs));
        sortedRolledLogs.sort(Comparator.comparingLong(File::lastModified).reversed()
                                    .thenComparing(File::getName));
        return sortedRolledLogs;
    }

    /**
     * Whether a file is a rolled archive of one of the collected logs: a basename, an underscore and {@code .log.gz},
     * as Log4j rolls {@code mekhq.log} to {@code mekhq_1.log.gz}.
     *
     * @param file the file to test
     *
     * @return {@code true} if the file is a rolled archive of a collected log
     */
    private static boolean isCollectableRolledLog(File file) {
        String fileName = file.getName();
        if (!file.isFile() || !fileName.endsWith(ROLLED_LOG_EXTENSION)) {
            return false;
        }
        for (String baseName : LOG_BASE_NAMES) {
            if (fileName.startsWith(baseName + "_")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes the candidates into one or more ZIP archives of at most {@code maxArchiveBytes} each.
     *
     * <p>Candidates are written in order. A required file that does not fit starts a new part. An optional file
     * never starts a new part: once one does not fit, it and the rest of its {@link DropGroup} are left out and named
     * in the result, so old history is lost before the player is asked to upload another file.</p>
     *
     * @param candidates      the files to write, in priority order, from
     *                        {@link #gatherCandidates(File, File, File, String, String, File, File)}
     * @param archiveFile     the user-chosen archive; parts are written next to it as {@code <name>-partN.zip}
     * @param maxArchiveBytes the size cap for each part
     *
     * @return the parts written and what was included or left out
     *
     * @throws IOException if an I/O error occurs while writing the archive
     */
    static BugReportArchiveResult createBugReportArchive(List<BugReportCandidate> candidates, File archiveFile,
          long maxArchiveBytes) throws IOException {
        List<String> includedEntries = new ArrayList<>();
        List<String> droppedEntries = new ArrayList<>();

        String baseName = archiveFile.getName();
        if (baseName.toLowerCase().endsWith(".zip")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        File parentDirectory = archiveFile.getParentFile();
        if (parentDirectory == null) {
            parentDirectory = archiveFile.getAbsoluteFile().getParentFile();
        }

        List<File> parts = new ArrayList<>();
        int partIndex = 1;

        File currentArchive = new File(parentDirectory, baseName + "-part" + partIndex + ".zip");
        CountingOutputStream countingOut = null;
        ZipOutputStream zipOut = null;

        try {
            countingOut = new CountingOutputStream(
                  new BufferedOutputStream(new FileOutputStream(currentArchive))
            );
            zipOut = new ZipOutputStream(countingOut);
            parts.add(currentArchive);

            Set<DropGroup> exhaustedGroups = EnumSet.noneOf(DropGroup.class);
            for (BugReportCandidate candidate : candidates) {
                File inputFile = candidate.source();
                String entryName = candidate.entryName();
                DropGroup dropGroup = candidate.dropGroup();

                long estimatedAddedBytes = inputFile.length() + ZIP_ENTRY_OVERHEAD_BYTES;
                boolean exceedsCap = countingOut.getCount() > 0
                                           && (countingOut.getCount() + estimatedAddedBytes) > maxArchiveBytes;

                // Each optional group is newest first, so once one file does not fit, the rest of the group is
                // older and is dropped rather than asking the player to upload another part.
                if ((dropGroup != DropGroup.REQUIRED) && (exceedsCap || exhaustedGroups.contains(dropGroup))) {
                    exhaustedGroups.add(dropGroup);
                    droppedEntries.add(inputFile.getName());
                    continue;
                }

                // If adding this file exceeds the cap, roll to next part (if current part already has something).
                // Note: This is conservative and should keep the actual ZIP size under the limit in practice.
                if (exceedsCap) {
                    try {
                        zipOut.close(); // closes countingOut as well
                    } finally {
                        try {
                            countingOut.close();
                        } catch (IOException ignored) {
                        }
                    }

                    zipOut = null;
                    countingOut = null;

                    partIndex++;
                    currentArchive = new File(parentDirectory, baseName + "-part" + partIndex + ".zip");
                    countingOut = new CountingOutputStream(
                          new BufferedOutputStream(new FileOutputStream(currentArchive))
                    );
                    zipOut = new ZipOutputStream(countingOut);
                    parts.add(currentArchive);
                }

                // If a single file is huge, it may exceed the cap even alone.
                if (inputFile.length() > (maxArchiveBytes - ZIP_ENTRY_OVERHEAD_BYTES)) {
                    LOGGER.warn(
                          "File '{}' is larger than the per-archive limit ({} bytes); it will be placed in its own "
                                + "part and may exceed the limit.",
                          inputFile.getName(),
                          maxArchiveBytes
                    );
                }

                addFileToZip(inputFile, entryName, zipOut);
                includedEntries.add(entryName);
            }
        } finally {
            if (zipOut != null) {
                try {
                    zipOut.close();
                } catch (IOException ex) {
                    LOGGER.error(ex, "Unable to close zip output stream");
                }
            } else if (countingOut != null) {
                try {
                    countingOut.close();
                } catch (IOException ex) {
                    LOGGER.error(ex, "Unable to close counting output stream");
                }
            }
        }

        int partCount = parts.size();
        LOGGER.info("Created bug report archive in {} part(s): {}", partCount, parts);
        if (!droppedEntries.isEmpty()) {
            LOGGER.info("Left out {} optional file(s) to avoid another part: {}",
                  droppedEntries.size(),
                  String.join(", ", droppedEntries));
        }

        return new BugReportArchiveResult(List.copyOf(parts),
              List.copyOf(includedEntries),
              List.copyOf(droppedEntries));
    }

    /**
     * Shows a modal warning dialog informing the user that the bug report data was split into multiple archive parts
     * and that all parts must be uploaded.
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static void showTooMuchDataDialog() {
        String message = getTextAt(RESOURCE_BUNDLE, "EasyBugReport.warning.tooMuchData");

        JOptionPane pane = new JOptionPane(
              message,
              JOptionPane.WARNING_MESSAGE,
              JOptionPane.DEFAULT_OPTION
        );

        JDialog dialog = pane.createDialog("");
        dialog.setModal(true);

        dialog.setVisible(true);
        dialog.dispose();
    }

    /**
     * Adds a single file to an open ZIP output stream.
     *
     * <p>The file is written using a newly created {@link ZipEntry} with the provided entry name, and its contents
     * are streamed using a fixed-size buffer until EOF.</p>
     *
     * @param source          the source file to read from
     * @param entryName       the path/name under which the file should appear inside the archive
     * @param zipOutputStream the open ZIP output stream to which the entry will be written
     *
     * @throws IOException if any I/O error occurs while reading or writing
     * @author Illiani
     * @since 0.50.11
     */
    private static void addFileToZip(File source, String entryName, ZipOutputStream zipOutputStream)
          throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zipOutputStream.putNextEntry(entry);

        try (InputStream fileInputStream = new FileInputStream(source)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, length);
            }
        }

        zipOutputStream.closeEntry();
    }
}
