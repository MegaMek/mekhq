/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.ForceType;
import mekhq.campaign.mission.enums.CombatRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ContractUtilitiesTest {
    static Campaign mockCampaign;

    ArrayList<CombatTeam> mockCombatTeams;

    @BeforeAll
    static void beforeAll() {
        mockCampaign = mock(Campaign.class);
    }

    @BeforeEach
    void beforeEach() {
        mockCombatTeams = new ArrayList<>();

        when(mockCampaign.getCombatTeamsAsList()).thenReturn(mockCombatTeams);
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
            mockCombatTeams.add(newMockCombatTeam(4, combatRole, ForceType.STANDARD));
            int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);

            assertEquals(1, reqLances);
        }

        @ParameterizedTest
        @MethodSource(value = "getCombatRoles")
        void calculateBaseNumberOfRequiredLancesTest3LancesBypassVariance(CombatRole combatRole) {
            newMockCombatTeams(3, 4, combatRole, ForceType.STANDARD);
            int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);

            assertEquals(3, reqLances);
        }

        @ParameterizedTest
        @MethodSource(value = "getCombatRoles")
        void calculateBaseNumberOfRequiredLancesTest9LanceBypassVariance(CombatRole combatRole) {
            newMockCombatTeams(9, 4, combatRole, ForceType.STANDARD);
            int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);

            assertEquals(9, reqLances);
        }

        @ParameterizedTest
        @MethodSource(value = "getNoncombatRoles")
        void calculateBaseNumberOfRequiredLancesTest1Lance(CombatRole combatRole) {
            newMockCombatTeams(1, 4, combatRole, ForceType.STANDARD);

            for (int i = 0; i < 5; i++) {
                final double TEST_VARIANCE = ContractUtilities.BASE_VARIANCE_FACTOR + (.2 - (i * .1));
                int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign,
                      false,
                      false,
                      TEST_VARIANCE);

                assertEquals(1, reqLances);
            }
        }

        @ParameterizedTest
        @MethodSource(value = "getCombatRoles")
        void calculateBaseNumberOfRequiredLancesTest3Lance(CombatRole combatRole) {
            newMockCombatTeams(3, 4, combatRole, ForceType.STANDARD);

            for (int i = 0; i < 5; i++) {
                final double TEST_VARIANCE = ContractUtilities.BASE_VARIANCE_FACTOR + (.2 - (i * .1));
                int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign,
                      false,
                      false,
                      TEST_VARIANCE);

                assertEquals(Math.ceil(3 * TEST_VARIANCE), reqLances);
            }
        }

        @ParameterizedTest
        @MethodSource(value = "getCombatRoles")
        void calculateBaseNumberOfRequiredLancesTest9Lance(CombatRole combatRole) {
            newMockCombatTeams(9, 4, combatRole, ForceType.STANDARD);

            for (int i = 0; i < 5; i++) {
                final double TEST_VARIANCE = ContractUtilities.BASE_VARIANCE_FACTOR + (.2 - (i * .1));
                int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign,
                      false,
                      false,
                      TEST_VARIANCE);

                assertEquals(Math.ceil(9 * TEST_VARIANCE), reqLances);
            }
        }

        @ParameterizedTest
        @MethodSource(value = "getNoncombatRoles")
        void calculateBaseNumberOfRequiredLancesTest9LanceBypassVarianceNonCombat(CombatRole combatRole) {
            newMockCombatTeams(9, 4, combatRole, ForceType.STANDARD);
            int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign, false, true, 1.0);

            assertEquals(1, reqLances);
        }

        @ParameterizedTest
        @MethodSource(value = "getNoncombatRoles")
        void calculateBaseNumberOfRequiredLancesTest9LanceNonCombat(CombatRole combatRole) {
            newMockCombatTeams(9, 4, combatRole, ForceType.STANDARD);

            for (int i = 0; i < 5; i++) {
                final double TEST_VARIANCE = ContractUtilities.BASE_VARIANCE_FACTOR + (.2 - (i * .1));
                int reqLances = ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign,
                      false,
                      false,
                      TEST_VARIANCE);

                assertEquals(1, reqLances);
            }
        }

    }

    void newMockCombatTeams(int numberOfCombatTeams, int size, CombatRole combatRole, ForceType forceType) {
        for (int i = 0; i < numberOfCombatTeams; i++) {
            mockCombatTeams.add(newMockCombatTeam(size, combatRole, forceType));
        }
    }

    CombatTeam newMockCombatTeam(int size, CombatRole combatRole, ForceType forceType) {
        Formation mockFormation = mock(Formation.class);
        when(mockFormation.isForceType(forceType)).thenReturn(true);
        when(mockFormation.getCombatRoleInMemory()).thenReturn(combatRole);

        CombatTeam mockCombatTeam = mock(CombatTeam.class);

        when(mockCombatTeam.getSize(any())).thenReturn(size);
        when(mockCombatTeam.getForce(any())).thenReturn(mockFormation);

        return mockCombatTeam;
    }
}
