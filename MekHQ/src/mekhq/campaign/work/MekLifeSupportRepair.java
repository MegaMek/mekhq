/*
 * MekLifeSupportRepair.java
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
import megamek.common.Mech;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekLifeSupportRepair extends RepairItem {

    public MekLifeSupportRepair(Unit unit, int h) {
        super(unit, h);
        this.name = "Repair life support";
        this.time = 60;
        this.difficulty = -1;
        if(hits > 1) {
            this.time = 120;
            this.difficulty = 1;
        }
    }
    
    @Override
    public void fix() {
        unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT);
    }

    @Override
    public WorkItem replace() {
        return new MekLifeSupportReplacement(unit);
    }
    
    @Override
    public String checkFixable() {
        for(int i = 0; i < unit.getEntity().locations(); i++) {
            if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i) > 0
                    && unit.isLocationDestroyed(i)) {
                return unit.getEntity().getLocationName(i) + " is destroyed.";
            }
        }
        return super.checkFixable();
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekLifeSupportRepair
                && ((MekLifeSupportRepair)task).getUnitId() == this.getUnitId());
    }
}
