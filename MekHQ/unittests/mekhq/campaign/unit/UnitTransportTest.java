/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import megamek.common.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class UnitTransportTest {

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @Test
    public void basicTransportedUnits() {
        Unit transport = new Unit();

        // We start with empty transport bays
        assertFalse(transport.hasTransportedUnits());
        assertNotNull(transport.getTransportedUnits());
        assertTrue(transport.getTransportedUnits().isEmpty());

        // Create a fake unit to transport
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());

        // Add a transported unit
        transport.addTransportedUnit(mockUnit);

        // Now we should have units
        assertTrue(transport.hasTransportedUnits());
        assertEquals(1, transport.getTransportedUnits().size());
        assertTrue(transport.getTransportedUnits().contains(mockUnit));

        // Adding the same unit again...
        transport.addTransportedUnit(mockUnit);

        // ... should leave everything the same.
        assertTrue(transport.hasTransportedUnits());
        assertEquals(1, transport.getTransportedUnits().size());
        assertTrue(transport.getTransportedUnits().contains(mockUnit));

        Unit mockOtherUnit = mock(Unit.class);
        when(mockOtherUnit.getId()).thenReturn(UUID.randomUUID());

        // We should not be able to remove an unknown unit
        transport.removeTransportedUnit(mockOtherUnit);

        // But we can add at least one more unit...
        transport.addTransportedUnit(mockOtherUnit);

        assertTrue(transport.hasTransportedUnits());
        assertEquals(2, transport.getTransportedUnits().size());
        assertTrue(transport.getTransportedUnits().contains(mockUnit));
        assertTrue(transport.getTransportedUnits().contains(mockOtherUnit));

        // ... and removing the first...
        assertTrue(transport.removeTransportedUnit(mockUnit));

        // ... should leave us with just that one other unit.
        assertTrue(transport.hasTransportedUnits());
        assertEquals(1, transport.getTransportedUnits().size());
        assertTrue(transport.getTransportedUnits().contains(mockOtherUnit));

        // ... and clearing out our transport bays...
        transport.clearTransportedUnits();

        // ... should leave us empty again.
        assertFalse(transport.hasTransportedUnits());
        assertNotNull(transport.getTransportedUnits());
        assertTrue(transport.getTransportedUnits().isEmpty());
    }

    @Test
    public void isCarryingAeroAndGround() {
        Unit transport = new Unit();

        // No units? No aeros.
        assertFalse(transport.hasTransportedUnits());
        assertFalse(transport.isCarryingSmallerAero());
        assertFalse(transport.isCarryingGround());

        // Add a ground unit
        Entity mockGroundEntity = mock(Mech.class);
        when(mockGroundEntity.isAero()).thenReturn(false);
        Unit mockGroundUnit = mock(Unit.class);
        when(mockGroundUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockGroundUnit.getEntity()).thenReturn(mockGroundEntity);

        transport.addTransportedUnit(mockGroundUnit);

        // No aeros, just a ground unit
        assertTrue(transport.hasTransportedUnits());
        assertFalse(transport.isCarryingSmallerAero());
        assertTrue(transport.isCarryingGround());

        // Add an aero unit
        Entity mockAeroEntity = mock(Aero.class);
        when(mockAeroEntity.isAero()).thenReturn(true);
        Unit mockAeroUnit = mock(Unit.class);
        when(mockAeroUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockAeroUnit.getEntity()).thenReturn(mockAeroEntity);

        transport.addTransportedUnit(mockAeroUnit);

        // Now we have an areo
        assertTrue(transport.hasTransportedUnits());
        assertTrue(transport.isCarryingSmallerAero());
        assertTrue(transport.isCarryingGround());

        // Removing the ground unit should not affect our aero calculation
        transport.removeTransportedUnit(mockGroundUnit);

        assertTrue(transport.hasTransportedUnits());
        assertTrue(transport.isCarryingSmallerAero());
        assertFalse(transport.isCarryingGround());
    }

    @Test
    public void testUnitTypeForAerosMatchesAeroBayType() {
        // Create a fake entity to back the real transport Unit
        Dropship mockVengeance = mock(Dropship.class);
        Unit transport = new Unit();
        ASFBay mockASFBay = mock(ASFBay.class);
        when(mockASFBay.getCapacity()).thenReturn(100.0);
        transport.setEntity(mockVengeance);

        // Initialize bays
        Vector<Bay> bays = new Vector<>();
        bays.add(mockASFBay);
        when(mockVengeance.getTransportBays()).thenReturn(bays);
        transport.initializeBaySpace();

        // Add an aero unit
        Entity aero = new AeroSpaceFighter();
        Unit mockAeroUnit = mock(Unit.class);
        when(mockAeroUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockAeroUnit.getEntity()).thenReturn(aero);

        // Verify the AeroSpaceFighter is recognized as a valid ASFBay occupant
        double remainingCap = transport.getCorrectBayCapacity(aero.getUnitType(), 50);
        assertEquals(100.0, remainingCap);
    }

    @Test
    public void unloadFromTransportShipDoesNothingIfNotLoaded() {
        Unit transport = spy(new Unit());

        Unit randomUnit = mock(Unit.class);
        when(randomUnit.hasTransportShipAssignment()).thenReturn(false);

        // Try removing a ship that's not on our transport.
        transport.removeTransportedUnit(randomUnit);

        // The unit should NOT have its assignment changed.
        verify(randomUnit, times(0)).setTransportShipAssignment(eq(null));

        // And we should not have had our bay space recalculated.
        verify(transport, times(0)).updateBayCapacity(anyInt(), anyDouble(),
                anyBoolean(), anyInt());
    }

    @Test
    public void unloadFromTransportShipDoesNothingIfLoadedOnAnotherShip() {
        Unit transport0 = spy(new Unit());

        Unit transport1 = mock(Unit.class);
        Unit randomUnit = mock(Unit.class);
        when(randomUnit.hasTransportShipAssignment()).thenReturn(true);
        when(randomUnit.getTransportShipAssignment()).thenReturn(new TransportShipAssignment(transport1, 0));

        // Try removing a ship that's on somebody else's transport
        transport0.removeTransportedUnit(randomUnit);

        // The unit should NOT have its assignment changed.
        verify(randomUnit, times(0)).setTransportShipAssignment(eq(null));

        // And we should not have had our bay space recalculated.
        verify(transport0, times(0)).updateBayCapacity(anyInt(), anyDouble(),
                anyBoolean(), anyInt());
    }
}
