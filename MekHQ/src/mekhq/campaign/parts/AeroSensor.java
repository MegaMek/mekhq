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
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;

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

	private boolean dropship;
	
	public AeroSensor() {
    	this(0, false, null);
    }
    
    public AeroSensor(int tonnage, boolean drop, Campaign c) {
        super(tonnage, c);
        this.name = "Aerospace Sensors";
        this.dropship = drop;
    }
    
    public AeroSensor clone() {
    	AeroSensor clone = new AeroSensor(getUnitTonnage(), dropship, campaign);
    	clone.hits = this.hits;
    	return clone;
    }
        
	@Override
	public void updateConditionFromEntity() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			hits = ((Aero)unit.getEntity()).getSensorHits();
		}
		if(hits > 0) {
			time = 120;
			difficulty = -1;
		} else {
			time = 0;
			difficulty = 0;
		}
		if(isSalvaging()) {
			time = 1200;
			difficulty = -2;
		}
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
			campaign.addPart(missing);
		}
		setUnit(null);
	}

	@Override
	public Part getMissingPart() {
		return new MissingAeroSensor(getUnitTonnage(), dropship, campaign);
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
		if(dropship) {
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
		return TechConstants.T_IS_TW_ALL;
	}
	
	@Override 
	public int getTechBase() {
		return T_BOTH;	
	}

	@Override
	public boolean isSamePartTypeAndStatus(Part part) {
		if(isReservedForRefit()) {
    		return false;
    	}
		return part instanceof AeroSensor && dropship == ((AeroSensor)part).isForDropShip()
				&& (dropship || getUnitTonnage() == part.getUnitTonnage())
				&& part.needsFixing() == this.needsFixing();
	}

	public boolean isForDropShip() {
		return dropship;
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<dropship>"
				+dropship
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
					dropship = true;
				} else {
					dropship = false;
				}
			}
		}
	}
	
	@Override
    public String getDetails() {
		String dropper = "";
		if(dropship) {
			dropper = " (dropship)";
		}
		return super.getDetails() + ", " + getUnitTonnage() + " tons" + dropper;
    }
	
}