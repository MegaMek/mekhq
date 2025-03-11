/*
 * Copyright (c) 2019-2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import megamek.common.Entity;
import megamek.common.OffBoardDirection;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.stratcon.StratconRulesManager;
import org.apache.logging.log4j.LogManager;

import java.util.*;

import static mekhq.campaign.mission.resupplyAndCaches.PerformResupply.performResupply;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType.RESUPPLY_LOOT;

/**
 * Handles processing for objectives for a scenario that has them
 * @author NickAragua
 */
public class ScenarioObjectiveProcessor {

    private Map<ScenarioObjective, Set<String>> qualifyingObjectiveUnits;
    private Map<ScenarioObjective, Set<String>> potentialObjectiveUnits;

    /**
     * Blank constructor
     */
    public ScenarioObjectiveProcessor() {
        qualifyingObjectiveUnits = new HashMap<>();
        potentialObjectiveUnits = new HashMap<>();
    }

    /**
     * Given a ResolveScenarioTracker, evaluate the units contained therein
     * in an effort to determine the units associated and units that meet each objective in the scenario played.
     * @param tracker Tracker to process
     */
    public void evaluateScenarioObjectives(ResolveScenarioTracker tracker) {
        for (ScenarioObjective objective : tracker.getScenario().getScenarioObjectives()) {
            Set<String> currentObjectiveUnitIDs = new HashSet<>(objective.getAssociatedUnitIDs());
            currentObjectiveUnitIDs.addAll(unrollObjectiveForces(objective, tracker));

            potentialObjectiveUnits.put(objective, currentObjectiveUnitIDs);

            Set<String> qualifyingUnits = evaluateObjective(objective, currentObjectiveUnitIDs,
                    tracker.getAllInvolvedUnits().values(), tracker);
            qualifyingObjectiveUnits.put(objective, qualifyingUnits);
        }
    }

    /**
     * Given an objective and a resolution tracker, "unroll" the force names specified in the objective
     * into a set of unit IDs.
     */
    private Set<String> unrollObjectiveForces(ScenarioObjective objective, ResolveScenarioTracker tracker) {
        Set<String> objectiveUnitIDs = new HashSet<>();

        // "expand" the player forces involved in the objective
        for (String forceName : objective.getAssociatedForceNames()) {
            boolean forceFound = false;

            if (MHQConstants.EGO_OBJECTIVE_NAME.equals(forceName)) {
                // get the units from the player's forces assigned to the scenario
                for (UUID unitID : tracker.getScenario().getForces(tracker.getCampaign()).getAllUnits(true)) {
                    objectiveUnitIDs.add(tracker.getCampaign().getUnit(unitID).getEntity().getExternalIdAsString());
                }
                continue;
            }

            for (Force force : tracker.getCampaign().getAllForces()) {
                if (force.getName().equals(forceName)) {
                    for (UUID unitID : force.getUnits()) {
                        objectiveUnitIDs.add(tracker.getCampaign().getUnit(unitID).getEntity().getExternalIdAsString());
                    }
                    forceFound = true;
                    break;
                }
            }

            // if we've found the objective's force in the campaign's force list, we can move on to the next one
            if (forceFound) {
                continue;
            }

            for (int botIndex = 0; botIndex < tracker.getScenario().getNumBots(); botIndex++) {
                BotForce botForce = tracker.getScenario().getBotForce(botIndex);

                if (forceName.equals(botForce.getName())) {
                    for (Entity entity : botForce.getFullEntityList(tracker.getCampaign())) {
                        objectiveUnitIDs.add(entity.getExternalIdAsString());
                    }
                    break;
                }
            }
        }

        return objectiveUnitIDs;
    }

    /**
     * Evaluates whether the given list of units meets the given objective.
     * @return The list of units that qualify for the given objective.
     */
    private Set<String> evaluateObjective(ScenarioObjective objective, Set<String> objectiveUnitIDs,
            Collection<Entity> units, ResolveScenarioTracker tracker) {
        Set<String> qualifyingUnits = new HashSet<>();

        for (Entity unit : units) {
            // the "opponent" depends on whether the given unit was opposed to the player or not
            boolean isFriendlyUnit = tracker.getScenario().isFriendlyUnit(unit, tracker.getCampaign());

            boolean opponentHasBattlefieldControl = isFriendlyUnit != tracker.playerHasBattlefieldControl();

            if (entityMeetsObjective(unit, objective, objectiveUnitIDs, opponentHasBattlefieldControl)) {
                qualifyingUnits.add(unit.getExternalIdAsString());
            }
        }

        return qualifyingUnits;
    }

    /**
     * Update the objective qualification status of a given entity based on its current state and
     * on whether or not it has "escaped". Assumes that the entity is potentially eligible for the objective.
     * @param entity
     * @param forceEntityEscape Whether the entity was marked as 'escaped', regardless of entity status
     * @param forceEntityDestruction Whether the entity was marked as 'destroyed', regardless of entity status
     * @param opponentHasBattlefieldControl
     */
    public void updateObjectiveEntityState(Entity entity, boolean forceEntityEscape,
            boolean forceEntityDestruction, boolean opponentHasBattlefieldControl) {
        if (entity == null) {
            return;
        }

        for (ScenarioObjective objective : potentialObjectiveUnits.keySet()) {
            boolean entityMeetsObjective = false;

            if (potentialObjectiveUnits.get(objective).contains(entity.getExternalIdAsString())) {
                switch (objective.getObjectiveCriterion()) {
                    case Destroy:
                        entityMeetsObjective = forceEntityDestruction ||
                            !forceEntityEscape && entityIsDestroyed(entity, opponentHasBattlefieldControl);
                        break;
                    case ForceWithdraw:
                        entityMeetsObjective = forceEntityDestruction ||
                            !forceEntityEscape && entityIsForcedWithdrawal(entity);
                        break;
                    case Capture:
                        entityMeetsObjective = !forceEntityEscape && entityIsCaptured(entity, opponentHasBattlefieldControl);
                        break;
                    case PreventReachMapEdge:
                        entityMeetsObjective = forceEntityDestruction ||
                                !entityHasReachedDestinationEdge(entity, objective);
                        break;
                    case Preserve:
                        entityMeetsObjective = forceEntityEscape ||
                            !forceEntityDestruction && !entityIsDestroyed(entity, opponentHasBattlefieldControl);
                        break;
                    case ReachMapEdge:
                        entityMeetsObjective = forceEntityEscape ||
                            !forceEntityDestruction && entityHasReachedDestinationEdge(entity, objective);
                        break;
                    // criteria that we have no way of tracking will not be doing any updates
                    default:
                        continue;
                }
            }

            if (entityMeetsObjective) {
                qualifyingObjectiveUnits.get(objective).add(entity.getExternalIdAsString());
            } else {
                qualifyingObjectiveUnits.get(objective).remove(entity.getExternalIdAsString());
            }
        }
    }

    /**
     * Determines if the given entity has met the given objective
     * @param entity Entity to check
     * @param objective Objective to check
     * @param opponentHasBattlefieldControl Whether the entity's opponent has battlefield control
     */
    private boolean entityMeetsObjective(Entity entity, ScenarioObjective objective,
            Set<String> objectiveUnitIDs, boolean opponentHasBattlefieldControl) {
        if (objectiveUnitIDs.contains(entity.getExternalIdAsString())) {
            return switch (objective.getObjectiveCriterion()) {
                case Destroy -> entityIsDestroyed(entity, opponentHasBattlefieldControl);
                case ForceWithdraw -> entityIsForcedWithdrawal(entity);
                case Capture -> entityIsCaptured(entity, !opponentHasBattlefieldControl);
                case PreventReachMapEdge -> !entityHasReachedDestinationEdge(entity, objective);
                case Preserve -> !entityIsDestroyed(entity, opponentHasBattlefieldControl);
                case ReachMapEdge -> entityHasReachedDestinationEdge(entity, objective);
                default -> false;
            };
        }

        return false;
    }

    /**
     * Check whether we should consider an entity as being destroyed for the purposes of a Destroy objective.
     */
    private boolean entityIsDestroyed(Entity entity, boolean opponentHasBattlefieldControl) {
        // "destroy" is a kill
        // it's destroyed if it's destroyed, or if it's been disabled and the enemy has no chance to recover it
        return entity.isDestroyed() || ((entity.getCrew().isDead() || entity.isImmobile()) && opponentHasBattlefieldControl);
    }

    /**
     * Check whether we should consider an entity as being forced to withdraw for the purposes of a ForceWithdraw objective
     */
    private boolean entityIsForcedWithdrawal(Entity entity) {
        // we consider an entity force-withdrawn if it's destroyed, crippled, or run off the field
        // note: immobility and having a dead crew are captured within 'crippled'
        return entity.isDestroyed() || entity.isCrippled(true) || entity.getRetreatedDirection() != OffBoardDirection.NONE;
    }

    /**
     * Check whether we should consider an entity as being captured for the purposes of a Capture objective.
     */
    private boolean entityIsCaptured(Entity entity, boolean opponentHasBattlefieldControl) {
        // we consider an entity captured if it's been immobilized but not destroyed and hasn't left the field
        // obviously can't capture it if we don't control the battlefield
        return entity.isImmobile() && !entity.isDestroyed() &&
                entity.getRetreatedDirection() == OffBoardDirection.NONE && !opponentHasBattlefieldControl;
    }

    /**
     * Check whether or not the entity can be considered as having reached the destination edge in the given objective
     */
    private boolean entityHasReachedDestinationEdge(Entity entity, ScenarioObjective objective) {
                // we've reached the destination edge if we've reached an edge and it's the right one
        return ((entity.getRetreatedDirection() != OffBoardDirection.NONE) && (entity.getRetreatedDirection() == objective.getDestinationEdge()));
    }

    /**
     * Processes the given scenario and its objectives scenario objective, applying objective effects to the campaign
     * as necessary
     * @param scenario The scenario to process.
     * @param objectiveOverrides Map containing user overrides of objective completion state
     * @param objectiveUnitCounts Map containing objectives and the number of units that qualified for each.
     */
    public ScenarioStatus determineScenarioStatus(Scenario scenario,
                                                  Map<ScenarioObjective, Boolean> objectiveOverrides,
                                                  Map<ScenarioObjective, Integer> objectiveUnitCounts) {
        int victoryScore = 0;

        if (!scenario.hasObjectives()) {
            return ScenarioStatus.DRAW;
        }

        for (ScenarioObjective objective : scenario.getScenarioObjectives()) {

            // if the scenario is not in our objectiveUnitCounts or objectiveOverrides, skip it
            if (!(objectiveUnitCounts.containsKey(objective) || objectiveOverrides.containsKey(objective))) {
                continue;
            }

            boolean objectiveMet = (objectiveOverrides.containsKey(objective) && objectiveOverrides.get(objective) != null) ?
                objectiveOverrides.get(objective) : objectiveMet(objective, objectiveUnitCounts.get(objective));

            List<ObjectiveEffect> objectiveEffects = objectiveMet ? objective.getSuccessEffects() : objective.getFailureEffects();

            for (ObjectiveEffect effect : objectiveEffects) {
                if (effect.effectType == ObjectiveEffectType.ScenarioVictory) {
                    victoryScore += effect.howMuch;
                } else if (effect.effectType == ObjectiveEffectType.ScenarioDefeat) {
                    victoryScore -= effect.howMuch;
                }
            }
        }

        if (victoryScore > 0) {
            return ScenarioStatus.VICTORY;
        } else if (victoryScore < 0) {
            return ScenarioStatus.DEFEAT;
        } else {
            return ScenarioStatus.DRAW;
        }
    }

    /**
     * Processes a particular scenario objective, applying its effects to the
     * current mission and campaign as necessary
     * @param objective The objective to process.
     * @param qualifyingUnitCount How many units qualified for the objective, used to scale the objective effect if necessary
     * @param completionOverride If null, objective completion is calculated dynamically, otherwise a fixed objective completion state.
     * @param tracker The tracker from which to draw unit data
     * @param dryRun Whether we're actually applying the objectives or just generating a report.
     */
    public String processObjective(Campaign campaign, ScenarioObjective objective, int qualifyingUnitCount, Boolean completionOverride,
            ResolveScenarioTracker tracker, boolean dryRun) {
        // if we've overridden the objective completion flag, great, otherwise, calculate it here
        boolean objectiveMet = completionOverride == null ? objectiveMet(objective, qualifyingUnitCount) : completionOverride;

        List<ObjectiveEffect> objectiveEffects = objectiveMet ? objective.getSuccessEffects() : objective.getFailureEffects();

        int numUnitsFailedObjective = potentialObjectiveUnits.get(objective).size() - qualifyingUnitCount;

        StringBuilder sb = new StringBuilder();
        if (dryRun) {
            sb.append(objective.getDescription());
            sb.append("\n\t");
            sb.append(objectiveMet ? "Completed" : "Failed");
            sb.append("\n\t");
        }

        for (ObjectiveEffect effect : objectiveEffects) {
            sb.append(processObjectiveEffect(campaign, effect,
                    effect.effectScaling == EffectScalingType.Inverted ? numUnitsFailedObjective : qualifyingUnitCount,
                    tracker, dryRun));
            sb.append("\n\t");
        }

        return sb.toString();
    }

    /**
     * Processes the specified objective effect during a scenario and applies the corresponding
     * changes to the campaign or contract. This method determines the type of effect and executes
     * its logic accordingly, supporting both dry-run and active execution modes.
     *
     * <p>Effect types supported include (but are not limited to):
     * <ul>
     *     <li>Scenario victory or defeat point adjustments.</li>
     *     <li>Updates to contract scores, morale, or support points.</li>
     *     <li>Triggering contract victory or defeat conditions.</li>
     *     <li>Handling AtB-specific bonuses, such as bonus resupply points or resource drops.</li>
     *     <li>Updating the status of strategic facilities (e.g., capturing/destroying facilities).</li>
     * </ul>
     *
     * <p>When run in dry-run mode, the method simply returns a description of the effect without
     * applying any changes to the campaign or contract. In active mode, the effect is applied to
     * the appropriate {@link Campaign}, {@link AtBContract}, or scenario entities.</p>
     *
     * @param campaign   the {@link Campaign} instance being updated.
     * @param effect     the {@link ObjectiveEffect} defining the effect type and its further details.
     * @param scaleFactor the factor by which the objective effect is scaled (used for value calculations).
     * @param tracker    the {@link ResolveScenarioTracker} providing context about the scenario, mission, and contract.
     * @param dryRun     a {@code boolean} flag indicating if the method should process in dry-run mode
     *                   ({@code true}) or actively apply the effect ({@code false}).
     * @return a {@link String} describing the effect, used during dry-run mode; otherwise an empty string is returned.
     */
    private String processObjectiveEffect(Campaign campaign, ObjectiveEffect effect, int scaleFactor,
                                          ResolveScenarioTracker tracker, boolean dryRun) {
        switch (effect.effectType) {
            case ScenarioVictory:
                if (dryRun) {
                    return String.format("%d Scenario Victory Point/s", effect.howMuch);
                }
                break;
            case ScenarioDefeat:
                if (dryRun) {
                    return String.format("%d Scenario Victory Point/s", -effect.howMuch);
                }
                break;
            case ContractScoreUpdate:
                // if atb contract, update contract score by how many units met criterion * scaling
                if (tracker.getMission() instanceof AtBContract contract) {
                    int effectMultiplier = effect.effectScaling == EffectScalingType.Fixed ? 1 : scaleFactor;
                    int scoreEffect = effect.howMuch * effectMultiplier;

                    if (dryRun) {
                        return String.format("%d Contract Score/Campaign Victory Points", scoreEffect);
                    } else {
                        contract.setContractScoreArbitraryModifier(contract.getContractScoreArbitraryModifier() + scoreEffect);
                    }
                }
                break;
            case SupportPointUpdate:
                if (tracker.getMission() instanceof AtBContract contract) {
                    if (contract.getStratconCampaignState() != null) {
                        int effectMultiplier = effect.effectScaling == EffectScalingType.Fixed ? 1 : scaleFactor;
                        int numSupportPoints = effect.howMuch * effectMultiplier;
                        if (dryRun) {
                            return String.format("%d support points will be added", numSupportPoints);
                        } else {
                            contract.getStratconCampaignState().changeSupportPoints(numSupportPoints);
                        }
                    }
                }
                break;
            case SupplyCache:
                if (tracker.getMission() instanceof AtBContract contract) {
                    Resupply resupply = new Resupply(campaign, contract, RESUPPLY_LOOT);

                    // This scaling means users will need to have at least 10 qualifying units before they get a Resupply.
                    int effectMultiplier = effect.effectScaling == EffectScalingType.Fixed ? 1 : (int) (scaleFactor * 0.1);
                    int size = effect.howMuch * effectMultiplier;

                    if (dryRun) {
                        return String.format("A size %d supply cache will be added", size);
                    } else {
                        performResupply(resupply, contract, size);
                    }
                }
                break;
            case ContractMoraleUpdate:
                break;
            case ContractVictory:
                if (dryRun) {
                    return "Contract ends with victory";
                } else {
                    tracker.getCampaign().addReport(
                            String.format("Victory in scenario %s ends the contract with a victory", tracker.getScenario().getDescription()));
                }
                break;
            case ContractDefeat:
                if (dryRun) {
                    return "Contract ends with loss";
                } else {
                    tracker.getCampaign().addReport(
                            String.format("Defeat in scenario %s ends the contract with a defeat", tracker.getScenario().getDescription()));
                }
                break;
            case BVBudgetUpdate:
                break;
            case AtBBonus:
                if (tracker.getMission() instanceof AtBContract contract) {
                    int effectMultiplier = effect.effectScaling == EffectScalingType.Fixed ? 1 : scaleFactor;
                    int numBonuses = effect.howMuch * effectMultiplier;
                    if (dryRun) {
                        return String.format("%d AtB bonus rolls", numBonuses);
                    } else {
                        int dropSize = 0;
                        for (int x = 0; x < numBonuses; x++) {
                            dropSize += contract.doBonusRoll(tracker.getCampaign(), true)
                                ? 1 : 0;
                        }

                        if (dropSize > 0) {
                            LogManager.getLogger().info("ScenarioObjectiveProcessor.java");
                            campaign.addReport("Bonus: Captured Supplies");
                            Resupply resupply = new Resupply(campaign, contract, RESUPPLY_LOOT);
                            performResupply(resupply, contract, dropSize);
                        }
                    }
                }
            case FacilityRemains:
                if ((tracker.getMission() instanceof AtBContract) && (tracker.getScenario() instanceof AtBScenario)) {
                    if (dryRun) {
                        return "This facility will not be captured.";
                    } else {
                        StratconRulesManager.updateFacilityForScenario((AtBScenario) tracker.getScenario(), (AtBContract) tracker.getMission(), false, false);
                    }
                }
                break;
            case FacilityRemoved:
                if ((tracker.getMission() instanceof AtBContract) && (tracker.getScenario() instanceof AtBScenario)) {
                    if (dryRun) {
                        return "This facility will be destroyed.";
                    } else {
                        StratconRulesManager.updateFacilityForScenario((AtBScenario) tracker.getScenario(), (AtBContract) tracker.getMission(), true, false);
                    }
                }
                break;
            case FacilityCaptured:
                if (tracker.getMission() instanceof AtBContract) {
                    if (dryRun) {
                        return "Allied forces will control this facility.";
                    } else {
                        StratconRulesManager.updateFacilityForScenario((AtBScenario) tracker.getScenario(), (AtBContract) tracker.getMission(), false, true);
                    }
                }
        }

        return "";
    }

    /**
     * Determines if the given objective will be met with the given number of units.
     */
    public boolean objectiveMet(ScenarioObjective objective, int qualifyingUnitCount) {
        if (objective.getFixedAmount() != null) {
            return qualifyingUnitCount >= objective.getFixedAmount();
        }

        if (!getPotentialObjectiveUnits().containsKey(objective)) {
            return false;
        }

        double potentialObjectiveUnitCount = getPotentialObjectiveUnits().get(objective).size();

        return qualifyingUnitCount / potentialObjectiveUnitCount >= (double) objective.getPercentage() / 100;
    }

    public Map<ScenarioObjective, Set<String>> getQualifyingObjectiveUnits() {
        return qualifyingObjectiveUnits;
    }

    public Map<ScenarioObjective, Set<String>> getPotentialObjectiveUnits() {
        return potentialObjectiveUnits;
    }
}
