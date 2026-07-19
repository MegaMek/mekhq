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

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Set;

import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;

/**
 * Places farmland on a StratCon track as an agricultural catchment around its cities: each city radiates farmland out
 * over nearby arable land, thinning with distance. How far the catchment reaches and how densely it fills come from the
 * sector's urban profile, scaled by the planet's habitability, population, and technology - so a fertile, well-settled
 * world is quilted with fields while a marginal frontier world shows only a sparse ring around each settlement.
 *
 * <p>Farmland is a base terrain (it replaces the arable hex it lands on), so it renders, serializes, and fogs like any
 * other terrain. Only hexes the biome manifest marks arable are ever converted; ocean, relief, forest, desert, frozen,
 * built-up, and already-farmed hexes are left alone.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFarmPlacer {
    private StratConFarmPlacer() {}

    /** Resolution of the per-hex probability roll. */
    private static final double PICK_RESOLUTION = 1000.0;

    /** The share of the base farm chance that survives out at the edge of a city's reach (nearer hexes farm more). */
    private static final double EDGE_FALLOFF = 0.4;

    /**
     * Grows farmland outward from every city on the track.
     *
     * @param track  the track to farm (must already have its cities and terrain placed)
     * @param planet the destination planet's resolved profile, driving how intensively the world is farmed
     * @param urban  the sector's urban profile, providing the farm reach and base density
     */
    public static void placeFarms(StratConTrackState track, PlanetProfile planet, UrbanProfile urban) {
        Set<StratConCoords> cities = track.getCities();
        if (cities.isEmpty()) {
            return;
        }

        int reach = urban.farmReachOrDefault();
        if (reach <= 0) {
            return;
        }

        double density = min(1.0, max(0.0, urban.farmDensityOrDefault() * farmingScale(planet)));
        if (density <= 0.0) {
            return;
        }

        for (StratConCoords city : cities) {
            growCatchment(track, city, reach, density);
        }
    }

    /**
     * Radiates farmland out from a single city over every arable hex within {@code reach} hexes (by hex distance),
     * converting each with a probability that thins with distance.
     */
    private static void growCatchment(StratConTrackState track, StratConCoords city, int reach, double density) {
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                int distance = city.distance(coords);
                if ((distance < 1) || (distance > reach)) {
                    continue;
                }

                if (isFarmable(track, coords) && (roll() < (density * distanceFalloff(distance, reach)))) {
                    track.setTerrainTile(coords, StratConBiomeManifest.FARMLAND);
                }
            }
        }
    }

    /**
     * @return the fraction of the base chance that applies at the given distance, tapering from 1.0 to
     *       {@link #EDGE_FALLOFF}.
     */
    private static double distanceFalloff(int distance, int reach) {
        if (reach <= 1) {
            return 1.0;
        }
        double t = (distance - 1) / (double) (reach - 1);
        return 1.0 - ((1.0 - EDGE_FALLOFF) * t);
    }

    /** @return {@code true} if the hex is open arable land that is not a city (and so not already farmland or water). */
    private static boolean isFarmable(StratConTrackState track, StratConCoords coords) {
        return !track.isCity(coords) && StratConBiomeManifest.isArableTerrain(track.getTerrainTile(coords));
    }

    /**
     * @return an intensity multiplier in roughly {@code [0.2, 1.1]} from the planet's habitability, population, and
     *       technology - harsh, empty, or low-tech worlds farm less; comfortable, populous, high-tech worlds farm
     *       more.
     */
    private static double farmingScale(PlanetProfile planet) {
        double habitabilityFactor = 0.35 + (0.65 * planet.habitability());
        double populationFactor = 0.55 + (0.45 * min(1.0, planet.populationLog() / 9.0));
        double techFactor = 0.85 + (0.25 * planet.techLevel());
        return habitabilityFactor * populationFactor * techFactor;
    }

    private static double roll() {
        return Compute.randomInt((int) PICK_RESOLUTION) / PICK_RESOLUTION;
    }
}
