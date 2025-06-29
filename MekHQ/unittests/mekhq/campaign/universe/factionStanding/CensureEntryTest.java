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

import static mekhq.campaign.universe.factionStanding.CensureEntry.COOLDOWN_PERIOD;
import static mekhq.campaign.universe.factionStanding.CensureEntry.EXPIRY_PERIOD;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CensureEntryTest {
    private final FactionCensureLevel censureLevel = FactionCensureLevel.WARNING;
    private final LocalDate issueDate = LocalDate.of(3151, 1, 1);

    @ParameterizedTest(name = "hasExpired: monthsToAdd={0}, expectedExpired={1}")
    @CsvSource({
          "0,false",
          "1,false",
          EXPIRY_PERIOD + ",false",
          (EXPIRY_PERIOD - 1) + ",false",
          (EXPIRY_PERIOD + 1) + ",true"
    })
    void testHasExpired_param(int monthsToAdd, boolean expectedExpired) {
        LocalDate currentDate = issueDate.plusMonths(monthsToAdd);
        CensureEntry entry = new CensureEntry(censureLevel, issueDate);

        if (expectedExpired) {
            assertTrue(entry.hasExpired(currentDate), "Expected true when months between: " + monthsToAdd);
        } else {
            assertFalse(entry.hasExpired(currentDate), "Expected false when months between: " + monthsToAdd);
        }
    }

    @ParameterizedTest(name = "canEscalate: monthsToAdd={0}, censureLevel={1}, expectedCanEscalate={2}")
    @CsvSource({
          // Normal escalation cases for WARNING (allowed to escalate)
          "0,WARNING,false",
          "1,WARNING,false",
          COOLDOWN_PERIOD + ",WARNING,false",
          (COOLDOWN_PERIOD - 1) + ",WARNING,false",
          (COOLDOWN_PERIOD + 1) + ",WARNING,true",
          // Escalation should be false for DISBAND even after cooldown
          (COOLDOWN_PERIOD + 1) + ",DISBAND,false"
    })
    void testCanEscalate_param(int monthsToAdd, FactionCensureLevel censureLv, boolean expectedCanEscalate) {
        LocalDate currentDate = issueDate.plusMonths(monthsToAdd);
        CensureEntry entry = new CensureEntry(censureLv, issueDate);

        if (expectedCanEscalate) {
            assertTrue(entry.canEscalate(currentDate),
                  "Expected true when months between: " + monthsToAdd + ", level=" + censureLv);
        } else {
            assertFalse(entry.canEscalate(currentDate),
                  "Expected false when months between: " + monthsToAdd + ", level=" + censureLv);
        }
    }
}

