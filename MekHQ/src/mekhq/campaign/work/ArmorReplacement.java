/*
 * ArmorReplacement.java
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

import megamek.common.Aero;
import megamek.common.EquipmentType;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Aaron
 */
public class ArmorReplacement extends ReplacementItem {

    private int loc;
    private int amount;
    private int type;
    private boolean rear;

    public ArmorReplacement(Unit unit, int l, int t, boolean r) {
        super(unit);
        this.loc = l;
        this.type = t;
        this.rear = r;
        this.amount = unit.getEntity().getOArmor(loc, rear) - unit.getEntity().getArmor(loc, rear);        
        this.difficulty = -2;
        this.time = 5 * amount;
        if(unit.getEntity() instanceof Tank) {
            this.time = 3 * amount;
        } else if (unit.getEntity() instanceof Aero) {
            if(((Aero)unit.getEntity()).isCapitalScale()) {
                this.time = 120 * amount;
            } else {
                this.time = 15 * amount;
            }
        }
        this.name = "Replace " + EquipmentType.getArmorTypeName(type) + " Armor";
    }
    
    @Override
    public String getDetails() {
        String locName = unit.getEntity().getLocationName(loc);
        if(rear) {
            locName += " Rear";
        }
        return locName + ", " + amount + " points";
    }
    

    @Override
    public void fix() {
        if(null != part) {
            int points = Math.min(amount, ((Armor)part).getAmount());
            unit.getEntity().setArmor(unit.getEntity().getArmor(loc, rear) + points, loc, rear);
            boolean taskFound = false;
            //need to check the salvage task for mutation
            for(WorkItem task : unit.campaign.getAllTasksForUnit(unit.getId())) {
                if(task instanceof ArmorSalvage 
                    && ((ArmorSalvage)task).getLoc() == loc
                    && ((ArmorSalvage)task).isRear() == rear) {
                    unit.campaign.mutateTask(task, getSalvage());
                    taskFound = true;
                }
            }
            if(!taskFound) {
                unit.campaign.addWork(getSalvage());
            }
        }
        useUpPart();
    }

    @Override
    public void complete() {
        if(unit.getEntity().getArmor(loc, rear) == unit.getEntity().getOArmor(loc, rear)) {
            setCompleted(true);
        } else {
            //we did not fully repair the armor, probably because of supply shortage
            this.amount = unit.getEntity().getOArmor(loc, rear) - unit.getEntity().getArmor(loc, rear);  
            this.time = 5 * amount;
            if(unit.getEntity() instanceof Tank) {
                this.time = 3 * amount;
            } else if (unit.getEntity() instanceof Aero) {
                if(((Aero)unit.getEntity()).isCapitalScale()) {
                    this.time = 120 * amount;
                } else {
                    this.time = 15 * amount;
                }
            }
        }
        unit.campaign.assignParts();
    }
    
    @Override
    public String checkFixable() {
        if (unit.isLocationDestroyed(loc)) {
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
    
    public boolean isRear() {
        return rear;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof ArmorReplacement 
                && ((ArmorReplacement)task).getUnitId() == this.getUnitId()
                && ((ArmorReplacement)task).getLoc() == this.getLoc()
                && ((ArmorReplacement)task).getType() == this.getType()
                && ((ArmorReplacement)task).isRear() == this.isRear());
    }
    
    @Override
    public TargetRoll getAllMods() {
        TargetRoll target = super.getAllMods();
        if(unit.getEntity().getArmorTechLevel() == TechConstants.T_IS_EXPERIMENTAL
                || unit.getEntity().getArmorTechLevel() == TechConstants.T_CLAN_EXPERIMENTAL) {
            target.addModifier(2,"experimental");
        }
        return target;
    }
    
    @Override
    public void useUpPart() {
        if(hasPart()) {
            Armor armor = (Armor)part;
            armor.setAmount(armor.getAmount() - amount);
            if(armor.getAmount() < 1) {
                super.useUpPart();
            }
        }
    }

    @Override
    public Part partNeeded() {
        return new Armor(false, unit.getEntity().getArmorType(), amount);
    }

    @Override
    public SalvageItem getSalvage() {
        return new ArmorSalvage(unit, loc, type, rear);
    }
}
