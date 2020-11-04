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
import org.mockito.ArgumentCaptor;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.Mech;
import mekhq.EventSpy;
import mekhq.campaign.event.PartArrivedEvent;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.PartNewEvent;
import mekhq.campaign.event.PartRemovedEvent;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class QuartermasterTest {
    @Test
    public void addPartDoesntAddTestUnitParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);
        TestUnit mockUnit = mock(TestUnit.class);
        when(mockPart.getUnit()).thenReturn(mockUnit);

        // Add a part on a test unit...
        quartermaster.addPart(mockPart, 0);

        // ...and verify we never put it in the warehouse.
        verify(mockWarehouse, times(0)).addPart(eq(mockPart), anyBoolean());
    }

    @Test
    public void addPartDoesntAddSpareMissingParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        MissingPart mockPart = mock(MissingPart.class);

        // Add a spare missing part...
        quartermaster.addPart(mockPart, 0);

        // ...and verify we never put it in the warehouse.
        verify(mockWarehouse, times(0)).addPart(eq(mockPart), anyBoolean());
    }

    @Test
    public void addPartTransitDaysNeverNegative() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);
        doReturn(mockPart).when(mockWarehouse).addPart(eq(mockPart), eq(true));

        // Add a spare missing part...
        quartermaster.addPart(mockPart, -10);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(mockPart, times(1)).setDaysToArrival(captor.capture());

        assertEquals(Integer.valueOf(0), captor.getValue());
    }

    @Test
    public void addPartPlacesSparePartInWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);
        doReturn(mockPart).when(mockWarehouse).addPart(eq(mockPart), eq(true));

        // Add a part...
        quartermaster.addPart(mockPart, 1);

        // ...and ensure it is added to the warehouse (with merging!)
        verify(mockWarehouse, times(1)).addPart(eq(mockPart), eq(true));
    }

    @Test
    public void addPartPlacesUnitPartInWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);
        Unit mockUnit = mock(Unit.class);
        when(mockPart.getUnit()).thenReturn(mockUnit);
        doReturn(mockPart).when(mockWarehouse).addPart(eq(mockPart), eq(true));

        // Add a part on an actual unit...
        quartermaster.addPart(mockPart, 0);

        // ...and ensure it is added to the warehouse (with merging!)
        verify(mockWarehouse, times(1)).addPart(eq(mockPart), eq(true));
    }

    @Test
    public void addPartPlacesUnitMissingPartInWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        MissingPart mockPart = mock(MissingPart.class);
        Unit mockUnit = mock(Unit.class);
        when(mockPart.getUnit()).thenReturn(mockUnit);

        // Add a missing part on an actual unit...
        quartermaster.addPart(mockPart, 0);

        // ...and ensure it is added to the warehouse (with merging!)
        verify(mockWarehouse, times(1)).addPart(eq(mockPart), eq(true));
    }

    @Test
    public void arrivePartDoesNothingForUnitParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);
        Unit mockUnit = mock(Unit.class);
        when(mockPart.getUnit()).thenReturn(mockUnit);

        // Arrive a part on a unit...
        quartermaster.arrivePart(mockPart);

        // ...and ensure it is never added to the warehouse.
        verify(mockWarehouse, times(0)).addPart(eq(mockPart), eq(true));
    }

    @Test
    public void arrivePartSetsDaysToArrivalToZero() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);

        // Arrive a part...
        quartermaster.arrivePart(mockPart);
        doReturn(mockPart).when(mockWarehouse).addPart(eq(mockPart), eq(true));

        // ...and ensure it is is now 'present'.
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(mockPart, times(1)).setDaysToArrival(captor.capture());

        // '0' days until arrival is 'present'
        assertEquals(Integer.valueOf(0), captor.getValue());
    }

    @Test
    public void arrivePartPlacesPartInWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);
        doReturn(mockPart).when(mockWarehouse).addPart(eq(mockPart), eq(true));

        // Arrive a part...
        quartermaster.arrivePart(mockPart);

        // ...and ensure it is added to the warehouse.
        verify(mockWarehouse, times(1)).addPart(eq(mockPart), eq(true));
    }

    @Test
    public void arrivePartNotifiesPartArrival() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);
        String arrivalReport = "Test Arrival Report";
        when(mockPart.getArrivalReport()).thenReturn(arrivalReport);
        doReturn(mockPart).when(mockWarehouse).addPart(eq(mockPart), eq(true));

        try (EventSpy eventSpy = new EventSpy()) {
            // Arrive a part...
            quartermaster.arrivePart(mockPart);

            // ...and see that we put in a report the part arrived...
            verify(mockCampaign, times(1)).addReport(eq(arrivalReport));

            // ...and make sure we got a notification!
            assertNotNull(eventSpy.findEvent(PartArrivedEvent.class, e -> e.getPart() == mockPart));
        }
    }

    @Test
    public void buyUnitAddsUnconditionallyIfNotPayingForUnits() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we don't pay for units...
        when(mockOptions.payForUnits()).thenReturn(false);

        // ...then we should automatically buy a unit...
        Entity mockEntity = mock(Entity.class);
        int transitDays = 10;
        assertTrue(quartermaster.buyUnit(mockEntity, transitDays));

        // ...and the new unit should be added to the campaign.
        verify(mockCampaign, times(1)).addNewUnit(eq(mockEntity), eq(false), eq(transitDays));
    }

    @Test
    public void buyUnitReturnsFalseIfOutOfCash() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for units...
        when(mockOptions.payForUnits()).thenReturn(true);

        // ...but can't afford a unit...
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        doReturn(false).when(mockFinances).debit(any(), anyInt(), anyString(), any());

        Entity mockEntity = mock(Entity.class);
        doReturn(1.0).when(mockEntity).getCost(anyBoolean());

        // ...then we should not be able to buy the unit...
        assertFalse(quartermaster.buyUnit(mockEntity, 0));

        // ...and the new unit should NOT be added to the campaign.
        verify(mockCampaign, times(0)).addNewUnit(eq(mockEntity), eq(false), eq(0));
    }

    @Test
    public void buyUnitBuysAUnitIfWeCanAffordIt() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for units...
        when(mockOptions.payForUnits()).thenReturn(true);

        // ...and can afford a unit...
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        ArgumentCaptor<Money> captor = ArgumentCaptor.forClass(Money.class);
        doReturn(true).when(mockFinances).debit(captor.capture(), anyInt(), anyString(), any());

        Entity mockEntity = mock(Entity.class);
        double cost = 1.0;
        doReturn(cost).when(mockEntity).getCost(anyBoolean());

        // ...then we should be able to buy the unit...
        assertTrue(quartermaster.buyUnit(mockEntity, 0));

        // ...and the new unit should be added to the campaign...
        verify(mockCampaign, times(1)).addNewUnit(eq(mockEntity), eq(false), eq(0));

        // ...and it should cost the right amount.
        assertEquals(Money.of(cost), captor.getValue());
    }

    @Test
    public void buyUnitBuysInfantryUsingAlternateCost() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for units...
        when(mockOptions.payForUnits()).thenReturn(true);

        // ...and can afford a unit...
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        ArgumentCaptor<Money> captor = ArgumentCaptor.forClass(Money.class);
        doReturn(true).when(mockFinances).debit(captor.capture(), anyInt(), anyString(), any());

        Infantry mockEntity = mock(Infantry.class);
        double cost = 2.0;
        when(mockEntity.getAlternateCost()).thenReturn(cost);

        // ...then we should be able to buy the infantry...
        assertTrue(quartermaster.buyUnit(mockEntity, 0));

        // ...and the new infantry should be added to the campaign...
        verify(mockCampaign, times(1)).addNewUnit(eq(mockEntity), eq(false), eq(0));

        // ...and it should cost the right amount.
        assertEquals(Money.of(cost), captor.getValue());
    }

    @Test
    public void buyUnitAppliesClanCostMultiplier() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for units...
        when(mockOptions.payForUnits()).thenReturn(true);

        // ...and clan units cost 2x...
        double clanMultiplier = 2.0;
        when(mockOptions.getClanPriceModifier()).thenReturn(clanMultiplier);

        // ...and can afford a unit...
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        ArgumentCaptor<Money> captor = ArgumentCaptor.forClass(Money.class);
        doReturn(true).when(mockFinances).debit(captor.capture(), anyInt(), anyString(), any());

        // ...and the unit is a clan unit...
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.isClan()).thenReturn(true);
        double cost = 1.0;
        doReturn(cost).when(mockEntity).getCost(anyBoolean());

        // ...then we should be able to buy the unit...
        assertTrue(quartermaster.buyUnit(mockEntity, 0));

        // ...and the new unit should be added to the campaign...
        verify(mockCampaign, times(1)).addNewUnit(eq(mockEntity), eq(false), eq(0));

        // ...and it should cost the right amount.
        assertEquals(Money.of(clanMultiplier * cost), captor.getValue());
    }

    @Test
    public void buyUnitAppliesClanCostMultiplierToInfantryAlso() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for units...
        when(mockOptions.payForUnits()).thenReturn(true);

        // ...and clan units cost 2x...
        double clanMultiplier = 2.0;
        when(mockOptions.getClanPriceModifier()).thenReturn(clanMultiplier);

        // ...and can afford a unit...
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        ArgumentCaptor<Money> captor = ArgumentCaptor.forClass(Money.class);
        doReturn(true).when(mockFinances).debit(captor.capture(), anyInt(), anyString(), any());

        // ...and the unit is clan infantry...
        Infantry mockEntity = mock(Infantry.class);
        when(mockEntity.isClan()).thenReturn(true);
        double cost = 1.0;
        when(mockEntity.getAlternateCost()).thenReturn(cost);

        // ...then we should be able to buy the clan infantry...
        assertTrue(quartermaster.buyUnit(mockEntity, 0));

        // ...and the new clan infantry should be added to the campaign...
        verify(mockCampaign, times(1)).addNewUnit(eq(mockEntity), eq(false), eq(0));

        // ...and it should cost the right amount.
        assertEquals(Money.of(clanMultiplier * cost), captor.getValue());
    }
}
