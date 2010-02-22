/*
 * EquipmentReplacement.java
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
import megamek.common.WeaponType;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EquipmentReplacement extends ReplacementItem {

    protected Mounted mounted;
    
    public EquipmentReplacement(Unit unit, Mounted m) {
        super(unit);
        this.mounted = m;
        this.name = "Replace " + m.getType().getName();
        this.time = 120;
        if(m.getType() instanceof WeaponType && unit.getEntity().getQuirks().booleanOption("mod_weapons")) {
            this.time = 60;
        }
        this.difficulty = 0;
    }
    
    @Override
    public String getDetails() {
        return unit.getEntity().getLocationName(mounted.getLocation()) + ", " + super.getDetails();
    }
    
    @Override
    public void fix() {
        super.fix();
        mounted.setHit(false);
        mounted.setDestroyed(false);
        mounted.setRepairable(true);
        unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, unit.getEntity().getEquipmentNum(mounted));
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
        return (task instanceof EquipmentReplacement
                && ((EquipmentReplacement)task).getUnitId() == this.getUnitId()
                && ((EquipmentReplacement)task).getUnit().getEntity().getEquipmentNum(((EquipmentReplacement)task).getMounted()) == unit.getEntity().getEquipmentNum(mounted));
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

    @Override
    public Part stratopsPartNeeded() {
        /*
        boolean salvage = false;
        int weight = (int) getUnit().getEntity().getWeight();
        int faction = getUnit().campaign.getFaction();
        EquipmentType equipmentType = mounted.getType();
        Entity entity = getUnit().getEntity();
        return new EquipmentPart(salvage, weight, faction, equipmentType, entity);
        */
        return new EquipmentPart(false, (int) getUnit().getEntity().getWeight(), getUnit().campaign.getFaction(), mounted.getType(), getUnit().getEntity());
    }

    @Override
    public SalvageItem getSalvage() {
        return new EquipmentSalvage(unit, mounted);
    }
}
