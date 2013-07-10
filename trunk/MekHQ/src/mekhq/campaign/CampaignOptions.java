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

import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author natit
 */
public class CampaignOptions implements Serializable {
	private static final long serialVersionUID = 5698008431749303602L;
	
	public final static int TECH_INTRO        = 0;
	public final static int TECH_STANDARD     = 1;
	public final static int TECH_ADVANCED     = 2;
	public final static int TECH_EXPERIMENTAL = 3;
	public final static int TECH_UNOFFICIAL   = 4;
	
	public final static int TRANSIT_UNIT_DAY   = 0;
	public final static int TRANSIT_UNIT_WEEK  = 1;
    public final static int TRANSIT_UNIT_MONTH = 2;
    public final static int TRANSIT_UNIT_NUM   = 3;

	
	public final static String S_TECH = "Tech";
	public final static String S_AUTO = "Automatic Success";
	
	public final static int REPAIR_SYSTEM_STRATOPS = 0;
    public final static int REPAIR_SYSTEM_WARCHEST_CUSTOM = 1;
    public final static int REPAIR_SYSTEM_GENERIC_PARTS = 2;
    //FIXME: This needs to be localized
    public final static String [] REPAIR_SYSTEM_NAMES = {"Strat Ops", "Warchest Custom", "Generic Spare Parts"};

    private double clanPriceModifier;
    private boolean useFactionForNames;
    private int repairSystem;
    private boolean useDragoonRating;
    private boolean useEraMods;
    
    //personnel related
    private boolean useTactics;
    private boolean useInitBonus;
    private boolean useToughness;
    private boolean useArtillery;
    private boolean useAbilities;
    private boolean useEdge;
    private boolean useImplants;
    private int healWaitingPeriod;
    private int naturalHealingWaitingPeriod;
    private boolean useAdvancedMedical; // Unofficial
    private boolean useDylansRandomXp; // Unofficial
    
    //unit related
    private boolean useQuirks;
    
    //tech and unit limits
    private boolean limitByYear;
    private boolean allowClanPurchases;
    private boolean allowISPurchases;
    private boolean allowCanonOnly;
    private int techLevel;
    
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
    private double usedPartsValue;
    private double damagedPartsValue;
    private double canceledOrderReimbursement;

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

    //Dragoon's Rating
    private DragoonsRatingMethod dragoonsRatingMethod;
    
    //random portraits related
    private boolean[] usePortraitForType;

    public CampaignOptions () {
        clanPriceModifier = 1.0;
        useFactionForNames = true;
        repairSystem = REPAIR_SYSTEM_STRATOPS; 
        useEraMods = false;
        useDragoonRating = true;
        useTactics = false;
        useInitBonus = false;
        useToughness = false;
        useArtillery = false;
        useAbilities = false;
        useEdge = false;
        useImplants = false;
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
        useLoanLimits = true;
        sellUnits = false;
        sellParts = false;
        limitByYear = true;
        allowClanPurchases = true;
        allowISPurchases = true;
        allowCanonOnly = false;
        techLevel = TECH_EXPERIMENTAL;
        scenarioXP = 1;
        killsForXP = 0;
        killXPAward = 0;
        nTasksXP = 25;
        tasksXP = 1;
        mistakeXP = 0;
        successXP = 0;
        usedPartsValue = 0.5;
        damagedPartsValue = 0.33;
        canceledOrderReimbursement = 0.5;
        usePortraitForType = new boolean[Person.T_NUM];
        for(int i = 0; i < Person.T_NUM; i++) {
        	usePortraitForType[i] = false;
        }
        usePortraitForType[Person.T_MECHWARRIOR] = true;
        idleXP = 0;
        targetIdleXP = 10;
        monthsIdleXP = 2;
        dragoonsRatingMethod = DragoonsRatingMethod.TAHARQA;
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
        
    }

    public DragoonsRatingMethod getDragoonsRatingMethod() {
        return dragoonsRatingMethod;
    }

    public void setDragoonsRatingMethod(DragoonsRatingMethod method) {
        this.dragoonsRatingMethod = method;
    }

    public static String getRepairSystemName (int repairSystem) {
        return REPAIR_SYSTEM_NAMES[repairSystem];
    }
    
    public static String getTechLevelName(int lvl) {
    	switch(lvl) {
    	case TECH_INTRO:
    		return "Introductory";
    	case TECH_STANDARD:
    		return "Standard";
    	case TECH_ADVANCED:
    		return "Advanced";
    	case TECH_EXPERIMENTAL:
    		return "Experimental";
    	case TECH_UNOFFICIAL:
            return "Unofficial";
    	default:
    		return "Unknown";	
    	}
    }
    
    public static String getTransitUnitName(int unit) {
        switch(unit) {
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
    
    public boolean useDragoonRating() {
        return useDragoonRating;
    }
    
    public void setDragoonRating(boolean b) {
        this.useDragoonRating = b;
    }
  
    public double getClanPriceModifier() {
        return clanPriceModifier;
    }
    
    public void setClanPriceModifier(double d) {
        this.clanPriceModifier = d;
    }
    
    public double getUsedPartsValue() {
        return usedPartsValue;
    }
    
    public void setUsedPartsValue(double d) {
        this.usedPartsValue = d;
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
   
    public void setTactics(boolean b) {
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
    
    public boolean useImplants() {
    	return useImplants;
    }
   
    public void setImplants(boolean b) {
    	this.useImplants = b;
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
    	return  sellUnits;
    }
    
    public void setSellUnits(boolean b) {
    	this.sellUnits = b;
    }
    
    public boolean canSellParts() {
    	return  sellParts;
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
    
    public int getTechLevel() {
    	return techLevel;
    }
    
    public void setTechLevel(int lvl) {
    	techLevel = lvl;
    }
    
    public boolean usePortraitForType(int type) {
    	if(type < 0 || type >= usePortraitForType.length) {
    		return false;
    	}
    	return usePortraitForType[type];
    }
    
    public void setUsePortraitForType(int type, boolean b) {
    	if(type < 0 || type >= usePortraitForType.length) {
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
    
	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<campaignOptions>");
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "clanPriceModifier", clanPriceModifier); //private double clanPriceModifier;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useFactionForNames", useFactionForNames); //private boolean useFinances;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "repairSystem", repairSystem); //private int repairSystem;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useDragoonRating", useDragoonRating);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "dragoonsRatingMethod", dragoonsRatingMethod.getDescription());
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useEraMods", useEraMods); 
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useTactics", useTactics);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useInitBonus", useInitBonus);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useToughness", useToughness);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useArtillery", useArtillery);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useAbilities", useAbilities);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useEdge", useEdge);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useImplants", useImplants);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useAdvancedMedical", useAdvancedMedical);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useDylansRandomXp", useDylansRandomXp);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useQuirks", useQuirks);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForParts", payForParts);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForUnits", payForUnits);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForSalaries", payForSalaries);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForOverhead", payForOverhead);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForMaintain", payForMaintain);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForTransport", payForTransport);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "usedPartsValue", usedPartsValue);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "damagedPartsValue", damagedPartsValue);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "canceledOrderReimbursement", canceledOrderReimbursement);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "sellUnits", sellUnits);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "sellParts", sellParts);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "scenarioXP", scenarioXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "killsForXP", killsForXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "killXPAward", killXPAward);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "nTasksXP", nTasksXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "tasksXP", tasksXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "mistakeXP", mistakeXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "successXP", successXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "idleXP", idleXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "targetIdleXP", targetIdleXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "monthsIdleXP", monthsIdleXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "limitByYear", limitByYear);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "allowClanPurchases", allowClanPurchases);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "allowISPurchases", allowISPurchases);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "allowCanonOnly", allowCanonOnly);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "waitingPeriod", waitingPeriod);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "acquisitionSkill", acquisitionSkill);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "acquisitionSupportStaffOnly", acquisitionSupportStaffOnly);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "techLevel", techLevel);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "nDiceTransitTime", nDiceTransitTime);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "constantTransitTime", constantTransitTime);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "unitTransitTime", unitTransitTime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "acquireMosBonus", acquireMosBonus);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "acquireMosUnit", acquireMosUnit);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "acquireMinimumTime", acquireMinimumTime);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "acquireMinimumTimeUnit", acquireMinimumTimeUnit);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "equipmentContractPercent", equipmentContractPercent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "equipmentContractBase", equipmentContractBase);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "equipmentContractSaleValue", equipmentContractSaleValue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "blcSaleValue", blcSaleValue);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "clanAcquisitionPenalty", clanAcquisitionPenalty);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "isAcquisitionPenalty", isAcquisitionPenalty);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useLoanLimits", useLoanLimits);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForRecruitment", payForRecruitment);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "healWaitingPeriod", healWaitingPeriod);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "naturalHealingWaitingPeriod", naturalHealingWaitingPeriod);
        

		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<usePortraitForType>"
				+Utilities.printBooleanArray(usePortraitForType)
				+"</usePortraitForType>");
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
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			MekHQ.logMessage("---",5);
			MekHQ.logMessage(wn2.getNodeName(),5);
			MekHQ.logMessage("\t"+wn2.getTextContent(),5);

			if (wn2.getNodeName().equalsIgnoreCase("clanPriceModifier")) {
				retVal.clanPriceModifier = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("useFactionForNames")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useFactionForNames = true;
				else
					retVal.useFactionForNames = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("repairSystem")) {
				retVal.repairSystem = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("useEraMods")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useEraMods = true;
				else
					retVal.useEraMods = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useTactics")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useTactics = true;
				else
					retVal.useTactics = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useInitBonus")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useInitBonus = true;
				else
					retVal.useInitBonus = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useToughness")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useToughness = true;
				else
					retVal.useToughness = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useArtillery")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useArtillery = true;
				else
					retVal.useArtillery = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useAbilities")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useAbilities = true;
				else
					retVal.useAbilities = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useEdge")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useEdge = true;
				else
					retVal.useEdge = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useImplants")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useImplants = true;
				else
					retVal.useImplants = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useAdvancedMedical")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useAdvancedMedical = true;
				else
					retVal.useAdvancedMedical = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useDylansRandomXp")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useDylansRandomXp = true;
				else
					retVal.useDylansRandomXp = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useQuirks")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useQuirks = true;
				else
					retVal.useQuirks = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("payForParts")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.payForParts = true;
				else
					retVal.payForParts = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("payForUnits")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.payForUnits = true;
				else
					retVal.payForUnits = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("payForSalaries")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.payForSalaries = true;
				else
					retVal.payForSalaries = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("payForOverhead")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.payForOverhead = true;
				else
					retVal.payForOverhead = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("payForMaintain")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.payForMaintain = true;
				else
					retVal.payForMaintain = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("payForTransport")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.payForTransport = true;
				else
					retVal.payForTransport = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("payForRecruitment")) {
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    retVal.payForRecruitment = true;
                else
                    retVal.payForRecruitment = false;
            } else if (wn2.getNodeName().equalsIgnoreCase("useLoanLimits")) {
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    retVal.useLoanLimits = true;
                else
                    retVal.useLoanLimits = false;
            } else if (wn2.getNodeName().equalsIgnoreCase("sellUnits")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.sellUnits = true;
				else
					retVal.sellUnits = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("sellParts")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.sellParts = true;
				else
					retVal.sellParts = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("usedPartsValue")) {
				retVal.usedPartsValue = Double.parseDouble(wn2.getTextContent().trim());
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
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    retVal.equipmentContractBase = true;
                else
                    retVal.equipmentContractBase = false;
            } else if (wn2.getNodeName().equalsIgnoreCase("equipmentContractSaleValue")) {
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    retVal.equipmentContractSaleValue = true;
                else
                    retVal.equipmentContractSaleValue = false;
            } else if (wn2.getNodeName().equalsIgnoreCase("blcSaleValue")) {
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    retVal.blcSaleValue = true;
                else
                    retVal.blcSaleValue = false;
            } else if (wn2.getNodeName().equalsIgnoreCase("acquisitionSupportStaffOnly")) {
                if (wn2.getTextContent().equalsIgnoreCase("true"))
                    retVal.acquisitionSupportStaffOnly = true;
                else
                    retVal.acquisitionSupportStaffOnly = false;
            } else if (wn2.getNodeName().equalsIgnoreCase("limitByYear")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.limitByYear = true;
				else
					retVal.limitByYear = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("allowClanPurchases")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.allowClanPurchases = true;
				else
					retVal.allowClanPurchases = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("allowISPurchases")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.allowISPurchases = true;
				else
					retVal.allowISPurchases = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("allowCanonOnly")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.allowCanonOnly = true;
				else
					retVal.allowCanonOnly = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("techLevel")) {
				retVal.techLevel = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("useDragoonRating")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useDragoonRating = true;
				else
					retVal.useDragoonRating = false;
            } else if (wn2.getNodeName().equalsIgnoreCase("dragoonsRatingMethod")) {
                if (!wn2.getTextContent().isEmpty() && (wn2.getTextContent() != null)) {
                    DragoonsRatingMethod method = DragoonsRatingMethod.getDragoonsRatingMethod(wn2.getTextContent());
                    retVal.setDragoonsRatingMethod((method != null) ? method : DragoonsRatingMethod.TAHARQA);
                }
			} else if (wn2.getNodeName().equalsIgnoreCase("usePortraitForType")) {
			 	String[] values = wn2.getTextContent().split(",");
				for(int i = 0; i < values.length; i++) {
					if (values[i].equalsIgnoreCase("true"))
						retVal.usePortraitForType[i] = true;
					else
						retVal.usePortraitForType[i] = false;
				}
			} 
		}

		MekHQ.logMessage("Load Campaign Options Complete!", 4);

		return retVal;
	}

    public enum DragoonsRatingMethod {
        TAHARQA("Taharqa"),
        FLD_MAN_MERCS_REV("FM: Mercenaries (rev)");

        private String description;

        DragoonsRatingMethod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static String[] getDragoonsRatingMethodNames() {
            String[] methods = new String[values().length];
            for (int i = -0; i < values().length; i++) {
                methods[i] = values()[i].getDescription();
            }
            return methods;
        }

        public static DragoonsRatingMethod getDragoonsRatingMethod(String description) {
            for (DragoonsRatingMethod m : values()) {
                if (m.getDescription().equalsIgnoreCase(description)) {
                    return m;
                }
            }
            return null;
        }

    }
}
