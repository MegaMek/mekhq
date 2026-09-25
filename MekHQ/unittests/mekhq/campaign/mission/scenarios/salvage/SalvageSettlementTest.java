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
package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SalvageSettlementTest {
    private static AbstractContract contract(double salvageRights, boolean isExchange) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getSalvageRightsMultiplier()).thenReturn(salvageRights);
        when(contract.isSalvageExchange()).thenReturn(isExchange);
        return contract;
    }

    /** Money compares by scale in {@code equals}, so compare by value instead. */
    private static void assertMoney(double expected, Money actual) {
        assertEquals(0, Money.of(expected).compareTo(actual), () -> "Expected " + expected + " but was " + actual);
    }

    @Nested
    class Creation {
        @Test
        void cappedRightsWithoutExchangeAreCapped() {
            assertInstanceOf(CappedSalvageSettlement.class,
                  SalvageSettlement.forCappedSalvageRights(contract(0.4, false)));
        }

        @Test
        void cappedRightsWithExchangeAreExchange() {
            assertInstanceOf(ExchangeSalvageSettlement.class,
                  SalvageSettlement.forCappedSalvageRights(contract(0.4, true)));
        }

        @Test
        void purchasesWithoutExchangeArePurchases() {
            assertInstanceOf(PurchaseSalvageSettlement.class,
                  SalvageSettlement.forSalvagePurchases(contract(0.4, false)));
        }

        @Test
        void purchasesWithExchangeAreExchange() {
            assertInstanceOf(ExchangeSalvageSettlement.class,
                  SalvageSettlement.forSalvagePurchases(contract(0.4, true)));
        }

        @Test
        void salvageSystemsCreateTheirSettlements() {
            AbstractContract contract = contract(0.4, false);
            assertInstanceOf(CappedSalvageSettlement.class, new LegacySalvage().createSettlement(contract));
            assertInstanceOf(CappedSalvageSettlement.class, new CamOpsStrictSalvage().createSettlement(contract));
            assertInstanceOf(CappedSalvageSettlement.class, new CamOpsRevisedSalvage().createSettlement(contract));
            assertInstanceOf(PurchaseSalvageSettlement.class, new ChaosCampaignSalvage().createSettlement(contract));
            assertInstanceOf(PurchaseSalvageSettlement.class, new MekHQSalvage().createSettlement(contract));
        }
    }

    @Nested
    class PlayerShare {
        @ParameterizedTest
        @CsvSource({ "0.4, 0.4, 40", "-0.5, 0.0, 0", "1.5, 1.0, 100", "0.25, 0.25, 25" })
        void shareIsClampedAndRounded(double salvageRights, double expectedShare, int expectedPercent) {
            SalvageSettlement settlement = SalvageSettlement.forSalvagePurchases(contract(salvageRights, false));
            assertEquals(expectedShare, settlement.getPlayerShare(), 0.0001);
            assertEquals(expectedPercent, settlement.getPlayerSharePercent());
        }
    }

    @Nested
    class Capped {
        private final SalvageSettlement settlement = new CappedSalvageSettlement(0.4);

        @Test
        void salvageIsCappedAndCanBeKeptOrSold() {
            assertTrue(settlement.isSalvageCapped());
            assertTrue(settlement.canKeepSalvage());
            assertTrue(settlement.canSellSalvage());
            assertFalse(settlement.isKeptSalvageBought());
        }

        @Test
        void noMoneyChangesHands() {
            assertMoney(0, settlement.getPurchaseCost(Money.of(1_000_000)));
            assertMoney(0, settlement.getCashShare(Money.of(1_000_000)));
            assertTrue(settlement.canAfford(Money.of(1_000_000), Money.zero()));
        }
    }

    @Nested
    class Exchange {
        private final SalvageSettlement settlement = new ExchangeSalvageSettlement(0.4);

        @Test
        void salvageCanNeitherBeKeptNorSold() {
            assertFalse(settlement.isSalvageCapped());
            assertFalse(settlement.canKeepSalvage());
            assertFalse(settlement.canSellSalvage());
            assertFalse(settlement.isKeptSalvageBought());
        }

        @Test
        void playerIsPaidTheirShareOfEmployerSalvage() {
            assertMoney(400_000, settlement.getCashShare(Money.of(1_000_000)));
            assertMoney(0, settlement.getPurchaseCost(Money.of(1_000_000)));
        }
    }

    @Nested
    class Purchase {
        private final SalvageSettlement settlement = new PurchaseSalvageSettlement(0.4);

        @Test
        void salvageIsUncappedAndBought() {
            assertFalse(settlement.isSalvageCapped());
            assertTrue(settlement.canKeepSalvage());
            assertFalse(settlement.canSellSalvage());
            assertTrue(settlement.isKeptSalvageBought());
        }

        @Test
        void keptSalvageCostsTheEmployersShare() {
            assertMoney(600_000, settlement.getPurchaseCost(Money.of(1_000_000)));
        }

        @Test
        void unboughtSalvagePaysThePlayersShare() {
            assertMoney(400_000, settlement.getCashShare(Money.of(1_000_000)));
        }

        @Test
        void purchasesMustBeAffordable() {
            assertTrue(settlement.canAfford(Money.of(1_000_000), Money.of(600_000)));
            assertFalse(settlement.canAfford(Money.of(1_000_000), Money.of(599_999)));
        }

        @Test
        void fullSalvageRightsMakeSalvageFree() {
            SalvageSettlement fullRights = new PurchaseSalvageSettlement(1.0);
            assertMoney(0, fullRights.getPurchaseCost(Money.of(1_000_000)));
            assertMoney(1_000_000, fullRights.getCashShare(Money.of(1_000_000)));
        }
    }

    @Nested
    class Payments {
        private static final LocalDate TODAY = LocalDate.of(3025, 1, 1);

        private final Campaign campaign = mockCampaign();
        private final Finances finances = campaign.getPlayerForce().getFinances();
        private final Scenario scenario = mock(Scenario.class);

        Payments() {
            when(campaign.getLocalDate()).thenReturn(TODAY);
            when(scenario.getName()).thenReturn("Battle of Tukayyid");
            when(scenario.getHyperlinkedName()).thenReturn("<a>Battle of Tukayyid</a>");
        }

        private static Money moneyOf(double amount) {
            return argThat(money -> (money != null) && (money.compareTo(Money.of(amount)) == 0));
        }

        @Test
        void purchasesAreChargedAsUnitPurchases() {
            new PurchaseSalvageSettlement(0.4).chargePurchases(campaign, scenario, Money.of(1_000_000));

            verify(finances).debit(eq(TransactionType.UNIT_PURCHASE), eq(TODAY), moneyOf(600_000),
                  argThat(reason -> reason.contains("Battle of Tukayyid") && !reason.startsWith("!")));
            verify(campaign).addReport(eq(DailyReportType.FINANCES),
                  argThat((String report) -> report.contains("<a>Battle of Tukayyid</a>")));
        }

        @Test
        void nothingIsChargedWhenNothingIsBought() {
            new PurchaseSalvageSettlement(0.4).chargePurchases(campaign, scenario, Money.zero());

            verify(finances, never()).debit(any(), any(), any(), anyString());
            verify(campaign, never()).addReport(any(DailyReportType.class), anyString());
        }

        @Test
        void freeSalvageIsNotCharged() {
            new PurchaseSalvageSettlement(1.0).chargePurchases(campaign, scenario, Money.of(1_000_000));

            verify(finances, never()).debit(any(), any(), any(), anyString());
        }

        @Test
        void cappedAndExchangeSettlementsNeverCharge() {
            new CappedSalvageSettlement(0.4).chargePurchases(campaign, scenario, Money.of(1_000_000));
            new ExchangeSalvageSettlement(0.4).chargePurchases(campaign, scenario, Money.of(1_000_000));

            verify(finances, never()).debit(any(), any(), any(), anyString());
        }

        @Test
        void purchaseCashShareIsPaidAsSalvage() {
            Money paid = new PurchaseSalvageSettlement(0.4).payCashShare(campaign, scenario, Money.of(1_000_000));

            assertMoney(400_000, paid);
            verify(finances).credit(eq(TransactionType.SALVAGE), eq(TODAY), moneyOf(400_000),
                  argThat(reason -> reason.contains("Battle of Tukayyid") && !reason.startsWith("!")));
            verify(campaign).addReport(eq(DailyReportType.FINANCES),
                  argThat((String report) -> report.contains("<a>Battle of Tukayyid</a>")));
        }

        @Test
        void exchangeCashShareIsPaidAsSalvageExchange() {
            Money paid = new ExchangeSalvageSettlement(0.4).payCashShare(campaign, scenario, Money.of(1_000_000));

            assertMoney(400_000, paid);
            verify(finances).credit(eq(TransactionType.SALVAGE_EXCHANGE), eq(TODAY), moneyOf(400_000),
                  argThat(reason -> !reason.startsWith("!")));
        }

        @Test
        void exchangeAndPurchaseUseDifferentWording() {
            new ExchangeSalvageSettlement(0.4).payCashShare(campaign, scenario, Money.of(1_000_000));
            new PurchaseSalvageSettlement(0.4).payCashShare(campaign, scenario, Money.of(1_000_000));

            org.mockito.ArgumentCaptor<String> reasons = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(finances, org.mockito.Mockito.times(2)).credit(any(), any(), any(), reasons.capture());
            assertFalse(reasons.getAllValues().get(0).equals(reasons.getAllValues().get(1)));
        }

        @Test
        void cappedSettlementPaysNoCashShare() {
            Money paid = new CappedSalvageSettlement(0.4).payCashShare(campaign, scenario, Money.of(1_000_000));

            assertMoney(0, paid);
            verify(finances, never()).credit(any(), any(), any(), anyString());
            verify(campaign, never()).addReport(any(DailyReportType.class), anyString());
        }

        @Test
        void noCashShareIsPaidWithoutEmployerSalvage() {
            Money paid = new ExchangeSalvageSettlement(0.4).payCashShare(campaign, scenario, Money.zero());

            assertMoney(0, paid);
            verify(finances, never()).credit(any(), any(), any(), anyString());
        }

        @Test
        void noCashShareIsPaidWithoutSalvageRights() {
            Money paid = new PurchaseSalvageSettlement(0.0).payCashShare(campaign, scenario, Money.of(1_000_000));

            assertMoney(0, paid);
            verify(finances, never()).credit(any(), any(), any(), anyString());
        }
    }
}
