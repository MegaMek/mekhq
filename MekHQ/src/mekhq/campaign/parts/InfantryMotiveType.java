/*
 * InfantryMotiveType.java
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

import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class InfantryMotiveType extends Part {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2915821210551422633L;
	
	private EntityMovementMode mode;

	public InfantryMotiveType() {
    	this(0, null, null);
    }
	
	public InfantryMotiveType(int tonnage, Campaign c, EntityMovementMode m) {
		super(tonnage, c);
		this.mode = m;
		if(null != mode) {
			assignName();
		}

	}
	
	private void assignName() {
		switch (mode) {
        case INF_UMU:
            name = "Scuba Gear";
            break;
        case INF_MOTORIZED:
        	name = "Motorized Vehicle";
            break;
        case INF_JUMP:
        	name = "Jump Pack";
            break;
        case HOVER:
        	name = "Hover Infantry Vehicle";
            break;
        case WHEELED:
        	name = "Wheeled Infantry Vehicle";
            break;
        case TRACKED:
        	name = "Tracked Infantry Vehicle";
            break;
        default:
        	name = "Unknown Motive Type";
		}
	}
	
	@Override
	public void updateConditionFromEntity(boolean checkForDestruction) {
		//nothing to do here
	}

	@Override 
	public int getBaseTime() {
		return 0;
	}
	
	@Override
	public int getDifficulty() {
		return 0;
	}
	
	@Override
	public void updateConditionFromPart() {
		//nothing to do here
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				int number = quantity;
				while(number > 0) {
					spare.incrementQuantity();
					number--;
				}
				campaign.removePart(this);
			}
			unit.removePart(this);
		}	
		setUnit(null);
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingInfantryMotiveType(0, campaign, mode);
	}

	@Override
	public String checkFixable() {
		//nothing to do here
		return null;
	}

	@Override
	public boolean needsFixing() {
		return false;
	}

	@Override
	public long getStickerPrice() {
		 switch (getMovementMode()){
	        case INF_UMU:
	            return 17888;
	        case INF_MOTORIZED:
	        	return (long)(17888 * 0.6);
	        case INF_JUMP:
	        	return (long)(17888 * 1.6);
	        case HOVER:
	        	return (long)(17888 * 2.2 * 5);
	        case WHEELED:
	        	return (long)(17888 * 2.2 * 6);
	        case TRACKED:
	        	return (long)(17888 * 2.2 * 7);
	        default:
	            return 0;
		 }
	}

	@Override
	public double getTonnage() {
		//TODO: what should this be?
		return 0;
	}

	@Override
	public int getTechRating() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAvailability(int era) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTechLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof InfantryMotiveType && mode.equals(((InfantryMotiveType)part).getMovementMode());
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<moveMode>"
				+mode
				+"</moveMode>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);		
			if (wn2.getNodeName().equalsIgnoreCase("mode")) {
				mode = EntityMovementMode.getMode(wn2.getTextContent());
				assignName();
			} 
			else if (wn2.getNodeName().equalsIgnoreCase("moveMode")) {
				mode = EntityMovementMode.getMode(wn2.getTextContent());
				assignName();
			}
		}
	}

	@Override
	public Part clone() {
		return new InfantryMotiveType(0, campaign, mode);
	}
	
	public EntityMovementMode getMovementMode() {
		return mode;
	}
	
	@Override
    public boolean needsMaintenance() {
        return false;
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