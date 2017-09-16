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

import megamek.common.CriticalSlot;
import megamek.common.Protomech;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingProtomekSensor extends MissingPart {
    private static final long serialVersionUID = 719878556021696393L;

    public MissingProtomekSensor() {
        this(0, null);
    }

    public MissingProtomekSensor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Protomech Sensors";
    }

    @Override
	public int getBaseTime() {
		return 120;
	}

	@Override
	public int getDifficulty() {
		return 0;
	}

    @Override
    public double getTonnage() {
        //TODO: how much do actuators weight?
        //apparently nothing
        return 0;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {

    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
              unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Protomech.SYSTEM_HEADCRIT, Protomech.LOC_HEAD, 1);
        }
    }

    @Override
    public String checkFixable() {
    	if(null == unit) {
    		return null;
    	}
        if(unit.isLocationBreached(Protomech.LOC_HEAD)) {
            return unit.getEntity().getLocationName(Protomech.LOC_HEAD) + " is breached.";
        }
        if(unit.isLocationDestroyed(Protomech.LOC_HEAD)) {
            return unit.getEntity().getLocationName(Protomech.LOC_HEAD) + " is destroyed.";
        }
        return null;
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if(null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            remove(false);
            //assign the replacement part to the unit
            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return part instanceof ProtomekSensor
                && getUnitTonnage() == ((ProtomekSensor)part).getUnitTonnage();
    }

    @Override
    public Part getNewPart() {
        return new ProtomekSensor(getUnitTonnage(), campaign);
    }

    @Override
   	public String getLocationName() {
   		return unit.getEntity().getLocationName(getLocation());
   	}

	@Override
	public int getLocation() {
		return Protomech.LOC_HEAD;
	}

    @Override
    public TechAdvancement getTechAdvancement() {
        return ProtomekLocation.TECH_ADVANCEMENT;
    }
    
    @Override
	public int getMassRepairOptionType() {
    	return Part.REPAIR_PART_TYPE.ELECTRONICS;
    }
}
