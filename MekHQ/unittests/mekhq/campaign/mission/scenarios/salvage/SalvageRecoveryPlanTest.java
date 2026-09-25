package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SalvageRecoveryPlanTest {
    private static final double DELTA = 0.0001;

    private static TestUnit wreck(double weight) {
        Entity entity = mock(Mek.class);
        when(entity.getWeight()).thenReturn(weight);
        return wreckOf(entity);
    }

    private static TestUnit wreckOf(Entity entity) {
        TestUnit wreck = mock(TestUnit.class);
        when(wreck.getEntity()).thenReturn(entity);
        return wreck;
    }

    /**
     * A tank that can drag up to its own weight, and carry up to its cargo capacity.
     */
    private static Unit tank(double weight, double cargoCapacity) {
        Tank tank = mock(Tank.class);
        when(tank.getWeight()).thenReturn(weight);
        when(tank.isTrailer()).thenReturn(false);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(tank);
        when(unit.getCargoCapacityForSalvage()).thenReturn(cargoCapacity);
        return unit;
    }

    /** A truck with cargo space that can't drag anything worth mentioning. */
    private static Unit truck(double cargoCapacity) {
        return tank(0.0, cargoCapacity);
    }

    @Nested
    class AutomaticRecovery {
        @Test
        void everyWreckIsRecoveredWithoutSalvageOperations() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new ChaosCampaignSalvage(), false);
            WreckRecovery recovery = plan.addWreck(wreck(100.0));

            plan.revalidate();

            assertEquals(RecoveryStatus.RECOVERED_AUTOMATICALLY, recovery.getStatus());
            assertTrue(recovery.isRecovered());
        }
    }

    @Nested
    class SingleWreck {
        private final SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), false);

        @Test
        void unassignedWreckIsNotAProblem() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));

            plan.revalidate();

            assertEquals(RecoveryStatus.UNASSIGNED, recovery.getStatus());
            assertFalse(recovery.isRecovered());
            assertFalse(recovery.getStatus().isProblem());
        }

        @Test
        void wreckThatFitsInCargoIsRecovered() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(truck(20.0), null);

            plan.revalidate();

            assertEquals(RecoveryStatus.RECOVERED, recovery.getStatus());
        }

        @Test
        void cargoCapacityIsNotCombined() {
            WreckRecovery recovery = plan.addWreck(wreck(30.0));
            recovery.setRecoveryUnits(truck(20.0), truck(20.0));

            plan.revalidate();

            assertEquals(RecoveryStatus.NO_CARGO_CAPACITY, recovery.getStatus());
            assertTrue(recovery.getStatus().isProblem());
        }

        @Test
        void towCapacityIsCombined() {
            WreckRecovery recovery = plan.addWreck(wreck(60.0));
            recovery.setRecoveryUnits(tank(30.0, 0.0), tank(30.0, 0.0));

            plan.revalidate();

            assertEquals(RecoveryStatus.RECOVERED, recovery.getStatus());
        }

        @Test
        void closestShortfallIsReported() {
            WreckRecovery dragShortfall = plan.addWreck(wreck(100.0));
            dragShortfall.setRecoveryUnits(tank(40.0, 10.0), null);
            WreckRecovery cargoShortfall = plan.addWreck(wreck(100.0));
            cargoShortfall.setRecoveryUnits(tank(10.0, 40.0), null);

            plan.revalidate();

            assertEquals(RecoveryStatus.NO_TOW_CAPACITY, dragShortfall.getStatus());
            assertEquals(RecoveryStatus.NO_CARGO_CAPACITY, cargoShortfall.getStatus());
        }

        @Test
        void largeVesselInSpaceNeedsNavalTug() {
            SalvageRecoveryPlan spacePlan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), true);
            Dropship dropship = mock(Dropship.class);
            when(dropship.getWeight()).thenReturn(2000.0);
            WreckRecovery recovery = spacePlan.addWreck(wreckOf(dropship));
            recovery.setRecoveryUnits(truck(5000.0), null);

            spacePlan.revalidate();

            assertEquals(RecoveryStatus.NO_NAVAL_TUG, recovery.getStatus());
        }
    }

    @Nested
    class OneWreckPerUnit {
        private final SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), false);

        @Test
        void assignedUnitIsNotOfferedElsewhere() {
            Unit carrier = truck(100.0);
            WreckRecovery first = plan.addWreck(wreck(20.0));
            WreckRecovery second = plan.addWreck(wreck(20.0));
            first.setRecoveryUnits(carrier, null);

            plan.revalidate();

            assertFalse(plan.isOffered(second, carrier, null));
            assertTrue(plan.isOffered(second, truck(100.0), null));
        }

        @Test
        void sameUnitCantFillBothSlots() {
            Unit carrier = truck(100.0);
            WreckRecovery recovery = plan.addWreck(wreck(20.0));

            plan.revalidate();

            assertFalse(plan.isOffered(recovery, carrier, carrier));
        }

        @Test
        void recoveryMethodChoiceIsNotOffered() {
            assertFalse(plan.isRecoveryMethodChoiceOffered());
        }
    }

    @Nested
    class SharedCapacity {
        private final SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), false);

        @Test
        void carrierSharesItsCargoBetweenWrecks() {
            Unit carrier = truck(50.0);
            WreckRecovery first = plan.addWreck(wreck(20.0));
            WreckRecovery second = plan.addWreck(wreck(20.0));
            WreckRecovery third = plan.addWreck(wreck(20.0));
            first.setRecoveryUnits(carrier, null);
            second.setRecoveryUnits(carrier, null);

            plan.revalidate();

            assertEquals(RecoveryStatus.CARRIED_IN_CARGO, first.getStatus());
            assertEquals(RecoveryStatus.CARRIED_IN_CARGO, second.getStatus());
            assertFalse(plan.isOffered(third, carrier, null), "Only 10 tons are left");

            SalvageRecoveryPlan.RemainingCapacity remainingCapacity = plan.getRemainingCapacity(carrier);
            assertNotNull(remainingCapacity);
            assertFalse(remainingCapacity.isBayCapacity());
            assertEquals(10.0, remainingCapacity.freeCargoTons(), DELTA);
        }

        @Test
        void overloadedCarrierRecoversNothing() {
            Unit carrier = truck(50.0);
            WreckRecovery first = plan.addWreck(wreck(30.0));
            WreckRecovery second = plan.addWreck(wreck(30.0));
            first.setRecoveryUnits(carrier, null);
            second.setRecoveryUnits(carrier, null);

            plan.revalidate();

            assertEquals(RecoveryStatus.CARGO_FULL, first.getStatus());
            assertEquals(RecoveryStatus.CARGO_FULL, second.getStatus());
            assertNull(plan.getRemainingCapacity(carrier));
        }

        @Test
        void draggingCommitsTheUnit() {
            Unit tug = tank(60.0, 0.0);
            WreckRecovery dragged = plan.addWreck(wreck(40.0));
            WreckRecovery other = plan.addWreck(wreck(40.0));
            dragged.setRecoveryUnits(tug, null);

            plan.revalidate();

            assertEquals(RecoveryStatus.COMMITTED, dragged.getStatus());
            assertEquals(RecoveryMethod.DRAG, dragged.getRecoveryMethod());
            assertFalse(plan.isOffered(other, tug, null));
        }

        @Test
        void committedUnitUsedElsewhereInvalidatesBoth() {
            Unit tug = tank(60.0, 0.0);
            WreckRecovery first = plan.addWreck(wreck(40.0));
            WreckRecovery second = plan.addWreck(wreck(40.0));
            first.setRecoveryUnits(tug, null);
            second.setRecoveryUnits(tug, null);

            plan.revalidate();

            assertEquals(RecoveryStatus.UNIT_IN_USE, first.getStatus());
            assertEquals(RecoveryStatus.UNIT_IN_USE, second.getStatus());
        }

        @Test
        void twoUnitTeamsAreCommitted() {
            Unit carrier = truck(100.0);
            WreckRecovery teamed = plan.addWreck(wreck(20.0));
            WreckRecovery other = plan.addWreck(wreck(20.0));
            teamed.setRecoveryUnits(carrier, truck(100.0));

            plan.revalidate();

            assertEquals(RecoveryStatus.COMMITTED, teamed.getStatus());
            assertFalse(plan.isOffered(other, carrier, null));
        }
    }

    @Nested
    class RecoveryMethodChoice {
        private final SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), false);

        @Test
        void choiceIsOfferedOnTheGroundOnly() {
            assertTrue(plan.isRecoveryMethodChoiceOffered());
            assertFalse(new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true).isRecoveryMethodChoiceOffered());
        }

        @Test
        void unitThatCanDoBothDefaultsToCarrying() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(tank(60.0, 30.0), null);

            plan.revalidate();

            assertTrue(recovery.isRecoveryMethodChoosable());
            assertEquals(RecoveryMethod.CARRY, recovery.getRecoveryMethod());
            assertEquals(RecoveryStatus.CARRIED_IN_CARGO, recovery.getStatus());
        }

        @Test
        void playerMayChooseToDrag() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(tank(60.0, 30.0), null);
            plan.revalidate();

            recovery.setPreferredRecoveryMethod(RecoveryMethod.DRAG);
            plan.revalidate();

            assertEquals(RecoveryMethod.DRAG, recovery.getRecoveryMethod());
            assertEquals(RecoveryStatus.COMMITTED, recovery.getStatus());
        }

        @Test
        void choiceResetsWhenTheUnitChanges() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(tank(60.0, 30.0), null);
            plan.revalidate();
            recovery.setPreferredRecoveryMethod(RecoveryMethod.DRAG);
            plan.revalidate();

            recovery.setRecoveryUnits(tank(60.0, 30.0), null);
            plan.revalidate();

            assertEquals(RecoveryMethod.CARRY, recovery.getRecoveryMethod());
        }

        @Test
        void unitThatCanOnlyCarryIsNotGivenAChoice() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(truck(30.0), null);

            plan.revalidate();

            assertFalse(recovery.isRecoveryMethodChoosable());
            assertEquals(RecoveryMethod.CARRY, recovery.getRecoveryMethod());
        }
    }
}
