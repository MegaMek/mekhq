/*
 * AutosaveService.java
 *
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;

public class AutosaveService implements IAutosaveService {
    private static final MMLogger logger = MMLogger.create(AutosaveService.class);

    // region Constructors
    public AutosaveService() {

    }
    // endregion Constructors

    @Override
    public void requestDayAdvanceAutosave(final Campaign campaign) {
        Objects.requireNonNull(campaign);

        final LocalDate today = campaign.getLocalDate();
        if (MekHQ.getMHQOptions().getAutosaveDailyValue()) {
            performAutosave(campaign);
        } else if (MekHQ.getMHQOptions().getAutosaveWeeklyValue()
                && (today.getDayOfWeek() == DayOfWeek.SUNDAY)) {
            performAutosave(campaign);
        } else if (MekHQ.getMHQOptions().getAutosaveMonthlyValue()
                && (today.getDayOfMonth() == today.lengthOfMonth())) {
            performAutosave(campaign);
        } else if (MekHQ.getMHQOptions().getAutosaveYearlyValue()
                && (today.getDayOfYear() == today.lengthOfYear())) {
            performAutosave(campaign);
        }
    }

    @Override
    public void requestBeforeMissionAutosave(final Campaign campaign) {
        Objects.requireNonNull(campaign);

        if (MekHQ.getMHQOptions().getAutosaveBeforeMissionsValue()) {
            performAutosave(campaign);
        }
    }

    private void performAutosave(final Campaign campaign) {
        try {
            final String fileName = getAutosaveFilename(campaign);
            if (!StringUtility.isNullOrBlank(fileName)) {
                try (FileOutputStream fos = new FileOutputStream(fileName);
                        GZIPOutputStream gos = new GZIPOutputStream(fos);
                        OutputStreamWriter osw = new OutputStreamWriter(gos, StandardCharsets.UTF_8);
                        PrintWriter writer = new PrintWriter(osw)) {
                    campaign.writeToXML(writer);
                    writer.flush();
                }
            } else {
                logger.error("Unable to perform an autosave because of a null or empty file name");
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    private @Nullable String getAutosaveFilename(final Campaign campaign) {
        // Get all autosave files in ascending order of date creation
        final String savesDirectoryPath = MekHQ.getCampaignsDirectory().getValue();
        final File folder = new File(savesDirectoryPath);
        final File[] files = folder.listFiles();
        if (files != null) {
            final List<File> autosaveFiles = Arrays.stream(files)
                    .filter(f -> f.getName().startsWith("Autosave-"))
                    .sorted(Comparator.comparing(File::lastModified))
                    .collect(Collectors.toList());

            // Delete older autosave files if needed
            final int maxNumberAutosaves = MekHQ.getMHQOptions().getMaximumNumberOfAutosavesValue();

            int index = 0;
            while ((autosaveFiles.size() >= maxNumberAutosaves) && (autosaveFiles.size() > index)) {
                if (autosaveFiles.get(index).delete()) {
                    autosaveFiles.remove(index);
                } else {
                    logger.error("Unable to delete file " + autosaveFiles.get(index).getName());
                    index++;
                }
            }

            // Find a unique name for this autosave
            String fileName = null;

            boolean repeatedName = true;
            index = 1;
            while (repeatedName) {
                fileName = String.format("Autosave-%d-%s-%s.cpnx.gz", index++, campaign.getName(),
                        campaign.getLocalDate().format(DateTimeFormatter
                                .ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                .withLocale(MekHQ.getMHQOptions().getDateLocale())));

                repeatedName = false;
                for (final File file : autosaveFiles) {
                    if (file.getName().compareToIgnoreCase(fileName) == 0) {
                        repeatedName = true;
                        break;
                    }
                }
            }
            return Paths.get(savesDirectoryPath, fileName).toString();
        }

        return null;
    }
}
