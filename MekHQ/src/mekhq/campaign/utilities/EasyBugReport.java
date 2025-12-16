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
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import megamek.client.generator.RandomCallsignGenerator;
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
    private static final long MAX_ARCHIVE_BYTES = 1024L * 1024L; // 24 MiB

    /**
     * Conservative per-entry overhead estimate (in bytes) used when deciding whether adding another file would exceed
     * {@link #MAX_ARCHIVE_BYTES}.
     *
     * <p>ZIP files add metadata per entry (headers, file name bytes, optional data descriptors, etc.), and the exact
     * overhead depends on the entry name length and ZIP features used. This constant intentionally overestimates
     * typical overhead to keep the resulting part sizes under the limit in practice.</p>
     */
    private static final long ZIP_ENTRY_OVERHEAD_BYTES = 2_048L;

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

        String campaignName = campaign.getName();
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
            createBugReportArchive(campaignFile, chosenArchiveFile);

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
     * Creates a ZIP archive containing the given campaign file and any valid log files located under
     * {@link mekhq.MHQConstants#LOGS_PATH}.
     *
     * <p>The campaign file is stored at the root of the archive. Log files are stored under the {@code logs/}
     * directory within the archive, and are limited to files ending in {@code .log} or {@code .log.gz}.</p>
     *
     * <p>If the logs directory does not exist or contains no matching files, the method logs this and still returns
     * the archive containing only the campaign file.</p>
     *
     * @param campaignFile the already-saved campaign file to be included at the root of the archive
     * @param archiveFile  the target ZIP file to write to
     *
     * @throws IOException if an I/O error occurs while writing the archive
     * @author Illiani
     * @since 0.50.11
     */
    private static void createBugReportArchive(File campaignFile, File archiveFile) throws IOException {
        List<File> inputFiles = new ArrayList<>();
        inputFiles.add(campaignFile);

        File logsDirectory = new File(LOGS_PATH);
        if (!logsDirectory.isDirectory()) {
            LOGGER.warn("Logs directory does not exist or is not a directory: {}", LOGS_PATH);
        } else {
            File[] logFiles = logsDirectory.listFiles(file -> file.isFile()
                                                                    && (file.getName().endsWith(".log") ||
                                                                              file.getName().endsWith(".log.gz")));
            if (logFiles != null && logFiles.length > 0) {
                // Stable order helps reproducibility and makes parts predictable
                List<File> sortedLogFiles = new ArrayList<>(List.of(logFiles));
                sortedLogFiles.sort(Comparator.comparing(File::getName));
                inputFiles.addAll(sortedLogFiles);
            } else {
                LOGGER.info("No .log or .log.gz files found in {}", LOGS_PATH);
            }
        }

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

            for (File inputFile : inputFiles) {
                String entryName = inputFile.equals(campaignFile)
                                         ? campaignFile.getName()
                                         : ("logs/" + inputFile.getName());

                long estimatedAddedBytes = inputFile.length() + ZIP_ENTRY_OVERHEAD_BYTES;

                // If adding this file exceeds the cap, roll to next part (if current part already has something).
                // Note: This is conservative and should keep the actual ZIP size under the limit in practice.
                if (countingOut.getCount() > 0
                          && (countingOut.getCount() + estimatedAddedBytes) > MAX_ARCHIVE_BYTES) {

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

                // If a single file is huge, it may exceed MAX_ARCHIVE_BYTES even alone.
                if (inputFile.length() > (MAX_ARCHIVE_BYTES - ZIP_ENTRY_OVERHEAD_BYTES)) {
                    LOGGER.warn(
                          "File '{}' is larger than the per-archive limit ({} bytes); it will be placed in its own "
                                + "part and may exceed the limit.",
                          inputFile.getName(),
                          MAX_ARCHIVE_BYTES
                    );
                }

                addFileToZip(inputFile, entryName, zipOut);
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

        if (partCount > 1) {
            showTooMuchDataDialog();
        }
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
