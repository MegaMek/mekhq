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
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ReloadItem extends UnitWorkItem {

    protected Mounted mounted;
    protected AmmoType atype;
    protected AmmoType orig_atype;
    protected boolean swap;
    
    public ReloadItem(Unit unit, Mounted m) {
        super(unit);
        this.swap = false;
        this.mounted = m;
        if(mounted.getType() instanceof AmmoType) {
            this.atype = (AmmoType)mounted.getType();
            this.orig_atype = (AmmoType)mounted.getType();
        }
        this.name = "Reload " + mounted.getDesc() + " with " + atype.getDesc();       
        //TODO: crap, time varies by skill level
        //TODO: also need to allow it to double if changing ammo type
        this.time = 15;
        if(unit.getEntity().isOmni()) {
            this.time = 8;
        }
        this.difficulty = TargetRoll.AUTOMATIC_SUCCESS;
    }
    
    @Override
    public String checkFixable() {
        //if this is not a swap and we are already topped off, then no need to waste time
        if(mounted.getShotsLeft() >= atype.getShots() && !swap) {
            return "the ammo bin is full.";
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
    
    public AmmoType getAmmoType() {
        return atype;
    }
    
    public void swapAmmo(AmmoType at) {
        if(at.getMunitionType() != orig_atype.getMunitionType()) {
            this.atype = at;
            mounted.setShotsLeft(at.getShots());
            if(!swap) {
                this.time *= 2;
            }
            this.swap = true;
            this.name = "Swap " + mounted.getDesc() + " with " + atype.getDesc();
        }
    }
    
    public double getTimeMultiplier() {
        double factor = 1.0;
        if(unit.getEntity().isOmni()) {
            factor *= 0.5;
        }
        if(swap) {
            factor *= 2.0;
        }
        return factor;
    }
    
    public boolean isFull() {
        return !swap && mounted.getShotsLeft() >= atype.getShots();
    }
    
    @Override
    public boolean isNeeded() {
        //if not swapping and topped off, then this reload is not needed
        return !isFull();
    }
    
    @Override
    public String getDesc() {
        if(isFull()) {
            return "(FULL) " + getName() + " " + getStats();
        }
        return super.getDesc(); 
    }
    
    public Mounted getMounted() {
        return mounted;
    }
}
