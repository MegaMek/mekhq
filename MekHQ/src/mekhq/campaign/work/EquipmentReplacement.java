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
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class EquipmentReplacement extends ReplacementItem {

    protected Mounted mounted;
    
    public EquipmentReplacement(Unit unit, Mounted m) {
        super(unit);
        this.mounted = m;
        this.name = "Replace " + m.getType().getName() + " (" + unit.getEntity().getLocationName(m.getLocation()) + ")";
        this.time = 120;
        this.difficulty = 0;
    }
    
    @Override
    public void fix() {
        mounted.setHit(false);
        mounted.setDestroyed(false);
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
                    slot.setHit(false);
                    slot.setDestroyed(false);
                }
            }
        }
    }
}
