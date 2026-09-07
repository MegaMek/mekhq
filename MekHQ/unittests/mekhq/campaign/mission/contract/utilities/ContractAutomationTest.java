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
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
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
