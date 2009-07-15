/*
 * ReloadItem.java
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

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import mekhq.campaign.SupportTeam;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ReloadItem extends WorkItem {

    protected Mounted mounted;
    protected AmmoType atype;
    protected boolean swap;
    
    public ReloadItem(Unit unit, Mounted m) {
        super(unit);
        this.swap = false;
        this.mounted = m;
        if(mounted.getType() instanceof AmmoType) {
            this.atype = (AmmoType)mounted.getType();
        }
        this.name = "Reload " + mounted.getDesc() + " with " + atype.getDesc();       
        //TODO: crap, time varies by skill level
        //TODO: also need to allow it to double if changing ammo type
        this.time = 15;
        this.difficulty = TargetRoll.AUTOMATIC_SUCCESS;
    }
    
    @Override
    public String checkFixable() {
        //if this is not a swap and we are already topped off, then no need to waste time
        if(mounted.getShotsLeft() >= atype.getShots() && !swap) {
            return "The ammo bin is full.";
        }
        if(unit.isLocationDestroyed(mounted.getLocation())) {
            return unit.getEntity().getLocationName(mounted.getLocation()) + " is destroyed.";
        }
        if(mounted.isHit() || mounted.isDestroyed()) {
            return "the ammo bin is damaged and must be replaced first.";
        }
        return super.checkFixable();
    }
    
    @Override
    public void fix() {
        mounted.changeAmmoType(atype);
        mounted.setShotsLeft(atype.getShots());
        if(swap) {
            this.name = "Swap " + mounted.getDesc() + " with " + atype.getDesc();
        } else {
            this.name = "Reload " + mounted.getDesc() + " with " + atype.getDesc();
        }
    }
    
    @Override
    public void assignTeam(SupportTeam  team) {
        switch(team.getRating()) {
           case SupportTeam.EXP_GREEN:
               this.time = 15;
               break;
           case SupportTeam.EXP_REGULAR:
               this.time = 10;
               break;
           case SupportTeam.EXP_VETERAN:
               this.time = 8;
               break;
           case SupportTeam.EXP_ELITE:
               this.time = 6;
               break;
       }
       if(swap) {
           time *= 2;
       }
       super.assignTeam(team);
    }
    
    public AmmoType getAmmoType() {
        return atype;
    }
    
    public void swapAmmo(AmmoType at) {
        this.atype = at;
        if(!swap) {
            this.time *= 2;
        }
        this.swap = true;
        this.name = "Swap " + mounted.getDesc() + " with " + atype.getDesc();     
    }
    
    @Override
    public void complete() {
        //reload items are never completed because the user may want to swap
        unassignTeam();
    }
}
