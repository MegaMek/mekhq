/*
 * MissingMekCockpit.java
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
public class MissingMekCockpit extends MissingPart {
	private static final long serialVersionUID = -1989526319692474127L;

	private int type;
	
	public MissingMekCockpit() {
		this(0, Mech.COCKPIT_STANDARD, null);
	}
	
	public MissingMekCockpit(int tonnage, int t, Campaign c) {
        super(tonnage, c);
        this.type = t;
        this.name = Mech.getCockpitDisplayString(type);
        this.time = 300;
        this.difficulty = 0;
    }

	@Override
	public double getTonnage() {
		switch (type) {
        case Mech.COCKPIT_SMALL:
            return 2;
        case Mech.COCKPIT_TORSO_MOUNTED:
            return 4;
        default:
            return 3;
		}
	}

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_COCKPIT;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("small")) {
				type = Integer.parseInt(wn2.getTextContent());
			}
		}
	}

	@Override
	public int getAvailability(int era) {
		switch (type) {
        case Mech.COCKPIT_COMMAND_CONSOLE:
        	if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_C;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_F;
			} else {
				return EquipmentType.RATING_E;
			}
        case Mech.COCKPIT_SMALL:
        	if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_E;
			}
        case Mech.COCKPIT_TORSO_MOUNTED:
        	if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_X;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_X;
			} else {
				return EquipmentType.RATING_F;
			}
        case Mech.COCKPIT_INDUSTRIAL:
        	if(era == EquipmentType.ERA_SL) {
				return EquipmentType.RATING_B;
			} else if(era == EquipmentType.ERA_SW) {
				return EquipmentType.RATING_C;
			} else {
				return EquipmentType.RATING_C;
			}
        default:
            return EquipmentType.RATING_C;
		}
	}

	@Override
	public int getTechRating() {
		switch (type) {
        case Mech.COCKPIT_SMALL: 	
        case Mech.COCKPIT_TORSO_MOUNTED:
            return EquipmentType.RATING_E;
        case Mech.COCKPIT_INDUSTRIAL:
            return EquipmentType.RATING_C;
        default:
            return EquipmentType.RATING_D;
		}
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof MekCockpit && ((MekCockpit)part).getType() == type;
	}
	
	public int getType() {
		return type;
	}
	 
    @Override
    public String checkFixable() {
        for(int i = 0; i < unit.getEntity().locations(); i++) {
        	if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, i) > 0) {
            	if(unit.isLocationBreached(i)) {
            		return unit.getEntity().getLocationName(i) + " is breached.";
            	}
            	if(unit.isLocationDestroyed(i)) {
            		return unit.getEntity().getLocationName(i) + " is destroyed.";
            	}
            }
        }
        return null;
    }

	@Override
	public Part getNewPart() {
		return new MekCockpit(getUnitTonnage(), type, campaign);
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT);
		}
	}
}
