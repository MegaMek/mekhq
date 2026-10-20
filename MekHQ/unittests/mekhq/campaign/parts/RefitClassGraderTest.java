/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Each rule of the Campaign Operations customization table (pp. 211-212), graded one change at a time (issues #9595
 * and #3666).
 */
class RefitClassGraderTest {
    private final RefitClassGrader grader = new RefitClassGrader();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void nothingGradedIsNoChange() {
        assertEquals(Refit.NO_CHANGE, grader.getRefitClass());
        assertTrue(grader.describeSteps().isEmpty());
    }

    @Test
    void armorAndRemovalAreClassA() {
        grader.gradeArmorChange();
        grader.gradeRemovedComponents(false);

        assertEquals(Refit.CLASS_A, grader.getRefitClass());
    }

    @Test
    void addingWhereSomethingWasRemovedIsClassB() {
        // A Large Laser in the right arm the AC/5 came out of
        grader.gradeAddedComponent("Large Laser", "Right Arm", true, false);

        assertEquals(Refit.CLASS_B, grader.getRefitClass());
    }

    @Test
    void addingWhereNothingWasRemovedIsClassC() {
        grader.gradeAddedComponent("Heat Sink", "Left Leg", false, false);

        assertEquals(Refit.CLASS_C, grader.getRefitClass());
    }

    @Test
    void addingSomethingThatSpansLocationsIsClassD() {
        grader.gradeAddedComponent("Null Signature System", "Center Torso", false, true);

        assertEquals(Refit.CLASS_D, grader.getRefitClass());
    }

    @Test
    void turningAWeaponInPlaceIsClassB() {
        grader.gradeMovedComponent("Flamer", "Center Torso", "Center Torso", true);

        assertEquals(Refit.CLASS_B, grader.getRefitClass());
    }

    @Test
    void movingToAnotherLocationIsClassC() {
        grader.gradeMovedComponent("Jump Jet", "Left Leg", "Left Torso", false);

        assertEquals(Refit.CLASS_C, grader.getRefitClass());
    }

    @Test
    void engineHeatSinksAreClassB() {
        grader.gradeEngineHeatSinkChange();

        assertEquals(Refit.CLASS_B, grader.getRefitClass());
    }

    @Test
    void caseGyroAndTurretWeightAreClassD() {
        RefitClassGrader caseGrader = new RefitClassGrader();
        caseGrader.gradeCaseAdded("Left Torso", false);
        RefitClassGrader gyroGrader = new RefitClassGrader();
        gyroGrader.gradeGyroChange();
        RefitClassGrader turretGrader = new RefitClassGrader();
        turretGrader.gradeTurret("Turret", true);

        assertEquals(Refit.CLASS_D, caseGrader.getRefitClass());
        assertEquals(Refit.CLASS_D, gyroGrader.getRefitClass());
        assertEquals(Refit.CLASS_D, turretGrader.getRefitClass());
    }

    @Test
    void caseInAnOmniPodIsAPodSwap() {
        grader.gradeCaseAdded("Left Torso", true);

        assertEquals(Refit.CLASS_OMNI, grader.getRefitClass());
    }

    @Test
    void engineIsClassEOrClassDWithAKit() {
        RefitClassGrader customGrader = new RefitClassGrader();
        customGrader.gradeEngineChange(false);
        RefitClassGrader kitGrader = new RefitClassGrader();
        kitGrader.gradeEngineChange(true);

        assertEquals(Refit.CLASS_E, customGrader.getRefitClass());
        assertEquals(Refit.CLASS_D, kitGrader.getRefitClass());
    }

    @Test
    void cockpitIsClassE() {
        grader.gradeCockpitChange();

        assertEquals(Refit.CLASS_E, grader.getRefitClass());
    }

    @Test
    void newTurretStructureAndFixedOmniPartsAreClassF() {
        RefitClassGrader turretGrader = new RefitClassGrader();
        turretGrader.gradeTurret("Turret", false);
        RefitClassGrader structureGrader = new RefitClassGrader();
        structureGrader.gradeStructureChange();
        RefitClassGrader omniGrader = new RefitClassGrader();
        omniGrader.gradeFixedOmniChange();

        assertEquals(Refit.CLASS_F, turretGrader.getRefitClass());
        assertEquals(Refit.CLASS_F, structureGrader.getRefitClass());
        assertEquals(Refit.CLASS_F, omniGrader.getRefitClass());
    }

    @Test
    void theHighestClassWins() {
        // The Wolverine WVR-6M example: A, B and C work make a Class C refit
        grader.gradeArmorChange();
        grader.gradeAddedComponent("Large Laser", "Right Arm", true, false);
        grader.gradeMovedComponent("Jump Jet", "Left Leg", "Left Torso", false);

        assertEquals(Refit.CLASS_C, grader.getRefitClass());
        assertEquals(3, grader.getSteps().size());
    }

    @Test
    void reasonsAreListedBiggestFirstAndOnce() {
        grader.gradeArmorChange();
        grader.gradeArmorChange();
        grader.gradeMovedComponent("Jump Jet", "Left Leg", "Left Torso", false);

        List<String> reasons = grader.describeSteps();

        assertEquals(2, reasons.size(), "Armor changed in two places is one reason");
        assertTrue(reasons.getFirst().contains("Moving Jump Jet from the Left Leg to the Left Torso"), reasons.getFirst());
        assertTrue(reasons.getFirst().startsWith(Refit.getRefitClassName(Refit.CLASS_C)), reasons.getFirst());
    }

    @Test
    void nullSignatureSystemSpansLocationsButArmorDoesNot() {
        EquipmentType nullSignature = EquipmentType.get("Mek Null Signature System");
        EquipmentType ferroFibrous = EquipmentType.get(EquipmentType.getArmorTypeName(
              EquipmentType.T_ARMOR_FERRO_FIBROUS, false));
        EquipmentType mediumLaser = EquipmentType.get("ISMediumLaser");

        assertTrue(RefitClassGrader.spansLocations(nullSignature, null));
        assertFalse(RefitClassGrader.spansLocations(ferroFibrous, null));
        assertFalse(RefitClassGrader.spansLocations(mediumLaser, null));
    }

    @Test
    void addingTripleStrengthMyomerIsClassF() {
        Entity oldEntity = entityWithMisc();
        Entity newEntity = entityWithMisc(EquipmentType.get(EquipmentTypeLookup.TSM));

        grader.gradeWholeUnitChanges(oldEntity, newEntity);

        assertEquals(Refit.CLASS_F, grader.getRefitClass());
    }

    @Test
    void addingFullHeadEjectionIsClassE() {
        Mek oldMek = mock(Mek.class);
        when(oldMek.getMisc()).thenReturn(List.of());
        Mek newMek = mock(Mek.class);
        when(newMek.getMisc()).thenReturn(List.of());
        when(newMek.hasFullHeadEject()).thenReturn(true);

        grader.gradeWholeUnitChanges(oldMek, newMek);

        assertEquals(Refit.CLASS_E, grader.getRefitClass());
    }

    @Test
    void addingAPowerAmplifierIsClassC() {
        Entity oldEntity = entityWithMisc();
        Entity newEntity = entityWithMisc();
        when(newEntity.getPowerAmplifierWeight()).thenReturn(1.0);

        grader.gradeWholeUnitChanges(oldEntity, newEntity);

        assertEquals(Refit.CLASS_C, grader.getRefitClass());
    }

    @Test
    void anUnchangedUnitHasNoWholeUnitWork() {
        grader.gradeWholeUnitChanges(entityWithMisc(), entityWithMisc());

        assertEquals(Refit.NO_CHANGE, grader.getRefitClass());
    }

    @Test
    void endoSteelInTheSamePlacesIsNoChange() {
        // Two Endo Steel slots in the left torso before and after, wherever they sit in it, is a pod swap
        Entity oldEntity = entityWithEndoSteel(0, 0, 2, 2);
        Entity newEntity = entityWithEndoSteel(0, 0, 2, 2);

        assertFalse(RefitClassGrader.haveWeightlessSlotsChanged(oldEntity, newEntity));
        assertTrue(RefitClassGrader.getLocationsGainingWeightlessSlots(oldEntity, newEntity).isEmpty());
    }

    @Test
    void endoSteelGoingFromTwoToOneIsAChange() {
        Entity oldEntity = entityWithEndoSteel(0, 0, 2, 2);
        Entity newEntity = entityWithEndoSteel(0, 0, 2, 1);

        assertTrue(RefitClassGrader.haveWeightlessSlotsChanged(oldEntity, newEntity));
        assertTrue(RefitClassGrader.getLocationsGainingWeightlessSlots(oldEntity, newEntity).isEmpty(),
              "No location gains slots, so there is no move time");
    }

    @Test
    void slotsMovedToTheOtherTorsoChargeThatLocation() {
        // The Balius A carries three Ferro-Fibrous slots in one side torso; the Prime carries them in the other
        Entity oldEntity = entityWithEndoSteel(0, 0, 0, 3);
        Entity newEntity = entityWithEndoSteel(0, 0, 3, 0);

        assertTrue(RefitClassGrader.haveWeightlessSlotsChanged(oldEntity, newEntity));
        assertEquals(List.of(2), RefitClassGrader.getLocationsGainingWeightlessSlots(oldEntity, newEntity));
    }

    @Test
    void movedSlotsAreClassF() {
        grader.gradeWeightlessSlotsMoved("Right Torso");

        assertEquals(Refit.CLASS_F, grader.getRefitClass());
    }

    /**
     * @param endoSteelSlotsByLocation how many Endo Steel slots each location holds, by location number
     */
    private static Entity entityWithEndoSteel(int... endoSteelSlotsByLocation) {
        MiscType endoSteel = (MiscType) EquipmentType.get(EquipmentType.getStructureTypeName(
              EquipmentType.T_STRUCTURE_ENDO_STEEL, false));
        MiscMounted mounted = mock(MiscMounted.class);
        when(mounted.getType()).thenReturn(endoSteel);
        when(mounted.getTonnage()).thenReturn(0.0);
        CriticalSlot criticalSlot = mock(CriticalSlot.class);
        when(criticalSlot.getType()).thenReturn(CriticalSlot.TYPE_EQUIPMENT);
        doReturn(mounted).when(criticalSlot).getMount();

        Entity entity = mock(Entity.class);
        when(entity.locations()).thenReturn(endoSteelSlotsByLocation.length);
        for (int location = 0; location < endoSteelSlotsByLocation.length; location++) {
            when(entity.getNumberOfCriticalSlots(location)).thenReturn(endoSteelSlotsByLocation[location]);
            for (int slot = 0; slot < endoSteelSlotsByLocation[location]; slot++) {
                when(entity.getCritical(location, slot)).thenReturn(criticalSlot);
            }
        }
        return entity;
    }

    private static Entity entityWithMisc(EquipmentType... equipmentTypes) {
        Entity entity = mock(Entity.class);
        List<MiscMounted> misc = new ArrayList<>();
        for (EquipmentType equipmentType : equipmentTypes) {
            MiscMounted mounted = mock(MiscMounted.class);
            when(mounted.getType()).thenReturn((MiscType) equipmentType);
            misc.add(mounted);
        }
        when(entity.getMisc()).thenReturn(misc);
        return entity;
    }
}
