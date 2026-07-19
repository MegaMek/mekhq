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

/**
 * The mountain-building (orogeny) profiles available to improved sector generation. Each identifies both a bucket of
 * tunable weighting parameters (authored in {@code OrogenyProfiles.yaml}) and a distinct mountain-shape algorithm in
 * the mountain placer. A new profile therefore needs both a YAML entry and a shape implementation.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum OrogenyProfileType {
    /** Long, roughly parallel mountain chains. */
    CORDILLERA,
    /** Many short, parallel ridges separated by flats. */
    BASIN_AND_RANGE,
    /** A single large, clustered rugged upland. */
    MASSIF,
    /** Sparse, isolated peaks scattered across the sector. */
    SCATTERED_PEAKS,
    /** A curved chain of mostly volcanic peaks. */
    VOLCANIC_ARC,
    /** Scattered clusters of volcanic and lunar rock, favored on airless worlds. */
    SHIELD_CRATERED,
    /** A few low, worn-down ranges. */
    ERODED_ROLLING,
    /** A large, flat-topped upland. */
    PLATEAU
}
