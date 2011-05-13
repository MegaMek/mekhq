/*
 * MissingMekSensor.java
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

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekSensor extends MissingPart {
	private static final long serialVersionUID = 931907976883324097L;

	public MissingMekSensor() {
		this(false, 0);
	}
	
	public MissingMekSensor(boolean salvage, int tonnage) {
        super(salvage, tonnage);
        this.name = "Mech Sensors";
        this.time = 260;
        this.difficulty = 0;
        computeCost();
    }
	
	protected void computeCost() {
		this.cost = 2000 * getTonnage();
	}

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MekSensor
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus());
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_SENSOR;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// Do nothing - no fields to load.
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
	public boolean isAcceptableReplacement(Part part) {
		return part instanceof MekSensor;
	}
	
	@Override
    public String checkFixable() {
        for(int i = 0; i < unit.getEntity().locations(); i++) {
            if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, i) > 0
                    && unit.isLocationDestroyed(i)) {
                return unit.getEntity().getLocationName(i) + " is destroyed.";
            }
        }
        return null;
    }

	@Override
	public Part getNewPart() {
		return new MekSensor(isSalvage(), getTonnage());
	}

}
