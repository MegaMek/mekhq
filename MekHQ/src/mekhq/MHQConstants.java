/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.SuiteConstants;

import java.time.LocalDate;
import java.time.Month;

/**
 * These are constants that hold across MekHQ.
 */
public final class MHQConstants extends SuiteConstants {
    //region General Constants
    public static final String PROJECT_NAME = "MekHQ";
    public static final int ASTECH_TEAM_SIZE = 6;
    public static final int MAX_JUMP_RADIUS = 30; //
    public static final int PREGNANCY_STANDARD_DURATION = 280; // standard duration of a pregnancy in days (40 weeks)
    public static final String EGO_OBJECTIVE_NAME = "Player";
    //endregion General Constants

    //region Faction Generation Constants
    public static final int FACTION_GENERATOR_BORDER_RANGE_IS = 60;
    public static final int FACTION_GENERATOR_BORDER_RANGE_CLAN = 90;
    public static final int FACTION_GENERATOR_BORDER_RANGE_NEAR_PERIPHERY = 90;
    public static final int FACTION_GENERATOR_BORDER_RANGE_DEEP_PERIPHERY = 210; // a bit more than this distance between HL and NC
    public static final LocalDate FORTRESS_REPUBLIC = LocalDate.of(3135, Month.NOVEMBER, 1);
    //endregion Faction Generation Constants

    //region GUI Constants
    public static final String COMMAND_OPEN_POPUP = "SHIFT_F10";
    public static final int BASE_SCROLLER_THRESHOLD = 20;
    //endregion GUI Constants

    //region MHQOptions
    //region Display
    public static final String DISPLAY_NODE = "mekhq/prefs/display";
    public static final String DISPLAY_DATE_FORMAT = "displayDateFormat";
    public static final String LONG_DISPLAY_DATE_FORMAT = "longDisplayDateFormat";
    public static final String HISTORICAL_DAILY_LOG = "historicalDailyLog";
    public static final int MAX_HISTORICAL_LOG_DAYS = 120; // max number of days that will be stored in the history, also used as a limit in the UI
    public static final String COMPANY_GENERATOR_STARTUP = "companyGeneratorStartup";
    public static final String SHOW_COMPANY_GENERATOR = "showCompanyGenerator";

    //region Command Center Tab
    public static final String COMMAND_CENTER_USE_UNIT_MARKET = "commandCenterUseUnitMarket";
    public static final String COMMAND_CENTER_MRMS = "commandCenterMRMS";
    //endregion Command Center Tab

    //region Interstellar Map Tab
    public static final String INTERSTELLAR_MAP_SHOW_JUMP_RADIUS = "interstellarMapShowJumpRadius";
    public static final String INTERSTELLAR_MAP_SHOW_JUMP_RADIUS_MINIMUM_ZOOM = "interstellarMapShowJumpRadiusMinimumZoom";
    public static final String INTERSTELLAR_MAP_JUMP_RADIUS_COLOUR = "interstellarMapJumpRadiusColour";
    public static final String INTERSTELLAR_MAP_SHOW_PLANETARY_ACQUISITION_RADIUS = "interstellarMapShowPlanetaryAcquisitionRadius";
    public static final String INTERSTELLAR_MAP_SHOW_PLANETARY_ACQUISITION_RADIUS_MINIMUM_ZOOM = "interstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom";
    public static final String INTERSTELLAR_MAP_PLANETARY_ACQUISITION_RADIUS_COLOUR = "interstellarMapPlanetaryAcquisitionRadiusColour";
    public static final String INTERSTELLAR_MAP_SHOW_CONTRACT_SEARCH_RADIUS = "interstellarMapShowContractSearchRadius";
    public static final String INTERSTELLAR_MAP_CONTRACT_SEARCH_RADIUS_COLOUR = "interstellarMapContractSearchRadiusColour";
    //endregion Interstellar Map Tab

    //region Personnel Tab
    public static final String PERSONNEL_FILTER_STYLE = "personnelFilterStyle";
    public static final String PERSONNEL_FILTER_ON_PRIMARY_ROLE = "personnelFilterOnPrimaryRole";
    //endregion Personnel Tab
    //endregion Display

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
    public static final String PREGNANT_FOREGROUND = "pregnantForeground";
    public static final String PREGNANT_BACKGROUND = "pregnantBackground";
    public static final String PAID_RETIREMENT_FOREGROUND = "paidRetirementForeground";
    public static final String PAID_RETIREMENT_BACKGROUND = "paidRetirementBackground";
    public static final String STRATCON_HEX_COORD_FOREGROUND = "stratconHexCoordForeground";
    //endregion Colours

    //region Fonts
    public static final String FONTS_NODE = "mekhq/prefs/fonts";
    public static final String MEDICAL_VIEW_DIALOG_HANDWRITING_FONT = "medicalViewDialogHandwritingFont";
    //endregion Fonts

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
    public static final String NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS = "newDayForceIconOperationalStatus";
    public static final String NEW_DAY_FORCE_ICON_OPERATIONAL_STATUS_STYLE = "newDayForceIconOperationalStatusStyle";
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
    public static final String UNIT_SPRITE_EXPORT_DIRECTORY_PATH = "unitSpriteExportDirectoryPath";
    public static final String LAYERED_FORCE_ICON_DIRECTORY_PATH = "layeredForceIconDirectoryPath";
    public static final String COMPANY_GENERATION_DIRECTORY_PATH = "companyGenerationDirectoryPath";
    //endregion File Paths

    //region Nag Tab
    public static final String NAG_NODE = "mekhq/prefs/nags";
    public static final String NAG_UNMAINTAINED_UNITS = "nagUnmaintainedUnits";
    public static final String NAG_PREGNANT_COMBATANT = "nagPregnantCombatant";
    public static final String NAG_PRISONERS = "nagPrisoners";
    public static final String NAG_UNTREATED_PERSONNEL = "nagUntreatedPersonnel";
    public static final String NAG_INSUFFICIENT_ASTECHS = "nagInsufficientAstechs";
    public static final String NAG_INSUFFICIENT_ASTECH_TIME = "nagInsufficientAstechTime";
    public static final String NAG_INSUFFICIENT_MEDICS = "nagInsufficientMedics";
    public static final String NAG_SHORT_DEPLOYMENT = "nagShortDeployment";
    public static final String NAG_UNRESOLVED_STRATCON_CONTACTS = "nagUnresolvedStratConContacts";
    public static final String NAG_OUTSTANDING_SCENARIOS = "nagOutstandingScenarios";
    public static final String NAG_CARGO_CAPACITY = "nagCargoCapacity";
    //endregion Nag Tab

    //region Miscellaneous Options
    public static final String MISCELLANEOUS_NODE = "mekhq/prefs/miscellaneous";
    public static final String START_GAME_DELAY = "startGameDelay";
    public static final String START_GAME_CLIENT_DELAY = "startGameClientDelay";
    public static final String START_GAME_CLIENT_RETRY_COUNT = "startGameClientRetryCount";
    public static final String START_GAME_BOT_CLIENT_DELAY = "startGameBotClientDelay";
    public static final String START_GAME_BOT_CLIENT_RETRY_COUNT = "startGameBotClientRetryCount";
    public static final String DEFAULT_COMPANY_GENERATION_METHOD = "defaultCompanyGenerationMethod";
    //endregion Miscellaneous Options
    //endregion MHQOptions

    //region File Paths
    // This holds all required file paths not saved as part of MekHQ Options
    public static final String LAYERED_FORCE_ICON_ADJUSTMENT_PATH = "Pieces/Adjustments/";
    public static final String LAYERED_FORCE_ICON_ALPHANUMERIC_PATH = "Pieces/Alphanumerics/";
    public static final String LAYERED_FORCE_ICON_ALPHANUMERIC_BOTTOM_RIGHT_PATH = "Bottom Right/";
    public static final String LAYERED_FORCE_ICON_ALPHANUMERIC_HQ_FILENAME = "HQ.png";
    public static final String LAYERED_FORCE_ICON_BACKGROUND_PATH = "Pieces/Backgrounds/";
    public static final String LAYERED_FORCE_ICON_FORMATION_PATH = "Pieces/Formations/";
    public static final String LAYERED_FORCE_ICON_FORMATION_CLAN_PATH = "Clan/";
    public static final String LAYERED_FORCE_ICON_FORMATION_STAR_FILENAME = "(02) Star.png";
    public static final String LAYERED_FORCE_ICON_FORMATION_TRINARY_FILENAME = "(06) Trinary.png";
    public static final String LAYERED_FORCE_ICON_FORMATION_COMSTAR_PATH = "ComStar/";
    public static final String LAYERED_FORCE_ICON_FORMATION_LEVEL_II_FILENAME = "(02) Level II.png";
    public static final String LAYERED_FORCE_ICON_FORMATION_LEVEL_III_FILENAME = "(04) Level III.png";
    public static final String LAYERED_FORCE_ICON_FORMATION_INNER_SPHERE_PATH = "Inner Sphere/";
    public static final String LAYERED_FORCE_ICON_FORMATION_LANCE_FILENAME = "(04) Lance.png";
    public static final String LAYERED_FORCE_ICON_FORMATION_COMPANY_FILENAME = "(05) Company.png";
    public static final String LAYERED_FORCE_ICON_FRAME_PATH = "Pieces/Frames/";
    public static final String LAYERED_FORCE_ICON_DEFAULT_FRAME_FILENAME = "Frame.png";
    public static final String LAYERED_FORCE_ICON_LOGO_PATH = "Pieces/Logos/";
    public static final String LAYERED_FORCE_ICON_SPECIAL_MODIFIER_PATH = "Pieces/Special Modifiers/";
    public static final String LAYERED_FORCE_ICON_OPERATIONAL_STATUS_BORDER_PATH = "Operational Indicators (Border)/";
    public static final String LAYERED_FORCE_ICON_OPERATIONAL_STATUS_TAB_PATH = "Operational Indicators (Tab)/";
    public static final String LAYERED_FORCE_ICON_OPERATIONAL_STATUS_FULLY_OPERATIONAL_FILENAME = "(01) Green - Fully Operational.png";
    public static final String LAYERED_FORCE_ICON_OPERATIONAL_STATUS_SUBSTANTIALLY_OPERATIONAL_FILENAME = "(02) Yellow - Substantially Operational.png";
    public static final String LAYERED_FORCE_ICON_OPERATIONAL_STATUS_MARGINALLY_OPERATIONAL_FILENAME = "(03) Red - Marginally Operational.png";
    public static final String LAYERED_FORCE_ICON_OPERATIONAL_STATUS_NOT_OPERATIONAL_FILENAME = "(04) Gray - Not Operational.png";
    public static final String LAYERED_FORCE_ICON_OPERATIONAL_STATUS_FACTORY_FRESH_FILENAME = "(05) Blue - Factory Fresh.png";
    public static final String LAYERED_FORCE_ICON_TYPE_PATH = "Pieces/Types/";
    public static final String LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH = "StratOps/";
    public static final String LAYERED_FORCE_ICON_BATTLEMECH_LEFT_FILENAME = "BattleMech (Left).png";
    public static final String LAYERED_FORCE_ICON_BATTLEMECH_CENTER_FILENAME = "BattleMech (Center).png";
    public static final String CUSTOM_MECHFILES_DIRECTORY_PATH = "data/mechfiles/customs/";
    public static final String AWARDS_DIRECTORY_PATH = "data/universe/awards/";
    public static final String RATINFO_DIR = "data/universe/ratdata/";
    public static final String ERAS_FILE_PATH = "data/universe/eras.xml";
    public static final String FACTION_HINTS_FILE = "data/universe/factionhints.xml";
    public static final String FINANCIAL_INSTITUTIONS_FILE_PATH = "data/universe/financialInstitutions.xml";
    public static final String RANDOM_DEATH_CAUSES_FILE_PATH = "data/universe/randomDeathCauses.xml";
    public static final String RANKS_FILE_PATH = "data/universe/ranks.xml";
    public static final String CAMPAIGN_PRESET_DIRECTORY = "mmconf/campaignPresets/";
    public static final String USER_FINANCIAL_INSTITUTIONS_FILE_PATH = "userdata/data/universe/financialInstitutions.xml";
    public static final String USER_RANDOM_DEATH_CAUSES_FILE_PATH = "userdata/data/universe/randomDeathCauses.xml";
    public static final String USER_RANKS_FILE_PATH = "userdata/data/universe/ranks.xml";
    public static final String USER_CAMPAIGN_PRESET_DIRECTORY = "userdata/mmconf/campaignPresets/";
    public static final String STRATCON_MUL_FILES_DIRECTORY="data/scenariotemplates/fixedmuls/";

    //region StratCon
    public static final String STRATCON_REQUIRED_HOSTILE_FACILITY_MODS = "./data/scenariomodifiers/requiredHostileFacilityModifiers.xml";
    public static final String STRATCON_HOSTILE_FACILITY_MODS = "./data/scenariomodifiers/hostileFacilityModifiers.xml";
    public static final String STRATCON_ALLIED_FACILITY_MODS = "./data/scenariomodifiers/alliedFacilityModifiers.xml";
    public static final String STRATCON_GROUND_MODS = "./data/scenariomodifiers/groundBattleModifiers.xml";
    public static final String STRATCON_AIR_MODS = "./data/scenariomodifiers/airBattleModifiers.xml";
    public static final String STRATCON_PRIMARY_PLAYER_FORCE_MODS = "./data/scenariomodifiers/primaryPlayerForceModifiers.xml";
    public static final String STRATCON_SCENARIO_MANIFEST = "./data/scenariotemplates/ScenarioManifest.xml";
    public static final String STRATCON_USER_SCENARIO_MANIFEST = "./data/scenariotemplates/UserScenarioManifest.xml";
    public static final String STRATCON_SCENARIO_TEMPLATE_PATH = "./data/scenariotemplates/";
    public static final String STRATCON_FACILITY_MANIFEST = "./data/stratconfacilities/facilitymanifest.xml";
    public static final String STRATCON_USER_FACILITY_MANIFEST = "./data/stratconfacilities/userfacilitymanifest.xml";
    public static final String STRATCON_FACILITY_PATH = "./data/stratconfacilities/";
    public static final String STRATCON_CONTRACT_MANIFEST = "./data/stratconcontractdefinitions/ContractDefinitionManifest.xml";
    public static final String STRATCON_USER_CONTRACT_MANIFEST = "./data/stratconcontractdefinitions/UserContractDefinitionManifest.xml";
    public static final String STRATCON_CONTRACT_PATH = "./data/stratconcontractdefinitions/";

    public static final String STRATCON_BIOME_MANIFEST_PATH = "./data/stratconbiomedefinitions/StratconBiomeManifest.xml";
    public static final String TERRAIN_CONDITIONS_ODDS_MANIFEST_PATH = "./data/terrainconditionsodds/TerrainConditionsOddsManifest.xml";
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

    //region StoryArcs
    public static final String STORY_ARC_DIRECTORY = "data/storyarcs/";
    public static final String USER_STORY_ARC_DIRECTORY = "userdata/storyarcs/";
    public static final String STORY_ARC_FILE = "storyArc.xml";
    public static final String STORY_ARC_CAMPAIGN_FILE = "initCampaign.cpnx.gz";
    //endregion StoryArcs

    //endregion File Paths
}
