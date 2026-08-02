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
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

import java.util.List;
import java.util.function.ToDoubleFunction;

import megamek.common.compute.Compute;

/**
 * The weighted roll shared by the sector-generation profile libraries - hydrology, orogeny, urban, and sector shape.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class WeightedProfilePicker {
    /**
     * Granularity of the roll. The pick is a random fraction of the total weight, and this is how many distinct
     * fractions there are - fine enough that a profile weighted a millionth of the total can still come up.
     */
    private static final double PICK_RESOLUTION = 1_000_000.0;

    private WeightedProfilePicker() {}

    /**
     * Picks one profile at random, each profile's chance being its share of the total weight.
     *
     * <p>A list whose weights sum to zero or less - every profile ruled out by the planet, or an author leaving them
     * all at zero - yields the first profile rather than nothing, so generation always has something to work with.</p>
     *
     * @param profiles the profiles to choose between; must not be empty
     * @param weigher  supplies a profile's weight, called exactly once per profile
     * @param <T>      the profile type
     *
     * @return the chosen profile
     */
    static <T> T pick(List<T> profiles, ToDoubleFunction<T> weigher) {
        double[] weights = new double[profiles.size()];
        double totalWeight = 0.0;
        for (int index = 0; index < profiles.size(); index++) {
            weights[index] = weigher.applyAsDouble(profiles.get(index));
            totalWeight += weights[index];
        }

        if (totalWeight <= 0.0) {
            return profiles.getFirst();
        }

        double roll = (Compute.randomInt((int) PICK_RESOLUTION) / PICK_RESOLUTION) * totalWeight;
        double cumulative = 0.0;
        for (int index = 0; index < profiles.size(); index++) {
            cumulative += weights[index];
            if (roll < cumulative) {
                return profiles.get(index);
            }
        }

        // Only reachable through floating-point drift in the cumulative sum; the last profile owns the remainder.
        return profiles.getLast();
    }
}
