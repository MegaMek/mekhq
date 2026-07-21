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
 * The definition of a single StratCon terrain type, as authored in the biome manifest. It ties a terrain's name to its
 * broad {@link StratConTerrainCategory}, and optionally to its hex image. Terrain classification therefore lives in the
 * data rather than in hardcoded name lists.
 *
 * <p>Fields are public and unannotated so they bind by name under JAXB's default access, matching
 * {@link StratConBiome}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConTerrainType {
    /** The terrain type's name, as referenced by biome terrain lists, map-type pools, and track terrain tiles. */
    public String name;

    /**
     * The path to this terrain's hex image. Optional: when absent, the manifest falls back to its {@code biomeImages}
     * lookup, so image data need not be duplicated here.
     */
    public String image;

    /** The terrain's broad category. Defaults to {@link StratConTerrainCategory#NEUTRAL} when unspecified. */
    public StratConTerrainCategory category;

    /**
     * Whether this terrain is arable, i.e. open land that a city's agricultural catchment can convert to farmland.
     * Defaults to {@code false}, so only terrains explicitly marked in the manifest (open, temperate ground) can be
     * farmed - never forest, swamp, desert, frozen ground, relief, ocean, or built-up hexes.
     */
    public boolean arable;

    /**
     * The Celsius offset this terrain's local climate applies to a sector's average temperature, used for the
     * selected-hex readout and for a scenario's board temperature. Defaults to {@code 0}, so only terrains the manifest
     * gives a value are anything other than temperate.
     *
     * <p>This is the terrain's <em>whole</em> offset, not a modifier stacked onto its category: a cold mountain
     * carries the elevation chill and the cold climate together in one number. Authoring it that way keeps the value a
     * reader can check against the tile in front of them, rather than one they have to reconstruct from rules
     * elsewhere.</p>
     */
    public int temperatureOffset;
}
