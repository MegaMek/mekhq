/*
 * LocationSalvage.java
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
import megamek.common.IArmorState;
import megamek.common.Mech;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LocationSalvage extends SalvageItem {

    protected int loc;
    
    public LocationSalvage(Unit unit, int i) {
        super(unit);
        this.name = "Salvage " + unit.getEntity().getLocationName(i);
        this.time = 240;
        this.difficulty = 3;
        this.loc = i;
    }
    
    @Override
    public void fix() {
        super.fix();
        unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
    }
    
    public int getLoc() {
        return loc;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof LocationSalvage
                && ((LocationSalvage)task).getUnitId() == this.getUnitId()
                && ((LocationSalvage)task).getLoc() == this.getLoc());
    }

    @Override
    public ReplacementItem getReplacement() {
        return new LocationReplacement(unit, loc);
    }

    @Override
    public Part getPart() {
        return new MekLocation(true, loc, unit.getEntity().getWeight(), unit.hasEndosteel(), unit.hasTSM());
    }
    
    @Override
    public String checkFixable() {
         //cant salvage torsos until arms and legs are gone
        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_RT && unit.getEntity().isLocationBad(Mech.LOC_RARM)) {
            return "must salvage/scarp right arm first";
        }
        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_RT && unit.getEntity().isLocationBad(Mech.LOC_RLEG)) {
            return "must salvage/scarp right leg first";
        }
        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_LT && unit.getEntity().isLocationBad(Mech.LOC_LARM)) {
            return "must salvage/scarp left arm first";
        }
        if(unit.getEntity() instanceof Mech && loc == Mech.LOC_LT && unit.getEntity().isLocationBad(Mech.LOC_LLEG)) {
            return "must salvage/scarp left leg first";
        }  
        //you can only salvage a location that has nothing left on it
        for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }
            //certain other specific crits need to be left out (uggh, must be a better way to do this!)
            if(slot.getType() == CriticalSlot.TYPE_SYSTEM 
                    && (slot.getIndex() == Mech.SYSTEM_COCKPIT
                          || slot.getIndex() == Mech.ACTUATOR_HIP
                          || slot.getIndex() == Mech.ACTUATOR_SHOULDER)) {
                continue;
            }
            if (slot.isRepairable()) {
                return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
            } 
        }
        return super.checkFixable();
    }

}
