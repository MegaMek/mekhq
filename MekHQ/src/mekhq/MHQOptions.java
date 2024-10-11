/*
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.UIManager;

import megamek.SuiteOptions;
import megamek.common.annotations.Nullable;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.gui.enums.ForceIconOperationalStatusStyle;
import mekhq.gui.enums.PersonnelFilterStyle;

public final class MHQOptions extends SuiteOptions {
    // region Display Tab
    public String getDisplayDateFormat() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).get(MHQConstants.DISPLAY_DATE_FORMAT, "yyyy-MM-dd");
    }

    public String getDisplayFormattedDate(final @Nullable LocalDate date) {
        return (date != null)
                ? date.format(DateTimeFormatter.ofPattern(getDisplayDateFormat()).withLocale(getDateLocale()))
                : "";
    }

    public void setDisplayDateFormat(String value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).put(MHQConstants.DISPLAY_DATE_FORMAT, value);
    }

    public LocalDate parseDisplayFormattedDate(String text) {
        return LocalDate.parse(text, DateTimeFormatter.ofPattern(getDisplayDateFormat()).withLocale(getDateLocale()));
    }

    public String getLongDisplayDateFormat() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).get(MHQConstants.LONG_DISPLAY_DATE_FORMAT,
                "EEEE, MMMM d, yyyy");
    }

    public String getLongDisplayFormattedDate(LocalDate date) {
        return (date != null)
                ? date.format(DateTimeFormatter.ofPattern(getLongDisplayDateFormat()).withLocale(getDateLocale()))
                : "";
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
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.COMPANY_GENERATOR_STARTUP,
                false);
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

    public boolean getShowUnitPicturesOnTOE() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.SHOW_UNIT_PICTURES_ON_TOE, true);
    }

    public void setShowUnitPicturesOnTOE(final boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putBoolean(MHQConstants.SHOW_UNIT_PICTURES_ON_TOE, value);
    }

    // region Command Center Tab
    public boolean getCommandCenterUseUnitMarket() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.COMMAND_CENTER_USE_UNIT_MARKET,
                true);
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
    // endregion Command Center Tab

    // region Interstellar Map Tab
    public boolean getInterstellarMapShowJumpRadius() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getBoolean(MHQConstants.INTERSTELLAR_MAP_SHOW_JUMP_RADIUS, true);
    }

    public void setInterstellarMapShowJumpRadius(final boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putBoolean(MHQConstants.INTERSTELLAR_MAP_SHOW_JUMP_RADIUS,
                value);
    }

    public double getInterstellarMapShowJumpRadiusMinimumZoom() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getDouble(MHQConstants.INTERSTELLAR_MAP_SHOW_JUMP_RADIUS_MINIMUM_ZOOM, 3d);
    }

    public void setInterstellarMapShowJumpRadiusMinimumZoom(final double value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE)
                .putDouble(MHQConstants.INTERSTELLAR_MAP_SHOW_JUMP_RADIUS_MINIMUM_ZOOM, value);
    }

    public Color getInterstellarMapJumpRadiusColour() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getInt(MHQConstants.INTERSTELLAR_MAP_JUMP_RADIUS_COLOUR, Color.DARK_GRAY.getRGB()));
    }

    public void setInterstellarMapJumpRadiusColour(final Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.INTERSTELLAR_MAP_JUMP_RADIUS_COLOUR,
                value.getRGB());
    }

    public boolean getInterstellarMapShowPlanetaryAcquisitionRadius() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getBoolean(MHQConstants.INTERSTELLAR_MAP_SHOW_PLANETARY_ACQUISITION_RADIUS, false);
    }

    public void setInterstellarMapShowPlanetaryAcquisitionRadius(final boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE)
                .putBoolean(MHQConstants.INTERSTELLAR_MAP_SHOW_PLANETARY_ACQUISITION_RADIUS, value);
    }

    public double getInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getDouble(MHQConstants.INTERSTELLAR_MAP_SHOW_PLANETARY_ACQUISITION_RADIUS_MINIMUM_ZOOM, 2d);
    }

    public void setInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom(final double value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE)
                .putDouble(MHQConstants.INTERSTELLAR_MAP_SHOW_PLANETARY_ACQUISITION_RADIUS_MINIMUM_ZOOM, value);
    }

    public Color getInterstellarMapPlanetaryAcquisitionRadiusColour() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getInt(MHQConstants.INTERSTELLAR_MAP_PLANETARY_ACQUISITION_RADIUS_COLOUR, 0xFF505050));
    }

    public void setInterstellarMapPlanetaryAcquisitionRadiusColour(final Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE)
                .putInt(MHQConstants.INTERSTELLAR_MAP_PLANETARY_ACQUISITION_RADIUS_COLOUR, value.getRGB());
    }

    public boolean getInterstellarMapShowContractSearchRadius() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getBoolean(MHQConstants.INTERSTELLAR_MAP_SHOW_CONTRACT_SEARCH_RADIUS, false);
    }

    public void setInterstellarMapShowContractSearchRadius(final boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE)
                .putBoolean(MHQConstants.INTERSTELLAR_MAP_SHOW_CONTRACT_SEARCH_RADIUS, value);
    }

    public Color getInterstellarMapContractSearchRadiusColour() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getInt(MHQConstants.INTERSTELLAR_MAP_CONTRACT_SEARCH_RADIUS_COLOUR, 0xFF606060));
    }

    public void setInterstellarMapContractSearchRadiusColour(final Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE)
                .putInt(MHQConstants.INTERSTELLAR_MAP_CONTRACT_SEARCH_RADIUS_COLOUR, value.getRGB());
    }
    // endregion Interstellar Map Tab

    // region Personnel Tab
    public PersonnelFilterStyle getPersonnelFilterStyle() {
        return PersonnelFilterStyle.valueOf(userPreferences.node(MHQConstants.DISPLAY_NODE)
                .get(MHQConstants.PERSONNEL_FILTER_STYLE, PersonnelFilterStyle.STANDARD.name()));
    }

    public void setPersonnelFilterStyle(PersonnelFilterStyle value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).put(MHQConstants.PERSONNEL_FILTER_STYLE, value.name());
    }

    public boolean getPersonnelFilterOnPrimaryRole() {
        return userPreferences.node(MHQConstants.DISPLAY_NODE).getBoolean(MHQConstants.PERSONNEL_FILTER_ON_PRIMARY_ROLE,
                false);
    }

    public void setPersonnelFilterOnPrimaryRole(boolean value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putBoolean(MHQConstants.PERSONNEL_FILTER_ON_PRIMARY_ROLE,
                value);
    }
    // endregion Personnel Tab
    // endregion Display Tab

    // region Colours
    public Color getDeployedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.DEPLOYED_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setDeployedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.DEPLOYED_FOREGROUND, value.getRGB());
    }

    public Color getDeployedBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.DEPLOYED_BACKGROUND,
                Color.LIGHT_GRAY.getRGB()));
    }

    public void setDeployedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.DEPLOYED_BACKGROUND, value.getRGB());
    }

    public Color getBelowContractMinimumForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getInt(MHQConstants.BELOW_CONTRACT_MINIMUM_FOREGROUND, Color.RED.getRGB()));
    }

    public void setBelowContractMinimumForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.BELOW_CONTRACT_MINIMUM_FOREGROUND,
                value.getRGB());
    }

    public Color getBelowContractMinimumBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(
                MHQConstants.BELOW_CONTRACT_MINIMUM_BACKGROUND, UIManager.getColor("Table.background").getRGB()));
    }

    public void setBelowContractMinimumBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.BELOW_CONTRACT_MINIMUM_BACKGROUND,
                value.getRGB());
    }

    public Color getInTransitForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.IN_TRANSIT_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setInTransitForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.IN_TRANSIT_FOREGROUND, value.getRGB());
    }

    public Color getInTransitBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.IN_TRANSIT_BACKGROUND,
                Color.MAGENTA.getRGB()));
    }

    public void setInTransitBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.IN_TRANSIT_BACKGROUND, value.getRGB());
    }

    public Color getRefittingForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.REFITTING_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setRefittingForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.REFITTING_FOREGROUND, value.getRGB());
    }

    public Color getRefittingBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.REFITTING_BACKGROUND,
                Color.CYAN.getRGB()));
    }

    public void setRefittingBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.REFITTING_BACKGROUND, value.getRGB());
    }

    public Color getMothballingForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.MOTHBALLING_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setMothballingForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.MOTHBALLING_FOREGROUND, value.getRGB());
    }

    public Color getMothballingBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.MOTHBALLING_BACKGROUND,
                0xFF9999FF));
    }

    public void setMothballingBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.MOTHBALLING_BACKGROUND, value.getRGB());
    }

    public Color getMothballedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.MOTHBALLED_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setMothballedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.MOTHBALLED_FOREGROUND, value.getRGB());
    }

    public Color getMothballedBackground() {
        return new Color(
                userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.MOTHBALLED_BACKGROUND, 0xFFCCCCFF));
    }

    public void setMothballedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.MOTHBALLED_BACKGROUND, value.getRGB());
    }

    public Color getNotRepairableForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NOT_REPAIRABLE_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setNotRepairableForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NOT_REPAIRABLE_FOREGROUND, value.getRGB());
    }

    public Color getNotRepairableBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NOT_REPAIRABLE_BACKGROUND,
                0xFFBE9637));
    }

    public void setNotRepairableBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NOT_REPAIRABLE_BACKGROUND, value.getRGB());
    }

    public Color getNonFunctionalForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NON_FUNCTIONAL_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setNonFunctionalForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NON_FUNCTIONAL_FOREGROUND, value.getRGB());
    }

    public Color getNonFunctionalBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.NON_FUNCTIONAL_BACKGROUND,
                0xFFCD5C5C));
    }

    public void setNonFunctionalBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NON_FUNCTIONAL_BACKGROUND, value.getRGB());
    }

    public Color getNeedsPartsFixedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getInt(MHQConstants.NEEDS_PARTS_FIXED_FOREGROUND, Color.BLACK.getRGB()));
    }

    public void setNeedsPartsFixedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NEEDS_PARTS_FIXED_FOREGROUND,
                value.getRGB());
    }

    public Color getNeedsPartsFixedBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getInt(MHQConstants.NEEDS_PARTS_FIXED_BACKGROUND, 0xFFEEEE00));
    }

    public void setNeedsPartsFixedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.NEEDS_PARTS_FIXED_BACKGROUND,
                value.getRGB());
    }

    public Color getUnmaintainedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.UNMAINTAINED_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setUnmaintainedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.UNMAINTAINED_FOREGROUND, value.getRGB());
    }

    public Color getUnmaintainedBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.UNMAINTAINED_BACKGROUND,
                Color.ORANGE.getRGB()));
    }

    public void setUnmaintainedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.UNMAINTAINED_BACKGROUND, value.getRGB());
    }

    public Color getUncrewedForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.UNCREWED_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setUncrewedForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.UNCREWED_FOREGROUND, value.getRGB());
    }

    public Color getUncrewedBackground() {
        return new Color(
                userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.UNCREWED_BACKGROUND, 0xFFDA82FF));
    }

    public void setUncrewedBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.UNCREWED_BACKGROUND, value.getRGB());
    }

    public Color getLoanOverdueForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.LOAN_OVERDUE_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setLoanOverdueForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.LOAN_OVERDUE_FOREGROUND, value.getRGB());
    }

    public Color getLoanOverdueBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.LOAN_OVERDUE_BACKGROUND,
                Color.RED.getRGB()));
    }

    public void setLoanOverdueBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.LOAN_OVERDUE_BACKGROUND, value.getRGB());
    }

    public Color getInjuredForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.INJURED_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setInjuredForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.INJURED_FOREGROUND, value.getRGB());
    }

    public Color getInjuredBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.INJURED_BACKGROUND,
                Color.RED.getRGB()));
    }

    public void setInjuredBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.INJURED_BACKGROUND, value.getRGB());
    }

    public Color getHealedInjuriesForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.HEALED_INJURIES_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setHealedInjuriesForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.HEALED_INJURIES_FOREGROUND, value.getRGB());
    }

    public Color getHealedInjuriesBackground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.HEALED_INJURIES_BACKGROUND,
                0xEE9A00));
    }

    public void setHealedInjuriesBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.HEALED_INJURIES_BACKGROUND, value.getRGB());
    }

    public Color getPregnantForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.PREGNANT_FOREGROUND,
                Color.BLACK.getRGB()));
    }

    public void setPregnantForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.PREGNANT_FOREGROUND, value.getRGB());
    }

    public Color getPregnantBackground() {
        return new Color(
                userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.PREGNANT_BACKGROUND, 0X2BAD43));
    }

    public void setPregnantBackground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.PREGNANT_BACKGROUND, value.getRGB());
    }

    public Color getStratConHexCoordForeground() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE)
                .getInt(MHQConstants.STRATCON_HEX_COORD_FOREGROUND, Color.GREEN.getRGB()));
    }

    public void setStratConHexCoordForeground(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.STRATCON_HEX_COORD_FOREGROUND,
                value.getRGB());
    }

    public Color getFontColorNegative() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.FONT_COLOR_NEGATIVE,
                Color.RED.getRGB()));
    }

    /**
     * @return the hexadecimal color code for the negative event font color.
     */
    public String getFontColorNegativeHexColor() {
        return convertFontColorToHexColor(getFontColorNegative());
    }

    public void setFontColorNegative(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.FONT_COLOR_NEGATIVE, value.getRGB());
    }

    public Color getFontColorPositive() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.FONT_COLOR_POSITIVE,
                Color.GREEN.getRGB()));
    }

    /**
     * @return the hexadecimal color code for the positive event font color.
     */
    public String getFontColorPositiveHexColor() {
        return convertFontColorToHexColor(getFontColorPositive());
    }

    public void setFontColorPositive(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.FONT_COLOR_POSITIVE, value.getRGB());
    }

    public Color getFontColorWarning() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.FONT_COLOR_WARNING,
                Color.ORANGE.getRGB()));
    }

    /**
     * @return the hexadecimal color code for the warning event font color.
     */
    public String getFontColorWarningHexColor() {
        return convertFontColorToHexColor(getFontColorWarning());
    }

    public void setFontColorWarning(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.FONT_COLOR_WARNING, value.getRGB());
    }


    public Color getFontColorSkillUltraGreen() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.FONT_COLOR_SKILL_ULTRAGREEN,
                0x42DFDF));
    }

    /**
     * @return the hexadecimal color code for the ultra green skill.
     */
    public String getFontColorSkillUltraGreenHexColor() {
        return convertFontColorToHexColor(getFontColorSkillUltraGreen());
    }

    public void setFontColorSkillUltraGreen(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.FONT_COLOR_SKILL_ULTRAGREEN, value.getRGB());
    }

    public Color getFontColorSkillGreen() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.FONT_COLOR_SKILL_GREEN,
                0x43CF43));
    }

    /**
     * @return the hexadecimal color code for the green skill.
     */
    public String getFontColorSkillGreenHexColor() {
        return convertFontColorToHexColor(getFontColorSkillGreen());
    }

    public void setFontColorSkillGreen(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.FONT_COLOR_SKILL_GREEN, value.getRGB());
    }

    public Color getFontColorSkillRegular() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.FONT_COLOR_SKILL_REGULAR,
                0xCF9F43));
    }

    /**
     * @return the hexadecimal color code for the regular skill.
     */
    public String getFontColorSkillRegularHexColor() {
        return convertFontColorToHexColor(getFontColorSkillRegular());
    }

    public void setFontColorSkillRegular(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.FONT_COLOR_SKILL_REGULAR, value.getRGB());
    }

    public Color getFontColorSkillVeteran() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.FONT_COLOR_SKILL_VETERAN,
                0xE85C5C));
    }

    /**
     * @return the hexadecimal color code for the veteran skill.
     */
    public String getFontColorSkillVeteranHexColor() {
        return convertFontColorToHexColor(getFontColorSkillVeteran());
    }

    public void setFontColorSkillVeteran(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.FONT_COLOR_SKILL_VETERAN, value.getRGB());
    }

    public Color getFontColorSkillElite() {
        return new Color(userPreferences.node(MHQConstants.DISPLAY_NODE).getInt(MHQConstants.FONT_COLOR_SKILL_ELITE,
                0xC344C3));
    }

    /**
     * @return the hexadecimal color code for the elite skill.
     */
    public String getFontColorSkillEliteHexColor() {
        return convertFontColorToHexColor(getFontColorSkillElite());
    }

    public void setFontColorSkillElite(Color value) {
        userPreferences.node(MHQConstants.DISPLAY_NODE).putInt(MHQConstants.FONT_COLOR_SKILL_ELITE, value.getRGB());
    }


    /**
     * Converts the font color to a hexadecimal color representation.
     *
     * @param color the font color to convert
     * @return the hexadecimal color representation of the font color
     */
    public String convertFontColorToHexColor(Color color) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        return String.format("#%02x%02x%02x", red, green, blue);
    }
    // endregion Colours

    // region Fonts
    public String getMedicalViewDialogHandwritingFont() {
        return userPreferences.node(MHQConstants.FONTS_NODE).get(MHQConstants.MEDICAL_VIEW_DIALOG_HANDWRITING_FONT,
                "Angelina");
    }

    public void setMedicalViewDialogHandwritingFont(final String value) {
        userPreferences.node(MHQConstants.FONTS_NODE).put(MHQConstants.MEDICAL_VIEW_DIALOG_HANDWRITING_FONT, value);
    }
    // endregion Fonts

    // region Autosave
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
        return userPreferences.node(MHQConstants.AUTOSAVE_NODE).getBoolean(MHQConstants.SAVE_BEFORE_MISSIONS_KEY,
                false);
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
    // endregion Autosave

    // region New Day
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
        return userPreferences.node(MHQConstants.NEW_DAY_NODE)
                .getBoolean(MHQConstants.NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS, true);
    }

    public void setNewDayForceIconOperationalStatus(final boolean value) {
        userPreferences.node(MHQConstants.NEW_DAY_NODE).putBoolean(MHQConstants.NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS,
                value);
    }

    public ForceIconOperationalStatusStyle getNewDayForceIconOperationalStatusStyle() {
        return ForceIconOperationalStatusStyle.valueOf(userPreferences.node(MHQConstants.NEW_DAY_NODE).get(
                MHQConstants.NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS_STYLE,
                ForceIconOperationalStatusStyle.BORDER.name()));
    }

    public void setNewDayForceIconOperationalStatusStyle(final ForceIconOperationalStatusStyle value) {
        userPreferences.node(MHQConstants.NEW_DAY_NODE).put(MHQConstants.NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS_STYLE,
                value.name());
    }
    // endregion New Day

    // region Campaign XML Save Options
    /**
     * @return A value indicating if the campaign should be written to a gzipped
     *         file, if possible.
     */
    public boolean getPreferGzippedOutput() {
        return userPreferences.node(MHQConstants.XML_SAVES_NODE).getBoolean(MHQConstants.PREFER_GZIPPED_CAMPAIGN_FILE,
                true);
    }

    /**
     * Sets a hint indicating that the campaign should be gzipped, if possible.
     * This allows the Save dialog to present the user with the correct file
     * type on subsequent saves.
     *
     * @param value A value indicating whether or not the campaign should be gzipped
     *              if possible.
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
    // endregion Campaign XML Save Options

    // region File Paths
    /**
     * @return the path of the folder to load when loading or saving bulk rank
     *         systems
     */
    public String getRankSystemsPath() {
        return userPreferences.node(MHQConstants.FILE_PATH_NODE).get(MHQConstants.RANK_SYSTEMS_DIRECTORY_PATH,
                "userdata/data/universe/");
    }

    /**
     * This sets the path where one saves or loads their rank systems from, as this
     * is not required
     * for any data but improves UX.
     *
     * @param value the path where the person saved their last bulk rank system
     *              export
     */
    public void setRankSystemsPath(final String value) {
        userPreferences.node(MHQConstants.FILE_PATH_NODE).put(MHQConstants.RANK_SYSTEMS_DIRECTORY_PATH, value);
    }

    /**
     * @return the path of the folder to load when loading or saving an individual
     *         rank system
     */
    public String getIndividualRankSystemPath() {
        return userPreferences.node(MHQConstants.FILE_PATH_NODE).get(MHQConstants.INDIVIDUAL_RANK_SYSTEM_DIRECTORY_PATH,
                "userdata/data/universe/");
    }

    /**
     * This sets the path where one saves or loads their individual rank system, as
     * this is not
     * required for any data but improves UX.
     *
     * @param value the path where the person saved their last individual rank
     *              system.
     */
    public void setIndividualRankSystemPath(final String value) {
        userPreferences.node(MHQConstants.FILE_PATH_NODE).put(MHQConstants.INDIVIDUAL_RANK_SYSTEM_DIRECTORY_PATH,
                value);
    }

    /**
     * @return the path of the folder to load when exporting a unit sprite
     */
    public String getUnitSpriteExportPath() {
        return userPreferences.node(MHQConstants.FILE_PATH_NODE).get(MHQConstants.UNIT_SPRITE_EXPORT_DIRECTORY_PATH,
                "");
    }

    /**
     * This sets the path where one saves their unit sprite during export, as this
     * is not
     * required for any data but improves UX.
     *
     * @param value the path where the person saved their last unit sprite export
     */
    public void setUnitSpriteExportPath(final String value) {
        userPreferences.node(MHQConstants.FILE_PATH_NODE).put(MHQConstants.UNIT_SPRITE_EXPORT_DIRECTORY_PATH, value);
    }

    /**
     * @return the path of the folder to load when exporting a layered force icon
     */
    public String getLayeredForceIconPath() {
        return userPreferences.node(MHQConstants.FILE_PATH_NODE).get(MHQConstants.LAYERED_FORCE_ICON_DIRECTORY_PATH,
                "userdata/data/images/force/");
    }

    /**
     * This sets the path where one saves their layered force icon during export, as
     * this is not
     * required for any data but improves UX.
     *
     * @param value the path where the person saved their last layered force icon
     *              export
     */
    public void setLayeredForceIconPath(final String value) {
        userPreferences.node(MHQConstants.FILE_PATH_NODE).put(MHQConstants.LAYERED_FORCE_ICON_DIRECTORY_PATH, value);
    }

    public String getCompanyGenerationDirectoryPath() {
        return userPreferences.node(MHQConstants.FILE_PATH_NODE).get(MHQConstants.COMPANY_GENERATION_DIRECTORY_PATH,
                "mmconf/mhqCompanyGenerationPresets/");
    }

    public void setCompanyGenerationDirectoryPath(final String value) {
        userPreferences.node(MHQConstants.FILE_PATH_NODE).put(MHQConstants.COMPANY_GENERATION_DIRECTORY_PATH, value);
    }
    // endregion File Paths

    // region Nag Tab
    public boolean getNagDialogIgnore(final String key) {
        return userPreferences.node(MHQConstants.NAG_NODE).getBoolean(key, false);
    }

    public void setNagDialogIgnore(final String key, final boolean value) {
        userPreferences.node(MHQConstants.NAG_NODE).putBoolean(key, value);
    }
    // endregion Nag Tab

    // region Miscellaneous Options
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
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).putInt(MHQConstants.START_GAME_CLIENT_DELAY,
                startGameClientDelay);
    }

    public int getStartGameClientRetryCount() {
        return userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).getInt(MHQConstants.START_GAME_CLIENT_RETRY_COUNT,
                1000);
    }

    public void setStartGameClientRetryCount(final int startGameClientRetryCount) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).putInt(MHQConstants.START_GAME_CLIENT_RETRY_COUNT,
                startGameClientRetryCount);
    }

    public int getStartGameBotClientDelay() {
        return userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).getInt(MHQConstants.START_GAME_BOT_CLIENT_DELAY,
                50);
    }

    public void setStartGameBotClientDelay(final int startGameBotClientDelay) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).putInt(MHQConstants.START_GAME_BOT_CLIENT_DELAY,
                startGameBotClientDelay);
    }

    public int getStartGameBotClientRetryCount() {
        return userPreferences.node(MHQConstants.MISCELLANEOUS_NODE)
                .getInt(MHQConstants.START_GAME_BOT_CLIENT_RETRY_COUNT, 250);
    }

    public void setStartGameBotClientRetryCount(final int startGameBotClientRetryCount) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).putInt(MHQConstants.START_GAME_BOT_CLIENT_RETRY_COUNT,
                startGameBotClientRetryCount);
    }

    public CompanyGenerationMethod getDefaultCompanyGenerationMethod() {
        return CompanyGenerationMethod.valueOf(userPreferences.node(MHQConstants.MISCELLANEOUS_NODE)
                .get(MHQConstants.DEFAULT_COMPANY_GENERATION_METHOD, CompanyGenerationMethod.WINDCHILD.name()));
    }

    public void setDefaultCompanyGenerationMethod(final CompanyGenerationMethod value) {
        userPreferences.node(MHQConstants.MISCELLANEOUS_NODE).put(MHQConstants.DEFAULT_COMPANY_GENERATION_METHOD,
                value.name());
    }
    // endregion Miscellaneous Options
}
