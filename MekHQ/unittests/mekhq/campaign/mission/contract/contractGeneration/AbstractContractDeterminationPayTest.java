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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractFinanceData;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Tests the shared behavior on {@link AbstractContractDeterminationPay}: the
 * {@link AbstractContractDeterminationPay#forCampaign(Campaign)} scheme selector, the
 * {@link AbstractContractDeterminationPay#determineContractPay} template method that composes the three pay components
 * into the contract's {@link ContractFinanceData}, and the shared transport-pay calculation.
 */
class AbstractContractDeterminationPayTest {
    private static final LocalDate DATE = LocalDate.of(3025, 1, 1);

    /** A campaign whose {@code campaignOptions.get(option)} returns the supplied value, and nothing else stubbed. */
    private static Campaign campaignWithOption(CampaignOption<Boolean> option, boolean value) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(option)).thenReturn(value);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    // region forCampaign

    @Test
    void forCampaignPicksChaosByDefault() {
        Campaign campaign = campaignWithOption(CampaignOption.USE_LEGACY_CONTRACT_PAY, false);
        assertInstanceOf(ChaosContractDeterminationPay.class,
              AbstractContractDeterminationPay.forCampaign(campaign));
    }

    @Test
    void forCampaignPicksCamOpsWhenLegacyContractPayIsSet() {
        Campaign campaign = campaignWithOption(CampaignOption.USE_LEGACY_CONTRACT_PAY, true);
        assertInstanceOf(CamOpsContractDeterminationPay.class,
              AbstractContractDeterminationPay.forCampaign(campaign));
    }

    // endregion forCampaign

    // region determineContractPay template

    /** A scheme with fixed, distinct pay components, so the template's wiring into the record can be checked. */
    private static class FixedPay extends AbstractContractDeterminationPay {
        static final Money MONTHLY = Money.of(500);
        static final Money COMBAT = Money.of(250);
        static final Money TRANSPORT = Money.of(1_000);

        @Override
        public Money getMonthlyPay(Campaign campaign, AbstractContract contract) {
            return MONTHLY;
        }

        @Override
        public Money getCombatPay(Campaign campaign, AbstractContract contract) {
            return COMBAT;
        }

        @Override
        public Money getTransportPay(Campaign campaign, LocalDate currentDate, AbstractContract contract,
              AbstractLocation currentLocation) {
            return TRANSPORT;
        }
    }

    @Test
    void determineContractPayComposesTheThreeComponentsIntoTheContract() {
        AbstractContract contract = mock(AbstractContract.class);

        new FixedPay().determineContractPay(mock(Campaign.class), DATE, contract, mock(AbstractLocation.class));

        ArgumentCaptor<ContractFinanceData> captor = ArgumentCaptor.forClass(ContractFinanceData.class);
        verify(contract).setContractFinanceData(captor.capture());
        ContractFinanceData financeData = captor.getValue();
        assertEquals(FixedPay.MONTHLY, financeData.monthlyPay());
        assertEquals(FixedPay.COMBAT, financeData.combatPay());
        assertEquals(FixedPay.TRANSPORT, financeData.transport());
    }

    // endregion determineContractPay template

    // region transport pay

    /**
     * Sets up a campaign/contract/location for the transport calculation and runs it. The jump path is stubbed via a
     * static mock; a {@code jumps} of {@code 0} stands in for "no route" (a {@code null} jump path).
     */
    private static Money transportPay(int scale, int jumps, double transportMultiplier, boolean atHiringHall,
          boolean twoWayPay, boolean convertSupportPoints) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.IS_USE_TWO_WAY_PAY)).thenReturn(twoWayPay);
        when(options.get(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION)).thenReturn(convertSupportPoints);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);

        PlanetarySystem currentSystem = mock(PlanetarySystem.class);
        when(currentSystem.isHiringHall(DATE)).thenReturn(atHiringHall);
        AbstractLocation currentLocation = mock(AbstractLocation.class);
        when(currentLocation.getCurrentSystem()).thenReturn(currentSystem);

        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getScale()).thenReturn(scale);
        when(contract.getTransportMultiplier()).thenReturn(transportMultiplier);

        JumpPath jumpPath = jumps == 0 ? null : mock(JumpPath.class);
        if (jumpPath != null) {
            when(jumpPath.getJumps()).thenReturn(jumps);
        }

        // Any concrete subclass inherits the shared transport calculation unchanged.
        AbstractContractDeterminationPay payScheme = new ChaosContractDeterminationPay();
        try (MockedStatic<ContractUtilities> contractUtilities = mockStatic(ContractUtilities.class)) {
            contractUtilities.when(() -> ContractUtilities.getJumpPath(eq(campaign), eq(contract), eq(currentLocation)))
                  .thenReturn(jumpPath);
            return payScheme.getTransportPay(campaign, DATE, contract, currentLocation);
        }
    }

    @Test
    void transportPayIsZeroWhenThereIsNoJourney() {
        assertEquals(Money.zero(), transportPay(4, 0, 1.0, false, false, false));
    }

    @Test
    void transportPayScalesWithScaleAndJumpCount() {
        // 50 * scale(3) * jumps(2) = 300 support points, unconverted.
        assertEquals(Money.of(300), transportPay(3, 2, 1.0, false, false, false));
    }

    @Test
    void transportPayAppliesTheNegotiatedTransportMultiplier() {
        // 50 * 2 * 1 = 100, rounded after the 1.5x multiplier -> 150 support points.
        assertEquals(Money.of(150), transportPay(2, 1, 1.5, false, false, false));
    }

    @Test
    void transportPayDoublesForTwoWayHiringHallPay() {
        // 50 * 2 * 1 = 100, doubled by the hiring-hall return leg -> 200 support points.
        assertEquals(Money.of(200), transportPay(2, 1, 1.0, true, true, false));
    }

    @Test
    void transportPayIgnoresHiringHallReturnWhenTwoWayPayIsOff() {
        assertEquals(Money.of(100), transportPay(2, 1, 1.0, true, false, false));
    }

    @Test
    void transportPayConvertsSupportPointsToCBillsWhenEnabled() {
        // 50 * 2 * 1 = 100 support points, converted at 10,000 C-bills each.
        assertEquals(Money.of(1_000_000), transportPay(2, 1, 1.0, false, false, true));
    }

    // endregion transport pay
}
