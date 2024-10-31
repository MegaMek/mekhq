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

import megamek.common.Mek;
import megamek.common.MiscType;
import mekhq.campaign.parts.enums.RefitClass;
import mekhq.campaign.parts.enums.RefitStepType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.unit.Unit;

/**
 * Holds data on one step of a refit process. It calculates as much as possible from the ingredients
 * given to it, but other things will need to be calculated outside of here. All values are based on
 * Campaign Operations... where possible.
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
    private int oldQuantity;
    private int newQuantity;
    private RefitStepType type;
    private String notes;
    private RefitClass refitClass;
    private boolean isFixedEquipmentChange;
    private int baseTime;

    // region Initialization
    /**
     * Creates a blank refit step. Might be useful for XML initialization.
     */
    private RefitStep() {
        baseTime = 0;
        refitClass = RefitClass.NO_CHANGE;
        type = RefitStepType.ERROR;
        neededPart = null;
        returnsPart = null;
        notes = "";
    }

    /**
     * Creates a RefitStep for a Refit operation.
     * @param oldUnit - the unit being refit FROM. Important to understand what kind of unit is involved.
     * @param oldPart - the part on the old unit
     * @param newPart - the part on the new unit
     * @throws IllegalArgumentException
     */
    RefitStep(Unit oldUnit, Part oldPart, Part newPart) throws IllegalArgumentException {
        this(oldUnit, oldPart, newPart, false);
    }

    /**
     * Creates a RefitStep for a Refit operation.
     * @param oldUnit - the unit being refit FROM. Important to understand what kind of unit is involved.
     * @param oldPart - the part on the old unit
     * @param newPart - the part on the new unit
     * @param untracked - is this an untracked part like engine heatsinks
     * @throws IllegalArgumentException
     */
    RefitStep(Unit oldUnit, Part oldPart, Part newPart, boolean untracked) throws IllegalArgumentException {
        this();

        if (null == oldPart && null == newPart) {
            throw new IllegalArgumentException("oldPart and newPart must not both be null");
        }

        // region Keeping Data

        // We don't actually keep the parts around or even any parts in some cases, so keep the
        // values required to report what's going on

        oldLoc = null == oldPart ? -1 : oldPart.getLocation();
        oldLocName = null == oldPart ? "" : oldPart.getLocationName();
        oldPartName = null == oldPart ? "" : oldPart.getName();
        newLoc = null == newPart ? -1 : newPart.getLocation();
        newLocName = null == newPart ? "" : newPart.getLocationName();
        newPartName = null == newPart ? "" : newPart.getName();

        if (null != oldPart) {
            if (oldPart instanceof Armor) {
                oldQuantity = ((Armor) oldPart).getAmount();
            } else {
                oldQuantity = oldPart.getQuantity();
            }
        } else {
            oldQuantity = 0;
        }

        if (null != newPart) {
            if (newPart instanceof Armor) {
                newQuantity = ((Armor) newPart).getAmount();
            } else {
                newQuantity = newPart.getQuantity();
            }
        } else {
            newQuantity = 0;
        }

        // region Untracked Items

        if (untracked) {
            if ((oldPart instanceof AeroHeatSink) && (newPart instanceof AeroHeatSink)) {
                AeroHeatSink oldAHS = (AeroHeatSink) oldPart;
                AeroHeatSink newAHS = (AeroHeatSink) oldPart;
                refitClass = RefitClass.CLASS_B; // Engine Heat Sinks - treating all untracked HS as this for now
                isFixedEquipmentChange = true;

                if (oldAHS.getType() == newAHS.getType()) {
                    if (oldAHS.getQuantity() == newAHS.getQuantity()) {
                        refitClass = RefitClass.NO_CHANGE;
                        type = RefitStepType.LEAVE;
                        isFixedEquipmentChange = false;
                        return;
                    } else {
                        int oldQuantity = oldAHS.getQuantity();
                        int newQuantity = newAHS.getQuantity();
                        AeroHeatSink deltaAHS = oldAHS.clone();
                        int delta = 0;
                        if (oldQuantity > newQuantity) {
                            delta = oldQuantity - newQuantity;
                            deltaAHS.setQuantity(delta);
                            type = RefitStepType.REMOVE_UNTRACKED_SINKS;
                            returnsPart = deltaAHS;
                        } else {
                            delta = newQuantity - oldQuantity;
                            deltaAHS.setQuantity(delta);
                            type = RefitStepType.ADD_UNTRACKED_SINKS;
                            neededPart = deltaAHS;
                        }
                        baseTime = delta * 20; // TODO: Class basetimes are off? - WeaverThree
                        return;
                    }
                } else {
                    // Changing HS Type
                    type = RefitStepType.CHANGE_UNTRACKED_SINKS;
                    returnsPart = oldAHS.clone();
                    neededPart = newAHS.clone();
                    baseTime = oldAHS.getQuantity() * 20;
                    baseTime += newAHS.getQuantity() * 20;
                    return;
                }
            } else if (tempIsHeatSink(oldPart) && tempIsHeatSink(newPart)) {
                EquipmentPart oldHS = (EquipmentPart) oldPart;
                EquipmentPart newHS = (EquipmentPart) newPart;
                refitClass = RefitClass.CLASS_B; // Engine Heat Sinks - treating all untracked HS as this for now
                isFixedEquipmentChange = true;

                if(oldHS.getType().equals(newHS.getType())) {
                    if(oldHS.getQuantity() == newHS.getQuantity()) {
                        refitClass = RefitClass.NO_CHANGE;
                        type = RefitStepType.LEAVE;
                        isFixedEquipmentChange = false;
                        return;
                    } else {
                        int oldQuantity = oldHS.getQuantity();
                        int newQuantity = newHS.getQuantity();
                        EquipmentPart deltaHS = oldHS.clone();
                        int delta = 0;
                        if (oldQuantity > newQuantity) {
                            delta = oldQuantity - newQuantity;
                            deltaHS.setQuantity(delta);
                            type = RefitStepType.REMOVE_UNTRACKED_SINKS;
                            returnsPart = deltaHS;
                        } else {
                            delta = newQuantity - oldQuantity;
                            deltaHS.setQuantity(delta);
                            type = RefitStepType.ADD_UNTRACKED_SINKS;
                            neededPart = deltaHS;
                        }
                        if (oldUnit.getEntity() instanceof Mek) {
                            baseTime = 90; // Meks treat engine sinks as "one location" for time
                        } else {
                            baseTime = 20 * delta; // All vehicles?
                        }
                    }
                } else {
                    // Changing HS Type
                    type = RefitStepType.CHANGE_UNTRACKED_SINKS;
                    returnsPart = oldHS.clone();
                    neededPart = newHS.clone();
                    if (oldUnit.getEntity() instanceof Mek) {
                        baseTime = 180; // One operation to remove, one operation to install?
                    } else {
                        baseTime = 20 * oldHS.getQuantity(); // All vehicles?
                        baseTime += 20 * newHS.getQuantity();
                    }
                    return;
                }
            }

            // If we reach this point for untracked, something has gone wrong
        
            type = RefitStepType.ERROR;
            refitClass = RefitClass.PLEASE_REPAIR;
            baseTime = 0;
            return;
        }

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
            // Capital scale armor is 10 points per point allocated
            int armorMultipler = oldUnit.getEntity().isCapitalScale() ? 10 : 1;

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
                        deltaArmor.setAmount(delta * armorMultipler);
                        type = RefitStepType.REMOVE_ARMOR;
                        returnsPart = deltaArmor;
                    } else {
                        delta = newAmount - oldAmount;
                        deltaArmor.setAmount(delta * armorMultipler);
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

                ((Armor) returnsPart).setAmount(((Armor) returnsPart).getAmount() * armorMultipler);
                ((Armor) neededPart).setAmount(((Armor) neededPart).getAmount() * armorMultipler);

                baseTime = oldArmor.getBaseTimeFor(oldUnit.getEntity()) * oldArmor.getAmount();
                baseTime += newArmor.getBaseTimeFor(oldUnit.getEntity()) * newArmor.getAmount();
                return;
            }
        
        } else if (oldPart instanceof Armor) {
            refitClass = RefitClass.CLASS_A;
            type = RefitStepType.REMOVE_ARMOR;
            isFixedEquipmentChange = true;
            returnsPart = oldPart.clone();
            baseTime = ((Armor) oldPart).getBaseTimeFor(oldUnit.getEntity()) * ((Armor) oldPart).getAmount();
            return;
        } else if (newPart instanceof Armor) {
            refitClass = RefitClass.CLASS_A;
            type = RefitStepType.ADD_ARMOR;
            isFixedEquipmentChange = true;
            neededPart = newPart.clone();
            baseTime = ((Armor) newPart).getBaseTimeFor(oldUnit.getEntity()) * ((Armor) newPart).getAmount();
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
            // FIXME: WeaverThree - Removing a Turret is changing the weight of the turret...
            refitClass = RefitClass.CLASS_D;
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



        //} else if () {



        }


        // If we reach this point, something has gone wrong

        type = RefitStepType.ERROR;
        refitClass = RefitClass.PLEASE_REPAIR;
        baseTime = 0;
        
    }

    /**
     * Determine if a Part is a heat sink because not all heat sinks are of class HeatSink right now.
     * I hope the need for this function goes away in the future.
     * @param part - the part to check
     * @return is this part a heat sink
     */
    public static boolean tempIsHeatSink(Part part) {
        if (part instanceof HeatSink) {
            return true;
        } else if ((part instanceof EquipmentPart)
                && (((EquipmentPart) part).getType().hasFlag(MiscType.F_LASER_HEAT_SINK)
                    || ((EquipmentPart) part).getType().hasFlag(MiscType.F_COMPACT_HEAT_SINK)
                    || ((EquipmentPart) part).getType().hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE))) { 
            return true;
        } else {
            return false;
        }
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

    public int getOldQuantity() {
        return oldQuantity;
    }

    public int getNewQuantity() {
        return newQuantity;
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