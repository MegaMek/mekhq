/*
 * Copyright (c) - 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import mekhq.Version;
import mekhq.campaign.againstTheBot.enums.AtBLanceRole;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.enums.FamilialRelationshipDisplayLevel;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.PrisonerCaptureStyle;
import mekhq.service.MassRepairOption;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.enums.BabySurnameStyle;
import mekhq.campaign.personnel.enums.TimeInDisplayFormat;
import mekhq.campaign.personnel.enums.Marriage;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialYearDuration;

/**
 * @author natit
 */
public class CampaignOptions implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 5698008431749303602L;

    //region Magic Numbers and Constants
    public static final int TECH_INTRO = 0;
    public static final int TECH_STANDARD = 1;
    public static final int TECH_ADVANCED = 2;
    public static final int TECH_EXPERIMENTAL = 3;
    public static final int TECH_UNOFFICIAL = 4;
    // This must always be the highest tech level in order to hide parts
    // that haven't been invented yet, or that are completely extinct
    public static final int TECH_UNKNOWN = 5;

    public static final int TRANSIT_UNIT_DAY = 0;
    public static final int TRANSIT_UNIT_WEEK = 1;
    public static final int TRANSIT_UNIT_MONTH = 2;
    public static final int TRANSIT_UNIT_NUM = 3;

    public static final String S_TECH = "Tech";
    public static final String S_AUTO = "Automatic Success";

    public static final int REPAIR_SYSTEM_STRATOPS = 0;
    public static final int REPAIR_SYSTEM_WARCHEST_CUSTOM = 1;
    public static final int REPAIR_SYSTEM_GENERIC_PARTS = 2;

    public static final double MAXIMUM_COMBAT_EQUIPMENT_PERCENT = 5.0;
    public static final double MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT = 1.0;
    public static final double MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT = 1.0;
    public static final double MAXIMUM_WARSHIP_EQUIPMENT_PERCENT = 1.0;

    public static final int PLANET_ACQUISITION_ALL = 0;
    public static final int PLANET_ACQUISITION_NEUTRAL = 1;
    public static final int PLANET_ACQUISITION_ALLY = 2;
    public static final int PLANET_ACQUISITION_SELF = 3;
    //endregion Magic Numbers and Constants

    //region Unlisted Variables
    private int repairSystem;
    public static final String[] REPAIR_SYSTEM_NAMES = {"Strat Ops", "Warchest Custom", "Generic Spare Parts"}; // FIXME: This needs to be localized

    //Mass Repair/Salvage Options
    private boolean massRepairUseRepair;
    private boolean massRepairUseSalvage;
    private boolean massRepairUseExtraTime;
    private boolean massRepairUseRushJob;
    private boolean massRepairAllowCarryover;
    private boolean massRepairOptimizeToCompleteToday;
    private boolean massRepairScrapImpossible;
    private boolean massRepairUseAssignedTechsFirst;
    private boolean massRepairReplacePod;
    private List<MassRepairOption> massRepairOptions;
    //endregion Unlisted Variables

    //region General Tab
    private UnitRatingMethod unitRatingMethod;
    private int manualUnitRatingModifier;
    //endregion General Tab

    //region Repair and Maintenance Tab
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
    private boolean useUnofficialMaintenance;
    private boolean logMaintenance;
    //endregion Repair and Maintenance Tab

    //region Supplies and Acquisition Tab
    // Acquisition
    private int waitingPeriod;
    private String acquisitionSkill;
    private boolean acquisitionSupportStaffOnly;
    private int clanAcquisitionPenalty;
    private int isAcquisitionPenalty;
    private int maxAcquisitions;

    // Delivery
    private int nDiceTransitTime;
    private int constantTransitTime;
    private int unitTransitTime;
    private int acquireMinimumTime;
    private int acquireMinimumTimeUnit;
    private int acquireMosBonus;
    private int acquireMosUnit;

    // Planetary Acquisition
    private boolean usePlanetaryAcquisition;
    private int maxJumpsPlanetaryAcquisition;
    private int planetAcquisitionFactionLimit;
    private boolean planetAcquisitionNoClanCrossover;
    private boolean noClanPartsFromIS;
    private int penaltyClanPartsFromIS;
    private boolean planetAcquisitionVerbose;
    private int[] planetTechAcquisitionBonus;
    private int[] planetIndustryAcquisitionBonus;
    private int[] planetOutputAcquisitionBonus;
    //endregion Supplies and Acquisition Tab

    //region Tech Limits Tab
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
    //endregion Tech Limits Tab

    //region Personnel Tab
    private boolean useTactics;
    private boolean useInitBonus;
    private boolean useToughness;
    private boolean useArtillery;
    private boolean useAbilities;
    private boolean useEdge;
    private boolean useSupportEdge;
    private boolean useImplants;
    private boolean altQualityAveraging;
    private boolean useAdvancedMedical; // Unofficial
    private boolean useDylansRandomXp; // Unofficial
    private int healWaitingPeriod;
    private int naturalHealingWaitingPeriod;
    private int minimumHitsForVees;
    private boolean useRandomHitsForVees;
    private boolean tougherHealing;
    private boolean useTransfers;
    private boolean useTimeInService;
    private TimeInDisplayFormat timeInServiceDisplayFormat;
    private boolean useTimeInRank;
    private TimeInDisplayFormat timeInRankDisplayFormat;
    private boolean useRetirementDateTracking;
    private boolean trackTotalEarnings;
    private boolean showOriginFaction;
    private boolean randomizeOrigin;
    private boolean randomizeDependentOrigin;
    private int originSearchRadius;
    private boolean isOriginExtraRandom;
    private double originDistanceScale;

    //family
    private int minimumMarriageAge;
    private int checkMutualAncestorsDepth;
    private boolean logMarriageNameChange;
    private boolean useManualMarriages;
    private boolean useRandomMarriages;
    private double chanceRandomMarriages;
    private int marriageAgeRange;
    private int[] randomMarriageSurnameWeights;
    private boolean useRandomSameSexMarriages;
    private double chanceRandomSameSexMarriages;
    private boolean useUnofficialProcreation;
    private double chanceProcreation;
    private boolean useUnofficialProcreationNoRelationship;
    private double chanceProcreationNoRelationship;
    private boolean displayTrueDueDate;
    private boolean logConception;
    private BabySurnameStyle babySurnameStyle;
    private boolean determineFatherAtBirth;
    private FamilialRelationshipDisplayLevel displayFamilyLevel;
    private boolean keepMarriedNameUponSpouseDeath;

    //salary
    private double salaryCommissionMultiplier;
    private double salaryEnlistedMultiplier;
    private double salaryAntiMekMultiplier;
    private double[] salaryXpMultiplier;
    private Money[] salaryTypeBase;

    //Prisoners
    private PrisonerCaptureStyle prisonerCaptureStyle;
    private PrisonerStatus defaultPrisonerStatus;
    private boolean prisonerBabyStatus;
    private boolean useAtBPrisonerDefection;
    private boolean useAtBPrisonerRansom;
    //endregion Personnel Tab

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
    private double clanPriceModifier;
    private double[] usedPartsValue;
    private double damagedPartsValue;
    private double canceledOrderReimbursement;
    //endregion Finance Tab

    //region Mercenary Tab
    private boolean equipmentContractBase;
    private double equipmentContractPercent;
    private boolean equipmentContractSaleValue;
    private double dropshipContractPercent;
    private double jumpshipContractPercent;
    private double warshipContractPercent;
    private boolean blcSaleValue;
    private boolean overageRepaymentInFinalPayment;
    //endregion Mercenary Tab

    //region Experience Tab
    private int scenarioXP;
    private int killXPAward;
    private int killsForXP;
    private int tasksXP;
    private int nTasksXP;
    private int successXP;
    private int mistakeXP;
    private int idleXP;
    private int monthsIdleXP;
    private int targetIdleXP;
    private int contractNegotiationXP;
    private int adminXP;
    private int adminXPPeriod;
    private int edgeCost;
    //endregion Experience Tab

    //region Skills Tab
    //endregion Skills Tab

    //region Special Abilities Tab
    //endregion Special Abilities Tab

    //region Skill Randomization Tab
    private int[] phenotypeProbabilities;
    //endregion Skill Randomization Tab

    //region Rank System Tab
    //endregion Rank System Tab

    //region Name and Portrait Generation
    private boolean useOriginFactionForNames;
    private boolean[] usePortraitForType;
    private boolean assignPortraitOnRoleChange;
    //endregion Name and Portrait Generation

    //region Personnel Market Tab
    private String personnelMarketName;
    private boolean personnelMarketReportRefresh;
    private int personnelMarketRandomEliteRemoval;
    private int personnelMarketRandomVeteranRemoval;
    private int personnelMarketRandomRegularRemoval;
    private int personnelMarketRandomGreenRemoval;
    private int personnelMarketRandomUltraGreenRemoval;
    private double personnelMarketDylansWeight;
    //endregion Personnel Market Tab

    //region Against the Bot Tab
    private boolean useAtB;
    private int skillLevel;

    // AtB Module: Unit Market // TODO : Fully Implement Me
    private boolean useAtBUnitMarket;

    // Unit Administration
    private boolean useShareSystem;
    private boolean sharesExcludeLargeCraft;
    private boolean sharesForAll;
    private boolean aeroRecruitsHaveUnits;
    private boolean retirementRolls;
    private boolean customRetirementMods;
    private boolean foundersNeverRetire;
    private boolean atbAddDependents;
    private boolean dependentsNeverLeave;
    private boolean trackUnitFatigue;
    private boolean useLeadership;
    private boolean trackOriginalUnit;
    private boolean useAero;
    private boolean useVehicles;
    private boolean clanVehicles;
    private boolean instantUnitMarketDelivery;
    private boolean contractMarketReportRefresh;
    private boolean unitMarketReportRefresh;

    // Contract Operations
    private int searchRadius;
    private boolean variableContractLength;
    private boolean mercSizeLimited;
    private boolean restrictPartsByMission;
    private boolean limitLanceWeight;
    private boolean limitLanceNumUnits;
    private boolean useStrategy;
    private int baseStrategyDeployment;
    private int additionalStrategyDeployment;
    private boolean adjustPaymentForStrategy;
    private int[] atbBattleChance;
    private boolean generateChases;

    // RATs
    private boolean staticRATs;
    private String[] rats;
    private boolean ignoreRatEra;

    // Scenarios
    private boolean doubleVehicles;
    private int opforLanceTypeMechs;
    private int opforLanceTypeMixed;
    private int opforLanceTypeVehicles;
    private boolean opforUsesVTOLs;
    private boolean allowOpforAeros;
    private int opforAeroChance;
    private boolean allowOpforLocalUnits;
    private int opforLocalUnitChance;
    private boolean adjustPlayerVehicles;
    private boolean regionalMechVariations;
    private boolean attachedPlayerCamouflage;
    private boolean playerControlsAttachedUnits;
    private boolean useDropShips;
    private boolean useWeatherConditions;
    private boolean useLightConditions;
    private boolean usePlanetaryConditions;
    //endregion Against the Bot Tab
    //endregion Variable Declarations

    //region Constructors
    public CampaignOptions() {
        //region Unlisted Variables
        repairSystem = REPAIR_SYSTEM_STRATOPS;

        //Mass Repair/Salvage Options
        massRepairUseRepair = true;
        massRepairUseSalvage = true;
        massRepairUseExtraTime = true;
        massRepairUseRushJob = true;
        massRepairAllowCarryover = true;
        massRepairOptimizeToCompleteToday = false;
        massRepairScrapImpossible = false;
        massRepairUseAssignedTechsFirst = false;
        massRepairReplacePod = true;
        massRepairOptions = new ArrayList<>();

        for (PartRepairType type : PartRepairType.values()) {
            massRepairOptions.add(new MassRepairOption(type));
        }
        //endregion Unlisted Variables

        //region General Tab
        unitRatingMethod = UnitRatingMethod.CAMPAIGN_OPS;
        manualUnitRatingModifier = 0;
        //endregion General Tab

        //region Repair and Maintenance Tab
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
        useUnofficialMaintenance = false;
        logMaintenance = false;
        //endregion Repair and Maintenance Tab

        //region Supplies and Acquisitions Tab
        // Acquisition
        waitingPeriod = 7;
        acquisitionSkill = S_TECH;
        acquisitionSupportStaffOnly = true;
        clanAcquisitionPenalty = 0;
        isAcquisitionPenalty = 0;
        maxAcquisitions = 0;

        // Delivery
        nDiceTransitTime = 1;
        constantTransitTime = 0;
        unitTransitTime = TRANSIT_UNIT_MONTH;
        acquireMinimumTime = 1;
        acquireMinimumTimeUnit = TRANSIT_UNIT_MONTH;
        acquireMosBonus = 1;
        acquireMosUnit = TRANSIT_UNIT_MONTH;

        // Planetary Acquisition
        usePlanetaryAcquisition = false;
        maxJumpsPlanetaryAcquisition = 2;
        planetAcquisitionFactionLimit = PLANET_ACQUISITION_NEUTRAL;
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
        //endregion Supplies and Acquisitions Tab

        //region Tech Limits Tab
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
        //endregion Tech Limits Tab

        //region Personnel Tab
        useTactics = false;
        useInitBonus = false;
        useToughness = false;
        useArtillery = false;
        useAbilities = false;
        useEdge = false;
        useSupportEdge = false;
        useImplants = false;
        altQualityAveraging = false;
        useAdvancedMedical = false;
        useDylansRandomXp = false;
        healWaitingPeriod = 1;
        naturalHealingWaitingPeriod = 15;
        minimumHitsForVees = 1;
        useRandomHitsForVees = false;
        tougherHealing = false;
        useTransfers = true;
        useTimeInService = false;
        timeInServiceDisplayFormat = TimeInDisplayFormat.YEARS;
        useTimeInRank = false;
        timeInRankDisplayFormat = TimeInDisplayFormat.MONTHS_YEARS;
        useRetirementDateTracking = false;
        trackTotalEarnings = false;
        showOriginFaction = true;
        randomizeOrigin = false;
        randomizeDependentOrigin = false;
        originSearchRadius = 45;
        isOriginExtraRandom = false;
        originDistanceScale = 0.6;

        //Family
        minimumMarriageAge = 16;
        checkMutualAncestorsDepth = 4;
        logMarriageNameChange = false;
        useManualMarriages = true;
        useRandomMarriages = false;
        chanceRandomMarriages = 0.00025;
        marriageAgeRange = 10;
        randomMarriageSurnameWeights = new int[Marriage.values().length - 1];
        randomMarriageSurnameWeights[Marriage.NO_CHANGE.getWeightsNumber()] = 100;
        randomMarriageSurnameWeights[Marriage.YOURS.getWeightsNumber()] = 55;
        randomMarriageSurnameWeights[Marriage.SPOUSE.getWeightsNumber()] = 55;
        randomMarriageSurnameWeights[Marriage.SPACE_YOURS.getWeightsNumber()] = 10;
        randomMarriageSurnameWeights[Marriage.BOTH_SPACE_YOURS.getWeightsNumber()] = 5;
        randomMarriageSurnameWeights[Marriage.HYP_YOURS.getWeightsNumber()] = 30;
        randomMarriageSurnameWeights[Marriage.BOTH_HYP_YOURS.getWeightsNumber()] = 20;
        randomMarriageSurnameWeights[Marriage.SPACE_SPOUSE.getWeightsNumber()] = 10;
        randomMarriageSurnameWeights[Marriage.BOTH_SPACE_SPOUSE.getWeightsNumber()] = 5;
        randomMarriageSurnameWeights[Marriage.HYP_SPOUSE.getWeightsNumber()] = 30;
        randomMarriageSurnameWeights[Marriage.BOTH_HYP_SPOUSE.getWeightsNumber()] = 20;
        randomMarriageSurnameWeights[Marriage.MALE.getWeightsNumber()] = 500;
        randomMarriageSurnameWeights[Marriage.FEMALE.getWeightsNumber()] = 160;
        useRandomSameSexMarriages = false;
        chanceRandomSameSexMarriages = 0.00002;
        useUnofficialProcreation = false;
        chanceProcreation = 0.0005;
        useUnofficialProcreationNoRelationship = false;
        chanceProcreationNoRelationship = 0.00005;
        displayTrueDueDate = false;
        logConception = false;
        babySurnameStyle = BabySurnameStyle.MOTHERS;
        determineFatherAtBirth = false;
        displayFamilyLevel = FamilialRelationshipDisplayLevel.SPOUSE;
        keepMarriedNameUponSpouseDeath = true;

        //Salary
        salaryAntiMekMultiplier = 1.5;
        salaryEnlistedMultiplier = 1.0;
        salaryCommissionMultiplier = 1.2;
        salaryXpMultiplier = new double[5];
        salaryXpMultiplier[SkillType.EXP_ULTRA_GREEN] = 0.6;
        salaryXpMultiplier[SkillType.EXP_GREEN] = 0.6;
        salaryXpMultiplier[SkillType.EXP_REGULAR] = 1.0;
        salaryXpMultiplier[SkillType.EXP_VETERAN] = 1.6;
        salaryXpMultiplier[SkillType.EXP_ELITE] = 3.2;
        salaryTypeBase = new Money[Person.T_NUM];
        salaryTypeBase[Person.T_NONE] = Money.of(0);
        salaryTypeBase[Person.T_MECHWARRIOR] = Money.of(1500);
        salaryTypeBase[Person.T_AERO_PILOT] = Money.of(1500);
        salaryTypeBase[Person.T_VEE_GUNNER] = Money.of(900);
        salaryTypeBase[Person.T_GVEE_DRIVER] = Money.of(900);
        salaryTypeBase[Person.T_NVEE_DRIVER] = Money.of(900);
        salaryTypeBase[Person.T_VTOL_PILOT] = Money.of(900);
        salaryTypeBase[Person.T_CONV_PILOT] = Money.of(900);
        salaryTypeBase[Person.T_INFANTRY] = Money.of(750);
        salaryTypeBase[Person.T_BA] = Money.of(960);
        salaryTypeBase[Person.T_SPACE_PILOT] = Money.of(1000);
        salaryTypeBase[Person.T_SPACE_GUNNER] = Money.of(1000);
        salaryTypeBase[Person.T_SPACE_CREW] = Money.of(1000);
        salaryTypeBase[Person.T_NAVIGATOR] = Money.of( 1000);
        salaryTypeBase[Person.T_DOCTOR] = Money.of(1500);
        salaryTypeBase[Person.T_ADMIN_COM] = Money.of(500);
        salaryTypeBase[Person.T_ADMIN_HR] = Money.of(500);
        salaryTypeBase[Person.T_ADMIN_LOG] = Money.of(500);
        salaryTypeBase[Person.T_ADMIN_TRA] = Money.of(500);
        salaryTypeBase[Person.T_MECH_TECH] = Money.of(800);
        salaryTypeBase[Person.T_AERO_TECH] = Money.of(800);
        salaryTypeBase[Person.T_BA_TECH] = Money.of(800);
        salaryTypeBase[Person.T_MECHANIC] = Money.of(800);
        salaryTypeBase[Person.T_ASTECH] = Money.of(400);
        salaryTypeBase[Person.T_MEDIC] = Money.of(400);
        salaryTypeBase[Person.T_PROTO_PILOT] = Money.of(960);
        salaryTypeBase[Person.T_LAM_PILOT] = Money.of(1500);
        salaryTypeBase[Person.T_VEHICLE_CREW] = Money.of(900);

        //Prisoner
        prisonerCaptureStyle = PrisonerCaptureStyle.TAHARQA;
        defaultPrisonerStatus = PrisonerStatus.PRISONER;
        prisonerBabyStatus = true;
        useAtBPrisonerDefection = false;
        useAtBPrisonerRansom = false;
        //endregion Personnel Tab

        //region Finances Tab
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
        financialYearDuration = FinancialYearDuration.DEFAULT_TYPE;
        newFinancialYearFinancesToCSVExport = false;
        clanPriceModifier = 1.0;
        usedPartsValue = new double[6];
        usedPartsValue[0] = 0.1;
        usedPartsValue[1] = 0.2;
        usedPartsValue[2] = 0.3;
        usedPartsValue[3] = 0.5;
        usedPartsValue[4] = 0.7;
        usedPartsValue[5] = 0.9;
        damagedPartsValue = 0.33;
        canceledOrderReimbursement = 0.5;
        //endregion Finances Tab

        //region Mercenary Tab
        equipmentContractBase = false;
        equipmentContractPercent = 5.0;
        equipmentContractSaleValue = false;
        dropshipContractPercent = 1.0;
        jumpshipContractPercent = 0.0;
        warshipContractPercent = 0.0;
        blcSaleValue = false;
        overageRepaymentInFinalPayment = false;
        //endregion Mercenary Tab

        //region Experience Tab
        scenarioXP = 1;
        killXPAward = 0;
        killsForXP = 0;
        tasksXP = 1;
        nTasksXP = 25;
        successXP = 0;
        mistakeXP = 0;
        idleXP = 0;
        monthsIdleXP = 2;
        targetIdleXP = 10;
        contractNegotiationXP = 0;
        adminXP = 0;
        adminXPPeriod = 1;
        edgeCost = 10;
        //endregion Experience Tab

        //region Skills Tab
        //endregion Skills Tab

        //region Special Abilities Tab
        //endregion Special Abilities Tab

        //region Skill Randomization Tab
        phenotypeProbabilities = new int[Phenotype.getExternalPhenotypes().size()];
        phenotypeProbabilities[Phenotype.MECHWARRIOR.getIndex()] = 95;
        phenotypeProbabilities[Phenotype.ELEMENTAL.getIndex()] = 100;
        phenotypeProbabilities[Phenotype.AEROSPACE.getIndex()] = 95;
        phenotypeProbabilities[Phenotype.VEHICLE.getIndex()] = 0;
        phenotypeProbabilities[Phenotype.PROTOMECH.getIndex()] = 95;
        phenotypeProbabilities[Phenotype.NAVAL.getIndex()] = 25;
        //endregion Skill Randomization Tab

        //region Rank System Tab
        //endregion Rank System Tab

        //region Name and Portrait Generation Tab
        useOriginFactionForNames = true;
        usePortraitForType = new boolean[Person.T_NUM];
        for (int i = 0; i < Person.T_NUM; i++) {
            usePortraitForType[i] = false;
        }
        usePortraitForType[Person.T_MECHWARRIOR] = true;
        assignPortraitOnRoleChange = false;
        //endregion Name and Portrait Generation Tab

        //region Personnel Market Tab
        personnelMarketName = PersonnelMarket.getTypeName(PersonnelMarket.TYPE_STRAT_OPS);
        personnelMarketReportRefresh = true;
        personnelMarketRandomEliteRemoval = 10;
        personnelMarketRandomVeteranRemoval = 8;
        personnelMarketRandomRegularRemoval = 6;
        personnelMarketRandomGreenRemoval = 4;
        personnelMarketRandomUltraGreenRemoval = 4;
        personnelMarketDylansWeight = 0.3;
        //endregion Personnel Market Tab

        //region Against the Bot Tab
        useAtB = false;
        skillLevel = 2;

        // AtB Module: Unit Market
        useAtBUnitMarket = false;

        // Unit Administration
        useShareSystem = false;
        sharesExcludeLargeCraft = false;
        sharesForAll = false;
        aeroRecruitsHaveUnits = false;
        retirementRolls = true;
        customRetirementMods = false;
        foundersNeverRetire = false;
        atbAddDependents = true;
        dependentsNeverLeave = false;
        trackUnitFatigue = false;
        useLeadership = true;
        trackOriginalUnit = false;
        useAero = false;
        useVehicles = true;
        clanVehicles = false;
        instantUnitMarketDelivery = false;
        contractMarketReportRefresh = true;
        unitMarketReportRefresh = true;

        // Contract Operations
        searchRadius = 800;
        variableContractLength = false;
        mercSizeLimited = false;
        restrictPartsByMission = true;
        limitLanceWeight = true;
        limitLanceNumUnits = true;
        useStrategy = true;
        baseStrategyDeployment = 3;
        additionalStrategyDeployment = 1;
        adjustPaymentForStrategy = false;
        atbBattleChance = new int[AtBLanceRole.values().length - 1];
        atbBattleChance[AtBLanceRole.FIGHTING.ordinal()] = 40;
        atbBattleChance[AtBLanceRole.DEFENCE.ordinal()] = 20;
        atbBattleChance[AtBLanceRole.SCOUTING.ordinal()] = 60;
        atbBattleChance[AtBLanceRole.TRAINING.ordinal()] = 10;
        generateChases = true;

        // RATs
        staticRATs = false;
        rats = new String[]{ "Xotl", "Total Warfare" }; // TODO : Localize me
        ignoreRatEra = false;

        // Scenarios
        doubleVehicles = false;
        opforLanceTypeMechs = 1;
        opforLanceTypeMixed = 2;
        opforLanceTypeVehicles = 3;
        opforUsesVTOLs = true;
        allowOpforAeros = false;
        opforAeroChance = 5;
        allowOpforLocalUnits = false;
        opforLocalUnitChance = 5;
        adjustPlayerVehicles = false;
        regionalMechVariations = false;
        attachedPlayerCamouflage = true;
        playerControlsAttachedUnits = false;
        useDropShips = false;
        useWeatherConditions = true;
        useLightConditions = true;
        usePlanetaryConditions = false;
        //endregion Against the Bot Tab
    }
    //endregion Constructors

    //region General Tab
    /**
     * @return the method of unit rating to use
     */
    public UnitRatingMethod getUnitRatingMethod() {
        return unitRatingMethod;
    }

    /**
     * @param method the method of unit rating to use
     */
    public void setUnitRatingMethod(UnitRatingMethod method) {
        this.unitRatingMethod = method;
    }

    public int getManualUnitRatingModifier() {
        return manualUnitRatingModifier;
    }

    public void setManualUnitRatingModifier(int manualUnitRatingModifier) {
        this.manualUnitRatingModifier = manualUnitRatingModifier;
    }
    //endregion General Tab

    //region Repair and Maintenance Tab
    //region Repair
    //endregion Repair

    //region Maintenance
    public boolean checkMaintenance() {
        return checkMaintenance;
    }

    public void setCheckMaintenance(boolean b) {
        checkMaintenance = b;
    }

    public int getMaintenanceCycleDays() {
        return maintenanceCycleDays;
    }

    public void setMaintenanceCycleDays(int d) {
        maintenanceCycleDays = d;
    }

    public int getMaintenanceBonus() {
        return maintenanceBonus;
    }

    public void setMaintenanceBonus(int d) {
        maintenanceBonus = d;
    }

    public boolean useQualityMaintenance() {
        return useQualityMaintenance;
    }

    public void setUseQualityMaintenance(boolean b) {
        useQualityMaintenance = b;
    }

    public boolean reverseQualityNames() {
        return reverseQualityNames;
    }

    public void setReverseQualityNames(boolean b) {
        reverseQualityNames = b;
    }

    public boolean useUnofficialMaintenance() {
        return useUnofficialMaintenance;
    }

    public void setUseUnofficialMaintenance(boolean b) {
        useUnofficialMaintenance = b;
    }

    public boolean logMaintenance() {
        return logMaintenance;
    }

    public void setLogMaintenance(boolean b) {
        logMaintenance = b;
    }
    //endregion Maintenance
    //endregion Repair and Maintenance Tab

    //region Supplies and Acquisitions Tab
    //endregion Supplies and Acquisitions Tab

    //region Personnel Tab
    public boolean useTactics() {
        return useTactics;
    }

    public void setUseTactics(boolean b) {
        this.useTactics = b;
    }

    public boolean useInitBonus() {
        return useInitBonus;
    }

    public void setInitBonus(boolean b) {
        this.useInitBonus = b;
    }

    public boolean useToughness() {
        return useToughness;
    }

    public void setToughness(boolean b) {
        this.useToughness = b;
    }

    public boolean useArtillery() {
        return useArtillery;
    }

    public void setArtillery(boolean b) {
        this.useArtillery = b;
    }

    public boolean useAbilities() {
        return useAbilities;
    }

    public void setAbilities(boolean b) {
        this.useAbilities = b;
    }

    public boolean useEdge() {
        return useEdge;
    }

    public void setEdge(boolean b) {
        this.useEdge = b;
    }

    public boolean useSupportEdge() {
        return useSupportEdge;
    }

    public void setSupportEdge(boolean b) {
        useSupportEdge = b;
    }

    public boolean useImplants() {
        return useImplants;
    }

    public void setImplants(boolean b) {
        this.useImplants = b;
    }

    public boolean useAltQualityAveraging() {
        return altQualityAveraging;
    }

    public void setAltQualityAveraging(boolean altQualityAveraging) {
        this.altQualityAveraging = altQualityAveraging;
    }

    public boolean useAdvancedMedical() {
        return useAdvancedMedical;
    }

    public void setAdvancedMedical(boolean b) {
        this.useAdvancedMedical = b;
    }

    public boolean useDylansRandomXp() {
        return useDylansRandomXp;
    }

    public void setDylansRandomXp(boolean b) {
        this.useDylansRandomXp = b;
    }

    public int getHealingWaitingPeriod() {
        return healWaitingPeriod;
    }

    public void setHealingWaitingPeriod(int d) {
        healWaitingPeriod = d;
    }

    public int getNaturalHealingWaitingPeriod() {
        return naturalHealingWaitingPeriod;
    }

    public void setNaturalHealingWaitingPeriod(int d) {
        naturalHealingWaitingPeriod = d;
    }

    public int getMinimumHitsForVees() {
        return minimumHitsForVees;
    }

    public void setMinimumHitsForVees(int d) {
        minimumHitsForVees = d;
    }

    public boolean useRandomHitsForVees() {
        return useRandomHitsForVees;
    }

    public void setUseRandomHitsForVees(boolean b) {
        useRandomHitsForVees = b;
    }

    public boolean useTougherHealing() {
        return tougherHealing;
    }

    public void setTougherHealing(boolean b) {
        tougherHealing = b;
    }

    public boolean useTransfers() {
        return useTransfers;
    }

    public void setUseTransfers(boolean b) {
        useTransfers = b;
    }

    /**
     * @return whether or not to use time in service
     */
    public boolean getUseTimeInService() {
        return useTimeInService;
    }

    /**
     * @param b the new value for whether to use time in service or not
     */
    public void setUseTimeInService(boolean b) {
        useTimeInService = b;
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
    public void setTimeInServiceDisplayFormat(TimeInDisplayFormat timeInServiceDisplayFormat) {
        this.timeInServiceDisplayFormat = timeInServiceDisplayFormat;
    }

    /**
     * @return whether or not to use time in rank
     */
    public boolean getUseTimeInRank() {
        return useTimeInRank;
    }

    /**
     * @param b the new value for whether or not to use time in rank
     */
    public void setUseTimeInRank(boolean b) {
        useTimeInRank = b;
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
    public void setTimeInRankDisplayFormat(TimeInDisplayFormat timeInRankDisplayFormat) {
        this.timeInRankDisplayFormat = timeInRankDisplayFormat;
    }

    /**
     * @return whether or not to track retirement dates
     */
    public boolean useRetirementDateTracking() {
        return useRetirementDateTracking;
    }

    /**
     * @param b the new value for whether or not to track retirement dates
     */
    public void setUseRetirementDateTracking(boolean b) {
        useRetirementDateTracking = b;
    }

    /**
     * @return whether or not to track the total earnings of personnel
     */
    public boolean trackTotalEarnings() {
        return trackTotalEarnings;
    }

    /**
     * @param b the new value for whether or not to track total earnings for personnel
     */
    public void setTrackTotalEarnings(boolean b) {
        trackTotalEarnings = b;
    }

    /**
     * Gets a value indicating whether or not to show a person's
     * origin faction when displaying their details.
     */
    public boolean showOriginFaction() {
        return showOriginFaction;
    }

    /**
     * Sets a value indicating whether or not to show a person's
     * origin faction when displaying their details.
     */
    public void setShowOriginFaction(boolean b) {
        showOriginFaction = b;
    }

    /**
     * Gets a value indicating whether or not to randomize the
     * origin of personnel.
     */
    public boolean randomizeOrigin() {
        return randomizeOrigin;
    }

    /**
     * Sets a value indicating whether or not to randomize
     * the origin of personnel.
     * @param b true for randomize, otherwise false
     */
    public void setRandomizeOrigin(boolean b) {
        randomizeOrigin = b;
    }

    /**
     * Gets a value indicating whether or not to randomize the origin of dependents
     */
    public boolean getRandomizeDependentOrigin() {
        return randomizeDependentOrigin;
    }

    /**
     * Sets a value indicating whether or not to randomize the origin of dependents
     * @param b true for randomize, otherwise false
     */
    public void setRandomizeDependentOrigin(boolean b) {
        randomizeDependentOrigin = b;
    }

    /**
     * Gets the search radius to use for randomizing
     * personnel origins.
     */
    public int getOriginSearchRadius() {
        return originSearchRadius;
    }

    /**
     * Sets the search radius to use for randomizing
     * personnel origins.
     * @param r The search radius.
     */
    public void setOriginSearchRadius(int r) {
        originSearchRadius = r;
    }

    /**
     * Gets a value indicating whether or not to randomize
     * origin to the planetary level, rather than just the
     * system level.
     */
    public boolean isOriginExtraRandom() {
        return isOriginExtraRandom;
    }

    /**
     * Sets a value indicating whether or not to randomize
     * origin to the planetary level, rather than just the
     * system level.
     */
    public void setOriginExtraRandom(boolean b) {
        isOriginExtraRandom = b;
    }

    /**
     * Gets the distance scale factor to apply when weighting
     * random origin planets.
     */
    public double getOriginDistanceScale() {
        return originDistanceScale;
    }

    /**
     * Sets the distance scale factor to apply when weighting
     * random origin planets (should be between 0.1 and 2).
     */
    public void setOriginDistanceScale(double v) {
        originDistanceScale = v;
    }

    //region family
    /**
     * @return the minimum age a person can get married at
     */
    public int getMinimumMarriageAge() {
        return minimumMarriageAge;
    }

    /**
     * @param b the minimum age a person can get married at
     */
    public void setMinimumMarriageAge(int b) {
        minimumMarriageAge = b;
    }

    /**
     * This gets the number of recursions to use when checking mutual ancestors between two personnel
     * @return the number of recursions to use
     */
    public int checkMutualAncestorsDepth() {
        return checkMutualAncestorsDepth;
    }

    /**
     * This sets the number of recursions to use when checking mutual ancestors between two personnel
     * @param b the number of recursions
     */
    public void setCheckMutualAncestorsDepth(int b) {
        checkMutualAncestorsDepth = b;
    }

    /**
     * @return whether or not to log a name change in a marriage
     */
    public boolean logMarriageNameChange() {
        return logMarriageNameChange;
    }

    /**
     * @param b whether to log marriage name changes or not
     */
    public void setLogMarriageNameChange(boolean b) {
        logMarriageNameChange = b;
    }

    /**
     * @return whether or not to use manual marriages
     */
    public boolean useManualMarriages() {
        return useManualMarriages;
    }

    /**
     * @param useManualMarriages whether or not to use manual marriages
     */
    public void setUseManualMarriages(boolean useManualMarriages) {
        this.useManualMarriages = useManualMarriages;
    }

    /**
     * @return whether or not to use random marriages
     */
    public boolean useRandomMarriages() {
        return useRandomMarriages;
    }

    /**
     * @param b whether or not to use random marriages
     */
    public void setUseRandomMarriages(boolean b) {
        useRandomMarriages = b;
    }

    /**
     * This gets the decimal chance (between 0 and 1) of a random marriage occurring
     * @return the chance, with a value between 0 and 1
     */
    public double getChanceRandomMarriages() {
        return chanceRandomMarriages;
    }

    /**
     * This sets the decimal chance (between 0 and 1) of a random marriage occurring
     * @param b the chance, with a value between 0 and 1
     */
    public void setChanceRandomMarriages(double b) {
        chanceRandomMarriages = b;
    }

    /**
     * A random marriage can only happen between two people whose ages differ (+/-) by the returned value
     * @return the age range ages can differ (+/-)
     */
    public int getMarriageAgeRange() {
        return marriageAgeRange;
    }

    /**
     * A random marriage can only happen between two people whose ages differ (+/-) by this value
     * @param b the maximum age range
     */
    public void setMarriageAgeRange(int b) {
        marriageAgeRange = b;
    }

    /**
     * @return the array of weights of potential surname changes for weighted marriage surname generation
     */
    public int[] getRandomMarriageSurnameWeights() {
        return randomMarriageSurnameWeights;
    }

    /**
     * This gets one of the values in the array of weights of potential surname changes for weighted marriage surname generation
     * @param pos the position in the array to set
     * @return the weight at index pos
     */
    public int getRandomMarriageSurnameWeights(int pos) {
        return randomMarriageSurnameWeights[pos];
    }

    /**
     * This sets one of the values in the array of weights of potential surname changes for weighted marriage surname generation
     * @param pos the position in the array to set
     * @param b the weight to use
     */
    public void setRandomMarriageSurnameWeight(int pos, int b) {
        randomMarriageSurnameWeights[pos] = b;
    }

    /**
     * @return whether or not to use random same sex marriages
     */
    public boolean useRandomSameSexMarriages() {
        return useRandomSameSexMarriages;
    }

    /**
     * @param b whether or not to use random same sex marriages
     */
    public void setUseRandomSameSexMarriages(boolean b) {
        useRandomSameSexMarriages = b;
    }

    /**
     * This gets the decimal chance (between 0 and 1) of a random same sex marriage occurring
     * @return the chance, with a value between 0 and 1
     */
    public double getChanceRandomSameSexMarriages() {
        return chanceRandomSameSexMarriages;
    }

    /**
     * This sets the decimal chance (between 0 and 1) of a random same sex marriage occurring
     * @param b the chance, with a value between 0 and 1
     */
    public void setChanceRandomSameSexMarriages(double b) {
        chanceRandomSameSexMarriages = b;
    }

    /**
     * @return whether or not to use unofficial procreation
     */
    public boolean useUnofficialProcreation() {
        return useUnofficialProcreation;
    }

    /**
     * @param b  whether or not to use unofficial procreation
     */
    public void setUseUnofficialProcreation(boolean b) {
        useUnofficialProcreation = b;
    }

    /**
     * This gets the decimal chance (between 0 and 1) of random procreation occurring
     * @return the chance, with a value between 0 and 1
     */
    public double getChanceProcreation() {
        return chanceProcreation;
    }

    /**
     * This sets the decimal chance (between 0 and 1) of random procreation occurring
     * @param b the chance, with a value between 0 and 1
     */
    public void setChanceProcreation(double b) {
        chanceProcreation = b;
    }

    /**
     * @return whether or not to use unofficial procreation without a relationship
     */
    public boolean useUnofficialProcreationNoRelationship() {
        return useUnofficialProcreationNoRelationship;
    }

    /**
     * @param b whether or not to use unofficial procreation without a relationship
     */
    public void setUseUnofficialProcreationNoRelationship(boolean b) {
        useUnofficialProcreationNoRelationship = b;
    }

    /**
     * This gets the decimal chance (between 0 and 1) of random procreation occurring without a relationship
     * @return the chance, with a value between 0 and 1
     */
    public double getChanceProcreationNoRelationship() {
        return chanceProcreationNoRelationship;
    }

    /**
     * This sets the decimal chance (between 0 and 1) of random procreation occurring without a relationship
     * @param b the chance, with a value between 0 and 1
     */
    public void setChanceProcreationNoRelationship(double b) {
        chanceProcreationNoRelationship = b;
    }

    /**
     * @return whether to show the expected or actual due date for personnel
     */
    public boolean getDisplayTrueDueDate() {
        return displayTrueDueDate;
    }

    /**
     * @param b whether to show the expected or actual due date for personnel
     */
    public void setDisplayTrueDueDate(boolean b) {
        displayTrueDueDate = b;
    }

    /**
     * @return whether to log conception
     */
    public boolean logConception() {
        return logConception;
    }

    /**
     * @param b whether to log conception
     */
    public void setLogConception(boolean b) {
        logConception = b;
    }

    /**
     * @return what style of surname to use for a baby
     */
    public BabySurnameStyle getBabySurnameStyle() {
        return babySurnameStyle;
    }

    /**
     * @param b the style of surname to use for a baby
     */
    public void setBabySurnameStyle(BabySurnameStyle b) {
        babySurnameStyle = b;
    }

    /**
     * @return whether or not to determine the father at birth instead of at conception
     */
    public boolean determineFatherAtBirth() {
        return determineFatherAtBirth;
    }

    /**
     * @param determineFatherAtBirth whether or not to determine the father at birth instead of at conception
     */
    public void setDetermineFatherAtBirth(boolean determineFatherAtBirth) {
        this.determineFatherAtBirth = determineFatherAtBirth;
    }

    /**
     * @return the level of familial relation to display
     */
    public FamilialRelationshipDisplayLevel getDisplayFamilyLevel() {
        return displayFamilyLevel;
    }

    /**
     * @param displayFamilyLevel the level of familial relation to display
     */
    public void setDisplayFamilyLevel(FamilialRelationshipDisplayLevel displayFamilyLevel) {
        this.displayFamilyLevel = displayFamilyLevel;
    }

    /**
     * @return whether to keep ones married name upon spouse death or not
     */
    public boolean getKeepMarriedNameUponSpouseDeath() {
        return keepMarriedNameUponSpouseDeath;
    }

    /**
     * @param b whether to keep ones married name upon spouse death or not
     */
    public void setKeepMarriedNameUponSpouseDeath(boolean b) {
        keepMarriedNameUponSpouseDeath = b;
    }
    //endregion family

    //region salary
    public double getSalaryCommissionMultiplier() {
        return salaryCommissionMultiplier;
    }

    public void setSalaryCommissionMultiplier(double d) {
        salaryCommissionMultiplier = d;
    }

    public double getSalaryEnlistedMultiplier() {
        return salaryEnlistedMultiplier;
    }

    public void setSalaryEnlistedMultiplier(double d) {
        salaryEnlistedMultiplier = d;
    }

    public double getSalaryAntiMekMultiplier() {
        return salaryAntiMekMultiplier;
    }

    public void setSalaryAntiMekMultiplier(double d) {
        salaryAntiMekMultiplier = d;
    }

    public double getSalaryXpMultiplier(int xp) {
        if (xp < 0 || xp >= salaryXpMultiplier.length) {
            return 1.0;
        }
        return salaryXpMultiplier[xp];
    }

    public void setSalaryXpMultiplier(double d, int xp) {
        if (xp < 0 || xp >= salaryXpMultiplier.length) {
            return;
        }
        this.salaryXpMultiplier[xp] = d;
    }

    public double getBaseSalary(int type) {
        if ((type < 0) || (type >= salaryTypeBase.length)) {
            return 0.0;
        }

        return salaryTypeBase[type].getAmount().doubleValue();
    }

    public Money getBaseSalaryMoney(int type) {
        if (type < 0 || type >= salaryTypeBase.length) {
            return Money.zero();
        }
        return salaryTypeBase[type];
    }

    public void setBaseSalary(int type, double base) {
        if ((type < 0) || (type >= salaryTypeBase.length)) {
            return;
        }

        this.salaryTypeBase[type] = Money.of(base);
    }
    //endregion salary

    //region Prisoners
    public PrisonerCaptureStyle getPrisonerCaptureStyle() {
        return prisonerCaptureStyle;
    }

    public void setPrisonerCaptureStyle(PrisonerCaptureStyle prisonerCaptureStyle) {
        this.prisonerCaptureStyle = prisonerCaptureStyle;
    }

    public PrisonerStatus getDefaultPrisonerStatus() {
        return defaultPrisonerStatus;
    }

    public void setDefaultPrisonerStatus(PrisonerStatus d) {
        defaultPrisonerStatus = d;
    }

    public boolean getPrisonerBabyStatus() {
        return prisonerBabyStatus;
    }

    public void setPrisonerBabyStatus(boolean prisonerBabyStatus) {
        this.prisonerBabyStatus = prisonerBabyStatus;
    }

    public boolean useAtBPrisonerDefection() {
        return useAtBPrisonerDefection;
    }

    public void setUseAtBPrisonerDefection(boolean useAtBPrisonerDefection) {
        this.useAtBPrisonerDefection = useAtBPrisonerDefection;
    }

    public boolean useAtBPrisonerRansom() {
        return useAtBPrisonerRansom;
    }

    public void setUseAtBPrisonerRansom(boolean useAtBPrisonerRansom) {
        this.useAtBPrisonerRansom = useAtBPrisonerRansom;
    }
    //endregion Prisoners
    //endregion Personnel Tab

    //region Finances Tab
    public boolean payForParts() {
        return payForParts;
    }

    public void setPayForParts(boolean b) {
        this.payForParts = b;
    }

    public boolean payForRepairs() {
        return payForRepairs;
    }

    public void setPayForRepairs(boolean b) {
        this.payForRepairs = b;
    }

    public boolean payForUnits() {
        return payForUnits;
    }

    public void setPayForUnits(boolean b) {
        this.payForUnits = b;
    }

    public boolean payForSalaries() {
        return payForSalaries;
    }

    public void setPayForSalaries(boolean b) {
        this.payForSalaries = b;
    }

    public boolean payForOverhead() {
        return payForOverhead;
    }

    public void setPayForOverhead(boolean b) {
        this.payForOverhead = b;
    }

    public boolean payForMaintain() {
        return payForMaintain;
    }

    public void setPayForMaintain(boolean b) {
        this.payForMaintain = b;
    }

    public boolean payForTransport() {
        return payForTransport;
    }

    public void setPayForTransport(boolean b) {
        this.payForTransport = b;
    }

    public boolean canSellUnits() {
        return sellUnits;
    }

    public void setSellUnits(boolean b) {
        this.sellUnits = b;
    }

    public boolean canSellParts() {
        return sellParts;
    }

    public void setSellParts(boolean b) {
        this.sellParts = b;
    }

    public boolean payForRecruitment() {
        return payForRecruitment;
    }

    public void setPayForRecruitment(boolean b) {
        this.payForRecruitment = b;
    }

    public boolean useLoanLimits() {
        return useLoanLimits;
    }

    public void setLoanLimits(boolean b) {
        this.useLoanLimits = b;
    }

    public boolean usePercentageMaint() {
        return usePercentageMaint;
    }

    public void setUsePercentageMaint(boolean b) {
        usePercentageMaint = b;
    }

    public boolean useInfantryDontCount() {
        return infantryDontCount;
    }

    public void setUseInfantryDontCount(boolean b) {
        infantryDontCount = b;
    }

    public boolean usePeacetimeCost() {
        return usePeacetimeCost;
    }

    public void setUsePeacetimeCost(boolean b) {
        this.usePeacetimeCost = b;
    }

    public boolean useExtendedPartsModifier() {
        return useExtendedPartsModifier;
    }

    public void setUseExtendedPartsModifier(boolean b) {
        this.useExtendedPartsModifier = b;
    }

    public boolean showPeacetimeCost() {
        return showPeacetimeCost;
    }

    public void setShowPeacetimeCost(boolean b) {
        this.showPeacetimeCost = b;
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
    public void setFinancialYearDuration(FinancialYearDuration financialYearDuration) {
        this.financialYearDuration = financialYearDuration;
    }

    /**
     * @return whether or not to export finances to CSV at the end of a financial year
     */
    public boolean getNewFinancialYearFinancesToCSVExport() {
        return newFinancialYearFinancesToCSVExport;
    }

    /**
     * @param b whether or not to export finances to CSV at the end of a financial year
     */
    public void setNewFinancialYearFinancesToCSVExport(boolean b) {
        newFinancialYearFinancesToCSVExport = b;
    }

    public double getClanPriceModifier() {
        return clanPriceModifier;
    }

    public void setClanPriceModifier(double d) {
        this.clanPriceModifier = d;
    }

    public double getUsedPartsValue(int quality) {
        return usedPartsValue[quality];
    }

    public void setUsedPartsValue(double d, int quality) {
        this.usedPartsValue[quality] = d;
    }

    public double getDamagedPartsValue() {
        return damagedPartsValue;
    }

    public void setDamagedPartsValue(double d) {
        this.damagedPartsValue = d;
    }

    public double GetCanceledOrderReimbursement() {
        return canceledOrderReimbursement;
    }

    public void setCanceledOrderReimbursement(double d) {
        this.canceledOrderReimbursement = d;
    }
    //endregion Finances Tab

    public static String getRepairSystemName(int repairSystem) {
        return REPAIR_SYSTEM_NAMES[repairSystem];
    }

    public static String getTechLevelName(int lvl) {
        switch (lvl) {
            case TECH_INTRO:
                return TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_INTRO];
            case TECH_STANDARD:
                return TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_STANDARD];
            case TECH_ADVANCED:
                return TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_ADVANCED];
            case TECH_EXPERIMENTAL:
                return TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_EXPERIMENTAL];
            case TECH_UNOFFICIAL:
                return TechConstants.T_SIMPLE_NAMES[TechConstants.T_SIMPLE_UNOFFICIAL];
            default:
                return "Unknown";
        }
    }

    public static String getFactionLimitName(int lvl) {
        switch (lvl) {
            case PLANET_ACQUISITION_ALL:
                return "All factions";
            case PLANET_ACQUISITION_NEUTRAL:
                return "Neutral or allied factions";
            case PLANET_ACQUISITION_ALLY:
                return "Allied factions";
            case PLANET_ACQUISITION_SELF:
                return "Only own faction";
            default:
                return "Unknown";
        }
    }

    public static String getTransitUnitName(int unit) {
        switch (unit) {
            case TRANSIT_UNIT_DAY:
                return "Days";
            case TRANSIT_UNIT_WEEK:
                return "Weeks";
            case TRANSIT_UNIT_MONTH:
                return "Months";
            default:
                return "Unknown";
        }
    }

    public boolean useEraMods() {
        return useEraMods;
    }

    public void setEraMods(boolean b) {
        this.useEraMods = b;
    }

    public boolean useAssignedTechFirst() {
        return assignedTechFirst;
    }

    public void setAssignedTechFirst(boolean assignedTechFirst) {
        this.assignedTechFirst = assignedTechFirst;
    }

    public boolean useResetToFirstTech() {
        return resetToFirstTech;
    }

    public void setResetToFirstTech(boolean resetToFirstTech) {
        this.resetToFirstTech = resetToFirstTech;
    }

    public int getRepairSystem() {
        return repairSystem;
    }

    public void setRepairSystem(int i) {
        this.repairSystem = i;
    }

    /**
     * @return true to use the origin faction for personnel names instead of a set faction
     */
    public boolean useOriginFactionForNames() {
        return useOriginFactionForNames;
    }

    /**
     * @param useOriginFactionForNames whether to use personnel names or a set faction
     */
    public void setUseOriginFactionForNames(boolean useOriginFactionForNames) {
        this.useOriginFactionForNames = useOriginFactionForNames;
    }

    // Personnel Market
    public boolean getPersonnelMarketReportRefresh() {
        return personnelMarketReportRefresh;
    }

    public void setPersonnelMarketReportRefresh(boolean b) {
        personnelMarketReportRefresh = b;
    }

    public String getPersonnelMarketType() {
        return personnelMarketName;
    }

    public void setPersonnelMarketType(String t) {
        personnelMarketName = t;
    }

    public int getPersonnelMarketRandomEliteRemoval() {
        return personnelMarketRandomEliteRemoval;
    }

    public void setPersonnelMarketRandomEliteRemoval(int i) {
        personnelMarketRandomEliteRemoval = i;
    }

    public int getPersonnelMarketRandomVeteranRemoval() {
        return personnelMarketRandomVeteranRemoval;
    }

    public void setPersonnelMarketRandomVeteranRemoval(int i) {
        personnelMarketRandomVeteranRemoval = i;
    }

    public int getPersonnelMarketRandomRegularRemoval() {
        return personnelMarketRandomRegularRemoval;
    }

    public void setPersonnelMarketRandomRegularRemoval(int i) {
        personnelMarketRandomRegularRemoval = i;
    }

    public int getPersonnelMarketRandomGreenRemoval() {
        return personnelMarketRandomGreenRemoval;
    }

    public void setPersonnelMarketRandomGreenRemoval(int i) {
        personnelMarketRandomGreenRemoval = i;
    }

    public int getPersonnelMarketRandomUltraGreenRemoval() {
        return personnelMarketRandomUltraGreenRemoval;
    }

    public void setPersonnelMarketRandomUltraGreenRemoval(int i) {
        personnelMarketRandomUltraGreenRemoval = i;
    }

    public double getPersonnelMarketDylansWeight() {
        return personnelMarketDylansWeight;
    }

    public void setPersonnelMarketDylansWeight(double d) {
        personnelMarketDylansWeight = d;
    }

    public boolean useQuirks() {
        return useQuirks;
    }

    public void setQuirks(boolean b) {
        this.useQuirks = b;
    }

    public int getScenarioXP() {
        return scenarioXP;
    }

    public void setScenarioXP(int xp) {
        scenarioXP = xp;
    }

    public int getKillsForXP() {
        return killsForXP;
    }

    public void setKillsForXP(int k) {
        killsForXP = k;
    }

    public int getKillXPAward() {
        return killXPAward;
    }

    public void setKillXPAward(int xp) {
        killXPAward = xp;
    }

    public int getNTasksXP() {
        return nTasksXP;
    }

    public void setNTasksXP(int xp) {
        nTasksXP = xp;
    }

    public int getTaskXP() {
        return tasksXP;
    }

    public void setTaskXP(int b) {
        tasksXP = b;
    }

    public int getMistakeXP() {
        return mistakeXP;
    }

    public void setMistakeXP(int b) {
        mistakeXP = b;
    }

    public int getSuccessXP() {
        return successXP;
    }

    public void setSuccessXP(int b) {
        successXP = b;
    }


    public boolean limitByYear() {
        return limitByYear;
    }

    public void setLimitByYear(boolean b) {
        limitByYear = b;
    }

    public boolean disallowExtinctStuff() {
        return disallowExtinctStuff;
    }

    public void setDisallowExtinctStuff(boolean b) {
        disallowExtinctStuff = b;
    }

    public boolean allowClanPurchases() {
        return allowClanPurchases;
    }

    public void setAllowClanPurchases(boolean b) {
        allowClanPurchases = b;
    }

    public boolean allowISPurchases() {
        return allowISPurchases;
    }

    public void setAllowISPurchases(boolean b) {
        allowISPurchases = b;
    }

    public boolean allowCanonOnly() {
        return allowCanonOnly;
    }

    public void setAllowCanonOnly(boolean b) {
        allowCanonOnly = b;
    }

    public boolean allowCanonRefitOnly() {
        return allowCanonRefitOnly;
    }

    public void setAllowCanonRefitOnly(boolean b) {
        allowCanonRefitOnly = b;
    }

    public boolean useVariableTechLevel() {
        return variableTechLevel;
    }

    public void setVariableTechLevel(boolean b) {
        variableTechLevel = b;
    }

    public void setFactionIntroDate(boolean b) {
        factionIntroDate = b;
    }

    public boolean useFactionIntroDate() {
        return factionIntroDate;
    }

    public boolean useAmmoByType() {
        return useAmmoByType;
    }

    public void setUseAmmoByType(boolean b) {
        useAmmoByType = b;
    }

    public int getTechLevel() {
        return techLevel;
    }

    public void setTechLevel(int lvl) {
        techLevel = lvl;
    }

    public int[] getPhenotypeProbabilities() {
        return phenotypeProbabilities;
    }

    public int getPhenotypeProbability(Phenotype phenotype) {
        return phenotypeProbabilities[phenotype.getIndex()];
    }

    public void setPhenotypeProbability(int index, int percentage) {
        phenotypeProbabilities[index] = percentage;
    }

    public boolean usePortraitForType(int type) {
        if (type < 0 || type >= usePortraitForType.length) {
            return false;
        }
        return usePortraitForType[type];
    }

    public void setUsePortraitForType(int type, boolean b) {
        if (type < 0 || type >= usePortraitForType.length) {
            return;
        }
        usePortraitForType[type] = b;
    }

    public boolean getAssignPortraitOnRoleChange() {
        return assignPortraitOnRoleChange;
    }

    public void setAssignPortraitOnRoleChange(boolean b) {
        assignPortraitOnRoleChange = b;
    }

    public int getIdleXP() {
        return idleXP;
    }

    public void setIdleXP(int xp) {
        idleXP = xp;
    }

    public int getTargetIdleXP() {
        return targetIdleXP;
    }

    public void setTargetIdleXP(int xp) {
        targetIdleXP = xp;
    }

    public int getMonthsIdleXP() {
        return monthsIdleXP;
    }

    public void setMonthsIdleXP(int m) {
        monthsIdleXP = m;
    }

    public int getContractNegotiationXP() {
        return contractNegotiationXP;
    }

    public void setContractNegotiationXP(int m) {
        contractNegotiationXP = m;
    }

    public int getAdminXP() {
        return adminXP;
    }

    public void setAdminXP(int m) {
        adminXP = m;
    }

    public int getAdminXPPeriod() {
        return adminXPPeriod;
    }

    public void setAdminXPPeriod(int m) {
        adminXPPeriod = m;
    }

    public int getEdgeCost() {
        return edgeCost;
    }

    public void setEdgeCost(int b) {
        edgeCost = b;
    }

    public int getWaitingPeriod() {
        return waitingPeriod;
    }

    public void setWaitingPeriod(int d) {
        waitingPeriod = d;
    }

    public String getAcquisitionSkill() {
        return acquisitionSkill;
    }

    public void setAcquisitionSkill(String skill) {
        acquisitionSkill = skill;
    }

    public void setAcquisitionSupportStaffOnly(boolean b) {
        this.acquisitionSupportStaffOnly = b;
    }

    public boolean isAcquisitionSupportStaffOnly() {
        return acquisitionSupportStaffOnly;
    }

    public int getNDiceTransitTime() {
        return nDiceTransitTime;
    }

    public void setNDiceTransitTime(int d) {
        nDiceTransitTime = d;
    }

    public int getConstantTransitTime() {
        return constantTransitTime;
    }

    public void setConstantTransitTime(int d) {
        constantTransitTime = d;
    }

    public int getUnitTransitTime() {
        return unitTransitTime;
    }

    public void setUnitTransitTime(int d) {
        unitTransitTime = d;
    }

    public int getAcquireMosUnit() {
        return acquireMosUnit;
    }

    public void setAcquireMosUnit(int b) {
        acquireMosUnit = b;
    }

    public int getAcquireMosBonus() {
        return acquireMosBonus;
    }

    public void setAcquireMosBonus(int b) {
        acquireMosBonus = b;
    }

    public int getAcquireMinimumTimeUnit() {
        return acquireMinimumTimeUnit;
    }

    public void setAcquireMinimumTimeUnit(int b) {
        acquireMinimumTimeUnit = b;
    }

    public int getAcquireMinimumTime() {
        return acquireMinimumTime;
    }

    public void setAcquireMinimumTime(int b) {
        acquireMinimumTime = b;
    }

    public boolean usesPlanetaryAcquisition() {
        return usePlanetaryAcquisition;
    }

    public void setPlanetaryAcquisition(boolean b) {
        usePlanetaryAcquisition = b;
    }

    public int getPlanetAcquisitionFactionLimit() {
        return planetAcquisitionFactionLimit;
    }

    public void setPlanetAcquisitionFactionLimit(int b) {
        planetAcquisitionFactionLimit = b;
    }

    public boolean disallowPlanetAcquisitionClanCrossover() {
        return planetAcquisitionNoClanCrossover;
    }

    public void setDisallowPlanetAcquisitionClanCrossover(boolean b) {
        planetAcquisitionNoClanCrossover = b;
    }

    public int getMaxJumpsPlanetaryAcquisition() {
        return maxJumpsPlanetaryAcquisition;
    }

    public void setMaxJumpsPlanetaryAcquisition(int m) {
        maxJumpsPlanetaryAcquisition = m;
    }

    public int getPenaltyClanPartsFroIS() {
        return penaltyClanPartsFromIS;
    }

    public void setPenaltyClanPartsFroIS(int i) {
        penaltyClanPartsFromIS = i ;
    }

    public boolean disallowClanPartsFromIS() {
        return noClanPartsFromIS;
    }

    public void setDisallowClanPartsFromIS(boolean b) {
        noClanPartsFromIS = b;
    }

    public boolean usePlanetAcquisitionVerboseReporting() {
        return planetAcquisitionVerbose;
    }

    public void setPlanetAcquisitionVerboseReporting(boolean b) {
        planetAcquisitionVerbose = b;
    }

    public double getEquipmentContractPercent() {
        return equipmentContractPercent;
    }

    public void setEquipmentContractPercent(double b) {
        equipmentContractPercent = Math.min(b, MAXIMUM_COMBAT_EQUIPMENT_PERCENT);
    }

    public boolean useEquipmentContractBase() {
        return equipmentContractBase;
    }

    public void setEquipmentContractBase(boolean b) {
        this.equipmentContractBase = b;
    }

    public boolean useEquipmentContractSaleValue() {
        return equipmentContractSaleValue;
    }

    public void setEquipmentContractSaleValue(boolean b) {
        this.equipmentContractSaleValue = b;
    }

    public double getDropshipContractPercent() {
        return dropshipContractPercent;
    }

    public void setDropshipContractPercent(double b) {
        dropshipContractPercent = Math.min(b, MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT);
    }

    public double getJumpshipContractPercent() {
        return jumpshipContractPercent;
    }

    public void setJumpshipContractPercent(double b) {
        jumpshipContractPercent = Math.min(b, MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT);
    }

    public double getWarshipContractPercent() {
        return warshipContractPercent;
    }

    public void setWarshipContractPercent(double b) {
        warshipContractPercent = Math.min(b, MAXIMUM_WARSHIP_EQUIPMENT_PERCENT);
    }

    public boolean useBLCSaleValue() {
        return blcSaleValue;
    }

    public void setBLCSaleValue(boolean b) {
        this.blcSaleValue = b;
    }

    public boolean getOverageRepaymentInFinalPayment() {
        return overageRepaymentInFinalPayment;
    }

    public void setOverageRepaymentInFinalPayment(boolean overageRepaymentInFinalPayment) {
        this.overageRepaymentInFinalPayment = overageRepaymentInFinalPayment;
    }

    public int getClanAcquisitionPenalty() {
        return clanAcquisitionPenalty;
    }

    public void setClanAcquisitionPenalty(int b) {
        clanAcquisitionPenalty = b;
    }

    public int getIsAcquisitionPenalty() {
        return isAcquisitionPenalty;
    }

    public void setIsAcquisitionPenalty(int b) {
        isAcquisitionPenalty = b;
    }

    public int getPlanetTechAcquisitionBonus(int type) {
        if (type < 0 || type >= planetTechAcquisitionBonus.length) {
            return 0;
        }
        return planetTechAcquisitionBonus[type];
    }

    public void setPlanetTechAcquisitionBonus(int base, int type) {
        if (type < 0 || type >= planetTechAcquisitionBonus.length) {
            return;
        }
        this.planetTechAcquisitionBonus[type] = base;
    }

    public int getPlanetIndustryAcquisitionBonus(int type) {
        if (type < 0 || type >= planetIndustryAcquisitionBonus.length) {
            return 0;
        }
        return planetIndustryAcquisitionBonus[type];
    }

    public void setPlanetIndustryAcquisitionBonus(int base, int type) {
        if (type < 0 || type >= planetIndustryAcquisitionBonus.length) {
            return;
        }
        this.planetIndustryAcquisitionBonus[type] = base;
    }

    public int getPlanetOutputAcquisitionBonus(int type) {
        if (type < 0 || type >= planetOutputAcquisitionBonus.length) {
            return 0;
        }
        return planetOutputAcquisitionBonus[type];
    }

    public void setPlanetOutputAcquisitionBonus(int base, int type) {
        if (type < 0 || type >= planetOutputAcquisitionBonus.length) {
            return;
        }
        this.planetOutputAcquisitionBonus[type] = base;
    }

    public boolean isDestroyByMargin() {
        return destroyByMargin;
    }

    public void setDestroyByMargin(boolean b) {
        destroyByMargin = b;
    }

    public int getDestroyMargin() {
        return destroyMargin;
    }

    public void setDestroyMargin(int d) {
        destroyMargin = d;
    }

    public int getDestroyPartTarget() {
        return destroyPartTarget;
    }

    public void setDestroyPartTarget(int d) {
        destroyPartTarget = d;
    }

    public boolean useAeroSystemHits() {
        return useAeroSystemHits;
    }

    public void setUseAeroSystemHits(boolean b) {
        useAeroSystemHits = b;
    }

    public int getMaxAcquisitions() {
        return maxAcquisitions;
    }

    public void setMaxAcquisitions(int d) {
        maxAcquisitions = d;
    }

    public boolean getUseAtB() {
        return useAtB;
    }

    public void setUseAtB(boolean useAtB) {
        this.useAtB = useAtB;
    }

    public boolean getUseAtBUnitMarket() {
        return useAtBUnitMarket;
    }

    public void setUseAtBUnitMarket(boolean useAtBUnitMarket) {
        this.useAtBUnitMarket = useAtBUnitMarket;
    }

    public boolean getUseAero() {
        return useAero;
    }

    public void setUseAero(boolean useAero) {
        this.useAero = useAero;
    }

    public boolean getUseVehicles() {
        return useVehicles;
    }

    public void setUseVehicles(boolean useVehicles) {
        this.useVehicles = useVehicles;
    }

    public boolean getClanVehicles() {
        return clanVehicles;
    }

    public void setClanVehicles(boolean clanVehicles) {
        this.clanVehicles = clanVehicles;
    }

    public boolean getDoubleVehicles() {
        return doubleVehicles;
    }

    public void setDoubleVehicles(boolean doubleVehicles) {
        this.doubleVehicles = doubleVehicles;
    }

    public boolean getAdjustPlayerVehicles() {
        return adjustPlayerVehicles;
    }

    public int getOpforLanceTypeMechs() {
        return opforLanceTypeMechs;
    }

    public void setOpforLanceTypeMechs(int weight) {
        opforLanceTypeMechs = weight;
    }

    public int getOpforLanceTypeMixed() {
        return opforLanceTypeMixed;
    }

    public void setOpforLanceTypeMixed(int weight) {
        opforLanceTypeMixed = weight;
    }

    public int getOpforLanceTypeVehicles() {
        return opforLanceTypeVehicles;
    }

    public void setOpforLanceTypeVehicles(int weight) {
        opforLanceTypeVehicles = weight;
    }

    public boolean getOpforUsesVTOLs() {
        return opforUsesVTOLs;
    }

    public void setOpforUsesVTOLs(boolean vtol) {
        opforUsesVTOLs = vtol;
    }

    public void setAdjustPlayerVehicles(boolean adjust) {
        adjustPlayerVehicles = adjust;
    }

    public boolean getUseDropShips() {
        return useDropShips;
    }

    public void setUseDropShips(boolean useDropShips) {
        this.useDropShips = useDropShips;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(int level) {
        skillLevel = level;
    }

    public boolean getAeroRecruitsHaveUnits() {
        return aeroRecruitsHaveUnits;
    }

    public void setAeroRecruitsHaveUnits(boolean haveUnits) {
        aeroRecruitsHaveUnits = haveUnits;
    }

    public boolean getUseShareSystem() {
        return useShareSystem;
    }

    public boolean getSharesExcludeLargeCraft() {
        return sharesExcludeLargeCraft;
    }

    public void setSharesExcludeLargeCraft(boolean exclude) {
        sharesExcludeLargeCraft = exclude;
    }

    public boolean getSharesForAll() {
        return sharesForAll;
    }

    public void setSharesForAll(boolean set) {
        sharesForAll = set;
    }

    public boolean doRetirementRolls() {
        return retirementRolls;
    }

    public void setRetirementRolls(boolean roll) {
        retirementRolls = roll;
    }

    public boolean getCustomRetirementMods() {
        return customRetirementMods;
    }

    public void setCustomRetirementMods(boolean mods) {
        customRetirementMods = mods;
    }

    public boolean getFoundersNeverRetire() {
        return foundersNeverRetire;
    }

    public void setFoundersNeverRetire(boolean mods) {
        foundersNeverRetire = mods;
    }

    public boolean canAtBAddDependents() {
        return atbAddDependents;
    }

    public void setAtBAddDependents(boolean b) {
        atbAddDependents = b;
    }

    public boolean getDependentsNeverLeave() {
        return dependentsNeverLeave;
    }

    public void setDependentsNeverLeave(boolean b) {
        dependentsNeverLeave = b;
    }

    public boolean getTrackOriginalUnit() {
        return trackOriginalUnit;
    }

    public void setTrackOriginalUnit(boolean track) {
        trackOriginalUnit = track;
    }

    public boolean isMercSizeLimited() {
        return mercSizeLimited;
    }

    public boolean getTrackUnitFatigue() {
        return trackUnitFatigue;
    }

    public void setTrackUnitFatigue(boolean fatigue) {
        trackUnitFatigue = fatigue;
    }

    public void setMercSizeLimited(boolean limit) {
        mercSizeLimited = limit;
    }

    public void setUseShareSystem(boolean shares) {
        useShareSystem = shares;
    }

    public boolean getRegionalMechVariations() {
        return regionalMechVariations;
    }

    public void setRegionalMechVariations(boolean regionalMechVariations) {
        this.regionalMechVariations = regionalMechVariations;
    }

    public boolean getAttachedPlayerCamouflage() {
        return attachedPlayerCamouflage;
    }

    public void setAttachedPlayerCamouflage(boolean attachedPlayerCamouflage) {
        this.attachedPlayerCamouflage = attachedPlayerCamouflage;
    }

    public boolean getPlayerControlsAttachedUnits() {
        return playerControlsAttachedUnits;
    }

    public void setPlayerControlsAttachedUnits(boolean playerControlsAttachedUnits) {
        this.playerControlsAttachedUnits = playerControlsAttachedUnits;
    }

    public String[] getRATs() {
        return rats;
    }

    public void setRATs(String[] rats) {
        this.rats = rats;
    }

    public boolean useStaticRATs() {
        return staticRATs;
    }

    public void setStaticRATs(boolean staticRATs) {
        this.staticRATs = staticRATs;
    }

    public boolean canIgnoreRatEra() {
        return ignoreRatEra;
    }

    public void setIgnoreRatEra(boolean ignore) {
        ignoreRatEra = ignore;
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(int radius) {
        searchRadius = radius;
    }

    public boolean getInstantUnitMarketDelivery() {
        return instantUnitMarketDelivery;
    }

    public void setInstantUnitMarketDelivery(boolean instant) {
        instantUnitMarketDelivery = instant;
    }

    /**
     * @param role the {@link AtBLanceRole} to get the battle chance for
     * @return the chance of having a battle for the specified role
     */
    public int getAtBBattleChance(AtBLanceRole role) {
        return (role == AtBLanceRole.UNASSIGNED) ? 0 : atbBattleChance[role.ordinal()];
    }

    /**
     * @param role      the {@link AtBLanceRole} ordinal value
     * @param frequency the frequency to set the generation to (percent chance from 0 to 100)
     */
    public void setAtBBattleChance(int role, int frequency) {
        if (frequency < 0) {
            frequency = 0;
        } else if (frequency > 100) {
            frequency = 100;
        }

        this.atbBattleChance[role] = frequency;
    }

    public boolean generateChases() {
        return generateChases;
    }

    public void setGenerateChases(boolean generateChases) {
        this.generateChases = generateChases;
    }

    public boolean getVariableContractLength() {
        return variableContractLength;
    }

    public void setVariableContractLength(boolean variable) {
        variableContractLength = variable;
    }

    public boolean getUseWeatherConditions() {
        return useWeatherConditions;
    }

    public void setUseWeatherConditions(boolean useWeatherConditions) {
        this.useWeatherConditions = useWeatherConditions;
    }

    public boolean getUseLightConditions() {
        return useLightConditions;
    }

    public void setUseLightConditions(boolean useLightConditions) {
        this.useLightConditions = useLightConditions;
    }

    public boolean getUsePlanetaryConditions() {
        return usePlanetaryConditions;
    }

    public void setUsePlanetaryConditions(boolean usePlanetaryConditions) {
        this.usePlanetaryConditions = usePlanetaryConditions;
    }

    public boolean getUseLeadership() {
        return useLeadership;
    }

    public void setUseLeadership(boolean useLeadership) {
        this.useLeadership = useLeadership;
    }

    public boolean getUseStrategy() {
        return useStrategy;
    }

    public void setUseStrategy(boolean useStrategy) {
        this.useStrategy = useStrategy;
    }

    public int getBaseStrategyDeployment() {
        return baseStrategyDeployment;
    }

    public void setBaseStrategyDeployment(int baseStrategyDeployment) {
        this.baseStrategyDeployment = baseStrategyDeployment;
    }

    public int getAdditionalStrategyDeployment() {
        return additionalStrategyDeployment;
    }

    public void setAdditionalStrategyDeployment(int additionalStrategyDeployment) {
        this.additionalStrategyDeployment = additionalStrategyDeployment;
    }

    public boolean getAdjustPaymentForStrategy() {
        return adjustPaymentForStrategy;
    }

    public void setAdjustPaymentForStrategy(boolean adjustPaymentForStrategy) {
        this.adjustPaymentForStrategy = adjustPaymentForStrategy;
    }

    public boolean getRestrictPartsByMission() {
        return restrictPartsByMission;
    }

    public void setRestrictPartsByMission(boolean restrictPartsByMission) {
        this.restrictPartsByMission = restrictPartsByMission;
    }

    public boolean getLimitLanceWeight() {
        return limitLanceWeight;
    }

    public void setLimitLanceWeight(boolean limit) {
        limitLanceWeight = limit;
    }

    public boolean getLimitLanceNumUnits() {
        return limitLanceNumUnits;
    }

    public void setLimitLanceNumUnits(boolean limit) {
        limitLanceNumUnits = limit;
    }

    public boolean getContractMarketReportRefresh() {
        return contractMarketReportRefresh;
    }

    public void setContractMarketReportRefresh(boolean refresh) {
        contractMarketReportRefresh = refresh;
    }

    public boolean getUnitMarketReportRefresh() {
        return unitMarketReportRefresh;
    }

    public void setUnitMarketReportRefresh(boolean refresh) {
        unitMarketReportRefresh = refresh;
    }

    //region Mass Repair/ Mass Salvage
    public boolean massRepairUseRepair() {
        return massRepairUseRepair;
    }

    public void setMassRepairUseRepair(boolean massRepairUseRepair) {
        this.massRepairUseRepair = massRepairUseRepair;
    }

    public boolean massRepairUseSalvage() {
        return massRepairUseSalvage;
    }

    public void setMassRepairUseSalvage(boolean massRepairUseSalvage) {
        this.massRepairUseSalvage = massRepairUseSalvage;
    }

    public boolean massRepairUseExtraTime() {
        return massRepairUseExtraTime;
    }

    public void setMassRepairUseExtraTime(boolean b) {
        this.massRepairUseExtraTime = b;
    }

    public boolean massRepairUseRushJob() {
        return massRepairUseRushJob;
    }

    public void setMassRepairUseRushJob(boolean b) {
        this.massRepairUseRushJob = b;
    }

    public boolean massRepairAllowCarryover() {
        return massRepairAllowCarryover;
    }

    public void setMassRepairAllowCarryover(boolean b) {
        this.massRepairAllowCarryover = b;
    }

    public boolean massRepairOptimizeToCompleteToday() {
        return massRepairOptimizeToCompleteToday;
    }

    public void setMassRepairOptimizeToCompleteToday(boolean massRepairOptimizeToCompleteToday) {
        this.massRepairOptimizeToCompleteToday = massRepairOptimizeToCompleteToday;
    }

    public boolean massRepairScrapImpossible() {
        return massRepairScrapImpossible;
    }

    public void setMassRepairScrapImpossible(boolean b) {
        this.massRepairScrapImpossible = b;
    }

    public boolean massRepairUseAssignedTechsFirst() {
        return massRepairUseAssignedTechsFirst;
    }

    public void setMassRepairUseAssignedTechsFirst(boolean massRepairUseAssignedTechsFirst) {
        this.massRepairUseAssignedTechsFirst = massRepairUseAssignedTechsFirst;
    }

    public void setMassRepairReplacePod(boolean setMassRepairReplacePod) {
        this.massRepairReplacePod = setMassRepairReplacePod;
    }

    public boolean massRepairReplacePod() {
        return massRepairReplacePod;
    }

    public List<MassRepairOption> getMassRepairOptions() {
        return (massRepairOptions != null) ? massRepairOptions : new ArrayList<>();
    }

    public void setMassRepairOptions(List<MassRepairOption> massRepairOptions) {
        this.massRepairOptions = massRepairOptions;
    }

    public void addMassRepairOption(MassRepairOption mro) {
        if (mro.getType() == PartRepairType.UNKNOWN_LOCATION) {
            return;
        }

        getMassRepairOptions().removeIf(massRepairOption -> massRepairOption.getType() == mro.getType());
        getMassRepairOptions().add(mro);
    }
    //endregion Mass Repair/ Mass Salvage

    public void setAllowOpforAeros(boolean allowOpforAeros) {
        this.allowOpforAeros = allowOpforAeros;
    }

    public boolean getAllowOpforAeros() {
        return allowOpforAeros;
    }

    public void setAllowOpforLocalUnits(boolean allowOpforLocalUnits) {
        this.allowOpforLocalUnits = allowOpforLocalUnits;
    }

    public boolean getAllowOpforLocalUnits() {
        return allowOpforLocalUnits;
    }

    public void setOpforAeroChance(int chance) {
        this.opforAeroChance = chance;
    }

    public int getOpforAeroChance() {
        return opforAeroChance;
    }

    public void setOpforLocalUnitChance(int chance) {
        this.opforLocalUnitChance = chance;
    }

    public int getOpforLocalUnitChance() {
        return opforLocalUnitChance;
    }

    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<campaignOptions>");
        //region General Tab
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "manualUnitRatingModifier", getManualUnitRatingModifier());
        //endregion General Tab

        //region Repair and Maintenance Tab
        //region Maintenance
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "logMaintenance", logMaintenance);
        //endregion Maintenance
        //endregion Repair and Maintenance Tab

        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useFactionForNames", useOriginFactionForNames);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "repairSystem", repairSystem);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "unitRatingMethod", unitRatingMethod.name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useEraMods", useEraMods);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "assignedTechFirst", assignedTechFirst);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "resetToFirstTech", resetToFirstTech);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useTactics", useTactics);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useInitBonus", useInitBonus);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useToughness", useToughness);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useArtillery", useArtillery);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAbilities", useAbilities);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useEdge", useEdge);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useSupportEdge", useSupportEdge);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useImplants", useImplants);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "altQualityAveraging", altQualityAveraging);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAdvancedMedical", useAdvancedMedical);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useDylansRandomXp", useDylansRandomXp);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useQuirks", useQuirks);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "showOriginFaction", showOriginFaction);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "randomizeOrigin", randomizeOrigin);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "randomizeDependentOrigin", randomizeDependentOrigin);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "originSearchRadius", originSearchRadius);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "isOriginExtraRandom", isOriginExtraRandom);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "scenarioXP", scenarioXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "killsForXP", killsForXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "killXPAward", killXPAward);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "nTasksXP", nTasksXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "tasksXP", tasksXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "mistakeXP", mistakeXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "successXP", successXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "idleXP", idleXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "targetIdleXP", targetIdleXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "monthsIdleXP", monthsIdleXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "contractNegotiationXP", contractNegotiationXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "adminWeeklyXP", adminXP);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "adminXPPeriod", adminXPPeriod);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "edgeCost", edgeCost);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "limitByYear", limitByYear);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "disallowExtinctStuff", disallowExtinctStuff);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "allowClanPurchases", allowClanPurchases);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "allowISPurchases", allowISPurchases);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "allowCanonOnly", allowCanonOnly);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "allowCanonRefitOnly", allowCanonRefitOnly);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "variableTechLevel", variableTechLevel);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "factionIntroDate", factionIntroDate);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAmmoByType", useAmmoByType);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "waitingPeriod", waitingPeriod);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "acquisitionSkill", acquisitionSkill);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "acquisitionSupportStaffOnly", acquisitionSupportStaffOnly);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "techLevel", techLevel);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "nDiceTransitTime", nDiceTransitTime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "constantTransitTime", constantTransitTime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "unitTransitTime", unitTransitTime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "acquireMosBonus", acquireMosBonus);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "acquireMosUnit", acquireMosUnit);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "acquireMinimumTime", acquireMinimumTime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "acquireMinimumTimeUnit", acquireMinimumTimeUnit);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usePlanetaryAcquisition", usePlanetaryAcquisition);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "planetAcquisitionFactionLimit", planetAcquisitionFactionLimit);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "planetAcquisitionNoClanCrossover", planetAcquisitionNoClanCrossover);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "noClanPartsFromIS", noClanPartsFromIS);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "penaltyClanPartsFromIS", penaltyClanPartsFromIS);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "planetAcquisitionVerbose", planetAcquisitionVerbose);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "maxJumpsPlanetaryAcquisition", maxJumpsPlanetaryAcquisition);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipmentContractPercent", equipmentContractPercent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "dropshipContractPercent", dropshipContractPercent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "jumpshipContractPercent", jumpshipContractPercent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "warshipContractPercent", warshipContractPercent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipmentContractBase", equipmentContractBase);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipmentContractSaleValue", equipmentContractSaleValue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "blcSaleValue", blcSaleValue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "overageRepaymentInFinalPayment", overageRepaymentInFinalPayment);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "clanAcquisitionPenalty", clanAcquisitionPenalty);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "isAcquisitionPenalty", isAcquisitionPenalty);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "healWaitingPeriod", healWaitingPeriod);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "naturalHealingWaitingPeriod", naturalHealingWaitingPeriod);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "destroyByMargin", destroyByMargin);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "destroyMargin", destroyMargin);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "destroyPartTarget", destroyPartTarget);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAeroSystemHits", useAeroSystemHits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "maintenanceCycleDays", maintenanceCycleDays);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "maintenanceBonus", maintenanceBonus);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useQualityMaintenance", useQualityMaintenance);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "reverseQualityNames", reverseQualityNames);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useUnofficalMaintenance", useUnofficialMaintenance);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "checkMaintenance", checkMaintenance);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useRandomHitsForVees", useRandomHitsForVees);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "minimumHitsForVees", minimumHitsForVees);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "maxAcquisitions", maxAcquisitions);

        //region Personnel Tab
        //region family
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "minimumMarriageAge", minimumMarriageAge);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "checkMutualAncestorsDepth", checkMutualAncestorsDepth);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "logMarriageNameChange", logMarriageNameChange);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useManualMarriages", useManualMarriages);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useRandomMarriages", useRandomMarriages);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "chanceRandomMarriages", chanceRandomMarriages);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "marriageAgeRange", marriageAgeRange);
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                + "<randomMarriageSurnameWeights>"
                + StringUtils.join(randomMarriageSurnameWeights, ',')
                + "</randomMarriageSurnameWeights>");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useRandomSameSexMarriages", useRandomSameSexMarriages);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "chanceRandomSameSexMarriages", chanceRandomSameSexMarriages);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useUnofficialProcreation", useUnofficialProcreation);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "chanceProcreation", chanceProcreation);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useUnofficialProcreationNoRelationship", useUnofficialProcreationNoRelationship);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "chanceProcreationNoRelationship", chanceProcreationNoRelationship);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "displayTrueDueDate", displayTrueDueDate);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "logConception", logConception);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "babySurnameStyle", babySurnameStyle.name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "determineFatherAtBirth", determineFatherAtBirth);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "displayFamilyLevel", displayFamilyLevel.name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "keepMarriedNameUponSpouseDeath", keepMarriedNameUponSpouseDeath);
        //endregion family

        //region Prisoners
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "prisonerCaptureStyle", prisonerCaptureStyle.name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "defaultPrisonerStatus", defaultPrisonerStatus.name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "prisonerBabyStatus", prisonerBabyStatus);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAtBPrisonerDefection", useAtBPrisonerDefection);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAtBPrisonerRansom", useAtBPrisonerRansom);
        //endregion Prisoners
        //endregion Personnel Tab

        //region Finances Tab
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForParts", payForParts);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForRepairs", payForRepairs);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForUnits", payForUnits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForSalaries", payForSalaries);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForOverhead", payForOverhead);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForMaintain", payForMaintain);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForTransport", payForTransport);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "sellUnits", sellUnits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "sellParts", sellParts);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForRecruitment", payForRecruitment);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useLoanLimits", useLoanLimits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usePercentageMaint", usePercentageMaint);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "infantryDontCount", infantryDontCount);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usePeacetimeCost", usePeacetimeCost);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useExtendedPartsModifier", useExtendedPartsModifier);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "showPeacetimeCost", showPeacetimeCost);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "financialYearDuration", financialYearDuration.name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "newFinancialYearFinancesToCSVExport", newFinancialYearFinancesToCSVExport);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "clanPriceModifier", clanPriceModifier);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueA", usedPartsValue[0]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueB", usedPartsValue[1]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueC", usedPartsValue[2]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueD", usedPartsValue[3]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueE", usedPartsValue[4]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueF", usedPartsValue[5]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "damagedPartsValue", damagedPartsValue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "canceledOrderReimbursement", canceledOrderReimbursement);
        //endregion Finances Tab

        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useTransfers", useTransfers);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useTimeInService", useTimeInService);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "timeInServiceDisplayFormat", timeInServiceDisplayFormat.name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useTimeInRank", useTimeInRank);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "timeInRankDisplayFormat", timeInRankDisplayFormat.name());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useRetirementDateTracking", useRetirementDateTracking);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "trackTotalEarnings", trackTotalEarnings);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "personnelMarketName", personnelMarketName);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "personnelMarketRandomEliteRemoval",
                                       personnelMarketRandomEliteRemoval);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "personnelMarketRandomVeteranRemoval",
                                       personnelMarketRandomVeteranRemoval);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "personnelMarketRandomRegularRemoval",
                                       personnelMarketRandomRegularRemoval);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "personnelMarketRandomGreenRemoval",
                                       personnelMarketRandomGreenRemoval);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "personnelMarketRandomUltraGreenRemoval",
                                       personnelMarketRandomUltraGreenRemoval);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "personnelMarketReportRefresh", personnelMarketReportRefresh);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "personnelMarketDylansWeight", personnelMarketDylansWeight);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "salaryEnlistedMultiplier", salaryEnlistedMultiplier);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "salaryCommissionMultiplier", salaryCommissionMultiplier);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "salaryAntiMekMultiplier", salaryAntiMekMultiplier);
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                + "<phenotypeProbabilities>"
                + StringUtils.join(phenotypeProbabilities, ',')
                + "</phenotypeProbabilities>");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "tougherHealing", tougherHealing);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAtB", useAtB);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAero", useAero);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useVehicles", useVehicles);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "clanVehicles", clanVehicles);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "doubleVehicles", doubleVehicles);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "adjustPlayerVehicles", adjustPlayerVehicles);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "opforLanceTypeMechs", opforLanceTypeMechs);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "opforLanceTypeMixed", opforLanceTypeMixed);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "opforLanceTypeVehicles", opforLanceTypeVehicles);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "opforUsesVTOLs", opforUsesVTOLs);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useDropShips", useDropShips);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "skillLevel", skillLevel);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "aeroRecruitsHaveUnits", aeroRecruitsHaveUnits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useShareSystem", useShareSystem);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "sharesExcludeLargeCraft", sharesExcludeLargeCraft);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "sharesForAll", sharesForAll);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "retirementRolls", retirementRolls);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "customRetirementMods", customRetirementMods);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "foundersNeverRetire", foundersNeverRetire);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "atbAddDependents", atbAddDependents);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "dependentsNeverLeave", dependentsNeverLeave);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "trackUnitFatigue", trackUnitFatigue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "mercSizeLimited", mercSizeLimited);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "trackOriginalUnit", trackOriginalUnit);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "regionalMechVariations", regionalMechVariations);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "attachedPlayerCamouflage", attachedPlayerCamouflage);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "playerControlsAttachedUnits", playerControlsAttachedUnits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "searchRadius", searchRadius);
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                + "<atbBattleChance>"
                + StringUtils.join(atbBattleChance, ',')
                + "</atbBattleChance>");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "generateChases", generateChases);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "variableContractLength", variableContractLength);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "instantUnitMarketDelivery", instantUnitMarketDelivery);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useWeatherConditions", useWeatherConditions);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useLightConditions", useLightConditions);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usePlanetaryConditions", usePlanetaryConditions);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useLeadership", useLeadership);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useStrategy", useStrategy);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "baseStrategyDeployment", baseStrategyDeployment);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "additionalStrategyDeployment", additionalStrategyDeployment);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "adjustPaymentForStrategy", adjustPaymentForStrategy);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "restrictPartsByMission", restrictPartsByMission);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "limitLanceWeight", limitLanceWeight);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "limitLanceNumUnits", limitLanceNumUnits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "contractMarketReportRefresh", contractMarketReportRefresh);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "unitMarketReportRefresh", unitMarketReportRefresh);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "assignPortraitOnRoleChange", assignPortraitOnRoleChange);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "allowOpforAeros", allowOpforAeros);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "allowOpforLocalUnits", allowOpforLocalUnits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "opforAeroChance", opforAeroChance);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "opforLocalUnitChance", opforLocalUnitChance);

        //Mass Repair/Salvage Options
        MekHqXmlUtil.writeSimpleXmlTag(pw1, ++indent, "massRepairUseRepair", massRepairUseRepair());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "massRepairUseSalvage", massRepairUseSalvage());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "massRepairUseExtraTime", massRepairUseExtraTime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "massRepairUseRushJob", massRepairUseRushJob);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "massRepairAllowCarryover", massRepairAllowCarryover);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "massRepairOptimizeToCompleteToday", massRepairOptimizeToCompleteToday);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "massRepairScrapImpossible", massRepairScrapImpossible);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "massRepairUseAssignedTechsFirst", massRepairUseAssignedTechsFirst);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "massRepairReplacePod", massRepairReplacePod);

        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "massRepairOptions");
        for (MassRepairOption massRepairOption : massRepairOptions) {
            massRepairOption.writeToXML(pw1, indent);
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "massRepairOptions");

        pw1.println(MekHqXmlUtil.indentStr(indent)
                + "<planetTechAcquisitionBonus>"
                + StringUtils.join(planetTechAcquisitionBonus, ',')
                + "</planetTechAcquisitionBonus>");
        pw1.println(MekHqXmlUtil.indentStr(indent)
                + "<planetIndustryAcquisitionBonus>"
                + StringUtils.join(planetIndustryAcquisitionBonus, ',')
                + "</planetIndustryAcquisitionBonus>");
        pw1.println(MekHqXmlUtil.indentStr(indent)
                + "<planetOutputAcquisitionBonus>"
                + StringUtils.join(planetOutputAcquisitionBonus, ',')
                + "</planetOutputAcquisitionBonus>");

        pw1.println(MekHqXmlUtil.indentStr(indent)
                    + "<salaryTypeBase>"
                    + Utilities.printMoneyArray(salaryTypeBase)
                    + "</salaryTypeBase>");
        pw1.println(MekHqXmlUtil.indentStr(indent)
                    + "<salaryXpMultiplier>"
                    + StringUtils.join(salaryXpMultiplier, ',')
                    + "</salaryXpMultiplier>");


        // cannot use StringUtils.join for a boolean array, so we are using this instead
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < usePortraitForType.length; i++) {
            csv.append(usePortraitForType[i]);
            if (i < usePortraitForType.length - 1) {
                csv.append(",");
            }
        }

        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "usePortraitForType", csv.toString());

        //region AtB Options
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "useAtBUnitMarket", useAtBUnitMarket);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "rats", StringUtils.join(rats, ','));
        if (staticRATs) {
            pw1.println(MekHqXmlUtil.indentStr(indent) + "<staticRATs/>");
        }

        if (ignoreRatEra) {
            pw1.println(MekHqXmlUtil.indentStr(indent) + "<ignoreRatEra/>");
        }
        //endregion AtB Options
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "campaignOptions");
    }

    public static CampaignOptions generateCampaignOptionsFromXml(Node wn, Version version) {
        MekHQ.getLogger().info("Loading Campaign Options from Version " + version.toString() + " XML...");

        wn.normalize();
        CampaignOptions retVal = new CampaignOptions();
        NodeList wList = wn.getChildNodes();

        // Okay, lets iterate through the children, eh?
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            MekHQ.getLogger().debug(String.format("%s\n\t%s", wn2.getNodeName(), wn2.getTextContent()));

            //region Repair and Maintenance Tab
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
            } else if (wn2.getNodeName().equalsIgnoreCase("useUnofficalMaintenance")) {
                retVal.useUnofficialMaintenance = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("logMaintenance")) {
                retVal.logMaintenance = Boolean.parseBoolean(wn2.getTextContent());
            //endregion Repair and Maintenance Tab

            } else if (wn2.getNodeName().equalsIgnoreCase("useFactionForNames")) {
                retVal.setUseOriginFactionForNames(Boolean.parseBoolean(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("repairSystem")) {
                retVal.repairSystem = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useEraMods")) {
                retVal.useEraMods = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("assignedTechFirst")) {
                retVal.assignedTechFirst = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("resetToFirstTech")) {
                retVal.resetToFirstTech = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useTactics")) {
                retVal.useTactics = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useInitBonus")) {
                retVal.useInitBonus = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useToughness")) {
                retVal.useToughness = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useArtillery")) {
                retVal.useArtillery = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useAbilities")) {
                retVal.useAbilities = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useEdge")) {
                retVal.useEdge = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useSupportEdge")) {
                retVal.useSupportEdge = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useImplants")) {
                retVal.useImplants = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("altQualityAveraging")) {
                retVal.altQualityAveraging = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useAdvancedMedical")) {
                retVal.useAdvancedMedical = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useDylansRandomXp")) {
                retVal.useDylansRandomXp = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useQuirks")) {
                retVal.useQuirks = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("showOriginFaction")) {
                retVal.showOriginFaction = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("randomizeOrigin")) {
                retVal.randomizeOrigin = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("randomizeDependentOrigin")) {
                retVal.randomizeDependentOrigin = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("originSearchRadius")) {
                retVal.originSearchRadius = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("isOriginExtraRandom")) {
                retVal.isOriginExtraRandom = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("originDistanceScale")) {
                retVal.originDistanceScale = Double.parseDouble(wn2.getTextContent());
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
            } else if (wn2.getNodeName().equalsIgnoreCase("idleXP")) {
                retVal.idleXP = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("targetIdleXP")) {
                retVal.targetIdleXP = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("monthsIdleXP")) {
                retVal.monthsIdleXP = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("contractNegotiationXP")) {
                retVal.contractNegotiationXP = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("adminWeeklyXP")) {
                retVal.adminXP = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("adminXPPeriod")) {
                retVal.adminXPPeriod = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("edgeCost")) {
                retVal.edgeCost = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("waitingPeriod")) {
                retVal.waitingPeriod = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("healWaitingPeriod")) {
                retVal.healWaitingPeriod = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("naturalHealingWaitingPeriod")) {
                retVal.naturalHealingWaitingPeriod = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("acquisitionSkill")) {
                retVal.acquisitionSkill = wn2.getTextContent().trim();
            } else if (wn2.getNodeName().equalsIgnoreCase("nDiceTransitTime")) {
                retVal.nDiceTransitTime = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("constantTransitTime")) {
                retVal.constantTransitTime = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("unitTransitTime")) {
                retVal.unitTransitTime = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("acquireMosBonus")) {
                retVal.acquireMosBonus = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("acquireMosUnit")) {
                retVal.acquireMosUnit = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("acquireMinimumTime")) {
                retVal.acquireMinimumTime = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("acquireMinimumTimeUnit")) {
                retVal.acquireMinimumTimeUnit = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("clanAcquisitionPenalty")) {
                retVal.clanAcquisitionPenalty = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("isAcquisitionPenalty")) {
                retVal.isAcquisitionPenalty = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("usePlanetaryAcquisition")) {
                retVal.usePlanetaryAcquisition = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("planetAcquisitionFactionLimit")) {
                retVal.planetAcquisitionFactionLimit = Integer.parseInt(wn2.getTextContent().trim());
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
            } else if (wn2.getNodeName().equalsIgnoreCase("dropshipContractPercent")) {
                retVal.setDropshipContractPercent(Double.parseDouble(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("jumpshipContractPercent")) {
                retVal.setJumpshipContractPercent(Double.parseDouble(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("warshipContractPercent")) {
                retVal.setWarshipContractPercent(Double.parseDouble(wn2.getTextContent().trim()));
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
            } else if (wn2.getNodeName().equalsIgnoreCase("useUnitRating") // Legacy
                    || wn2.getNodeName().equalsIgnoreCase("useDragoonRating")) { // Legacy
                if (!Boolean.parseBoolean(wn2.getTextContent())) {
                    retVal.setUnitRatingMethod(UnitRatingMethod.NONE);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("unitRatingMethod")
                    || wn2.getNodeName().equalsIgnoreCase("dragoonsRatingMethod")) {
                retVal.setUnitRatingMethod(UnitRatingMethod.parseFromString(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("manualUnitRatingModifier")) {
                retVal.setManualUnitRatingModifier(Integer.parseInt(wn2.getTextContent()));
            } else if (wn2.getNodeName().equalsIgnoreCase("usePortraitForType")) {
                String[] values = wn2.getTextContent().split(",");
                for (int i = 0; i < values.length; i++) {
                    if (i < retVal.usePortraitForType.length) {
                        retVal.usePortraitForType[i] = Boolean.parseBoolean(values[i].trim());
                    }
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
            } else if (wn2.getNodeName().equalsIgnoreCase("minimumHitsForVees")) {
                retVal.minimumHitsForVees = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("maxAcquisitions")) {
                retVal.maxAcquisitions = Integer.parseInt(wn2.getTextContent().trim());

            //region Personnel Tab
            //region Family
            } else if (wn2.getNodeName().equalsIgnoreCase("minimumMarriageAge")) {
                retVal.minimumMarriageAge = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("checkMutualAncestorsDepth")) {
                retVal.checkMutualAncestorsDepth = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("logMarriageNameChange")) {
                retVal.logMarriageNameChange = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useManualMarriages")) {
                retVal.setUseManualMarriages(Boolean.parseBoolean(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("useRandomMarriages")) {
                retVal.useRandomMarriages = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("chanceRandomMarriages")) {
                retVal.chanceRandomMarriages = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("marriageAgeRange")) {
                retVal.marriageAgeRange = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("randomMarriageSurnameWeights")) {
                String[] values = wn2.getTextContent().split(",");
                if (values.length == 13) {
                    for (int i = 0; i < values.length; i++) {
                        retVal.randomMarriageSurnameWeights[i] = Integer.parseInt(values[i]);
                    }
                } else if (values.length == 9) {
                    migrateMarriageSurnameWeights(retVal, values);
                } else {
                    MekHQ.getLogger().error("Unknown length of randomMarriageSurnameWeights");
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("useRandomSameSexMarriages")) {
                retVal.useRandomSameSexMarriages = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("chanceRandomSameSexMarriages")) {
                retVal.chanceRandomSameSexMarriages = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useUnofficialProcreation")) {
                retVal.useUnofficialProcreation = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("chanceProcreation")) {
                retVal.chanceProcreation = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useUnofficialProcreationNoRelationship")) {
                retVal.useUnofficialProcreationNoRelationship = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("chanceProcreationNoRelationship")) {
                retVal.chanceProcreationNoRelationship = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("displayTrueDueDate")) {
                retVal.displayTrueDueDate = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("logConception")) {
                retVal.logConception = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("babySurnameStyle")) {
                retVal.setBabySurnameStyle(BabySurnameStyle.parseFromString(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("determineFatherAtBirth")) {
                retVal.setDetermineFatherAtBirth(Boolean.parseBoolean(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("displayFamilyLevel")) {
                retVal.setDisplayFamilyLevel(FamilialRelationshipDisplayLevel.parseFromString(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("keepMarriedNameUponSpouseDeath")) {
                retVal.keepMarriedNameUponSpouseDeath = Boolean.parseBoolean(wn2.getTextContent().trim());
            //endregion Family

            //region Prisoners
            } else if (wn2.getNodeName().equalsIgnoreCase("prisonerCaptureStyle")) {
                retVal.setPrisonerCaptureStyle(PrisonerCaptureStyle.valueOf(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("defaultPrisonerStatus")) {
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
            //endregion Prisoners
            //endregion Personnel Tab

            //region Finances Tab
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
                retVal.financialYearDuration = FinancialYearDuration.valueOf(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("newFinancialYearFinancesToCSVExport")) {
                retVal.newFinancialYearFinancesToCSVExport = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("clanPriceModifier")) {
                retVal.clanPriceModifier = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueA")) {
                retVal.usedPartsValue[0] = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueB")) {
                retVal.usedPartsValue[1] = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueC")) {
                retVal.usedPartsValue[2] = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueD")) {
                retVal.usedPartsValue[3] = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueE")) {
                retVal.usedPartsValue[4] = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValueF")) {
                retVal.usedPartsValue[5] = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("damagedPartsValue")) {
                retVal.damagedPartsValue = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("canceledOrderReimbursement")) {
                retVal.canceledOrderReimbursement = Double.parseDouble(wn2.getTextContent().trim());
            //endregion Finances Tab

            } else if (wn2.getNodeName().equalsIgnoreCase("useTransfers")) {
                retVal.useTransfers = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useTimeInService")) {
                retVal.useTimeInService = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("timeInServiceDisplayFormat")) {
                retVal.timeInServiceDisplayFormat = TimeInDisplayFormat.valueOf(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useTimeInRank")) {
                retVal.useTimeInRank = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("timeInRankDisplayFormat")) {
                retVal.timeInRankDisplayFormat = TimeInDisplayFormat.valueOf(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useRetirementDateTracking")) {
                retVal.useRetirementDateTracking = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("trackTotalEarnings")) {
                retVal.trackTotalEarnings = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("capturePrisoners")) { // Legacy
                retVal.setPrisonerCaptureStyle(Boolean.parseBoolean(wn2.getTextContent().trim())
                        ? PrisonerCaptureStyle.TAHARQA : PrisonerCaptureStyle.NONE);
            } else if (wn2.getNodeName().equalsIgnoreCase("useRandomHitsForVees")) {
                retVal.useRandomHitsForVees = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketType")) { // Legacy
                retVal.personnelMarketName = PersonnelMarket.getTypeName(Integer.parseInt(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketName")) {
                retVal.personnelMarketName = wn2.getTextContent().trim();
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomEliteRemoval")) {
                retVal.personnelMarketRandomEliteRemoval = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomVeteranRemoval")) {
                retVal.personnelMarketRandomVeteranRemoval = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomRegularRemoval")) {
                retVal.personnelMarketRandomRegularRemoval = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomGreenRemoval")) {
                retVal.personnelMarketRandomGreenRemoval = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketRandomUltraGreenRemoval")) {
                retVal.personnelMarketRandomUltraGreenRemoval = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketReportRefresh")) {
                retVal.personnelMarketReportRefresh = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketDylansWeight")) {
                retVal.personnelMarketDylansWeight = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("salaryCommissionMultiplier")) {
                retVal.salaryCommissionMultiplier = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("salaryEnlistedMultiplier")) {
                retVal.salaryEnlistedMultiplier = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("salaryAntiMekMultiplier")) {
                retVal.salaryAntiMekMultiplier = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("salaryTypeBase")) {
                // CAW: we need at least the correct number of salaries as are expected in this version,
                //      otherwise we'll not be able to set a salary for a role.
                retVal.salaryTypeBase = Utilities.readMoneyArray(wn2, Person.T_NUM);
            } else if (wn2.getNodeName().equalsIgnoreCase("salaryXpMultiplier")) {
                String[] values = wn2.getTextContent().split(",");
                for (int i = 0; i < values.length; i++) {
                    retVal.salaryXpMultiplier[i] = Double.parseDouble(values[i]);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("phenotypeProbabilities")) {
                String[] values = wn2.getTextContent().split(",");
                for (int i = 0; i < values.length; i++) {
                    retVal.phenotypeProbabilities[i] = Integer.parseInt(values[i]);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoMW")) {
                retVal.phenotypeProbabilities[Phenotype.MECHWARRIOR.getIndex()] = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoBA")) {
                retVal.phenotypeProbabilities[Phenotype.ELEMENTAL.getIndex()] = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoAero")) {
                retVal.phenotypeProbabilities[Phenotype.AEROSPACE.getIndex()] = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoVee")) {
                retVal.phenotypeProbabilities[Phenotype.VEHICLE.getIndex()] = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("tougherHealing")) {
                retVal.tougherHealing = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useAtB")) {
                retVal.useAtB = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useAtBUnitMarket")) {
                retVal.setUseAtBUnitMarket(Boolean.parseBoolean(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("useAero")) {
                retVal.useAero = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useVehicles")) {
                retVal.useVehicles = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("clanVehicles")) {
                retVal.clanVehicles = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("doubleVehicles")) {
                retVal.doubleVehicles = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("adjustPlayerVehicles")) {
                retVal.adjustPlayerVehicles = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("opforLanceTypeMechs")) {
                retVal.opforLanceTypeMechs = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("opforLanceTypeMixed")) {
                retVal.opforLanceTypeMixed = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("opforLanceTypeVehicles")) {
                retVal.opforLanceTypeVehicles = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("opforUsesVTOLs")) {
                retVal.opforUsesVTOLs = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useDropShips")) {
                retVal.useDropShips = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("skillLevel")) {
                retVal.skillLevel = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("aeroRecruitsHaveUnits")) {
                retVal.aeroRecruitsHaveUnits = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useShareSystem")) {
                retVal.useShareSystem = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("sharesExcludeLargeCraft")) {
                retVal.sharesExcludeLargeCraft = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("sharesForAll")) {
                retVal.sharesForAll = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("retirementRolls")) {
                retVal.retirementRolls = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("customRetirementMods")) {
                retVal.customRetirementMods = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("foundersNeverRetire")) {
                retVal.foundersNeverRetire = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("atbAddDependents")) {
                retVal.atbAddDependents = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("dependentsNeverLeave")) {
                retVal.dependentsNeverLeave = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("trackUnitFatigue")) {
                retVal.trackUnitFatigue = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("trackOriginalUnit")) {
                retVal.trackOriginalUnit = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("mercSizeLimited")) {
                retVal.mercSizeLimited = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("regionalMechVariations")) {
                retVal.regionalMechVariations = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("attachedPlayerCamouflage")) {
                retVal.attachedPlayerCamouflage = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("playerControlsAttachedUnits")) {
                retVal.setPlayerControlsAttachedUnits(Boolean.parseBoolean(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("searchRadius")) {
                retVal.searchRadius = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("intensity")) { // Legacy
                double intensity = Double.parseDouble(wn2.getTextContent().trim());

                retVal.atbBattleChance[AtBLanceRole.FIGHTING.ordinal()] = (int) Math.round(((40.0 * intensity) / (40.0 * intensity + 60.0)) * 100.0 + 0.5);
                retVal.atbBattleChance[AtBLanceRole.DEFENCE.ordinal()] = (int) Math.round(((20.0 * intensity) / (20.0 * intensity + 80.0)) * 100.0 + 0.5);
                retVal.atbBattleChance[AtBLanceRole.SCOUTING.ordinal()] = (int) Math.round(((60.0 * intensity) / (60.0 * intensity + 40.0)) * 100.0 + 0.5);
                retVal.atbBattleChance[AtBLanceRole.TRAINING.ordinal()] = (int) Math.round(((10.0 * intensity) / (10.0 * intensity + 90.0)) * 100.0 + 0.5);
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
            } else if (wn2.getNodeName().equalsIgnoreCase("variableContractLength")) {
                retVal.variableContractLength = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("instantUnitMarketDelivery")) {
                retVal.instantUnitMarketDelivery = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useWeatherConditions")) {
                retVal.useWeatherConditions = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useLightConditions")) {
                retVal.useLightConditions = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("usePlanetaryConditions")) {
                retVal.usePlanetaryConditions = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useLeadership")) {
                retVal.useLeadership = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useStrategy")) {
                retVal.useStrategy = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("baseStrategyDeployment")) {
                retVal.baseStrategyDeployment = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("additionalStrategyDeployment")) {
                retVal.additionalStrategyDeployment = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("adjustPaymentForStrategy")) {
                retVal.adjustPaymentForStrategy = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("restrictPartsByMission")) {
                retVal.restrictPartsByMission = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("limitLanceWeight")) {
                retVal.limitLanceWeight = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("limitLanceNumUnits")) {
                retVal.limitLanceNumUnits = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useAtBCapture")) { // Legacy
                if (Boolean.parseBoolean(wn2.getTextContent().trim())) {
                    retVal.setPrisonerCaptureStyle(PrisonerCaptureStyle.ATB);
                    retVal.setUseAtBPrisonerDefection(true);
                    retVal.setUseAtBPrisonerRansom(true);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("contractMarketReportRefresh")) {
                retVal.contractMarketReportRefresh = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("unitMarketReportRefresh")) {
                retVal.unitMarketReportRefresh = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("startGameDelay")) { // Legacy
                MekHQ.getMekHQOptions().setStartGameDelay(Integer.parseInt(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("allowOpforLocalUnits")) {
                retVal.allowOpforLocalUnits = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("allowOpforAeros")) {
                retVal.allowOpforAeros = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("opforAeroChance")) {
                retVal.opforAeroChance = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("opforLocalUnitChance")) {
                retVal.opforLocalUnitChance = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("historicalDailyLog")) { // Legacy
                MekHQ.getMekHQOptions().setHistoricalDailyLog(Boolean.parseBoolean(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("rats")) {
                retVal.rats = MekHqXmlUtil.unEscape(wn2.getTextContent().trim()).split(",");
            } else if (wn2.getNodeName().equalsIgnoreCase("staticRATs")) {
                retVal.staticRATs = true;
            } else if (wn2.getNodeName().equalsIgnoreCase("ignoreRatEra")) {
                retVal.ignoreRatEra = true;
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairUseRepair")) {
                retVal.setMassRepairUseRepair(Boolean.parseBoolean(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairUseSalvage")) {
                retVal.setMassRepairUseSalvage(Boolean.parseBoolean(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairUseExtraTime")) {
                retVal.massRepairUseExtraTime = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairUseRushJob")) {
                retVal.massRepairUseRushJob = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairAllowCarryover")) {
                retVal.massRepairAllowCarryover = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairOptimizeToCompleteToday")) {
                retVal.massRepairOptimizeToCompleteToday = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairScrapImpossible")) {
                retVal.massRepairScrapImpossible = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairUseAssignedTechsFirst")) {
                retVal.massRepairUseAssignedTechsFirst = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairReplacePod")) {
                retVal.massRepairReplacePod = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("massRepairOptions")) {
                retVal.setMassRepairOptions(MassRepairOption.parseListFromXML(wn2, version));
            }
        }

        MekHQ.getLogger().debug("Load Campaign Options Complete!");

        return retVal;
    }

    //region Migration
    /**
     * This is annoyingly required for the case of anyone having changed the surname weights.
     * The code is not nice, but will nicely handle the cases where anyone has made changes
     * @param retVal the return CampaignOptions
     * @param values the values to migrate
     */
    private static void migrateMarriageSurnameWeights(CampaignOptions retVal, String[] values) {
        int[] weights = new int[values.length];

        for (int i = 0; i < weights.length; i++) {
            weights[i] = Integer.parseInt(values[i]);
        }

        // Now we need to test to figure out the weights have changed. If not, we will keep the
        // new default values. If they have, we save their changes and add the new surname weights
        if (
                (weights[0] != retVal.randomMarriageSurnameWeights[0])
                || (weights[1] != retVal.randomMarriageSurnameWeights[1] + 5)
                || (weights[2] != retVal.randomMarriageSurnameWeights[2] + 5)
                || (weights[3] != retVal.randomMarriageSurnameWeights[9] + 5)
                || (weights[4] != retVal.randomMarriageSurnameWeights[10] + 5)
                || (weights[5] != retVal.randomMarriageSurnameWeights[5] + 5)
                || (weights[6] != retVal.randomMarriageSurnameWeights[6] + 5)
                || (weights[7] != retVal.randomMarriageSurnameWeights[11])
                || (weights[8] != retVal.randomMarriageSurnameWeights[12])
        ) {
            retVal.randomMarriageSurnameWeights[0] = weights[0];
            retVal.randomMarriageSurnameWeights[1] = weights[1];
            retVal.randomMarriageSurnameWeights[2] = weights[2];
            // 3 is newly added
            // 4 is newly added
            retVal.randomMarriageSurnameWeights[5] = weights[3];
            retVal.randomMarriageSurnameWeights[6] = weights[4];
            // 7 is newly added
            // 8 is newly added
            retVal.randomMarriageSurnameWeights[9] = weights[5];
            retVal.randomMarriageSurnameWeights[10] = weights[6];
            retVal.randomMarriageSurnameWeights[11] = weights[7];
            retVal.randomMarriageSurnameWeights[12] = weights[8];
        }
    }
    //endregion Migration
}
