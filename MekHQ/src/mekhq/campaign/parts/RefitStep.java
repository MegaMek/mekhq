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

import megamek.common.Engine;
import mekhq.campaign.parts.enums.RefitClass;
import mekhq.campaign.parts.enums.RefitStepType;
import mekhq.campaign.unit.Unit;

/**
 * Holds data on one step of a refit process. It calculates as much as possible from the ingrediants
 * given to it, but other things will need to be calculated outside of here. All values are based on
 * Campaign Operations.
 */
public class RefitStep {
    // region Instance Variables
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

    // region Initialization
    private RefitStep() {
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

        // region Armor

        if ((oldPart instanceof Armor && newPart instanceof Armor)) {
            // Refit code should have found us armors from the same location
            Armor oldArmor = (Armor) oldPart;
            Armor newArmor = (Armor) newPart;
            if ((oldLoc != newLoc) || (oldArmor.isRearMounted() != newArmor.isRearMounted())) {
                throw new IllegalArgumentException(
                        "Moving armor between locations directly is not supported. " + oldUnit);
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
                    return;
                }
            } else {
                // Armor types differ, remove old and add new
                type = RefitStepType.CHANGE_ARMOR_TYPE;
                returnsPart = oldArmor.clone();
                neededPart = newArmor.clone();

                baseTime = oldArmor.getBaseTimeFor(oldUnit.getEntity()) * oldArmor.getAmount();
                baseTime += newArmor.getBaseTimeFor(oldUnit.getEntity()) * newArmor.getAmount();
                return;
            }
        
        } else if (oldPart instanceof Armor) {
            refitClass = RefitClass.CLASS_A;
            type = RefitStepType.REMOVE_ARMOR;
            isFixedEquipmentChange = true;
            returnsPart = oldPart.clone();
            return;
        } else if (newPart instanceof Armor) {
            refitClass = RefitClass.CLASS_A;
            type = RefitStepType.ADD_ARMOR;
            isFixedEquipmentChange = true;
            neededPart = newPart.clone();
            return;
        
        // region Locations

        } else if (((oldPart instanceof MekLocation) || (oldPart instanceof MissingMekLocation))
                && (newPart instanceof MekLocation)) {
            boolean oldTsm;
            int oldStructure;
            if (oldPart instanceof MekLocation) {
                oldTsm = ((MekLocation) oldPart).isTsm();
                oldStructure = ((MekLocation) oldPart).getStructureType();
            } else {
                oldTsm = ((MissingMekLocation) oldPart).isTsm();
                oldStructure = ((MissingMekLocation) oldPart).getStructureType();
            }

            MekLocation newMekLocation = (MekLocation) newPart;

            if (oldTsm == newMekLocation.isTsm() && oldStructure == newMekLocation.getStructureType()) {
                refitClass = RefitClass.NO_CHANGE;
                type = RefitStepType.LEAVE;
                return;
            } else {
                refitClass = RefitClass.CLASS_F;
                type = RefitStepType.CHANGE_STRUCTURE_TYPE;
                baseTime = 0;
                if (oldTsm != newMekLocation.isTsm()) {
                    baseTime += 360;
                }
                if (oldStructure != newMekLocation.getStructureType()) {
                    baseTime += 360;
                }
                neededPart = newPart.clone();
                returnsPart = (oldPart instanceof MekLocation) ? oldPart.clone() : null; // No returning Missing Parts
                return;
            }
        

        } else if (((oldPart instanceof MissingRotor) || (oldPart instanceof MissingTurret))
                    && null != newPart) {
            // We'll just leave the broken parts on
            refitClass = RefitClass.NO_CHANGE;
            type = RefitStepType.LEAVE;
            baseTime = 0;
            return;

        } else if (((oldPart instanceof Turret) || (oldPart instanceof MissingTurret)) && null == newPart) {
            // FIXME: WeaverThree - Adding a turret is F, should removing it be? Unclear.
            refitClass = RefitClass.CLASS_F;
            type = RefitStepType.REMOVE_TURRET;
            isFixedEquipmentChange = true;
            returnsPart = oldPart instanceof Turret ? oldPart.clone() : null;
            baseTime = 160;
            return;
        } else if ((null == oldPart) && (newPart instanceof Turret)) {
            refitClass = RefitClass.CLASS_F;
            type = RefitStepType.ADD_TURRET;
            isFixedEquipmentChange = true;
            neededPart = newPart.clone();
            baseTime = 160;
            return;

        } else if ((oldPart instanceof TankLocation) && (newPart instanceof TankLocation)) {
            // There's nothing else you can change about a tank location
            refitClass = RefitClass.NO_CHANGE;
            type = RefitStepType.LEAVE;
            baseTime = 0;
            return;
        
        // region Core Equipment
        } else if (((oldPart instanceof EnginePart) || (oldPart instanceof MissingEnginePart))
                && (newPart instanceof EnginePart)) {
            
            boolean equal;

            if (oldPart instanceof EnginePart) {
                equal = oldPart.isSamePartType(newPart);
            } else {
                equal = ((MissingEnginePart) oldPart).isAcceptableReplacement(newPart, true);
            }

            if (equal) {
                refitClass = RefitClass.NO_CHANGE;
                type = RefitStepType.LEAVE;
                baseTime = 0;
                return;
            } else {
                refitClass = RefitClass.CLASS_E; // Refit code responsible for downgrading for kit
                type = RefitStepType.CHANGE;
                baseTime = 360;
                returnsPart = (oldPart instanceof EnginePart) ? oldPart.clone() : null;
                neededPart = newPart.clone();
                return;
            }


        } else if (((oldPart instanceof MekGyro) || (oldPart instanceof MissingMekGyro))
                && (newPart instanceof MekGyro)) {
            
            boolean equal;
            if (oldPart instanceof MekGyro) {
                equal = oldPart.isSamePartType(newPart);
            } else {
                equal = ((MissingMekGyro) oldPart).isAcceptableReplacement(newPart, true);
            }

            if (equal) {
                refitClass = RefitClass.NO_CHANGE;
                type = RefitStepType.LEAVE;
                baseTime = 0;
                return;
            } else {
                refitClass = RefitClass.CLASS_D;
                type = RefitStepType.CHANGE;
                baseTime = 360;
                returnsPart = (oldPart instanceof MekGyro) ? oldPart.clone() : null;
                neededPart = newPart.clone();
                return;
            }

      } else if (((oldPart instanceof MekGyro) || (oldPart instanceof MissingMekGyro))
                && (newPart instanceof MekGyro)) {
            
            boolean equal;
            if (oldPart instanceof MekGyro) {
                equal = oldPart.isSamePartType(newPart);
            } else {
                equal = ((MissingMekGyro) oldPart).isAcceptableReplacement(newPart, true);
            }

            if (equal) {
                refitClass = RefitClass.NO_CHANGE;
                type = RefitStepType.LEAVE;
                baseTime = 0;
                return;
            } else {
                refitClass = RefitClass.CLASS_D;
                type = RefitStepType.CHANGE;
                baseTime = 200;
                returnsPart = (oldPart instanceof MekGyro) ? oldPart.clone() : null;
                neededPart = newPart.clone();
                return;
            }


        } else if (((oldPart instanceof MekCockpit) || (oldPart instanceof MissingMekCockpit))
                && (newPart instanceof MekCockpit)) {
            
            boolean equal;
            if (oldPart instanceof MekCockpit) {
                equal = oldPart.isSamePartType(newPart);
            } else {
                equal = ((MissingMekCockpit) oldPart).isAcceptableReplacement(newPart, true);
            }

            if (equal) {
                refitClass = RefitClass.NO_CHANGE;
                type = RefitStepType.LEAVE;
                baseTime = 0;
                return;
            } else {
                refitClass = RefitClass.CLASS_D;
                type = RefitStepType.CHANGE;
                baseTime = 300; // FIXME: WeaverThree - From MissingMekCockpit - not in CamOps
                returnsPart = (oldPart instanceof MekCockpit) ? oldPart.clone() : null;
                neededPart = newPart.clone();
                return;
            }
        }


        // If we reach this point, something has gone wrong

        type = RefitStepType.ERROR;
        refitClass = RefitClass.PLEASE_REPAIR;
        baseTime = 0;
        
    }


    // region Getter/Setters

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

    public boolean isFixedEquipmentChange() {
        return isFixedEquipmentChange;
    }



}