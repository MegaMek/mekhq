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
package mekhq.campaign.digitalGM.stratCon.generation;

import megamek.common.compute.Compute;

/**
 * The latitude band a StratCon sector sits in, used by improved sector generation to bias a sector's temperature away
 * from the planet's equatorial baseline. Bands grow progressively colder toward the poles; the northern and southern
 * halves of each band share the same temperature offset.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum LatitudeBand {
    EQUATORIAL(0),
    NORTH_TROPICAL(-8),
    SOUTH_TROPICAL(-8),
    NORTH_TEMPERATE(-20),
    SOUTH_TEMPERATE(-20),
    NORTH_POLAR(-40),
    SOUTH_POLAR(-40);

    private final int temperatureOffset;

    LatitudeBand(int temperatureOffset) {
        this.temperatureOffset = temperatureOffset;
    }

    /**
     * @return the temperature offset in degrees Celsius applied to the planet's equatorial temperature for a sector in
     *       this band (zero at the equator, progressively more negative toward the poles)
     */
    public int getTemperatureOffset() {
        return temperatureOffset;
    }

    /**
     * @return a uniformly random latitude band
     */
    public static LatitudeBand random() {
        return values()[Compute.randomInt(values().length)];
    }
}
