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

import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.NO_ACCOLADE;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.TAKING_NOTICE_0;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FactionAccoladeLevelTest {
    @Test
    void testAccoladeLevelOrdering() {
        int lastRecognitionLevel = NO_ACCOLADE.getRecognition() - 1;
        for (FactionAccoladeLevel accoladeLevel : FactionAccoladeLevel.values()) {
            int actual = accoladeLevel.getRecognition();
            int expected = ++lastRecognitionLevel;
            assertEquals(expected, actual,
                  "Accolade level ordering for " +
                        accoladeLevel +
                        " is incorrect. Was " +
                        actual +
                        ", expected " +
                        expected);
            lastRecognitionLevel = actual;
        }
    }

    @Test
    void testAccoladeStandingRequirementsOrdering() {
        int lastRequiredStandingLevel = TAKING_NOTICE_0.getRequiredStandingLevel() - 1;
        for (FactionAccoladeLevel accoladeLevel : FactionAccoladeLevel.values()) {
            if (accoladeLevel.is(NO_ACCOLADE)) {
                continue;
            }

            int actual = accoladeLevel.getRequiredStandingLevel();
            int lowerBound = lastRequiredStandingLevel;
            int upperBound = ++lastRequiredStandingLevel;
            assertTrue(actual == lowerBound || actual == upperBound,
                  "Standing level ordering for " +
                        accoladeLevel +
                        " is incorrect. Was " +
                        actual +
                        ", expected " +
                        lowerBound +
                        " or " +
                        upperBound);
            lastRequiredStandingLevel = actual;
        }
    }

    @ParameterizedTest
    @EnumSource(FactionAccoladeLevel.class)
    void testLookupNameValidity(FactionAccoladeLevel accoladeLevel) {
        String name = accoladeLevel.name();
        String lookupName = accoladeLevel.getLookupName();
        boolean isValid = name.contains(lookupName);
        assertTrue(isValid,
              "Lookup name for " + accoladeLevel + " is invalid. Was " + lookupName + ". It should contain " + name);
    }
}
