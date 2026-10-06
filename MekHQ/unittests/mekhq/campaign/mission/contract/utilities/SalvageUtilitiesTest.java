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
package mekhq.campaign.mission.contract.utilities;

import static mekhq.campaign.mission.contract.utilities.SalvageUtilities.calculateSalvagePercentage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link SalvageUtilities#calculateSalvagePercentage}, which rounds up by design: the result is compared against
 * the contract's negotiated salvage cap, so a true 50.001% against a 50% cap is a breach and must surface as 51 rather
 * than being truncated back to a compliant 50.
 */
class SalvageUtilitiesTest {
    @ParameterizedTest
    @CsvSource({
          "1, 1, 50",     // even split
          "1, 3, 25",     // exact quarter, no rounding needed
          "3, 1, 75",
          "1, 0, 100",    // the player took everything
          "0, 1, 0"       // the employer took everything
    })
    void exactPercentagesAreReportedExactly(long playerShare, long employerShare, int expectedPercent) {
        assertEquals(expectedPercent, calculateSalvagePercentage(Money.of(playerShare), Money.of(employerShare)));
    }

    @ParameterizedTest
    @CsvSource({
          "1, 2, 34",       // 33.33% rounds up
          "2, 1, 67",       // 66.66% rounds up
          "1, 999, 1",      // 0.1% rounds up rather than vanishing
          "5001, 4999, 51"  // 50.01% against a 50% cap must read as a breach
    })
    void fractionalPercentagesRoundUp(long playerShare, long employerShare, int expectedPercent) {
        assertEquals(expectedPercent, calculateSalvagePercentage(Money.of(playerShare), Money.of(employerShare)),
              "any fraction of a percent must round up, so a fractional breach of the cap still reads as a breach");
    }

    @Test
    void nothingSalvagedYieldsZeroRatherThanDividingByZero() {
        assertEquals(0, calculateSalvagePercentage(Money.zero(), Money.zero()));
    }

    /**
     * A near-whole percentage must not be nudged up by floating-point noise; the arithmetic runs on {@code BigDecimal}
     * precisely so that 99% stays 99% (see issue #5683).
     */
    @Test
    void aWholePercentageIsNotNudgedUpByRoundingNoise() {
        assertEquals(99, calculateSalvagePercentage(Money.of(99), Money.of(1)));
        assertEquals(1, calculateSalvagePercentage(Money.of(1), Money.of(99)));
    }
}
