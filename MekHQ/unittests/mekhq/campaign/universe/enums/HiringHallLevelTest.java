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
package mekhq.campaign.universe.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the Hot Spots: Draconis Reach data carried by {@link HiringHallLevel}: the A-F rating, the planetary cost
 * multiplier (pg 69), and the combined display label.
 */
class HiringHallLevelTest {
    private static final double DELTA = 1e-9;

    @ParameterizedTest
    @CsvSource({ "GREAT, A", "STANDARD, B", "MINOR, C", "QUESTIONABLE, D", "NONE, F" })
    void eachLevelMapsToItsHotSpotsRating(HiringHallLevel hiringHallLevel, char expectedRating) {
        assertEquals(expectedRating, hiringHallLevel.getHotSpotsRating());
    }

    @Test
    void ratingEIsUnusedAndNoRatingIsShared() {
        Set<Character> ratings = new HashSet<>();
        for (HiringHallLevel hiringHallLevel : HiringHallLevel.values()) {
            assertTrue(ratings.add(hiringHallLevel.getHotSpotsRating()),
                  "rating " + hiringHallLevel.getHotSpotsRating() + " is used twice");
        }
        assertFalse(ratings.contains('E'));
    }

    @ParameterizedTest
    @CsvSource({ "GREAT, 0.5", "STANDARD, 0.5", "MINOR, 0.75", "QUESTIONABLE, 1.0", "NONE, 1.0" })
    void planetaryCostMultiplierFollowsTheRating(HiringHallLevel hiringHallLevel, double expectedMultiplier) {
        assertEquals(expectedMultiplier, hiringHallLevel.getPlanetaryCostMultiplier(), DELTA);
    }

    @ParameterizedTest
    @EnumSource(HiringHallLevel.class)
    void planetaryCostMultiplierNeverRaisesCosts(HiringHallLevel hiringHallLevel) {
        double multiplier = hiringHallLevel.getPlanetaryCostMultiplier();
        assertTrue(multiplier > 0 && multiplier <= 1.0);
    }

    @ParameterizedTest
    @CsvSource({ "GREAT, Great (A)", "STANDARD, Standard (B)", "MINOR, Minor (C)",
                 "QUESTIONABLE, Questionable (D)", "NONE, None (F)" })
    void labelShowsTheCamOpsNameThenTheHotSpotsRating(HiringHallLevel hiringHallLevel, String expectedLabel) {
        assertEquals(expectedLabel, hiringHallLevel.getLabel());
    }

    /** Saves store the enum by name, so the label must not leak into {@code toString()}. */
    @ParameterizedTest
    @EnumSource(HiringHallLevel.class)
    void toStringIsStillTheConstantName(HiringHallLevel hiringHallLevel) {
        assertEquals(hiringHallLevel.name(), hiringHallLevel.toString());
        assertEquals(hiringHallLevel, HiringHallLevel.valueOf(hiringHallLevel.toString()));
    }

    @Test
    void onlyNoneIsNone() {
        for (HiringHallLevel hiringHallLevel : HiringHallLevel.values()) {
            assertEquals(hiringHallLevel == HiringHallLevel.NONE, hiringHallLevel.isNone());
        }
    }
}
