/*
 * StructuralIntegrity.java
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

package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.ConvFighter;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.SimpleTechLevel;
import megamek.common.SmallCraft;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class StructuralIntegrity extends Part {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7723466837496688673L;
	
	// Slight variations for ASFs, CFs, and SC/DS
    static final TechAdvancement TA_ASF = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2200, 2470, 2490).setApproximate(true, false, false)
            .setPrototypeFactions(F_TA).setProductionFactions(F_TH)
            .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_CF = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, 2470, 2490).setProductionFactions(F_TH)
            .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_DS = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2200, 2470, 2490).setApproximate(true, false, false)
            .setPrototypeFactions(F_TA).setProductionFactions(F_TH)
            .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

	private int pointsNeeded;
	
	public StructuralIntegrity() {
		this(0, null);
	}
	
	public StructuralIntegrity(int entityWeight, Campaign c) {
		super(entityWeight, c);
		pointsNeeded = 0;
		this.name = "Structural Integrity";
	}
	
	public StructuralIntegrity clone() {
		StructuralIntegrity clone = new StructuralIntegrity(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
		clone.pointsNeeded = this.pointsNeeded;
		return clone;
	}
	
	
	@Override
	public long getStickerPrice() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			if(unit.getEntity() instanceof Dropship || unit.getEntity() instanceof SmallCraft) {
				return ((Aero)unit.getEntity()).get0SI() * 100000;
			}
			else if(unit.getEntity() instanceof ConvFighter) {
				return ((Aero)unit.getEntity()).get0SI() * 4000;
			} else {
				return ((Aero)unit.getEntity()).get0SI() * 50000;
			}
		}
		return 0;
	}
	
	@Override
	public double getTonnage() {
		//not important I suppose
		return 0;
	}

	@Override
	public boolean isSamePartType(Part part) {
		//can't be salvaged or scrapped, so ignore
		return false;
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("pointsNeeded")) {
				pointsNeeded = Integer.parseInt(wn2.getTextContent());
			} 
		}	
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<pointsNeeded>"
				+pointsNeeded
				+"</pointsNeeded>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	public String checkFixable() {
		return null;
	}
	
	@Override
    public String getDetails() {
		if(null != unit) {
			return pointsNeeded + " points destroyed";
		}
		return "SI not on unit? Wazz up with dat?";
    }


	@Override
	public void fix() {
		super.fix();
		pointsNeeded = 0;
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setSI(((Aero)unit.getEntity()).get0SI());
		}
	}

	@Override
	public MissingPart getMissingPart() {
		//you cant replace this part, so return null
		return null;
	}

	@Override
	public String checkScrappable() {
		return "Structural Integrity cannot be scrapped";
	}
	
	@Override
	public boolean canNeverScrap() {
		return true;
	}
	
	@Override
	public boolean isSalvaging() {
		return false;
	}
	
	@Override
	public void remove(boolean salvage) {
		//you can't remove this part so don't do anything
	}

	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		if(null != unit && unit.getEntity() instanceof Aero) {
			pointsNeeded = ((Aero)unit.getEntity()).get0SI() - ((Aero)unit.getEntity()).getSI();
		}	
		
	}
	
	@Override 
	public int getBaseTime() {
		return 600 * pointsNeeded;
	}
	
	@Override
	public int getDifficulty() {
		return 1;
	}

	@Override
	public void updateConditionFromPart() {
		//you can't replace this part, so don't do anything here
	}

	@Override
	public boolean needsFixing() {
		return pointsNeeded > 0;
	}
	
	public void doMaintenanceDamage(int d) {
        int points = ((Aero)unit.getEntity()).getSI();
        points = Math.max(points - d, 1);
        ((Aero)unit.getEntity()).setSI(points);
        updateConditionFromEntity(false);
    }

	@Override
	public String getLocationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocation() {
		return Entity.LOC_NONE;
	}
	
	@Override
	public TechAdvancement getTechAdvancement() {
	    if (null == getUnit()) {
	        return TA_GENERIC;
	    } else if (getUnit().getEntity().hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
	        return TA_DS;
	    } else if (getUnit().getEntity().hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            return TA_CF;
	    } else {
            return TA_ASF;
	    }
	}
}