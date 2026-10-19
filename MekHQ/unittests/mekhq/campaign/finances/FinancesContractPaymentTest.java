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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
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
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.parts.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the monthly contract payment in {@link Finances#newDay}: credited on the first of the month to every active
 * contract, except one already breached, which pays nothing more.
 */
class FinancesContractPaymentTest {
    private static final LocalDate FIRST_OF_MONTH = LocalDate.of(3052, 6, 1);
    private static final Money STARTING_FUNDS = Money.of(10_000_000);
    private static final Money MONTHLY_PAY = Money.of(250_000);

    private Campaign campaign;
    private AbstractContract contract;
    private Finances finances;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);
        campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, false);
        campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, false);
        campaignOptions.set(CampaignOption.USE_PEACETIME_COST, false);
        campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, false);
        campaignOptions.set(CampaignOption.USE_TAXES, false);
        campaignOptions.set(CampaignOption.USE_SHARE_SYSTEM, false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(FIRST_OF_MONTH);

        contract = mock(AbstractContract.class);
        when(contract.getMonthlyPayOut()).thenReturn(MONTHLY_PAY);
        when(contract.getName()).thenReturn("Test Contract");
        when(contract.getHyperlinkedName()).thenReturn("Test Contract");
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        Accountant accountant = mock(Accountant.class);
        when(accountant.getPeacetimeCost(anyBoolean())).thenReturn(Money.zero());
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

    private List<Transaction> contractPayments() {
        return finances.getTransactions()
                     .stream()
                     .filter(transaction -> transaction.getType() == TransactionType.CONTRACT_PAYMENT)
                     .toList();
    }

    private void newDay(LocalDate today) {
        finances.newDay(campaign, today.minusDays(1), today);
    }

    @Test
    void activeContractIsPaidOnTheFirstOfTheMonth() {
        newDay(FIRST_OF_MONTH);

        assertEquals(1, contractPayments().size());
        assertEquals(STARTING_FUNDS.plus(MONTHLY_PAY), finances.getBalance());
    }

    /** A contract breached on the last day of a month is still active on its final day, the first. */
    @Test
    void breachedContractIsNotPaid() {
        when(contract.getMandatedCompletionStatus()).thenReturn(MissionStatus.BREACH);

        newDay(FIRST_OF_MONTH);

        assertTrue(contractPayments().isEmpty());
        assertEquals(STARTING_FUNDS, finances.getBalance());
    }

    /** Only a breach withholds pay: a contract that must end as a partial success is still paid until it ends. */
    @Test
    void contractEndingAsPartialIsStillPaid() {
        when(contract.getMandatedCompletionStatus()).thenReturn(MissionStatus.PARTIAL);

        newDay(FIRST_OF_MONTH);

        assertEquals(1, contractPayments().size());
    }
}
