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
 * A single orogeny (mountain-building) profile's tunable parameters, deserialized from {@code OrogenyProfiles.yaml}.
 *
 * <p>Selection is multi-factor: a profile is weighted by how well the planet matches its Gaussian centers for gravity,
 * temperature, and water coverage, multiplied by categorical affinities for rocky, icy, and airless worlds. A
 * {@code null} Gaussian center means the profile is indifferent to that condition (it contributes a neutral factor of
 * one). Categorical multipliers default to one (neutral) when omitted.</p>
 *
 * <p>The multipliers, range-count modifier, and volcanism chance are nullable so that an omitted value is neutral
 * rather than zero: a missing multiplier or modifier resolves to one, and a missing volcanism chance resolves to the
 * default. Use the {@code *OrDefault()} accessors to read resolved values.</p>
 *
 * @param type               the profile's identity, which also selects its mountain-shape algorithm
 * @param gravityCenter      surface gravity (G) this profile is most favored at, or {@code null} if indifferent
 * @param temperatureCenter  equatorial temperature (Celsius) this profile is most favored at, or {@code null}
 * @param waterCenter        water coverage (percent) this profile is most favored at, or {@code null}
 * @param rockyMultiplier    weight multiplier applied on rocky worlds, or {@code null} for neutral
 * @param icyMultiplier      weight multiplier applied on icy worlds, or {@code null} for neutral
 * @param airlessMultiplier  weight multiplier applied on airless worlds, or {@code null} for neutral
 * @param rangeCountModifier multiplier on the gravity-derived number of mountain ranges, or {@code null} for neutral
 * @param volcanismChance    percent chance a range in this profile is volcanic, or {@code null} for the default
 *
 * @author Illiani
 * @since 0.51.01
 */
public record OrogenyProfile(OrogenyProfileType type, @Nullable Double gravityCenter,
      @Nullable Double temperatureCenter, @Nullable Double waterCenter, @Nullable Double rockyMultiplier,
      @Nullable Double icyMultiplier, @Nullable Double airlessMultiplier, @Nullable Double rangeCountModifier,
      @Nullable Integer volcanismChance) {

    /** The volcanism chance used when a profile omits its own. */
    public static final int DEFAULT_VOLCANISM_CHANCE = 10;

    public double rockyMultiplierOrDefault() {
        return (rockyMultiplier == null) ? 1.0 : rockyMultiplier;
    }

    public double icyMultiplierOrDefault() {
        return (icyMultiplier == null) ? 1.0 : icyMultiplier;
    }

    public double airlessMultiplierOrDefault() {
        return (airlessMultiplier == null) ? 1.0 : airlessMultiplier;
    }

    public double rangeCountModifierOrDefault() {
        return (rangeCountModifier == null) ? 1.0 : rangeCountModifier;
    }

    public int volcanismChanceOrDefault() {
        return (volcanismChance == null) ? DEFAULT_VOLCANISM_CHANCE : volcanismChance;
    }
}
