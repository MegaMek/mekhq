/*
 * MekHqPreferences.java
 *
 * Copyright (c) 2019 MekHQ team. All rights reserved.
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

package mekhq;

import mekhq.preferences.PersonnelMarketPreferences;
import mekhq.preferences.UnitMarketPreferences;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Manages all user preferences related to MekHQ.
 * Preferences are stored as string, but they are exposed as strongly typed.
 */
public class MekHqPreferences {
    private static final String LOOK_AND_FEEL = "laf";
    private static final String CAMPAIGNS_DIRECTORY = "CampaignsDirectory";

    private static final String DEFAULT_LOOK_AND_FEEL = UIManager.getSystemLookAndFeelClassName();
    private static final String DEFAULT_CAMPAIGNS_DIRECTORY = "./campaigns/";

    private Properties preferences;
    private PersonnelMarketPreferences personnelMarketPreferences;
    private UnitMarketPreferences unitMarketPreferences;

    public MekHqPreferences() {
        this.preferences = new Properties();

        // Default values for top level preferences
        this.preferences.setProperty(LOOK_AND_FEEL, DEFAULT_LOOK_AND_FEEL);
        this.preferences.setProperty(CAMPAIGNS_DIRECTORY, DEFAULT_CAMPAIGNS_DIRECTORY);

        // Create nested preferences
        this.personnelMarketPreferences = new PersonnelMarketPreferences(this.preferences);
        this.unitMarketPreferences = new UnitMarketPreferences(this.preferences);
    }

    public void loadFromFile(String file) {
        final String METHOD_NAME = "loadFromFile";

        try {
            try (FileInputStream input = new FileInputStream(file)) {
                MekHQ.getLogger().debug(
                        MekHqPreferences.class,
                        METHOD_NAME,
                        "Loading MekHQ preferences from: " + file);
                this.preferences.load(input);
            }
        } catch (FileNotFoundException e) {
            MekHQ.getLogger().error(
                    MekHqPreferences.class,
                    METHOD_NAME,
                    "No MekHQ preferences file found: " + file + ". Reverting to defaults.",
                    e);
        } catch (IOException e) {
            MekHQ.getLogger().error(
                    MekHqPreferences.class,
                    METHOD_NAME,
                    "Error reading from the preferences file: " + file + ". Reverting to defaults.",
                    e);
        }
    }

    public void saveToFile(String file) {
        final String METHOD_NAME = "saveToFile";

        try {
            try (FileOutputStream output = new FileOutputStream(file)) {
                MekHQ.getLogger().debug(
                        MekHqPreferences.class,
                        METHOD_NAME,
                        "Saving MekHQ preferences to: " + file);
                this.preferences.store(output, "MekHQ Preferences");
            }
        } catch (FileNotFoundException e) {
            MekHQ.getLogger().error(
                    MekHqPreferences.class,
                    METHOD_NAME,
                    "Could not save preferences to: " + file,
                    e);
        } catch (IOException e) {
            MekHQ.getLogger().error(
                    MekHqPreferences.class,
                    METHOD_NAME,
                    "Error writing to the preferences file: " + file,
                    e);
        }
    }

    public PersonnelMarketPreferences forPersonnelMarket() {
        return this.personnelMarketPreferences;
    }

    public UnitMarketPreferences forUnitMarket() {
        return this.unitMarketPreferences;
    }

    public String getLookAndFeel() {
        return this.preferences.getProperty(LOOK_AND_FEEL);
    }

    public void setLookAndFeel(String laf) {
        assert laf != null && laf.trim().length() > 0;
        this.preferences.setProperty(LOOK_AND_FEEL, laf);
    }

    public String getCampaignsDirectory() {
        return this.preferences.getProperty(CAMPAIGNS_DIRECTORY);
    }

    public void setCampaignsDirectory(String path) {
        assert path != null && path.trim().length() > 0;
        this.preferences.setProperty(CAMPAIGNS_DIRECTORY, path);
    }
}
