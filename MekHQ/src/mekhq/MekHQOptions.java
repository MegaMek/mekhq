/*
 * Copyright (c) 2020-2021 - The MekHQ Team. All Rights Reserved.
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

import mekhq.gui.enums.PersonnelFilterStyle;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;

public final class MekHQOptions {
    //region Variable Declarations
    private static final Preferences userPreferences = Preferences.userRoot();
    //endregion Variable Declarations

    //region Display
    public String getDisplayDateFormat() {
        return userPreferences.node(MekHqConstants.DISPLAY_NODE).get(MekHqConstants.DISPLAY_DATE_FORMAT, "yyyy-MM-dd");
    }

    public String getDisplayFormattedDate(LocalDate date) {
        return (date != null) ? date.format(DateTimeFormatter.ofPattern(getDisplayDateFormat())) : "";
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
        return (date != null) ? date.format(DateTimeFormatter.ofPattern(getLongDisplayDateFormat())) : "";
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

    //region Expanded MekHQ Display Options
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

    //region Personnel Tab Display Options
    public PersonnelFilterStyle getPersonnelFilterStyle() {
        return PersonnelFilterStyle.valueOf(userPreferences.node(MekHqConstants.DISPLAY_NODE)
                .get(MekHqConstants.PERSONNEL_FILTER_STYLE, "STANDARD"));
    }

    public void setPersonnelFilterStyle(PersonnelFilterStyle value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).put(MekHqConstants.PERSONNEL_FILTER_STYLE, value.name());
    }

    public boolean getPersonnelFilterOnPrimaryRole() {
        return userPreferences.node(MekHqConstants.DISPLAY_NODE).getBoolean(MekHqConstants.PERSONNEL_FILTER_ON_PRIMARY_ROLE, false);
    }

    public void setPersonnelFilterOnPrimaryRole(boolean value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putBoolean(MekHqConstants.PERSONNEL_FILTER_ON_PRIMARY_ROLE, value);
    }
    //endregion Personnel Tab Display Options
    //endregion Expanded MekHQ Display Options

    //region Colours
    public Color getDeployedForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.DEPLOYED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setDeployedForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.DEPLOYED_FOREGROUND, value.getRGB());
    }

    public Color getDeployedBackground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.DEPLOYED_BACKGROUND, Color.LIGHT_GRAY.getRGB()));
    }

    public void setDeployedBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.DEPLOYED_BACKGROUND, value.getRGB());
    }

    public Color getBelowContractMinimumForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.BELOW_CONTRACT_MINIMUM_FOREGROUND, Color.RED.getRGB()));
    }

    public void setBelowContractMinimumForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.BELOW_CONTRACT_MINIMUM_FOREGROUND, value.getRGB());
    }

    public Color getBelowContractMinimumBackground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.BELOW_CONTRACT_MINIMUM_BACKGROUND, UIManager.getColor("Table.background").getRGB()));
    }

    public void setBelowContractMinimumBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.BELOW_CONTRACT_MINIMUM_BACKGROUND, value.getRGB());
    }

    public Color getInTransitForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.IN_TRANSIT_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setInTransitForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.IN_TRANSIT_FOREGROUND, value.getRGB());
    }

    public Color getInTransitBackground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.IN_TRANSIT_BACKGROUND, Color.MAGENTA.getRGB()));
    }

    public void setInTransitBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.IN_TRANSIT_BACKGROUND, value.getRGB());
    }

    public Color getRefittingForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.REFITTING_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setRefittingForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.REFITTING_FOREGROUND, value.getRGB());
    }

    public Color getRefittingBackground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.REFITTING_BACKGROUND, Color.CYAN.getRGB()));
    }

    public void setRefittingBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.REFITTING_BACKGROUND, value.getRGB());
    }

    public Color getMothballingForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.MOTHBALLING_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setMothballingForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.MOTHBALLING_FOREGROUND, value.getRGB());
    }

    public Color getMothballingBackground() {
        // new Color(153, 153, 255)
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.MOTHBALLING_BACKGROUND, 0xFF9999FF));
    }

    public void setMothballingBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.MOTHBALLING_BACKGROUND, value.getRGB());
    }

    public Color getMothballedForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.MOTHBALLED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setMothballedForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.MOTHBALLED_FOREGROUND, value.getRGB());
    }

    public Color getMothballedBackground() {
        // new Color(204, 204, 255)
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.MOTHBALLED_BACKGROUND, 0xFFCCCCFF));
    }

    public void setMothballedBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.MOTHBALLED_BACKGROUND, value.getRGB());
    }

    public Color getNotRepairableForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.NOT_REPAIRABLE_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setNotRepairableForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.NOT_REPAIRABLE_FOREGROUND, value.getRGB());
    }

    public Color getNotRepairableBackground() {
        // new Color(190, 150, 55)
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.NOT_REPAIRABLE_BACKGROUND, 0xFFBE9637));
    }

    public void setNotRepairableBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.NOT_REPAIRABLE_BACKGROUND, value.getRGB());
    }

    public Color getNonFunctionalForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.NON_FUNCTIONAL_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setNonFunctionalForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.NON_FUNCTIONAL_FOREGROUND, value.getRGB());
    }

    public Color getNonFunctionalBackground() {
        // new Color(205, 92, 92)
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.NON_FUNCTIONAL_BACKGROUND, 0xFFCD5C5C));
    }

    public void setNonFunctionalBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.NON_FUNCTIONAL_BACKGROUND, value.getRGB());
    }

    public Color getNeedsPartsFixedForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.NEEDS_PARTS_FIXED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setNeedsPartsFixedForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.NEEDS_PARTS_FIXED_FOREGROUND, value.getRGB());
    }

    public Color getNeedsPartsFixedBackground() {
        // new Color(238, 238, 0)
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.NEEDS_PARTS_FIXED_BACKGROUND, 0xFFEEEE00));
    }

    public void setNeedsPartsFixedBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.NEEDS_PARTS_FIXED_BACKGROUND, value.getRGB());
    }

    public Color getUnmaintainedForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.UNMAINTAINED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setUnmaintainedForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.UNMAINTAINED_FOREGROUND, value.getRGB());
    }

    public Color getUnmaintainedBackground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.UNMAINTAINED_BACKGROUND, Color.ORANGE.getRGB()));
    }

    public void setUnmaintainedBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.UNMAINTAINED_BACKGROUND, value.getRGB());
    }

    public Color getUncrewedForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.UNCREWED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setUncrewedForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.UNCREWED_FOREGROUND, value.getRGB());
    }

    public Color getUncrewedBackground() {
        // new Color(218, 130, 255)
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.UNCREWED_BACKGROUND, 0xFFDA82FF));
    }

    public void setUncrewedBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.UNCREWED_BACKGROUND, value.getRGB());
    }

    public Color getLoanOverdueForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.LOAN_OVERDUE_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setLoanOverdueForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.LOAN_OVERDUE_FOREGROUND, value.getRGB());
    }

    public Color getLoanOverdueBackground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.LOAN_OVERDUE_BACKGROUND, Color.RED.getRGB()));
    }

    public void setLoanOverdueBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.LOAN_OVERDUE_BACKGROUND, value.getRGB());
    }

    public Color getInjuredForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.INJURED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setInjuredForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.INJURED_FOREGROUND, value.getRGB());
    }

    public Color getInjuredBackground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.INJURED_BACKGROUND, Color.RED.getRGB()));
    }

    public void setInjuredBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.INJURED_BACKGROUND, value.getRGB());
    }

    public Color getHealedInjuriesForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.HEALED_INJURIES_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setHealedInjuriesForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.HEALED_INJURIES_FOREGROUND, value.getRGB());
    }

    public Color getHealedInjuriesBackground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.HEALED_INJURIES_BACKGROUND, 0xEE9A00));
    }

    public void setHealedInjuriesBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.HEALED_INJURIES_BACKGROUND, value.getRGB());
    }

    public Color getPaidRetirementForeground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.PAID_RETIREMENT_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setPaidRetirementForeground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.PAID_RETIREMENT_FOREGROUND, value.getRGB());
    }

    public Color getPaidRetirementBackground() {
        return new Color(userPreferences.node(MekHqConstants.DISPLAY_NODE).getInt(MekHqConstants.PAID_RETIREMENT_BACKGROUND, Color.LIGHT_GRAY.getRGB()));
    }

    public void setPaidRetirementBackground(Color value) {
        userPreferences.node(MekHqConstants.DISPLAY_NODE).putInt(MekHqConstants.PAID_RETIREMENT_BACKGROUND, value.getRGB());
    }
    //endregion Colours
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
    public boolean getNewDayAstechPoolFill() {
        return userPreferences.node(MekHqConstants.NEW_DAY_NODE).getBoolean(MekHqConstants.NEW_DAY_ASTECH_POOL_FILL, true);
    }

    public void setNewDayAstechPoolFill(final boolean value) {
        userPreferences.node(MekHqConstants.NEW_DAY_NODE).putBoolean(MekHqConstants.NEW_DAY_ASTECH_POOL_FILL, value);
    }

    public boolean getNewDayMedicPoolFill() {
        return userPreferences.node(MekHqConstants.NEW_DAY_NODE).getBoolean(MekHqConstants.NEW_DAY_MEDIC_POOL_FILL, true);
    }

    public void setNewDayMedicPoolFill(final boolean value) {
        userPreferences.node(MekHqConstants.NEW_DAY_NODE).putBoolean(MekHqConstants.NEW_DAY_MEDIC_POOL_FILL, value);
    }

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

    public boolean getSaveMothballState() {
        return userPreferences.node(MekHqConstants.XML_SAVES_NODE).getBoolean(MekHqConstants.SAVE_MOTHBALL_STATE, true);
    }

    public void setSaveMothballState(boolean value) {
        userPreferences.node(MekHqConstants.XML_SAVES_NODE).putBoolean(MekHqConstants.SAVE_MOTHBALL_STATE, value);
    }
    //endregion Campaign XML Save Options

    //region File Paths
    /**
     * @return the path of the folder to load when loading or saving rank systems
     */
    public String getRankSystemsPath() {
        return userPreferences.node(MekHqConstants.FILE_PATH_NODE).get(MekHqConstants.RANK_SYSTEMS_DIRECTORY_PATH, "userdata/data/universe/");
    }

    /**
     * This sets the path where one saves or loads their rank systems from, as this is not required
     * for any data but improves UX.
     *
     * @param value the path where the person saved their last individual rank system.
     */
    public void setRankSystemsPath(final String value) {
        userPreferences.node(MekHqConstants.FILE_PATH_NODE).put(MekHqConstants.RANK_SYSTEMS_DIRECTORY_PATH, value);
    }

    /**
     * @return the path of the folder to load when loading or saving an individual rank system
     */
    public String getIndividualRankSystemPath() {
        return userPreferences.node(MekHqConstants.FILE_PATH_NODE).get(MekHqConstants.INDIVIDUAL_RANK_SYSTEM_DIRECTORY_PATH, "userdata/data/universe/");
    }

    /**
     * This sets the path where one saves or loads their individual rank system, as this is not
     * required for any data but improves UX.
     *
     * @param value the path where the person saved their last individual rank system.
     */
    public void setIndividualRankSystemPath(final String value) {
        userPreferences.node(MekHqConstants.FILE_PATH_NODE).put(MekHqConstants.INDIVIDUAL_RANK_SYSTEM_DIRECTORY_PATH, value);
    }
    //endregion File Paths

    //region Nag Tab
    public boolean getNagDialogIgnore(final String key) {
        return userPreferences.node(MekHqConstants.NAG_NODE).getBoolean(key, false);
    }

    public void setNagDialogIgnore(final String key, final boolean value) {
        userPreferences.node(MekHqConstants.NAG_NODE).putBoolean(key, value);
    }
    //endregion Nag Tab

    //region Miscellaneous Options
    public int getStartGameDelay() {
        return userPreferences.node(MekHqConstants.MISCELLANEOUS_NODE).getInt(MekHqConstants.START_GAME_DELAY, 500);
    }

    public void setStartGameDelay(int startGameDelay) {
        userPreferences.node(MekHqConstants.MISCELLANEOUS_NODE).putInt(MekHqConstants.START_GAME_DELAY, startGameDelay);
    }
    //endregion Miscellaneous Options
}
