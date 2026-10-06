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
package mekhq.campaign.digitalGM.stratCon.biome;

/**
 * The broad class a StratCon terrain type belongs to. The category is authored alongside each terrain in the biome
 * manifest, so improved sector generation and the wider StratCon code can reason about terrain by category rather than
 * by matching hardcoded terrain-name strings.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum StratConTerrainCategory {
    /** Open water. Always revealed; never hosts scenarios or facilities; capped so a sector keeps dry land. */
    OCEAN,
    /** Mountainous ground. Drawn as ranges and never overwritten by later terrain passes. */
    MOUNTAIN,
    /** Built-up ground. Handled as an overlay, so it is excluded from the base dry-terrain fill. */
    URBAN,
    /** Volcanic ground. Occasionally replaces a mountain range; part of the airless terrain set. */
    VOLCANIC,
    /** Barren, lifeless rock (lunar/planetary surfaces); part of the airless terrain set. */
    LUNAR,
    /** Living vegetation. Excluded on tainted, toxic, or airless worlds; favored on breathable, wet worlds. */
    VEGETATION,
    /** Barren, dry, or frozen ground. Favored on non-breathable, dry, icy, or rocky worlds. */
    BARREN,
    /** Rolling hills. Used as the foothill/piedmont ring around mountains, and neutral in the base fill weighting. */
    HILLS,
    /** Cultivated farmland. Placed as a catchment around cities over arable land, so it is excluded from the base fill. */
    AGRICULTURE,
    /** Terrain with no vegetation/barren bias, and the default for anything uncategorized. */
    NEUTRAL
}
