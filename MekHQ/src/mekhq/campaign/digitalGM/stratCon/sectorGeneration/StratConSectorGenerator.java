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

import java.util.EnumSet;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;

/**
 * The improved StratCon terrain generator: builds a sector's terrain from planetary data and a hydrology profile,
 * rather than the legacy random-stripe placement. This is the entry point wired behind the alternate-terrain option.
 *
 * <p>The pipeline runs in a strict order: select the biome from temperature, choose a hydrology profile and place
 * oceans, place mountains, derive the geographic fields and fill the remaining dry land from them, place cities and
 * their farmland, lay the roads, then reveal the open water.</p>
 *
 * <p>That order is a constraint rather than a preference, because the later stages read what the earlier ones wrote
 * off the track. It is declared in {@link GenerationStage} and enforced by {@link PipelineOrder}, so a reordering
 * fails loudly instead of quietly producing a worse sector.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConSectorGenerator {
    private StratConSectorGenerator() {}

    private static final String FALLBACK_OCEAN_TERRAIN = "Sea";

    /**
     * The stages of {@link #generate}, and what each one requires to have already run.
     *
     * <p>The order is not a matter of taste: several stages read what earlier ones wrote off the track rather than
     * receiving it as an argument, so running them out of order produces a quietly worse sector instead of an error.
     * {@link StratConTerrainFields} measures each hex's distance to water and to relief, so it is meaningless before
     * oceans and mountains exist; {@link StratConRoadPlacer} builds its network from the cities and farmland it finds
     * on the track, so running it first lays no roads at all. Declaring the constraint here makes a reordering fail
     * immediately and loudly - see {@link PipelineOrder}.</p>
     */
    enum GenerationStage {
        BIOME,
        OCEANS(BIOME),
        MOUNTAINS(BIOME),
        TERRAIN_FIELDS(OCEANS, MOUNTAINS),
        TERRAIN_FILL(TERRAIN_FIELDS),
        CITIES(TERRAIN_FILL),
        FARMLAND(CITIES),
        ROADS(TERRAIN_FILL, CITIES, FARMLAND);

        // A plain array rather than an EnumSet: EnumSet.of would touch the enum class while its constants are still
        // being constructed, which fails at class-initialization time.
        private final GenerationStage[] prerequisites;

        GenerationStage(GenerationStage... prerequisites) {
            this.prerequisites = prerequisites;
        }

        GenerationStage[] prerequisites() {
            return prerequisites;
        }
    }

    /**
     * Records which pipeline stages have been reached, and refuses one whose prerequisites have not.
     *
     * <p>A stage that is deliberately not run - cities and farmland, when the Ares Conventions suppress them - is
     * {@link #skip(GenerationStage) skipped} rather than omitted, so the stages that follow it still consider their
     * prerequisite satisfied. Roads legitimately run over a sector with no cities; what they cannot do is run
     * <em>before</em> the cities that were going to be placed.</p>
     */
    static final class PipelineOrder {
        private final Set<GenerationStage> reached = EnumSet.noneOf(GenerationStage.class);

        /**
         * Marks a stage as about to run.
         *
         * @throws IllegalStateException if any of the stage's prerequisites has neither run nor been skipped
         */
        void enter(GenerationStage stage) {
            for (GenerationStage prerequisite : stage.prerequisites()) {
                if (!reached.contains(prerequisite)) {
                    throw new IllegalStateException("StratCon sector generation ran " +
                                                          stage +
                                                          " before " +
                                                          prerequisite +
                                                          ", which it reads off the track. Restore the pipeline order" +
                                                          " in StratConSectorGenerator.generate().");
                }
            }

            reached.add(stage);
        }

        /** Marks a stage as deliberately not run, satisfying it as a prerequisite for later stages. */
        void skip(GenerationStage stage) {
            reached.add(stage);
        }

        boolean hasReached(GenerationStage stage) {
            return reached.contains(stage);
        }
    }

    /**
     * Generates terrain for the given track using the improved pipeline.
     *
     * @param track        the track to fill; its width, height, and temperature must already be set
     * @param profile      the destination planet's resolved data
     * @param latitudeBand the sector's latitude band, which drives the latitudinal terrain gradient
     * @param allowCities  {@code true} to place cities; {@code false} suppresses them entirely (e.g. when both sides
     *                     observe the Ares Conventions)
     */
    public static void generate(StratConTrackState track, PlanetProfile profile, LatitudeBand latitudeBand,
          boolean allowCities) {
        PipelineOrder order = new PipelineOrder();

        order.enter(GenerationStage.BIOME);
        StratConBiome biome = selectBiome(track.getTemperature());
        String oceanTerrain = oceanTerrainFor(biome);

        // Record the latitude band for the sector info panel; the urban profile is recorded below when cities are on.
        track.setLatitudeBand(latitudeBand.name());
        track.setUrbanProfile(null);

        // Hydrology: pick a profile from the planet's water coverage, then place oceans in that profile's shape.
        StratConHydrology hydrology = StratConHydrology.getInstance();
        HydrologyProfile hydrologyProfile = hydrology.selectProfile(profile.waterPercent());
        track.setHydrologyProfile(hydrologyProfile.type().name());
        int oceanPercent = hydrology.rollOceanPercent(hydrologyProfile);
        int oceanTargetHexes = (int) Math.round((oceanPercent / 100.0) * track.getWidth() * track.getHeight());
        order.enter(GenerationStage.OCEANS);
        StratConOceanPlacer.placeOceans(track, hydrologyProfile.type(), oceanTargetHexes, oceanTerrain);

        // Mountains: an orogeny profile selected from the planet's conditions shapes the ranges; gravity scales their
        // number. Volcanic where the profile calls for it, never over ocean, and only when the biome offers mountains.
        OrogenyProfile orogeny = StratConOrogeny.getInstance().selectProfile(profile);
        track.setOrogenyProfile(orogeny.type().name());
        order.enter(GenerationStage.MOUNTAINS);
        StratConMountainPlacer.placeMountains(track,
              mountainTerrainFor(biome),
              orogeny,
              profile.gravity(),
              profile.habitability());

        // Dry fill: geography-aware terrain that follows moisture, rain shadow, and coldness, painted in coherent
        // patches from the biome's climate-appropriate terrains.
        int windDirection = Compute.randomInt(StratConHexGeometry.HEX_DIRECTIONS);
        order.enter(GenerationStage.TERRAIN_FIELDS);
        StratConTerrainFields fields = StratConTerrainFields.compute(track, latitudeBand, windDirection);
        order.enter(GenerationStage.TERRAIN_FILL);
        StratConTerrainFiller.fill(track, biome, profile, fields);

        // Cities: an overlay whose count comes from population and whose arrangement comes from the urban profile.
        if (allowCities) {
            UrbanProfile urban = StratConUrban.getInstance().selectProfile(profile);
            track.setUrbanProfile(urban.type().name());
            order.enter(GenerationStage.CITIES);
            StratConCityPlacer.placeCities(track, profile, urban);
            // Farmland: a catchment of cultivated hexes radiating out from each city over arable land.
            order.enter(GenerationStage.FARMLAND);
            StratConFarmPlacer.placeFarms(track, profile, urban);
        } else {
            order.skip(GenerationStage.CITIES);
            order.skip(GenerationStage.FARMLAND);
        }

        // Roads: connect the cities and branch each network off the map.
        order.enter(GenerationStage.ROADS);
        StratConRoadPlacer.recalculateRoads(track);

        // Open water carries no fog of war.
        revealOceanHexes(track);
    }

    /**
     * Selects the biome whose temperature band contains the track temperature, falling back to the coldest biome when
     * the temperature is below every band.
     */
    private static StratConBiome selectBiome(int temperatureCelsius) {
        int kelvin = temperatureCelsius + StratConContractInitializer.ZERO_CELSIUS_IN_KELVIN;
        var tempMap = StratConBiomeManifest.getInstance().getTempMap(StratConBiomeManifest.TERRAN_BIOME);
        var entry = tempMap.floorEntry(kelvin);
        if (entry == null) {
            entry = tempMap.firstEntry();
        }
        return entry.getValue();
    }

    /**
     * @return the biome's ocean terrain type (climate-appropriate), or a plain sea when the biome offers no water
     */
    private static String oceanTerrainFor(StratConBiome biome) {
        for (String terrainType : biome.allowedTerrainTypes) {
            if (StratConBiomeManifest.isOceanTerrain(terrainType)) {
                return terrainType;
            }
        }
        return FALLBACK_OCEAN_TERRAIN;
    }

    /**
     * @return the biome's mountain terrain type, or {@code null} when the biome offers no mountains
     */
    private static @Nullable String mountainTerrainFor(StratConBiome biome) {
        for (String terrainType : biome.allowedTerrainTypes) {
            if (StratConBiomeManifest.isMountainTerrain(terrainType)) {
                return terrainType;
            }
        }
        return null;
    }

    /**
     * Adds every ocean hex to the track's revealed set, so open water is always visible.
     */
    public static void revealOceanHexes(StratConTrackState track) {
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords))) {
                    track.getRevealedCoords().add(coords);
                }
            }
        }
    }
}
