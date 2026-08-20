/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static mekhq.campaign.digitalGM.stratCon.SupportPointNegotiation.negotiateInitialSupportPoints;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityFactory;
import mekhq.campaign.digitalGM.stratCon.gm.StratConGMs;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.LatitudeBand;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.SectorShapeProfile;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.SectorSpec;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorCountMethod;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorGenerator;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorPlanner;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorShape;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.enums.Alphabet;

/**
 * This class handles StratCon state initialization when a contract is signed.
 */
public class StratConContractInitializer {
    private static final MMLogger LOGGER = MMLogger.create(StratConContractInitializer.class);

    public static final int NUM_FORMATIONS_PER_TRACK = 3;
    public static final int ZERO_CELSIUS_IN_KELVIN = 273;

    /** Legacy sizing: hexes per required formation, laid out as a wider-than-tall rectangle. */
    private static final int LEGACY_HEXES_PER_FORMATION = 28;

    /**
     * Improved sizing: combat teams per assumed recon force. One in three of the teams fronting a sector is taken to be
     * out scouting it.
     */
    static final int COMBAT_TEAMS_PER_RECON_FORCE = 3;

    /**
     * Improved sizing: the land hexes one recon combat team is expected to cover in a quarter - three hexes a week over
     * the twelve weeks of three months. A sector is sized so the recon teams fronting it can scout all of its dry
     * ground inside three months.
     *
     * <p>Note what the underlying mechanic actually reveals, before revising this: a <em>regular</em> deployment
     * uncovers a single hex (scan range is zero unless a facility grants more), while a Patrol-role deployment covers
     * its hex plus the six around it.</p>
     */
    static final int RECON_HEXES_PER_QUARTER = 36;

    /** Improved sizing: the minimum dry fraction of a sector, so oceans never leave too little land. */
    private static final double MINIMUM_LAND_FRACTION = 0.25;

    /**
     * Improved sizing: the most hexes a generated sector may cover, whatever the planet and options ask for. Roughly a
     * 32x32 sector. Without it, a large ocean world with a condensed multi-unit sector and a doubled size multiplier
     * asks for several thousand hexes.
     */
    private static final int MAX_SECTOR_HEXES = 1024;

    /**
     * Upper bound on the {@link #maximumTeamsPerSector} search. A dry, small world with a reduced size multiplier fits
     * a great many teams into one sector, and nothing needs the exact figure past this - it only has to be larger than
     * any contract will ever ask for.
     */
    private static final int MAXIMUM_TEAMS_PER_SECTOR_SEARCH_LIMIT = 1000;

    /**
     * The most of a sector's dry land that may be given over to facilities, leaving the rest for scenarios to spawn on
     * over the life of the contract. Ordinary contracts sit well under this; it bites when a large contract's
     * facilities - which scale with its combat teams - are concentrated into few sectors whose area is capped.
     */
    private static final double MAXIMUM_FACILITY_COVERAGE = 0.5;

    /** Improved sizing: bounds on either dimension, so no shape profile can produce a sliver or a runaway map. */
    public static final int MIN_SECTOR_DIMENSION = 4;
    private static final int MAX_SECTOR_DIMENSION = 48;

    /** Terrain given to a newly-exposed hex that has no mapped neighbor to take after (an otherwise blank sector). */
    private static final String DEFAULT_FILL_TERRAIN = "Plains";

    /**
     * Initializes the campaign state given a contract, campaign and contract definition
     */
    public static void initializeCampaignState(AbstractContract contract, Campaign campaign,
          StratConContractDefinition contractDefinition) {
        StratConCampaignState campaignState = new StratConCampaignState(contract);
        campaignState.setBriefingText(contractDefinition.getBriefing() +
                                            "<br/>" +
                                            contract.getCommandRights().getStratConText());
        campaignState.setAllowEarlyVictory(contractDefinition.isAllowEarlyVictory());

        // dependency: this is required here in order for scenario initialization to
        // work properly
        contract.setStratConCampaignState(campaignState);

        // First, initialize the proper number of tracks. Then: for each objective:
        // step 1: calculate objective count if scaled, multiply # required lances by factor, round up, otherwise just
        // fixed number
        // step 2: evenly distribute objectives through tracks if uneven number is remaining, distribute randomly
        // when objective is specific scenario victory, place specially flagged scenarios when objective is
        // allied/hostile facility, place those facilities

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseMaplessMode = campaignOptions.isUseStratConMaplessMode();

        // Resolve the contract's destination planet once (not the campaign's current location); its data drives every
        // sector's size and temperature.
        PlanetProfile planetProfile = PlanetProfile.from(contract, campaign);

        // Decide how many sectors to generate and how large each one is. The planner always returns at least one
        // sector, so no separate zero-sector fallback is needed.
        List<SectorSpec> sectorSpecs = StratConSectorPlanner.generateSectorSpecs(contract.getScale(),
              campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD),
              maximumTeamsPerSector(planetProfile,
                    campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_SIZE_MULTIPLIER)));

        // Ares Conventions: when both the employer and the enemy are signatories, urban targeting is off-limits.
        int year = campaign.getLocalDate().getYear();
        boolean allowCities = !(contract.getEmployerFaction().isAresConventionsSignatory(year) &&
                                      contract.getEnemyFaction().isAresConventionsSignatory(year));

        // Sectors are named after the Greek alphabet in order (Sector Alpha, Sector Beta, ...). When there are more
        // sectors than letters the letters wrap around, so first tally how many sectors will share each letter; any
        // letter used more than once is disambiguated with a running suffix (Sector Alpha-1, Sector Alpha-2, ...).
        Alphabet[] greekLetters = Alphabet.values();
        int[] letterTotals = new int[greekLetters.length];
        for (int index = 0; index < sectorSpecs.size(); index++) {
            letterTotals[index % greekLetters.length]++;
        }
        int[] letterSeen = new int[greekLetters.length];

        for (int index = 0; index < sectorSpecs.size(); index++) {
            int scenarioOdds = getScenarioOdds(contractDefinition);
            int deploymentTime = isUseMaplessMode ? 0 : getDeploymentTime(contractDefinition);

            StratConTrackState track = initializeTrackState(sectorSpecs.get(index),
                  planetProfile,
                  campaignOptions,
                  allowCities,
                  scenarioOdds,
                  deploymentTime);

            int letterIndex = index % greekLetters.length;
            String greek = greekLetters[letterIndex].getGreek();
            if (letterTotals[letterIndex] > 1) {
                track.setDisplayableName(String.format("Sector %s-%d", greek, ++letterSeen[letterIndex]));
            } else {
                track.setDisplayableName(String.format("Sector %s", greek));
            }
            campaignState.addTrack(track);
        }

        // now seed the tracks with objectives and facilities
        if (!isUseMaplessMode) {
            for (ObjectiveParameters objectiveParams : contractDefinition.getObjectiveParameters()) {
                int objectiveCount = objectiveParams.objectiveCount > 0 ?
                                           (int) objectiveParams.objectiveCount :
                                           (int) max(1,
                                                 -objectiveParams.objectiveCount * contract.getScale());

                List<Integer> trackObjects = trackObjectDistribution(objectiveCount, campaignState.getTrackCount());

                for (int x = 0; x < trackObjects.size(); x++) {
                    int numObjects = trackObjects.get(x);

                    switch (objectiveParams.objectiveType) {
                        case SpecificScenarioVictory:
                            initializeObjectiveScenarios(campaign,
                                  contract,
                                  campaignState.getTrack(x),
                                  numObjects,
                                  objectiveParams.objectiveScenarios,
                                  objectiveParams.objectiveScenarioModifiers);
                            break;
                        case AlliedFacilityControl:
                            initializeTrackFacilities(campaignState.getTrack(x),
                                  numObjects,
                                  ForceAlignment.Allied,
                                  true,
                                  objectiveParams.objectiveScenarioModifiers);
                            break;
                        case HostileFacilityControl:
                        case FacilityDestruction:
                            initializeTrackFacilities(campaignState.getTrack(x),
                                  numObjects,
                                  ForceAlignment.Opposing,
                                  true,
                                  objectiveParams.objectiveScenarioModifiers);
                            break;
                        case AnyScenarioVictory:
                            // set up a "win X scenarios" objective
                            StratConStrategicObjective sso = new StratConStrategicObjective();
                            sso.setDesiredObjectiveCount(numObjects);
                            sso.setObjectiveType(StrategicObjectiveType.AnyScenarioVictory);
                            campaignState.getTrack(x).addStrategicObjective(sso);

                            // modifiers defined for "any scenario" by definition apply to any scenario
                            // so they get added to the global campaign modifiers. Use sparingly since
                            // this can snowball pretty quickly.
                            if (objectiveParams.objectiveScenarioModifiers != null) {
                                for (String modifier : objectiveParams.objectiveScenarioModifiers) {
                                    if (!campaignState.getGlobalScenarioModifiers().contains(modifier)) {
                                        campaignState.getGlobalScenarioModifiers().add(modifier);
                                    }
                                }
                            }

                            break;
                    }
                }
            }
        }

        // if any modifiers are to be applied across all scenarios in the campaign
        // do so here; do not add duplicates
        if (contractDefinition.getGlobalScenarioModifiers() != null) {
            for (String modifier : contractDefinition.getGlobalScenarioModifiers()) {
                if (!campaignState.getGlobalScenarioModifiers().contains(modifier)) {
                    campaignState.getGlobalScenarioModifiers().add(modifier);
                }
            }
        }

        // non-objective allied facilities
        if (!isUseMaplessMode) {
            int facilityCount = contractDefinition.getAlliedFacilityCount() > 0 ?
                                      (int) contractDefinition.getAlliedFacilityCount() :
                                      (int) (-contractDefinition.getAlliedFacilityCount() *
                                                   contract.getScale());

            List<Integer> trackObjects = trackObjectDistribution(facilityCount, campaignState.getTrackCount());

            for (int x = 0; x < trackObjects.size(); x++) {
                int numObjects = trackObjects.get(x);

                initializeTrackFacilities(campaignState.getTrack(x),
                      numObjects,
                      ForceAlignment.Allied,
                      false,
                      Collections.emptyList());
            }

            // non-objective hostile facilities
            facilityCount = contractDefinition.getHostileFacilityCount() > 0 ?
                                  (int) contractDefinition.getHostileFacilityCount() :
                                  (int) (-contractDefinition.getHostileFacilityCount() *
                                               contract.getScale());

            trackObjects = trackObjectDistribution(facilityCount, campaignState.getTrackCount());

            for (int x = 0; x < trackObjects.size(); x++) {
                int numObjects = trackObjects.get(x);

                initializeTrackFacilities(campaignState.getTrack(x),
                      numObjects,
                      ForceAlignment.Opposing,
                      false,
                      Collections.emptyList());
            }
        }

        // Now that facilities exist, fold the planet-owner's facilities into each sector's road network via the GM's
        // sector-generation strategy (a road-less generator ignores this). Facilities are only placed outside mapless.
        if (!isUseMaplessMode) {
            for (StratConTrackState track : campaignState.getTracks()) {
                connectFacilitiesToRoads(track, contract, campaign);
            }
        }

        // Determine starting Support Points
        negotiateInitialSupportPoints(campaign, contract);
    }

    /**
     * Retrieves a random deployment time from the provided {@link StratConContractDefinition}.
     *
     * <p>The deployment time is selected randomly from the list of deployment times in the
     * given {@code StratConContractDefinition}.</p>
     *
     * @param contractDefinition the contract definition containing deployment time options
     *
     * @return a randomly selected deployment time
     *
     * @throws IllegalArgumentException if the list of deployment times is empty
     * @throws NullPointerException     if {@code contractDefinition} or its deployment times list is null
     * @author Illiani
     * @since 0.50.05
     */
    private static int getDeploymentTime(StratConContractDefinition contractDefinition) {
        return contractDefinition.getDeploymentTimes()
                     .get(Compute.randomInt(contractDefinition.getDeploymentTimes().size()));
    }

    /**
     * Retrieves a random scenario odds value from the provided {@link StratConContractDefinition}.
     *
     * <p>The scenario odds are selected randomly from the list of scenario odds in the
     * given {@code StratConContractDefinition}.</p>
     *
     * @param contractDefinition the contract definition containing scenario odds options
     *
     * @return a randomly selected scenario odds value
     *
     * @throws IllegalArgumentException if the list of scenario odds is empty
     * @throws NullPointerException     if {@code contractDefinition} or its scenario odds list is null
     * @author Illiani
     * @since 0.50.05
     */
    public static int getScenarioOdds(StratConContractDefinition contractDefinition) {
        return contractDefinition.getScenarioOdds().get(Compute.randomInt(contractDefinition.getScenarioOdds().size()));
    }


    /**
     * Sets up the initial state of a single track from its {@link SectorSpec} and the destination planet's profile.
     *
     * <p>Sizing and temperature follow one of two regimes, chosen by
     * {@link StratConSectorCountMethod#usesImprovedSizing()}. Every method but {@code LEGACY} uses the improved sizing
     * (a per-quarter scouting budget scaled by the sector's combat teams, planetary size, the size multiplier, and
     * hydrology) and a latitude-driven temperature. {@code LEGACY} keeps the historical behaviour: a rectangle sized
     * from the formation count, and a broad equatorial temperature swing with no latitude input.</p>
     *
     * @param sector          the sector blueprint (required combat teams, latitude band)
     * @param planetProfile   the destination planet's resolved data
     * @param campaignOptions the campaign options governing which sizing/temperature regime applies
     * @param allowCities     {@code true} to allow the improved terrain generator to place cities; {@code false}
     *                        suppresses them (has no effect on the legacy terrain path, which places no cities)
     * @param scenarioOdds    the per-track scenario odds
     * @param deploymentTime  the per-track deployment time
     *
     * @return the initialized track
     */
    public static StratConTrackState initializeTrackState(SectorSpec sector, PlanetProfile planetProfile,
          CampaignOptions campaignOptions, boolean allowCities, int scenarioOdds, int deploymentTime) {
        StratConTrackState retVal = new StratConTrackState();
        retVal.setRequiredLanceCount(sector.requiredLances());

        boolean useImprovedSizing = campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD)
                                          .usesImprovedSizing();

        if (useImprovedSizing) {
            applyImprovedDimensions(retVal,
                  sector,
                  planetProfile,
                  campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_SIZE_MULTIPLIER));
            retVal.setTemperature(improvedTemperature(planetProfile, sector.latitudeBand()));
        } else {
            applyLegacyDimensions(retVal, sector.requiredLances());
            retVal.setTemperature(legacyTemperature(planetProfile.temperatureCelsius()));
        }

        retVal.setScenarioOdds(scenarioOdds);
        retVal.setDeploymentTime(deploymentTime);

        // Place terrain via the GM's sector-generation strategy (improved geography-aware pipeline or legacy placer).
        StratConGMs.sectorGeneration(campaignOptions)
              .initializeTrack(retVal, planetProfile, sector.latitudeBand(), allowCities);

        return retVal;
    }

    /**
     * Regenerates a single track's terrain in place, as used by the GM "Regenerate Sector" tool. Clears the existing
     * terrain, cities, and fog, then re-runs terrain generation - the improved geography-aware generator when the
     * alternate-terrain option is set, otherwise the legacy placer. The track's dimensions are kept, but a fresh
     * latitude band is rolled and the temperature is recomputed from it, and assigned forces are left untouched.
     * Scenarios and facilities are preserved, but any that the new coastline leaves on an ocean hex are relocated back
     * onto land.
     *
     * @param track    the track to regenerate
     * @param contract the contract the track belongs to (source of the planet profile and Ares-Conventions status)
     * @param campaign the campaign (source of options and the current date)
     */
    public static void regenerateTrack(StratConTrackState track, AbstractContract contract, Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        PlanetProfile planetProfile = PlanetProfile.from(contract, campaign);

        int year = campaign.getLocalDate().getYear();
        boolean allowCities = !(contract.getEmployerFaction().isAresConventionsSignatory(year) &&
                                      contract.getEnemyFaction().isAresConventionsSignatory(year));

        // Re-roll the latitude band and recompute the temperature from it (matching initializeTrackState), so a
        // regenerated sector's climate actually changes and drives the new biome selection - not just its terrain.
        boolean useImprovedSizing = campaignOptions.get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD)
                                          .usesImprovedSizing();
        LatitudeBand latitudeBand = LatitudeBand.random();
        if (useImprovedSizing) {
            track.setTemperature(improvedTemperature(planetProfile, latitudeBand));
        } else {
            track.setTemperature(legacyTemperature(planetProfile.temperatureCelsius()));
        }

        // Re-roll the sector's proportions too, so regenerating does not keep handing back the same shape of map.
        if (useImprovedSizing) {
            reshapeForRegeneration(track);
        }

        var sectorStrategy = StratConGMs.sectorGeneration(campaignOptions);
        sectorStrategy.regenerateTrack(track, planetProfile, latitudeBand, allowCities);

        // A regenerated coastline can leave existing facilities and scenarios sitting on new ocean hexes; move them
        // back onto land.
        relocateOccupantsOffOcean(track);

        // Fold the planet-owner's facilities into the (possibly rebuilt) road network; a road-less generator ignores it.
        connectFacilitiesToRoads(track, contract, campaign);
    }

    /**
     * What a proposed resize would disturb, so a GM can be told before anything is moved.
     *
     * @param facilities how many facilities would be displaced back inside the sector
     * @param scenarios  how many scenarios would be displaced back inside the sector
     * @param objectives how many strategic objectives sit on the ground being cut away
     * @param forces     how many deployed forces would be recalled
     * @param freeHexes  how many hexes inside the new bounds are free to receive a displaced occupant
     */
    public record ResizeImpact(int facilities, int scenarios, int objectives, int forces, int freeHexes) {
        public boolean isEmpty() {
            return (facilities == 0) && (scenarios == 0) && (objectives == 0) && (forces == 0);
        }

        /** @return how many facilities and scenarios would have to be found a new hex inside the sector. */
        public int displacedOccupants() {
            return facilities + scenarios;
        }

        /**
         * @return {@code true} if the sector would still have somewhere to put everything that must move. A resize that
         *       does not fit is refused rather than performed, because the alternative is destroying bases and
         *       scenarios - and any strategic objective riding on them - to make the numbers work.
         */
        public boolean fits() {
            return displacedOccupants() <= freeHexes;
        }
    }

    /**
     * Reports what {@link #resizeTrack} would disturb at the given size, without changing anything.
     *
     * @param track     the track to be resized
     * @param newWidth  the proposed width
     * @param newHeight the proposed height
     *
     * @return a tally of the occupants that would have to be moved or recalled
     */
    public static ResizeImpact previewResize(StratConTrackState track, int newWidth, int newHeight) {
        int facilities = 0;
        int scenarios = 0;
        int objectives = 0;
        int forces = 0;

        for (StratConCoords coords : track.getFacilities().keySet()) {
            if (isOutside(coords, newWidth, newHeight)) {
                facilities++;
            }
        }
        for (StratConCoords coords : track.getScenarios().keySet()) {
            if (isOutside(coords, newWidth, newHeight)) {
                scenarios++;
            }
        }
        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            if ((objective.getObjectiveCoords() != null) &&
                      isOutside(objective.getObjectiveCoords(), newWidth, newHeight)) {
                objectives++;
            }
        }
        for (StratConCoords coords : track.getAssignedForceCoords().values()) {
            if (isOutside(coords, newWidth, newHeight)) {
                forces++;
            }
        }

        return new ResizeImpact(facilities, scenarios, objectives, forces, freeHexes(track, newWidth, newHeight));
    }

    /**
     * Counts the hexes that would still be able to take a relocated occupant at the proposed size, mirroring what
     * {@link #getUnoccupiedCoords(StratConTrackState)} considers eligible: dry land, holding no scenario, no facility,
     * and no deployed force. Anything already inside the new bounds keeps its hex, so it is counted as taken.
     */
    private static int freeHexes(StratConTrackState track, int newWidth, int newHeight) {
        Collection<StratConCoords> forceCoords = track.getAssignedForceCoords().values();
        int free = 0;

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                StratConCoords coords = new StratConCoords(x, y);
                boolean available = !StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords)) &&
                                          (track.getScenario(coords) == null) &&
                                          (track.getFacility(coords) == null) &&
                                          !forceCoords.contains(coords);
                if (available) {
                    free++;
                }
            }
        }

        return free;
    }

    /**
     * Resizes a sector, growing or shrinking it at its right and bottom edges.
     *
     * <p>Only those two edges are offered on purpose. StratCon hexes sit on a parity-offset grid - a hex's neighbors
     * depend on whether its x is odd or even (see {@link StratConCoords#translate}) - so shifting every hex to make
     * room at the left or top would silently rewire the whole map's adjacency and tear apart coastlines, ranges, and
     * roads. Growing at the far edges leaves every existing hex on its original coordinates.</p>
     *
     * <p>Ground outside the new bounds is discarded, but its occupants are not: facilities and scenarios are moved
     * back inside (with their strategic objectives), and any force left standing outside is recalled. Call
     * {@link #previewResize} first so the GM knows what is about to move.</p>
     *
     * @param track     the track to resize
     * @param newWidth  the new width, at least 1
     * @param newHeight the new height, at least 1
     * @param contract  the contract, for the planet-owner road rules
     * @param campaign  the campaign, for the current date and options
     */
    public static boolean resizeTrack(StratConTrackState track, int newWidth, int newHeight, AbstractContract contract,
          Campaign campaign) {
        int width = max(1, newWidth);
        int height = max(1, newHeight);

        // Refuse a size with nowhere to put everything that would be displaced, BEFORE touching the track. Squeezing
        // them out would mean destroying bases and scenarios - and any strategic objective riding on them, which would
        // quietly make the contract unwinnable.
        if (!previewResize(track, width, height).fits()) {
            return false;
        }

        applyNewBounds(track, width, height);

        fillNewHexes(track);

        applyTerrainChange(track, contract, campaign);
        return true;
    }

    /**
     * Moves a sector's bounds and puts its occupants back inside: ground outside the new bounds is discarded, but
     * facilities and scenarios are relocated (carrying their strategic objectives and any forces deployed to them), and
     * a force left standing off the map is recalled.
     *
     * <p>Callers must check {@link #previewResize} first - this assumes the sector can hold what it is about to
     * displace.</p>
     */
    private static void applyNewBounds(StratConTrackState track, int width, int height) {
        // Note who is about to be left outside before the bounds move, so they can be re-homed afterward.
        List<StratConCoords> displacedFacilities = outsideCoords(track.getFacilities().keySet(), width, height);
        List<StratConCoords> displacedScenarios = outsideCoords(track.getScenarios().keySet(), width, height);

        track.setWidth(width);
        track.setHeight(height);
        track.trimToBounds();

        for (StratConCoords source : occupiedCoords(displacedFacilities, displacedScenarios)) {
            StratConCoords destination = getUnoccupiedCoords(track);
            if (destination == null) {
                // The capacity check above should have prevented this; drop the occupant rather than strand it outside
                // the sector, where it would be invisible and unreachable but still counted.
                LOGGER.warn("No room to relocate occupant at {} on track {}; removing it.",
                      source,
                      track.getDisplayableName());
                dropOccupant(track, source);
                continue;
            }

            relocateOccupant(track, source, destination);
        }

        // Forces left standing on ground that no longer exists are recalled.
        recallForcesOutsideBounds(track);
    }

    /**
     * Collects the distinct hexes holding a displaced facility or scenario, facility hexes first, so that a hex shared
     * by both is visited once.
     *
     * <p>A facility scenario is created on its facility's coordinates, and later completion, capture, or destruction
     * resolves the facility by looking it up from the scenario's coordinates. Relocating the facility and the scenario
     * separately - each to its own free hex - splits that pair, and the battle can no longer act on its facility. So
     * the two must move together, as one occupant of a single hex.</p>
     */
    private static Collection<StratConCoords> occupiedCoords(Collection<StratConCoords> facilityCoords,
          Collection<StratConCoords> scenarioCoords) {
        Set<StratConCoords> coords = new LinkedHashSet<>(facilityCoords);
        coords.addAll(scenarioCoords);
        return coords;
    }

    /**
     * Moves everything sitting on {@code source} - a facility, a scenario, or the facility scenario that is both - to
     * {@code destination} as one occupant, carrying its strategic objective and any forces deployed to it. This keeps a
     * facility and its co-located scenario on the same hex, which their capture and destruction rules depend on.
     */
    private static void relocateOccupant(StratConTrackState track, StratConCoords source, StratConCoords destination) {
        StratConFacility facility = track.getFacility(source);
        if (facility != null) {
            track.removeFacility(source);
            track.addFacility(destination, facility);
        }

        StratConScenario scenario = track.getScenario(source);
        if (scenario != null) {
            track.getScenarios().remove(source);
            scenario.setCoords(destination);
            track.getScenarios().put(destination, scenario);
        }

        track.moveObjective(source, destination);
        moveAssignedForces(track, source, destination);
    }

    /**
     * Removes whatever occupies {@code source} - facility, scenario, and its strategic objective - when no free hex can
     * receive it.
     */
    private static void dropOccupant(StratConTrackState track, StratConCoords source) {
        track.removeFacility(source);
        track.getScenarios().remove(source);
        removeObjectiveAt(track, source);
    }

    /** Drops any strategic objective tied to a hex whose occupant could not be saved, so nothing points at dead ground. */
    private static void removeObjectiveAt(StratConTrackState track, StratConCoords coords) {
        track.getStrategicObjectives().removeIf(objective -> coords.equals(objective.getObjectiveCoords()));
    }

    /** Moves any forces deployed to {@code source} along with the occupant that just moved to {@code destination}. */
    private static void moveAssignedForces(StratConTrackState track, StratConCoords source,
          StratConCoords destination) {
        Set<Integer> forces = track.getAssignedCoordForces().remove(source);
        if ((forces == null) || forces.isEmpty()) {
            return;
        }

        track.getAssignedCoordForces().computeIfAbsent(destination, key -> new HashSet<>()).addAll(forces);
        for (int forceID : forces) {
            track.getAssignedForceCoords().put(forceID, destination);
        }
    }

    /** Recalls every force still standing outside the sector, so no formation is stranded off the map. */
    private static void recallForcesOutsideBounds(StratConTrackState track) {
        List<Integer> stranded = new ArrayList<>();
        for (Map.Entry<Integer, StratConCoords> entry : track.getAssignedForceCoords().entrySet()) {
            if (track.isOutOfBounds(entry.getValue())) {
                stranded.add(entry.getKey());
            }
        }

        for (int forceID : stranded) {
            track.unassignFormation(forceID);
        }
        track.getAssignedCoordForces().keySet().removeIf(track::isOutOfBounds);
    }

    /**
     * Gives newly-exposed hexes terrain by extending their nearest already-mapped neighbor, so a grown sector reads as
     * more of the same country rather than a blank margin.
     */
    private static void fillNewHexes(StratConTrackState track) {
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (!track.getTerrainTile(coords).isEmpty()) {
                    continue;
                }

                // Walking left-to-right and top-to-bottom means the neighbor we copy has already been filled itself,
                // so terrain propagates outward from the old sector instead of leaving holes.
                String source = (x > 0) ?
                                      track.getTerrainTile(new StratConCoords(x - 1, y)) :
                                      track.getTerrainTile(new StratConCoords(x, max(0, y - 1)));
                track.setTerrainTile(coords, source.isEmpty() ? DEFAULT_FILL_TERRAIN : source);
            }
        }
    }

    private static boolean isOutside(StratConCoords coords, int width, int height) {
        return (coords.getX() >= width) || (coords.getY() >= height);
    }

    private static List<StratConCoords> outsideCoords(Collection<StratConCoords> coords, int width, int height) {
        List<StratConCoords> outside = new ArrayList<>();
        for (StratConCoords candidate : coords) {
            if (isOutside(candidate, width, height)) {
                outside.add(candidate);
            }
        }
        return outside;
    }

    /**
     * Re-settles a track after its terrain has been edited by a GM, so the sector stays internally consistent: cities
     * and occupants cannot be left sitting on new water, open water carries no fog, and the road network has to be
     * re-laid because ocean and relief are what drive its path costs.
     *
     * <p>Call this once when an edit is finished rather than per hex - it rebuilds the whole road network.</p>
     *
     * @param track    the track whose terrain just changed
     * @param contract the contract, for the planet-owner road rules
     * @param campaign the campaign, for the current date and options
     */
    public static void applyTerrainChange(StratConTrackState track, AbstractContract contract, Campaign campaign) {
        // A city that has just been flooded is no longer a city.
        track.getCities().removeIf(coords -> StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords)));

        StratConSectorGenerator.revealOceanHexes(track);
        relocateOccupantsOffOcean(track);
        connectFacilitiesToRoads(track, contract, campaign);
    }

    /**
     * Folds the planet-owner's facilities on the given track into its road network, via the GM's sector-generation
     * strategy (a road-less generator ignores this). Call whenever a GM edits what sits on the map - adding or removing
     * a city, adding or removing a facility - so the network matches the sites that are actually there. Sector
     * generation and regeneration lay the roads the same way.
     *
     * <p>Deliberately <em>not</em> called for anything that happens in play: a facility changing hands, or being
     * destroyed, during scenario resolution. The line is between editing the map and playing on it. A road is built
     * ground, so who holds the base at the end of it does not decide whether the road was ever laid - and because this
     * rebuilds the entire network from scratch, calling it mid-campaign could redraw roads far from the facility that
     * changed.</p>
     *
     * @param track    the track whose road network to refresh
     * @param contract the contract (source of the planet and the employer/enemy factions)
     * @param campaign the campaign, for the current date and options
     */
    public static void connectFacilitiesToRoads(StratConTrackState track, AbstractContract contract,
          Campaign campaign) {
        StratConGMs.sectorGeneration(campaign.getCampaignOptions())
              .connectFacilitiesToRoads(track,
                    planetOwnedFacilityCoords(track, contract, campaign.getLocalDate()));
    }

    /**
     * Returns the hexes of facilities whose owning side controls the contract's planet. Allied (and player) facilities
     * count when the employer holds the planet; opposing facilities count when the enemy holds it. On a contested world
     * both sides can qualify; on a world held by neither contract party, none do.
     *
     * @param track    the track whose facilities to examine
     * @param contract the contract (source of the planet and the employer/enemy factions)
     * @param date     the date at which to evaluate planetary ownership
     *
     * @return the coordinates of the qualifying facilities (possibly empty)
     */
    private static Set<StratConCoords> planetOwnedFacilityCoords(StratConTrackState track, AbstractContract contract,
          LocalDate date) {
        Set<StratConCoords> result = new HashSet<>();

        Planet planet = contract.getTargetPlanet();
        if (planet == null) {
            return result;
        }

        Set<Faction> owners = planet.getFactionSet(date);
        boolean employerOwns = owners.contains(contract.getEmployerFaction());
        boolean enemyOwns = owners.contains(contract.getEnemyFaction());

        if (!employerOwns && !enemyOwns) {
            return result;
        }

        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            ForceAlignment owner = entry.getValue().getOwner();
            boolean planetOwned = (enemyOwns && (owner == ForceAlignment.Opposing)) ||
                                        (employerOwns &&
                                               ((owner == ForceAlignment.Allied) || (owner == ForceAlignment.Player)));
            if (planetOwned) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    /**
     * Moves any facility or scenario that a regeneration left sitting on an ocean hex to a fresh, non-ocean unoccupied
     * hex, carrying any strategic-objective marker along with it. Occupants are left in place if the sector has no free
     * land to receive them.
     *
     * @param track the freshly regenerated track to clean up
     */
    private static void relocateOccupantsOffOcean(StratConTrackState track) {
        List<StratConCoords> floodedFacilities = floodedCoords(track, track.getFacilities().keySet());
        List<StratConCoords> floodedScenarios = floodedCoords(track, track.getScenarios().keySet());

        for (StratConCoords source : occupiedCoords(floodedFacilities, floodedScenarios)) {
            StratConCoords destination = getUnoccupiedCoords(track);
            if (destination == null) {
                // No dry land left to receive it; leave the occupant where it is rather than destroy it.
                break;
            }

            relocateOccupant(track, source, destination);
        }

        // A force standing on ground that just flooded, with no facility or scenario to carry it ashore, is recalled.
        recallForcesOnOcean(track);
    }

    /** @return those of the given occupant hexes that a terrain change has left sitting on ocean. */
    private static List<StratConCoords> floodedCoords(StratConTrackState track, Collection<StratConCoords> coords) {
        List<StratConCoords> flooded = new ArrayList<>();
        for (StratConCoords candidate : coords) {
            if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(candidate))) {
                flooded.add(candidate);
            }
        }
        return flooded;
    }

    /**
     * Recalls every force left standing on water, so no formation is stranded at sea after a hex floods.
     *
     * <p>The ocean counterpart to {@link #recallForcesOutsideBounds}: a regeneration or a GM painting {@code Sea} over
     * an occupied hex can put a deployed force in water just as a shrink can put one outside the map.</p>
     */
    private static void recallForcesOnOcean(StratConTrackState track) {
        List<Integer> stranded = new ArrayList<>();
        for (Map.Entry<Integer, StratConCoords> entry : track.getAssignedForceCoords().entrySet()) {
            if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(entry.getValue()))) {
                stranded.add(entry.getKey());
            }
        }

        for (int forceID : stranded) {
            track.unassignFormation(forceID);
        }
        track.getAssignedCoordForces()
              .keySet()
              .removeIf(coords -> StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords)));
    }

    /**
     * Applies the legacy track dimensions: a total of {@code formations * 28} hexes laid out as a rectangle that is
     * wider than it is tall, so a scout formation deployed to a fresh spot each week can more or less cover it.
     *
     * <p>The area is capped at {@link #MAX_SECTOR_HEXES}, matching the improved path. Nothing reaches that today,
     * because the legacy count hands every sector at most three formations and so at most 84 hexes - the cap is here so
     * that a future count method built on legacy sizing cannot produce an unbounded map. No trim loop is needed after
     * it: {@code width} is the floored quotient of the capped total, so the laid-out area cannot exceed it.</p>
     */
    private static void applyLegacyDimensions(StratConTrackState track, int numFormations) {
        int numHexes = min(MAX_SECTOR_HEXES, numFormations * LEGACY_HEXES_PER_FORMATION);
        int height = max(1, (int) Math.floor(Math.sqrt(numHexes)));
        int width = numHexes / height;
        track.setWidth(width);
        track.setHeight(height);
    }

    /**
     * Applies the improved track dimensions. Starts from a quarter's scouting budget for the sector's combat teams,
     * scaled by planetary size and the configured size multiplier to get the dry playable target, then grows the sector
     * to offset ocean so the dry target survives. The area is laid out by {@link #rollSectorShape}, which rolls a
     * weighted aspect ratio - sectors are deliberately not square.
     */
    private static void applyImprovedDimensions(StratConTrackState track, SectorSpec sector, PlanetProfile profile,
          double sizeMultiplier) {
        // The ceiling is a backstop rather than a working limit: the planner has already split the contract into enough
        // sectors that none of them should ask for more than this. It stays because rounding and the shape roll can
        // land a hex or two over.
        int totalHexes = min(MAX_SECTOR_HEXES, requestedHexes(sector.requiredLances(), profile, sizeMultiplier));

        SectorDimensions shape = rollSectorShape(totalHexes);
        track.setWidth(shape.width());
        track.setHeight(shape.height());
    }

    /**
     * @return the total hexes a sector fronting the given combat teams asks for on this planet, before the area ceiling
     *       is applied.
     *
     *       <p>Size the sector to what its own recon can cover in a quarter: a third of the combat teams fronting it
     *       are assumed to be recon, and each covers {@link #RECON_HEXES_PER_QUARTER} hexes of dry ground in three
     *       months. The recon count is deliberately NOT rounded - a sector fronting four teams gets 1.33 recon teams'
     *       worth of ground, not one team's. Rounding down undersized every sector whose team count was not a multiple
     *       of three. It is floored at one, because even the smallest sector is scouted by someone. The dry total is
     *       then grown by the planet's land fraction, since ocean is not playable.</p>
     */
    static int requestedHexes(int combatTeams, PlanetProfile profile, double sizeMultiplier) {
        double reconTeams = max(1.0, combatTeams / (double) COMBAT_TEAMS_PER_RECON_FORCE);
        double landHexes = reconTeams * RECON_HEXES_PER_QUARTER * profile.sizeFactor() * sizeMultiplier;
        int playableHexes = max(1, (int) Math.round(landHexes));

        double landFraction = max(MINIMUM_LAND_FRACTION, 1.0 - (profile.waterPercent() / 100.0));

        return (int) Math.round(playableHexes / landFraction);
    }

    /**
     * @return the most combat teams a single sector on this planet can front without asking for more ground than
     *       {@link #MAX_SECTOR_HEXES} will grant.
     *
     *       <p>Used by the planner to split a contract into enough sectors that none of them is clipped by the
     *       ceiling. The threshold is planetary, not fixed: a dry, small world fits far more teams into one sector than
     *       a large ocean world does, so this cannot be a constant. Searched rather than solved because the sizing rule
     *       rounds twice and floors the recon share, which an inverted formula would have to reproduce exactly to stay
     *       in step with it.</p>
     */
    public static int maximumTeamsPerSector(PlanetProfile profile, double sizeMultiplier) {
        int teams = 1;
        while ((teams < MAXIMUM_TEAMS_PER_SECTOR_SEARCH_LIMIT) &&
                     (requestedHexes(teams + 1, profile, sizeMultiplier) <= MAX_SECTOR_HEXES)) {
            teams++;
        }

        return teams;
    }

    /** A sector's laid-out proportions. */
    private record SectorDimensions(int width, int height) {}

    /**
     * Lays a sector's area out into width and height, rolling a fresh {@link SectorShapeProfile#SectorShapeProfile} for
     * the proportions.
     *
     * @param totalHexes the area to lay out
     *
     * @return the resulting dimensions, both inside the playable bounds and together inside the area ceiling
     */
    private static SectorDimensions rollSectorShape(int totalHexes) {
        // Both dimensions are derived from the ratio rather than one from the other, so a 1.0 ratio really is square
        // instead of drifting a hex wide.
        double aspectRatio = StratConSectorShape.getInstance().selectProfile().aspectRatioOrDefault();
        int width = clampDimension((int) Math.round(Math.sqrt(totalHexes * aspectRatio)));
        int height = clampDimension((int) Math.round(Math.sqrt(totalHexes / aspectRatio)));

        // Rounding each dimension independently can nudge the laid-out area just past the ceiling, so trim the longer
        // side until it fits. The ceiling is meant to be a hard bound, not an approximate one.
        while (((width * height) > MAX_SECTOR_HEXES) && (max(width, height) > MIN_SECTOR_DIMENSION)) {
            if (width >= height) {
                width--;
            } else {
                height--;
            }
        }

        return new SectorDimensions(width, height);
    }

    /**
     * Re-rolls a sector's proportions when it is regenerated, so a regenerated sector is not stuck with the shape it
     * happened to be given when the contract was signed. Its <em>area</em> is preserved - regeneration re-rolls the
     * terrain and climate, not how much sector there is to fight over.
     *
     * <p>If the new proportions would leave more facilities and scenarios outside the sector than it can re-home, the
     * current shape is kept. A cosmetic re-roll is not worth destroying anything over.</p>
     */
    private static void reshapeForRegeneration(StratConTrackState track) {
        SectorDimensions shape = rollSectorShape(track.getWidth() * track.getHeight());
        if ((shape.width() == track.getWidth()) && (shape.height() == track.getHeight())) {
            return;
        }

        if (!previewResize(track, shape.width(), shape.height()).fits()) {
            return;
        }

        applyNewBounds(track, shape.width(), shape.height());
    }

    /** Keeps a sector dimension inside the playable range, so no shape can produce a sliver or a runaway map. */
    private static int clampDimension(int dimension) {
        return Math.clamp(dimension, MIN_SECTOR_DIMENSION, MAX_SECTOR_DIMENSION);
    }

    /**
     * Improved temperature: the planet's equatorial temperature, shifted colder by the sector's latitude band, plus a
     * small local variation of -5 to +5 degrees.
     */
    private static int improvedTemperature(PlanetProfile profile, LatitudeBand latitudeBand) {
        int localVariation = Compute.randomInt(11) - 5;
        return profile.temperatureCelsius() + latitudeBand.getTemperatureOffset() + localVariation;
    }

    /**
     * Legacy temperature: the equatorial temperature with a random -40 to +10 degree swing, on the notion that the
     * equator is about as hot as it gets, with some exceptions.
     */
    private static int legacyTemperature(int equatorialTemperature) {
        int tempVariation = Compute.randomInt(51) - 40;
        return equatorialTemperature + tempVariation;
    }

    /**
     * Generates an array list representing the number of objects to place in a given number of tracks.
     */
    private static List<Integer> trackObjectDistribution(int numObjects, int numTracks) {
        // This ensures we're not at risk of dividing by 0
        numTracks = max(1, numTracks);

        List<Integer> retVal = new ArrayList<>();
        int leftOver = numObjects % numTracks;

        for (int track = 0; track < numTracks; track++) {
            int trackObjects = numObjects / numTracks;

            // if we are unevenly distributed, add an extra one
            if (leftOver > 0) {
                trackObjects++;
                leftOver--;
            }

            retVal.add(trackObjects);
        }

        // don't always front-load extra objects
        Collections.shuffle(retVal);
        return retVal;
    }

    /**
     * Worker function that takes a track state and plops down the given number of facilities owned by the given faction
     * Avoids places with existing facilities and scenarios, capable of taking facility sub set and setting strategic
     * objective flag.
     */
    // Package-private rather than private so the capacity rules can be tested directly; reaching them through contract
    // initialization would mean standing up a whole contract to assert on a placement loop.
    static void initializeTrackFacilities(StratConTrackState trackState, int numFacilities, ForceAlignment owner,
          boolean strategicObjective, List<String> modifiers) {

        int capacity = facilityCapacity(trackState);
        int placed = 0;

        for (int fCount = 0; fCount < numFacilities; fCount++) {
            // Stop deliberately at capacity rather than running on until placement happens to fail.
            if ((trackState.getFacilities().size() + trackState.getScenarios().size()) >= capacity) {
                break;
            }

            StratConFacility sf = owner == ForceAlignment.Allied ?
                                        StratConFacilityFactory.getRandomAlliedFacility() :
                                        StratConFacilityFactory.getRandomHostileFacility();

            sf.setOwner(owner);
            sf.setStrategicObjective(strategicObjective);
            sf.getLocalModifiers().addAll(modifiers);

            StratConCoords coords = getUnoccupiedCoords(trackState);

            if (coords == null) {
                break;
            }

            placed++;
            trackState.addFacility(coords, sf);

            if (strategicObjective) {
                StratConStrategicObjective sso = new StratConStrategicObjective();
                sso.setObjectiveCoords(coords);

                if (sf.getOwner() == ForceAlignment.Allied) {
                    trackState.getRevealedCoords().add(coords);
                    sf.setVisible(true);
                    sso.setObjectiveType(StrategicObjectiveType.AlliedFacilityControl);
                } else {
                    sf.setVisible(false);
                    sso.setObjectiveType(StrategicObjectiveType.HostileFacilityControl);
                }

                trackState.addStrategicObjective(sso);
            }
        }

        if (placed < numFacilities) {
            LOGGER.info("Sector {} had room for {} of {} {} facilities. The contract asks for facilities in " +
                              "proportion to its combat teams, but a sector's area is capped, so a large contract in " +
                              "few sectors can want more than its ground will hold.",
                  trackState.getDisplayableName(),
                  placed,
                  numFacilities,
                  owner);
        }
    }

    /**
     * @return how many facilities a sector will accept, being a fraction of the hexes that can actually hold one.
     *
     *       <p>Two things make this narrower than it looks. Only dry land counts - {@link #getUnoccupiedCoords} never
     *       returns an ocean hex - so a wet sector holds far fewer facilities than its width times its height would
     *       suggest. And only part of that land is offered, because scenarios need somewhere to spawn for the life of
     *       the contract; a sector paved with facilities has nowhere left to fight.</p>
     */
    private static int facilityCapacity(StratConTrackState trackState) {
        int placeable = 0;
        for (int x = 0; x < trackState.getWidth(); x++) {
            for (int y = 0; y < trackState.getHeight(); y++) {
                if (!StratConBiomeManifest.isOceanTerrain(trackState.getTerrainTile(new StratConCoords(x, y)))) {
                    placeable++;
                }
            }
        }

        return max(1, (int) Math.round(placeable * MAXIMUM_FACILITY_COVERAGE));
    }

    /**
     * Initializes and populates a StratCon track with a specified number of objective scenarios. This method selects
     * scenario templates, places them on the track in unoccupied coordinates, and optionally assigns facilities and
     * objectives based on predefined rules.
     *
     * <p>The key steps of this method include:
     * <ul>
     *   <li>Selecting scenario templates from the provided list of objective scenarios.</li>
     *   <li>Identifying unoccupied coordinates on the track to place each scenario.</li>
     *   <li>Adding facilities if the scenario template requires them (hostile or allied).</li>
     *   <li>Generating and configuring scenarios with relevant attributes and modifiers:</li>
     *   <ul>
     *     <li>Clearing scenario dates to maintain persistence.</li>
     *     <li>Marking scenarios as strategic objectives.</li>
     *     <li>Adding optional modifiers to provide additional effects or conditions.</li>
     *   </ul>
     *   <li>Tracking newly added scenarios as strategic objectives for gameplay purposes.</li>
     * </ul>
     *
     * @param campaign           the {@link Campaign} managing the state of the overall gameplay
     * @param contract           the {@link AbstractContract} related to the current StratCon campaign
     * @param trackState         the {@link StratConTrackState} representing the track where objectives are placed
     * @param numScenarios       the number of objective scenarios to generate
     * @param objectiveScenarios a list of {@link String} identifiers for potential scenarios that can be generated
     * @param objectiveModifiers a list of optional {@link String} modifiers to apply to the generated scenarios; can be
     *                           {@code null} if no modifiers are required
     */
    private static void initializeObjectiveScenarios(Campaign campaign, AbstractContract contract,
          StratConTrackState trackState, int numScenarios, List<String> objectiveScenarios,
          List<String> objectiveModifiers) {
        // pick scenario from subset
        // place it on the map somewhere nothing else has been placed yet
        // if it's a facility scenario, place the facility
        // run generateScenario() to apply all the necessary mods
        // apply objective mods (?)

        int trackSize = trackState.getWidth() * trackState.getHeight();

        for (int sCount = 0; sCount < numScenarios; sCount++) {
            // if there's no possible empty places to put down a new scenario, then move on
            if ((trackState.getFacilities().size() + trackState.getScenarios().size()) >= trackSize) {
                break;
            }

            // pick
            ScenarioTemplate template = StratConScenarioFactory.getSpecificScenario(objectiveScenarios.get(Compute.randomInt(
                  objectiveScenarios.size())));

            if (template == null) {
                LOGGER.error("Unable to place objective scenario on track {}, as no scenario template was available.",
                      trackState.getDisplayableName());
                continue;
            }

            StratConCoords coords = getUnoccupiedCoords(trackState);

            if (coords == null) {
                LOGGER.error("Unable to place objective scenario on track {}, as all coords were occupied. Aborting.",
                      trackState.getDisplayableName());
                return;
            }

            // facility
            boolean addedFacility = false;
            if (template.isFacilityScenario()) {
                StratConFacility facility = template.isHostileFacility() ?
                                                  StratConFacilityFactory.getRandomHostileFacility() :
                                                  StratConFacilityFactory.getRandomAlliedFacility();
                trackState.addFacility(coords, facility);
                addedFacility = true;
            }

            // create scenario - don't assign a force yet
            StratConScenario scenario = StratConRulesManager.generateScenario(campaign,
                  contract,
                  trackState,
                  Formation.FORMATION_NONE,
                  coords,
                  template,
                  null);

            if (scenario != null) {
                // clear dates, because we don't want the scenario disappearing on us
                scenario.setDeploymentDate(null);
                scenario.setActionDate(null);
                scenario.setReturnDate(null);
                scenario.setStrategicObjective(true);
                scenario.setTurningPoint(false);
                scenario.getBackingScenario().setCloaked(true);
                // apply objective mods
                if (objectiveModifiers != null) {
                    for (String modifier : objectiveModifiers) {
                        scenario.getBackingScenario()
                              .addScenarioModifier(AtBScenarioModifier.getScenarioModifier(modifier));
                    }
                }

                trackState.addScenario(scenario);

                StratConStrategicObjective sso = new StratConStrategicObjective();
                sso.setObjectiveCoords(coords);
                sso.setObjectiveType(StrategicObjectiveType.SpecificScenarioVictory);
                sso.setDesiredObjectiveCount(1);
                trackState.addStrategicObjective(sso);
            } else if (addedFacility) {
                trackState.removeFacility(coords);
            }
        }
    }

    /**
     * Searches for a random, unoccupied coordinate on the specified {@link StratConTrackState}.
     *
     * <p>This method provides a basic, simplified call to search for an unoccupied coordinate
     * with default settings: hexes containing player facilities and forces are not considered eligible targets, and
     * strategic targets are not emphasized.</p>
     *
     * <p>Delegates to {@link #getUnoccupiedCoords(StratConTrackState, boolean, boolean, boolean)}
     * with default values.</p>
     *
     * @param trackState the {@link StratConTrackState} on which to search for unoccupied coordinates
     *
     * @return a {@link StratConCoords} object representing a suitable, unoccupied location, or {@code null} if no such
     *       location is available
     */
    public static @Nullable StratConCoords getUnoccupiedCoords(StratConTrackState trackState) {
        return getUnoccupiedCoords(trackState, false, false, false);
    }

    /**
     * Searches for a suitable, random unoccupied coordinate on the specified {@link StratConTrackState}, applying
     * optional rules regarding player facilities, player forces, and strategic weighting.
     *
     * <p>A coordinate is considered suitable when all the following are true:</p>
     * <ul>
     *     <li>There is <b>no active scenario</b> at that coordinate.</li>
     *     <li>The coordinate does <b>not</b> contain player-assigned forces, unless {@code
     *     allowPlayerForces} is {@code true}.</li>
     *     <li>The coordinate either contains no facility, or contains an <b>allied facility</b> and
     *     {@code allowPlayerFacilities} is {@code true}.</li>
     * </ul>
     *
     * <p>Suitable coordinates are added into a weighted pool:</p>
     * <ul>
     *     <li>Empty coordinates (no facility, no forces) receive a default weight of {@code 1}.</li>
     *     <li>Allowed allied facilities receive a higher weight when {@code emphasizeStrategicTargets} is
     *     {@code true}.</li>
     *     <li>Allowed player-force coordinates also receive the strategic weight when
     *     {@code emphasizeStrategicTargets} is {@code true}.</li>
     * </ul>
     *
     * <p>The strategic emphasis weight is equal to {@code max(trackWidth, trackHeight)} when
     * {@code emphasizeStrategicTargets} is enabled, and {@code 1} otherwise.</p>
     *
     * <p>The method returns one randomly weighted coordinate, or {@code null} if no valid coordinates are
     * available.</p>
     *
     * @param trackState                the {@link StratConTrackState} to search for unoccupied coordinates
     * @param allowPlayerFacilities     whether allied (player-owned or allied-owned) facilities are considered valid
     * @param allowPlayerForces         whether coordinates containing player-assigned forces are valid; if
     *                                  {@code false}, such coordinates are excluded entirely
     * @param emphasizeStrategicTargets whether to apply increased weighting to strategic locations (allowed allied
     *                                  facilities and allowed player-force coordinates)
     *
     * @return a randomly weighted, valid {@link StratConCoords}, or {@code null} if none exist
     */
    public static @Nullable StratConCoords getUnoccupiedCoords(StratConTrackState trackState,
          boolean allowPlayerFacilities, boolean allowPlayerForces, boolean emphasizeStrategicTargets) {
        final int trackHeight = trackState.getHeight();
        final int trackWidth = trackState.getWidth();

        int defaultWeight = 1;
        int strategicEmphasis = emphasizeStrategicTargets ? max(trackHeight, trackWidth) : defaultWeight;

        Collection<StratConCoords> forceCoords = trackState.getAssignedForceCoords().values();
        WeightedIntMap<StratConCoords> weightedMap = new WeightedIntMap<>();
        for (int y = 0; y < trackHeight; y++) {
            for (int x = 0; x < trackWidth; x++) {
                StratConCoords coords = new StratConCoords(x, y);
                // Ocean hexes never host scenarios or facilities.
                if (StratConBiomeManifest.isOceanTerrain(trackState.getTerrainTile(coords))) {
                    continue;
                }

                if (trackState.getScenario(coords) != null) {
                    continue;
                }

                if (forceCoords.contains(coords)) {
                    if (allowPlayerForces) {
                        weightedMap.add(strategicEmphasis, coords);
                    }
                    continue;
                }

                StratConFacility facility = trackState.getFacility(coords);
                if (facility == null) {
                    weightedMap.add(defaultWeight, coords);
                } else if (allowPlayerFacilities && facility.isOwnerAlliedToPlayer()) {
                    weightedMap.add(strategicEmphasis, coords);
                }
            }
        }

        return weightedMap.randomItem();
    }

    /**
     * Given a mission (that's an AtB contract), restore track state information, such as pointers from StratCon
     * scenario objects to AtB scenario objects.
     */
    public static void restoreTransientStratconInformation(AbstractContract mission, Campaign campaign) {
        // Having loaded scenarios and such, we now need to go through any StratCon
        // scenarios for this contract
        // and set their backing scenario pointers to the existing scenarios stored in
        // the campaign for this contract
        StratConCampaignState campaignState = mission.getStratConCampaignState();
        if (campaignState != null) {
            for (StratConTrackState track : campaignState.getTracks()) {
                for (StratConScenario scenario : track.getScenarios().values()) {
                    Scenario campaignScenario = campaign.getScenario(scenario.getBackingScenarioID());

                    if ((campaignScenario instanceof AtBDynamicScenario)) {
                        scenario.setBackingScenario((AtBDynamicScenario) campaignScenario);
                    } else {
                        LOGGER.warn("Unable to set backing scenario for StratCon scenario in track {} ID {}",
                              track.getDisplayableName(),
                              scenario.getBackingScenarioID());
                    }
                }
            }
        }
    }
}
