/*
 * SalvageItem.java
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
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class SalvageItem extends UnitWorkItem {
    
    //the id of a corresponding repair item that must be dealt with when this item is processed
    protected int repairId = NONE;
    
    public SalvageItem(Unit u) {
        super(u);
    }
    
    @Override
    public void fix() {
        unit.campaign.addPart(getPart());
        unit.campaign.addWork(getReplacement());
        RepairItem repair = (RepairItem)unit.campaign.getTask(repairId);
        if(null != repair) {
            //remove the repair item
            unit.campaign.removeTask(repair);
        }
    }
    
    public abstract ReplacementItem getReplacement();
    
    public abstract Part getPart();
    
    public int getRepairId() {
        return repairId;
    }
    
    public void setRepairId(int id) {
        this.repairId = id;
    }

}
