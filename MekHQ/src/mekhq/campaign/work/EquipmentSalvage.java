/*
 * EquipmentSalvage.java
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
import megamek.common.WeaponType;
import mekhq.campaign.Campaign;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EquipmentSalvage extends SalvageItem {

    protected Mounted mounted;
    
    public EquipmentSalvage(Unit unit, Mounted m) {
        super(unit);
        this.mounted = m;
        String loc = "";
        if(m.getLocation() > -1 && m.getLocation() < unit.getEntity().locations()) {
            loc = " (" + unit.getEntity().getLocationName(m.getLocation()) + ")";
        }
        this.name = "Salvage " + m.getType().getName() + loc;
        this.time = 120;
        if(m.getType() instanceof WeaponType && unit.getEntity().getQuirks().booleanOption("mod_weapons")) {
            this.time = 60;
        }
        this.difficulty = 0;
    }
    
    public Mounted getMounted() {
        return mounted;
    }
    
    @Override
    public void fix() {
        super.fix();
        mounted.setHit(true);
        mounted.setDestroyed(true);
        mounted.setRepairable(false);
        //I also need to clear the critical slot
        //I think the only way to do this is to loop through all crits and find any that are associated
        //with this equipment
        for(int loc = 0; loc < unit.getEntity().locations(); loc++) {
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                // ignore empty & system slots
                if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                    continue;
                }
                if (unit.getEntity().getEquipmentNum(mounted) == slot.getIndex()) {
                    slot.setHit(true);
                    slot.setDestroyed(true);
                    slot.setRepairable(false);
                }
            }
        }
    }
    
    @Override
    public Part getPart() {
        return new EquipmentPart(true, mounted.getType());
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof EquipmentSalvage
                && ((EquipmentSalvage)task).getUnitId() == this.getUnitId()
                && ((EquipmentSalvage)task).getUnit().getEntity().getEquipmentNum(((EquipmentSalvage)task).getMounted()) == unit.getEntity().getEquipmentNum(mounted));
    }

    @Override
    public ReplacementItem getReplacement() {
        return new EquipmentReplacement(unit, mounted);
    }

}
