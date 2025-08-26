/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import megamek.common.compute.Compute;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.contractMarket.AbstractContractMarket;
import mekhq.campaign.market.contractMarket.AtbMonthlyContractMarket;
import mekhq.campaign.market.contractMarket.CamOpsContractMarket;
import mekhq.campaign.market.contractMarket.DisabledContractMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ContractMarketMethodTest {
    final int REQUIRED_UNITS_IN_COMBAT_TEAMS_VARIANCE_DICE = 2;

    //region Variable Declarations
    private static final ContractMarketMethod[] methods = ContractMarketMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
          MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("ContractMarketMethod.NONE.toolTipText"),
              ContractMarketMethod.NONE.getToolTipText());
        assertEquals(resources.getString("ContractMarketMethod.ATB_MONTHLY.toolTipText"),
              ContractMarketMethod.ATB_MONTHLY.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final ContractMarketMethod contractMarketMethod : methods) {
            if (contractMarketMethod == ContractMarketMethod.NONE) {
                assertTrue(contractMarketMethod.isNone());
            } else {
                assertFalse(contractMarketMethod.isNone());
            }
        }
    }

    @Test
    public void testIsAtBMonthly() {
        for (final ContractMarketMethod contractMarketMethod : methods) {
            if (contractMarketMethod == ContractMarketMethod.ATB_MONTHLY) {
                assertTrue(contractMarketMethod.isAtBMonthly());
            } else {
                assertFalse(contractMarketMethod.isAtBMonthly());
            }
        }
    }
    //endregion Boolean Comparison Methods

/*
    @Test
    public void testGetContractMarket() {
        assertInstanceOf(DisabledContractMarket.class, ContractMarketMethod.NONE.getContractMarket());
        assertInstanceOf(AtBMonthlyContractMarket.class, ContractMarketMethod.ATB_MONTHLY.getContractMarket());
    }
*/

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("ContractMarketMethod.NONE.text"),
              ContractMarketMethod.NONE.toString());
        assertEquals(resources.getString("ContractMarketMethod.ATB_MONTHLY.text"),
              ContractMarketMethod.ATB_MONTHLY.toString());
    }

    @Nested
    class AbstractContractMarketCalculateRequiredCombatElements {

        Faction mockFaction;
        Campaign mockCampaign;
        AtBContract mockAtBContract;

        public static Stream<Arguments> getContractTypesForTests() {
            return Stream.of(Arguments.of(new CamOpsContractMarket()),
                  Arguments.of(new AtbMonthlyContractMarket()),
                  Arguments.of(new DisabledContractMarket()));
        }

        @BeforeEach
        void beforeEach() {
            mockFaction = mock(Faction.class);
            mockCampaign = mock(Campaign.class);

            when(mockCampaign.getFaction()).thenReturn(mockFaction);

            mockAtBContract = mock(AtBContract.class);

            when(mockAtBContract.isSubcontract()).thenReturn(false);
        }

        @ParameterizedTest
        @MethodSource(value = "getContractTypesForTests")
        void initialTest(AbstractContractMarket contractMarket) {
            // Arrange

            // Act
            int teams = contractMarket.calculateRequiredCombatElements(mockCampaign, mockAtBContract, true);

            // Assert
            assertEquals(1, teams);
        }

        @ParameterizedTest
        @MethodSource(value = "getContractTypesForTests")
        void testNineCombatTeamsBypassVariance(AbstractContractMarket contractMarket) {
            final int COMBAT_TEAMS = 9;
            final int UNITS_IN_COMBAT_TEAMS = 4 * COMBAT_TEAMS;
            int teams;
            // Arrange
            try (MockedStatic<ContractUtilities> contractUtilities = Mockito.mockStatic(ContractUtilities.class)) {
                contractUtilities.when(() -> ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(
                      mockCampaign)).thenReturn(UNITS_IN_COMBAT_TEAMS);
                contractUtilities.when(() -> ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign))
                      .thenReturn(COMBAT_TEAMS);
                // Act
                teams = contractMarket.calculateRequiredCombatElements(mockCampaign, mockAtBContract, true);
            }
            // Assert
            assertEquals(24,
                  teams,
                  "Default variance for nine combat teams should be 2/3 the number of units in the " + "combat teams");
        }

        @ParameterizedTest
        @MethodSource(value = "getContractTypesForTests")
        void testOneEmptyCombatTeam(AbstractContractMarket contractMarket) {
            final int COMBAT_TEAMS = 1;
            final int UNITS_IN_COMBAT_TEAMS = 0;

            testRequiredCombatElementsWithVariance(contractMarket, UNITS_IN_COMBAT_TEAMS, COMBAT_TEAMS);
        }

        @ParameterizedTest
        @MethodSource(value = "getContractTypesForTests")
        void testOneCombatTeamWithOneUnit(AbstractContractMarket contractMarket) {
            final int COMBAT_TEAMS = 1;
            final int UNITS_IN_COMBAT_TEAMS = 1;

            testRequiredCombatElementsWithVariance(contractMarket, UNITS_IN_COMBAT_TEAMS, COMBAT_TEAMS);
        }

        @ParameterizedTest
        @MethodSource(value = "getContractTypesForTests")
        void testOneCombatTeamWithTwoUnits(AbstractContractMarket contractMarket) {
            final int COMBAT_TEAMS = 1;
            final int UNITS_IN_COMBAT_TEAMS = 2;

            testRequiredCombatElementsWithVariance(contractMarket, UNITS_IN_COMBAT_TEAMS, COMBAT_TEAMS);
        }

        @ParameterizedTest
        @MethodSource(value = "getContractTypesForTests")
        void testOneCombatTeam(AbstractContractMarket contractMarket) {
            final int COMBAT_TEAMS = 1;
            final int UNITS_IN_COMBAT_TEAMS = 4 * COMBAT_TEAMS;

            testRequiredCombatElementsWithVariance(contractMarket, UNITS_IN_COMBAT_TEAMS, COMBAT_TEAMS);
        }

        @ParameterizedTest
        @MethodSource(value = "getContractTypesForTests")
        void testThreeCombatTeams(AbstractContractMarket contractMarket) {
            final int COMBAT_TEAMS = 3;
            final int UNITS_IN_COMBAT_TEAMS = 4 * COMBAT_TEAMS;

            testRequiredCombatElementsWithVariance(contractMarket, UNITS_IN_COMBAT_TEAMS, COMBAT_TEAMS);
        }

        @ParameterizedTest
        @MethodSource(value = "getContractTypesForTests")
        void testNineCombatTeams(AbstractContractMarket contractMarket) {
            final int COMBAT_TEAMS = 9;
            final int UNITS_IN_COMBAT_TEAMS = 4 * COMBAT_TEAMS;

            testRequiredCombatElementsWithVariance(contractMarket, UNITS_IN_COMBAT_TEAMS, COMBAT_TEAMS);
        }

        private void testRequiredCombatElementsWithVariance(AbstractContractMarket contractMarket,
              int UNITS_IN_COMBAT_TEAMS, int COMBAT_TEAMS) {
            ArrayList<Integer> requiredUnitInCombatTeams = new ArrayList<>();
            // Arrange
            try (MockedStatic<ContractUtilities> contractUtilities = Mockito.mockStatic(ContractUtilities.class)) {
                contractUtilities.when(() -> ContractUtilities.calculateBaseNumberOfUnitsRequiredInCombatTeams(
                      mockCampaign)).thenReturn(Math.max(UNITS_IN_COMBAT_TEAMS, 1));
                contractUtilities.when(() -> ContractUtilities.calculateBaseNumberOfRequiredLances(mockCampaign))
                      .thenReturn(Math.max(COMBAT_TEAMS, 1));
                try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
                    // Let's go from the lowest possible roll to the highest and collect all the required unit counts
                    for (int varianceRoll = REQUIRED_UNITS_IN_COMBAT_TEAMS_VARIANCE_DICE;
                          varianceRoll < REQUIRED_UNITS_IN_COMBAT_TEAMS_VARIANCE_DICE * 6;
                          varianceRoll++) {
                        compute.when(() -> Compute.d6(anyInt())).thenReturn(varianceRoll);

                        // Act (this is the method we are testing!)
                        requiredUnitInCombatTeams.add(contractMarket.calculateRequiredCombatElements(mockCampaign,
                              mockAtBContract,
                              false));
                    }
                }
            }
            // Assert
            assertRequiredCombatElementsResults(COMBAT_TEAMS, UNITS_IN_COMBAT_TEAMS, requiredUnitInCombatTeams);
        }

        /**
         * This test shouldn't test specific values. It should look at the overall trend. We don't want to fail future
         * changes that tweak the scale of this unless they're doing something wild. This test should be to ensure that
         * we are getting a spread.
         *
         * @param combatTeams                  how many combat teams?
         * @param unitsInCombatTeams           how many units are in those combat teams?
         * @param requiredCombatElementsCounts ArrayList of Integers for the required count of units in combat teams at
         *                                     each variance roll
         */
        private void assertRequiredCombatElementsResults(int combatTeams, int unitsInCombatTeams,
              ArrayList<Integer> requiredCombatElementsCounts) {
            int smallestValue = Integer.MAX_VALUE;
            int largestValue = Integer.MIN_VALUE;

            for (int requiredCombatElementsCount : requiredCombatElementsCounts) {
                // First, let's handle edge cases with unusual values.
                // If there's no one or only one unit in combat teams, we expect a result of 1.
                if (2 > unitsInCombatTeams) {
                    assertEquals(1,
                          requiredCombatElementsCount,
                          "If there's no one or only one unit in combat teams, we expect a result of 1");
                    return;
                }

                // Next, let's get some safe assumptions out of the way.
                assertTrue(unitsInCombatTeams >= requiredCombatElementsCount,
                      "Required units should not be more than the units a player has in combat teams.");

                // Finally let's compare some values to test later
                if (requiredCombatElementsCount < smallestValue) {
                    smallestValue = requiredCombatElementsCount;
                }
                if (requiredCombatElementsCount > largestValue) {
                    largestValue = requiredCombatElementsCount;
                }
            }

            assertTrue((smallestValue >= (int) Math.ceil(0.4 * unitsInCombatTeams)),
                  "The minimum shouldn't be too low (adjust this if it is!) Smallest Value: " + smallestValue);

            if (unitsInCombatTeams > 2) {

                assertTrue((largestValue >= (int) Math.ceil(0.7 * unitsInCombatTeams)),
                      "The maximum shouldn't be too low (adjust this if it is!) Largest Value: " + largestValue);

                assertNotEquals(smallestValue,
                      largestValue,
                      "There should be some variance (if we have enough units)" +
                            "! Smallest Value: " +
                            smallestValue +
                            " Largest Value: " +
                            largestValue);

                assertNotEquals(smallestValue,
                      unitsInCombatTeams,
                      "The smallest value shouldn't be the units in combat teams" +
                            "! Smallest Value: " +
                            smallestValue +
                            " Units in Combat Teams: " +
                            unitsInCombatTeams);
            }
        }
    }
}
