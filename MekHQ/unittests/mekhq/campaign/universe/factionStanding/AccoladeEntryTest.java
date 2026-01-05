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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.AccoladeEntry.COOLDOWN_PERIOD;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class AccoladeEntryTest {
    private final LocalDate issueDate = LocalDate.of(3151, 1, 1);

    @ParameterizedTest(name = "canImprove: monthsToAdd={0}, accoladeLevel={1}, expectedCanImprove={2}")
    @CsvSource({
          // Normal improvement cases for PROPAGANDA_REEL (allowed to improve)
          "0,PROPAGANDA_REEL,false",
          "1,PROPAGANDA_REEL,false",
          COOLDOWN_PERIOD + ",PROPAGANDA_REEL,true",
          (COOLDOWN_PERIOD - 1) + ",PROPAGANDA_REEL,false",
          (COOLDOWN_PERIOD + 1) + ",PROPAGANDA_REEL,true",
          // Improvement should be false for LETTER_FROM_HEAD_OF_STATE even after cooldown
          (COOLDOWN_PERIOD + 1) + ",LETTER_FROM_HEAD_OF_STATE,false"
    })
    void testCanImprove(int monthsToAdd, FactionAccoladeLevel accoladeLevel,
          boolean expectedCanImprove) {
        LocalDate currentDate = issueDate.plusMonths(monthsToAdd);
        AccoladeEntry entry = new AccoladeEntry(accoladeLevel, issueDate);

        if (expectedCanImprove) {
            assertTrue(entry.canImprove(currentDate, FactionStandingLevel.STANDING_LEVEL_8),
                  "Expected true when months between: " + monthsToAdd + ", level=" + accoladeLevel);
        } else {
            assertFalse(entry.canImprove(currentDate, FactionStandingLevel.STANDING_LEVEL_8),
                  "Expected false when months between: " + monthsToAdd + ", level=" + accoladeLevel);
        }
    }

    private static Stream<Arguments> provideDifferentStandingLevels() {
        return Stream.of(
              arguments(7, FactionAccoladeLevel.PROPAGANDA_REEL, FactionStandingLevel.STANDING_LEVEL_8, true),
              arguments(5, FactionAccoladeLevel.PROPAGANDA_REEL, FactionStandingLevel.STANDING_LEVEL_7, false),
              arguments(6, FactionAccoladeLevel.PROPAGANDA_REEL, FactionStandingLevel.STANDING_LEVEL_5, false),
              arguments(7,
                    FactionAccoladeLevel.LETTER_FROM_HEAD_OF_STATE,
                    FactionStandingLevel.STANDING_LEVEL_8,
                    false)
        );
    }

    @ParameterizedTest(name = "canImproveWithDifferentStandingLevels: {0}, {1}, {2}, {3}")
    @MethodSource("provideDifferentStandingLevels")
    void testCanImproveWithDifferentStandingLevels(int monthsToAdd, FactionAccoladeLevel accoladeLevel,
          FactionStandingLevel standingLevel, boolean expectedCanImprove) {
        LocalDate currentDate = issueDate.plusMonths(monthsToAdd);
        AccoladeEntry entry = new AccoladeEntry(accoladeLevel, issueDate);

        if (expectedCanImprove) {
            assertTrue(entry.canImprove(currentDate, standingLevel),
                  "Expected true when months between: " + monthsToAdd + ", level=" + accoladeLevel
                        + ", standing=" + standingLevel);
        } else {
            assertFalse(entry.canImprove(currentDate, standingLevel),
                  "Expected false when months between: " + monthsToAdd + ", level=" + accoladeLevel
                        + ", standing=" + standingLevel);
        }
    }

    private static Stream<Arguments> provideCooldownDateCases() {
        return Stream.of(
              arguments(6, FactionAccoladeLevel.PROPAGANDA_REEL, true),
              arguments(7, FactionAccoladeLevel.PROPAGANDA_REEL, true),
              arguments(5, FactionAccoladeLevel.STATUE_OR_SIBKO, false)
        );
    }

    @ParameterizedTest(name = "canImproveAtCooldownDate: {0}, {1}, {2}")
    @MethodSource("provideCooldownDateCases")
    void testCanImproveWithReachingCooldownDate(int monthsToAdd, FactionAccoladeLevel accoladeLevel,
          boolean expectedCanImprove) {
        LocalDate currentDate = issueDate.plusMonths(monthsToAdd);
        AccoladeEntry entry = new AccoladeEntry(accoladeLevel, issueDate);

        if (expectedCanImprove) {
            assertTrue(entry.canImprove(currentDate, FactionStandingLevel.STANDING_LEVEL_8),
                  "Expected true at cooldown date for months: " + monthsToAdd + ", level=" + accoladeLevel);
        } else {
            assertFalse(entry.canImprove(currentDate, FactionStandingLevel.STANDING_LEVEL_8),
                  "Expected false at cooldown date for months: " + monthsToAdd + ", level=" + accoladeLevel);
        }
    }
}

