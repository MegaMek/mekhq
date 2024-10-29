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
    private Part neededPart;
    private Part returnsPart;
    private int oldLoc;
    private int newLoc;
    private String oldLocName;
    private String newLocName;
    private String oldPartName;
    private String newPartName;
    private RefitStepType type;
    private String notes;
    private RefitClass refitClass;
    private int baseTime;
    private boolean isFixedEquipmentChange;

    public RefitStep() {
        baseTime = 0;
        refitClass = RefitClass.NO_CHANGE;
        neededPart = null;
        returnsPart = null;
    }

    RefitStep(Unit oldUnit, Part oldPart, Part newPart) throws IllegalArgumentException {
        this();

        if (null == oldPart && null == newPart) {
            throw new IllegalArgumentException("oldPart and newPart must not both be null");
        }

        // We don't actually keep the parts around or even any parts in some cases, so keep the
        // values required to report what's going on

        oldLoc = null == oldPart ? -1 : oldPart.getLocation();
        oldLocName = null == oldPart ? "" : oldPart.getLocationName();
        oldPartName = null == oldPart ? "" : oldPart.getName();
        newLoc = null == newPart ? -1 : newPart.getLocation();
        newLocName = null == newPart ? "" : newPart.getLocationName();
        newPartName = null == newPart ? "" : newPart.getName();

        if (oldPart instanceof Armor) {
            // Refit code should have found us armors from the same location
            Armor oldArmor = (Armor) oldPart;
            Armor newArmor = (Armor) newPart;
            if ((oldLoc != newLoc) || (oldArmor.isRearMounted() != newArmor.isRearMounted())) {
                throw new IllegalArgumentException("Moving armor between locations directly is not supported.");
            }
            
            // This covers every armor change except no change
            refitClass = RefitClass.CLASS_A;
            isFixedEquipmentChange = true;

            if (oldArmor.getType() == newArmor.getType()) {
                if(oldArmor.getAmount() == newArmor.getAmount()) {
                    refitClass = RefitClass.NO_CHANGE;
                    type = RefitStepType.LEAVE;
                    isFixedEquipmentChange = false;
                    return;
                } else {
                    int oldAmount = oldArmor.getAmount();
                    int newAmount = newArmor.getAmount();
                    Armor deltaArmor = oldArmor.clone();
                    int delta = 0;
                    if (oldAmount > newAmount) {
                        delta = oldAmount - newAmount;
                        deltaArmor.setAmount(delta);
                        type = RefitStepType.REMOVE_ARMOR;
                        returnsPart = deltaArmor;
                    } else {
                        delta = newAmount - oldAmount;
                        deltaArmor.setAmount(delta);
                        type = RefitStepType.ADD_ARMOR;
                        neededPart = deltaArmor;
                    }
                    baseTime = deltaArmor.getBaseTimeFor(oldUnit.getEntity()) * delta;
                }
            } else {
                // Armor types differ, remove old and add new
                type = RefitStepType.CHANGE_ARMOR_TYPE;
                returnsPart = oldArmor.clone();
                neededPart = newArmor.clone();

                baseTime = oldArmor.getBaseTimeFor(oldUnit.getEntity()) * oldArmor.getAmount();
                baseTime += newArmor.getBaseTimeFor(oldUnit.getEntity()) * newArmor.getAmount();
            }
            

        } else if (newLoc == oldLoc) {
            type = RefitStepType.LEAVE;
            isFixedEquipmentChange = false;
            return;
        }
    }


    public Part getNeededPart() {
        return neededPart;
    }

    public Part getReturnsPart() {
        return returnsPart;
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

    public String getOldPartName() {
        return oldPartName;
    }

    public String getNewPartName() {
        return newPartName;
    }

    public RefitStepType getType() {
        return type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public RefitClass getRefitClass() {
        return refitClass;
    }

    public void setRefitClass(RefitClass refitClass) {
        this.refitClass = refitClass;
    }

    public void setRefitClassToHarder(RefitClass refitClass) {
        this.refitClass = this.refitClass.keepHardest(refitClass);
    }

    public int getBaseTime() {
        return baseTime;
    }



}