/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.UUID;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingMekLocation;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

public class MissingPartTest {
    @Test
    public void reservePartDoesNothingWithoutTheRightPart() {
        Campaign mockCampaign = mockCampaign();
        LocalWarehouse warehouse = new LocalWarehouse();
        when(mockCampaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
        mekhq.campaign.ForceQuartermaster quartermaster = new mekhq.campaign.ForceQuartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        // Add a not-suitable parts to the warehouse
        Part leftArmForRefit = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        // Add a person to do the work
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        missingPart.setTech(person);

        // Find the replacement part for overnight work
        missingPart.reservePart();

        // Ensure we did not find a part
        assertFalse(missingPart.hasReplacementPart());
        assertNull(missingPart.getReplacementPart());
    }

    @Test
    public void reservePartDoesNothingWithoutATech() {
        Campaign mockCampaign = mockCampaign();
        LocalWarehouse warehouse = new LocalWarehouse();
        when(mockCampaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
        mekhq.campaign.ForceQuartermaster quartermaster = new mekhq.campaign.ForceQuartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        // Add a suitable parts to the warehouse
        Part leftArm = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        warehouse.addPart(leftArm);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        // Find the replacement part for overnight work, without anyone to do the work
        missingPart.reservePart();

        // Ensure we did not find a part
        assertFalse(missingPart.hasReplacementPart());
        assertNull(missingPart.getReplacementPart());
    }

    @Test
    public void reservePartFindsTheRightPart() {
        Campaign mockCampaign = mockCampaign();
        LocalWarehouse warehouse = new LocalWarehouse();
        when(mockCampaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
        mekhq.campaign.ForceQuartermaster quartermaster = new mekhq.campaign.ForceQuartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        // Add a suitable parts to the warehouse
        Part leftArm = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        warehouse.addPart(leftArm);

        // Add a not-suitable parts to the warehouse
        Part leftArmForRefit = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        // Add a person to do the work
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        missingPart.setTech(person);

        // Find the replacement part for overnight work
        missingPart.reservePart();

        // Ensure we found the right part
        assertTrue(missingPart.hasReplacementPart());
        assertEquals(leftArm, missingPart.getReplacementPart());
        assertFalse(leftArm.isSpare());
        assertTrue(leftArm.isReservedForReplacement());
        assertEquals(1, leftArm.getQuantity());
    }

    @Test
    public void reservePartTakesJustOne() {
        Campaign mockCampaign = mockCampaign();
        LocalWarehouse warehouse = new LocalWarehouse();
        when(mockCampaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
        mekhq.campaign.ForceQuartermaster quartermaster = new mekhq.campaign.ForceQuartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        // Add a few suitable parts to the warehouse
        MekLocation leftArm = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        int startingQuantity = 3;
        leftArm.setQuantity(startingQuantity);
        warehouse.addPart(leftArm);

        // Add a not-suitable parts to the warehouse
        MekLocation leftArmForRefit = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        // Add a person to do the work
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        missingPart.setTech(person);

        // Find the replacement part for overnight work
        missingPart.reservePart();

        // Ensure we found the right part
        assertTrue(missingPart.hasReplacementPart());
        Part replacement = missingPart.getReplacementPart();
        assertTrue(replacement.getId() > 0);
        assertNotEquals(leftArm.getId(), replacement.getId());
        assertTrue(replacement.isReservedForReplacement());
        assertInstanceOf(MekLocation.class, replacement);
        assertTrue(missingPart.isAcceptableReplacement(replacement, false));

        // Ensure the original part is unchanged
        assertTrue(leftArm.isSpare());
        assertFalse(leftArm.isReservedForReplacement());
        assertEquals(startingQuantity - 1, leftArm.getQuantity());
    }

    @Test
    public void cancelReservationReturnsThePart() {
        Campaign mockCampaign = mockCampaign();
        LocalWarehouse warehouse = new LocalWarehouse();
        when(mockCampaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
        mekhq.campaign.ForceQuartermaster quartermaster = new mekhq.campaign.ForceQuartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        // Add a suitable parts to the warehouse
        Part leftArm = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        warehouse.addPart(leftArm);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        // Add a person to do the work
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        missingPart.setTech(person);

        // Find the replacement part for overnight work
        missingPart.reservePart();

        // Ensure we found the right part
        assertTrue(missingPart.hasReplacementPart());
        assertEquals(leftArm, missingPart.getReplacementPart());
        assertFalse(leftArm.isSpare());
        assertTrue(leftArm.isReservedForReplacement());
        assertEquals(1, leftArm.getQuantity());

        // Cancel the reservation for the part
        missingPart.cancelReservation();

        // Ensure we returned the part and it is free for use
        assertFalse(missingPart.hasReplacementPart());
        assertNull(missingPart.getReplacementPart());
        assertTrue(leftArm.isSpare());
        assertFalse(leftArm.isReservedForReplacement());
        assertEquals(1, leftArm.getQuantity());
    }

    @Test
    public void cancelReservationReturnsJustOnePart() {
        Campaign mockCampaign = mockCampaign();
        LocalWarehouse warehouse = new LocalWarehouse();
        when(mockCampaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
        mekhq.campaign.ForceQuartermaster quartermaster = new mekhq.campaign.ForceQuartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        // Add a few suitable parts to the warehouse
        MekLocation leftArm = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        int startingQuantity = 3;
        leftArm.setQuantity(startingQuantity);
        leftArm.setBrandNew(false);
        warehouse.addPart(leftArm);

        // Add a not-suitable parts to the warehouse
        MekLocation leftArmForRefit = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        // Add a person to do the work
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        missingPart.setTech(person);

        // Find the replacement part for overnight work
        missingPart.reservePart();

        // Ensure we found the right part
        assertTrue(missingPart.hasReplacementPart());
        Part replacement = missingPart.getReplacementPart();
        assertTrue(missingPart.isAcceptableReplacement(replacement, false));

        // Ensure the original part is unchanged
        assertTrue(leftArm.isSpare());
        assertFalse(leftArm.isReservedForReplacement());
        assertEquals(startingQuantity - 1, leftArm.getQuantity());

        // Cancel the reservation
        missingPart.cancelReservation();

        // Ensure we returned the part to the warehouse
        assertTrue(leftArm.isSpare());
        assertFalse(leftArm.isReservedForReplacement());
        assertEquals(startingQuantity, leftArm.getQuantity());
    }

    @Test
    public void fabricationMultipliesActualTimeByTen() {
        Campaign mockCampaign = mockCampaign();
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        int normalTime = missingPart.getActualTime();

        missingPart.setFabricating(true);

        assertEquals(normalTime * 10, missingPart.getActualTime());
    }

    @Test
    public void fabricationAddsTwoToTargetModifiers() {
        Campaign mockCampaign = mockCampaign();
        when(mockCampaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        int normalMod = missingPart.getAllMods(null).getValue();

        missingPart.setFabricating(true);

        assertEquals(normalMod + 2, missingPart.getAllMods(null).getValue());
    }

    @Test
    public void fabricatorAbilityOffsetsTheFabricationPenalty() {
        Campaign mockCampaign = mockCampaign();
        when(mockCampaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);
        missingPart.setFabricating(true);

        Person plainTech = mock(Person.class);
        when(plainTech.getOptions()).thenReturn(new PersonnelOptions());

        Person fabricatorTech = mock(Person.class);
        PersonnelOptions fabricatorOptions = new PersonnelOptions();
        fabricatorOptions.getOption(PersonnelOptions.TECH_FABRICATOR).setValue(true);
        when(fabricatorTech.getOptions()).thenReturn(fabricatorOptions);

        int withoutSpa = missingPart.getAllMods(plainTech).getValue();
        int withSpa = missingPart.getAllMods(fabricatorTech).getValue();

        // The Fabricator SPA reduces the fabrication target number by 2.
        assertEquals(withoutSpa - 2, withSpa);
    }

    @Test
    public void juryRiggerAbilityReducesFabricationCostByAQuarter() {
        Campaign mockCampaign = mockCampaign();
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.USE_BALANCED_FABRICATION, true);
        options.set(CampaignOption.PAY_FOR_PARTS, true);
        options.set(CampaignOption.PAY_FOR_REPAIRS, true);
        when(mockCampaign.getCampaignOptions()).thenReturn(options);
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);
        missingPart.setFabricating(true);

        Person plainTech = mock(Person.class);
        when(plainTech.getOptions()).thenReturn(new PersonnelOptions());

        Person juryRiggerTech = mock(Person.class);
        PersonnelOptions juryRiggerOptions = new PersonnelOptions();
        juryRiggerOptions.getOption(PersonnelOptions.TECH_JURY_RIGGER).setValue(true);
        when(juryRiggerTech.getOptions()).thenReturn(juryRiggerOptions);

        Money fullCost = missingPart.getFabricationCost(plainTech);
        Money discountedCost = missingPart.getFabricationCost(juryRiggerTech);

        assertEquals(fullCost.multipliedBy(0.75), discountedCost);
    }

    @Test
    public void wastefulFlawIncreasesFabricationCostByAQuarter() {
        Campaign mockCampaign = mockCampaign();
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.USE_BALANCED_FABRICATION, true);
        options.set(CampaignOption.PAY_FOR_PARTS, true);
        options.set(CampaignOption.PAY_FOR_REPAIRS, true);
        when(mockCampaign.getCampaignOptions()).thenReturn(options);
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);
        missingPart.setFabricating(true);

        Person plainTech = mock(Person.class);
        when(plainTech.getOptions()).thenReturn(new PersonnelOptions());

        Person wastefulTech = mock(Person.class);
        PersonnelOptions wastefulOptions = new PersonnelOptions();
        wastefulOptions.getOption(PersonnelOptions.TECH_WASTEFUL).setValue(true);
        when(wastefulTech.getOptions()).thenReturn(wastefulOptions);

        Money fullCost = missingPart.getFabricationCost(plainTech);
        Money surchargedCost = missingPart.getFabricationCost(wastefulTech);

        assertEquals(fullCost.multipliedBy(1.25), surchargedCost);
    }

    @Test
    public void balancedFabricationCostsTenTimesThePartPrice() {
        Campaign mockCampaign = mockCampaign();
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.USE_BALANCED_FABRICATION, true);
        options.set(CampaignOption.PAY_FOR_PARTS, true);
        options.set(CampaignOption.PAY_FOR_REPAIRS, false);
        when(mockCampaign.getCampaignOptions()).thenReturn(options);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        Money expected = missingPart.getNewPart().getActualValue().multipliedBy(10);
        assertEquals(expected, missingPart.getFabricationCost());
    }

    @Test
    public void balancedFabricationCombinesTenTimesRepairAndPartCosts() {
        Campaign mockCampaign = mockCampaign();
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.USE_BALANCED_FABRICATION, true);
        options.set(CampaignOption.PAY_FOR_PARTS, true);
        options.set(CampaignOption.PAY_FOR_REPAIRS, true);
        when(mockCampaign.getCampaignOptions()).thenReturn(options);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        Part newPart = missingPart.getNewPart();
        Money expected = newPart.getUndamagedValue().multipliedBy(0.2).multipliedBy(10)
                               .plus(newPart.getActualValue().multipliedBy(10));
        assertEquals(expected, missingPart.getFabricationCost());
    }

    @Test
    public void fabricationIsFreeWhenNotPayingForParts() {
        Campaign mockCampaign = mockCampaign();
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.USE_BALANCED_FABRICATION, true);
        options.set(CampaignOption.PAY_FOR_PARTS, true);
        options.set(CampaignOption.PAY_FOR_REPAIRS, false);
        when(mockCampaign.getCampaignOptions()).thenReturn(options);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        assertTrue(missingPart.getFabricationCost().isZero());
    }

    @Test
    public void rulesAccurateFabricationCombinesRepairAndPartCosts() {
        Campaign mockCampaign = mockCampaign();
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.USE_BALANCED_FABRICATION, false);
        options.set(CampaignOption.PAY_FOR_PARTS, true);
        options.set(CampaignOption.PAY_FOR_REPAIRS, true);
        when(mockCampaign.getCampaignOptions()).thenReturn(options);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        Part newPart = missingPart.getNewPart();
        Money expected = newPart.getUndamagedValue().multipliedBy(0.2).multipliedBy(10)
                               .plus(newPart.getActualValue().multipliedBy(0.5));
        assertEquals(expected, missingPart.getFabricationCost());
    }

    @Test
    public void cannotFabricateWithoutAUnit() {
        Campaign mockCampaign = mockCampaign();
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        assertFalse(missingPart.canFabricate().isEmpty());
    }

    @Test
    public void cancelFabricationClearsTheFlagAndTech() {
        Campaign mockCampaign = mockCampaign();
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);
        missingPart.setFabricating(true);
        missingPart.setFabricateUntilSuccess(true);
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        missingPart.setTech(person);
        missingPart.addTimeSpent(500);

        missingPart.cancelFabrication();

        assertFalse(missingPart.isFabricating());
        assertFalse(missingPart.isFabricateUntilSuccess());
        assertNull(missingPart.getTech());
        assertEquals(0, missingPart.getTimeSpent());
    }

    @Test
    public void cannotFabricateAboveTechRatingCWithoutAFactory() {
        Campaign mockCampaign = mockCampaign();
        when(mockCampaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        // Standard mek internal structure is Tech Rating D, above the A-C fabrication limit.
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        Unit unit = mock(Unit.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(unit.getEntity().getWeight()).thenReturn(20.0);
        when(unit.getSite()).thenReturn(Unit.SITE_FIELD_WORKSHOP);
        missingPart.setUnit(unit);

        // Without the optional rule, Tech Rating D can only be fabricated at a factory.
        assertEquals("Too Complex: needs factory conditions", missingPart.canFabricate());
    }

    @Test
    public void macGyverAbilityLowersTheEffectiveTechRatingByOne() {
        Campaign mockCampaign = mockCampaign();
        when(mockCampaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        // Standard mek internal structure is Tech Rating D - not fabricable in the field by a plain tech.
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        Unit unit = mock(Unit.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(unit.getEntity().getWeight()).thenReturn(20.0);
        when(unit.getSite()).thenReturn(Unit.SITE_FIELD_WORKSHOP);
        missingPart.setUnit(unit);

        Person plainTech = mock(Person.class);
        when(plainTech.getOptions()).thenReturn(new PersonnelOptions());

        Person macGyverTech = mock(Person.class);
        PersonnelOptions macGyverOptions = new PersonnelOptions();
        macGyverOptions.getOption(PersonnelOptions.TECH_MACGYVER).setValue(true);
        when(macGyverTech.getOptions()).thenReturn(macGyverOptions);

        // MacGyver treats the Tech Rating D part as C, so it becomes fabricable in the field.
        assertFalse(missingPart.canFabricate(plainTech).isBlank());
        assertTrue(missingPart.canFabricate(macGyverTech).isBlank());
    }

    @Test
    public void maintenanceFacilityOptionAllowsTechRatingDFabrication() {
        Campaign mockCampaign = mockCampaign();
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.FABRICATE_D_IN_MAINTENANCE_FACILITY, true);
        when(mockCampaign.getCampaignOptions()).thenReturn(options);
        // Standard mek internal structure is Tech Rating D.
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        Unit unit = mock(Unit.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(unit.getEntity().getWeight()).thenReturn(20.0);
        when(unit.getSite()).thenReturn(Unit.SITE_FACILITY_MAINTENANCE);
        missingPart.setUnit(unit);

        assertTrue(missingPart.canFabricate().isEmpty());
    }

    @Test
    public void maintenanceFacilityOptionDoesNotApplyBelowAMaintenanceFacility() {
        Campaign mockCampaign = mockCampaign();
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.FABRICATE_D_IN_MAINTENANCE_FACILITY, true);
        when(mockCampaign.getCampaignOptions()).thenReturn(options);
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        Unit unit = mock(Unit.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(unit.getEntity().getWeight()).thenReturn(20.0);
        // A basic facility is below the maintenance-facility threshold, so Tech Rating D is still disallowed.
        when(unit.getSite()).thenReturn(Unit.SITE_FACILITY_BASIC);
        missingPart.setUnit(unit);

        // The optional rule is on, so the explanation should point at the maintenance facility it still needs.
        assertEquals("Too Complex: needs a maintenance facility or better", missingPart.canFabricate());
    }

    @Test
    public void factoryConditionsAllowFabricationRegardlessOfTechRating() {
        Campaign mockCampaign = mockCampaign();
        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        Unit unit = mock(Unit.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(unit.getEntity().getWeight()).thenReturn(20.0);
        when(unit.getSite()).thenReturn(Unit.SITE_FACTORY_CONDITIONS);
        missingPart.setUnit(unit);

        assertTrue(missingPart.canFabricate().isEmpty());
    }

    @Test
    public void cancelReservationReturnsNothingIfReplacementUsed() {
        Campaign mockCampaign = mockCampaign();
        LocalWarehouse warehouse = new LocalWarehouse();
        when(mockCampaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
        mekhq.campaign.ForceQuartermaster quartermaster = new mekhq.campaign.ForceQuartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        // Add a few suitable parts to the warehouse
        MekLocation leftArm = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        int startingQuantity = 3;
        leftArm.setQuantity(startingQuantity);
        warehouse.addPart(leftArm);

        // Add a not-suitable parts to the warehouse
        MekLocation leftArmForRefit = new MekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mek.LOC_LEFT_ARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
              false, false, false, mockCampaign);

        // Add a person to do the work
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(UUID.randomUUID());
        missingPart.setTech(person);

        // Find the replacement part for overnight work
        missingPart.reservePart();

        // Ensure we found the right part
        assertTrue(missingPart.hasReplacementPart());
        Part replacement = missingPart.getReplacementPart();
        assertTrue(missingPart.isAcceptableReplacement(replacement, false));

        // Ensure the original part is unchanged
        assertTrue(leftArm.isSpare());
        assertFalse(leftArm.isReservedForReplacement());
        assertEquals(startingQuantity - 1, leftArm.getQuantity());

        // Use the replacement part
        replacement.changeQuantity(-1);

        // Cancel the reservation
        missingPart.cancelReservation();

        // Ensure we did not return the replacement part to the warehouse
        assertTrue(leftArm.isSpare());
        assertFalse(leftArm.isReservedForReplacement());
        assertEquals(startingQuantity - 1, leftArm.getQuantity());
    }
}
