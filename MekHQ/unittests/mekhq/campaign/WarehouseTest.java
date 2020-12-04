/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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

package mekhq.campaign;

import org.junit.Test;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import mekhq.EventSpy;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.PartNewEvent;
import mekhq.campaign.event.PartRemovedEvent;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WarehouseTest {
    @Test
    public void testWarehouseSimplePartActions() {
        Warehouse warehouse = new Warehouse();

        // A new warehouse is empty
        assertTrue(warehouse.getParts().isEmpty());

        // Create a mock part
        int mockId = 10;
        Part mockPart = mock(Part.class);
        when(mockPart.getId()).thenReturn(mockId);

        // Add the mock part to our warehouse
        warehouse.addPart(mockPart);

        // The part should be returned when we get it by ID
        assertEquals(mockPart, warehouse.getPart(mockId));

        // forEachPart should have our part
        warehouse.forEachPart(p -> {
            // There should only be one part in the warehouse
            // and it should be our part
            assertEquals(mockPart, p);
        });

        // getParts should return the part
        assertTrue(warehouse.getParts().contains(mockPart));

        // The part should also be removed when we request it
        assertTrue(warehouse.removePart(mockPart));

        // And the part should no longer be in the warehouse
        assertNull(warehouse.getPart(mockId));

        // We should not run over any part once removed
        warehouse.forEachPart(p -> {
            assertTrue(false);
        });

        // getParts should no longer contain anything
        assertTrue(warehouse.getParts().isEmpty());
    }

    @Test
    public void testWarehouseAddNewPart() {
        Warehouse warehouse = new Warehouse();

        // Create a mock part without an ID
        Part mockPart = mock(Part.class, RETURNS_DEEP_STUBS);
        when(mockPart.getId()).thenCallRealMethod();
        doCallRealMethod().when(mockPart).setId(anyInt());

        // Add the mock part to our warehouse
        warehouse.addPart(mockPart);

        // We should have been assigned an ID
        assertTrue(mockPart.getId() > 0);

        // The part should be returned when we get it by ID
        assertEquals(mockPart, warehouse.getPart(mockPart.getId()));

        // forEachPart should have our part
        warehouse.forEachPart(p -> {
            // There should only be one part in the warehouse
            // and it should be our part
            assertEquals(mockPart, p);
        });

        // getParts should return the part
        assertTrue(warehouse.getParts().contains(mockPart));

        // The part should also be removed when we request it
        assertTrue(warehouse.removePart(mockPart));

        // And the part should no longer be in the warehouse
        assertNull(warehouse.getPart(mockPart.getId()));

        // We should not run over any part once removed
        warehouse.forEachPart(p -> {
            assertTrue(false);
        });

        // getParts should no longer contain anything
        assertTrue(warehouse.getParts().isEmpty());
    }

    @Test
    public void testWarehouseAddSecondNewPart() {
        Warehouse warehouse = new Warehouse();

        // Create a mock part without an ID
        Part mockPart0 = mock(Part.class, RETURNS_DEEP_STUBS);
        when(mockPart0.getId()).thenCallRealMethod();
        doCallRealMethod().when(mockPart0).setId(anyInt());

        // Add the mock part to our warehouse
        warehouse.addPart(mockPart0);

        // We should have been assigned an ID
        assertTrue(mockPart0.getId() > 0);

        // Create a second mock part without an ID
        Part mockPart1 = mock(Part.class, RETURNS_DEEP_STUBS);
        when(mockPart1.getId()).thenCallRealMethod();
        doCallRealMethod().when(mockPart1).setId(anyInt());

        // Add the mock part to our warehouse
        warehouse.addPart(mockPart1);

        // We should have been assigned an ID...
        assertTrue(mockPart1.getId() > 0);

        // ... that is not the same as our previous part
        assertNotEquals(mockPart0.getId(), mockPart1.getId());
    }

    @Test
    public void testWarehouseAddPartEvent() {
        Warehouse warehouse = new Warehouse();

        // Create a mock part
        int mockId = 10;
        Part mockPart = mock(Part.class);
        when(mockPart.getId()).thenReturn(mockId);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the mock part to our warehouse
            warehouse.addPart(mockPart);

            // This part never existed so there should be
            // a PartNewEvent fired.
            assertTrue(eventSpy.getEvents()
                    .stream()
                    .filter(e -> e instanceof PartNewEvent)
                    .filter(e -> mockPart == ((PartNewEvent) e).getPart())
                    .findAny()
                    .isPresent());

            // Add the part again, simulating being say removed from a
            // unit or something
            warehouse.addPart(mockPart);

            // There should be only ONE event as we did not add
            // this part to the warehouse
            assertEquals(1,
                    eventSpy.getEvents()
                            .stream()
                            .filter(e -> e instanceof PartNewEvent)
                            .filter(e -> mockPart == ((PartNewEvent) e).getPart())
                            .count());
        }
    }

    @Test
    public void testWarehouseRemovePart() {
        Warehouse warehouse = new Warehouse();

        // Create a mock part
        int mockId = 10;
        Part mockPart = mock(Part.class);
        when(mockPart.getId()).thenReturn(mockId);

        try (EventSpy eventSpy = new EventSpy()) {
            // Ensure we can't remove a part that doesn't exist
            assertFalse(warehouse.removePart(mockPart));

            // If we didn't remove a part, we should have no event
            assertFalse(eventSpy.getEvents()
                    .stream()
                    .filter(e -> e instanceof PartRemovedEvent)
                    .findAny()
                    .isPresent());

            // Add the mock part to our warehouse
            warehouse.addPart(mockPart);

            // Ensure we can then remove the part
            assertTrue(warehouse.removePart(mockPart));

            // There should be an event where we removed the mock part
            assertEquals(1,
                    eventSpy.getEvents()
                            .stream()
                            .filter(e -> e instanceof PartRemovedEvent)
                            .filter(e -> mockPart == ((PartRemovedEvent) e).getPart())
                            .count());
        }
    }

    @Test
    public void testWarehouseRemoveChildParts() {
        Warehouse warehouse = new Warehouse();

        // Add a parent part to the warehouse
        Part mockParentPart = createMockPart(1);
        warehouse.addPart(mockParentPart);

        // Create child parts for the parent part
        List<Part> mockChildParts = new ArrayList<>();
        mockChildParts.add(createMockPart(2));
        mockChildParts.add(createMockPart(3));
        when(mockParentPart.getChildParts()).thenReturn(mockChildParts);

        for (Part mockChildPart : mockChildParts) {
            when(mockChildPart.getParentPart()).thenReturn(mockParentPart);
            when(mockChildPart.hasParentPart()).thenReturn(true);

            warehouse.addPart(mockChildPart);
        }

        try (EventSpy eventSpy = new EventSpy()) {
            // Ensure we can then remove the part
            assertTrue(warehouse.removePart(mockParentPart));

            // There should be three events where we removed parts
            assertEquals(3,
                    eventSpy.getEvents()
                            .stream()
                            .filter(e -> e instanceof PartRemovedEvent)
                            .count());

            // And the three events should correlate to the child parts being removed
            assertNotNull(eventSpy.findEvent(PartRemovedEvent.class, e -> mockParentPart == e.getPart()));
            for (Part mockChildPart : mockChildParts) {
                assertNotNull(eventSpy.findEvent(PartRemovedEvent.class, e -> mockChildPart == e.getPart()));
            }
        }
    }

    @Test
    public void testAddSpareRegularPart() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add a spare part to the warehouse
        Part mockPart = spy(new MekLocation());
        mockPart.setCampaign(mockCampaign);
        mockPart.setQuantity(1);

        // Add the part to our warehouse, merging it
        // with any existing part
        Part addedPart = warehouse.addPart(mockPart, true);

        // We should only have one of these parts
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockPart, addedPart);
        assertTrue(addedPart.isSpare());
        assertEquals(1, addedPart.getQuantity());

        // Make a new part, also spare
        Part mockSparePart = spy(new MekLocation());
        mockSparePart.setCampaign(mockCampaign);
        mockSparePart.setQuantity(2);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the spare part to our warehouse, and
            // ask that it be merged with an existing part
            addedPart = warehouse.addPart(mockSparePart, true);

            // We should see that the original part was changed
            assertNotNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockPart == e.getPart()));

            // And we should see that the other part was never added
            assertNull(eventSpy.findEvent(PartNewEvent.class, e -> mockSparePart == e.getPart()));
            assertTrue(mockSparePart.getId() <= 0);
        }

        // We should still only have one instance of the
        // part, but instead we will now have 3 of them.
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockPart, addedPart);
        assertTrue(addedPart.isSpare());
        assertEquals(3, addedPart.getQuantity());
    }

    @Test
    public void testReturnSpareRegularPart() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add a spare part to the warehouse
        Part mockPart = spy(new MekLocation());
        mockPart.setCampaign(mockCampaign);
        mockPart.setQuantity(2);

        // Add the part to our warehouse, merging it
        // with any existing part (there aren't any).
        Part addedPart = warehouse.addPart(mockPart, true);

        // We should only have one instance of this part
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockPart, addedPart);
        assertTrue(addedPart.isSpare());
        assertEquals(2, addedPart.getQuantity());

        // Make a new part that is on a unit
        Part mockUnitPart = spy(new MekLocation());
        mockUnitPart.setCampaign(mockCampaign);
        mockUnitPart.setQuantity(1);
        mockUnitPart.setUnit(createMockUnit());

        // Add the new part that is part of a unit
        addedPart = warehouse.addPart(mockUnitPart, true);

        // We should have two parts in the warehouse,
        // and they should be distinct.
        assertEquals(2, warehouse.getParts().size());
        assertEquals(mockUnitPart, addedPart);
        assertFalse(addedPart.isSpare());
        assertEquals(2, mockPart.getQuantity());
        assertEquals(1, addedPart.getQuantity());

        try (EventSpy eventSpy = new EventSpy()) {
            // Now lets take the new part off of the unit...
            mockUnitPart.setUnit(null);

            // ...and add it back to the Warehouse.
            addedPart = warehouse.addPart(mockUnitPart, true);

            // We should see that the existing spare part was changed
            assertNotNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockPart == e.getPart()));

            // And we should see that the "unit part" was removed
            // when it was merged with the existing spare part
            assertNotNull(eventSpy.findEvent(PartRemovedEvent.class, e -> mockUnitPart == e.getPart()));
            assertTrue(mockUnitPart.getId() <= 0);
        }

        // We should now only have one instance of the
        // part, and we will now have 3 of them.
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockPart, addedPart);
        assertTrue(addedPart.isSpare());
        assertEquals(3, addedPart.getQuantity());
    }

    @Test
    public void testAddSpareArmorPart() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add some spare armor to the warehouse
        Armor mockArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 16);

        // Add the armor to our warehouse, merging it
        // with any existing part
        Part addedArmor = warehouse.addPart(mockArmor, true);
        assertTrue(addedArmor instanceof Armor);

        // We should only have one of these parts
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockArmor, addedArmor);
        assertTrue(addedArmor.isSpare());

        // Make some new armor, also spare
        Armor mockSpareArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 32);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the spare armor to our warehouse, and
            // ask that it be merged with an existing part
            addedArmor = warehouse.addPart(mockSpareArmor, true);
            assertTrue(addedArmor instanceof Armor);

            // We should see that the original part was changed
            assertNotNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockArmor == e.getPart()));

            // And we should see that the other part was never added
            assertNull(eventSpy.findEvent(PartNewEvent.class, e -> mockSpareArmor == e.getPart()));
            assertTrue(mockSpareArmor.getId() <= 0);
        }

        // We should still only have one instance of the
        // part, but instead we will now have 3 of them.
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockArmor, addedArmor);
        assertTrue(addedArmor.isSpare());

        // Double check the math from above.
        assertEquals(3.0, addedArmor.getTonnage(), 0.000001);
        assertEquals(48, ((Armor) addedArmor).getAmount());
    }

    @Test
    public void testAddSpareAmmoStoragePart() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add some spare ammo to the warehouse
        AmmoStorage mockAmmoStorage = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 20);

        // Add the ammo to our warehouse, merging it
        // with any existing part (there are none)
        Part addedAmmo = warehouse.addPart(mockAmmoStorage, true);
        assertTrue(addedAmmo instanceof AmmoStorage);

        // We should only have one of these parts
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockAmmoStorage, addedAmmo);
        assertTrue(addedAmmo.isSpare());

        // Make some new ammo, also spare
        AmmoStorage mockSpareAmmo = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 40);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the spare ammo to our warehouse, and
            // ask that it be merged with an existing part
            addedAmmo = warehouse.addPart(mockSpareAmmo, true);
            assertTrue(addedAmmo instanceof AmmoStorage);

            // We should see that the original part was changed
            assertNotNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockAmmoStorage == e.getPart()));

            // And we should see that the other part was never added
            assertNull(eventSpy.findEvent(PartNewEvent.class, e -> mockSpareAmmo == e.getPart()));
            assertTrue(mockSpareAmmo.getId() <= 0);
        }

        // We should still only have one instance of the
        // part, but instead we will now have 3 of them.
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockAmmoStorage, addedAmmo);
        assertTrue(addedAmmo.isSpare());

        // Double check the math from above.
        assertEquals(3.0, addedAmmo.getTonnage(), 0.000001);
        assertEquals(60, ((AmmoStorage) addedAmmo).getShots());
    }

    @Test
    public void testAddSparePartWontMixWithRefitPart() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add a spare part to the warehouse reserved for a refit
        Part mockPart = spy(new MekLocation());
        mockPart.setCampaign(mockCampaign);
        mockPart.setQuantity(1);
        mockPart.setRefitUnit(createMockUnit());

        // Add the part to our warehouse, merging it
        // with any existing part (there are none)
        Part addedPart = warehouse.addPart(mockPart, true);

        // We should only have one of these parts
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockPart, addedPart);
        assertFalse(addedPart.isSpare());
        assertEquals(1, addedPart.getQuantity());

        // Make a new part, also spare
        Part mockSparePart = spy(new MekLocation());
        mockSparePart.setCampaign(mockCampaign);
        mockSparePart.setQuantity(2);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the spare part to our warehouse, and
            // ask that it be merged with an existing part
            addedPart = warehouse.addPart(mockSparePart, true);
            assertTrue(mockSparePart.getId() > 0);
            assertEquals(mockSparePart, addedPart);
            assertTrue(addedPart.isSpare());
            assertEquals(2, addedPart.getQuantity());

            // We should see that the original part was NOT changed as it is part
            // of a refit
            assertNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockPart == e.getPart()));

            // And we should see that the other part was added
            assertNotNull(eventSpy.findEvent(PartNewEvent.class, e -> mockSparePart == e.getPart()));
        }

        // We should have two instances of the parts
        assertEquals(2, warehouse.getParts().size());
    }

    @Test
    public void testAddSparePartWontMixWithReplacementPart() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add a spare part to the warehouse reserved for
        // an overnight task on a unit
        Part mockPart = spy(new MekLocation());
        mockPart.setCampaign(mockCampaign);
        mockPart.setQuantity(1);
        mockPart.setReservedBy(createMockTech());

        // Add the part to our warehouse, merging it
        // with any existing part (there are none)
        Part addedPart = warehouse.addPart(mockPart, true);

        // We should only have one of these parts
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockPart, addedPart);
        assertFalse(addedPart.isSpare());
        assertEquals(1, addedPart.getQuantity());

        // Make a new part, also spare
        Part mockSparePart = spy(new MekLocation());
        mockSparePart.setCampaign(mockCampaign);
        mockSparePart.setQuantity(2);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the spare part to our warehouse, and
            // ask that it be merged with an existing part
            addedPart = warehouse.addPart(mockSparePart, true);
            assertTrue(mockSparePart.getId() > 0);
            assertEquals(mockSparePart, addedPart);
            assertTrue(addedPart.isSpare());
            assertEquals(2, addedPart.getQuantity());

            // We should see that the original part was NOT changed as it is part
            // of an overnight task
            assertNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockPart == e.getPart()));

            // And we should see that the other part was added
            assertNotNull(eventSpy.findEvent(PartNewEvent.class, e -> mockSparePart == e.getPart()));
        }

        // We should have two instances of the parts
        assertEquals(2, warehouse.getParts().size());
    }

    @Test
    public void testAddSparePartWontMixWithPartUnderRepair() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add a spare part under repair to the warehouse
        Part mockPart = spy(new MekLocation());
        mockPart.setCampaign(mockCampaign);
        mockPart.setQuantity(1);
        mockPart.setTech(createMockTech());

        // Add the part to our warehouse, merging it
        // with any existing part (there are none)
        Part addedPart = warehouse.addPart(mockPart, true);

        // We should only have one of these parts
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockPart, addedPart);
        assertTrue(addedPart.isSpare());
        assertEquals(1, addedPart.getQuantity());

        // Make a new part, also spare
        Part mockSparePart = spy(new MekLocation());
        mockSparePart.setCampaign(mockCampaign);
        mockSparePart.setQuantity(2);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the spare part to our warehouse, and
            // ask that it be merged with an existing part
            addedPart = warehouse.addPart(mockSparePart, true);
            assertTrue(mockSparePart.getId() > 0);
            assertEquals(mockSparePart, addedPart);
            assertTrue(addedPart.isSpare());
            assertEquals(2, addedPart.getQuantity());

            // We should see that the original part was NOT changed as it is part
            // of an overnight task
            assertNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockPart == e.getPart()));

            // And we should see that the other part was added
            assertNotNull(eventSpy.findEvent(PartNewEvent.class, e -> mockSparePart == e.getPart()));
        }

        // We should have two instances of the parts
        assertEquals(2, warehouse.getParts().size());
    }

    @Test
    public void testAddSpareArmorWontMixWithRefitArmor() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add a spare armor to the warehouse reserved for a refit
        Armor mockArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 16);
        mockArmor.setRefitUnit(createMockUnit());

        // Add the armor to our warehouse
        Part addedArmor = warehouse.addPart(mockArmor, true);
        assertTrue(addedArmor instanceof Armor);

        // We should only have one of these parts
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockArmor, addedArmor);
        assertFalse(addedArmor.isSpare());

        // Make some new armor, also spare
        Armor mockSpareArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 32);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the spare part to our warehouse, and
            // ask that it be merged with an existing part
            addedArmor = warehouse.addPart(mockSpareArmor, true);
            assertTrue(mockSpareArmor.getId() > 0);
            assertEquals(mockSpareArmor, addedArmor);
            assertTrue(addedArmor.isSpare());

            // Double check the math from above.
            assertEquals(2.0, addedArmor.getTonnage(), 0.000001);
            assertEquals(32, ((Armor) addedArmor).getAmount());

            // We should see that the original part was NOT changed as it is part
            // of a refit
            assertNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockArmor == e.getPart()));

            // And we should see that the other part was added
            assertNotNull(eventSpy.findEvent(PartNewEvent.class, e -> mockSpareArmor == e.getPart()));
        }

        // We should have two instances of the parts
        assertEquals(2, warehouse.getParts().size());
    }

    @Test
    public void testAddSpareArmorWontMixWithReplacementArmor() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add a spare part to the warehouse reserved for
        // an overnight task on a unit
        Armor mockArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 16);
        mockArmor.setReservedBy(createMockTech());

        // Add the armor to our warehouse
        Part addedArmor = warehouse.addPart(mockArmor, true);
        assertTrue(addedArmor instanceof Armor);

        // We should only have one of these parts
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockArmor, addedArmor);
        assertFalse(addedArmor.isSpare());

        // Make some new armor, also spare
        Armor mockSpareArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 32);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the spare part to our warehouse, and
            // ask that it be merged with an existing part
            addedArmor = warehouse.addPart(mockSpareArmor, true);
            assertTrue(mockSpareArmor.getId() > 0);
            assertEquals(mockSpareArmor, addedArmor);
            assertTrue(addedArmor.isSpare());

            // Double check the math from above.
            assertEquals(2.0, addedArmor.getTonnage(), 0.000001);
            assertEquals(32, ((Armor) addedArmor).getAmount());

            // We should see that the original part was NOT changed as it is part
            // of a refit
            assertNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockArmor == e.getPart()));

            // And we should see that the other part was added
            assertNotNull(eventSpy.findEvent(PartNewEvent.class, e -> mockSpareArmor == e.getPart()));
        }

        // We should have two instances of the parts
        assertEquals(2, warehouse.getParts().size());
    }

    @Test
    public void testAddSpareAmmoStorageWonMixWithRefitAmmoStorage() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Add some spare ammo to the warehouse reserved for a refit
        AmmoStorage mockAmmoStorage = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 20);
        mockAmmoStorage.setRefitUnit(createMockUnit());

        // Add the ammo to our warehouse, merging it
        // with any existing part (there are none)
        Part addedAmmo = warehouse.addPart(mockAmmoStorage, true);
        assertTrue(addedAmmo instanceof AmmoStorage);

        // We should only have one of these parts
        assertEquals(1, warehouse.getParts().size());
        assertEquals(mockAmmoStorage, addedAmmo);
        assertFalse(addedAmmo.isSpare());

        // Make some new ammo, also spare
        AmmoStorage mockSpareAmmo = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 40);

        try (EventSpy eventSpy = new EventSpy()) {
            // Add the spare ammo to our warehouse, and
            // ask that it be merged with an existing part
            addedAmmo = warehouse.addPart(mockSpareAmmo, true);
            assertTrue(addedAmmo instanceof AmmoStorage);
            assertTrue(mockSpareAmmo.getId() > 0);
            assertEquals(mockSpareAmmo, addedAmmo);
            assertTrue(addedAmmo.isSpare());
            assertEquals(2.0, addedAmmo.getTonnage(), 0.000001);
            assertEquals(40, ((AmmoStorage) addedAmmo).getShots());

            // We should see that the original part was changed
            assertNull(eventSpy.findEvent(PartChangedEvent.class, e -> mockAmmoStorage == e.getPart()));

            // And we should see that the other part was never added
            assertNotNull(eventSpy.findEvent(PartNewEvent.class, e -> mockSpareAmmo == e.getPart()));
        }

        // We should have 2 instances of these parts
        assertEquals(2, warehouse.getParts().size());
    }

    @Test
    public void testGetSpareParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // Spare
        Part mockSparePart = spy(new MekLocation());
        Part addedPart = warehouse.addPart(mockSparePart, true);
        assertEquals(mockSparePart, addedPart);

        Part mockUnitPart = spy(new MekLocation());
        mockUnitPart.setUnit(createMockUnit());
        addedPart = warehouse.addPart(mockUnitPart, true);
        assertEquals(mockUnitPart, addedPart);

        // Spare (being repaired)
        Part mockSparePartUnderRepair = spy(new MekLocation());
        mockSparePartUnderRepair.setTech(createMockTech());
        addedPart = warehouse.addPart(mockSparePartUnderRepair, true);
        assertEquals(mockSparePartUnderRepair, addedPart);

        Part mockPartForRefit = spy(new MekLocation());
        mockPartForRefit.setRefitUnit(createMockUnit());
        addedPart = warehouse.addPart(mockPartForRefit, true);
        assertEquals(mockPartForRefit, addedPart);

        Part mockPartForRepairTask = spy(new MekLocation());
        mockPartForRepairTask.setReservedBy(createMockTech());
        addedPart = warehouse.addPart(mockPartForRepairTask, true);
        assertEquals(mockPartForRepairTask, addedPart);

        // Spare
        Armor mockSpareArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 16);
        addedPart = warehouse.addPart(mockSpareArmor, true);
        assertEquals(mockSpareArmor, addedPart);

        Armor mockUnitArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 16);
        mockUnitArmor.setUnit(createMockUnit());
        addedPart = warehouse.addPart(mockUnitArmor, true);
        assertEquals(mockUnitArmor, addedPart);

        // Spare
        AmmoStorage mockSpareAmmo = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 20);
        addedPart = warehouse.addPart(mockSpareAmmo, true);
        assertEquals(mockSpareAmmo, addedPart);

        AmmoStorage mockRefitAmmo = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 20);
        mockRefitAmmo.setRefitUnit(createMockUnit());
        addedPart = warehouse.addPart(mockRefitAmmo, true);
        assertEquals(mockRefitAmmo, addedPart);

        // Test: getSpareParts
        List<Part> spareParts = warehouse.getSpareParts();
        assertEquals(4, spareParts.size());
        assertTrue(spareParts.contains(mockSparePart));
        assertTrue(spareParts.contains(mockSparePartUnderRepair));
        assertTrue(spareParts.contains(mockSpareArmor));
        assertTrue(spareParts.contains(mockSpareAmmo));

        // Test: streamSpareParts
        spareParts = warehouse.streamSpareParts().collect(Collectors.toList());
        assertEquals(4, spareParts.size());
        assertTrue(spareParts.contains(mockSparePart));
        assertTrue(spareParts.contains(mockSparePartUnderRepair));
        assertTrue(spareParts.contains(mockSpareArmor));
        assertTrue(spareParts.contains(mockSpareAmmo));
    }

    @Test
    public void testForEachSpareParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // The warehouse is empty!
        warehouse.forEachSparePart(spare -> {
            assertTrue(false);
        });

        // Spare
        Part mockSparePart = spy(new MekLocation());
        Part addedPart = warehouse.addPart(mockSparePart, true);
        assertEquals(mockSparePart, addedPart);

        Part mockUnitPart = spy(new MekLocation());
        mockUnitPart.setUnit(createMockUnit());
        addedPart = warehouse.addPart(mockUnitPart, true);
        assertEquals(mockUnitPart, addedPart);

        // Spare (being repaired)
        Part mockSparePartUnderRepair = spy(new MekLocation());
        mockSparePartUnderRepair.setTech(createMockTech());
        addedPart = warehouse.addPart(mockSparePartUnderRepair, true);
        assertEquals(mockSparePartUnderRepair, addedPart);

        Part mockPartForRefit = spy(new MekLocation());
        mockPartForRefit.setRefitUnit(createMockUnit());
        addedPart = warehouse.addPart(mockPartForRefit, true);
        assertEquals(mockPartForRefit, addedPart);

        Part mockPartForRepairTask = spy(new MekLocation());
        mockPartForRepairTask.setReservedBy(createMockTech());
        addedPart = warehouse.addPart(mockPartForRepairTask, true);
        assertEquals(mockPartForRepairTask, addedPart);

        // Spare
        Armor mockSpareArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 16);
        addedPart = warehouse.addPart(mockSpareArmor, true);
        assertEquals(mockSpareArmor, addedPart);

        Armor mockUnitArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 16);
        mockUnitArmor.setUnit(createMockUnit());
        addedPart = warehouse.addPart(mockUnitArmor, true);
        assertEquals(mockUnitArmor, addedPart);

        // Spare
        AmmoStorage mockSpareAmmo = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 20);
        addedPart = warehouse.addPart(mockSpareAmmo, true);
        assertEquals(mockSpareAmmo, addedPart);

        AmmoStorage mockRefitAmmo = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 20);
        mockRefitAmmo.setRefitUnit(createMockUnit());
        addedPart = warehouse.addPart(mockRefitAmmo, true);
        assertEquals(mockRefitAmmo, addedPart);

        List<Part> spareParts = warehouse.getSpareParts();
        assertEquals(4, spareParts.size());

        warehouse.forEachSparePart(spare -> {
            assertTrue(spareParts.contains(spare));
        });
    }

    @Test
    public void testFindSparePart() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse warehouse = new Warehouse();

        // The warehouse is empty!
        assertNull(warehouse.findSparePart(spare -> true));

        // Spare
        Part mockSparePart = spy(new MekLocation());
        Part addedPart = warehouse.addPart(mockSparePart, true);
        assertEquals(mockSparePart, warehouse.findSparePart(spare -> spare.getId() == mockSparePart.getId()));

        Part mockUnitPart = spy(new MekLocation());
        mockUnitPart.setUnit(createMockUnit());
        addedPart = warehouse.addPart(mockUnitPart, true);
        assertNull(warehouse.findSparePart(spare -> spare.getId() == mockUnitPart.getId()));

        // Spare (being repaired)
        Part mockSparePartUnderRepair = spy(new MekLocation());
        mockSparePartUnderRepair.setTech(createMockTech());
        addedPart = warehouse.addPart(mockSparePartUnderRepair, true);
        assertEquals(mockSparePartUnderRepair, addedPart);
        assertEquals(mockSparePartUnderRepair, warehouse.findSparePart(spare -> spare.getId() == mockSparePartUnderRepair.getId()));

        Part mockPartForRefit = spy(new MekLocation());
        mockPartForRefit.setRefitUnit(createMockUnit());
        addedPart = warehouse.addPart(mockPartForRefit, true);
        assertEquals(mockPartForRefit, addedPart);
        assertNull(warehouse.findSparePart(spare -> spare.getId() == mockPartForRefit.getId()));

        Part mockPartForRepairTask = spy(new MekLocation());
        mockPartForRepairTask.setReservedBy(createMockTech());
        addedPart = warehouse.addPart(mockPartForRepairTask, true);
        assertEquals(mockPartForRepairTask, addedPart);
        assertNull(warehouse.findSparePart(spare -> spare.getId() == mockPartForRepairTask.getId()));

        // Spare
        Armor mockSpareArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 16);
        addedPart = warehouse.addPart(mockSpareArmor, true);
        assertEquals(mockSpareArmor, addedPart);
        assertEquals(mockSpareArmor, warehouse.findSparePart(spare -> spare.getId() == mockSpareArmor.getId()));

        Armor mockUnitArmor = createMockArmor(mockCampaign, EquipmentType.T_ARMOR_STANDARD, 16);
        mockUnitArmor.setUnit(createMockUnit());
        addedPart = warehouse.addPart(mockUnitArmor, true);
        assertEquals(mockUnitArmor, addedPart);
        assertNull(warehouse.findSparePart(spare -> spare.getId() == mockUnitArmor.getId()));

        // Spare
        AmmoStorage mockSpareAmmo = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 20);
        addedPart = warehouse.addPart(mockSpareAmmo, true);
        assertEquals(mockSpareAmmo, addedPart);
        assertEquals(mockSpareAmmo, warehouse.findSparePart(spare -> spare.getId() == mockSpareAmmo.getId()));

        AmmoStorage mockRefitAmmo = createMockAmmoStorage(mockCampaign, getAmmoType("ISAC5 Ammo"), 20);
        mockRefitAmmo.setRefitUnit(createMockUnit());
        addedPart = warehouse.addPart(mockRefitAmmo, true);
        assertEquals(mockRefitAmmo, addedPart);
        assertNull(warehouse.findSparePart(spare -> spare.getId() == mockRefitAmmo.getId()));

        // Ensure we actually test the predicate, no matter how silly
        assertNull(warehouse.findSparePart(spare -> false));

        // The warehouse full of spare parts, so find (any) one!
        Part sparePart = warehouse.findSparePart(spare -> true);
        assertNotNull(sparePart);
        assertTrue(sparePart.isSpare());
    }

    /**
     * Creates a mock part with the given ID.
     * @param id The unique ID of the part.
     * @return The mocked part with the given ID.
     */
    private Part createMockPart(int id) {
        Part mockPart = mock(Part.class);
        when(mockPart.getId()).thenReturn(id);

        return mockPart;
    }

    /**
     * Creates a mock unit.
     * @return The mock unit.
     */
    private Unit createMockUnit() {
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());
        Mech mockEntity = mock(Mech.class);
        when(mockEntity.getWeight()).thenReturn(0.0); // CAW: match spare parts without unit tonnage.
        when(mockUnit.getEntity()).thenReturn(mockEntity);

        return mockUnit;
    }

    /**
     * Creates a mock tech.
     * @return The mock tech.
     */
    private Person createMockTech() {
        Person mockTech = mock(Person.class);
        when(mockTech.getId()).thenReturn(UUID.randomUUID());
        return mockTech;
    }

    /**
     * Creates mock Armor for the campaign.
     * @param campaign The campaign to assign to the Armor.
     * @param armorType The type of armor.
     * @param points The number of points of armor.
     */
    private Armor createMockArmor(Campaign campaign, int armorType, int points) {
        return spy(new Armor(1, armorType, points, Entity.LOC_NONE, false, false, campaign));
    }

    /**
     * Creates mock AmmoStorage for the campaign.
     * @param campaign The campaign to assign to the AmmoStorage.
     * @param ammoType The type of ammo.
     * @param shots The number of shots ammo.
     */
    private AmmoStorage createMockAmmoStorage(Campaign campaign, AmmoType ammoType, int shots) {
        return spy(new AmmoStorage(1, ammoType, shots, campaign));
    }

    /**
     * Gets an AmmoType by name (performing any initialization required
     * on the MM side).
     * @param name The lookup name for the AmmoType.
     * @return The ammo type for the given name.
     */
    private synchronized static AmmoType getAmmoType(String name) {
        return (AmmoType) EquipmentType.get(name);
    }
}
