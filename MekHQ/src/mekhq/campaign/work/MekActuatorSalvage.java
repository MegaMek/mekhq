/*
 * MekActuatorSalvage.java
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
public class MekActuatorSalvage extends SalvageItem {

    protected int loc;
    protected int type;
    
    public MekActuatorSalvage(Unit unit, int i, int t) {
        super(unit);
        this.loc = i;
        this.type = t;
        this.name = "Salvage actuator (" + unit.getEntity().getLocationName(loc) + ")";
        this.time = 90;
        this.difficulty = -3;
    }

    @Override
    public ReplacementItem getReplacement() {
        return new MekActuatorReplacement(unit, loc, type);
    }
    
    public int getLoc() {
        return loc;
    }
    
    public int getType() {
        return type;
    }

    @Override
    public Part getPart() {
        return new MekActuator(true, unit.getEntity().getWeight(), type);
    }
    
    @Override
    public void fix() {
        super.fix();
        //FIXME: I dont think this will do the right thing
        unit.getEntity().addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, type));
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekActuatorSalvage
                && ((MekActuatorSalvage)task).getUnitId() == this.getUnitId()
                && ((MekActuatorSalvage)task).getLoc() == this.getLoc()
                && ((MekActuatorSalvage)task).getType() == this.getType());
    }

}
