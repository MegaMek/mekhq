/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.Vector;

import megamek.common.bays.HeavyVehicleBay;
import megamek.common.bays.LightVehicleBay;
import megamek.common.units.Dropship;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.enums.TransporterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a {@link TransportShipAssignment} keeps its assigned bay type across a save/load cycle, where the
 * transport ship starts out as an unresolved {@link Unit.UnitRef}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class TransportShipAssignmentTest {
    private static final int HEAVY_BAY_NUMBER = 1;
    private static final int LIGHT_BAY_NUMBER = 2;

    private UUID transportId;
    private Unit transport;
    private HeavyVehicleBay heavyVehicleBay;
    private LightVehicleBay lightVehicleBay;

    @BeforeEach
    void setUp() {
        heavyVehicleBay = new HeavyVehicleBay(24, 1, HEAVY_BAY_NUMBER);
        lightVehicleBay = new LightVehicleBay(12, 1, LIGHT_BAY_NUMBER);

        Dropship transportEntity = mock(Dropship.class);
        when(transportEntity.getBayById(HEAVY_BAY_NUMBER)).thenReturn(heavyVehicleBay);
        when(transportEntity.getBayById(LIGHT_BAY_NUMBER)).thenReturn(lightVehicleBay);
        when(transportEntity.getDockingCollars()).thenReturn(new Vector<>());

        transportId = UUID.randomUUID();
        transport = mock(Unit.class);
        when(transport.getId()).thenReturn(transportId);
        when(transport.getEntity()).thenReturn(transportEntity);
    }

    @Test
    void constructorWithRealTransportResolvesBay() {
        TransportShipAssignment assignment = new TransportShipAssignment(transport, LIGHT_BAY_NUMBER);

        assertSame(lightVehicleBay, assignment.getTransportedLocation());
        assertEquals(TransporterType.LIGHT_VEHICLE_BAY, assignment.getTransporterType());
    }

    @Test
    void constructorWithUnitRefLeavesBayUnresolved() {
        TransportShipAssignment assignment = new TransportShipAssignment(new Unit.UnitRef(transportId),
              LIGHT_BAY_NUMBER);

        assertNull(assignment.getTransportedLocation());
        assertFalse(assignment.hasTransporterType());
    }

    @Test
    void fixReferencesResolvesBayAfterLoad() {
        TransportShipAssignment assignment = new TransportShipAssignment(new Unit.UnitRef(transportId),
              LIGHT_BAY_NUMBER);
        Campaign campaign = mockCampaignWithTransport(transport);
        Unit transportedUnit = mock(Unit.class);

        assignment.fixReferences(campaign, transportedUnit);

        assertSame(transport, assignment.getTransportShip());
        assertEquals(LIGHT_BAY_NUMBER, assignment.getBayNumber());
        assertSame(lightVehicleBay, assignment.getTransportedLocation());
        assertEquals(TransporterType.LIGHT_VEHICLE_BAY, assignment.getTransporterType());
        verify(transportedUnit, never()).setTransportShipAssignment(null);
    }

    @Test
    void fixReferencesKeepsDistinctBayTypesAfterLoad() {
        TransportShipAssignment heavyAssignment = new TransportShipAssignment(new Unit.UnitRef(transportId),
              HEAVY_BAY_NUMBER);
        TransportShipAssignment lightAssignment = new TransportShipAssignment(new Unit.UnitRef(transportId),
              LIGHT_BAY_NUMBER);
        Campaign campaign = mockCampaignWithTransport(transport);

        heavyAssignment.fixReferences(campaign, mock(Unit.class));
        lightAssignment.fixReferences(campaign, mock(Unit.class));

        assertEquals(TransporterType.HEAVY_VEHICLE_BAY, heavyAssignment.getTransporterType());
        assertEquals(TransporterType.LIGHT_VEHICLE_BAY, lightAssignment.getTransporterType());
    }

    @Test
    void fixReferencesIsSafeToCallTwice() {
        TransportShipAssignment assignment = new TransportShipAssignment(new Unit.UnitRef(transportId),
              LIGHT_BAY_NUMBER);
        Campaign campaign = mockCampaignWithTransport(transport);
        Unit transportedUnit = mock(Unit.class);

        assignment.fixReferences(campaign, transportedUnit);
        assignment.fixReferences(campaign, transportedUnit);

        assertSame(transport, assignment.getTransportShip());
        assertEquals(TransporterType.LIGHT_VEHICLE_BAY, assignment.getTransporterType());
    }

    @Test
    void fixReferencesClearsAssignmentWhenTransportMissing() {
        TransportShipAssignment assignment = new TransportShipAssignment(new Unit.UnitRef(transportId),
              LIGHT_BAY_NUMBER);
        Campaign campaign = mockCampaignWithTransport(null);
        Unit transportedUnit = mock(Unit.class);

        assignment.fixReferences(campaign, transportedUnit);

        verify(transportedUnit).setTransportShipAssignment(null);
    }

    private Campaign mockCampaignWithTransport(Unit hangarTransport) {
        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaign.getPlayerForce().getHangar().getUnit(transportId)).thenReturn(hangarTransport);
        return campaign;
    }
}
