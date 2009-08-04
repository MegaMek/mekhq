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

    public ArmorReplacement(Unit unit, int loc, int amount) {
        super(unit);
        this.loc = loc;
        this.amount = amount;
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
        this.name = "Replace armor (" + unit.getEntity().getLocationName(loc) + ", " + amount + ")";
    }

    @Override
    public void fix() {
        unit.getEntity().setArmor(unit.getEntity().getOArmor(loc, false), loc, false);
        unit.getEntity().setArmor(unit.getEntity().getOArmor(loc, true), loc, true);
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

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof ArmorReplacement 
                && ((ArmorReplacement)task).getUnitId() == this.getUnitId()
                && ((ArmorReplacement)task).getLoc() == this.getLoc());
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
    public boolean useUpPart() {
        if(!hasPart()) {
            return false;
        }
        Armor armor = (Armor)part;
        armor.setAmount(armor.getAmount() - amount);
        if(armor.getAmount() > 0) {
            return false;
        } else {
            this.part = null;
            return true;
        }
    }

    @Override
    public Part partNeeded() {
        return new Armor(false, unit.getEntity().getArmorType(), amount);
    }
}
