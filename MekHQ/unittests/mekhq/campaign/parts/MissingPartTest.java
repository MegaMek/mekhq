/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.UUID;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingMekLocation;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

public class MissingPartTest {
    @Test
    public void reservePartDoesNothingWithoutTheRightPart() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
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
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
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
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
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
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
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
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
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
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
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
    public void cancelReservationReturnsNothingIfReplacementUsed() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
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
        replacement.decrementQuantity();

        // Cancel the reservation
        missingPart.cancelReservation();

        // Ensure we did not return the replacement part to the warehouse
        assertTrue(leftArm.isSpare());
        assertFalse(leftArm.isReservedForReplacement());
        assertEquals(startingQuantity - 1, leftArm.getQuantity());
    }
}
