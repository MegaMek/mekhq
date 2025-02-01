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

import megamek.common.Minefield;
import megamek.common.TargetRoll;
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
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.mission.resupplyAndCaches.StarLeagueCache;
import mekhq.campaign.mission.resupplyAndCaches.StarLeagueCache.CacheType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.unit.Unit;
import org.apache.commons.math3.util.Pair;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.Compute.d6;
import static megamek.common.Compute.randomInt;
import static megamek.common.Coords.ALL_DIRECTIONS;
import static megamek.common.UnitType.*;
import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.icons.enums.OperationalStatus.determineLayeredForceIconOperationalStatus;
import static mekhq.campaign.mission.AtBDynamicScenarioFactory.finalizeScenario;
import static mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment.Allied;
import static mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment.Opposing;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.AllGroundTerrain;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.LowAtmosphere;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.Space;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.SpecificGroundTerrain;
import static mekhq.campaign.personnel.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.SkillType.S_TACTICS;
import static mekhq.campaign.personnel.SkillType.getSkillHash;
import static mekhq.campaign.stratcon.StratconContractInitializer.getUnoccupiedCoords;
import static mekhq.campaign.stratcon.StratconRulesManager.ReinforcementEligibilityType.AUXILIARY;
import static mekhq.campaign.stratcon.StratconRulesManager.ReinforcementResultsType.DELAYED;
import static mekhq.campaign.stratcon.StratconRulesManager.ReinforcementResultsType.FAILED;
import static mekhq.campaign.stratcon.StratconRulesManager.ReinforcementResultsType.INTERCEPTED;
import static mekhq.campaign.stratcon.StratconRulesManager.ReinforcementResultsType.SUCCESS;
import static mekhq.campaign.stratcon.StratconScenarioFactory.convertSpecificUnitTypeToGeneral;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

/**
 * This class contains "rules" logic for the AtB-Stratcon state
 *
 * @author NickAragua
 */
public class StratconRulesManager {
    public final static int BASE_LEADERSHIP_BUDGET = 1000;

    private static final MMLogger logger = MMLogger.create(StratconRulesManager.class);

    /**
     * What makes a particular lance eligible to be reinforcements for a scenario
     */
    public enum ReinforcementEligibilityType {
        /**
         * Nothing
         */
        NONE,

        /**
         * Combat Team is already deployed to the track
         */
        CHAINED_SCENARIO,

        /**
         * We pay a support point and make a regular roll
         */
        REGULAR,

        /**
         * The Combat Team's deployment orders are "Frontline" or "Auxiliary".
         * We pay a support point and make an enhanced roll
         */
        AUXILIARY
    }

    /**
     * What were the results of the reinforcement roll?
     */
    public enum ReinforcementResultsType {
        /**
         * The reinforcement attempt was successful.
         */
        SUCCESS,

        /**
         * The reinforcements arrive later than normal.
         */
        DELAYED,

        /**
         * The attempt failed, nothing else happens.
         */
        FAILED,

        /**
         * The reinforcements were intercepted.
         */
        INTERCEPTED
    }

    /**
     * This method generates scenario dates for each week of the StratCon campaign.
     * <p>
     * The method first determines the number of turning point scenario rolls based on the required
     * lance count from the track, then multiplies that count depending on the contract's morale level.
     * <p>
     * If auto-assign for lances is enabled, and either there are no available forces or the number of
     * weekly scenarios equals or exceeds the number of available forces, it breaks from the scenario
     * generation loop.
     * <p>
     * For each scenario, a scenario odds target number is calculated, and a roll is made against
     * this target. If the roll is less than the target number, a new weekly scenario is created
     * with a random date within the week.
     *
     * @param campaign The campaign.
     * @param campaignState The state of the StratCon campaign.
     * @param contract The AtBContract for the campaign.
     * @param track The StratCon campaign track.
     */
    public static void generateScenariosDatesForWeek(Campaign campaign, StratconCampaignState campaignState,
                                                 AtBContract contract, StratconTrackState track) {
        // maps scenarios to force IDs
        final boolean autoAssignLances = contract.getCommandRights().isIntegrated();
        List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract);

        int scenarioRolls = track.getRequiredLanceCount();

        AtBMoraleLevel moraleLevel = contract.getMoraleLevel();

        switch (moraleLevel) {
            case ADVANCING -> scenarioRolls = (int) round(scenarioRolls * 1.33);
            case DOMINATING -> scenarioRolls = (int) round(scenarioRolls * 1.66);
            case OVERWHELMING -> scenarioRolls = scenarioRolls * 2;
            default -> {}
        }

        for (int scenarioIndex = 0; scenarioIndex < scenarioRolls; scenarioIndex++) {
            if (autoAssignLances && availableForceIDs.isEmpty()) {
                break;
            }

            if (autoAssignLances && (scenarioIndex >= availableForceIDs.size())) {
                break;
            }

            int targetNum = calculateScenarioOdds(track, contract, false);
            int roll = randomInt(100);

            if (roll < targetNum) {
                LocalDate scenarioDate = campaign.getLocalDate().plusDays(randomInt(7));
                campaignState.addWeeklyScenario(scenarioDate);
                logger.info(String.format("StratCon Weekly Scenario Roll: %s vs. %s (%s)", roll, targetNum, scenarioDate));
            } else {
                logger.info(String.format("StratCon Weekly Scenario Roll: %s vs. %s", roll, targetNum));
            }
        }
    }

    /**
     * This method generates a weekly scenario for a specific track.
     * <p>
     * First, it initializes empty collections for generated scenarios and available forces, and
     * determines whether lances are auto-assigned.
     * <p>
     * Then it generates a requested number of scenarios. If auto-assign is enabled and there
     * are no available forces, it breaks from the scenario generation loop.
     * <p>
     * For each scenario, it first tries to create a scenario for existing forces on the track.
     * If that is not possible, it selects random force, removes it from available forces, and
     * creates a scenario for it. For any scenario, if it is under liaison command, it may set the
     * scenario as required and attaches the liaison.
     * <p>
     * After scenarios are generated, OpFors, events, etc. are finalized for each scenario.
     *
     * @param campaign      The current campaign.
     * @param campaignState The relevant StratCon campaign state.
     * @param contract      The relevant contract.
     * @param scenarioCount The number of scenarios to generate.
     */
    public static void generateDailyScenariosForTrack(Campaign campaign, StratconCampaignState campaignState,
                                                      AtBContract contract, int scenarioCount) {
        final boolean autoAssignLances = contract.getCommandRights().isIntegrated();

        // get this list just so we have it available
        List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract);

        // Build the available force pool - this ensures operational forces have an increased
        // chance of being picked
        if (autoAssignLances && !availableForceIDs.isEmpty()) {
            List<Integer> availableForcePool = new ArrayList<>();

            for (int forceId : availableForceIDs) {
                Force force = campaign.getForce(forceId);

                if (force == null) {
                    continue;
                }

                if (force.isDeployed()) {
                    continue;
                }

                int operationalStatus = 0;
                int unitCount = 0;

                for (UUID unitId : force.getAllUnits(true)) {
                    try {
                        Unit unit = campaign.getUnit(unitId);
                        operationalStatus += determineLayeredForceIconOperationalStatus(unit).ordinal();
                        unitCount++;
                    } catch (Exception e) {
                        logger.warn(e.getMessage(), e);
                    }
                }

                int calculatedOperationStatus = (int) round(Math.pow((3 - (double) operationalStatus / unitCount), 2.0));

                for (int i = 0; i < calculatedOperationStatus; i++) {
                    availableForcePool.add(forceId);
                }
            }

            Collections.shuffle(availableForcePool);
            availableForceIDs = availableForcePool;
        }

        Map<MapLocation, List<Integer>> sortedAvailableForceIDs = sortForcesByMapType(availableForceIDs, campaign);

        for (int scenarioIndex = 0; scenarioIndex < scenarioCount; scenarioIndex++) {
            if (autoAssignLances && availableForceIDs.isEmpty()) {
                break;
            }

            List<StratconTrackState> tracks = campaignState.getTracks();
            StratconTrackState track = campaignState.getTracks().get(0);

            if (tracks.size() > 1) {
                track = getRandomItem(tracks);
            }

            if (autoAssignLances && availableForceIDs.isEmpty()) {
                break;
            }

            StratconCoords scenarioCoords = getUnoccupiedCoords(track, true);

            if (scenarioCoords == null) {
                logger.warn("Target track is full, skipping scenario generation");
                continue;
            }

            // if forces are already assigned to these coordinates, use those instead of randomly
            // selected ones
            StratconScenario scenario;
            if (track.getAssignedCoordForces().containsKey(scenarioCoords)) {
                scenario = generateScenarioForExistingForces(scenarioCoords,
                    track.getAssignedCoordForces().get(scenarioCoords), contract, campaign, track);
            // otherwise, pick a random force from the avail
            } else {
                int randomForceIndex = randomInt(availableForceIDs.size());
                int randomForceID = availableForceIDs.get(randomForceIndex);

                // remove the force from the available lists, so we don't designate it as primary
                // twice
                if (autoAssignLances) {
                    availableForceIDs.removeIf(id -> id.equals(randomForceIndex));

                    // we want to remove the actual int with the value, not the value at the index
                    sortedAvailableForceIDs.get(AllGroundTerrain).removeIf(id -> id.equals(randomForceID));
                    sortedAvailableForceIDs.get(LowAtmosphere).removeIf(id -> id.equals(randomForceID));
                    sortedAvailableForceIDs.get(Space).removeIf(id -> id.equals(randomForceID));
                }

                // two scenarios on the same coordinates wind up increasing in size
                if (track.getScenarios().containsKey(scenarioCoords)) {
                    track.getScenarios().get(scenarioCoords).incrementRequiredPlayerLances();
                    assignAppropriateExtraForceToScenario(track.getScenarios().get(scenarioCoords),
                        sortedAvailableForceIDs);
                    continue;
                }

                scenario = setupScenario(scenarioCoords, randomForceID, campaign, contract, track);
            }

            if (scenario != null) {
                finalizeBackingScenario(campaign, contract, track, autoAssignLances, scenario);
            }
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
            null, false, null);
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
     * @param daysTilDeployment How many days beyond current date until the scenario, or {@code null}
     *                         to pick a random date within the next 7 days.
     * @return A newly generated {@link StratconScenario}, or {@code null} if scenario creation fails.
     */
     public static @Nullable StratconScenario generateExternalScenario(Campaign campaign, AtBContract contract,
                                                                       @Nullable StratconTrackState track,
                                                                       @Nullable StratconCoords scenarioCoords,
                                                                       @Nullable ScenarioTemplate template,
                                                                       boolean allowPlayerFacilities,
                                                                       @Nullable Integer daysTilDeployment) {
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
         List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract);
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
                 template, daysTilDeployment);
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
                 int randomForceIndex = randomInt(availableForces);
                 randomForceID = availableForceIDs.get(randomForceIndex);
             }

             scenario = setupScenario(scenarioCoords, randomForceID, campaign, contract, track,
                 template, false, daysTilDeployment);
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
     * Generates a reinforcement interception scenario for a given StratCon track.
     * An interception scenario is set up at unoccupied coordinates on the track.
     * If the scenario setup is successful, it is finalized and the deployment date for the
     * scenario is set as the current date.
     *
     * @param campaign the current campaign
     * @param contract the {@link AtBContract} for which the scenario is created
     * @param track the {@link StratconTrackState} where the scenario is located, or {@code null}
     * if not located on a track
     * @param template the {@link ScenarioTemplate} used to create the scenario
     * @param interceptedForce the {@link Force} that's being intercepted in the scenario
     */
     public static @Nullable void generateReinforcementInterceptionScenario(
         Campaign campaign, StratconScenario linkedScenario, AtBContract contract,
         StratconTrackState track, ScenarioTemplate template, Force interceptedForce) {
         StratconCoords scenarioCoords = getUnoccupiedCoords(track, false);

         StratconScenario scenario = setupScenario(scenarioCoords, interceptedForce.getId(), campaign,
             contract, track, template, true, 0);

         if (scenario == null) {
             logger.error("Failed to generate a random interception scenario, aborting scenario generation.");
             return;
         }

         finalizeBackingScenario(campaign, contract, track, true, scenario);
         scenario.setActionDate(campaign.getLocalDate());
         scenario.getBackingScenario().setStatus(ScenarioStatus.CURRENT);
         scenario.getBackingScenario().setlinkedScenarioID(linkedScenario.getBackingScenario().getId());
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
     * @param daysTilDeployment How many days until the scenario takes place, or {@code null} to
     *                         pick a random day within the next 7 days.
     *
     * @return The created {@link StratconScenario} or @code null},
     * if no {@link ScenarioTemplate} is found or if all coordinates in the provided
     * {@link StratconTrackState} are occupied (and therefore, scenario placement is not possible).
     */
    public static @Nullable StratconScenario addHiddenExternalScenario(Campaign campaign,
                                                                       AtBContract contract,
                                                                       @Nullable StratconTrackState trackState,
                                                                       @Nullable ScenarioTemplate template,
                                                                       boolean allowPlayerFacilities,
                                                                       @Nullable Integer daysTilDeployment) {
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
            trackState, FORCE_NONE, coords, template, daysTilDeployment);

        if (scenario == null) {
            return null;
        }

        // clear dates, because we don't want the scenario disappearing on us
        scenario.setDeploymentDate(null);
        scenario.setActionDate(null);
        scenario.setReturnDate(null);
        scenario.setStrategicObjective(true);
        scenario.setTurningPoint(true);
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
        final AtBDynamicScenario backingScenario = scenario.getBackingScenario();

        // First determine if the scenario is a Turning Point (that win/lose will affect CVP)
        determineIfTurningPointScenario(contract, scenario);

        // Then add any Cadre Duty units
        if (contract.getContractType().isCadreDuty()) {
            addCadreDutyTrainees(backingScenario);
        }

        // Finally, finish scenario set up
        finalizeScenario(backingScenario, contract, campaign);
        setScenarioParametersFromBiome(track, scenario);
        swapInPlayerUnits(scenario, campaign, FORCE_NONE);

        if (!autoAssignLances && !scenario.ignoreForceAutoAssignment()) {
            for (int forceID : scenario.getPlayerTemplateForceIDs()) {
                backingScenario.removeForce(forceID);
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
     * Adds a Cadre Duty trainees modifier to the given scenario based on the location of the battle.
     *
     * <p>
     * This method determines the type of trainees to be added to the scenario by evaluating the map
     * location parameter of the scenario's template. Depending on whether the battle is an air or
     * space battle versus a ground battle, the appropriate Cadre Duty trainees scenario modifier
     * is applied to the backing scenario.
     * </p>
     *
     * <p>
     * The logic is as follows:
     * <ul>
     *     <li>If the battle occurs in low atmosphere or space, the air trainees modifier is added.</li>
     *     <li>If the battle occurs on the ground at any other map location, the ground trainees
     *     modifier is added.</li>
     * </ul>
     *
     * @param backingScenario The {@link AtBDynamicScenario} representing the current scenario to which the modifier will be applied.
     */
    private static void addCadreDutyTrainees(AtBDynamicScenario backingScenario) {
        final ScenarioTemplate template = backingScenario.getTemplate();
        final MapLocation mapLocation = template.mapParameters.getMapLocation();
        boolean isAirBattle = (mapLocation == LowAtmosphere) || (mapLocation == Space);

        if (isAirBattle) {
            backingScenario.addScenarioModifier(
                AtBScenarioModifier.getScenarioModifier(MHQConstants.SCENARIO_MODIFIER_TRAINEES_AIR));
        } else {
            backingScenario.addScenarioModifier(
                AtBScenarioModifier.getScenarioModifier(MHQConstants.SCENARIO_MODIFIER_TRAINEES_GROUND));
        }
    }

    /**
     * Determines if a given StratCon scenario should be marked as critical within the context of a
     * contract.
     * <p>
     * This method evaluates the scenario's template, type, and the contract's command rights to decide
     * if the scenario should be flagged as a "turning point." Turning Point scenarios can cause CVP
     * to be increased or decreased.
     * </p>
     *
     * <p>
     * The logic follows these rules:
     * <ul>
     *     <li>If the scenario template or its type is not related to resupply operations, the
     *     method evaluates the contract's command rights.</li>
     *     <li>For <strong>INTEGRATED</strong> or <strong>HOUSE</strong> command rights:
     *     non-resupply scenarios are always marked as required.</li>
     *     <li>For <strong>LIAISON</strong> or <strong>INDEPENDENT</strong> command rights:
     *     non-resupply scenarios have a 25% chance (1 in 4) to be marked as required. An attached
     *     units modifier is also set if the scenario becomes required.</li>
     * </ul>
     *
     * @param contract The {@link AtBContract} representing the current contract.
     * @param scenario The {@link StratconScenario} being evaluated to determine if it is a Turning Point.
     */
    private static void determineIfTurningPointScenario(AtBContract contract, StratconScenario scenario) {
        ScenarioTemplate template = scenario.getScenarioTemplate();
        boolean isResupply = scenario.getBackingScenario().getStratConScenarioType().isResupply();

        if (isResupply) {
            scenario.setTurningPoint(false);
            return;
        }

        boolean isObjective = scenario.isStrategicObjective();

        if (template == null || !template.getStratConScenarioType().isResupply()) {
            ContractCommandRights commandRights = contract.getCommandRights();
            switch (commandRights) {
                case INTEGRATED -> {
                    scenario.setTurningPoint(true);
                    if (randomInt(3) == 0 || isObjective) {
                        setAttachedUnitsModifier(scenario, contract);
                    }
                }
                case HOUSE, LIAISON -> {
                    if (randomInt(3) == 0 || isObjective) {
                        scenario.setTurningPoint(true);
                        setAttachedUnitsModifier(scenario, contract);
                    }
                }
                case INDEPENDENT -> {
                    if (randomInt(3) == 0 || isObjective) {
                        scenario.setTurningPoint(true);
                    }
                }
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
                    .get(randomInt(facilityBiome.allowedTerrainTypes.size()));
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
                backingScenario.setMap(mapTypeList.get(randomInt(mapTypeList.size())));
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
                            sft.getAllowedUnitType()) &&
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
            track, null, null);
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
     * @param daysTilDeployment How many days until the scenario takes place, or {@code null} to
     *                         pick a random day within the next 7 days.
     * @return The newly generated {@link StratconScenario}.
     */
    public static @Nullable StratconScenario generateScenarioForExistingForces(StratconCoords scenarioCoords,
                                                                               Set<Integer> forceIDs,
                                                                               AtBContract contract,
                                                                               Campaign campaign,
                                                                               StratconTrackState track,
                                                                               @Nullable ScenarioTemplate template,
                                                                               @Nullable Integer daysTilDeployment) {
        boolean firstForce = true;
        StratconScenario scenario = null;

        for (int forceID : forceIDs) {
            if (firstForce) {
                scenario = setupScenario(scenarioCoords, forceID, campaign, contract, track,
                    template, false, daysTilDeployment);
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
     * Deploys a combat team (force) to a specified coordinate within the strategic track and performs the
     * associated deployment activities, including handling scenarios, facilities, scouting behavior,
     * and fog of war updates.
     *
     * <p>The method processes the deployment as follows:
     * <ol>
     *     <li>Reveals the fog of war at or near the deployment coordinates based on the force's role, using
     *     {@code processForceDeployment}.</li>
     *     <li>If the deployment coordinates contain an existing hostile facility, a scenario involving
     *     that facility is created.</li>
     *     <li>If the deployment coordinates are empty, a chance-based scenario may be created depending
     *     on the scenario odds.</li>
     *     <li>If a scenario is revealed (either from the facility or randomly):</li>
     *         <li>- The deployed force is assigned to that scenario.</li>
     *         <li>- The scenario is finalized and parameters are adjusted accordingly.</li>
     *     <li>If a deploying force is performing a scouting mission:</li>
     *         <li>- The target coordinates may be shifted to an unoccupied adjacent coordinate if available.</li>
     *     <li>If the coordinates contain a non-allied facility or qualify for a new scenario, a
     *     scenario is generated:</li>
     *         <li>- If forces are already deployed at the location, generate a scenario involving
     *         these forces.</li>
     *         <li>- If no forces are present, assign available forces from the campaign or randomly
     *         select a suitable combat team for the scenario.</li>
     *         <li>- If applicable, determine whether the scenario is under liaison command based on
     *             contract command rights, and update the scenario requirements.</li>
     * </ol>
     *
     * @param coords   the {@link StratconCoords} representing the deployment coordinates.
     * @param forceID  the unique identifier of the combat team (force) being deployed.
     * @param campaign the current {@link Campaign} context, which provides access to combat teams, facilities,
     *                 and other campaign-level data.
     * @param contract the {@link AtBContract} associated with the campaign, which determines rules
     *                 and command rights for the deployment.
     * @param track    the {@link StratconTrackState} representing the strategic track, including details
     *                 about scenarios, facilities, and force assignments.
     * @param sticky   a {@code boolean} flag indicating whether the deployment is "sticky," meaning
     *                 the forces remain at the deployment location without automatically updating
     *                 their position.
     */
    public static void deployForceToCoords(StratconCoords coords, int forceID, Campaign campaign,
                                           AtBContract contract, StratconTrackState track, boolean sticky) {
        CombatTeam combatTeam = campaign.getCombatTeamsTable().get(forceID);

        // This shouldn't be possible, but never hurts to have a little insurance
        if (combatTeam == null) {
            return;
        }

        boolean isPatrol = combatTeam.getRole().isPatrol();

        // the following things should happen:
        // 1. call to "process force deployment", which reveals fog of war in or around the coords,
        // depending on force role
        // 2. if coords are a hostile facility, we get a facility scenario
        // 3. if coords are empty, we *may* get a scenario
        processForceDeployment(coords, forceID, campaign, track, sticky);

        // we may stumble on a fixed objective scenario - in that case assign the force
        // to it and finalize we also will not be encountering any of the other stuff so bug out
        // afterward
        StratconScenario revealedScenario = track.getScenario(coords);
        if (revealedScenario != null) {
            if (!revealedScenario.getBackingScenario().isFinalized()) {
                finalizeScenario(revealedScenario.getBackingScenario(), contract, campaign);
                setScenarioParametersFromBiome(track, revealedScenario);
            }
            revealedScenario.addPrimaryForce(forceID);
            commitPrimaryForces(campaign, revealedScenario, track);
            return;
        }

        StratconFacility facility = track.getFacility(coords);
        boolean isNonAlliedFacility = (facility != null) && (facility.getOwner() != Allied);

        int targetNum = calculateScenarioOdds(track, contract, true);
        boolean spawnScenario = (facility == null) && (randomInt(100) <= targetNum);

        if (isNonAlliedFacility || spawnScenario) {
            StratconScenario scenario;

            // If we're not deploying on top of an enemy facility, migrate the scenario
            if (!isNonAlliedFacility && isPatrol) {
                StratconCoords newCoords = getUnoccupiedAdjacentCoords(coords, track);

                if (newCoords != null) {
                    coords = newCoords;
                }
            }

            // Patrols only get autoAssigned to the scenario if they're dropped on top of a non-allied facility
            boolean autoAssignLances = !isPatrol || isNonAlliedFacility;

            // Do we already have forces deployed to the target coordinates?
            // If so, assign them to the scenario.
            Set<Integer> preDeployedForce = track.getAssignedCoordForces().get(coords);

            if (preDeployedForce != null && !preDeployedForce.isEmpty()) {
                scenario = generateScenarioForExistingForces(coords,
                    track.getAssignedCoordForces().get(coords), contract, campaign, track);
            // Otherwise, pick a random force from those available
            } else {
                List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract);
                Collections.shuffle(availableForceIDs);

                // If the player doesn't have any available forces, we grab a force at random to
                // seed the scenario
                if (availableForceIDs.isEmpty()) {
                    ArrayList<CombatTeam> combatTeams = campaign.getAllCombatTeams();
                    if (!combatTeams.isEmpty()) {
                        combatTeam = getRandomItem(combatTeams);

                        forceID = combatTeam.getForceId();
                    } else {
                        // If the player doesn't have any combat teams (somehow), they get a free pass
                        return;
                    }
                }

                scenario = setupScenario(coords, forceID, campaign, contract, track);
            }

            finalizeBackingScenario(campaign, contract, track, autoAssignLances, scenario);
        }
    }

    /**
     * Finds an unoccupied coordinates adjacent to the given origin coordinates.
     *
     * <p>Adjacent coordinates are determined based on all possible directions defined by {@code ALL_DIRECTIONS}.
     * A coordinate is considered "unoccupied" if the following conditions are met:
     * <ul>
     *     <li>No scenario is assigned to the coordinate (using {@link StratconTrackState#getScenario})</li>
     *     <li>No facility exists at the coordinate (using {@link StratconTrackState#getFacility})</li>
     *     <li>The coordinate is not occupied by any assigned forces (using {@link StratconTrackState#getAssignedForceCoords})</li>
     *     <li>The coordinate is on the map</li>
     * </ul>
     * If multiple suitable coordinates are found, one is selected at random and returned.
     * If no suitable coordinates are available, the method returns {@code null}.
     *
     * @param originCoords the coordinate from which to search for unoccupied adjacent ones
     * @param trackState   the state of the track containing information about scenarios, facilities, and forces
     * @return a randomly selected unoccupied adjacent coordinate, or {@code null} if none are available
     */
    private static @Nullable StratconCoords getUnoccupiedAdjacentCoords(StratconCoords originCoords,
                                                                       StratconTrackState trackState) {
        // We need to reduce width/height by one because coordinates index from 0, not 1
        final int trackWidth = trackState.getWidth() - 1;
        final int trackHeight = trackState.getHeight() - 1;

        List<StratconCoords> suitableCoords = new ArrayList<>();
        for (int direction : ALL_DIRECTIONS) {
            StratconCoords newCoords = originCoords.translate(direction);

            if (trackState.getScenario(newCoords) != null) {
                continue;
            }

            if (trackState.getFacility(newCoords) != null) {
                continue;
            }

            if (trackState.getAssignedForceCoords().containsValue(newCoords)) {
                continue;
            }

            // This is to ensure we're not trying to place a scenario off the map
            if ((newCoords.getX() < 0)
                || (newCoords.getX() > trackWidth)
                || (newCoords.getY() < 0)
                || (newCoords.getY() > trackHeight)) {
                continue;
            }

            suitableCoords.add(newCoords);
        }

        if (suitableCoords.isEmpty()) {
            return null;
        }

        return getRandomItem(suitableCoords);
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
        return setupScenario(coords, forceID, campaign, contract, track, null, false, null);
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
     * @param ignoreFacilities  Whether we should ignore any facilities at the selected location
     * @param daysTilDeployment How many days until the scenario takes place, or {@code null} to
     *                         pick a random day within the next 7 days.
     * @return The newly set up {@link StratconScenario}.
     */
    private static @Nullable StratconScenario setupScenario(StratconCoords coords, int forceID,
                                                            Campaign campaign, AtBContract contract,
                                                            StratconTrackState track,
                                                            @Nullable ScenarioTemplate template,
                                                            boolean ignoreFacilities,
                                                            @Nullable Integer daysTilDeployment) {
        StratconScenario scenario;

        if (track.getFacilities().containsKey(coords) && !ignoreFacilities) {
            StratconFacility facility = track.getFacility(coords);
            boolean alliedFacility = facility.getOwner() == Allied;
            template = StratconScenarioFactory.getFacilityScenario(alliedFacility);
            scenario = generateScenario(campaign, contract, track, forceID, coords, template, daysTilDeployment);
            setupFacilityScenario(scenario, facility);
        } else {
            if (template != null) {
                scenario = generateScenario(campaign, contract, track, forceID, coords, template, daysTilDeployment);
            } else {
                scenario = generateScenario(campaign, contract, track, forceID, coords, daysTilDeployment);
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
        boolean alliedFacility = facility.getOwner() == Allied;

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
     * Processes the deployment of a force to the specified coordinates on the given track.
     *
     * <p>This includes revealing the deployed coordinates, identifying and revealing facilities
     * and scenarios within the scan range, and updating necessary game states such as fatigue
     * and force assignments. It does not include assigning the force to specific scenarios.</p>
     *
     * <strong>Behavior:</strong>
     * <ul>
     *   <li>If the force's deployment coordinates are unrevealed, fatigue is increased for the force.</li>
     *   <li>Ensures that fatigue is increased only once during the deployment process.</li>
     *   <li>Reveals all coordinates, facilities, and scenarios within the force's scan range.</li>
     *   <li>Handles cloaked scenarios by activating them and updating game states as necessary.</li>
     *   <li>Updates the track's revealed coordinates to include the deployment and adjacent areas within range.</li>
     *   <li>Assigns the deployed force to the specified coordinates and clears their previous track assignments.</li>
     * </ul>
     *
     * <strong>Notes:</strong>
     * <ul>
     *   <li>Scout or patrol roles may increase the scan range.</li>
     *   <li>The method uses a breadth-first search (BFS) approach to traverse the hex grid and reveal neighbors
     *       within the scan range efficiently, avoiding redundant processing using a visited set.</li>
     * </ul>
     *
     * @param coords    The coordinates where the force is being deployed.
     * @param forceID   The ID of the force being deployed.
     * @param campaign  The current campaign context, used to retrieve combat teams and update game events.
     * @param track     The current track state where the deployment is happening.
     * @param sticky    Whether the force should be persistently assigned to the track.
     *
     * @throws IllegalStateException if the force or the associated combat team is missing or invalid.
     */
    public static void processForceDeployment(StratconCoords coords, int forceID, Campaign campaign,
                                              StratconTrackState track, boolean sticky) {
        // we want to ensure we only increase Fatigue once
        boolean hasFatigueIncreased = false;

        // BFS queue for coordinates, tracks distance from the starting point
        Queue<Pair<StratconCoords, Integer>> queue = new LinkedList<>();
        // Keep a set of visited coordinates to avoid redundancy
        Set<StratconCoords> visited = new HashSet<>();

        // Start with the initial deployment coordinate at distance 0
        queue.add(new Pair<>(coords, 0));
        visited.add(coords);

        // Determine scan range
        int scanRange = track.getScanRangeIncrease();

        CombatTeam combatTeam = campaign.getCombatTeamsTable().get(forceID);

        if (combatTeam != null && combatTeam.getRole().isPatrol()) {
            scanRange++;
        }

        // Process starting point
        if (!track.getRevealedCoords().contains(coords)) {
            increaseFatigue(forceID, campaign);
            hasFatigueIncreased = true;
        }

        track.getRevealedCoords().add(coords);

        StratconFacility targetFacility = track.getFacility(coords);
        if (targetFacility != null) {
            targetFacility.setVisible(true);
        }

        StratconScenario scenario = track.getScenario(coords);

        if (scenario != null) {
            AtBDynamicScenario backingScenario = scenario.getBackingScenario();

            if (backingScenario != null) {
                if (backingScenario.isCloaked()) {
                    backingScenario.setCloaked(false);
                }

                if (backingScenario.getDate() == null) {
                    setScenarioDates(0, track, campaign, scenario);
                }

                MekHQ.triggerEvent(new ScenarioChangedEvent(backingScenario));
            }
        }

        // Traverse neighboring coordinates up to the specified distance
        while (!queue.isEmpty()) {
            Pair<StratconCoords, Integer> current = queue.poll();
            StratconCoords currentCoords = current.getKey();
            int distance = current.getValue();

            // Only process neighbors if they're within the max distance
            if (distance < scanRange) {
                for (int direction = 0; direction < 6; direction++) {
                    StratconCoords checkCoords = currentCoords.translate(direction);

                    // Skip already visited coordinates
                    if (visited.contains(checkCoords)) {
                        continue;
                    }

                    // Mark as visited
                    visited.add(checkCoords);
                    queue.add(new Pair<>(checkCoords, distance + 1)); // Add the neighbor with incremented distance

                    // Process facilities
                    targetFacility = track.getFacility(checkCoords);
                    if (targetFacility != null) {
                        targetFacility.setVisible(true);
                    }

                    // Increase fatigue only once
                    if (!track.getRevealedCoords().contains(checkCoords) && !hasFatigueIncreased) {
                        increaseFatigue(forceID, campaign);
                        hasFatigueIncreased = true;
                    }

                    // Mark the current coordinate as revealed
                    track.getRevealedCoords().add(checkCoords);
                }
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
     * Processes the effects of deploying a reinforcement force to a scenario.
     * Based on the reinforcement type, the campaign state, and the results dice rolls, skills,
     * and intercept odds), this method determines whether the reinforcement deployment succeeds,
     * fails, is delayed, or is intercepted.
     *
     * <p>Key steps include:
     * <ul>
     *   <li>Checking if the reinforcement type is {@link ReinforcementEligibilityType#CHAINED_SCENARIO},
     *   which automatically succeeds.</li>
     *   <li>Calculating the results of dice rolls, optionally adjusted for skills such as Tactics,
     *       and comparing it against the target number to determine success or failure.</li>
     *   <li>Handling critical failures, interception attempts, and enemy routing.</li>
     *   <li>Generating follow-up scenarios for intercepted reinforcements or handling delays.</li>
     * </ul>
     *
     * @param force                     the {@link Force} being deployed as a reinforcement
     * @param reinforcementType         the type of reinforcement (e.g., auxiliary or chained scenario)
     * @param campaignState             the current state of the campaign
     * @param scenario                  the scenario to which the reinforcements are being deployed
     * @param campaign                  the overarching campaign instance managing the scenario
     * @param reinforcementTargetNumber the target number that the reinforcement roll must meet or exceed
     * @param isGMReinforcement         {@code true} if the player is using GM powers to bypass the
     *                                             reinforcement check, {@code false} otherwise.
     * @return a {@link ReinforcementResultsType} indicating the result of the reinforcement deployment:
     * <ul>
     *     <li>{@link ReinforcementResultsType#SUCCESS} - The reinforcement is deployed successfully.</li>
     *     <li>{@link ReinforcementResultsType#FAILED} - The reinforcement deployment fails.</li>
     *     <li>{@link ReinforcementResultsType#DELAYED} - The reinforcement is delayed.</li>
     *     <li>{@link ReinforcementResultsType#INTERCEPTED} - The reinforcement is intercepted,
     *     possibly resulting in a new scenario.</li>
     * </ul>
     */
    public static ReinforcementResultsType processReinforcementDeployment(Force force,
                                                                          ReinforcementEligibilityType reinforcementType,
                                                                          StratconCampaignState campaignState,
                                                                          StratconScenario scenario,
                                                                          Campaign campaign,
                                                                          int reinforcementTargetNumber,
                                                                          boolean isGMReinforcement) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AtBStratCon",
            MekHQ.getMHQOptions().getLocale());

        if (reinforcementType.equals(ReinforcementEligibilityType.CHAINED_SCENARIO)) {
            return SUCCESS;
        }

        AtBContract contract = campaignState.getContract();

        // Determine StratCon Track and other context for recalculation
        StratconTrackState track = null;
        for (StratconTrackState trackState : campaignState.getTracks()) {
            if (trackState.getScenarios().containsValue(scenario)) {
                track = trackState;
                break;
            }
        }

        // Make the roll
        int roll = d6(2);

        // If the formation is set to Maneuver or Auxiliary, use the highest of two rolls
        String maneuverRoleReport = "";
        if (reinforcementType == AUXILIARY) {
            int secondRoll = d6(2);
            roll = max(roll, secondRoll);
            maneuverRoleReport = String.format(" (%s)", roll);
        }

        StringBuilder reportStatus = new StringBuilder();

        if (isGMReinforcement) {
            reportStatus.append(String.format(resources.getString("reinforcementsAttempt.text.gm"),
                scenario.getName()));
            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsAutomaticSuccess.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return SUCCESS;
        } else {
            reportStatus.append(String.format(resources.getString("reinforcementsAttempt.text"),
                scenario.getName(), roll, maneuverRoleReport, reinforcementTargetNumber));
        }

        // Critical Failure
        if (roll == 2) {
            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsCriticalFailure.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return FAILED;
        }

        // Reinforcement successful
        if (roll >= reinforcementTargetNumber) {
            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsSuccess.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return SUCCESS;
        }

        // Reinforcement roll failed, make interception check
        int interceptionOdds = calculateScenarioOdds(track, campaignState.getContract(), true);
        int interceptionRoll = randomInt(100);

        // Check passed
        if (interceptionRoll >= interceptionOdds) {
            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsCommandFailure.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor()),
                CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return DELAYED;
        }

        // Check failed, but enemy is routed
        if (contract.getMoraleLevel().isRouted()) {
            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsSuccessRouted.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return SUCCESS;
        }

        // Check failed, enemy attempt interception
        reportStatus.append(' ');
        reportStatus.append(String.format(resources.getString("reinforcementsInterceptionAttempt.text"),
            spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor()),
            CLOSING_SPAN_TAG));

        UUID commanderId = force.getForceCommanderID();

        if (commanderId == null) {
            logger.error("Force Commander ID is null.");

            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsErrorNoCommander.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return FAILED;
        }

        Person commander = campaign.getPerson(commanderId);

        if (commander == null) {
            logger.error("Failed to fetch commander from ID.");

            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsErrorUnableToFetchCommander.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return FAILED;
        }


        roll = d6(2);
        int targetNumber = 9;
        Skill tactics = commander.getSkill(S_TACTICS);

        if (tactics != null) {
            targetNumber -= tactics.getFinalSkillValue();
        } else {
            // Effectively a -1 penalty for being unskilled
            targetNumber++;
        }

        if (roll >= targetNumber) {
            reportStatus.append(' ');
            String reportString = tactics != null
                ? resources.getString("reinforcementEvasionSuccessful.text")
                :  resources.getString("reinforcementEvasionSuccessful.noSkill");
            reportStatus.append(String.format(reportString,
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG, roll, targetNumber));


            campaign.addReport(reportStatus.toString());

            if (campaign.getCampaignOptions().isUseFatigue()) {
                increaseFatigue(force.getId(), campaign);
            }

            return DELAYED;
        }

        reportStatus.append(' ');
        reportStatus.append(String.format(resources.getString("reinforcementEvasionUnsuccessful.text"),
            spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
            CLOSING_SPAN_TAG, roll, targetNumber));
        campaign.addReport(reportStatus.toString());

        ScenarioTemplate scenarioTemplate = getInterceptionScenarioTemplate(force, campaign);

        generateReinforcementInterceptionScenario(campaign, scenario, contract, track, scenarioTemplate, force);

        return INTERCEPTED;
    }

    /**
     * Retrieves the appropriate {@link ScenarioTemplate} for an interception scenario based on the
     * provided {@link Force} and {@link Campaign}.
     * <p>
     * The method determines which scenario template file should be used by analyzing the primary unit
     * type of the {@link Force} within the given {@link Campaign}. It then deserializes the template
     * file into a {@link ScenarioTemplate} object.
     * <p>
     * Special cases:
     * <ul>
     *   <li>If the primary unit type is `CONV_FIGHTER` or `AEROSPACEFIGHTER` (and a random check passes),
     *       a "Low-Atmosphere" template is selected.</li>
     *   <li>If the primary unit type qualifies as an `AEROSPACEFIGHTER` or higher,
     *       a "Space" template is selected.</li>
     *   <li>Otherwise, the default template is used.</li>
     * </ul>
     *
     * @param force    The {@link Force} instance that the scenario is based on.
     *                 This is used to determine the primary unit type.
     * @param campaign The {@link Campaign} in which the interception is taking place.
     *                 Provides context for evaluating the {@link Force}.
     * @return A {@link ScenarioTemplate} instance based on the template file matching the logic above,
     *         or a default template if no specific case is matched.
     * @see ScenarioTemplate#Deserialize(String)
     */
    private static ScenarioTemplate getInterceptionScenarioTemplate(Force force, Campaign campaign) {
        String templateString = "data/scenariotemplates/%sReinforcements Intercepted.xml";

        ScenarioTemplate scenarioTemplate = ScenarioTemplate.Deserialize(String.format(templateString, ""));

        int primaryUnitType = force.getPrimaryUnitType(campaign);

        if ((primaryUnitType == CONV_FIGHTER)
            || (primaryUnitType == AEROSPACEFIGHTER) && (randomInt(3) == 0)) {
            scenarioTemplate = ScenarioTemplate.Deserialize(String.format(templateString, "Low-Atmosphere "));
        } else if (primaryUnitType >= AEROSPACEFIGHTER) {
            scenarioTemplate = ScenarioTemplate.Deserialize(String.format(templateString, "Space "));
        }
        return scenarioTemplate;
    }

    /**
     * Calculates the target roll required for determining reinforcements in a specific campaign scenario.
     *
     * <p>This method evaluates the reinforcement target number by considering various factors
     * such as the administrative skill of the command liaison, facility ownership influence,
     * contract-related skill levels, and command rights configurations. Multiple modifiers
     * are applied step-by-step to generate the final {@link TargetRoll}.</p>
     *
     * <strong>Steps in Calculation:</strong>
     * <ol>
     *     <li><b>Base Target Number:</b></li>
     *             <li>-- If the {@code commandLiaison} is provided and has administrative skill,
     *             it replaces the default base target number with their skill value.</li>
     *             <li>-- If no liaison is provided, or they lack administrative skill, the base target
     *             number remains at the default value.</li>
     *     <li><b>Facilities Modifier:</b></li>
     *             <li>-- Iterates through the facilities in the relevant track to determine their
     *             ownership.</li>
     *             <li>-- If a facility is owned by the player or allied forces, a negative modifier
     *             is applied, reducing the target number.</li>
     *             <li>-- If a facility is owned by non-allied forces, a positive modifier is applied,
     *             increasing the target number.</li>
     *     <li><b>Skill Modifier:</b></li>
     *             <li>-- The skill modifier reflects the ally and enemy skill adjustments from the contract.</li>
     *             <li>-- If the campaign is operating under an "Independent" rights condition, additional
     *             checks and adjustments are made based on ally and enemy skill levels.</li>
     *     <li><b>Liaison Command Modifier:</b></li>
     *             <li>-- If command rights indicate that a liaison is required, the modifier is adjusted.</li>
     * </ol>
     *
     * @param campaign         the {@link Campaign} instance representing the current operational campaign.
     * @param scenario         the {@link StratconScenario} for which reinforcement details are being determined.
     * @param commandLiaison   the {@link Person} acting as the command liaison, or {@code null} if no liaison exists.
     * @param campaignState    the {@link StratconCampaignState} representing the state of the overarching campaign.
     * @param contract         the {@link AtBContract} defining the terms of the contract for this scenario.
     * @return                 a {@link TargetRoll} object representing the calculated reinforcement target number,
     *                         with appropriate modifiers applied.
     */
    public static TargetRoll calculateReinforcementTargetNumber(Campaign campaign,
                                                                StratconScenario scenario,
                                                                @Nullable Person commandLiaison,
                                                                StratconCampaignState campaignState,
                                                                AtBContract contract) {
        // Create Target Roll
        TargetRoll reinforcementTargetNumber = new TargetRoll();

        // Base Target Number
        int skillTargetNumber = 12;
        SkillType skillType = getSkillHash().get(S_ADMIN);
        if (skillType != null) {
            skillTargetNumber = getSkillHash().get(S_ADMIN).getTarget();
        }

        if (commandLiaison != null) {
            Skill skill = commandLiaison.getSkill(S_ADMIN);

            if (skill != null) {
                skillTargetNumber = skill.getFinalSkillValue();
            }

            reinforcementTargetNumber.addModifier(skillTargetNumber,
                "Administration (" + commandLiaison.getFullTitle() +')');
        } else {
            reinforcementTargetNumber.addModifier(skillTargetNumber,
                "Administration (Unskilled)");
        }

        // Facilities Modifier
        StratconTrackState track = scenario.getTrackForScenario(campaign, campaignState);

        int facilityModifier = 0;
        if (track != null) {
            for (StratconFacility facility : track.getFacilities().values()) {
                if (facility.getOwner().equals(ForceAlignment.Player) || facility.getOwner().equals(Allied)) {
                    facilityModifier--;
                } else {
                    facilityModifier++;
                }
            }
        }

        reinforcementTargetNumber.addModifier(facilityModifier, "Facilities");

        // Skill Modifier
        int skillModifier = -contract.getAllySkill().getAdjustedValue();

        ContractCommandRights commandRights = contract.getCommandRights();
        if (commandRights.isIndependent()) {
            if (campaign.getCampaignOptions().getUnitRatingMethod().isCampaignOperations()) {
                skillModifier = -campaign.getReputation().getAverageSkillLevel().getAdjustedValue();
            }
        }

        skillModifier += contract.getEnemySkill().getAdjustedValue();

        reinforcementTargetNumber.addModifier(skillModifier, "Skill Modifier");

        // Liaison Modifier
         if (commandRights.isLiaison()) {
            int liaisonModifier = -1;
            reinforcementTargetNumber.addModifier(liaisonModifier, "Liaison Command Rights");
        }

        // Return final value
        return reinforcementTargetNumber;
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

        MapLocation selectedLocation = mapLocations.get(randomInt(mapLocations.size()));
        List<Integer> forceIDs = sortedAvailableForceIDs.get(selectedLocation);
        int forceIndex = randomInt(forceIDs.size());
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
        int tactics = scenario.getBackingScenario().getLanceCommanderSkill(S_TACTICS, campaign);
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
                CombatTeam lance = campaign.getCombatTeamsTable().get(commanderUnit.getForceId());

                return (lance != null) && lance.getRole().isFrontline();
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
                case BATTLE_ARMOR:
                case INFANTRY:
                case MEK:
                case TANK:
                case PROTOMEK:
                case VTOL:
                    retVal.get(AllGroundTerrain).add(forceID);
                    break;
                case AEROSPACEFIGHTER:
                    retVal.get(Space).add(forceID);
                    // intentional fallthrough here, ASFs can go to atmospheric maps too
                case CONV_FIGHTER:
                    retVal.get(LowAtmosphere).add(forceID);
                    break;
            }
        }
        return retVal;
    }

    /**
     * Generates a StratCon scenario at the specified coordinates for the given force on the specified track.
     * The scenario is determined based on a random template suitable for the unit type of the specified force,
     * and it is optionally configured with a deployment delay.
     *
     * <p>This method selects a random scenario template based on the primary unit type of the force,
     * then delegates the scenario creation and configuration to another overloaded {@code generateScenario} method
     * which handles specific template-based scenario generation.</p>
     *
     * @param campaign           the {@link Campaign} managing the overall gameplay state
     * @param contract           the {@link AtBContract} governing the StratCon campaign
     * @param track              the {@link StratconTrackState} where the scenario is placed
     * @param forceID            the ID of the force for which the scenario is generated
     * @param coords             the {@link StratconCoords} specifying where the scenario will be generated
     * @param daysTilDeployment  the number of days until the scenario is deployed; if {@code null},
     *                          deployment dates are determined dynamically
     * @return the generated {@link StratconScenario}, or {@code null} if scenario generation fails
     */
    private static @Nullable StratconScenario generateScenario(Campaign campaign, AtBContract contract,
                                                               StratconTrackState track, int forceID,
                                                               StratconCoords coords,
                                                               @Nullable Integer daysTilDeployment) {
        int unitType = campaign.getForce(forceID).getPrimaryUnitType(campaign);
        ScenarioTemplate template = StratconScenarioFactory.getRandomScenario(unitType);
        // useful for debugging specific scenario types
        // template = StratconScenarioFactory.getSpecificScenario("Defend Grounded
        // Dropship.xml");

        return generateScenario(campaign, contract, track, forceID, coords, template, daysTilDeployment);
    }

    /**
     * Generates a StratCon scenario at the specified coordinates for the given force on the specified track,
     * using the provided scenario template. The scenario is customized and registered with the campaign.
     *
     * <p>The generated scenario is configured as follows:
     * <ul>
     *     <li>If no template is provided, a random template is chosen based on the unit type of the given force.</li>
     *     <li>If provided, deployment dates are explicitly set. Otherwise, dates are determined dynamically.</li>
     *     <li>Global modifiers, facility modifiers, attached unit modifiers, and allied force modifiers
     *         are applied as appropriate.</li>
     *     <li>The scenario is marked as unresolved and is registered with the campaign and track.</li>
     * </ul>
     * This method also handles special conditions:
     * <ul>
     *     <li>Forces with specific command rights (House or Integrated) will mark the scenario as required.</li>
     *     <li>If no force is provided, the scenario is treated as part of contract initialization (e.g., allied forces).</li>
     * </ul>
     *
     * @param campaign           the {@link Campaign} managing the gameplay state
     * @param contract           the {@link AtBContract} governing the StratCon campaign
     * @param track              the {@link StratconTrackState} to which the scenario belongs
     * @param forceID            the ID of the force for which the scenario is generated, or
     * {@link Force#FORCE_NONE} if none
     * @param coords             the {@link StratconCoords} specifying where the scenario will be placed
     * @param template           the {@link ScenarioTemplate} to use for scenario generation; if
     * {@code null}, a random one is selected
     * @param daysTilDeployment  the number of days until the scenario is deployed; if {@code null},
     *                          dates will be dynamically set
     * @return the generated {@link StratconScenario}, or {@code null} if scenario generation failed
     */
    static @Nullable StratconScenario generateScenario(Campaign campaign, AtBContract contract,
                                                       StratconTrackState track, int forceID,
                                                       StratconCoords coords, ScenarioTemplate template,
                                                       @Nullable Integer daysTilDeployment) {
        StratconScenario scenario = new StratconScenario();

        if (template == null) {
            int unitType = MEK;

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

        // do any facility or global modifiers
        applyFacilityModifiers(scenario, track, coords);
        applyGlobalModifiers(scenario, contract.getStratconCampaignState());

        AtBDynamicScenarioFactory.setScenarioModifiers(campaign.getCampaignOptions(),
            scenario.getBackingScenario());
        scenario.setCurrentState(ScenarioState.UNRESOLVED);

        if (daysTilDeployment == null) {
            setScenarioDates(track, campaign, scenario);
        } else {
            setScenarioDates(daysTilDeployment, track, campaign, scenario);
        }

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
            } else if (facility.isVisible() || (randomInt(100) <= 75)) {
                modifierIDs = facility.getSharedModifiers();
            }

            for (String modifierID : modifierIDs) {
                AtBScenarioModifier modifier = AtBScenarioModifier.getScenarioModifier(modifierID);
                if (modifier == null) {
                    logger.error(String.format("Modifier %s not found for facility %s", modifierID,
                            facility.getFormattedDisplayableName()));
                    continue;
                }

                modifier.setAdditionalBriefingText('(' + facility.getDisplayableName() + ") "
                    + modifier.getAdditionalBriefingText());
                scenario.getBackingScenario().addScenarioModifier(modifier);
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
                if (scenario.isTurningPoint()) {
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
     * track's properties and current campaign date
     */
    private static void setScenarioDates(StratconTrackState track, Campaign campaign, StratconScenario scenario) {
        int deploymentDay = track.getDeploymentTime() < 7 ? randomInt(7 - track.getDeploymentTime()) : 0;
        setScenarioDates(deploymentDay, track, campaign, scenario);
    }

    /**
     * Worker function that sets scenario deploy/battle/return dates based on the
     * track's properties and current campaign date. Takes a fixed deployment day of X days from
     * campaign's today date.
     */
    private static void setScenarioDates(int deploymentDay, StratconTrackState track, Campaign campaign,
            StratconScenario scenario) {
        // set up deployment day, battle day, return day here
        // safety code to prevent attempts to generate random int with upper bound of 0
        // which is apparently illegal
        int battleDay = deploymentDay
                + (track.getDeploymentTime() > 0 ? randomInt(track.getDeploymentTime()) : 0);
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

        return ((unitType == AEROSPACEFIGHTER) ||
                (unitType == CONV_FIGHTER) ||
                (unitType == DROPSHIP) ||
                (unitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX)) &&
                (template.getStartingAltitude() > 0);
    }

    /**
     * Determines whether the force in question has the same primary unit type as
     * the force template.
     *
     * @return Whether or not the unit types match.
     */
    public static boolean forceCompositionMatchesDeclaredUnitType(int primaryUnitType, int unitType) {
        // special cases are "ATB_MIX" and "ATB_AERO_MIX", which encompass multiple unit types
        if (unitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX) {
            return primaryUnitType < JUMPSHIP;
        } else if (unitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX) {
            return primaryUnitType >= CONV_FIGHTER;
        } else {
            return primaryUnitType == unitType;
        }
    }

    /**
     * Retrieves a list of force IDs for all combat teams that are both available and suitable for
     * deployment under a specific contract.
     *
     * <p>This method filters out combat teams that do not meet the following criteria:
     * <ul>
     *   <li>The combat team must be assigned to the specified contract.</li>
     *   <li>The combat team must not currently be deployed.</li>
     *   <li>The combat team must have a role other than "In Reserve".</li>
     * </ul>
     *
     * @param campaign The {@link Campaign} object containing all contracts, formations, and states.
     * @param contract The {@link AtBContract} under which the combat teams are evaluated for deployment.
     * @return A {@link List} of force IDs ({@link Integer}) corresponding to all suitable combat teams ready for deployment.
     */
    public static List<Integer> getAvailableForceIDs(Campaign campaign, AtBContract contract) {
        // First, build a list of all combat teams in the campaign
        ArrayList<CombatTeam> combatTeams = campaign.getAllCombatTeams();

        if (combatTeams.isEmpty()) {
            // If we don't have any combat teams, there is no point in continuing, so we exit early
            return Collections.emptyList();
        }

        // Finally, loop through the available combat teams adding those found to be suitable to
        // the appropriate list.
        List<Integer> suitableForces = new ArrayList<>();
        for (CombatTeam combatTeam : combatTeams) {
            // If the combat team isn't assigned to the current contract, it isn't eligible to be deployed
            if (!Objects.equals(contract, combatTeam.getContract(campaign))) {
                continue;
            }

            // So long as the combat team isn't In Reserve or Auxiliary, they are eligible to be deployed
            CombatRole combatRole = combatTeam.getRole();
            if (!combatRole.isReserve() && !combatRole.isAuxiliary()) {

                if (!combatRole.isTraining() || contract.getContractType().isCadreDuty()) {
                    suitableForces.add(combatTeam.getForceId());
                }
            }
        }

        return suitableForces;
    }

    /**
     * Retrieves a list of all force IDs eligible for deployment to a scenario.
     * <p>
     * This method evaluates all forces in the specified {@link Campaign} and identifies those that
     * meet the criteria for deployment.
     * <p>
     * The criteria ensure that the forces:
     * <ul>
     *   <li>Are combat-capable (i.e., not auxiliary or in reserve).</li>
     *   <li>Are not currently assigned to a track (except for the current track if deploying as
     *   reinforcements).</li>
     *   <li>Are not already deployed to a scenario.</li>
     *   <li>Have not previously failed to deploy (if deploying as reinforcements).</li>
     *   <li>Match the specified unit type.</li>
     * </ul>
     * Forces that meet all conditions are returned as a list of unique force IDs.
     *
     * @param unitType the desired type of unit to evaluate for deployment eligibility.
     * @param campaign the {@link Campaign} containing the forces to evaluate.
     * @param currentTrack the {@link StratconTrackState} representing the current track, used to
     *                    filter eligible forces.
     * @param reinforcements {@code true} if the forces are being deployed as reinforcements;
     *                                   otherwise {@code false}.
     * @param currentScenario the current {@link StratconScenario}, if any, used to exclude failed
     *                       reinforcements. Can be {@code null}.
     * @param campaignState the current {@link StratconCampaignState} representing the campaign
     *                      state for further filtering of eligible forces.
     * @return a {@link List} of unique force IDs that meet all deployment criteria.
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

        for (CombatTeam formation : campaign.getCombatTeamsTable().values()) {
            Force force = campaign.getForce(formation.getForceId());

            if (force == null) {
                continue;
            }

            if (force.isDeployed()) {
                continue;
            }

            if (formation.getRole().isReserve()) {
                continue;
            }

            if (formation.getRole().isAuxiliary() && !reinforcements) {
                continue;
            }

            int primaryUnitType = force.getPrimaryUnitType(campaign);
            boolean noReinforcementRestriction = !reinforcements ||
                (getReinforcementType(force.getId(), currentTrack, campaign, campaignState) != ReinforcementEligibilityType.NONE);

            if ((force.getScenarioId() <= 0)
                && !force.getAllUnits(true).isEmpty()
                && !forcesInTracks.contains(force.getId())
                && forceCompositionMatchesDeclaredUnitType(primaryUnitType, unitType)
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
            boolean isEligibleInfantry = ((u.getEntity().getUnitType() == INFANTRY)
                    || (u.getEntity().getUnitType() == BATTLE_ARMOR)) && !u.isUnmanned();

            boolean isEligibleGunEmplacement = u.getEntity().getUnitType() == GUN_EMPLACEMENT;

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
    public static List<Unit> getEligibleLeadershipUnits(Campaign campaign, ArrayList<Integer> forceIDs,
                                                        int leadershipSkill) {
        List<Unit> eligibleUnits = new ArrayList<>();

        // If there is no leadership skill, we shouldn't continue
        if (leadershipSkill <= 0) {
            return eligibleUnits;
        }

        // The criteria are as follows:
        // - unit is eligible to be spawned on the scenario type
        // - unit has a lower BV than the BV budget granted from Leadership
        // Leadership budget is capped at 5 levels
        int totalBudget = min(BASE_LEADERSHIP_BUDGET * leadershipSkill, BASE_LEADERSHIP_BUDGET * 5);

        int primaryUnitType = getPrimaryUnitType(campaign, forceIDs);

        // If there are no units (somehow), we've no reason to continue
        if (primaryUnitType == -1) {
            return eligibleUnits;
        }

        int generalUnitType = convertSpecificUnitTypeToGeneral(primaryUnitType);

        for (UUID unitId : campaign.getForce(0).getAllUnits(true)) {
            Unit unit = campaign.getUnit(unitId);
            if (unit == null) {
                continue;
            }

            // the general idea is that we want something that can be deployed to the scenario -
            // e.g., no infantry on air scenarios etc.
            boolean validUnitType = (forceCompositionMatchesDeclaredUnitType(unit.getEntity().getUnitType(),
                        generalUnitType));

            if (validUnitType
                && !unit.isDeployed()
                && !unit.isMothballed()
                && (unit.getEntity().calculateBattleValue(true, true) <= totalBudget)
                && (unit.checkDeployment() == null)
                && !isUnitDeployedToStratCon(unit)) {
                eligibleUnits.add(unit);
            }
        }

        return eligibleUnits;
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
     * Calculates the majority unit type for the forces given the IDs.
     */
    private static int getPrimaryUnitType(Campaign campaign, ArrayList<Integer> forceIDs) {
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
            return ReinforcementEligibilityType.NONE;
        }

        // TODO: If the force has completed a scenario which allows it,
        // it can deploy "for free" (ReinforcementEligibilityType.ChainedScenario)

        // if the force is in 'fight' stance, it'll be able to deploy using 'fight lance' rules
        if (campaign.getCombatTeamsTable().containsKey(forceID)) {
            Hashtable<Integer, CombatTeam> combatTeamsTable = campaign.getCombatTeamsTable();
            CombatTeam formation = combatTeamsTable.get(forceID);

            if (formation == null) {
                return ReinforcementEligibilityType.NONE;
            }

            if (campaignState.getSupportPoints() > 0) {
                if (formation.getRole().isManeuver() || formation.getRole().isAuxiliary()) {
                    return AUXILIARY;
                } else {
                    return ReinforcementEligibilityType.REGULAR;
                }
            }
        }

        return ReinforcementEligibilityType.NONE;
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
        boolean alliedFacility = (facility != null) && (facility.getOwner() == Allied);

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
            return -1;
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

                    if (scenario.isTurningPoint() && !backingScenario.getStatus().isDraw()) {
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

                    if (backingScenario.getStratConScenarioType().isLosTech()) {
                        if (victory) {
                            int roll = randomInt(10);
                            StarLeagueCache cache = new StarLeagueCache(campaign, ((AtBContract) mission),
                                CacheType.TRASH_CACHE.ordinal());

                            // The rumor is a dud
//                            if (false) { // TODO replace placeholder value
//                                cache.createDudDialog(track, scenario);
//                            } else {
//                                if (Objects.equals(cache.getFaction().getShortName(), "SL")) {
//                                    cache.createProposalDialog();
//                                }
//                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Processes completion of a Stratcon scenario that is linked to another scenario
     * pulls forces off completed scenario and moves them to linked one.
     * 
     * Should only be used after a scenario is resolved
     */
    public static void linkedScenarioProcessing(ResolveScenarioTracker tracker, List<Integer> forces) {
        Scenario nextScenario = tracker.getCampaign().getScenario(tracker.getScenario().getLinkedScenario());

        if (nextScenario instanceof AtBScenario nextAtBScenario) {
            StratconCampaignState campaignState = nextAtBScenario.getContract(tracker.getCampaign())
                    .getStratconCampaignState();
            if (campaignState == null) {
                return;
            }
            for (StratconTrackState track : campaignState.getTracks()) {
                if (track.getBackingScenariosMap().containsKey(nextScenario.getId())) {
                    StratconScenario scenario = track.getBackingScenariosMap().get(nextScenario.getId());
                    for (int forceID : forces) {
                        track.unassignForce(forceID);
                        nextScenario.addForces(forceID);
                    }

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
        if (facility.getOwner() == Allied) {
            facility.setOwner(Opposing);
        } else {
            facility.setOwner(Allied);
        }
    }

    /**
     * Worker function that goes through a track and undeploys any forces where the
     * return date is on or before the given date.
     */
    public static void processTrackForceReturnDates(StratconTrackState track, Campaign campaign) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AtBStratCon",
            MekHQ.getMHQOptions().getLocale());

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

                campaign.addReport(String.format(resources.getString("force.undeployed"),
                    force.getName()));
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
                if (scenario.isTurningPoint()) {
                    campaignState.updateVictoryPoints(-1);
                }

                track.removeScenario(scenario);

                if (scenario.getBackingScenario().getStratConScenarioType().isResupply()) {
                    return true;
                }

                StratconFacility localFacility = track.getFacility(scenario.getCoords());
                if (localFacility != null) {
                    // if the ignored scenario was on top of an allied facility
                    // then it'll get captured, and the player will possibly lose a SO
                    if (localFacility.getOwner() == Allied) {
                        localFacility.setOwner(Opposing);
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

                        int daysForward = max(1, track.getDeploymentTime());

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
        Campaign campaign = ev.getCampaign();

        // don't do any of this if StratCon isn't turned on
        if (!campaign.getCampaignOptions().isUseStratCon()) {
            return;
        }

        LocalDate today = campaign.getLocalDate();
        boolean isMonday = today.getDayOfWeek() == DayOfWeek.MONDAY;
        boolean isStartOfMonth = today.getDayOfMonth() == 1;

        // run scenario generation routine for every track attached to an active
        // contract
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            StratconCampaignState campaignState = contract.getStratconCampaignState();

            if (campaignState != null) {
                for (StratconTrackState track : campaignState.getTracks()) {
                    cleanupPhantomScenarios(track);

                    // check if some of the forces have finished deployment
                    // please do this before generating scenarios for track
                    // to avoid unintentionally cleaning out integrated force deployments on
                    // 0-deployment-length tracks
                    processTrackForceReturnDates(track, campaign);

                    processFacilityEffects(track, campaignState, isStartOfMonth);

                    // loop through scenarios - if we haven't deployed in time,
                    // fail it and apply consequences
                    for (StratconScenario scenario : track.getScenarios().values()) {
                        if ((scenario.getDeploymentDate() != null) &&
                                scenario.getDeploymentDate().isBefore(campaign.getLocalDate()) &&
                                scenario.getPrimaryForceIDs().isEmpty()) {
                            processIgnoredScenario(scenario, campaignState);
                        }
                    }

                    // on monday, generate new scenario dates
                    if (isMonday) {
                        generateScenariosDatesForWeek(campaign, campaignState, contract, track);
                    }
                }

                List<LocalDate> weeklyScenarioDates = campaignState.getWeeklyScenarios();

                if (weeklyScenarioDates.contains(today)) {
                    int scenarioCount = 0;
                    for (LocalDate date : weeklyScenarioDates) {
                        if (date.equals(today)) {
                            scenarioCount++;
                        }
                    }
                    weeklyScenarioDates.removeIf(date -> date.equals(today));

                    generateDailyScenariosForTrack(campaign, campaignState, contract, scenarioCount);
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
