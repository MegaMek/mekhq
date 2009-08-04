/*
 * LocationSalvage.java
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
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LocationSalvage extends SalvageItem {

    protected int loc;
    
    public LocationSalvage(Unit unit, int i) {
        super(unit);
        this.name = "Salvage " + unit.getEntity().getLocationName(i);
        this.time = 240;
        this.difficulty = 3;
        this.loc = i;
    }
    
    @Override
    public void fix() {
        super.fix();
        unit.getEntity().setInternal(0, loc);
    }
    
    public int getLoc() {
        return loc;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof LocationSalvage
                && ((LocationSalvage)task).getUnitId() == this.getUnitId()
                && ((LocationSalvage)task).getLoc() == this.getLoc());
    }

    @Override
    public ReplacementItem getReplacement() {
        return new LocationReplacement(unit, loc);
    }

    @Override
    public Part getPart() {
        return new MekLocation(true, loc, unit.getEntity().getWeight(), unit.hasEndosteel(), unit.hasTSM());
    }

}
