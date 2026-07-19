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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;

/**
 * Places cities onto a StratCon track as an overlay (a city sits on top of whatever base terrain its hex holds). The
 * number of cities scales with the planet's population; their arrangement follows the selected {@link UrbanProfile}
 * (coastal bias and clustering). Cities never sit on open water.
 *
 * <p>Whether cities are placed at all is decided by the caller (they are suppressed entirely when both the employer
 * and enemy observe the Ares Conventions).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConCityPlacer {
    private StratConCityPlacer() {}

    // Population thresholds and the fraction of dry hexes that become cities in each tier (§8).
    private static final long POPULATION_HAMLET = 10_000L;
    private static final long POPULATION_SMALL = 1_000_000L;
    private static final long POPULATION_MEDIUM = 100_000_000L;
    private static final long POPULATION_LARGE = 1_000_000_000L;
    /** Population log10 (~a billion) at which a sector is considered maximally urbanized for battle-map scaling. */
    private static final double URBANIZATION_POPULATION_LOG = 9.0;

    private static final double DENSITY_SMALL = 0.01;
    private static final double DENSITY_MEDIUM = 0.02;
    private static final double DENSITY_LARGE = 0.04;
    private static final double DENSITY_TEEMING = 0.06;

    private static final double COASTAL_WEIGHT = 5.0;
    private static final double DISTANCE_FLOOR = 0.1;
    private static final double PICK_RESOLUTION = 1_000_000.0;

    /**
     * Places cities on the track according to the planet's population and the urban profile.
     *
     * @param track  the track to populate; cities are added to its overlay
     * @param planet the destination planet's resolved data
     * @param urban  the selected urban profile
     */
    public static void placeCities(StratConTrackState track, PlanetProfile planet, UrbanProfile urban) {
        List<StratConCoords> land = landHexes(track);
        int cityCount = min(cityCount(land.size(), planet, urban), land.size());
        if (cityCount <= 0) {
            return;
        }

        // How built-up this sector's cities read on the battle map, from the planet's population (log10 ~9 = billions).
        // Carried onto city-hex scenarios so a metropolis fights bigger than a frontier hamlet.
        track.setUrbanizationLevel(Math.clamp(planet.populationLog() / URBANIZATION_POPULATION_LOG, 0.0, 1.0));

        // Primate City: one dominant metropolis - grow all the city hexes as a single connected blob rather than
        // scattering them across the sector.
        if (urban.type() == UrbanProfileType.PRIMATE_CITY) {
            placePrimateCity(track, land, cityCount, urban.coastalBiasOrDefault());
            return;
        }

        double coastalBias = urban.coastalBiasOrDefault();
        double clustering = urban.clusteringOrDefault();
        int maxDistance = max(1, track.getWidth() + track.getHeight());

        for (int placed = 0; placed < cityCount; placed++) {
            StratConCoords chosen = pickCityHex(track, land, coastalBias, clustering, maxDistance);
            if (chosen == null) {
                break;
            }
            track.addCity(chosen);
        }
    }

    /**
     * Places a Primate City: a single dominant metropolis. Seeds one city hex (respecting the coastal bias) and grows a
     * connected blob of {@code cityCount} city hexes outward from it, never crossing ocean, so all the cities form one
     * contiguous urban area instead of separate settlements.
     */
    private static void placePrimateCity(StratConTrackState track, List<StratConCoords> land, int cityCount,
          double coastalBias) {
        int maxDistance = max(1, track.getWidth() + track.getHeight());
        StratConCoords seed = pickCityHex(track, land, coastalBias, 0.0, maxDistance);
        if (seed == null) {
            return;
        }

        // The blob may not spread onto water.
        Set<StratConCoords> ocean = new HashSet<>();
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords))) {
                    ocean.add(coords);
                }
            }
        }

        for (StratConCoords cityHex : StratConHexGeometry.growBlob(track, seed, cityCount, ocean)) {
            track.addCity(cityHex);
        }
    }

    /**
     * @return the number of cities to place: the population-tier density times the dry-hex count and the profile's
     *       count modifier, at least one when the tier calls for any cities, and zero otherwise
     */
    static int cityCount(int landHexes, PlanetProfile planet, UrbanProfile urban) {
        double density = cityDensity(planet.population());
        if (density <= 0.0) {
            return 0;
        }
        int count = (int) round(landHexes * density * urban.cityCountModifierOrDefault());
        return max(1, count);
    }

    /**
     * @return the fraction of dry hexes that host a city at the given population, or {@code 0.0} for an unknown or
     *       negligible population
     */
    static double cityDensity(@Nullable Long population) {
        if ((population == null) || (population < POPULATION_HAMLET)) {
            return 0.0;
        }
        if (population < POPULATION_SMALL) {
            return DENSITY_SMALL;
        }
        if (population < POPULATION_MEDIUM) {
            return DENSITY_MEDIUM;
        }
        if (population < POPULATION_LARGE) {
            return DENSITY_LARGE;
        }
        return DENSITY_TEEMING;
    }

    /**
     * Picks the next city hex by weight, favoring coasts (per the profile's coastal bias) and either clustering near or
     * spreading away from existing cities (per the profile's clustering).
     */
    private static @Nullable StratConCoords pickCityHex(StratConTrackState track, List<StratConCoords> land,
          double coastalBias, double clustering, int maxDistance) {
        List<StratConCoords> candidates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0.0;

        // One sweep out from every existing city gives each candidate its distance to the nearest one, measured by
        // stepping across the map rather than by coordinate distance (see StratConHexGeometry.withinRadius).
        Map<StratConCoords, Integer> cityDistances = StratConHexGeometry.stepDistancesFrom(track,
              track.getCities(),
              Integer.MAX_VALUE);

        for (StratConCoords coords : land) {
            if (track.isCity(coords)) {
                continue;
            }

            double weight = 1.0;
            if (isCoastal(track, coords)) {
                weight += coastalBias * COASTAL_WEIGHT;
            }

            if (!track.getCities().isEmpty()) {
                int distance = cityDistances.getOrDefault(coords, maxDistance);
                double near = 1.0 / (1.0 + distance);
                double far = (double) distance / maxDistance;
                double distanceFactor = (clustering * near) + ((1.0 - clustering) * far);
                weight *= (DISTANCE_FLOOR + distanceFactor);
            }

            candidates.add(coords);
            weights.add(weight);
            total += weight;
        }

        if (candidates.isEmpty()) {
            return null;
        }
        if (total <= 0.0) {
            return candidates.get(Compute.randomInt(candidates.size()));
        }

        double roll = (Compute.randomInt((int) PICK_RESOLUTION) / PICK_RESOLUTION) * total;
        double cumulative = 0.0;
        for (int index = 0; index < candidates.size(); index++) {
            cumulative += weights.get(index);
            if (roll < cumulative) {
                return candidates.get(index);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private static boolean isCoastal(StratConTrackState track, StratConCoords coords) {
        for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, coords)) {
            if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(neighbor))) {
                return true;
            }
        }
        return false;
    }

    private static List<StratConCoords> landHexes(StratConTrackState track) {
        List<StratConCoords> land = new ArrayList<>();
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (!StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords))) {
                    land.add(coords);
                }
            }
        }
        return land;
    }
}
