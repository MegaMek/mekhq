/*
 * VeeInternalRepair.java
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
import megamek.common.VTOL;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class VeeInternalRepair extends InternalRepair {

    public VeeInternalRepair(Unit unit, int i) {
        super(unit, i);
        this.time = 60;
        this.difficulty = 0;
    }
    
    @Override
    public WorkItem replace() {
        //you can only replace turrets and rotors
        if(unit.getEntity() instanceof VTOL && loc == VTOL.LOC_ROTOR) {
            return new RotorReplacement(unit, loc);
        }
        if(unit.getEntity() instanceof Tank && loc == VTOL.LOC_TURRET) {
            return new TurretReplacement(unit, loc);
        }
        return(this);
    }
}
