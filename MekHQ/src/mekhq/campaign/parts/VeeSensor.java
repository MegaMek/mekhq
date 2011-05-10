/*
 * VeeSensor.java
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

import megamek.common.EquipmentType;
import mekhq.campaign.work.ReplacementItem;
import mekhq.campaign.work.VeeSensorReplacement;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class VeeSensor extends Part {
	private static final long serialVersionUID = 4101969895094531892L;

	public VeeSensor() {
		this(false, 0);
	}
	
	public VeeSensor(boolean salvage, int tonnage) {
        super(salvage, tonnage);
        this.name = "Vehicle Sensors";
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof VeeSensorReplacement;
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof VeeSensor
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus());
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// Do nothing.
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public Part getReplacementPart() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateCondition() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean needsFixing() {
		// TODO Auto-generated method stub
		return false;
	}
}
