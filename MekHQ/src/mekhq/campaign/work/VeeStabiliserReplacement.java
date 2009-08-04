/*
 * VeeStabiliserReplacement.java
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
import mekhq.campaign.parts.VeeStabiliser;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
class VeeStabiliserReplacement extends ReplacementItem {

    private int loc;
    
    public VeeStabiliserReplacement(Unit unit, int i) {
        super(unit);
        this.loc = i;
        this.name =  " Replace stabilizer (" + unit.getEntity().getLocationName(loc) + ")";
        this.time = 60;
        this.difficulty = 0;
    }

    @Override
    public void fix() {
        //TODO: no method for setting the stabilizerhits to zero in Tank
    }
    
    public int getLoc() {
        return loc;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof VeeStabiliserReplacement
                && ((VeeStabiliserReplacement)task).getUnitId() == this.getUnitId()
                && ((VeeStabiliserReplacement)task).getLoc() == this.getLoc());
    }

    @Override
    public Part partNeeded() {
        return new VeeStabiliser(false);
    }

}
