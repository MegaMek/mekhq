/*
 * ActuatorReplacement.java
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
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekActuatorReplacement extends ReplacementItem {
    
    protected int loc;
    protected int type;
    
    public MekActuatorReplacement(Unit unit, int i, int t) {
        super(unit);
        this.loc = i;
        this.type = t;
        this.name = "Replace actuator (" + unit.getEntity().getLocationName(loc) + ")";
        this.time = 90;
        this.difficulty = -3;
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
        super.fix();
        unit.repairSystem(CriticalSlot.TYPE_SYSTEM, type, loc);
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekActuatorReplacement
                && ((MekActuatorReplacement)task).getUnitId() == this.getUnitId()
                && ((MekActuatorReplacement)task).getLoc() == this.getLoc()
                && ((MekActuatorReplacement)task).getType() == this.getType());
    }

    @Override
    public Part partNeeded() {
        return new MekActuator(false, unit.getEntity().getWeight(), type);
    }

    @Override
    public SalvageItem getSalvage() {
        return new MekActuatorSalvage(unit, loc, type);
    }
}
