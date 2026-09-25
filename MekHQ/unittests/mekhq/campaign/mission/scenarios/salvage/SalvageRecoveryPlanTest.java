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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.bays.ASFBay;
import megamek.common.bays.Bay;
import megamek.common.bays.SmallCraftBay;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.SmallCraft;
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
            Unit tug = tank(30.0, 100.0);
            WreckRecovery teamed = plan.addWreck(wreck(40.0));
            WreckRecovery other = plan.addWreck(wreck(20.0));
            teamed.setRecoveryUnits(tug, tank(30.0, 0.0));

            plan.revalidate();

            assertEquals(RecoveryMethod.DRAG, teamed.getRecoveryMethod());
            assertEquals(RecoveryStatus.COMMITTED, teamed.getStatus());
            assertFalse(plan.isOffered(other, tug, null));
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

            assertEquals(RecoveryMethod.CARRY, recovery.getRecoveryMethod());
            assertEquals(RecoveryStatus.CARRIED_IN_CARGO, recovery.getStatus());
        }

        @Test
        void unitThatCanOnlyDragDefaultsToDragging() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(tank(60.0, 0.0), null);

            plan.revalidate();

            assertEquals(RecoveryMethod.DRAG, recovery.getRecoveryMethod());
            assertEquals(RecoveryStatus.COMMITTED, recovery.getStatus());
        }

        @Test
        void unitThatCanDoNeitherDefaultsToTheCloserMethod() {
            WreckRecovery towCloser = plan.addWreck(wreck(100.0));
            towCloser.setRecoveryUnits(tank(40.0, 10.0), null);
            WreckRecovery cargoCloser = plan.addWreck(wreck(100.0));
            cargoCloser.setRecoveryUnits(tank(10.0, 40.0), null);

            plan.revalidate();

            assertEquals(RecoveryStatus.NO_TOW_CAPACITY, towCloser.getStatus());
            assertEquals(RecoveryStatus.NO_CARGO_CAPACITY, cargoCloser.getStatus());
        }

        @Test
        void choosingToDragWithoutTowCapacityIsInvalid() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(truck(30.0), null);
            plan.revalidate();

            recovery.setPreferredRecoveryMethod(RecoveryMethod.DRAG);
            plan.revalidate();

            assertEquals(RecoveryMethod.DRAG, recovery.getRecoveryMethod());
            assertEquals(RecoveryStatus.NO_TOW_CAPACITY, recovery.getStatus());
            assertTrue(recovery.getStatus().isProblem());
        }

        @Test
        void choosingToCarryWithoutCargoSpaceIsInvalid() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(tank(60.0, 0.0), null);
            plan.revalidate();

            recovery.setPreferredRecoveryMethod(RecoveryMethod.CARRY);
            plan.revalidate();

            assertEquals(RecoveryMethod.CARRY, recovery.getRecoveryMethod());
            assertEquals(RecoveryStatus.NO_CARGO_CAPACITY, recovery.getStatus());
        }

        @Test
        void twoUnitsDragByDefault() {
            WreckRecovery recovery = plan.addWreck(wreck(50.0));
            recovery.setRecoveryUnits(tank(30.0, 0.0), tank(30.0, 0.0));

            plan.revalidate();

            assertEquals(RecoveryMethod.DRAG, recovery.getRecoveryMethod());
            assertEquals(RecoveryStatus.COMMITTED, recovery.getStatus());
        }

        @Test
        void choosingToCarryWithTwoUnitsIsInvalid() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(truck(100.0), tank(30.0, 0.0));
            plan.revalidate();

            recovery.setPreferredRecoveryMethod(RecoveryMethod.CARRY);
            plan.revalidate();

            assertEquals(RecoveryMethod.CARRY, recovery.getRecoveryMethod());
            assertEquals(RecoveryStatus.CARRY_NEEDS_SINGLE_UNIT, recovery.getStatus());
            assertTrue(recovery.getStatus().isProblem());
        }

        @Test
        void unassignedWreckHasNoMethod() {
            WreckRecovery recovery = plan.addWreck(wreck(20.0));

            plan.revalidate();

            assertNull(recovery.getRecoveryMethod());
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
        void choiceIsKeptWhileTheUnitsStayTheSame() {
            Unit tank = tank(60.0, 30.0);
            WreckRecovery recovery = plan.addWreck(wreck(20.0));
            recovery.setRecoveryUnits(tank, null);
            recovery.setPreferredRecoveryMethod(RecoveryMethod.DRAG);

            recovery.setRecoveryUnits(tank, null);
            plan.revalidate();

            assertEquals(RecoveryMethod.DRAG, recovery.getRecoveryMethod());
        }
    }

    @Nested
    class WreckWithoutEntity {
        @Test
        void wreckWithoutEntityIsRecoveredByAnyUnit() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), false);
            WreckRecovery recovery = plan.addWreck(mock(TestUnit.class));
            recovery.setRecoveryUnits(truck(0.0), null);

            plan.revalidate();

            // It can't take a share of the carrier, as there's nothing to weigh
            assertEquals(RecoveryStatus.COMMITTED, recovery.getStatus());
        }

        @Test
        void wreckWithoutEntityIsNeverCarriedAdditionally() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), false);
            Unit carrier = truck(100.0);
            WreckRecovery carried = plan.addWreck(wreck(10.0));
            WreckRecovery unknown = plan.addWreck(mock(TestUnit.class));
            carried.setRecoveryUnits(carrier, null);

            plan.revalidate();

            assertFalse(plan.isOffered(unknown, carrier, null));
        }
    }

    @Nested
    class Space {
        private static Unit carrierWithBays(Bay... bays) {
            Dropship dropship = mock(Dropship.class);
            when(dropship.getTransportBays()).thenReturn(new Vector<>(java.util.List.of(bays)));
            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(dropship);
            when(unit.getCargoCapacityForSalvage()).thenReturn(1000.0);
            return unit;
        }

        private static Bay fighterBay(int doors) {
            return new ASFBay(2, doors, 1);
        }

        private static Bay smallCraftBay(int doors) {
            return new SmallCraftBay(2, doors, 2);
        }

        private static TestUnit fighter() {
            AeroSpaceFighter fighter = mock(AeroSpaceFighter.class);
            when(fighter.getWeight()).thenReturn(50.0);
            return wreckOf(fighter);
        }

        private static TestUnit smallCraft() {
            SmallCraft smallCraft = mock(SmallCraft.class);
            when(smallCraft.getWeight()).thenReturn(200.0);
            return wreckOf(smallCraft);
        }

        @Test
        void noDraggingInSpace() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), true);
            WreckRecovery recovery = plan.addWreck(wreck(40.0));
            recovery.setRecoveryUnits(tank(100.0, 10.0), null);

            plan.revalidate();

            assertEquals(RecoveryStatus.NO_CARGO_CAPACITY, recovery.getStatus());
        }

        @Test
        void otherWrecksInSpaceGoInCargo() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true);
            Unit carrier = carrierWithBays();
            WreckRecovery recovery = plan.addWreck(wreck(40.0));
            recovery.setRecoveryUnits(carrier, null);

            plan.revalidate();

            assertEquals(RecoveryStatus.CARRIED_IN_CARGO, recovery.getStatus());
        }

        @Test
        void fighterNeedsABay() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), true);
            WreckRecovery recovery = plan.addWreck(fighter());
            recovery.setRecoveryUnits(carrierWithBays(), null);

            plan.revalidate();

            assertEquals(RecoveryStatus.NO_SUITABLE_BAY_EQUIPMENT, recovery.getStatus());
        }

        @Test
        void fighterFitsAFighterOrSmallCraftBay() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), true);
            WreckRecovery inFighterBay = plan.addWreck(fighter());
            inFighterBay.setRecoveryUnits(carrierWithBays(fighterBay(1)), null);
            WreckRecovery inSmallCraftBay = plan.addWreck(fighter());
            inSmallCraftBay.setRecoveryUnits(carrierWithBays(smallCraftBay(1)), null);

            plan.revalidate();

            assertEquals(RecoveryStatus.RECOVERED, inFighterBay.getStatus());
            assertEquals(RecoveryStatus.RECOVERED, inSmallCraftBay.getStatus());
        }

        @Test
        void smallCraftDoesNotFitAFighterBay() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), true);
            WreckRecovery recovery = plan.addWreck(smallCraft());
            recovery.setRecoveryUnits(carrierWithBays(fighterBay(1)), null);

            plan.revalidate();

            assertEquals(RecoveryStatus.NO_SUITABLE_BAY_EQUIPMENT, recovery.getStatus());
        }

        @Test
        void bayWithoutWorkingDoorsCantTakeAWreck() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), true);
            WreckRecovery recovery = plan.addWreck(fighter());
            recovery.setRecoveryUnits(carrierWithBays(fighterBay(0)), null);

            plan.revalidate();

            assertEquals(RecoveryStatus.NO_SUITABLE_BAY_EQUIPMENT, recovery.getStatus());
        }

        @Test
        void eitherTeamMemberMayProvideTheBay() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), true);
            WreckRecovery recovery = plan.addWreck(fighter());
            recovery.setRecoveryUnits(carrierWithBays(), carrierWithBays(fighterBay(1)));

            plan.revalidate();

            assertEquals(RecoveryStatus.RECOVERED, recovery.getStatus());
        }

        @Test
        void eachBayHoldsOneWreck() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true);
            Unit carrier = carrierWithBays(fighterBay(1), smallCraftBay(1));
            WreckRecovery first = plan.addWreck(fighter());
            WreckRecovery second = plan.addWreck(fighter());
            WreckRecovery third = plan.addWreck(fighter());
            first.setRecoveryUnits(carrier, null);
            second.setRecoveryUnits(carrier, null);

            plan.revalidate();

            assertEquals(RecoveryStatus.CARRIED_IN_BAY, first.getStatus());
            assertEquals(RecoveryStatus.CARRIED_IN_BAY, second.getStatus());
            assertFalse(plan.isOffered(third, carrier, null));

            SalvageRecoveryPlan.RemainingCapacity remainingCapacity = plan.getRemainingCapacity(carrier);
            assertNotNull(remainingCapacity);
            assertTrue(remainingCapacity.isBayCapacity());
            assertEquals(0, remainingCapacity.freeBays());
        }

        @Test
        void carrierWithFreeBayIsOfferedForAnotherFighter() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true);
            Unit carrier = carrierWithBays(fighterBay(1), fighterBay(1));
            WreckRecovery first = plan.addWreck(fighter());
            WreckRecovery second = plan.addWreck(fighter());
            first.setRecoveryUnits(carrier, null);

            plan.revalidate();

            assertTrue(plan.isOffered(second, carrier, null));
            assertEquals(1, plan.getRemainingCapacity(carrier).freeBays());
        }

        @Test
        void smallCraftTakesTheSmallCraftBayFirst() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true);
            Unit carrier = carrierWithBays(fighterBay(1), smallCraftBay(1));
            WreckRecovery fighter = plan.addWreck(fighter());
            WreckRecovery smallCraft = plan.addWreck(smallCraft());
            fighter.setRecoveryUnits(carrier, null);
            smallCraft.setRecoveryUnits(carrier, null);

            plan.revalidate();

            // The fighter goes in the fighter bay, leaving the small craft bay for the small craft
            assertEquals(RecoveryStatus.CARRIED_IN_BAY, fighter.getStatus());
            assertEquals(RecoveryStatus.CARRIED_IN_BAY, smallCraft.getStatus());
        }

        @Test
        void twoSmallCraftNeedTwoSmallCraftBays() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true);
            Unit carrier = carrierWithBays(fighterBay(1), smallCraftBay(1));
            WreckRecovery first = plan.addWreck(smallCraft());
            WreckRecovery second = plan.addWreck(smallCraft());
            first.setRecoveryUnits(carrier, null);
            second.setRecoveryUnits(carrier, null);

            plan.revalidate();

            assertEquals(RecoveryStatus.NO_FREE_BAY, first.getStatus());
            assertEquals(RecoveryStatus.NO_FREE_BAY, second.getStatus());
        }

        @Test
        void carrierFullOfFightersIsNotOfferedAnotherWreckKind() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true);
            Unit carrier = carrierWithBays(fighterBay(1), smallCraftBay(1));
            WreckRecovery fighter = plan.addWreck(fighter());
            WreckRecovery smallCraft = plan.addWreck(smallCraft());
            Dropship dropship = mock(Dropship.class);
            when(dropship.getWeight()).thenReturn(2000.0);
            WreckRecovery dropshipWreck = plan.addWreck(wreckOf(dropship));
            WreckRecovery cargoWreck = plan.addWreck(wreck(10.0));
            fighter.setRecoveryUnits(carrier, null);
            smallCraft.setRecoveryUnits(carrier, null);
            cargoWreck.setRecoveryUnits(carrier, null);

            plan.revalidate();

            // Tugging would commit the carrier, so a carrier already carrying wrecks can't take on a DropShip
            assertFalse(plan.isOffered(dropshipWreck, carrier, null));
            // Its bays are full, so it can't take another small craft
            WreckRecovery anotherSmallCraft = plan.addWreck(smallCraft());
            plan.revalidate();
            assertFalse(plan.isOffered(anotherSmallCraft, carrier, null));
            // But a small wreck still fits in its cargo space
            WreckRecovery anotherCargoWreck = plan.addWreck(wreck(10.0));
            plan.revalidate();
            assertTrue(plan.isOffered(anotherCargoWreck, carrier, null));
        }

        @Test
        void carrierWithoutEntityHasNoBays() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true);
            Unit carrier = mock(Unit.class);
            WreckRecovery recovery = plan.addWreck(fighter());
            recovery.setRecoveryUnits(carrier, null);

            plan.revalidate();

            assertEquals(RecoveryStatus.NO_SUITABLE_BAY_EQUIPMENT, recovery.getStatus());
        }

        @Test
        void remainingBaysOfACarrierThatLostItsEntityAreZero() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true);
            Unit carrier = carrierWithBays(fighterBay(1), fighterBay(1));
            WreckRecovery recovery = plan.addWreck(fighter());
            recovery.setRecoveryUnits(carrier, null);
            plan.revalidate();

            when(carrier.getEntity()).thenReturn(null);

            assertEquals(0, plan.getRemainingCapacity(carrier).freeBays());
        }

        @Test
        void wreckWithoutEntityInSpaceNeedsCargo() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), true);
            WreckRecovery recovery = plan.addWreck(mock(TestUnit.class));
            recovery.setRecoveryUnits(carrierWithBays(), null);

            plan.revalidate();

            assertEquals(RecoveryStatus.RECOVERED, recovery.getStatus());
        }

        @Test
        void largeVesselWithTugIsTuggedAndCommitsTheTug() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), true);
            Unit tug = mock(Unit.class);
            megamek.common.units.Jumpship tugEntity = mock(megamek.common.units.Jumpship.class);
            when(tug.getEntity()).thenReturn(tugEntity);
            Dropship dropship = mock(Dropship.class);
            when(dropship.getWeight()).thenReturn(2000.0);
            WreckRecovery tugged = plan.addWreck(wreckOf(dropship));
            WreckRecovery other = plan.addWreck(fighter());
            tugged.setRecoveryUnits(tug, null);

            try (org.mockito.MockedStatic<CamOpsSalvageUtilities> utilities =
                       org.mockito.Mockito.mockStatic(CamOpsSalvageUtilities.class,
                             org.mockito.Mockito.CALLS_REAL_METHODS)) {
                utilities.when(() -> CamOpsSalvageUtilities.hasNavalTug(tugEntity)).thenReturn(true);

                plan.revalidate();

                assertEquals(RecoveryStatus.COMMITTED, tugged.getStatus());
                assertFalse(plan.isOffered(other, tug, null));
            }
        }
    }

    @Nested
    class Offers {
        @Test
        void unassignedUnitIsAlwaysOffered() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), false);
            WreckRecovery recovery = plan.addWreck(wreck(20.0));

            plan.revalidate();

            assertTrue(plan.isOffered(recovery, truck(0.0), null));
        }

        @Test
        void carrierAlreadyInATeamSlotIsNotOfferedForSharing() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsRevisedSalvage(), false);
            Unit carrier = truck(100.0);
            WreckRecovery carried = plan.addWreck(wreck(10.0));
            WreckRecovery other = plan.addWreck(wreck(10.0));
            carried.setRecoveryUnits(carrier, null);

            plan.revalidate();

            // Teaming the carrier up with another unit would commit it, so it can't share
            assertFalse(plan.isOffered(other, carrier, truck(0.0)));
            assertTrue(plan.isOffered(other, carrier, null));
        }

        @Test
        void recoveriesAreListedInOrder() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), false);
            WreckRecovery first = plan.addWreck(wreck(10.0));
            WreckRecovery second = plan.addWreck(wreck(10.0));

            assertEquals(java.util.List.of(first, second), plan.getRecoveries());
        }

        @Test
        void recoveriesCantBeChangedFromOutside() {
            SalvageRecoveryPlan plan = new SalvageRecoveryPlan(new CamOpsStrictSalvage(), false);

            org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                  () -> plan.getRecoveries().add(null));
        }

        @Test
        void automaticRecoveryNeverOffersAMethodChoice() {
            assertFalse(new SalvageRecoveryPlan(new ChaosCampaignSalvage(), false).isRecoveryMethodChoiceOffered());
        }
    }
}
