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
package mekhq.campaign.randomEvents.personalities;

import static mekhq.campaign.randomEvents.personalities.PersonalityController.generateReasoning;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PersonalityControllerTest {
    private static final int TALENT_DIE_SIZE = 8346;

    @Test
    void testGenerateReasoning_neverDecreasesAsRollRises() {
        Reasoning previous = generateReasoning(0);
        for (int roll = 1; roll < TALENT_DIE_SIZE; roll++) {
            Reasoning current = generateReasoning(roll);
            assertTrue(current.getLevel() >= previous.getLevel(),
                  "Talent dropped from " + previous + " to " + current + " at roll " + roll);
            previous = current;
        }
    }

    @ParameterizedTest
    @CsvSource({
          "92, OBTUSE",
          "162, LIMITED_INSIGHT",
          "284, LIMITED_INSIGHT",
          "285, UNDER_PERFORMING",
          "500, UNDER_PERFORMING",
          "501, BELOW_AVERAGE",
          "877, BELOW_AVERAGE",
          "878, AVERAGE"
    })
    void testGenerateReasoning_bandsBelowAverageAreInOrder(int roll, Reasoning expected) {
        assertEquals(expected, generateReasoning(roll));
    }
}
