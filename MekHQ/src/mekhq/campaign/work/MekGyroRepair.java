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

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekGyroRepair extends RepairItem {

    public MekGyroRepair(Unit unit, int crits) {
        super(unit, crits);
        this.name = "Repair gyro";
        this.time = 120;
        this.difficulty = 1;
        if(crits > 1) {
            this.time = 240;
            this.difficulty = 4;
        }
    }
    
    @Override
    public void fix() {
        unit.getEntity().removeCriticals(Mech.LOC_CT, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO));
    }

    @Override
    public WorkItem replace() {
        return new MekGyroReplacement(unit);
    }

}
