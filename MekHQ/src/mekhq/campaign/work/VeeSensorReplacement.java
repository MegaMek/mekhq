/*
 * VeeSensorReplacement.java
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

package mekhq.campaign.work;

import java.io.PrintWriter;

import megamek.common.Tank;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.VeeSensor;

/**
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class VeeSensorReplacement extends ReplacementItem {
	private static final long serialVersionUID = 2196730442088755437L;

	public VeeSensorReplacement() {
		this(null);
	}

	public VeeSensorReplacement(Unit unit) {
		super(unit);
		this.name = "Replace sensors";
		this.time = 260;
		this.difficulty = 0;
		reCalc();
	}
    
    @Override
    public void reCalc() {
    	// Do nothing.
    	super.reCalc();
    }

	@Override
	public void fix() {
		super.fix();
		if (unit.getEntity() instanceof Tank) {
			((Tank) unit.getEntity()).setSensorHits(0);
		}
	}

	@Override
	public boolean sameAs(WorkItem task) {
		return (task instanceof VeeSensorReplacement && ((VeeSensorReplacement) task)
				.getUnitId() == this.getUnitId());
	}

	@Override
	public Part stratopsPartNeeded() {
		return new VeeSensor(false, (int) unit.getEntity().getWeight());
	}

	@Override
	public SalvageItem getSalvage() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		writeToXmlEnd(pw1, indent, id);
	}
}
