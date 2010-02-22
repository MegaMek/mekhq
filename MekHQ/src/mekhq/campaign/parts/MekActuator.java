/*
 * MekActuator.java
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

import megamek.common.BipedMech;
import megamek.common.Mech;
import mekhq.campaign.work.MekActuatorReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekActuator extends Part {
    
    protected int type;

    public int getType() {
        return type;
    }
    
    public MekActuator(boolean salvage, int tonnage, int type) {
        super(salvage, tonnage);
        this.type = type;
        Mech m = new BipedMech();
        this.name = m.getSystemName(type) + " Actuator (" + tonnage + " tons)" ;
        computeCost();
    }

    private void computeCost () {
        int unitCost = 0;
        switch (getType()) {
            case (Mech.ACTUATOR_UPPER_ARM) : {
                unitCost = 100;
                break;
            }
            case (Mech.ACTUATOR_LOWER_ARM) : {
                unitCost = 50;
                break;
            }
            case (Mech.ACTUATOR_HAND) : {
                unitCost = 80;
                break;
            }
            case (Mech.ACTUATOR_UPPER_LEG) : {
                unitCost = 150;
                break;
            }
            case (Mech.ACTUATOR_LOWER_LEG) : {
                unitCost = 80;
                break;
            }
            case (Mech.ACTUATOR_FOOT) : {
                unitCost = 120;
                break;
            }
            case (Mech.ACTUATOR_HIP) : {
                // not used
                unitCost = 0;
                break;
            }
            case (Mech.ACTUATOR_SHOULDER) : {
                // not used
                unitCost = 0;
                break;
            }
        }
        this.cost = getTonnage() * unitCost;
    }

    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return task instanceof MekActuatorReplacement 
                && tonnage == ((MekActuatorReplacement)task).getUnit().getEntity().getWeight()
                && type == ((MekActuatorReplacement)task).getType();
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MekActuator
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getType() == ((MekActuator)part).getType()
                && getTonnage() == ((MekActuator)part).getTonnage();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_ACTUATOR;
    }

    @Override
    public String getSaveString () {
        return getName() + ";" + getTonnage() + ";" + getType();
    }
}
