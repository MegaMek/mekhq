/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.SuiteOptions;
import megamek.common.annotations.Nullable;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.gui.enums.ForceIconOperationalStatusStyle;
import mekhq.gui.enums.PersonnelFilterStyle;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class MHQOptions extends SuiteOptions {
    //region Display Tab
    public String getDisplayDateFormat() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).get(MHQConstants.DISPLAY_DATE_FORMAT, "yyyy-MM-dd");
    }

    public String getDisplayFormattedDate(final @Nullable LocalDate date) {
        return (date != null) ? date.format(DateTimeFormatter.ofPattern(getDisplayDateFormat()).withLocale(getDateLocale())) : "";
    }

    public void setDisplayDateFormat(String value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).put(MHQConstants.DISPLAY_DATE_FORMAT, value);
    }

    public LocalDate parseDisplayFormattedDate(String text) {
        return LocalDate.parse(text, DateTimeFormatter.ofPattern(getDisplayDateFormat()).withLocale(getDateLocale()));
    }

    public String getLongDisplayDateFormat() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).get(MHQConstants.LONG_DISPLAY_DATE_FORMAT, "EEEE, MMMM d, yyyy");
    }

    public String getLongDisplayFormattedDate(LocalDate date) {
        return (date != null) ? date.format(DateTimeFormatter.ofPattern(getLongDisplayDateFormat()).withLocale(getDateLocale())) : "";
    }

    public void setLongDisplayDateFormat(String value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).put(MHQConstants.LONG_DISPLAY_DATE_FORMAT, value);
    }

    public boolean getHistoricalDailyLog() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.HISTORICAL_DAILY_LOG, false);
    }

    public void setHistoricalDailyLog(boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putBoolean(MHQConstants.HISTORICAL_DAILY_LOG, value);
    }

    public boolean getCompanyGeneratorStartup() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.COMPANY_GENERATOR_STARTUP, false);
    }

    public void setCompanyGeneratorStartup(final boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putBoolean(MHQConstants.COMPANY_GENERATOR_STARTUP, value);
    }

    public boolean getShowCompanyGenerator() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.SHOW_COMPANY_GENERATOR, true);
    }

    public void setShowCompanyGenerator(final boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putBoolean(MHQConstants.SHOW_COMPANY_GENERATOR, value);
    }

    //region Command Center Display
    public boolean getCommandCenterUseUnitMarket() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.COMMAND_CENTER_USE_UNIT_MARKET, true);
    }

    public void setCommandCenterUseUnitMarket(boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putBoolean(MHQConstants.COMMAND_CENTER_USE_UNIT_MARKET, value);
    }

    public boolean getCommandCenterMRMS() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.COMMAND_CENTER_MRMS, false);
    }

    public void setCommandCenterMRMS(boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putBoolean(MHQConstants.COMMAND_CENTER_MRMS, value);
    }
    //endregion Command Center Display

    //region Personnel Tab Display Options
    public PersonnelFilterStyle getPersonnelFilterStyle() {
        return PersonnelFilterStyle.valueOf(userPreferences.node(MHQConstants.DISPLAY_NODE)
                .get(MHQConstants.PERSONNEL_FILTER_STYLE, PersonnelFilterStyle.STANDARD.name()));
    }

    public void setPersonnelFilterStyle(PersonnelFilterStyle value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).put(MHQConstants.PERSONNEL_FILTER_STYLE, value.name());
    }

    public boolean getPersonnelFilterOnPrimaryRole() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.PERSONNEL_FILTER_ON_PRIMARY_ROLE, false);
    }

    public void setPersonnelFilterOnPrimaryRole(boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putBoolean(MHQConstants.PERSONNEL_FILTER_ON_PRIMARY_ROLE, value);
    }
    //endregion Personnel Tab Display Options
    //endregion Display Tab

    //region Colours
    public Color getDeployedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.DEPLOYED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setDeployedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.DEPLOYED_FOREGROUND, value.getRGB());
    }

    public Color getDeployedBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.DEPLOYED_BACKGROUND, Color.LIGHT_GRAY.getRGB()));
    }

    public void setDeployedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.DEPLOYED_BACKGROUND, value.getRGB());
    }

    public Color getBelowContractMinimumForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.BELOW_CONTRACT_MINIMUM_FOREGROUND, Color.RED.getRGB()));
    }

    public void setBelowContractMinimumForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.BELOW_CONTRACT_MINIMUM_FOREGROUND, value.getRGB());
    }

    public Color getBelowContractMinimumBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.BELOW_CONTRACT_MINIMUM_BACKGROUND, UIManager.getColor("Table.background").getRGB()));
    }

    public void setBelowContractMinimumBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.BELOW_CONTRACT_MINIMUM_BACKGROUND, value.getRGB());
    }

    public Color getInTransitForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.IN_TRANSIT_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setInTransitForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.IN_TRANSIT_FOREGROUND, value.getRGB());
    }

    public Color getInTransitBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.IN_TRANSIT_BACKGROUND, Color.MAGENTA.getRGB()));
    }

    public void setInTransitBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.IN_TRANSIT_BACKGROUND, value.getRGB());
    }

    public Color getRefittingForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.REFITTING_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setRefittingForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.REFITTING_FOREGROUND, value.getRGB());
    }

    public Color getRefittingBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.REFITTING_BACKGROUND, Color.CYAN.getRGB()));
    }

    public void setRefittingBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.REFITTING_BACKGROUND, value.getRGB());
    }

    public Color getMothballingForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.MOTHBALLING_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setMothballingForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.MOTHBALLING_FOREGROUND, value.getRGB());
    }

    public Color getMothballingBackground() {
        // new Color(153, 153, 255)
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.MOTHBALLING_BACKGROUND, 0xFF9999FF));
    }

    public void setMothballingBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.MOTHBALLING_BACKGROUND, value.getRGB());
    }

    public Color getMothballedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.MOTHBALLED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setMothballedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.MOTHBALLED_FOREGROUND, value.getRGB());
    }

    public Color getMothballedBackground() {
        // new Color(204, 204, 255)
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.MOTHBALLED_BACKGROUND, 0xFFCCCCFF));
    }

    public void setMothballedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.MOTHBALLED_BACKGROUND, value.getRGB());
    }

    public Color getNotRepairableForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NOT_REPAIRABLE_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setNotRepairableForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NOT_REPAIRABLE_FOREGROUND, value.getRGB());
    }

    public Color getNotRepairableBackground() {
        // new Color(190, 150, 55)
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NOT_REPAIRABLE_BACKGROUND, 0xFFBE9637));
    }

    public void setNotRepairableBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NOT_REPAIRABLE_BACKGROUND, value.getRGB());
    }

    public Color getNonFunctionalForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NON_FUNCTIONAL_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setNonFunctionalForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NON_FUNCTIONAL_FOREGROUND, value.getRGB());
    }

    public Color getNonFunctionalBackground() {
        // new Color(205, 92, 92)
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NON_FUNCTIONAL_BACKGROUND, 0xFFCD5C5C));
    }

    public void setNonFunctionalBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NON_FUNCTIONAL_BACKGROUND, value.getRGB());
    }

    public Color getNeedsPartsFixedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NEEDS_PARTS_FIXED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setNeedsPartsFixedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NEEDS_PARTS_FIXED_FOREGROUND, value.getRGB());
    }

    public Color getNeedsPartsFixedBackground() {
        // new Color(238, 238, 0)
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NEEDS_PARTS_FIXED_BACKGROUND, 0xFFEEEE00));
    }

    public void setNeedsPartsFixedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NEEDS_PARTS_FIXED_BACKGROUND, value.getRGB());
    }

    public Color getUnmaintainedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.UNMAINTAINED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setUnmaintainedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.UNMAINTAINED_FOREGROUND, value.getRGB());
    }

    public Color getUnmaintainedBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.UNMAINTAINED_BACKGROUND, Color.ORANGE.getRGB()));
    }

    public void setUnmaintainedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.UNMAINTAINED_BACKGROUND, value.getRGB());
    }

    public Color getUncrewedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.UNCREWED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setUncrewedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.UNCREWED_FOREGROUND, value.getRGB());
    }

    public Color getUncrewedBackground() {
        // new Color(218, 130, 255)
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.UNCREWED_BACKGROUND, 0xFFDA82FF));
    }

    public void setUncrewedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.UNCREWED_BACKGROUND, value.getRGB());
    }

    public Color getLoanOverdueForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.LOAN_OVERDUE_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setLoanOverdueForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.LOAN_OVERDUE_FOREGROUND, value.getRGB());
    }

    public Color getLoanOverdueBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.LOAN_OVERDUE_BACKGROUND, Color.RED.getRGB()));
    }

    public void setLoanOverdueBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.LOAN_OVERDUE_BACKGROUND, value.getRGB());
    }

    public Color getInjuredForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.INJURED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setInjuredForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.INJURED_FOREGROUND, value.getRGB());
    }

    public Color getInjuredBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.INJURED_BACKGROUND, Color.RED.getRGB()));
    }

    public void setInjuredBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.INJURED_BACKGROUND, value.getRGB());
    }

    public Color getHealedInjuriesForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.HEALED_INJURIES_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setHealedInjuriesForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.HEALED_INJURIES_FOREGROUND, value.getRGB());
    }

    public Color getHealedInjuriesBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.HEALED_INJURIES_BACKGROUND, 0xEE9A00));
    }

    public void setHealedInjuriesBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.HEALED_INJURIES_BACKGROUND, value.getRGB());
    }

    public Color getPregnantForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.PREGNANT_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setPregnantForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.PREGNANT_FOREGROUND, value.getRGB());
    }

    public Color getPregnantBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.PREGNANT_BACKGROUND, 0X2BAD43));
    }

    public void setPregnantBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.PREGNANT_BACKGROUND, value.getRGB());
    }

    public Color getPaidRetirementForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.PAID_RETIREMENT_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setPaidRetirementForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.PAID_RETIREMENT_FOREGROUND, value.getRGB());
    }

    public Color getPaidRetirementBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.PAID_RETIREMENT_BACKGROUND, Color.LIGHT_GRAY.getRGB()));
    }

    public void setPaidRetirementBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.PAID_RETIREMENT_BACKGROUND, value.getRGB());
    }
    //endregion Colours

    //region Fonts
    public String getMedicalViewDialogHandwritingFont() {
        return userPreferences.node(MHQConstants.FONTS_NODE).get(MHQConstants.MEDICAL_VIEW_DIALOG_HANDWRITING_FONT, "Angelina");
    }

    public void setMedicalViewDialogHandwritingFont(final String value) {
        userPreferences.node(MHQConstants.FONTS_NODE).put(MHQConstants.MEDICAL_VIEW_DIALOG_HANDWRITING_FONT, value);
    }
    //endregion Fonts

    //region Autosave
    public boolean getNoAutosaveValue() {
        return userPreferences.node(MHQConstants.AUTOSAVE_NODE).getBoolean(MHQConstants.NO_SAVE_KEY, false);
    }

    public void setNoAutosaveValue(boolean value) {
        userPreferences.node(MHQConstants.AUTOSAVE_NODE).putBoolean(MHQConstants.NO_SAVE_KEY, value);
    }

    public boolean getAutosaveDailyValue() {
        return userPreferences.node(MHQConstants.AUTOSAVE_NODE).getBoolean(MHQConstants.SAVE_DAILY_KEY, false);
    }

    public void setAutosaveDailyValue(boolean value) {
        userPreferences.node(MHQConstants.AUTOSAVE_NODE).putBoolean(MHQConstants.SAVE_DAILY_KEY, value);
    }

    public boolean getAutosaveWeeklyValue() {
        return userPreferences.node(MHQConstants.AUTOSAVE_NODE).getBoolean(MHQConstants.SAVE_WEEKLY_KEY, true);
    }

    public void setAutosaveWeeklyValue(boolean value) {
        userPreferences.node(MHQConstants.AUTOSAVE_NODE).putBoolean(MHQConstants.SAVE_WEEKLY_KEY, value);
    }

    public boolean getAutosaveMonthlyValue() {
        return userPreferences.node(MHQConstants.AUTOSAVE_NODE).getBoolean(MHQConstants.SAVE_MONTHLY_KEY, false);
    }

    public void setAutosaveMonthlyValue(boolean value) {
        userPreferences.node(MHQConstants.AUTOSAVE_NODE).putBoolean(MHQConstants.SAVE_MONTHLY_KEY, value);
    }

    public boolean getAutosaveYearlyValue() {
        return userPreferences.node(MHQConstants.AUTOSAVE_NODE).getBoolean(MHQConstants.SAVE_YEARLY_KEY, false);
    }

    public void setAutosaveYearlyValue(boolean value) {
        userPreferences.node(MHQConstants.AUTOSAVE_NODE).putBoolean(MHQConstants.SAVE_YEARLY_KEY, value);
    }

    public boolean getAutosaveBeforeMissionsValue() {
        return userPreferences.node(MHQConstants.AUTOSAVE_NODE).getBoolean(MHQConstants.SAVE_BEFORE_MISSIONS_KEY, false);
    }

    public void setAutosaveBeforeMissionsValue(boolean value) {
        userPreferences.node(MHQConstants.AUTOSAVE_NODE).putBoolean(MHQConstants.SAVE_BEFORE_MISSIONS_KEY, value);
    }

    public int getMaximumNumberOfAutosavesValue() {
        return userPreferences.node(MHQConstants.AUTOSAVE_NODE).getInt(MHQConstants.MAXIMUM_NUMBER_SAVES_KEY,
                MHQConstants.DEFAULT_NUMBER_SAVES);
    }

    public void setMaximumNumberOfAutosavesValue(int value) {
        userPreferences.node(MHQConstants.AUTOSAVE_NODE).putInt(MHQConstants.MAXIMUM_NUMBER_SAVES_KEY, value);
    }
    //endregion Autosave

    //region New Day
    public boolean getNewDayAstechPoolFill() {
        return userPreferences.node(MHQConstants.NEW_DAY_NODE).getBoolean(MHQConstants.NEW_DAY_ASTECH_POOL_FILL, true);
    }

    public void setNewDayAstechPoolFill(final boolean value) {
        userPreferences.node(MHQConstants.NEW_DAY_NODE).putBoolean(MHQConstants.NEW_DAY_ASTECH_POOL_FILL, value);
    }

    public boolean getNewDayMedicPoolFill() {
        return userPreferences.node(MHQConstants.NEW_DAY_NODE).getBoolean(MHQConstants.NEW_DAY_MEDIC_POOL_FILL, true);
    }

    public void setNewDayMedicPoolFill(final boolean value) {
        userPreferences.node(MHQConstants.NEW_DAY_NODE).putBoolean(MHQConstants.NEW_DAY_MEDIC_POOL_FILL, value);
    }

    public boolean getNewDayMRMS() {
        return userPreferences.node(MHQConstants.NEW_DAY_NODE).getBoolean(MHQConstants.NEW_DAY_MRMS, false);
    }

    public void setNewDayMRMS(final boolean value) {
        userPreferences.node(MHQConstants.NEW_DAY_NODE).putBoolean(MHQConstants.NEW_DAY_MRMS, value);
    }

    public boolean getNewDayForceIconOperationalStatus() {
        return userPreferences.node(MHQConstants.NEW_DAY_NODE).getBoolean(MHQConstants.NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS, true);
    }

    public void setNewDayForceIconOperationalStatus(final boolean value) {
        userPreferences.node(MHQConstants.NEW_DAY_NODE).putBoolean(MHQConstants.NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS, value);
    }

    public ForceIconOperationalStatusStyle getNewDayForceIconOperationalStatusStyle() {
        return ForceIconOperationalStatusStyle.valueOf(userPreferences.node(MHQConstants.NEW_DAY_NODE).get(MHQConstants.NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS_STYLE, ForceIconOperationalStatusStyle.BORDER.name()));
    }

    public void setNewDayForceIconOperationalStatusStyle(final ForceIconOperationalStatusStyle value) {
        userPreferences.node(MHQConstants.NEW_DAY_NODE).put(MHQConstants.NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS_STYLE, value.name());
    }
    //endregion New Day

    //region Campaign XML Save Options
    /**
     * @return A value indicating if the campaign should be written to a gzipped file, if possible.
     */
    public boolean getPreferGzippedOutput() {
        return userPreferences.node(MHQConstants.XML_SAVES_NODE).getBoolean(MHQConstants.PREFER_GZIPPED_CAMPAIGN_FILE, true);
    }

    /**
     * Sets a hint indicating that the campaign should be gzipped, if possible.
     * This allows the Save dialog to present the user with the correct file
     * type on subsequent saves.
     *
     * @param value A value indicating whether or not the campaign should be gzipped if possible.
     */
    public void setPreferGzippedOutput(boolean value) {
        userPreferences.node(MHQConstants.XML_SAVES_NODE).putBoolean(MHQConstants.PREFER_GZIPPED_CAMPAIGN_FILE, value);
    }

    public boolean getWriteCustomsToXML() {
        return userPreferences.node(MHQConstants.XML_SAVES_NODE).getBoolean(MHQConstants.WRITE_CUSTOMS_TO_XML, true);
    }

    public void setWriteCustomsToXML(boolean value) {
        userPreferences.node(MHQConstants.XML_SAVES_NODE).putBoolean(MHQConstants.WRITE_CUSTOMS_TO_XML, value);
    }

    public boolean getSaveMothballState() {
        return userPreferences.node(MHQConstants.XML_SAVES_NODE).getBoolean(MHQConstants.SAVE_MOTHBALL_STATE, true);
    }

    public void setSaveMothballState(boolean value) {
        userPreferences.node(MHQConstants.XML_SAVES_NODE).putBoolean(MHQConstants.SAVE_MOTHBALL_STATE, value);
    }
    //endregion Campaign XML Save Options

    //region File Paths
    /**
     * @return the path of the folder to load when loading or saving bulk rank systems
     */
    public String getRankSystemsPath() {
        return userPreferences.node(MHQConstants.FILE_PATH_NODE).get(MHQConstants.RANK_SYSTEMS_DIRECTORY_PATH, "userdata/data/universe/");
    }

    /**
     * This sets the path where one saves or loads their rank systems from, as this is not required
     * for any data but improves UX.
     *
     * @param value the path where the person saved their last bulk rank system export
     */
    public void setRankSystemsPath(final String value) {
        userPreferences.node(MHQConstants.FILE_PATH_NODE).put(MHQConstants.RANK_SYSTEMS_DIRECTORY_PATH, value);
    }

    /**
     * @return the path of the folder to load when loading or saving an individual rank system
     */
    public String getIndividualRankSystemPath() {
        return userPreferences.node(MHQConstants.FILE_PATH_NODE).get(MHQConstants.INDIVIDUAL_RANK_SYSTEM_DIRECTORY_PATH, "userdata/data/universe/");
    }

    /**
     * This sets the path where one saves or loads their individual rank system, as this is not
     * required for any data but improves UX.
     *
     * @param value the path where the person saved their last individual rank system.
     */
    public void setIndividualRankSystemPath(final String value) {
        userPreferences.node(MHQConstants.FILE_PATH_NODE).put(MHQConstants.INDIVIDUAL_RANK_SYSTEM_DIRECTORY_PATH, value);
    }

    /**
     * @return the path of the folder to load when exporting a layered force icon
     */
    public String getLayeredForceIconPath() {
        return userPreferences.node(MHQConstants.FILE_PATH_NODE).get(MHQConstants.LAYERED_FORCE_ICON_DIRECTORY_PATH, "userdata/data/images/force/");
    }

    /**
     * This sets the path where one saves their layered force icon during export, as this is not
     * required for any data but improves UX.
     *
     * @param value the path where the person saved their last layered force icon export
     */
    public void setLayeredForceIconPath(final String value) {
        userPreferences.node(MHQConstants.FILE_PATH_NODE).put(MHQConstants.LAYERED_FORCE_ICON_DIRECTORY_PATH, value);
    }

    public String getCompanyGenerationDirectoryPath() {
        return userPreferences.node(MHQConstants.FILE_PATH_NODE).get(MHQConstants.COMPANY_GENERATION_DIRECTORY_PATH, "mmconf/mhqCompanyGenerationPresets/");
    }

    public void setCompanyGenerationDirectoryPath(final String value) {
        userPreferences.node(MHQConstants.FILE_PATH_NODE).put(MHQConstants.COMPANY_GENERATION_DIRECTORY_PATH, value);
    }
    //endregion File Paths

    //region Nag Tab
    public boolean getNagDialogIgnore(final String key) {
        return userPreferences.node(MHQConstants.NAG_NODE).getBoolean(key, false);
    }

    public void setNagDialogIgnore(final String key, final boolean value) {
        userPreferences.node(MHQConstants.NAG_NODE).putBoolean(key, value);
    }
    //endregion Nag Tab

    //region Miscellaneous Options
    public int getStartGameDelay() {
        return userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).getInt(MHQConstants.START_GAME_DELAY, 1000);
    }

    public void setStartGameDelay(final int startGameDelay) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).putInt(MHQConstants.START_GAME_DELAY, startGameDelay);
    }

    public int getStartGameClientDelay() {
        return userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).getInt(MHQConstants.START_GAME_CLIENT_DELAY, 50);
    }

    public void setStartGameClientDelay(final int startGameClientDelay) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).putInt(MHQConstants.START_GAME_CLIENT_DELAY, startGameClientDelay);
    }

    public int getStartGameClientRetryCount() {
        return userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).getInt(MHQConstants.START_GAME_CLIENT_RETRY_COUNT, 1000);
    }

    public void setStartGameClientRetryCount(final int startGameClientRetryCount) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).putInt(MHQConstants.START_GAME_CLIENT_RETRY_COUNT, startGameClientRetryCount);
    }

    public int getStartGameBotClientDelay() {
        return userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).getInt(MHQConstants.START_GAME_BOT_CLIENT_DELAY, 50);
    }

    public void setStartGameBotClientDelay(final int startGameBotClientDelay) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).putInt(MHQConstants.START_GAME_BOT_CLIENT_DELAY, startGameBotClientDelay);
    }

    public int getStartGameBotClientRetryCount() {
        return userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).getInt(MHQConstants.START_GAME_BOT_CLIENT_RETRY_COUNT, 250);
    }

    public void setStartGameBotClientRetryCount(final int startGameBotClientRetryCount) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).putInt(MHQConstants.START_GAME_BOT_CLIENT_RETRY_COUNT, startGameBotClientRetryCount);
    }

    public CompanyGenerationMethod getDefaultCompanyGenerationMethod() {
        return CompanyGenerationMethod.valueOf(userPreferences.node(MHQConstants.MISCELLANEOUS_NODE)
                .get(MHQConstants.DEFAULT_COMPANY_GENERATION_METHOD, CompanyGenerationMethod.WINDCHILD.name()));
    }

    public void setDefaultCompanyGenerationMethod(final CompanyGenerationMethod value) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).put(MHQConstants.DEFAULT_COMPANY_GENERATION_METHOD, value.name());
    }
    //endregion Miscellaneous Options
}
