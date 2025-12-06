package mekhq.campaign.utilities;

import static mekhq.MHQConstants.LOGS_PATH;
import static mekhq.gui.CampaignGUI.saveCampaign;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;

public class EasyBugReport {
    private static final MMLogger LOGGER = MMLogger.create(EasyBugReport.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.EasyBugReport";

    private final Campaign campaign;

    public EasyBugReport(JFrame frame, Campaign campaign) {
        this.campaign = campaign;

        saveCampaignForBugReport(frame);
    }

    public void saveCampaignForBugReport(JFrame frame) {
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
        saveCampaign(frame, campaign, campaignFile, true);

        // Now package campaign + logs into the user-chosen archive
        try {
            File archiveFile = createBugReportArchive(campaignFile, chosenArchiveFile);
            LOGGER.info("Bug report archive created: {}", archiveFile.getName());

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

    private File createBugReportArchive(File campaignFile, File archiveFile) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(archiveFile);
              ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

            // Add the campaign file at the root of the archive
            addFileToZip(campaignFile, campaignFile.getName(), zipOutputStream);

            // Add logs under 'logs/<filename>'
            File logsDirectory = new File(LOGS_PATH);
            if (!logsDirectory.isDirectory()) {
                LOGGER.warn("Logs directory does not exist or is not a directory: {}", LOGS_PATH);
                return archiveFile;
            }

            File[] logFiles = logsDirectory.listFiles(f -> f.isFile() &&
                                                                 (f.getName().endsWith(".log") ||
                                                                        f.getName().endsWith(".log.gz")));

            if (logFiles == null || logFiles.length == 0) {
                LOGGER.info("No .log or .log.gz files found in {}", LOGS_PATH);
                return archiveFile;
            }

            for (File logFile : logFiles) {
                String entryName = "logs/" + logFile.getName();
                addFileToZip(logFile, entryName, zipOutputStream);
            }
        }

        return archiveFile;
    }

    private void addFileToZip(File source, String entryName, ZipOutputStream zipOutputStream) throws IOException {
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
