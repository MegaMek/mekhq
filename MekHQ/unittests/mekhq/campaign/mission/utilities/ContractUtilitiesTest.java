/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.mission.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ContractUtilitiesTest {
    static Campaign mockCampaign;

    ArrayList<CombatTeam> mockCombatTeams;

    @BeforeAll
    static void beforeAll() {
        mockCampaign = mockCampaign();
    }

    @BeforeEach
    void beforeEach() {
        mockCombatTeams = new ArrayList<>();

        when(mockCampaign.getPlayerForce().getCombatTeamsAsList(mockCampaign)).thenReturn(mockCombatTeams);
    }

    @Nested
    public class TestCalculateBaseNumberOfRequiredLances {

        static List<Arguments> getCombatRoles() {
            return Arrays.stream(CombatRole.values()).filter(CombatRole::isCombatRole).map(Arguments::of).toList();
        }

        static List<Arguments> getNoncombatRoles() {
            return Arrays.stream(CombatRole.values()).filter(c -> !c.isCombatRole()).map(Arguments::of).toList();
        }

        @ParameterizedTest
        @MethodSource(value = "getCombatRoles")
        void calculateBaseNumberOfRequiredLancesTest1LanceBypassVariance(CombatRole combatRole) {
            mockCombatTeams.add(newMockCombatTeam(4, combatRole, FormationType.STANDARD));
            int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, true, 1.0);

            assertEquals(1, reqLances);
        }

        @ParameterizedTest
        @MethodSource(value = "getCombatRoles")
        void calculateBaseNumberOfRequiredLancesTest3LancesBypassVariance(CombatRole combatRole) {
            newMockCombatTeams(3, 4, combatRole, FormationType.STANDARD);
            int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, true, 1.0);

            assertEquals(3, reqLances);
        }

        @ParameterizedTest
        @MethodSource(value = "getCombatRoles")
        void calculateBaseNumberOfRequiredLancesTest9LanceBypassVariance(CombatRole combatRole) {
            newMockCombatTeams(9, 4, combatRole, FormationType.STANDARD);
            int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, true, 1.0);

            assertEquals(9, reqLances);
        }

        @ParameterizedTest
        @MethodSource(value = "getNoncombatRoles")
        void calculateBaseNumberOfRequiredLancesTest1Lance(CombatRole combatRole) {
            newMockCombatTeams(1, 4, combatRole, FormationType.STANDARD);

            for (int i = 0; i < 5; i++) {
                final double TEST_VARIANCE = ContractUtilities.BASE_VARIANCE_FACTOR + (.2 - (i * .1));
                int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign,
                      false,
                      TEST_VARIANCE);

                assertEquals(1, reqLances);
            }
        }

        @ParameterizedTest
        @MethodSource(value = "getCombatRoles")
        void calculateBaseNumberOfRequiredLancesTest3Lance(CombatRole combatRole) {
            newMockCombatTeams(3, 4, combatRole, FormationType.STANDARD);

            for (int i = 0; i < 5; i++) {
                final double TEST_VARIANCE = ContractUtilities.BASE_VARIANCE_FACTOR + (.2 - (i * .1));
                int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign,
                      false,
                      TEST_VARIANCE);

                assertEquals(Math.ceil(3 * TEST_VARIANCE), reqLances);
            }
        }

        @ParameterizedTest
        @MethodSource(value = "getCombatRoles")
        void calculateBaseNumberOfRequiredLancesTest9Lance(CombatRole combatRole) {
            newMockCombatTeams(9, 4, combatRole, FormationType.STANDARD);

            for (int i = 0; i < 5; i++) {
                final double TEST_VARIANCE = ContractUtilities.BASE_VARIANCE_FACTOR + (.2 - (i * .1));
                int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign,
                      false,
                      TEST_VARIANCE);

                assertEquals(Math.ceil(9 * TEST_VARIANCE), reqLances);
            }
        }

        @ParameterizedTest
        @MethodSource(value = "getNoncombatRoles")
        void calculateBaseNumberOfRequiredLancesTest9LanceBypassVarianceNonCombat(CombatRole combatRole) {
            newMockCombatTeams(9, 4, combatRole, FormationType.STANDARD);
            int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, true, 1.0);

            assertEquals(1, reqLances);
        }

        @ParameterizedTest
        @MethodSource(value = "getNoncombatRoles")
        void calculateBaseNumberOfRequiredLancesTest9LanceNonCombat(CombatRole combatRole) {
            newMockCombatTeams(9, 4, combatRole, FormationType.STANDARD);

            for (int i = 0; i < 5; i++) {
                final double TEST_VARIANCE = ContractUtilities.BASE_VARIANCE_FACTOR + (.2 - (i * .1));
                int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign,
                      false,
                      TEST_VARIANCE);

                assertEquals(1, reqLances);
            }
        }

    }

    /** Tests for contracts on another world of the system the force is already in. */
    @Nested
    public class SameSystemContract {
        PlanetarySystem system;
        Planet homeWorld;
        Planet contractWorld;
        Campaign campaign;
        AbstractContract contract;
        AbstractLocation location;

        @BeforeEach
        void beforeEach() {
            system = mock(PlanetarySystem.class);
            homeWorld = mock(Planet.class);
            contractWorld = mock(Planet.class);
            when(homeWorld.getTimeToPlanet(contractWorld, 1.0)).thenReturn(3.5);

            campaign = mock(Campaign.class);
            when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
            when(campaign.calculateJumpPath(system, system)).thenAnswer(invocation -> {
                JumpPath jumpPath = new JumpPath();
                jumpPath.addSystem(system);
                return jumpPath;
            });

            contract = mock(AbstractContract.class);
            when(contract.getTargetSystem()).thenReturn(system);
            when(contract.getTargetPlanet()).thenReturn(contractWorld);

            location = mock(AbstractLocation.class);
            when(location.getCurrentSystem()).thenReturn(system);
            when(location.getPlanet()).thenReturn(homeWorld);
        }

        @Test
        void aForceAtAWorldTransfersFromThatWorld() {
            when(location.isOnPlanet()).thenReturn(true);

            JumpPath jumpPath = ContractUtilities.getJumpPath(campaign, contract, location);

            assertSame(homeWorld, jumpPath.getOriginPlanet());
            assertTrue(jumpPath.isInSystemTransfer());
            assertEquals(4, ContractUtilities.getTravelDays(campaign, contract, location, false,
                  mock(FactionStandings.class)), "Travel time should be the 3.5 day transfer, rounded up");
        }

        @Test
        void aForceAlreadyUnderwayHasNoDepartureWorld() {
            when(location.isOnPlanet()).thenReturn(false);

            JumpPath jumpPath = ContractUtilities.getJumpPath(campaign, contract, location);

            assertNull(jumpPath.getOriginPlanet());
            assertFalse(jumpPath.isInSystemTransfer());
        }

        @Test
        void aCachedPathFromAnotherWorldIsReplotted() {
            JumpPath stalePath = new JumpPath();
            stalePath.addSystem(system);
            stalePath.setOriginPlanet(mock(Planet.class));
            stalePath.setTargetPlanet(contractWorld);
            when(contract.getCachedJumpPathDirect()).thenReturn(stalePath);
            when(location.isOnPlanet()).thenReturn(true);

            JumpPath jumpPath = ContractUtilities.getJumpPath(campaign, contract, location);

            assertSame(homeWorld, jumpPath.getOriginPlanet());
        }
    }

    void newMockCombatTeams(int numberOfCombatTeams, int size, CombatRole combatRole, FormationType formationType) {
        for (int i = 0; i < numberOfCombatTeams; i++) {
            mockCombatTeams.add(newMockCombatTeam(size, combatRole, formationType));
        }
    }

    CombatTeam newMockCombatTeam(int size, CombatRole combatRole, FormationType formationType) {
        Formation mockFormation = mock(Formation.class);
        when(mockFormation.isFormationType(formationType)).thenReturn(true);
        when(mockFormation.getCombatRoleInMemory()).thenReturn(combatRole);

        CombatTeam mockCombatTeam = mock(CombatTeam.class);

        when(mockCombatTeam.getSize(any())).thenReturn(size);
        when(mockCombatTeam.getFormation(any())).thenReturn(mockFormation);

        return mockCombatTeam;
    }
}
