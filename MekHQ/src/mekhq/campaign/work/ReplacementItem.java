/*
 * ReplacementItem.java
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

package mekhq.campaign.work;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.TargetRoll;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.GenericSparePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.team.SupportTeam;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class ReplacementItem extends UnitWorkItem {
	private static final long serialVersionUID = -3241463995328584963L;
	protected Part part;
    protected boolean partCheck;
    protected int partId = -1; // Used for XML Serialization.
    
    public ReplacementItem(Unit unit) {
        super(unit);
        this.partCheck = false;
        reCalc();
    }
    
    @Override
    public void reCalc() {
    	// Do nothing.
    	super.reCalc();
    }

    public Part getPart() {
        return part;
    }
    
    public void setPart(Part part) {
        this.part = part;
    }
    
    public boolean hasPart() {
        return null != part;
    }
    
    /**
     * uses the part and if this depletes the part, returns true
     * @return
     */
    public void useUpPart() {
        if(hasPart()) {
            if (getPart() instanceof GenericSparePart) {
                GenericSparePart genericSparePart = (GenericSparePart) getPart();
                GenericSparePart partNeeded = (GenericSparePart) partNeeded();
                genericSparePart.setAmount(genericSparePart.getAmount() - partNeeded.getAmount());
                if (genericSparePart.getAmount() < 1) {
                    ((GenericSparePart) getPart()).setAmount(0);
                }
            } else {
                unit.campaign.removePart(part);
                this.part = null;
            }
        }
    }
    
    public boolean hasCheckedForPart() {
        return partCheck;
    }
    
    public void setPartCheck(boolean b) {
        this.partCheck = b;
    }
    
    @Override
    public void fix() {
        if (hasPart() && getPart() instanceof GenericSparePart) {
            GenericSparePart genericSparePart = (GenericSparePart) getPart();
            GenericSparePart partNeeded = (GenericSparePart) partNeeded();
            unit.campaign.addWork(getSalvage());
            useUpPart();
        } else {
            unit.campaign.addWork(getSalvage());
            useUpPart();
        }
    }
    
    @Override
    public TargetRoll getAllMods() {
        TargetRoll target = super.getAllMods();
        if(null != part && part.isSalvage()) {
            target.addModifier(1, "salvaged part");
        }
        return target;
    }
    
    @Override
    protected String maxSkillReached() {
        useUpPart();
        //reset the skill min counter back to green
        setSkillMin(SupportTeam.EXP_GREEN);
        return "<br/><emph><b>Component destroyed!</b></emph>";
    }
    
    public abstract Part stratopsPartNeeded();
    
    public abstract SalvageItem getSalvage();
    
    @Override
    public String getDetails() {
        if (partNeeded() instanceof GenericSparePart) {
            // The correct amount is in partNeeded
            if (hasPart()) {
                return "Using " + partNeeded().getDesc();
            } else {
                return "Needs " + partNeeded().getDesc();
            }
        } else {
            if(hasPart()) {
                return "Using " + part.getDesc();
            } else {
                return "Needs " + partNeeded().getDesc();
            }
        }
    }
    
    @Override
    public String getToolTip() {
        String toReturn = "<html>" + getStats() + "<br/>";
        if (partNeeded() instanceof GenericSparePart) {
            if(hasPart()) {
                toReturn += "Using " + partNeeded().getDesc() + "<br/>";
            } else {
                toReturn += "Needs " + partNeeded().getDesc() + "<br/>";
            }
        } else {
            if(hasPart()) {
                toReturn += "Using " + part.getDesc() + "<br/>";
            } else {
                toReturn += "Needs " + partNeeded().getDesc() + "<br/>";
            }
        }
        toReturn += "</html>";
        return toReturn;
    }

    public Part partNeeded () {
        Part stratopsPartNeeded = stratopsPartNeeded();
        if (stratopsPartNeeded == null) {
            return null;
        }
        
        int repairSystem = unit.campaign.getCampaignOptions().getRepairSystem();
        if (repairSystem == CampaignOptions.REPAIR_SYSTEM_STRATOPS) {
            return stratopsPartNeeded;
        } else if (repairSystem == CampaignOptions.REPAIR_SYSTEM_GENERIC_PARTS) {
            int amount = 1;
            // Proportion of the total base value (undamaged) of all parts represented by this part
            double costProportion = ((double) stratopsPartNeeded.getCost()) / ((double) unit.getFullBaseValueOfParts());
            amount = (int) Math.round(costProportion * unit.getBuyCost());

            return new GenericSparePart(stratopsPartNeeded.getTech(), amount);
        } else if (repairSystem == CampaignOptions.REPAIR_SYSTEM_WARCHEST_CUSTOM) {
            return stratopsPartNeeded;
        } else {
            return null;
        }
    }

    public boolean hasEnoughGenericSpareParts () {
        if (!hasPart()) {
            return false;
        } else if (partNeeded() instanceof GenericSparePart) {
            GenericSparePart partNeeded = (GenericSparePart) partNeeded();
            GenericSparePart currentPart = (GenericSparePart) getPart();
            if (currentPart.getAmount() < partNeeded.getAmount())
                return false;
            else
                return true;
        } else {
            return true;
        }
    }

    @Override
    public String checkFixable() {
        // Checked through SupportTeam::getTargetFor
        /*
        if (partNeeded() instanceof GenericSparePart
                && !hasEnoughGenericSpareParts()) {
            return "Not enough spare parts";
        }
        */
        return super.checkFixable();
    }

	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		super.writeToXmlBegin(pw1, indent, id);
		
		// The part may not be defined.
		// If it isn't, well, we can't save it.
		if (part != null) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+"<partId>"
					+part.getId()
					+"</partId>");
		}
		
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<partCheck>"
				+partCheck
				+"</partCheck>");
	}

	public int getPartId() {
		return partId;
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("partId")) {
				partId = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("partCheck")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					partCheck = true;
				else
					partCheck = false;
			}
		}
		
		super.loadFieldsFromXmlNode(wn);
	}
}
