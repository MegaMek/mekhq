/*
 * InternalRepair.java
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
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class InternalRepair extends RepairItem {

    int loc;
    
    public InternalRepair(Unit unit, int i) {
        super(unit, 0);
        this.loc = i;
        this.name = "Repair internal structure (" + unit.getEntity().getLocationName(loc) + ")";
    }
        
        
    @Override
    public void fix() {
        unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
    }

    @Override
    public WorkItem replace() {
        removeSalvage();
        unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
        return new LocationReplacement(unit, loc);
    }
    
    public int getLoc() {
        return loc;
    }
    
}
