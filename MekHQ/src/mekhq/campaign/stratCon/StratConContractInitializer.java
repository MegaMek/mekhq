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
package mekhq.campaign.stratCon;

import static java.lang.Math.min;
import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static mekhq.campaign.stratCon.SupportPointNegotiation.negotiateInitialSupportPoints;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.stratCon.StratConContractDefinition.StrategicObjectiveType;

/**
 * This class handles StratCon state initialization when a contract is signed.
 */
public class StratConContractInitializer {
    private static final MMLogger LOGGER = MMLogger.create(StratConContractInitializer.class);

    public static final int NUM_LANCES_PER_TRACK = 3;
    public static final int ZERO_CELSIUS_IN_KELVIN = 273;

    /**
     * Initializes the campaign state given a contract, campaign and contract definition
     */
    public static void initializeCampaignState(AtBContract contract, Campaign campaign,
          StratConContractDefinition contractDefinition) {
        StratConCampaignState campaignState = new StratConCampaignState(contract);
        campaignState.setBriefingText(contractDefinition.getBriefing() +
                                            "<br/>" +
                                            contract.getCommandRights().getStratConText());
        campaignState.setAllowEarlyVictory(contractDefinition.isAllowEarlyVictory());

        // dependency: this is required here in order for scenario initialization to
        // work properly
        contract.setStratConCampaignState(campaignState);

        // First, initialize the proper number of tracks. Then:
        // for each objective:
        // step 1: calculate objective count
        // if scaled, multiply # required lances by factor, round up, otherwise just
        // fixed #
        // step 2: evenly distribute objectives through tracks
        // if uneven number remaining, distribute randomly
        // when objective is specific scenario victory, place specially flagged
        // scenarios
        // when objective is allied/hostile facility, place those facilities

        int maximumTrackIndex = Math.max(0, contract.getRequiredCombatTeams() / NUM_LANCES_PER_TRACK);
        int planetaryTemperature = campaign.getLocation().getPlanet().getTemperature(campaign.getLocalDate());

        for (int x = 0; x < maximumTrackIndex; x++) {
            int scenarioOdds = getScenarioOdds(contractDefinition);
            int deploymentTime = getDeploymentTime(contractDefinition);

            StratConTrackState track = initializeTrackState(NUM_LANCES_PER_TRACK,
                  scenarioOdds,
                  deploymentTime,
                  planetaryTemperature);
            track.setDisplayableName(String.format("Sector %d", x));
            campaignState.addTrack(track);
        }

        // a campaign will have X tracks going at a time, where
        // X = # required lances / 3, rounded up. The last track will have fewer
        // required lances.
        int oddLanceCount = contract.getRequiredCombatTeams() % NUM_LANCES_PER_TRACK;
        if (oddLanceCount > 0) {
            int scenarioOdds = getScenarioOdds(contractDefinition);
            int deploymentTime = getDeploymentTime(contractDefinition);

            StratConTrackState track = initializeTrackState(oddLanceCount,
                  scenarioOdds,
                  deploymentTime,
                  planetaryTemperature);
            track.setDisplayableName(String.format("Sector %d", campaignState.getTrackCount()));
            campaignState.addTrack(track);
        }

        // Last chance generation, to ensure we never generate a StratCon map with 0 tracks
        if (campaignState.getTrackCount() == 0) {
            int scenarioOdds = getScenarioOdds(contractDefinition);
            int deploymentTime = getDeploymentTime(contractDefinition);

            StratConTrackState track = initializeTrackState(1, scenarioOdds, deploymentTime, planetaryTemperature);
            track.setDisplayableName(String.format("Sector %d", campaignState.getTrackCount()));
            campaignState.addTrack(track);
        }

        // now seed the tracks with objectives and facilities
        boolean isNotUsingMaplessMode = !campaign.getCampaignOptions().isUseStratConMaplessMode();
        if (isNotUsingMaplessMode) {
            for (ObjectiveParameters objectiveParams : contractDefinition.getObjectiveParameters()) {
                int objectiveCount = objectiveParams.objectiveCount > 0 ?
                                           (int) objectiveParams.objectiveCount :
                                           (int) Math.max(1,
                                                 -objectiveParams.objectiveCount * contract.getRequiredCombatTeams());

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
        if (isNotUsingMaplessMode) {
            int facilityCount = contractDefinition.getAlliedFacilityCount() > 0 ?
                                      (int) contractDefinition.getAlliedFacilityCount() :
                                      (int) (-contractDefinition.getAlliedFacilityCount() *
                                                   contract.getRequiredCombatTeams());

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
                                               contract.getRequiredCombatTeams());

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

        // Determine starting morale
        if (contract.getContractType().isGarrisonDuty()) {
            contract.setMoraleLevel(AtBMoraleLevel.ROUTED);

            LocalDate routEnd = contract.getStartDate().plusMonths(Math.max(1, Compute.d6() - 3)).minusDays(1);
            contract.setRoutEndDate(routEnd);
        } else {
            contract.checkMorale(campaign, campaign.getLocalDate());

            if (contract.getMoraleLevel().isRouted()) {
                contract.setMoraleLevel(AtBMoraleLevel.CRITICAL);
            }

            if (contract.getContractType().isReliefDuty()) {
                int currentMoraleLevel = min(6, contract.getMoraleLevel().ordinal() + 1);

                contract.setMoraleLevel(AtBMoraleLevel.parseFromString(String.valueOf(currentMoraleLevel)));
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
    private static int getScenarioOdds(StratConContractDefinition contractDefinition) {
        return contractDefinition.getScenarioOdds().get(Compute.randomInt(contractDefinition.getScenarioOdds().size()));
    }

    /**
     * Set up initial state of a track, dimensions are based on number of assigned lances.
     */
    public static StratConTrackState initializeTrackState(int numLances, int scenarioOdds, int deploymentTime,
          int planetaryTemp) {
        // to initialize a track,
        // 1. we set the # of required lances
        // 2. set the track size to a total of num lances * 28 hexes, a rectangle that is
        // wider than it is taller
        // the idea being to create a roughly rectangular playing field that,
        // if one deploys a scout lance each week to a different spot, can be more or
        // less fully covered

        StratConTrackState retVal = new StratConTrackState();
        retVal.setRequiredLanceCount(numLances);

        // set width and height
        int numHexes = numLances * 28;
        int height = (int) Math.floor(Math.sqrt(numHexes));
        int width = numHexes / height;
        retVal.setWidth(width);
        retVal.setHeight(height);

        retVal.setScenarioOdds(scenarioOdds);
        retVal.setDeploymentTime(deploymentTime);

        // figure out track "average" temperature; this is the equatorial temperature
        // with
        // a random number between 10 and -40 added to it: equator is about as hot as it
        // gets with some exceptions
        int tempVariation = Compute.randomInt(51) - 40;
        retVal.setTemperature(planetaryTemp + tempVariation);

        // place terrain based on temperature
        StratConTerrainPlacer.InitializeTrackTerrain(retVal);

        return retVal;
    }

    /**
     * Generates an array list representing the number of objects to place in a given number of tracks.
     */
    private static List<Integer> trackObjectDistribution(int numObjects, int numTracks) {
        // This ensures we're not at risk of dividing by 0
        numTracks = Math.max(1, numTracks);

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
    private static void initializeTrackFacilities(StratConTrackState trackState, int numFacilities,
          ForceAlignment owner, boolean strategicObjective, List<String> modifiers) {

        int trackSize = trackState.getWidth() * trackState.getHeight();

        for (int fCount = 0; fCount < numFacilities; fCount++) {
            // if there's no possible empty places to put down a new scenario, then move on
            if ((trackState.getFacilities().size() + trackState.getScenarios().size()) >= trackSize) {
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
                LOGGER.warn("Unable to place facility on track {}, as all coords were occupied. Aborting.",
                      trackState.getDisplayableName());
                return;
            }

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
     * @param contract           the {@link AtBContract} related to the current StratCon campaign
     * @param trackState         the {@link StratConTrackState} representing the track where objectives are placed
     * @param numScenarios       the number of objective scenarios to generate
     * @param objectiveScenarios a list of {@link String} identifiers for potential scenarios that can be generated
     * @param objectiveModifiers a list of optional {@link String} modifiers to apply to the generated scenarios; can be
     *                           {@code null} if no modifiers are required
     */
    private static void initializeObjectiveScenarios(Campaign campaign, AtBContract contract,
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

            StratConCoords coords = getUnoccupiedCoords(trackState);

            if (coords == null) {
                LOGGER.error("Unable to place objective scenario on track {}, as all coords were occupied. Aborting.",
                      trackState.getDisplayableName());
                return;
            }

            // facility
            if (template.isFacilityScenario()) {
                StratConFacility facility = template.isHostileFacility() ?
                                                  StratConFacilityFactory.getRandomHostileFacility() :
                                                  StratConFacilityFactory.getRandomAlliedFacility();
                trackState.addFacility(coords, facility);
            }

            // create scenario - don't assign a force yet
            StratConScenario scenario = StratConRulesManager.generateScenario(campaign,
                  contract,
                  trackState,
                  Force.FORCE_NONE,
                  coords,
                  template,
                  null);

            if (scenario != null) {
                // clear dates, because we don't want the scenario disappearing on us
                scenario.setDeploymentDate(null);
                scenario.setActionDate(null);
                scenario.setReturnDate(null);
                scenario.setStrategicObjective(true);
                scenario.setTurningPoint(true);
                scenario.getBackingScenario().setCloaked(true);
                // apply objective mods
                if (objectiveModifiers != null) {
                    for (String modifier : objectiveModifiers) {
                        scenario.getBackingScenario()
                              .addScenarioModifier(AtBScenarioModifier.getScenarioModifier(modifier));
                    }
                }

                trackState.addScenario(scenario);
            }

            StratConStrategicObjective sso = new StratConStrategicObjective();
            sso.setObjectiveCoords(coords);
            sso.setObjectiveType(StrategicObjectiveType.SpecificScenarioVictory);
            sso.setDesiredObjectiveCount(1);
            trackState.addStrategicObjective(sso);
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
     * Searches for a suitable, random unoccupied coordinate on the specified {@link StratConTrackState}.
     *
     * <p>A coordinate is considered suitable based on the absence of any existing scenario, and depending
     * on the input parameters, may also depend on the presence or absence of facilities, player forces, or an emphasis
     * on strategic locations.</p>
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *   <li>Iterates over all possible coordinates on the provided track.</li>
     *   <li>Filters the coordinates based on the following criteria:
     *     <ul>
     *       <li>A coordinate is not occupied by a scenario.</li>
     *       <li>{@code allowPlayerFacilities = true}: Locations with facilities owned by the player
     *           are suitable and factored into the result.</li>
     *       <li>{@code allowPlayerForces = true}: Coordinates with forces assigned to them are also
     *           included in the result.</li>
     *       <li>{@code emphasizeStrategicTargets = true}: Strategic locations, such as those containing
     *           facilities not owned by players, are given additional weight to increase their chance
     *           of being selected.</li>
     *     </ul>
     *   </li>
     *   <li>Returns a randomly chosen coordinate from the remaining suitable locations, if any.</li>
     * </ol>
     *
     * <p>If no suitable coordinates are found, the method will return {@code null}.</p>
     *
     * @param trackState                the {@link StratConTrackState} containing the area to search
     * @param allowPlayerFacilities     a {@code boolean} indicating whether player-owned or allied facilities should be
     *                                  considered suitable
     * @param allowPlayerForces         a {@code boolean} indicating whether coordinates with player-assigned forces
     *                                  count as valid
     * @param emphasizeStrategicTargets a {@code boolean} determining whether strategic targets (e.g., enemy-held
     *                                  facilities) are given additional weighting
     *
     * @return a {@link StratConCoords} object representing the location of a suitable, unoccupied coordinate, or
     *       {@code null} if no valid coordinates are available
     */
    public static @Nullable StratConCoords getUnoccupiedCoords(StratConTrackState trackState,
          boolean allowPlayerFacilities, boolean allowPlayerForces, boolean emphasizeStrategicTargets) {
        final int trackHeight = trackState.getHeight();
        final int trackWidth = trackState.getWidth();

        int weightingMultiplier = 8;

        List<StratConCoords> suitableCoords = new ArrayList<>();

        for (int y = 0; y < trackHeight; y++) {
            for (int x = 0; x < trackWidth; x++) {
                StratConCoords coords = new StratConCoords(x, y);

                if (trackState.getScenario(coords) != null) {
                    continue;
                }

                StratConFacility facility = trackState.getFacility(coords);
                if (facility == null) {
                    suitableCoords.add(coords);
                } else if (allowPlayerFacilities && !facility.isOwnerAlliedToPlayer()) {
                    if (emphasizeStrategicTargets) {
                        for (int weight = 0; weight < weightingMultiplier; weight++) {
                            suitableCoords.add(coords);
                        }
                    } else {
                        suitableCoords.add(coords);
                    }
                }

                if (allowPlayerForces && trackState.getAssignedForceCoords().containsValue(coords)) {
                    if (emphasizeStrategicTargets) {
                        weightingMultiplier /= 2;
                        for (int weight = 0; weight < weightingMultiplier; weight++) {
                            suitableCoords.add(coords);
                        }
                    }
                }
            }
        }

        if (suitableCoords.isEmpty()) {
            return null;
        } else {
            return getRandomItem(suitableCoords);
        }
    }

    /**
     * Given a mission (that's an AtB contract), restore track state information, such as pointers from StratCon
     * scenario objects to AtB scenario objects.
     */
    public static void restoreTransientStratconInformation(Mission m, Campaign campaign) {
        if (m instanceof AtBContract atbContract) {
            // Having loaded scenarios and such, we now need to go through any StratCon
            // scenarios for this contract
            // and set their backing scenario pointers to the existing scenarios stored in
            // the campaign for this contract
            if (atbContract.getStratconCampaignState() != null) {
                for (StratConTrackState track : atbContract.getStratconCampaignState().getTracks()) {
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
}
