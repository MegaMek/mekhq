/*
 * MekHqConstants.java
 *
 * Copyright (c) 2019 - The MegaMek Team. All rights reserved.
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

public final class MekHqConstants {
    //region MekHQ Options
    //region Display
    public static final String DISPLAY_NODE = "mekhq/prefs/display";
    public static final String DISPLAY_DATE_FORMAT = "displayDateFormat";
    public static final String LONG_DISPLAY_DATE_FORMAT = "longDisplayDateFormat";
    public static final String HISTORICAL_DAILY_LOG = "historicalDailyLog";
    public static final int MAX_HISTORICAL_LOG_DAYS = 120; // max number of days that will be stored in the history, also used as a limit in the UI

    //region Command Center
    public static final String COMMAND_CENTER_USE_UNIT_MARKET = "commandCenterUseUnitMarket";
    public static final String COMMAND_CENTER_MRMS = "commandCenterMRMS";
    //endregion Command Center

    //region Personnel Tab Display Options
    public static final String PERSONNEL_FILTER_STYLE = "personnelFilterStyle";
    public static final String PERSONNEL_FILTER_ON_PRIMARY_ROLE = "personnelFilterOnPrimaryRole";
    //endregion Personnel Tab Display Options

    //region Colours
    public static final String DEPLOYED_FOREGROUND = "deployedForeground";
    public static final String DEPLOYED_BACKGROUND = "deployedBackground";
    public static final String BELOW_CONTRACT_MINIMUM_FOREGROUND = "belowContractMinimumForeground";
    public static final String BELOW_CONTRACT_MINIMUM_BACKGROUND = "belowContractMinimumBackground";
    public static final String IN_TRANSIT_FOREGROUND = "inTransitForeground";
    public static final String IN_TRANSIT_BACKGROUND = "inTransitBackground";
    public static final String REFITTING_FOREGROUND = "refittingForeground";
    public static final String REFITTING_BACKGROUND = "refittingBackground";
    public static final String MOTHBALLING_FOREGROUND = "mothballingForeground";
    public static final String MOTHBALLING_BACKGROUND = "mothballingBackground";
    public static final String MOTHBALLED_FOREGROUND = "mothballedForeground";
    public static final String MOTHBALLED_BACKGROUND = "mothballedBackground";
    public static final String NOT_REPAIRABLE_FOREGROUND = "notRepairableForeground";
    public static final String NOT_REPAIRABLE_BACKGROUND = "notRepairableBackground";
    public static final String NON_FUNCTIONAL_FOREGROUND = "nonFunctionalForeground";
    public static final String NON_FUNCTIONAL_BACKGROUND = "nonFunctionalBackground";
    public static final String NEEDS_PARTS_FIXED_FOREGROUND = "needsPartsFixedForeground";
    public static final String NEEDS_PARTS_FIXED_BACKGROUND = "needsPartsFixedBackground";
    public static final String UNMAINTAINED_FOREGROUND = "unmaintainedForeground";
    public static final String UNMAINTAINED_BACKGROUND = "unmaintainedBackground";
    public static final String UNCREWED_FOREGROUND = "uncrewedForeground";
    public static final String UNCREWED_BACKGROUND = "uncrewedBackground";
    public static final String LOAN_OVERDUE_FOREGROUND = "loanOverdueForeground";
    public static final String LOAN_OVERDUE_BACKGROUND = "loanOverdueBackground";
    public static final String INJURED_FOREGROUND = "injuredForeground";
    public static final String INJURED_BACKGROUND = "injuredBackground";
    public static final String HEALED_INJURIES_FOREGROUND = "healedInjuriesForeground";
    public static final String HEALED_INJURIES_BACKGROUND = "healedInjuriesBackground";
    public static final String PAID_RETIREMENT_FOREGROUND = "paidRetirementForeground";
    public static final String PAID_RETIREMENT_BACKGROUND = "paidRetirementBackground";
    //endregion Colours
    //endregion Display

    //region Autosave
    public static final String AUTOSAVE_NODE = "mekhq/prefs/autosave";
    public static final String NO_SAVE_KEY = "noSave";
    public static final String SAVE_DAILY_KEY = "saveDaily";
    public static final String SAVE_WEEKLY_KEY = "saveWeekly";
    public static final String SAVE_MONTHLY_KEY = "saveMonthly";
    public static final String SAVE_YEARLY_KEY = "saveYearly";
    public static final String SAVE_BEFORE_MISSIONS_KEY = "saveBeforeMissions";
    public static final String MAXIMUM_NUMBER_SAVES_KEY = "maximumNumberAutoSaves";
    public static final int DEFAULT_NUMBER_SAVES = 5;
    //endregion Autosave

    //region New Day
    public static final String NEW_DAY_NODE = "mekhq/prefs/newDay";
    public static final String NEW_DAY_MRMS = "newDayMRMS";
    //endregion New Day

    //region Campaign XML Save Options
    public static final String XML_SAVES_NODE = "mekhq/prefs/xmlsaves";
    public static final String PREFER_GZIPPED_CAMPAIGN_FILE = "preferGzippedCampaignFile";
    public static final String WRITE_CUSTOMS_TO_XML = "writeCustomsToXML";
    public static final String SAVE_MOTHBALL_STATE = "saveMothballState";
    //endregion Campaign XML Save Options

    //region File Paths
    public static final String FILE_PATH_NODE = "mekhq/prefs/filepaths";
    public static final String AWARDS_DIRECTORY_PATH = "awardsDirectoryPath";
    //endregion File Paths

    //region Miscellaneous Options
    public static final String MISCELLANEOUS_NODE = "mekhq/prefs/miscellaneous";
    public static final String START_GAME_DELAY = "startGameDelay";
    //endregion Miscellaneous Options
    //endregion MekHQ Options

    /** This is used in creating the name of save files, e.g. the MekHQ campaign file */
    public static final String FILENAME_DATE_FORMAT = "yyyyMMdd";
}
