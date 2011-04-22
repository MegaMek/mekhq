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
    private boolean useFinances;
    private boolean useFactionForNames;
    private int repairSystem;

    public CampaignOptions () {
        useFactionModifiers = false;
        clanPriceModifier = 1.0;
        useEasierRefit = false;
        useFinances = false;
        useFactionForNames = true;
        repairSystem = REPAIR_SYSTEM_STRATOPS;    
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
    
    public boolean useFinances() {
        return useFinances;
    }
    
    public void setFinances(boolean b) {
        this.useFinances = b;
    }
    
    public boolean useFactionForNames() {
        return useFactionForNames;
    }
    
    public void setFactionForNames(boolean b) {
        this.useFactionForNames = b;
    }

	public void writeToXml(PrintWriter pw1, int indent) {
		pw1.println(MekHqXmlUtil.indentStr(indent) + "<campaignOptions>");
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useFactionModifiers", useFactionModifiers); //private boolean useFactionModifiers;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "clanPriceModifier", clanPriceModifier); //private double clanPriceModifier;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useEasierRefit", useEasierRefit); //private boolean useEasierRefit;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useFinances", useFinances); //private boolean useFinances;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "useFactionForNames", useFactionForNames); //private boolean useFinances;
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "repairSystem", repairSystem); //private int repairSystem;
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
			} else if (wn2.getNodeName().equalsIgnoreCase("useFinances")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useFinances = true;
				else
					retVal.useFinances = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("useFactionForNames")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.useFactionForNames = true;
				else
					retVal.useFactionForNames = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("repairSystem")) {
				retVal.repairSystem = Integer.parseInt(wn2.getTextContent());
			}
		}

		MekHQApp.logMessage("Load Campaign Options Complete!", 4);

		return retVal;
	}
}
