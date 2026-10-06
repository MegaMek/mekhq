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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the ceiling on a Bloodname roll.
 *
 * <p>The target is built from the warrior's two relevant skill values on top of a base of six, which
 * puts every experience tier below Elite past what 2d6 can reach - Green needs 17, Regular 15, Veteran
 * 13. Winning a Trial of Bloodright is meant to be a long shot for an ordinary warrior, not something
 * the dice rule out, so the target is capped at the best a roll can produce.</p>
 */
class BloodnameTargetCapTest {

    /** The highest total two six-sided dice can produce. */
    private static final int HIGHEST_POSSIBLE_ROLL = 12;

    /** The lowest total two six-sided dice can produce. */
    private static final int LOWEST_POSSIBLE_ROLL = 2;

    @Test
    void theHardestTargetIsStillReachable() {
        assertTrue(ForceHumanResources.MAXIMUM_BLOODNAME_TARGET <= HIGHEST_POSSIBLE_ROLL,
              "a target above " + HIGHEST_POSSIBLE_ROLL + " can never be rolled on 2d6, which would "
                    + "make a Bloodname impossible rather than merely unlikely");
    }

    @Test
    void theHardestTargetIsNotAGiveaway() {
        // The other half of the contract: capping the target must not turn a long shot into a
        // formality for warriors who have earned nothing.
        assertTrue(ForceHumanResources.MAXIMUM_BLOODNAME_TARGET > LOWEST_POSSIBLE_ROLL,
              "the hardest target should still be hard");
        assertTrue(ForceHumanResources.MAXIMUM_BLOODNAME_TARGET >= 11,
              "a warrior on the hardest target should need a near-perfect roll, not a middling one");
    }
}
