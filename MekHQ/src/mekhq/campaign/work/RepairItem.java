/*
 * RepairItem.java
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

import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class RepairItem extends UnitWorkItem {

    int hits;
    //the id of a corresponding salvage item that must be removed if this repair is mutated
    //into a replacement
    int salvageId = NONE;
    
    public RepairItem(Unit unit, int h) {
        super(unit);
        this.hits = h;
    }
    
    public abstract WorkItem replace();
    
    public int getSalvageId() {
        return salvageId;
    }
    
    public void setSalvageId(int id) {
        this.salvageId = id;
    }
    
}
