/*
 * Rotor.java
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

package mekhq.campaign.parts;

import megamek.common.VTOL;
import mekhq.campaign.work.ReplacementItem;
import mekhq.campaign.work.RotorReplacement;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Rotor extends Part {
    
    public Rotor(boolean salvage, int tonnage) {
        super(salvage, tonnage);
        this.name = "VTOL Rotor" + " (" + getTonnage() + ")";
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof RotorReplacement 
                && ((RotorReplacement)task).getUnit().getEntity() instanceof VTOL
                && ((RotorReplacement)task).getUnit().getEntity().getWeight() == tonnage;
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof Rotor
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getTonnage() == ((Rotor)part).getTonnage();
    }
    
}
