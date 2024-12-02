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

import megamek.codeUtilities.ObjectUtility;
import megamek.common.Compute;
import megamek.common.Minefield;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.event.ScenarioChangedEvent;
import mekhq.campaign.event.StratconDeploymentEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.StrategicFormation;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.atb.AtBScenarioModifier.EventTiming;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.unit.Unit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;
import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.AllGroundTerrain;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.LowAtmosphere;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.Space;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.SpecificGroundTerrain;
import static mekhq.campaign.stratcon.StratconContractInitializer.getUnoccupiedCoords;

/**
 * This class contains "rules" logic for the AtB-Stratcon state
 *
 * @author NickAragua
 */
public class StratconRulesManager {
    private static final MMLogger logger = MMLogger.create(StratconRulesManager.class);

    /**
     * What makes a particular lance eligible to be reinforcements for a scenario
     */
    public enum ReinforcementEligibilityType {
        /**
         * Nothing
         */
        None,

        /**
         * Lance is already deployed to the track
         */
        ChainedScenario,

        /**
         * We pay a support point or convert a Campaign Victory Point to a support point
         */
        SupportPoint,

        /**
         * The lance's deployment orders are "Fight"
         */
        FightLance
    }

    /**
     * This function potentially generates non-player-initiated scenarios for the
     * given track.
     */
    public static void generateScenariosForTrack(Campaign campaign, AtBContract contract, StratconTrackState track) {
        // maps scenarios to force IDs
        List<StratconScenario> generatedScenarios = new ArrayList<>();
        final boolean autoAssignLances = contract.getCommandRights().isIntegrated();

        // get this list just so we have it available
        List<Integer> availableForceIDs = getAvailableForceIDs(campaign);
        Map<MapLocation, List<Integer>> sortedAvailableForceIDs = sortForcesByMapType(availableForceIDs, campaign);

        // make X rolls, where X is the number of required lances for the track
        // that's the chance to spawn a scenario.
        // if a scenario occurs, then we pick a random non-deployed lance and use it to
        // drive the opfor generation later
        // once we've determined that scenarios occur, we loop through the ones that we
        // generated
        // and use the random force to drive opfor generation (#required lances
        // multiplies the BV budget of all
        int scenarioRolls = track.getRequiredLanceCount();

        if (!autoAssignLances) {
            AtBMoraleLevel moraleLevel = contract.getMoraleLevel();

            switch (moraleLevel) {
                case STALEMATE -> scenarioRolls = (int) round(scenarioRolls * 1.25);
                case ADVANCING -> scenarioRolls = (int) round(scenarioRolls * 1.5);
                case DOMINATING -> scenarioRolls = scenarioRolls * 2;
                case OVERWHELMING -> scenarioRolls = scenarioRolls * 3;
            }
        }

        for (int scenarioIndex = 0; scenarioIndex < scenarioRolls; scenarioIndex++) {
            if (autoAssignLances && availableForceIDs.isEmpty()) {
                break;
            }

            int targetNum = calculateScenarioOdds(track, contract, false);
            int roll = Compute.randomInt(100);

            logger.info(String.format("StratCon Weekly Scenario Roll: %s vs. %s", roll, targetNum));

            // if we haven't already used all the player forces and are required to randomly
            // generate a scenario
            if (roll < targetNum) {
                // pick random coordinates and force to drive the scenario
                StratconCoords scenarioCoords = getUnoccupiedCoords(track, true);

                if (scenarioCoords == null) {
                    logger.warn("Target track is full, skipping scenario generation");
                    continue;
                }

                // if forces are already assigned to these coordinates, use those instead
                // of randomly-selected ones
                if (track.getAssignedCoordForces().containsKey(scenarioCoords)) {
                    StratconScenario scenario = generateScenarioForExistingForces(scenarioCoords,
                            track.getAssignedCoordForces().get(scenarioCoords), contract, campaign, track);

                    if (scenario != null) {
                        generatedScenarios.add(scenario);
                    }
                    continue;
                }

                // otherwise, pick a random force from the avail
                int randomForceIndex = Compute.randomInt(availableForceIDs.size());
                int randomForceID = availableForceIDs.get(randomForceIndex);

                // remove the force from the available lists, so we don't designate it as primary
                // twice
                if (autoAssignLances) {
                    availableForceIDs.remove(randomForceIndex);
                }

                // we want to remove the actual int with the value, not the value at the index
                sortedAvailableForceIDs.get(AllGroundTerrain).remove((Integer) randomForceID);
                sortedAvailableForceIDs.get(LowAtmosphere).remove((Integer) randomForceID);
                sortedAvailableForceIDs.get(Space).remove((Integer) randomForceID);

                // two scenarios on the same coordinates wind up increasing in size
                if (track.getScenarios().containsKey(scenarioCoords)) {
                    track.getScenarios().get(scenarioCoords).incrementRequiredPlayerLances();
                    assignAppropriateExtraForceToScenario(track.getScenarios().get(scenarioCoords),
                            sortedAvailableForceIDs);
                    continue;
                }

                StratconScenario scenario = setupScenario(scenarioCoords, randomForceID, campaign, contract, track);

                if (scenario != null) {
                    generatedScenarios.add(scenario);
                }
            }
        }

        // If we didn't generate any scenarios, we can just return here
        if (generatedScenarios.isEmpty()) {
            return;
        }

        // if under liaison command, pick a random scenario from the ones generated
        // to set as required and attach liaison
        if (contract.getCommandRights().isLiaison()) {
            StratconScenario randomScenario = ObjectUtility.getRandomItem(generatedScenarios);
            randomScenario.setRequiredScenario(true);
            setAttachedUnitsModifier(randomScenario, contract);
        }

        // now, we loop through all the scenarios we set up
        // and generate the opfors / events / etc
        // if not auto-assigning lances, we then back out the lance assignments.
        for (StratconScenario scenario : generatedScenarios) {
            finalizeBackingScenario(campaign, contract, track, autoAssignLances, scenario);
        }
    }

    /**
     * Generates a StratCon scenario.
     * This is a utility method that allows us to generate a scenario quickly without specifying
     * track state and scenario template.
     *
     * @param campaign The current campaign.
     * @param contract The contract associated with the scenario.
     * @return A newly generated {@link StratconScenario}, or {@code null} if scenario creation fails.
     */
    public static @Nullable StratconScenario generateExternalScenario(Campaign campaign, AtBContract contract) {
        return generateExternalScenario(campaign, contract, null, null,
            null, false);
    }

    /**
     * Generates a new StratCon scenario using advanced configuration.
     * It provides a scenario based on a given campaign, contract, track, template.
     * This is meant for scenario control on a higher level than the overloading methods.
     *
     * @param campaign The current campaign.
     * @param contract The contract associated with the scenario.
     * @param track    The {@link StratconTrackState} the scenario should be assigned to, or
     *                 {@code null} to select a random track.
     * @param scenarioCoords   The {@link StratconCoords} where in the track to place the scenario, or
     *                 {@code null} to select a random hex. If populated, {@code track} cannot be
     *                 {@code null}
     * @param template A specific {@link ScenarioTemplate} to use for scenario generation,
     *                 or {@code null} to select scenario template randomly.
     * @param allowPlayerFacilities Whether the scenario is allowed to spawn on top of
     *                             player-allied facilities.
     * @return A newly generated {@link StratconScenario}, or {@code null} if scenario creation fails.
     */
     public static @Nullable StratconScenario generateExternalScenario(Campaign campaign, AtBContract contract,
                                    @Nullable StratconTrackState track, @Nullable StratconCoords scenarioCoords,
                                    @Nullable ScenarioTemplate template, boolean allowPlayerFacilities) {
         // If we're not generating for a specific track, randomly pick one.
         if (track == null) {
             track = getRandomTrack(contract);

             if (track == null) {
                 logger.error("Failed to generate a random track, aborting scenario generation.");
                 return null;
             }
         }

         // Are we automatically assigning lances?
         boolean autoAssignLances = contract.getCommandRights().isIntegrated();

         // Grab the available lances and sort them by map type
         List<Integer> availableForceIDs = getAvailableForceIDs(campaign);
         Map<MapLocation, List<Integer>> sortedAvailableForceIDs = sortForcesByMapType(availableForceIDs, campaign);

         // Select the target coords.
         if (scenarioCoords == null) {
             scenarioCoords = getUnoccupiedCoords(track, allowPlayerFacilities);
         }

         if (scenarioCoords == null) {
             logger.warn("Target track is full, aborting scenario generation.");
             return null;
         }

         // If forces are already assigned to the target coordinates, use those instead of randomly
         // selected a new force
         StratconScenario scenario = null;
         if (track.getAssignedCoordForces().containsKey(scenarioCoords)) {
             scenario = generateScenarioForExistingForces(scenarioCoords,
                 track.getAssignedCoordForces().get(scenarioCoords), contract, campaign, track,
                 template);
         }

         // Otherwise, pick a random force from those available
         // If a template has been specified, remove forces that aren't appropriate for the
         // template.
         if (template != null) {
             MapLocation location = template.mapParameters.getMapLocation();

             switch (location) {
                 case AllGroundTerrain, SpecificGroundTerrain -> {
                     sortedAvailableForceIDs.get(LowAtmosphere).clear();
                     sortedAvailableForceIDs.get(Space).clear();
                 }
                 case LowAtmosphere -> {
                     sortedAvailableForceIDs.get(AllGroundTerrain).clear();
                     sortedAvailableForceIDs.get(Space).clear();
                 }
                 case Space -> {
                     sortedAvailableForceIDs.get(AllGroundTerrain).clear();
                     sortedAvailableForceIDs.get(LowAtmosphere).clear();
                 }
             }
         }

         // If we haven't generated a scenario yet, it's because we need to pick a random force.
         if (scenario == null) {
             int availableForces = availableForceIDs.size();
             int randomForceID = FORCE_NONE;

             if (availableForces > 0) {
                 int randomForceIndex = Compute.randomInt(availableForces);
                 randomForceID = availableForceIDs.get(randomForceIndex);
             }

             scenario = setupScenario(scenarioCoords, randomForceID, campaign, contract, track, template);
         }

         if (scenario == null) {
             return null;
         }

         // We end by finalizing the scenario
         finalizeBackingScenario(campaign, contract, track, autoAssignLances, scenario);

         // We return the scenario in case we want to make specific changes.
         return scenario;
     }

    /**
     * Adds a {@link StratconScenario} to the specified contract. This scenario is cloaked so will
     * not be visible until the player uncovers it.
     * If no {@link StratconTrackState} or {@link ScenarioTemplate} is provided, random one will be
     * picked.
     *
     * @param campaign   The current campaign.
     * @param contract   The {@link AtBContract} associated with the scenario.
     * @param trackState The {@link StratconTrackState} in which the scenario occurs.
     *                  If {@code null}, a random trackState is selected.
     * @param template   The {@link ScenarioTemplate} for the scenario.
     *                  If {@code null}, the default template is used.
     * @param allowPlayerFacilities Whether the scenario is allowed to spawn on top of
     *                             player-allied facilities.
     *
     * @return The created {@link StratconScenario} or @code null},
     * if no {@link ScenarioTemplate} is found or if all coordinates in the provided
     * {@link StratconTrackState} are occupied (and therefore, scenario placement is not possible).
     */
    public static @Nullable StratconScenario addHiddenExternalScenario(Campaign campaign, AtBContract contract,
                                                      @Nullable StratconTrackState trackState,
                                                      @Nullable ScenarioTemplate template,
                                                      boolean allowPlayerFacilities) {
        // If we're not generating for a specific track, randomly pick one.
        if (trackState == null) {
            trackState = getRandomTrack(contract);

            if (trackState == null) {
                logger.error("Failed to generate a random track, aborting scenario generation.");
                return null;
            }
        }

        StratconCoords coords = getUnoccupiedCoords(trackState, allowPlayerFacilities);

        if (coords == null) {
            logger.error(String.format("Unable to place objective scenario on track %s," +
                    " as all coords were occupied. Aborting.",
                trackState.getDisplayableName()));
            return null;
        }

        // create scenario - don't assign a force yet
        StratconScenario scenario = StratconRulesManager.generateScenario(campaign, contract,
            trackState, FORCE_NONE, coords, template);

        if (scenario == null) {
            return null;
        }

        // clear dates, because we don't want the scenario disappearing on us
        scenario.setDeploymentDate(null);
        scenario.setActionDate(null);
        scenario.setReturnDate(null);
        scenario.setStrategicObjective(true);
        scenario.getBackingScenario().setCloaked(true);

        trackState.addScenario(scenario);

        return scenario;
    }

    /**
     * Fetches a random {@link StratconTrackState} from the {@link StratconCampaignState}.
     * If no tracks are present, it logs an error message and returns {@code null}.
     *
     * @param contract The {@link AtBContract} from which the track state will be fetched.
     * @return The randomly chosen {@link StratconTrackState}, or {@code null} if no tracks are available.
     */
     public static @Nullable StratconTrackState getRandomTrack(AtBContract contract) {
          List<StratconTrackState> tracks = contract.getStratconCampaignState().getTracks();
          Random rand = new Random();

          if (!tracks.isEmpty()) {
               return tracks.get(rand.nextInt(tracks.size()));
          } else {
               logger.error("No tracks available. Unable to fetch random track");
               return null;
          }
     }

    /**
     * Finalizes the backing scenario, setting up the OpFor, scenario parameters, and other
     * necessary steps.
     *
     * @param campaign        The current campaign.
     * @param contract        The contract associated with the scenario.
     * @param track           The relevant {@link StratconTrackState}.
     * @param autoAssignLances  Flag indicating whether lances are to be auto-assigned.
     * @param scenario        The {@link StratconScenario} scenario to be finalized.
     */
    private static void finalizeBackingScenario(Campaign campaign, AtBContract contract,
                        @Nullable StratconTrackState track, boolean autoAssignLances,
                        StratconScenario scenario) {
        AtBDynamicScenarioFactory.finalizeScenario(scenario.getBackingScenario(), contract, campaign);
        setScenarioParametersFromBiome(track, scenario);
        swapInPlayerUnits(scenario, campaign, FORCE_NONE);

        if (!autoAssignLances && !scenario.ignoreForceAutoAssignment()) {
            for (int forceID : scenario.getPlayerTemplateForceIDs()) {
                scenario.getBackingScenario().removeForce(forceID);
            }

            scenario.setCurrentState(ScenarioState.UNRESOLVED);
            track.addScenario(scenario);
        } else {
            commitPrimaryForces(campaign, scenario, track);
            // if we're auto-assigning lances, deploy all assigned forces to the track as well
            for (int forceID : scenario.getPrimaryForceIDs()) {
                processForceDeployment(scenario.getCoords(), forceID, campaign, track, false);
            }
        }
    }

    /**
     * Picks the scenario terrain based on the scenario coordinates' biome
     * Note that "finalizeScenario" currently wipes out temperature/map info so this
     * method must be called afterward.
     */
    public static void setScenarioParametersFromBiome(StratconTrackState track, StratconScenario scenario) {
        StratconCoords coords = scenario.getCoords();
        AtBDynamicScenario backingScenario = scenario.getBackingScenario();
        StratconBiomeManifest biomeManifest = StratconBiomeManifest.getInstance();

        // for non-surface scenarios, we will skip the temperature update
        if (backingScenario.getBoardType() != Scenario.T_SPACE &&
                backingScenario.getBoardType() != Scenario.T_ATMOSPHERE) {
            backingScenario.setTemperature(track.getTemperature());
        }

        StratconFacility facility = track.getFacility(scenario.getCoords());
        String terrainType;

        // facilities have their own terrain lists
        if (facility != null) {
            int kelvinTemp = track.getTemperature() + StratconContractInitializer.ZERO_CELSIUS_IN_KELVIN;
            StratconBiome facilityBiome;

            // if facility doesn't have a biome temp map or no entry for the current
            // temperature, use the default one
            if (facility.getBiomes().isEmpty() || (facility.getBiomeTempMap().floorEntry(kelvinTemp) == null)) {
                facilityBiome = biomeManifest.getTempMap(StratconBiomeManifest.TERRAN_FACILITY_BIOME)
                        .floorEntry(kelvinTemp).getValue();
            } else {
                facilityBiome = facility.getBiomeTempMap().floorEntry(kelvinTemp).getValue();
            }
            terrainType = facilityBiome.allowedTerrainTypes
                    .get(Compute.randomInt(facilityBiome.allowedTerrainTypes.size()));
        } else {
            terrainType = track.getTerrainTile(coords);
        }

        var mapTypes = biomeManifest.getBiomeMapTypes();

        // don't have a map list for the given terrain, leave it alone
        if (!mapTypes.containsKey(terrainType)) {
            return;
        }

        // if we are in space, do not update the map; note that it's ok to do so in low
        // atmo
        if (backingScenario.getBoardType() != Scenario.T_SPACE) {
            var mapTypeList = mapTypes.get(terrainType).mapTypes;
            backingScenario.setHasTrack(true);
            backingScenario.setTerrainType(terrainType);
            // for now, if we're using a fixed map or in a facility, don't replace the
            // scenario
            // TODO: facility spaces will always have a relevant biome
            if (!backingScenario.isUsingFixedMap()) {
                backingScenario.setMap(mapTypeList.get(Compute.randomInt(mapTypeList.size())));
            }
            backingScenario.setLightConditions();
            backingScenario.setWeatherConditions();
        }
    }

    /**
     * Worker function that looks through the scenario's templates and swaps in
     * player units for "player or allied force" templates.
     */
    private static void swapInPlayerUnits(StratconScenario scenario, Campaign campaign, int explicitForceID) {
        for (ScenarioForceTemplate sft : scenario.getScenarioTemplate().getAllScenarioForces()) {
            if (sft.getGenerationMethod() == ForceGenerationMethod.PlayerOrFixedUnitCount.ordinal()) {
                int unitCount = (int) scenario.getBackingScenario().getBotUnitTemplates().values().stream()
                        .filter(template -> template.getForceName().equals(sft.getForceName()))
                        .count();

                // get all the units that have been generated for this template

                // or the units embedded in bot forces
                unitCount += scenario.getBackingScenario().getBotForceTemplates().entrySet().stream()
                        .filter(tuple -> tuple.getValue().getForceName().equals(sft.getForceName()))
                        .mapToInt(tuple -> tuple.getKey().getFullEntityList(campaign).size())
                        .sum();

                // now we have a unit count. Don't bother with the next step if we don't have
                // any substitutions to make
                if (unitCount == 0) {
                    continue;
                }

                Collection<Unit> potentialUnits = new HashSet<>();

                // find units in player's campaign by default, all units in the TO&E are eligible
                if (explicitForceID == FORCE_NONE) {
                    for (UUID unitId : campaign.getForces().getUnits()) {
                        try {
                            potentialUnits.add(campaign.getUnit(unitId));
                        } catch (Exception exception) {
                            logger.error(String.format("Error retrieving unit (%s): %s",
                                unitId, exception.getMessage()));
                        }
                    }
                // if we're using a seed force, then units transporting this force are eligible
                } else {
                    Force force = campaign.getForce(explicitForceID);
                    for (UUID unitID : force.getUnits()) {
                        Unit unit = campaign.getUnit(unitID);
                        if (unit.getTransportShipAssignment() != null) {
                            potentialUnits.add(unit.getTransportShipAssignment().getTransportShip());
                        }
                    }
                }
                for (Unit unit : potentialUnits) {
                    if ((sft.getAllowedUnitType() == 11) && (!campaign.getCampaignOptions().isUseDropShips())) {
                        continue;
                    }

                    // if it's the right type of unit and is around
                    if (forceCompositionMatchesDeclaredUnitType(unit.getEntity().getUnitType(),
                            sft.getAllowedUnitType(), false) &&
                            unit.isAvailable() && unit.isFunctional()) {

                        // add the unit to the scenario and bench the appropriate bot unit if one is
                        // present
                        scenario.addUnit(unit, sft.getForceName(), false);
                        AtBDynamicScenarioFactory.benchAllyUnit(unit.getId(), sft.getForceName(),
                                scenario.getBackingScenario());
                        unitCount--;

                        // once we've supplied enough units, end the process
                        if (unitCount == 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates a StratCon scenario for forces already existing at the given coordinates on the
     * provided track.
     *
     * @param scenarioCoords    The coordinates where the scenario will be placed on the track.
     * @param forceIDs          The set of force IDs (ideally for the forces already at the
     *                          specified location).
     * @param contract          The contract associated with the current scenario.
     * @param campaign          The current campaign.
     * @param track             The relevant StratCon track.
     * @return The newly generated {@link StratconScenario}.
     */
    public static @Nullable StratconScenario generateScenarioForExistingForces(StratconCoords scenarioCoords,
                                    Set<Integer> forceIDs, AtBContract contract, Campaign campaign,
                                    StratconTrackState track) {
        return generateScenarioForExistingForces(scenarioCoords, forceIDs, contract, campaign,
            track, null);
    }

    /**
     * Generates a StratCon scenario for forces already existing at the given coordinates on the
     * provided track. This method allows us to specify a specific scenario template.
     *
     * @param scenarioCoords    The coordinates where the scenario will be placed on the track.
     * @param forceIDs          The set of force IDs (ideally for the forces already at the
     *                          specified location).
     * @param contract          The contract associated with the current scenario.
     * @param campaign          The current campaign.
     * @param track             The relevant StratCon track.
     * @param template          A specific {@link ScenarioTemplate} to use, or {@code null} to
     *                          select a random template.
     * @return The newly generated {@link StratconScenario}.
     */
    public static @Nullable StratconScenario generateScenarioForExistingForces(StratconCoords scenarioCoords,
                                    Set<Integer> forceIDs, AtBContract contract, Campaign campaign,
                                    StratconTrackState track, @Nullable ScenarioTemplate template) {
        boolean firstForce = true;
        StratconScenario scenario = null;

        for (int forceID : forceIDs) {
            if (firstForce) {
                scenario = setupScenario(scenarioCoords, forceID, campaign, contract, track, template);
                firstForce = false;

                if (scenario == null) {
                    return null;
                }
            } else {
                scenario.incrementRequiredPlayerLances();
                scenario.addPrimaryForce(forceID);
            }
        }

        // this is theoretically possible if forceIDs is empty - not likely in practice
        // but might as well, to future-proof.
        if (scenario != null) {
            scenario.setIgnoreForceAutoAssignment(true);
        }

        return scenario;
    }

    /**
     * Deploys a force to the given coordinates on the given track as a result of
     * explicit player
     * action.
     */
    public static void deployForceToCoords(StratconCoords coords, int forceID, Campaign campaign, AtBContract contract,
            StratconTrackState track, boolean sticky) {
        // the following things should happen:
        // 1. call to "process force deployment", which reveals fog of war in or around
        // the coords, depending on force role
        // 2. if coords are a hostile facility, we get a facility scenario
        // 3. if coords are empty, we *may* get a scenario

        processForceDeployment(coords, forceID, campaign, track, sticky);

        // we may stumble on a fixed objective scenario - in that case assign the force
        // to it and finalize
        // we also will not be encountering any of the other stuff so bug out afterwards
        StratconScenario revealedScenario = track.getScenario(coords);
        if (revealedScenario != null) {
            revealedScenario.addPrimaryForce(forceID);
            AtBDynamicScenarioFactory.finalizeScenario(revealedScenario.getBackingScenario(), contract, campaign);
            setScenarioParametersFromBiome(track, revealedScenario);
            commitPrimaryForces(campaign, revealedScenario, track);
            return;
        }

        // don't create a scenario on top of allied facilities
        StratconFacility facility = track.getFacility(coords);
        boolean isNonAlliedFacility = (facility != null) && (facility.getOwner() != ForceAlignment.Allied);
        int targetNum = calculateScenarioOdds(track, contract, true);
        boolean spawnScenario = (facility == null) && (Compute.randomInt(100) <= targetNum);

        if (isNonAlliedFacility || spawnScenario) {
            StratconScenario scenario = setupScenario(coords, forceID, campaign, contract, track);
            // we deploy immediately in this case, since we deployed the force manually
            setScenarioDates(0, track, campaign, scenario);
            AtBDynamicScenarioFactory.finalizeScenario(scenario.getBackingScenario(), contract, campaign);
            setScenarioParametersFromBiome(track, scenario);

            // if we wound up with a field scenario, we may sub in dropships carrying
            // units of the force in question
            if (spawnScenario && !isNonAlliedFacility) {
                swapInPlayerUnits(scenario, campaign, forceID);
            }

            commitPrimaryForces(campaign, scenario, track);
        }
    }

    /**
     * Sets up a StratCon scenario with the given parameters.
     *
     * @param coords    The coordinates where the scenario is to be placed on the track.
     * @param forceID   The ID of the forces involved in the scenario.
     * @param campaign  The current campaign.
     * @param contract  The contract associated with the current scenario.
     * @param track     The relevant StratCon track.
     * @return The newly set up {@link StratconScenario}.
     */
    private static @Nullable StratconScenario setupScenario(StratconCoords coords, int forceID, Campaign campaign,
                                                  AtBContract contract, StratconTrackState track) {
        return setupScenario(coords, forceID, campaign, contract, track, null);
    }

    /**
     * Sets up a Stratcon scenario with the given parameters optionally allowing use a specific scenario template.
     * <p>
     * If a facility is already present at the provided coordinates, the scenario will be setup for that facility.
     * If there is no facility, a new scenario will be generated; if the ScenarioTemplate argument provided was non-null,
     * it will be used, else a randomly selected scenario will be generated.
     * In case the generated scenario turns out to be a facility scenario, a new facility will be added to the track at
     * the provided coordinates and setup for that facility.
     *
     * @param coords    The coordinates where the scenario is to be placed on the track.
     * @param forceID   The ID of the forces involved in the scenario.
     * @param campaign  The current campaign.
     * @param contract  The contract associated with the current scenario.
     * @param track     The relevant StratCon track.
     * @param template  A specific {@link ScenarioTemplate} to use for scenario setup, or
     *                  {@code null} to select the scenario template randomly.
     * @return The newly set up {@link StratconScenario}.
     */
    private static @Nullable StratconScenario setupScenario(StratconCoords coords, int forceID, Campaign campaign,
                                                  AtBContract contract, StratconTrackState track,
                                                  @Nullable ScenarioTemplate template) {
        StratconScenario scenario;

        if (track.getFacilities().containsKey(coords)) {
            StratconFacility facility = track.getFacility(coords);
            boolean alliedFacility = facility.getOwner() == ForceAlignment.Allied;
            template = StratconScenarioFactory.getFacilityScenario(alliedFacility);
            scenario = generateScenario(campaign, contract, track, forceID, coords, template);
            setupFacilityScenario(scenario, facility);
        } else {
            if (template != null) {
                scenario = generateScenario(campaign, contract, track, forceID, coords, template);
            } else {
                scenario = generateScenario(campaign, contract, track, forceID, coords);
            }

            if (scenario == null) {
                return null;
            }

            // we may generate a facility scenario randomly - if so, do the facility-related
            // stuff and add a new facility to the track
            if (scenario.getBackingScenario().getTemplate().isFacilityScenario()) {
                StratconFacility facility = scenario.getBackingScenario().getTemplate().isHostileFacility()
                        ? StratconFacilityFactory.getRandomHostileFacility()
                        : StratconFacilityFactory.getRandomAlliedFacility();
                facility.setVisible(true);
                track.addFacility(coords, facility);
                setupFacilityScenario(scenario, facility);
            }
        }

        return scenario;
    }

    /**
     * carries out tasks relevant to facility scenarios
     */
    private static void setupFacilityScenario(StratconScenario scenario, StratconFacility facility) {
        // this includes:
        // for hostile facilities
        // - add a destroy objective (always the option to level the facility)
        // - add a capture objective (always the option to capture the facility)
        // - if so indicated by parameter, roll a random hostile facility objective and
        // add it if not capture/destroy
        // for allied facilities
        // - add a defend objective (always the option to defend the facility)
        // - if so indicated by parameter, roll a random allied facility objective and
        // add it if not defend
        AtBScenarioModifier objectiveModifier = null;
        boolean alliedFacility = facility.getOwner() == ForceAlignment.Allied;

        objectiveModifier = alliedFacility ? AtBScenarioModifier.getRandomAlliedFacilityModifier()
                : AtBScenarioModifier.getRandomHostileFacilityModifier();

        if (objectiveModifier != null) {
            scenario.getBackingScenario().addScenarioModifier(objectiveModifier);
            scenario.getBackingScenario().setName(String.format("%s - %s - %s", facility.getFacilityType(),
                    alliedFacility ? "Allied" : "Hostile", objectiveModifier.getModifierName()));
        }

        // add the "fixed" hostile facility modifiers after the primary ones
        if (!alliedFacility) {
            for (AtBScenarioModifier modifier : AtBScenarioModifier.getRequiredHostileFacilityModifiers()) {
                if (!scenario.getBackingScenario().alreadyHasModifier(modifier)) {
                    scenario.getBackingScenario().addScenarioModifier(modifier);
                }
            }
        }
    }

    /**
     * Applies time-sensitive facility effects.
     */
    private static void processFacilityEffects(StratconTrackState track,
            StratconCampaignState campaignState, boolean isStartOfMonth) {
        for (StratconFacility facility : track.getFacilities().values()) {
            if (isStartOfMonth) {
                campaignState.addSupportPoints(facility.getMonthlySPModifier());
            }
        }
    }

    /**
     * Process the deployment of a force to the given coordinates on the given
     * track.
     * This does not include assigning the force to any scenarios
     */
    public static void processForceDeployment(StratconCoords coords, int forceID, Campaign campaign,
            StratconTrackState track, boolean sticky) {
        // plan of action:
        // increase fatigue if the coordinates are not currently unrevealed
        // reveal deployed coordinates
        // reveal facility in deployed coordinates (and all adjacent coordinates for
        // scout lances)
        // reveal scenario in deployed coordinates (and all adjacent coordinates for
        // scout lances)

        // we want to ensure we only increase Fatigue once
        boolean hasFatigueIncreased = false;

        if (!track.getRevealedCoords().contains(coords)) {
            increaseFatigue(forceID, campaign);
            hasFatigueIncreased = true;
        }

        track.getRevealedCoords().add(coords);

        StratconFacility facility = track.getFacility(coords);
        if (facility != null) {
            facility.setVisible(true);
        }

        StratconScenario scenario = track.getScenario(coords);
        // if we're deploying on top of a scenario and it's "cloaked"
        // then we have to activate it
        if ((scenario != null) && scenario.getBackingScenario().isCloaked()) {
            scenario.getBackingScenario().setCloaked(false);
            setScenarioDates(0, track, campaign, scenario); // must be called before commitPrimaryForces
            MekHQ.triggerEvent(new ScenarioChangedEvent(scenario.getBackingScenario()));
        }

        if (campaign.getStrategicFormationsTable().get(forceID).getRole().isScouting()) {
            for (int direction = 0; direction < 6; direction++) {
                StratconCoords checkCoords = coords.translate(direction);

                facility = track.getFacility(checkCoords);
                if (facility != null) {
                    facility.setVisible(true);
                }

                scenario = track.getScenario(checkCoords);
                // if we've revealed a scenario and it's "cloaked"
                // we have to activate it
                if ((scenario != null) && scenario.getBackingScenario().isCloaked()) {
                    scenario.getBackingScenario().setCloaked(false);
                    setScenarioDates(0, track, campaign, scenario);
                    MekHQ.triggerEvent(new ScenarioChangedEvent(scenario.getBackingScenario()));
                }

                if ((!track.getRevealedCoords().contains(checkCoords)) && (!hasFatigueIncreased)) {
                    increaseFatigue(forceID, campaign);
                    hasFatigueIncreased = true;
                }

                track.getRevealedCoords().add(coords.translate(direction));
            }
        }

        // the force may be located in other places on the track - clear it out
        track.unassignForce(forceID);
        track.assignForce(forceID, coords, campaign.getLocalDate(), sticky);
        MekHQ.triggerEvent(new StratconDeploymentEvent(campaign.getForce(forceID)));
    }

    /**
     * Increases the fatigue for all crew members per Unit in a force.
     *
     * @param forceID  the ID of the force
     * @param campaign the campaign
     */
    private static void increaseFatigue(int forceID, Campaign campaign) {
        for (UUID unit : campaign.getForce(forceID).getAllUnits(false)) {
            for (Person person : campaign.getUnit(unit).getCrew()) {
                person.increaseFatigue(campaign.getCampaignOptions().getFatigueRate());

                if (campaign.getCampaignOptions().isUseFatigue()) {
                    Fatigue.processFatigueActions(campaign, person);
                }
            }
        }
    }

    /**
     * Worker function that processes the effects of deploying a reinforcement force to a scenario
     *
     * @param reinforcementType the type of reinforcement being deployed
     * @param campaignState the state of the campaign
     * @param scenario the current scenario
     * @param campaign the campaign instance
     * @return {@code true} if the reinforcement deployment is successful, {@code false} otherwise
     */
    public static boolean processReinforcementDeployment(ReinforcementEligibilityType reinforcementType,
            StratconCampaignState campaignState, StratconScenario scenario, Campaign campaign) {
        // if the force is already deployed to the track, we're done
        // if the force is a fight lance or we're using a support point
        // if there is an SP to burn, burn it and we're done
        // if there is a VP to burn, burn it and we're done
        // now, roll 2d6 + lance commander tactics
        // 9+ = deploy
        // 6+ = deploy, apply negative modifier to scenario
        // 2+ = fail to deploy, apply negative modifier to scenario; if fight lance,
        // treat as 6+

        switch (reinforcementType) {
            case FightLance:
            case SupportPoint:
                if (campaignState.getSupportPoints() > 0) {
                    campaignState.useSupportPoint();
                    return true;
                }

                int tactics = scenario.getBackingScenario().getLanceCommanderSkill(SkillType.S_TACTICS, campaign);
                int roll = Compute.d6(2);
                int result = roll + tactics;

                StringBuilder reportStatus = new StringBuilder();
                reportStatus
                        .append(String.format("Attempting to reinforce scenario %s without SP/VP, roll 2d6 + %d: %d",
                                scenario.getName(), tactics, result));

                // fail to reinforce
                if ((result < 6) && (reinforcementType != ReinforcementEligibilityType.FightLance)) {
                    reportStatus.append(" - reinforcement attempt failed.");
                    campaign.addReport(reportStatus.toString());
                    return false;
                    // succeed but get an extra negative event added to the scenario
                } else if (result < 9) {
                    MapLocation mapLocation = scenario.getScenarioTemplate().mapParameters.getMapLocation();
                    AtBScenarioModifier scenarioModifier = AtBScenarioModifier.getRandomBattleModifier(mapLocation,
                            false);

                    // keep rolling until we get an applicable one
                    // TODO: have the AtBScenarioModifier sort these out instead for performance?
                    while (scenarioModifier.getEventTiming() != EventTiming.PostForceGeneration) {
                        scenarioModifier = AtBScenarioModifier.getRandomBattleModifier(mapLocation, false);
                    }

                    scenarioModifier.processModifier(scenario.getBackingScenario(), campaign,
                            EventTiming.PostForceGeneration);

                    reportStatus.append(String.format(
                            " - reinforcement attempt succeeded; extra negative modifier (%s) applied to scenario.",
                            scenarioModifier.getModifierName()));
                    campaign.addReport(reportStatus.toString());
                    return true;
                    // succeed without reservation
                } else {
                    reportStatus.append(" - reinforcement attempt succeeded;");
                    campaign.addReport(reportStatus.toString());
                    return true;
                }
            case ChainedScenario:
                return true;
            case None:
            default:
                return false;
        }
    }

    /**
     * Assigns a force to the scenario such that the majority of the force can be
     * deployed
     */
    private static void assignAppropriateExtraForceToScenario(StratconScenario scenario,
            Map<MapLocation, List<Integer>> sortedAvailableForceIDs) {
        // the goal of this function is to avoid assigning ground units to air battles
        // and ground units/conventional fighters to space battle

        List<MapLocation> mapLocations = new ArrayList<>();
        mapLocations.add(Space); // can always add ASFs

        MapLocation scenarioMapLocation = scenario.getScenarioTemplate().mapParameters.getMapLocation();

        if (scenarioMapLocation == LowAtmosphere) {
            mapLocations.add(LowAtmosphere); // can add conventional fighters to ground or low atmo battles
        }

        if ((scenarioMapLocation == AllGroundTerrain)
                || (scenarioMapLocation == SpecificGroundTerrain)) {
            mapLocations.add(AllGroundTerrain); // can only add ground units to ground battles
        }

        MapLocation selectedLocation = mapLocations.get(Compute.randomInt(mapLocations.size()));
        List<Integer> forceIDs = sortedAvailableForceIDs.get(selectedLocation);
        int forceIndex = Compute.randomInt(forceIDs.size());
        int forceID = forceIDs.get(forceIndex);
        forceIDs.remove(forceIndex);

        scenario.addPrimaryForce(forceID);
    }

    /**
     * Worker function that "locks in" a scenario - Adds it to the campaign so it's
     * visible in the
     * briefing room, adds it to the track
     */
    public static void commitPrimaryForces(Campaign campaign, StratconScenario scenario,
            StratconTrackState trackState) {
        trackState.addScenario(scenario);

        // set up dates for the scenario if doesn't have them already
        if (scenario.getDeploymentDate() == null) {
            scenario.setDeploymentDate(campaign.getLocalDate());
        }

        if (scenario.getActionDate() == null) {
            scenario.setActionDate(campaign.getLocalDate());
        }

        if (scenario.getReturnDate() == null) {
            scenario.setReturnDate(campaign.getLocalDate().plusDays(trackState.getDeploymentTime()));
        }

        // set the # of rerolls based on the actual lance assigned.
        int tactics = scenario.getBackingScenario().getLanceCommanderSkill(SkillType.S_TACTICS, campaign);
        scenario.getBackingScenario().setRerolls(tactics);
        // The number of defensive points available to a force entering a scenario is
        // 2 x tactics. By default, those points are spent on conventional minefields.
        if (commanderLanceHasDefensiveAssignment(scenario.getBackingScenario(), campaign)) {
            scenario.setNumDefensivePoints(tactics * 2);
            scenario.updateMinefieldCount(Minefield.TYPE_CONVENTIONAL, tactics * 2);
        }

        for (int forceID : scenario.getPlayerTemplateForceIDs()) {
            Force force = campaign.getForce(forceID);
            force.clearScenarioIds(campaign, true);
            force.setScenarioId(scenario.getBackingScenarioID(), campaign);
        }

        scenario.commitPrimaryForces();
    }

    /**
     * Utility method to determine if the current scenario's force commander's force
     * is on defence
     */
    private static boolean commanderLanceHasDefensiveAssignment(AtBDynamicScenario scenario, Campaign campaign) {
        Person lanceCommander = scenario.getLanceCommander(campaign);
        if (lanceCommander != null){
            Unit commanderUnit = lanceCommander.getUnit();
            if (commanderUnit != null) {
                StrategicFormation lance = campaign.getStrategicFormationsTable().get(commanderUnit.getForceId());

                return (lance != null) && lance.getRole().isDefence();
            }
        }

        return false;
    }

    /**
     * A hackish worker function that takes the given list of force IDs and
     * separates it into three
     * sets; one of forces that can be "primary" on a ground map one of forces that
     * can be "primary" on
     * an atmospheric map one of forces that can be "primary" in a space map
     *
     * @param forceIDs List of force IDs to check
     * @return Sorted hash map
     */
    private static Map<MapLocation, List<Integer>> sortForcesByMapType(List<Integer> forceIDs, Campaign campaign) {
        Map<MapLocation, List<Integer>> retVal = new HashMap<>();

        retVal.put(AllGroundTerrain, new ArrayList<>());
        retVal.put(LowAtmosphere, new ArrayList<>());
        retVal.put(Space, new ArrayList<>());

        for (int forceID : forceIDs) {
            switch (campaign.getForce(forceID).getPrimaryUnitType(campaign)) {
                case UnitType.BATTLE_ARMOR:
                case UnitType.INFANTRY:
                case UnitType.MEK:
                case UnitType.TANK:
                case UnitType.PROTOMEK:
                case UnitType.VTOL:
                    retVal.get(AllGroundTerrain).add(forceID);
                    break;
                case UnitType.AEROSPACEFIGHTER:
                    retVal.get(Space).add(forceID);
                    // intentional fallthrough here, ASFs can go to atmospheric maps too
                case UnitType.CONV_FIGHTER:
                    retVal.get(LowAtmosphere).add(forceID);
                    break;
            }
        }
        return retVal;
    }

    /**
     * Worker function that generates stratcon scenario at the given coords, for the
     * given force, on the
     * given track. Also registers it with the track and campaign.
     */
    private static @Nullable StratconScenario generateScenario(Campaign campaign, AtBContract contract, StratconTrackState track,
            int forceID, StratconCoords coords) {
        int unitType = campaign.getForce(forceID).getPrimaryUnitType(campaign);
        ScenarioTemplate template = StratconScenarioFactory.getRandomScenario(unitType);
        // useful for debugging specific scenario types
        // template = StratconScenarioFactory.getSpecificScenario("Defend Grounded
        // Dropship.xml");

        return generateScenario(campaign, contract, track, forceID, coords, template);
    }

    /**
     * Worker function that generates stratcon scenario at the given coords, for the
     * given force, on the
     * given track, using the given template. Also registers it with the campaign.
     */
    static @Nullable StratconScenario generateScenario(Campaign campaign, AtBContract contract, StratconTrackState track,
            int forceID, StratconCoords coords, ScenarioTemplate template) {
        StratconScenario scenario = new StratconScenario();

        if (template == null) {
            int unitType = UnitType.MEK;

            try {
                unitType = campaign.getForce(forceID).getPrimaryUnitType(campaign);
            } catch (NullPointerException ignored) {
                // This just means the player has no units
            }

            template = StratconScenarioFactory.getRandomScenario(unitType);
        }

        if (template == null) {
            logger.error("Failed to fetch random scenario template. Aborting scenario generation.");
            return null;
        }

        AtBDynamicScenario backingScenario = AtBDynamicScenarioFactory.initializeScenarioFromTemplate(template,
                contract, campaign);
        scenario.setBackingScenario(backingScenario);
        scenario.setCoords(coords);

        // by default, certain conditions may make this bigger
        scenario.setRequiredPlayerLances(1);

        // do an appropriate allied force if the contract calls for it
        // do any attached or integrated units
        setAlliedForceModifier(scenario, contract);
        setAttachedUnitsModifier(scenario, contract);
        applyFacilityModifiers(scenario, track, coords);
        applyGlobalModifiers(scenario, contract.getStratconCampaignState());

        if (contract.getCommandRights().isHouse() || contract.getCommandRights().isIntegrated()) {
            scenario.setRequiredScenario(true);
        }

        AtBDynamicScenarioFactory.setScenarioModifiers(campaign.getCampaignOptions(), scenario.getBackingScenario());
        scenario.setCurrentState(ScenarioState.UNRESOLVED);
        setScenarioDates(track, campaign, scenario);

        // the backing scenario ID must be updated after registering the backing
        // scenario
        // with the campaign, so that the stratcon - backing scenario association is
        // maintained
        // registering the scenario with the campaign should be done after setting
        // dates, otherwise, the report messages for new scenarios look weird
        // also, suppress the "new scenario" report if not generating a scenario
        // for a specific force, as this indicates a contract initialization
        campaign.addScenario(backingScenario, contract, forceID == FORCE_NONE);
        scenario.setBackingScenarioID(backingScenario.getId());

        if (forceID > FORCE_NONE) {
            scenario.addPrimaryForce(forceID);
        }

        return scenario;
    }

    /**
     * Apply global scenario modifiers from campaign state to given scenario.
     */
    private static void applyGlobalModifiers(StratconScenario scenario, StratconCampaignState campaignState) {
        for (String modifierName : campaignState.getGlobalScenarioModifiers()) {
            AtBScenarioModifier modifier = AtBScenarioModifier.getScenarioModifier(modifierName);

            if (modifier == null) {
                logger.error(String.format("Modifier %s not found; ignoring", modifierName));
                continue;
            }

            scenario.getBackingScenario().addScenarioModifier(modifier);
        }
    }

    /**
     * Applies scenario modifiers from the current track to the given scenario.
     */
    private static void applyFacilityModifiers(StratconScenario scenario, StratconTrackState track,
            StratconCoords coords) {
        // loop through all the facilities on the track
        // if a facility has been revealed, then it has a 100% chance to apply its
        // effect
        // if a facility has not been revealed, then it has a x% chance to apply its
        // effect
        // where x is the current "aggro rating"
        // if a facility is on the scenario coordinates, then it applies the local
        // effects
        for (StratconCoords facilityCoords : track.getFacilities().keySet()) {
            boolean scenarioAtFacility = facilityCoords.equals(coords);
            StratconFacility facility = track.getFacilities().get(facilityCoords);
            List<String> modifierIDs = new ArrayList<>();

            if (scenarioAtFacility) {
                modifierIDs = facility.getLocalModifiers();
            } else if (facility.isVisible() || (Compute.randomInt(100) <= 75)) {
                modifierIDs = facility.getSharedModifiers();
            }

            for (String modifierID : modifierIDs) {
                AtBScenarioModifier modifier = AtBScenarioModifier.getScenarioModifier(modifierID);
                if (modifier == null) {
                    logger.error(String.format("Modifier %s not found for facility %s", modifierID,
                            facility.getFormattedDisplayableName()));
                    continue;
                }

                modifier.setAdditionalBriefingText(
                        "(from " + facility.getDisplayableName() + ") " +
                                modifier.getAdditionalBriefingText());
                scenario.getBackingScenario().addScenarioModifier(modifier);
            }
        }
    }

    /**
     * Set up the appropriate primary allied force modifier, if any
     *
     * @param contract The scenario's contract.
     */
    private static void setAlliedForceModifier(StratconScenario scenario, AtBContract contract) {
        int alliedUnitOdds = 0;

        // first, we determine the odds of having an allied unit present
        // TODO: move this override out to the contract definition
        if (contract.getContractType().isReliefDuty()) {
            alliedUnitOdds = 50;
        } else {
            switch (contract.getCommandRights()) {
                case INTEGRATED:
                    alliedUnitOdds = 50;
                    break;
                case HOUSE:
                    alliedUnitOdds = 30;
                    break;
                case LIAISON:
                    alliedUnitOdds = 10;
                    break;
                default:
                    break;
            }
        }

        AtBDynamicScenario backingScenario = scenario.getBackingScenario();

        // if an allied unit is present, then we want to make sure that
        // it's ground units for ground battles
        if (Compute.randomInt(100) <= alliedUnitOdds) {
            if ((backingScenario.getTemplate().mapParameters.getMapLocation() == LowAtmosphere)
                    || (backingScenario.getTemplate().mapParameters.getMapLocation() == Space)) {
                backingScenario.addScenarioModifier(
                        AtBScenarioModifier.getScenarioModifier(MHQConstants.SCENARIO_MODIFIER_ALLIED_AIR_UNITS));
            } else {
                backingScenario.addScenarioModifier(
                        AtBScenarioModifier.getScenarioModifier(MHQConstants.SCENARIO_MODIFIER_ALLIED_GROUND_UNITS));
            }
        }
    }

    /**
     * Set the 'attached' units modifier for the current scenario (integrated,
     * house, liaison), and make
     * sure we're not deploying ground units to an air scenario
     *
     * @param contract The scenario's contract
     */
    public static void setAttachedUnitsModifier(StratconScenario scenario, AtBContract contract) {
        AtBDynamicScenario backingScenario = scenario.getBackingScenario();
        boolean airBattle = (backingScenario.getTemplate().mapParameters.getMapLocation() == LowAtmosphere)
                || (backingScenario.getTemplate().mapParameters.getMapLocation() == Space);

        // if we're on cadre duty, we're getting three trainees, period
        if (contract.getContractType().isCadreDuty()) {
            if (airBattle) {
                backingScenario.addScenarioModifier(
                        AtBScenarioModifier.getScenarioModifier(MHQConstants.SCENARIO_MODIFIER_TRAINEES_AIR));
            } else {
                backingScenario.addScenarioModifier(
                        AtBScenarioModifier.getScenarioModifier(MHQConstants.SCENARIO_MODIFIER_TRAINEES_GROUND));
            }
            return;
        }

        // if we're under non-independent command rights, a supervisor may come along
        switch (contract.getCommandRights()) {
            case INTEGRATED:
                backingScenario.addScenarioModifier(AtBScenarioModifier
                        .getScenarioModifier(airBattle ? MHQConstants.SCENARIO_MODIFIER_INTEGRATED_UNITS_AIR
                                : MHQConstants.SCENARIO_MODIFIER_INTEGRATED_UNITS_GROUND));
                break;
            case HOUSE:
                backingScenario.addScenarioModifier(
                        AtBScenarioModifier.getScenarioModifier(airBattle ? MHQConstants.SCENARIO_MODIFIER_HOUSE_CO_AIR
                                : MHQConstants.SCENARIO_MODIFIER_HOUSE_CO_GROUND));
                break;
            case LIAISON:
                if (scenario.isRequiredScenario()) {
                    backingScenario.addScenarioModifier(
                            AtBScenarioModifier
                                    .getScenarioModifier(airBattle ? MHQConstants.SCENARIO_MODIFIER_LIAISON_AIR
                                            : MHQConstants.SCENARIO_MODIFIER_LIAISON_GROUND));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Worker function that sets scenario deploy/battle/return dates based on the
     * track's properties and
     * current campaign date
     */
    private static void setScenarioDates(StratconTrackState track, Campaign campaign, StratconScenario scenario) {
        int deploymentDay = track.getDeploymentTime() < 7 ? Compute.randomInt(7 - track.getDeploymentTime()) : 0;
        setScenarioDates(deploymentDay, track, campaign, scenario);
    }

    /**
     * Worker function that sets scenario deploy/battle/return dates based on the
     * track's properties and
     * current campaign date. Takes a fixed deployment day of X days from campaign's
     * today date.
     */
    private static void setScenarioDates(int deploymentDay, StratconTrackState track, Campaign campaign,
            StratconScenario scenario) {
        // set up deployment day, battle day, return day here
        // safety code to prevent attempts to generate random int with upper bound of 0
        // which is apparently illegal
        int battleDay = deploymentDay
                + (track.getDeploymentTime() > 0 ? Compute.randomInt(track.getDeploymentTime()) : 0);
        int returnDay = deploymentDay + track.getDeploymentTime();

        LocalDate deploymentDate = campaign.getLocalDate().plusDays(deploymentDay);
        LocalDate battleDate = campaign.getLocalDate().plusDays(battleDay);
        LocalDate returnDate = campaign.getLocalDate().plusDays(returnDay);

        scenario.setDeploymentDate(deploymentDate);
        scenario.setActionDate(battleDate);
        scenario.setReturnDate(returnDate);
    }

    /**
     * Helper function that determines if the unit type specified in the given
     * scenario force template
     * would start out airborne on a ground map (hot dropped units aside)
     */
    private static boolean unitTypeIsAirborne(ScenarioForceTemplate template) {
        int unitType = template.getAllowedUnitType();

        return ((unitType == UnitType.AEROSPACEFIGHTER) ||
                (unitType == UnitType.CONV_FIGHTER) ||
                (unitType == UnitType.DROPSHIP) ||
                (unitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX)) &&
                (template.getStartingAltitude() > 0);
    }

    /**
     * Determines whether the force in question has the same primary unit type as
     * the force template.
     *
     * @return Whether or not the unit types match.
     */
    public static boolean forceCompositionMatchesDeclaredUnitType(int primaryUnitType, int unitType,
            boolean reinforcements) {
        // special cases are "ATB_MIX" and "ATB_AERO_MIX", which encompass multiple unit
        // types
        if (unitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX) {
            // "AtB mix" is usually ground units, but air units can sub in
            return (primaryUnitType == UnitType.MEK) || (primaryUnitType == UnitType.TANK)
                    || (primaryUnitType == UnitType.INFANTRY)
                    || (primaryUnitType == UnitType.BATTLE_ARMOR)
                    || (primaryUnitType == UnitType.PROTOMEK)
                    || (primaryUnitType == UnitType.VTOL)
                    || (primaryUnitType == UnitType.AEROSPACEFIGHTER) && reinforcements
                    || (primaryUnitType == UnitType.CONV_FIGHTER) && reinforcements;
        } else if (unitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX) {
            return (primaryUnitType == UnitType.AEROSPACEFIGHTER) || (primaryUnitType == UnitType.CONV_FIGHTER);
        } else {
            return primaryUnitType == unitType;
        }
    }

    /**
     * This is a set of all force IDs for forces that can be deployed to a scenario.
     *
     * @param campaign Current campaign
     * @return List of available force IDs.
     */
    public static List<Integer> getAvailableForceIDs(Campaign campaign) {
        // first, we gather a set of all forces that are already deployed to a track so
        // we eliminate those later
        Set<Integer> forcesInTracks = campaign.getActiveAtBContracts().stream()
                .flatMap(contract -> contract.getStratconCampaignState().getTracks().stream())
                .flatMap(track -> track.getAssignedForceCoords().keySet().stream())
                .collect(Collectors.toSet());

        // now, we get all the forces that qualify as "lances", and filter out those
        // that are
        // deployed to a scenario and not in a track already

        return campaign.getStrategicFormationsTable().keySet().stream()
                .mapToInt(key -> key)
                .mapToObj(campaign::getForce).filter(force -> (force != null)
                        && !force.isDeployed()
                        && force.isCombatForce()
                        && !forcesInTracks.contains(force.getId()))
                .map(Force::getId)
                .collect(Collectors.toList());
    }

    /**
     * This is a list of all force IDs for forces that can be deployed to a scenario
     * in the given force template a) have not been assigned to a track b) are combat-capable c) are
     * not deployed to a scenario d) if attempting to deploy as reinforcements, haven't already failed
     * to deploy
     */
    public static List<Integer> getAvailableForceIDs(int unitType, Campaign campaign, StratconTrackState currentTrack,
            boolean reinforcements, @Nullable StratconScenario currentScenario, StratconCampaignState campaignState) {
        List<Integer> retVal = new ArrayList<>();

        // assemble a set of all force IDs that are currently assigned to tracks that are not this one
        Set<Integer> forcesInTracks = campaign.getActiveAtBContracts().stream()
                .flatMap(contract -> contract.getStratconCampaignState().getTracks().stream())
                .filter(track -> (!Objects.equals(track, currentTrack)) || !reinforcements)
                .flatMap(track -> track.getAssignedForceCoords().keySet().stream())
                .collect(Collectors.toSet());

        // if there's an existing scenario, and we're doing reinforcements,
        // prevent forces that failed to deploy from trying to deploy again
        if (reinforcements && (currentScenario != null)) {
            forcesInTracks.addAll(currentScenario.getFailedReinforcements());
        }

        for (StrategicFormation formation : campaign.getStrategicFormationsTable().values()) {
            Force force = campaign.getForce(formation.getForceId());

            if (force == null) {
                continue;
            }

            int primaryUnitType = force.getPrimaryUnitType(campaign);
            boolean noReinforcementRestriction = !reinforcements ||
                (getReinforcementType(force.getId(), currentTrack, campaign, campaignState) != ReinforcementEligibilityType.None);

            if ((force.getScenarioId() <= 0)
                && !force.getAllUnits(true).isEmpty()
                && !forcesInTracks.contains(force.getId())
                && forceCompositionMatchesDeclaredUnitType(primaryUnitType, unitType, reinforcements)
                && noReinforcementRestriction
                && !subElementsOrSelfDeployed(force, campaign)) {

                retVal.add(force.getId());
            }
        }

        return retVal;
    }

    /**
     * Returns true if any sub-element (unit or sub-force) of this force is
     * deployed.
     */
    private static boolean subElementsOrSelfDeployed(Force force, Campaign campaign) {
        if (force.isDeployed()) {
            return true;
        }

        if (force.getUnits().stream()
                .map(campaign::getUnit)
                .anyMatch(Unit::isDeployed)) {
            return true;
        }

        return force.getSubForces().stream()
                .anyMatch(child -> subElementsOrSelfDeployed(child, campaign));
    }

    /**
     * Returns a list of individual units eligible for deployment in scenarios run
     * by "Defend" lances
     *
     * @return List of unit IDs.
     */
    public static List<Unit> getEligibleDefensiveUnits(Campaign campaign) {
        List<Unit> retVal = new ArrayList<>();

        for (Unit u : campaign.getUnits()) {
            // "defensive" units are infantry, battle armor and (Weisman help you) gun
            // emplacements
            // and also said unit should be intact/alive/etc
            boolean isEligibleInfantry = ((u.getEntity().getUnitType() == UnitType.INFANTRY)
                    || (u.getEntity().getUnitType() == UnitType.BATTLE_ARMOR)) && !u.isUnmanned();

            boolean isEligibleGunEmplacement = u.getEntity().getUnitType() == UnitType.GUN_EMPLACEMENT;

            if ((isEligibleInfantry || isEligibleGunEmplacement)
                    && !u.isDeployed()
                    && !u.isMothballed()
                    && (u.checkDeployment() == null)
                    && !isUnitDeployedToStratCon(u)) {

                // this is a little inefficient, but probably there aren't too many active AtB
                // contracts at a time
                for (AtBContract contract : campaign.getActiveAtBContracts()) {
                    if (contract.getStratconCampaignState().isForceDeployedHere(u.getForceId())) {
                        continue;
                    }
                }

                retVal.add(u);
            }
        }

        return retVal;
    }

    /**
     * Returns a list of individual units eligible for deployment in scenarios that
     * result from the
     * lance leader having a leadership score
     *
     * @return List of unit IDs.
     */
    public static List<Unit> getEligibleLeadershipUnits(Campaign campaign, Set<Integer> forceIDs) {
        List<Unit> retVal = new ArrayList<>();

        // The criteria are as follows:
        // - unit is of a different unit type than the primary unit type of the force
        // - unit has a lower BV than the force's lowest BV unit

        Integer lowestBV = getLowestBV(campaign, forceIDs);

        // no units assigned, the rest is meaningless.
        if (lowestBV == null) {
            return retVal;
        }

        int primaryUnitType = getPrimaryUnitType(campaign, forceIDs);
        int generalUnitType = StratconScenarioFactory.convertSpecificUnitTypeToGeneral(primaryUnitType);

        for (Unit u : campaign.getUnits()) {
            // the general idea is that we want a different unit type than the primary
            // but also something that can be deployed to the scenario -
            // e.g. no infantry on air scenarios etc.
            boolean validUnitType = (primaryUnitType != u.getEntity().getUnitType()) &&
                    forceCompositionMatchesDeclaredUnitType(u.getEntity().getUnitType(), generalUnitType, true);

            if (validUnitType && !u.isDeployed() && !u.isMothballed()
                    && (u.getEntity().calculateBattleValue() < lowestBV)
                    && (u.checkDeployment() == null)
                    && !isUnitDeployedToStratCon(u)) {
                retVal.add(u);
            }
        }

        return retVal;
    }

    /**
     * Check if the unit's force (if one exists) has been deployed to a StratCon
     * track
     */
    public static boolean isUnitDeployedToStratCon(Unit u) {
        if (!u.getCampaign().getCampaignOptions().isUseStratCon()) {
            return false;
        }

        // this is a little inefficient, but probably there aren't too many active AtB
        // contracts at a time
        return u.getCampaign().getActiveAtBContracts().stream()
                .anyMatch(contract -> (contract.getStratconCampaignState() != null) &&
                        contract.getStratconCampaignState().isForceDeployedHere(u.getForceId()));
    }

    /**
     * Given a campaign and a list of force IDs, calculate the unit with the lowest
     * BV.
     */
    private static Integer getLowestBV(Campaign campaign, Set<Integer> forceIDs) {
        Integer lowestBV = null;

        for (int forceID : forceIDs) {
            Force force = campaign.getForce(forceID);
            if (force == null) {
                continue;
            }

            for (UUID id : force.getUnits()) {
                if (campaign.getUnit(id) == null) {
                    continue;
                }

                int currentBV = campaign.getUnit(id).getEntity().calculateBattleValue();

                if ((lowestBV == null) || (currentBV < lowestBV)) {
                    lowestBV = currentBV;
                }
            }
        }

        return lowestBV;
    }

    /**
     * Calculates the majority unit type for the forces given the IDs.
     */
    private static int getPrimaryUnitType(Campaign campaign, Set<Integer> forceIDs) {
        Map<Integer, Integer> unitTypeBuckets = new TreeMap<>();
        int biggestBucketID = -1;
        int biggestBucketCount = 0;

        for (int forceID : forceIDs) {
            Force force = campaign.getForce(forceID);
            if (force == null) {
                continue;
            }

            for (UUID id : force.getUnits()) {
                Unit unit = campaign.getUnit(id);
                if ((unit == null) || (unit.getEntity() == null)) {
                    continue;
                }

                int unitType = unit.getEntity().getUnitType();

                unitTypeBuckets.merge(unitType, 1, Integer::sum);

                if (unitTypeBuckets.get(unitType) > biggestBucketCount) {
                    biggestBucketCount = unitTypeBuckets.get(unitType);
                    biggestBucketID = unitType;
                }
            }
        }

        return biggestBucketID;
    }

    /**
     * Determines what rules to use when deploying a force for reinforcements to the
     * given track.
     */
    public static ReinforcementEligibilityType getReinforcementType(int forceID, StratconTrackState trackState,
            Campaign campaign, StratconCampaignState campaignState) {
        // if the force is deployed elsewhere, it cannot be deployed as reinforcements
        if (campaign.getActiveAtBContracts().stream()
                .flatMap(contract -> contract.getStratconCampaignState().getTracks().stream())
                .anyMatch(track -> !Objects.equals(track, trackState)
                        && track.getAssignedForceCoords().containsKey(forceID))) {
            return ReinforcementEligibilityType.None;
        }

        // TODO: If the force has completed a scenario which allows it,
        // it can deploy "for free" (ReinforcementEligibilityType.ChainedScenario)

        // if the force is in 'fight' stance, it'll be able to deploy using 'fight
        // lance' rules
        if (campaign.getStrategicFormationsTable().containsKey(forceID)
                && (campaign.getStrategicFormationsTable().get(forceID).getRole().isFighting())) {
            return ReinforcementEligibilityType.FightLance;
        }

        // otherwise, the force requires support points to deploy
        if (campaignState.getSupportPoints() > 0) {
            return ReinforcementEligibilityType.SupportPoint;
        }

        /// if we don't have any of these things, it can't be deployed
        return ReinforcementEligibilityType.None;
    }

    /**
     * Can any force be manually deployed to the given coordinates on the given
     * track
     * for the given contract?
     */
    public static boolean canManuallyDeployAnyForce(StratconCoords coords,
            StratconTrackState track, AtBContract contract) {
        // Rules: can't manually deploy under integrated command
        // can't manually deploy if there's already a force deployed there
        // exception: on allied facilities
        // can't manually deploy if there's a non-cloaked scenario

        if (contract.getCommandRights().isIntegrated()) {
            return false;
        }

        StratconScenario scenario = track.getScenario(coords);
        boolean nonCloakedOrNoscenario = (scenario == null) || scenario.getBackingScenario().isCloaked();

        StratconFacility facility = track.getFacility(coords);
        boolean alliedFacility = (facility != null) && (facility.getOwner() == ForceAlignment.Allied);

        return (!track.areAnyForceDeployedTo(coords) || alliedFacility) && nonCloakedOrNoscenario;
    }

    /**
     * Given a track and the current campaign state, and if the player is deploying
     * a force or not,
     * figure out the odds of a scenario occurring.
     */
    public static int calculateScenarioOdds(StratconTrackState track, AtBContract contract,
            boolean isReinforcements) {
        if (contract.getMoraleLevel().isRouted()) {
            return 0;
        }

        int moraleModifier = switch (contract.getMoraleLevel()) {
            case CRITICAL -> {
                if (isReinforcements) {
                    yield -10;
                } else {
                    yield 0;
                }
            }
            case WEAKENED -> -5;
            case ADVANCING -> 5;
            case DOMINATING -> {
                if (isReinforcements) {
                    yield 20;
                } else {
                    yield 10;
                }
            }
            case OVERWHELMING -> {
                if (isReinforcements) {
                    yield 50;
                } else {
                    yield 25;
                }
            }
            default -> 0;
        };

        int dataCenterModifier = track.getScenarioOddsAdjustment();

        return track.getScenarioOdds() + moraleModifier + dataCenterModifier;
    }

    /**
     * Removes the facility associated with the given scenario from the relevant
     * track
     */
    public static void updateFacilityForScenario(AtBScenario scenario, AtBContract contract, boolean destroy,
            boolean capture) {
        if (contract.getStratconCampaignState() == null) {
            return;
        }

        // this is kind of kludgy, but there's currently no way to link a scenario back
        // to its backing scenario
        // TODO: introduce mapping in contract or at least trackstate
        // basically, we're looping through all scenarios on all the contract's tracks
        // if we find one with the same ID as the one being resolved, that's our
        // facility: get rid of it.
        for (StratconTrackState trackState : contract.getStratconCampaignState().getTracks()) {
            for (StratconCoords coords : trackState.getScenarios().keySet()) {
                StratconScenario potentialScenario = trackState.getScenario(coords);
                if (potentialScenario.getBackingScenarioID() == scenario.getId()) {
                    if (destroy) {
                        trackState.removeFacility(coords);
                    } else {
                        StratconFacility facility = trackState.getFacility(coords);

                        if (facility == null) {
                            continue;
                        }

                        if (capture) {
                            facility.incrementOwnershipChangeScore();
                        } else {
                            facility.decrementOwnershipChangeScore();
                        }
                    }

                    break;
                }
            }
        }
    }

    /**
     * Processes completion of a Stratcon scenario, if the given tracker is
     * associated with a
     * stratcon-enabled mission. Intended to be called after
     * ResolveScenarioTracker.finish()
     * has been invoked.
     */
    public static void processScenarioCompletion(ResolveScenarioTracker tracker) {
        Campaign campaign = tracker.getCampaign();
        Mission mission = tracker.getMission();

        if (mission instanceof AtBContract) {
            StratconCampaignState campaignState = ((AtBContract) mission).getStratconCampaignState();
            if (campaignState == null) {
                return;
            }

            Scenario backingScenario = tracker.getScenario();

            boolean victory = backingScenario.getStatus().isOverallVictory();

            for (StratconTrackState track : campaignState.getTracks()) {
                if (track.getBackingScenariosMap().containsKey(backingScenario.getId())) {
                    // things that may potentially happen:
                    // scenario is removed from track - implemented
                    // track gets remaining forces added to reinforcement pool
                    // facility gets remaining forces stored in reinforcement pool
                    // process VP and SO

                    StratconScenario scenario = track.getBackingScenariosMap().get(backingScenario.getId());

                    StratconFacility facility = track.getFacility(scenario.getCoords());

                    if (scenario.isRequiredScenario() && !backingScenario.getStatus().isDraw()) {
                        campaignState.updateVictoryPoints(victory ? 1 : -1);
                    }

                    // this must be done before removing the scenario from the track
                    // in case any objectives are linked to the scenario's coordinates
                    updateStrategicObjectives(victory, scenario, track);

                    if ((facility != null) && (facility.getOwnershipChangeScore() > 0)) {
                        switchFacilityOwner(facility);
                    }

                    processTrackForceReturnDates(track, campaign);

                    track.removeScenario(scenario);
                    break;
                }
            }
        }
    }

    /**
     * Worker function that updates strategic objectives relevant to the passed in
     * scenario, track and campaign state. For example, "win scenario A" or "win X
     * scenarios".
     */
    private static void updateStrategicObjectives(boolean victory, StratconScenario scenario,
            StratconTrackState track) {

        // first, we check if this scenario is associated with any specific scenario
        // objectives
        StratconStrategicObjective specificObjective = track.getObjectivesByCoords().get(scenario.getCoords());
        if ((specificObjective != null) &&
                (specificObjective.getObjectiveType() == StrategicObjectiveType.SpecificScenarioVictory)) {

            if (victory) {
                specificObjective.incrementCurrentObjectiveCount();
            } else {
                specificObjective.setCurrentObjectiveCount(StratconStrategicObjective.OBJECTIVE_FAILED);
            }
        }

        // "any scenario victory" is not linked to any specific coordinates, so we have
        // to
        // search through the track's objectives and update those.
        for (StratconStrategicObjective objective : track.getStrategicObjectives()) {
            if ((objective.getObjectiveType() == StrategicObjectiveType.AnyScenarioVictory) && victory) {
                objective.incrementCurrentObjectiveCount();
            }
        }
    }

    /**
     * Contains logic for what should happen when a facility gets captured:
     * modifier/type/alignment switches etc.
     */
    public static void switchFacilityOwner(StratconFacility facility) {
        if ((facility.getCapturedDefinition() != null) && !facility.getCapturedDefinition().isBlank()) {
            StratconFacility newOwnerData = StratconFacilityFactory.getFacilityByName(facility.getCapturedDefinition());

            if (newOwnerData != null) {
                facility.copyRulesDataFrom(newOwnerData);
                return;
            }
        }

        // if we the facility didn't have any data defined for what happens when it's
        // captured
        // fall back to the default of just switching the owner
        if (facility.getOwner() == ForceAlignment.Allied) {
            facility.setOwner(ForceAlignment.Opposing);
        } else {
            facility.setOwner(ForceAlignment.Allied);
        }
    }

    /**
     * Worker function that goes through a track and undeploys any forces where the
     * return date is on or before the given date.
     */
    public static void processTrackForceReturnDates(StratconTrackState track, Campaign campaign) {
        List<Integer> forcesToUndeploy = new ArrayList<>();
        LocalDate date = campaign.getLocalDate();

        // for each force on the track, if the return date is today or in the past,
        // and the scenario has not yet occurred, undeploy it.
        // "return to base", unless it's been told to stay in the field
        for (int forceID : track.getAssignedForceReturnDates().keySet()) {
            Force force = campaign.getForce(forceID);

            if ((track.getAssignedForceReturnDates().get(forceID).equals(date)
                    || track.getAssignedForceReturnDates().get(forceID).isBefore(date))
                    && (force != null) && !track.getBackingScenariosMap().containsKey(force.getScenarioId())
                    && !track.getStickyForces().contains(forceID)) {
                forcesToUndeploy.add(forceID);
            }
        }

        for (int forceID : forcesToUndeploy) {
            track.unassignForce(forceID);
        }
    }

    /**
     * Processes an ignored dynamic scenario - locates it on one of the tracks and
     * calls the standared
     * 'ignored scenario' routine.
     *
     * @return Whether or not we also need to get rid of the backing scenario from
     *         the campaign
     */
    public static boolean processIgnoredScenario(AtBDynamicScenario scenario, StratconCampaignState campaignState) {
        return campaignState.getTracks().stream()
                .filter(track -> track.getBackingScenariosMap().containsKey(scenario.getId()))
                .findFirst()
                .map(track -> processIgnoredScenario(track.getBackingScenariosMap().get(scenario.getId()),
                        campaignState))
                .orElse(true);

    }

    /**
     * Processes an ignored Stratcon scenario
     *
     * @return Whether or not we also need to get rid of the backing scenario from
     *         the campaign
     */
    public static boolean processIgnoredScenario(StratconScenario scenario, StratconCampaignState campaignState) {
        for (StratconTrackState track : campaignState.getTracks()) {
            if (track.getScenarios().containsKey(scenario.getCoords())) {
                // subtract VP if scenario is 'required'
                if (scenario.isRequiredScenario()) {
                    campaignState.updateVictoryPoints(-1);
                }

                track.removeScenario(scenario);

                StratconFacility localFacility = track.getFacility(scenario.getCoords());
                if (localFacility != null) {
                    // if the ignored scenario was on top of an allied facility
                    // then it'll get captured, and the player will possibly lose a SO
                    if (localFacility.getOwner() == ForceAlignment.Allied) {
                        localFacility.setOwner(ForceAlignment.Opposing);
                    }

                    return true;
                } else {
                    // if it's an open-field
                    // move scenario towards nearest allied facility
                    StratconCoords closestAlliedFacilityCoords = track
                            .findClosestAlliedFacilityCoords(scenario.getCoords());

                    if (closestAlliedFacilityCoords != null) {
                        StratconCoords newCoords = scenario.getCoords()
                                .translate(scenario.getCoords().direction(closestAlliedFacilityCoords));

                        boolean objectiveMoved = track.moveObjective(scenario.getCoords(), newCoords);
                        if (!objectiveMoved) {
                            track.failObjective(scenario.getCoords());
                        }

                        scenario.setCoords(newCoords);

                        int daysForward = Math.max(1, track.getDeploymentTime());

                        scenario.setDeploymentDate(scenario.getDeploymentDate().plusDays(daysForward));
                        scenario.setActionDate(scenario.getActionDate().plusDays(daysForward));
                        scenario.setReturnDate(scenario.getReturnDate().plusDays(daysForward));

                        // refresh the scenario's position on the track
                        track.addScenario(scenario);

                        // TODO: Write some functionality to "copy" a scenario's bot forces
                        // over between scenarios

                        // TODO: if the allied facility is in the new coords, replace this scenario
                        // with a facility defense, with the opfor coming directly from all hostiles
                        // assigned to this scenario

                        // update the scenario's biome
                        setScenarioParametersFromBiome(track, scenario);
                        scenario.setCurrentState(ScenarioState.UNRESOLVED);
                        return false;
                    } else {
                        track.failObjective(scenario.getCoords());
                        // TODO: if there's no allied facilities here, add its forces to track
                        // reinforcement pool
                        return true;
                    }
                }
            }
        }

        // if we couldn't find the scenario on any tracks, then let's just
        // rid of any underlying AtB scenarios as well
        return true;
    }

    public void startup() {
        MekHQ.registerHandler(this);
    }

    /**
     * Event handler for the new day event.
     */
    @Subscribe
    public void handleNewDay(NewDayEvent ev) {
        // don't do any of this if StratCon isn't turned on
        if (!ev.getCampaign().getCampaignOptions().isUseStratCon()) {
            return;
        }
        boolean isMonday = ev.getCampaign().getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY;
        boolean isStartOfMonth = ev.getCampaign().getLocalDate().getDayOfMonth() == 1;

        // run scenario generation routine for every track attached to an active
        // contract
        for (AtBContract contract : ev.getCampaign().getActiveAtBContracts()) {
            StratconCampaignState campaignState = contract.getStratconCampaignState();

            if (campaignState != null) {
                for (StratconTrackState track : campaignState.getTracks()) {
                    cleanupPhantomScenarios(track);

                    // check if some of the forces have finished deployment
                    // please do this before generating scenarios for track
                    // to avoid unintentionally cleaning out integrated force deployments on
                    // 0-deployment-length tracks
                    processTrackForceReturnDates(track, ev.getCampaign());

                    processFacilityEffects(track, campaignState, isStartOfMonth);

                    // loop through scenarios - if we haven't deployed in time,
                    // fail it and apply consequences
                    for (StratconScenario scenario : track.getScenarios().values()) {
                        if ((scenario.getDeploymentDate() != null) &&
                                scenario.getDeploymentDate().isBefore(ev.getCampaign().getLocalDate()) &&
                                scenario.getPrimaryForceIDs().isEmpty()) {
                            processIgnoredScenario(scenario, campaignState);
                        }
                    }

                    // on monday, generate new scenarios
                    if (isMonday) {
                        generateScenariosForTrack(ev.getCampaign(), contract, track);
                    }
                }
            }
        }
    }

    /**
     * Worker function that goes through a track and cleans up scenarios missing
     * required data
     */
    private void cleanupPhantomScenarios(StratconTrackState track) {
        List<StratconScenario> cleanupList = track.getScenarios().values().stream()
                .filter(scenario -> (scenario.getDeploymentDate() == null)
                        && !scenario.isStrategicObjective())
                .collect(Collectors.toList());

        for (StratconScenario scenario : cleanupList) {
            track.removeScenario(scenario);
        }
    }

    public void shutdown() {
        MekHQ.unregisterHandler(this);
    }
}
