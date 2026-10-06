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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.scenarios.salvage.SalvageRecoverySession.ConfirmBlocker;
import mekhq.campaign.mission.scenarios.salvage.SalvageRecoverySession.SalvageClaim;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SalvageRecoverySessionTest {
    private final Map<UUID, RecoveryTimeData> recoveryTimes = new HashMap<>();

    private TestUnit wreck(double weight, double sellValue, int recoveryMinutes) {
        Entity entity = mock(Mek.class);
        when(entity.getWeight()).thenReturn(weight);
        TestUnit wreck = mock(TestUnit.class);
        UUID id = UUID.randomUUID();
        when(wreck.getId()).thenReturn(id);
        when(wreck.getEntity()).thenReturn(entity);
        when(wreck.getSellValue()).thenReturn(Money.of(sellValue));
        recoveryTimes.put(id, new RecoveryTimeData(0, 0, 0, 0, 0, 0, recoveryMinutes, recoveryMinutes));
        return wreck;
    }

    /** A vehicle that can drag up to its own weight and carry up to its cargo capacity. */
    private static Unit tank(double weight, double cargoCapacity) {
        Tank tank = mock(Tank.class);
        when(tank.getWeight()).thenReturn(weight);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(tank);
        when(unit.getCargoCapacityForSalvage()).thenReturn(cargoCapacity);
        return unit;
    }

    private SalvageRecoverySession session(AbstractSalvage rules, SalvageSettlement settlement,
          List<TestUnit> wrecks, List<Unit> recoveryUnits, int availableMinutes, double unitInitial,
          double employerInitial, double funds) {
        return new SalvageRecoverySession(rules, settlement, false, wrecks, recoveryUnits, recoveryTimes,
              availableMinutes, Money.of(unitInitial), Money.of(employerInitial), Money.of(funds));
    }

    private static void assertMoney(double expected, Money actual) {
        assertEquals(0, Money.of(expected).compareTo(actual), () -> "Expected " + expected + " but was " + actual);
    }

    @Nested
    class Setup {
        @Test
        void wrecksAreListedMostValuableFirst() {
            TestUnit cheap = wreck(20, 100, 60);
            TestUnit dear = wreck(20, 900, 60);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(cheap, dear), List.of(), 0, 0, 0, 0);

            assertSame(dear, session.getRecoveries().get(0).getWreck());
            assertSame(cheap, session.getRecoveries().get(1).getWreck());
        }

        @Test
        void everyWreckStartsWithTheEmployer() {
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 100, 60)), List.of(), 0, 0, 0, 0);

            assertEquals(SalvageClaim.EMPLOYER, session.getClaim(session.getRecoveries().getFirst()));
            assertEquals(1, session.getUnassignedCount());
            assertEquals(0, session.getRecoveredCount());
        }

        @Test
        void negativeTechTimeIsTreatedAsNone() {
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(), List.of(), -30, 0, 0, 0);

            assertEquals(0, session.getAvailableMinutes());
        }

        @Test
        void automaticRecoveryRecoversEverything() {
            SalvageRecoverySession session = session(new ChaosCampaignSalvage(),
                  new PurchaseSalvageSettlement(0.25), List.of(wreck(20, 100, 60), wreck(30, 200, 90)), List.of(), 0,
                  0, 0, 0);

            assertFalse(session.isUsingSalvageOperations());
            assertEquals(2, session.getRecoveredCount());
            assertEquals(0, session.getUsedMinutes());
            assertTrue(session.canConfirm());
        }
    }

    @Nested
    class Claims {
        @Test
        void cappedSettlementsOfferEmployerKeepAndSell() {
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(), List.of(), 0, 0, 0, 0);

            assertEquals(List.of(SalvageClaim.EMPLOYER, SalvageClaim.KEEP, SalvageClaim.SELL),
                  session.getAvailableClaims());
        }

        @Test
        void purchaseSettlementsOfferEmployerAndBuy() {
            SalvageRecoverySession session = session(new MekHQSalvage(), new PurchaseSalvageSettlement(0.5),
                  List.of(), List.of(), 0, 0, 0, 0);

            assertEquals(List.of(SalvageClaim.EMPLOYER, SalvageClaim.KEEP), session.getAvailableClaims());
        }

        @Test
        void exchangeSettlementsOnlyOfferTheEmployer() {
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new ExchangeSalvageSettlement(0.5),
                  List.of(), List.of(), 0, 0, 0, 0);

            assertEquals(List.of(SalvageClaim.EMPLOYER), session.getAvailableClaims());
        }

        @Test
        void unrecoveredWrecksCantBeClaimed() {
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 100, 60)), List.of(), 0, 0, 0, 0);
            WreckRecovery recovery = session.getRecoveries().getFirst();

            assertFalse(session.setClaim(recovery, SalvageClaim.KEEP));
            assertEquals(SalvageClaim.EMPLOYER, session.getClaim(recovery));
        }

        @Test
        void claimsTheSettlementDoesntOfferAreRefused() {
            SalvageRecoverySession session = session(new ChaosCampaignSalvage(), new PurchaseSalvageSettlement(0.5),
                  List.of(wreck(20, 100, 60)), List.of(), 0, 0, 0, 0);

            assertFalse(session.setClaim(session.getRecoveries().getFirst(), SalvageClaim.SELL));
        }

        @Test
        void losingTheRecoveryHandsTheWreckBackToTheEmployer() {
            Unit truck = tank(0, 50);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 100, 60)), List.of(truck), 600, 0, 0, 0);
            WreckRecovery recovery = session.getRecoveries().getFirst();
            session.assign(recovery, truck, null);
            assertTrue(session.setClaim(recovery, SalvageClaim.KEEP));

            session.assign(recovery, null, null);

            assertEquals(SalvageClaim.EMPLOYER, session.getClaim(recovery));
            assertTrue(session.getKeptSalvage().isEmpty());
        }
    }

    @Nested
    class Tallies {
        private final Unit truckA = tank(0, 100);
        private final Unit truckB = tank(0, 100);
        private final Unit truckC = tank(0, 100);
        private final TestUnit kept = wreck(20, 1000, 60);
        private final TestUnit sold = wreck(20, 500, 90);
        private final TestUnit employer = wreck(20, 400, 30);
        private final TestUnit unrecovered = wreck(20, 50, 45);

        private SalvageRecoverySession assigned(SalvageSettlement settlement, double funds) {
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), settlement,
                  List.of(kept, sold, employer, unrecovered), List.of(truckA, truckB, truckC), 600, 2000, 6000,
                  funds);
            List<WreckRecovery> recoveries = session.getRecoveries();
            session.assign(recoveries.get(0), truckA, null);
            session.assign(recoveries.get(1), truckB, null);
            session.assign(recoveries.get(2), truckC, null);
            session.setClaim(recoveries.get(0), SalvageClaim.KEEP);
            session.setClaim(recoveries.get(1), SalvageClaim.SELL);
            return session;
        }

        @Test
        void recoveredWrecksAreSortedByClaim() {
            SalvageRecoverySession session = assigned(new CappedSalvageSettlement(0.5), 0);

            assertEquals(List.of(kept), session.getKeptSalvage());
            assertEquals(List.of(sold), session.getSoldSalvage());
            assertEquals(List.of(employer), session.getEmployerSalvage());
            assertEquals(3, session.getRecoveredCount());
            assertEquals(1, session.getUnassignedCount());
        }

        @Test
        void onlyAssignedWrecksUseTechTime() {
            assertEquals(180, assigned(new CappedSalvageSettlement(0.5), 0).getUsedMinutes());
        }

        @Test
        void totalsIncludeTheContractSoFar() {
            SalvageRecoverySession session = assigned(new CappedSalvageSettlement(0.5), 0);

            assertMoney(1000, session.getKeptValue());
            assertMoney(500, session.getSoldValue());
            assertMoney(400, session.getEmployerValue());
            assertMoney(3500, session.getUnitSalvageTotal());
            assertMoney(6400, session.getEmployerSalvageTotal());
        }

        @Test
        void salvagePercentIsTheUnitsShareOfAllSalvage() {
            SalvageRecoverySession session = assigned(new CappedSalvageSettlement(0.5), 0);

            // 3500 / (3500 + 6400)
            assertEquals(0, new BigDecimal("35.3535").compareTo(session.getSalvagePercent()));
            assertFalse(session.isOverSalvageCap());
        }

        @Test
        void salvagePercentIsZeroWithoutAnySalvage() {
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(), List.of(), 0, 0, 0, 0);

            assertEquals(0, BigDecimal.ZERO.compareTo(session.getSalvagePercent()));
        }

        @Test
        void purchasesCostTheEmployersShareAndPayTheUnitsShare() {
            SalvageRecoverySession session = session(new MekHQSalvage(), new PurchaseSalvageSettlement(0.25),
                  List.of(kept, employer), List.of(truckA, truckB), 600, 0, 0, 10000);
            session.assign(session.getRecoveries().get(0), truckA, null);
            session.assign(session.getRecoveries().get(1), truckB, null);
            session.setClaim(session.getRecoveries().get(0), SalvageClaim.KEEP);

            assertMoney(750, session.getPurchaseCost());
            assertMoney(100, session.getCashShare());
        }

        @Test
        void exchangeTotalAddsTheCashShare() {
            SalvageRecoverySession session = assigned(new ExchangeSalvageSettlement(0.25), 0);

            // Exchange can't keep or sell, so every recovered wreck goes to the employer: 1000 + 500 + 400
            assertMoney(1900, session.getEmployerValue());
            assertMoney(2000 + 475, session.getExchangeUnitTotal());
        }
    }

    @Nested
    class RecoveryTimes {
        @Test
        void wrecksWithoutRecoveryTimeTakeNone() {
            TestUnit wreck = wreck(20, 100, 60);
            recoveryTimes.clear();
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck), List.of(), 600, 0, 0, 0);
            WreckRecovery recovery = session.getRecoveries().getFirst();

            assertEquals(0, session.getRecoveryMinutes(recovery));
            assertNull(session.getRecoveryTimeData(recovery));
        }

        @Test
        void recoveryTimeDataIsLookedUpByWreck() {
            TestUnit wreck = wreck(20, 100, 75);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck), List.of(), 600, 0, 0, 0);

            assertEquals(75, session.getRecoveryTimeData(session.getRecoveries().getFirst()).totalRecoveryTime());
        }
    }

    @Nested
    class ConfirmBlockers {
        @Test
        void unrecoverableAssignmentsBlock() {
            Unit smallTruck = tank(0, 5);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 100, 60)), List.of(smallTruck), 600, 0, 0, 0);
            session.assign(session.getRecoveries().getFirst(), smallTruck, null);

            assertEquals(1, session.getProblemCount());
            assertEquals(List.of(ConfirmBlocker.UNRECOVERABLE_ASSIGNMENTS), session.getConfirmBlockers());
            assertFalse(session.canConfirm());
        }

        @Test
        void claimingOverTheCapBlocks() {
            Unit truck = tank(0, 100);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.1),
                  List.of(wreck(20, 1000, 60)), List.of(truck), 600, 0, 1000, 0);
            session.assign(session.getRecoveries().getFirst(), truck, null);
            session.setClaim(session.getRecoveries().getFirst(), SalvageClaim.KEEP);

            assertTrue(session.isOverSalvageCap());
            assertEquals(List.of(ConfirmBlocker.OVER_SALVAGE_CAP), session.getConfirmBlockers());
        }

        @Test
        void alreadyBeingOverTheCapOnlyBlocksNewClaims() {
            Unit truck = tank(0, 100);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.1),
                  List.of(wreck(20, 1000, 60)), List.of(truck), 600, 5000, 1000, 0);
            session.assign(session.getRecoveries().getFirst(), truck, null);

            assertTrue(session.isOverSalvageCap());
            assertTrue(session.canConfirm());
        }

        @Test
        void exchangeIsNeverCapped() {
            Unit truck = tank(0, 100);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new ExchangeSalvageSettlement(0.1),
                  List.of(wreck(20, 1000, 60)), List.of(truck), 600, 5000, 0, 0);

            assertFalse(session.isOverSalvageCap());
        }

        @Test
        void unaffordablePurchasesBlock() {
            SalvageRecoverySession session = session(new ChaosCampaignSalvage(), new PurchaseSalvageSettlement(0.25),
                  List.of(wreck(20, 1000, 60)), List.of(), 0, 0, 0, 749);
            session.setClaim(session.getRecoveries().getFirst(), SalvageClaim.KEEP);

            assertEquals(List.of(ConfirmBlocker.UNAFFORDABLE_PURCHASES), session.getConfirmBlockers());
        }

        @Test
        void runningOutOfTechTimeBlocks() {
            Unit truck = tank(0, 100);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 100, 90)), List.of(truck), 60, 0, 0, 0);
            session.assign(session.getRecoveries().getFirst(), truck, null);

            assertEquals(List.of(ConfirmBlocker.NOT_ENOUGH_TECH_TIME), session.getConfirmBlockers());
        }

        @Test
        void blockersAreListedTogether() {
            Unit smallTruck = tank(0, 5);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 100, 90)), List.of(smallTruck), 60, 0, 0, 0);
            session.assign(session.getRecoveries().getFirst(), smallTruck, null);

            assertEquals(List.of(ConfirmBlocker.UNRECOVERABLE_ASSIGNMENTS, ConfirmBlocker.NOT_ENOUGH_TECH_TIME),
                  session.getConfirmBlockers());
        }
    }

    @Nested
    class Assignments {
        @Test
        void freeSlotsFillInOrder() {
            Unit first = tank(40, 0);
            Unit second = tank(40, 0);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(70, 100, 60)), List.of(first, second), 600, 0, 0, 0);
            WreckRecovery recovery = session.getRecoveries().getFirst();

            assertTrue(session.assignToFreeSlot(recovery, first));
            assertTrue(session.assignToFreeSlot(recovery, second));
            assertFalse(session.assignToFreeSlot(recovery, tank(40, 0)), "both slots are taken");

            assertSame(first, recovery.getFirstUnit());
            assertSame(second, recovery.getSecondUnit());
            assertTrue(recovery.isRecovered());
        }

        @Test
        void aUnitCantFillBothSlots() {
            Unit unit = tank(40, 0);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(70, 100, 60)), List.of(unit), 600, 0, 0, 0);
            WreckRecovery recovery = session.getRecoveries().getFirst();
            session.assignToFreeSlot(recovery, unit);

            assertFalse(session.assignToFreeSlot(recovery, unit));
        }

        @Test
        void secondSlotIsFilledWhenOnlyItIsFree() {
            Unit first = tank(40, 0);
            Unit second = tank(40, 0);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(70, 100, 60)), List.of(first, second), 600, 0, 0, 0);
            WreckRecovery recovery = session.getRecoveries().getFirst();
            session.assign(recovery, null, first);

            assertTrue(session.assignToFreeSlot(recovery, second));
            assertSame(second, recovery.getFirstUnit());
        }

        @Test
        void aCommittedUnitCantBeAssignedElsewhere() {
            Unit unit = tank(40, 0);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(30, 100, 60), wreck(30, 50, 60)), List.of(unit), 600, 0, 0, 0);
            session.assignToFreeSlot(session.getRecoveries().get(0), unit);

            assertFalse(session.assignToFreeSlot(session.getRecoveries().get(1), unit));
        }

        @Test
        void clearingUnassignsEverything() {
            Unit truck = tank(0, 100);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 100, 60)), List.of(truck), 600, 0, 0, 0);
            session.assign(session.getRecoveries().getFirst(), truck, null);

            session.clearAssignments();

            assertEquals(1, session.getUnassignedCount());
            assertNull(session.getRecoveries().getFirst().getFirstUnit());
        }

        @Test
        void preferredMethodIsApplied() {
            Unit tank = tank(60, 30);
            SalvageRecoverySession session = session(new CamOpsRevisedSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 100, 60)), List.of(tank), 600, 0, 0, 0);
            WreckRecovery recovery = session.getRecoveries().getFirst();
            session.assign(recovery, tank, null);

            session.setPreferredRecoveryMethod(recovery, RecoveryMethod.DRAG);

            assertEquals(RecoveryStatus.COMMITTED, recovery.getStatus());
        }
    }

    @Nested
    class AutoAssign {
        @Test
        void mostValuableWrecksGetTheSmallestSufficientUnit() {
            Unit bigTruck = tank(0, 100);
            Unit smallTruck = tank(0, 25);
            TestUnit valuableHeavy = wreck(80, 1000, 60);
            TestUnit cheapLight = wreck(20, 100, 60);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(cheapLight, valuableHeavy), List.of(bigTruck, smallTruck), 600, 0, 0, 0);

            assertEquals(2, session.autoAssign());

            assertSame(bigTruck, session.getRecoveries().get(0).getFirstUnit());
            assertSame(smallTruck, session.getRecoveries().get(1).getFirstUnit());
            assertTrue(session.canConfirm());
        }

        @Test
        void pairsAreUsedWhenNoSingleUnitCanRecoverTheWreck() {
            Unit first = tank(40, 0);
            Unit second = tank(40, 0);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(70, 1000, 60)), List.of(first, second), 600, 0, 0, 0);

            assertEquals(1, session.autoAssign());

            WreckRecovery recovery = session.getRecoveries().getFirst();
            assertTrue(recovery.isRecovered());
            assertEquals(2, recovery.getRecoveryUnits().size());
        }

        @Test
        void techTimeIsRespected() {
            Unit truckA = tank(0, 100);
            Unit truckB = tank(0, 100);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 1000, 90), wreck(20, 500, 90)), List.of(truckA, truckB), 100, 0, 0, 0);

            assertEquals(1, session.autoAssign());
            assertTrue(session.canConfirm());
        }

        @Test
        void wrecksNobodyCanRecoverAreLeftUnassigned() {
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(100, 1000, 60)), List.of(tank(10, 10)), 600, 0, 0, 0);

            assertEquals(0, session.autoAssign());
            assertEquals(RecoveryStatus.UNASSIGNED, session.getRecoveries().getFirst().getStatus());
        }

        @Test
        void playerAssignmentsAreKept() {
            Unit bigTruck = tank(0, 100);
            Unit smallTruck = tank(0, 25);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 1000, 60)), List.of(bigTruck, smallTruck), 600, 0, 0, 0);
            session.assign(session.getRecoveries().getFirst(), bigTruck, null);

            assertEquals(0, session.autoAssign());
            assertSame(bigTruck, session.getRecoveries().getFirst().getFirstUnit());
        }

        @Test
        void sharedCargoIsFilled() {
            Unit truck = tank(0, 50);
            SalvageRecoverySession session = session(new CamOpsRevisedSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(20, 1000, 60), wreck(20, 900, 60), wreck(20, 800, 60)), List.of(truck), 600, 0, 0,
                  0);

            assertEquals(2, session.autoAssign(), "only two 20 t wrecks fit in 50 t");
            assertTrue(session.canConfirm());
        }

        @Test
        void pairsSkipUnitsAlreadyInUse() {
            Unit busy = tank(40, 0);
            Unit first = tank(40, 0);
            Unit second = tank(40, 0);
            SalvageRecoverySession session = session(new CamOpsStrictSalvage(), new CappedSalvageSettlement(0.5),
                  List.of(wreck(30, 2000, 60), wreck(70, 1000, 60)), List.of(busy, first, second), 600, 0, 0, 0);
            session.assign(session.getRecoveries().get(0), busy, null);

            assertEquals(1, session.autoAssign());

            WreckRecovery pairRecovery = session.getRecoveries().get(1);
            assertFalse(pairRecovery.getRecoveryUnits().contains(busy));
            assertTrue(pairRecovery.isRecovered());
        }

        @Test
        void automaticRecoveryNeedsNoAssigning() {
            SalvageRecoverySession session = session(new ChaosCampaignSalvage(), new PurchaseSalvageSettlement(0.5),
                  new ArrayList<>(List.of(wreck(20, 1000, 60))), List.of(tank(0, 100)), 600, 0, 0, 0);

            assertEquals(0, session.autoAssign());
        }
    }
}
