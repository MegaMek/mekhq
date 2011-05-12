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
import megamek.common.EquipmentType;
import megamek.common.Mech;
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
		this(false, 0, 0);
	}
	
    public int getType() {
        return type;
    }
    
    public MissingMekActuator(boolean salvage, int tonnage, int type) {
        this(salvage, tonnage, type, -1);
    }
    
    public MissingMekActuator(boolean salvage, int tonnage, int type, int loc) {
    	super(salvage, tonnage);
        this.type = type;
        Mech m = new BipedMech();
        this.name = m.getSystemName(type) + " Actuator" ;
        this.location = loc;
        this.time = 90;
        this.difficulty = -3;
        computeCost();
    }

    private void computeCost () {
        long unitCost = 0;
        switch (getType()) {
            case (Mech.ACTUATOR_UPPER_ARM) : {
                unitCost = 100;
                break;
            }
            case (Mech.ACTUATOR_LOWER_ARM) : {
                unitCost = 50;
                break;
            }
            case (Mech.ACTUATOR_HAND) : {
                unitCost = 80;
                break;
            }
            case (Mech.ACTUATOR_UPPER_LEG) : {
                unitCost = 150;
                break;
            }
            case (Mech.ACTUATOR_LOWER_LEG) : {
                unitCost = 80;
                break;
            }
            case (Mech.ACTUATOR_FOOT) : {
                unitCost = 120;
                break;
            }
            case (Mech.ACTUATOR_HIP) : {
                // not used
                unitCost = 0;
                break;
            }
            case (Mech.ACTUATOR_SHOULDER) : {
                // not used
                unitCost = 0;
                break;
            }
        }
        this.cost = getTonnage() * unitCost;
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MekActuator
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getType() == ((MekActuator)part).getType()
                && getTonnage() == ((MekActuator)part).getTonnage();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_ACTUATOR;
    }
    
    public int getLocation() {
    	return location;
    }
    
	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<location>"
				+location
				+"</location>");
		writeToXmlEnd(pw1, indent, id);
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
		Part replacement = findReplacement();
		if(null != replacement) {
			unit.addPart(replacement);
			((MekActuator)replacement).setLocation(location);
			remove(false);
			//assign the replacement part to the unit			
			replacement.updateConditionFromPart();
		}
	}
	
	@Override
	public boolean isAcceptableReplacement(Part part) {
		if(part instanceof MekActuator) {
			MekActuator actuator = (MekActuator)part;
			return actuator.getType() == type && tonnage == actuator.getTonnage();
		}
		return false;
	}
	
	@Override
	public String checkFixable() {
		if(unit.isLocationDestroyed(location)) {
			return unit.getEntity().getLocationName(location) + " is destroyed.";
		}
		return null;
	}

	@Override
	public Part getNewPart() {
		return new MekActuator(isSalvage(), getTonnage(), type, -1);
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
}
