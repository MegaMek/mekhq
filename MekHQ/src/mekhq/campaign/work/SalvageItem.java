/*
 * SalvageItem.java
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

import megamek.common.EquipmentType;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Part;
import mekhq.campaign.team.SupportTeam;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class SalvageItem extends UnitWorkItem {
	private static final long serialVersionUID = -309078315443493468L;
	//the id of a corresponding repair item that must be dealt with when this item is processed
    protected int repairId = NONE;
    
    public SalvageItem(Unit u) {
        super(u);
    }
    
    @Override
    public void reCalc() {
    	super.reCalc();
    }
  
    @Override
    public String getDetails() {
        if(NONE != repairId) {
            return "Damaged";
        } else {
            return "Functional";
        }
        
    }
    
    @Override
    public void fix() {
        removePart();
        unit.campaign.addPart(getPart());
        unit.campaign.addWork(getReplacement());
        RepairItem repair = (RepairItem)unit.campaign.getTask(repairId);
        if(null != repair) {
            //remove the repair item
            unit.campaign.removeTask(repair);
        }
    }
    
    public void scrap() {
        removePart();
        unit.campaign.addWork(getReplacement());
        RepairItem repair = (RepairItem)unit.campaign.getTask(repairId);
        if(null != repair) {
            //remove the repair item
            unit.campaign.removeTask(repair);
        }
        unit.campaign.removeTask(this);
    }
    
    @Override
    public boolean canScrap() {
        //can only scrap an item if it is legal to salvage it
        return null == checkFixable();
    }
    
    /**
     * sets the given part as destroyed and unrepairable on the entity
     */
    public abstract void removePart();
    
    public abstract ReplacementItem getReplacement();
    
    public abstract Part getPart();
    
    public int getRepairId() {
        return repairId;
    }
    
    public void setRepairId(int id) {
        this.repairId = id;
    }
    
    @Override
    public String maxSkillReached() {
        //See notes in RepairItem#MaxSkillReached
        return "<br/><emph><b>Item cannot be salvaged, it must be scapped instead.</b></emph>";
    }
    
    @Override
    public String getToolTip() {
        if(getSkillMin() > SupportTeam.EXP_ELITE) {
            return "<html> This task is impossible.<br/>You can use the right-click menu to scrap this component.</html>";
        } 
        return super.getToolTip();
    }

	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		super.writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<repairId>"
				+repairId
				+"</repairId>");
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("repairId")) {
				repairId = Integer.parseInt(wn2.getTextContent());
			}
		}
		
		super.loadFieldsFromXmlNode(wn);
	}
	
	@Override
	public int getTechRating() {
		if(null != getPart()) {
			return getPart().getTechRating();
		} else {
			return EquipmentType.RATING_C;
		}
	}
}
