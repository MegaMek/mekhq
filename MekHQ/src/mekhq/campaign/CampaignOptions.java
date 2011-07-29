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

import mekhq.MekHQApp;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author natit
 */
public class CampaignOptions implements Serializable {
	private static final long serialVersionUID = 5698008431749303602L;
	public final static int REPAIR_SYSTEM_STRATOPS = 0;
    public final static int REPAIR_SYSTEM_WARCHEST_CUSTOM = 1;
    public final static int REPAIR_SYSTEM_GENERIC_PARTS = 2;
    //FIXME: This needs to be localized
    public final static String [] REPAIR_SYSTEM_NAMES = {"Strat Ops", "Warchest Custom", "Generic Spare Parts"};

    private boolean useFactionModifiers;
    private double clanPriceModifier;
    private boolean useEasierRefit;
    private boolean useFactionForNames;
    private int repairSystem;
    
    //personnel related
    private boolean useTactics;
    private boolean useInitBonus;
    private boolean useToughness;
    private boolean useArtillery;
    private boolean useAbilities;
    private boolean useEdge;
    private boolean useImplants;
    
    //unit related
    private boolean useQuirks;
    
    //finance related
    private boolean payForParts;
    private boolean payForUnits;
    private boolean payForSalaries;
    private boolean payForOverhead;
    private boolean payForMaintain;
    private boolean payForTransport;
    private boolean sellUnits;
    private boolean sellParts;

    //xp related
    private int scenarioXP;
    private int killsForXP;
    private int killXPAward;

    public CampaignOptions () {
        useFactionModifiers = false;
        clanPriceModifier = 1.0;
        useEasierRefit = false;
        useFactionForNames = true;
        repairSystem = REPAIR_SYSTEM_STRATOPS;    
        useTactics = false;
        useInitBonus = false;
        useToughness = false;
        useArtillery = false;
        useAbilities = false;
        useEdge = false;
        useImplants = false;
        useQuirks = false;
        payForParts = false;
        payForUnits = false;
        payForSalaries = false;
        payForOverhead = false;
        payForMaintain = false;
        payForTransport = false;
        sellUnits = false;
        sellParts = false;
        scenarioXP = 1;
        killsForXP = 0;
        killXPAward = 0;
    }

    public static String getRepairSystemName (int repairSystem) {
        return REPAIR_SYSTEM_NAMES[repairSystem];
    }
    
    public boolean useFactionModifiers() {
        return useFactionModifiers;
    }
    
    public void setFactionModifiers(boolean b) {
        this.useFactionModifiers = b;
    }
    
    public boolean useEasierRefit() {
        return useEasierRefit;
    }
    
    public void setEasierRefit(boolean b) {
        this.useEasierRefit = b;
    }
    
    public double getClanPriceModifier() {
        return clanPriceModifier;
    }
    
    public void setClanPriceModifier(double d) {
        this.clanPriceModifier = d;
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

	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<campaignOptions>");
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useFactionModifiers", useFactionModifiers); //private boolean useFactionModifiers;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "clanPriceModifier", clanPriceModifier); //private double clanPriceModifier;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useEasierRefit", useEasierRefit); //private boolean useEasierRefit;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useFactionForNames", useFactionForNames); //private boolean useFinances;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "repairSystem", repairSystem); //private int repairSystem;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useTactics", useTactics);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useInitBonus", useInitBonus);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useToughness", useToughness);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useArtillery", useArtillery);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useAbilities", useAbilities);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useEdge", useEdge);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useImplants", useImplants);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useQuirks", useQuirks);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForParts", payForParts);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForUnits", payForUnits);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForSalaries", payForSalaries);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForOverhead", payForOverhead);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForMaintain", payForMaintain);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "payForTransport", payForTransport);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "sellUnits", sellUnits);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "sellParts", sellParts);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "scenarioXP", scenarioXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "killsForXP", killsForXP);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "killXPAward", killXPAward);

		pw1.println(MekHqXmlUtil.indentStr(indent) + "</campaignOptions>");
	}

	public static CampaignOptions generateCampaignOptionsFromXml(Node wn) {
		MekHQApp.logMessage("Loading Campaign Options from XML...", 4);

		wn.normalize();
		CampaignOptions retVal = new CampaignOptions();
		NodeList wList = wn.getChildNodes();

		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < wList.getLength(); x++) {
			Node wn2 = wList.item(x);

			// If it's not an element node, we ignore it.
			if (wn2.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			MekHQApp.logMessage("---",5);
			MekHQApp.logMessage(wn2.getNodeName(),5);
			MekHQApp.logMessage("\t"+wn2.getTextContent(),5);

			if (wn2.getNodeName().equalsIgnoreCase("useFactionModifiers")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useFactionModifiers = true;
				else
					retVal.useFactionModifiers = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("clanPriceModifier")) {
				retVal.clanPriceModifier = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("useEasierRefit")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useEasierRefit = true;
				else
					retVal.useEasierRefit = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useFactionForNames")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useFactionForNames = true;
				else
					retVal.useFactionForNames = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("repairSystem")) {
				retVal.repairSystem = Integer.parseInt(wn2.getTextContent());
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
			} else if (wn2.getNodeName().equalsIgnoreCase("scenarioXP")) {
				retVal.scenarioXP = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("killsForXP")) {
				retVal.killsForXP = Integer.parseInt(wn2.getTextContent().trim());
			} else if (wn2.getNodeName().equalsIgnoreCase("killXPAward")) {
				retVal.killXPAward = Integer.parseInt(wn2.getTextContent().trim());
			} 
		}

		MekHQApp.logMessage("Load Campaign Options Complete!", 4);

		return retVal;
	}
}
