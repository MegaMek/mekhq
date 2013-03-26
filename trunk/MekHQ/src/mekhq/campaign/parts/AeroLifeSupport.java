/*
 * AeroLifeSupport.java
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

import megamek.common.Aero;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class AeroLifeSupport extends Part {

	/**
	 * 
	 */
	private static final long serialVersionUID = -717866644605314883L;

	private long cost;
	private boolean fighter;
	
	public AeroLifeSupport() {
    	this(0, 0, false, null);
    }
    
    public AeroLifeSupport(int tonnage, long cost, boolean f, Campaign c) {
        super(tonnage, c);
        this.cost = cost;
        this.name = "Fighter Life Support";
        this.fighter = f;
        if(!fighter) {
        	this.name = "Spacecraft Life Support";
        }
    }
    
    public AeroLifeSupport clone() {
    	AeroLifeSupport clone = new AeroLifeSupport(getUnitTonnage(), cost, fighter, campaign);
        clone.copyBaseData(this);
    	return clone;
    }
        
	@Override
	public void updateConditionFromEntity() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			 if(((Aero)unit.getEntity()).hasLifeSupport()) {
				 hits = 0;
			 } else { 
				 hits = 1;
			 }
		}
		if(hits > 0) {
			time = 120;
			difficulty = 1;
		} else {
			time = 0;
			difficulty = 0;
		}
		if(isSalvaging()) {
			time = 6720;
			difficulty = 0;
		}
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			if(hits > 0) {
				((Aero)unit.getEntity()).setLifeSupport(false);
			} else {
				((Aero)unit.getEntity()).setLifeSupport(true);
			}
		}
		
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setLifeSupport(true);
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setLifeSupport(false);
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.addPart(missing, 0);
		}
		setSalvaging(false);
		setUnit(null);
		updateConditionFromEntity();
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingAeroLifeSupport(getUnitTonnage(), cost, fighter, campaign);
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public long getStickerPrice() {
		return cost;
	}
	
	public void calculateCost() {
		if(fighter) {
			cost = 50000;
		}
		if(null != unit) {
			cost = 5000 * (((Aero)unit.getEntity()).getNCrew() + ((Aero)unit.getEntity()).getNPassenger());
		}	
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}
	
	@Override
	public int getTechLevel() {
		return TechConstants.T_IS_TW_ALL;
	}
	
	@Override 
	public int getTechBase() {
		return T_BOTH;	
	}

	public boolean isForFighter() {
		return fighter;
	}
	
	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof AeroLifeSupport && fighter == ((AeroLifeSupport)part).isForFighter()
				&& (getStickerPrice() == part.getStickerPrice());
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<fighter>"
				+fighter
				+"</fighter>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<cost>"
				+cost
				+"</cost>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);		
			if (wn2.getNodeName().equalsIgnoreCase("fighter")) {
				if(wn2.getTextContent().trim().equalsIgnoreCase("true")) {
					fighter = true;
				} else {
					fighter = false;
				}
			}
			else if (wn2.getNodeName().equalsIgnoreCase("cost")) {
				cost = Long.parseLong(wn2.getTextContent());
			} 
		}
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_AERO);
	}
	
}