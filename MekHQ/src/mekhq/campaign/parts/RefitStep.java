/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import mekhq.campaign.parts.enums.RefitClass;
import mekhq.campaign.parts.enums.RefitStepType;
import mekhq.campaign.unit.Unit;

/**
 * Holds data on one step of a refit process. It calculates as much as possible from the ingrediants
 * given to it, but other things will need to be calculated outside of here. All values are based on
 * Campaign Operations.
 */
public class RefitStep {
    private Part part;
    private int oldLoc;
    private int newLoc;
    private String oldLocName;
    private String newLocName;
    private RefitStepType type;
    private String notes;
    private RefitClass rClass;
    private int baseTime;

    public RefitStep() {
        baseTime = 0;
        rClass = RefitClass.NO_CHANGE;
    }

    RefitStep(Part part, int newLoc, Unit oldUnit) {
        this();
        oldLoc = part.getLocation();
        oldLocName = part.getLocationName();
        this.newLoc = newLoc;
        newLocName = oldUnit.getEntity().getLocationName(newLoc);

        if (newLoc == oldLoc) {
            type = RefitStepType.LEAVE;
            return;
        }

        if (part instanceof Armor) {
            rClass = RefitClass.CLASS_A;
            if (oldLoc == -1) {
                type = RefitStepType.ADD_ARMOR;
            } else if (newLoc == -1) {
                type = RefitStepType.REMOVE_ARMOR;
            } else {
                throw new IllegalArgumentException("Moving armor between locations directly is not supported.");
            }
        
            baseTime = ((Armor) part).getBaseTimeFor(oldUnit.getEntity()) * part.getQuantity();
        }
    }


    public Part getPart() {
        return part;
    }

    public int getQuantity() {
        return part.getQuantity();
    }

    public int getOldLoc() {
        return oldLoc;
    }

    public int getNewLoc() {
        return newLoc;
    }

    public String getOldLocName() {
        return oldLocName;
    }

    public String getNewLocName() {
        return newLocName;
    }

    public RefitStepType getType() {
        return type;
    }

    public String getNotes() {
        return notes;
    }

    public RefitClass getRefitClass() {
        return rClass;
    }

    public void setRefitClass(RefitClass rClass) {
        this.rClass = rClass;
    }

    public void setRefitClassToHarder(RefitClass rClass) {
        this.rClass = this.rClass.keepHardest(rClass);
    }

    public int getBaseTime() {
        return baseTime;
    }



}