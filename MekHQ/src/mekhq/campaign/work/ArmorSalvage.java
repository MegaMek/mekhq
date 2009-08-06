/*
 * ArmorSalvage.java
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
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ArmorSalvage extends SalvageItem {

    protected int loc;
    protected int amount;
    protected int type;
    protected boolean rear;
    
    public ArmorSalvage(Unit unit, int l, int t, boolean r) {
        super(unit);
        this.loc = l;
        this.type = t;
        this.rear = r;
        this.amount = unit.getEntity().getArmor(loc, rear);
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
        String locName = unit.getEntity().getLocationName(loc);
        if(rear) {
            locName += " Rear";
        }
        this.name = "Salvage " + EquipmentType.getArmorTypeName(type) + " Armor (" + locName + ", " + amount + ")";
    }

    @Override
    public ReplacementItem getReplacement() {
        return new ArmorReplacement(unit, loc, type, rear);
    }

    @Override
    public Part getPart() {
        return new Armor(true, type, amount);
    }
    
    public int getLoc() {
        return loc;
    }

    public boolean isRear() {
        return rear;
    }
    
    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof ArmorSalvage
                && ((ArmorSalvage)task).getUnitId() == this.getUnitId()
                && ((ArmorSalvage)task).getLoc() == this.getLoc()
                && ((ArmorSalvage)task).isRear() == this.isRear());
    }
    
    @Override
    public void fix() {
        unit.getEntity().setArmor(0, loc, rear);
        //need to find old armor replacement and mutate it
        boolean taskFound = false;
        for(WorkItem task : unit.campaign.getAllTasksForUnit(unit.getId())) {
            if(task instanceof ArmorReplacement 
                    && ((ArmorReplacement)task).getLoc() == loc
                    && ((ArmorReplacement)task).isRear() == rear) {
                unit.campaign.mutateTask(task, getReplacement());
                taskFound = true;
            }
        }
        if(!taskFound) {
            unit.campaign.addWork(getReplacement());
        }
        boolean partFound = false;
        for(Part part : unit.campaign.getParts()) {
            if(part instanceof Armor && ((Armor)part).getType() == type) {
                Armor armor = (Armor)part;
                armor.setAmount(armor.getAmount() + amount);
                partFound = true;
            }
        }
        if(!partFound) {
            unit.campaign.addPart(getPart());
        }
    }
}
