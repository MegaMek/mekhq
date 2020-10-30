/*
 * UnitTransportTest.java
 *
 * Copyright (c) 2020 The Megamek Team. All rights reserved.
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.Test;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.Mech;

public class UnitTransportTest {
    @Test
    public void basicTransportedUnits() {
        Unit transport = new Unit();

        // We start with empty transport bays
        assertFalse(transport.hasTransportedUnits());
        assertNotNull(transport.getTransportedUnits());
        assertTrue(transport.getTransportedUnits().isEmpty());

        // Create a fake unit to transprot
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

        // ...should leave everything the same.
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

        // ...and removing the first...
        assertTrue(transport.removeTransportedUnit(mockUnit));

        // ...should leave us with just that one other unit.
        assertTrue(transport.hasTransportedUnits());
        assertEquals(1, transport.getTransportedUnits().size());
        assertTrue(transport.getTransportedUnits().contains(mockOtherUnit));

        // ...and clearing out our transport bays...
        transport.clearTransportedUnits();

        // ...should leave us empty again.
        assertFalse(transport.hasTransportedUnits());
        assertNotNull(transport.getTransportedUnits());
        assertTrue(transport.getTransportedUnits().isEmpty());
    }

    @Test
    public void isCarryingAeroAndGround() {
        Unit transport = new Unit();

        // No units? No aeros.
        assertFalse(transport.hasTransportedUnits());
        assertFalse(transport.isCarryingAero());
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
        assertFalse(transport.isCarryingAero());
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
        assertTrue(transport.isCarryingAero());
        assertTrue(transport.isCarryingGround());

        // Removing the ground unit should not affect our aero calculation
        transport.removeTransportedUnit(mockGroundUnit);

        assertTrue(transport.hasTransportedUnits());
        assertTrue(transport.isCarryingAero());
        assertFalse(transport.isCarryingGround());
    }
}
