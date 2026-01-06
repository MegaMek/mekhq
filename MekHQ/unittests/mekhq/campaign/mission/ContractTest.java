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
        // With 100% transportComp, employer reimburses full transport cost (10)
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
        // 10% of getTotalAmount (190) = 19
        initializeTest();
        assertEquals(Money.of(19), contract.getSigningBonusAmount());
    }

    @Test
    public void testGetFeeAmount() {
        // 5% of getTotalAmount (190) = 9.5
        initializeTest();
        assertEquals(Money.of(9.5), contract.getFeeAmount());
    }

    @Test
    public void testGetTotalAmount() {
        // base(130) + overhead(10) + support(10) + transport(10) + transit(30) = 190
        initializeTest();
        assertEquals(Money.of(190), contract.getTotalAmount());
    }

    @Test
    public void testGetTotalAmountPlusFees() {
        // getTotalAmount(190) - getFeeAmount(9.5) = 180.5
        initializeTest();
        assertEquals(Money.of(180.5), contract.getTotalAmountPlusFees());
    }

    @Test
    public void testGetAdvanceAmount() {
        // 10% of getTotalAmountPlusFees(180.5) = 18.05
        initializeTest();
        assertEquals(Money.of(18.05), contract.getAdvanceAmount());
    }

    @Test
    public void testGetTotalAmountPlusFeesAndBonuses() {
        // getTotalAmountPlusFees(180.5) + signingBonus(19) = 199.5
        initializeTest();
        assertEquals(Money.of(199.5), contract.getTotalAmountPlusFeesAndBonuses());
    }

    @Test
    public void testGetMonthlyPayout() {
        // getTotalAmountPlusFees(180.5) * 90% / 10 months = 16.245 -> rounds to 16.24
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
        // With 100% transportComp, employer reimburses full transport cost (10)
        initializeTest();
        assertEquals(Money.of(10), contract.getEmployerTransportReimbursement(mockCampaign));
    }

    @Test
    public void testGetPlayerTransportCost() {
        // With 100% transportComp, player pays nothing (employer covers all)
        initializeTest();
        assertEquals(Money.of(0), contract.getPlayerTransportCost(mockCampaign));
    }

    @Test
    public void testBetterTransportTermsIncreaseReimbursement() {
        // Test that higher transportComp means MORE reimbursement (better for player)
        initializeTestWithTransportComp(50);
        Money reimbursementAt50Percent = contract.getEmployerTransportReimbursement(mockCampaign);

        initializeTestWithTransportComp(75);
        Money reimbursementAt75Percent = contract.getEmployerTransportReimbursement(mockCampaign);

        // 75% should give more reimbursement than 50%
        assertEquals(1, reimbursementAt75Percent.compareTo(reimbursementAt50Percent));
    }

    @Test
    public void testZeroTransportCompMeansNoReimbursement() {
        // With 0% transportComp, employer reimburses nothing
        initializeTestWithTransportComp(0);
        assertEquals(Money.of(0), contract.getEmployerTransportReimbursement(mockCampaign));
        // Player pays full transport cost
        assertEquals(Money.of(10), contract.getPlayerTransportCost(mockCampaign));
    }

    @Test
    public void testPartialTransportCompGivesPartialReimbursement() {
        // With 50% transportComp, employer reimburses half
        initializeTestWithTransportComp(50);
        assertEquals(Money.of(5), contract.getEmployerTransportReimbursement(mockCampaign));
        // Player pays the other half
        assertEquals(Money.of(5), contract.getPlayerTransportCost(mockCampaign));
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

    private void initializeTestWithTransportComp(int transportComp) {
        final PlanetarySystem mockPlanetarySystem = mock(PlanetarySystem.class);

        final JumpPath mockJumpPath = mock(JumpPath.class);
        when(mockJumpPath.getJumps()).thenReturn(2);
        when(mockJumpPath.getFirstSystem()).thenReturn(mockPlanetarySystem);

        initCampaign(mockJumpPath);
        initContractWithTransportComp(transportComp);
        contract.calculateContract(mockCampaign);
    }

    private void initContractWithTransportComp(int transportComp) {
        contract = spy(new Contract());

        contract.setOverheadComp(2); // Full overhead compensation
        contract.setMultiplier(1.3);

        contract.setLength(10);

        contract.setStraightSupport(100);
        contract.setTransportComp(transportComp);

        contract.setSigningBonusPct(10);
        contract.setMRBCFee(true);
        contract.setAdvancePct(10);

        when(contract.getSystem()).thenReturn(new PlanetarySystem());
        when(mockCampaign.isUseCommandCircuitForContract(contract)).thenReturn(false);
    }
}
