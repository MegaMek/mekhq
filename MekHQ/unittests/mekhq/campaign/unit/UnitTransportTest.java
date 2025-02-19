/*
 * Copyright (c) 2020-2025 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UnitTransportTest {

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();


    }

    @ParameterizedTest
    @EnumSource(value = CampaignTransportType.class)
    public void basicTransportedUnits(CampaignTransportType campaignTransportType) {
        Game mockGame = mock(Game.class);
        Campaign mockCampaign = mock(Campaign.class);


        Unit transport = new Unit();
        when(mockCampaign.getGame()).thenReturn(mockGame);
        mockCampaign.importUnit(transport);



        // We start with empty transport bays
        assertFalse(transport.hasTransportedUnits(campaignTransportType));
        assertNotNull(transport.getTransportedUnits(campaignTransportType));
        assertTrue(transport.getTransportedUnits(campaignTransportType).isEmpty());

        // Create a fake unit to transport
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());

        // Add a transported unit
        transport.addTransportedUnit(campaignTransportType, mockUnit);

        // Now we should have units
        assertTrue(transport.hasTransportedUnits(campaignTransportType));
        assertEquals(1, transport.getTransportedUnits(campaignTransportType).size());
        assertTrue(transport.getTransportedUnits(campaignTransportType).contains(mockUnit));

        // Adding the same unit again...
        transport.addTransportedUnit(campaignTransportType, mockUnit);

        // ... should leave everything the same.
        assertTrue(transport.hasTransportedUnits(campaignTransportType));
        assertEquals(1, transport.getTransportedUnits(campaignTransportType).size());
        assertTrue(transport.getTransportedUnits(campaignTransportType).contains(mockUnit));

        Unit mockOtherUnit = mock(Unit.class);
        when(mockOtherUnit.getId()).thenReturn(UUID.randomUUID());

        // We should not be able to remove an unknown unit
        transport.removeTransportedUnit(campaignTransportType, mockOtherUnit);

        // But we can add at least one more unit...
        transport.addTransportedUnit(campaignTransportType, mockOtherUnit);

        assertTrue(transport.hasTransportedUnits(campaignTransportType));
        assertEquals(2, transport.getTransportedUnits(campaignTransportType).size());
        assertTrue(transport.getTransportedUnits(campaignTransportType).contains(mockUnit));
        assertTrue(transport.getTransportedUnits(campaignTransportType).contains(mockOtherUnit));

        // ... and removing the first...
        assertTrue(transport.removeTransportedUnit(campaignTransportType, mockUnit));

        // ... should leave us with just that one other unit.
        assertTrue(transport.hasTransportedUnits(campaignTransportType));
        assertEquals(1, transport.getTransportedUnits(campaignTransportType).size());
        assertTrue(transport.getTransportedUnits(campaignTransportType).contains(mockOtherUnit));

        // ... and clearing out our transport bays...
        transport.clearTransportedUnits(campaignTransportType);

        // ... should leave us empty again.
        assertFalse(transport.hasTransportedUnits(campaignTransportType));
        assertNotNull(transport.getTransportedUnits(campaignTransportType));
        assertTrue(transport.getTransportedUnits(campaignTransportType).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = CampaignTransportType.class)
    public void isCarryingAeroAndGround(CampaignTransportType campaignTransportType) {
        Unit transport = new Unit();

        // No units? No aeros.
        assertFalse(transport.hasTransportedUnits(campaignTransportType));

        // Add a ground unit
        Entity mockGroundEntity = mock(Mek.class);
        when(mockGroundEntity.isAero()).thenReturn(false);
        Unit mockGroundUnit = mock(Unit.class);
        when(mockGroundUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockGroundUnit.getEntity()).thenReturn(mockGroundEntity);

        transport.addTransportedUnit(campaignTransportType, mockGroundUnit);

        // No aeros, just a ground unit
        assertTrue(transport.hasTransportedUnits(campaignTransportType));

        // Add an aero unit
        Entity mockAeroEntity = mock(Aero.class);
        when(mockAeroEntity.isAero()).thenReturn(true);
        Unit mockAeroUnit = mock(Unit.class);
        when(mockAeroUnit.getId()).thenReturn(UUID.randomUUID());
        when(mockAeroUnit.getEntity()).thenReturn(mockAeroEntity);

        transport.addTransportedUnit(campaignTransportType, mockAeroUnit);

        // Now we have an areo
        assertTrue(transport.hasTransportedUnits(campaignTransportType));

        // Removing the ground unit should not affect our aero calculation
        transport.removeTransportedUnit(campaignTransportType, mockGroundUnit);

        assertTrue(transport.hasTransportedUnits(campaignTransportType));
    }

    @ParameterizedTest
    @EnumSource(value = CampaignTransportType.class)
    public void testUnitTypeForAerosMatchesAeroBayType() {
        Campaign campaign = mock(Campaign.class);

        // Create a fake entity to back the real transport Unit
        Dropship mockVengeance = mock(Dropship.class);
        Unit transport = new Unit(mockVengeance,campaign);
        ASFBay mockASFBay = new ASFBay(100, 1 ,0);

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

    @ParameterizedTest
    @EnumSource(value = CampaignTransportType.class)
    public void unloadFromTransportDoesNothingIfNotLoaded(CampaignTransportType campaignTransportType) {
        Unit transport = spy(new Unit());

        Unit randomUnit = mock(Unit.class);
        when(randomUnit.hasTransportAssignment(campaignTransportType)).thenReturn(false);

        // Try removing a ship that's not on our transport.
        transport.removeTransportedUnit(campaignTransportType, randomUnit);

        // The unit should NOT have its assignment changed.
        verify(randomUnit, times(0)).setTransportAssignment(eq(campaignTransportType), eq(null));

        // And we should not have had our bay space recalculated.
        verify(transport, times(0)).initializeTransportSpace(campaignTransportType);
    }

    @ParameterizedTest
    @EnumSource(value = CampaignTransportType.class)
    public void unloadFromTransportDoesNothingIfLoadedOnAnotherTransport(CampaignTransportType campaignTransportType) {
        Unit transport0 = spy(new Unit());

        Unit transport1 = mock(Unit.class);
        Unit randomUnit = mock(Unit.class);
        ITransportAssignment transportAssignment = mock(campaignTransportType.getTransportAssignmentType());
        when(randomUnit.hasTransportAssignment(eq(campaignTransportType))).thenReturn(true);
        when(randomUnit.getTransportAssignment(eq(campaignTransportType))).thenReturn(transportAssignment);
        when(transportAssignment.getTransport()).thenReturn(transport1);

        // Try removing a ship that's on somebody else's transport
        transport0.removeTransportedUnit(campaignTransportType, randomUnit);

        // The unit should NOT have its assignment changed.
        verify(randomUnit, times(0)).setTransportAssignment(eq(campaignTransportType), eq(null));

        // And we should not have had our bay space recalculated.
        verify(transport0, times(0)).initializeTransportSpace(campaignTransportType);
    }
}
