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

import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import mekhq.campaign.mission.contract.contractSpecialRules.ContractSupportPayments;
import mekhq.campaign.parts.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests the monthly Hot Spots upkeep debit in {@link Finances#newDay}: charged on the first of the month as
 * maintenance, never alongside the costs it replaces, and reported as unpaid when the force can't afford it.
 */
class FinancesHotSpotsUpkeepTest {
    private static final LocalDate FIRST_OF_MONTH = LocalDate.of(3052, 6, 1);
    private static final Money STARTING_FUNDS = Money.of(10_000_000);
    private static final Money UPKEEP = Money.of(1_500_000);
    private static final Money OVERHEAD = Money.of(50_000);
    private static final Money PEACETIME = Money.of(70_000);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Accountant accountant;
    private Finances finances;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, true);
        campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, true);
        campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, true);
        campaignOptions.set(CampaignOption.USE_PEACETIME_COST, true);
        campaignOptions.set(CampaignOption.SHOW_PEACETIME_COST, false);
        campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, false);
        campaignOptions.set(CampaignOption.USE_TAXES, false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(FIRST_OF_MONTH);
        List<AbstractContract> noContracts = List.of();
        when(campaign.getActiveContracts()).thenReturn(noContracts);

        accountant = mock(Accountant.class);
        when(accountant.getHotSpotsUpkeepCosts()).thenReturn(UPKEEP);
        when(accountant.getOverheadExpenses()).thenReturn(OVERHEAD);
        when(accountant.getPeacetimeCost(anyBoolean())).thenReturn(PEACETIME);
        when(accountant.getMonthlyFoodAndHousingExpenses()).thenReturn(Money.zero());
        when(accountant.getWeeklyMaintenanceCosts()).thenReturn(Money.zero());
        when(campaign.getAccountant()).thenReturn(accountant);

        finances = new Finances();
        finances.credit(TransactionType.STARTING_CAPITAL, FIRST_OF_MONTH.minusDays(1), STARTING_FUNDS, "Start");

        // The net worth snapshot at the end of the day builds a financial report
        when(campaign.getPlayerForce().getFinances()).thenReturn(finances);
        when(campaign.getTotalRentFeesExcludingBays()).thenReturn(Money.zero());
        LocalHangar hangar = mock(LocalHangar.class);
        when(campaign.getPlayerForce().getHangar()).thenReturn(hangar);
        LocalWarehouse warehouse = mock(LocalWarehouse.class);
        when(warehouse.streamSpareParts()).thenAnswer(invocation -> Stream.<Part>empty());
        when(campaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
    }

    private List<Transaction> transactionsOfType(TransactionType type) {
        return finances.getTransactions().stream().filter(transaction -> transaction.getType() == type).toList();
    }

    private void newDay(LocalDate today) {
        finances.newDay(campaign, today.minusDays(1), today);
    }

    @Test
    void upkeepIsDebitedOnTheFirstOfTheMonth() {
        newDay(FIRST_OF_MONTH);

        assertEquals(STARTING_FUNDS.minus(UPKEEP), finances.getBalance());
        List<Transaction> maintenance = transactionsOfType(TransactionType.MAINTENANCE);
        assertEquals(1, maintenance.size());
        assertEquals(UPKEEP.multipliedBy(-1), maintenance.get(0).getAmount());
        verify(campaign, atLeastOnce()).addReport(eq(FINANCES), anyString());
    }

    @Test
    void upkeepIsNotDebitedMidMonth() {
        newDay(FIRST_OF_MONTH.plusDays(14));

        assertEquals(STARTING_FUNDS, finances.getBalance());
        assertTrue(transactionsOfType(TransactionType.MAINTENANCE).isEmpty());
    }

    @Test
    void overheadAndPeacetimeCostsAreNotChargedAlongsideUpkeep() {
        newDay(FIRST_OF_MONTH);

        assertTrue(transactionsOfType(TransactionType.OVERHEAD).isEmpty());
        verify(accountant, never()).getOverheadExpenses();
        verify(accountant, never()).getPeacetimeCost(anyBoolean());
    }

    @Test
    void unaffordableUpkeepIsNotDebitedAndIsReported() {
        when(accountant.getHotSpotsUpkeepCosts()).thenReturn(STARTING_FUNDS.plus(Money.of(1)));

        newDay(FIRST_OF_MONTH);

        assertEquals(STARTING_FUNDS, finances.getBalance());
        assertTrue(transactionsOfType(TransactionType.MAINTENANCE).isEmpty());
        verify(campaign, atLeastOnce()).addReport(eq(FINANCES), anyString());
    }

    @Test
    void withoutUpkeepOverheadIsChargedInstead() {
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);
        campaignOptions.set(CampaignOption.USE_PEACETIME_COST, false);

        newDay(FIRST_OF_MONTH);

        assertEquals(STARTING_FUNDS.minus(OVERHEAD), finances.getBalance());
        verify(accountant, never()).getHotSpotsUpkeepCosts();
    }

    /** A force with no Scale owes nothing, so no upkeep is charged and no zero-value transaction is recorded. */
    @Test
    void zeroUpkeepIsNotChargedOrRecorded() {
        when(accountant.getHotSpotsUpkeepCosts()).thenReturn(Money.zero());

        newDay(FIRST_OF_MONTH);

        assertEquals(STARTING_FUNDS, finances.getBalance());
        assertTrue(transactionsOfType(TransactionType.MAINTENANCE).isEmpty());
    }

    /** Straight support covers repairs and maintenance, not peacetime costs. */
    @Test
    void peacetimeCostsAreNotReimbursedByStraightSupport() {
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);
        campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, false);
        try (MockedStatic<ContractSupportPayments> supportPayments = mockStatic(ContractSupportPayments.class)) {
            newDay(FIRST_OF_MONTH);

            supportPayments.verify(() -> ContractSupportPayments.reimburseStraightSupport(any(), any(), any()),
                  never());
        }
        assertEquals(STARTING_FUNDS.minus(PEACETIME), finances.getBalance());
    }
}
