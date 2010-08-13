/*
 * WorkItem.java
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
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.team.SupportTeam;

/**
 * Abstract extension of WorkItem for all work on units
 * @author Taharqa
 */
public abstract class UnitWorkItem extends WorkItem {
	private static final long serialVersionUID = 8133426984234337677L;
	//the unit for whom the work is being performed
    protected Unit unit;
    protected int unitId = -1; // For XML serialization.
    
    public UnitWorkItem(Unit unit) {
        super();
        this.unit = unit;
    }
    
    @Override
    public void reCalc() {
    	// Do nothing.
    }
    
    public Unit getUnit() {
        return unit;
    }
    
    public int getUnitId() {
        return unit.getId();
    }
    
    @Override
    public String getDisplayName() {
        return  getDesc() + " for " + unit.getEntity().getDisplayName();
    }
    
    @Override
    public TargetRoll getAllMods() {
        TargetRoll mods = super.getAllMods();
        mods.append(unit.getSiteMod());
        if(unit.getEntity().getQuirks().booleanOption("easy_maintain")) {
            mods.addModifier(-1, "easy to maintain");
        }
        else if(unit.getEntity().getQuirks().booleanOption("difficult_maintain")) {
            mods.addModifier(1, "difficult to maintain");
        }
        return mods;
    }
    
    public boolean canScrap() {
        return true;
    }
    
    protected abstract String maxSkillReached();
    
    @Override
    public String fail(int rating) {
         //increment the minimum skill level required
        setSkillMin(rating + 1);
        String toReturn = super.fail(rating);
        if(getSkillMin() > SupportTeam.EXP_ELITE) {
            toReturn += maxSkillReached();
        }
        return toReturn;
    }

	protected void writeToXmlBegin(PrintWriter pw1, int indent, int id) {
		super.writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+ "<unitId>"
				+ getUnitId()
				+ "</unitId>");
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
				unitId = Integer.parseInt(wn2.getTextContent());
			}
		}
	}

	public int getUnitStoredId() {
		return unitId;
	}
	
	public void setUnit(Unit nt) {
		unit = nt;
	}
}
