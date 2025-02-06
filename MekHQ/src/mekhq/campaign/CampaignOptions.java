/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2020-2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import megamek.common.enums.SkillLevel;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.autoresolve.AutoResolveMethod;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.Skills;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.service.mrms.MRMSOption;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author natit
 */
public class CampaignOptions {
    private static final MMLogger logger = MMLogger.create(CampaignOptions.class);
    private static final ClientPreferences CLIENT_PREFERENCES = PreferenceManager.getClientPreferences();
    // region Magic Numbers
    public static final int TECH_INTRO = 0;
    public static final int TECH_STANDARD = 1;
    public static final int TECH_ADVANCED = 2;
    public static final int TECH_EXPERIMENTAL = 3;
    public static final int TECH_UNOFFICIAL = 4;
    // This must always be the highest tech level to hide parts
    // that haven't been invented yet, or that are completely extinct
    public static final int TECH_UNKNOWN = 5;

    public static final int TRANSIT_UNIT_WEEK = 1;
    public static final int TRANSIT_UNIT_MONTH = 2;

    public static final String S_TECH = "Tech";
    public static final String S_AUTO = "Automatic Success";

    public static final double MAXIMUM_COMBAT_EQUIPMENT_PERCENT = 5.0;
    public static final double MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT = 1.0;
    public static final double MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT = 1.0;
    public static final double MAXIMUM_WARSHIP_EQUIPMENT_PERCENT = 1.0;

    public static String getTechLevelName(final int techLevel) {
        return switch (techLevel) {
            case TECH_INTRO -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_INTRO];
            case TECH_STANDARD -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_STANDARD];
            case TECH_ADVANCED -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_ADVANCED];
            case TECH_EXPERIMENTAL -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_EXPERIMENTAL];
            case TECH_UNOFFICIAL -> TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_UNOFFICIAL];
            default -> "Unknown";
        };
    }
    // endregion Magic Numbers

    // region Variable Declarations
    // region General Tab
    private UnitRatingMethod unitRatingMethod;
    private int manualUnitRatingModifier;
    // endregion General Tab

    // region Repair and Maintenance Tab
    // Repair
    private boolean useEraMods;
    private boolean assignedTechFirst;
    private boolean resetToFirstTech;
    private boolean useQuirks;
    private boolean useAeroSystemHits;
    private boolean destroyByMargin;
    private int destroyMargin;
    private int destroyPartTarget;

    // Maintenance
    private boolean checkMaintenance;
    private int maintenanceCycleDays;
    private int maintenanceBonus;
    private boolean useQualityMaintenance;
    private boolean reverseQualityNames;
    private boolean useRandomUnitQualities;
    private boolean usePlanetaryModifiers;
    private boolean useUnofficialMaintenance;
    private boolean logMaintenance;
    private int defaultMaintenanceTime;

    // Mass Repair / Mass Salvage
    private boolean mrmsUseRepair;
    private boolean mrmsUseSalvage;
    private boolean mrmsUseExtraTime;
    private boolean mrmsUseRushJob;
    private boolean mrmsAllowCarryover;
    private boolean mrmsOptimizeToCompleteToday;
    private boolean mrmsScrapImpossible;
    private boolean mrmsUseAssignedTechsFirst;
    private boolean mrmsReplacePod;
    private List<MRMSOption> mrmsOptions;
    // endregion Repair and Maintenance Tab

    // region Supplies and Acquisition Tab
    // Acquisition
    private int waitingPeriod;
    private String acquisitionSkill;
    private boolean acquisitionSupportStaffOnly;
    private int clanAcquisitionPenalty;
    private int isAcquisitionPenalty;
    private int maxAcquisitions;

    // autoLogistics
    private int autoLogisticsHeatSink;
    private int autoLogisticsMekHead;
    private int autoLogisticsMekLocation;
    private int autoLogisticsNonRepairableLocation;
    private int autoLogisticsArmor;
    private int autoLogisticsAmmunition;
    private int autoLogisticsOther;

    // Delivery
    private int unitTransitTime;

    // Planetary Acquisition
    private boolean usePlanetaryAcquisition;
    private int maxJumpsPlanetaryAcquisition;
    private PlanetaryAcquisitionFactionLimit planetAcquisitionFactionLimit;
    private boolean planetAcquisitionNoClanCrossover;
    private boolean noClanPartsFromIS;
    private int penaltyClanPartsFromIS;
    private boolean planetAcquisitionVerbose;
    private final int[] planetTechAcquisitionBonus;
    private final int[] planetIndustryAcquisitionBonus;
    private final int[] planetOutputAcquisitionBonus;
    // endregion Supplies and Acquisition Tab

    // region Tech Limits Tab
    private boolean limitByYear;
    private boolean disallowExtinctStuff;
    private boolean allowClanPurchases;
    private boolean allowISPurchases;
    private boolean allowCanonOnly;
    private boolean allowCanonRefitOnly;
    private int techLevel;
    private boolean variableTechLevel;
    private boolean factionIntroDate;
    private boolean useAmmoByType; // Unofficial
    // endregion Tech Limits Tab

    // region Personnel Tab
    // General Personnel
    private boolean useTactics;
    private boolean useInitiativeBonus;
    private boolean useToughness;
    private boolean useRandomToughness;
    private boolean useArtillery;
    private boolean useAbilities;
    private boolean useEdge;
    private boolean useSupportEdge;
    private boolean useImplants;
    private boolean alternativeQualityAveraging;
    private boolean useTransfers;
    private boolean useExtendedTOEForceName;
    private boolean personnelLogSkillGain;
    private boolean personnelLogAbilityGain;
    private boolean personnelLogEdgeGain;
    private boolean displayPersonnelLog;
    private boolean displayScenarioLog;
    private boolean displayKillRecord;

    // Expanded Personnel Information
    private boolean useTimeInService;
    private TimeInDisplayFormat timeInServiceDisplayFormat;
    private boolean useTimeInRank;
    private TimeInDisplayFormat timeInRankDisplayFormat;
    private boolean trackTotalEarnings;
    private boolean trackTotalXPEarnings;
    private boolean showOriginFaction;

    // Admin
    private boolean adminsHaveNegotiation;
    private boolean adminsHaveScrounge;
    private boolean adminExperienceLevelIncludeNegotiation;
    private boolean adminExperienceLevelIncludeScrounge;

    // Medical
    private boolean useAdvancedMedical; // Unofficial
    private int healWaitingPeriod;
    private int naturalHealingWaitingPeriod;
    private int minimumHitsForVehicles;
    private boolean useRandomHitsForVehicles;
    private boolean tougherHealing;
    private int maximumPatients;

    // Prisoners
    private PrisonerCaptureStyle prisonerCaptureStyle;
    private PrisonerStatus defaultPrisonerStatus;
    private boolean prisonerBabyStatus;
    private boolean useAtBPrisonerDefection;
    private boolean useAtBPrisonerRansom;

    // Dependent
    private boolean useRandomDependentAddition;
    private boolean useRandomDependentRemoval;

    // Personnel Removal
    private boolean usePersonnelRemoval;
    private boolean useRemovalExemptCemetery;
    private boolean useRemovalExemptRetirees;

    // Salary
    private boolean disableSecondaryRoleSalary;
    private double salaryAntiMekMultiplier;
    private double salarySpecialistInfantryMultiplier;
    private Map<SkillLevel, Double> salaryXPMultipliers;
    private Money[] roleBaseSalaries;

    // Awards
    private AwardBonus awardBonusStyle;
    private boolean enableAutoAwards;
    private boolean issuePosthumousAwards;
    private boolean issueBestAwardOnly;
    private boolean ignoreStandardSet;
    private int awardTierSize;
    private boolean enableContractAwards;
    private boolean enableFactionHunterAwards;
    private boolean enableInjuryAwards;
    private boolean enableIndividualKillAwards;
    private boolean enableFormationKillAwards;
    private boolean enableRankAwards;
    private boolean enableScenarioAwards;
    private boolean enableSkillAwards;
    private boolean enableTheatreOfWarAwards;
    private boolean enableTimeAwards;
    private boolean enableTrainingAwards;
    private boolean enableMiscAwards;
    private String awardSetFilterList;
    // endregion Personnel Tab

    // region Life Paths Tab
    // Personnel Randomization
    private boolean useDylansRandomXP; // Unofficial
    private int nonBinaryDiceSize;

    // Random Histories
    private RandomOriginOptions randomOriginOptions;
    private boolean useRandomPersonalities;
    private boolean useRandomPersonalityReputation;
    private boolean useIntelligenceXpMultiplier;
    private boolean useSimulatedRelationships;


    // Family
    private FamilialRelationshipDisplayLevel familyDisplayLevel;

    // Anniversaries
    private boolean announceBirthdays;
    private boolean announceRecruitmentAnniversaries;
    private boolean announceOfficersOnly;
    private boolean announceChildBirthdays;

    // Marriage
    private boolean useManualMarriages;
    private boolean useClanPersonnelMarriages;
    private boolean usePrisonerMarriages;
    private int checkMutualAncestorsDepth;
    private int noInterestInMarriageDiceSize;
    private boolean logMarriageNameChanges;
    private Map<MergingSurnameStyle, Integer> marriageSurnameWeights;
    private RandomMarriageMethod randomMarriageMethod;
    private boolean useRandomSameSexMarriages; // legacy, pre-50.01
    private boolean useRandomClanPersonnelMarriages;
    private boolean useRandomPrisonerMarriages;
    private int randomMarriageAgeRange;
    private int randomMarriageDiceSize;
    private int randomSameSexMarriageDiceSize;
    private int randomNewDependentMarriage;

    // Divorce
    private boolean useManualDivorce;
    private boolean useClanPersonnelDivorce;
    private boolean usePrisonerDivorce;
    private Map<SplittingSurnameStyle, Integer> divorceSurnameWeights;
    private RandomDivorceMethod randomDivorceMethod;
    private boolean useRandomOppositeSexDivorce;
    private boolean useRandomSameSexDivorce;
    private boolean useRandomClanPersonnelDivorce;
    private boolean useRandomPrisonerDivorce;
    private int randomDivorceDiceSize;

    // Procreation
    private boolean useManualProcreation;
    private boolean useClanPersonnelProcreation;
    private boolean usePrisonerProcreation;
    private int multiplePregnancyOccurrences;
    private BabySurnameStyle babySurnameStyle;
    private boolean assignNonPrisonerBabiesFounderTag;
    private boolean assignChildrenOfFoundersFounderTag;
    private boolean useMaternityLeave;
    private boolean determineFatherAtBirth;
    private boolean displayTrueDueDate;
    private int noInterestInChildrenDiceSize;
    private boolean logProcreation;
    private RandomProcreationMethod randomProcreationMethod;
    private boolean useRelationshiplessRandomProcreation;
    private boolean useRandomClanPersonnelProcreation;
    private boolean useRandomPrisonerProcreation;
    private int randomProcreationRelationshipDiceSize;
    private int randomProcreationRelationshiplessDiceSize;

    // Education
    private boolean useEducationModule;
    private Integer curriculumXpRate;
    private Integer maximumJumpCount;
    private boolean useReeducationCamps;
    private boolean enableLocalAcademies;
    private boolean enablePrestigiousAcademies;
    private boolean enableUnitEducation;
    private boolean enableOverrideRequirements;
    private boolean enableShowIneligibleAcademies;
    private int entranceExamBaseTargetNumber;
    private Double facultyXpRate;
    private boolean enableBonuses;
    private Integer adultDropoutChance;
    private Integer childrenDropoutChance;
    private boolean allAges;
    private Integer militaryAcademyAccidents;

    // Death
    private Map<AgeGroup, Boolean> enabledRandomDeathAgeGroups;
    private boolean useRandomDeathSuicideCause;
    private double randomDeathMultiplier;
    // endregion Life Paths Tab

    //region Turnover and Retention
    private boolean useRandomRetirement;

    private int turnoverFixedTargetNumber;
    private boolean aeroRecruitsHaveUnits;
    private boolean trackOriginalUnit;
    private TurnoverFrequency turnoverFrequency;
    private boolean useContractCompletionRandomRetirement;
    private boolean useRandomFounderTurnover;
    private boolean useFounderRetirement;
    private boolean useSubContractSoldiers;
    private int serviceContractDuration;
    private int serviceContractModifier;
    private boolean payBonusDefault;
    private int payBonusDefaultThreshold;

    private boolean useCustomRetirementModifiers;
    private boolean useFatigueModifiers;
    private boolean useSkillModifiers;
    private boolean useAgeModifiers;
    private boolean useUnitRatingModifiers;
    private boolean useFactionModifiers;
    private boolean useHostileTerritoryModifiers;
    private boolean useMissionStatusModifiers;
    private boolean useFamilyModifiers;
    private boolean useLoyaltyModifiers;
    private boolean useHideLoyalty;

    private int payoutRateOfficer;
    private int payoutRateEnlisted;
    private int payoutRetirementMultiplier;
    private boolean usePayoutServiceBonus;
    private int payoutServiceBonusRate;

    private boolean useAdministrativeStrain;
    private int administrativeCapacity;
    private int multiCrewStrainDivider;

    private boolean useManagementSkill;
    private boolean useCommanderLeadershipOnly;
    private int managementSkillPenalty;

    private boolean useFatigue;
    private int fatigueRate;
    private boolean useInjuryFatigue;
    private int fieldKitchenCapacity;
    private boolean fieldKitchenIgnoreNonCombatants;
    private int fatigueLeaveThreshold;
    //endregion Turnover and Retention

    //region Finance tab
    private boolean payForParts;
    private boolean payForRepairs;
    private boolean payForUnits;
    private boolean payForSalaries;
    private boolean payForOverhead;
    private boolean payForMaintain;
    private boolean payForTransport;
    private boolean sellUnits;
    private boolean sellParts;
    private boolean payForRecruitment;
    private boolean useLoanLimits;
    private boolean usePercentageMaint; // Unofficial
    private boolean infantryDontCount; // Unofficial
    private boolean usePeacetimeCost;
    private boolean useExtendedPartsModifier;
    private boolean showPeacetimeCost;
    private FinancialYearDuration financialYearDuration;
    private boolean newFinancialYearFinancesToCSVExport;

    // Price Multipliers
    private double commonPartPriceMultiplier;
    private double innerSphereUnitPriceMultiplier;
    private double innerSpherePartPriceMultiplier;
    private double clanUnitPriceMultiplier;
    private double clanPartPriceMultiplier;
    private double mixedTechUnitPriceMultiplier;
    private double[] usedPartPriceMultipliers;
    private double damagedPartsValueMultiplier;
    private double unrepairablePartsValueMultiplier;
    private double cancelledOrderRefundMultiplier;

    // Taxes
    private boolean useTaxes;
    private int taxesPercentage;

    // Shares
    private boolean useShareSystem;
    private boolean sharesForAll;
    // endregion Finance Tab

    // region Mercenary Tab
    private boolean equipmentContractBase;
    private double equipmentContractPercent;
    private boolean equipmentContractSaleValue;
    private double dropShipContractPercent;
    private double jumpShipContractPercent;
    private double warShipContractPercent;
    private boolean blcSaleValue;
    private boolean overageRepaymentInFinalPayment;
    // endregion Mercenary Tab

    // region Experience Tab
    private double xpCostMultiplier;
    private int scenarioXP;
    private int killXPAward;
    private int killsForXP;
    private int tasksXP;
    private int nTasksXP;
    private int successXP;
    private int mistakeXP;
    private int vocationalXP;
    private int vocationalXPCheckFrequency;
    private int vocationalXPTargetNumber;
    private int contractNegotiationXP;
    private int adminXP;
    private int adminXPPeriod;
    private int missionXpFail;
    private int missionXpSuccess;
    private int missionXpOutstandingSuccess;

    private int edgeCost;
    // endregion Experience Tab

    // region Skills Tab
    // endregion Skills Tab

    // region Special Abilities Tab
    // endregion Special Abilities Tab

    // region Skill Randomization Tab
    private final int[] phenotypeProbabilities;
    // endregion Skill Randomization Tab

    // region Rank System Tab
    // endregion Rank System Tab

    // region Name and Portrait Generation
    private boolean useOriginFactionForNames;
    private final boolean[] usePortraitForRole;
    private boolean assignPortraitOnRoleChange;
    // endregion Name and Portrait Generation

    // region Markets Tab
    // Personnel Market
    private String personnelMarketName;
    private boolean personnelMarketReportRefresh;
    private Map<SkillLevel, Integer> personnelMarketRandomRemovalTargets;
    private double personnelMarketDylansWeight;
    private boolean usePersonnelHireHiringHallOnly;

    // Unit Market
    private UnitMarketMethod unitMarketMethod;
    private boolean unitMarketRegionalMekVariations;
    private int unitMarketSpecialUnitChance;
    private int unitMarketRarityModifier;
    private boolean instantUnitMarketDelivery;
    private boolean unitMarketReportRefresh;

    // Contract Market
    private ContractMarketMethod contractMarketMethod;
    private int contractSearchRadius;
    private boolean variableContractLength;
    private boolean contractMarketReportRefresh;
    private int contractMaxSalvagePercentage;
    private int dropShipBonusPercentage;
    // endregion Markets Tab

    // region RATs Tab
    private boolean useStaticRATs;
    private String[] rats;
    private boolean ignoreRATEra;
    // endregion RATs Tab

    // region Against the Bot Tab
    private boolean useAtB;
    private boolean useStratCon;
    private SkillLevel skillLevel;

    // Unit Administration
    private boolean useAero;
    private boolean useVehicles;
    private boolean clanVehicles;

    // Contract Operations
    private boolean mercSizeLimited;
    private boolean restrictPartsByMission;
    private boolean limitLanceWeight;
    private boolean limitLanceNumUnits;
    private boolean useStrategy;
    private int baseStrategyDeployment;
    private int additionalStrategyDeployment;
    private final int[] atbBattleChance;
    private boolean generateChases;

    // Scenarios
    private boolean useGenericBattleValue;
    private boolean useVerboseBidding;
    private boolean doubleVehicles;
    private int opForLanceTypeMeks;
    private int opForLanceTypeMixed;
    private int opForLanceTypeVehicles;
    private boolean opForUsesVTOLs;
    private boolean allowOpForAeros;
    private int opForAeroChance;
    private boolean allowOpForLocalUnits;
    private int opForLocalUnitChance;
    private boolean adjustPlayerVehicles;
    private boolean regionalMekVariations;
    private boolean attachedPlayerCamouflage;
    private boolean playerControlsAttachedUnits;
    private boolean useDropShips;
    private boolean useWeatherConditions;
    private boolean useLightConditions;
    private boolean usePlanetaryConditions;
    private int fixedMapChance;
    private int spaUpgradeIntensity;
    private int scenarioModMax;
    private int scenarioModChance;
    private int scenarioModBV;
    private boolean autoConfigMunitions;
    private AutoResolveMethod autoResolveMethod;
    private String strategicViewMinimapTheme;
    private boolean autoResolveVictoryChanceEnabled;
    private int autoResolveNumberOfScenarios;
    private boolean autoResolveExperimentalPacarGuiEnabled;
    // endregion Against the Bot Tab
    // endregion Variable Declarations

    // region Constructors
    public CampaignOptions() {
        // Initialize any reused variables
        final PersonnelRole[] personnelRoles = PersonnelRole.values();

        // region General Tab
        unitRatingMethod = UnitRatingMethod.CAMPAIGN_OPS;
        manualUnitRatingModifier = 0;
        // endregion General Tab

        // region Repair and Maintenance Tab
        // Repair
        useEraMods = false;
        assignedTechFirst = false;
        resetToFirstTech = false;
        useQuirks = false;
        useAeroSystemHits = false;
        destroyByMargin = false;
        destroyMargin = 4;
        destroyPartTarget = 10;

        // Maintenance
        checkMaintenance = true;
        maintenanceCycleDays = 7;
        maintenanceBonus = -1;
        useQualityMaintenance = true;
        reverseQualityNames = false;
        setUseRandomUnitQualities(true);
        setUsePlanetaryModifiers(true);
        useUnofficialMaintenance = false;
        logMaintenance = false;
        defaultMaintenanceTime = 4;

        // Mass Repair / Mass Salvage
        setMRMSUseRepair(true);
        setMRMSUseSalvage(true);
        setMRMSUseExtraTime(true);
        setMRMSUseRushJob(true);
        setMRMSAllowCarryover(true);
        setMRMSOptimizeToCompleteToday(false);
        setMRMSScrapImpossible(false);
        setMRMSUseAssignedTechsFirst(false);
        setMRMSReplacePod(true);
        setMRMSOptions(new ArrayList<>());
        for (final PartRepairType type : PartRepairType.values()) {
            getMRMSOptions().add(new MRMSOption(type));
        }
        // endregion Repair and Maintenance Tab

        // region Supplies and Acquisitions Tab
        // Acquisition
        waitingPeriod = 7;
        acquisitionSkill = S_TECH;
        acquisitionSupportStaffOnly = true;
        clanAcquisitionPenalty = 0;
        isAcquisitionPenalty = 0;
        maxAcquisitions = 0;

        // autoLogistics
        autoLogisticsHeatSink = 250;
        autoLogisticsMekHead = 200;
        autoLogisticsMekLocation = 100;
        autoLogisticsNonRepairableLocation = 0;
        autoLogisticsArmor = 500;
        autoLogisticsAmmunition = 500;
        autoLogisticsOther = 50;

        // Delivery
        unitTransitTime = TRANSIT_UNIT_MONTH;

        // Planetary Acquisition
        usePlanetaryAcquisition = false;
        maxJumpsPlanetaryAcquisition = 2;
        planetAcquisitionFactionLimit = PlanetaryAcquisitionFactionLimit.NEUTRAL;
        planetAcquisitionNoClanCrossover = true;
        noClanPartsFromIS = true;
        penaltyClanPartsFromIS = 4;
        planetAcquisitionVerbose = false;
        // Planet Socio-Industrial Modifiers
        planetTechAcquisitionBonus = new int[6];
        planetTechAcquisitionBonus[EquipmentType.RATING_A] = -1;
        planetTechAcquisitionBonus[EquipmentType.RATING_B] = 0;
        planetTechAcquisitionBonus[EquipmentType.RATING_C] = 1;
        planetTechAcquisitionBonus[EquipmentType.RATING_D] = 2;
        planetTechAcquisitionBonus[EquipmentType.RATING_E] = 4;
        planetTechAcquisitionBonus[EquipmentType.RATING_F] = 8;
        planetIndustryAcquisitionBonus = new int[6];
        planetIndustryAcquisitionBonus[EquipmentType.RATING_A] = 0;
        planetIndustryAcquisitionBonus[EquipmentType.RATING_B] = 0;
        planetIndustryAcquisitionBonus[EquipmentType.RATING_C] = 0;
        planetIndustryAcquisitionBonus[EquipmentType.RATING_D] = 0;
        planetIndustryAcquisitionBonus[EquipmentType.RATING_E] = 0;
        planetIndustryAcquisitionBonus[EquipmentType.RATING_F] = 0;
        planetOutputAcquisitionBonus = new int[6];
        planetOutputAcquisitionBonus[EquipmentType.RATING_A] = -1;
        planetOutputAcquisitionBonus[EquipmentType.RATING_B] = 0;
        planetOutputAcquisitionBonus[EquipmentType.RATING_C] = 1;
        planetOutputAcquisitionBonus[EquipmentType.RATING_D] = 2;
        planetOutputAcquisitionBonus[EquipmentType.RATING_E] = 4;
        planetOutputAcquisitionBonus[EquipmentType.RATING_F] = 8;
        // endregion Supplies and Acquisitions Tab

        // region Tech Limits Tab
        limitByYear = true;
        disallowExtinctStuff = false;
        allowClanPurchases = true;
        allowISPurchases = true;
        allowCanonOnly = false;
        allowCanonRefitOnly = false;
        techLevel = TECH_EXPERIMENTAL;
        variableTechLevel = false;
        factionIntroDate = false;
        useAmmoByType = false;
        // endregion Tech Limits Tab

        // region Personnel Tab
        // General Personnel
        setUseTactics(false);
        setUseInitiativeBonus(false);
        setUseToughness(false);
        setUseRandomToughness(false);
        setUseArtillery(false);
        setUseAbilities(false);
        setUseEdge(false);
        setUseSupportEdge(false);
        setUseImplants(false);
        setAlternativeQualityAveraging(false);
        setUseTransfers(true);
        setUseExtendedTOEForceName(false);
        setPersonnelLogSkillGain(false);
        setPersonnelLogAbilityGain(false);
        setPersonnelLogEdgeGain(false);
        setDisplayPersonnelLog(false);
        setDisplayScenarioLog(false);
        setDisplayKillRecord(false);

        // Expanded Personnel Information
        setUseTimeInService(false);
        setTimeInServiceDisplayFormat(TimeInDisplayFormat.YEARS);
        setUseTimeInRank(false);
        setTimeInRankDisplayFormat(TimeInDisplayFormat.MONTHS_YEARS);
        setTrackTotalEarnings(false);
        setTrackTotalXPEarnings(false);
        setShowOriginFaction(true);

        // Admin
        setAdminsHaveNegotiation(false);
        setAdminExperienceLevelIncludeNegotiation(false);
        setAdminsHaveScrounge(false);
        setAdminExperienceLevelIncludeScrounge(false);

        // Medical
        setUseAdvancedMedical(false);
        setHealingWaitingPeriod(1);
        setNaturalHealingWaitingPeriod(15);
        setMinimumHitsForVehicles(1);
        setUseRandomHitsForVehicles(false);
        setTougherHealing(false);
        setMaximumPatients(25);

        // Prisoners
        setPrisonerCaptureStyle(PrisonerCaptureStyle.TAHARQA);
        setDefaultPrisonerStatus(PrisonerStatus.PRISONER);
        setPrisonerBabyStatus(true);
        setUseAtBPrisonerDefection(false);
        setUseAtBPrisonerRansom(false);

        // Dependent
        setUseRandomDependentAddition(false);
        setUseRandomDependentRemoval(false);

        // Personnel Removal
        setUsePersonnelRemoval(false);
        setUseRemovalExemptCemetery(false);
        setUseRemovalExemptRetirees(false);

        // Salary
        setDisableSecondaryRoleSalary(false);
        setSalaryAntiMekMultiplier(1.5);
        setSalarySpecialistInfantryMultiplier(1.28);
        setSalaryXPMultipliers(new HashMap<>());
        getSalaryXPMultipliers().put(SkillLevel.NONE, 0.5);
        getSalaryXPMultipliers().put(SkillLevel.ULTRA_GREEN, 0.6);
        getSalaryXPMultipliers().put(SkillLevel.GREEN, 0.6);
        getSalaryXPMultipliers().put(SkillLevel.REGULAR, 1.0);
        getSalaryXPMultipliers().put(SkillLevel.VETERAN, 1.6);
        getSalaryXPMultipliers().put(SkillLevel.ELITE, 3.2);
        getSalaryXPMultipliers().put(SkillLevel.HEROIC, 6.4);
        getSalaryXPMultipliers().put(SkillLevel.LEGENDARY, 12.8);
        setRoleBaseSalaries(new Money[personnelRoles.length]);
        setRoleBaseSalary(PersonnelRole.MEKWARRIOR, 1500);
        setRoleBaseSalary(PersonnelRole.LAM_PILOT, 2250);
        setRoleBaseSalary(PersonnelRole.GROUND_VEHICLE_DRIVER, 900);
        setRoleBaseSalary(PersonnelRole.NAVAL_VEHICLE_DRIVER, 900);
        setRoleBaseSalary(PersonnelRole.VTOL_PILOT, 900);
        setRoleBaseSalary(PersonnelRole.VEHICLE_GUNNER, 900);
        setRoleBaseSalary(PersonnelRole.VEHICLE_CREW, 900);
        setRoleBaseSalary(PersonnelRole.AEROSPACE_PILOT, 1500);
        setRoleBaseSalary(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT, 900);
        setRoleBaseSalary(PersonnelRole.PROTOMEK_PILOT, 960);
        setRoleBaseSalary(PersonnelRole.BATTLE_ARMOUR, 960);
        setRoleBaseSalary(PersonnelRole.SOLDIER, 750);
        setRoleBaseSalary(PersonnelRole.VESSEL_PILOT, 1000);
        setRoleBaseSalary(PersonnelRole.VESSEL_GUNNER, 1000);
        setRoleBaseSalary(PersonnelRole.VESSEL_CREW, 1000);
        setRoleBaseSalary(PersonnelRole.VESSEL_NAVIGATOR, 1000);
        setRoleBaseSalary(PersonnelRole.MEK_TECH, 800);
        setRoleBaseSalary(PersonnelRole.MECHANIC, 800);
        setRoleBaseSalary(PersonnelRole.AERO_TEK, 800);
        setRoleBaseSalary(PersonnelRole.BA_TECH, 800);
        setRoleBaseSalary(PersonnelRole.ASTECH, 400);
        setRoleBaseSalary(PersonnelRole.DOCTOR, 1500);
        setRoleBaseSalary(PersonnelRole.MEDIC, 400);
        setRoleBaseSalary(PersonnelRole.ADMINISTRATOR_COMMAND, 500);
        setRoleBaseSalary(PersonnelRole.ADMINISTRATOR_LOGISTICS, 500);
        setRoleBaseSalary(PersonnelRole.ADMINISTRATOR_TRANSPORT, 500);
        setRoleBaseSalary(PersonnelRole.ADMINISTRATOR_HR, 500);
        setRoleBaseSalary(PersonnelRole.DEPENDENT, 0);
        setRoleBaseSalary(PersonnelRole.NONE, 0);

        // Awards
        setAwardBonusStyle(AwardBonus.BOTH);
        setEnableAutoAwards(false);
        setIssuePosthumousAwards(false);
        setIssueBestAwardOnly(true);
        setIgnoreStandardSet(false);
        setAwardTierSize(5);
        setEnableContractAwards(true);
        setEnableFactionHunterAwards(true);
        setEnableInjuryAwards(true);
        setEnableIndividualKillAwards(true);
        setEnableFormationKillAwards(true);
        setEnableRankAwards(true);
        setEnableScenarioAwards(true);
        setEnableSkillAwards(true);
        setEnableTheatreOfWarAwards(true);
        setEnableTimeAwards(true);
        setEnableTrainingAwards(true);
        setEnableMiscAwards(true);
        setAwardSetFilterList("");
        // endregion Personnel Tab

        // region Life Paths Tab
        // Personnel Randomization
        setUseDylansRandomXP(false);
        setNonBinaryDiceSize(60);

        // Random Histories
        setRandomOriginOptions(new RandomOriginOptions(true));
        setUseRandomPersonalities(false);
        setUseRandomPersonalityReputation(true);
        setUseIntelligenceXpMultiplier(true);
        setUseSimulatedRelationships(false);

        // Family
        setFamilyDisplayLevel(FamilialRelationshipDisplayLevel.SPOUSE);

        // Anniversaries
        setAnnounceBirthdays(true);
        setAnnounceRecruitmentAnniversaries(true);
        setAnnounceOfficersOnly(true);
        setAnnounceChildBirthdays(true);

        // Marriage
        setUseManualMarriages(true);
        setUseClanPersonnelMarriages(false);
        setUsePrisonerMarriages(true);
        setCheckMutualAncestorsDepth(4);
        setNoInterestInMarriageDiceSize(10);
        setLogMarriageNameChanges(false);
        setMarriageSurnameWeights(new HashMap<>());
        getMarriageSurnameWeights().put(MergingSurnameStyle.NO_CHANGE, 100);
        getMarriageSurnameWeights().put(MergingSurnameStyle.YOURS, 55);
        getMarriageSurnameWeights().put(MergingSurnameStyle.SPOUSE, 55);
        getMarriageSurnameWeights().put(MergingSurnameStyle.SPACE_YOURS, 10);
        getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_SPACE_YOURS, 5);
        getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_YOURS, 30);
        getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_YOURS, 20);
        getMarriageSurnameWeights().put(MergingSurnameStyle.SPACE_SPOUSE, 10);
        getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_SPACE_SPOUSE, 5);
        getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_SPOUSE, 30);
        getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE, 20);
        getMarriageSurnameWeights().put(MergingSurnameStyle.MALE, 500);
        getMarriageSurnameWeights().put(MergingSurnameStyle.FEMALE, 160);
        setRandomMarriageMethod(RandomMarriageMethod.NONE);
        setUseRandomClanPersonnelMarriages(false);
        setUseRandomPrisonerMarriages(false);
        setRandomMarriageAgeRange(10);
        setRandomMarriageDiceSize(5000);
        setRandomSameSexMarriageDiceSize(14);
        setRandomNewDependentMarriage(20);

        // Divorce
        setUseManualDivorce(true);
        setUseClanPersonnelDivorce(true);
        setUsePrisonerDivorce(false);
        setDivorceSurnameWeights(new HashMap<>());
        getDivorceSurnameWeights().put(SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME, 10);
        getDivorceSurnameWeights().put(SplittingSurnameStyle.SPOUSE_CHANGES_SURNAME, 10);
        getDivorceSurnameWeights().put(SplittingSurnameStyle.BOTH_CHANGE_SURNAME, 30);
        getDivorceSurnameWeights().put(SplittingSurnameStyle.BOTH_KEEP_SURNAME, 50);
        setRandomDivorceMethod(RandomDivorceMethod.NONE);
        setUseRandomOppositeSexDivorce(true);
        setUseRandomSameSexDivorce(true);
        setUseRandomClanPersonnelDivorce(true);
        setUseRandomPrisonerDivorce(false);
        setRandomDivorceDiceSize(900);

        // Procreation
        setUseManualProcreation(true);
        setUseClanPersonnelProcreation(false);
        setUsePrisonerProcreation(true);
        setMultiplePregnancyOccurrences(50); // Hellin's Law is 89, but we make it more common, so it shows up more
        setBabySurnameStyle(BabySurnameStyle.MOTHERS);
        setAssignNonPrisonerBabiesFounderTag(false);
        setAssignChildrenOfFoundersFounderTag(false);
        setUseMaternityLeave(true);
        setDetermineFatherAtBirth(false);
        setDisplayTrueDueDate(false);
        setNoInterestInChildrenDiceSize(3);
        setLogProcreation(false);
        setRandomProcreationMethod(RandomProcreationMethod.NONE);
        setUseRelationshiplessRandomProcreation(false);
        setUseRandomClanPersonnelProcreation(false);
        setUseRandomPrisonerProcreation(true);
        setRandomProcreationRelationshipDiceSize(500);
        setRandomProcreationRelationshiplessDiceSize(2000);

        // Education
        setUseEducationModule(false);
        setCurriculumXpRate(3);
        setMaximumJumpCount(5);
        setUseReeducationCamps(true);
        setEnableLocalAcademies(true);
        setEnablePrestigiousAcademies(true);
        setEnableUnitEducation(true);
        setEnableOverrideRequirements(false);
        setEnableShowIneligibleAcademies(true);
        setEntranceExamBaseTargetNumber(14);
        setFacultyXpRate(1.00);
        setEnableBonuses(true);
        setAdultDropoutChance(1000);
        setChildrenDropoutChance(10000);
        setAllAges(false);
        setMilitaryAcademyAccidents(10000);

        // Death
        setEnabledRandomDeathAgeGroups(new HashMap<>());
        getEnabledRandomDeathAgeGroups().put(AgeGroup.ELDER, true);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.ADULT, true);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.TEENAGER, true);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.PRETEEN, false);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.CHILD, false);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.TODDLER, false);
        getEnabledRandomDeathAgeGroups().put(AgeGroup.BABY, false);
        setUseRandomDeathSuicideCause(false);
        setRandomDeathMultiplier(0);
        // endregion Life Paths Tab

        // region Turnover and Retention
        // Retirement
        setUseRandomRetirement(false);
        setTurnoverFrequency(TurnoverFrequency.MONTHLY);
        setTurnoverFixedTargetNumber(3);
        setAeroRecruitsHaveUnits(false);
        setUseContractCompletionRandomRetirement(true);
        setUseRandomFounderTurnover(true);
        setUseFounderRetirement(true);
        setUseSubContractSoldiers(false);
        setServiceContractDuration(36);
        setServiceContractModifier(3);
        setPayBonusDefault(false);
        setPayBonusDefaultThreshold(3);

        setUseCustomRetirementModifiers(true);
        setUseFatigueModifiers(true);
        setUseSkillModifiers(true);
        setUseAgeModifiers(true);
        setUseUnitRatingModifiers(true);
        setUseFactionModifiers(true);
        setUseMissionStatusModifiers(true);
        setUseHostileTerritoryModifiers(true);
        setUseFamilyModifiers(true);

        setUseLoyaltyModifiers(true);
        setUseHideLoyalty(false);

        setPayoutRateOfficer(3);
        setPayoutRateEnlisted(3);
        setPayoutRetirementMultiplier(12);
        setUsePayoutServiceBonus(true);
        setPayoutServiceBonusRate(10);

        setUseAdministrativeStrain(true);
        setAdministrativeCapacity(10);
        setMultiCrewStrainDivider(5);

        setUseManagementSkill(true);
        setUseCommanderLeadershipOnly(false);
        setManagementSkillPenalty(0);

        setUseFatigue(false);
        setFatigueRate(1);
        setUseInjuryFatigue(true);
        setFieldKitchenCapacity(150);
        setFieldKitchenIgnoreNonCombatants(true);
        setFatigueLeaveThreshold(13);
        // endregion Turnover and Retention

        // region Finances Tab
        payForParts = false;
        payForRepairs = false;
        payForUnits = false;
        payForSalaries = false;
        payForOverhead = false;
        payForMaintain = false;
        payForTransport = false;
        sellUnits = false;
        sellParts = false;
        payForRecruitment = false;
        useLoanLimits = false;
        usePercentageMaint = false;
        infantryDontCount = false;
        usePeacetimeCost = false;
        useExtendedPartsModifier = false;
        showPeacetimeCost = false;
        setFinancialYearDuration(FinancialYearDuration.ANNUAL);
        newFinancialYearFinancesToCSVExport = false;

        // Price Multipliers
        setCommonPartPriceMultiplier(1.0);
        setInnerSphereUnitPriceMultiplier(1.0);
        setInnerSpherePartPriceMultiplier(1.0);
        setClanUnitPriceMultiplier(1.0);
        setClanPartPriceMultiplier(1.0);
        setMixedTechUnitPriceMultiplier(1.0);
        setUsedPartPriceMultipliers(0.1, 0.2, 0.3, 0.5, 0.7, 0.9);
        setDamagedPartsValueMultiplier(0.33);
        setUnrepairablePartsValueMultiplier(0.1);
        setCancelledOrderRefundMultiplier(0.5);

        // Taxes
        setUseTaxes(false);
        setTaxesPercentage(30);

        // Shares
        setUseShareSystem(false);
        setSharesForAll(true);
        // endregion Finances Tab

        // region Mercenary Tab
        equipmentContractBase = false;
        equipmentContractPercent = 5.0;
        equipmentContractSaleValue = false;
        setDropShipContractPercent(1.0);
        setJumpShipContractPercent(0.0);
        setWarShipContractPercent(0.0);
        blcSaleValue = false;
        overageRepaymentInFinalPayment = false;
        // endregion Mercenary Tab

        // region Experience Tab
        xpCostMultiplier = 1.00;
        scenarioXP = 1;
        killXPAward = 0;
        killsForXP = 0;
        tasksXP = 1;
        nTasksXP = 25;
        successXP = 0;
        mistakeXP = 0;
        vocationalXP = 1;
        vocationalXPCheckFrequency = 1;
        vocationalXPTargetNumber = 7;
        contractNegotiationXP = 0;
        adminXP = 0;
        adminXPPeriod = 1;
        missionXpFail = 1;
        missionXpSuccess = 3;
        missionXpOutstandingSuccess = 5;
        edgeCost = 10;
        // endregion Experience Tab

        // region Skills Tab
        // endregion Skills Tab

        // region Special Abilities Tab
        // endregion Special Abilities Tab

        // region Skill Randomization Tab
        phenotypeProbabilities = new int[Phenotype.getExternalPhenotypes().size()];
        phenotypeProbabilities[Phenotype.MEKWARRIOR.ordinal()] = 95;
        phenotypeProbabilities[Phenotype.ELEMENTAL.ordinal()] = 100;
        phenotypeProbabilities[Phenotype.AEROSPACE.ordinal()] = 95;
        phenotypeProbabilities[Phenotype.VEHICLE.ordinal()] = 0;
        phenotypeProbabilities[Phenotype.PROTOMEK.ordinal()] = 95;
        phenotypeProbabilities[Phenotype.NAVAL.ordinal()] = 25;
        // endregion Skill Randomization Tab

        // region Rank System Tab
        // endregion Rank System Tab

        // region Name and Portrait Generation Tab
        useOriginFactionForNames = true;
        usePortraitForRole = new boolean[personnelRoles.length];
        Arrays.fill(usePortraitForRole, false);
        usePortraitForRole[PersonnelRole.MEKWARRIOR.ordinal()] = true;
        assignPortraitOnRoleChange = false;
        // endregion Name and Portrait Generation Tab

        // region Markets Tab
        // Personnel Market
        setPersonnelMarketName(PersonnelMarket.getTypeName(PersonnelMarket.TYPE_NONE));
        setPersonnelMarketReportRefresh(true);
        setPersonnelMarketRandomRemovalTargets(new HashMap<>());
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.NONE, 3);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.ULTRA_GREEN, 4);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.GREEN, 4);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.REGULAR, 6);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.VETERAN, 8);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.ELITE, 10);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.HEROIC, 11);
        getPersonnelMarketRandomRemovalTargets().put(SkillLevel.LEGENDARY, 11);
        setPersonnelMarketDylansWeight(0.3);
        setUsePersonnelHireHiringHallOnly(false);

        // Unit Market
        setUnitMarketMethod(UnitMarketMethod.NONE);
        setUnitMarketRegionalMekVariations(true);
        setUnitMarketSpecialUnitChance(30);
        setUnitMarketRarityModifier(0);
        setInstantUnitMarketDelivery(false);
        setUnitMarketReportRefresh(true);

        // Contract Market
        setContractMarketMethod(ContractMarketMethod.NONE);
        setContractSearchRadius(800);
        setVariableContractLength(true);
        setContractMarketReportRefresh(true);
        setContractMaxSalvagePercentage(100);
        setDropShipBonusPercentage(0);
        // endregion Markets Tab

        // region RATs Tab
        setUseStaticRATs(false);
        setRATs("Xotl", "Total Warfare");
        setIgnoreRATEra(false);
        // endregion RATs Tab

        // region Against the Bot Tab
        useAtB = false;
        useStratCon = false;
        setSkillLevel(SkillLevel.REGULAR);
        autoResolveMethod = AutoResolveMethod.PRINCESS;
        autoResolveVictoryChanceEnabled = false;
        autoResolveNumberOfScenarios = 100;
        autoResolveExperimentalPacarGuiEnabled = false;
        strategicViewMinimapTheme = "gbc green.theme";
        // Unit Administration
        useAero = false;
        useVehicles = true;
        clanVehicles = false;

        // Contract Operations
        mercSizeLimited = false;
        restrictPartsByMission = true;
        limitLanceWeight = true;
        limitLanceNumUnits = true;
        useStrategy = true;
        baseStrategyDeployment = 3;
        additionalStrategyDeployment = 1;
        atbBattleChance = new int[CombatRole.values().length - 1];
        atbBattleChance[CombatRole.MANEUVER.ordinal()] = 40;
        atbBattleChance[CombatRole.FRONTLINE.ordinal()] = 20;
        atbBattleChance[CombatRole.PATROL.ordinal()] = 60;
        atbBattleChance[CombatRole.TRAINING.ordinal()] = 10;
        generateChases = true;

        // Scenarios
        useGenericBattleValue = true;
        useVerboseBidding = false;
        doubleVehicles = false;
        setOpForLanceTypeMeks(1);
        setOpForLanceTypeMixed(2);
        setOpForLanceTypeVehicles(3);
        setOpForUsesVTOLs(true);
        setAllowOpForAeros(false);
        setOpForAeroChance(5);
        setAllowOpForLocalUnits(false);
        setOpForLocalUnitChance(5);
        setFixedMapChance(25);
        setSpaUpgradeIntensity(0);
        adjustPlayerVehicles = false;
        regionalMekVariations = false;
        attachedPlayerCamouflage = true;
        playerControlsAttachedUnits = false;
        useDropShips = false;
        useWeatherConditions = true;
        useLightConditions = true;
        usePlanetaryConditions = false;
        autoConfigMunitions = true;
        setScenarioModMax(3);
        setScenarioModChance(25);
        setScenarioModBV(50);
        // endregion Against the Bot Tab
    }
    // endregion Constructors

    // region General Tab
    /**
     * @return the method of unit rating to use
     */
    public UnitRatingMethod getUnitRatingMethod() {
        return unitRatingMethod;
    }

    /**
     * @param unitRatingMethod the method of unit rating to use
     */
    public void setUnitRatingMethod(final UnitRatingMethod unitRatingMethod) {
        this.unitRatingMethod = unitRatingMethod;
    }

    public int getManualUnitRatingModifier() {
        return manualUnitRatingModifier;
    }

    public void setManualUnitRatingModifier(final int manualUnitRatingModifier) {
        this.manualUnitRatingModifier = manualUnitRatingModifier;
    }
    // endregion General Tab

    // region Repair and Maintenance Tab
    // region Repair
    // endregion Repair

    // region Maintenance
    public boolean isCheckMaintenance() {
        return checkMaintenance;
    }

    public void setCheckMaintenance(final boolean checkMaintenance) {
        this.checkMaintenance = checkMaintenance;
    }

    public int getMaintenanceCycleDays() {
        return maintenanceCycleDays;
    }

    public void setMaintenanceCycleDays(final int maintenanceCycleDays) {
        this.maintenanceCycleDays = maintenanceCycleDays;
    }

    public int getMaintenanceBonus() {
        return maintenanceBonus;
    }

    public void setMaintenanceBonus(final int maintenanceBonus) {
        this.maintenanceBonus = maintenanceBonus;
    }

    public boolean isUseQualityMaintenance() {
        return useQualityMaintenance;
    }

    public void setUseQualityMaintenance(final boolean useQualityMaintenance) {
        this.useQualityMaintenance = useQualityMaintenance;
    }

    public boolean isReverseQualityNames() {
        return reverseQualityNames;
    }

    public void setReverseQualityNames(final boolean reverseQualityNames) {
        this.reverseQualityNames = reverseQualityNames;
    }

    public boolean isUseRandomUnitQualities() {
        return useRandomUnitQualities;
    }

    public void setUseRandomUnitQualities(final boolean useRandomUnitQualities) {
        this.useRandomUnitQualities = useRandomUnitQualities;
    }

    public boolean isUsePlanetaryModifiers() {
        return usePlanetaryModifiers;
    }

    public void setUsePlanetaryModifiers(final boolean usePlanetaryModifiers) {
        this.usePlanetaryModifiers = usePlanetaryModifiers;
    }

    public boolean isUseUnofficialMaintenance() {
        return useUnofficialMaintenance;
    }

    public void setUseUnofficialMaintenance(final boolean useUnofficialMaintenance) {
        this.useUnofficialMaintenance = useUnofficialMaintenance;
    }

    public boolean isLogMaintenance() {
        return logMaintenance;
    }

    public void setLogMaintenance(final boolean logMaintenance) {
        this.logMaintenance = logMaintenance;
    }

    /**
     * @return the default maintenance time in minutes
     */
    public int getDefaultMaintenanceTime() {
        return defaultMaintenanceTime;
    }

    /**
     * Sets the default maintenance time.
     *
     * @param defaultMaintenanceTime the default maintenance time multiplier
     */
    public void setDefaultMaintenanceTime(final int defaultMaintenanceTime) {
        this.defaultMaintenanceTime = defaultMaintenanceTime;
    }
    // endregion Maintenance

    // region Mass Repair/ Mass Salvage
    public boolean isMRMSUseRepair() {
        return mrmsUseRepair;
    }

    public void setMRMSUseRepair(final boolean mrmsUseRepair) {
        this.mrmsUseRepair = mrmsUseRepair;
    }

    public boolean isMRMSUseSalvage() {
        return mrmsUseSalvage;
    }

    public void setMRMSUseSalvage(final boolean mrmsUseSalvage) {
        this.mrmsUseSalvage = mrmsUseSalvage;
    }

    public boolean isMRMSUseExtraTime() {
        return mrmsUseExtraTime;
    }

    public void setMRMSUseExtraTime(final boolean mrmsUseExtraTime) {
        this.mrmsUseExtraTime = mrmsUseExtraTime;
    }

    public boolean isMRMSUseRushJob() {
        return mrmsUseRushJob;
    }

    public void setMRMSUseRushJob(final boolean mrmsUseRushJob) {
        this.mrmsUseRushJob = mrmsUseRushJob;
    }

    public boolean isMRMSAllowCarryover() {
        return mrmsAllowCarryover;
    }

    public void setMRMSAllowCarryover(final boolean mrmsAllowCarryover) {
        this.mrmsAllowCarryover = mrmsAllowCarryover;
    }

    public boolean isMRMSOptimizeToCompleteToday() {
        return mrmsOptimizeToCompleteToday;
    }

    public void setMRMSOptimizeToCompleteToday(final boolean mrmsOptimizeToCompleteToday) {
        this.mrmsOptimizeToCompleteToday = mrmsOptimizeToCompleteToday;
    }

    public boolean isMRMSScrapImpossible() {
        return mrmsScrapImpossible;
    }

    public void setMRMSScrapImpossible(final boolean mrmsScrapImpossible) {
        this.mrmsScrapImpossible = mrmsScrapImpossible;
    }

    public boolean isMRMSUseAssignedTechsFirst() {
        return mrmsUseAssignedTechsFirst;
    }

    public void setMRMSUseAssignedTechsFirst(final boolean mrmsUseAssignedTechsFirst) {
        this.mrmsUseAssignedTechsFirst = mrmsUseAssignedTechsFirst;
    }

    public boolean isMRMSReplacePod() {
        return mrmsReplacePod;
    }

    public void setMRMSReplacePod(final boolean mrmsReplacePod) {
        this.mrmsReplacePod = mrmsReplacePod;
    }

    public List<MRMSOption> getMRMSOptions() {
        return mrmsOptions;
    }

    public void setMRMSOptions(final List<MRMSOption> mrmsOptions) {
        this.mrmsOptions = mrmsOptions;
    }

    public void addMRMSOption(final MRMSOption mrmsOption) {
        if (mrmsOption.getType().isUnknownLocation()) {
            return;
        }

        getMRMSOptions().removeIf(option -> option.getType() == mrmsOption.getType());
        getMRMSOptions().add(mrmsOption);
    }
    // endregion Mass Repair/ Mass Salvage
    // endregion Repair and Maintenance Tab

    // region Supplies and Acquisitions Tab
    // endregion Supplies and Acquisitions Tab

    // region Personnel Tab
    // region General Personnel
    public boolean isUseTactics() {
        return useTactics;
    }

    public void setUseTactics(final boolean useTactics) {
        this.useTactics = useTactics;
    }

    public boolean isUseInitiativeBonus() {
        return useInitiativeBonus;
    }

    public void setUseInitiativeBonus(final boolean useInitiativeBonus) {
        this.useInitiativeBonus = useInitiativeBonus;
    }

    public boolean isUseToughness() {
        return useToughness;
    }

    public void setUseToughness(final boolean useToughness) {
        this.useToughness = useToughness;
    }

    public boolean isUseRandomToughness() {
        return useRandomToughness;
    }

    public void setUseRandomToughness(final boolean useRandomToughness) {
        this.useRandomToughness = useRandomToughness;
    }

    public boolean isUseArtillery() {
        return useArtillery;
    }

    public void setUseArtillery(final boolean useArtillery) {
        this.useArtillery = useArtillery;
    }

    public boolean isUseAbilities() {
        return useAbilities;
    }

    public void setUseAbilities(final boolean useAbilities) {
        this.useAbilities = useAbilities;
    }

    public boolean isUseEdge() {
        return useEdge;
    }

    public void setUseEdge(final boolean useEdge) {
        this.useEdge = useEdge;
    }

    public boolean isUseSupportEdge() {
        return useSupportEdge;
    }

    public void setUseSupportEdge(final boolean useSupportEdge) {
        this.useSupportEdge = useSupportEdge;
    }

    public boolean isUseImplants() {
        return useImplants;
    }

    public void setUseImplants(final boolean useImplants) {
        this.useImplants = useImplants;
    }

    public boolean isAlternativeQualityAveraging() {
        return alternativeQualityAveraging;
    }

    public void setAlternativeQualityAveraging(final boolean alternativeQualityAveraging) {
        this.alternativeQualityAveraging = alternativeQualityAveraging;
    }

    public boolean isUseTransfers() {
        return useTransfers;
    }

    public void setUseTransfers(final boolean useTransfers) {
        this.useTransfers = useTransfers;
    }

    public boolean isUseExtendedTOEForceName() {
        return useExtendedTOEForceName;
    }

    public void setUseExtendedTOEForceName(final boolean useExtendedTOEForceName) {
        this.useExtendedTOEForceName = useExtendedTOEForceName;
    }

    public boolean isPersonnelLogSkillGain() {
        return personnelLogSkillGain;
    }

    public void setPersonnelLogSkillGain(final boolean personnelLogSkillGain) {
        this.personnelLogSkillGain = personnelLogSkillGain;
    }

    public boolean isPersonnelLogAbilityGain() {
        return personnelLogAbilityGain;
    }

    public void setPersonnelLogAbilityGain(final boolean personnelLogAbilityGain) {
        this.personnelLogAbilityGain = personnelLogAbilityGain;
    }

    public boolean isPersonnelLogEdgeGain() {
        return personnelLogEdgeGain;
    }

    public void setPersonnelLogEdgeGain(final boolean personnelLogEdgeGain) {
        this.personnelLogEdgeGain = personnelLogEdgeGain;
    }

    public boolean isDisplayPersonnelLog() {
        return displayPersonnelLog;
    }

    public void setDisplayPersonnelLog(final boolean displayPersonnelLog) {
        this.displayPersonnelLog = displayPersonnelLog;
    }

    public boolean isDisplayScenarioLog() {
        return displayScenarioLog;
    }

    public void setDisplayScenarioLog(final boolean displayScenarioLog) {
        this.displayScenarioLog = displayScenarioLog;
    }

    public boolean isDisplayKillRecord() {
        return displayKillRecord;
    }

    public void setDisplayKillRecord(final boolean displayKillRecord) {
        this.displayKillRecord = displayKillRecord;
    }

    public boolean isUseFatigue() {
        return useFatigue;
    }

    public void setUseFatigue(final boolean useFatigue) {
        this.useFatigue = useFatigue;
    }

    public Integer getFatigueRate() {
        return fatigueRate;
    }

    public void setFatigueRate(final Integer fatigueRate) {
        this.fatigueRate = fatigueRate;
    }

    public boolean isUseInjuryFatigue() {
        return useInjuryFatigue;
    }

    public void setUseInjuryFatigue(final boolean useInjuryFatigue) {
        this.useInjuryFatigue = useInjuryFatigue;
    }

    public Integer getFieldKitchenCapacity() {
        return fieldKitchenCapacity;
    }

    public void setFieldKitchenCapacity(final Integer fieldKitchenCapacity) {
        this.fieldKitchenCapacity = fieldKitchenCapacity;
    }

    public boolean isUseFieldKitchenIgnoreNonCombatants() {
        return fieldKitchenIgnoreNonCombatants;
    }

    public void setFieldKitchenIgnoreNonCombatants(final boolean fieldKitchenIgnoreNonCombatants) {
        this.fieldKitchenIgnoreNonCombatants = fieldKitchenIgnoreNonCombatants;
    }

    public Integer getFatigueLeaveThreshold() {
        return fatigueLeaveThreshold;
    }

    public void setFatigueLeaveThreshold(final Integer fatigueLeaveThreshold) {
        this.fatigueLeaveThreshold = fatigueLeaveThreshold;
    }

    // endregion General Personnel

    // region Expanded Personnel Information
    /**
     * @return whether to use time in service
     */
    public boolean isUseTimeInService() {
        return useTimeInService;
    }

    /**
     * @param useTimeInService the new value for whether to use time in service or
     *                         not
     */
    public void setUseTimeInService(final boolean useTimeInService) {
        this.useTimeInService = useTimeInService;
    }

    /**
     * @return the format to display the Time in Service in
     */
    public TimeInDisplayFormat getTimeInServiceDisplayFormat() {
        return timeInServiceDisplayFormat;
    }

    /**
     * @param timeInServiceDisplayFormat the new display format for Time in Service
     */
    public void setTimeInServiceDisplayFormat(final TimeInDisplayFormat timeInServiceDisplayFormat) {
        this.timeInServiceDisplayFormat = timeInServiceDisplayFormat;
    }

    /**
     * @return whether to use time in rank
     */
    public boolean isUseTimeInRank() {
        return useTimeInRank;
    }

    /**
     * @param useTimeInRank the new value for whether to use time in rank
     */
    public void setUseTimeInRank(final boolean useTimeInRank) {
        this.useTimeInRank = useTimeInRank;
    }

    /**
     * @return the format to display the Time in Rank in
     */
    public TimeInDisplayFormat getTimeInRankDisplayFormat() {
        return timeInRankDisplayFormat;
    }

    /**
     * @param timeInRankDisplayFormat the new display format for Time in Rank
     */
    public void setTimeInRankDisplayFormat(final TimeInDisplayFormat timeInRankDisplayFormat) {
        this.timeInRankDisplayFormat = timeInRankDisplayFormat;
    }

    /**
     * @return whether to track the total earnings of personnel
     */
    public boolean isTrackTotalEarnings() {
        return trackTotalEarnings;
    }

    /**
     * @param trackTotalEarnings the new value for whether to track total earnings for personnel
     */
    public void setTrackTotalEarnings(final boolean trackTotalEarnings) {
        this.trackTotalEarnings = trackTotalEarnings;
    }

    /**
     * @return whether to track the total experience earnings of personnel
     */
    public boolean isTrackTotalXPEarnings() {
        return trackTotalXPEarnings;
    }

    /**
     * @param trackTotalXPEarnings the new value for whether to track total experience
     *                             earnings for personnel
     */
    public void setTrackTotalXPEarnings(final boolean trackTotalXPEarnings) {
        this.trackTotalXPEarnings = trackTotalXPEarnings;
    }

    /**
     * Gets a value indicating whether to show a person's origin faction when displaying
     * their details.
     */
    public boolean isShowOriginFaction() {
        return showOriginFaction;
    }

    /**
     * Sets a value indicating whether to show a person's origin faction when displaying
     * their details.
     */
    public void setShowOriginFaction(final boolean showOriginFaction) {
        this.showOriginFaction = showOriginFaction;
    }

    public boolean isAdminsHaveNegotiation() {
        return adminsHaveNegotiation;
    }

    public void setAdminsHaveNegotiation(final boolean useAdminsHaveNegotiation) {
        this.adminsHaveNegotiation = useAdminsHaveNegotiation;
    }

    public boolean isAdminsHaveScrounge() {
        return adminsHaveScrounge;
    }

    public void setAdminsHaveScrounge(final boolean useAdminsHaveScrounge) {
        this.adminsHaveScrounge = useAdminsHaveScrounge;
    }

    public boolean isAdminExperienceLevelIncludeNegotiation() {
        return adminExperienceLevelIncludeNegotiation;
    }

    public void setAdminExperienceLevelIncludeNegotiation(final boolean useAdminExperienceLevelIncludeNegotiation) {
        this.adminExperienceLevelIncludeNegotiation = useAdminExperienceLevelIncludeNegotiation;
    }

    public boolean isAdminExperienceLevelIncludeScrounge() {
        return adminExperienceLevelIncludeScrounge;
    }

    public void setAdminExperienceLevelIncludeScrounge(final boolean useAdminExperienceLevelIncludeScrounge) {
        this.adminExperienceLevelIncludeScrounge = useAdminExperienceLevelIncludeScrounge;
    }
    // endregion Expanded Personnel Information

    // region Medical
    public boolean isUseAdvancedMedical() {
        return useAdvancedMedical;
    }

    public void setUseAdvancedMedical(final boolean useAdvancedMedical) {
        this.useAdvancedMedical = useAdvancedMedical;
    }

    public int getHealingWaitingPeriod() {
        return healWaitingPeriod;
    }

    public void setHealingWaitingPeriod(final int healWaitingPeriod) {
        this.healWaitingPeriod = healWaitingPeriod;
    }

    public int getNaturalHealingWaitingPeriod() {
        return naturalHealingWaitingPeriod;
    }

    public void setNaturalHealingWaitingPeriod(final int naturalHealingWaitingPeriod) {
        this.naturalHealingWaitingPeriod = naturalHealingWaitingPeriod;
    }

    public int getMinimumHitsForVehicles() {
        return minimumHitsForVehicles;
    }

    public void setMinimumHitsForVehicles(final int minimumHitsForVehicles) {
        this.minimumHitsForVehicles = minimumHitsForVehicles;
    }

    public boolean isUseRandomHitsForVehicles() {
        return useRandomHitsForVehicles;
    }

    public void setUseRandomHitsForVehicles(final boolean useRandomHitsForVehicles) {
        this.useRandomHitsForVehicles = useRandomHitsForVehicles;
    }

    public boolean isTougherHealing() {
        return tougherHealing;
    }

    public void setTougherHealing(final boolean tougherHealing) {
        this.tougherHealing = tougherHealing;
    }

    public int getMaximumPatients() {
        return maximumPatients;
    }

    public void setMaximumPatients(final int maximumPatients) {
        this.maximumPatients = maximumPatients;
    }
    // endregion Medical

    // region Prisoners
    public PrisonerCaptureStyle getPrisonerCaptureStyle() {
        return prisonerCaptureStyle;
    }

    public void setPrisonerCaptureStyle(final PrisonerCaptureStyle prisonerCaptureStyle) {
        this.prisonerCaptureStyle = prisonerCaptureStyle;
    }

    public PrisonerStatus getDefaultPrisonerStatus() {
        return defaultPrisonerStatus;
    }

    public void setDefaultPrisonerStatus(final PrisonerStatus defaultPrisonerStatus) {
        this.defaultPrisonerStatus = defaultPrisonerStatus;
    }

    public boolean isPrisonerBabyStatus() {
        return prisonerBabyStatus;
    }

    public void setPrisonerBabyStatus(final boolean prisonerBabyStatus) {
        this.prisonerBabyStatus = prisonerBabyStatus;
    }

    public boolean isUseAtBPrisonerDefection() {
        return useAtBPrisonerDefection;
    }

    public void setUseAtBPrisonerDefection(final boolean useAtBPrisonerDefection) {
        this.useAtBPrisonerDefection = useAtBPrisonerDefection;
    }

    public boolean isUseAtBPrisonerRansom() {
        return useAtBPrisonerRansom;
    }

    public void setUseAtBPrisonerRansom(final boolean useAtBPrisonerRansom) {
        this.useAtBPrisonerRansom = useAtBPrisonerRansom;
    }
    // endregion Prisoners

    // region Personnel Randomization
    public boolean isUseDylansRandomXP() {
        return useDylansRandomXP;
    }

    public void setUseDylansRandomXP(final boolean useDylansRandomXP) {
        this.useDylansRandomXP = useDylansRandomXP;
    }
    public int getNonBinaryDiceSize() {
        return nonBinaryDiceSize;
    }

    public void setNonBinaryDiceSize(final int nonBinaryDiceSize) {
        this.nonBinaryDiceSize = nonBinaryDiceSize;
    }
    //endregion Personnel Randomization

    //region Random Histories
    public RandomOriginOptions getRandomOriginOptions() {
        return randomOriginOptions;
    }

    public void setRandomOriginOptions(final RandomOriginOptions randomOriginOptions) {
        this.randomOriginOptions = randomOriginOptions;
    }

    public boolean isUseRandomPersonalities() {
        return useRandomPersonalities;
    }

    public void setUseRandomPersonalities(final boolean useRandomPersonalities) {
        this.useRandomPersonalities = useRandomPersonalities;
    }

    public boolean isUseRandomPersonalityReputation() {
        return useRandomPersonalityReputation;
    }

    public void setUseRandomPersonalityReputation(final boolean useRandomPersonalityReputation) {
        this.useRandomPersonalityReputation = useRandomPersonalityReputation;
    }

    public boolean isUseIntelligenceXpMultiplier() {
        return useIntelligenceXpMultiplier;
    }

    public void setUseIntelligenceXpMultiplier(final boolean useIntelligenceXpMultiplier) {
        this.useIntelligenceXpMultiplier = useIntelligenceXpMultiplier;
    }

    public boolean isUseSimulatedRelationships() {
        return useSimulatedRelationships;
    }

    public void setUseSimulatedRelationships(final boolean useSimulatedRelationships) {
        this.useSimulatedRelationships = useSimulatedRelationships;
    }
    //endregion Random Histories

    // region Retirement
    public boolean isUseRandomRetirement() {
        return useRandomRetirement;
    }

    public void setUseRandomRetirement(final boolean useRandomRetirement) {
        this.useRandomRetirement = useRandomRetirement;
    }

    public TurnoverFrequency getTurnoverFrequency() {
        return turnoverFrequency;
    }

    public void setTurnoverFrequency(final TurnoverFrequency turnoverFrequency) {
        this.turnoverFrequency = turnoverFrequency;
    }

    public boolean isUseContractCompletionRandomRetirement() {
        return useContractCompletionRandomRetirement;
    }

    public void setUseContractCompletionRandomRetirement(final boolean useContractCompletionRandomRetirement) {
        this.useContractCompletionRandomRetirement = useContractCompletionRandomRetirement;
    }

    public boolean isUseCustomRetirementModifiers() {
        return useCustomRetirementModifiers;
    }

    public void setUseCustomRetirementModifiers(final boolean useCustomRetirementModifiers) {
        this.useCustomRetirementModifiers = useCustomRetirementModifiers;
    }

    public boolean isUseFatigueModifiers() {
        return useFatigueModifiers;
    }

    public void setUseFatigueModifiers(final boolean useFatigueModifiers) {
        this.useFatigueModifiers = useFatigueModifiers;
    }

    public boolean isUseLoyaltyModifiers() {
        return useLoyaltyModifiers;
    }

    public void setUseLoyaltyModifiers(final boolean useLoyaltyModifiers) {
        this.useLoyaltyModifiers = useLoyaltyModifiers;
    }

    public boolean isUseHideLoyalty() {
        return useHideLoyalty;
    }

    public void setUseHideLoyalty(final boolean useHideLoyalty) {
        this.useHideLoyalty = useHideLoyalty;
    }

    public boolean isUseRandomFounderTurnover() {
        return useRandomFounderTurnover;
    }

    public void setUseRandomFounderTurnover(final boolean useRandomFounderTurnover) {
        this.useRandomFounderTurnover = useRandomFounderTurnover;
    }

    public boolean isUseFounderRetirement() {
        return useFounderRetirement;
    }

    public void setUseFounderRetirement(final boolean useFounderRetirement) {
        this.useFounderRetirement = useFounderRetirement;
    }

    public boolean isUseSubContractSoldiers() {
        return useSubContractSoldiers;
    }

    public void setUseSubContractSoldiers(final boolean useSubContractSoldiers) {
        this.useSubContractSoldiers = useSubContractSoldiers;
    }

    public Integer getTurnoverFixedTargetNumber() {
        return turnoverFixedTargetNumber;
    }

    public void setTurnoverFixedTargetNumber(final Integer turnoverFixedTargetNumber) {
        this.turnoverFixedTargetNumber = turnoverFixedTargetNumber;
    }

    public Integer getPayoutRateOfficer() {
        return payoutRateOfficer;
    }

    public void setPayoutRateOfficer(final Integer payoutRateOfficer) {
        this.payoutRateOfficer = payoutRateOfficer;
    }

    public Integer getPayoutRateEnlisted() {
        return payoutRateEnlisted;
    }

    public void setPayoutRateEnlisted(final Integer payoutRateEnlisted) {
        this.payoutRateEnlisted = payoutRateEnlisted;
    }

    public Integer getPayoutRetirementMultiplier() {
        return payoutRetirementMultiplier;
    }

    public void setPayoutRetirementMultiplier(final Integer payoutRetirementMultiplier) {
        this.payoutRetirementMultiplier = payoutRetirementMultiplier;
    }

    public boolean isUsePayoutServiceBonus() {
        return usePayoutServiceBonus;
    }

    public void setUsePayoutServiceBonus(final boolean usePayoutServiceBonus) {
        this.usePayoutServiceBonus = usePayoutServiceBonus;
    }

    public Integer getPayoutServiceBonusRate() {
        return payoutServiceBonusRate;
    }

    public void setPayoutServiceBonusRate(final Integer payoutServiceBonusRate) {
        this.payoutServiceBonusRate = payoutServiceBonusRate;
    }

    public boolean isUseSkillModifiers() {
        return useSkillModifiers;
    }

    public void setUseSkillModifiers(final boolean useSkillModifiers) {
        this.useSkillModifiers = useSkillModifiers;
    }

    public boolean isUseAgeModifiers() {
        return useAgeModifiers;
    }

    public void setUseAgeModifiers(final boolean useAgeModifiers) {
        this.useAgeModifiers = useAgeModifiers;
    }

    public boolean isUseUnitRatingModifiers() {
        return useUnitRatingModifiers;
    }

    public void setUseUnitRatingModifiers(final boolean useUnitRatingModifiers) {
        this.useUnitRatingModifiers = useUnitRatingModifiers;
    }

    public boolean isUseFactionModifiers() {
        return useFactionModifiers;
    }

    public void setUseFactionModifiers(final boolean useFactionModifiers) {
        this.useFactionModifiers = useFactionModifiers;
    }

    public boolean isUseMissionStatusModifiers() {
        return useMissionStatusModifiers;
    }

    public void setUseMissionStatusModifiers(final boolean useMissionStatusModifiers) {
        this.useMissionStatusModifiers = useMissionStatusModifiers;
    }

    public boolean isUseHostileTerritoryModifiers() {
        return useHostileTerritoryModifiers;
    }

    public void setUseHostileTerritoryModifiers(final boolean useHostileTerritoryModifiers) {
        this.useHostileTerritoryModifiers = useHostileTerritoryModifiers;
    }

    public boolean isUseFamilyModifiers() {
        return useFamilyModifiers;
    }

    public void setUseFamilyModifiers(final boolean useFamilyModifiers) {
        this.useFamilyModifiers = useFamilyModifiers;
    }

    public boolean isUseAdministrativeStrain() {
        return useAdministrativeStrain;
    }

    public void setUseAdministrativeStrain(final boolean useAdministrativeStrain) {
        this.useAdministrativeStrain = useAdministrativeStrain;
    }

    public Integer getAdministrativeCapacity() {
        return administrativeCapacity;
    }

    public void setAdministrativeCapacity(final Integer administrativeCapacity) {
        this.administrativeCapacity = administrativeCapacity;
    }

    public Integer getMultiCrewStrainDivider() {
        return multiCrewStrainDivider;
    }

    public void setMultiCrewStrainDivider(final Integer multiCrewStrainDivider) {
        this.multiCrewStrainDivider = multiCrewStrainDivider;
    }

    public boolean isUseManagementSkill() {
        return useManagementSkill;
    }

    public void setUseManagementSkill(final boolean useManagementSkill) {
        this.useManagementSkill = useManagementSkill;
    }

    public boolean isUseCommanderLeadershipOnly() {
        return useCommanderLeadershipOnly;
    }

    public void setUseCommanderLeadershipOnly(final boolean useCommanderLeadershipOnly) {
        this.useCommanderLeadershipOnly = useCommanderLeadershipOnly;
    }

    public Integer getManagementSkillPenalty() {
        return managementSkillPenalty;
    }

    public void setManagementSkillPenalty(final Integer managementSkillPenalty) {
        this.managementSkillPenalty = managementSkillPenalty;
    }

    public Integer getServiceContractDuration() {
        return serviceContractDuration;
    }

    public void setServiceContractDuration(final Integer serviceContractDuration) {
        this.serviceContractDuration = serviceContractDuration;
    }

    public Integer getServiceContractModifier() {
        return serviceContractModifier;
    }

    public void setServiceContractModifier(final Integer serviceContractModifier) {
        this.serviceContractModifier = serviceContractModifier;
    }

    public boolean isPayBonusDefault() {
        return payBonusDefault;
    }

    public void setPayBonusDefault(final boolean payBonusDefault) {
        this.payBonusDefault = payBonusDefault;
    }

    public int getPayBonusDefaultThreshold() {
        return payBonusDefaultThreshold;
    }

    public void setPayBonusDefaultThreshold(final int payBonusDefaultThreshold) {
        this.payBonusDefaultThreshold = payBonusDefaultThreshold;
    }
    // endregion Retirement

    // region Family
    /**
     * @return the level of familial relation to display
     */
    public FamilialRelationshipDisplayLevel getFamilyDisplayLevel() {
        return familyDisplayLevel;
    }

    /**
     * @param familyDisplayLevel the level of familial relation to display
     */
    public void setFamilyDisplayLevel(final FamilialRelationshipDisplayLevel familyDisplayLevel) {
        this.familyDisplayLevel = familyDisplayLevel;
    }
    // endregion Family

    // region anniversaries
    public boolean isAnnounceBirthdays() {
        return announceBirthdays;
    }

    public void setAnnounceBirthdays(final boolean announceBirthdays) {
        this.announceBirthdays = announceBirthdays;
    }

    /**
     * Checks if recruitment anniversaries should be announced.
     *
     * @return {@code true} if recruitment anniversaries should be announced, {@code false} otherwise.
     */
    public boolean isAnnounceRecruitmentAnniversaries() {
        return announceRecruitmentAnniversaries;
    }

    /**
     * Set whether to announce recruitment anniversaries.
     *
     * @param announceRecruitmentAnniversaries {@code true} to announce recruitment anniversaries,
     * {@code false} otherwise
     */
    public void setAnnounceRecruitmentAnniversaries(final boolean announceRecruitmentAnniversaries) {
        this.announceRecruitmentAnniversaries = announceRecruitmentAnniversaries;
    }

    public boolean isAnnounceOfficersOnly() {
        return announceOfficersOnly;
    }

    public void setAnnounceOfficersOnly(final boolean announceOfficersOnly) {
        this.announceOfficersOnly = announceOfficersOnly;
    }

    public boolean isAnnounceChildBirthdays() {
        return announceChildBirthdays;
    }

    public void setAnnounceChildBirthdays(final boolean announceChildBirthdays) {
        this.announceChildBirthdays = announceChildBirthdays;
    }
    // endregion anniversaries

    //region Dependents
    public boolean isUseRandomDependentAddition() {
        return useRandomDependentAddition;
    }

    public void setUseRandomDependentAddition(final boolean useRandomDependentAddition) {
        this.useRandomDependentAddition = useRandomDependentAddition;
    }

    public boolean isUseRandomDependentRemoval() {
        return useRandomDependentRemoval;
    }

    public void setUseRandomDependentRemoval(final boolean useRandomDependentRemoval) {
        this.useRandomDependentRemoval = useRandomDependentRemoval;
    }
    // endregion Dependent

    // region Personnel Removal
    public boolean isUsePersonnelRemoval() {
        return usePersonnelRemoval;
    }

    public void setUsePersonnelRemoval(final boolean usePersonnelRemoval) {
        this.usePersonnelRemoval = usePersonnelRemoval;
    }

    public boolean isUseRemovalExemptCemetery() {
        return useRemovalExemptCemetery;
    }

    public void setUseRemovalExemptCemetery(final boolean useRemovalExemptCemetery) {
        this.useRemovalExemptCemetery = useRemovalExemptCemetery;
    }

    public boolean isUseRemovalExemptRetirees() {
        return useRemovalExemptRetirees;
    }

    public void setUseRemovalExemptRetirees(final boolean useRemovalExemptRetirees) {
        this.useRemovalExemptRetirees = useRemovalExemptRetirees;
    }
    // endregion Personnel Removal

    // region Salary
    public boolean isDisableSecondaryRoleSalary() {
        return disableSecondaryRoleSalary;
    }

    public void setDisableSecondaryRoleSalary(final boolean disableSecondaryRoleSalary) {
        this.disableSecondaryRoleSalary = disableSecondaryRoleSalary;
    }

    public double getSalaryAntiMekMultiplier() {
        return salaryAntiMekMultiplier;
    }

    public void setSalaryAntiMekMultiplier(final double salaryAntiMekMultiplier) {
        this.salaryAntiMekMultiplier = salaryAntiMekMultiplier;
    }

    public double getSalarySpecialistInfantryMultiplier() {
        return salarySpecialistInfantryMultiplier;
    }

    public void setSalarySpecialistInfantryMultiplier(final double salarySpecialistInfantryMultiplier) {
        this.salarySpecialistInfantryMultiplier = salarySpecialistInfantryMultiplier;
    }

    public Map<SkillLevel, Double> getSalaryXPMultipliers() {
        return salaryXPMultipliers;
    }

    public void setSalaryXPMultipliers(final Map<SkillLevel, Double> salaryXPMultipliers) {
        this.salaryXPMultipliers = salaryXPMultipliers;
    }

    public Money[] getRoleBaseSalaries() {
        return roleBaseSalaries;
    }

    public void setRoleBaseSalaries(final Money... roleBaseSalaries) {
        this.roleBaseSalaries = roleBaseSalaries;
    }

    public void setRoleBaseSalary(final PersonnelRole role, final double baseSalary) {
        setRoleBaseSalary(role, Money.of(baseSalary));
    }

    public void setRoleBaseSalary(final PersonnelRole role, final Money baseSalary) {
        getRoleBaseSalaries()[role.ordinal()] = baseSalary;
    }
    // endregion Salary

    // region Marriage
    /**
     * @return whether to use manual marriages
     */
    public boolean isUseManualMarriages() {
        return useManualMarriages;
    }

    /**
     * @param useManualMarriages whether to use manual marriages
     */
    public void setUseManualMarriages(final boolean useManualMarriages) {
        this.useManualMarriages = useManualMarriages;
    }

    public boolean isUseClanPersonnelMarriages() {
        return useClanPersonnelMarriages;
    }

    public void setUseClanPersonnelMarriages(final boolean useClanPersonnelMarriages) {
        this.useClanPersonnelMarriages = useClanPersonnelMarriages;
    }

    public boolean isUsePrisonerMarriages() {
        return usePrisonerMarriages;
    }

    public void setUsePrisonerMarriages(final boolean usePrisonerMarriages) {
        this.usePrisonerMarriages = usePrisonerMarriages;
    }

    /**
     * This gets the number of recursions to use when checking mutual ancestors
     * between two personnel
     *
     * @return the number of recursions to use
     */
    public int getCheckMutualAncestorsDepth() {
        return checkMutualAncestorsDepth;
    }

    /**
     * This sets the number of recursions to use when checking mutual ancestors
     * between two personnel
     *
     * @param checkMutualAncestorsDepth the number of recursions
     */
    public void setCheckMutualAncestorsDepth(final int checkMutualAncestorsDepth) {
        this.checkMutualAncestorsDepth = checkMutualAncestorsDepth;
    }

    public int getNoInterestInMarriageDiceSize() {
        return noInterestInMarriageDiceSize;
    }

    public void setNoInterestInMarriageDiceSize(final int noInterestInMarriageDiceSize) {
        this.noInterestInMarriageDiceSize = noInterestInMarriageDiceSize;
    }

    /**
     * @return whether to log a name change in a marriage
     */
    public boolean isLogMarriageNameChanges() {
        return logMarriageNameChanges;
    }

    /**
     * @param logMarriageNameChanges whether to log marriage name changes or not
     */
    public void setLogMarriageNameChanges(final boolean logMarriageNameChanges) {
        this.logMarriageNameChanges = logMarriageNameChanges;
    }

    /**
     * @return the weight map of potential surname changes for weighted marriage
     *         surname generation
     */
    public Map<MergingSurnameStyle, Integer> getMarriageSurnameWeights() {
        return marriageSurnameWeights;
    }

    /**
     * @param marriageSurnameWeights the new marriage surname weight map
     */
    public void setMarriageSurnameWeights(final Map<MergingSurnameStyle, Integer> marriageSurnameWeights) {
        this.marriageSurnameWeights = marriageSurnameWeights;
    }

    public RandomMarriageMethod getRandomMarriageMethod() {
        return randomMarriageMethod;
    }

    public void setRandomMarriageMethod(final RandomMarriageMethod randomMarriageMethod) {
        this.randomMarriageMethod = randomMarriageMethod;
    }

    /**
     * @return whether to use random same-sex marriages
     */
    @Deprecated
    public boolean isUseRandomSameSexMarriages() {
        return useRandomSameSexMarriages;
    }

    /**
     * @param useRandomSameSexMarriages whether to use random same-sex marriages
     */
    @Deprecated
    public void setUseRandomSameSexMarriages(final boolean useRandomSameSexMarriages) {
        this.useRandomSameSexMarriages = useRandomSameSexMarriages;
    }

    public boolean isUseRandomClanPersonnelMarriages() {
        return useRandomClanPersonnelMarriages;
    }

    public void setUseRandomClanPersonnelMarriages(final boolean useRandomClanPersonnelMarriages) {
        this.useRandomClanPersonnelMarriages = useRandomClanPersonnelMarriages;
    }

    public boolean isUseRandomPrisonerMarriages() {
        return useRandomPrisonerMarriages;
    }

    public void setUseRandomPrisonerMarriages(final boolean useRandomPrisonerMarriages) {
        this.useRandomPrisonerMarriages = useRandomPrisonerMarriages;
    }

    /**
     * A random marriage can only happen between two people whose ages differ (+/-)
     * by the returned value
     *
     * @return the age range ages can differ (+/-)
     */
    public int getRandomMarriageAgeRange() {
        return randomMarriageAgeRange;
    }

    /**
     * A random marriage can only happen between two people whose ages differ (+/-)
     * by this value
     *
     * @param randomMarriageAgeRange the new maximum age range
     */
    public void setRandomMarriageAgeRange(final int randomMarriageAgeRange) {
        this.randomMarriageAgeRange = randomMarriageAgeRange;
    }

    /**
     * @return the number of sides on the die used to determine random marriage
     */
    public int getRandomMarriageDiceSize() {
        return randomMarriageDiceSize;
    }

    /**
     * Sets the size of the random marriage die.
     *
     * @param randomMarriageDiceSize the size of the random marriage die
     */
    public void setRandomMarriageDiceSize(final int randomMarriageDiceSize) {
        this.randomMarriageDiceSize = randomMarriageDiceSize;
    }

    /**
     * @return the number of sides on the die used to determine random same-sex marriage
     */
    public int getRandomSameSexMarriageDiceSize() {
        return randomSameSexMarriageDiceSize;
    }

    /**
     * Sets the size of the random same-sex marriage die.
     *
     * @param randomSameSexMarriageDiceSize the size of the random same-sex marriage die
     */
    public void setRandomSameSexMarriageDiceSize(final int randomSameSexMarriageDiceSize) {
        this.randomSameSexMarriageDiceSize = randomSameSexMarriageDiceSize;
    }

    /**
     * @return the number of sides on the die used to determine whether marriage occurs outside of current personnel
     */
    public int getRandomNewDependentMarriage() {
        return randomNewDependentMarriage;
    }

    /**
     * Sets the size of the die used to determine whether marriage occurs outside of current personnel
     *
     * @param randomNewDependentMarriage the size of the die used to determine whether marriage occurs outside of current personnel
     */
    public void setRandomNewDependentMarriage(final int randomNewDependentMarriage) {
        this.randomNewDependentMarriage = randomNewDependentMarriage;
    }
    // endregion Marriage

    // region Divorce
    public boolean isUseManualDivorce() {
        return useManualDivorce;
    }

    public void setUseManualDivorce(final boolean useManualDivorce) {
        this.useManualDivorce = useManualDivorce;
    }

    public boolean isUseClanPersonnelDivorce() {
        return useClanPersonnelDivorce;
    }

    public void setUseClanPersonnelDivorce(final boolean useClanPersonnelDivorce) {
        this.useClanPersonnelDivorce = useClanPersonnelDivorce;
    }

    public boolean isUsePrisonerDivorce() {
        return usePrisonerDivorce;
    }

    public void setUsePrisonerDivorce(final boolean usePrisonerDivorce) {
        this.usePrisonerDivorce = usePrisonerDivorce;
    }

    public Map<SplittingSurnameStyle, Integer> getDivorceSurnameWeights() {
        return divorceSurnameWeights;
    }

    public void setDivorceSurnameWeights(final Map<SplittingSurnameStyle, Integer> divorceSurnameWeights) {
        this.divorceSurnameWeights = divorceSurnameWeights;
    }

    public RandomDivorceMethod getRandomDivorceMethod() {
        return randomDivorceMethod;
    }

    public void setRandomDivorceMethod(final RandomDivorceMethod randomDivorceMethod) {
        this.randomDivorceMethod = randomDivorceMethod;
    }

    public boolean isUseRandomOppositeSexDivorce() {
        return useRandomOppositeSexDivorce;
    }

    public void setUseRandomOppositeSexDivorce(final boolean useRandomOppositeSexDivorce) {
        this.useRandomOppositeSexDivorce = useRandomOppositeSexDivorce;
    }

    public boolean isUseRandomSameSexDivorce() {
        return useRandomSameSexDivorce;
    }

    public void setUseRandomSameSexDivorce(final boolean useRandomSameSexDivorce) {
        this.useRandomSameSexDivorce = useRandomSameSexDivorce;
    }

    public boolean isUseRandomClanPersonnelDivorce() {
        return useRandomClanPersonnelDivorce;
    }

    public void setUseRandomClanPersonnelDivorce(final boolean useRandomClanPersonnelDivorce) {
        this.useRandomClanPersonnelDivorce = useRandomClanPersonnelDivorce;
    }

    public boolean isUseRandomPrisonerDivorce() {
        return useRandomPrisonerDivorce;
    }

    public void setUseRandomPrisonerDivorce(final boolean useRandomPrisonerDivorce) {
        this.useRandomPrisonerDivorce = useRandomPrisonerDivorce;
    }

    public int getRandomDivorceDiceSize() {
        return randomDivorceDiceSize;
    }

    public void setRandomDivorceDiceSize(final int randomDivorceDiceSize) {
        this.randomDivorceDiceSize = randomDivorceDiceSize;
    }
    // endregion Divorce

    // region Procreation
    public boolean isUseManualProcreation() {
        return useManualProcreation;
    }

    public void setUseManualProcreation(final boolean useManualProcreation) {
        this.useManualProcreation = useManualProcreation;
    }

    public boolean isUseClanPersonnelProcreation() {
        return useClanPersonnelProcreation;
    }

    public void setUseClanPersonnelProcreation(final boolean useClanPersonnelProcreation) {
        this.useClanPersonnelProcreation = useClanPersonnelProcreation;
    }

    public boolean isUsePrisonerProcreation() {
        return usePrisonerProcreation;
    }

    public void setUsePrisonerProcreation(final boolean usePrisonerProcreation) {
        this.usePrisonerProcreation = usePrisonerProcreation;
    }

    /**
     * @return the X occurrences for there to be a single multiple child occurrence (i.e., 1 in X)
     */
    public int getMultiplePregnancyOccurrences() {
        return multiplePregnancyOccurrences;
    }

    /**
     * @param multiplePregnancyOccurrences the number of occurrences for there to be a single
     *                                     occurrence of a multiple child pregnancy (i.e., 1 in X)
     */
    public void setMultiplePregnancyOccurrences(final int multiplePregnancyOccurrences) {
        this.multiplePregnancyOccurrences = multiplePregnancyOccurrences;
    }

    /**
     * @return what style of surname to use for a baby
     */
    public BabySurnameStyle getBabySurnameStyle() {
        return babySurnameStyle;
    }

    /**
     * @param babySurnameStyle the style of surname to use for a baby
     */
    public void setBabySurnameStyle(final BabySurnameStyle babySurnameStyle) {
        this.babySurnameStyle = babySurnameStyle;
    }

    public boolean isAssignNonPrisonerBabiesFounderTag() {
        return assignNonPrisonerBabiesFounderTag;
    }

    public void setAssignNonPrisonerBabiesFounderTag(final boolean assignNonPrisonerBabiesFounderTag) {
        this.assignNonPrisonerBabiesFounderTag = assignNonPrisonerBabiesFounderTag;
    }

    public boolean isAssignChildrenOfFoundersFounderTag() {
        return assignChildrenOfFoundersFounderTag;
    }

    public void setAssignChildrenOfFoundersFounderTag(final boolean assignChildrenOfFoundersFounderTag) {
        this.assignChildrenOfFoundersFounderTag = assignChildrenOfFoundersFounderTag;
    }

    public boolean isUseMaternityLeave() {
        return useMaternityLeave;
    }

    public void setUseMaternityLeave(final boolean useMaternityLeave) {
        this.useMaternityLeave = useMaternityLeave;
    }

    /**
     * @return whether to determine the father at birth instead of at conception
     */
    public boolean isDetermineFatherAtBirth() {
        return determineFatherAtBirth;
    }

    /**
     * @param determineFatherAtBirth whether to determine the father at birth instead of at conception
     */
    public void setDetermineFatherAtBirth(final boolean determineFatherAtBirth) {
        this.determineFatherAtBirth = determineFatherAtBirth;
    }

    /**
     * @return whether to show the expected or actual due date for personnel
     */
    public boolean isDisplayTrueDueDate() {
        return displayTrueDueDate;
    }

    /**
     * @param displayTrueDueDate whether to show the expected or actual due date for
     *                           personnel
     */
    public void setDisplayTrueDueDate(final boolean displayTrueDueDate) {
        this.displayTrueDueDate = displayTrueDueDate;
    }

    public int getNoInterestInChildrenDiceSize() {
        return noInterestInChildrenDiceSize;
    }

    public void setNoInterestInChildrenDiceSize(final int noInterestInChildrenDiceSize) {
        this.noInterestInChildrenDiceSize = noInterestInChildrenDiceSize;
    }

    /**
     * @return whether to log procreation
     */
    public boolean isLogProcreation() {
        return logProcreation;
    }

    /**
     * @param logProcreation whether to log procreation
     */
    public void setLogProcreation(final boolean logProcreation) {
        this.logProcreation = logProcreation;
    }

    public RandomProcreationMethod getRandomProcreationMethod() {
        return randomProcreationMethod;
    }

    public void setRandomProcreationMethod(final RandomProcreationMethod randomProcreationMethod) {
        this.randomProcreationMethod = randomProcreationMethod;
    }

    /**
     * @return whether to use random procreation for personnel without a spouse
     */
    public boolean isUseRelationshiplessRandomProcreation() {
        return useRelationshiplessRandomProcreation;
    }

    /**
     * @param useRelationshiplessRandomProcreation whether to use random procreation without a spouse
     */
    public void setUseRelationshiplessRandomProcreation(final boolean useRelationshiplessRandomProcreation) {
        this.useRelationshiplessRandomProcreation = useRelationshiplessRandomProcreation;
    }

    public boolean isUseRandomClanPersonnelProcreation() {
        return useRandomClanPersonnelProcreation;
    }

    public void setUseRandomClanPersonnelProcreation(final boolean useRandomClanPersonnelProcreation) {
        this.useRandomClanPersonnelProcreation = useRandomClanPersonnelProcreation;
    }

    public boolean isUseRandomPrisonerProcreation() {
        return useRandomPrisonerProcreation;
    }

    public void setUseRandomPrisonerProcreation(final boolean useRandomPrisonerProcreation) {
        this.useRandomPrisonerProcreation = useRandomPrisonerProcreation;
    }

    /**
     * This gets the decimal chance (between 0 and 1) of random procreation
     * occurring
     *
     * @return the chance, with a value between 0 and 1
     */
    public int getRandomProcreationRelationshipDiceSize() {
        return randomProcreationRelationshipDiceSize;
    }

    /**
     * This sets the dice size for random procreation
     * @param randomProcreationRelationshipDiceSize the chance, with a value between 0 and 1
     */
    public void setRandomProcreationRelationshipDiceSize(final int randomProcreationRelationshipDiceSize) {
        this.randomProcreationRelationshipDiceSize = randomProcreationRelationshipDiceSize;
    }

    /**
     * @return the dice size for random procreation
     */
    public int getRandomProcreationRelationshiplessDiceSize() {
        return randomProcreationRelationshiplessDiceSize;
    }

    /**
     * This sets the decimal chance (between 0 and 1) of random procreation occurring without a relationship
     * @param randomProcreationRelationshiplessDiceSize the chance, with a value between 0 and 1
     */
    public void setRandomProcreationRelationshiplessDiceSize(final int randomProcreationRelationshiplessDiceSize) {
        this.randomProcreationRelationshiplessDiceSize = randomProcreationRelationshiplessDiceSize;
    }
    // endregion Procreation

    // region Death
    public boolean isUseEducationModule() {
        return useEducationModule;
    }

    public void setUseEducationModule(boolean useEducationModule) {
        this.useEducationModule = useEducationModule;
    }

    public Integer getCurriculumXpRate() {
        return curriculumXpRate;
    }

    public void setCurriculumXpRate(final int curriculumXpRate) {
        this.curriculumXpRate = curriculumXpRate;
    }

    public Integer getMaximumJumpCount() {
        return maximumJumpCount;
    }

    public void setMaximumJumpCount(Integer maximumJumpCount) {
        this.maximumJumpCount = maximumJumpCount;
    }

    public boolean isUseReeducationCamps() {
        return useReeducationCamps;
    }

    public void setUseReeducationCamps(boolean useReeducationCamps) {
        this.useReeducationCamps = useReeducationCamps;
    }

    public boolean isEnableLocalAcademies() {
        return enableLocalAcademies;
    }

    public void setEnableLocalAcademies(boolean enableLocalAcademies) {
        this.enableLocalAcademies = enableLocalAcademies;
    }

    public boolean isEnablePrestigiousAcademies() {
        return enablePrestigiousAcademies;
    }

    public void setEnablePrestigiousAcademies(boolean enablePrestigiousAcademies) {
        this.enablePrestigiousAcademies = enablePrestigiousAcademies;
    }

    public boolean isEnableUnitEducation() {
        return enableUnitEducation;
    }

    public void setEnableUnitEducation(boolean enableUnitEducation) {
        this.enableUnitEducation = enableUnitEducation;
    }

    public boolean isEnableOverrideRequirements() {
        return enableOverrideRequirements;
    }

    public void setEnableOverrideRequirements(boolean enableOverrideRequirements) {
        this.enableOverrideRequirements = enableOverrideRequirements;
    }

    public boolean isEnableShowIneligibleAcademies() {
        return enableShowIneligibleAcademies;
    }

    public void setEnableShowIneligibleAcademies(boolean enableShowIneligibleAcademies) {
        this.enableShowIneligibleAcademies = enableShowIneligibleAcademies;
    }

    public int getEntranceExamBaseTargetNumber() {
        return entranceExamBaseTargetNumber;
    }

    public void setEntranceExamBaseTargetNumber(int entranceExamBaseTargetNumber) {
        this.entranceExamBaseTargetNumber = entranceExamBaseTargetNumber;
    }

    public Double getFacultyXpRate() {
        return facultyXpRate;
    }

    public void setFacultyXpRate(Double facultyXpRate) {
        this.facultyXpRate = facultyXpRate;
    }

    public boolean isEnableBonuses() {
        return enableBonuses;
    }

    public void setEnableBonuses(boolean enableBonuses) {
        this.enableBonuses = enableBonuses;
    }

    public Integer getAdultDropoutChance() {
        return adultDropoutChance;
    }

    public void setAdultDropoutChance(Integer adultDropoutChance) {
        this.adultDropoutChance = adultDropoutChance;
    }

    public Integer getChildrenDropoutChance() {
        return childrenDropoutChance;
    }

    public void setChildrenDropoutChance(Integer childrenDropoutChance) {
        this.childrenDropoutChance = childrenDropoutChance;
    }

    public boolean isAllAges() {
        return allAges;
    }

    public void setAllAges(boolean allAges) {
        this.allAges = allAges;
    }

    public Integer getMilitaryAcademyAccidents() {
        return militaryAcademyAccidents;
    }

    public void setMilitaryAcademyAccidents(Integer militaryAcademyAccidents) {
        this.militaryAcademyAccidents = militaryAcademyAccidents;
    }

    public Map<AgeGroup, Boolean> getEnabledRandomDeathAgeGroups() {
        return enabledRandomDeathAgeGroups;
    }

    public void setEnabledRandomDeathAgeGroups(final Map<AgeGroup, Boolean> enabledRandomDeathAgeGroups) {
        this.enabledRandomDeathAgeGroups = enabledRandomDeathAgeGroups;
    }

    public boolean isUseRandomDeathSuicideCause() {
        return useRandomDeathSuicideCause;
    }

    public void setUseRandomDeathSuicideCause(final boolean useRandomDeathSuicideCause) {
        this.useRandomDeathSuicideCause = useRandomDeathSuicideCause;
    }

    public double getRandomDeathMultiplier() {
        return randomDeathMultiplier;
    }

    public void setRandomDeathMultiplier(final int randomDeathMultiplier) {
        this.randomDeathMultiplier = randomDeathMultiplier;
    }
    // endregion Death

    // region Awards
    public boolean isIssuePosthumousAwards() {
        return issuePosthumousAwards;
    }

    public void setIssuePosthumousAwards(final boolean issuePosthumousAwards) {
        this.issuePosthumousAwards = issuePosthumousAwards;
    }

    public boolean isIssueBestAwardOnly() {
        return issueBestAwardOnly;
    }

    public void setIssueBestAwardOnly(final boolean issueBestAwardOnly) {
        this.issueBestAwardOnly = issueBestAwardOnly;
    }

    public boolean isIgnoreStandardSet() {
        return ignoreStandardSet;
    }

    public void setIgnoreStandardSet(final boolean ignoreStandardSet) {
        this.ignoreStandardSet = ignoreStandardSet;
    }

    public int getAwardTierSize() {
        return awardTierSize;
    }

    public void setAwardTierSize(final int awardTierSize) {
        this.awardTierSize = awardTierSize;
    }

    public AwardBonus getAwardBonusStyle() {
        return awardBonusStyle;
    }

    public void setAwardBonusStyle(final AwardBonus awardBonusStyle) {
        this.awardBonusStyle = awardBonusStyle;
    }

    public boolean isEnableAutoAwards() {
        return enableAutoAwards;
    }

    public void setEnableAutoAwards(final boolean enableAutoAwards) {
        this.enableAutoAwards = enableAutoAwards;
    }

    public boolean isEnableContractAwards() {
        return enableContractAwards;
    }

    public void setEnableContractAwards(final boolean enableContractAwards) {
        this.enableContractAwards = enableContractAwards;
    }

    public boolean isEnableFactionHunterAwards() {
        return enableFactionHunterAwards;
    }

    public void setEnableFactionHunterAwards(final boolean enableFactionHunterAwards) {
        this.enableFactionHunterAwards = enableFactionHunterAwards;
    }

    public boolean isEnableInjuryAwards() {
        return enableInjuryAwards;
    }

    public void setEnableInjuryAwards(final boolean enableInjuryAwards) {
        this.enableInjuryAwards = enableInjuryAwards;
    }

    public boolean isEnableIndividualKillAwards() {
        return enableIndividualKillAwards;
    }

    public void setEnableIndividualKillAwards(final boolean enableIndividualKillAwards) {
        this.enableIndividualKillAwards = enableIndividualKillAwards;
    }

    public boolean isEnableFormationKillAwards() {
        return enableFormationKillAwards;
    }

    public void setEnableFormationKillAwards(final boolean enableFormationKillAwards) {
        this.enableFormationKillAwards = enableFormationKillAwards;
    }

    public boolean isEnableRankAwards() {
        return enableRankAwards;
    }

    public void setEnableRankAwards(final boolean enableRankAwards) {
        this.enableRankAwards = enableRankAwards;
    }

    public boolean isEnableScenarioAwards() {
        return enableScenarioAwards;
    }

    public void setEnableScenarioAwards(final boolean enableScenarioAwards) {
        this.enableScenarioAwards = enableScenarioAwards;
    }

    public boolean isEnableSkillAwards() {
        return enableSkillAwards;
    }

    public void setEnableSkillAwards(final boolean enableSkillAwards) {
        this.enableSkillAwards = enableSkillAwards;
    }

    public boolean isEnableTheatreOfWarAwards() {
        return enableTheatreOfWarAwards;
    }

    public void setEnableTheatreOfWarAwards(final boolean enableTheatreOfWarAwards) {
        this.enableTheatreOfWarAwards = enableTheatreOfWarAwards;
    }

    public boolean isEnableTimeAwards() {
        return enableTimeAwards;
    }

    public void setEnableTimeAwards(final boolean enableTimeAwards) {
        this.enableTimeAwards = enableTimeAwards;
    }

    public boolean isEnableTrainingAwards() {
        return enableTrainingAwards;
    }

    public void setEnableTrainingAwards(final boolean enableTrainingAwards) {
        this.enableTrainingAwards = enableTrainingAwards;
    }

    public boolean isEnableMiscAwards() {
        return enableMiscAwards;
    }

    public void setEnableMiscAwards(final boolean enableMiscAwards) {
        this.enableMiscAwards = enableMiscAwards;
    }

    public String getAwardSetFilterList() {
        return awardSetFilterList;
    }

    public void setAwardSetFilterList(final String awardSetFilterList) {
        this.awardSetFilterList = awardSetFilterList;
    }
    // endregion Awards
    // endregion Personnel Tab

    // region Finances Tab
    public boolean isPayForParts() {
        return payForParts;
    }

    public void setPayForParts(final boolean payForParts) {
        this.payForParts = payForParts;
    }

    public boolean isPayForRepairs() {
        return payForRepairs;
    }

    public void setPayForRepairs(final boolean payForRepairs) {
        this.payForRepairs = payForRepairs;
    }

    public boolean isPayForUnits() {
        return payForUnits;
    }

    public void setPayForUnits(final boolean payForUnits) {
        this.payForUnits = payForUnits;
    }

    public boolean isPayForSalaries() {
        return payForSalaries;
    }

    public void setPayForSalaries(final boolean payForSalaries) {
        this.payForSalaries = payForSalaries;
    }

    public boolean isPayForOverhead() {
        return payForOverhead;
    }

    public void setPayForOverhead(final boolean payForOverhead) {
        this.payForOverhead = payForOverhead;
    }

    public boolean isPayForMaintain() {
        return payForMaintain;
    }

    public void setPayForMaintain(final boolean payForMaintain) {
        this.payForMaintain = payForMaintain;
    }

    public boolean isPayForTransport() {
        return payForTransport;
    }

    public void setPayForTransport(final boolean payForTransport) {
        this.payForTransport = payForTransport;
    }

    public boolean isSellUnits() {
        return sellUnits;
    }

    public void setSellUnits(final boolean sellUnits) {
        this.sellUnits = sellUnits;
    }

    public boolean isSellParts() {
        return sellParts;
    }

    public void setSellParts(final boolean sellParts) {
        this.sellParts = sellParts;
    }

    public boolean isPayForRecruitment() {
        return payForRecruitment;
    }

    public void setPayForRecruitment(final boolean payForRecruitment) {
        this.payForRecruitment = payForRecruitment;
    }

    public boolean isUseLoanLimits() {
        return useLoanLimits;
    }

    public void setLoanLimits(final boolean useLoanLimits) {
        this.useLoanLimits = useLoanLimits;
    }

    public boolean isUsePercentageMaint() {
        return usePercentageMaint;
    }

    public void setUsePercentageMaint(final boolean usePercentageMaint) {
        this.usePercentageMaint = usePercentageMaint;
    }

    public boolean isInfantryDontCount() {
        return infantryDontCount;
    }

    public void setUseInfantryDontCount(final boolean infantryDontCount) {
        this.infantryDontCount = infantryDontCount;
    }

    public boolean isUsePeacetimeCost() {
        return usePeacetimeCost;
    }

    public void setUsePeacetimeCost(final boolean usePeacetimeCost) {
        this.usePeacetimeCost = usePeacetimeCost;
    }

    public boolean isUseExtendedPartsModifier() {
        return useExtendedPartsModifier;
    }

    public void setUseExtendedPartsModifier(final boolean useExtendedPartsModifier) {
        this.useExtendedPartsModifier = useExtendedPartsModifier;
    }

    public boolean isShowPeacetimeCost() {
        return showPeacetimeCost;
    }

    public void setShowPeacetimeCost(final boolean showPeacetimeCost) {
        this.showPeacetimeCost = showPeacetimeCost;
    }

    /**
     * @return the duration of a financial year
     */
    public FinancialYearDuration getFinancialYearDuration() {
        return financialYearDuration;
    }

    /**
     * @param financialYearDuration the financial year duration to set
     */
    public void setFinancialYearDuration(final FinancialYearDuration financialYearDuration) {
        this.financialYearDuration = financialYearDuration;
    }

    /**
     * @return whether to export finances to CSV at the end of a financial year
     */
    public boolean isNewFinancialYearFinancesToCSVExport() {
        return newFinancialYearFinancesToCSVExport;
    }

    /**
     * @param newFinancialYearFinancesToCSVExport whether to export finances to CSV at the end of a financial year
     */
    public void setNewFinancialYearFinancesToCSVExport(final boolean newFinancialYearFinancesToCSVExport) {
        this.newFinancialYearFinancesToCSVExport = newFinancialYearFinancesToCSVExport;
    }

    // region Price Multipliers
    public double getCommonPartPriceMultiplier() {
        return commonPartPriceMultiplier;
    }

    public void setCommonPartPriceMultiplier(final double commonPartPriceMultiplier) {
        this.commonPartPriceMultiplier = commonPartPriceMultiplier;
    }

    public double getInnerSphereUnitPriceMultiplier() {
        return innerSphereUnitPriceMultiplier;
    }

    public void setInnerSphereUnitPriceMultiplier(final double innerSphereUnitPriceMultiplier) {
        this.innerSphereUnitPriceMultiplier = innerSphereUnitPriceMultiplier;
    }

    public double getInnerSpherePartPriceMultiplier() {
        return innerSpherePartPriceMultiplier;
    }

    public void setInnerSpherePartPriceMultiplier(final double innerSpherePartPriceMultiplier) {
        this.innerSpherePartPriceMultiplier = innerSpherePartPriceMultiplier;
    }

    public double getClanUnitPriceMultiplier() {
        return clanUnitPriceMultiplier;
    }

    public void setClanUnitPriceMultiplier(final double clanUnitPriceMultiplier) {
        this.clanUnitPriceMultiplier = clanUnitPriceMultiplier;
    }

    public double getClanPartPriceMultiplier() {
        return clanPartPriceMultiplier;
    }

    public void setClanPartPriceMultiplier(final double clanPartPriceMultiplier) {
        this.clanPartPriceMultiplier = clanPartPriceMultiplier;
    }

    public double getMixedTechUnitPriceMultiplier() {
        return mixedTechUnitPriceMultiplier;
    }

    public void setMixedTechUnitPriceMultiplier(final double mixedTechUnitPriceMultiplier) {
        this.mixedTechUnitPriceMultiplier = mixedTechUnitPriceMultiplier;
    }

    public double[] getUsedPartPriceMultipliers() {
        return usedPartPriceMultipliers;
    }

    public void setUsedPartPriceMultipliers(final double... usedPartPriceMultipliers) {
        this.usedPartPriceMultipliers = usedPartPriceMultipliers;
    }

    public double getDamagedPartsValueMultiplier() {
        return damagedPartsValueMultiplier;
    }

    public void setDamagedPartsValueMultiplier(final double damagedPartsValueMultiplier) {
        this.damagedPartsValueMultiplier = damagedPartsValueMultiplier;
    }

    public double getUnrepairablePartsValueMultiplier() {
        return unrepairablePartsValueMultiplier;
    }

    public void setUnrepairablePartsValueMultiplier(final double unrepairablePartsValueMultiplier) {
        this.unrepairablePartsValueMultiplier = unrepairablePartsValueMultiplier;
    }

    public double getCancelledOrderRefundMultiplier() {
        return cancelledOrderRefundMultiplier;
    }

    public void setCancelledOrderRefundMultiplier(final double cancelledOrderRefundMultiplier) {
        this.cancelledOrderRefundMultiplier = cancelledOrderRefundMultiplier;
    }
    // endregion Price Multipliers

    // region Taxes
    public boolean isUseTaxes() {
        return useTaxes;
    }

    public void setUseTaxes(final boolean useTaxes) {
        this.useTaxes = useTaxes;
    }

    public Integer getTaxesPercentage() {
        return taxesPercentage;
    }

    public void setTaxesPercentage(final int taxesPercentage) {
        this.taxesPercentage = taxesPercentage;
    }
    // endregion Taxes
    // endregion Finances Tab

    // region Markets Tab
    // region Personnel Market
    public String getPersonnelMarketName() {
        return personnelMarketName;
    }

    public void setPersonnelMarketName(final String personnelMarketName) {
        this.personnelMarketName = personnelMarketName;
    }

    public boolean isPersonnelMarketReportRefresh() {
        return personnelMarketReportRefresh;
    }

    public void setPersonnelMarketReportRefresh(final boolean personnelMarketReportRefresh) {
        this.personnelMarketReportRefresh = personnelMarketReportRefresh;
    }

    public Map<SkillLevel, Integer> getPersonnelMarketRandomRemovalTargets() {
        return personnelMarketRandomRemovalTargets;
    }

    public void setPersonnelMarketRandomRemovalTargets(
            final Map<SkillLevel, Integer> personnelMarketRandomRemovalTargets) {
        this.personnelMarketRandomRemovalTargets = personnelMarketRandomRemovalTargets;
    }

    public double getPersonnelMarketDylansWeight() {
        return personnelMarketDylansWeight;
    }

    public void setPersonnelMarketDylansWeight(final double personnelMarketDylansWeight) {
        this.personnelMarketDylansWeight = personnelMarketDylansWeight;
    }

    public boolean isUsePersonnelHireHiringHallOnly() {
        return usePersonnelHireHiringHallOnly;
    }

    public void setUsePersonnelHireHiringHallOnly(final boolean usePersonnelHireHiringHallOnly) {
        this.usePersonnelHireHiringHallOnly = usePersonnelHireHiringHallOnly;
    }
    // endregion Personnel Market

    // region Unit Market
    public UnitMarketMethod getUnitMarketMethod() {
        return unitMarketMethod;
    }

    public void setUnitMarketMethod(final UnitMarketMethod unitMarketMethod) {
        this.unitMarketMethod = unitMarketMethod;
    }

    public boolean isUnitMarketRegionalMekVariations() {
        return unitMarketRegionalMekVariations;
    }

    public void setUnitMarketRegionalMekVariations(final boolean unitMarketRegionalMekVariations) {
        this.unitMarketRegionalMekVariations = unitMarketRegionalMekVariations;
    }

    public int getUnitMarketSpecialUnitChance() {
        return unitMarketSpecialUnitChance;
    }

    public void setUnitMarketSpecialUnitChance(final int unitMarketSpecialUnitChance) {
        this.unitMarketSpecialUnitChance = unitMarketSpecialUnitChance;
    }

    public int getUnitMarketRarityModifier() {
        return unitMarketRarityModifier;
    }

    public void setUnitMarketRarityModifier(final int unitMarketRarityModifier) {
        this.unitMarketRarityModifier = unitMarketRarityModifier;
    }

    public boolean isInstantUnitMarketDelivery() {
        return instantUnitMarketDelivery;
    }

    public void setInstantUnitMarketDelivery(final boolean instantUnitMarketDelivery) {
        this.instantUnitMarketDelivery = instantUnitMarketDelivery;
    }

    public boolean isUnitMarketReportRefresh() {
        return unitMarketReportRefresh;
    }

    public void setUnitMarketReportRefresh(final boolean unitMarketReportRefresh) {
        this.unitMarketReportRefresh = unitMarketReportRefresh;
    }
    // endregion Unit Market

    // region Contract Market
    public ContractMarketMethod getContractMarketMethod() {
        return contractMarketMethod;
    }

    public void setContractMarketMethod(final ContractMarketMethod contractMarketMethod) {
        this.contractMarketMethod = contractMarketMethod;
    }

    public int getContractSearchRadius() {
        return contractSearchRadius;
    }

    public void setContractSearchRadius(final int contractSearchRadius) {
        this.contractSearchRadius = contractSearchRadius;
    }

    public boolean isVariableContractLength() {
        return variableContractLength;
    }

    public void setVariableContractLength(final boolean variableContractLength) {
        this.variableContractLength = variableContractLength;
    }

    public boolean isContractMarketReportRefresh() {
        return contractMarketReportRefresh;
    }

    public void setContractMarketReportRefresh(final boolean contractMarketReportRefresh) {
        this.contractMarketReportRefresh = contractMarketReportRefresh;
    }

    public int getContractMaxSalvagePercentage() {
        return contractMaxSalvagePercentage;
    }

    public void setContractMaxSalvagePercentage(final int contractMaxSalvagePercentage) {
        this.contractMaxSalvagePercentage = contractMaxSalvagePercentage;
    }

    public int getDropShipBonusPercentage() {
        return dropShipBonusPercentage;
    }

    public void setDropShipBonusPercentage(final int dropShipBonusPercentage) {
        this.dropShipBonusPercentage = dropShipBonusPercentage;
    }
    // endregion Contract Market
    // endregion Markets Tab

    // region RATs Tab
    public boolean isUseStaticRATs() {
        return useStaticRATs;
    }

    public void setUseStaticRATs(final boolean useStaticRATs) {
        this.useStaticRATs = useStaticRATs;
    }

    public String[] getRATs() {
        return rats;
    }

    public void setRATs(final String... rats) {
        this.rats = rats;
    }

    public boolean isIgnoreRATEra() {
        return ignoreRATEra;
    }

    public void setIgnoreRATEra(final boolean ignore) {
        this.ignoreRATEra = ignore;
    }
    // endregion RATs Tab

    public boolean isUseEraMods() {
        return useEraMods;
    }

    public void setEraMods(final boolean useEraMods) {
        this.useEraMods = useEraMods;
    }

    public boolean isAssignedTechFirst() {
        return assignedTechFirst;
    }

    public void setAssignedTechFirst(final boolean assignedTechFirst) {
        this.assignedTechFirst = assignedTechFirst;
    }

    public boolean isResetToFirstTech() {
        return resetToFirstTech;
    }

    public void setResetToFirstTech(final boolean resetToFirstTech) {
        this.resetToFirstTech = resetToFirstTech;
    }

    /**
     * @return true to use the origin faction for personnel names instead of a set
     *         faction
     */
    public boolean isUseOriginFactionForNames() {
        return useOriginFactionForNames;
    }

    /**
     * @param useOriginFactionForNames whether to use personnel names or a set
     *                                 faction
     */
    public void setUseOriginFactionForNames(final boolean useOriginFactionForNames) {
        this.useOriginFactionForNames = useOriginFactionForNames;
    }

    public boolean isUseQuirks() {
        return useQuirks;
    }

    public void setQuirks(final boolean useQuirks) {
        this.useQuirks = useQuirks;
    }

    public double getXpCostMultiplier() {
        return xpCostMultiplier;
    }

    public void setXpCostMultiplier(final double xpCostMultiplier) {
        this.xpCostMultiplier = xpCostMultiplier;
    }

    public int getScenarioXP() {
        return scenarioXP;
    }

    public void setScenarioXP(final int scenarioXP) {
        this.scenarioXP = scenarioXP;
    }

    public int getKillsForXP() {
        return killsForXP;
    }

    public void setKillsForXP(final int killsForXP) {
        this.killsForXP = killsForXP;
    }

    public int getKillXPAward() {
        return killXPAward;
    }

    public void setKillXPAward(final int killXPAward) {
        this.killXPAward = killXPAward;
    }

    public int getNTasksXP() {
        return nTasksXP;
    }

    public void setNTasksXP(final int nTasksXP) {
        this.nTasksXP = nTasksXP;
    }

    public int getTaskXP() {
        return tasksXP;
    }

    public void setTaskXP(final int tasksXP) {
        this.tasksXP = tasksXP;
    }

    public int getMistakeXP() {
        return mistakeXP;
    }

    public void setMistakeXP(final int mistakeXP) {
        this.mistakeXP = mistakeXP;
    }

    public int getSuccessXP() {
        return successXP;
    }

    public void setSuccessXP(final int successXP) {
        this.successXP = successXP;
    }

    public boolean isLimitByYear() {
        return limitByYear;
    }

    public void setLimitByYear(final boolean limitByYear) {
        this.limitByYear = limitByYear;
    }

    public boolean isDisallowExtinctStuff() {
        return disallowExtinctStuff;
    }

    public void setDisallowExtinctStuff(final boolean disallowExtinctStuff) {
        this.disallowExtinctStuff = disallowExtinctStuff;
    }

    public boolean isAllowClanPurchases() {
        return allowClanPurchases;
    }

    public void setAllowClanPurchases(final boolean allowClanPurchases) {
        this.allowClanPurchases = allowClanPurchases;
    }

    public boolean isAllowISPurchases() {
        return allowISPurchases;
    }

    public void setAllowISPurchases(final boolean allowISPurchases) {
        this.allowISPurchases = allowISPurchases;
    }

    public boolean isAllowCanonOnly() {
        return allowCanonOnly;
    }

    public void setAllowCanonOnly(final boolean allowCanonOnly) {
        this.allowCanonOnly = allowCanonOnly;
    }

    public boolean isAllowCanonRefitOnly() {
        return allowCanonRefitOnly;
    }

    public void setAllowCanonRefitOnly(final boolean allowCanonRefitOnly) {
        this.allowCanonRefitOnly = allowCanonRefitOnly;
    }

    public boolean isVariableTechLevel() {
        return variableTechLevel;
    }

    public void setVariableTechLevel(final boolean variableTechLevel) {
        this.variableTechLevel = variableTechLevel;
    }

    public boolean isFactionIntroDate() {
        return factionIntroDate;
    }

    public void setFactionIntroDate(final boolean factionIntroDate) {
        this.factionIntroDate = factionIntroDate;
    }

    public boolean isUseAmmoByType() {
        return useAmmoByType;
    }

    public void setUseAmmoByType(final boolean useAmmoByType) {
        this.useAmmoByType = useAmmoByType;
    }

    public int getTechLevel() {
        return techLevel;
    }

    public void setTechLevel(final int techLevel) {
        this.techLevel = techLevel;
    }

    public int[] getPhenotypeProbabilities() {
        return phenotypeProbabilities;
    }

    public int getPhenotypeProbability(final Phenotype phenotype) {
        return getPhenotypeProbabilities()[phenotype.ordinal()];
    }

    public void setPhenotypeProbability(final int index, final int percentage) {
        this.phenotypeProbabilities[index] = percentage;
    }

    public boolean[] isUsePortraitForRoles() {
        return usePortraitForRole;
    }

    public boolean isUsePortraitForRole(final PersonnelRole role) {
        return isUsePortraitForRoles()[role.ordinal()];
    }

    public void setUsePortraitForRole(final int index, final boolean use) {
        this.usePortraitForRole[index] = use;
    }

    public boolean isAssignPortraitOnRoleChange() {
        return assignPortraitOnRoleChange;
    }

    public void setAssignPortraitOnRoleChange(final boolean assignPortraitOnRoleChange) {
        this.assignPortraitOnRoleChange = assignPortraitOnRoleChange;
    }

    public int getVocationalXP() {
        return vocationalXP;
    }

    public void setVocationalXP(final int vocationalXP) {
        this.vocationalXP = vocationalXP;
    }

    public int getVocationalXPTargetNumber() {
        return vocationalXPTargetNumber;
    }

    public void setVocationalXPTargetNumber(final int vocationalXPTargetNumber) {
        this.vocationalXPTargetNumber = vocationalXPTargetNumber;
    }

    public int getVocationalXPCheckFrequency() {
        return vocationalXPCheckFrequency;
    }

    public void setVocationalXPCheckFrequency(final int vocationalXPCheckFrequency) {
        this.vocationalXPCheckFrequency = vocationalXPCheckFrequency;
    }

    public int getContractNegotiationXP() {
        return contractNegotiationXP;
    }

    public void setContractNegotiationXP(final int contractNegotiationXP) {
        this.contractNegotiationXP = contractNegotiationXP;
    }

    public int getAdminXP() {
        return adminXP;
    }

    public void setAdminXP(final int adminXP) {
        this.adminXP = adminXP;
    }

    public int getAdminXPPeriod() {
        return adminXPPeriod;
    }

    public void setAdminXPPeriod(final int adminXPPeriod) {
        this.adminXPPeriod = adminXPPeriod;
    }

    public int getMissionXpFail() {
        return missionXpFail;
    }

    public void setMissionXpFail(final int missionXpFail) {
        this.missionXpFail = missionXpFail;
    }

    public int getMissionXpSuccess() {
        return missionXpSuccess;
    }

    public void setMissionXpSuccess(final int missionXpSuccess) {
        this.missionXpSuccess = missionXpSuccess;
    }

    public int getMissionXpOutstandingSuccess() {
        return missionXpOutstandingSuccess;
    }

    public void setMissionXpOutstandingSuccess(final int missionXpOutstandingSuccess) {
        this.missionXpOutstandingSuccess = missionXpOutstandingSuccess;
    }

    public int getEdgeCost() {
        return edgeCost;
    }

    public void setEdgeCost(final int edgeCost) {
        this.edgeCost = edgeCost;
    }

    public int getWaitingPeriod() {
        return waitingPeriod;
    }

    public void setWaitingPeriod(final int acquisitionSkill) {
        this.waitingPeriod = acquisitionSkill;
    }

    public String getAcquisitionSkill() {
        return acquisitionSkill;
    }

    public void setAcquisitionSkill(final String acquisitionSkill) {
        this.acquisitionSkill = acquisitionSkill;
    }

    public boolean isAcquisitionSupportStaffOnly() {
        return acquisitionSupportStaffOnly;
    }

    public void setAcquisitionSupportStaffOnly(final boolean acquisitionSupportStaffOnly) {
        this.acquisitionSupportStaffOnly = acquisitionSupportStaffOnly;
    }

    public int getUnitTransitTime() {
        return unitTransitTime;
    }

    public void setUnitTransitTime(final int unitTransitTime) {
        this.unitTransitTime = unitTransitTime;
    }

    public boolean isUsePlanetaryAcquisition() {
        return usePlanetaryAcquisition;
    }

    public void setPlanetaryAcquisition(final boolean usePlanetaryAcquisition) {
        this.usePlanetaryAcquisition = usePlanetaryAcquisition;
    }

    public PlanetaryAcquisitionFactionLimit getPlanetAcquisitionFactionLimit() {
        return planetAcquisitionFactionLimit;
    }

    public void setPlanetAcquisitionFactionLimit(final PlanetaryAcquisitionFactionLimit planetAcquisitionFactionLimit) {
        this.planetAcquisitionFactionLimit = planetAcquisitionFactionLimit;
    }

    public boolean isPlanetAcquisitionNoClanCrossover() {
        return planetAcquisitionNoClanCrossover;
    }

    public void setDisallowPlanetAcquisitionClanCrossover(final boolean planetAcquisitionNoClanCrossover) {
        this.planetAcquisitionNoClanCrossover = planetAcquisitionNoClanCrossover;
    }

    public int getMaxJumpsPlanetaryAcquisition() {
        return maxJumpsPlanetaryAcquisition;
    }

    public void setMaxJumpsPlanetaryAcquisition(final int maxJumpsPlanetaryAcquisition) {
        this.maxJumpsPlanetaryAcquisition = maxJumpsPlanetaryAcquisition;
    }

    public int getPenaltyClanPartsFromIS() {
        return penaltyClanPartsFromIS;
    }

    public void setPenaltyClanPartsFromIS(final int penaltyClanPartsFromIS) {
        this.penaltyClanPartsFromIS = penaltyClanPartsFromIS;
    }

    public boolean isNoClanPartsFromIS() {
        return noClanPartsFromIS;
    }

    public void setDisallowClanPartsFromIS(final boolean noClanPartsFromIS) {
        this.noClanPartsFromIS = noClanPartsFromIS;
    }

    public boolean isPlanetAcquisitionVerbose() {
        return planetAcquisitionVerbose;
    }

    public void setPlanetAcquisitionVerboseReporting(final boolean planetAcquisitionVerbose) {
        this.planetAcquisitionVerbose = planetAcquisitionVerbose;
    }

    public double getEquipmentContractPercent() {
        return equipmentContractPercent;
    }

    public void setEquipmentContractPercent(final double equipmentContractPercent) {
        this.equipmentContractPercent = Math.min(equipmentContractPercent, MAXIMUM_COMBAT_EQUIPMENT_PERCENT);
    }

    public boolean isEquipmentContractBase() {
        return equipmentContractBase;
    }

    public void setEquipmentContractBase(final boolean equipmentContractBase) {
        this.equipmentContractBase = equipmentContractBase;
    }

    public boolean isEquipmentContractSaleValue() {
        return equipmentContractSaleValue;
    }

    public void setEquipmentContractSaleValue(final boolean equipmentContractSaleValue) {
        this.equipmentContractSaleValue = equipmentContractSaleValue;
    }

    public double getDropShipContractPercent() {
        return dropShipContractPercent;
    }

    public void setDropShipContractPercent(final double dropShipContractPercent) {
        this.dropShipContractPercent = Math.min(dropShipContractPercent, MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT);
    }

    public double getJumpShipContractPercent() {
        return jumpShipContractPercent;
    }

    public void setJumpShipContractPercent(final double jumpShipContractPercent) {
        this.jumpShipContractPercent = Math.min(jumpShipContractPercent, MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT);
    }

    public double getWarShipContractPercent() {
        return warShipContractPercent;
    }

    public void setWarShipContractPercent(final double warShipContractPercent) {
        this.warShipContractPercent = Math.min(warShipContractPercent, MAXIMUM_WARSHIP_EQUIPMENT_PERCENT);
    }

    public boolean isBLCSaleValue() {
        return blcSaleValue;
    }

    public void setBLCSaleValue(final boolean blcSaleValue) {
        this.blcSaleValue = blcSaleValue;
    }

    public boolean isOverageRepaymentInFinalPayment() {
        return overageRepaymentInFinalPayment;
    }

    public void setOverageRepaymentInFinalPayment(final boolean overageRepaymentInFinalPayment) {
        this.overageRepaymentInFinalPayment = overageRepaymentInFinalPayment;
    }

    public int getClanAcquisitionPenalty() {
        return clanAcquisitionPenalty;
    }

    public void setClanAcquisitionPenalty(final int clanAcquisitionPenalty) {
        this.clanAcquisitionPenalty = clanAcquisitionPenalty;
    }

    public int getIsAcquisitionPenalty() {
        return isAcquisitionPenalty;
    }

    public void setIsAcquisitionPenalty(final int isAcquisitionPenalty) {
        this.isAcquisitionPenalty = isAcquisitionPenalty;
    }

    public int getPlanetTechAcquisitionBonus(final int type) {
        return ((type < 0) || (type >= planetTechAcquisitionBonus.length)) ? 0 : planetTechAcquisitionBonus[type];
    }

    public void setPlanetTechAcquisitionBonus(final int base, final int type) {
        if ((type < 0) || (type >= planetTechAcquisitionBonus.length)) {
            return;
        }
        this.planetTechAcquisitionBonus[type] = base;
    }

    public int getPlanetIndustryAcquisitionBonus(final int type) {
        return ((type < 0) || (type >= planetIndustryAcquisitionBonus.length)) ? 0
                : planetIndustryAcquisitionBonus[type];
    }

    public void setPlanetIndustryAcquisitionBonus(final int base, final int type) {
        if ((type < 0) || (type >= planetIndustryAcquisitionBonus.length)) {
            return;
        }
        this.planetIndustryAcquisitionBonus[type] = base;
    }

    public int getPlanetOutputAcquisitionBonus(final int type) {
        return ((type < 0) || (type >= planetOutputAcquisitionBonus.length)) ? 0 : planetOutputAcquisitionBonus[type];
    }

    public void setPlanetOutputAcquisitionBonus(final int base, final int type) {
        if ((type < 0) || (type >= planetOutputAcquisitionBonus.length)) {
            return;
        }
        this.planetOutputAcquisitionBonus[type] = base;
    }

    public boolean isDestroyByMargin() {
        return destroyByMargin;
    }

    public void setDestroyByMargin(final boolean destroyByMargin) {
        this.destroyByMargin = destroyByMargin;
    }

    public int getDestroyMargin() {
        return destroyMargin;
    }

    public void setDestroyMargin(final int destroyMargin) {
        this.destroyMargin = destroyMargin;
    }

    public int getDestroyPartTarget() {
        return destroyPartTarget;
    }

    public void setDestroyPartTarget(final int destroyPartTarget) {
        this.destroyPartTarget = destroyPartTarget;
    }

    public boolean isUseAeroSystemHits() {
        return useAeroSystemHits;
    }

    public void setUseAeroSystemHits(final boolean useAeroSystemHits) {
        this.useAeroSystemHits = useAeroSystemHits;
    }

    public int getMaxAcquisitions() {
        return maxAcquisitions;
    }

    public void setMaxAcquisitions(final int maxAcquisitions) {
        this.maxAcquisitions = maxAcquisitions;
    }

    public int getAutoLogisticsHeatSink() {
        return autoLogisticsHeatSink;
    }

    public void setAutoLogisticsHeatSink(int autoLogisticsHeatSink) {
        this.autoLogisticsHeatSink = autoLogisticsHeatSink;
    }

    public int getAutoLogisticsMekHead() {
        return autoLogisticsMekHead;
    }

    public void setAutoLogisticsMekHead(int autoLogisticsMekHead) {
        this.autoLogisticsMekHead = autoLogisticsMekHead;
    }

    public int getAutoLogisticsMekLocation() {
        return autoLogisticsMekLocation;
    }

    public void setAutoLogisticsMekLocation(int autoLogisticsMekLocation) {
        this.autoLogisticsMekLocation = autoLogisticsMekLocation;
    }

    public int getAutoLogisticsNonRepairableLocation() {
        return autoLogisticsNonRepairableLocation;
    }

    public void setAutoLogisticsNonRepairableLocation(int autoLogisticsNonRepairableLocation) {
        this.autoLogisticsNonRepairableLocation = autoLogisticsNonRepairableLocation;
    }

    public int getAutoLogisticsArmor() {
        return autoLogisticsArmor;
    }

    public void setAutoLogisticsArmor(int autoLogisticsArmor) {
        this.autoLogisticsArmor = autoLogisticsArmor;
    }

    public int getAutoLogisticsAmmunition() {
        return autoLogisticsAmmunition;
    }

    public void setAutoLogisticsAmmunition(int autoLogisticsAmmunition) {
        this.autoLogisticsAmmunition = autoLogisticsAmmunition;
    }

    public int getAutoLogisticsOther() {
        return autoLogisticsOther;
    }

    public void setAutoLogisticsOther(int autoLogisticsOther) {
        this.autoLogisticsOther = autoLogisticsOther;
    }

    public boolean isUseAtB() {
        return useAtB || useStratCon;
    }

    public void setUseAtB(final boolean useAtB) {
        this.useAtB = useAtB;
    }

    public boolean isUseStratCon() {
        return useStratCon;
    }

    public void setUseStratCon(final boolean useStratCon) {
        this.useStratCon = useStratCon;
    }

    public boolean isUseAero() {
        return useAero;
    }

    public void setUseAero(final boolean useAero) {
        this.useAero = useAero;
    }

    public boolean isUseVehicles() {
        return useVehicles;
    }

    public void setUseVehicles(final boolean useVehicles) {
        this.useVehicles = useVehicles;
    }

    public boolean isClanVehicles() {
        return clanVehicles;
    }

    public void setClanVehicles(final boolean clanVehicles) {
        this.clanVehicles = clanVehicles;
    }

    /**
     * Returns whether Generic BV is being used.
     *
     * @return {@code true} if Generic BV is enabled, {@code false} otherwise.
     */
    public boolean isUseGenericBattleValue() {
        return useGenericBattleValue;
    }


    /**
     * Sets the flag indicating whether BV Balanced bot forces should use Generic BV.
     *
     * @param useGenericBattleValue flag indicating whether to use Generic BV
     */
    public void setUseGenericBattleValue(final boolean useGenericBattleValue) {
        this.useGenericBattleValue = useGenericBattleValue;
    }

    /**
     * Returns whether the verbose bidding mode is enabled.
     *
     * @return {@code true} if verbose bidding is enabled, {@code false} otherwise.
     */
    public boolean isUseVerboseBidding() {
        return useVerboseBidding;
    }

    /**
     * Sets the flag indicating whether verbose bidding should be used.
     *
     * @param useVerboseBidding flag indicating whether to use verbose bidding
     */
    public void setUseVerboseBidding(final boolean useVerboseBidding) {
        this.useVerboseBidding = useVerboseBidding;
    }

    public boolean isDoubleVehicles() {
        return doubleVehicles;
    }

    public void setDoubleVehicles(final boolean doubleVehicles) {
        this.doubleVehicles = doubleVehicles;
    }

    public boolean isAdjustPlayerVehicles() {
        return adjustPlayerVehicles;
    }

    public void setAdjustPlayerVehicles(final boolean adjustPlayerVehicles) {
        this.adjustPlayerVehicles = adjustPlayerVehicles;
    }

    public int getOpForLanceTypeMeks() {
        return opForLanceTypeMeks;
    }

    public void setOpForLanceTypeMeks(final int opForLanceTypeMeks) {
        this.opForLanceTypeMeks = opForLanceTypeMeks;
    }

    public int getOpForLanceTypeMixed() {
        return opForLanceTypeMixed;
    }

    public void setOpForLanceTypeMixed(final int opForLanceTypeMixed) {
        this.opForLanceTypeMixed = opForLanceTypeMixed;
    }

    public int getOpForLanceTypeVehicles() {
        return opForLanceTypeVehicles;
    }

    public void setOpForLanceTypeVehicles(final int opForLanceTypeVehicles) {
        this.opForLanceTypeVehicles = opForLanceTypeVehicles;
    }

    public boolean isOpForUsesVTOLs() {
        return opForUsesVTOLs;
    }

    public void setOpForUsesVTOLs(final boolean opForUsesVTOLs) {
        this.opForUsesVTOLs = opForUsesVTOLs;
    }

    public boolean isUseDropShips() {
        return useDropShips;
    }

    public void setUseDropShips(final boolean useDropShips) {
        this.useDropShips = useDropShips;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(final SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public boolean isAeroRecruitsHaveUnits() {
        return aeroRecruitsHaveUnits;
    }

    public void setAeroRecruitsHaveUnits(final boolean aeroRecruitsHaveUnits) {
        this.aeroRecruitsHaveUnits = aeroRecruitsHaveUnits;
    }

    public boolean isUseShareSystem() {
        return useShareSystem;
    }

    public void setUseShareSystem(final boolean useShareSystem) {
        this.useShareSystem = useShareSystem;
    }

    public boolean isSharesForAll() {
        return sharesForAll;
    }

    public void setSharesForAll(final boolean sharesForAll) {
        this.sharesForAll = sharesForAll;
    }

    public boolean isTrackOriginalUnit() {
        return trackOriginalUnit;
    }

    public void setTrackOriginalUnit(final boolean trackOriginalUnit) {
        this.trackOriginalUnit = trackOriginalUnit;
    }

    public boolean isMercSizeLimited() {
        return mercSizeLimited;
    }

    public void setMercSizeLimited(final boolean mercSizeLimited) {
        this.mercSizeLimited = mercSizeLimited;
    }

    public boolean isRegionalMekVariations() {
        return regionalMekVariations;
    }

    public void setRegionalMekVariations(final boolean regionalMekVariations) {
        this.regionalMekVariations = regionalMekVariations;
    }

    public boolean isAttachedPlayerCamouflage() {
        return attachedPlayerCamouflage;
    }

    public void setAttachedPlayerCamouflage(final boolean attachedPlayerCamouflage) {
        this.attachedPlayerCamouflage = attachedPlayerCamouflage;
    }

    public boolean isPlayerControlsAttachedUnits() {
        return playerControlsAttachedUnits;
    }

    public void setPlayerControlsAttachedUnits(final boolean playerControlsAttachedUnits) {
        this.playerControlsAttachedUnits = playerControlsAttachedUnits;
    }

    /**
     * Retrieves the chance of having a battle for the specified {@link CombatRole}.
     * <p>
     * This is a convenience method that calls {@link #getAtBBattleChance(CombatRole, boolean)}
     * with {@code useStratConBypass} set to {@code false}. As a result, if StratCon is enabled,
     * the method will return {@code 0} regardless of other conditions.
     * </p>
     *
     * @param role the {@link CombatRole} to evaluate the battle chance for.
     * @return the chance of having a battle for the specified role.
     *
     * @see #getAtBBattleChance(CombatRole, boolean)
     */
    public int getAtBBattleChance(CombatRole role) {
        return getAtBBattleChance(role, false);
    }

    /**
     * Retrieves the chance of having a battle for the specified {@link CombatRole}.
     * <p>
     * This method calculates the battle chance percentage for the provided combat role based on
     * its ordinal position in the {@code atbBattleChance} array. If StratCon is enabled and the
     * {@code useStratConBypass} parameter is set to {@code true}, the method immediately
     * returns {@code 0}.
     * <p>
     * Combat roles marked as {@link CombatRole#RESERVE} or {@link CombatRole#AUXILIARY} are not
     * eligible for battles and also return {@code 0}.
     *
     * @param role               the {@link CombatRole} to evaluate the battle chance for.
     * @param useStratConBypass  a {@code boolean} indicating whether to bypass the StratCon-check logic.
     *                           If {@code false}, this allows the method to ignore StratCon-enabled status.
     */
    public int getAtBBattleChance(CombatRole role, boolean useStratConBypass) {
        if (useStratCon && useStratConBypass) {
            return 0;
        }

        if (role.isReserve() || role.isAuxiliary()) {
            return 0;
        }

        return atbBattleChance[role.ordinal()];
    }

    /**
     * @param role      the {@link CombatRole} ordinal value
     * @param frequency the frequency to set the generation to (percent chance from
     *                  0 to 100)
     */
    public void setAtBBattleChance(final int role, final int frequency) {
        this.atbBattleChance[role] = MathUtility.clamp(frequency, 0, 100);
    }

    public boolean isGenerateChases() {
        return generateChases;
    }

    public void setGenerateChases(final boolean generateChases) {
        this.generateChases = generateChases;
    }

    public boolean isUseWeatherConditions() {
        return useWeatherConditions;
    }

    public void setUseWeatherConditions(final boolean useWeatherConditions) {
        this.useWeatherConditions = useWeatherConditions;
    }

    public boolean isUseLightConditions() {
        return useLightConditions;
    }

    public void setUseLightConditions(final boolean useLightConditions) {
        this.useLightConditions = useLightConditions;
    }

    public boolean isUsePlanetaryConditions() {
        return usePlanetaryConditions;
    }

    public void setUsePlanetaryConditions(final boolean usePlanetaryConditions) {
        this.usePlanetaryConditions = usePlanetaryConditions;
    }

    public boolean isUseStrategy() {
        return useStrategy;
    }

    public void setUseStrategy(final boolean useStrategy) {
        this.useStrategy = useStrategy;
    }

    public int getBaseStrategyDeployment() {
        return baseStrategyDeployment;
    }

    public void setBaseStrategyDeployment(final int baseStrategyDeployment) {
        this.baseStrategyDeployment = baseStrategyDeployment;
    }

    public int getAdditionalStrategyDeployment() {
        return additionalStrategyDeployment;
    }

    public void setAdditionalStrategyDeployment(final int additionalStrategyDeployment) {
        this.additionalStrategyDeployment = additionalStrategyDeployment;
    }

    public boolean isRestrictPartsByMission() {
        return restrictPartsByMission;
    }

    public void setRestrictPartsByMission(final boolean restrictPartsByMission) {
        this.restrictPartsByMission = restrictPartsByMission;
    }

    public boolean isLimitLanceWeight() {
        return limitLanceWeight;
    }

    public void setLimitLanceWeight(final boolean limitLanceWeight) {
        this.limitLanceWeight = limitLanceWeight;
    }

    public boolean isLimitLanceNumUnits() {
        return limitLanceNumUnits;
    }

    public void setLimitLanceNumUnits(final boolean limitLanceNumUnits) {
        this.limitLanceNumUnits = limitLanceNumUnits;
    }

    public boolean isAllowOpForAeros() {
        return allowOpForAeros;
    }

    public void setAllowOpForAeros(final boolean allowOpForAeros) {
        this.allowOpForAeros = allowOpForAeros;
    }

    public boolean isAllowOpForLocalUnits() {
        return allowOpForLocalUnits;
    }

    public void setAllowOpForLocalUnits(final boolean allowOpForLocalUnits) {
        this.allowOpForLocalUnits = allowOpForLocalUnits;
    }

    public int getOpForAeroChance() {
        return opForAeroChance;
    }

    public void setOpForAeroChance(final int opForAeroChance) {
        this.opForAeroChance = opForAeroChance;
    }

    public int getOpForLocalUnitChance() {
        return opForLocalUnitChance;
    }

    public void setOpForLocalUnitChance(final int opForLocalUnitChance) {
        this.opForLocalUnitChance = opForLocalUnitChance;
    }

    public int getFixedMapChance() {
        return fixedMapChance;
    }

    public void setFixedMapChance(final int fixedMapChance) {
        this.fixedMapChance = fixedMapChance;
    }

    public int getSpaUpgradeIntensity() {
        return spaUpgradeIntensity;
    }

    public void setSpaUpgradeIntensity(final int spaUpgradeIntensity) {
        this.spaUpgradeIntensity = spaUpgradeIntensity;
    }

    public int getScenarioModMax() {
        return scenarioModMax;
    }

    public void setScenarioModMax(final int scenarioModMax) {
        this.scenarioModMax = scenarioModMax;
    }

    public int getScenarioModChance() {
        return scenarioModChance;
    }

    public void setScenarioModChance(final int scenarioModChance) {
        this.scenarioModChance = scenarioModChance;
    }

    public int getScenarioModBV() {
        return scenarioModBV;
    }

    public void setScenarioModBV(final int scenarioModBV) {
        this.scenarioModBV = scenarioModBV;
    }

    public boolean isAutoConfigMunitions() {
        return autoConfigMunitions;
    }

    public void setAutoConfigMunitions(final boolean autoConfigMunitions) {
        this.autoConfigMunitions = autoConfigMunitions;
    }

    // region File IO
    public void writeToXml(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "campaignOptions");
        // region General Tab
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "manualUnitRatingModifier", getManualUnitRatingModifier());
        // endregion General Tab

        // region Repair and Maintenance Tab
        // region Maintenance
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "logMaintenance", logMaintenance);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "defaultMaintenanceTime", defaultMaintenanceTime);
        // endregion Maintenance

        // region Mass Repair / Mass Salvage
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsUseRepair", isMRMSUseRepair());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsUseSalvage", isMRMSUseSalvage());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsUseExtraTime", isMRMSUseExtraTime());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsUseRushJob", isMRMSUseRushJob());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsAllowCarryover", isMRMSAllowCarryover());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsOptimizeToCompleteToday", isMRMSOptimizeToCompleteToday());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsScrapImpossible", isMRMSScrapImpossible());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsUseAssignedTechsFirst", isMRMSUseAssignedTechsFirst());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mrmsReplacePod", isMRMSReplacePod());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "mrmsOptions");
        for (final MRMSOption mrmsOption : getMRMSOptions()) {
            mrmsOption.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "mrmsOptions");
        // endregion Mass Repair / Mass Salvage
        // endregion Repair and Maintenance Tab

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFactionForNames", useOriginFactionForNames);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitRatingMethod", unitRatingMethod.name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useEraMods", useEraMods);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignedTechFirst", assignedTechFirst);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "resetToFirstTech", resetToFirstTech);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useQuirks", useQuirks);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "xpCostMultiplier", xpCostMultiplier);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioXP", scenarioXP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "killsForXP", killsForXP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "killXPAward", killXPAward);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nTasksXP", nTasksXP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "tasksXP", tasksXP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mistakeXP", mistakeXP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "successXP", successXP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vocationalXP", vocationalXP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vocationalXPTargetNumber", vocationalXPTargetNumber);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vocationalXPCheckFrequency", vocationalXPCheckFrequency);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractNegotiationXP", contractNegotiationXP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adminWeeklyXP", adminXP);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adminXPPeriod", adminXPPeriod);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "missionXpFail", missionXpFail);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "missionXpSuccess", missionXpSuccess);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "missionXpOutstandingSuccess", missionXpOutstandingSuccess);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "edgeCost", edgeCost);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "limitByYear", limitByYear);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "disallowExtinctStuff", disallowExtinctStuff);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowClanPurchases", allowClanPurchases);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowISPurchases", allowISPurchases);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowCanonOnly", allowCanonOnly);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowCanonRefitOnly", allowCanonRefitOnly);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "variableTechLevel", variableTechLevel);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "factionIntroDate", factionIntroDate);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAmmoByType", useAmmoByType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "waitingPeriod", waitingPeriod);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "acquisitionSkill", acquisitionSkill);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "acquisitionSupportStaffOnly", acquisitionSupportStaffOnly);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "techLevel", techLevel);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitTransitTime", unitTransitTime);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePlanetaryAcquisition", usePlanetaryAcquisition);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetAcquisitionFactionLimit",
                getPlanetAcquisitionFactionLimit().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetAcquisitionNoClanCrossover",
                planetAcquisitionNoClanCrossover);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "noClanPartsFromIS", noClanPartsFromIS);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "penaltyClanPartsFromIS", penaltyClanPartsFromIS);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetAcquisitionVerbose", planetAcquisitionVerbose);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maxJumpsPlanetaryAcquisition", maxJumpsPlanetaryAcquisition);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentContractPercent", equipmentContractPercent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "dropShipContractPercent", getDropShipContractPercent());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "jumpShipContractPercent", getJumpShipContractPercent());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "warShipContractPercent", getWarShipContractPercent());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentContractBase", equipmentContractBase);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentContractSaleValue", equipmentContractSaleValue);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "blcSaleValue", blcSaleValue);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "overageRepaymentInFinalPayment", overageRepaymentInFinalPayment);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clanAcquisitionPenalty", clanAcquisitionPenalty);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "isAcquisitionPenalty", isAcquisitionPenalty);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "destroyByMargin", destroyByMargin);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "destroyMargin", destroyMargin);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "destroyPartTarget", destroyPartTarget);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAeroSystemHits", useAeroSystemHits);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maintenanceCycleDays", maintenanceCycleDays);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maintenanceBonus", maintenanceBonus);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useQualityMaintenance", useQualityMaintenance);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reverseQualityNames", reverseQualityNames);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomUnitQualities", isUseRandomUnitQualities());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePlanetaryModifiers", isUsePlanetaryModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useUnofficialMaintenance", isUseUnofficialMaintenance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "checkMaintenance", checkMaintenance);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maxAcquisitions", maxAcquisitions);

        // autoLogistics
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsHeatSink", autoLogisticsHeatSink);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsMekHead", autoLogisticsMekHead);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsMekLocation", autoLogisticsMekLocation);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsNonRepairableLocation", autoLogisticsNonRepairableLocation);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsArmor", autoLogisticsArmor);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsAmmunition", autoLogisticsAmmunition);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoLogisticsOther", autoLogisticsOther);

        // region Personnel Tab
        // region General Personnel
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTactics", isUseTactics());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useInitiativeBonus", isUseInitiativeBonus());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useToughness", isUseToughness());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomToughness", isUseRandomToughness());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useArtillery", isUseArtillery());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAbilities", isUseAbilities());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useEdge", isUseEdge());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useSupportEdge", isUseSupportEdge());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useImplants", isUseImplants());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "alternativeQualityAveraging", isAlternativeQualityAveraging());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTransfers", isUseTransfers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useExtendedTOEForceName", isUseExtendedTOEForceName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personnelLogSkillGain", isPersonnelLogSkillGain());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personnelLogAbilityGain", isPersonnelLogAbilityGain());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personnelLogEdgeGain", isPersonnelLogEdgeGain());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayPersonnelLog", isDisplayPersonnelLog());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayScenarioLog", isDisplayScenarioLog());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayKillRecord", isDisplayKillRecord());
        // endregion General Personnel

        // region Expanded Personnel Information
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTimeInService", isUseTimeInService());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "timeInServiceDisplayFormat",
                getTimeInServiceDisplayFormat().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTimeInRank", isUseTimeInRank());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "timeInRankDisplayFormat", getTimeInRankDisplayFormat().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "trackTotalEarnings", isTrackTotalEarnings());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "trackTotalXPEarnings", isTrackTotalXPEarnings());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "showOriginFaction", isShowOriginFaction());
        // endregion Expanded Personnel Information

        // region Admin
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adminsHaveNegotiation", isAdminsHaveNegotiation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adminsHaveScrounge", isAdminsHaveScrounge());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adminExperienceLevelIncludeNegotiation",
                isAdminExperienceLevelIncludeNegotiation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adminExperienceLevelIncludeScrounge",
                isAdminExperienceLevelIncludeScrounge());
        // endregion Admin

        // region Medical
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAdvancedMedical", isUseAdvancedMedical());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "healWaitingPeriod", getHealingWaitingPeriod());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "naturalHealingWaitingPeriod", getNaturalHealingWaitingPeriod());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "minimumHitsForVehicles", getMinimumHitsForVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomHitsForVehicles", isUseRandomHitsForVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "tougherHealing", isTougherHealing());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maximumPatients", getMaximumPatients());
        // endregion Medical

        // region Prisoners
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "prisonerCaptureStyle", getPrisonerCaptureStyle().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "defaultPrisonerStatus", getDefaultPrisonerStatus().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "prisonerBabyStatus", isPrisonerBabyStatus());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAtBPrisonerDefection", isUseAtBPrisonerDefection());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAtBPrisonerRansom", isUseAtBPrisonerRansom());
        // endregion Prisoners

        //region Dependent
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomDependentAddition", isUseRandomDependentAddition());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomDependentRemoval", isUseRandomDependentRemoval());
        // endregion Dependent

        // region Personnel Removal
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePersonnelRemoval", isUsePersonnelRemoval());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRemovalExemptCemetery", isUseRemovalExemptCemetery());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRemovalExemptRetirees", isUseRemovalExemptRetirees());
        // endregion Personnel Removal

        // region Salary
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "disableSecondaryRoleSalary", isDisableSecondaryRoleSalary());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salaryAntiMekMultiplier", getSalaryAntiMekMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salarySpecialistInfantryMultiplier",
                getSalarySpecialistInfantryMultiplier());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "salaryXPMultipliers");
        for (final Entry<SkillLevel, Double> entry : getSalaryXPMultipliers().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "salaryXPMultipliers");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salaryTypeBase", Utilities.printMoneyArray(getRoleBaseSalaries()));
        // endregion Salary

        // region Awards
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "awardBonusStyle", getAwardBonusStyle().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableAutoAwards", isEnableAutoAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "issuePosthumousAwards", isIssuePosthumousAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "issueBestAwardOnly", isIssueBestAwardOnly());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "ignoreStandardSet", isIgnoreStandardSet());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "awardTierSize", getAwardTierSize());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableContractAwards", isEnableContractAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableFactionHunterAwards", isEnableFactionHunterAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableInjuryAwards", isEnableInjuryAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableIndividualKillAwards", isEnableIndividualKillAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableFormationKillAwards", isEnableFormationKillAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableRankAwards", isEnableRankAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableScenarioAwards", isEnableScenarioAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableSkillAwards", isEnableSkillAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableTheatreOfWarAwards", isEnableTheatreOfWarAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableTimeAwards", isEnableTimeAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableTrainingAwards", isEnableTrainingAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableMiscAwards", isEnableMiscAwards());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "awardSetFilterList", getAwardSetFilterList());
        // endregion Awards
        // endregion Personnel Tab

        // region Life Paths Tab
        // region Personnel Randomization
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useDylansRandomXP", isUseDylansRandomXP());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nonBinaryDiceSize", getNonBinaryDiceSize());
       //endregion Personnel Randomization

        //region Random Histories
        getRandomOriginOptions().writeToXML(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomPersonalities", isUseRandomPersonalities());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomPersonalityReputation",
                isUseRandomPersonalityReputation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useIntelligenceXpMultiplier", isUseIntelligenceXpMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useSimulatedRelationships", isUseSimulatedRelationships());
        //endregion Random Histories

        // region Retirement
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomRetirement", isUseRandomRetirement());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "turnoverBaseTn", getTurnoverFixedTargetNumber());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "turnoverFrequency", getTurnoverFrequency().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "aeroRecruitsHaveUnits", isAeroRecruitsHaveUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "trackOriginalUnit", isTrackOriginalUnit());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useContractCompletionRandomRetirement",
                isUseContractCompletionRandomRetirement());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomFounderTurnover", isUseRandomFounderTurnover());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFounderRetirement", isUseFounderRetirement());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useSubContractSoldiers", isUseSubContractSoldiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "serviceContractDuration", getServiceContractDuration());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "serviceContractModifier", getServiceContractModifier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payBonusDefault", isPayBonusDefault());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payBonusDefaultThreshold", getPayBonusDefaultThreshold());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useCustomRetirementModifiers", isUseCustomRetirementModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFatigueModifiers", isUseFatigueModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useSkillModifiers", isUseSkillModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAgeModifiers", isUseAgeModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useUnitRatingModifiers", isUseUnitRatingModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFactionModifiers", isUseFactionModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useMissionStatusModifiers", isUseMissionStatusModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useHostileTerritoryModifiers", isUseHostileTerritoryModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFamilyModifiers", isUseFamilyModifiers());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useLoyaltyModifiers", isUseLoyaltyModifiers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useHideLoyalty", isUseHideLoyalty());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payoutRateOfficer", getPayoutRateOfficer());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payoutRateEnlisted", getPayoutRateEnlisted());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payoutRetirementMultiplier", getPayoutRetirementMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePayoutServiceBonus", isUsePayoutServiceBonus());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payoutServiceBonusRate", getPayoutServiceBonusRate());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAdministrativeStrain", isUseAdministrativeStrain());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "administrativeStrain", getAdministrativeCapacity());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "multiCrewStrainDivider", getMultiCrewStrainDivider());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useManagementSkill", isUseManagementSkill());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useCommanderLeadershipOnly", isUseCommanderLeadershipOnly());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "managementSkillPenalty", getManagementSkillPenalty());

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useFatigue", isUseFatigue());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fatigueRate", getFatigueRate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useInjuryFatigue", isUseInjuryFatigue());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fieldKitchenCapacity", getFieldKitchenCapacity());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fieldKitchenIgnoreNonCombatants",
                isUseFieldKitchenIgnoreNonCombatants());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fatigueLeaveThreshold", getFatigueLeaveThreshold());
        // endregion Retirement

        // region Family
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "familyDisplayLevel", getFamilyDisplayLevel().name());
        // endregion Family

        // region Announcements
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "announceBirthdays", isAnnounceBirthdays());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "announceRecruitmentAnniversaries", isAnnounceRecruitmentAnniversaries());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "announceOfficersOnly", isAnnounceOfficersOnly());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "announceChildBirthdays", isAnnounceChildBirthdays());
        // endregion Announcements

        // region Marriage
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useManualMarriages", isUseManualMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useClanPersonnelMarriages", isUseClanPersonnelMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePrisonerMarriages", isUsePrisonerMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "checkMutualAncestorsDepth", getCheckMutualAncestorsDepth());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "noInterestInMarriageDiceSize", getNoInterestInMarriageDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "logMarriageNameChanges", isLogMarriageNameChanges());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "marriageSurnameWeights");
        for (final Entry<MergingSurnameStyle, Integer> entry : getMarriageSurnameWeights().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "marriageSurnameWeights");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomMarriageMethod", getRandomMarriageMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomClanPersonnelMarriages", isUseRandomClanPersonnelMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomPrisonerMarriages", isUseRandomPrisonerMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomMarriageAgeRange", getRandomMarriageAgeRange());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomMarriageDiceSize", getRandomMarriageDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomSameSexMarriageDiceSize", getRandomSameSexMarriageDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomNewDependentMarriage", getRandomNewDependentMarriage());
        //endregion Marriage

        // region Divorce
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useManualDivorce", isUseManualDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useClanPersonnelDivorce", isUseClanPersonnelDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePrisonerDivorce", isUsePrisonerDivorce());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "divorceSurnameWeights");
        for (final Entry<SplittingSurnameStyle, Integer> entry : getDivorceSurnameWeights().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "divorceSurnameWeights");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomDivorceMethod", getRandomDivorceMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomOppositeSexDivorce", isUseRandomOppositeSexDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomSameSexDivorce", isUseRandomSameSexDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomClanPersonnelDivorce", isUseRandomClanPersonnelDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomPrisonerDivorce", isUseRandomPrisonerDivorce());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomDivorceDiceSize", getRandomDivorceDiceSize());
        //endregion Divorce

        // region Procreation
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useManualProcreation", isUseManualProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useClanPersonnelProcreation", isUseClanPersonnelProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePrisonerProcreation", isUsePrisonerProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "multiplePregnancyOccurrences", getMultiplePregnancyOccurrences());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "babySurnameStyle", getBabySurnameStyle().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignNonPrisonerBabiesFounderTag", isAssignNonPrisonerBabiesFounderTag());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignChildrenOfFoundersFounderTag", isAssignChildrenOfFoundersFounderTag());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useMaternityLeave", isUseMaternityLeave());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "determineFatherAtBirth", isDetermineFatherAtBirth());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "displayTrueDueDate", isDisplayTrueDueDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "noInterestInChildrenDiceSize", getNoInterestInChildrenDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "logProcreation", isLogProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomProcreationMethod", getRandomProcreationMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRelationshiplessRandomProcreation",
                isUseRelationshiplessRandomProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomClanPersonnelProcreation",
                isUseRandomClanPersonnelProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomPrisonerProcreation", isUseRandomPrisonerProcreation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomProcreationRelationshipDiceSize", getRandomProcreationRelationshipDiceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomProcreationRelationshiplessDiceSize", getRandomProcreationRelationshiplessDiceSize());
        //endregion Procreation

        // region Education
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useEducationModule", isUseEducationModule());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "curriculumXpRate", getCurriculumXpRate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maximumJumpCount", getMaximumJumpCount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useReeducationCamps", isUseReeducationCamps());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableLocalAcademies", isEnableLocalAcademies());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enablePrestigiousAcademies", isEnablePrestigiousAcademies());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableUnitEducation", isEnableUnitEducation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableOverrideRequirements", isEnableOverrideRequirements());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableShowIneligibleAcademies", isEnableShowIneligibleAcademies());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "entranceExamBaseTargetNumber", getEntranceExamBaseTargetNumber());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "facultyXpRate", getFacultyXpRate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enableBonuses", isEnableBonuses());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adultDropoutChance", getAdultDropoutChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "childrenDropoutChance", getChildrenDropoutChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allAges", isAllAges());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "militaryAcademyAccidents", getMilitaryAcademyAccidents());
        // endregion Education

        // region Death
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "enabledRandomDeathAgeGroups");
        for (final Entry<AgeGroup, Boolean> entry : getEnabledRandomDeathAgeGroups().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "enabledRandomDeathAgeGroups");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useRandomDeathSuicideCause", isUseRandomDeathSuicideCause());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomDeathMultiplier", getRandomDeathMultiplier());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "ageRangeRandomDeathMaleValues");
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "ageRangeRandomDeathMaleValues");
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "ageRangeRandomDeathFemaleValues");
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "ageRangeRandomDeathFemaleValues");
        // endregion Death
        // endregion Life Paths Tab

        // region Finances Tab
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForParts", payForParts);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForRepairs", payForRepairs);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForUnits", payForUnits);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForSalaries", payForSalaries);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForOverhead", payForOverhead);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForMaintain", payForMaintain);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForTransport", payForTransport);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sellUnits", sellUnits);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sellParts", sellParts);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForRecruitment", payForRecruitment);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useLoanLimits", useLoanLimits);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePercentageMaint", usePercentageMaint);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "infantryDontCount", infantryDontCount);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePeacetimeCost", usePeacetimeCost);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useExtendedPartsModifier", useExtendedPartsModifier);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "showPeacetimeCost", showPeacetimeCost);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "financialYearDuration", financialYearDuration.name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "newFinancialYearFinancesToCSVExport",
                newFinancialYearFinancesToCSVExport);

        // region Price Multipliers
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commonPartPriceMultiplier", getCommonPartPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "innerSphereUnitPriceMultiplier",
                getInnerSphereUnitPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "innerSpherePartPriceMultiplier",
                getInnerSpherePartPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clanUnitPriceMultiplier", getClanUnitPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clanPartPriceMultiplier", getClanPartPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mixedTechUnitPriceMultiplier", getMixedTechUnitPriceMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usedPartPriceMultipliers", getUsedPartPriceMultipliers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "damagedPartsValueMultiplier", getDamagedPartsValueMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unrepairablePartsValueMultiplier",
                getUnrepairablePartsValueMultiplier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "cancelledOrderRefundMultiplier",
                getCancelledOrderRefundMultiplier());

        // Shares
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useShareSystem", isUseShareSystem());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sharesForAll", isSharesForAll());
        // endregion Price Multipliers

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useTaxes", isUseTaxes());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "taxesPercentage", getTaxesPercentage());
        // region Taxes
        // endregion Taxes
        // endregion Finances Tab

        // region Markets Tab
        // region Personnel Market
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personnelMarketName", getPersonnelMarketName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personnelMarketReportRefresh", isPersonnelMarketReportRefresh());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "personnelMarketRandomRemovalTargets");
        for (final Entry<SkillLevel, Integer> entry : getPersonnelMarketRandomRemovalTargets().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "personnelMarketRandomRemovalTargets");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personnelMarketDylansWeight", getPersonnelMarketDylansWeight());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePersonnelHireHiringHallOnly",
                isUsePersonnelHireHiringHallOnly());
        // endregion Personnel Market

        // region Unit Market
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitMarketMethod", getUnitMarketMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitMarketRegionalMekVariations",
                isUnitMarketRegionalMekVariations());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitMarketSpecialUnitChance", getUnitMarketSpecialUnitChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitMarketRarityModifier", getUnitMarketRarityModifier());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "instantUnitMarketDelivery", isInstantUnitMarketDelivery());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitMarketReportRefresh", isUnitMarketReportRefresh());
        // endregion Unit Market

        // region Contract Market
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractMarketMethod", getContractMarketMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractSearchRadius", getContractSearchRadius());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "variableContractLength", isVariableContractLength());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractMarketReportRefresh", isContractMarketReportRefresh());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractMaxSalvagePercentage", getContractMaxSalvagePercentage());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "dropShipBonusPercentage", getDropShipBonusPercentage());
        // endregion Contract Market
        // endregion Markets Tab

        // region RATs Tab
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useStaticRATs", isUseStaticRATs());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rats", getRATs());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "ignoreRATEra", isIgnoreRATEra());
        // endregion RATs Tab

        // region AtB Tab
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "skillLevel", getSkillLevel().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoResolveMethod", getAutoResolveMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoResolveVictoryChanceEnabled", isAutoResolveVictoryChanceEnabled());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoResolveNumberOfScenarios", getAutoResolveNumberOfScenarios());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoResolveUseExperimentalPacarGui", isAutoResolveExperimentalPacarGuiEnabled());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "strategicViewTheme", getStrategicViewTheme().getName());
        // endregion AtB Tab

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "phenotypeProbabilities", phenotypeProbabilities);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAtB", useAtB);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useStratCon", useStratCon);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useAero", useAero);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useVehicles", useVehicles);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clanVehicles", clanVehicles);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useGenericBattleValue", useGenericBattleValue);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useVerboseBidding", useVerboseBidding);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "doubleVehicles", doubleVehicles);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "adjustPlayerVehicles", adjustPlayerVehicles);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForLanceTypeMeks", getOpForLanceTypeMeks());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForLanceTypeMixed", getOpForLanceTypeMixed());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForLanceTypeVehicles", getOpForLanceTypeVehicles());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForUsesVTOLs", isOpForUsesVTOLs());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useDropShips", useDropShips);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "mercSizeLimited", mercSizeLimited);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "regionalMekVariations", regionalMekVariations);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "attachedPlayerCamouflage", attachedPlayerCamouflage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "playerControlsAttachedUnits", playerControlsAttachedUnits);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "atbBattleChance", atbBattleChance);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateChases", generateChases);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useWeatherConditions", useWeatherConditions);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useLightConditions", useLightConditions);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePlanetaryConditions", usePlanetaryConditions);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useStrategy", useStrategy);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "baseStrategyDeployment", baseStrategyDeployment);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "additionalStrategyDeployment", additionalStrategyDeployment);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "restrictPartsByMission", restrictPartsByMission);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "limitLanceWeight", limitLanceWeight);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "limitLanceNumUnits", limitLanceNumUnits);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignPortraitOnRoleChange", assignPortraitOnRoleChange);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowOpForAeros", isAllowOpForAeros());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allowOpForLocalUnits", isAllowOpForLocalUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForAeroChance", getOpForAeroChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opForLocalUnitChance", getOpForLocalUnitChance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fixedMapChance", fixedMapChance);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "spaUpgradeIntensity", spaUpgradeIntensity);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioModMax", scenarioModMax);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioModChance", scenarioModChance);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "scenarioModBV", scenarioModBV);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoConfigMunitions", autoConfigMunitions);

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetTechAcquisitionBonus", planetTechAcquisitionBonus);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetIndustryAcquisitionBonus", planetIndustryAcquisitionBonus);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "planetOutputAcquisitionBonus", planetOutputAcquisitionBonus);

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "usePortraitForType", isUsePortraitForRoles());

        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "campaignOptions");
    }

    public static CampaignOptions generateCampaignOptionsFromXml(Node wn, Version version) {
        logger.info("Loading Campaign Options from Version {} XML...", version);

        wn.normalize();
        CampaignOptions retVal = new CampaignOptions();
        NodeList wList = wn.getChildNodes();

        // Okay, let's iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            logger.debug("{}\n\t{}", wn2.getNodeName(), wn2.getTextContent());
            try {
                // region Repair and Maintenance Tab
                if (wn2.getNodeName().equalsIgnoreCase("checkMaintenance")) {
                    retVal.checkMaintenance = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("maintenanceCycleDays")) {
                    retVal.maintenanceCycleDays = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("maintenanceBonus")) {
                    retVal.maintenanceBonus = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useQualityMaintenance")) {
                    retVal.useQualityMaintenance = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("reverseQualityNames")) {
                    retVal.reverseQualityNames = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomUnitQualities")) {
                    retVal.setUseRandomUnitQualities(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("usePlanetaryModifiers")) {
                    retVal.setUsePlanetaryModifiers(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useUnofficialMaintenance")) {
                    retVal.setUseUnofficialMaintenance(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("logMaintenance")) {
                    retVal.logMaintenance = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("defaultMaintenanceTime")) {
                    retVal.defaultMaintenanceTime = Integer.parseInt(wn2.getTextContent());

                    // region Mass Repair / Mass Salvage
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsUseRepair")) {
                    retVal.setMRMSUseRepair(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsUseSalvage")) {
                    retVal.setMRMSUseSalvage(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsUseExtraTime")) {
                    retVal.setMRMSUseExtraTime(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsUseRushJob")) {
                    retVal.setMRMSUseRushJob(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsAllowCarryover")) {
                    retVal.setMRMSAllowCarryover(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsOptimizeToCompleteToday")) {
                    retVal.setMRMSOptimizeToCompleteToday(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsScrapImpossible")) {
                    retVal.setMRMSScrapImpossible(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsUseAssignedTechsFirst")) {
                    retVal.setMRMSUseAssignedTechsFirst(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsReplacePod")) {
                    retVal.setMRMSReplacePod(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mrmsOptions")) {
                    retVal.setMRMSOptions(MRMSOption.parseListFromXML(wn2, version));
                    // endregion Mass Repair / Mass Salvage
                    // endregion Repair and Maintenance Tab

                } else if (wn2.getNodeName().equalsIgnoreCase("useFactionForNames")) {
                    retVal.setUseOriginFactionForNames(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useEraMods")) {
                    retVal.useEraMods = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("assignedTechFirst")) {
                    retVal.assignedTechFirst = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("resetToFirstTech")) {
                    retVal.resetToFirstTech = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("useQuirks")) {
                    retVal.useQuirks = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("xpCostMultiplier")) {
                    retVal.xpCostMultiplier = Double.parseDouble(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioXP")) {
                    retVal.scenarioXP = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("killsForXP")) {
                    retVal.killsForXP = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("killXPAward")) {
                    retVal.killXPAward = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("nTasksXP")) {
                    retVal.nTasksXP = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("tasksXP")) {
                    retVal.tasksXP = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("successXP")) {
                    retVal.successXP = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("mistakeXP")) {
                    retVal.mistakeXP = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("vocationalXP")
                    // <50.03 compatibility handler
                    || wn2.getNodeName().equalsIgnoreCase("idleXP")) {
                    retVal.vocationalXP = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("vocationalXPTargetNumber")
                    // <50.03 compatibility handler
                    || wn2.getNodeName().equalsIgnoreCase("targetIdleXP")) {
                    retVal.vocationalXPTargetNumber = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("vocationalXPCheckFrequency")
                    // <50.03 compatibility handler
                    || wn2.getNodeName().equalsIgnoreCase("monthsIdleXP")) {
                    retVal.vocationalXPCheckFrequency = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("contractNegotiationXP")) {
                    retVal.contractNegotiationXP = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("adminWeeklyXP")) {
                    retVal.adminXP = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("adminXPPeriod")) {
                    retVal.adminXPPeriod = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("missionXpFail")) {
                    retVal.missionXpFail = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("missionXpSuccess")) {
                    retVal.missionXpSuccess = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("missionXpOutstandingSuccess")) {
                    retVal.missionXpOutstandingSuccess = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("edgeCost")) {
                    retVal.edgeCost = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("waitingPeriod")) {
                    retVal.waitingPeriod = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("acquisitionSkill")) {
                    retVal.acquisitionSkill = wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("unitTransitTime")) {
                    retVal.unitTransitTime = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("clanAcquisitionPenalty")) {
                    retVal.clanAcquisitionPenalty = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("isAcquisitionPenalty")) {
                    retVal.isAcquisitionPenalty = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("usePlanetaryAcquisition")) {
                    retVal.usePlanetaryAcquisition = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("planetAcquisitionFactionLimit")) {
                    retVal.setPlanetAcquisitionFactionLimit(
                            PlanetaryAcquisitionFactionLimit.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("planetAcquisitionNoClanCrossover")) {
                    retVal.planetAcquisitionNoClanCrossover = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("noClanPartsFromIS")) {
                    retVal.noClanPartsFromIS = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("penaltyClanPartsFromIS")) {
                    retVal.penaltyClanPartsFromIS = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("planetAcquisitionVerbose")) {
                    retVal.planetAcquisitionVerbose = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("maxJumpsPlanetaryAcquisition")) {
                    retVal.maxJumpsPlanetaryAcquisition = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("planetTechAcquisitionBonus")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.planetTechAcquisitionBonus[i] = Integer.parseInt(values[i]);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("planetIndustryAcquisitionBonus")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.planetIndustryAcquisitionBonus[i] = Integer.parseInt(values[i]);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("planetOutputAcquisitionBonus")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.planetOutputAcquisitionBonus[i] = Integer.parseInt(values[i]);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("equipmentContractPercent")) {
                    retVal.setEquipmentContractPercent(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("dropShipContractPercent")) {
                    retVal.setDropShipContractPercent(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("jumpShipContractPercent")) {
                    retVal.setJumpShipContractPercent(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("warShipContractPercent")) {
                    retVal.setWarShipContractPercent(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("equipmentContractBase")) {
                    retVal.equipmentContractBase = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("equipmentContractSaleValue")) {
                    retVal.equipmentContractSaleValue = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("blcSaleValue")) {
                    retVal.blcSaleValue = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("overageRepaymentInFinalPayment")) {
                    retVal.setOverageRepaymentInFinalPayment(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("acquisitionSupportStaffOnly")) {
                    retVal.acquisitionSupportStaffOnly = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("limitByYear")) {
                    retVal.limitByYear = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("disallowExtinctStuff")) {
                    retVal.disallowExtinctStuff = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("allowClanPurchases")) {
                    retVal.allowClanPurchases = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("allowISPurchases")) {
                    retVal.allowISPurchases = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("allowCanonOnly")) {
                    retVal.allowCanonOnly = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("allowCanonRefitOnly")) {
                    retVal.allowCanonRefitOnly = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useAmmoByType")) {
                    retVal.useAmmoByType = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("variableTechLevel")) {
                    retVal.variableTechLevel = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("factionIntroDate")) {
                    retVal.factionIntroDate = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("techLevel")) {
                    retVal.techLevel = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("unitRatingMethod")
                        || wn2.getNodeName().equalsIgnoreCase("dragoonsRatingMethod")) {
                    retVal.setUnitRatingMethod(UnitRatingMethod.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("manualUnitRatingModifier")) {
                    retVal.setManualUnitRatingModifier(Integer.parseInt(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("usePortraitForType")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.setUsePortraitForRole(i, Boolean.parseBoolean(values[i].trim()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("assignPortraitOnRoleChange")) {
                    retVal.assignPortraitOnRoleChange = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("destroyByMargin")) {
                    retVal.destroyByMargin = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("destroyMargin")) {
                    retVal.destroyMargin = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("destroyPartTarget")) {
                    retVal.destroyPartTarget = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useAeroSystemHits")) {
                    retVal.useAeroSystemHits = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("maxAcquisitions")) {
                    retVal.maxAcquisitions = Integer.parseInt(wn2.getTextContent().trim());

                    // autoLogistics
                } else if (wn2.getNodeName().equalsIgnoreCase("autoLogisticsHeatSink")) {
                    retVal.autoLogisticsHeatSink = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("autoLogisticsMekHead")) {
                    retVal.autoLogisticsMekHead = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("autoLogisticsMekLocation")) {
                    retVal.autoLogisticsMekLocation = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("autoLogisticsNonRepairableLocation")) {
                    retVal.autoLogisticsNonRepairableLocation = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("autoLogisticsArmor")) {
                    retVal.autoLogisticsArmor = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("autoLogisticsAmmunition")) {
                    retVal.autoLogisticsAmmunition = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("autoLogisticsOther")) {
                    retVal.autoLogisticsOther = Integer.parseInt(wn2.getTextContent().trim());

                    // region Personnel Tab
                    // region General Personnel
                } else if (wn2.getNodeName().equalsIgnoreCase("useTactics")) {
                    retVal.setUseTactics(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useInitiativeBonus")) {
                    retVal.setUseInitiativeBonus(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useToughness")) {
                    retVal.setUseToughness(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomToughness")) {
                    retVal.setUseRandomToughness(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useArtillery")) {
                    retVal.setUseArtillery(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useAbilities")) {
                    retVal.setUseAbilities(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useEdge")) {
                    retVal.setUseEdge(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useSupportEdge")) {
                    retVal.setUseSupportEdge(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useImplants")) {
                    retVal.setUseImplants(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("alternativeQualityAveraging")) {
                    retVal.setAlternativeQualityAveraging(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useTransfers")) {
                    retVal.setUseTransfers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useExtendedTOEForceName")) {
                    retVal.setUseExtendedTOEForceName(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelLogSkillGain")) {
                    retVal.setPersonnelLogSkillGain(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelLogAbilityGain")) {
                    retVal.setPersonnelLogAbilityGain(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelLogEdgeGain")) {
                    retVal.setPersonnelLogEdgeGain(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("displayPersonnelLog")) {
                    retVal.setDisplayPersonnelLog(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("displayScenarioLog")) {
                    retVal.setDisplayScenarioLog(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("displayKillRecord")) {
                    retVal.setDisplayKillRecord(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    // endregion General Personnel

                    // region Expanded Personnel Information
                } else if (wn2.getNodeName().equalsIgnoreCase("useTimeInService")) {
                    retVal.setUseTimeInService(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("timeInServiceDisplayFormat")) {
                    retVal.setTimeInServiceDisplayFormat(TimeInDisplayFormat.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useTimeInRank")) {
                    retVal.setUseTimeInRank(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("timeInRankDisplayFormat")) {
                    retVal.setTimeInRankDisplayFormat(TimeInDisplayFormat.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("trackTotalEarnings")) {
                    retVal.setTrackTotalEarnings(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("trackTotalXPEarnings")) {
                    retVal.setTrackTotalXPEarnings(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("showOriginFaction")) {
                    retVal.setShowOriginFaction(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("adminsHaveNegotiation")) {
                    retVal.setAdminsHaveNegotiation(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("adminsHaveScrounge")) {
                    retVal.setAdminsHaveScrounge(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("adminExperienceLevelIncludeNegotiation")) {
                    retVal.setAdminExperienceLevelIncludeNegotiation(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("adminExperienceLevelIncludeScrounge")) {
                    retVal.setAdminExperienceLevelIncludeScrounge(Boolean.parseBoolean(wn2.getTextContent()));
                    // endregion Expanded Personnel Information

                    // region Medical
                } else if (wn2.getNodeName().equalsIgnoreCase("useAdvancedMedical")) {
                    retVal.setUseAdvancedMedical(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("healWaitingPeriod")) {
                    retVal.setHealingWaitingPeriod(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("naturalHealingWaitingPeriod")) {
                    retVal.setNaturalHealingWaitingPeriod(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("minimumHitsForVehicles")) {
                    retVal.setMinimumHitsForVehicles(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomHitsForVehicles")) {
                    retVal.setUseRandomHitsForVehicles(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("tougherHealing")) {
                    retVal.setTougherHealing(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("maximumPatients")) {
                    retVal.setMaximumPatients(Integer.parseInt(wn2.getTextContent().trim()));
                    // endregion Medical

                    // region Prisoners
                } else if (wn2.getNodeName().equalsIgnoreCase("prisonerCaptureStyle")) {
                    retVal.setPrisonerCaptureStyle(PrisonerCaptureStyle.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("defaultPrisonerStatus")) {
                    // Most of this is legacy handlers - 0.47.X Removal
                    String prisonerStatus = wn2.getTextContent().trim();

                    try {
                        prisonerStatus = String.valueOf(Integer.parseInt(prisonerStatus) + 1);
                    } catch (Exception ignored) {

                    }

                    retVal.setDefaultPrisonerStatus(PrisonerStatus.parseFromString(prisonerStatus));
                } else if (wn2.getNodeName().equalsIgnoreCase("prisonerBabyStatus")) {
                    retVal.setPrisonerBabyStatus(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useAtBPrisonerDefection")) {
                    retVal.setUseAtBPrisonerDefection(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useAtBPrisonerRansom")) {
                    retVal.setUseAtBPrisonerRansom(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    // endregion Prisoners

                    //region Dependent
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomDependentAddition")) {
                    retVal.setUseRandomDependentAddition(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomDependentRemoval")) {
                    retVal.setUseRandomDependentRemoval(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    // endregion Dependent

                    // region Personnel Removal
                } else if (wn2.getNodeName().equalsIgnoreCase("usePersonnelRemoval")) {
                    retVal.setUsePersonnelRemoval(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRemovalExemptCemetery")) {
                    retVal.setUseRemovalExemptCemetery(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRemovalExemptRetirees")) {
                    retVal.setUseRemovalExemptRetirees(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    // endregion Personnel Removal

                    // region Salary
                } else if (wn2.getNodeName().equalsIgnoreCase("disableSecondaryRoleSalary")) {
                    retVal.setDisableSecondaryRoleSalary(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("salaryAntiMekMultiplier")) {
                    retVal.setSalaryAntiMekMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("salarySpecialistInfantryMultiplier")) {
                    retVal.setSalarySpecialistInfantryMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("salaryXPMultipliers")) {
                    if (!wn2.hasChildNodes()) {
                        continue;
                    }
                    final NodeList nl2 = wn2.getChildNodes();
                    for (int j = 0; j < nl2.getLength(); j++) {
                        final Node wn3 = nl2.item(j);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        retVal.getSalaryXPMultipliers().put(
                                SkillLevel.valueOf(wn3.getNodeName().trim()),
                                Double.parseDouble(wn3.getTextContent().trim()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("salaryTypeBase")) {
                    retVal.setRoleBaseSalaries(Utilities.readMoneyArray(wn2, retVal.getRoleBaseSalaries().length));
                    // endregion Salary

                    // region Awards
                } else if (wn2.getNodeName().equalsIgnoreCase("awardBonusStyle")) {
                    retVal.setAwardBonusStyle(AwardBonus.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableAutoAwards")) {
                    retVal.setEnableAutoAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("issuePosthumousAwards")) {
                    retVal.setIssuePosthumousAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("issueBestAwardOnly")) {
                    retVal.setIssueBestAwardOnly(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("ignoreStandardSet")) {
                    retVal.setIgnoreStandardSet(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("awardTierSize")) {
                    retVal.setAwardTierSize(Integer.parseInt(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableContractAwards")) {
                    retVal.setEnableContractAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableFactionHunterAwards")) {
                    retVal.setEnableFactionHunterAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableInjuryAwards")) {
                    retVal.setEnableInjuryAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableIndividualKillAwards")) {
                    retVal.setEnableIndividualKillAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableFormationKillAwards")) {
                    retVal.setEnableFormationKillAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableRankAwards")) {
                    retVal.setEnableRankAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableScenarioAwards")) {
                    retVal.setEnableScenarioAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableSkillAwards")) {
                    retVal.setEnableSkillAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableTheatreOfWarAwards")) {
                    retVal.setEnableTheatreOfWarAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableTimeAwards")) {
                    retVal.setEnableTimeAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableTrainingAwards")) {
                    retVal.setEnableTrainingAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableMiscAwards")) {
                    retVal.setEnableMiscAwards(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("awardSetFilterList")) {
                    retVal.setAwardSetFilterList(wn2.getTextContent().trim());
                    // endregion Awards
                    // endregion Personnel Tab

                    // region Life Paths Tab
                    // region Personnel Randomization
                } else if (wn2.getNodeName().equalsIgnoreCase("useDylansRandomXP")) {
                    retVal.setUseDylansRandomXP(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("nonBinaryDiceSize")) {
                    retVal.setNonBinaryDiceSize(Integer.parseInt(wn2.getTextContent().trim()));
                //endregion Personnel Randomization

                    //region Random Histories
                } else if (wn2.getNodeName().equalsIgnoreCase("randomOriginOptions")) {
                    if (!wn2.hasChildNodes()) {
                        continue;
                    }
                    final RandomOriginOptions randomOriginOptions = RandomOriginOptions
                            .parseFromXML(wn2.getChildNodes(), true);
                    if (randomOriginOptions == null) {
                        continue;
                    }
                    retVal.setRandomOriginOptions(randomOriginOptions);
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomPersonalities")) {
                    retVal.setUseRandomPersonalities(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomPersonalityReputation")) {
                    retVal.setUseRandomPersonalityReputation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useIntelligenceXpMultiplier")) {
                    retVal.setUseIntelligenceXpMultiplier(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useSimulatedRelationships")) {
                    retVal.setUseSimulatedRelationships(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    //endregion Random Histories

                    // region Family
                } else if (wn2.getNodeName().equalsIgnoreCase("familyDisplayLevel")) {
                    retVal.setFamilyDisplayLevel(
                            FamilialRelationshipDisplayLevel.parseFromString(wn2.getTextContent().trim()));
                    // endregion Family

                    // region anniversaries
                } else if (wn2.getNodeName().equalsIgnoreCase("announceBirthdays")) {
                    retVal.setAnnounceBirthdays(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("announceRecruitmentAnniversaries")) {
                    retVal.setAnnounceRecruitmentAnniversaries(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("announceOfficersOnly")) {
                    retVal.setAnnounceOfficersOnly(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("announceChildBirthdays")) {
                    retVal.setAnnounceChildBirthdays(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    // endregion anniversaries

                    // region Marriage
                } else if (wn2.getNodeName().equalsIgnoreCase("useManualMarriages")) {
                    retVal.setUseManualMarriages(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useClanPersonnelMarriages")) {
                    retVal.setUseClanPersonnelMarriages(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("usePrisonerMarriages")) {
                    retVal.setUsePrisonerMarriages(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("checkMutualAncestorsDepth")) {
                    retVal.setCheckMutualAncestorsDepth(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("noInterestInMarriageDiceSize")) {
                    retVal.setNoInterestInMarriageDiceSize(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("logMarriageNameChanges")) {
                    retVal.setLogMarriageNameChanges(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("marriageSurnameWeights")) {
                    if (!wn2.hasChildNodes()) {
                        continue;
                    }
                    final NodeList nl2 = wn2.getChildNodes();
                    for (int j = 0; j < nl2.getLength(); j++) {
                        final Node wn3 = nl2.item(j);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        retVal.getMarriageSurnameWeights().put(
                                MergingSurnameStyle.parseFromString(wn3.getNodeName().trim()),
                                Integer.parseInt(wn3.getTextContent().trim()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("randomMarriageMethod")) {
                    retVal.setRandomMarriageMethod(RandomMarriageMethod.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomClanPersonnelMarriages")
                        || wn2.getNodeName().equalsIgnoreCase("useRandomClannerMarriages")) { // Legacy, 0.49.12 removal
                    retVal.setUseRandomClanPersonnelMarriages(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomPrisonerMarriages")) {
                    retVal.setUseRandomPrisonerMarriages(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomMarriageAgeRange")) {
                    retVal.setRandomMarriageAgeRange(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomMarriageDiceSize")) {
                    retVal.setRandomMarriageDiceSize(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomSameSexMarriageDiceSize")) {
                    retVal.setRandomSameSexMarriageDiceSize(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomSameSexMarriages")) { // Legacy, pre-50.01
                    if (!Boolean.parseBoolean(wn2.getTextContent().trim())) {
                        retVal.setRandomSameSexMarriageDiceSize(0);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("randomNewDependentMarriage")) {
                    retVal.setRandomNewDependentMarriage(Integer.parseInt(wn2.getTextContent().trim()));
                    //endregion Marriage

                    // region Divorce
                } else if (wn2.getNodeName().equalsIgnoreCase("useManualDivorce")) {
                    retVal.setUseManualDivorce(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useClanPersonnelDivorce")) {
                    retVal.setUseClanPersonnelDivorce(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("usePrisonerDivorce")) {
                    retVal.setUsePrisonerDivorce(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("divorceSurnameWeights")) {
                    if (!wn2.hasChildNodes()) {
                        continue;
                    }
                    final NodeList nl2 = wn2.getChildNodes();
                    for (int j = 0; j < nl2.getLength(); j++) {
                        final Node wn3 = nl2.item(j);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        retVal.getDivorceSurnameWeights().put(
                                SplittingSurnameStyle.valueOf(wn3.getNodeName().trim()),
                                Integer.parseInt(wn3.getTextContent().trim()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("randomDivorceMethod")) {
                    retVal.setRandomDivorceMethod(RandomDivorceMethod.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomOppositeSexDivorce")) {
                    retVal.setUseRandomOppositeSexDivorce(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomSameSexDivorce")) {
                    retVal.setUseRandomSameSexDivorce(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomClanPersonnelDivorce")) {
                    retVal.setUseRandomClanPersonnelDivorce(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomPrisonerDivorce")) {
                    retVal.setUseRandomPrisonerDivorce(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomDivorceDiceSize")) {
                    retVal.setRandomDivorceDiceSize(Integer.parseInt(wn2.getTextContent().trim()));
                    //endregion Divorce

                    // region Procreation
                } else if (wn2.getNodeName().equalsIgnoreCase("useManualProcreation")) {
                    retVal.setUseManualProcreation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useClanPersonnelProcreation")) {
                    retVal.setUseClanPersonnelProcreation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("usePrisonerProcreation")) {
                    retVal.setUsePrisonerProcreation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("multiplePregnancyOccurrences")) {
                    retVal.setMultiplePregnancyOccurrences(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("babySurnameStyle")) {
                    retVal.setBabySurnameStyle(BabySurnameStyle.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("assignNonPrisonerBabiesFounderTag")) {
                    retVal.setAssignNonPrisonerBabiesFounderTag(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("assignChildrenOfFoundersFounderTag")) {
                    retVal.setAssignChildrenOfFoundersFounderTag(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useMaternityLeave")) {
                    retVal.setUseMaternityLeave(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("determineFatherAtBirth")) {
                    retVal.setDetermineFatherAtBirth(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("displayTrueDueDate")) {
                    retVal.setDisplayTrueDueDate(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("noInterestInChildrenDiceSize")) {
                    retVal.setNoInterestInChildrenDiceSize(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("logProcreation")) {
                    retVal.setLogProcreation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomProcreationMethod")) {
                    retVal.setRandomProcreationMethod(RandomProcreationMethod.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRelationshiplessRandomProcreation")) {
                    retVal.setUseRelationshiplessRandomProcreation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomClanPersonnelProcreation")) {
                    retVal.setUseRandomClanPersonnelProcreation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomPrisonerProcreation")) {
                    retVal.setUseRandomPrisonerProcreation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomProcreationRelationshipDiceSize")) {
                    retVal.setRandomProcreationRelationshipDiceSize(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomProcreationRelationshiplessDiceSize")) {
                    retVal.setRandomProcreationRelationshiplessDiceSize(Integer.parseInt(wn2.getTextContent().trim()));
                    //endregion Procreation

                    // region Education
                } else if (wn2.getNodeName().equalsIgnoreCase("useEducationModule")) {
                    retVal.setUseEducationModule(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("curriculumXpRate")) {
                    retVal.setCurriculumXpRate(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("maximumJumpCount")) {
                    retVal.setMaximumJumpCount(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useReeducationCamps")) {
                    retVal.setUseReeducationCamps(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableLocalAcademies")) {
                    retVal.setEnableLocalAcademies(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enablePrestigiousAcademies")) {
                    retVal.setEnablePrestigiousAcademies(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableUnitEducation")) {
                    retVal.setEnableUnitEducation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableOverrideRequirements")) {
                    retVal.setEnableOverrideRequirements(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableShowIneligibleAcademies")) {
                    retVal.setEnableShowIneligibleAcademies(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("entranceExamBaseTargetNumber")) {
                    retVal.setEntranceExamBaseTargetNumber(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("facultyXpRate")) {
                    retVal.setFacultyXpRate(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enableBonuses")) {
                    retVal.setEnableBonuses(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("adultDropoutChance")) {
                    retVal.setAdultDropoutChance(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("childrenDropoutChance")) {
                    retVal.setChildrenDropoutChance(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("allAges")) {
                    retVal.setAllAges(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("militaryAcademyAccidents")) {
                    retVal.setMilitaryAcademyAccidents(Integer.parseInt(wn2.getTextContent().trim()));
                    // endregion Education
                } else if (wn2.getNodeName().equalsIgnoreCase("enabledRandomDeathAgeGroups")) {
                    if (!wn2.hasChildNodes()) {
                        continue;
                    }
                    final NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        final Node wn3 = nl2.item(i);
                        try {
                            retVal.getEnabledRandomDeathAgeGroups().put(
                                    AgeGroup.valueOf(wn3.getNodeName()),
                                    Boolean.parseBoolean(wn3.getTextContent().trim()));
                        } catch (Exception ignored) {

                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomDeathSuicideCause")) {
                    retVal.setUseRandomDeathSuicideCause(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomDeathMultiplier")) {
                    retVal.setRandomDeathMultiplier(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomRetirement")) {
                    retVal.setUseRandomRetirement(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("turnoverBaseTn")) {
                    retVal.setTurnoverFixedTargetNumber(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("turnoverFrequency")) {
                    retVal.setTurnoverFrequency(TurnoverFrequency.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("trackOriginalUnit")) {
                    retVal.setTrackOriginalUnit(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("aeroRecruitsHaveUnits")) {
                    retVal.setAeroRecruitsHaveUnits(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useContractCompletionRandomRetirement")) {
                    retVal.setUseContractCompletionRandomRetirement(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomFounderTurnover")) {
                    retVal.setUseRandomFounderTurnover(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useFounderRetirement")) {
                    retVal.setUseFounderRetirement(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useSubContractSoldiers")) {
                    retVal.setUseSubContractSoldiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("serviceContractDuration")) {
                    retVal.setServiceContractDuration(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("serviceContractModifier")) {
                    retVal.setServiceContractModifier(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("payBonusDefault")) {
                    retVal.setPayBonusDefault(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("payBonusDefaultThreshold")) {
                    retVal.setPayBonusDefaultThreshold(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useCustomRetirementModifiers")) {
                    retVal.setUseCustomRetirementModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useFatigueModifiers")) {
                    retVal.setUseFatigueModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useSkillModifiers")) {
                    retVal.setUseSkillModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useAgeModifiers")) {
                    retVal.setUseAgeModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useUnitRatingModifiers")) {
                    retVal.setUseUnitRatingModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useFactionModifiers")) {
                    retVal.setUseFactionModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useMissionStatusModifiers")) {
                    retVal.setUseMissionStatusModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useHostileTerritoryModifiers")) {
                    retVal.setUseHostileTerritoryModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useFamilyModifiers")) {
                    retVal.setUseFamilyModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useLoyaltyModifiers")) {
                    retVal.setUseLoyaltyModifiers(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useHideLoyalty")) {
                    retVal.setUseHideLoyalty(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("payoutRateOfficer")) {
                    retVal.setPayoutRateOfficer(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("payoutRateEnlisted")) {
                    retVal.setPayoutRateEnlisted(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("payoutRetirementMultiplier")) {
                    retVal.setPayoutRetirementMultiplier(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("usePayoutServiceBonus")) {
                    retVal.setUsePayoutServiceBonus(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("payoutServiceBonusRate")) {
                    retVal.setPayoutServiceBonusRate(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useAdministrativeStrain")) {
                    retVal.setUseAdministrativeStrain(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("administrativeStrain")) {
                    retVal.setAdministrativeCapacity(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("multiCrewStrainDivider")) {
                    retVal.setMultiCrewStrainDivider(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useManagementSkill")) {
                    retVal.setUseManagementSkill(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useCommanderLeadershipOnly")) {
                    retVal.setUseCommanderLeadershipOnly(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("managementSkillPenalty")) {
                    retVal.setManagementSkillPenalty(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useFatigue")) {
                    retVal.setUseFatigue(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("fatigueRate")) {
                    retVal.setFatigueRate(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useInjuryFatigue")) {
                    retVal.setUseInjuryFatigue(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("fieldKitchenCapacity")) {
                    retVal.setFieldKitchenCapacity(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("fieldKitchenIgnoreNonCombatants")) {
                    retVal.setFieldKitchenIgnoreNonCombatants(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("fatigueLeaveThreshold")) {
                    retVal.setFatigueLeaveThreshold(Integer.parseInt(wn2.getTextContent().trim()));
                    // endregion Turnover and Retention

                    // region Finances Tab
                } else if (wn2.getNodeName().equalsIgnoreCase("payForParts")) {
                    retVal.payForParts = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("payForRepairs")) {
                    retVal.payForRepairs = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("payForUnits")) {
                    retVal.payForUnits = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("payForSalaries")) {
                    retVal.payForSalaries = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("payForOverhead")) {
                    retVal.payForOverhead = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("payForMaintain")) {
                    retVal.payForMaintain = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("payForTransport")) {
                    retVal.payForTransport = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("sellUnits")) {
                    retVal.sellUnits = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("sellParts")) {
                    retVal.sellParts = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("payForRecruitment")) {
                    retVal.payForRecruitment = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("useLoanLimits")) {
                    retVal.useLoanLimits = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("usePercentageMaint")) {
                    retVal.usePercentageMaint = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("infantryDontCount")) {
                    retVal.infantryDontCount = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("usePeacetimeCost")) {
                    retVal.usePeacetimeCost = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("useExtendedPartsModifier")) {
                    retVal.useExtendedPartsModifier = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("showPeacetimeCost")) {
                    retVal.showPeacetimeCost = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("financialYearDuration")) {
                    retVal.setFinancialYearDuration(FinancialYearDuration.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("newFinancialYearFinancesToCSVExport")) {
                    retVal.newFinancialYearFinancesToCSVExport = Boolean.parseBoolean(wn2.getTextContent().trim());

                    // region Price Multipliers
                } else if (wn2.getNodeName().equalsIgnoreCase("commonPartPriceMultiplier")) {
                    retVal.setCommonPartPriceMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("innerSphereUnitPriceMultiplier")) {
                    retVal.setInnerSphereUnitPriceMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("innerSpherePartPriceMultiplier")) {
                    retVal.setInnerSpherePartPriceMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("clanUnitPriceMultiplier")) {
                    retVal.setClanUnitPriceMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("clanPartPriceMultiplier")) {
                    retVal.setClanPartPriceMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("mixedTechUnitPriceMultiplier")) {
                    retVal.setMixedTechUnitPriceMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("usedPartPriceMultipliers")) {
                    final String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        try {
                            retVal.getUsedPartPriceMultipliers()[i] = Double.parseDouble(values[i]);
                        } catch (Exception ignored) {

                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("damagedPartsValueMultiplier")) {
                    retVal.setDamagedPartsValueMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("unrepairablePartsValueMultiplier")) {
                    retVal.setUnrepairablePartsValueMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("cancelledOrderRefundMultiplier")) {
                    retVal.setCancelledOrderRefundMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                    // endregion Price Multipliers

                    // region Taxes
                } else if (wn2.getNodeName().equalsIgnoreCase("useTaxes")) {
                    retVal.setUseTaxes(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("taxesPercentage")) {
                    retVal.setTaxesPercentage(Integer.parseInt(wn2.getTextContent().trim()));
                    // endregion Taxes
                    // endregion Finances Tab

                    // Shares
                } else if (wn2.getNodeName().equalsIgnoreCase("useShareSystem")) {
                    retVal.setUseShareSystem(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("sharesForAll")) {
                    retVal.setSharesForAll(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    // endregion Price Multipliers
                    // endregion Finances Tab

                    // region Markets Tab
                    // region Personnel Market
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketName")) {
                    String marketName = wn2.getTextContent().trim();
                    // Backwards compatibility with saves from before these rules moved to Camops
                    if (marketName.equals("Strat Ops")) {
                        marketName = "Campaign Ops";
                    }
                    retVal.setPersonnelMarketName(marketName);
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketReportRefresh")) {
                    retVal.setPersonnelMarketReportRefresh(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomRemovalTargets")) {
                    if (!wn2.hasChildNodes()) {
                        continue;
                    }
                    final NodeList nl2 = wn2.getChildNodes();
                    for (int j = 0; j < nl2.getLength(); j++) {
                        final Node wn3 = nl2.item(j);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        retVal.getPersonnelMarketRandomRemovalTargets().put(
                                SkillLevel.valueOf(wn3.getNodeName().trim()),
                                Integer.parseInt(wn3.getTextContent().trim()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketDylansWeight")) {
                    retVal.setPersonnelMarketDylansWeight(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("usePersonnelHireHiringHallOnly")) {
                    retVal.setUsePersonnelHireHiringHallOnly(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    // endregion Personnel Market

                    // region Unit Market
                } else if (wn2.getNodeName().equalsIgnoreCase("unitMarketMethod")) {
                    retVal.setUnitMarketMethod(UnitMarketMethod.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("unitMarketRegionalMekVariations")) {
                    retVal.setUnitMarketRegionalMekVariations(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("unitMarketSpecialUnitChance")) {
                    retVal.setUnitMarketSpecialUnitChance(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("unitMarketRarityModifier")) {
                    retVal.setUnitMarketRarityModifier(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("instantUnitMarketDelivery")) {
                    retVal.setInstantUnitMarketDelivery(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("unitMarketReportRefresh")) {
                    retVal.setUnitMarketReportRefresh(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    // endregion Unit Market

                    // region Contract Market
                } else if (wn2.getNodeName().equalsIgnoreCase("contractMarketMethod")) {
                    retVal.setContractMarketMethod(ContractMarketMethod.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("contractSearchRadius")) {
                    retVal.setContractSearchRadius(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("variableContractLength")) {
                    retVal.setVariableContractLength(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("contractMarketReportRefresh")) {
                    retVal.setContractMarketReportRefresh(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("contractMaxSalvagePercentage")) {
                    retVal.setContractMaxSalvagePercentage(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("dropShipBonusPercentage")) {
                    retVal.setDropShipBonusPercentage(Integer.parseInt(wn2.getTextContent().trim()));
                    // endregion Contract Market
                    // endregion Markets Tab

                    // region RATs Tab
                } else if (wn2.getNodeName().equals("useStaticRATs")) {
                    retVal.setUseStaticRATs(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("rats")) {
                    retVal.setRATs(MHQXMLUtility.unEscape(wn2.getTextContent().trim()).split(","));
                } else if (wn2.getNodeName().equals("ignoreRATEra")) {
                    retVal.setIgnoreRATEra(Boolean.parseBoolean(wn2.getTextContent().trim()));
                    // endregion RATs Tab

                    // region AtB Tab
                } else if (wn2.getNodeName().equalsIgnoreCase("skillLevel")) {
                    retVal.setSkillLevel(SkillLevel.valueOf(wn2.getTextContent().trim()));
                    // region ACAR Tab
                } else if (wn2.getNodeName().equalsIgnoreCase("autoResolveMethod")) {
                    retVal.setAutoResolveMethod(AutoResolveMethod.valueOf(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("autoResolveVictoryChanceEnabled")) {
                    retVal.setAutoResolveVictoryChanceEnabled(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("autoResolveNumberOfScenarios")) {
                    retVal.setAutoResolveNumberOfScenarios(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("autoResolveUseExperimentalPacarGui")) {
                    retVal.setAutoResolveExperimentalPacarGuiEnabled(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("strategicViewTheme")) {
                    retVal.setStrategicViewTheme(wn2.getTextContent().trim());
                    // endregion ACAR Tab
                    // endregion AtB Tab

                } else if (wn2.getNodeName().equalsIgnoreCase("phenotypeProbabilities")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.phenotypeProbabilities[i] = Integer.parseInt(values[i]);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("useAtB")) {
                    retVal.useAtB = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useStratCon")) {
                    retVal.useStratCon = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useAero")) {
                    retVal.useAero = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useVehicles")) {
                    retVal.useVehicles = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("clanVehicles")) {
                    retVal.clanVehicles = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useGenericBattleValue")) {
                    retVal.useGenericBattleValue = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useVerboseBidding")) {
                    retVal.useVerboseBidding = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("doubleVehicles")) {
                    retVal.doubleVehicles = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("adjustPlayerVehicles")) {
                    retVal.adjustPlayerVehicles = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("opForLanceTypeMeks")) {
                    retVal.setOpForLanceTypeMeks(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("opForLanceTypeMixed")) {
                    retVal.setOpForLanceTypeMixed(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("opForLanceTypeVehicles")) {
                    retVal.setOpForLanceTypeVehicles(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("opForUsesVTOLs")) {
                    retVal.setOpForUsesVTOLs(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useDropShips")) {
                    retVal.useDropShips = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("mercSizeLimited")) {
                    retVal.mercSizeLimited = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("regionalMekVariations")) {
                    retVal.regionalMekVariations = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("attachedPlayerCamouflage")) {
                    retVal.attachedPlayerCamouflage = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("playerControlsAttachedUnits")) {
                    retVal.setPlayerControlsAttachedUnits(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("atbBattleChance")) {
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        try {
                            retVal.atbBattleChance[i] = Integer.parseInt(values[i]);
                        } catch (Exception ignored) {
                            // Badly coded, but this is to migrate devs and their games as the swap was
                            // done before a release and is thus better to handle this way than through
                            // a more code complex method
                            retVal.atbBattleChance[i] = (int) Math.round(Double.parseDouble(values[i]));
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("generateChases")) {
                    retVal.setGenerateChases(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useWeatherConditions")) {
                    retVal.useWeatherConditions = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useLightConditions")) {
                    retVal.useLightConditions = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("usePlanetaryConditions")) {
                    retVal.usePlanetaryConditions = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("useStrategy")) {
                    retVal.useStrategy = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("baseStrategyDeployment")) {
                    retVal.baseStrategyDeployment = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("additionalStrategyDeployment")) {
                    retVal.additionalStrategyDeployment = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("restrictPartsByMission")) {
                    retVal.restrictPartsByMission = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("limitLanceWeight")) {
                    retVal.limitLanceWeight = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("limitLanceNumUnits")) {
                    retVal.limitLanceNumUnits = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("allowOpForLocalUnits")) {
                    retVal.setAllowOpForLocalUnits(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("allowOpForAeros")) {
                    retVal.setAllowOpForAeros(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("opForAeroChance")) {
                    retVal.setOpForAeroChance(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("opForLocalUnitChance")) {
                    retVal.setOpForLocalUnitChance(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("fixedMapChance")) {
                    retVal.fixedMapChance = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("spaUpgradeIntensity")) {
                    retVal.setSpaUpgradeIntensity(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioModMax")) {
                    retVal.setScenarioModMax(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioModChance")) {
                    retVal.setScenarioModChance(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioModBV")) {
                    retVal.setScenarioModBV(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("autoconfigMunitions")) {
                    retVal.setAutoConfigMunitions(Boolean.parseBoolean(wn2.getTextContent().trim()));

                    //region Legacy
                    // Removed in 0.49.*
                } else if (wn2.getNodeName().equalsIgnoreCase("salaryXPMultiplier")) { // Legacy, 0.49.12 removal
                    String[] values = wn2.getTextContent().split(",");
                    for (int i = 0; i < values.length; i++) {
                        retVal.getSalaryXPMultipliers().put(Skills.SKILL_LEVELS[i + 1], Double.parseDouble(values[i]));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomEliteRemoval")) { // Legacy, 0.49.12 removal
                    retVal.getPersonnelMarketRandomRemovalTargets().put(SkillLevel.ELITE, Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomVeteranRemoval")) { // Legacy, 0.49.12 removal
                    retVal.getPersonnelMarketRandomRemovalTargets().put(SkillLevel.VETERAN, Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomRegularRemoval")) { // Legacy, 0.49.12 removal
                    retVal.getPersonnelMarketRandomRemovalTargets().put(SkillLevel.REGULAR, Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomGreenRemoval")) { // Legacy, 0.49.12 removal
                    retVal.getPersonnelMarketRandomRemovalTargets().put(SkillLevel.GREEN, Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomUltraGreenRemoval")) { // Legacy, 0.49.12 removal
                    retVal.getPersonnelMarketRandomRemovalTargets().put(SkillLevel.ULTRA_GREEN, Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomizeOrigin")) { // Legacy, 0.49.7 Removal
                    retVal.getRandomOriginOptions().setRandomizeOrigin(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomizeDependentOrigin")) { // Legacy, 0.49.7 Removal
                    retVal.getRandomOriginOptions().setRandomizeDependentOrigin(Boolean.parseBoolean(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("originSearchRadius")) { // Legacy, 0.49.7 Removal
                    retVal.getRandomOriginOptions().setOriginSearchRadius(Integer.parseInt(wn2.getTextContent()));
                } else if (wn2.getNodeName().equalsIgnoreCase("extraRandomOrigin")) { // Legacy, 0.49.7 Removal
                    retVal.getRandomOriginOptions().setExtraRandomOrigin(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("originDistanceScale")) { // Legacy, 0.49.7 Removal
                    retVal.getRandomOriginOptions().setOriginDistanceScale(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("dependentsNeverLeave")) { // Legacy - 0.49.7 Removal
                    retVal.setUseRandomDependentRemoval(!Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("marriageAgeRange")) { // Legacy - 0.49.6 Removal
                    retVal.setRandomMarriageAgeRange(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useRandomMarriages")) { // Legacy - 0.49.6 Removal
                    retVal.setRandomMarriageMethod(Boolean.parseBoolean(wn2.getTextContent().trim())
                            ? RandomMarriageMethod.DICE_ROLL : RandomMarriageMethod.NONE);
                } else if (wn2.getNodeName().equalsIgnoreCase("logMarriageNameChange")) { // Legacy - 0.49.6 Removal
                    retVal.setLogMarriageNameChanges(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("randomMarriageSurnameWeights")) { // Legacy - 0.49.6 Removal
                    final String[] values = wn2.getTextContent().split(",");
                    if (values.length == 13) {
                        final MergingSurnameStyle[] marriageSurnameStyles = MergingSurnameStyle.values();
                        for (int i = 0; i < values.length; i++) {
                            retVal.getMarriageSurnameWeights().put(marriageSurnameStyles[i], Integer.parseInt(values[i]));
                        }
                    } else if (values.length == 9) {
                        retVal.migrateMarriageSurnameWeights47(values);
                    } else {
                        logger.error("Unknown length of randomMarriageSurnameWeights");
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("logConception")) { // Legacy - 0.49.4 Removal
                    retVal.setLogProcreation(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("staticRATs")) { // Legacy - 0.49.4 Removal
                    retVal.setUseStaticRATs(true);
                } else if (wn2.getNodeName().equalsIgnoreCase("ignoreRatEra")) { // Legacy - 0.49.4 Removal
                    retVal.setIgnoreRATEra(true);
                } else if (wn2.getNodeName().equalsIgnoreCase("clanPriceModifier")) { // Legacy - 0.49.3 Removal
                    final double value = Double.parseDouble(wn2.getTextContent());
                    retVal.setClanUnitPriceMultiplier(value);
                    retVal.setClanPartPriceMultiplier(value);
                } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueA")) { // Legacy - 0.49.3 Removal
                    retVal.getUsedPartPriceMultipliers()[0] = Double.parseDouble(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueB")) { // Legacy - 0.49.3 Removal
                    retVal.getUsedPartPriceMultipliers()[1] = Double.parseDouble(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueC")) { // Legacy - 0.49.3 Removal
                    retVal.getUsedPartPriceMultipliers()[2] = Double.parseDouble(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueD")) { // Legacy - 0.49.3 Removal
                    retVal.getUsedPartPriceMultipliers()[3] = Double.parseDouble(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueE")) { // Legacy - 0.49.3 Removal
                    retVal.getUsedPartPriceMultipliers()[4] = Double.parseDouble(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueF")) { // Legacy - 0.49.3 Removal
                    retVal.getUsedPartPriceMultipliers()[5] = Double.parseDouble(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("damagedPartsValue")) { // Legacy - 0.49.3 Removal
                    retVal.setDamagedPartsValueMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("canceledOrderReimbursement")) { // Legacy - 0.49.3 Removal
                    retVal.setCancelledOrderRefundMultiplier(Double.parseDouble(wn2.getTextContent().trim()));

                    // Removed in 0.47.*
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketType")) { // Legacy
                    retVal.setPersonnelMarketName(PersonnelMarket.getTypeName(Integer.parseInt(wn2.getTextContent().trim())));
                } else if (wn2.getNodeName().equalsIgnoreCase("useAtBCapture")) { // Legacy
                    if (Boolean.parseBoolean(wn2.getTextContent().trim())) {
                        retVal.setPrisonerCaptureStyle(PrisonerCaptureStyle.ATB);
                        retVal.setUseAtBPrisonerDefection(true);
                        retVal.setUseAtBPrisonerRansom(true);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("intensity")) { // Legacy
                    double intensity = Double.parseDouble(wn2.getTextContent().trim());

                    retVal.atbBattleChance[CombatRole.MANEUVER.ordinal()] = (int) Math.round(((40.0 * intensity) / (40.0 * intensity + 60.0)) * 100.0 + 0.5);
                    retVal.atbBattleChance[CombatRole.FRONTLINE.ordinal()] = (int) Math.round(((20.0 * intensity) / (20.0 * intensity + 80.0)) * 100.0 + 0.5);
                    retVal.atbBattleChance[CombatRole.PATROL.ordinal()] = (int) Math.round(((60.0 * intensity) / (60.0 * intensity + 40.0)) * 100.0 + 0.5);
                    retVal.atbBattleChance[CombatRole.TRAINING.ordinal()] = (int) Math.round(((10.0 * intensity) / (10.0 * intensity + 90.0)) * 100.0 + 0.5);
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketType")) { // Legacy
                    retVal.personnelMarketName = PersonnelMarket.getTypeName(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("capturePrisoners")) { // Legacy
                    retVal.setPrisonerCaptureStyle(Boolean.parseBoolean(wn2.getTextContent().trim())
                            ? PrisonerCaptureStyle.TAHARQA : PrisonerCaptureStyle.NONE);
                } else if (wn2.getNodeName().equalsIgnoreCase("startGameDelay")) { // Legacy
                    MekHQ.getMHQOptions().setStartGameDelay(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("historicalDailyLog")) { // Legacy
                    MekHQ.getMHQOptions().setHistoricalDailyLog(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("useUnitRating") // Legacy
                        || wn2.getNodeName().equalsIgnoreCase("useDragoonRating")) { // Legacy
                    if (!Boolean.parseBoolean(wn2.getTextContent())) {
                        retVal.setUnitRatingMethod(UnitRatingMethod.NONE);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoMW")) { // Legacy
                    retVal.phenotypeProbabilities[Phenotype.MEKWARRIOR.ordinal()] = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoBA")) { // Legacy
                    retVal.phenotypeProbabilities[Phenotype.ELEMENTAL.ordinal()] = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoAero")) { // Legacy
                    retVal.phenotypeProbabilities[Phenotype.AEROSPACE.ordinal()] = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoVee")) { // Legacy
                    retVal.phenotypeProbabilities[Phenotype.VEHICLE.ordinal()] = Integer.parseInt(wn2.getTextContent().trim());
                }
            } catch (Exception ex) {
                logger.error(ex, "Unknown Exception: generationCampaignOptionsFromXML");
            }
        }

        logger.debug("Load Campaign Options Complete!");

        return retVal;
    }

    /**
     * This is annoyingly required for the case of anyone having changed the surname
     * weights. The code is not nice, but will nicely handle the cases where anyone
     * has made changes
     *
     * @param values the values to migrate
     */
    public void migrateMarriageSurnameWeights47(final String... values) {
        int[] weights = new int[values.length];

        for (int i = 0; i < weights.length; i++) {
            try {
                weights[i] = Integer.parseInt(values[i]);
            } catch (Exception ex) {
                logger.error(ex, "Unknown Exception: migrateMarriageSurnameWeights47");
                weights[i] = 0;
            }
        }

        // Now we need to test it to figure out the weights have changed. If not, we will keep the
        // new default values. If they have, we save their changes and add the new surname weights
        if (
                (weights[0] != getMarriageSurnameWeights().get(MergingSurnameStyle.NO_CHANGE))
                        || (weights[1] != getMarriageSurnameWeights().get(MergingSurnameStyle.YOURS) + 5)
                        || (weights[2] != getMarriageSurnameWeights().get(MergingSurnameStyle.SPOUSE) + 5)
                        || (weights[3] != getMarriageSurnameWeights().get(MergingSurnameStyle.HYPHEN_SPOUSE) + 5)
                        || (weights[4] != getMarriageSurnameWeights().get(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE) + 5)
                        || (weights[5] != getMarriageSurnameWeights().get(MergingSurnameStyle.HYPHEN_YOURS) + 5)
                        || (weights[6] != getMarriageSurnameWeights().get(MergingSurnameStyle.BOTH_HYPHEN_YOURS) + 5)
                        || (weights[7] != getMarriageSurnameWeights().get(MergingSurnameStyle.MALE))
                        || (weights[8] != getMarriageSurnameWeights().get(MergingSurnameStyle.FEMALE))
        ) {
            getMarriageSurnameWeights().put(MergingSurnameStyle.NO_CHANGE, weights[0]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.YOURS, weights[1]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.SPOUSE, weights[2]);
            // SPACE_YOURS is newly added
            // BOTH_SPACE_YOURS is newly added
            getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_YOURS, weights[3]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_YOURS, weights[4]);
            // SPACE_SPOUSE is newly added
            // BOTH_SPACE_SPOUSE is newly added
            getMarriageSurnameWeights().put(MergingSurnameStyle.HYPHEN_SPOUSE, weights[5]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE, weights[6]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.MALE, weights[7]);
            getMarriageSurnameWeights().put(MergingSurnameStyle.FEMALE, weights[8]);
        }
    }
    // endregion File IO
    public AutoResolveMethod getAutoResolveMethod() {
        return autoResolveMethod;
    }

    public void setAutoResolveMethod(final AutoResolveMethod autoResolveMethod) {
        this.autoResolveMethod = autoResolveMethod;
    }

    public void setStrategicViewTheme(String minimapStyle) {
        // it is persisted here to have something in the campaign options persisted that will change the GUI preference for the theme
        this.strategicViewMinimapTheme = minimapStyle;
        CLIENT_PREFERENCES.setStrategicViewTheme(minimapStyle);
    }

    public File getStrategicViewTheme() {
        CLIENT_PREFERENCES.setStrategicViewTheme(strategicViewMinimapTheme);
        return CLIENT_PREFERENCES.getStrategicViewTheme();
    }

    public boolean isAutoResolveVictoryChanceEnabled() {
        return autoResolveVictoryChanceEnabled;
    }

    public void setAutoResolveVictoryChanceEnabled(final boolean autoResolveVictoryChanceEnabled) {
        this.autoResolveVictoryChanceEnabled = autoResolveVictoryChanceEnabled;
    }

    public void setAutoResolveNumberOfScenarios(int autoResolveNumberOfScenarios) {
        this.autoResolveNumberOfScenarios = autoResolveNumberOfScenarios;
    }

    public int getAutoResolveNumberOfScenarios() {
        return autoResolveNumberOfScenarios;
    }

    public boolean isAutoResolveExperimentalPacarGuiEnabled() {
        return autoResolveExperimentalPacarGuiEnabled;
    }

    public void setAutoResolveExperimentalPacarGuiEnabled(boolean autoResolveExperimentalPacarGuiEnabled) {
        this.autoResolveExperimentalPacarGuiEnabled = autoResolveExperimentalPacarGuiEnabled;
    }
}
