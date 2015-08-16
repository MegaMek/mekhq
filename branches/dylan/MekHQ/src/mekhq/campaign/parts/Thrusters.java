/*
 * Avionics.java
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
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Thrusters extends Part {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -336290094932539638L;
	private boolean isLeftThrusters = false;

	public Thrusters() {
    	this(0, null);
    }
	
	public Thrusters(int tonnage, Campaign c) {
		this(tonnage, c, false);
	}
    
    public Thrusters(int tonnage, Campaign c, boolean left) {
        super(tonnage, c);
        this.name = "Thrusters";
        isLeftThrusters = left;
    }
    
    public Thrusters clone() {
    	Thrusters clone = new Thrusters(0, campaign, isLeftThrusters);
        clone.copyBaseData(this);
    	return clone;
    }
        
	@Override
	public void updateConditionFromEntity() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			if (isLeftThrusters) {
				hits = ((Aero)unit.getEntity()).getLeftThrustHits();
			} else {
				hits = ((Aero)unit.getEntity()).getRightThrustHits();
			}
		}
		if(hits > 0) {
			time = 90;
			difficulty = -1;
		} else {
			time = 0;
			difficulty = 0;
		}
		if(isSalvaging()) {
			time = 600;
			difficulty = -2;
		}
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			if (isLeftThrusters) {
				((Aero)unit.getEntity()).setLeftThrustHits(hits);
			} else {
				((Aero)unit.getEntity()).setRightThrustHits(hits);
			}
		}
		
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Aero) {
			if (isLeftThrusters) {
				((Aero)unit.getEntity()).setLeftThrustHits(0);
			} else {
				((Aero)unit.getEntity()).setRightThrustHits(0);
			}
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Aero) {
			if (isLeftThrusters) {
				((Aero)unit.getEntity()).setLeftThrustHits(4);
			} else {
				((Aero)unit.getEntity()).setRightThrustHits(4);
			}
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
		return new MissingAvionics(getUnitTonnage(), campaign);
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
		return 25000;
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

	@Override
	public boolean isSamePartType(Part part) {
		boolean match = false;
		if (part instanceof Thrusters) {
			Thrusters t = (Thrusters) part;
			if (t.isLeftThrusters() == isLeftThrusters) {
				match = true;
			}
		}
		return match;
	}
    
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<isLeftThrusters>"
				+isLeftThrusters
				+"</isLeftThrusters>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("isLeftThrusters")) {
				isLeftThrusters = Boolean.parseBoolean(wn2.getTextContent());
			}
		}
	}
	
	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_AERO);
	}
	
	public boolean isLeftThrusters() {
		return isLeftThrusters;
	}
	
	public void setLeftThrusters(boolean b) {
		isLeftThrusters = b;
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
	
}