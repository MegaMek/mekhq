/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.stratcon;

import megamek.common.compute.Compute;
import megamek.common.board.Coords;

/**
 * This class handles placement of Stratcon terrain
 */
public class StratconTerrainPlacer {
    /**
     * Loads base terrain and "stripes" the passed-in track.
     *
     * @param track The track to process.
     */
    public static void InitializeTrackTerrain(StratconTrackState track) {
        // 1. get the correct biome list according to track temperature
        // 2. pick random biome to be "base terrain"; apply it to all track hexes
        // 3. "stripe" the other biomes:
        // striping is "starting coordinate" (random track coord) and "ending
        // coordinate"
        // TODO: Maybe more than one of each biome?
        // TODO: Map category being displayed for some reason
        int kelvinTemp = track.getTemperature() + StratconContractInitializer.ZERO_CELSIUS_IN_KELVIN;
        StratconBiome biome = StratconBiomeManifest.getInstance().getTempMap(StratconBiomeManifest.TERRAN_BIOME)
                                    .floorEntry(kelvinTemp).getValue();

        int baseTerrainIndex = Compute.randomInt(biome.allowedTerrainTypes.size());

        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                track.setTerrainTile(new StratconCoords(x, y), biome.allowedTerrainTypes.get(baseTerrainIndex));
            }
        }

        // the following code is useful for quickly displaying all possible (Terran)
        // terrain types on a newly generated track
        // unclean, but a pain enough to reproduce that I've left it here
        /*
         * int x = 0;
         * int y = 0;
         * Set<String> terrainTypes = new HashSet<>();
         * for (StratconBiome testBiome :
         * StratconBiomeManifest.getInstance().getTempMap("Terran").values()) {
         * terrainTypes.addAll(testBiome.allowedTerrainTypes);
         * }
         *
         * for (String biomeName : terrainTypes) {
         * track.setTerrainTile(new StratconCoords(x, y), biomeName);
         * x++;
         * if (x >= track.getWidth()) {
         * x = 0;
         * y++;
         * }
         * }
         */

        for (int x = 0; x < biome.allowedTerrainTypes.size(); x++) {
            if (x != baseTerrainIndex) {
                DrawStripe(track, biome.allowedTerrainTypes.get(x));
            }
        }
    }

    /**
     * Draws a "stripe" of the given terrain type on the given track
     *
     * @param track           Track to stripe
     * @param terrainTypeName Terrain type
     */
    private static void DrawStripe(StratconTrackState track, String terrainTypeName) {
        int startX = Compute.randomInt(track.getWidth());
        int startY = Compute.randomInt(track.getHeight());

        int endX = Compute.randomInt(track.getWidth());
        int endY = Compute.randomInt(track.getHeight());
        Coords startPoint = new Coords(startX, startY);
        Coords endPoint = new Coords(endX, endY);

        for (Coords coords : StratconCoords.intervening(startPoint, endPoint)) {
            track.setTerrainTile(new StratconCoords(coords.getX(), coords.getY()), terrainTypeName);
        }
    }
}
