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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.Mech;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

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
    public TechAdvancement getTechAdvancement() {
        return Mech.getCockpitTechAdvancement(type);
    }

	@Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ELECTRONICS;
    }
}
