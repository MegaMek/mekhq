/*
 * VeeStabiliserRepair.java
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

import megamek.common.Tank;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class VeeStabiliserRepair extends RepairItem {

    private int loc;
    
    public VeeStabiliserRepair(Unit unit, int i) {
        super(unit, 1);
        this.loc = i;
        this.name = "Repair stabilizer (" + unit.getEntity().getLocationName(loc) + ")";
        this.time = 60;
        this.difficulty = 1;
    }
    
    @Override
    public WorkItem replace() {
        removeSalvage();
        return new VeeStabiliserReplacement(unit, loc);
    }

    @Override
    public void fix() {
        if(unit.getEntity() instanceof Tank) {
            //TODO: no method in Tank to remove stabilizer hit
        }
    }
    
    public int getLoc() {
        return loc;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof VeeStabiliserRepair
                && ((VeeStabiliserRepair)task).getUnitId() == this.getUnitId()
                && ((VeeStabiliserRepair)task).getLoc() == this.getLoc());
    }

}
