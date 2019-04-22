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
import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.campaign.Campaign;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
    public void requestDayAdvanceAutosave(Campaign campaign, int dayOfTheWeek) {
        assert campaign != null;

        if (this.isDailyAutosaveEnabled()) {
            this.performAutosave(campaign);
        } else if (dayOfTheWeek == Calendar.SUNDAY && this.isWeeklyAutosaveEnabled()) {
            this.performAutosave(campaign);
        }
    }

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

    private boolean isMissionAutosaveEnabled() {
        return this.userPreferences.getBoolean(MekHqConstants.SAVE_BEFORE_MISSIONS_KEY, false);
    }

    private void performAutosave(Campaign campaign) {
        try {
            String fileName = this.getAutosaveFilename(campaign);

            try (GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(fileName))) {
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"));
                campaign.writeToXml(writer);
                writer.flush();
                writer.close();
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
        List<File> autosaveFiles = Arrays.stream(folder.listFiles())
                .filter(f -> f.getName().startsWith("Autosave-"))
                .sorted(Comparator.comparing(f -> f.lastModified()))
                .collect(Collectors.toList());

        // Delete older autosave files if needed
        int maxNumberAutosaves = this.userPreferences.getInt(MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY, MekHqConstants.DEFAULT_NUMBER_SAVES);
        while (autosaveFiles.size() >= maxNumberAutosaves && autosaveFiles.size() > 0) {
            autosaveFiles.get(0).delete();
            autosaveFiles.remove(0);
        }

        // Find a unique name for this autosave
        String fileName = null;

        boolean repeatedName = true;
        int index = 0;
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
}
