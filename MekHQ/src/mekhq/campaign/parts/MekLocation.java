/*
 * Location.java
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

import megamek.common.Mech;
import mekhq.campaign.work.LocationReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekLocation extends Part {

    protected int loc;
    protected float tonnage;
    protected boolean endo;
    protected boolean tsm;
    
    public MekLocation(boolean salvage, int i, float ton, boolean e, boolean t) {
        super(salvage);
        this.loc = i;
        this.tonnage = ton;
        this.endo = e;
        this.tsm = t;
        //TODO: need to account for internal structure and myomer types
        //crap, no static report for location names?
        this.name = "Mech Location";
        switch(i) {
            case(Mech.LOC_HEAD):
                this.name = "Mech Head";
                break;
            case(Mech.LOC_CT):
                this.name = "Mech Center Torso";
                break;
            case(Mech.LOC_LT):
                this.name = "Mech Left Torso";
                break;
            case(Mech.LOC_RT):
                this.name = "Mech Right Torso";
                break;
            case(Mech.LOC_LARM):
                this.name = "Mech Left Arm";
                break;
            case(Mech.LOC_RARM):
                this.name = "Mech Right Arm";
                break;
            case(Mech.LOC_LLEG):
                this.name = "Mech Left Leg";
                break;
            case(Mech.LOC_RLEG):
                this.name = "Mech Right Leg";
                break;
        }
        if(endo) {
            this.name += " (Endosteel)";
        }
        if(tsm) {
            this.name += " (TSM)";
        }
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof LocationReplacement 
                && ((LocationReplacement)task).getUnit().getEntity() instanceof Mech
                && ((LocationReplacement)task).getLoc() == loc
                && ((LocationReplacement)task).getUnit().getEntity().getWeight() == tonnage
                && ((LocationReplacement)task).getUnit().hasTSM() == tsm
                && ((LocationReplacement)task).getUnit().hasEndosteel() == endo;
    }

}
