/*
 * MekSensorReplacement.java
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
import megamek.common.Mech;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekSensorReplacement extends ReplacementItem {

    public MekSensorReplacement(Unit unit) {
        super(unit);
        this.name = "Replace sensors";
        this.time = 260;
        this.difficulty = 0;
    }
    
    @Override
    public void fix() {
        for(int i = 0; i < unit.getEntity().locations(); i++) {
            unit.getEntity().removeCriticals(i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS));
        }
    }
    
    @Override
    public String checkFixable() {
        for(int i = 0; i < unit.getEntity().locations(); i++) {
            if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, i) > 0
                    && unit.isLocationDestroyed(i)) {
                return unit.getEntity().getLocationName(i) + " is destroyed.";
            }
        }
        return super.checkFixable();
    }
}
