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
package mekhq.campaign.personnel.familiarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Boundary coverage for {@link Familiarity}. The private {@code getFamiliarityLevel} bucketing is exercised through the
 * public {@link Familiarity#getPilotingMaintenanceBonus(int)} / {@link Familiarity#getGunneryRepairBonus(int)}
 * accessors, since a bucket-boundary off-by-one is exactly the class of bug this suite guards against.
 */
class FamiliarityTest {
    @Test
    void testCaps() {
        assertEquals(0, Familiarity.DISABLED.getFamiliarityCap());
        assertEquals(200, Familiarity.NORMAL.getFamiliarityCap());
        assertEquals(300, Familiarity.HARD.getFamiliarityCap());
    }

    @Test
    void testIsEnabled() {
        assertFalse(Familiarity.DISABLED.isEnabled());
        assertTrue(Familiarity.NORMAL.isEnabled());
        assertTrue(Familiarity.HARD.isEnabled());
    }

    /**
     * DISABLED grants nothing at any familiarity, including the boundaries and out-of-range values.
     */
    @ParameterizedTest
    @CsvSource({ "-50", "0", "99", "100", "199", "200", "299", "300", "500" })
    void testDisabledAlwaysZero(int familiarity) {
        assertEquals(0, Familiarity.DISABLED.getPilotingMaintenanceBonus(familiarity));
        assertEquals(0, Familiarity.DISABLED.getGunneryRepairBonus(familiarity));
    }

    /**
     * NORMAL: piloting/maintenance steps 0 -> 1 at the 100 boundary and then holds; gunnery/repair steps 0 -> 1 at the
     * 200 boundary. The 99/100 and 199/200 pairs pin the bucket edges.
     */
    @ParameterizedTest
    @CsvSource({
          // familiarity, expectedPiloting, expectedGunnery
          "0,   0, 0",
          "99,  0, 0",
          "100, 1, 0",
          "199, 1, 0",
          "200, 1, 1",
          "299, 1, 1",
          "300, 1, 1",
          "999, 1, 1" })
    void testNormalBonuses(int familiarity, int expectedPiloting, int expectedGunnery) {
        assertEquals(expectedPiloting, Familiarity.NORMAL.getPilotingMaintenanceBonus(familiarity),
              "piloting/maintenance at familiarity " + familiarity);
        assertEquals(expectedGunnery, Familiarity.NORMAL.getGunneryRepairBonus(familiarity),
              "gunnery/repair at familiarity " + familiarity);
    }

    /**
     * HARD: an unfamiliar crew is penalized (-1) below 100. Piloting/maintenance climbs -1 -> 0 -> 1 at the 100 and 200
     * boundaries; gunnery/repair climbs -1 -> 0 -> 1 at the 100 and 300 boundaries.
     */
    @ParameterizedTest
    @CsvSource({
          // familiarity, expectedPiloting, expectedGunnery
          "-10, -1, -1",
          "0,   -1, -1",
          "99,  -1, -1",
          "100,  0,  0",
          "199,  0,  0",
          "200,  1,  0",
          "299,  1,  0",
          "300,  1,  1",
          "400,  1,  1" })
    void testHardBonuses(int familiarity, int expectedPiloting, int expectedGunnery) {
        assertEquals(expectedPiloting, Familiarity.HARD.getPilotingMaintenanceBonus(familiarity),
              "piloting/maintenance at familiarity " + familiarity);
        assertEquals(expectedGunnery, Familiarity.HARD.getGunneryRepairBonus(familiarity),
              "gunnery/repair at familiarity " + familiarity);
    }
}
