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
package mekhq.campaign.mission.contract.contractSpecialRules;

import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules.DOUBLE_SUPPORT_PAYOUTS;
import static mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules.NO_IN_CONTRACT_SUPPORT;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ContractSupportPayments}: the straight-support reimbursement of campaign-wide costs, battle loss
 * compensation, the lump-sum settlement of support withheld under {@code NO_IN_CONTRACT_SUPPORT}, and the doubling
 * applied by {@code DOUBLE_SUPPORT_PAYOUTS}.
 */
class ContractSupportPaymentsTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractSpecialRules";
    private static final LocalDate DATE = LocalDate.of(3025, 6, 15);

    private Finances finances;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        finances = mock(Finances.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(playerForce.getFinances()).thenReturn(finances);
        campaign = mock(Campaign.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getLocalDate()).thenReturn(DATE);
    }

    private static AbstractContract contractWithSupportMultiplier(double multiplier) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getSupportMultiplier()).thenReturn(multiplier);
        return contract;
    }

    // region reimburseStraightSupport

    @Test
    void reimburseStraightSupportDoesNothingForAZeroCost() {
        AbstractContract contract = contractWithSupportMultiplier(1.0);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        ContractSupportPayments.reimburseStraightSupport(campaign, Money.zero(), "Repairs");

        verifyNoInteractions(finances);
    }

    @Test
    void reimburseStraightSupportDoesNothingForANegativeCost() {
        AbstractContract contract = contractWithSupportMultiplier(1.0);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        ContractSupportPayments.reimburseStraightSupport(campaign, Money.of(-500), "Repairs");

        verifyNoInteractions(finances);
    }

    @Test
    void reimburseStraightSupportDoesNothingWhenNoContractIsActive() {
        when(campaign.getActiveContracts()).thenReturn(Collections.emptyList());

        ContractSupportPayments.reimburseStraightSupport(campaign, Money.of(1000), "Repairs");

        verifyNoInteractions(finances);
    }

    @Test
    void reimburseStraightSupportDoesNothingWhenTheSupportMultiplierIsZero() {
        AbstractContract contract = contractWithSupportMultiplier(0.0);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        ContractSupportPayments.reimburseStraightSupport(campaign, Money.of(1000), "Repairs");

        verifyNoInteractions(finances);
    }

    @Test
    void reimburseStraightSupportDoesNothingWhenTheSupportMultiplierIsNegative() {
        AbstractContract contract = contractWithSupportMultiplier(-0.5);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        ContractSupportPayments.reimburseStraightSupport(campaign, Money.of(1000), "Repairs");

        verifyNoInteractions(finances);
    }

    @Test
    void reimburseStraightSupportCreditsTheShareSilently() {
        AbstractContract contract = contractWithSupportMultiplier(0.5);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        ContractSupportPayments.reimburseStraightSupport(campaign, Money.of(1000), "Repairs");

        String expectedNote = getFormattedTextAt(RESOURCE_BUNDLE, "ContractSupport.straightSupport.note", "Repairs");
        verify(finances).credit(TransactionType.CONTRACT_PAYMENT, DATE, Money.of(500), expectedNote);
        // Straight support is credited silently: it fires on every repair/maintenance charge.
        verify(campaign, never()).addReport(any(), anyString());
    }

    @Test
    void reimburseStraightSupportDoublesTheShareUnderDoubleSupportPayouts() {
        AbstractContract contract = contractWithSupportMultiplier(0.5);
        when(contract.usesSpecialRule(DOUBLE_SUPPORT_PAYOUTS)).thenReturn(true);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        ContractSupportPayments.reimburseStraightSupport(campaign, Money.of(1000), "Repairs");

        // 1000 * 0.5 = 500, doubled to 1000.
        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(DATE), eq(Money.of(1000)), anyString());
    }

    @Test
    void reimburseStraightSupportWithholdsInsteadOfCreditingUnderNoInContractSupport() {
        AbstractContract contract = contractWithSupportMultiplier(0.5);
        when(contract.usesSpecialRule(NO_IN_CONTRACT_SUPPORT)).thenReturn(true);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        ContractSupportPayments.reimburseStraightSupport(campaign, Money.of(1000), "Repairs");

        verify(contract).changeWithheldSupportPayments(Money.of(500));
        verifyNoInteractions(finances);
        verify(campaign, never()).addReport(any(), anyString());
    }

    @Test
    void reimburseStraightSupportWithholdsTheAlreadyDoubledAmount() {
        AbstractContract contract = contractWithSupportMultiplier(0.5);
        when(contract.usesSpecialRule(NO_IN_CONTRACT_SUPPORT)).thenReturn(true);
        when(contract.usesSpecialRule(DOUBLE_SUPPORT_PAYOUTS)).thenReturn(true);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        ContractSupportPayments.reimburseStraightSupport(campaign, Money.of(1000), "Repairs");

        // Doubling is applied before the withhold check: 1000 * 0.5 = 500, doubled to 1000, then withheld whole.
        verify(contract).changeWithheldSupportPayments(Money.of(1000));
        verifyNoInteractions(finances);
    }

    // endregion reimburseStraightSupport

    // region payBattlefieldLoss

    @Test
    void payBattlefieldLossDoesNothingWhenTheAmountIsNotPositive() {
        AbstractContract contract = mock(AbstractContract.class);

        ContractSupportPayments.payBattlefieldLoss(campaign, contract, Money.zero(), "Wolverine");

        verifyNoInteractions(finances);
        verify(campaign, never()).addReport(any(), anyString());
    }

    @Test
    void payBattlefieldLossCreditsAndReportsTheAmount() {
        AbstractContract contract = mock(AbstractContract.class);

        ContractSupportPayments.payBattlefieldLoss(campaign, contract, Money.of(50_000), "Wolverine");

        String expectedNote = getFormattedTextAt(RESOURCE_BUNDLE, "ContractSupport.battlefieldLoss.note", "Wolverine");
        verify(finances).credit(TransactionType.CONTRACT_PAYMENT, DATE, Money.of(50_000), expectedNote);

        String expectedReport = getFormattedTextAt(RESOURCE_BUNDLE,
              "ContractSupport.battlefieldLoss.report",
              Money.of(50_000).toAmountAndSymbolString(),
              "Wolverine");
        verify(campaign).addReport(FINANCES, expectedReport);
    }

    @Test
    void payBattlefieldLossDoublesTheAmountUnderDoubleSupportPayouts() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.usesSpecialRule(DOUBLE_SUPPORT_PAYOUTS)).thenReturn(true);

        ContractSupportPayments.payBattlefieldLoss(campaign, contract, Money.of(50_000), "Wolverine");

        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(DATE), eq(Money.of(100_000)), anyString());
    }

    @Test
    void payBattlefieldLossWithholdsInsteadOfCreditingUnderNoInContractSupport() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.usesSpecialRule(NO_IN_CONTRACT_SUPPORT)).thenReturn(true);

        ContractSupportPayments.payBattlefieldLoss(campaign, contract, Money.of(50_000), "Wolverine");

        verify(contract).changeWithheldSupportPayments(Money.of(50_000));
        verifyNoInteractions(finances);
        verify(campaign, never()).addReport(any(), anyString());
    }

    // endregion payBattlefieldLoss

    // region renderWithheldSupport

    @Test
    void renderWithheldSupportDoesNothingWhenNothingIsWithheld() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getWithheldSupportPayments()).thenReturn(Money.zero());

        ContractSupportPayments.renderWithheldSupport(campaign, contract);

        verifyNoInteractions(finances);
        verify(campaign, never()).addReport(any(), anyString());
        verify(contract, never()).setWithheldSupportPayments(any());
    }

    @Test
    void renderWithheldSupportPaysTheFullWithheldTotalAndClearsIt() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getWithheldSupportPayments()).thenReturn(Money.of(7_500));
        when(contract.getName()).thenReturn("Raid on Sudeten");
        when(contract.getHyperlinkedName()).thenReturn("<a>Raid on Sudeten</a>");

        ContractSupportPayments.renderWithheldSupport(campaign, contract);

        String expectedNote = getFormattedTextAt(RESOURCE_BUNDLE, "ContractSupport.withheld.note", "Raid on Sudeten");
        verify(finances).credit(TransactionType.CONTRACT_PAYMENT, DATE, Money.of(7_500), expectedNote);

        String expectedReport = getFormattedTextAt(RESOURCE_BUNDLE,
              "ContractSupport.withheld.report",
              Money.of(7_500).toAmountAndSymbolString(),
              "<a>Raid on Sudeten</a>");
        verify(campaign).addReport(FINANCES, expectedReport);

        verify(contract).setWithheldSupportPayments(Money.zero());
    }

    // endregion renderWithheldSupport

    // region getStraightSupportContract

    @Test
    void getStraightSupportContractReturnsNullWhenNoContractIsActive() {
        when(campaign.getActiveContracts()).thenReturn(Collections.emptyList());

        assertNull(ContractSupportPayments.getStraightSupportContract(campaign));
    }

    @Test
    void getStraightSupportContractReturnsTheOnlyContractRegardlessOfItsMultiplier() {
        AbstractContract contract = contractWithSupportMultiplier(0.0);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        assertSame(contract, ContractSupportPayments.getStraightSupportContract(campaign));
    }

    @Test
    void getStraightSupportContractPicksTheHighestMultiplier() {
        AbstractContract low = contractWithSupportMultiplier(0.25);
        AbstractContract high = contractWithSupportMultiplier(0.75);
        AbstractContract mid = contractWithSupportMultiplier(0.5);
        when(campaign.getActiveContracts()).thenReturn(List.of(low, high, mid));

        assertSame(high, ContractSupportPayments.getStraightSupportContract(campaign));
    }

    @Test
    void getStraightSupportContractKeepsTheFirstContractOnATie() {
        AbstractContract first = contractWithSupportMultiplier(0.5);
        AbstractContract second = contractWithSupportMultiplier(0.5);
        when(campaign.getActiveContracts()).thenReturn(List.of(first, second));

        assertSame(first, ContractSupportPayments.getStraightSupportContract(campaign));
    }

    // endregion getStraightSupportContract
}
