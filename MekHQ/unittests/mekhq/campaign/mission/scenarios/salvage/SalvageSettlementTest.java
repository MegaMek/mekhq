package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
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
}
