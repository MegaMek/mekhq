/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

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
        // With 100% transport compensation, transportAmount is employer's full reimbursement
        // Full transport cost = 5 (jumpCost) * 2 (two-way) = 10
        // Employer reimburses 100% = 10
        initializeTest();
        assertEquals(Money.of(10), contract.getTransportAmount());
    }

    @Test
    public void testGetTransitAmount() {
        initializeTest();
        assertEquals(Money.of(30), contract.getTransitAmount());
    }

    @Test
    public void testSigningBonusAmount() {
        // Signing bonus = (base + overhead + transport + transit + support) * signBonus%
        // = (130 + 10 + 10 + 30 + 10) * 10% = 190 * 10% = 19
        initializeTest();
        assertEquals(Money.of(19.0), contract.getSigningBonusAmount());
    }

    @Test
    public void testGetFeeAmount() {
        // Fee = (base + overhead + transport + transit + support) * 5%
        // = 190 * 5% = 9.5
        initializeTest();
        assertEquals(Money.of(9.5), contract.getFeeAmount());
    }

    @Test
    public void testGetTotalAmount() {
        // Total = base + support + overhead + transportAmount + transit
        // = 130 + 10 + 10 + 10 + 30 = 190
        initializeTest();
        assertEquals(Money.of(190.0), contract.getTotalAmount());
    }

    @Test
    public void testGetTotalAmountPlusFees() {
        // TotalPlusFees = Total - feeAmount = 190 - 9.5 = 180.5
        initializeTest();
        assertEquals(Money.of(180.5), contract.getTotalAmountPlusFees());
    }

    @Test
    public void testGetAdvanceAmount() {
        // Advance = TotalAmountPlusFees * advancePct% = 180.5 * 10% = 18.05
        initializeTest();
        assertEquals(Money.of(18.05), contract.getAdvanceAmount());
    }

    @Test
    public void testGetTotalAmountPlusFeesAndBonuses() {
        // = TotalAmountPlusFees + signingAmount = 180.5 + 19 = 199.5
        initializeTest();
        assertEquals(Money.of(199.5), contract.getTotalAmountPlusFeesAndBonuses());
    }

    @Test
    public void testGetMonthlyPayout() {
        // MonthlyPayout = (TotalAmountPlusFeesAndBonuses - TotalAdvanceAmount) / length
        // TotalAdvanceAmount = advanceAmount + signingAmount = 18.05 + 19 = 37.05
        // = (199.5 - 37.05) / 10 = 16.245 (rounded to 16.24)
        initializeTest();
        assertEquals(Money.of(16.24), contract.getMonthlyPayOut());
    }

    @Test
    public void testGetSharesPercentDefaultsTo30() {
        initializeTest();
        assertEquals(30, contract.getSharesPercent());
    }

    @Test
    public void testGetEmployerTransportReimbursement() {
        // With 100% transport compensation, employer reimburses full transport cost
        // Full transport cost = 5 (jumpCost) * 2 (two-way) = 10
        initializeTest();
        assertEquals(Money.of(10), contract.getEmployerTransportReimbursement(mockCampaign));
    }

    @Test
    public void testGetPlayerTransportCost() {
        // With 100% transport compensation, player pays nothing
        initializeTest();
        assertEquals(Money.of(0), contract.getPlayerTransportCost(mockCampaign));
    }

    @Test
    public void testGetTotalTransportationFees() {
        // Returns full transport cost regardless of compensation
        // = 5 (jumpCost) * 2 (two-way) = 10
        initializeTest();
        assertEquals(Money.of(10), contract.getTotalTransportationFees(mockCampaign));
    }

    @Test
    public void testTransportReimbursementWithPartialCompensation() {
        // Test with 50% transport compensation
        initializeTestWithTransportComp(50);

        // Full transport cost = 10
        // Employer reimburses 50% = 5
        assertEquals(Money.of(5), contract.getEmployerTransportReimbursement(mockCampaign));
        assertEquals(Money.of(5), contract.getPlayerTransportCost(mockCampaign));
        assertEquals(Money.of(5), contract.getTransportAmount()); // Stored employer reimbursement
    }

    @Test
    public void testBetterTransportTermsIncreaseReimbursement() {
        // 50% compensation
        initializeTestWithTransportComp(50);
        Money reimbursement50 = contract.getEmployerTransportReimbursement(mockCampaign);

        // 75% compensation (better terms)
        initializeTestWithTransportComp(75);
        Money reimbursement75 = contract.getEmployerTransportReimbursement(mockCampaign);

        // Better terms should result in higher reimbursement
        assertTrue(reimbursement75.isGreaterThan(reimbursement50),
              "75% transport compensation should reimburse more than 50%");
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
        when(mockCampaign.isUseCommandCircuitForContract(contract)).thenReturn(false);
    }

    private void initCampaign(final JumpPath mockJumpPath) {
        mockCampaign = mock(Campaign.class);

        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaignOptions.isUsePeacetimeCost()).thenReturn(true);
        when(mockCampaignOptions.isPayForTransport()).thenReturn(true);
        when(mockCampaignOptions.isUseTwoWayPay()).thenReturn(true);

        Money jumpCost = Money.of(5);
        Money contractBase = Money.of(10);
        Money overHeadExpenses = Money.of(1);
        Money peacetimeCost = Money.of(1);

        Accountant mockAccountant = mock(Accountant.class);
        when(mockAccountant.getOverheadExpenses()).thenReturn(overHeadExpenses);
        when(mockAccountant.getContractBase()).thenReturn(contractBase);
        when(mockAccountant.getPeacetimeCost()).thenReturn(peacetimeCost);

        when(mockCampaign.calculateJumpPath(nullable(PlanetarySystem.class),
              nullable(PlanetarySystem.class))).thenReturn(mockJumpPath);
        when(mockCampaign.getAtBUnitRatingMod()).thenReturn(10);
        when(mockCampaign.getAccountant()).thenReturn(mockAccountant);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.of(3067, 1, 1));

        CurrentLocation mockCurrentLocation = mock(CurrentLocation.class);
        when(mockCampaign.getLocation()).thenReturn(mockCurrentLocation);

        TransportCostCalculations mockTransportCostCalculation = mock(TransportCostCalculations.class);
        when(mockCampaign.getTransportCostCalculation(EXP_REGULAR)).thenReturn(mockTransportCostCalculation);
        when(mockTransportCostCalculation.calculateJumpCostForEntireJourney(any(Integer.class),
              any(Integer.class))).thenReturn(jumpCost);
    }

    private void initializeTestWithTransportComp(int transportCompPercent) {
        final PlanetarySystem mockPlanetarySystem = mock(PlanetarySystem.class);

        final JumpPath mockJumpPath = mock(JumpPath.class);
        when(mockJumpPath.getJumps()).thenReturn(2);
        when(mockJumpPath.getFirstSystem()).thenReturn(mockPlanetarySystem);

        initCampaign(mockJumpPath);
        initContractWithTransportComp(transportCompPercent);
        contract.calculateContract(mockCampaign);
    }

    private void initContractWithTransportComp(int transportCompPercent) {
        contract = spy(new Contract());

        contract.setOverheadComp(2); // Full overhead compensation
        contract.setMultiplier(1.3);

        contract.setLength(10);

        contract.setStraightSupport(100);
        contract.setTransportComp(transportCompPercent);

        contract.setSigningBonusPct(10);
        contract.setMRBCFee(true);
        contract.setAdvancePct(10);

        when(contract.getSystem()).thenReturn(new PlanetarySystem());
        when(mockCampaign.isUseCommandCircuitForContract(contract)).thenReturn(false);
    }
}
