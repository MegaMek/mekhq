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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.Vector;

import megamek.common.*;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

//

/**
 * Psi - Transport has been split into Ship & Tactical transport.
 *
 * @see UnitTransportTest
 */
public class LegacyUnitShipTransportTest {

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();


    }

    @Test
    public void basicTransportedUnits() {
        Game mockGame = mock(Game.class);
        Campaign mockCampaign = mock(Campaign.class);


        Unit transport = new Unit();
        when(mockCampaign.getGame()).thenReturn(mockGame);
        mockCampaign.importUnit(transport);


        // We start with empty transport bays
        assertFalse(transport.hasShipTransportedUnits());
        assertNotNull(transport.getShipTransportedUnits());
        assertTrue(transport.getShipTransportedUnits().isEmpty());

        // Create a fake unit to transport
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());

        // Add a transported unit
        transport.addShipTransportedUnit(mockUnit);

        // Now we should have units
        assertTrue(transport.hasShipTransportedUnits());
        assertEquals(1, transport.getShipTransportedUnits().size());
        assertTrue(transport.getShipTransportedUnits().contains(mockUnit));

        // Adding the same unit again...
        transport.addShipTransportedUnit(mockUnit);

        // ... should leave everything the same.
        assertTrue(transport.hasShipTransportedUnits());
        assertEquals(1, transport.getShipTransportedUnits().size());
        assertTrue(transport.getShipTransportedUnits().contains(mockUnit));

        Unit mockOtherUnit = mock(Unit.class);
        when(mockOtherUnit.getId()).thenReturn(UUID.randomUUID());

        // We should not be able to remove an unknown unit
        transport.removeShipTransportedUnit(mockOtherUnit);

        // But we can add at least one more unit...
        transport.addShipTransportedUnit(mockOtherUnit);

        assertTrue(transport.hasShipTransportedUnits());
        assertEquals(2, transport.getShipTransportedUnits().size());
        assertTrue(transport.getShipTransportedUnits().contains(mockUnit));
        assertTrue(transport.getShipTransportedUnits().contains(mockOtherUnit));

        // ... and removing the first...
        assertTrue(transport.removeShipTransportedUnit(mockUnit));

        // ... should leave us with just that one other unit.
        assertTrue(transport.hasShipTransportedUnits());
        assertEquals(1, transport.getShipTransportedUnits().size());
        assertTrue(transport.getShipTransportedUnits().contains(mockOtherUnit));

        // ... and clearing out our transport bays...
        transport.clearShipTransportedUnits();

        // ... should leave us empty again.
        assertFalse(transport.hasShipTransportedUnits());
        assertNotNull(transport.getShipTransportedUnits());
        assertTrue(transport.getShipTransportedUnits().isEmpty());
    }

    @Test
    public void isCarryingAeroAndGround() {
        Unit transport = new Unit();

        // No units? No aeros.
        assertFalse(transport.hasShipTransportedUnits());
        assertFalse(transport.isCarryingSmallerAero());
        assertFalse(transport.isCarryingGround());

        // Add a ground unit
        Entity mockGroundEntity = mock(Mek.class);
        when(mockGroundEntity.isAero()).thenReturn(false);
        Unit mockGroundUnit = mock(Unit.class);
        when(mockGroundUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockGroundUnit.getEntity()).thenReturn(mockGroundEntity);

        transport.addShipTransportedUnit(mockGroundUnit);

        // No aeros, just a ground unit
        assertTrue(transport.hasShipTransportedUnits());
        assertFalse(transport.isCarryingSmallerAero());
        assertTrue(transport.isCarryingGround());

        // Add an aero unit
        Entity mockAeroEntity = mock(Aero.class);
        when(mockAeroEntity.isAero()).thenReturn(true);
        Unit mockAeroUnit = mock(Unit.class);
        when(mockAeroUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockAeroUnit.getEntity()).thenReturn(mockAeroEntity);

        transport.addShipTransportedUnit(mockAeroUnit);

        // Now we have an areo
        assertTrue(transport.hasShipTransportedUnits());
        assertTrue(transport.isCarryingSmallerAero());
        assertTrue(transport.isCarryingGround());

        // Removing the ground unit should not affect our aero calculation
        transport.removeShipTransportedUnit(mockGroundUnit);

        assertTrue(transport.hasShipTransportedUnits());
        assertTrue(transport.isCarryingSmallerAero());
        assertFalse(transport.isCarryingGround());
    }

    @Test
    public void testUnitTypeForAerosMatchesAeroBayType() {
        Campaign campaign = mock(Campaign.class);

        // Create a fake entity to back the real transport Unit
        Dropship mockVengeance = mock(Dropship.class);
        Unit transport = new Unit(mockVengeance, campaign);
        ASFBay mockASFBay = new ASFBay(100, 1, 0);

        // Initialize bays
        Vector<Bay> bays = new Vector<>();
        bays.add(mockASFBay);
        Vector<Transporter> transporters = new Vector<>();
        transporters.add(mockASFBay);
        when(mockVengeance.getTransportBays()).thenReturn(bays);
        when(mockVengeance.getTransports()).thenReturn(transporters);
        mockASFBay.setGame(mock(Game.class));
        transport.initializeShipTransportSpace();

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
        transport.removeShipTransportedUnit(randomUnit);

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
        TransportShipAssignment transportAssignment = mock(TransportShipAssignment.class);
        when(randomUnit.hasTransportShipAssignment()).thenReturn(true);
        when(randomUnit.getTransportShipAssignment()).thenReturn(transportAssignment);
        when(transportAssignment.getTransport()).thenReturn(transport1);

        // Try removing a ship that's on somebody else's transport
        transport0.removeShipTransportedUnit(randomUnit);

        // The unit should NOT have its assignment changed.
        verify(randomUnit, times(0)).setTransportShipAssignment(eq(null));

        // And we should not have had our bay space recalculated.
        verify(transport0, times(0)).updateBayCapacity(anyInt(), anyDouble(),
              anyBoolean(), anyInt());
    }
}
