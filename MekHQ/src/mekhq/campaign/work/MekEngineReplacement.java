/*
 * MekEngineReplacement.java
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
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.MekEngine;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekEngineReplacement extends ReplacementItem {

    public MekEngineReplacement(Unit unit) {
        super(unit);
        this.name = "Replace " + unit.getEntity().getEngine().getEngineName() + " Engine";
        this.time = 360;
        this.difficulty = -1;
    }

    @Override
    public void fix() {
        super.fix();
        unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
    }
    
    @Override
    public String checkFixable() {
        for(int i = 0; i < unit.getEntity().locations(); i++) {
            if(unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i) > 0
                    && unit.isLocationDestroyed(i)) {
                return unit.getEntity().getLocationName(i) + " is destroyed.";
            }
        }
        return super.checkFixable();
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekEngineReplacement
                && ((MekEngineReplacement)task).getUnitId() == this.getUnitId());
    }

    @Override
    public TargetRoll getAllMods() {
        TargetRoll target = super.getAllMods();
        if(unit.getEntity().getEngine().getTechType() == TechConstants.T_IS_EXPERIMENTAL
                || unit.getEntity().getEngine().getTechType() == TechConstants.T_CLAN_EXPERIMENTAL) {
            target.addModifier(2,"experimental");
        }
        return target;
    }

    @Override
    public Part partNeeded() {
        return new MekEngine(false, unit.getEntity().getEngine());
    }

    @Override
    public SalvageItem getSalvage() {
        return new MekEngineSalvage(unit);
    }
    
}
