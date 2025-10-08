/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.campaignOptions;

import static megamek.common.TechConstants.getSimpleLevel;
import static megamek.common.options.OptionsConstants.*;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.SUPPORT;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.Version;
import megamek.codeUtilities.MathUtility;
import megamek.common.TechConstants;
import megamek.common.enums.SkillLevel;
import megamek.common.options.GameOptions;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.service.mrms.MRMSOption;
import org.w3c.dom.Node;

/**
 * @author natit
 */
public class CampaignOptions {
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptions.class);
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

    public static final int REPUTATION_PERFORMANCE_CUT_OFF_YEARS = 10;

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
    private boolean clampReputationPayMultiplier;
    private boolean reduceReputationPerformanceModifier;
    private boolean reputationPerformanceModifierCutOff;
    // endregion General Tab

    // region Repair and Maintenance Tab
    // Repair
    private boolean useEraMods;
    private boolean assignedTechFirst;
    private boolean resetToFirstTech;
    private boolean techsUseAdministration;
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
    private ProcurementPersonnelPick acquisitionPersonnelCategory;
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
    private int autoLogisticsActuators;
    private int autoLogisticsJumpJets;
    private int autoLogisticsEngines;
    private int autoLogisticsWeapons;
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
    private final EnumMap<PlanetarySophistication, Integer> planetTechAcquisitionBonus = new EnumMap<>(
          PlanetarySophistication.class);
    private final EnumMap<PlanetaryRating, Integer> planetIndustryAcquisitionBonus = new EnumMap<>(PlanetaryRating.class);
    private final EnumMap<PlanetaryRating, Integer> planetOutputAcquisitionBonus = new EnumMap<>(PlanetaryRating.class);

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
    // endregion Techlimits Tab

    // region Personnel Tab
    // General Personnel
    private boolean useTactics;
    private boolean useInitiativeBonus;
    private boolean useToughness;
    private boolean useRandomToughness;
    private boolean useArtillery;
    private boolean useAbilities;
    private boolean useCommanderAbilitiesOnly;
    private boolean useEdge;
    private boolean useSupportEdge;
    private boolean useImplants;
    private boolean alternativeQualityAveraging;
    private boolean useAgeEffects;
    private boolean useTransfers;
    private boolean useExtendedTOEForceName;
    private boolean personnelLogSkillGain;
    private boolean personnelLogAbilityGain;
    private boolean personnelLogEdgeGain;
    private boolean displayPersonnelLog;
    private boolean displayScenarioLog;
    private boolean displayKillRecord;
    private boolean displayMedicalRecord;
    private boolean displayPatientRecord;
    private boolean displayAssignmentRecord;
    private boolean displayPerformanceRecord;

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
    private boolean adminExperienceLevelIncludeNegotiation;

    // Medical
    private boolean useAdvancedMedical; // Unofficial
    private int healWaitingPeriod;
    private int naturalHealingWaitingPeriod;
    private int minimumHitsForVehicles;
    private boolean useRandomHitsForVehicles;
    private boolean tougherHealing;
    private int maximumPatients;
    private boolean doctorsUseAdministration;

    // Prisoners
    private PrisonerCaptureStyle prisonerCaptureStyle;
    private boolean useFunctionalEscapeArtist;

    // Dependent
    private boolean useRandomDependentAddition;
    private boolean useRandomDependentRemoval;
    private int dependentProfessionDieSize;
    private int civilianProfessionDieSize;

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
    private boolean useSimulatedRelationships;

    // Family
    private FamilialRelationshipDisplayLevel familyDisplayLevel;

    // Anniversaries
    private boolean announceBirthdays;
    private boolean announceRecruitmentAnniversaries;
    private boolean announceOfficersOnly;
    private boolean announceChildBirthdays;

    // Life Events
    private boolean showLifeEventDialogBirths;
    private boolean showLifeEventDialogComingOfAge;
    private boolean showLifeEventDialogCelebrations;

    // Coming of Age
    private boolean rewardComingOfAgeAbilities;
    private boolean rewardComingOfAgeRPSkills;

    // Marriage
    private boolean useManualMarriages;
    private boolean useClanPersonnelMarriages;
    private boolean usePrisonerMarriages;
    private int checkMutualAncestorsDepth;
    private int noInterestInMarriageDiceSize;
    private boolean logMarriageNameChanges;
    private Map<MergingSurnameStyle, Integer> marriageSurnameWeights;
    private RandomMarriageMethod randomMarriageMethod;
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

    // region Turnover and Retention
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

    private boolean UseHRStrain;
    private int hrCapacity;

    private boolean useManagementSkill;
    private boolean useCommanderLeadershipOnly;
    private int managementSkillPenalty;

    private boolean useFatigue;
    private int fatigueRate;
    private boolean useInjuryFatigue;
    private int fieldKitchenCapacity;
    private boolean fieldKitchenIgnoreNonCombatants;
    private int fatigueLeaveThreshold;
    // endregion Turnover and Retention

    // region Finance tab
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
    private boolean payForFood;
    private boolean payForHousing;
    private boolean useLoanLimits;
    private boolean usePercentageMaintenance; // Unofficial
    private boolean infantryDontCount; // Unofficial
    private boolean usePeacetimeCost;
    private boolean useExtendedPartsModifier;
    private boolean showPeacetimeCost;
    private FinancialYearDuration financialYearDuration;
    private boolean newFinancialYearFinancesToCSVExport;
    private boolean simulateGrayMonday;
    private boolean allowMonthlyReinvestment;
    private boolean allowMonthlyConnections;

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
    private boolean allowDuplicatePortraits;
    // endregion Name and Portrait Generation

    // region Markets Tab
    // Personnel Market
    private PersonnelMarketStyle personnelMarketStyle;
    private boolean usePersonnelHireHiringHallOnly;
    private boolean personnelMarketReportRefresh;

    @Deprecated(since = "0.50.06")
    private String personnelMarketName;

    @Deprecated(since = "0.50.06")
    private Map<SkillLevel, Integer> personnelMarketRandomRemovalTargets;

    @Deprecated(since = "0.50.06")
    private double personnelMarketDylansWeight;

    // Unit Market
    private UnitMarketMethod unitMarketMethod;
    private boolean unitMarketRegionalMekVariations;
    private int unitMarketSpecialUnitChance;
    private int unitMarketRarityModifier;
    private boolean instantUnitMarketDelivery;
    private boolean mothballUnitMarketDeliveries;
    private boolean unitMarketReportRefresh;

    // Contract Market
    private ContractMarketMethod contractMarketMethod;
    private int contractSearchRadius;
    private boolean variableContractLength;
    private boolean useDynamicDifficulty;
    private boolean contractMarketReportRefresh;
    private int contractMaxSalvagePercentage;
    private int dropShipBonusPercentage;
    // endregion Markets Tab

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
    private boolean allowOpForAerospace;
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
    private boolean autoGenerateOpForCallSigns;
    private SkillLevel minimumCallsignSkillLevel;
    // endregion Against the Bot Tab

    // start region Faction Standing
    private boolean trackFactionStanding;
    private boolean trackClimateRegardChanges;
    private boolean useFactionStandingNegotiation;
    private boolean useFactionStandingResupply;
    private boolean useFactionStandingCommandCircuit;
    private boolean useFactionStandingOutlawed; // TODO
    private boolean useFactionStandingBatchallRestrictions;
    private boolean useFactionStandingRecruitment;
    private boolean useFactionStandingBarracksCosts;
    private boolean useFactionStandingUnitMarket;
    private boolean useFactionStandingContractPay;
    private boolean useFactionStandingSupportPoints;
    private double regardMultiplier;
    //endregion Faction Standing
    // endregion Variable Declarations

    // region Constructors
    public CampaignOptions() {
        // Initialize any reused variables
        final PersonnelRole[] personnelRoles = PersonnelRole.values();

        // region General Tab
        unitRatingMethod = UnitRatingMethod.CAMPAIGN_OPS;
        manualUnitRatingModifier = 0;
        clampReputationPayMultiplier = false;
        reduceReputationPerformanceModifier = false;
        reputationPerformanceModifierCutOff = false;
        // endregion General Tab

        // region Repair and Maintenance Tab
        // Repair
        useEraMods = false;
        assignedTechFirst = false;
        resetToFirstTech = false;
        techsUseAdministration = false;
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
        acquisitionPersonnelCategory = SUPPORT;
        clanAcquisitionPenalty = 0;
        isAcquisitionPenalty = 0;
        maxAcquisitions = 0;

        // autoLogistics
        autoLogisticsHeatSink = 50;
        autoLogisticsMekHead = 40;
        autoLogisticsMekLocation = 25;
        autoLogisticsNonRepairableLocation = 0;
        autoLogisticsArmor = 100;
        autoLogisticsAmmunition = 100;
        autoLogisticsActuators = 100;
        autoLogisticsJumpJets = 50;
        autoLogisticsEngines = 0;
        autoLogisticsWeapons = 50;
        autoLogisticsOther = 0;

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
        planetTechAcquisitionBonus.put(PlanetarySophistication.ADVANCED, -2); // TODO: needs to be verified
        planetTechAcquisitionBonus.put(PlanetarySophistication.A, -1);
        planetTechAcquisitionBonus.put(PlanetarySophistication.B, 0);
        planetTechAcquisitionBonus.put(PlanetarySophistication.C, 1);
        planetTechAcquisitionBonus.put(PlanetarySophistication.D, 2);
        planetTechAcquisitionBonus.put(PlanetarySophistication.F, 8);
        planetTechAcquisitionBonus.put(PlanetarySophistication.REGRESSED, 16); // TODO: needs to be verified
        planetIndustryAcquisitionBonus.put(PlanetaryRating.A, 0);
        planetIndustryAcquisitionBonus.put(PlanetaryRating.B, 0);
        planetIndustryAcquisitionBonus.put(PlanetaryRating.C, 0);
        planetIndustryAcquisitionBonus.put(PlanetaryRating.D, 0);
        planetIndustryAcquisitionBonus.put(PlanetaryRating.F, 0);
        planetOutputAcquisitionBonus.put(PlanetaryRating.A, -1);
        planetOutputAcquisitionBonus.put(PlanetaryRating.B, 0);
        planetOutputAcquisitionBonus.put(PlanetaryRating.C, 1);
        planetOutputAcquisitionBonus.put(PlanetaryRating.D, 2);
        planetOutputAcquisitionBonus.put(PlanetaryRating.F, 8);
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
        // endregion Techlimits Tab

        // region Personnel Tab
        // General Personnel
        setUseTactics(false);
        setUseInitiativeBonus(false);
        setUseToughness(false);
        setUseRandomToughness(false);
        setUseArtillery(false);
        setUseAbilities(false);
        setUseCommanderAbilitiesOnly(false);
        setUseEdge(false);
        setUseSupportEdge(false);
        setUseImplants(false);
        setAlternativeQualityAveraging(false);
        setUseAgeEffects(false);
        setUseTransfers(true);
        setUseExtendedTOEForceName(false);
        setPersonnelLogSkillGain(false);
        setPersonnelLogAbilityGain(false);
        setPersonnelLogEdgeGain(false);
        setDisplayPersonnelLog(false);
        setDisplayScenarioLog(false);
        setDisplayKillRecord(false);
        setDisplayMedicalRecord(false);
        displayPatientRecord = false;
        setRewardComingOfAgeAbilities(false);
        setRewardComingOfAgeRPSkills(false);

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

        // Medical
        setUseAdvancedMedical(false);
        setHealingWaitingPeriod(1);
        setNaturalHealingWaitingPeriod(15);
        setMinimumHitsForVehicles(1);
        setUseRandomHitsForVehicles(false);
        setTougherHealing(false);
        setMaximumPatients(25);
        setDoctorsUseAdministration(false);

        // Prisoners
        setPrisonerCaptureStyle(PrisonerCaptureStyle.NONE);
        useFunctionalEscapeArtist = false;

        // Dependent
        setUseRandomDependentAddition(false);
        setUseRandomDependentRemoval(false);
        setDependentProfessionDieSize(4);
        setCivilianProfessionDieSize(2);

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
        for (PersonnelRole role : personnelRoles) {
            setRoleBaseSalary(role, 250);
        }
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
        setRoleBaseSalary(PersonnelRole.NOBLE, 2500);
        setRoleBaseSalary(PersonnelRole.DEPENDENT, 50);
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
        setUseSimulatedRelationships(false);

        // Family
        setFamilyDisplayLevel(FamilialRelationshipDisplayLevel.SPOUSE);

        // Anniversaries
        setAnnounceBirthdays(true);
        setAnnounceRecruitmentAnniversaries(true);
        setAnnounceOfficersOnly(true);
        setAnnounceChildBirthdays(true);

        // Life Events
        setShowLifeEventDialogBirths(true);
        setShowLifeEventDialogComingOfAge(true);
        setShowLifeEventDialogCelebrations(true);

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

        setUseHRStrain(true);
        setHRCapacity(10);

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
        payForFood = false;
        payForHousing = false;
        useLoanLimits = false;
        usePercentageMaintenance = false;
        infantryDontCount = false;
        usePeacetimeCost = false;
        useExtendedPartsModifier = false;
        showPeacetimeCost = false;
        setFinancialYearDuration(FinancialYearDuration.ANNUAL);
        newFinancialYearFinancesToCSVExport = false;
        simulateGrayMonday = false;
        allowMonthlyReinvestment = false;
        allowMonthlyConnections = false;

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
        allowDuplicatePortraits = true;
        // endregion Name and Portrait Generation Tab

        // region Markets Tab
        // Personnel Market
        personnelMarketStyle = PERSONNEL_MARKET_DISABLED;
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
        setUseDynamicDifficulty(false);
        setContractMarketReportRefresh(true);
        setContractMaxSalvagePercentage(100);
        setDropShipBonusPercentage(0);
        // endregion Markets Tab

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
        setAllowOpForAerospace(false);
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
        autoGenerateOpForCallSigns = true;
        minimumCallsignSkillLevel = SkillLevel.VETERAN;
        useFactionStandingNegotiation = true;
        useFactionStandingResupply = true;
        useFactionStandingCommandCircuit = true;
        useFactionStandingOutlawed = true;
        useFactionStandingBatchallRestrictions = true;
        useFactionStandingRecruitment = true;
        useFactionStandingBarracksCosts = true;
        useFactionStandingUnitMarket = true;
        useFactionStandingContractPay = true;
        useFactionStandingSupportPoints = true;
        regardMultiplier = 1.0;
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

    public boolean isClampReputationPayMultiplier() {
        return clampReputationPayMultiplier;
    }

    public void setClampReputationPayMultiplier(final boolean clampReputationPayMultiplier) {
        this.clampReputationPayMultiplier = clampReputationPayMultiplier;
    }

    public boolean isReduceReputationPerformanceModifier() {
        return reduceReputationPerformanceModifier;
    }

    public void setReduceReputationPerformanceModifier(final boolean reduceReputationPerformanceModifier) {
        this.reduceReputationPerformanceModifier = reduceReputationPerformanceModifier;
    }

    public boolean isReputationPerformanceModifierCutOff() {
        return reputationPerformanceModifierCutOff;
    }

    public void setReputationPerformanceModifierCutOff(final boolean reputationPerformanceModifierCutOff) {
        this.reputationPerformanceModifierCutOff = reputationPerformanceModifierCutOff;
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

    public boolean isUseCommanderAbilitiesOnly() {
        return useCommanderAbilitiesOnly;
    }

    public void setUseCommanderAbilitiesOnly(final boolean useCommanderAbilitiesOnly) {
        this.useCommanderAbilitiesOnly = useCommanderAbilitiesOnly;
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

    public boolean isUseAgeEffects() {
        return useAgeEffects;
    }

    public void setUseAgeEffects(final boolean useAgeEffects) {
        this.useAgeEffects = useAgeEffects;
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

    public boolean isDisplayMedicalRecord() {
        return displayMedicalRecord;
    }

    public void setDisplayMedicalRecord(final boolean displayMedicalRecord) {
        this.displayMedicalRecord = displayMedicalRecord;
    }

    public boolean isDisplayPatientRecord() {
        return displayPatientRecord;
    }

    public void setDisplayPatientRecord(final boolean displayPatientRecord) {
        this.displayPatientRecord = displayPatientRecord;
    }

    public boolean isDisplayAssignmentRecord() {
        return displayAssignmentRecord;
    }

    public void setDisplayAssignmentRecord(final boolean displayAssignmentRecord) {
        this.displayAssignmentRecord = displayAssignmentRecord;
    }

    public boolean isDisplayPerformanceRecord() {
        return displayPerformanceRecord;
    }

    public void setDisplayPerformanceRecord(final boolean displayPerformanceRecord) {
        this.displayPerformanceRecord = displayPerformanceRecord;
    }

    public boolean isRewardComingOfAgeAbilities() {
        return rewardComingOfAgeAbilities;
    }

    public void setRewardComingOfAgeAbilities(final boolean rewardComingOfAgeAbilities) {
        this.rewardComingOfAgeAbilities = rewardComingOfAgeAbilities;
    }

    public boolean isRewardComingOfAgeRPSkills() {
        return rewardComingOfAgeRPSkills;
    }

    public void setRewardComingOfAgeRPSkills(final boolean rewardComingOfAgeRPSkills) {
        this.rewardComingOfAgeRPSkills = rewardComingOfAgeRPSkills;
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
     * @param useTimeInService the new value for whether to use time in service or not
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
     * @param trackTotalXPEarnings the new value for whether to track total experience earnings for personnel
     */
    public void setTrackTotalXPEarnings(final boolean trackTotalXPEarnings) {
        this.trackTotalXPEarnings = trackTotalXPEarnings;
    }

    /**
     * Gets a value indicating whether to show a person's origin faction when displaying their details.
     */
    public boolean isShowOriginFaction() {
        return showOriginFaction;
    }

    /**
     * Sets a value indicating whether to show a person's origin faction when displaying their details.
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

    public boolean isAdminExperienceLevelIncludeNegotiation() {
        return adminExperienceLevelIncludeNegotiation;
    }

    public void setAdminExperienceLevelIncludeNegotiation(final boolean useAdminExperienceLevelIncludeNegotiation) {
        this.adminExperienceLevelIncludeNegotiation = useAdminExperienceLevelIncludeNegotiation;
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

    public boolean isDoctorsUseAdministration() {
        return doctorsUseAdministration;
    }

    public void setDoctorsUseAdministration(final boolean doctorsUseAdministration) {
        this.doctorsUseAdministration = doctorsUseAdministration;
    }

    // endregion Medical

    // region Prisoners
    public PrisonerCaptureStyle getPrisonerCaptureStyle() {
        return prisonerCaptureStyle;
    }

    public void setPrisonerCaptureStyle(final PrisonerCaptureStyle prisonerCaptureStyle) {
        this.prisonerCaptureStyle = prisonerCaptureStyle;
    }

    public boolean isUseFunctionalEscapeArtist() {
        return useFunctionalEscapeArtist;
    }

    public void setUseFunctionalEscapeArtist(final boolean useFunctionalEscapeArtist) {
        this.useFunctionalEscapeArtist = useFunctionalEscapeArtist;
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
    // endregion Personnel Randomization

    // region Random Histories
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

    @Deprecated(since = "0.50.07", forRemoval = true)
    public boolean isUseReasoningXpMultiplier() {
        return false;
    }

    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setUseReasoningXpMultiplier(final boolean useReasoningXpMultiplier) {
    }

    public boolean isUseSimulatedRelationships() {
        return useSimulatedRelationships;
    }

    public void setUseSimulatedRelationships(final boolean useSimulatedRelationships) {
        this.useSimulatedRelationships = useSimulatedRelationships;
    }
    // endregion Random Histories

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

    /**
     * Use {@link #isUseHRStrain()} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public boolean isUseAdministrativeStrain() {
        return UseHRStrain;
    }

    public boolean isUseHRStrain() {
        return UseHRStrain;
    }

    /**
     * Use {@link #setUseHRStrain(boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setUseAdministrativeStrain(final boolean UseHRStrain) {
        this.UseHRStrain = UseHRStrain;
    }

    public void setUseHRStrain(final boolean UseHRStrain) {
        this.UseHRStrain = UseHRStrain;
    }

    /**
     * Use {@link #getHRCapacity()} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public Integer getAdministrativeCapacity() {
        return hrCapacity;
    }

    public Integer getHRCapacity() {
        return hrCapacity;
    }

    /**
     * Use {@link #setHRCapacity(Integer)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setAdministrativeCapacity(final Integer hrCapacity) {
        this.hrCapacity = hrCapacity;
    }

    public void setHRCapacity(final Integer hrCapacity) {
        this.hrCapacity = hrCapacity;
    }

    /**
     * @deprecated Unused
     */
    @Deprecated(since = "0.50.6", forRemoval = true)
    public Integer getMultiCrewStrainDivider() {
        return 1;
    }

    /**
     * @deprecated Unused
     */
    @Deprecated(since = "0.50.6", forRemoval = true)
    public void setMultiCrewStrainDivider(final Integer multiCrewStrainDivider) {

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
     * @param announceRecruitmentAnniversaries {@code true} to announce recruitment anniversaries, {@code false}
     *                                         otherwise
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

    //startregion Life Events
    public boolean isShowLifeEventDialogBirths() {
        return showLifeEventDialogBirths;
    }

    public void setShowLifeEventDialogBirths(final boolean showLifeEventDialogBirths) {
        this.showLifeEventDialogBirths = showLifeEventDialogBirths;
    }

    public boolean isShowLifeEventDialogComingOfAge() {
        return showLifeEventDialogComingOfAge;
    }

    public void setShowLifeEventDialogComingOfAge(final boolean showLifeEventDialogComingOfAge) {
        this.showLifeEventDialogComingOfAge = showLifeEventDialogComingOfAge;
    }

    public boolean isShowLifeEventDialogCelebrations() {
        return showLifeEventDialogCelebrations;
    }

    public void setShowLifeEventDialogCelebrations(final boolean showLifeEventDialogCelebrations) {
        this.showLifeEventDialogCelebrations = showLifeEventDialogCelebrations;
    }
    //endregion Life Events

    // region Dependents
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

    public int getDependentProfessionDieSize() {
        return dependentProfessionDieSize;
    }

    public void setDependentProfessionDieSize(final int dependentProfessionDieSize) {
        this.dependentProfessionDieSize = dependentProfessionDieSize;
    }

    public int getCivilianProfessionDieSize() {
        return civilianProfessionDieSize;
    }

    public void setCivilianProfessionDieSize(final int civilianProfessionDieSize) {
        this.civilianProfessionDieSize = civilianProfessionDieSize;
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
     * This gets the number of recursions to use when checking mutual ancestors between two personnel
     *
     * @return the number of recursions to use
     */
    public int getCheckMutualAncestorsDepth() {
        return checkMutualAncestorsDepth;
    }

    /**
     * This sets the number of recursions to use when checking mutual ancestors between two personnel
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
     * @return the weight map of potential surname changes for weighted marriage surname generation
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
     * A random marriage can only happen between two people whose ages differ (+/-) by the returned value
     *
     * @return the age range ages can differ (+/-)
     */
    public int getRandomMarriageAgeRange() {
        return randomMarriageAgeRange;
    }

    /**
     * A random marriage can only happen between two people whose ages differ (+/-) by this value
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
     * @param randomNewDependentMarriage the size of the die used to determine whether marriage occurs outside of
     *                                   current personnel
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
     * @param multiplePregnancyOccurrences the number of occurrences for there to be a single occurrence of a multiple
     *                                     child pregnancy (i.e., 1 in X)
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
     * @param displayTrueDueDate whether to show the expected or actual due date for personnel
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
     * This gets the decimal chance (between 0 and 1) of random procreation occurring
     *
     * @return the chance, with a value between 0 and 1
     */
    public int getRandomProcreationRelationshipDiceSize() {
        return randomProcreationRelationshipDiceSize;
    }

    /**
     * This sets the dice size for random procreation
     *
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
     *
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

    public void setRandomDeathMultiplier(final double randomDeathMultiplier) {
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

    public boolean isPayForFood() {
        return payForFood;
    }

    public void setPayForFood(final boolean payForFood) {
        this.payForFood = payForFood;
    }

    public boolean isPayForHousing() {
        return payForHousing;
    }

    public void setPayForHousing(final boolean payForHousing) {
        this.payForHousing = payForHousing;
    }

    public boolean isUseLoanLimits() {
        return useLoanLimits;
    }

    public void setLoanLimits(final boolean useLoanLimits) {
        this.useLoanLimits = useLoanLimits;
    }

    public boolean isUsePercentageMaintenance() {
        return usePercentageMaintenance;
    }

    public void setUsePercentageMaintenance(final boolean usePercentageMaintenance) {
        this.usePercentageMaintenance = usePercentageMaintenance;
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

    public boolean isSimulateGrayMonday() {
        return simulateGrayMonday;
    }

    public void setSimulateGrayMonday(final boolean simulateGrayMonday) {
        this.simulateGrayMonday = simulateGrayMonday;
    }

    public boolean isAllowMonthlyReinvestment() {
        return allowMonthlyReinvestment;
    }

    public void setAllowMonthlyReinvestment(final boolean allowMonthlyReinvestment) {
        this.allowMonthlyReinvestment = allowMonthlyReinvestment;
    }

    public boolean isAllowMonthlyConnections() {
        return allowMonthlyConnections;
    }

    public void setAllowMonthlyConnections(final boolean allowMonthlyConnections) {
        this.allowMonthlyConnections = allowMonthlyConnections;
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
    public PersonnelMarketStyle getPersonnelMarketStyle() {
        return personnelMarketStyle;
    }

    public void setPersonnelMarketStyle(final PersonnelMarketStyle personnelMarketStyle) {
        this.personnelMarketStyle = personnelMarketStyle;
    }

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

    public boolean isMothballUnitMarketDeliveries() {
        return mothballUnitMarketDeliveries;
    }

    public void setMothballUnitMarketDeliveries(final boolean mothballUnitMarketDeliveries) {
        this.mothballUnitMarketDeliveries = mothballUnitMarketDeliveries;
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

    public boolean isUseDynamicDifficulty() {
        return useDynamicDifficulty;
    }

    public void setUseDynamicDifficulty(final boolean useDynamicDifficulty) {
        this.useDynamicDifficulty = useDynamicDifficulty;
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

    /**
     * @deprecated no longer in use
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public boolean isUseStaticRATs() {
        return false;
    }

    /**
     * @deprecated no longer in use
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setUseStaticRATs(final boolean useStaticRATs) {
    }

    /**
     * @deprecated no longer in use
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public String[] getRATs() {
        return new String[0];
    }

    /**
     * @deprecated no longer in use
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setRATs(final String... rats) {
    }

    /**
     * @deprecated no longer in use
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public boolean isIgnoreRATEra() {
        return false;
    }

    /**
     * @deprecated no longer in use
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setIgnoreRATEra(final boolean ignore) {
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
     * Checks whether administrative adjustments are applied for technician time calculations.
     *
     * <p>This configuration determines if technicians' daily available time should be adjusted
     * using administrative multipliers in relevant calculations.</p>
     *
     * @return {@code true} if administrative adjustments are enabled for technicians, {@code false} otherwise.
     */
    public boolean isTechsUseAdministration() {
        return techsUseAdministration;
    }

    /**
     * Sets whether administrative adjustments should be applied to technician time calculations.
     *
     * <p>Enabling this setting applies administrative multipliers to modify technicians' daily available time
     * in relevant calculations.</p>
     *
     * @param techsUseAdministration {@code true} to enable administrative adjustments for technicians, {@code false} to
     *                               disable them.
     */
    public void setTechsUseAdministration(final boolean techsUseAdministration) {
        this.techsUseAdministration = techsUseAdministration;
    }

    /**
     * @return true to use the origin faction for personnel names instead of a set faction
     */
    public boolean isUseOriginFactionForNames() {
        return useOriginFactionForNames;
    }

    /**
     * @param useOriginFactionForNames whether to use personnel names or a set faction
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

    public void setIsUseFactionIntroDate(final boolean factionIntroDate) {
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

    public boolean isAllowDuplicatePortraits() {
        return allowDuplicatePortraits;
    }

    public void setAllowDuplicatePortraits(final boolean allowDuplicatePortraits) {
        this.allowDuplicatePortraits = allowDuplicatePortraits;
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

    /**
     * Checks if the acquisition personnel category matches a specified category.
     *
     * @param category The {@link ProcurementPersonnelPick} category to check against.
     *
     * @return {@code true} if the current acquisition personnel category matches the specified category, {@code false}
     *       otherwise.
     */
    public boolean isAcquisitionPersonnelCategory(ProcurementPersonnelPick category) {
        return acquisitionPersonnelCategory == category;
    }

    /**
     * Retrieves the current acquisition personnel category.
     *
     * <p>This method returns the {@link ProcurementPersonnelPick} value assigned to indicate what
     * personnel category can make acquisition checks.</p>
     *
     * <p><b>Usage:</b> Generally, for most use-cases, you'll want to use the shortcut method
     * {@link #isAcquisitionPersonnelCategory(ProcurementPersonnelPick)} instead.</p>
     *
     * @return The current {@link ProcurementPersonnelPick} that represents the acquisition's personnel category.
     */
    public ProcurementPersonnelPick getAcquisitionPersonnelCategory() {
        return acquisitionPersonnelCategory;
    }

    /**
     * Sets the acquisition personnel category.
     *
     * <p>This method defines what personnel category (represented as a {@link ProcurementPersonnelPick})
     * is eligible to make acquisition checks in the campaign system.</p>
     *
     * @param acquisitionPersonnelCategory The {@link ProcurementPersonnelPick} value to assign.
     */
    public void setAcquisitionPersonnelCategory(final ProcurementPersonnelPick acquisitionPersonnelCategory) {
        this.acquisitionPersonnelCategory = acquisitionPersonnelCategory;
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

    public int getPlanetTechAcquisitionBonus(final PlanetarySophistication sophistication) {
        return planetTechAcquisitionBonus.getOrDefault(sophistication, 0);
    }

    public EnumMap<PlanetarySophistication, Integer> getAllPlanetTechAcquisitionBonuses() {
        return planetTechAcquisitionBonus;
    }

    public void setPlanetTechAcquisitionBonus(final int base, final PlanetarySophistication sophistication) {
        this.planetTechAcquisitionBonus.put(sophistication, base);
    }

    public int getPlanetIndustryAcquisitionBonus(final PlanetaryRating rating) {
        return planetIndustryAcquisitionBonus.getOrDefault(rating, 0);
    }

    public EnumMap<PlanetaryRating, Integer> getAllPlanetIndustryAcquisitionBonuses() {
        return planetIndustryAcquisitionBonus;
    }

    public void setPlanetIndustryAcquisitionBonus(final int base, final PlanetaryRating rating) {
        this.planetIndustryAcquisitionBonus.put(rating, base);
    }

    public int getPlanetOutputAcquisitionBonus(final PlanetaryRating rating) {
        return planetOutputAcquisitionBonus.getOrDefault(rating, 0);
    }

    public EnumMap<PlanetaryRating, Integer> getAllPlanetOutputAcquisitionBonuses() {
        return planetOutputAcquisitionBonus;
    }

    public void setPlanetOutputAcquisitionBonus(final int base, final PlanetaryRating rating) {
        this.planetOutputAcquisitionBonus.put(rating, base);
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

    public int getAutoLogisticsActuators() {
        return autoLogisticsActuators;
    }

    public void setAutoLogisticsActuators(int autoLogisticsActuators) {
        this.autoLogisticsActuators = autoLogisticsActuators;
    }

    public int getAutoLogisticsJumpJets() {
        return autoLogisticsJumpJets;
    }

    public void setAutoLogisticsJumpJets(int autoLogisticsJumpJets) {
        this.autoLogisticsJumpJets = autoLogisticsJumpJets;
    }

    public int getAutoLogisticsEngines() {
        return autoLogisticsEngines;
    }

    public void setAutoLogisticsEngines(int autoLogisticsEngines) {
        this.autoLogisticsEngines = autoLogisticsEngines;
    }

    public int getAutoLogisticsWeapons() {
        return autoLogisticsWeapons;
    }

    public void setAutoLogisticsWeapons(int autoLogisticsWeapons) {
        this.autoLogisticsWeapons = autoLogisticsWeapons;
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
     * This is a convenience method that calls {@link #getAtBBattleChance(CombatRole, boolean)} with
     * {@code useStratConBypass} set to {@code false}. As a result, if StratCon is enabled, the method will return
     * {@code 0} regardless of other conditions.
     * </p>
     *
     * @param role the {@link CombatRole} to evaluate the battle chance for.
     *
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
     * This method calculates the battle chance percentage for the provided combat role based on its ordinal position in
     * the {@code atbBattleChance} array. If StratCon is enabled and the {@code useStratConBypass} parameter is set to
     * {@code true}, the method immediately returns {@code 0}.
     * <p>
     * Combat roles marked as {@link CombatRole#RESERVE} or {@link CombatRole#AUXILIARY} are not eligible for battles
     * and also return {@code 0}.
     *
     * @param role              the {@link CombatRole} to evaluate the battle chance for.
     * @param useStratConBypass a {@code boolean} indicating whether to bypass the StratCon-check logic. If
     *                          {@code false}, this allows the method to ignore StratCon-enabled status.
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

    public int[] getAllAtBBattleChances() {
        return atbBattleChance;
    }

    /**
     * @param role      the {@link CombatRole} ordinal value
     * @param frequency the frequency to set the generation to (percent chance from 0 to 100)
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

    /**
     * @deprecated unused.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public boolean isUseStrategy() {
        return false;
    }

    /**
     * @deprecated unused.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void setUseStrategy(final boolean useStrategy) {
    }

    /**
     * @deprecated unused except in deprecated methods
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public int getBaseStrategyDeployment() {
        return 0;
    }

    /**
     * @deprecated unused.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void setBaseStrategyDeployment(final int baseStrategyDeployment) {
    }

    /**
     * @deprecated unused except in deprecated methods
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public int getAdditionalStrategyDeployment() {
        return 0;
    }

    /**
     * @deprecated unused.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void setAdditionalStrategyDeployment(final int additionalStrategyDeployment) {
    }

    public boolean isRestrictPartsByMission() {
        return restrictPartsByMission;
    }

    public void setRestrictPartsByMission(final boolean restrictPartsByMission) {
        this.restrictPartsByMission = restrictPartsByMission;
    }

    /**
     * @deprecated unused.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public boolean isLimitLanceWeight() {
        return false;
    }

    /**
     * @deprecated unused.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void setLimitLanceWeight(final boolean limitLanceWeight) {
    }

    public boolean isLimitLanceNumUnits() {
        return false;
    }

    /**
     * @deprecated unused.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public void setLimitLanceNumUnits(final boolean limitLanceNumUnits) {
    }

    public boolean isAllowOpForAerospace() {
        return allowOpForAerospace;
    }

    public void setAllowOpForAerospace(final boolean allowOpForAerospace) {
        this.allowOpForAerospace = allowOpForAerospace;
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

    /** Use {@link CampaignOptionsMarshaller#writeCampaignOptionsToXML(CampaignOptions, PrintWriter, int)} instead. */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void writeToXml(final PrintWriter pw, int indent) {
        CampaignOptionsMarshaller.writeCampaignOptionsToXML(this, pw, indent);
    }

    /** Use {@link CampaignOptionsUnmarshaller#generateCampaignOptionsFromXml(Node, Version)} instead */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static CampaignOptions generateCampaignOptionsFromXml(Node parentNod, Version version) {
        return CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(parentNod, version);
    }

    /**
     * This is annoyingly required for the case of anyone having changed the surname weights. The code is not nice, but
     * will nicely handle the cases where anyone has made changes
     *
     * @param values the values to migrate
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public void migrateMarriageSurnameWeights(final String... values) {
        int[] weights = new int[values.length];

        for (int i = 0; i < weights.length; i++) {
            try {
                weights[i] = Integer.parseInt(values[i]);
            } catch (Exception ex) {
                LOGGER.error(ex, "Unknown Exception: migrateMarriageSurnameWeights47");
                weights[i] = 0;
            }
        }

        // Now we need to test it to figure out the weights have changed. If not, we
        // will keep the
        // new default values. If they have, we save their changes and add the new
        // surname weights
        if ((weights[0] != getMarriageSurnameWeights().get(MergingSurnameStyle.NO_CHANGE)) ||
                  (weights[1] != getMarriageSurnameWeights().get(MergingSurnameStyle.YOURS) + 5) ||
                  (weights[2] != getMarriageSurnameWeights().get(MergingSurnameStyle.SPOUSE) + 5) ||
                  (weights[3] != getMarriageSurnameWeights().get(MergingSurnameStyle.HYPHEN_SPOUSE) + 5) ||
                  (weights[4] != getMarriageSurnameWeights().get(MergingSurnameStyle.BOTH_HYPHEN_SPOUSE) + 5) ||
                  (weights[5] != getMarriageSurnameWeights().get(MergingSurnameStyle.HYPHEN_YOURS) + 5) ||
                  (weights[6] != getMarriageSurnameWeights().get(MergingSurnameStyle.BOTH_HYPHEN_YOURS) + 5) ||
                  (weights[7] != getMarriageSurnameWeights().get(MergingSurnameStyle.MALE)) ||
                  (weights[8] != getMarriageSurnameWeights().get(MergingSurnameStyle.FEMALE))) {
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
        // it is persisted here to have something in the campaign options persisted that
        // will change the GUI preference for the theme
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

    /**
     * Determines if faction standing negotiation is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingNegotiationSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing negotiation is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingNegotiation() {
        return useFactionStandingNegotiation;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing negotiation is active.
     *
     * @return {@code true} if both faction standing tracking and faction standing negotiation usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingNegotiationSafe() {
        return trackFactionStanding && useFactionStandingNegotiation;
    }

    /**
     * Sets whether the system should use faction standing negotiation.
     *
     * @param useFactionStandingNegotiation a boolean indicating if faction standing negotiation should be enabled
     *                                      (true) or disabled (false)
     */
    public void setUseFactionStandingNegotiation(boolean useFactionStandingNegotiation) {
        this.useFactionStandingNegotiation = useFactionStandingNegotiation;
    }

    /**
     * Determines if faction standing resupply modifiers is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingResupplySafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing resupply modifiers is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingResupply() {
        return useFactionStandingResupply;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing resupply modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing resupply modifier usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingResupplySafe() {
        return trackFactionStanding && useFactionStandingResupply;
    }

    public void setUseFactionStandingResupply(boolean useFactionStandingResupply) {
        this.useFactionStandingResupply = useFactionStandingResupply;
    }

    /**
     * Determines if faction standing command circuit access is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingCommandCircuitSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing command circuit access is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingCommandCircuit() {
        return useFactionStandingCommandCircuit;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing command circuits are
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing command circuit usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingCommandCircuitSafe() {
        return trackFactionStanding && useFactionStandingCommandCircuit;
    }

    public void setUseFactionStandingCommandCircuit(boolean useFactionStandingCommandCircuit) {
        this.useFactionStandingCommandCircuit = useFactionStandingCommandCircuit;
    }

    /**
     * Determines if faction standing outlawing is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingOutlawedSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing outlawing is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingOutlawed() {
        return useFactionStandingOutlawed;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing outlawing is active.
     *
     * @return {@code true} if both faction standing tracking and faction standing outlaw usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingOutlawedSafe() {
        return trackFactionStanding && useFactionStandingOutlawed;
    }

    public void setUseFactionStandingOutlawed(boolean useFactionStandingOutlawed) {
        this.useFactionStandingOutlawed = useFactionStandingOutlawed;
    }

    /**
     * Determines if faction standing batchall restriction is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingBatchallRestrictionsSafe()}
     * as that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing batchall restriction is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingBatchallRestrictions() {
        return useFactionStandingBatchallRestrictions;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing batchall restrictions are
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing batchall restrictions usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingBatchallRestrictionsSafe() {
        return trackFactionStanding && useFactionStandingBatchallRestrictions;
    }

    public void setUseFactionStandingBatchallRestrictions(boolean useFactionStandingBatchallRestrictions) {
        this.useFactionStandingBatchallRestrictions = useFactionStandingBatchallRestrictions;
    }

    /**
     * Determines if faction standing recruitment modifiers is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingRecruitmentSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing recruitment modifiers is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingRecruitment() {
        return useFactionStandingRecruitment;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing recruitment modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing recruitment modifier usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingRecruitmentSafe() {
        return trackFactionStanding && useFactionStandingRecruitment;
    }

    public void setUseFactionStandingRecruitment(boolean useFactionStandingRecruitment) {
        this.useFactionStandingRecruitment = useFactionStandingRecruitment;
    }

    /**
     * Determines if faction standing barrack costs is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingBarracksCostsSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing barrack costs is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingBarracksCosts() {
        return useFactionStandingBarracksCosts;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing barrack cost modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing barrack cost modifier usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingBarracksCostsSafe() {
        return trackFactionStanding && useFactionStandingBarracksCosts;
    }

    public void setUseFactionStandingBarracksCosts(boolean useFactionStandingBarracksCosts) {
        this.useFactionStandingBarracksCosts = useFactionStandingBarracksCosts;
    }

    /**
     * Determines if faction standing unit market modifiers is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingUnitMarketSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing unit market modifiers is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingUnitMarket() {
        return useFactionStandingUnitMarket;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing unit market modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing unit market modifier usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingUnitMarketSafe() {
        return trackFactionStanding && useFactionStandingUnitMarket;
    }

    public void setUseFactionStandingUnitMarket(boolean useFactionStandingUnitMarket) {
        this.useFactionStandingUnitMarket = useFactionStandingUnitMarket;
    }

    /**
     * Determines if faction standing contract pay is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingContractPaySafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing contract pay is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingContractPay() {
        return useFactionStandingContractPay;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing contract payment modifiers
     * is active.
     *
     * @return {@code true} if both faction standing tracking and faction standing contract pay modifier usage are
     *       enabled; {@code false} otherwise.
     */
    public boolean isUseFactionStandingContractPaySafe() {
        return trackFactionStanding && useFactionStandingContractPay;
    }

    public void setUseFactionStandingContractPay(boolean useFactionStandingContractPay) {
        this.useFactionStandingContractPay = useFactionStandingContractPay;
    }

    /**
     * Determines if faction standing support points is enabled.
     *
     * <p><b>Usage:</b> for most use cases you will want to use {@link #isUseFactionStandingSupportPointsSafe()} as
     * that also verifies that Faction Standing is enabled.</p>
     *
     * @return {@code true} if faction standing support points is enabled, {@code false} otherwise.
     */
    public boolean isUseFactionStandingSupportPoints() {
        return useFactionStandingSupportPoints;
    }

    /**
     * Checks whether tracking faction standing is enabled and if the use of faction standing support point modifiers is
     * active.
     *
     * @return {@code true} if both faction standing tracking and faction standing resupply modifier usage are enabled;
     *       {@code false} otherwise.
     */
    public boolean isUseFactionStandingSupportPointsSafe() {
        return trackFactionStanding && useFactionStandingSupportPoints;
    }

    public void setUseFactionStandingSupportPoints(boolean useFactionStandingSupportPoints) {
        this.useFactionStandingSupportPoints = useFactionStandingSupportPoints;
    }

    public boolean isTrackFactionStanding() {
        return trackFactionStanding;
    }

    public void setTrackFactionStanding(boolean trackFactionStanding) {
        this.trackFactionStanding = trackFactionStanding;
    }

    public boolean isTrackClimateRegardChanges() {
        return trackClimateRegardChanges;
    }

    public void setTrackClimateRegardChanges(boolean trackClimateRegardChanges) {
        this.trackClimateRegardChanges = trackClimateRegardChanges;
    }

    public double getRegardMultiplier() {
        return regardMultiplier;
    }

    public void setRegardMultiplier(double regardMultiplier) {
        this.regardMultiplier = regardMultiplier;
    }

    public boolean isAutoGenerateOpForCallSigns() {
        return autoGenerateOpForCallSigns;
    }

    public void setAutoGenerateOpForCallSigns(boolean autoGenerateOpForCallSigns) {
        this.autoGenerateOpForCallSigns = autoGenerateOpForCallSigns;
    }

    public SkillLevel getMinimumCallsignSkillLevel() {
        return minimumCallsignSkillLevel;
    }

    public void setMinimumCallsignSkillLevel(SkillLevel skillLevel) {
        this.minimumCallsignSkillLevel = skillLevel;
    }

    /**
     * Updates the campaign options to reflect the current game options settings.
     *
     * <p>
     * This method retrieves the {@link GameOptions} and updates the corresponding campaign-specific settings, such as
     * the use of tactics, initiative bonuses, toughness, artillery, pilot abilities, edge, implants, quirks, canon
     * restrictions, and allowed tech level. This synchronization ensures that the campaign options match the current
     * state of the game options.
     * </p>
     *
     * @param gameOptions the {@link GameOptions} whose values will be used to update the campaign options.
     */
    public void updateCampaignOptionsFromGameOptions(GameOptions gameOptions) {
        useTactics = gameOptions.getOption(RPG_COMMAND_INIT).booleanValue();
        useInitiativeBonus = gameOptions.getOption(RPG_INDIVIDUAL_INITIATIVE).booleanValue();
        useToughness = gameOptions.getOption(RPG_TOUGHNESS).booleanValue();
        useArtillery = gameOptions.getOption(RPG_ARTILLERY_SKILL).booleanValue();
        useAbilities = gameOptions.getOption(RPG_PILOT_ADVANTAGES).booleanValue();
        useEdge = gameOptions.getOption(EDGE).booleanValue();
        useImplants = gameOptions.getOption(RPG_MANEI_DOMINI).booleanValue();
        useQuirks = gameOptions.getOption(ADVANCED_STRATOPS_QUIRKS).booleanValue();
        allowCanonOnly = gameOptions.getOption(ALLOWED_CANON_ONLY).booleanValue();
        techLevel = getSimpleLevel(gameOptions.getOption(ALLOWED_TECH_LEVEL).stringValue());
    }

    /**
     * Updates the game options to reflect the current campaign options settings.
     *
     * <p>
     * This method synchronizes the values of the given {@link GameOptions} with the current campaign-specific options,
     * such as the use of tactics, initiative bonuses, toughness, artillery, pilot abilities, edge, implants, quirks,
     * canon restrictions, and allowed tech level. These updates ensure parity between the campaign options and the game
     * options.
     * </p>
     *
     * @param gameOptions the {@link GameOptions} to update based on the current campaign options.
     */
    public void updateGameOptionsFromCampaignOptions(GameOptions gameOptions) {
        gameOptions.getOption(RPG_INDIVIDUAL_INITIATIVE).setValue(useInitiativeBonus);
        gameOptions.getOption(RPG_COMMAND_INIT).setValue(useTactics || useInitiativeBonus);
        gameOptions.getOption(RPG_TOUGHNESS).setValue(useToughness);
        gameOptions.getOption(RPG_ARTILLERY_SKILL).setValue(useArtillery);
        gameOptions.getOption(RPG_PILOT_ADVANTAGES).setValue(useAbilities);
        gameOptions.getOption(EDGE).setValue(useEdge);
        gameOptions.getOption(RPG_MANEI_DOMINI).setValue(useImplants);
        gameOptions.getOption(ADVANCED_STRATOPS_QUIRKS).setValue(useQuirks);
        gameOptions.getOption(ALLOWED_CANON_ONLY).setValue(allowCanonOnly);
        gameOptions.getOption(ALLOWED_CANON_ONLY).setValue(allowCanonOnly);

        gameOptions.getOption(ALLOWED_TECH_LEVEL).setValue(TechConstants.T_SIMPLE_NAMES[techLevel]);
    }
}
