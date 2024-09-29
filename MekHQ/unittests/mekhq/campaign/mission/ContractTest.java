/*
 * ContractTest.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.campaign.mission;

import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Miguel Azevedo
 * @since 20/07/18 10:23 AM
 */
public class ContractTest {
    private Contract contract;
    private Campaign mockCampaign;

    @Test
    public void testGetBaseAmount() {
        initializeTest();
        assertEquals(Money.of(130), contract.getBaseAmount());
    }

    @Test
    public void testGetOverheadAmount() {
        initializeTest();
        assertEquals(Money.of(10), contract.getOverheadAmount());
    }

    @Test
    public void testGetSupportAmount() {
        initializeTest();
        assertEquals(Money.of(10), contract.getSupportAmount());
    }

    @Test
    public void testGetTransportAmount() {
        initializeTest();
        assertEquals(Money.of(20), contract.getTransportAmount());
    }

    @Test
    public void testGetTransitAmount() {
        initializeTest();
        assertEquals(Money.of(30), contract.getTransitAmount());
    }

    @Test
    public void testSigningBonusAmount() {
        initializeTest();
        assertEquals(Money.of(20), contract.getSigningBonusAmount());
    }

    @Test
    public void testGetFeeAmount() {
        initializeTest();
        assertEquals(Money.of(10), contract.getFeeAmount());
    }

    @Test
    public void testGetTotalAmount() {
        initializeTest();
        assertEquals(Money.of(200), contract.getTotalAmount());
    }

    @Test
    public void testGetTotalAmountPlusFees() {
        initializeTest();
        assertEquals(Money.of(190), contract.getTotalAmountPlusFees());
    }

    @Test
    public void testGetAdvanceAmount() {
        initializeTest();
        assertEquals(Money.of(19), contract.getAdvanceAmount());
    }

    @Test
    public void testGetTotalAmountPlusFeesAndBonuses() {
        initializeTest();
        assertEquals(Money.of(210), contract.getTotalAmountPlusFeesAndBonuses());
    }

    @Test
    public void testGetMonthlyPayout() {
        initializeTest();
        assertEquals(Money.of(17.10), contract.getMonthlyPayOut());
    }

    private void initializeTest() {
        final PlanetarySystem mockPlanetarySystem = mock(PlanetarySystem.class);

        final JumpPath mockJumpPath = mock(JumpPath.class);
        when(mockJumpPath.getJumps()).thenReturn(2);
        when(mockJumpPath.getFirstSystem()).thenReturn(mockPlanetarySystem);

        initCampaign(mockJumpPath);
        initContract();
        contract.calculateContract(mockCampaign);
    }

    private void initContract() {
        contract = spy(new Contract());

        contract.setOverheadComp(2); // Full overhead compensation
        contract.setMultiplier(1.3);

        contract.setLength(10);

        contract.setStraightSupport(100);
        contract.setTransportComp(100);

        contract.setSigningBonusPct(10);
        contract.setMRBCFee(true);
        contract.setAdvancePct(10);

        when(contract.getSystem()).thenReturn(new PlanetarySystem());
    }

    private void initCampaign(final JumpPath mockJumpPath) {
        mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.isUsePeacetimeCost()).thenReturn(true);
        when(mockCampaignOptions.isPayForTransport()).thenReturn(true);
        when(mockCampaignOptions.getUnitRatingMethod()).thenReturn(UnitRatingMethod.CAMPAIGN_OPS);

        Money jumpCost = Money.of(5);
        Money contractBase = Money.of(10);
        Money overHeadExpenses = Money.of(1);
        Money peacetimeCost = Money.of(1);

        Accountant mockAccountant = mock(Accountant.class);
        when(mockAccountant.getOverheadExpenses()).thenReturn(overHeadExpenses);
        when(mockAccountant.getContractBase()).thenReturn(contractBase);
        when(mockAccountant.getPeacetimeCost()).thenReturn(peacetimeCost);

        when(mockCampaign.calculateJumpPath(nullable(PlanetarySystem.class), nullable(PlanetarySystem.class))).thenReturn(mockJumpPath);
        when(mockCampaign.calculateCostPerJump(anyBoolean(), anyBoolean())).thenReturn(jumpCost);
        when(mockCampaign.getAtBUnitRatingMod()).thenReturn(10);
        when(mockCampaign.getAccountant()).thenReturn(mockAccountant);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.of(3067, 1, 1));
    }
}
