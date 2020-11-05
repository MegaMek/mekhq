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

import megamek.common.Entity;
import megamek.common.Infantry;
import mekhq.EventSpy;
import mekhq.campaign.event.PartArrivedEvent;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

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
        doReturn(mockPart).when(mockWarehouse).addPart(eq(mockPart), eq(true));

        // Arrive a part...
        quartermaster.arrivePart(mockPart);

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
        doReturn(false).when(mockFinances).debit(any(), eq(Transaction.C_UNIT), anyString(), any());

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
        doReturn(true).when(mockFinances).debit(captor.capture(), eq(Transaction.C_UNIT), anyString(), any());

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
        doReturn(true).when(mockFinances).debit(captor.capture(), eq(Transaction.C_UNIT), anyString(), any());

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
        doReturn(true).when(mockFinances).debit(captor.capture(), eq(Transaction.C_UNIT), anyString(), any());

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
        doReturn(true).when(mockFinances).debit(captor.capture(), eq(Transaction.C_UNIT), anyString(), any());

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

    @Test
    public void sellUnitCreditsCorrectAmount() {
        Campaign mockCampaign = mock(Campaign.class);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(UUID.randomUUID());
        Money sellValue = Money.of(42.0);
        when(mockUnit.getSellValue()).thenReturn(sellValue);

        // When you sell a unit with a certain value...
        quartermaster.sellUnit(mockUnit);

        // ...make sure we get that money!
        verify(mockFinances, times(1)).credit(eq(sellValue), eq(Transaction.C_UNIT_SALE),
                anyString(), any());
    }

    @Test
    public void sellUnitRemovesTheUnitFromTheCampaign() {
        Campaign mockCampaign = mock(Campaign.class);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Unit mockUnit = mock(Unit.class);
        UUID mockId = UUID.randomUUID();
        when(mockUnit.getId()).thenReturn(mockId);
        when(mockUnit.getSellValue()).thenReturn(Money.of(42.0));

        // When you sell a unit...
        quartermaster.sellUnit(mockUnit);

        // ...make sure we don't get to keep the unit.
        verify(mockCampaign, times(1)).removeUnit(eq(mockId));
    }

    @Test
    public void buyPartAddsUnconditionallyIfNotPayingForParts() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we don't pay for parts...
        when(mockOptions.payForParts()).thenReturn(false);

        Part mockPart = mock(Part.class);

        // ...then we should buy a part unconditionally...
        int transitDays = 10;
        assertTrue(quartermaster.buyPart(mockPart, 50.0, transitDays));

        // ...and the part should arrive in the correct number of days...
        verify(mockPart, times(1)).setDaysToArrival(eq(transitDays));

        // ...and it should be added to the warehouse.
        verify(mockWarehouse, times(1)).addPart(eq(mockPart), eq(true));
    }

    @Test
    public void buyPartAddsRefitKitUnconditionallyIfNotPayingForParts() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we don't pay for parts...
        when(mockOptions.payForParts()).thenReturn(false);

        Refit mockRefit = mock(Refit.class);

        // ...then we should buy a refit kit unconditionally...
        int transitDays = 10;
        assertTrue(quartermaster.buyPart(mockRefit, 50.0, transitDays));

        // ...and the refit kit should add its parts, arriving in
        // the correct amount of time.
        verify(mockRefit, times(1)).addRefitKitParts(eq(transitDays));
    }

    @Test
    public void buyPartReturnsFalseIfOutOfCash() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Part mockPart = mock(Part.class);
        Money cost = Money.of(42.0);
        when(mockPart.getStickerPrice()).thenReturn(cost);

        // ...and we can't afford the part...
        doReturn(false).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...then we should not be able to buy the part...
        assertFalse(quartermaster.buyPart(mockPart, 0));

        // ...and it should NOT be added to the warehouse.
        verify(mockWarehouse, times(0)).addPart(eq(mockPart), eq(true));
    }

    @Test
    public void buyPartOfRefitReturnsFalseIfOutOfCash() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Refit mockRefit = mock(Refit.class);
        Money cost = Money.of(42.0);
        when(mockRefit.getStickerPrice()).thenReturn(cost);

        // ...and we can't afford the refit kit...
        doReturn(false).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...then we should not be able to buy the refit kit...
        assertFalse(quartermaster.buyPart(mockRefit, 0));

        // ...and it should NOT add its parts.
        verify(mockRefit, times(0)).addRefitKitParts(anyInt());
    }

    @Test
    public void buyPartCalculatesWithoutUsingCostMultiplier() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Part mockPart = mock(Part.class);
        Money cost = Money.of(1.0);
        when(mockPart.getStickerPrice()).thenReturn(cost);

        ArgumentCaptor<Money> costCaptor = ArgumentCaptor.forClass(Money.class);
        doReturn(false).when(mockFinances).debit(costCaptor.capture(), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...when we try to buy the part...
        quartermaster.buyPart(mockPart, 0);

        // ...it should cost the exact amount without a multiplier.
        assertEquals(cost, costCaptor.getValue());
    }

    @Test
    public void buyPartCalculatesUsingCostMultiplier() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Part mockPart = mock(Part.class);
        Money cost = Money.of(1.0);
        when(mockPart.getStickerPrice()).thenReturn(cost);

        ArgumentCaptor<Money> costCaptor = ArgumentCaptor.forClass(Money.class);
        doReturn(false).when(mockFinances).debit(costCaptor.capture(), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...when we try to buy the part...
        double costMultiplier = 10.0;
        quartermaster.buyPart(mockPart, costMultiplier, 0);

        // ...it should cost the exact amount with a multiplier.
        assertEquals(cost.multipliedBy(costMultiplier), costCaptor.getValue());
    }

    @Test
    public void buyPartRefitCalculatesWithoutUsingCostMultiplier() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Refit mockRefit = mock(Refit.class);
        Money cost = Money.of(1.0);
        when(mockRefit.getStickerPrice()).thenReturn(cost);

        ArgumentCaptor<Money> costCaptor = ArgumentCaptor.forClass(Money.class);
        doReturn(false).when(mockFinances).debit(costCaptor.capture(), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...when we try to buy the refit kit...
        quartermaster.buyPart(mockRefit, 0);

        // ...it should cost the exact amount without a multiplier.
        assertEquals(cost, costCaptor.getValue());
    }

    @Test
    public void buyPartRefitCalculatesUsingCostMultiplier() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Refit mockRefit = mock(Refit.class);
        Money cost = Money.of(1.0);
        when(mockRefit.getStickerPrice()).thenReturn(cost);

        ArgumentCaptor<Money> costCaptor = ArgumentCaptor.forClass(Money.class);
        doReturn(false).when(mockFinances).debit(costCaptor.capture(), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...when we try to buy the refit kit with a cost multiplier...
        double costMultiplier = 2.0;
        quartermaster.buyPart(mockRefit, costMultiplier, 0);

        // ...it should cost the exact amount with a multiplier.
        assertEquals(cost.multipliedBy(costMultiplier), costCaptor.getValue());
    }

    @Test
    public void buyPartBuysThePartIfAble() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Part mockPart = mock(Part.class);
        Money cost = Money.of(42.0);
        when(mockPart.getStickerPrice()).thenReturn(cost);

        // ...and we can afford the part...
        doReturn(true).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...then we should be able to buy the part...
        assertTrue(quartermaster.buyPart(mockPart, 0));

        // ...and it should be added to the warehouse.
        verify(mockWarehouse, times(1)).addPart(eq(mockPart), eq(true));
    }

    @Test
    public void buyPartBuysTheRefitIfAble() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Refit mockRefit = mock(Refit.class);
        Money cost = Money.of(42.0);
        when(mockRefit.getStickerPrice()).thenReturn(cost);

        // ...and we can afford the refit kit...
        doReturn(true).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...then we should be able to buy the refit kit...
        assertTrue(quartermaster.buyPart(mockRefit, 0));

        // ...and it should add its parts.
        verify(mockRefit, times(1)).addRefitKitParts(anyInt());
    }

    @Test
    public void buyRefurbishmentReturnsTrueIfNotPayingForParts() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we don't pay for parts...
        when(mockOptions.payForParts()).thenReturn(false);

        Part mockPart = mock(Part.class);
        Money cost = Money.of(42.0);
        when(mockPart.getStickerPrice()).thenReturn(cost);

        // ...then we should be able to refurbish the part...
        assertTrue(quartermaster.buyRefurbishment(mockPart));

        // ...and we never should have tried to spend our money!
        verify(mockFinances, times(0)).debit(any(), anyInt(), anyString(), any());
    }

    @Test
    public void buyRefurbishmentReturnsFalseIfOutOfCash() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Part mockPart = mock(Part.class);
        Money cost = Money.of(42.0);
        when(mockPart.getStickerPrice()).thenReturn(cost);

        // ...and we can't afford the refurbishment...
        doReturn(false).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...then we should not be able to refurbish the part.
        assertFalse(quartermaster.buyRefurbishment(mockPart));
    }

    @Test
    public void buyRefurbishmentReturnsTrueIfWeHaveTheMoney() {
        Campaign mockCampaign = mock(Campaign.class);
        CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockOptions);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // If we pay for parts...
        when(mockOptions.payForParts()).thenReturn(true);

        Part mockPart = mock(Part.class);
        Money cost = Money.of(42.0);
        when(mockPart.getStickerPrice()).thenReturn(cost);

        // ...and we can afford the refurbishment...
        doReturn(true).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP),
                anyString(), any());

        // ...then we should be able to refurbish the part.
        assertTrue(quartermaster.buyRefurbishment(mockPart));
    }

    @Test
    public void sellPartWontSellZeroParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);

        quartermaster.sellPart(mockPart, 0);

        // No attempt should be made to sell or remove zero parts.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removePart(eq(mockPart), anyInt());
    }

    @Test
    public void sellPartWontSellNegativeParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockPart = mock(Part.class);

        quartermaster.sellPart(mockPart, -10);

        // No attempt should be made to sell or remove negative parts.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removePart(eq(mockPart), anyInt());
    }

    @Test
    public void sellPartWontSellMoreThanInStock() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Zero parts on hand...
        Part mockPart = mock(Part.class);
        when(mockPart.getQuantity()).thenReturn(0);

        // ...so try to sell 10 of them...
        quartermaster.sellPart(mockPart, 10);

        // ...and no attempt should be made to sell or remove parts we don't have.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removePart(eq(mockPart), anyInt());
    }

    @Test
    public void sellPartCalculatesSalePrice() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Two parts on hand worth 1 C-bill each...
        Part mockPart = mock(Part.class);
        when(mockPart.getQuantity()).thenReturn(2);
        when(mockPart.getActualValue()).thenReturn(Money.of(1.0));

        // ...so try to sell 2 of them...
        quartermaster.sellPart(mockPart, 2);

        // ...and we should be credited 2 C-bills for the sale!
        verify(mockFinances, times(1)).credit(eq(Money.of(2.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
    }

    @Test
    public void sellPartRemovesPartsFromWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Ten parts on hand worth 1 C-bill each...
        Part mockPart = mock(Part.class);
        when(mockPart.getQuantity()).thenReturn(10);
        when(mockPart.getActualValue()).thenReturn(Money.of(1.0));

        // ...so try to sell some of them...
        int saleQuantity = 7;
        quartermaster.sellPart(mockPart, saleQuantity);

        // ...and we should remove that exact number!
        verify(mockWarehouse, times(1)).removePart(eq(mockPart), eq(saleQuantity));
    }

    @Test
    public void sellPartRemovesNoMorePartsFromWarehouseThanOnHand() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Five parts on hand worth 1 C-bill each...
        Part mockPart = mock(Part.class);
        int warehouseQuantity = 5;
        when(mockPart.getQuantity()).thenReturn(warehouseQuantity);
        when(mockPart.getActualValue()).thenReturn(Money.of(1.0));

        // ...so try to sell more of them than we have...
        int saleQuantity = 100;
        quartermaster.sellPart(mockPart, saleQuantity);

        // ...and we should remove no more than we have!
        verify(mockWarehouse, times(1)).removePart(eq(mockPart), eq(warehouseQuantity));
    }

    @Test
    public void sellPartCalculatesSalePriceWhenFewerPartsOnHand() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Five parts on hand worth 1 C-bill each...
        Part mockPart = mock(Part.class);
        int warehouseQuantity = 5;
        when(mockPart.getQuantity()).thenReturn(warehouseQuantity);
        double value = 1.0;
        when(mockPart.getActualValue()).thenReturn(Money.of(value));

        // ...so try to sell more of them than we have...
        int saleQuantity = 100;
        quartermaster.sellPart(mockPart, saleQuantity);

        // ...and we should be credited for no more than we have!
        verify(mockFinances, times(1)).credit(eq(Money.of(warehouseQuantity * value)), eq(Transaction.C_EQUIP_SALE),
                anyString(), any());
    }

    @Test
    public void sellAllPartsSellsNothingIfYouHaveNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Zero parts on hand...
        Part mockPart = mock(Part.class);
        when(mockPart.getQuantity()).thenReturn(0);

        // ...so try to sell all of them...
        quartermaster.sellPart(mockPart);

        // ...and no attempt should be made to sell or remove parts we don't have.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removePart(eq(mockPart), anyInt());
    }

    @Test
    public void sellAllPartsRemovesPartsFromWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Ten parts on hand worth 1 C-bill each...
        Part mockPart = mock(Part.class);
        int warehouseQuantity = 10;
        when(mockPart.getQuantity()).thenReturn(warehouseQuantity);
        when(mockPart.getActualValue()).thenReturn(Money.of(1.0));

        // ...so try to sell all of them...
        quartermaster.sellPart(mockPart);

        // ...and we should remove all of them!
        verify(mockFinances, times(1)).credit(eq(Money.of(10.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(1)).removePart(eq(mockPart), eq(warehouseQuantity));
    }
}
