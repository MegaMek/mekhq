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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import mekhq.MekHQ;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationDispatch;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class CampaignLocationManagerTest {

    @Test
    void queueTravel_refusesNullDestination() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Unit unit = mock(Unit.class);

        manager.queueTravel(List.of(unit), null);

        assertFalse(manager.isQueuedForTravel(unit));
    }

    @Test
    void isQueuedForTravel_trueWhileQueued() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Campaign campaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);

        manager.queueTravel(List.of(unit), campaign);

        assertTrue(manager.isQueuedForTravel(unit));
    }

    @Test
    void dispatchPendingTravel_skipsTravelersRemovedFromCampaign() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Campaign campaign = mock(Campaign.class);
        Unit soldUnit = mock(Unit.class);
        UUID unitId = UUID.randomUUID();
        when(soldUnit.getId()).thenReturn(unitId);
        when(campaign.getUnit(unitId)).thenReturn(null);

        manager.queueTravel(List.of(soldUnit), campaign);

        try (MockedStatic<LocationDispatch> dispatch = mockStatic(LocationDispatch.class)) {
            manager.dispatchPendingTravel(campaign);
            dispatch.verifyNoInteractions();
        }
        assertFalse(manager.isQueuedForTravel(soldUnit));
    }

    @Test
    void dispatchPendingTravel_dispatchesTravelersStillInCampaign() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Campaign campaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);
        when(campaign.getUnit(unitId)).thenReturn(unit);

        manager.queueTravel(List.of(unit), campaign);

        try (MockedStatic<LocationDispatch> dispatch = mockStatic(LocationDispatch.class)) {
            manager.dispatchPendingTravel(campaign);
            dispatch.verify(() -> LocationDispatch.dispatchTravelers(List.of(unit), campaign, campaign));
        }
        assertFalse(manager.isQueuedForTravel(unit));
    }

    @Test
    void queueTravel_reQueueRemovesPriorPendingTravel() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Campaign campaign = mock(Campaign.class);
        ILocation firstDestination = mock(ILocation.class);
        ILocation secondDestination = mock(ILocation.class);
        Unit unit = mock(Unit.class);
        UUID unitId = UUID.randomUUID();
        when(unit.getId()).thenReturn(unitId);
        when(campaign.getUnit(unitId)).thenReturn(unit);

        manager.queueTravel(List.of(unit), firstDestination);
        manager.queueTravel(List.of(unit), secondDestination);

        try (MockedStatic<LocationDispatch> dispatch = mockStatic(LocationDispatch.class)) {
            manager.dispatchPendingTravel(campaign);
            dispatch.verify(() -> LocationDispatch.dispatchTravelers(List.of(unit), secondDestination, campaign));
            dispatch.verify(() -> LocationDispatch.dispatchTravelers(List.of(unit), firstDestination, campaign),
                  never());
        }
    }

    @Test
    void gmTeleport_dispatchesThenProcessesArrivals() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Campaign campaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);

        try (MockedStatic<LocationDispatch> dispatch = mockStatic(LocationDispatch.class)) {
            manager.gmTeleport(campaign, List.of(unit), campaign);
            dispatch.verify(() -> LocationDispatch.dispatchTravelers(List.of(unit), campaign, campaign));
        }
        verify(campaign).processArrivals(campaign);
    }

    @Test
    void gmCompleteTravel_startsQueuedItemToItsDestination() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Campaign campaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);

        // Queue the unit to travel to the campaign (used here as the destination ILocation).
        manager.queueTravel(List.of(unit), campaign);

        try (MockedStatic<LocationDispatch> dispatch = mockStatic(LocationDispatch.class)) {
            manager.gmCompleteTravel(campaign, List.of(unit));
            dispatch.verify(() -> LocationDispatch.dispatchTravelers(List.of(unit), campaign, campaign));
        }
        assertFalse(manager.isQueuedForTravel(unit));
        verify(campaign).processArrivals(campaign);
    }

    @Test
    void gmCompleteTravel_noopWhenNothingTravelingOrQueued() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Campaign campaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);

        try (MockedStatic<LocationDispatch> dispatch = mockStatic(LocationDispatch.class)) {
            manager.gmCompleteTravel(campaign, List.of(unit));
            dispatch.verifyNoInteractions();
        }
        verify(campaign).processArrivals(campaign);
    }

    @Test
    void gmCompleteTravel_forcesInTransitNodeToArrive() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Campaign campaign = mock(Campaign.class);
        Unit unit = mock(Unit.class);
        CurrentLocation travelNode = mock(CurrentLocation.class);
        when(unit.getCurrentLocation()).thenReturn(travelNode);
        when(travelNode.hasArrived()).thenReturn(false);

        try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
            manager.gmCompleteTravel(campaign, List.of(unit));
        }

        verify(travelNode).setTransitTime(0);
        verify(travelNode).setJumpPath(null);
        verify(campaign).processArrivals(campaign);
    }

    @Test
    void gmCompleteTravel_refreshesUnselectedCoTravelers() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Campaign campaign = mock(Campaign.class);
        Unit selectedUnit = mock(Unit.class);
        Unit coTraveler = mock(Unit.class);
        CurrentLocation travelNode = mock(CurrentLocation.class);
        when(selectedUnit.getCurrentLocation()).thenReturn(travelNode);
        when(travelNode.hasArrived()).thenReturn(false);
        when(travelNode.fetchPersonnelAtLocation()).thenReturn(Set.of());
        when(travelNode.fetchUnitsAtLocation()).thenReturn(Set.of(selectedUnit, coTraveler));
        when(travelNode.fetchPartsAtLocation()).thenReturn(Set.of());

        try (MockedStatic<MekHQ> mekhq = mockStatic(MekHQ.class)) {
            manager.gmCompleteTravel(campaign, List.of(selectedUnit));

            // The co-traveler, though not selected, arrives with the node and must get a refresh event too.
            mekhq.verify(() -> MekHQ.triggerEvent(argThat(
                  event -> event instanceof UnitChangedEvent changed && changed.getUnit() == coTraveler)));
        }
    }

    @Test
    void countUnselectedCoTravelers_countsOnlyItemsNotInSelection() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Unit selectedUnit = mock(Unit.class);
        Unit coTraveler = mock(Unit.class);
        CurrentLocation travelNode = mock(CurrentLocation.class);
        when(selectedUnit.getCurrentLocation()).thenReturn(travelNode);
        when(travelNode.hasArrived()).thenReturn(false);
        when(travelNode.fetchPersonnelAtLocation()).thenReturn(Set.of());
        when(travelNode.fetchUnitsAtLocation()).thenReturn(Set.of(selectedUnit, coTraveler));
        when(travelNode.fetchPartsAtLocation()).thenReturn(Set.of());

        assertEquals(1, manager.countUnselectedCoTravelers(List.of(selectedUnit)));
    }

    @Test
    void countUnselectedCoTravelers_zeroWhenNotTraveling() {
        CampaignLocationManager manager = new CampaignLocationManager();
        Unit unit = mock(Unit.class);

        assertEquals(0, manager.countUnselectedCoTravelers(List.of(unit)));
    }
}
