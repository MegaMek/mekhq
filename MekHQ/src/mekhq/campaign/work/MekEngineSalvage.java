/*
 * MekEngineSalvage.java
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
import mekhq.campaign.parts.MekEngine;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekEngineSalvage extends SalvageItem {

    public MekEngineSalvage(Unit unit) {
        super(unit);
        this.name = "Salvage Engine";
        this.time = 360;
        this.difficulty = -1;
    }
    
    @Override
    public String getDetails() {
        return unit.getEntity().getEngine().getEngineName() + ", " + super.getDetails();
    }
    
    @Override
    public ReplacementItem getReplacement() {
        return new MekEngineReplacement(unit);
    }

    @Override
    public Part getPart() {
        return new MekEngine(true, (int) unit.getEntity().getWeight(), getUnit().campaign.getFaction(), 
                unit.getEntity().getEngine(), unit.campaign.getCampaignOptions().getClanPriceModifier());
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekEngineSalvage
                && ((MekEngineSalvage)task).getUnitId() == this.getUnitId());
    }

    @Override
    public void removePart() {
        unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE);
    }

}
