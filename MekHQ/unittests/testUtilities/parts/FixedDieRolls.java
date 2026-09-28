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
package testUtilities.parts;

import megamek.common.MMRandom;
import megamek.common.compute.Compute;

/**
 * A stand-in for MegaMek's random number generator on which every six-sided die shows the same face, so any roll made
 * through {@link Compute} has a known result: with every die showing 3, a 2d6 roll is 6, and a 3d6 roll that keeps two
 * dice is also 6. Technician skill checks and maintenance checks roll through {@link Compute}, not through the
 * campaign's own dice, so this is how a test fixes their rolls.
 *
 * <pre>{@code
 * FixedDieRolls.everyDieShows(6);   // every 2d6 roll is 12
 * try {
 *     campaign.fixPart(part, tech);
 * } finally {
 *     FixedDieRolls.restore();
 * }
 * }</pre>
 */
public final class FixedDieRolls extends MMRandom {
    private final int face;

    private FixedDieRolls(int face) {
        this.face = face;
    }

    /**
     * Makes every die rolled through {@link Compute} show the given face until {@link #restore()} is called.
     *
     * @param face the face every die shows, from {@code 1} to {@code 6}
     */
    public static void everyDieShows(int face) {
        if ((face < 1) || (face > 6)) {
            throw new IllegalArgumentException("A six-sided die has no face " + face);
        }
        Compute.setRNG(new FixedDieRolls(face));
    }

    /**
     * Puts MegaMek's normal random number generator back.
     */
    public static void restore() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    /**
     * Returns the chosen face less one, which a die adds one to; for a range smaller than a die, the highest value in
     * that range.
     */
    @Override
    public int randomInt(int maxValue) {
        return Math.min(face - 1, maxValue - 1);
    }

    /**
     * Returns the middle of the range, for the rare roll that asks for a fraction.
     */
    @Override
    public float randomFloat() {
        return 0.5f;
    }
}
