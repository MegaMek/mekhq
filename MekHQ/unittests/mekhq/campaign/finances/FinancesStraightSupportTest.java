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
package mekhq.campaign.finances;

import static mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules.DOUBLE_SUPPORT_PAYOUTS;
import static mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules.NO_IN_CONTRACT_SUPPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.parts.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the straight-support refund of overhead and food and housing in {@link Finances#newDay}: paid back on the
 * first of the month only once the charge has been debited, withheld under {@code NO_IN_CONTRACT_SUPPORT}, never more
 * than the charge even under {@code DOUBLE_SUPPORT_PAYOUTS}, and not paid at all without an active contract.
 */
class FinancesStraightSupportTest {
    private static final LocalDate FIRST_OF_MONTH = LocalDate.of(3052, 6, 1);
    private static final Money STARTING_FUNDS = Money.of(10_000_000);
    private static final Money OVERHEAD = Money.of(100_000);
    private static final Money FOOD_AND_HOUSING = Money.of(40_000);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Accountant accountant;
    private AbstractContract contract;
    private Finances finances;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);
        campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, false);
        campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, true);
        campaignOptions.set(CampaignOption.PAY_FOR_PEACETIME_SPARE_PARTS, false);
        campaignOptions.set(CampaignOption.PAY_FOR_TRAINING_MUNITIONS, false);
        campaignOptions.set(CampaignOption.PAY_FOR_PEACETIME_FUEL, false);
        campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, false);
        campaignOptions.set(CampaignOption.USE_TAXES, false);
        campaignOptions.set(CampaignOption.USE_SHARE_SYSTEM, false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(FIRST_OF_MONTH);

        // The contract pays nothing itself, so every credit is straight support
        contract = mock(AbstractContract.class);
        when(contract.getMonthlyPayOut()).thenReturn(Money.zero());
        when(contract.getName()).thenReturn("Test Contract");
        when(contract.getHyperlinkedName()).thenReturn("Test Contract");
        when(contract.getSupportMultiplier()).thenReturn(0.5);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        accountant = mock(Accountant.class);
        when(accountant.getOverheadExpenses()).thenReturn(OVERHEAD);
        when(accountant.getPeacetimeCost(anyBoolean())).thenReturn(Money.zero());
        when(accountant.getMonthlyFoodAndHousingExpenses()).thenReturn(Money.zero());
        when(accountant.getWeeklyMaintenanceCosts()).thenReturn(Money.zero());
        when(campaign.getAccountant()).thenReturn(accountant);

        finances = new Finances();
        finances.credit(TransactionType.STARTING_CAPITAL, FIRST_OF_MONTH.minusDays(1), STARTING_FUNDS, "Start");

        // Straight support is credited to the player force's finances; the net worth snapshot at the end of the day
        // also builds a financial report
        when(campaign.getPlayerForce().getFinances()).thenReturn(finances);
        when(campaign.getTotalRentFeesExcludingBays()).thenReturn(Money.zero());
        LocalHangar hangar = mock(LocalHangar.class);
        when(campaign.getPlayerForce().getHangar()).thenReturn(hangar);
        LocalWarehouse warehouse = mock(LocalWarehouse.class);
        when(warehouse.streamSpareParts()).thenAnswer(invocation -> Stream.<Part>empty());
        when(campaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
    }

    private List<Transaction> supportCredits() {
        return finances.getTransactions()
                     .stream()
                     .filter(transaction -> transaction.getType() == TransactionType.CONTRACT_PAYMENT)
                     .filter(transaction -> transaction.getAmount().isPositive())
                     .toList();
    }

    private void newDay(LocalDate today) {
        finances.newDay(campaign, today.minusDays(1), today);
    }

    @Test
    void overheadIsRefundedAfterItIsDebited() {
        newDay(FIRST_OF_MONTH);

        // 100,000 overhead, half paid back
        assertEquals(STARTING_FUNDS.minus(Money.of(50_000)), finances.getBalance());
        assertEquals(1, supportCredits().size());
        assertEquals(Money.of(50_000), supportCredits().get(0).getAmount());
    }

    @Test
    void foodAndHousingIsRefundedAfterItIsDebited() {
        campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, false);
        when(accountant.getMonthlyFoodAndHousingExpenses()).thenReturn(FOOD_AND_HOUSING);

        newDay(FIRST_OF_MONTH);

        // 40,000 food and housing, half paid back
        assertEquals(STARTING_FUNDS.minus(Money.of(20_000)), finances.getBalance());
        assertEquals(1, supportCredits().size());
        assertEquals(Money.of(20_000), supportCredits().get(0).getAmount());
    }

    @Test
    void overheadAndFoodAndHousingAreEachRefunded() {
        when(accountant.getMonthlyFoodAndHousingExpenses()).thenReturn(FOOD_AND_HOUSING);

        newDay(FIRST_OF_MONTH);

        // 140,000 charged, 70,000 paid back
        assertEquals(STARTING_FUNDS.minus(Money.of(70_000)), finances.getBalance());
        assertEquals(2, supportCredits().size());
    }

    @Test
    void nothingIsRefundedMidMonth() {
        when(accountant.getMonthlyFoodAndHousingExpenses()).thenReturn(FOOD_AND_HOUSING);

        newDay(FIRST_OF_MONTH.plusDays(14));

        assertEquals(STARTING_FUNDS, finances.getBalance());
        assertTrue(supportCredits().isEmpty());
    }

    /** A charge the force can't afford is not paid, so there is nothing to refund. */
    @Test
    void unaffordableOverheadIsNotRefunded() {
        when(accountant.getOverheadExpenses()).thenReturn(STARTING_FUNDS.plus(Money.of(1)));

        newDay(FIRST_OF_MONTH);

        assertEquals(STARTING_FUNDS, finances.getBalance());
        assertTrue(supportCredits().isEmpty());
    }

    @Test
    void overheadIsNotRefundedWithoutAnActiveContract() {
        List<AbstractContract> noContracts = List.of();
        when(campaign.getActiveContracts()).thenReturn(noContracts);

        newDay(FIRST_OF_MONTH);

        assertEquals(STARTING_FUNDS.minus(OVERHEAD), finances.getBalance());
        assertTrue(supportCredits().isEmpty());
    }

    @Test
    void overheadIsNotRefundedWithoutStraightSupport() {
        when(contract.getSupportMultiplier()).thenReturn(0.0);

        newDay(FIRST_OF_MONTH);

        assertEquals(STARTING_FUNDS.minus(OVERHEAD), finances.getBalance());
        assertTrue(supportCredits().isEmpty());
    }

    @Test
    void refundIsWithheldUnderNoInContractSupport() {
        when(contract.usesSpecialRule(NO_IN_CONTRACT_SUPPORT)).thenReturn(true);

        newDay(FIRST_OF_MONTH);

        assertEquals(STARTING_FUNDS.minus(OVERHEAD), finances.getBalance());
        assertTrue(supportCredits().isEmpty());
        verify(contract).changeWithheldSupportPayments(Money.of(50_000));
    }

    @Test
    void doubledRefundIsCappedAtTheCharge() {
        when(contract.getSupportMultiplier()).thenReturn(1.0);
        when(contract.usesSpecialRule(DOUBLE_SUPPORT_PAYOUTS)).thenReturn(true);
        when(accountant.getMonthlyFoodAndHousingExpenses()).thenReturn(FOOD_AND_HOUSING);

        newDay(FIRST_OF_MONTH);

        // Doubled to 200%, but capped at the whole charge: the costs are covered, never turned into a profit
        assertEquals(STARTING_FUNDS, finances.getBalance());
        verify(contract, never()).changeWithheldSupportPayments(any());
    }
}
