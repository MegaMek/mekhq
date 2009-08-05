/*
 * RotorRepair.java
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

import megamek.common.VTOL;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class RotorRepair extends RepairItem {
    
    public RotorRepair(Unit unit) {
        super(unit, 1);
        this.name = "Repair rotor damage";
        this.time = 120;
        this.difficulty = 2;
    }

    @Override
    public WorkItem replace() {
        unit.getEntity().setInternal(0, VTOL.LOC_ROTOR);
        return new RotorReplacement(unit, VTOL.LOC_ROTOR);
    }

    @Override
    public void fix() {
        if(unit.getEntity() instanceof VTOL) {
            unit.getEntity().setInternal(unit.getEntity().getInternal(VTOL.LOC_ROTOR)+1, VTOL.LOC_ROTOR);
        }
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof RotorRepair
                && ((RotorRepair)task).getUnitId() == this.getUnitId());
    }

}
