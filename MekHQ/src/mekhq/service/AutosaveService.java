/*
 * AutosaveService.java
 *
 * Copyright (c) 2019 MekHQ Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.service;

import megamek.common.logging.MMLogger;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.campaign.Campaign;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class AutosaveService implements IAutosaveService {
    private final Preferences userPreferences = Preferences.userRoot().node(MekHqConstants.AUTOSAVE_NODE);
    private final MMLogger logger;

    public AutosaveService(MMLogger logger) {
        assert logger != null;

        this.logger = logger;
    }

    @Override
    public void requestDayAdvanceAutosave(Campaign campaign, Calendar calendar) {
        assert campaign != null;

        if (this.isDailyAutosaveEnabled()) {
            this.performAutosave(campaign);
        } else if (this.isWeeklyAutosaveEnabled()
                && (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)) {
            this.performAutosave(campaign);
        } else if (this.isMonthlyAutosaveEnabled()
                && (calendar.get(Calendar.DAY_OF_MONTH) == calendar.getActualMaximum(Calendar.DAY_OF_MONTH))) {
            this.performAutosave(campaign);
        } else if (this.isYearlyAutosaveEnabled()
                && (calendar.get(Calendar.DAY_OF_YEAR) == calendar.getActualMaximum(Calendar.DAY_OF_YEAR))) {
            this.performAutosave(campaign);
        }
    }

    @Override
    public void requestBeforeMissionAutosave(Campaign campaign) {
        assert campaign != null;

        if (this.isMissionAutosaveEnabled()) {
            this.performAutosave(campaign);
        }
    }

    private boolean isDailyAutosaveEnabled() {
        return this.userPreferences.getBoolean(MekHqConstants.SAVE_DAILY_KEY, false);
    }

    private boolean isWeeklyAutosaveEnabled() {
        return this.userPreferences.getBoolean(MekHqConstants.SAVE_WEEKLY_KEY, false);
    }

    private boolean isMonthlyAutosaveEnabled() {
        return this.userPreferences.getBoolean(MekHqConstants.SAVE_MONTHLY_KEY, false);
    }

    private boolean isYearlyAutosaveEnabled() {
        return this.userPreferences.getBoolean(MekHqConstants.SAVE_YEARLY_KEY, false);
    }

    private boolean isMissionAutosaveEnabled() {
        return this.userPreferences.getBoolean(MekHqConstants.SAVE_BEFORE_MISSIONS_KEY, false);
    }

    private void performAutosave(Campaign campaign) {
        try {
            String fileName = this.getAutosaveFilename(campaign);
            if (!StringUtil.isNullOrEmpty(fileName)) {
                try (FileOutputStream fos = new FileOutputStream(fileName);
                     GZIPOutputStream output = new GZIPOutputStream(fos)) {
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
                    campaign.writeToXml(writer);
                    writer.flush();
                    writer.close();
                }
            } else {
                this.logger.error(this.getClass(), "performAutosave",
                        "Unable to perform an autosave because of a null or empty file name");
            }
        }
        catch (Exception ex) {
            this.logger.error(this.getClass(), "performAutosave", ex);
        }
    }

    private String getAutosaveFilename(Campaign campaign) {
        // Get all autosave files in ascending order of date creation
        String savesDirectoryPath = MekHQ.getCampaignsDirectory().getValue();
        File folder = new File(savesDirectoryPath);
        File[] files = folder.listFiles();
        if (files != null) {
            List<File> autosaveFiles = Arrays.stream(files)
                    .filter(f -> f.getName().startsWith("Autosave-"))
                    .sorted(Comparator.comparing(File::lastModified))
                    .collect(Collectors.toList());

            // Delete older autosave files if needed
            int maxNumberAutosaves = this.userPreferences.getInt(MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY,
                    MekHqConstants.DEFAULT_NUMBER_SAVES);

            int index = 0;
            while (autosaveFiles.size() >= maxNumberAutosaves && autosaveFiles.size() > index) {
                if (autosaveFiles.get(index).delete()) {
                    autosaveFiles.remove(index);
                } else {
                    this.logger.error(this.getClass(), "getAutosaveFilename",
                            "Unable to delete file " + autosaveFiles.get(index).getName());
                    index++;
                }
            }

            // Find a unique name for this autosave
            String fileName = null;

            boolean repeatedName = true;
            index = 1;
            while (repeatedName) {
                fileName = String.format(
                        "Autosave-%d-%s-%s.cpnx.gz",
                        index++,
                        campaign.getName(),
                        campaign.getShortDateAsString());

                repeatedName = false;
                for (File file : autosaveFiles) {
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
