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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.board.Coords.ALL_DIRECTIONS;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.units.UnitType.CONV_FIGHTER;
import static megamek.common.units.UnitType.JUMPSHIP;
import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.mission.AtBDynamicScenarioFactory.finalizeScenario;
import static mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment.Allied;
import static mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment.Opposing;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.AllGroundTerrain;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.LowAtmosphere;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.Space;
import static mekhq.campaign.mission.ScenarioMapParameters.MapLocation.SpecificGroundTerrain;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.personnel.PersonnelOptions.ADMIN_COORDINATOR;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_TACTICS;
import static mekhq.campaign.stratCon.StratConContractInitializer.getUnoccupiedCoords;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementEligibilityType.AUXILIARY;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementResultsType.DELAYED;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementResultsType.FAILED;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementResultsType.INSTANT;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementResultsType.INTERCEPTED;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementResultsType.SUCCESS;
import static mekhq.campaign.stratCon.StratConScenarioFactory.convertSpecificUnitTypeToGeneral;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.Minefield;
import megamek.common.event.Subscribe;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.StratConDeploymentEvent;
import mekhq.campaign.events.scenarios.ScenarioChangedEvent;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.mission.enums.ScenarioType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.ScoutingSkills;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.stratCon.StratConScenario.ScenarioState;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.EntityUtilities;
import mekhq.utilities.ReportingUtilities;
import org.apache.commons.math3.util.Pair;

/**
 * This class contains "rules" logic for the AtB-StratCon state
 *
 * @author NickAragua
 */
public class StratConRulesManager {
    public final static int BASE_LEADERSHIP_BUDGET = 500;

    private static final MMLogger LOGGER = MMLogger.create(StratConRulesManager.class);

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
         * The Combat Team's deployment orders are "Frontline" or "Auxiliary". We pay a support point and make an
         * enhanced roll
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
         * The reinforcements arrive instantly.
         */
        INSTANT,

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
     * The method first determines the number of turning point scenario rolls based on the required lance count from the
     * track, then multiplies that count depending on the contract's morale level.
     * <p>
     * If auto-assign for lances is enabled, and either there are no available forces or the number of weekly scenarios
     * equals or exceeds the number of available forces, it breaks from the scenario generation loop.
     * <p>
     * For each scenario, a scenario odds target number is calculated, and a roll is made against this target. If the
     * roll is less than the target number, a new weekly scenario is created with a random date within the week.
     *
     * @param campaign      The campaign.
     * @param campaignState The state of the StratCon campaign.
     * @param contract      The AtBContract for the campaign.
     * @param track         The StratCon campaign track.
     */
    public static void generateScenariosDatesForWeek(Campaign campaign, StratConCampaignState campaignState,
          AtBContract contract, StratConTrackState track) {
        // maps scenarios to force IDs
        int scenarioRolls = track.getRequiredLanceCount();
        for (int scenarioIndex = 0; scenarioIndex < scenarioRolls; scenarioIndex++) {
            int targetNum = calculateScenarioOdds(track, contract, false);
            int roll = randomInt(100);

            if (roll < targetNum) {
                LocalDate scenarioDate = campaign.getLocalDate().plusDays(randomInt(7));
                campaignState.addWeeklyScenario(scenarioDate);
                LOGGER.info("StratCon Weekly Scenario Roll: {} vs. {} ({})",
                      roll,
                      targetNum, scenarioDate);
            } else {
                LOGGER.info("StratCon Weekly Scenario Roll: {} vs. {}", roll, targetNum);
            }
        }
    }

    /**
     * This method generates a weekly scenario for a specific track.
     * <p>
     * First, it initializes empty collections for generated scenarios and available forces, and determines whether
     * lances are auto-assigned.
     * <p>
     * Then it generates a requested number of scenarios. If auto-assign is enabled and there are no available forces,
     * it breaks from the scenario generation loop.
     * <p>
     * For each scenario, it first tries to create a scenario for existing forces on the track. If that is not possible,
     * it selects random force, removes it from available forces, and creates a scenario for it. For any scenario, if it
     * is under liaison command, it may set the scenario as required and attaches the liaison.
     * <p>
     * After scenarios are generated, OpFors, events, etc. are finalized for each scenario.
     *
     * @param campaign      The current campaign.
     * @param campaignState The relevant StratCon campaign state.
     * @param contract      The relevant contract.
     * @param scenarioCount The number of scenarios to generate.
     */
    public static void generateDailyScenariosForTrack(Campaign campaign, StratConCampaignState campaignState,
          AtBContract contract, int scenarioCount) {
        // get this list just so we have it available
        List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract, false);

        Map<MapLocation, List<Integer>> sortedAvailableForceIDs = sortForcesByMapType(availableForceIDs,
              campaign.getHangar(),
              campaign.getAllForces());

        for (int scenarioIndex = 0; scenarioIndex < scenarioCount; scenarioIndex++) {
            List<StratConTrackState> tracks = campaignState.getTracks();
            StratConTrackState track = campaignState.getTracks().get(0);

            if (tracks.size() > 1) {
                track = getRandomItem(tracks);
            }

            final int deploymentDelay = track.getDeploymentTime();
            final LocalDate scenarioTargetDate = campaign.getLocalDate().plusDays(deploymentDelay);
            final LocalDate contractEnd = campaignState.getContract().getEndingDate();

            if (!scenarioTargetDate.isBefore(contractEnd)) {
                LOGGER.info("Skipping scenario because it is on or after the contract end date.");
                return;
            }

            StratConCoords scenarioCoords = getUnoccupiedCoords(track, true, true, true);

            if (scenarioCoords == null) {
                LOGGER.warn("Target track is full, skipping scenario generation");
                continue;
            }

            // if forces are already assigned to these coordinates, use those instead of randomly
            // selected ones
            StratConScenario scenario;
            if (track.getAssignedCoordForces().containsKey(scenarioCoords)) {
                scenario = generateScenarioForExistingForces(scenarioCoords,
                      track.getAssignedCoordForces().get(scenarioCoords),
                      contract,
                      campaign,
                      track);
                // otherwise, pick a random force from the avail
            } else {
                int randomForceID = getRandomItem(availableForceIDs);

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
                finalizeBackingScenario(campaign, contract, track, false, scenario);
            }
        }
    }

    /**
     * Generates a new StratCon scenario with default behavior.
     *
     * <p>This is a simplified utility method that generates a scenario without requiring detailed configuration of the
     * track or scenario template.</p>
     *
     * <p>This method delegates to the more advanced
     * {@link #generateExternalScenario(Campaign, AtBContract, StratConTrackState, StratConCoords, ScenarioTemplate,
     * boolean, boolean, boolean, Integer)} method with default parameters, selecting a random track and scenario
     * configurations automatically.</p>
     *
     * <p><b>Note:</b> When using this method scenarios cannot spawn on top of player forces or facilities.</p>
     *
     * @param campaign The current {@link Campaign} for which to generate the scenario.
     * @param contract The {@link AtBContract} associated with the scenario.
     *
     * @return A newly generated {@link StratConScenario}, or {@code null} if scenario creation fails due to constraints
     *       such as no available tracks or valid coordinates.
     */
    public static @Nullable StratConScenario generateExternalScenario(Campaign campaign, AtBContract contract) {
        return generateExternalScenario(campaign, contract, null, null, null, false, false, false, null);
    }

    /**
     * Generates a new StratCon scenario using advanced configuration options.
     *
     * <p>Supports detailed control over various aspects of the scenario, including placement on a specific track,
     * selection of coordinates, use of specific templates, and strategic constraints.</p>
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *   <li>If no track is specified, selects a random track for the scenario.</li>
     *   <li>Determines target coordinates based on availability and input parameters:
     *       <ul>
     *         <li>If coordinates are provided, validates their availability.</li>
     *         <li>If coordinates are {@code null}, searches for unoccupied coordinates based on track status,
     *             facility ownership, player-assigned forces, and strategic weighting.</li>
     *       </ul>
     *   </li>
     *   <li>Selects available forces for the scenario:
     *       <ul>
     *         <li>If forces are already assigned to the target coordinates, uses them for scenario creation.</li>
     *         <li>Otherwise, selects random forces based on availability and optional constraints set by the
     *             scenario's template.</li>
     *       </ul>
     *   </li>
     *   <li>Finalizes the scenario, integrating it into the campaign and contract settings.</li>
     * </ol>
     *
     * <p>During scenario generation, constraints are applied based on the input parameters to ensure valid
     * placement and force selection. If no valid setup is found or the track is already full, the scenario
     * generation will fail, returning {@code null}.</p>
     *
     * @param campaign                  The current {@link Campaign} under which the scenario is generated.
     * @param contract                  The {@link AtBContract} associated with the scenario.
     * @param track                     The specific {@link StratConTrackState} where the scenario should be placed, or
     *                                  {@code null} to allow selection of a random track.
     * @param scenarioCoords            The target {@link StratConCoords} for placing the scenario, or {@code null} to
     *                                  select a random, unoccupied coordinate. If specified, {@code track} must not be
     *                                  {@code null}.
     * @param template                  The {@link ScenarioTemplate} to use for scenario configuration, or {@code null}
     *                                  to select one randomly.
     * @param allowPlayerFacilities     A {@code boolean} indicating whether the scenario can be placed on top of
     *                                  player-occupied facilities.
     * @param allowPlayerForces         A {@code boolean} indicating whether coordinates hosting player forces are
     *                                  considered valid for scenario placement.
     * @param emphasizeStrategicTargets A {@code boolean} that increases the likelihood of selecting strategic targets,
     *                                  such as <b>PLAYER</b>-held facilities, for scenario placement.
     * @param daysTilDeployment         An {@link Integer} specifying the number of days until the scenario occurs, or
     *                                  {@code null} to select a random date within the next 7 days.
     *
     * @return A newly created {@link StratConScenario}, or {@code null} if scenario generation fails due to invalid
     *       configurations, insufficient forces, or a lack of valid coordinates.
     *
     * @throws IllegalArgumentException If {@code scenarioCoords} is specified while {@code track} is {@code null}.
     */
    public static @Nullable StratConScenario generateExternalScenario(Campaign campaign, AtBContract contract,
          @Nullable StratConTrackState track, @Nullable StratConCoords scenarioCoords,
          @Nullable ScenarioTemplate template, boolean allowPlayerFacilities, boolean allowPlayerForces,
          boolean emphasizeStrategicTargets, @Nullable Integer daysTilDeployment) {
        // If we're not generating for a specific track, randomly pick one.
        if (track == null) {
            track = getRandomTrack(contract);

            if (track == null) {
                LOGGER.error("Failed to generate a random track, aborting scenario generation.");
                return null;
            }
        }

        // Grab the available lances and sort them by map type
        List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract, false);
        Map<MapLocation, List<Integer>> sortedAvailableForceIDs = sortForcesByMapType(availableForceIDs,
              campaign.getHangar(),
              campaign.getAllForces());

        // Select the target coords.
        if (scenarioCoords == null) {
            scenarioCoords = getUnoccupiedCoords(track,
                  allowPlayerFacilities,
                  allowPlayerForces,
                  emphasizeStrategicTargets);
        }

        if (scenarioCoords == null) {
            LOGGER.warn("Target track is full, aborting scenario generation.");
            return null;
        }

        // If forces are already assigned to the target coordinates, use those instead of randomly
        // selected a new force
        StratConScenario scenario = null;
        if (track.getAssignedCoordForces().containsKey(scenarioCoords)) {
            scenario = generateScenarioForExistingForces(scenarioCoords,
                  track.getAssignedCoordForces().get(scenarioCoords),
                  contract,
                  campaign,
                  track,
                  template,
                  daysTilDeployment);
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


            scenario = setupScenario(scenarioCoords,
                  randomForceID,
                  campaign,
                  contract,
                  track,
                  template,
                  campaign.getCampaignOptions().isUseStratConMaplessMode(),
                  daysTilDeployment);
        }

        if (scenario == null) {
            return null;
        }

        // We end by finalizing the scenario
        finalizeBackingScenario(campaign, contract, track, false, scenario);

        // We return the scenario in case we want to make specific changes.
        return scenario;
    }

    /**
     * Generates a reinforcement interception scenario for a given StratCon track. An interception scenario is set up at
     * unoccupied coordinates on the track. If the scenario setup is successful, it is finalized and the deployment date
     * for the scenario is set as the current date.
     *
     * @param campaign         the current campaign
     * @param contract         the {@link AtBContract} for which the scenario is created
     * @param track            the {@link StratConTrackState} where the scenario is located, or {@code null} if not
     *                         located on a track
     * @param template         the {@link ScenarioTemplate} used to create the scenario
     * @param interceptedForce the {@link Force} that's being intercepted in the scenario
     */
    public static @Nullable void generateReinforcementInterceptionScenario(Campaign campaign,
          StratConScenario linkedScenario, AtBContract contract, StratConTrackState track, ScenarioTemplate template,
          Force interceptedForce) {
        StratConCoords scenarioCoords = getUnoccupiedCoords(track);

        StratConScenario scenario = setupScenario(scenarioCoords,
              interceptedForce.getId(),
              campaign,
              contract,
              track,
              template,
              true,
              0);

        if (scenario == null) {
            LOGGER.error("Failed to generate a random interception scenario, aborting scenario generation.");
            return;
        }

        finalizeBackingScenario(campaign, contract, track, true, scenario);
        scenario.setActionDate(campaign.getLocalDate());
        scenario.getBackingScenario().setStatus(ScenarioStatus.CURRENT);
        scenario.getBackingScenario().setLinkedScenarioID(linkedScenario.getBackingScenario().getId());
    }

    /**
     * Adds a hidden {@link StratConScenario} to the specified contract within the current campaign.
     *
     * <p>The added scenario is cloaked, meaning it will not be visible until discovered by the player.
     * If no specific {@link StratConTrackState} or {@link ScenarioTemplate} is provided, they will be selected
     * randomly. The scenario is created without preassigned forces and is marked as a strategic objective with specific
     * strategic behavior.</p>
     *
     * <p><strong>Note:</strong> This method is a utility function. While it may not currently be in use, it
     * is intended for future usage and should not be deprecated or removed.</p>
     *
     * @param campaign                  The current campaign in which the scenario is being added.
     * @param contract                  The {@link AtBContract} associated with the scenario.
     * @param trackState                The {@link StratConTrackState} where the scenario will occur. If {@code null}, a
     *                                  random track is selected.
     * @param template                  The {@link ScenarioTemplate} used for scenario generation. If {@code null}, the
     *                                  default template is used.
     * @param allowPlayerFacilities     A flag indicating whether player facilities can influence scenario placement.
     * @param allowPlayerForces         A flag indicating whether player forces can influence scenario placement.
     * @param emphasizeStrategicTargets A flag indicating whether strategic targets are prioritized during placement.
     * @param daysTilDeployment         The number of days until scenario deployment, or {@code null} to randomly pick a
     *                                  day within the next 7 days.
     *
     * @return The created {@link StratConScenario}, or {@code null} if:
     *       <ul>
     *           <li>No {@link ScenarioTemplate} is available.</li>
     *           <li>All coordinates in the selected {@link StratConTrackState} are occupied and scenario placement is not possible.</li>
     *       </ul>
     */
    public static @Nullable StratConScenario addHiddenExternalScenario(Campaign campaign, AtBContract contract,
          @Nullable StratConTrackState trackState, @Nullable ScenarioTemplate template, boolean allowPlayerFacilities,
          boolean allowPlayerForces, boolean emphasizeStrategicTargets, @Nullable Integer daysTilDeployment) {
        // If we're not generating for a specific track, randomly pick one.
        if (trackState == null) {
            trackState = getRandomTrack(contract);

            if (trackState == null) {
                LOGGER.error(
                      "Failed to generate a random track for addHiddenExternalScenario, aborting scenario generation.");
                return null;
            }
        }

        StratConCoords coords = getUnoccupiedCoords(trackState,
              allowPlayerFacilities,
              allowPlayerForces,
              emphasizeStrategicTargets);

        if (coords == null) {
            LOGGER.error("Unable to place objective scenario on track {}, as all coords were occupied. Aborting.",
                  trackState.getDisplayableName());
            return null;
        }

        // create scenario - don't assign a force yet
        StratConScenario scenario = StratConRulesManager.generateScenario(campaign,
              contract,
              trackState,
              FORCE_NONE,
              coords,
              template,
              daysTilDeployment);

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
     * Fetches a random {@link StratConTrackState} from the {@link StratConCampaignState}. If no tracks are present, it
     * logs an error message and returns {@code null}.
     *
     * @param contract The {@link AtBContract} from which the track state will be fetched.
     *
     * @return The randomly chosen {@link StratConTrackState}, or {@code null} if no tracks are available.
     */
    public static @Nullable StratConTrackState getRandomTrack(AtBContract contract) {
        List<StratConTrackState> tracks = contract.getStratconCampaignState().getTracks();
        Random rand = new Random();

        if (!tracks.isEmpty()) {
            return tracks.get(rand.nextInt(tracks.size()));
        } else {
            LOGGER.error("No tracks available. Unable to fetch random track");
            return null;
        }
    }

    /**
     * Finalizes the backing scenario, setting up the OpFor, scenario parameters, and other necessary steps.
     *
     * @param campaign         The current campaign.
     * @param contract         The contract associated with the scenario.
     * @param track            The relevant {@link StratConTrackState}.
     * @param autoAssignLances Flag indicating whether lances are to be auto-assigned.
     * @param scenario         The {@link StratConScenario} scenario to be finalized.
     */
    public static void finalizeBackingScenario(Campaign campaign, AtBContract contract,
          @Nullable StratConTrackState track, boolean autoAssignLances, StratConScenario scenario) {
        final AtBDynamicScenario backingScenario = scenario.getBackingScenario();

        // First determine if the scenario is a Turning Point (that win/lose will affect CVP)
        determineIfTurningPointScenario(contract, scenario);
        if (!scenario.isTurningPoint()) {
            determineIfCrisisScenario(contract.getMoraleLevel(), backingScenario);
        }

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
     * This method determines the type of trainees to be added to the scenario by evaluating the map location parameter
     * of the scenario's template. Depending on whether the battle is an air or space battle versus a ground battle, the
     * appropriate Cadre Duty trainees scenario modifier is applied to the backing scenario.
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
     * @param backingScenario The {@link AtBDynamicScenario} representing the current scenario to which the modifier
     *                        will be applied.
     */
    private static void addCadreDutyTrainees(AtBDynamicScenario backingScenario) {
        final ScenarioTemplate template = backingScenario.getTemplate();
        final MapLocation mapLocation = template.mapParameters.getMapLocation();
        boolean isAirBattle = (mapLocation == LowAtmosphere) || (mapLocation == Space);

        if (isAirBattle) {
            backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(MHQConstants.SCENARIO_MODIFIER_TRAINEES_AIR));
        } else {
            backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(MHQConstants.SCENARIO_MODIFIER_TRAINEES_GROUND));
        }
    }

    /**
     * Determines if a given StratCon scenario should be marked as critical within the context of a contract.
     * <p>
     * This method evaluates the scenario's template, type, and the contract's command rights to decide if the scenario
     * should be flagged as a "turning point." Turning Point scenarios can cause CVP to be increased or decreased.
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
     * @param scenario The {@link StratConScenario} being evaluated to determine if it is a Turning Point.
     */
    private static void determineIfTurningPointScenario(AtBContract contract, StratConScenario scenario) {
        ScenarioType scenarioType = scenario.getBackingScenario().getStratConScenarioType();
        boolean isResupply = scenarioType.isResupply();
        boolean isJailBreak = scenarioType.isJailBreak();

        if (isResupply || isJailBreak) {
            scenario.setTurningPoint(false);
            return;
        }

        boolean isObjective = scenario.isStrategicObjective();

        ContractCommandRights commandRights = contract.getCommandRights();
        switch (commandRights) {
            case INTEGRATED -> {
                scenario.setTurningPoint(true);
                setAttachedUnitsModifier(scenario, contract);
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

    /**
     * Determines whether a scenario should be marked as a crisis scenario based on morale level and random chance.
     *
     * <p>This method evaluates whether a given scenario should be flagged as a "crisis" by performing a random roll
     * based on the current morale level. Crisis scenarios represent critical situations that require immediate
     * attention and may have significant consequences.</p>
     *
     * <p>The determination follows these rules:</p>
     * <ul>
     *   <li>If the scenario is already marked as a "special" scenario type (via {@link ScenarioType#isSpecial()}),
     *       the method returns immediately without making any changes. Special scenarios cannot be crisis scenarios.</li>
     *   <li>Otherwise, a random roll is performed using a die size determined by the current morale level
     *       (via {@link AtBMoraleLevel#getCrisisDieSize()}).</li>
     *   <li>If the roll results in {@code 0} (the minimum value), the scenario is marked as a crisis scenario.</li>
     *   <li>If the roll results in any other value, the scenario is not marked as a crisis.</li>
     * </ul>
     *
     * @param morale          The {@link AtBMoraleLevel} representing the current morale state, which determines the
     *                        size of the die used for the crisis check.
     * @param backingScenario The {@link AtBDynamicScenario} being evaluated. This scenario will be marked as a crisis
     *                        if the conditions are met.
     */
    private static void determineIfCrisisScenario(AtBMoraleLevel morale, AtBDynamicScenario backingScenario) {
        ScenarioType scenarioType = backingScenario.getStratConScenarioType();
        boolean isSpecial = scenarioType.isSpecial();
        if (isSpecial) {
            return;
        }

        int crisisDieSize = morale.getCrisisDieSize();
        int roll = randomInt(crisisDieSize);
        backingScenario.setIsCrisis(roll == 0);
    }

    /**
     * Picks the scenario terrain based on the scenario coordinates' biome Note that "finalizeScenario" currently wipes
     * out temperature/map info so this method must be called afterward.
     */
    public static void setScenarioParametersFromBiome(StratConTrackState track, StratConScenario scenario) {
        StratConCoords coords = scenario.getCoords();
        AtBDynamicScenario backingScenario = scenario.getBackingScenario();
        StratConBiomeManifest biomeManifest = StratConBiomeManifest.getInstance();

        // for non-surface scenarios, we will skip the temperature update
        if (backingScenario.getBoardType() != Scenario.T_SPACE &&
                  backingScenario.getBoardType() != Scenario.T_ATMOSPHERE) {
            backingScenario.setTemperature(track.getTemperature());
        }

        StratConFacility facility = track.getFacility(scenario.getCoords());
        String terrainType;

        // facilities have their own terrain lists
        if (facility != null) {
            int kelvinTemp = track.getTemperature() + StratConContractInitializer.ZERO_CELSIUS_IN_KELVIN;
            StratConBiome facilityBiome;

            // if facility doesn't have a biome temp map or no entry for the current
            // temperature, use the default one
            if (facility.getBiomes().isEmpty() || (facility.getBiomeTempMap().floorEntry(kelvinTemp) == null)) {
                facilityBiome = biomeManifest.getTempMap(StratConBiomeManifest.TERRAN_FACILITY_BIOME)
                                      .floorEntry(kelvinTemp)
                                      .getValue();
            } else {
                facilityBiome = facility.getBiomeTempMap().floorEntry(kelvinTemp).getValue();
            }
            terrainType = facilityBiome.allowedTerrainTypes.get(randomInt(facilityBiome.allowedTerrainTypes.size()));
        } else {
            terrainType = track.getTerrainTile(coords);
        }

        var mapTypes = biomeManifest.getBiomeMapTypes();

        // don't have a map list for the given terrain, leave it alone
        if (!mapTypes.containsKey(terrainType)) {
            return;
        }

        // if we are in space, do not update the map; note that it's ok to do so in low
        // atmosphere
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
     * Worker function that swaps in player units for "player or allied force" templates in a scenario. Looks through
     * the scenario's templates and replaces bot units with player units based on the provided force information and
     * validation rules.
     *
     * @param scenario        The scenario in which player units are to be swapped.
     * @param campaign        The player's campaign containing available units and forces.
     * @param explicitForceID The ID of an explicitly selected force. If {@code FORCE_NONE}, all units in the campaign's
     *                        TO&E are considered.
     */
    private static void swapInPlayerUnits(StratConScenario scenario, Campaign campaign, int explicitForceID) {
        for (ScenarioForceTemplate scenarioForceTemplate : scenario.getScenarioTemplate().getAllScenarioForces()) {
            if (scenarioForceTemplate.getGenerationMethod() != ForceGenerationMethod.PlayerOrFixedUnitCount.ordinal()) {
                continue;
            }

            // Calculate unit count based on bot unit templates and bot force templates
            int unitCount = calculateUnitCount(scenario, campaign, scenarioForceTemplate);

            // Skip if there are no units to substitute
            if (unitCount == 0) {
                continue;
            }

            // Find potential units to substitute based on the explicitForceID
            Collection<Unit> potentialUnits = findPotentialUnits(campaign, explicitForceID);

            // Iterate through the potential units and substitute up to `unitCount` units
            for (Unit unit : potentialUnits) {
                if (isValidUnitForScenario(unit,
                      scenarioForceTemplate,
                      campaign.getCampaignOptions().isUseDropShips())) {
                    scenario.addUnit(unit, scenarioForceTemplate.getForceName(), false);
                    AtBDynamicScenarioFactory.benchAllyUnit(unit.getId(),
                          scenarioForceTemplate.getForceName(),
                          scenario.getBackingScenario());
                    unitCount--;


                    if (unitCount == 0) {
                        break; // Stop once enough units have been substituted
                    }
                }
            }
        }
    }

    /**
     * Calculates the total unit count for a given scenario force template. This includes counting bots generated via
     * unit templates and bot forces that are linked to the specific template.
     *
     * @param scenario              The scenario containing the bot data and force templates.
     * @param campaign              The player's campaign, used to access bot unit details.
     * @param scenarioForceTemplate The template for which the unit count is being calculated.
     *
     * @return The total count of units (both bot and custom bot forces) linked to the template.
     */
    private static int calculateUnitCount(StratConScenario scenario, Campaign campaign,
          ScenarioForceTemplate scenarioForceTemplate) {
        int unitCount = 0;

        // Count bot unit templates that match the force name
        for (ScenarioForceTemplate template : scenario.getBackingScenario().getBotUnitTemplates().values()) {
            if (template.getForceName().equals(scenarioForceTemplate.getForceName())) {
                unitCount++;
            }
        }

        // Add bot force units that match the force name
        for (Entry<BotForce, ScenarioForceTemplate> entry : scenario.getBackingScenario()
                                                                  .getBotForceTemplates()
                                                                  .entrySet()) {
            BotForce key = entry.getKey();
            ScenarioForceTemplate value = entry.getValue();

            if (value.getForceName().equals(scenarioForceTemplate.getForceName())) {
                unitCount += key.getFullEntityList(campaign).size();
            }
        }

        return unitCount;
    }

    /**
     * Retrieves a collection of potential units from the player's campaign for use in substitution. If
     * {@code explicitForceID} is {@code FORCE_NONE}, all units from the campaign's TO&E are considered. Otherwise, the
     * units in the specified force's transport ships are included.
     *
     * @param campaign        The player's campaign containing potential units for substitution.
     * @param explicitForceID The ID of the force the player is explicitly using for substitution. If
     *                        {@code FORCE_NONE}, the entire TO&E is considered.
     *
     * @return A collection of units that are eligible for substitution into the scenario.
     */
    private static Collection<Unit> findPotentialUnits(Campaign campaign, int explicitForceID) {
        Collection<Unit> potentialUnits = new HashSet<>();

        if (explicitForceID == FORCE_NONE) {
            // Include all units in the campaign's TO&E
            List<UUID> allUnits = campaign.getAllUnitsInTheTOE(false);
            // We need to shuffle the list, otherwise the same unit will always be selected
            Collections.shuffle(allUnits);

            for (UUID unitId : allUnits) {
                try {
                    potentialUnits.add(campaign.getUnit(unitId));
                } catch (Exception exception) {
                    LOGGER.error("Error retrieving unit ({}): {}", unitId, exception.getMessage());
                }
            }
        } else {
            // Include only those units transporting the seed force
            Force force = campaign.getForce(explicitForceID);

            if (force == null) {
                return Collections.emptyList();
            }

            for (UUID unitID : force.getUnits()) {
                Unit unit = campaign.getUnit(unitID);

                if (unit == null) {
                    continue;
                }

                if (unit.getTransportShipAssignment() != null) {
                    potentialUnits.add(unit.getTransportShipAssignment().getTransportShip());
                }
            }
        }

        return potentialUnits;
    }

    /**
     * Validates if a given unit can be included in the scenario based on the template's rules and restrictions. It
     * checks unit type, availability, functionality, and specific conditions such as DropShip usage.
     *
     * @param unit                  The unit to validate.
     * @param scenarioForceTemplate The force template containing the rules for unit validation.
     * @param isUsePlayerDropShips  Indicates if DropShips are allowed based on campaign options.
     *
     * @return {@code true} if the unit matches the template's requirements and can be included in the scenario,
     *       {@code false} otherwise.
     */
    private static boolean isValidUnitForScenario(Unit unit, ScenarioForceTemplate scenarioForceTemplate,
          boolean isUsePlayerDropShips) {
        // Check if DropShips are allowed and the correct unit type matches
        if (scenarioForceTemplate.getAllowedUnitType() == 11 && !isUsePlayerDropShips) {
            return false;
        }

        // Validate the unit type, availability, and functionality
        return forceCompositionMatchesDeclaredUnitType(unit.getEntity().getUnitType(),
              scenarioForceTemplate.getAllowedUnitType()) && unit.isAvailable() && unit.isFunctional();
    }

    /**
     * Generates a StratCon scenario for forces already existing at the given coordinates on the provided track.
     *
     * @param scenarioCoords The coordinates where the scenario will be placed on the track.
     * @param forceIDs       The set of force IDs (ideally for the forces already at the specified location).
     * @param contract       The contract associated with the current scenario.
     * @param campaign       The current campaign.
     * @param track          The relevant StratCon track.
     *
     * @return The newly generated {@link StratConScenario}.
     */
    public static @Nullable StratConScenario generateScenarioForExistingForces(StratConCoords scenarioCoords,
          Set<Integer> forceIDs, AtBContract contract, Campaign campaign, StratConTrackState track) {
        return generateScenarioForExistingForces(scenarioCoords, forceIDs, contract, campaign, track, null, null);
    }

    /**
     * Generates a StratCon scenario for forces already existing at the given coordinates on the provided track. This
     * method allows us to specify a specific scenario template.
     *
     * @param scenarioCoords    The coordinates where the scenario will be placed on the track.
     * @param forceIDs          The set of force IDs (ideally for the forces already at the specified location).
     * @param contract          The contract associated with the current scenario.
     * @param campaign          The current campaign.
     * @param track             The relevant StratCon track.
     * @param template          A specific {@link ScenarioTemplate} to use, or {@code null} to select a random
     *                          template.
     * @param daysTilDeployment How many days until the scenario takes place, or {@code null} to pick a random day
     *                          within the next 7 days.
     *
     * @return The newly generated {@link StratConScenario}.
     */
    public static @Nullable StratConScenario generateScenarioForExistingForces(StratConCoords scenarioCoords,
          Set<Integer> forceIDs, AtBContract contract, Campaign campaign, StratConTrackState track,
          @Nullable ScenarioTemplate template, @Nullable Integer daysTilDeployment) {
        boolean firstForce = true;
        StratConScenario scenario = null;

        for (int forceID : forceIDs) {
            if (firstForce) {
                scenario = setupScenario(scenarioCoords,
                      forceID,
                      campaign,
                      contract,
                      track,
                      template,
                      campaign.getCampaignOptions().isUseStratConMaplessMode(),
                      daysTilDeployment);
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
     * Deploys a combat team (force) to a specified coordinate within the strategic track and performs the associated
     * deployment activities, including handling scenarios, facilities, scouting behavior, and fog of war updates.
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
     * @param coords   the {@link StratConCoords} representing the deployment coordinates.
     * @param forceID  the unique identifier of the combat team (force) being deployed.
     * @param campaign the current {@link Campaign} context, which provides access to combat teams, facilities, and
     *                 other campaign-level data.
     * @param contract the {@link AtBContract} associated with the campaign, which determines rules and command rights
     *                 for the deployment.
     * @param track    the {@link StratConTrackState} representing the strategic track, including details about
     *                 scenarios, facilities, and force assignments.
     * @param sticky   a {@code boolean} flag indicating whether the deployment is "sticky," meaning the forces remain
     *                 at the deployment location without automatically updating their position.
     */
    public static void deployForceToCoords(StratConCoords coords, int forceID, Campaign campaign, AtBContract contract,
          StratConTrackState track, boolean sticky) {
        CombatTeam combatTeam = campaign.getCombatTeamsAsMap().get(forceID);

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
        StratConScenario revealedScenario = track.getScenario(coords);
        if (revealedScenario != null) {
            revealedScenario.addPrimaryForce(forceID);
            commitPrimaryForces(campaign, revealedScenario, track);
            if (!revealedScenario.getBackingScenario().isFinalized()) {
                finalizeScenario(revealedScenario.getBackingScenario(), contract, campaign);
                setScenarioParametersFromBiome(track, revealedScenario);
            }
            return;
        }

        StratConFacility facility = track.getFacility(coords);
        boolean isNonAlliedFacility = (facility != null) && (facility.getOwner() != Allied);

        int targetNum = calculateScenarioOdds(track, contract, true);
        boolean spawnScenario = (facility == null) && (randomInt(100) <= targetNum);

        if (isNonAlliedFacility || spawnScenario) {
            StratConScenario scenario;

            // If we're not deploying on top of an enemy facility, migrate the scenario
            if (!isNonAlliedFacility && isPatrol) {
                StratConCoords newCoords = getUnoccupiedAdjacentCoords(coords, track);

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
                      track.getAssignedCoordForces().get(coords),
                      contract,
                      campaign,
                      track);
                // Otherwise, pick a random force from those available
            } else {
                List<Integer> availableForceIDs = getAvailableForceIDs(campaign, contract, false);
                Collections.shuffle(availableForceIDs);

                // If the player doesn't have any available forces, we grab a force at random to
                // seed the scenario
                if (availableForceIDs.isEmpty()) {
                    ArrayList<CombatTeam> combatTeams = campaign.getCombatTeamsAsList();
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
     * Finds an unoccupied coordinate adjacent to the given origin coordinate.
     *
     * <p>This method examines all directions defined by {@code ALL_DIRECTIONS} around {@code originCoords} and
     * evaluates each for suitability as an unoccupied adjacent coordinate. A coordinate is considered "unoccupied" if
     * it meets all of the following conditions:</p>
     *
     * <ul>
     *     <li>There is no scenario assigned to the coordinate (via {@link StratConTrackState#getScenario})</li>
     *     <li>There is no facility present at the coordinate (via {@link StratConTrackState#getFacility})</li>
     *     <li>The coordinate is not occupied by any assigned forces (via {@link StratConTrackState#getAssignedForceCoords})</li>
     *     <li>The coordinate is within the boundaries of the map</li>
     *     <li>The coordinate is among the set of revealed coordinates (via {@link StratConTrackState#getRevealedCoords})</li>
     * </ul>
     *
     * <p>If multiple suitable coordinates are found, one is selected at random and returned. If no such coordinates
     * are available, {@code originCoords} is returned.</p>
     *
     * @param originCoords the starting coordinate to search around
     * @param trackState   the track state holding map, facility, scenario, and force assignment data
     *
     * @return a randomly selected unoccupied and revealed adjacent coordinate, or {@code originCoords} if none are
     *       available
     */
    private static StratConCoords getUnoccupiedAdjacentCoords(StratConCoords originCoords,
          StratConTrackState trackState) {
        // We need to reduce width/height by one because coordinates index from 0, not 1
        final int trackWidth = trackState.getWidth() - 1;
        final int trackHeight = trackState.getHeight() - 1;

        Set<StratConCoords> revealedCoords = trackState.getRevealedCoords();
        List<StratConCoords> suitableCoords = new ArrayList<>();
        for (int direction : ALL_DIRECTIONS) {
            StratConCoords newCoords = originCoords.translate(direction);

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
            if ((newCoords.getX() < 0) ||
                      (newCoords.getX() > trackWidth) ||
                      (newCoords.getY() < 0) ||
                      (newCoords.getY() > trackHeight)) {
                continue;
            }

            if (revealedCoords.contains(newCoords)) {
                suitableCoords.add(newCoords);
            }
        }

        if (suitableCoords.isEmpty()) {
            return originCoords;
        }

        return getRandomItem(suitableCoords);
    }

    /**
     * Sets up a StratCon scenario with the given parameters.
     *
     * @param coords   The coordinates where the scenario is to be placed on the track.
     * @param forceID  The ID of the forces involved in the scenario.
     * @param campaign The current campaign.
     * @param contract The contract associated with the current scenario.
     * @param track    The relevant StratCon track.
     *
     * @return The newly set up {@link StratConScenario}.
     */
    public static @Nullable StratConScenario setupScenario(StratConCoords coords, int forceID, Campaign campaign,
          AtBContract contract, StratConTrackState track) {
        return setupScenario(coords,
              forceID,
              campaign,
              contract,
              track,
              null,
              campaign.getCampaignOptions().isUseStratConMaplessMode(),
              null);
    }

    /**
     * Sets up a Stratcon scenario with the given parameters optionally allowing use a specific scenario template.
     * <p>
     * If a facility is already present at the provided coordinates, the scenario will be setup for that facility. If
     * there is no facility, a new scenario will be generated; if the ScenarioTemplate argument provided was non-null,
     * it will be used, else a randomly selected scenario will be generated. In case the generated scenario turns out to
     * be a facility scenario, a new facility will be added to the track at the provided coordinates and setup for that
     * facility.
     *
     * @param coords            The coordinates where the scenario is to be placed on the track.
     * @param forceID           The ID of the forces involved in the scenario.
     * @param campaign          The current campaign.
     * @param contract          The contract associated with the current scenario.
     * @param track             The relevant StratCon track.
     * @param template          A specific {@link ScenarioTemplate} to use for scenario setup, or {@code null} to select
     *                          the scenario template randomly.
     * @param ignoreFacilities  Whether we should ignore any facilities at the selected location
     * @param daysTilDeployment How many days until the scenario takes place, or {@code null} to pick a random day
     *                          within the next 7 days.
     *
     * @return The newly set up {@link StratConScenario}.
     */
    public static @Nullable StratConScenario setupScenario(StratConCoords coords, int forceID, Campaign campaign,
          AtBContract contract, StratConTrackState track, @Nullable ScenarioTemplate template, boolean ignoreFacilities,
          @Nullable Integer daysTilDeployment) {
        StratConScenario scenario;

        if (track.getFacilities().containsKey(coords) && !ignoreFacilities) {
            StratConFacility facility = track.getFacility(coords);
            boolean alliedFacility = facility.getOwner() == Allied;
            template = StratConScenarioFactory.getFacilityScenario(alliedFacility);
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
            if (!campaign.getCampaignOptions().isUseStratConMaplessMode()) {
                if (scenario.getBackingScenario().getTemplate().isFacilityScenario()) {
                    StratConFacility facility = scenario.getBackingScenario().getTemplate().isHostileFacility() ?
                                                      StratConFacilityFactory.getRandomHostileFacility() :
                                                      StratConFacilityFactory.getRandomAlliedFacility();
                    facility.setVisible(true);
                    track.addFacility(coords, facility);
                    setupFacilityScenario(scenario, facility);
                }
            }
        }

        return scenario;
    }

    /**
     * carries out tasks relevant to facility scenarios
     */
    private static void setupFacilityScenario(StratConScenario scenario, StratConFacility facility) {
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
        AtBScenarioModifier objectiveModifier;
        boolean alliedFacility = facility.getOwner() == Allied;

        objectiveModifier = alliedFacility ?
                                  AtBScenarioModifier.getRandomAlliedFacilityModifier() :
                                  AtBScenarioModifier.getRandomHostileFacilityModifier();

        if (objectiveModifier != null) {
            scenario.getBackingScenario().addScenarioModifier(objectiveModifier);
            scenario.getBackingScenario()
                  .setName(String.format("%s - %s - %s",
                        facility.getFacilityType(),
                        alliedFacility ? "Allied" : "Hostile",
                        objectiveModifier.getModifierName()));
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
    private static void processFacilityEffects(StratConTrackState track, StratConCampaignState campaignState,
          boolean isStartOfMonth) {
        for (StratConFacility facility : track.getFacilities().values()) {
            if (isStartOfMonth) {
                campaignState.changeSupportPoints(facility.getMonthlySPModifier());
            }
        }
    }

    /**
     * Processes the deployment of a combat force to a specified location on a track in the campaign.
     *
     * <p>This method handles actions related to force deployment, including:</p>
     * <ul>
     *   <li>Revealing the deployed coordinates and all adjacent coordinates within the force's scan range.</li>
     *   <li>Updating the visibility of facilities and scenarios in the affected area.</li>
     *   <li>Assigning the force to the specified deployment coordinates and clearing previous track assignments.</li>
     *   <li>Triggering any necessary events, such as deployment event handling or scenario updates.</li>
     * </ul>
     *
     * <p>Patrol and scouting roles may extend the scan range, and fatigue is increased only once
     * if the deployment reveals previously unrevealed coordinates.</p>
     *
     * @param coords   The {@link StratConCoords} where the combat force is being deployed.
     * @param forceID  The unique ID of the combat force being deployed.
     * @param campaign The current {@link Campaign} instance representing the game's state.
     * @param track    The {@link StratConTrackState} where the force is being deployed.
     * @param sticky   Whether the force should remain persistently assigned to this track.
     */
    public static void processForceDeployment(StratConCoords coords, int forceID, Campaign campaign,
          StratConTrackState track, boolean sticky) {
        scanNeighboringCoords(coords, forceID, campaign, track);

        // the force may be located in other places on the track - clear it out
        track.unassignForce(forceID);
        track.assignForce(forceID, coords, campaign.getLocalDate(), sticky);
        MekHQ.triggerEvent(new StratConDeploymentEvent(campaign.getForce(forceID)));
    }

    /**
     * Scans neighboring coordinates around the deployment location to reveal facilities, scenarios, and coordinates
     * within the force's scan range. Updates campaign and track states as needed.
     *
     * <p>This method uses a breadth-first search (BFS) approach to efficiently traverse the hex grid,
     * marking which coordinates have been visited and ensuring no redundant operations occur. It also increases
     * fatigue, reveals cloaked scenarios, and activates facilities or scenarios in the affected area.</p>
     *
     * @param coords   The {@link StratConCoords} of the initial deployment location.
     * @param forceID  The unique ID of the force being deployed.
     * @param campaign The current {@link Campaign} instance representing the game's state.
     * @param track    The {@link StratConTrackState} where the deployment and scanning are being tracked.
     */
    private static void scanNeighboringCoords(StratConCoords coords, int forceID, Campaign campaign,
          StratConTrackState track) {
        // we want to ensure we only increase Fatigue once
        boolean hasFatigueIncreased = false;

        // Keep a set of visited coordinates to avoid redundancy
        Set<StratConCoords> visited = new HashSet<>();
        visited.add(coords);

        // Determine scan range
        int scanRangeIncrease = track.getScanRangeIncrease();
        CombatTeam combatTeam = campaign.getCombatTeamsAsMap().get(forceID);
        if (combatTeam != null && combatTeam.getRole().isPatrol()) {
            scanRangeIncrease++;
        }

        // Process starting point
        if (!track.getRevealedCoords().contains(coords)) {
            increaseFatigue(forceID, campaign);
            hasFatigueIncreased = true;
        }

        track.getRevealedCoords().add(coords);

        StratConFacility targetFacility = track.getFacility(coords);
        if (targetFacility != null) {
            targetFacility.setVisible(true);
        }

        StratConScenario scenario = track.getScenario(coords);

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

        if ((scenario != null) || (targetFacility != null && !targetFacility.isOwnerAlliedToPlayer())) {
            return;
        }

        // Build a map of scouts and whether they're in light units
        Force force = campaign.getForce(forceID);
        Hangar hangar = campaign.getHangar();
        List<ScoutRecord> scouts = force == null ? new ArrayList<>() : buildScoutMap(force, hangar);

        boolean isClanCampaign = campaign.isClanCampaign();
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean useAdvancedScouting = campaignOptions.isUseAdvancedScouting();
        boolean isUsingAgeEffects = campaignOptions.isUseAgeEffects();
        // Each scout may scan up to scanMultiplier hexes
        for (ScoutRecord scoutData : scouts) {
            Person scout = scoutData.scout();
            int hexesScouted = 0;

            // Set up per-scout BFS structures (do not revisit global revealed or visited hexes)
            Queue<Pair<StratConCoords, Integer>> scoutQueue = new LinkedList<>();
            Set<StratConCoords> scoutVisited = new HashSet<>(visited);

            scoutQueue.add(new Pair<>(coords, 0));
            scoutVisited.add(coords);

            while (!scoutQueue.isEmpty() && hexesScouted < scanRangeIncrease) {
                Pair<StratConCoords, Integer> current = scoutQueue.poll();
                StratConCoords currentCoords = current.getKey();
                int distance = current.getValue();

                // Only process neighbors if they're within the max distance
                if (distance < scanRangeIncrease) {
                    for (int direction = 0; direction < 6; direction++) {
                        StratConCoords checkCoords = currentCoords.translate(direction);

                        // Skip already visited coordinates (refer to per-scout AND global)
                        if (scoutVisited.contains(checkCoords) ||
                                  visited.contains(checkCoords) ||
                                  track.getRevealedCoords().contains(checkCoords)) {
                            continue;
                        }

                        TargetRollModifier weightModifier = getUnitWeightModifier(scoutData.entityWeight());
                        TargetRollModifier speedModifier = getUnitSpeedModifier(scoutData.unitAtBSpeed());
                        TargetRollModifier sensorsModifier = new TargetRollModifier(
                              scoutData.hasSensorEquipment() ? -1 : 0, "Unit Sensors");

                        SkillCheckUtility skillCheck = new SkillCheckUtility(scout, scoutData.skillName(),
                              List.of(weightModifier, speedModifier, sensorsModifier), 0, false, false,
                              isUsingAgeEffects, isClanCampaign, campaign.getLocalDate());
                        campaign.addReport(skillCheck.getResultsText());

                        // Mark the current coordinate as revealed (count only on success)
                        boolean wasScoutingSuccessful = !useAdvancedScouting || skillCheck.isSuccess();
                        if (wasScoutingSuccessful) {
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

                            track.getRevealedCoords().add(checkCoords);

                            // Mark as visited in both sets
                            scoutVisited.add(checkCoords);
                            visited.add(checkCoords);
                            scoutQueue.add(new Pair<>(checkCoords,
                                  distance + 1)); // Add the neighbor with incremented distance
                        }

                        hexesScouted++;
                        if (useAdvancedScouting && (hexesScouted >= scanRangeIncrease)) {
                            break; // Stop scouting after reaching per-scout limit
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a TargetRollModifier based on the provided unit weight. Lighter units gain bonuses, heavier units gain
     * penalties.
     *
     * @param unitWeight the unit's weight in tons
     *
     * @return appropriate TargetRollModifier for the weight bracket
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static TargetRollModifier getUnitWeightModifier(double unitWeight) {
        int modifier = 6; // default for anything greater than 100t

        if (unitWeight <= 35) {
            modifier = -2;
        } else if (unitWeight <= 55) {
            modifier = 0;
        } else if (unitWeight <= 75) {
            modifier = 2;
        } else if (unitWeight <= 100) {
            modifier = 4;
        }

        return new TargetRollModifier(modifier, "Unit Weight Modifier");
    }

    /**
     * Determines the target roll modifier based on the given unit's speed value.
     *
     * <p>This method evaluates {@code unitSpeed} and assigns a modifier as follows (all ranges are inclusive):</p>
     * <ul>
     *     <li>Speed ≤ 3: modifier = 1</li>
     *     <li>Speed 4–7 (inclusive): modifier = 0</li>
     *     <li>Speed ≥ 8: modifier = -1</li>
     * </ul>
     *
     * <p>The returned {@link TargetRollModifier} includes the computed modifier and the description "Unit Speed
     * Modifier".</p>
     *
     * @param unitSpeed the speed of the unit to evaluate
     *
     * @return a {@link TargetRollModifier} representing the speed-based modifier
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static TargetRollModifier getUnitSpeedModifier(int unitSpeed) {
        int modifier;
        if (unitSpeed <= 3) {
            modifier = 1;
        } else if (unitSpeed <= 7) {
            modifier = 0;
        } else { // speed 8+
            modifier = -1;
        }

        return new TargetRollModifier(modifier, "Unit Speed Modifier");
    }

    /**
     * Builds and returns a list of {@link ScoutRecord} instances representing the best scout for each unit in the given
     * force.
     *
     * <p>For each unit retrieved from the {@code Force}, this method examines all crew members to determine which
     * has the highest scouting-related skill (as evaluated by
     * {@link ScoutingSkills#getBestScoutingSkill(Person)}).</p>
     *
     * <p>The crew member with the highest skill level becomes the designated scout for that unit. The method also
     * determines whether each unit is a "light unit" based on its weight class.</p>
     *
     * <p>All such {@link ScoutRecord} entries are collected, sorted in descending order of scout skill level, and
     * returned as a list. Units with no crew are logged and skipped.</p>
     *
     * @param force  the {@link Force} containing units to evaluate
     * @param hangar the {@link Hangar} used to help retrieve units from the force
     *
     * @return a list of {@link ScoutRecord} objects, each representing the best scout and their skill details for a
     *       unit, sorted from the highest to lowest scout skill level
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static List<ScoutRecord> buildScoutMap(Force force, Hangar hangar) {
        List<ScoutRecord> scouts = new ArrayList<>();
        for (Unit unit : force.getAllUnitsAsUnits(hangar, false)) {
            boolean hasSensorEquipment = false;
            Entity entity = unit.getEntity();
            if (entity != null) {
                boolean hasImprovedSensors = EntityUtilities.hasImprovedSensors(entity);
                boolean hasActiveProbe = EntityUtilities.hasActiveProbe(entity);
                hasSensorEquipment = hasImprovedSensors || hasActiveProbe;
            }

            List<Person> unitCrew = unit.getCrew();
            if (unitCrew.isEmpty()) {
                LOGGER.info("No crew for unit: {} {}", unit.getName(), unit.getId());
                continue;
            }

            // Find the best scout in this unit, if any
            Person bestScout = null;
            String bestScoutSkillName = SkillType.S_SENSOR_OPERATIONS;
            int bestScoutSkillLevel = -1;
            for (Person crewMember : unitCrew) {
                if (bestScout == null) {
                    bestScout = crewMember;
                }

                String scoutSkillName = ScoutingSkills.getBestScoutingSkill(crewMember);
                if (scoutSkillName == null) {
                    continue;
                }

                SkillModifierData skillModifierData = crewMember.getSkillModifierData();

                Skill scoutSkill = crewMember.getSkill(scoutSkillName);
                int scoutSkillLevel = (scoutSkill == null) ? -1 : scoutSkill.getTotalSkillLevel(skillModifierData);
                if (scoutSkillLevel > bestScoutSkillLevel) {
                    bestScout = crewMember;
                    bestScoutSkillName = scoutSkillName;
                    bestScoutSkillLevel = scoutSkillLevel;
                }
            }

            double weight = 200.0;
            if (entity != null) {
                weight = entity.getWeight();
            }

            int unitSpeed = entity == null ? 0 : AtBDynamicScenarioFactory.calculateAtBSpeed(entity);

            ScoutRecord scoutRecord = new ScoutRecord(bestScout, bestScoutSkillName, bestScoutSkillLevel, weight,
                  unitSpeed, hasSensorEquipment);
            LOGGER.info("Unit {} has best scout: {} with skill {} at level {} and is weight: {}t",
                  unit.getId(), bestScout, bestScoutSkillName, bestScoutSkillLevel, weight);
            scouts.add(scoutRecord);
        }

        // Sort scouts by the skill level of their best scout skill, the highest first
        scouts.sort(Comparator.comparingInt(ScoutRecord::scoutSkillLevel).reversed());
        return scouts;
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
                int fatigueChangeRate = campaign.getCampaignOptions().getFatigueRate();
                person.changeFatigue(fatigueChangeRate);

                if (campaign.getCampaignOptions().isUseFatigue()) {
                    Fatigue.processFatigueActions(campaign, person);
                }
            }
        }
    }

    /**
     * Use
     * {@link #processReinforcementDeployment(Force, ReinforcementEligibilityType, StratConCampaignState,
     * StratConScenario, Campaign, int, boolean, boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static ReinforcementResultsType processReinforcementDeployment(Force force,
          ReinforcementEligibilityType reinforcementType, StratConCampaignState campaignState,
          StratConScenario scenario, Campaign campaign, int reinforcementTargetNumber, boolean isGMReinforcement) {
        return processReinforcementDeployment(force, reinforcementType, campaignState, scenario, campaign,
              reinforcementTargetNumber, isGMReinforcement, false);
    }

    /**
     * Processes the effects of deploying a reinforcement force to a scenario. Based on the reinforcement type, the
     * campaign state, and the results dice rolls, skills, and intercept odds, this method determines whether the
     * reinforcement deployment succeeds, fails, is delayed, or is intercepted.
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
     * @param isGMReinforcement         {@code true} if the player is using GM powers to bypass the reinforcement check,
     *                                  {@code false} otherwise.
     * @param isInstantlyDeployed       {@code true} if the player is deploying instantly
     *
     * @return a {@link ReinforcementResultsType} indicating the result of the reinforcement deployment:
     *       <ul>
     *           <li>{@link ReinforcementResultsType#SUCCESS} - The reinforcement is deployed successfully.</li>
     *           <li>{@link ReinforcementResultsType#FAILED} - The reinforcement deployment fails.</li>
     *           <li>{@link ReinforcementResultsType#DELAYED} - The reinforcement is delayed.</li>
     *           <li>{@link ReinforcementResultsType#INTERCEPTED} - The reinforcement is intercepted,
     *           possibly resulting in a new scenario.</li>
     *       </ul>
     */
    public static ReinforcementResultsType processReinforcementDeployment(Force force,
          ReinforcementEligibilityType reinforcementType, StratConCampaignState campaignState,
          StratConScenario scenario, Campaign campaign, int reinforcementTargetNumber, boolean isGMReinforcement,
          boolean isInstantlyDeployed) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AtBStratCon",
              MekHQ.getMHQOptions().getLocale());

        if (reinforcementType.equals(ReinforcementEligibilityType.CHAINED_SCENARIO)) {
            return INSTANT;
        }

        AtBContract contract = campaignState.getContract();

        // Determine StratCon Track and other context for recalculation
        StratConTrackState track = null;
        for (StratConTrackState trackState : campaignState.getTracks()) {
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
                  scenario.getHyperlinkedName()));
            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsAutomaticSuccess.text"),
                  spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                  CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());

            return isInstantlyDeployed ? INSTANT : SUCCESS;
        } else {
            reportStatus.append(String.format(resources.getString("reinforcementsAttempt.text"),
                  scenario.getHyperlinkedName(),
                  roll,
                  maneuverRoleReport,
                  reinforcementTargetNumber));
        }

        // Critical Failure
        if (roll == 2) {
            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsCriticalFailure.text"),
                  spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                  CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return FAILED;
        }

        // Reinforcement successful
        if (roll >= reinforcementTargetNumber) {
            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsSuccess.text"),
                  spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                  CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());

            return isInstantlyDeployed ? INSTANT : SUCCESS;
        }

        // Reinforcement roll failed, make interception check
        int interceptionOdds = calculateScenarioOdds(track, campaignState.getContract(), true);
        int interceptionRoll = randomInt(100);

        // Check passed
        if (interceptionRoll >= interceptionOdds || contract.getMoraleLevel().isRouted()) {
            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsCommandFailure.text"),
                  spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()),
                  CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return DELAYED;
        }

        // Check failed, enemy attempt interception
        reportStatus.append(' ');
        reportStatus.append(String.format(resources.getString("reinforcementsInterceptionAttempt.text"),
              spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()),
              CLOSING_SPAN_TAG));

        UUID commanderId = force.getForceCommanderID();

        if (commanderId == null) {
            LOGGER.error("Force Commander ID is null.");

            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsErrorNoCommander.text"),
                  spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                  CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return FAILED;
        }

        Person commander = campaign.getPerson(commanderId);

        if (commander == null) {
            LOGGER.error("Failed to fetch commander from ID.");

            reportStatus.append(' ');
            reportStatus.append(String.format(resources.getString("reinforcementsErrorUnableToFetchCommander.text"),
                  spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                  CLOSING_SPAN_TAG));
            campaign.addReport(reportStatus.toString());
            return FAILED;
        }

        campaign.addReport(reportStatus.toString());


        roll = d6(2);
        int targetNumber = 9;
        Skill tactics = commander.getSkill(S_TACTICS);

        SkillCheckUtility skillCheckUtility = new SkillCheckUtility(commander, S_TACTICS, null, 0, true, false,
              campaign.getCampaignOptions().isUseAgeEffects(), campaign.isClanCampaign(), campaign.getLocalDate());
        campaign.addReport(skillCheckUtility.getResultsText());

        if (skillCheckUtility.isSuccess()) {
            String reportString = tactics != null ?
                                        resources.getString("reinforcementEvasionSuccessful.text") :
                                        resources.getString("reinforcementEvasionSuccessful.noSkill");
            campaign.addReport(String.format(reportString,
                  spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                  CLOSING_SPAN_TAG));

            if (campaign.getCampaignOptions().isUseFatigue()) {
                increaseFatigue(force.getId(), campaign);
            }

            return DELAYED;
        }

        campaign.addReport(String.format(resources.getString("reinforcementEvasionUnsuccessful.text"),
              spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
              CLOSING_SPAN_TAG,
              roll,
              targetNumber));

        ScenarioTemplate scenarioTemplate = getInterceptionScenarioTemplate(force, campaign.getHangar());

        generateReinforcementInterceptionScenario(campaign, scenario, contract, track, scenarioTemplate, force);

        return INTERCEPTED;
    }

    /**
     * Retrieves the appropriate {@link ScenarioTemplate} for an interception scenario based on the provided
     * {@link Force} and {@link Campaign}.
     *
     * <p>This method determines the correct scenario template file to use by analyzing the composition
     * of the {@link Force} within the context of the given {@link Campaign}. The selected template file is then
     * deserialized into a {@link ScenarioTemplate} object.</p>
     *
     * <p><strong>Special Cases:</strong></p>
     * <ul>
     *     <li>A "Space" template is chosen if all units are aerospace and a random condition is met
     *             (1 in 3 chance).</li>
     *     <li>A "Low-Atmosphere" template is selected if the {@link Force} contains only airborne
     *             units but does not meet the criteria for a "Space" template.</li>
     *     <li>A default ground template is selected if no specific cases are matched.</li>
     * </ul>
     *
     * @param force  The {@link Force} instance that the scenario is based on. The force composition is used to
     *               determine the appropriate scenario template.
     * @param hangar The {@link Hangar} instance from which to retrieve the {@link Unit}.
     *
     * @return A {@link ScenarioTemplate} instance representing the chosen scenario template file based on the logic
     *       described, or a default template if no special conditions are satisfied.
     */
    private static ScenarioTemplate getInterceptionScenarioTemplate(Force force, Hangar hangar) {
        String templateString = "data/scenariotemplates/%sReinforcements Intercepted.xml";

        ScenarioTemplate scenarioTemplate = ScenarioTemplate.Deserialize(String.format(templateString, ""));

        boolean airborneOnly = force.forceContainsOnlyAerialForces(hangar, false, false);

        boolean aerospaceOnly = false;
        if (airborneOnly) {
            aerospaceOnly = force.forceContainsOnlyAerialForces(hangar, false, true);
        }

        if (aerospaceOnly && (randomInt(3) == 0)) {
            scenarioTemplate = ScenarioTemplate.Deserialize(String.format(templateString, "Space "));
        } else if (airborneOnly) {
            scenarioTemplate = ScenarioTemplate.Deserialize(String.format(templateString, "Low-Atmosphere "));
        }

        return scenarioTemplate;
    }

    /**
     * Calculates the target roll required for determining reinforcements in a specific campaign scenario.
     *
     * <p>This method evaluates the reinforcement target number by considering various factors
     * such as the administrative skill of the command liaison, facility ownership influence, contract-related skill
     * levels, and command rights configurations. Multiple modifiers are applied step-by-step to generate the final
     * {@link TargetRoll}.</p>
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
     * @param commandLiaison the {@link Person} acting as the command liaison, or {@code null} if no liaison exists.
     * @param contract       the {@link AtBContract} defining the terms of the contract for this scenario.
     *
     * @return a {@link TargetRoll} object representing the calculated reinforcement target number, with appropriate
     *       modifiers applied.
     */
    public static TargetRoll calculateReinforcementTargetNumber(@Nullable Person commandLiaison, AtBContract contract) {
        // Create Target Roll
        TargetRoll reinforcementTargetNumber = new TargetRoll(7, "Base Target Number");

        // Base Target Number
        Skill skill = commandLiaison != null ? commandLiaison.getSkill(S_ADMIN) : null;
        int skillModifier;
        if (skill == null) {
            skillModifier = 0;
        } else {
            SkillModifierData skillModifierData = commandLiaison.getSkillModifierData();
            skillModifier = REGULAR.getExperienceLevel() - skill.getExperienceLevel(skillModifierData);
        }

        // Admin Skill Modifier
        reinforcementTargetNumber.addModifier(skillModifier, "Administration Skill");

        // Enemy Morale Modifier
        reinforcementTargetNumber.addModifier(contract.getMoraleLevel().getLevel() - STALEMATE.getLevel(),
              "Enemy Morale");

        // Skill Modifier
        int enemySkillModifier = contract.getEnemySkill().getAdjustedValue() - REGULAR.getAdjustedValue();
        reinforcementTargetNumber.addModifier(enemySkillModifier, "Enemy Skill Modifier");

        // Liaison Modifier
        ContractCommandRights commandRights = contract.getCommandRights();
        if (commandRights.isLiaison()) {
            int liaisonModifier = -1;
            reinforcementTargetNumber.addModifier(liaisonModifier, "Liaison Command Rights");
        }

        // Liaison SPA
        if (commandLiaison != null) {
            PersonnelOptions options = commandLiaison.getOptions();
            if (options.booleanOption(ADMIN_COORDINATOR)) {
                int liaisonModifier = -1;
                reinforcementTargetNumber.addModifier(liaisonModifier, "Coordinator SPA");
            }
        }

        // Return final value
        return reinforcementTargetNumber;
    }

    /**
     * Assigns a force to the scenario such that the majority of the force can be deployed
     */
    private static void assignAppropriateExtraForceToScenario(StratConScenario scenario,
          Map<MapLocation, List<Integer>> sortedAvailableForceIDs) {
        // the goal of this function is to avoid assigning ground units to air battles
        // and ground units/conventional fighters to space battle

        List<MapLocation> mapLocations = new ArrayList<>();
        mapLocations.add(Space); // can always add ASFs

        MapLocation scenarioMapLocation = scenario.getScenarioTemplate().mapParameters.getMapLocation();

        if (scenarioMapLocation == LowAtmosphere) {
            mapLocations.add(LowAtmosphere); // can add conventional fighters to ground or low atmosphere battles
        }

        if ((scenarioMapLocation == AllGroundTerrain) || (scenarioMapLocation == SpecificGroundTerrain)) {
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
     * Worker function that "locks in" a scenario - Adds it to the campaign so it's visible in the briefing room, adds
     * it to the track
     */
    public static void commitPrimaryForces(Campaign campaign, StratConScenario scenario,
          StratConTrackState trackState) {
        trackState.addScenario(scenario);

        // set up dates for the scenario if it doesn't have them already
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
     * Utility method to determine if the current scenario's force commander's force is on defence
     */
    private static boolean commanderLanceHasDefensiveAssignment(AtBDynamicScenario scenario, Campaign campaign) {
        Person lanceCommander = scenario.getLanceCommander(campaign);
        if (lanceCommander != null) {
            Unit commanderUnit = lanceCommander.getUnit();
            if (commanderUnit != null) {
                CombatTeam lance = campaign.getCombatTeamsAsMap().get(commanderUnit.getForceId());

                return (lance != null) && lance.getRole().isFrontline();
            }
        }

        return false;
    }

    /**
     * Categorizes a list of force IDs into groups based on the type of map they can primarily support.
     *
     * <p>This overloaded method analyzes each force associated with the given force IDs in the context of
     * the provided {@link Hangar} and a pre-resolved list of {@link Force} objects. It determines whether each force is
     * suited for ground, atmospheric, or space maps, assigning them to the appropriate map types. Forces may belong to
     * multiple map types based on their composition.</p>
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>Forces are classified into the following map types:
     *       <ul>
     *         <li><strong>AllGroundTerrain</strong>: Includes all forces.</li>
     *         <li><strong>LowAtmosphere</strong>: Forces that only contain airborne units.</li>
     *         <li><strong>Space</strong>: Forces that exclusively contain aerospace-capable units.</li>
     *       </ul>
     *   </li>
     *   <li>A force can appear in multiple map types, such as both "LowAtmosphere" and "Space" for
     *       aerospace-only forces.</li>
     *   <li>Logs an error and continues processing if any force associated with a given ID cannot
     *       be found in the provided list of forces.</li>
     * </ul>
     *
     * @param forceIDs  A list of force IDs to classify.
     * @param hangar    The {@link Hangar} instance containing aerial or aerospace-related information about forces.
     * @param allForces A pre-resolved list of {@link Force} objects. Forces are accessed using their IDs as indices,
     *                  providing performance benefits when compared to fetching forces on demand.
     *
     * @return A {@link Map} where each {@link MapLocation} key corresponds to a map type, and the value is a list of
     *       force IDs that can operate in that map type.
     */
    public static Map<MapLocation, List<Integer>> sortForcesByMapType(List<Integer> forceIDs, Hangar hangar,
          List<Force> allForces) {
        boolean airborneOnly;
        boolean aerospaceOnly;

        Map<MapLocation, List<Integer>> retVal = new HashMap<>();

        retVal.put(AllGroundTerrain, new ArrayList<>());
        retVal.put(LowAtmosphere, new ArrayList<>());
        retVal.put(Space, new ArrayList<>());

        for (int forceID : forceIDs) {
            Force force = null;
            for (Force individualForce : allForces) {
                if (individualForce.getId() == forceID) {
                    force = individualForce;
                    break;
                }
            }

            if (force == null) {
                LOGGER.error("Force ID {} is null in sortForcesByMapType", forceID);
                continue;
            }

            airborneOnly = force.forceContainsOnlyAerialForces(hangar, false, false);

            aerospaceOnly = false;
            if (airborneOnly) {
                aerospaceOnly = force.forceContainsOnlyAerialForces(hangar, false, true);
            }

            if (aerospaceOnly) {
                retVal.get(LowAtmosphere).add(forceID);
                retVal.get(Space).add(forceID);
            } else if (airborneOnly) {
                retVal.get(LowAtmosphere).add(forceID);
            }

            retVal.get(AllGroundTerrain).add(forceID);
        }
        return retVal;
    }

    /**
     * Generates a StratCon scenario at the specified coordinates for the given force on the specified track. The
     * scenario is determined based on a random template suitable for the unit type of the specified force, and it is
     * optionally configured with a deployment delay.
     *
     * <p>This method selects a random scenario template based on the primary unit type of the force,
     * then delegates the scenario creation and configuration to another overloaded {@code generateScenario} method
     * which handles specific template-based scenario generation.</p>
     *
     * @param campaign          the {@link Campaign} managing the overall gameplay state
     * @param contract          the {@link AtBContract} governing the StratCon campaign
     * @param track             the {@link StratConTrackState} where the scenario is placed
     * @param forceID           the ID of the force for which the scenario is generated
     * @param coords            the {@link StratConCoords} specifying where the scenario will be generated
     * @param daysTilDeployment the number of days until the scenario is deployed; if {@code null}, deployment dates are
     *                          determined dynamically
     *
     * @return the generated {@link StratConScenario}, or {@code null} if scenario generation fails
     */
    private static @Nullable StratConScenario generateScenario(Campaign campaign, AtBContract contract,
          StratConTrackState track, int forceID, StratConCoords coords, @Nullable Integer daysTilDeployment) {
        int unitType = campaign.getForce(forceID).getPrimaryUnitType(campaign);
        ScenarioTemplate template = StratConScenarioFactory.getRandomScenario(unitType);
        // useful for debugging specific scenario types
        // template = StratConScenarioFactory.getSpecificScenario("Defend Grounded
        // Dropship.xml");

        return generateScenario(campaign, contract, track, forceID, coords, template, daysTilDeployment);
    }

    /**
     * Generates a StratCon scenario at the specified coordinates for the given force on the specified track, using the
     * provided scenario template. The scenario is customized and registered with the campaign.
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
     * @param campaign          the {@link Campaign} managing the gameplay state
     * @param contract          the {@link AtBContract} governing the StratCon campaign
     * @param track             the {@link StratConTrackState} to which the scenario belongs
     * @param forceID           the ID of the force for which the scenario is generated, or {@link Force#FORCE_NONE} if
     *                          none
     * @param coords            the {@link StratConCoords} specifying where the scenario will be placed
     * @param template          the {@link ScenarioTemplate} to use for scenario generation; if {@code null}, a random
     *                          one is selected
     * @param daysTilDeployment the number of days until the scenario is deployed; if {@code null}, dates will be
     *                          dynamically set
     *
     * @return the generated {@link StratConScenario}, or {@code null} if scenario generation failed
     */
    static @Nullable StratConScenario generateScenario(Campaign campaign, AtBContract contract,
          StratConTrackState track, int forceID, StratConCoords coords, ScenarioTemplate template,
          @Nullable Integer daysTilDeployment) {
        StratConScenario scenario = new StratConScenario();

        if (template == null) {
            int unitType = MEK;

            try {
                unitType = campaign.getForce(forceID).getPrimaryUnitType(campaign);
            } catch (NullPointerException ignored) {
                // This just means the player has no units
            }

            template = StratConScenarioFactory.getRandomScenario(unitType);
        }

        if (template == null) {
            LOGGER.error("Failed to fetch random scenario template. Aborting scenario generation.");
            return null;
        }

        AtBDynamicScenario backingScenario = AtBDynamicScenarioFactory.initializeScenarioFromTemplate(template,
              contract,
              campaign);
        scenario.setBackingScenario(backingScenario);
        scenario.setCoords(coords);

        // by default, certain conditions may make this bigger
        scenario.setRequiredPlayerLances(1);

        // do any facility or global modifiers
        if (!campaign.getCampaignOptions().isUseStratConMaplessMode()) {
            applyFacilityModifiers(scenario, track, coords);
        }
        applyGlobalModifiers(scenario, contract.getStratconCampaignState());

        AtBDynamicScenarioFactory.setScenarioModifiers(campaign.getCampaignOptions(), scenario.getBackingScenario());
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
    private static void applyGlobalModifiers(StratConScenario scenario, StratConCampaignState campaignState) {
        for (String modifierName : campaignState.getGlobalScenarioModifiers()) {
            AtBScenarioModifier modifier = AtBScenarioModifier.getScenarioModifier(modifierName);

            if (modifier == null) {
                LOGGER.error("Modifier {} not found; ignoring", modifierName);
                continue;
            }

            scenario.getBackingScenario().addScenarioModifier(modifier);
        }
    }

    /**
     * Applies scenario modifiers from the current track to the given scenario.
     */
    private static void applyFacilityModifiers(StratConScenario scenario, StratConTrackState track,
          StratConCoords coords) {
        // loop through all the facilities on the track
        // if a facility has been revealed, then it has a 100% chance to apply its
        // effect
        // if a facility has not been revealed, then it has an x% chance to apply its
        // effect
        // where x is the current "aggro rating"
        // if a facility is on the scenario coordinates, then it applies the local
        // effects
        for (StratConCoords facilityCoords : track.getFacilities().keySet()) {
            boolean scenarioAtFacility = facilityCoords.equals(coords);
            StratConFacility facility = track.getFacilities().get(facilityCoords);
            List<String> modifierIDs = new ArrayList<>();

            if (scenarioAtFacility) {
                modifierIDs = facility.getLocalModifiers();
            } else if (facility.isVisible() || (randomInt(100) <= 75)) {
                modifierIDs = facility.getSharedModifiers();
            }

            for (String modifierID : modifierIDs) {
                AtBScenarioModifier modifier = AtBScenarioModifier.getScenarioModifier(modifierID);
                if (modifier == null) {
                    LOGGER.error("Modifier {} not found for facility {}",
                          modifierID,
                          facility.getFormattedDisplayableName());
                    continue;
                }

                modifier.setAdditionalBriefingText('(' +
                                                         facility.getDisplayableName() +
                                                         ") " +
                                                         modifier.getAdditionalBriefingText());
                scenario.getBackingScenario().addScenarioModifier(modifier);
            }
        }
    }

    /**
     * Set the 'attached' units modifier for the current scenario (integrated, house, liaison), and make sure we're not
     * deploying ground units to an air scenario
     *
     * @param contract The scenario's contract
     */
    public static void setAttachedUnitsModifier(StratConScenario scenario, AtBContract contract) {
        AtBDynamicScenario backingScenario = scenario.getBackingScenario();
        boolean airBattle = (backingScenario.getTemplate().mapParameters.getMapLocation() == LowAtmosphere) ||
                                  (backingScenario.getTemplate().mapParameters.getMapLocation() == Space);
        // if we're under non-independent command rights, a supervisor may come along
        switch (contract.getCommandRights()) {
            case INTEGRATED:
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(airBattle ?
                                                                                                  MHQConstants.SCENARIO_MODIFIER_INTEGRATED_UNITS_AIR :
                                                                                                  MHQConstants.SCENARIO_MODIFIER_INTEGRATED_UNITS_GROUND));
                break;
            case HOUSE:
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(airBattle ?
                                                                                                  MHQConstants.SCENARIO_MODIFIER_HOUSE_CO_AIR :
                                                                                                  MHQConstants.SCENARIO_MODIFIER_HOUSE_CO_GROUND));
                break;
            case LIAISON:
                if (scenario.isTurningPoint()) {
                    backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(airBattle ?
                                                                                                      MHQConstants.SCENARIO_MODIFIER_LIAISON_AIR :
                                                                                                      MHQConstants.SCENARIO_MODIFIER_LIAISON_GROUND));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Worker function that sets scenario deploy/battle/return dates based on the track's properties and current
     * campaign date
     */
    private static void setScenarioDates(StratConTrackState track, Campaign campaign, StratConScenario scenario) {
        int deploymentDay = track.getDeploymentTime() < 7 ? randomInt(7 - track.getDeploymentTime()) : 0;
        setScenarioDates(deploymentDay, track, campaign, scenario);
    }

    /**
     * Worker function that sets scenario deploy/battle/return dates based on the track's properties and current
     * campaign date. Takes a fixed deployment day of X days from campaign's today date.
     */
    private static void setScenarioDates(int deploymentDay, StratConTrackState track, Campaign campaign,
          StratConScenario scenario) {
        // set up deployment day, battle day, return day here
        // safety code to prevent attempts to generate random int with upper bound of 0
        // which is apparently illegal
        int battleDay = deploymentDay + (track.getDeploymentTime() > 0 ? randomInt(track.getDeploymentTime()) : 0);
        int returnDay = deploymentDay + track.getDeploymentTime();

        LocalDate deploymentDate = campaign.getLocalDate().plusDays(deploymentDay);
        LocalDate battleDate = campaign.getLocalDate().plusDays(battleDay);
        LocalDate returnDate = campaign.getLocalDate().plusDays(returnDay);

        scenario.setDeploymentDate(deploymentDate);
        scenario.setActionDate(battleDate);
        scenario.setReturnDate(returnDate);
    }

    /**
     * Determines whether the force in question has the same primary unit type as the force template.
     *
     * @return Whether the unit types match.
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
     * Retrieves a list of force IDs corresponding to combat teams that are eligible for deployment under a specific
     * contract. The eligibility is determined based on various criteria such as assignment to the current contract,
     * deployment status, and combat role restrictions.
     *
     * <p>The method identifies suitable combat teams for deployment by:
     * <ul>
     *   <li>Filtering combat teams assigned to the specified contract.</li>
     *   <li>Excluding combat teams that are already actively deployed.</li>
     *   <li>Ensuring that combat teams have roles other than "In Reserve" or "Auxiliary"
     *       (unless role restrictions are bypassed).</li>
     *   <li>If the team role is "Training," it is included only when the contract type is a Cadre Duty.</li>
     * </ul>
     *
     * @param campaign               The {@link Campaign} containing data regarding contracts, combat teams, and their
     *                               statuses.
     * @param contract               The {@link AtBContract} contract for which combat teams are evaluated based on
     *                               their eligibility.
     * @param bypassRoleRestrictions A boolean flag to indicate whether restrictions based on combat roles should be
     *                               ignored. If {@code true}, all combat teams assigned to the contract are considered
     *                               eligible.
     *
     * @return A {@link List} of {@link Integer} force IDs representing combat teams that are ready and suitable for
     *       deployment.
     */
    public static List<Integer> getAvailableForceIDs(Campaign campaign, AtBContract contract,
          boolean bypassRoleRestrictions) {
        // First, build a list of all combat teams in the campaign
        ArrayList<CombatTeam> combatTeams = campaign.getCombatTeamsAsList();

        if (combatTeams.isEmpty()) {
            // If we don't have any combat teams, there is no point in continuing, so we exit early
            return Collections.emptyList();
        }

        // Finally, loop through the available combat teams adding those found to be suitable to
        // the appropriate list.
        List<Integer> suitableForces = new ArrayList<>();
        for (CombatTeam combatTeam : combatTeams) {
            // If the combat team isn't assigned to the current contract, it isn't eligible to be deployed
            if (!contract.equals(combatTeam.getContract(campaign))) {
                continue;
            }

            // If the combat team doesn't have a valid force (somehow) skip it.
            Force force = combatTeam.getForce(campaign);
            if (force == null) {
                continue;
            }

            // Skip any that are already assigned to a scenario.
            if (force.isDeployed()) {
                continue;
            }

            // So long as the combat team isn't In Reserve or Auxiliary, they are eligible to be deployed
            CombatRole combatRole = combatTeam.getRole();
            if (bypassRoleRestrictions) {
                suitableForces.add(combatTeam.getForceId());
            } else if (!combatRole.isReserve() && !combatRole.isAuxiliary()) {
                if (!combatRole.isTraining()) {
                    if (!combatRole.isCadre() || contract.getContractType().isCadreDuty()) {
                        suitableForces.add(combatTeam.getForceId());
                    }
                }
            }
        }

        if (suitableForces.isEmpty()) {
            if (!bypassRoleRestrictions) {
                LOGGER.info("No suitable combat teams found for contract {}. Relaxing restrictions", contract.getId());
                suitableForces = getAvailableForceIDs(campaign, contract, true);
            } else {
                LOGGER.info("No suitable combat teams found for contract {} despite relaxed restrictions." +
                                  " Scenario generation will likely be skipped.", contract.getId());
            }
        }

        return suitableForces;
    }

    /**
     * Retrieves a list of all force IDs eligible for deployment to a scenario.
     * <p>
     * This method evaluates all forces in the specified {@link Campaign} and identifies those that meet the criteria
     * for deployment.
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
     * @param unitType        the desired type of unit to evaluate for deployment eligibility.
     * @param campaign        the {@link Campaign} containing the forces to evaluate.
     * @param currentTrack    the {@link StratConTrackState} representing the current track, used to filter eligible
     *                        forces.
     * @param reinforcements  {@code true} if the forces are being deployed as reinforcements; otherwise {@code false}.
     * @param currentScenario the current {@link StratConScenario}, if any, used to exclude failed reinforcements. Can
     *                        be {@code null}.
     * @param campaignState   the current {@link StratConCampaignState} representing the campaign state for further
     *                        filtering of eligible forces.
     *
     * @return a {@link List} of unique force IDs that meet all deployment criteria.
     */
    public static List<Integer> getAvailableForceIDsForManualDeployment(int unitType, Campaign campaign,
          StratConTrackState currentTrack, boolean reinforcements, @Nullable StratConScenario currentScenario,
          StratConCampaignState campaignState) {
        List<Integer> retVal = new ArrayList<>();

        // assemble a set of all force IDs that are currently assigned to tracks that are not this one
        Set<Integer> forcesInTracks = campaign.getActiveAtBContracts()
                                            .stream()
                                            .flatMap(contract -> contract.getStratconCampaignState()
                                                                       .getTracks()
                                                                       .stream())
                                            .filter(track -> (!Objects.equals(track, currentTrack)) || !reinforcements)
                                            .flatMap(track -> track.getAssignedForceCoords().keySet().stream())
                                            .collect(Collectors.toSet());

        // if there's an existing scenario, and we're doing reinforcements,
        // prevent forces that failed to deploy from trying to deploy again
        if (reinforcements && (currentScenario != null)) {
            forcesInTracks.addAll(currentScenario.getFailedReinforcements());
        }

        for (CombatTeam formation : campaign.getCombatTeamsAsMap().values()) {
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
                                                       (getReinforcementType(force.getId(),
                                                             currentTrack,
                                                             campaign,
                                                             campaignState) != ReinforcementEligibilityType.NONE);

            if ((force.getScenarioId() <= 0) &&
                      !force.getAllUnits(true).isEmpty() &&
                      !forcesInTracks.contains(force.getId()) &&
                      forceCompositionMatchesDeclaredUnitType(primaryUnitType, unitType) &&
                      noReinforcementRestriction &&
                      !subElementsOrSelfDeployed(force, campaign)) {

                retVal.add(force.getId());
            }
        }

        return retVal;
    }

    /**
     * Returns true if any sub-element (unit or sub-force) of this force is deployed.
     */
    private static boolean subElementsOrSelfDeployed(Force force, Campaign campaign) {
        if (force.isDeployed()) {
            return true;
        }

        if (force.getUnits().stream().map(campaign::getUnit).anyMatch(Unit::isDeployed)) {
            return true;
        }

        return force.getSubForces().stream().anyMatch(child -> subElementsOrSelfDeployed(child, campaign));
    }

    /**
     * Retrieves a list of units that are eligible for deployment in support of a Frontline force.
     *
     * <p>A unit is considered eligible if:</p>
     * <ul>
     *   <li>It is valid (available, properly deployed, and of a suitable type i.e., conventional
     *   infantry or battle armor).</li>
     *   <li>The force to which it belongs is valid (not deployed, part of a combat team, and not in
     *   reserve).</li>
     * </ul>
     *
     * @param campaign The campaign instance holding the units and forces involved.
     *
     * @return A list of {@code Unit} objects that meet the requirements for deployment in support of a Frontline force.
     */
    public static List<Unit> getEligibleFrontlineUnits(Campaign campaign, StratConScenario currentScenario) {
        List<Unit> defensiveUnits = new ArrayList<>();

        // Retrieve the list of units from force 0
        List<UUID> unitIDs = campaign.getAllUnitsInTheTOE(true);

        for (UUID unitId : unitIDs) {
            Unit unit = campaign.getUnit(unitId);

            // Validate the unit
            if (!isUnitValidForFrontlineDeployment(unit)) {
                continue;
            }

            // Validate the force associated with the unit
            if (!isForceEligible(unit, campaign, currentScenario)) {
                continue;
            }

            defensiveUnits.add(unit);
        }

        return defensiveUnits;
    }

    /**
     * Checks if a unit is valid for deployment in support of a Frontline force.
     *
     * <p>A unit is considered valid if:</p>
     * <ul>
     *   <li>The unit is not null.</li>
     *   <li>The unit is available for deployment.</li>
     *   <li>The unit's deployment checks return no errors.</li>
     *   <li>The unit's entity is of type conventional infantry or battle armor.</li>
     * </ul>
     *
     * @param unit The {@code Unit} object to validate.
     *
     * @return {@code true} if the unit is valid; {@code false} otherwise.
     */
    private static boolean isUnitValidForFrontlineDeployment(@Nullable Unit unit) {
        if (unit == null) {
            return false;
        }

        if (!unit.isAvailable()) {
            return false;
        }

        if (unit.checkDeployment() != null) {
            return false;
        }

        Entity entity = unit.getEntity();
        return entity != null && (unit.isConventionalInfantry() || unit.isBattleArmor());
    }

    /**
     * Checks if the force associated with a unit is eligible for deployment in support of a Frontline force.
     *
     * <p>A force is considered eligible if:</p>
     * <ul>
     *   <li>The force is not null.</li>
     *   <li>The force has not already been deployed.</li>
     *   <li>The force is part of a combat team.</li>
     *   <li>The combat team is not assigned a reserve role.</li>
     * </ul>
     *
     * @param unit     The {@code Unit} whose associated force is being validated.
     * @param campaign The {@code Campaign} object used to retrieve information about the force.
     *
     * @return {@code true} if the associated force is eligible; {@code false} otherwise.
     */
    private static boolean isForceEligible(Unit unit, Campaign campaign, StratConScenario currentScenario) {
        int forceId = unit.getForceId();
        Force force = campaign.getForce(forceId);

        // If the force is deployed, skip; added check for insurance
        if (force == null || force.isDeployed()) {
            return false;
        }

        // Check the associated combat team and its role
        CombatTeam combatTeam = force.isCombatTeam() ? campaign.getCombatTeamsAsMap().get(forceId) : null;

        if (combatTeam == null) {
            return false;
        }

        if (combatTeam.getRole().isReserve()) {
            return false;
        }

        AtBContract forceContract = combatTeam.getContract(campaign);
        AtBContract scenarioContract = currentScenario.getBackingContract(campaign);

        return forceContract.equals(scenarioContract);
    }

    /**
     * Retrieves a list of units that are eligible for leadership deployment.
     *
     * <p>A unit is considered eligible for leadership deployment if:</p>
     * <ul>
     *   <li>There is sufficient leadership skill to justify leadership deployment.</li>
     *   <li>The total leadership budget (based on leadership skill) is greater than 0.</li>
     *   <li>The unit matches the general unit type of the primary force in the scenario.</li>
     *   <li>The unit's battle value is within the computed budget.</li>
     *   <li>The unit and its associated force are valid for deployment.</li>
     * </ul>
     *
     * @param campaign        The campaign instance holding the units and forces involved.
     * @param currentScenario The current StratCon scenario being processed.
     * @param leadershipSkill The leadership skill value used to calculate budget.
     *
     * @return A list of {@code Unit} objects eligible for deployment as leadership units.
     */
    public static List<Unit> getEligibleLeadershipUnits(Campaign campaign, StratConScenario currentScenario,
          int leadershipSkill) {
        List<Integer> forceIds = currentScenario.getPrimaryForceIDs();
        List<Unit> leadershipUnits = new ArrayList<>();

        // If there is no leadership skill, we shouldn't continue
        if (leadershipSkill <= 0) {
            return leadershipUnits;
        }

        int totalBudget = min(BASE_LEADERSHIP_BUDGET * leadershipSkill, BASE_LEADERSHIP_BUDGET * 5);

        int primaryUnitType = getPrimaryUnitType(campaign, forceIds);

        // If there are no units (somehow), we've no reason to continue
        if (primaryUnitType == -1) {
            return leadershipUnits;
        }

        int generalUnitType = convertSpecificUnitTypeToGeneral(primaryUnitType);


        // Retrieve the list of units from force 0
        List<UUID> unitIDs = campaign.getAllUnitsInTheTOE(true);

        for (UUID unitId : unitIDs) {
            Unit unit = campaign.getUnit(unitId);

            // Validate the unit
            if (!isUnitValidForLeadershipDeployment(unit, generalUnitType, totalBudget)) {
                continue;
            }

            // Validate the force associated with the unit
            if (!isForceEligible(unit, campaign, currentScenario)) {
                continue;
            }

            leadershipUnits.add(unit);
        }

        return leadershipUnits;
    }

    /**
     * Checks if a unit is valid for leadership deployment.
     *
     * <p>A unit is considered valid for leadership deployment if:</p>
     * <ul>
     *   <li>The unit is not null.</li>
     *   <li>The unit is available for deployment.</li>
     *   <li>The unit has no existing deployment records (i.e., not already deployed).</li>
     *   <li>The unit's entity is valid and has a battle value within the provided leadership budget.</li>
     *   <li>The unit matches the general unit type required for the deployment.</li>
     * </ul>
     *
     * @param unit            The {@code Unit} object to validate for leadership deployment.
     * @param generalUnitType The general unit type required for this deployment.
     * @param totalBudget     The total battle value budget available for leadership deployment.
     *
     * @return {@code true} if the unit is valid for leadership deployment; {@code false} otherwise.
     */
    private static boolean isUnitValidForLeadershipDeployment(@Nullable Unit unit, int generalUnitType,
          int totalBudget) {
        if (unit == null) {
            return false;
        }

        if (!unit.isAvailable()) {
            return false;
        }

        if (unit.checkDeployment() != null) {
            return false;
        }

        Entity entity = unit.getEntity();

        if (entity == null) {
            return false;
        }

        if (entity.calculateBattleValue(true, true) > totalBudget) {
            return false;
        }

        return forceCompositionMatchesDeclaredUnitType(entity.getUnitType(), generalUnitType);
    }

    /**
     * Check if the unit's force (if one exists) has been deployed to a StratCon track
     */
    public static boolean isUnitDeployedToStratCon(Unit unit) {
        if (!unit.getCampaign().getCampaignOptions().isUseStratCon()) {
            return false;
        }

        // this is a little inefficient, but probably there aren't too many active AtB
        // contracts at a time
        return unit.getCampaign()
                     .getActiveAtBContracts()
                     .stream()
                     .anyMatch(contract -> (contract.getStratconCampaignState() != null) &&
                                                 contract.getStratconCampaignState()
                                                       .isForceDeployedHere(unit.getForceId()));
    }

    public static boolean isForceDeployedToStratCon(List<AtBContract> activeAtBContracts, int forceId) {
        return activeAtBContracts
                     .stream()
                     .anyMatch(contract -> (contract.getStratconCampaignState() != null) &&
                                                 contract.getStratconCampaignState()
                                                       .isForceDeployedHere(forceId));
    }

    /**
     * Calculates the majority unit type for the forces given the IDs.
     */
    private static int getPrimaryUnitType(Campaign campaign, List<Integer> forceIDs) {
        Map<Integer, Integer> unitTypeBuckets = new TreeMap<>();
        int biggestBucketID = -1;
        int biggestBucketCount = 0;

        for (int forceID : forceIDs) {
            Force force = campaign.getForce(forceID);
            if (force == null) {
                continue;
            }

            for (UUID id : force.getAllUnits(true)) {
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
     * Determines what rules to use when deploying a force for reinforcements to the given track.
     */
    public static ReinforcementEligibilityType getReinforcementType(int forceID, StratConTrackState trackState,
          Campaign campaign, StratConCampaignState campaignState) {
        // if the force is deployed elsewhere, it cannot be deployed as reinforcements
        if (campaign.getActiveAtBContracts()
                  .stream()
                  .flatMap(contract -> contract.getStratconCampaignState().getTracks().stream())
                  .anyMatch(track -> !Objects.equals(track, trackState) &&
                                           track.getAssignedForceCoords().containsKey(forceID))) {
            return ReinforcementEligibilityType.NONE;
        }

        // TODO: If the force has completed a scenario which allows it,
        // it can deploy "for free" (ReinforcementEligibilityType.ChainedScenario)

        // if the force is in 'fight' stance, it'll be able to deploy using 'fight lance' rules
        if (campaign.getCombatTeamsAsMap().containsKey(forceID)) {
            Hashtable<Integer, CombatTeam> combatTeamsTable = campaign.getCombatTeamsAsMap();
            CombatTeam formation = combatTeamsTable.get(forceID);

            if (formation == null) {
                return ReinforcementEligibilityType.NONE;
            }

            if (campaignState.getSupportPoints() > 0 || campaign.isGM()) {
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
     * Can any force be manually deployed to the given coordinates on the given track for the given contract?
     */
    public static boolean canManuallyDeployAnyForce(StratConCoords coords, StratConTrackState track,
          AtBContract contract) {
        // Rules: can't manually deploy if there's already a force deployed there
        // exception: on allied facilities
        // can't manually deploy if there's a non-cloaked scenario
        StratConScenario scenario = track.getScenario(coords);
        boolean nonCloakedOrNoScenario = (scenario == null) || scenario.getBackingScenario().isCloaked();

        StratConFacility facility = track.getFacility(coords);
        boolean alliedFacility = (facility != null) && (facility.getOwner() == Allied);

        return (!track.areAnyForceDeployedTo(coords) || alliedFacility) && nonCloakedOrNoScenario;
    }

    /**
     * Calculates the scenario odds for a given StratCon track and contract.
     *
     * <p>This method computes the likelihood of a scenario occurring by combining the base scenario odds from the
     * track with modifiers based on the contract's morale level and data center adjustments.</p>
     *
     * <p>The calculation follows these rules:</p>
     * <ul>
     *   <li>If the contract's morale level is {@link AtBMoraleLevel#ROUTED}, the method immediately returns {@code
     *   -1}, indicating that no scenarios can occur.</li>
     *   <li>If {@code isReinforcements} is {@code true}, a morale-based modifier is applied:
     *       <ul>
     *         <li>{@link AtBMoraleLevel#CRITICAL}: -10 penalty</li>
     *         <li>{@link AtBMoraleLevel#WEAKENED}: -5 penalty</li>
     *         <li>{@link AtBMoraleLevel#ADVANCING}: +5 bonus</li>
     *         <li>{@link AtBMoraleLevel#DOMINATING}: +20 bonus</li>
     *         <li>{@link AtBMoraleLevel#OVERWHELMING}: +50 bonus</li>
     *         <li>All other morale levels: no modifier</li>
     *       </ul>
     *   </li>
     *   <li>The track's data center modifier is retrieved and applied to the final calculation.</li>
     * </ul>
     *
     * <p>The final scenario odds value is calculated as:</p>
     * <pre>
     *     base scenario odds + morale modifier + data center modifier
     * </pre>
     *
     * @param track            The {@link StratConTrackState} containing the base scenario odds and data center modifier
     *                         information.
     * @param contract         The {@link AtBContract} containing the morale level information that affects scenario
     *                         odds.
     * @param isReinforcements A flag indicating whether this calculation is for reinforcement scenarios. When
     *                         {@code true}, morale modifiers are applied; when {@code false}, morale has no effect on
     *                         the calculation.
     *
     * @return The calculated scenario odds value. Returns {@code -1} if the contract's morale level is
     *       {@link AtBMoraleLevel#ROUTED}, indicating no scenarios should occur. Otherwise, returns the sum of the base
     *       scenario odds, morale modifier (if applicable), and data center modifier.
     */
    public static int calculateScenarioOdds(StratConTrackState track, AtBContract contract, boolean isReinforcements) {
        if (contract.getMoraleLevel().isRouted()) {
            return -1;
        }

        int moraleModifier = 0;

        if (isReinforcements) {
            moraleModifier += switch (contract.getMoraleLevel()) {
                case CRITICAL -> -10;
                case WEAKENED -> -5;
                case ADVANCING -> 5;
                case DOMINATING -> 20;
                case OVERWHELMING -> 50;
                default -> 0;
            };
        }

        int dataCenterModifier = track.getScenarioOddsAdjustment();

        return track.getScenarioOdds() + moraleModifier + dataCenterModifier;
    }

    /**
     * Removes the facility associated with the given scenario from the relevant track
     */
    public static void updateFacilityForScenario(AtBScenario scenario, AtBContract contract, boolean destroy,
          boolean capture) {
        if (contract.getStratconCampaignState() == null) {
            return;
        }

        // this is kind of kludgy, but there's currently no way to link a scenario back
        // to its backing scenario
        // TODO: introduce mapping in contract or at least track state
        // basically, we're looping through all scenarios on all the contract's tracks
        // if we find one with the same ID as the one being resolved, that's our
        // facility: get rid of it.
        for (StratConTrackState trackState : contract.getStratconCampaignState().getTracks()) {
            for (StratConCoords coords : trackState.getScenarios().keySet()) {
                StratConScenario potentialScenario = trackState.getScenario(coords);
                if (potentialScenario.getBackingScenarioID() == scenario.getId()) {
                    if (destroy) {
                        trackState.removeFacility(coords);
                    } else {
                        StratConFacility facility = trackState.getFacility(coords);

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
     * Processes completion of a StratCon scenario, if the given tracker is associated with a StratCon-enabled mission.
     * Intended to be called after ResolveScenarioTracker.finish() has been invoked.
     */
    public static void processScenarioCompletion(ResolveScenarioTracker tracker) {
        Campaign campaign = tracker.getCampaign();
        Mission mission = tracker.getMission();

        if (mission instanceof AtBContract) {
            StratConCampaignState campaignState = ((AtBContract) mission).getStratconCampaignState();
            if (campaignState == null) {
                return;
            }

            Scenario backingScenario = tracker.getScenario();

            boolean victory = backingScenario.getStatus().isOverallVictory();

            for (StratConTrackState track : campaignState.getTracks()) {
                if (track.getBackingScenariosMap().containsKey(backingScenario.getId())) {
                    // things that may potentially happen:
                    // scenario is removed from track - implemented
                    // track gets remaining forces added to reinforcement pool
                    // facility gets remaining forces stored in reinforcement pool
                    // process VP and SO

                    StratConScenario scenario = track.getBackingScenariosMap().get(backingScenario.getId());

                    StratConFacility facility = track.getFacility(scenario.getCoords());

                    if (scenario.isTurningPoint() && !backingScenario.getStatus().isDraw()) {
                        campaignState.updateVictoryPoints(victory ? 1 : -1);
                    }

                    ScenarioType scenarioType = backingScenario.getStratConScenarioType();
                    if (scenarioType.isSpecial() || backingScenario.isCrisis()) {
                        if (!backingScenario.getStatus().isOverallVictory()) {
                            // If the player loses this scenario, they lose -1 CVP. This represents the importance of
                            // the crisis.
                            campaignState.updateVictoryPoints(-1);
                        }
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
     * Processes completion of a StratCon scenario that is linked to another scenario pulls force off completed
     * scenario, checks to see if entire force is moving on or subset of units
     * <p>
     * Should only be used after a scenario is resolved
     */
    public static void linkedScenarioProcessing(ResolveScenarioTracker tracker,
          HashMap<Integer, List<UUID>> linkedForces) {
        Scenario nextScenario = tracker.getCampaign().getScenario(tracker.getScenario().getLinkedScenario());
        Campaign campaign = tracker.getCampaign();

        if (nextScenario instanceof AtBScenario nextAtBScenario) {

            StratConCampaignState campaignState = nextAtBScenario.getContract(campaign).getStratconCampaignState();
            if (campaignState == null) {
                return;
            }

            for (StratConTrackState track : campaignState.getTracks()) {
                if (track.getBackingScenariosMap().containsKey(nextScenario.getId())) {
                    StratConScenario scenario = track.getBackingScenariosMap().get(nextScenario.getId());
                    //Go through each force that was in previous scenario undeploy it and check to see if entire force is moving on
                    //if so deploy whole force.  Otherwise, just deploy selected units.
                    for (int forceId : linkedForces.keySet()) {
                        track.unassignForce(forceId);

                        if (linkedForces.get(forceId).size() == campaign.getForce(forceId).getAllUnits(false).size()) {
                            scenario.addForce(campaign.getForce(forceId),
                                  ScenarioForceTemplate.REINFORCEMENT_TEMPLATE_ID,
                                  campaign);
                        } else {
                            for (UUID unitId : linkedForces.get(forceId)) {
                                scenario.addUnit(campaign.getUnit(unitId),
                                      ScenarioForceTemplate.REINFORCEMENT_TEMPLATE_ID,
                                      false);
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Worker function that updates strategic objectives relevant to the passed in scenario, track and campaign state.
     * For example, "win scenario A" or "win X scenarios".
     */
    private static void updateStrategicObjectives(boolean victory, StratConScenario scenario,
          StratConTrackState track) {

        // first, we check if this scenario is associated with any specific scenario
        // objectives
        StratConStrategicObjective specificObjective = track.getObjectivesByCoords().get(scenario.getCoords());
        if ((specificObjective != null) &&
                  (specificObjective.getObjectiveType() == StrategicObjectiveType.SpecificScenarioVictory)) {

            if (victory) {
                specificObjective.incrementCurrentObjectiveCount();
            } else {
                specificObjective.setCurrentObjectiveCount(StratConStrategicObjective.OBJECTIVE_FAILED);
            }
        }

        // "any scenario victory" is not linked to any specific coordinates, so we have
        // to
        // search through the track's objectives and update those.
        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            if ((objective.getObjectiveType() == StrategicObjectiveType.AnyScenarioVictory) && victory) {
                objective.incrementCurrentObjectiveCount();
            }
        }
    }

    /**
     * Contains logic for what should happen when a facility gets captured: modifier/type/alignment switches etc.
     */
    public static void switchFacilityOwner(StratConFacility facility) {
        if ((facility.getCapturedDefinition() != null) && !facility.getCapturedDefinition().isBlank()) {
            StratConFacility newOwnerData = StratConFacilityFactory.getFacilityByName(facility.getCapturedDefinition());

            if (newOwnerData != null) {
                facility.copyRulesDataFrom(newOwnerData);
                return;
            }
        }

        // if we have the facility didn't have any data defined for what happens when it's
        // captured
        // fall back to the default of just switching the owner
        if (facility.getOwner() == Allied) {
            facility.setOwner(Opposing);
        } else {
            facility.setOwner(Allied);
        }
    }

    /**
     * Worker function that goes through a track and undeploys any forces where the return date is on or before the
     * given date.
     */
    public static void processTrackForceReturnDates(StratConTrackState track, Campaign campaign) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AtBStratCon",
              MekHQ.getMHQOptions().getLocale());

        List<Integer> forcesToUndeploy = new ArrayList<>();
        LocalDate date = campaign.getLocalDate();

        // for each force on the track, if the return date is today or in the past,
        // and the scenario has not yet occurred, undeploy it.
        // "return to base", unless it's been told to stay in the field
        for (int forceID : track.getAssignedForceReturnDates().keySet()) {
            Force force = campaign.getForce(forceID);

            if ((track.getAssignedForceReturnDates().get(forceID).equals(date) ||
                       track.getAssignedForceReturnDates().get(forceID).isBefore(date)) &&
                      (force != null) &&
                      !track.getBackingScenariosMap().containsKey(force.getScenarioId()) &&
                      !track.getStickyForces().contains(forceID)) {
                forcesToUndeploy.add(forceID);

                campaign.addReport(String.format(resources.getString("force.undeployed"), force.getName()));
            }
        }

        for (int forceID : forcesToUndeploy) {
            track.unassignForce(forceID);
        }
    }

    /**
     * Processes an ignored dynamic scenario by locating it on one of the tracks and invoking the standard 'ignored
     * scenario' routine for additional processing.
     *
     * <p>This method iterates over the tracks in the campaign state to find the specified scenario by its ID. Once
     * located, it processes the scenario using the appropriate logic to handle ignored scenarios.</p>
     *
     * @param scenarioId    The ID of the dynamic scenario to be processed.
     * @param campaignState The state of the current campaign, used to access tracks and scenarios.
     */
    public static void processIgnoredDynamicScenario(int scenarioId, StratConCampaignState campaignState) {
        for (StratConTrackState track : campaignState.getTracks()) {
            Map<Integer, StratConScenario> backingScenarios = track.getBackingScenariosMap();
            StratConScenario stratConScenario = backingScenarios.get(scenarioId);

            if (stratConScenario != null) {
                processIgnoredStratConScenario(stratConScenario, track, campaignState);
                break;
            }
        }
    }

    /**
     * Processes an ignored StratCon scenario by removing it from the campaign state and updating related state
     * variables, including victory points, facility ownership, and objectives.
     *
     * <p>This method is called when a StratCon scenario is ignored, and it ensures that the state of the campaign is
     * updated accordingly. The following operations are performed:</p>
     *
     * <ul>
     *   <li><b>Victory Points Adjustment:</b>
     *       If the scenario is marked as "special" or a "turning point," the campaign's victory points are reduced by 1
     *       to reflect a penalty before the scenario is removed.</li>
     *   <li><b>Scenario Removal:</b>
     *       The ignored scenario is removed from its associated track.</li>
     *   <li><b>Facility Ownership and Objective Status:</b>
     *       <ul>
     *         <li>If no facility is associated with the scenario's coordinates, the objective tied to the scenario's
     *         location is marked as failed.</li>
     *         <li>If a facility exists at the scenario's location and is owned by allied forces, ownership is flipped
     *         to the opposing forces.</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param scenario      The {@link StratConScenario} that is being ignored and processed for removal. This includes
     *                      information such as the scenario type and coordinates.
     * @param track         The {@link StratConTrackState} representing the track the scenario is located on, which will
     *                      be updated to reflect scenario removal and any resulting state changes.
     * @param campaignState The {@link StratConCampaignState} representing the overall state of the campaign, which will
     *                      be updated during the processing (e.g., victory points adjustments).
     */
    public static void processIgnoredStratConScenario(StratConScenario scenario, StratConTrackState track,
          StratConCampaignState campaignState) {
        AtBDynamicScenario backingScenario = scenario.getBackingScenario();
        boolean isCrisis = backingScenario != null && backingScenario.isCrisis();

        // Update victory points if the scenario is marked as "special" or "turning point"
        if (scenario.isSpecial() || scenario.isTurningPoint() || isCrisis) {
            campaignState.updateVictoryPoints(-1);
        }

        // Remove the scenario from the track
        track.removeScenario(scenario);

        // Check the facility associated with the scenario, if any
        StratConFacility localFacility = track.getFacility(scenario.getCoords());
        if (localFacility == null) {
            // Fail the objective if no facility is found
            track.failObjective(scenario.getCoords());
        } else if (localFacility.getOwner() == Allied) {
            // Update the facility's ownership if it belongs to allies
            localFacility.setOwner(Opposing);
        }
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
            StratConCampaignState campaignState = contract.getStratconCampaignState();

            if (campaignState != null) {
                for (StratConTrackState track : campaignState.getTracks()) {
                    cleanupPhantomScenarios(track);

                    // check if some of the forces have finished deployment
                    // please do this before generating scenarios for track
                    // to avoid unintentionally cleaning out integrated force deployments on
                    // 0-deployment-length tracks
                    processTrackForceReturnDates(track, campaign);

                    if (!campaign.getCampaignOptions().isUseStratConMaplessMode()) {
                        processFacilityEffects(track, campaignState, isStartOfMonth);
                    }

                    // loop through scenarios - if we haven't deployed in time,
                    // fail it and apply consequences
                    for (StratConScenario scenario : track.getScenarios().values()) {
                        if ((scenario.getDeploymentDate() != null) &&
                                  scenario.getDeploymentDate().isBefore(campaign.getLocalDate()) &&
                                  scenario.getPrimaryForceIDs().isEmpty()) {
                            processIgnoredStratConScenario(scenario, track, campaignState);
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
     * Worker function that goes through a track and cleans up scenarios missing required data
     */
    private void cleanupPhantomScenarios(StratConTrackState track) {
        List<StratConScenario> cleanupList = track.getScenarios()
                                                   .values()
                                                   .stream()
                                                   .filter(scenario -> (scenario.getDeploymentDate() == null) &&
                                                                             !scenario.isStrategicObjective())
                                                   .toList();

        for (StratConScenario scenario : cleanupList) {
            track.removeScenario(scenario);
        }
    }

    public void shutdown() {
        MekHQ.unregisterHandler(this);
    }
}
