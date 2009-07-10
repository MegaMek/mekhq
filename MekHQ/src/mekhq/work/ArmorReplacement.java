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

package mekhq.work;

import megamek.common.Entity;

/**
 *
 * @author Aaron
 */
public class ArmorReplacement extends WorkItem {

    private int loc;
    private int amount;
    private boolean rear;
    
    public ArmorReplacement(Entity entity, int loc, int amount, boolean rear) {
        super(entity);
        this.loc = loc;
        this.amount = amount;
        this.rear = rear;
        this.difficulty = -2;
        this.time = 5 * amount; 
        this.name = entity.getLocationName(loc) + " armor replacement " + "(" + amount + " points)";
    } 
    
    @Override
    public void fix() {
        entity.setArmor(entity.getOArmor(loc, rear), loc, rear);
    }

    
}
