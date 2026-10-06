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
package mekhq.campaign.mission.contract.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import megamek.common.units.Entity;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests that {@link ContractAutomation}'s automated mothballing and activation are scoped to a single
 * {@link Detachment} and never touch another detachment's units.
 */
class ContractAutomationTest {

    /** A non-large-craft unit that is available and not under repair, so it is eligible for auto-mothballing. */
    private static Unit mothballableUnit() {
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        when(entity.isLargeCraft()).thenReturn(false);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.isAvailable(false)).thenReturn(true);
        when(unit.isUnderRepair()).thenReturn(false);
        return unit;
    }

    private static Campaign campaignWithFormationUnits(PlayerForce force, Formation formation) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getPlayerForce()).thenReturn(force);
        when(force.getAllFormations()).thenReturn(List.of(formation));
        return campaign;
    }

    @Nested
    class Mothballing {
        @Test
        void onlyMothballsUnitsBelongingToTheGivenDetachment() {
            UUID idInDetachment = UUID.randomUUID();
            UUID idOtherDetachment = UUID.randomUUID();

            Unit unitInDetachment = mothballableUnit();
            Unit unitOtherDetachment = mothballableUnit();

            when(unitInDetachment.getId()).thenReturn(idInDetachment);
            when(unitOtherDetachment.getId()).thenReturn(idOtherDetachment);

            PlayerForce force = mock(PlayerForce.class);
            Formation formation = mock(Formation.class);

            // Both units sit in the shared TO&E...
            when(formation.getUnits())
                  .thenReturn(new Vector<>(List.of(idInDetachment, idOtherDetachment)));

            Campaign campaign = campaignWithFormationUnits(force, formation);
            when(campaign.getUnit(idInDetachment)).thenReturn(unitInDetachment);
            when(campaign.getUnit(idOtherDetachment)).thenReturn(unitOtherDetachment);

            // ...but only one of them is physically at this detachment.
            Detachment detachment = spy(new Detachment());
            LocalHangar hangar = mock(LocalHangar.class);
            doReturn(hangar).when(detachment).getHangar();
            when(hangar.getUnits()).thenReturn(List.of(unitInDetachment));

            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                ContractAutomation.performAutomatedMothballing(campaign, detachment);

                // The list is recorded on the detachment, not force-wide.
                assertIterableEquals(
                      List.of(idInDetachment),
                      detachment.getAutomatedMothballUnits());

                // The unit from this detachment is mothballed.
                verify(unitInDetachment).startMothballing(null, true);

                // The unit from another detachment is never mothballed.
                verify(unitOtherDetachment, never()).startMothballing(null, true);
            }
        }

        @Test
        void largeCraftAreSkipped() {
            UUID id = UUID.randomUUID();
            Unit dropShip = mock(Unit.class);
            Entity entity = mock(Entity.class);
            when(entity.isLargeCraft()).thenReturn(true);
            when(dropShip.getEntity()).thenReturn(entity);

            PlayerForce force = mock(PlayerForce.class);
            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(id)));
            Campaign campaign = campaignWithFormationUnits(force, formation);
            when(campaign.getUnit(id)).thenReturn(dropShip);

            Detachment detachment = spy(new Detachment());
            LocalHangar hangar = mock(LocalHangar.class);
            doReturn(hangar).when(detachment).getHangar();
            when(hangar.getUnits()).thenReturn(List.of(dropShip));

            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                ContractAutomation.performAutomatedMothballing(campaign, detachment);

                assertTrue(detachment.getAutomatedMothballUnits().isEmpty());
                verify(dropShip, never()).startMothballing(null, true);
            }
        }
    }

    /**
     * Tests the "Don't Mothball Units in Bays When Travelling" and "Don't Mothball Salvage When Travelling" client
     * options.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Nested
    class TravelMothballOptions {
        /** Wires a single-unit detachment and returns it; the unit sits in the only formation. */
        private static Detachment detachmentFor(Campaign campaign, UUID unitId, Unit unit) {
            PlayerForce force = mock(PlayerForce.class);
            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(unitId)));
            when(force.getAllFormations()).thenReturn(List.of(formation));
            when(campaign.getPlayerForce()).thenReturn(force);
            when(campaign.getUnit(unitId)).thenReturn(unit);

            Detachment detachment = spy(new Detachment());
            LocalHangar hangar = mock(LocalHangar.class);
            doReturn(hangar).when(detachment).getHangar();
            when(hangar.getUnits()).thenReturn(List.of(unit));
            return detachment;
        }

        private static MHQOptions options(boolean doNotMothballUnitsInBays, boolean doNotMothballSalvage) {
            MHQOptions mhqOptions = mock(MHQOptions.class);
            when(mhqOptions.getDoNotMothballUnitsInBays()).thenReturn(doNotMothballUnitsInBays);
            when(mhqOptions.getDoNotMothballSalvage()).thenReturn(doNotMothballSalvage);
            return mhqOptions;
        }

        /** Runs automated mothballing on a single unit and reports whether that unit was mothballed. */
        private static boolean runAndCheckMothballed(Unit unit, MHQOptions mhqOptions) {
            UUID unitId = UUID.randomUUID();
            when(unit.getId()).thenReturn(unitId);
            Campaign campaign = mock(Campaign.class);
            Detachment detachment = detachmentFor(campaign, unitId, unit);

            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class)) {
                mekHQ.when(MekHQ::getMHQOptions).thenReturn(mhqOptions);
                ContractAutomation.performAutomatedMothballing(campaign, detachment);
            }

            boolean recorded = detachment.getAutomatedMothballUnits().contains(unitId);
            if (recorded) {
                verify(unit).startMothballing(null, true);
            } else {
                verify(unit, never()).startMothballing(null, true);
            }
            return recorded;
        }

        @Test
        void unitInBayIsSkippedWhenOptionEnabled() {
            Unit unit = mothballableUnit();
            when(unit.hasTransportShipAssignment()).thenReturn(true);

            assertFalse(runAndCheckMothballed(unit, options(true, false)));
        }

        @Test
        void unitInBayIsMothballedWhenOptionDisabled() {
            Unit unit = mothballableUnit();
            when(unit.hasTransportShipAssignment()).thenReturn(true);

            assertTrue(runAndCheckMothballed(unit, options(false, false)));
        }

        @Test
        void tacticallyTransportedUnitIsMothballedEvenWhenBayOptionEnabled() {
            // Infantry in an APC is a tactical transport assignment, not a transport bay, so the option ignores it.
            Unit unit = mothballableUnit();
            when(unit.hasTransportShipAssignment()).thenReturn(false);
            when(unit.hasTacticalTransportAssignment()).thenReturn(true);

            assertTrue(runAndCheckMothballed(unit, options(true, false)));
        }

        @Test
        void unitNotInBayIsMothballedWhenBayOptionEnabled() {
            Unit unit = mothballableUnit();

            assertTrue(runAndCheckMothballed(unit, options(true, false)));
        }

        @Test
        void salvageIsSkippedWhenOptionEnabled() {
            Unit unit = mothballableUnit();
            when(unit.isSalvage()).thenReturn(true);

            assertFalse(runAndCheckMothballed(unit, options(false, true)));
        }

        @Test
        void salvageIsMothballedWhenOptionDisabled() {
            Unit unit = mothballableUnit();
            when(unit.isSalvage()).thenReturn(true);

            assertTrue(runAndCheckMothballed(unit, options(false, false)));
        }

        @Test
        void nonSalvageIsMothballedWhenSalvageOptionEnabled() {
            Unit unit = mothballableUnit();

            assertTrue(runAndCheckMothballed(unit, options(false, true)));
        }

        @Test
        void bothOptionsSkipTheirOwnUnitsOnly() {
            Unit unitInBay = mothballableUnit();
            when(unitInBay.hasTransportShipAssignment()).thenReturn(true);
            Unit salvage = mothballableUnit();
            when(salvage.isSalvage()).thenReturn(true);
            Unit ordinary = mothballableUnit();

            MHQOptions mhqOptions = options(true, true);
            assertFalse(runAndCheckMothballed(unitInBay, mhqOptions));
            assertFalse(runAndCheckMothballed(salvage, mhqOptions));
            assertTrue(runAndCheckMothballed(ordinary, mhqOptions));
        }

        @Test
        void contractStartRespectsOptions() {
            // Contract-market travel goes through performContractStart, which must honour the same options.
            LocalDate today = LocalDate.of(3025, 1, 1);
            UUID bayId = UUID.randomUUID();
            UUID salvageId = UUID.randomUUID();
            UUID ordinaryId = UUID.randomUUID();

            Unit unitInBay = mothballableUnit();
            when(unitInBay.getId()).thenReturn(bayId);
            when(unitInBay.hasTransportShipAssignment()).thenReturn(true);
            Unit salvage = mothballableUnit();
            when(salvage.getId()).thenReturn(salvageId);
            when(salvage.isSalvage()).thenReturn(true);
            Unit ordinary = mothballableUnit();
            when(ordinary.getId()).thenReturn(ordinaryId);

            AbstractLocation currentLocation = mock(AbstractLocation.class);
            when(currentLocation.getCurrentPlanetDirect()).thenReturn(mock(Planet.class));
            when(currentLocation.isOnPlanet()).thenReturn(true);

            LocalHangar hangar = mock(LocalHangar.class);
            when(hangar.getUnits()).thenReturn(List.of(unitInBay, salvage, ordinary));

            Detachment detachment = spy(new Detachment());
            doReturn(hangar).when(detachment).getHangar();
            doReturn(currentLocation).when(detachment).getCurrentLocation();

            PlayerForce force = mock(PlayerForce.class);
            when(force.getForceDetachment()).thenReturn(detachment);
            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(bayId, salvageId, ordinaryId)));
            when(force.getAllFormations()).thenReturn(List.of(formation));

            Campaign campaign = mock(Campaign.class);
            when(campaign.getPlayerForce()).thenReturn(force);
            when(campaign.getUnit(bayId)).thenReturn(unitInBay);
            when(campaign.getUnit(salvageId)).thenReturn(salvage);
            when(campaign.getUnit(ordinaryId)).thenReturn(ordinary);
            when(campaign.getLocalDate()).thenReturn(today);

            AbstractContract contract = mock(AbstractContract.class);
            when(contract.getTargetPlanet()).thenReturn(mock(Planet.class));

            MHQOptions mhqOptions = options(true, true);
            try (MockedStatic<MekHQ> mekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractUtilities> utilities = mockStatic(ContractUtilities.class)) {
                mekHQ.when(MekHQ::getMHQOptions).thenReturn(mhqOptions);
                utilities.when(() -> ContractUtilities.hasArrivedAtContractLocation(any(), any())).thenReturn(false);
                utilities.when(() -> ContractUtilities.getJumpPath(any(), any(), any())).thenReturn(null);

                ContractAutomation.performContractStart(campaign, contract, true, false);
            }

            verify(unitInBay, never()).startMothballing(null, true);
            verify(salvage, never()).startMothballing(null, true);
            verify(ordinary).startMothballing(null, true);
            assertIterableEquals(List.of(ordinaryId), detachment.getAutomatedMothballUnits());
        }
    }

    @Nested
    class Activation {
        @Test
        void activatesAndClearsOnlyTheGivenDetachment() {
            UUID idA = UUID.randomUUID();
            UUID idB = UUID.randomUUID();
            Unit unitA = mock(Unit.class);
            when(unitA.isMothballed()).thenReturn(true);

            Campaign campaign = mock(Campaign.class);
            when(campaign.getUnit(idA)).thenReturn(unitA);

            PlayerForce playerForce = mock(PlayerForce.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);

            ForceHumanResources forceHumanResources = mock(ForceHumanResources.class);
            when(playerForce.getHumanResources()).thenReturn(forceHumanResources);

            Detachment detachmentA = new Detachment();
            detachmentA.setAutomatedMothballUnits(new ArrayList<>(List.of(idA)));
            Detachment detachmentB = new Detachment();
            detachmentB.setAutomatedMothballUnits(new ArrayList<>(List.of(idB)));

            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                ContractAutomation.performAutomatedActivation(campaign, detachmentA);

                verify(unitA).startActivating(null, true);
                // Detachment A's pending list is cleared...
                assertTrue(detachmentA.getAutomatedMothballUnits().isEmpty());
                // ...and detachment B is left completely untouched.
                assertIterableEquals(List.of(idB), detachmentB.getAutomatedMothballUnits());
            }
        }

        @Test
        void alreadyActiveUnitsAreNotReactivatedButListIsStillCleared() {
            UUID id = UUID.randomUUID();
            Unit alreadyActive = mock(Unit.class);
            when(alreadyActive.isMothballed()).thenReturn(false);

            Campaign campaign = mock(Campaign.class);
            when(campaign.getUnit(id)).thenReturn(alreadyActive);

            PlayerForce playerForce = mock(PlayerForce.class);
            when(campaign.getPlayerForce()).thenReturn(playerForce);

            ForceHumanResources forceHumanResources = mock(ForceHumanResources.class);
            when(playerForce.getHumanResources()).thenReturn(forceHumanResources);

            Detachment detachment = new Detachment();
            detachment.setAutomatedMothballUnits(new ArrayList<>(List.of(id)));

            try (MockedStatic<MekHQ> ignored = mockStatic(MekHQ.class)) {
                ContractAutomation.performAutomatedActivation(campaign, detachment);

                verify(alreadyActive, never()).startActivating(null, true);
                assertTrue(detachment.getAutomatedMothballUnits().isEmpty());
            }
        }
    }

    @Nested
    class ContractStart {
        /** The mocks {@link ContractAutomation#performContractStart} touches, bundled for reuse across tests. */
        private record Fixture(Campaign campaign, AbstractContract contract, Unit unit, Detachment detachment,
              LocalDate today) {}

        /**
         * Wires up a campaign whose sole force detachment holds one mothballable unit and is offered a contract. When
         * {@code alreadyAtTarget} is set the detachment's current planet is the contract's target planet (no travel);
         * otherwise the two planets differ.
         */
        private static Fixture fixture(boolean alreadyAtTarget) {
            LocalDate today = LocalDate.of(3025, 1, 1);

            UUID id = UUID.randomUUID();
            Unit unit = mothballableUnit();
            when(unit.getId()).thenReturn(id);

            Planet targetPlanet = mock(Planet.class);
            Planet currentPlanet = alreadyAtTarget ? targetPlanet : mock(Planet.class);

            AbstractLocation currentLocation = mock(AbstractLocation.class);
            when(currentLocation.getCurrentPlanetDirect()).thenReturn(currentPlanet);
            // A force that has settled at a world is out of transit; arrival is judged on that, not just the planet.
            when(currentLocation.isOnPlanet()).thenReturn(true);

            LocalHangar hangar = mock(LocalHangar.class);
            when(hangar.getUnits()).thenReturn(List.of(unit));

            Detachment detachment = mock(Detachment.class);
            when(detachment.getCurrentLocation()).thenReturn(currentLocation);
            when(detachment.getHangar()).thenReturn(hangar);

            PlayerForce force = mock(PlayerForce.class);
            when(force.getForceDetachment()).thenReturn(detachment);
            Formation formation = mock(Formation.class);
            when(formation.getUnits()).thenReturn(new Vector<>(List.of(id)));
            when(force.getAllFormations()).thenReturn(List.of(formation));

            Campaign campaign = mock(Campaign.class);
            when(campaign.getPlayerForce()).thenReturn(force);
            when(campaign.getUnit(id)).thenReturn(unit);
            when(campaign.getLocalDate()).thenReturn(today);

            AbstractContract contract = mock(AbstractContract.class);
            when(contract.getTargetPlanet()).thenReturn(targetPlanet);

            return new Fixture(campaign, contract, unit, detachment, today);
        }

        @Test
        void mothballsUnitsWhenThereIsAJourneyAhead() {
            Fixture f = fixture(false);

            try (MockedStatic<MekHQ> ignoredMekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractUtilities> ignoredUtilities = mockStatic(ContractUtilities.class)) {
                // No automated jump plotted; we only care that mothballing ran.
                ignoredUtilities.when(() -> ContractUtilities.getJumpPath(any(), any(), any())).thenReturn(null);

                ContractAutomation.performContractStart(f.campaign(), f.contract(), true, false);

                verify(f.unit()).startMothballing(null, true);
            }
        }

        @Test
        void doesNotMothballWhenAlreadyAtTargetPlanet() {
            Fixture f = fixture(true);

            try (MockedStatic<MekHQ> ignoredMekHQ = mockStatic(MekHQ.class)) {
                ContractAutomation.performContractStart(f.campaign(), f.contract(), true, false);

                // Regression test for issue #9945: with no travel there is no arrival event to reactivate the units,
                // so the mothball must be skipped entirely rather than stranding them mothballed indefinitely.
                verify(f.unit(), never()).startMothballing(null, true);
                verify(f.detachment(), never()).setAutomatedMothballUnits(anyList());
                // ...and, there being no journey, the contract still starts today.
                verify(f.contract()).setStartAndEndDate(f.today());
            }
        }

        @Test
        void doesNotMothballWhenMothballFlagIsUnset() {
            Fixture f = fixture(false);

            try (MockedStatic<MekHQ> ignoredMekHQ = mockStatic(MekHQ.class);
                  MockedStatic<ContractUtilities> ignoredUtilities = mockStatic(ContractUtilities.class)) {
                ignoredUtilities.when(() -> ContractUtilities.getJumpPath(any(), any(), any())).thenReturn(null);

                ContractAutomation.performContractStart(f.campaign(), f.contract(), false, false);

                verify(f.unit(), never()).startMothballing(null, true);
            }
        }
    }

    @Test
    void newDetachmentHasNoPendingMothballUnits() {
        assertEquals(List.of(), new Detachment().getAutomatedMothballUnits());
    }
}
