/*
 * MekGyroRepair.java
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

import megamek.common.CriticalSlot;
import megamek.common.Mech;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekGyroRepair extends RepairItem {
	private static final long serialVersionUID = -4885044444425124132L;

	public MekGyroRepair(Unit unit, int crits) {
        super(unit, crits);
        this.name = "Repair " + ((Mech)unit.getEntity()).getSystemName(Mech.SYSTEM_GYRO);
        this.time = 120;
        this.difficulty = 1;
        if(crits > 1) {
            this.time = 240;
            this.difficulty = 4;
        }
    }
    
    @Override
    public void fix() {
        unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
    }

    @Override
    public void doReplaceChanges() {
        removeSalvage();
        unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
    }

    @Override
    public WorkItem getReplacementTask () {
        return new MekGyroReplacement(unit);
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekGyroRepair
                && ((MekGyroRepair)task).getUnitId() == this.getUnitId());
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		writeToXmlEnd(pw1, indent, id);
	}
}
