/*
 * MissingMekActuator.java
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

import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekActuator extends MissingPart {
	private static final long serialVersionUID = 719878556021696393L;
	protected int type;
	protected int location;

	public MissingMekActuator() {
		this(0, 0, null);
	}
	
    public int getType() {
        return type;
    }
    
    public MissingMekActuator(int tonnage, int type, Campaign c) {
        this(tonnage, type, -1, c);
    }
    
    public MissingMekActuator(int tonnage, int type, int loc, Campaign c) {
    	super(tonnage, c);
        this.type = type;
        Mech m = new BipedMech();
        this.name = m.getSystemName(type) + " Actuator" ;
        this.location = loc;
        this.time = 90;
        this.difficulty = -3;
    }

    @Override
    public double getTonnage() {
    	//TODO: how much do actuators weight?
    	//apparently nothing
    	return 0;
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_ACTUATOR;
    }
    
    public int getLocation() {
    	return location;
    }
    
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<location>"
				+location
				+"</location>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("location")) {
				location = Integer.parseInt(wn2.getTextContent());
			} 
		}
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}

	@Override 
	public void fix() {
		Part replacement = findReplacement(false);
		if(null != replacement) {
			Part actualReplacement = replacement.clone();
			unit.addPart(actualReplacement);
			campaign.addPart(actualReplacement);
			replacement.decrementQuantity();
			((MekActuator)actualReplacement).setLocation(location);
			remove(false);
			//assign the replacement part to the unit			
			actualReplacement.updateConditionFromPart();
		}
	}
	
	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		if(part instanceof MekActuator) {
			MekActuator actuator = (MekActuator)part;
			return actuator.getType() == type && getUnitTonnage() == actuator.getUnitTonnage();
		}
		return false;
	}
	
	@Override
	public String checkFixable() {
		if(unit.isLocationBreached(location)) {
			return unit.getEntity().getLocationName(location) + " is breached.";
		}
		if(unit.isLocationDestroyed(location)) {
			return unit.getEntity().getLocationName(location) + " is destroyed.";
		}
		return null;
	}
	
	@Override
	public boolean onBadHipOrShoulder() {
		return null != unit && unit.hasBadHipOrShoulder(location);
	}

	@Override
	public Part getNewPart() {
		return new MekActuator(getUnitTonnage(), type, -1, campaign);
	}
	
	
	private boolean hasReallyCheckedToday() {
		return checkedToday;
	}
	
	@Override
	public boolean hasCheckedToday() {
		//if this unit has been checked for any other equipment of this same type
		//then return false, regardless of whether this one has been checked
		if(null != unit) {
			for(Part part : unit.getParts()) {
				if(part.getId() == getId()) {
					continue;
				}
				if(part instanceof MissingMekActuator 
						&& ((MissingMekActuator)part).getType() == type 
						&& ((MissingMekActuator)part).hasReallyCheckedToday()) {
					return true;
				}
			}
		}
		return super.hasCheckedToday();
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, type, location);
		}
	}
	
	@Override
	public boolean isOmniPoddable() {
		return type == Mech.ACTUATOR_LOWER_ARM || type == Mech.ACTUATOR_HAND;
	}
}
