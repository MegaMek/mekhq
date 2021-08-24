/*
 * MekHqConstants.java
 *
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
    // This is used in creating the name of save files, e.g. the MekHQ campaign file
    public static final String FILENAME_DATE_FORMAT = "yyyyMMdd";
    public static final int MAXIMUM_D6_VALUE = 6;
    public static final int ASTECH_TEAM_SIZE = 6;

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
    public static final String NEW_DAY_ASTECH_POOL_FILL = "newDayAstechPoolFill";
    public static final String NEW_DAY_MEDIC_POOL_FILL = "newDayMedicPoolFill";
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
    public static final String RANK_SYSTEMS_DIRECTORY_PATH = "rankSystemsDirectoryPath";
    public static final String INDIVIDUAL_RANK_SYSTEM_DIRECTORY_PATH = "individualRankSystemDirectoryPath";
    //endregion File Paths

    //region Nag Tab
    public static final String NAG_NODE = "mekhq/prefs/nags";
    public static final String NAG_UNMAINTAINED_UNITS = "nagUnmaintainedUnits";
    public static final String NAG_INSUFFICIENT_ASTECHS = "nagInsufficientAstechs";
    public static final String NAG_INSUFFICIENT_ASTECH_TIME = "nagInsufficientAstechTime";
    public static final String NAG_INSUFFICIENT_MEDICS = "nagInsufficientMedics";
    public static final String NAG_SHORT_DEPLOYMENT = "nagShortDeployment";
    public static final String NAG_UNRESOLVED_STRATCON_CONTACTS = "nagUnresolvedStratConContacts";
    public static final String NAG_OUTSTANDING_SCENARIOS = "nagOutstandingScenarios";
    //endregion Nag Tab

    //region Miscellaneous Options
    public static final String MISCELLANEOUS_NODE = "mekhq/prefs/miscellaneous";
    public static final String START_GAME_DELAY = "startGameDelay";
    //endregion Miscellaneous Options
    //endregion MekHQ Options

    //region File Paths
    // This holds all required file paths not saved as part of MekHQ Options
    public static final String LAYERED_FORCE_ICON_TYPE_PATH = "Pieces/Type/";
    public static final String LAYERED_FORCE_ICON_FORMATION_PATH = "Pieces/Formations/";
    public static final String LAYERED_FORCE_ICON_ADJUSTMENT_PATH = "Pieces/Adjustments/";
    public static final String LAYERED_FORCE_ICON_ALPHANUMERIC_PATH = "Pieces/Alphanumerics/";
    public static final String LAYERED_FORCE_ICON_SPECIAL_MODIFIER_PATH = "Pieces/Special Modifiers/";
    public static final String LAYERED_FORCE_ICON_BACKGROUND_PATH = "Pieces/Backgrounds/";
    public static final String LAYERED_FORCE_ICON_FRAME_PATH = "Pieces/Frames/";
    public static final String LAYERED_FORCE_ICON_LOGO_PATH = "Pieces/Logos/";
    public static final String AWARDS_DIRECTORY_PATH = "data/universe/awards/";
    public static final String RATINFO_DIR = "data/universe/ratdata/";
    public static final String ERAS_FILE_PATH = "data/universe/eras.xml";
    public static final String FACTION_HINTS_FILE = "data/universe/factionhints.xml";
    public static final String RANKS_FILE_PATH = "data/universe/ranks.xml";
    public static final String USER_RANKS_FILE_PATH = "userdata/data/universe/ranks.xml";

    //region StratCon
    public static final String STRATCON_REQUIRED_HOSTILE_FACILITY_MODS = "./data/scenariomodifiers/requiredHostileFacilityModifiers.xml";
    public static final String STRATCON_HOSTILE_FACILITY_MODS = "./data/scenariomodifiers/hostileFacilityModifiers.xml";
    public static final String STRATCON_ALLIED_FACILITY_MODS = "./data/scenariomodifiers/alliedFacilityModifiers.xml";
    public static final String STRATCON_GROUND_MODS = "./data/scenariomodifiers/groundBattleModifiers.xml";
    public static final String STRATCON_AIR_MODS = "./data/scenariomodifiers/airBattleModifiers.xml";
    public static final String STRATCON_PRIMARY_PLAYER_FORCE_MODS = "./data/scenariomodifiers/primaryPlayerForceModifiers.xml";
    public static final String STRATCON_SCENARIO_MANIFEST = "./data/scenariotemplates/ScenarioManifest.xml";
    public static final String STRATCON_USER_SCENARIO_MANIFEST = "./data/scenariotemplates/UserScenarioManifest.xml";
    public static final String STRATCON_SCENARIO_TEMPLATE_PATH = "./data/ScenarioTemplates/";
    public static final String STRATCON_FACILITY_MANIFEST = "./data/stratconfacilities/facilitymanifest.xml";
    public static final String STRATCON_USER_FACILITY_MANIFEST = "./data/stratconfacilities/userfacilitymanifest.xml";
    public static final String STRATCON_FACILITY_PATH = "./data/stratconfacilities/";
    public static final String STRATCON_CONTRACT_MANIFEST = "./data/stratconcontractdefinitions/ContractDefinitionManifest.xml";
    public static final String STRATCON_USER_CONTRACT_MANIFEST = "./data/stratconcontractdefinitions/UserContractDefinitionManifest.xml";
    public static final String STRATCON_CONTRACT_PATH = "./data/stratconcontractdefinitions/";
    public static final String HOSTILE_FACILITY_SCENARIO = "Hostile Facility.xml";
    public static final String ALLIED_FACILITY_SCENARIO = "Allied Facility.xml";
    public static final String SCENARIO_MODIFIER_ALLIED_GROUND_UNITS = "PrimaryAlliesGround.xml";
    public static final String SCENARIO_MODIFIER_ALLIED_AIR_UNITS = "PrimaryAlliesAir.xml";
    public static final String SCENARIO_MODIFIER_LIAISON_GROUND = "LiaisonGround.xml";
    public static final String SCENARIO_MODIFIER_HOUSE_CO_GROUND = "HouseOfficerGround.xml";
    public static final String SCENARIO_MODIFIER_INTEGRATED_UNITS_GROUND = "IntegratedAlliesGround.xml";
    public static final String SCENARIO_MODIFIER_LIAISON_AIR = "LiaisonAir.xml";
    public static final String SCENARIO_MODIFIER_HOUSE_CO_AIR = "HouseOfficerAir.xml";
    public static final String SCENARIO_MODIFIER_INTEGRATED_UNITS_AIR = "IntegratedAlliesAir.xml";
    public static final String SCENARIO_MODIFIER_TRAINEES_AIR = "AlliedTraineesAir.xml";
    public static final String SCENARIO_MODIFIER_TRAINEES_GROUND = "AlliedTraineesGround.xml";
    public static final String SCENARIO_MODIFIER_ALLIED_GROUND_SUPPORT = "AlliedGroundSupportImmediate.xml";
    public static final String SCENARIO_MODIFIER_ALLIED_AIR_SUPPORT = "AlliedAirSupportImmediate.xml";
    public static final String SCENARIO_MODIFIER_ALLIED_ARTY_SUPPORT = "AlliedArtillerySupportImmediate.xml";
    //endregion StratCon
    //endregion File Paths
}
