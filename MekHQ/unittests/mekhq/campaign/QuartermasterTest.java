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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import megamek.common.Entity;
import megamek.common.Infantry;
import mekhq.EventSpy;
import mekhq.campaign.event.PartArrivedEvent;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.OmniPod;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
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
        verify(mockFinances, times(1)).credit(eq(sellValue), eq(Transaction.C_UNIT_SALE), anyString(), any());
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
        doReturn(false).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP), anyString(), any());

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
        doReturn(false).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP), anyString(), any());

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
        doReturn(false).when(mockFinances).debit(costCaptor.capture(), eq(Transaction.C_EQUIP), anyString(), any());

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
        doReturn(false).when(mockFinances).debit(costCaptor.capture(), eq(Transaction.C_EQUIP), anyString(), any());

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
        doReturn(false).when(mockFinances).debit(costCaptor.capture(), eq(Transaction.C_EQUIP), anyString(), any());

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
        doReturn(false).when(mockFinances).debit(costCaptor.capture(), eq(Transaction.C_EQUIP), anyString(), any());

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
        doReturn(true).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP), anyString(), any());

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
        doReturn(true).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP), anyString(), any());

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
        doReturn(false).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP), anyString(), any());

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
        doReturn(true).when(mockFinances).debit(eq(cost), eq(Transaction.C_EQUIP), anyString(), any());

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

    @Test
    public void sellAmmoWontSellZeroAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        AmmoStorage mockAmmo = mock(AmmoStorage.class);

        quartermaster.sellAmmo(mockAmmo, 0);

        // No attempt should be made to sell or remove zero parts.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removeAmmo(eq(mockAmmo), anyInt());
    }

    @Test
    public void sellAmmoWontSellNegativeAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        AmmoStorage mockAmmo = mock(AmmoStorage.class);

        quartermaster.sellAmmo(mockAmmo, -10);

        // No attempt should be made to sell or remove negative parts.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removeAmmo(eq(mockAmmo), anyInt());
    }

    @Test
    public void sellAmmoWontSellMoreThanInStock() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Zero parts on hand...
        AmmoStorage mockAmmo = mock(AmmoStorage.class);
        when(mockAmmo.getShots()).thenReturn(0);

        // ...so try to sell 10 of them...
        quartermaster.sellAmmo(mockAmmo, 10);

        // ...and no attempt should be made to sell or remove parts we don't have.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removeAmmo(eq(mockAmmo), anyInt());
    }

    @Test
    public void sellAmmoCalculatesSalePrice() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // One hundred rounds of ammo on hand worth 1 C-bill each...
        AmmoStorage mockAmmo = mock(AmmoStorage.class);
        when(mockAmmo.getShots()).thenReturn(100);
        when(mockAmmo.getActualValue()).thenReturn(Money.of(100.0));

        // ...so try to sell 2 of them...
        quartermaster.sellAmmo(mockAmmo, 2);

        // ...and we should be credited 2 C-bills for the sale!
        verify(mockFinances, times(1)).credit(eq(Money.of(2.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
    }

    @Test
    public void sellAmmoRemovesAmmoFromWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Ten rounds of ammo on hand worth 1 C-bill each...
        AmmoStorage mockAmmo = mock(AmmoStorage.class);
        when(mockAmmo.getShots()).thenReturn(10);
        when(mockAmmo.getActualValue()).thenReturn(Money.of(10.0));

        // ...so try to sell some of them...
        int saleQuantity = 7;
        quartermaster.sellAmmo(mockAmmo, saleQuantity);

        // ...and we should remove that exact number!
        verify(mockWarehouse, times(1)).removeAmmo(eq(mockAmmo), eq(saleQuantity));
    }

    @Test
    public void sellAmmoRemovesNoMoreAmmoFromWarehouseThanOnHand() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Five rounds of ammo on hand worth 1 C-bill each...
        AmmoStorage mockAmmo = mock(AmmoStorage.class);
        int warehouseQuantity = 5;
        when(mockAmmo.getShots()).thenReturn(warehouseQuantity);
        when(mockAmmo.getActualValue()).thenReturn(Money.of(5.0));

        // ...so try to sell more of them than we have...
        int saleQuantity = 100;
        quartermaster.sellAmmo(mockAmmo, saleQuantity);

        // ...and we should remove no more than we have!
        verify(mockWarehouse, times(1)).removeAmmo(eq(mockAmmo), eq(warehouseQuantity));
    }

    @Test
    public void sellAmmoCalculatesSalePriceWhenFewerAmmoOnHand() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Five rounds of ammo on hand worth 1 C-bill each...
        AmmoStorage mockAmmo = mock(AmmoStorage.class);
        int warehouseQuantity = 5;
        when(mockAmmo.getShots()).thenReturn(warehouseQuantity);
        double value = 5.0;
        when(mockAmmo.getActualValue()).thenReturn(Money.of(value));

        // ...so try to sell more of them than we have...
        int saleQuantity = 100;
        quartermaster.sellAmmo(mockAmmo, saleQuantity);

        // ...and we should be credited for no more than we have!
        verify(mockFinances, times(1)).credit(eq(Money.of(value)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
    }

    @Test
    public void sellAllAmmoSellsNothingIfYouHaveNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Zero rounds of ammo on hand...
        AmmoStorage mockAmmo = mock(AmmoStorage.class);
        when(mockAmmo.getShots()).thenReturn(0);

        // ...so try to sell all of them...
        quartermaster.sellAmmo(mockAmmo);

        // ...and no attempt should be made to sell or remove parts we don't have.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removeAmmo(eq(mockAmmo), anyInt());
    }

    @Test
    public void sellAllAmmoRemovesAmmoFromWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // One hundred rounds of ammo on hand worth 1 C-bill each...
        AmmoStorage mockAmmo = mock(AmmoStorage.class);
        int warehouseQuantity = 100;
        when(mockAmmo.getShots()).thenReturn(warehouseQuantity);
        when(mockAmmo.getActualValue()).thenReturn(Money.of(100.0));

        // ...so try to sell all of them...
        quartermaster.sellAmmo(mockAmmo);

        // ...and we should sell and remove all of them!
        verify(mockFinances, times(1)).credit(eq(Money.of(100.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(1)).removeAmmo(eq(mockAmmo), eq(warehouseQuantity));
    }

    @Test
    public void sellPartWithAmmoSellsAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Ten rounds of ammo on hand worth 1 C-bill each...
        AmmoStorage mockAmmo = mock(AmmoStorage.class);
        when(mockAmmo.getShots()).thenReturn(10);
        when(mockAmmo.getActualValue()).thenReturn(Money.of(10.0));

        // ...so try to sell some of them...
        int saleQuantity = 7;
        quartermaster.sellPart(mockAmmo, saleQuantity);

        // ...and we should sell and remove that exact number!
        verify(mockFinances, times(1)).credit(eq(Money.of(7.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(1)).removeAmmo(eq(mockAmmo), eq(saleQuantity));
    }

    @Test
    public void sellAllPartsWithAmmoSellsAllAmmo() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // One hundred rounds of ammo on hand worth 1 C-bill each...
        AmmoStorage mockAmmo = mock(AmmoStorage.class);
        int warehouseQuantity = 100;
        when(mockAmmo.getShots()).thenReturn(warehouseQuantity);
        when(mockAmmo.getActualValue()).thenReturn(Money.of(100.0));

        // ...so try to sell all of them...
        quartermaster.sellPart(mockAmmo);

        // ...and we should sell and remove all of them!
        verify(mockFinances, times(1)).credit(eq(Money.of(100.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(1)).removeAmmo(eq(mockAmmo), eq(warehouseQuantity));
    }

    @Test
    public void sellArmorWontSellZeroArmor() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Armor mockArmor = mock(Armor.class);

        quartermaster.sellArmor(mockArmor, 0);

        // No attempt should be made to sell or remove zero parts.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removeArmor(eq(mockArmor), anyInt());
    }

    @Test
    public void sellArmorWontSellNegativeArmor() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Armor mockArmor = mock(Armor.class);

        quartermaster.sellArmor(mockArmor, -10);

        // No attempt should be made to sell or remove negative parts.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removeArmor(eq(mockArmor), anyInt());
    }

    @Test
    public void sellArmorWontSellMoreThanInStock() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Zero parts on hand...
        Armor mockArmor = mock(Armor.class);
        when(mockArmor.getAmount()).thenReturn(0);

        // ...so try to sell 10 of them...
        quartermaster.sellArmor(mockArmor, 10);

        // ...and no attempt should be made to sell or remove parts we don't have.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removeArmor(eq(mockArmor), anyInt());
    }

    @Test
    public void sellArmorCalculatesSalePrice() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // One hundred points of armor on hand worth 1 C-bill each...
        Armor mockArmor = mock(Armor.class);
        when(mockArmor.getAmount()).thenReturn(100);
        when(mockArmor.getActualValue()).thenReturn(Money.of(100.0));

        // ...so try to sell 2 of them...
        quartermaster.sellArmor(mockArmor, 2);

        // ...and we should be credited 2 C-bills for the sale!
        verify(mockFinances, times(1)).credit(eq(Money.of(2.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
    }

    @Test
    public void sellArmorRemovesArmorFromWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Ten points of armor on hand worth 1 C-bill each...
        Armor mockArmor = mock(Armor.class);
        when(mockArmor.getAmount()).thenReturn(10);
        when(mockArmor.getActualValue()).thenReturn(Money.of(10.0));

        // ...so try to sell some of them...
        int saleQuantity = 7;
        quartermaster.sellArmor(mockArmor, saleQuantity);

        // ...and we should remove that exact number!
        verify(mockWarehouse, times(1)).removeArmor(eq(mockArmor), eq(saleQuantity));
    }

    @Test
    public void sellArmorRemovesNoMoreArmorFromWarehouseThanOnHand() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Five points of armor on hand worth 1 C-bill each...
        Armor mockArmor = mock(Armor.class);
        int warehouseQuantity = 5;
        when(mockArmor.getAmount()).thenReturn(warehouseQuantity);
        when(mockArmor.getActualValue()).thenReturn(Money.of(5.0));

        // ...so try to sell more of them than we have...
        int saleQuantity = 100;
        quartermaster.sellArmor(mockArmor, saleQuantity);

        // ...and we should remove no more than we have!
        verify(mockWarehouse, times(1)).removeArmor(eq(mockArmor), eq(warehouseQuantity));
    }

    @Test
    public void sellArmorCalculatesSalePriceWhenFewerArmorOnHand() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Five points of armor on hand worth 1 C-bill each...
        Armor mockArmor = mock(Armor.class);
        int warehouseQuantity = 5;
        when(mockArmor.getAmount()).thenReturn(warehouseQuantity);
        double value = 5.0;
        when(mockArmor.getActualValue()).thenReturn(Money.of(value));

        // ...so try to sell more of them than we have...
        int saleQuantity = 100;
        quartermaster.sellArmor(mockArmor, saleQuantity);

        // ...and we should be credited for no more than we have!
        verify(mockFinances, times(1)).credit(eq(Money.of(value)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
    }

    @Test
    public void sellAllArmorSellsNothingIfYouHaveNothing() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Zero points of armor on hand...
        Armor mockArmor = mock(Armor.class);
        when(mockArmor.getAmount()).thenReturn(0);

        // ...so try to sell all of them...
        quartermaster.sellArmor(mockArmor);

        // ...and no attempt should be made to sell or remove parts we don't have.
        verify(mockFinances, times(0)).credit(any(), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(0)).removeArmor(eq(mockArmor), anyInt());
    }

    @Test
    public void sellAllArmorRemovesArmorFromWarehouse() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // One hundred points of armor on hand worth 1 C-bill each...
        Armor mockArmor = mock(Armor.class);
        int warehouseQuantity = 100;
        when(mockArmor.getAmount()).thenReturn(warehouseQuantity);
        when(mockArmor.getActualValue()).thenReturn(Money.of(100.0));

        // ...so try to sell all of them...
        quartermaster.sellArmor(mockArmor);

        // ...and we should sell and remove all of them!
        verify(mockFinances, times(1)).credit(eq(Money.of(100.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(1)).removeArmor(eq(mockArmor), eq(warehouseQuantity));
    }

    @Test
    public void sellPartWithArmorSellsArmor() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Ten points of armor on hand worth 1 C-bill each...
        Armor mockArmor = mock(Armor.class);
        when(mockArmor.getAmount()).thenReturn(10);
        when(mockArmor.getActualValue()).thenReturn(Money.of(10.0));

        // ...so try to sell some of them...
        int saleQuantity = 7;
        quartermaster.sellPart(mockArmor, saleQuantity);

        // ...and we should sell and remove that exact number!
        verify(mockFinances, times(1)).credit(eq(Money.of(7.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(1)).removeArmor(eq(mockArmor), eq(saleQuantity));
    }

    @Test
    public void sellAllPartsWithArmorSellsAllArmor() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Finances mockFinances = mock(Finances.class);
        when(mockCampaign.getFinances()).thenReturn(mockFinances);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // One hundred points of armor on hand worth 1 C-bill each...
        Armor mockArmor = mock(Armor.class);
        int warehouseQuantity = 100;
        when(mockArmor.getAmount()).thenReturn(warehouseQuantity);
        when(mockArmor.getActualValue()).thenReturn(Money.of(100.0));

        // ...so try to sell all of them...
        quartermaster.sellPart(mockArmor);

        // ...and we should sell and remove all of them!
        verify(mockFinances, times(1)).credit(eq(Money.of(100.0)), eq(Transaction.C_EQUIP_SALE), anyString(), any());
        verify(mockWarehouse, times(1)).removeArmor(eq(mockArmor), eq(warehouseQuantity));
    }

    @Test
    public void depodPartOnlyDepodsOmniPoddedParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockOmniPart = mock(Part.class);
        when(mockOmniPart.isOmniPodded()).thenReturn(false);
        when(mockOmniPart.getQuantity()).thenReturn(1);

        quartermaster.depodPart(mockOmniPart, 1);

        verify(mockOmniPart, times(0)).decrementQuantity();
    }

    @Test
    public void depodPartDoesNotDepodZeroParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockOmniPart = mock(Part.class);
        when(mockOmniPart.isOmniPodded()).thenReturn(true);
        when(mockOmniPart.getQuantity()).thenReturn(1);

        quartermaster.depodPart(mockOmniPart, 0);

        verify(mockOmniPart, times(0)).decrementQuantity();
    }

    @Test
    public void depodPartDoesNotDepodNegativeParts() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        Part mockOmniPart = mock(Part.class);
        when(mockOmniPart.isOmniPodded()).thenReturn(true);
        when(mockOmniPart.getQuantity()).thenReturn(1);

        quartermaster.depodPart(mockOmniPart, -10);

        verify(mockOmniPart, times(0)).decrementQuantity();
    }

    @Test
    public void depodPartAddsPartAndCorrectOmniPod() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Create a spare omni-podded part...
        Part mockOmniPart = mock(Part.class);
        when(mockOmniPart.isOmniPodded()).thenReturn(true);
        when(mockOmniPart.getQuantity()).thenReturn(1);

        // ...and create a clone of the part...
        Part mockOmniPartClone = mock(Part.class);
        when(mockOmniPartClone.getStickerPrice()).thenReturn(Money.of(5.0));
        when(mockOmniPart.clone()).thenReturn(mockOmniPartClone);
        when(mockOmniPartClone.clone()).thenReturn(mockOmniPartClone); // CAW: test only.

        // ...and depod that part...
        quartermaster.depodPart(mockOmniPart, 1);

        // ...giving us a clone of the part...
        verify(mockWarehouse, times(1)).addPart(eq(mockOmniPartClone), eq(true));
        verify(mockOmniPartClone, atLeast(1)).setOmniPodded(eq(false));

        // ...and a new omnipod for the clone...
        ArgumentCaptor<Part> omniPodCaptor = ArgumentCaptor.forClass(Part.class);
        verify(mockWarehouse, times(2)).addPart(omniPodCaptor.capture(), eq(true));

        // ...and decrementing the number of the original parts.
        verify(mockOmniPart, times(1)).decrementQuantity();

        // The second call contains the omnipod.
        Part omniPod = omniPodCaptor.getAllValues().get(1);
        assertTrue(omniPod instanceof OmniPod);
        // OmniPods cost 1/5th the part's cost, so since our mock part costs
        // 5 C-bills, if we're calculating things propertly then the OmniPod
        // will cost only a buck.
        assertEquals(Money.of(1.0), omniPod.getStickerPrice());
    }

    @Test
    public void depodPartAddsCorrectNumberOfPartAndOmniPod() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Create a spare omni-podded part...
        Part mockOmniPart = mock(Part.class);
        when(mockOmniPart.isOmniPodded()).thenReturn(true);
        when(mockOmniPart.getQuantity()).thenReturn(10);
        when(mockOmniPart.clone()).then(createOmniPodPartAnswer());

        // ...and depod four of that part...
        int quantity = 4;
        quartermaster.depodPart(mockOmniPart, quantity);

        // ...giving us four clones of the part and its omnipod...
        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(mockWarehouse, times(2 * quantity)).addPart(partCaptor.capture(), eq(true));

        // ...and decrementing the number of the original parts.
        verify(mockOmniPart, times(quantity)).decrementQuantity();

        // There should then be 4 of the parts and 4 of their omnipods
        List<Part> addedParts = partCaptor.getAllValues();
        assertEquals(quantity, addedParts.stream().filter(p -> !(p instanceof OmniPod)).count());
        assertEquals(quantity, addedParts.stream().filter(p -> (p instanceof OmniPod)).count());
    }

    @Test
    public void depodPartAddsCorrectNumberOfPartAndOmniPodIfLessOnHand() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Create a spare omni-podded part...
        Part mockOmniPart = mock(Part.class);
        when(mockOmniPart.isOmniPodded()).thenReturn(true);
        int warehouseQuantity = 2;
        when(mockOmniPart.getQuantity()).thenReturn(warehouseQuantity);
        when(mockOmniPart.clone()).then(createOmniPodPartAnswer());

        // ...and try to depod four of that part...
        int quantity = 4;
        quartermaster.depodPart(mockOmniPart, quantity);

        // ...giving us only two clones of the part and its omnipod...
        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(mockWarehouse, times(2 * warehouseQuantity)).addPart(partCaptor.capture(), eq(true));

        // ...and decrementing the number of the original parts.
        verify(mockOmniPart, times(warehouseQuantity)).decrementQuantity();

        // There should then be two of the parts and two of their omnipods
        List<Part> addedParts = partCaptor.getAllValues();
        assertEquals(warehouseQuantity, addedParts.stream().filter(p -> !(p instanceof OmniPod)).count());
        assertEquals(warehouseQuantity, addedParts.stream().filter(p -> (p instanceof OmniPod)).count());
    }

    @Test
    public void depodAllPartsAddsCorrectNumberOfPartAndOmniPod() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Create a spare omni-podded part...
        Part mockOmniPart = mock(Part.class);
        when(mockOmniPart.isOmniPodded()).thenReturn(true);
        int quantity = 4;
        when(mockOmniPart.getQuantity()).thenReturn(quantity);
        when(mockOmniPart.clone()).then(createOmniPodPartAnswer());

        // ...and depod all of that part...
        quartermaster.depodPart(mockOmniPart);

        // ...giving us four clones of the part and its omnipod...
        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(mockWarehouse, times(2 * quantity)).addPart(partCaptor.capture(), eq(true));

        // ...and decrementing the number of the original parts.
        verify(mockOmniPart, times(quantity)).decrementQuantity();

        // There should then be 4 of the parts and 4 of their omnipods
        List<Part> addedParts = partCaptor.getAllValues();
        assertEquals(quantity, addedParts.stream().filter(p -> !(p instanceof OmniPod)).count());
        assertEquals(quantity, addedParts.stream().filter(p -> (p instanceof OmniPod)).count());
    }

    @Test
    public void depodPartRaisesChangedEventIfSomeRemain() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Create a spare omni-podded part...
        Part mockOmniPart = mock(Part.class);
        when(mockOmniPart.isOmniPodded()).thenReturn(true);
        when(mockOmniPart.getQuantity()).thenReturn(10).thenReturn(5);
        when(mockOmniPart.clone()).then(createOmniPodPartAnswer());

        try (EventSpy eventSpy = new EventSpy()) {
            // ...and depod some of that part...
            quartermaster.depodPart(mockOmniPart, 5);

            // ...and since some remain, we should receive a PartChangedEvent.
            assertNotNull(eventSpy.findEvent(PartChangedEvent.class, e -> e.getPart() == mockOmniPart));
        }
    }

    @Test
    public void depodPartDoesNotRaiseChangedEventIfNoneRemain() {
        Campaign mockCampaign = mock(Campaign.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        Quartermaster quartermaster = new Quartermaster(mockCampaign);

        // Create a spare omni-podded part...
        Part mockOmniPart = mock(Part.class);
        when(mockOmniPart.isOmniPodded()).thenReturn(true);
        when(mockOmniPart.getQuantity()).thenReturn(10).thenReturn(0);
        when(mockOmniPart.clone()).then(createOmniPodPartAnswer());

        try (EventSpy eventSpy = new EventSpy()) {
            // ...and depod all of that part...
            quartermaster.depodPart(mockOmniPart);

            // ...and since none remain, we should NOT receive a PartChangedEvent.
            assertNull(eventSpy.findEvent(PartChangedEvent.class, e -> e.getPart() == mockOmniPart));
        }
    }

    private Answer<Part> createOmniPodPartAnswer() {
        return new Answer<Part>() {
            @Override
            public Part answer(InvocationOnMock invocation) throws Throwable {
                Part mockOmniPart = mock(Part.class);
                when(mockOmniPart.isOmniPodded()).thenReturn(true);
                when(mockOmniPart.getQuantity()).thenReturn(1);
                // ...omniception!
                when(mockOmniPart.clone()).then(createOmniPodPartAnswer());
                return mockOmniPart;
            }
        };
    }
}
