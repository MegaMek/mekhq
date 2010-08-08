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

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.Utilities;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ReloadItem extends UnitWorkItem {
	private static final long serialVersionUID = 4011208324466306626L;
	protected Mounted mounted;
    protected boolean swap;
    protected long munition;
    protected String ammoDesc;
    
    //TODO: I can put ammo type back in here, I just need a restore function
    
    public ReloadItem(Unit unit, Mounted m) {
        super(unit);
        this.swap = false;
        this.mounted = m;
        if(mounted.getType() instanceof AmmoType) {
            this.munition = ((AmmoType)mounted.getType()).getMunitionType();
            this.ammoDesc = ((AmmoType)mounted.getType()).getDesc();
        }
        this.name = "Reload " + mounted.getDesc();       
        //TODO: crap, time varies by skill level
        //TODO: also need to allow it to double if changing ammo type
        this.time = 15;
        if(unit.getEntity().isOmni()) {
            this.time = 8;
        }
        if(unit.getEntity().getQuirks().booleanOption("fast_reload")) {
            time = (int)Math.round(time / 2.0);
        }
        this.difficulty = TargetRoll.AUTOMATIC_SUCCESS;
    }
    
    @Override
    public String getDetails() {
        String desc = ammoDesc;
        if (getCost() > 0) {
            NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
            String text = numberFormat.format(getCost()) + " " + (getCost()!=0?"CBills":"CBill");
            desc += " (" + text + ")";
        }
        return desc;
    }
    
    @Override
    public String checkFixable() {
        //if this is not a swap and we are already topped off, then no need to waste time
        if(mounted.getShotsLeft() >= ((AmmoType)mounted.getType()).getShots() && !swap) {
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
        for(AmmoType atype : Utilities.getMunitionsFor(unit.getEntity(), (AmmoType)mounted.getType())) {
            if(atype.getMunitionType() == munition) {
                mounted.changeAmmoType(atype);
                break;
            }
        }
        mounted.setShotsLeft(((AmmoType)mounted.getType()).getShots());
    }
    
    public void swapAmmo(AmmoType at) {
        if(at.getMunitionType() != ((AmmoType)mounted.getType()).getMunitionType()) {
            this.munition = at.getMunitionType();
            if(!swap) {
                this.time *= 2;
            }
            this.swap = true;
            this.ammoDesc = at.getDesc();
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
        if(unit.getEntity().getQuirks().booleanOption("fast_reload")) {
            factor = factor / 2.0;
        }
        return factor;
    }
    
    public Mounted getMounted() {
        return mounted;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof ReloadItem
                && ((ReloadItem)task).getUnitId() == this.getUnitId()
                && ((ReloadItem)task).getUnit().getEntity().getEquipmentNum(((ReloadItem)task).getMounted()) == unit.getEntity().getEquipmentNum(mounted));
    }

    @Override
    protected String maxSkillReached() {
        //this should never happen because it is automatic success so failure should never be called
        return "";
    }

    public int getCost () {
        // TODO Ammo cost
        return 0;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<ammoDesc>"
				+ammoDesc
				+"</ammoDesc>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<munition>"
				+munition
				+"</munition>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<swap>"
				+swap
				+"</swap>");
		//TODO: Handle writing Mounted to XML
		writeToXmlEnd(pw1, indent, id);
	}
}
