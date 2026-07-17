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
package mekhq.campaign.digitalGM.stratCon;

import megamek.common.annotations.Nullable;

/**
 * A single urban (settlement) profile's tunable parameters, deserialized from {@code UrbanProfiles.yaml}.
 *
 * <p>Selection is multi-factor: a profile is weighted by how well the planet matches its Gaussian centers for
 * population (as {@code log10}), water coverage, habitability, and technology level. A {@code null} center means the
 * profile is indifferent to that condition. The placement parameters shape how the profile arranges cities.</p>
 *
 * @param type               the profile's identity, which also selects its city-placement algorithm
 * @param populationCenter   {@code log10} population this profile is most favored at, or {@code null} if indifferent
 * @param waterCenter        water coverage (percent) this profile is most favored at, or {@code null}
 * @param habitabilityCenter habitability (0..1) this profile is most favored at, or {@code null}
 * @param techCenter         technology level (0..1) this profile is most favored at, or {@code null}
 * @param cityCountModifier  multiplier on the population-derived city count, or {@code null} for neutral
 * @param clustering         how tightly cities cluster, {@code 0.0} (spread apart) to {@code 1.0} (tight), or
 *                           {@code null} for spread
 * @param coastalBias        how strongly cities favor the coast, {@code 0.0} to {@code 1.0}, or {@code null} for none
 *
 * @author Illiani
 * @since 0.51.01
 */
public record UrbanProfile(UrbanProfileType type, @Nullable Double populationCenter, @Nullable Double waterCenter,
      @Nullable Double habitabilityCenter, @Nullable Double techCenter, @Nullable Double cityCountModifier,
      @Nullable Double clustering, @Nullable Double coastalBias) {

    public double cityCountModifierOrDefault() {
        return (cityCountModifier == null) ? 1.0 : cityCountModifier;
    }

    public double clusteringOrDefault() {
        return (clustering == null) ? 0.0 : clustering;
    }

    public double coastalBiasOrDefault() {
        return (coastalBias == null) ? 0.0 : coastalBias;
    }
}
