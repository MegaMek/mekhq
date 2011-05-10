/*
 * MekActuator.java
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

import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.work.MekActuatorReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekActuator extends Part {
	private static final long serialVersionUID = 719878556021696393L;
	protected int type;
	protected int location;

	public MekActuator() {
		this(false, 0, 0);
	}
	
    public int getType() {
        return type;
    }
    
    public MekActuator(boolean salvage, int tonnage, int type) {
        this(salvage, tonnage, type, -1);
    }
    
    public MekActuator(boolean salvage, int tonnage, int type, int loc) {
    	super(salvage, tonnage);
        this.type = type;
        Mech m = new BipedMech();
        this.name = m.getSystemName(type) + " Actuator" ;
        this.location = loc;
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
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof MekActuatorReplacement 
                && tonnage == ((MekActuatorReplacement)task).getUnit().getEntity().getWeight()
                && type == ((MekActuatorReplacement)task).getType();
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
		hits = 0;
		if(null != unit) {
			unit.repairSystem(CriticalSlot.TYPE_SYSTEM, type, location);
		}
	}

	@Override
	public Part getReplacementPart() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, type, location);
			if(!salvage) {
				unit.campaign.removePart(this);
			}
			//TODO create replacement part and add it to entity
		}
		unit.removePart(this);
		unit = null;
		location = -1;
	}

	@Override
	public void updateCondition() {
		if(null != unit) {
			hits = unit.getEntity().getHitCriticals(CriticalSlot.TYPE_SYSTEM, type, location);	
			if(hits == 0) {
				time = 0;
				difficulty = 0;
			} 
			else if(hits >= 1) {
				time = 120;
				difficulty = 0;
			}
		}
		
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}
	
	@Override
	public String getDetails() {
		if(null != unit) {
			return unit.getEntity().getLocationName(location);
		}
		return "";
	}
}
