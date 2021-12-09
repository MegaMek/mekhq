/*
 * Copyright (C) 2020 MegaMek team
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.Test;

import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class MissingPartTest {
    @Test
    public void reservePartDoesNothingWithoutTheRightPart() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);
        when(mockCampaign.getQuartermaster()).thenReturn(quartermaster);

        // Add a not-suitable parts to the warehouse
        Part leftArmForRefit = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
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
        Part leftArm = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        warehouse.addPart(leftArm);

        MissingPart missingPart = new MissingMekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
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
        Part leftArm = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        warehouse.addPart(leftArm);

        // Add a not-suitable parts to the warehouse
        Part leftArmForRefit = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
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
        MekLocation leftArm = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        int startingQuantity = 3;
        leftArm.setQuantity(startingQuantity);
        warehouse.addPart(leftArm);

        // Add a not-suitable parts to the warehouse
        MekLocation leftArmForRefit = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
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
        assertTrue(replacement instanceof MekLocation);
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
        Part leftArm = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        warehouse.addPart(leftArm);

        MissingPart missingPart = new MissingMekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
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
        MekLocation leftArm = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        int startingQuantity = 3;
        leftArm.setQuantity(startingQuantity);
        warehouse.addPart(leftArm);

        // Add a not-suitable parts to the warehouse
        MekLocation leftArmForRefit = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
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
        MekLocation leftArm = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        int startingQuantity = 3;
        leftArm.setQuantity(startingQuantity);
        warehouse.addPart(leftArm);

        // Add a not-suitable parts to the warehouse
        MekLocation leftArmForRefit = new MekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
                false, false, false, false, false, mockCampaign);
        leftArmForRefit.setRefitUnit(mock(Unit.class));
        warehouse.addPart(leftArmForRefit);

        MissingPart missingPart = new MissingMekLocation(Mech.LOC_LARM, 20, EquipmentType.T_STRUCTURE_STANDARD,
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
