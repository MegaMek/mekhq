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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign;

import org.junit.Test;

import mekhq.EventSpy;
import mekhq.campaign.event.PartNewEvent;
import mekhq.campaign.parts.Part;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

        // ...that is not the same as our previous part
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
}
