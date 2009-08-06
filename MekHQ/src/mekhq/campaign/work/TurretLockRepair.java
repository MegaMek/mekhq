/*
 * TurretLockRepair.java
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

import megamek.common.IArmorState;
import megamek.common.Tank;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class TurretLockRepair extends RepairItem {

    public TurretLockRepair(Unit unit) {
        super(unit, 1);
        this.name = "Unlock turret";
        this.time = 90;
        this.difficulty = -1;
        
    }
    
    @Override
    public WorkItem replace() {
        removeSalvage();
        unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, Tank.LOC_TURRET);
        return new TurretReplacement(unit, Tank.LOC_TURRET);
    }

    @Override
    public void fix() {
        if(unit.getEntity() instanceof Tank) {
            //TODO: no method to unlock turrets in Tank
        }
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof TurretLockRepair
                && ((TurretLockRepair)task).getUnitId() == this.getUnitId());
    }

}
