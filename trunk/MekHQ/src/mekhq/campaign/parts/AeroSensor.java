/*
 * AeroSensor.java
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
import megamek.common.Compute;
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
public class AeroSensor extends Part {

	/**
	 * 
	 */
	private static final long serialVersionUID = -717866644605314883L;

	private boolean largeCraft;
	
	public AeroSensor() {
    	this(0, false, null);
    }
    
    public AeroSensor(int tonnage, boolean lc, Campaign c) {
        super(tonnage, c);
        this.name = "Aerospace Sensors";
        this.largeCraft = lc;
    }
    
    public AeroSensor clone() {
    	AeroSensor clone = new AeroSensor(getUnitTonnage(), largeCraft, campaign);
        clone.copyBaseData(this);
    	return clone;
    }
        
	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		int priorHits = hits;
		if(null != unit && unit.getEntity() instanceof Aero) {
			hits = ((Aero)unit.getEntity()).getSensorHits();
			if(checkForDestruction 
					&& hits > priorHits 
					&& Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
				remove(false);
				return;
			}
		}
	}
	
	@Override 
	public int getBaseTime() {
		if(isSalvaging()) {
			return 1200;
		}
		return 120;
	}
	
	@Override
	public int getDifficulty() {
		if(isSalvaging()) {
			return -2;
		}
		return -1;
	}


	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setSensorHits(hits);
		}
		
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setSensorHits(0);
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setSensorHits(3);
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
		updateConditionFromEntity(false);
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingAeroSensor(getUnitTonnage(), largeCraft, campaign);
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
		if(largeCraft) {
			return 80000;
		}
		return 2000 * getUnitTonnage();
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public int getTechRating() {
		//go with ASF sensors
		return EquipmentType.RATING_C;
	}

	@Override
	public int getAvailability(int era) {
		//go with ASF sensors
		return EquipmentType.RATING_C;
	}
	
	@Override
	public int getTechLevel() {
		return TechConstants.T_ALLOWED_ALL;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof AeroSensor && largeCraft == ((AeroSensor)part).isForDropShip()
				&& (largeCraft || getUnitTonnage() == part.getUnitTonnage());
	}

	public boolean isForDropShip() {
		return largeCraft;
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<dropship>"
				+largeCraft
				+"</dropship>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);		
			if (wn2.getNodeName().equalsIgnoreCase("dropship")) {
				if(wn2.getTextContent().trim().equalsIgnoreCase("true")) {
					largeCraft = true;
				} else {
					largeCraft = false;
				}
			}
		}
	}
	
	@Override
    public String getDetails() {
		String dropper = "";
		if(largeCraft) {
			dropper = " (dropship)";
		}
		return super.getDetails() + ", " + getUnitTonnage() + " tons" + dropper;
    }
	
	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_AERO);
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
	public int getIntroDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getExtinctDate() {
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getReIntroDate() {
		return EquipmentType.DATE_NONE;
	}
	
	
}