/*
 * MekGyroReplacement.java
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
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Taharqa
 */
public class MekGyroReplacement extends ReplacementItem {
    
    public MekGyroReplacement(Unit unit) {
        super(unit);
        this.name = "Replace " + ((Mech)unit.getEntity()).getSystemName(Mech.SYSTEM_GYRO);
        this.time = 200;
        this.difficulty = 0;
    }

    @Override
    public void fix() {
        super.fix();
        unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekGyroReplacement
                && ((MekGyroReplacement)task).getUnitId() == this.getUnitId());
    }

    @Override
    public Part stratopsPartNeeded() {
        return new MekGyro(false, (int) unit.getEntity().getWeight(), unit.getEntity().getGyroType(), unit.getEntity().getOriginalWalkMP());
    }

    @Override
    public SalvageItem getSalvage() {
        return new MekGyroSalvage(unit);
    }

}
