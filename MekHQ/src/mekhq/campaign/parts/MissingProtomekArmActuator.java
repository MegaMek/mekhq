/*
 * MissingProtomekActuator.java
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
import megamek.common.Protomech;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingProtomekArmActuator extends MissingPart {
    private static final long serialVersionUID = 719878556021696393L;
    protected int location;

    public MissingProtomekArmActuator() {
        this(0, 0, null);
    }

    public MissingProtomekArmActuator(int tonnage, Campaign c) {
        this(tonnage, -1, c);
    }

    public MissingProtomekArmActuator(int tonnage, int loc, Campaign c) {
        super(tonnage, c);
        this.name = "Protomech Arm Actuator";
        this.location = loc;
    }

    @Override
	public int getBaseTime() {
		return 120;
	}

	@Override
	public int getDifficulty() {
		return 0;
	}

    public void setLocation(int loc) {
        this.location = loc;
    }


    @Override
    public double getTonnage() {
        //TODO: how much do actuators weight?
        //apparently nothing
        return 0;
    }

    public int getLocation() {
        return location;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
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

            if (wn2.getNodeName().equalsIgnoreCase("location")) {
                location = Integer.parseInt(wn2.getTextContent());
            }
        }
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
              unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_ARMCRIT, location, 1);
        }
    }

    @Override
    public String checkFixable() {
    	if(null == unit) {
    		return null;
    	}
        if(unit.isLocationBreached(location)) {
            return unit.getEntity().getLocationName(location) + " is breached.";
        }
        if(unit.isLocationDestroyed(location)) {
            return unit.getEntity().getLocationName(location) + " is destroyed.";
        }
        return null;
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if(null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            ((ProtomekArmActuator)actualReplacement).setLocation(location);
            remove(false);
            //assign the replacement part to the unit
            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof ProtomekArmActuator
                && getUnitTonnage() == ((ProtomekArmActuator)part).getUnitTonnage();
    }

    @Override
    public Part getNewPart() {
        return new ProtomekArmActuator(getUnitTonnage(), location, campaign);
    }

	@Override
	public String getLocationName() {
		return unit != null ? unit.getEntity().getLocationName(location) : null;
	}

	@Override
	public TechAdvancement getTechAdvancement() {
	    return ProtomekLocation.TECH_ADVANCEMENT;
	}

	@Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ACTUATOR;
    }
}
