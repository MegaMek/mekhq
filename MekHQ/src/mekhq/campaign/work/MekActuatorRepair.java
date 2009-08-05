/*
 * MekActuatorRepair.java
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
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekActuatorRepair extends RepairItem {
    
    protected int loc;
    protected int type;
    
    public MekActuatorRepair(Unit unit, int h, int i, int t) {
        super(unit, h);
        this.loc = i;
        this.type = t;
        this.name = "Repair actuator (" + unit.getEntity().getLocationName(loc) + ")";
        this.time = 120;
        this.difficulty = 0;
    }
    
    @Override
    public String checkFixable() {
        if(unit.isLocationDestroyed(loc)) {
            return unit.getEntity().getLocationName(loc) + " is destroyed.";
        }
        return super.checkFixable();
    }
    
    public int getLoc() {
        return loc;
    }
    
    public int getType() {
        return type;
    }
    
    @Override
    public void fix() {
        unit.repairSystem(CriticalSlot.TYPE_SYSTEM, type, loc);
    }

    @Override
    public WorkItem replace() {
        return new MekActuatorReplacement(unit, loc, type);
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekActuatorRepair
                && ((MekActuatorRepair)task).getUnitId() == this.getUnitId()
                && ((MekActuatorRepair)task).getLoc() == this.getLoc()
                && ((MekActuatorRepair)task).getType() == this.getType());
    }

}
