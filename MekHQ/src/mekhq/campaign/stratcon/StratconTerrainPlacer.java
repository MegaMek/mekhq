/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.stratcon;
import megamek.common.Compute;
import megamek.common.Coords;

/**
 * This class handles placement of Stratcon terrain
 */
public class StratconTerrainPlacer {
    /**
     * Loads base terrain and "stripes" the passed-in track.
     * @param track The track to process.
     */
    public static void InitializeTrackTerrain(StratconTrackState track) {
        // 1. get the correct biome list according to track temperature
        // 2. pick random biome to be "base terrain"; apply it to all track hexes
        // 3. "stripe" the other biomes:
        //      striping is "starting coordinate" (random track coord) and "ending coordinate"
        //      TODO: Maybe more than one of each biome?
        //      TODO: Map category being displayed for some reason
        int kelvinTemp = track.getTemperature() + StratconContractInitializer.ZERO_CELSIUS_IN_KELVIN;
        try {
            StratconBiome biome = StratconBiomeManifest.getInstance().biomeMap.floorEntry(kelvinTemp).getValue();

            int baseTerrainIndex = Compute.randomInt(biome.allowedTerrainTypes.size());

            for (int x = 0; x < track.getWidth(); x++) {
                for (int y = 0; y < track.getHeight(); y++) {
                    track.setTerrainTile(new StratconCoords(x, y), biome.allowedTerrainTypes.get(baseTerrainIndex));
                }
            }

            for (int x = 0; x < biome.allowedTerrainTypes.size(); x++) {
                if (x != baseTerrainIndex) {
                    DrawStripe(track, biome.allowedTerrainTypes.get(x));
                }
            }
        }
        catch(Exception e) {
            int alpha = 1;
            throw e;
        }
    }

    /**
     * Draws a "stripe" of the given terrain type on the given track
     * @param track Track to stripe
     * @param terrainTypeName Terrain type
     */
    private static void DrawStripe(StratconTrackState track, String terrainTypeName) {
        int startX = Compute.randomInt(track.getWidth());
        int startY = Compute.randomInt(track.getHeight());

        int endX = Compute.randomInt(track.getWidth());
        int endY = Compute.randomInt(track.getHeight());
        Coords startPoint = new Coords(startX, startY);
        Coords endPoint = new Coords(endX, endY);

        for(Coords coords : StratconCoords.intervening(startPoint, endPoint)) {
            track.setTerrainTile(new StratconCoords(coords.getX(), coords.getY()), terrainTypeName);
        }
    }
}
