/*
 * LocationReplacement.java
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
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LocationReplacement extends ReplacementItem {

    int loc;
    
    public LocationReplacement(Unit unit, int i) {
        super(unit);
        this.name = "Replace " + unit.getEntity().getLocationName(i);
        this.time = 240;
        this.difficulty = 3;
        this.loc = i;
    }
    
    @Override
    public void fix() {
        unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
        //repair any hips or shoulders
        if(unit.getEntity() instanceof Mech) {
            unit.getEntity().removeCriticals(loc, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP));
            unit.getEntity().removeCriticals(loc, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER));
        }
    }
    
    public int getLoc() {
        return loc;
    }
    
    @Override
    public String checkFixable() {
        if(unit.getEntity() instanceof Mech) {
            //cant replace appendages when corresponding torso is gone
            if((loc == Mech.LOC_LARM || loc == Mech.LOC_LLEG) && unit.getEntity().isLocationBad(Mech.LOC_LT)) {
                return "must replace left torso first";
            } 
            else if((loc == Mech.LOC_RARM || loc == Mech.LOC_RLEG) && unit.getEntity().isLocationBad(Mech.LOC_RT)) {
                return "must replace right torso first";
            } 
        }
        return super.checkFixable();
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof LocationReplacement
                && ((LocationReplacement)task).getUnitId() == this.getUnitId()
                && ((LocationReplacement)task).getLoc() == this.getLoc());
    }

    @Override
    public Part partNeeded() {
        return new MekLocation(false, loc, unit.getEntity().getWeight(), unit.hasEndosteel(), unit.hasTSM());
    }

    
}
