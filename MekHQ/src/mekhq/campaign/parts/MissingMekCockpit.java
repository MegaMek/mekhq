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
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekCockpit extends MissingPart {
	private static final long serialVersionUID = -1989526319692474127L;

	private int type;
	protected boolean isClan;

	public MissingMekCockpit() {
		this(0, Mech.COCKPIT_STANDARD, false, null);
	}

	public MissingMekCockpit(int tonnage, int t, boolean isClan,Campaign c) {
        super(tonnage, c);
        this.type = t;
        this.isClan = isClan;
        this.name = Mech.getCockpitDisplayString(type);
    }

	@Override
	public int getBaseTime() {
		return 300;
	}

	@Override
	public int getDifficulty() {
		return 0;
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
		switch(type) {
		case Mech.COCKPIT_SMALL:
		case Mech.COCKPIT_INTERFACE:
		case Mech.COCKPIT_SUPERHEAVY:
		case Mech.COCKPIT_SUPERHEAVY_TRIPOD:
		case Mech.COCKPIT_TRIPOD:
            return EquipmentType.RATING_E;
		case Mech.COCKPIT_INDUSTRIAL:
		case Mech.COCKPIT_PRIMITIVE_INDUSTRIAL:
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
	
	public boolean isClan() {
	    return isClan;
	}

    @Override
    public String checkFixable() {
    	if(null == unit) {
    		return null;
    	}
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
		return new MekCockpit(getUnitTonnage(), type, isClan, campaign);
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT);
		}
	}

	@Override
	public String getLocationName() {
		return null;
	}

	@Override
	public int getLocation() {
		if(type == Mech.COCKPIT_TORSO_MOUNTED) {
			return Mech.LOC_CT;
		} else {
			return Mech.LOC_HEAD;
		}
	}

	@Override
	public int getIntroDate() {
    	//TODO: where are aerospace cockpits
    	//TODO: differentiate clan for some designs
		switch(type) {
		case Mech.COCKPIT_STANDARD:
			return 2468;
		case Mech.COCKPIT_SMALL:
			return 3060;
		case Mech.COCKPIT_COMMAND_CONSOLE:
			return 2625;
		case Mech.COCKPIT_TORSO_MOUNTED:
		case Mech.COCKPIT_DUAL:
			return 3053;
		case Mech.COCKPIT_INDUSTRIAL:
			return 2469;
		case Mech.COCKPIT_PRIMITIVE:
			return 2430;
		case Mech.COCKPIT_PRIMITIVE_INDUSTRIAL:
			return 2300;
		case Mech.COCKPIT_SUPERHEAVY:
			return 3060;
		case Mech.COCKPIT_SUPERHEAVY_TRIPOD:
			return 3130;
		case Mech.COCKPIT_TRIPOD:
			return 2590;
		case Mech.COCKPIT_INTERFACE:
			return 3074;
		default:
			return EquipmentType.DATE_NONE;
		}
	}

	@Override
	public int getExtinctDate() {
		switch(type) {
		case Mech.COCKPIT_PRIMITIVE:
			return 2520;
		case Mech.COCKPIT_PRIMITIVE_INDUSTRIAL:
			return 2520;
		case Mech.COCKPIT_COMMAND_CONSOLE:
			return 2850;
		default:
			return EquipmentType.DATE_NONE;
		}
	}

	@Override
	public int getReIntroDate() {
		switch(type) {
		case Mech.COCKPIT_COMMAND_CONSOLE:
			return 3030;
		default:
			return EquipmentType.DATE_NONE;
		}
	}
	
	@Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ELECTRONICS;
    }
}
