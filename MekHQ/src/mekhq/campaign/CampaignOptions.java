/*
 * PartInventiry.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import megamek.common.TechConstants;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.UnitRatingMethod;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author natit
 */
public class CampaignOptions implements Serializable {
    private static final long serialVersionUID = 5698008431749303602L;

    public final static int TECH_INTRO = 0;
    public final static int TECH_STANDARD = 1;
    public final static int TECH_ADVANCED = 2;
    public final static int TECH_EXPERIMENTAL = 3;
    public final static int TECH_UNOFFICIAL = 4;
    // This must always be the highest tech level in order to hide parts
    // that haven't been invented yet, or that are completely extinct
    public final static int TECH_UNKNOWN = 5;

    public final static int TRANSIT_UNIT_DAY = 0;
    public final static int TRANSIT_UNIT_WEEK = 1;
    public final static int TRANSIT_UNIT_MONTH = 2;
    public final static int TRANSIT_UNIT_NUM = 3;


    public final static String S_TECH = "Tech";
    public final static String S_AUTO = "Automatic Success";

    public final static int PRISONER_RANK = 0;
    public final static int BONDSMAN_RANK = 1;

    public final static int REPAIR_SYSTEM_STRATOPS = 0;
    public final static int REPAIR_SYSTEM_WARCHEST_CUSTOM = 1;
    public final static int REPAIR_SYSTEM_GENERIC_PARTS = 2;
    //FIXME: This needs to be localized
    public final static String[] REPAIR_SYSTEM_NAMES = {"Strat Ops", "Warchest Custom", "Generic Spare Parts"};

    private boolean useFactionForNames;
    private boolean useUnitRating;

    //personnel related
    private boolean useTactics;
    private boolean useInitBonus;
    private boolean useToughness;
    private boolean useArtillery;
    private boolean useAbilities;
    private boolean useEdge;
    private boolean useRemfEdge;
    private boolean useImplants;
	private boolean altQualityAveraging;
    private int healWaitingPeriod;
    private int naturalHealingWaitingPeriod;
    private boolean useAdvancedMedical; // Unofficial
    private boolean useDylansRandomXp; // Unofficial
    private boolean useRandomHitsForVees;
    private int minimumHitsForVees;
    private boolean tougherHealing;
    private int maxAcquisitions;
    private boolean useUnofficialProcreation;
    private boolean useUnofficialProcreationNoRelationship;
    private boolean useParentage;
    private boolean logConception;
    private boolean useTransfers;
    private boolean capturePrisoners;
    private int defaultPrisonerStatus;

    //personnel market related
    private boolean personnelMarketReportRefresh;
    private int personnelMarketType;
    private int personnelMarketRandomEliteRemoval;
    private int personnelMarketRandomVeteranRemoval;
    private int personnelMarketRandomRegularRemoval;
    private int personnelMarketRandomGreenRemoval;
    private int personnelMarketRandomUltraGreenRemoval;
    private double personnelMarketDylansWeight;

    //unit related
    private boolean useQuirks;

    //tech and unit limits
    private boolean limitByYear;
    private boolean disallowExtinctStuff;
    private boolean allowClanPurchases;
    private boolean allowISPurchases;
    private boolean allowCanonOnly;
    private boolean allowCanonRefitOnly;
    private int techLevel;
    private boolean useAmmoByType; // Unofficial

    //finance related
    private boolean payForParts;
    private boolean payForUnits;
    private boolean payForSalaries;
    private boolean payForRecruitment;
    private boolean payForOverhead;
    private boolean payForMaintain;
    private boolean payForTransport;
    private boolean sellUnits;
    private boolean sellParts;
    private boolean useLoanLimits;
    private double[] usedPartsValue;
    private double damagedPartsValue;
    private double canceledOrderReimbursement;
    private boolean usePercentageMaint; // Unofficial
    private boolean infantryDontCount; // Unofficial
    private double clanPriceModifier;

    //contract related
    private boolean equipmentContractBase;
    private double equipmentContractPercent;
    private boolean equipmentContractSaleValue;
    private boolean blcSaleValue;

    //acquisition related
    private int waitingPeriod;
    private String acquisitionSkill;
    private boolean acquisitionSupportStaffOnly;
    private int nDiceTransitTime;
    private int constantTransitTime;
    private int unitTransitTime;
    private int acquireMosBonus;
    private int acquireMosUnit;
    private int acquireMinimumTime;
    private int acquireMinimumTimeUnit;
    private int clanAcquisitionPenalty;
    private int isAcquisitionPenalty;

    //xp related
    private int scenarioXP;
    private int killsForXP;
    private int killXPAward;
    private int nTasksXP;
    private int tasksXP;
    private int mistakeXP;
    private int successXP;
    private int idleXP;
    private int targetIdleXP;
    private int monthsIdleXP;
    private int contractNegotiationXP;
    private int adminXP;
    private int adminXPPeriod;
    private int edgeCost;

    //repair related
    private boolean destroyByMargin;
    private int destroyMargin;
    private int repairSystem;
    private boolean useEraMods;
	private boolean assignedTechFirst;
	private boolean resetToFirstTech;
	private int destroyPartTarget;

    //maintenance related
    private boolean checkMaintenance;
    private int maintenanceCycleDays;
    private int maintenanceBonus;
    private boolean useQualityMaintenance;
	private boolean useUnofficalMaintenance;
	private boolean reverseQualityNames;

    //Dragoon's Rating
    private UnitRatingMethod unitRatingMethod;

    //salary related
    private int[] salaryTypeBase;
    private double[] salaryXpMultiplier;
    private double salaryCommissionMultiplier;
    private double salaryEnlistedMultiplier;
    private double salaryAntiMekMultiplier;

    //phenotype related
    private int probPhenoMW;
    private int probPhenoAero;
    private int probPhenoBA;
    private int probPhenoVee;

    //random portraits related
    private boolean[] usePortraitForType;

    //Against the Bot related
    private boolean useAtB;
    private boolean useAero;
    private boolean useVehicles;
    private boolean clanVehicles;
    private boolean useDropShips;
    private int skillLevel;
    private boolean doubleVehicles;
    private boolean adjustPlayerVehicles;
    private int opforLanceTypeMechs;
    private int opforLanceTypeMixed;
    private int opforLanceTypeVehicles;
    private boolean opforUsesVTOLs;
    private boolean regionalMechVariations;
    private boolean aeroRecruitsHaveUnits;
    private boolean useShareSystem;
    private boolean sharesExcludeLargeCraft;
    private boolean sharesForAll;
    private boolean retirementRolls;
    private boolean trackUnitFatigue;
    private boolean customRetirementMods;
    private boolean trackOriginalUnit;
    private boolean mercSizeLimited;
    private String[] rats = {"Xotl", "Total Warfare"};
    private boolean staticRATs;
    private boolean ignoreRatEra;
    private int searchRadius;
    private double intensity;
    private boolean variableContractLength;
    private boolean instantUnitMarketDelivery;
    private boolean useWeatherConditions;
    private boolean useLightConditions;
    private boolean usePlanetaryConditions;
    private boolean useLeadership;
    private boolean useStrategy;
    private int baseStrategyDeployment;
    private int additionalStrategyDeployment;
    private boolean adjustPaymentForStrategy;
    private boolean restrictPartsByMission;
    private boolean limitLanceWeight;
    private boolean limitLanceNumUnits;
    private boolean useAtBCapture;
    private boolean contractMarketReportRefresh;
    private boolean unitMarketReportRefresh;
    private int startGameDelay;

    //Mass Repair/Salvage Options
    private boolean massRepairUseExtraTime;
    private boolean massRepairUseRushJob;
    private boolean massRepairAllowCarryover;
    private boolean massRepairOptimizeToCompleteToday;
    private boolean massRepairScrapImpossible;
    private boolean massRepairUseAssignedTechsFirst;
    private boolean massRepairReplacePod;
    private List<MassRepairOption> massRepairOptions;

    public CampaignOptions() {
        clanPriceModifier = 1.0;
        useFactionForNames = true;
        repairSystem = REPAIR_SYSTEM_STRATOPS;
        useEraMods = false;
        assignedTechFirst = false;
		resetToFirstTech = false;
        useUnitRating = true;
        useTactics = false;
        useInitBonus = false;
        useToughness = false;
        useArtillery = false;
        useAbilities = false;
        useEdge = false;
        useRemfEdge = false;
        useImplants = false;
		altQualityAveraging = false;
        useAdvancedMedical = false;
        useDylansRandomXp = false;
        useQuirks = false;
        payForParts = false;
        payForUnits = false;
        payForSalaries = false;
        payForRecruitment = false;
        payForOverhead = false;
        payForMaintain = false;
        payForTransport = false;
        useLoanLimits = false;
        sellUnits = false;
        sellParts = false;
        limitByYear = true;
        disallowExtinctStuff = false;
        allowClanPurchases = true;
        allowISPurchases = true;
        allowCanonOnly = false;
        allowCanonRefitOnly = false;
        useAmmoByType = false;
        usePercentageMaint = false;
        infantryDontCount = false;
        techLevel = TECH_EXPERIMENTAL;
        scenarioXP = 1;
        killsForXP = 0;
        killXPAward = 0;
        nTasksXP = 25;
        tasksXP = 1;
        mistakeXP = 0;
        successXP = 0;
        usedPartsValue = new double[6];
        usedPartsValue[0] = 0.1;
        usedPartsValue[1] = 0.2;
        usedPartsValue[2] = 0.3;
        usedPartsValue[3] = 0.5;
        usedPartsValue[4] = 0.7;
        usedPartsValue[5] = 0.9;
        damagedPartsValue = 0.33;
        canceledOrderReimbursement = 0.5;
        usePortraitForType = new boolean[Person.T_NUM];
        for (int i = 0; i < Person.T_NUM; i++) {
            usePortraitForType[i] = false;
        }
        usePortraitForType[Person.T_MECHWARRIOR] = true;
        idleXP = 0;
        targetIdleXP = 10;
        monthsIdleXP = 2;
        contractNegotiationXP = 0;
        adminXP = 0;
        adminXPPeriod = 1;
        edgeCost = 1;
        unitRatingMethod = UnitRatingMethod.CAMPAIGN_OPS;
        waitingPeriod = 7;
        acquisitionSkill = S_TECH;
        acquisitionSupportStaffOnly = true;
        nDiceTransitTime = 1;
        constantTransitTime = 0;
        unitTransitTime = TRANSIT_UNIT_MONTH;
        acquireMosBonus = 1;
        acquireMosUnit = TRANSIT_UNIT_MONTH;
        acquireMinimumTime = 1;
        acquireMinimumTimeUnit = TRANSIT_UNIT_MONTH;
        equipmentContractBase = false;
        equipmentContractPercent = 5.0;
        equipmentContractSaleValue = false;
        blcSaleValue = false;
        clanAcquisitionPenalty = 0;
        isAcquisitionPenalty = 0;
        healWaitingPeriod = 1;
        naturalHealingWaitingPeriod = 15;
        destroyByMargin = false;
        destroyMargin = 4;
        destroyPartTarget = 10;
        maintenanceCycleDays = 7;
        maintenanceBonus = -1;
        useQualityMaintenance = true;
        useUnofficalMaintenance = false;
        reverseQualityNames = false;
        checkMaintenance = true;
        useRandomHitsForVees = false;
        minimumHitsForVees = 1;
        maxAcquisitions = 0;
        useUnofficialProcreation = false;
        useUnofficialProcreationNoRelationship = false;
        useParentage = false;
        logConception = false;
        useTransfers = true;
        capturePrisoners = true;
        defaultPrisonerStatus = PRISONER_RANK;
        personnelMarketReportRefresh = true;
        personnelMarketType = PersonnelMarket.TYPE_STRAT_OPS;
        personnelMarketRandomEliteRemoval = 10;
        personnelMarketRandomVeteranRemoval = 8;
        personnelMarketRandomRegularRemoval = 6;
        personnelMarketRandomGreenRemoval = 4;
        personnelMarketRandomUltraGreenRemoval = 4;
        personnelMarketDylansWeight = 0.3;
        salaryTypeBase = new int[Person.T_NUM];
        salaryTypeBase[Person.T_MECHWARRIOR] = 1500;
        salaryTypeBase[Person.T_AERO_PILOT] = 1500;
        salaryTypeBase[Person.T_VEE_GUNNER] = 900;
        salaryTypeBase[Person.T_GVEE_DRIVER] = 900;
        salaryTypeBase[Person.T_NVEE_DRIVER] = 900;
        salaryTypeBase[Person.T_VTOL_PILOT] = 900;
        salaryTypeBase[Person.T_CONV_PILOT] = 900;
        salaryTypeBase[Person.T_INFANTRY] = 750;
        salaryTypeBase[Person.T_BA] = 960;
        salaryTypeBase[Person.T_SPACE_PILOT] = 1000;
        salaryTypeBase[Person.T_SPACE_GUNNER] = 1000;
        salaryTypeBase[Person.T_SPACE_CREW] = 1000;
        salaryTypeBase[Person.T_NAVIGATOR] = 1000;
        salaryTypeBase[Person.T_DOCTOR] = 1500;
        salaryTypeBase[Person.T_ADMIN_COM] = 500;
        salaryTypeBase[Person.T_ADMIN_HR] = 500;
        salaryTypeBase[Person.T_ADMIN_LOG] = 500;
        salaryTypeBase[Person.T_ADMIN_TRA] = 500;
        salaryTypeBase[Person.T_MECH_TECH] = 800;
        salaryTypeBase[Person.T_AERO_TECH] = 800;
        salaryTypeBase[Person.T_BA_TECH] = 800;
        salaryTypeBase[Person.T_MECHANIC] = 800;
        salaryTypeBase[Person.T_ASTECH] = 400;
        salaryTypeBase[Person.T_MEDIC] = 400;
        salaryTypeBase[Person.T_PROTO_PILOT] = 960;
        salaryXpMultiplier = new double[5];
        salaryXpMultiplier[SkillType.EXP_ULTRA_GREEN] = 0.6;
        salaryXpMultiplier[SkillType.EXP_GREEN] = 0.6;
        salaryXpMultiplier[SkillType.EXP_REGULAR] = 1.0;
        salaryXpMultiplier[SkillType.EXP_VETERAN] = 1.6;
        salaryXpMultiplier[SkillType.EXP_ELITE] = 3.2;
        salaryAntiMekMultiplier = 1.5;
        salaryCommissionMultiplier = 1.2;
        salaryEnlistedMultiplier = 1.0;
        probPhenoMW = 95;
        probPhenoAero = 95;
        probPhenoBA = 100;
        probPhenoVee = 0;
        tougherHealing = false;

        useAtB = false;
    	useAero = false;
    	useVehicles = true;
    	clanVehicles = false;
    	doubleVehicles = true;
        adjustPlayerVehicles = false;
        opforLanceTypeMechs = 1;
        opforLanceTypeMixed = 2;
        opforLanceTypeVehicles = 3;
        opforUsesVTOLs = true;
    	useDropShips = false;
    	skillLevel = 2;
        aeroRecruitsHaveUnits = false;
        useShareSystem = false;
        sharesExcludeLargeCraft = false;
        sharesForAll = false;
        retirementRolls = true;
        customRetirementMods = false;
        trackUnitFatigue = false;
        trackOriginalUnit = false;
        mercSizeLimited = false;
    	regionalMechVariations = false;
    	searchRadius = 800;
    	intensity = 1.0;
    	variableContractLength = false;
        instantUnitMarketDelivery = false;
        useWeatherConditions = true;
    	useLightConditions = true;
    	usePlanetaryConditions = true;
    	usePlanetaryConditions = false;
    	useLeadership = true;
    	useStrategy = true;
    	baseStrategyDeployment = 3;
    	additionalStrategyDeployment = 1;
    	adjustPaymentForStrategy = false;
    	restrictPartsByMission = true;
    	limitLanceWeight = true;
    	limitLanceNumUnits = true;
    	useAtBCapture = false;
        contractMarketReportRefresh = true;
        unitMarketReportRefresh = true;
        startGameDelay = 500;
        
        //Mass Repair/Salvage Options
        massRepairUseExtraTime = true;
        massRepairUseRushJob = true;
        massRepairAllowCarryover = true;
        massRepairOptimizeToCompleteToday = false;
        massRepairScrapImpossible = false;
        massRepairUseAssignedTechsFirst = false;
        massRepairReplacePod = true;
        massRepairOptions = new ArrayList<MassRepairOption>();
        
        for (int i = 0; i < MassRepairOption.VALID_REPAIR_TYPES.length; i++) {
        	massRepairOptions.add(new MassRepairOption(MassRepairOption.VALID_REPAIR_TYPES[i]));
        }
   }

    public UnitRatingMethod getUnitRatingMethod() {
        return unitRatingMethod;
    }

    public void setUnitRatingMethod(UnitRatingMethod method) {
        this.unitRatingMethod = method;
    }

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

    public boolean useDragoonRating() {
        return useUnitRating;
    }

    public void setDragoonRating(boolean b) {
        this.useUnitRating = b;
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

    public int getRepairSystem() {
        return repairSystem;
    }

    public void setRepairSystem(int i) {
        this.repairSystem = i;
    }

    public boolean useFactionForNames() {
        return useFactionForNames;
    }

    public void setFactionForNames(boolean b) {
        this.useFactionForNames = b;
    }

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
    
    public boolean useRemfEdge() {
        return useRemfEdge;
    }

    public void setRemfEdge(boolean b) {
        this.useRemfEdge = b;
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

    // Personnel Market
    public boolean getPersonnelMarketReportRefresh() {
        return personnelMarketReportRefresh;
    }

    public void setPersonnelMarketReportRefresh(boolean b) {
        personnelMarketReportRefresh = b;
    }

    public int getPersonnelMarketType() {
        return personnelMarketType;
    }

    public void setPersonnelMarketType(int t) {
        personnelMarketType = t;
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

    public boolean payForParts() {
        return payForParts;
    }

    public void setPayForParts(boolean b) {
        this.payForParts = b;
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

    public boolean payForRecruitment() {
        return payForRecruitment;
    }

    public void setPayForRecruitment(boolean b) {
        this.payForRecruitment = b;
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

    public boolean useLoanLimits() {
        return useLoanLimits;
    }

    public void setLoanLimits(boolean b) {
        this.useLoanLimits = b;
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

    public boolean useAmmoByType() {
        return useAmmoByType;
    }

    public void setUseAmmoByType(boolean b) {
        useAmmoByType = b;
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

    public int getTechLevel() {
        return techLevel;
    }

    public void setTechLevel(int lvl) {
        techLevel = lvl;
    }

    public int getProbPhenoMW() {
        return probPhenoMW;
    }

    public void setProbPhenoMW(int p) {
        probPhenoMW = p;
    }

    public int getProbPhenoAero() {
        return probPhenoAero;
    }

    public void setProbPhenoAero(int p) {
        probPhenoAero = p;
    }

    public int getProbPhenoBA() {
        return probPhenoBA;
    }

    public void setProbPhenoBA(int p) {
        probPhenoBA = p;
    }

    public int getProbPhenoVee() {
        return probPhenoVee;
    }

    public void setProbPhenoVee(int p) {
        probPhenoVee = p;
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

    public double getEquipmentContractPercent() {
        return equipmentContractPercent;
    }

    public void setEquipmentContractPercent(double b) {
        equipmentContractPercent = b;
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

    public boolean useBLCSaleValue() {
        return blcSaleValue;
    }

    public void setBLCSaleValue(boolean b) {
        this.blcSaleValue = b;
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

    public boolean useUnofficalMaintenance() {
        return useUnofficalMaintenance;
    }

    public void setUseUnofficalMaintenance(boolean b) {
    	useUnofficalMaintenance = b;
    }

    public boolean reverseQualityNames() {
        return reverseQualityNames;
    }

    public void setReverseQualityNames(boolean b) {
    	reverseQualityNames = b;
    }

    public boolean checkMaintenance() {
        return checkMaintenance;
    }

    public void setCheckMaintenance(boolean b) {
        checkMaintenance = b;
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

    public boolean useRandomHitsForVees() {
        return useRandomHitsForVees;
    }

    public void setUseRandomHitsForVees(boolean b) {
        useRandomHitsForVees = b;
    }

    public int getMaxAcquisitions() {
        return maxAcquisitions;
    }

    public void setMaxAcquisitions(int d) {
        maxAcquisitions = d;
    }

    public boolean useUnofficialProcreation() {
        return useUnofficialProcreation;
    }

    public void setUseUnofficialProcreation(boolean b) {
    	useUnofficialProcreation = b;
    }

    public boolean useUnofficialProcreationNoRelationship() {
        return useUnofficialProcreationNoRelationship;
    }

    public void setUseUnofficialProcreationNoRelationship(boolean b) {
    	useUnofficialProcreationNoRelationship = b;
    }

    public boolean useParentage() {
        return useParentage;
    }

    public void setUseParentage(boolean b) {
        useParentage = b;
    }

    public boolean logConception() {
        return logConception;
    }

    public void setLogConception(boolean b) {
        logConception = b;
    }

    public boolean useTransfers() {
    	return useTransfers;
    }

    public void setUseTransfers(boolean b) {
    	useTransfers = b;
    }

    public boolean capturePrisoners() {
    	return capturePrisoners;
    }

    public void setCapturePrisoners(boolean b) {
    	capturePrisoners = b;
    }

    public int getDefaultPrisonerStatus() {
        return defaultPrisonerStatus;
    }

    public void setDefaultPrisonerStatus(int d) {
    	defaultPrisonerStatus = d;
    }

    public int getMinimumHitsForVees() {
        return minimumHitsForVees;
    }

    public void setMinimumHitsForVees(int d) {
        minimumHitsForVees = d;
    }

    public int getBaseSalary(int type) {
        if (type < 0 || type >= salaryTypeBase.length) {
            return 0;
        }
        return salaryTypeBase[type];
    }

    public void setBaseSalary(int base, int type) {
        if (type < 0 || type >= salaryTypeBase.length) {
            return;
        }
        this.salaryTypeBase[type] = base;
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

    public double getSalaryEnlistedMultiplier() {
        return salaryEnlistedMultiplier;
    }

    public void setSalaryEnlistedMultiplier(double d) {
        salaryEnlistedMultiplier = d;
    }

    public double getSalaryCommissionMultiplier() {
        return salaryCommissionMultiplier;
    }

    public void setSalaryCommissionMultiplier(double d) {
        salaryCommissionMultiplier = d;
    }

    public double getSalaryAntiMekMultiplier() {
        return salaryAntiMekMultiplier;
    }

    public void setSalaryAntiMekMultiplier(double d) {
        salaryAntiMekMultiplier = d;
    }

    public boolean useTougherHealing() {
        return tougherHealing;
    }

    public void setTougherHealing(boolean b) {
        tougherHealing = b;
    }

	public boolean getUseAtB() {
		return useAtB;
	}

	public void setUseAtB(boolean useAtB) {
		this.useAtB = useAtB;
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

	public String[] getRATs() {
		return rats;
	}

	public void setRATs (String[] rats) {
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

	public double getIntensity() {
		return intensity;
	}

	public void setIntensity(double intensity) {
		this.intensity = intensity;
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

	public boolean getUseAtBCapture() {
		return useAtBCapture;
	}

	public void setUseAtBCapture(boolean set) {
		useAtBCapture = set;
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

	public int getStartGameDelay() {
		return startGameDelay;
	}

	public void setStartGameDelay(int delay) {
		startGameDelay = delay;
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
		return massRepairOptions;
	}

	public void setMassRepairOptions(List<MassRepairOption> massRepairOptions) {
		this.massRepairOptions = massRepairOptions;
	}

	public void addMassRepairOption(MassRepairOption mro) {
		if (mro.getType() == -1) {
			return;
		}
		
		int foundIdx = -1;
		
		for (int i = 0; i < massRepairOptions.size(); i++) {
			if (massRepairOptions.get(i).getType() == mro.getType()) {
				foundIdx = i;
				break;
			}
		}
		
		if (foundIdx == -1) {
			massRepairOptions.add(mro);
		} else {
			massRepairOptions.add(foundIdx, mro);
			massRepairOptions.remove(foundIdx + 1);
		}
		
		Collections.sort(massRepairOptions, new Comparator<MassRepairOption>() {
			@Override
			public int compare(MassRepairOption o1, MassRepairOption o2) {
				return o1.getType() < o2.getType() ? -1 : 1;
			}
		});
	}

	public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<campaignOptions>");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "clanPriceModifier", clanPriceModifier); //private double
        // clanPriceModifier;
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useFactionForNames", useFactionForNames); //private boolean
        // useFinances;
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "repairSystem", repairSystem); //private int repairSystem;
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useUnitRating", useUnitRating);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "unitRatingMethod", unitRatingMethod.getDescription());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useEraMods", useEraMods);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "assignedTechFirst", assignedTechFirst);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "resetToFirstTech", resetToFirstTech);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useTactics", useTactics);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useInitBonus", useInitBonus);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useToughness", useToughness);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useArtillery", useArtillery);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAbilities", useAbilities);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useEdge", useEdge);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useRemfEdge", useRemfEdge);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useImplants", useImplants);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "altQualityAveraging", altQualityAveraging);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAdvancedMedical", useAdvancedMedical);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useDylansRandomXp", useDylansRandomXp);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useQuirks", useQuirks);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForParts", payForParts);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForUnits", payForUnits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForSalaries", payForSalaries);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForOverhead", payForOverhead);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForMaintain", payForMaintain);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForTransport", payForTransport);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueA", usedPartsValue[0]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueB", usedPartsValue[1]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueC", usedPartsValue[2]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueD", usedPartsValue[3]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueE", usedPartsValue[4]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usedPartsValueF", usedPartsValue[5]);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "damagedPartsValue", damagedPartsValue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "canceledOrderReimbursement", canceledOrderReimbursement);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "sellUnits", sellUnits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "sellParts", sellParts);
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
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAmmoByType", useAmmoByType);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "usePercentageMaint", usePercentageMaint);
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
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipmentContractPercent", equipmentContractPercent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipmentContractBase", equipmentContractBase);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipmentContractSaleValue", equipmentContractSaleValue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "blcSaleValue", blcSaleValue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "clanAcquisitionPenalty", clanAcquisitionPenalty);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "isAcquisitionPenalty", isAcquisitionPenalty);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useLoanLimits", useLoanLimits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "payForRecruitment", payForRecruitment);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "healWaitingPeriod", healWaitingPeriod);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "naturalHealingWaitingPeriod", naturalHealingWaitingPeriod);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "destroyByMargin", destroyByMargin);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "destroyMargin", destroyMargin);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "destroyPartTarget", destroyPartTarget);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "maintenanceCycleDays", maintenanceCycleDays);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "maintenanceBonus", maintenanceBonus);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useQualityMaintenance", useQualityMaintenance);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "reverseQualityNames", reverseQualityNames);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useUnofficalMaintenance", useUnofficalMaintenance);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "checkMaintenance", checkMaintenance);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useRandomHitsForVees", useRandomHitsForVees);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "minimumHitsForVees", minimumHitsForVees);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "maxAcquisitions", maxAcquisitions);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useUnofficialProcreation", useUnofficialProcreation);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useUnofficialProcreationNoRelationship", useUnofficialProcreationNoRelationship);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useParentage", useParentage);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "logConception", logConception);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useTransfers", useTransfers);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "capturePrisoners", capturePrisoners);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "defaultPrisonerStatus", defaultPrisonerStatus);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "personnelMarketType", personnelMarketType);
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
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "probPhenoMW", probPhenoMW);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "probPhenoAero", probPhenoAero);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "probPhenoBA", probPhenoBA);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "probPhenoVee", probPhenoVee);
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
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "trackUnitFatigue", trackUnitFatigue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "mercSizeLimited", mercSizeLimited);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "trackOriginalUnit", trackOriginalUnit);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "regionalMechVariations", regionalMechVariations);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "searchRadius", searchRadius);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "intensity", intensity);
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
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "useAtBCapture", useAtBCapture);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "contractMarketReportRefresh", contractMarketReportRefresh);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "unitMarketReportRefresh", unitMarketReportRefresh);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "startGameDelay", startGameDelay);

        //Mass Repair/Salvage Options
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "massRepairUseExtraTime", massRepairUseExtraTime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "massRepairUseRushJob", massRepairUseRushJob);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "massRepairAllowCarryover", massRepairAllowCarryover);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "massRepairOptimizeToCompleteToday", massRepairOptimizeToCompleteToday);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "massRepairScrapImpossible", massRepairScrapImpossible);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "massRepairUseAssignedTechsFirst", massRepairUseAssignedTechsFirst);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "massRepairReplacePod", massRepairReplacePod);
        
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<massRepairOptions>");

        for (int i = 0; i < massRepairOptions.size(); i++) {
        	MassRepairOption mro = massRepairOptions.get(i);
        	
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2) + "<massRepairOption" + i + ">");
        	
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3, "type", mro.getType());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3, "active", mro.isActive() ? 1 : 0);
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3, "skillMin", mro.getSkillMin());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3, "skillMax", mro.getSkillMax());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3, "btnMin", mro.getBthMin());
        	MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3, "btnMax", mro.getBthMax());
        	
        	pw1.println(MekHqXmlUtil.indentStr(indent + 2) + "</massRepairOption" + i + ">");
        }
        
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "</massRepairOptions>");
        
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<salaryTypeBase>"
                    + Utilities.printIntegerArray(salaryTypeBase)
                    + "</salaryTypeBase>");
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<salaryXpMultiplier>"
                    + Utilities.printDoubleArray(salaryXpMultiplier)
                    + "</salaryXpMultiplier>");

        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                    + "<usePortraitForType>"
                    + Utilities.printBooleanArray(usePortraitForType)
                    + "</usePortraitForType>");
        String csv = "";
        for (int i = 0; i < rats.length; i++) {
        	csv += rats[i];
        	if (i < rats.length - 1) {
        		csv += ",";
        	}
        }
        pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                + "<rats>"
                + MekHqXmlUtil.escape(csv)
                + "</rats>");
        if (staticRATs) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<staticRATs/>");
        }
        if (ignoreRatEra) {
            pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<ignoreRatEra/>");
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</campaignOptions>");
    }

    public static CampaignOptions generateCampaignOptionsFromXml(Node wn) {
        MekHQ.logMessage("Loading Campaign Options from XML...", 4);

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

            MekHQ.logMessage("---", 5);
            MekHQ.logMessage(wn2.getNodeName(), 5);
            MekHQ.logMessage("\t" + wn2.getTextContent(), 5);

            if (wn2.getNodeName().equalsIgnoreCase("clanPriceModifier")) {
                retVal.clanPriceModifier = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useFactionForNames")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.useFactionForNames = true;
                } else {
                    retVal.useFactionForNames = false;
                }
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
            } else if (wn2.getNodeName().equalsIgnoreCase("useRemfEdge")) {
                retVal.useRemfEdge = Boolean.parseBoolean(wn2.getTextContent());
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
            } else if (wn2.getNodeName().equalsIgnoreCase("payForParts")) {
                retVal.payForParts = Boolean.parseBoolean(wn2.getTextContent());
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
            } else if (wn2.getNodeName().equalsIgnoreCase("payForRecruitment")) {
                retVal.payForRecruitment = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useLoanLimits")) {
                retVal.useLoanLimits = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("sellUnits")) {
                retVal.sellUnits = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("sellParts")) {
                retVal.sellParts = Boolean.parseBoolean(wn2.getTextContent());
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
            } else if (wn2.getNodeName().equalsIgnoreCase("equipmentContractPercent")) {
                retVal.equipmentContractPercent = Double.parseDouble(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("equipmentContractBase")) {
                retVal.equipmentContractBase = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("equipmentContractSaleValue")) {
                retVal.equipmentContractSaleValue = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("blcSaleValue")) {
                retVal.blcSaleValue = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("acquisitionSupportStaffOnly")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.acquisitionSupportStaffOnly = true;
                } else {
                    retVal.acquisitionSupportStaffOnly = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("limitByYear")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.limitByYear = true;
                } else {
                    retVal.limitByYear = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("disallowExtinctStuff")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.disallowExtinctStuff = true;
                } else {
                    retVal.disallowExtinctStuff = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("allowClanPurchases")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.allowClanPurchases = true;
                } else {
                    retVal.allowClanPurchases = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("allowISPurchases")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.allowISPurchases = true;
                } else {
                    retVal.allowISPurchases = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("allowCanonOnly")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.allowCanonOnly = true;
                } else {
                    retVal.allowCanonOnly = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("allowCanonRefitOnly")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.allowCanonRefitOnly = true;
                } else {
                    retVal.allowCanonRefitOnly = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("useAmmoByType")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.useAmmoByType = true;
                } else {
                    retVal.useAmmoByType = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("usePercentageMaint")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.usePercentageMaint = true;
                } else {
                    retVal.usePercentageMaint = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("techLevel")) {
                retVal.techLevel = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useUnitRating") || wn2.getNodeName().equalsIgnoreCase
                    ("useDragoonRating")) {
                if (wn2.getTextContent().equalsIgnoreCase("true")) {
                    retVal.useUnitRating = true;
                } else {
                    retVal.useUnitRating = false;
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("unitRatingMethod") || wn2.getNodeName().equalsIgnoreCase
                    ("dragoonsRatingMethod")) {
                if (!wn2.getTextContent().isEmpty() && (wn2.getTextContent() != null)) {
                    UnitRatingMethod method = UnitRatingMethod.getUnitRatingMethod(wn2.getTextContent());
                    retVal.setUnitRatingMethod((method != null) ? method : UnitRatingMethod.CAMPAIGN_OPS);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("usePortraitForType")) {
                String[] values = wn2.getTextContent().split(","); //$NON-NLS-1$
                for (int i = 0; i < values.length; i++) {
                    if(i < retVal.usePortraitForType.length) {
                        retVal.usePortraitForType[i] = Boolean.parseBoolean(values[i].trim());
                    }
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("destroyByMargin")) {
                retVal.destroyByMargin = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("destroyMargin")) {
                retVal.destroyMargin = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("destroyPartTarget")) {
                retVal.destroyPartTarget = Integer.parseInt(wn2.getTextContent().trim());
            }else if (wn2.getNodeName().equalsIgnoreCase("maintenanceCycleDays")) {
                retVal.maintenanceCycleDays = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("maintenanceBonus")) {
                retVal.maintenanceBonus = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useQualityMaintenance")) {
                retVal.useQualityMaintenance = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("reverseQualityNames")) {
                retVal.reverseQualityNames = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("useUnofficalMaintenance")) {
                retVal.useUnofficalMaintenance = Boolean.parseBoolean(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("checkMaintenance")) {
                retVal.checkMaintenance = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("minimumHitsForVees")) {
                retVal.minimumHitsForVees = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("maxAcquisitions")) {
                retVal.maxAcquisitions = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useUnofficialProcreation")) {
            	retVal.useUnofficialProcreation = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useUnofficialProcreationNoRelationship")) {
            	retVal.useUnofficialProcreationNoRelationship = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useParentage")) {
                retVal.useParentage = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("logConception")) {
                retVal.logConception = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useTransfers")) {
            	retVal.useTransfers = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("capturePrisoners")) {
            	retVal.capturePrisoners = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("defaultPrisonerStatus")) {
                retVal.defaultPrisonerStatus = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useRandomHitsForVees")) {
                retVal.useRandomHitsForVees = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("personnelMarketType")) {
                retVal.personnelMarketType = Integer.parseInt(wn2.getTextContent().trim());
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
                String[] values = wn2.getTextContent().split(",");
                for (int i = 0; i < values.length; i++) {
                    retVal.salaryTypeBase[i] = Integer.parseInt(values[i]);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("salaryXpMultiplier")) {
                String[] values = wn2.getTextContent().split(",");
                for (int i = 0; i < values.length; i++) {
                    retVal.salaryXpMultiplier[i] = Double.parseDouble(values[i]);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoMW")) {
                retVal.probPhenoMW = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoAero")) {
                retVal.probPhenoAero = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoBA")) {
                retVal.probPhenoBA = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("probPhenoVee")) {
                retVal.probPhenoVee = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("tougherHealing")) {
                retVal.tougherHealing = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("useAtB")) {
                retVal.useAtB = Boolean.parseBoolean(wn2.getTextContent().trim());
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
            } else if (wn2.getNodeName().equalsIgnoreCase("trackUnitFatigue")) {
                retVal.trackUnitFatigue = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("trackOriginalUnit")) {
                retVal.trackOriginalUnit = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("mercSizeLimited")) {
                retVal.mercSizeLimited = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("regionalMechVariations")) {
                retVal.regionalMechVariations = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("searchRadius")) {
                retVal.searchRadius = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("intensity")) {
                retVal.intensity = Double.parseDouble(wn2.getTextContent().trim());
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
            } else if (wn2.getNodeName().equalsIgnoreCase("useAtBCapture")) {
                retVal.useAtBCapture = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("contractMarketReportRefresh")) {
                retVal.contractMarketReportRefresh = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("unitMarketReportRefresh")) {
                retVal.unitMarketReportRefresh = Boolean.parseBoolean(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("startGameDelay")) {
                retVal.startGameDelay = Integer.parseInt(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("rats")) {
            	retVal.rats = MekHqXmlUtil.unEscape(wn2.getTextContent().trim()).split(",");
            } else if (wn2.getNodeName().equalsIgnoreCase("staticRATs")) {
                retVal.staticRATs = true;
            } else if (wn2.getNodeName().equalsIgnoreCase("ignoreRatEra")) {
                retVal.ignoreRatEra = true;
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
                NodeList mroList = wn2.getChildNodes();

                for (int mroIdx = 0; mroIdx < mroList.getLength(); mroIdx++) {
                    Node mroNode = mroList.item(mroIdx);

                    if (mroNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    
                    for (int mroTypeIdx = 0; mroTypeIdx < MassRepairOption.VALID_REPAIR_TYPES.length; mroTypeIdx++) {
                    	if (mroNode.getNodeName().equalsIgnoreCase("massRepairOption" + mroTypeIdx)) {
                    		
                    		MassRepairOption mro = new MassRepairOption();
                    		mro.setType(-1);
                    		
                            NodeList mroItemList = mroNode.getChildNodes();

                            for (int mroItemIdx = 0; mroItemIdx < mroItemList.getLength(); mroItemIdx++) {
                                Node mroItemNode = mroItemList.item(mroItemIdx);

                                if (mroItemNode.getNodeType() != Node.ELEMENT_NODE) {
                                    continue;
                                }
                     
                                MekHQ.logMessage("---", 5);
                                MekHQ.logMessage("massRepairOption" + mroTypeIdx + "." + mroItemNode.getNodeName(), 5);
                                MekHQ.logMessage("\t" + mroItemNode.getTextContent(), 5);
                                
                                
                                if (mroItemNode.getNodeName().equalsIgnoreCase("type")) {
                                	mro.setType(Integer.parseInt(mroItemNode.getTextContent().trim()));
                                } else if (mroItemNode.getNodeName().equalsIgnoreCase("active")) {
                                    mro.setActive(Integer.parseInt(mroItemNode.getTextContent().trim()) == 1);
                                } else if (mroItemNode.getNodeName().equalsIgnoreCase("skillMin")) {
                                	mro.setSkillMin(Integer.parseInt(mroItemNode.getTextContent().trim()));
                                } else if (mroItemNode.getNodeName().equalsIgnoreCase("skillMax")) {
                                	mro.setSkillMax(Integer.parseInt(mroItemNode.getTextContent().trim()));
                                } else if (mroItemNode.getNodeName().equalsIgnoreCase("btnMin")) {
                                	mro.setBthMin(Integer.parseInt(mroItemNode.getTextContent().trim()));
                                } else if (mroItemNode.getNodeName().equalsIgnoreCase("btnMax")) {
                                	mro.setBthMax(Integer.parseInt(mroItemNode.getTextContent().trim()));
                                }
                            }
                            
                            if (mro.getType() != -1) {
                            	retVal.addMassRepairOption(mro);
                            }
                    	}
                    }                    
                }            	
            }
        }

        MekHQ.logMessage("Load Campaign Options Complete!", 4);

        return retVal;
    }
    
    public static class MassRepairOption {
    	public MassRepairOption() {
    		
    	}
    	
    	public MassRepairOption(int type) {
    		this (type, false, SkillType.EXP_ULTRA_GREEN, SkillType.EXP_ELITE, 4, 4);
    	}
    	
    	public MassRepairOption(int type, boolean active, int skillMin, int skillMax, int bthMin, int bthMax) {
			this.type = type;
			this.active = active;
			this.skillMin = skillMin;
			this.skillMax = skillMax;
			this.bthMin = bthMin;
			this.bthMax = bthMax;
		}

    	public static int[] VALID_REPAIR_TYPES = new int[] { Part.REPAIR_PART_TYPE.ARMOR, Part.REPAIR_PART_TYPE.AMMO, 
    			Part.REPAIR_PART_TYPE.WEAPON, Part.REPAIR_PART_TYPE.GENERAL_LOCATION, Part.REPAIR_PART_TYPE.ENGINE, 
    			Part.REPAIR_PART_TYPE.GYRO, Part.REPAIR_PART_TYPE.ACTUATOR, Part.REPAIR_PART_TYPE.ELECTRONICS,
    			Part.REPAIR_PART_TYPE.POD_SPACE, Part.REPAIR_PART_TYPE.GENERAL };
    	
		private int type;
    	private boolean active = true;
    	private int skillMin;
    	private int skillMax;
    	private int bthMin;
    	private int bthMax;
    	
		public int getType() {
			return type;
		}
		
		public void setType(int type) {
			this.type = type;
		}
		
		public boolean isActive() {
			return active;
		}
		
		public void setActive(boolean active) {
			this.active = active;
		}
		
		public int getSkillMin() {
			return skillMin;
		}
		
		public void setSkillMin(int skillMin) {
			this.skillMin = skillMin;
		}
		
		public int getSkillMax() {
			return skillMax;
		}
		
		public void setSkillMax(int skillMax) {
			this.skillMax = skillMax;
		}
		
		public int getBthMin() {
			return bthMin;
		}
		
		public void setBthMin(int bthMin) {
			this.bthMin = bthMin;
		}
		
		public int getBthMax() {
			return bthMax;
		}
		
		public void setBthMax(int bthMax) {
			this.bthMax = bthMax;
		}
    }
}
