/*
 * Armor.java
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

package mekhq.campaign.parts;

import megamek.common.EquipmentType;
import mekhq.campaign.work.ArmorReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Armor extends Part {

    protected int type;
    protected int amount;
    
    public Armor(boolean salvage, int t, int points) {
        super(false);
        this.type = t;
        this.amount = points;
        this.name = EquipmentType.getArmorTypeName(type) + " Armor";
    }
    
    public int getType() {
        return type;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int a) {
        this.amount = a;
    }
    
    @Override
    public String getDesc() {
        return name + " (" + amount + ")";
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof ArmorReplacement 
                && ((ArmorReplacement)task).getUnit().getEntity().getArmorType() == type;
    }

}
