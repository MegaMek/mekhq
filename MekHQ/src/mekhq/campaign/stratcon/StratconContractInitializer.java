/*
 * Copyright (c) 2019-2024 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.stratcon.StratconContractDefinition.ObjectiveParameters;
import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static java.lang.Math.min;
import static mekhq.campaign.stratcon.SupportPointNegotiation.negotiateInitialSupportPoints;

/**
 * This class handles StratCon state initialization when a contract is signed.
 */
public class StratconContractInitializer {
    private static final MMLogger logger = MMLogger.create(StratconContractInitializer.class);

    public static final int NUM_LANCES_PER_TRACK = 3;
    public static final int ZERO_CELSIUS_IN_KELVIN = 273;

    /**
     * Initializes the campaign state given a contract, campaign and contract
     * definition
     */
    public static void initializeCampaignState(AtBContract contract, Campaign campaign,
            StratconContractDefinition contractDefinition) {
        StratconCampaignState campaignState = new StratconCampaignState(contract);
        campaignState.setBriefingText(
                contractDefinition.getBriefing() + "<br/>" + contract.getCommandRights().getStratConText());
        campaignState.setAllowEarlyVictory(contractDefinition.isAllowEarlyVictory());

        // dependency: this is required here in order for scenario initialization to
        // work properly
        contract.setStratconCampaignState(campaignState);

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

        int maximumTrackIndex = Math.max(0, contract.getRequiredLances() / NUM_LANCES_PER_TRACK);
        int planetaryTemperature = campaign.getLocation().getPlanet().getTemperature(campaign.getLocalDate());

        for (int x = 0; x < maximumTrackIndex; x++) {
            int scenarioOdds = contractDefinition.getScenarioOdds()
                    .get(Compute.randomInt(contractDefinition.getScenarioOdds().size()));
            int deploymentTime = contractDefinition.getDeploymentTimes()
                    .get(Compute.randomInt(contractDefinition.getDeploymentTimes().size()));

            StratconTrackState track = initializeTrackState(NUM_LANCES_PER_TRACK, scenarioOdds, deploymentTime,
                    planetaryTemperature);
            track.setDisplayableName(String.format("Sector %d", x));
            campaignState.addTrack(track);
        }

        // a campaign will have X tracks going at a time, where
        // X = # required lances / 3, rounded up. The last track will have fewer
        // required lances.
        int oddLanceCount = contract.getRequiredLances() % NUM_LANCES_PER_TRACK;
        if (oddLanceCount > 0) {
            int scenarioOdds = contractDefinition.getScenarioOdds()
                    .get(Compute.randomInt(contractDefinition.getScenarioOdds().size()));
            int deploymentTime = contractDefinition.getDeploymentTimes()
                    .get(Compute.randomInt(contractDefinition.getDeploymentTimes().size()));

            StratconTrackState track = initializeTrackState(oddLanceCount, scenarioOdds, deploymentTime,
                    planetaryTemperature);
            track.setDisplayableName(String.format("Sector %d", campaignState.getTracks().size()));
            campaignState.addTrack(track);
        }

        // now seed the tracks with objectives and facilities
        for (ObjectiveParameters objectiveParams : contractDefinition.getObjectiveParameters()) {
            int objectiveCount = objectiveParams.objectiveCount > 0 ? (int) objectiveParams.objectiveCount
                    : (int) Math.max(1, -objectiveParams.objectiveCount * contract.getRequiredLances());

            List<Integer> trackObjects = trackObjectDistribution(objectiveCount, campaignState.getTracks().size());

            for (int x = 0; x < trackObjects.size(); x++) {
                int numObjects = trackObjects.get(x);

                switch (objectiveParams.objectiveType) {
                    case SpecificScenarioVictory:
                        initializeObjectiveScenarios(campaign, contract, campaignState.getTrack(x), numObjects,
                                objectiveParams.objectiveScenarios, objectiveParams.objectiveScenarioModifiers);
                        break;
                    case AlliedFacilityControl:
                        initializeTrackFacilities(campaignState.getTrack(x), numObjects, ForceAlignment.Allied,
                                true, objectiveParams.objectiveScenarioModifiers);
                        break;
                    case HostileFacilityControl:
                    case FacilityDestruction:
                        initializeTrackFacilities(campaignState.getTrack(x), numObjects, ForceAlignment.Opposing,
                                true, objectiveParams.objectiveScenarioModifiers);
                        break;
                    case AnyScenarioVictory:
                        // set up a "win X scenarios" objective
                        StratconStrategicObjective sso = new StratconStrategicObjective();
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
        int facilityCount = contractDefinition.getAlliedFacilityCount() > 0
                ? (int) contractDefinition.getAlliedFacilityCount()
                : (int) (-contractDefinition.getAlliedFacilityCount() * contract.getRequiredLances());

        List<Integer> trackObjects = trackObjectDistribution(facilityCount, campaignState.getTracks().size());

        for (int x = 0; x < trackObjects.size(); x++) {
            int numObjects = trackObjects.get(x);

            initializeTrackFacilities(campaignState.getTrack(x), numObjects, ForceAlignment.Allied,
                    false, Collections.emptyList());
        }

        // non-objective hostile facilities
        facilityCount = contractDefinition.getHostileFacilityCount() > 0
                ? (int) contractDefinition.getHostileFacilityCount()
                : (int) (-contractDefinition.getHostileFacilityCount() * contract.getRequiredLances());

        trackObjects = trackObjectDistribution(facilityCount, campaignState.getTracks().size());

        for (int x = 0; x < trackObjects.size(); x++) {
            int numObjects = trackObjects.get(x);

            initializeTrackFacilities(campaignState.getTrack(x), numObjects, ForceAlignment.Opposing,
                    false, Collections.emptyList());
        }

        // clean up objectives for integrated command:
        // we're still going to have all the objective facilities and scenarios
        // but the player has no control over where they go, so they're
        // not on the hook for actually completing objectives,
        // just fighting where they're told to fight
        if (contract.getCommandRights() == ContractCommandRights.INTEGRATED) {
            for (StratconTrackState track : campaignState.getTracks()) {
                track.getStrategicObjectives().clear();
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

        // Roll to see if a hidden cache is present
        if (campaign.getLocalDate().isAfter(LocalDate.of(2900, 1, 1))) {
//            if (Compute.randomInt(100) == 0) {
//                ScenarioTemplate template = ScenarioTemplate.Deserialize(
//                    "data/scenariotemplates/Chasing a Rumor.xml");
//
//                if (template != null) {
//                    StratconScenario hiddenCache = addHiddenExternalScenario(campaign, contract,
//                        null, template, false);
//
//                    if (hiddenCache != null) {
//                        logger.info(String.format("A secret cache has been spawned for contract %s",
//                            contract.getName()));
//                    }
//                } else {
//                    logger.error("'Chasing a Rumor' scenario failed to deserialize");
//                }
//            }
        }

        // now we're done
    }

    /**
     * Set up initial state of a track, dimensions are based on number of assigned
     * lances.
     */
    public static StratconTrackState initializeTrackState(int numLances, int scenarioOdds,
            int deploymentTime, int planetaryTemp) {
        // to initialize a track,
        // 1. we set the # of required lances
        // 2. set the track size to a total of numlances * 28 hexes, a rectangle that is
        // wider than it is taller
        // the idea being to create a roughly rectangular playing field that,
        // if one deploys a scout lance each week to a different spot, can be more or
        // less fully covered

        StratconTrackState retVal = new StratconTrackState();
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
        StratconTerrainPlacer.InitializeTrackTerrain(retVal);

        return retVal;
    }

    /**
     * Generates an array list representing the number of objects to place in a
     * given number of tracks.
     */
    private static List<Integer> trackObjectDistribution(int numObjects, int numTracks) {
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
     * Worker function that takes a trackstate and plops down the given number of
     * facilities owned by the given faction
     * Avoids places with existing facilities and scenarios, capable of taking
     * facility sub set and setting strategic objective flag.
     */
    private static void initializeTrackFacilities(StratconTrackState trackState, int numFacilities,
            ForceAlignment owner, boolean strategicObjective, List<String> modifiers) {

        int trackSize = trackState.getWidth() * trackState.getHeight();

        for (int fCount = 0; fCount < numFacilities; fCount++) {
            // if there's no possible empty places to put down a new scenario, then move on
            if ((trackState.getFacilities().size() + trackState.getScenarios().size()) >= trackSize) {
                break;
            }

            StratconFacility sf = owner == ForceAlignment.Allied ? StratconFacilityFactory.getRandomAlliedFacility()
                    : StratconFacilityFactory.getRandomHostileFacility();

            sf.setOwner(owner);
            sf.setStrategicObjective(strategicObjective);
            sf.getLocalModifiers().addAll(modifiers);

            StratconCoords coords = getUnoccupiedCoords(trackState, false);

            if (coords == null) {
                logger.warn(String.format("Unable to place facility on track %s," +
                        " as all coords were occupied. Aborting.",
                    trackState.getDisplayableName()));
                return;
            }

            trackState.addFacility(coords, sf);

            if (strategicObjective) {
                StratconStrategicObjective sso = new StratconStrategicObjective();
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
     * Initializes and populates a StratCon track with a specified number of objective scenarios.
     * This method selects scenario templates, places them on the track in unoccupied coordinates,
     * and optionally assigns facilities and objectives based on predefined rules.
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
     * @param campaign            the {@link Campaign} managing the state of the overall gameplay
     * @param contract            the {@link AtBContract} related to the current StratCon campaign
     * @param trackState          the {@link StratconTrackState} representing the track where objectives are placed
     * @param numScenarios        the number of objective scenarios to generate
     * @param objectiveScenarios  a list of {@link String} identifiers for potential scenarios that can be generated
     * @param objectiveModifiers  a list of optional {@link String} modifiers to apply to the generated scenarios;
     *                             can be {@code null} if no modifiers are required
     */
    private static void initializeObjectiveScenarios(Campaign campaign, AtBContract contract,
            StratconTrackState trackState,
            int numScenarios, List<String> objectiveScenarios, List<String> objectiveModifiers) {
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
            ScenarioTemplate template = StratconScenarioFactory.getSpecificScenario(
                    objectiveScenarios.get(Compute.randomInt(objectiveScenarios.size())));

            StratconCoords coords = getUnoccupiedCoords(trackState, false);

            if (coords == null) {
                logger.error(String.format("Unable to place objective scenario on track %s," +
                        " as all coords were occupied. Aborting.",
                    trackState.getDisplayableName()));
                return;
            }

            // facility
            if (template.isFacilityScenario()) {
                StratconFacility facility = template.isHostileFacility()
                        ? StratconFacilityFactory.getRandomHostileFacility()
                        : StratconFacilityFactory.getRandomAlliedFacility();
                trackState.addFacility(coords, facility);
            }

            // create scenario - don't assign a force yet
            StratconScenario scenario = StratconRulesManager.generateScenario(campaign, contract, trackState,
                    Force.FORCE_NONE, coords, template, null);

            // clear dates, because we don't want the scenario disappearing on us
            scenario.setDeploymentDate(null);
            scenario.setActionDate(null);
            scenario.setReturnDate(null);
            scenario.setStrategicObjective(true);
            scenario.getBackingScenario().setCloaked(true);
            // apply objective mods
            if (objectiveModifiers != null) {
                for (String modifier : objectiveModifiers) {
                    scenario.getBackingScenario()
                            .addScenarioModifier(AtBScenarioModifier.getScenarioModifier(modifier));
                }
            }

            trackState.addScenario(scenario);

            StratconStrategicObjective sso = new StratconStrategicObjective();
            sso.setObjectiveCoords(coords);
            sso.setObjectiveType(StrategicObjectiveType.SpecificScenarioVictory);
            sso.setDesiredObjectiveCount(1);
            trackState.addStrategicObjective(sso);
        }
    }

    /**
     * Searches for a suitable coordinate on the given {@link StratconTrackState}.
     * The suitability of a coordinate is determined by the absence of a scenario in the coordinate
     * and the absence of a facility or the presence of a player-allied facility (determined by the
     * value of allowPlayerFacilities).
     * <p>
     * The method iterates through all possible coordinates and shuffles them to ensure randomness.
     * A random coordinate is then selected from the list of suitable coordinates and returned.
     *
     * @param trackState            the {@link StratconTrackState} object on which to perform the search
     * @param allowPlayerFacilities a {@link boolean} value indicating whether player-owned facilities
     *                             should be considered suitable
     * @return a {@link StratconCoords} object representing the coordinates of a suitable location,
     * or {@code null} if no suitable location was found
     */
    public static @Nullable StratconCoords getUnoccupiedCoords(StratconTrackState trackState, boolean allowPlayerFacilities) {
        int trackHeight = trackState.getHeight();
        int trackWidth = trackState.getWidth();

        List<StratconCoords> suitableCoords = new ArrayList<>();

        for (int y = 0; y < trackHeight; y++) {
            for (int x = 0; x < trackWidth; x++) {
                StratconCoords coords = new StratconCoords(x, y);

                if (trackState.getScenario(coords) != null) {
                    continue;
                }

                if (trackState.getFacility(coords) == null) {
                    suitableCoords.add(coords);
                    continue;
                }

                if (trackState.getFacility(coords).getOwner() != ForceAlignment.Opposing) {
                    if (allowPlayerFacilities) {
                        suitableCoords.add(coords);
                    }
                }
            }
        }

        Collections.shuffle(suitableCoords);

        if (suitableCoords.isEmpty()) {
            return null;
        } else {
            int randomIndex = new Random().nextInt(suitableCoords.size());
            return suitableCoords.get(randomIndex);
        }
    }

    /**
     * Given a mission (that's an AtB contract), restore track state information,
     * such as pointers from StratCon scenario objects to AtB scenario objects.
     */
    public static void restoreTransientStratconInformation(Mission m, Campaign campaign) {
        if (m instanceof AtBContract atbContract) {
            // Having loaded scenarios and such, we now need to go through any StratCon
            // scenarios for this contract
            // and set their backing scenario pointers to the existing scenarios stored in
            // the campaign for this contract
            if (atbContract.getStratconCampaignState() != null) {
                for (StratconTrackState track : atbContract.getStratconCampaignState().getTracks()) {
                    for (StratconScenario scenario : track.getScenarios().values()) {
                        Scenario campaignScenario = campaign.getScenario(scenario.getBackingScenarioID());

                        if ((campaignScenario instanceof AtBDynamicScenario)) {
                            scenario.setBackingScenario((AtBDynamicScenario) campaignScenario);
                        } else {
                            logger.warn(String.format(
                                    "Unable to set backing scenario for stratcon scenario in track %s ID %d",
                                    track.getDisplayableName(), scenario.getBackingScenarioID()));
                        }
                    }
                }
            }
        }
    }
}
