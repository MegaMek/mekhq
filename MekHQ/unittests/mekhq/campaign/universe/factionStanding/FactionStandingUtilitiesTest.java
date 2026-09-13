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
package mekhq.campaign.universe.factionStanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionHints.FactionHints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FactionStandingUtilitiesTest {
    static Stream<Arguments> standingLevelProvider() {
        return Stream.of(
              // In-range for all standing levels
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_1.getMinimumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_0),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_1.getMaximumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_1),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_2.getMinimumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_1),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_2.getMaximumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_2),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_3.getMinimumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_2),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_3.getMaximumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_3),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_4.getMinimumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_3),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_4.getMaximumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_4),
              // Typical mid-range values
              Arguments.of((FactionStandingLevel.STANDING_LEVEL_2.getMinimumRegard() +
                                  FactionStandingLevel.STANDING_LEVEL_2.getMaximumRegard()) / 2,
                    FactionStandingLevel.STANDING_LEVEL_2),
              // Out-of-range (below min, above max)
              Arguments.of(Double.NEGATIVE_INFINITY, FactionStandingLevel.STANDING_LEVEL_0),
              Arguments.of(Double.POSITIVE_INFINITY, FactionStandingLevel.STANDING_LEVEL_8),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_0.getMinimumRegard() - 100.0,
                    FactionStandingLevel.STANDING_LEVEL_0),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_8.getMaximumRegard() + 100.0,
                    FactionStandingLevel.STANDING_LEVEL_8)
        );
    }

    @ParameterizedTest
    @MethodSource("standingLevelProvider")
    @DisplayName("Test calculateFactionStandingLevel for in-range and out-of-range values")
    void testCalculateFactionStandingLevel(double inputRegard, FactionStandingLevel expectedLevel) {
        assertEquals(expectedLevel, FactionStandingUtilities.calculateFactionStandingLevel(inputRegard));
    }

    @Test
    @DisplayName("Test calculateFactionStandingLevel always returns a valid standing level")
    void testAlwaysReturnsStandingLevel() {
        for (double regard : new double[] { Double.MIN_VALUE, 0.0, 1.0, -1.0, 100000, -100000, Double.NaN }) {
            assertNotNull(FactionStandingUtilities.calculateFactionStandingLevel(regard));
        }
    }

    @Test
    void preparedAccessDataMatchesPlanetarySystemAndContractLookup() {
        LocalDate when = LocalDate.of(3025, 1, 1);
        Faction campaignFaction = mock(Faction.class);
        Faction systemOwner = mock(Faction.class);
        PlanetarySystem targetSystem = mock(PlanetarySystem.class);
        FactionStandings factionStandings = mock(FactionStandings.class);
        FactionHints factionHints = mock(FactionHints.class);
        when(systemOwner.getShortName()).thenReturn("OWNER");
        when(targetSystem.getPopulation(when)).thenReturn(1_000_000L);
        when(targetSystem.getFactionSet(when)).thenReturn(Set.of(systemOwner));
        when(factionStandings.getRegardForFaction("OWNER", true)).thenReturn(
              FactionStandingLevel.STANDING_LEVEL_0.getMinimumRegard());

        boolean directAccess = FactionStandingUtilities.canEnterTargetSystem(campaignFaction, factionStandings,
              null, targetSystem, when, List.of(), factionHints);
        boolean preparedAccess = FactionStandingUtilities.canEnterTargetSystem(campaignFaction, factionStandings,
              null, 1_000_000L, Set.of(systemOwner), when, Set.of(), Set.of(), factionHints);
        assertEquals(directAccess, preparedAccess);

        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getEmployerFaction()).thenReturn(systemOwner);
        directAccess = FactionStandingUtilities.canEnterTargetSystem(campaignFaction, factionStandings,
              null, targetSystem, when, List.of(contract), factionHints);
        preparedAccess = FactionStandingUtilities.canEnterTargetSystem(campaignFaction, factionStandings,
              null, 1_000_000L, Set.of(systemOwner), when, Set.of(systemOwner), Set.of(), factionHints);
        assertEquals(directAccess, preparedAccess);
        assertTrue(preparedAccess);
      }

      @Test
    @DisplayName("An empty system (no population) is always enterable")
    void testCanEnterTargetSystem_emptySystem_grantsAccess() {
        LocalDate when = LocalDate.of(3021, 4, 17);
        PlanetarySystem targetSystem = mock(PlanetarySystem.class);
        when(targetSystem.getPopulation(when)).thenReturn(0L);

        assertTrue(FactionStandingUtilities.canEnterTargetSystem(mock(Faction.class),
              mock(FactionStandings.class), null, targetSystem, when, List.of(), mock(FactionHints.class)));
    }

    @Test
    @DisplayName("An uncolonized system with a residual population but no controlling faction is enterable")
    void testCanEnterTargetSystem_uncolonizedWithResidualPopulation_grantsAccess() {
        // Regression test for issue #9909: dying/uncolonized worlds (e.g. Nuncavoy, Sacromonte) still report a
        // residual population but have no faction events, so getFactionSet() is empty. Previously the outlaw check
        // (highest regard defaults to the minimum) and the at-war check (allMatch on an empty stream is vacuously
        // true) both flagged such systems as inaccessible.
        LocalDate when = LocalDate.of(3021, 4, 17);
        Faction campaignFaction = mock(Faction.class);
        FactionStandings factionStandings = mock(FactionStandings.class);
        FactionHints factionHints = mock(FactionHints.class);
        PlanetarySystem targetSystem = mock(PlanetarySystem.class);
        when(targetSystem.getPopulation(when)).thenReturn(88_154L);
        when(targetSystem.getFactionSet(when)).thenReturn(Set.of());

        assertTrue(FactionStandingUtilities.canEnterTargetSystem(campaignFaction,
              factionStandings, null, targetSystem, when, List.of(), factionHints));
        assertTrue(FactionStandingUtilities.canEnterTargetSystem(campaignFaction,
              factionStandings, null, 88_154L, Set.of(), when, Set.of(), Set.of(), factionHints));
    }

}
