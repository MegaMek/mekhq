/*
 * NewClass.java
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
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EquipmentRepair extends RepairItem {
    
    protected Mounted mounted;
    
    public EquipmentRepair(Unit unit, int h, Mounted m) {
        super(unit, h);
        this.mounted = m;
        this.name = "Repair " + m.getType().getName() + " (" + unit.getEntity().getLocationName(m.getLocation()) + ")";
        this.time = 100;
        this.difficulty = -3;
        if(hits == 2) {
            this.time = 150;
            this.difficulty = -2;
        } else if (hits == 3) {
            this.time = 200;
            this.difficulty = 0;
        } else if (hits >= 4) {
            this.time = 250;
            this.difficulty = 2;
        }
    }

    @Override
    public void fix() {
        mounted.setHit(false);
        mounted.setDestroyed(false);
        unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
    }
    
    @Override
    public WorkItem replace() {
        removeSalvage();
        mounted.setHit(true);
        mounted.setDestroyed(true);
        unit.destroySystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
        return new EquipmentReplacement(unit, mounted);
    }
    
    @Override
    public String checkFixable() {
        //only fixable if location is not destroyed
        //we have to cycle through all locations because some equipment is spreadable
        for(int loc = 0; loc < unit.getEntity().locations(); loc++) {
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                // ignore empty & system slots
                if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                    continue;
                }
                if (unit.getEntity().getEquipmentNum(mounted) == slot.getIndex()) {
                    if(unit.isLocationDestroyed(loc)) {
                        return unit.getEntity().getLocationName(loc) + " is destroyed.";
                    }
                }
            }
        }
        return super.checkFixable();
    }
    
    public Mounted getMounted() {
        return mounted;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof EquipmentRepair 
                && ((EquipmentRepair)task).getUnitId() == this.getUnitId()
                && ((EquipmentRepair)task).getUnit().getEntity().getEquipmentNum(((EquipmentRepair)task).getMounted()) == unit.getEntity().getEquipmentNum(mounted));
    }
    
    @Override
    public TargetRoll getAllMods() {
        TargetRoll target = super.getAllMods();
        if(mounted.getType().getTechLevel() == TechConstants.T_IS_EXPERIMENTAL
                || mounted.getType().getTechLevel() == TechConstants.T_CLAN_EXPERIMENTAL) {
            target.addModifier(2,"experimental");
        }
        return target;
    }
}
