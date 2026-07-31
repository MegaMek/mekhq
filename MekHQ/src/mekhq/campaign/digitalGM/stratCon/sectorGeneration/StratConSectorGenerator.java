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
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;

/**
 * The improved StratCon terrain generator: builds a sector's terrain from planetary data and a hydrology profile,
 * rather than the legacy random-stripe placement. This is the entry point wired behind the alternate-terrain option.
 *
 * <p>The pipeline runs in a strict order: select the biome from temperature, choose a hydrology profile and place
 * oceans, place mountains, derive the geographic fields and fill the remaining dry land from them, place cities and
 * their farmland, then reveal the open water.</p>
 *
 * <p><b>Roads are not laid here.</b> A sector's road network spans its cities, its farmland <em>and</em> the
 * planet-owner's facilities, and facilities are seeded after generation returns - so laying roads inside this method
 * built a network that the caller immediately discarded and rebuilt. The caller lays them once, through
 * {@code StratConContractInitializer.connectFacilitiesToRoads}, after the sector is populated. Both paths that generate
 * a sector - initial contract setup and the GM's Regenerate Sector - do so.</p>
 *
 * <h2>Why the order is fixed</h2>
 *
 * <p>Most stages take the track as an argument and read off it what earlier stages wrote there, rather than being
 * handed that information directly. Reordering them therefore does not fail on its own - it quietly produces a worse
 * sector. The dependencies are:</p>
 *
 * <ul>
 *     <li><b>Everything after the biome.</b> The biome fixes which terrain names may appear at this temperature, and
 *     every later stage draws from that palette.</li>
 *     <li><b>Mountains need the oceans.</b> {@link StratConMountainPlacer} refuses to overwrite water, so ranges laid
 *     before the sea would be drowned by it and the sector would lose relief it was meant to have.</li>
 *     <li><b>Terrain fields need both.</b> {@link StratConTerrainFields} measures every hex's distance to open water
 *     and to relief. Computed first it measures distance to nothing: moisture reads zero everywhere and the rain
 *     shadow falls nowhere.</li>
 *     <li><b>The dry fill needs the fields.</b> This one dependency is enforced by the compiler, because
 *     {@link StratConTerrainFiller#fill} takes the computed fields as a parameter.</li>
 *     <li><b>Farmland needs the cities</b> it radiates out from, so with no cities on the track it places nothing.</li>
 * </ul>
 *
 * <p>Every one of those failures is silent, and several would pass a casual look at the resulting map. That is what
 * {@link GenerationStage} and {@link PipelineOrder} exist for: the constraint is declared once, and a reordering
 * throws on the first sector generated instead of shipping subtly wrong terrain.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConSectorGenerator {
    private StratConSectorGenerator() {}

    private static final String FALLBACK_OCEAN_TERRAIN = "Sea";

    /**
     * The stages of {@link #generate}, and what each one requires to have already run. See the class javadoc for what
     * each dependency is and what breaks when it is violated.
     *
     * <h2>Why an enum, when there is a class per stage</h2>
     *
     * <p>The constant names shadow the placer classes almost one for one, which invites the obvious question: why not
     * make this an interface the placers implement, so there is a single concept instead of two parallel lists?</p>
     *
     * <p>Because this enum <b>declares ordering and nothing else</b>. It holds no behavior, nothing dispatches on it,
     * and it is never mapped to a class at runtime; its entire content is the prerequisite graph, and its only uses are
     * the {@link PipelineOrder#enter} calls in {@link #generate}. It is a vocabulary for stating a constraint, not a
     * second spelling of the placers. Collapsing it into them would not remove a duplicated concept - it would scatter
     * the constraint across seven files, where no one reading any one of them could see the whole order.</p>
     *
     * <p>Making the placers implement a common stage interface is also more expensive than it looks, and would cost
     * something real:</p>
     *
     * <ul>
     *     <li>They do not share a signature. Ocean placement needs a hydrology type, a hex target and a terrain name;
     *     mountains need an orogeny profile, gravity and habitability; the fields need a latitude band and a wind
     *     direction. Unifying them requires a context object carrying all of it.</li>
     *     <li>That context cannot simply be the track. {@link StratConTrackState} is the save format - it is
     *     JAXB-bound - so hanging the planet profile, biome, wind direction and field arrays off it would mean writing
     *     generation scratch into every save, or maintaining transient-field discipline forever.</li>
     *     <li>It would demote the one ordering constraint the compiler currently enforces.
     *     {@link StratConTerrainFiller#fill} takes the computed fields as a parameter, so it cannot be called before
     *     them; on a shared context that becomes a nullable slot checked at runtime, if at all.</li>
     * </ul>
     *
     * <p>A genuinely different way to build a sector is not a second stage list either - it is another
     * {@link mekhq.campaign.digitalGM.ISectorGenerationStrategy}, which is the seam that already picks between this
     * generator and the legacy placer. The placers are public and stateless, so such a strategy can compose them in
     * whatever order it likes without any of the machinery above.</p>
     */
    enum GenerationStage {
        BIOME,
        OCEANS(BIOME),
        MOUNTAINS(BIOME),
        TERRAIN_FIELDS(OCEANS, MOUNTAINS),
        TERRAIN_FILL(TERRAIN_FIELDS),
        CITIES(TERRAIN_FILL),
        FARMLAND(CITIES);

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
     * <p>One instance lives for the duration of a single {@link #generate} call and is thrown away with it, so this
     * holds no state between sectors and costs a few set operations per sector. The check is a development guard rather
     * than a runtime feature: it never fires for correct code, and its whole purpose is to convert a silent degradation
     * into an immediate, named failure the first time a sector is generated after someone reorders the pipeline.</p>
     *
     * <p>A stage that is deliberately not run - cities and farmland, when the Ares Conventions suppress them - is
     * {@link #skip(GenerationStage) skipped} rather than omitted, so the stages that follow it still consider their
     * prerequisite satisfied. Roads legitimately run over a sector with no cities; what they cannot do is run
     * <em>before</em> the cities that were going to be placed. Omitting the stages instead would make the guard throw
     * on every Ares-Conventions contract.</p>
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
        track.getRevealedCoords().addAll(StratConHexGeometry.oceanHexes(track));
    }
}
