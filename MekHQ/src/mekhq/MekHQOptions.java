/*
 * Copyright (c) 2020 - The MekHQ Team. All Rights Reserved.
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
package mekhq;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;

public final class MekHQOptions {
    private static final Preferences userPreferences = Preferences.userRoot();

    //region Display
    public String getDisplayDateFormat() {
        return userPreferences.node(MekHqConstants.DISPLAY_NODE).get(MekHqConstants.DISPLAY_DATE_FORMAT, "yyyy-MM-dd");
    }

    public String getDisplayFormattedDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(getDisplayDateFormat()));
    }

    public void setDisplayDateFormat(String value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).put(MekHqConstants.DISPLAY_DATE_FORMAT, value);
    }

    public LocalDate parseDisplayFormattedDate(String text) {
        return LocalDate.parse(text, DateTimeFormatter.ofPattern(getDisplayDateFormat()));
    }

    public String getLongDisplayDateFormat() {
        return userPreferences.node(MekHqConstants.DISPLAY_NODE).get(MekHqConstants.LONG_DISPLAY_DATE_FORMAT, "EEEE, MMMM d, yyyy");
    }

    public String getLongDisplayFormattedDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(getLongDisplayDateFormat()));
    }

    public void setLongDisplayDateFormat(String value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).put(MekHqConstants.LONG_DISPLAY_DATE_FORMAT, value);
    }

    public boolean getHistoricalDailyLog() {
        return userPreferences.node(MekHqConstants.DISPLAY_NODE).getBoolean(MekHqConstants.HISTORICAL_DAILY_LOG, false);
    }

    public void setHistoricalDailyLog(boolean value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putBoolean(MekHqConstants.HISTORICAL_DAILY_LOG, value);
    }

    //region Command Center Display
    public boolean getCommandCenterUseUnitMarket() {
        return userPreferences.node(MekHqConstants.DISPLAY_NODE).getBoolean(MekHqConstants.COMMAND_CENTER_USE_UNIT_MARKET, true);
    }

    public void setCommandCenterUseUnitMarket(boolean value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putBoolean(MekHqConstants.COMMAND_CENTER_USE_UNIT_MARKET, value);
    }

    public boolean getCommandCenterMRMS() {
        return userPreferences.node(MekHqConstants.DISPLAY_NODE).getBoolean(MekHqConstants.COMMAND_CENTER_MRMS, false);
    }

    public void setCommandCenterMRMS(boolean value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putBoolean(MekHqConstants.COMMAND_CENTER_MRMS, value);
    }
    //endregion Command Center Display
    //endregion Display

    //region Autosave
    public boolean getNoAutosaveValue() {
        return userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.NO_SAVE_KEY, false);
    }

    public void setNoAutosaveValue(boolean value) {
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.NO_SAVE_KEY, value);
    }

    public boolean getAutosaveDailyValue() {
        return userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_DAILY_KEY, false);
    }

    public void setAutosaveDailyValue(boolean value) {
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_DAILY_KEY, value);
    }

    public boolean getAutosaveWeeklyValue() {
        return userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_WEEKLY_KEY, true);
    }

    public void setAutosaveWeeklyValue(boolean value) {
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_WEEKLY_KEY, value);
    }

    public boolean getAutosaveMonthlyValue() {
        return userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_MONTHLY_KEY, false);
    }

    public void setAutosaveMonthlyValue(boolean value) {
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_MONTHLY_KEY, value);
    }

    public boolean getAutosaveYearlyValue() {
        return userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_YEARLY_KEY, false);
    }

    public void setAutosaveYearlyValue(boolean value) {
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_YEARLY_KEY, value);
    }

    public boolean getAutosaveBeforeMissionsValue() {
        return userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getBoolean(MekHqConstants.SAVE_BEFORE_MISSIONS_KEY, false);
    }

    public void setAutosaveBeforeMissionsValue(boolean value) {
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putBoolean(MekHqConstants.SAVE_BEFORE_MISSIONS_KEY, value);
    }

    public int getMaximumNumberOfAutosavesValue() {
        return userPreferences.node(MekHqConstants.AUTOSAVE_NODE).getInt(MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY,
                MekHqConstants.DEFAULT_NUMBER_SAVES);
    }

    public void setMaximumNumberOfAutosavesValue(int value) {
        userPreferences.node(MekHqConstants.AUTOSAVE_NODE).putInt(MekHqConstants.MAXIMUM_NUMBER_SAVES_KEY, value);
    }
    //endregion Autosave

    //region New Day
    public boolean getNewDayMRMS() {
        return userPreferences.node(MekHqConstants.NEW_DAY_NODE).getBoolean(MekHqConstants.NEW_DAY_MRMS, false);
    }

    public void setNewDayMRMS(boolean value) {
        userPreferences.node(MekHqConstants.NEW_DAY_NODE).putBoolean(MekHqConstants.NEW_DAY_MRMS, value);
    }
    //endregion New Day

    //region Campaign XML Save Options
    /**
     * @return A value indicating if the campaign should be written to a gzipped file, if possible.
     */
    public boolean getPreferGzippedOutput() {
        return userPreferences.node(MekHqConstants.XML_SAVES_NODE).getBoolean(MekHqConstants.PREFER_GZIPPED_CAMPAIGN_FILE, true);
    }

    /**
     * Sets a hint indicating that the campaign should be gzipped, if possible.
     * This allows the Save dialog to present the user with the correct file
     * type on subsequent saves.
     *
     * @param value A value indicating whether or not the campaign should be gzipped if possible.
     */
    public void setPreferGzippedOutput(boolean value) {
        userPreferences.node(MekHqConstants.XML_SAVES_NODE).putBoolean(MekHqConstants.PREFER_GZIPPED_CAMPAIGN_FILE, value);
    }

    public boolean getWriteCustomsToXML() {
        return userPreferences.node(MekHqConstants.XML_SAVES_NODE).getBoolean(MekHqConstants.WRITE_CUSTOMS_TO_XML, true);
    }

    public void setWriteCustomsToXML(boolean value) {
        userPreferences.node(MekHqConstants.XML_SAVES_NODE).putBoolean(MekHqConstants.WRITE_CUSTOMS_TO_XML, value);
    }
    //endregion Campaign XML Save Options

    //region Miscellaneous Options
    public int getStartGameDelay() {
        return userPreferences.node(MekHqConstants.MISCELLANEOUS_NODE).getInt(MekHqConstants.START_GAME_DELAY, 500);
    }

    public void setStartGameDelay(int startGameDelay) {
        userPreferences.node(MekHqConstants.MISCELLANEOUS_NODE).putInt(MekHqConstants.START_GAME_DELAY, startGameDelay);
    }
    //endregion Miscellaneous Options
}
