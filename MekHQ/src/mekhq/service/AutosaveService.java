/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
    private static final MMLogger LOGGER = MMLogger.create(AutosaveService.class);

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
        } else if (MekHQ.getMHQOptions().getAutosaveWeeklyValue() && (today.getDayOfWeek() == DayOfWeek.SUNDAY)) {
            performAutosave(campaign);
        } else if (MekHQ.getMHQOptions().getAutosaveMonthlyValue() &&
                         (today.getDayOfMonth() == today.lengthOfMonth())) {
            performAutosave(campaign);
        } else if (MekHQ.getMHQOptions().getAutosaveYearlyValue() && (today.getDayOfYear() == today.lengthOfYear())) {
            performAutosave(campaign);
        }
    }

    @Override
    public void requestBeforeScenarioAutosave(final Campaign campaign) {
        Objects.requireNonNull(campaign);

        if (MekHQ.getMHQOptions().getAutosaveBeforeScenariosValue()) {
            performAutosave(campaign);
        }
    }

    @Override
    public void requestBeforeMissionEndAutosave(final Campaign campaign) {
        Objects.requireNonNull(campaign);

        if (MekHQ.getMHQOptions().getAutosaveBeforeMissionEndValue()) {
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
                LOGGER.error("Unable to perform an autosave because of a null or empty file name");
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
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
            final int maxNumberAutoSaves = MekHQ.getMHQOptions().getMaximumNumberOfAutoSavesValue();

            int index = 0;
            while ((autosaveFiles.size() >= maxNumberAutoSaves) && (autosaveFiles.size() > index)) {
                if (autosaveFiles.get(index).delete()) {
                    autosaveFiles.remove(index);
                } else {
                    LOGGER.error("Unable to delete file {}", autosaveFiles.get(index).getName());
                    index++;
                }
            }

            // Find a unique name for this autosave
            String fileName = null;

            boolean repeatedName = true;
            index = 1;
            while (repeatedName) {
                fileName = String.format("Autosave-%d-%s-%s.cpnx.gz",
                      index++,
                      campaign.getName(),
                      campaign.getLocalDate()
                            .format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
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
