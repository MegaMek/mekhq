/*
 * StripUnitActionTest.java
 *
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.Warehouse;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class PartTest {
    @Test
    public void sparePart() {
        Part part = new MekSensor();

        assertNull(part.getUnit());
        assertNull(part.getParentPart());
        assertNull(part.getRefitUnit());
        assertFalse(part.isReservedForReplacement());
        assertTrue(part.isSpare());
    }

    @Test
    public void hasUnitIsNotSpare() {
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.getWeight()).thenReturn(20.0);

        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        Part part = new MekSensor();
        part.setUnit(mockUnit);

        assertNotNull(part.getUnit());
        assertNull(part.getParentPart());
        assertNull(part.getRefitUnit());
        assertFalse(part.isReservedForReplacement());
        assertFalse(part.isSpare());
    }

    @Test
    public void isReservedForRefitNotSpare() {
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());

        Part part = new MekSensor();
        part.setRefitUnit(mockUnit);

        assertNull(part.getUnit());
        assertNull(part.getParentPart());
        assertNotNull(part.getRefitUnit());
        assertFalse(part.isReservedForReplacement());
        assertFalse(part.isSpare());
    }

    @Test
    public void isChildPartNotSpare() {
        Part parent = mock(Part.class);

        Part part = new MekSensor();
        part.setParentPart(parent);

        assertNull(part.getUnit());
        assertNotNull(part.getParentPart());
        assertNull(part.getRefitUnit());
        assertFalse(part.isReservedForReplacement());
        assertFalse(part.isSpare());
    }

    @Test
    public void isReservedForReplacementNotSpare() {
        Person mockTech = mock(Person.class);
        when(mockTech.getId()).thenReturn(UUID.randomUUID());

        Part part = new MekSensor();
        part.setReservedBy(mockTech);

        assertNull(part.getUnit());
        assertNull(part.getParentPart());
        assertNull(part.getRefitUnit());
        assertTrue(part.isReservedForReplacement());
        assertFalse(part.isSpare());
    }

    @Test
    public void incrementQuantity() {
        Part part = new MekLocation();

        int quantity = part.getQuantity();

        part.incrementQuantity();

        assertEquals(quantity + 1, part.getQuantity());
    }

    @Test
    public void decrementQuantity() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Part part = new MekLocation();
        part.setCampaign(mockCampaign);

        part.setQuantity(2);

        // Setting the quantity specifically should work.
        assertEquals(2, part.getQuantity());

        part.decrementQuantity();

        // Quantity should now be 1
        assertEquals(1, part.getQuantity());

        part.decrementQuantity();

        // Quantity should now be 0...
        assertEquals(0, part.getQuantity());

        // ...which means we should have removed the part.
        verify(mockWarehouse, times(1)).removePart(eq(part));
    }

    @Test
    public void decrementQuantityDoesNotGoNegative() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Part part = new MekLocation();
        part.setCampaign(mockCampaign);

        part.setQuantity(1);

        part.decrementQuantity();

        // Quantity should now be 0...
        assertEquals(0, part.getQuantity());

        part.decrementQuantity();

        // Quantity should still be 0...
        assertEquals(0, part.getQuantity());
    }

    @Test
    public void decrementQuantityZeroRemovesChildParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Part part = new MekLocation();
        part.setCampaign(mockCampaign);

        Part childPart0 = mock(Part.class);
        part.addChildPart(childPart0);

        Part childPart1 = mock(Part.class);
        part.addChildPart(childPart1);

        part.setQuantity(1);

        // Remove the part by decrementing its quantity to 0.
        part.decrementQuantity();

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(mockWarehouse, times(3)).removePart(partCaptor.capture());

        List<Part> removedParts = partCaptor.getAllValues();
        assertTrue(removedParts.contains(childPart0));
        assertTrue(removedParts.contains(childPart1));
        assertTrue(removedParts.contains(part));
    }

    @Test
    public void setQuantity() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Part part = new MekLocation();
        part.setCampaign(mockCampaign);

        part.setQuantity(10);

        // Setting the quantity specifically should work.
        assertEquals(10, part.getQuantity());

        part.setQuantity(0);

        // Quantity should now be 0...
        assertEquals(0, part.getQuantity());

        // ...which means we should have removed the part.
        verify(mockWarehouse, times(1)).removePart(eq(part));
    }

    @Test
    public void setNegativeQuantity() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Part part = new MekLocation();
        part.setCampaign(mockCampaign);

        part.setQuantity(10);

        // Setting the quantity specifically should work.
        assertEquals(10, part.getQuantity());

        part.setQuantity(-5);

        // Quantity should be zero, even though we set a negative count.
        assertEquals(0, part.getQuantity());

        // ...which means we should have removed the part.
        verify(mockWarehouse, times(1)).removePart(eq(part));
    }

    @Test
    public void setQuantityZeroRemovesChildParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Part part = new MekLocation();
        part.setCampaign(mockCampaign);

        Part childPart0 = mock(Part.class);
        part.addChildPart(childPart0);

        Part childPart1 = mock(Part.class);
        part.addChildPart(childPart1);

        // Remove the part by setting its quantity to 0.
        part.setQuantity(0);

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(mockWarehouse, times(3)).removePart(partCaptor.capture());

        List<Part> removedParts = partCaptor.getAllValues();
        assertTrue(removedParts.contains(childPart0));
        assertTrue(removedParts.contains(childPart1));
        assertTrue(removedParts.contains(part));
    }

    @Test
    public void daysToArrival() {
        Part part = new MekLocation();

        part.setDaysToArrival(5);

        assertEquals(5, part.getDaysToArrival());

        assertFalse(part.isPresent());

        part.setDaysToArrival(0);

        assertTrue(part.isPresent());

        part.setDaysToArrival(-5);

        assertEquals(0, part.getDaysToArrival());
        assertTrue(part.isPresent());
    }

    @Test
    public void childParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Part part = new MekLocation();
        part.setCampaign(mockCampaign);

        // Parts should start without child parts.
        assertFalse(part.hasChildParts());
        assertNotNull(part.getChildParts());
        assertTrue(part.getChildParts().isEmpty());

        // Add a child part
        Part childPart0 = mock(Part.class);
        part.addChildPart(childPart0);

        assertTrue(part.hasChildParts());
        assertNotNull(part.getChildParts());
        assertEquals(1, part.getChildParts().size());
        assertTrue(part.getChildParts().contains(childPart0));
        verify(childPart0, times(1)).setParentPart(eq(part));

        // Add a second child part
        Part childPart1 = mock(Part.class);
        part.addChildPart(childPart1);

        assertTrue(part.hasChildParts());
        assertNotNull(part.getChildParts());
        assertEquals(2, part.getChildParts().size());
        assertTrue(part.getChildParts().contains(childPart0));
        assertTrue(part.getChildParts().contains(childPart1));
        verify(childPart1, times(1)).setParentPart(eq(part));

        // Remove a random part not associated with this part
        Part randomPart = mock(Part.class);
        part.removeChildPart(randomPart);

        assertTrue(part.hasChildParts());
        assertNotNull(part.getChildParts());
        assertEquals(2, part.getChildParts().size());
        assertTrue(part.getChildParts().contains(childPart0));
        assertTrue(part.getChildParts().contains(childPart1));
        verify(randomPart, times(0)).setParentPart(eq(null));

        // Remove one of the actual child parts
        part.removeChildPart(childPart1);

        assertTrue(part.hasChildParts());
        assertNotNull(part.getChildParts());
        assertEquals(1, part.getChildParts().size());
        assertTrue(part.getChildParts().contains(childPart0));
        assertFalse(part.getChildParts().contains(childPart1));
        verify(childPart1, times(1)).setParentPart(eq(null));

        // Now remove al lthe remaining parts
        part.removeAllChildParts();

        assertFalse(part.hasChildParts());
        assertNotNull(part.getChildParts());
        assertTrue(part.getChildParts().isEmpty());
        verify(childPart0, times(1)).setParentPart(eq(null));
    }
}
