/*
 * MekGyroSalvage.java
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
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekGyroSalvage extends SalvageItem {

    public MekGyroSalvage(Unit unit) {
        super(unit);
        this.name = "Salvage " + ((Mech)unit.getEntity()).getSystemName(Mech.SYSTEM_GYRO);
        this.time = 200;
        this.difficulty = 0;
    }

    @Override
    public ReplacementItem getReplacement() {
        return new MekGyroReplacement(unit);
    }

    @Override
    public Part getPart() {
        return new MekGyro(true, unit.getEntity().getGyroType(), unit.getEntity().getWeight());
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekGyroSalvage
                && ((MekGyroSalvage)task).getUnitId() == this.getUnitId());
    }

    @Override
    public void removePart() {
        unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
    }

}
