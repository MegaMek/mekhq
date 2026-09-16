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
package mekhq.campaign.digitalGM.stratCon.deployment;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.getReinforcementType;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.processReinforcementDeployment;
import static mekhq.campaign.digitalGM.stratCon.StratConScenario.ScenarioState.REINFORCEMENTS_COMMITTED;
import static mekhq.campaign.enums.DailyReportType.POLITICS;
import static mekhq.campaign.mission.scenarios.AtBDynamicScenarioFactory.scaleObjectiveTimeLimits;
import static mekhq.campaign.mission.scenarios.AtBDynamicScenarioFactory.translateTemplateObjectives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.Minefield;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.gm.StratConGMs;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.EnemyData;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.SupportCarrierDeployment;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * The non-GUI deployment operations for the StratCon deployment wizard: the campaign-state mutations that commit forces
 * and units to a track or scenario. Migrated out of the wizard so the GUI owns only presentation (the boards, the
 * inspector, and the confirmation dialogs) while the rules-affecting work lives in the campaign layer.
 *
 * <p>Every method here takes decisions the player has already made (which forces, how many support points, whether GM
 * or instant) and applies them; it shows no dialogs and reads no widgets.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConDeploymentService {
    private static final MMLogger LOGGER = MMLogger.create(StratConDeploymentService.class);

    private StratConDeploymentService() {}

    /**
     * Deploys primary forces to the selected hex, or assigns them to the unresolved scenario there. Mirrors the legacy
     * {@code TrackForceAssignmentUI} commit.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void deployPrimaryForces(Campaign campaign, StratConCampaignState campaignState,
          StratConTrackState track, StratConCoords coords, boolean assignToScenario, List<Integer> forceIds) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        for (int forceId : forceIds) {
            if (assignToScenario) {
                StratConGMs.forceDeployment(campaignOptions)
                      .assignForceToScenario(coords, forceId, campaign, campaignState.getContract(), track, false);
            } else {
                StratConGMs.forceDeployment(campaignOptions)
                      .deployForceToCoords(coords, forceId, campaign, campaignState.getContract(), track, false);
            }
        }
    }

    /**
     * Commits reinforcement forces to a scenario, grouped by the reinforcement template slot they fill. Each force
     * makes its reinforcement roll; a failed roll is recorded and the force is not added, while a delayed or instant
     * result collects the force's deploying units into the scenario's respective arrival lists.
     *
     * @param forcesByTemplate    the forces to commit, keyed by their reinforcement template id
     * @param reinforcementTargetNumber the roll target, or {@code null} for a GM commit that bypasses the roll
     *
     * @return the forces that were processed (for the caller's "staying home" summary), in commit order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Formation> commitReinforcementForces(Campaign campaign, StratConCampaignState campaignState,
          StratConTrackState track, StratConScenario scenario, Map<String, List<Formation>> forcesByTemplate,
          @Nullable Integer reinforcementTargetNumber, boolean isGMReinforcement, boolean isInstantlyDeployed) {
        List<UUID> delayedReinforcements = scenario.getBackingScenario().getFriendlyDelayedReinforcements();
        List<UUID> instantReinforcements = scenario.getBackingScenario().getFriendlyInstantReinforcements();
        List<Formation> committedFormations = new ArrayList<>();

        for (Map.Entry<String, List<Formation>> entry : forcesByTemplate.entrySet()) {
            String templateId = entry.getKey();
            for (Formation formation : entry.getValue()) {
                committedFormations.add(formation);

                ReinforcementEligibilityType reinforcementType = getReinforcementType(formation.getId(),
                      track,
                      campaign,
                      campaignState);
                ReinforcementResultsType reinforcementResults = processReinforcementDeployment(formation,
                      reinforcementType,
                      campaignState,
                      scenario,
                      campaign,
                      reinforcementTargetNumber,
                      isGMReinforcement,
                      isInstantlyDeployed);

                if (reinforcementResults.ordinal() >= ReinforcementResultsType.FAILED.ordinal()) {
                    scenario.addFailedReinforcements(formation.getId());
                    continue;
                }

                scenario.addForce(formation, templateId, campaign);

                if (reinforcementResults == ReinforcementResultsType.DELAYED) {
                    collectDeployingUnits(campaign, scenario, formation, delayedReinforcements);
                } else if (reinforcementResults == ReinforcementResultsType.INSTANT) {
                    collectDeployingUnits(campaign, scenario, formation, instantReinforcements);
                }
            }
        }

        return committedFormations;
    }

    /**
     * Adds utility (frontline/defensive) units to the scenario. Each arrives instantly.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void addUtilityUnits(StratConScenario scenario, List<Unit> units) {
        List<UUID> instantReinforcements = scenario.getBackingScenario().getFriendlyInstantReinforcements();
        for (Unit unit : units) {
            instantReinforcements.add(unit.getId());
            scenario.addUnit(unit, ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID, false);
        }
    }

    /**
     * Adds auxiliary (leadership) units to the scenario. Each arrives instantly.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void addAuxiliaryUnits(StratConScenario scenario, List<Unit> units) {
        List<UUID> instantReinforcements = scenario.getBackingScenario().getFriendlyInstantReinforcements();
        for (Unit unit : units) {
            instantReinforcements.add(unit.getId());
            scenario.addUnit(unit, ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID, true);
        }
    }

    /**
     * Drops every unit that already belongs to one of the given formations. A formation deploys as a whole, including
     * its sub-formations, so its units must not be offered - or committed - a second time as loose auxiliary or utility
     * units; doing so would add the unit to the scenario twice and charge its battle value to the leadership budget.
     *
     * @param units      the candidate loose units, in the order they should be kept
     * @param formations the formations whose units are excluded; an empty collection excludes nothing
     *
     * @return a new list holding the units not covered by any of the formations, in their original order
     */
    public static List<Unit> excludeUnitsOfFormations(List<Unit> units, Collection<Formation> formations) {
        Set<UUID> coveredUnitIds = unitIdsOfFormations(formations);
        List<Unit> remainingUnits = new ArrayList<>();
        int excludedCount = 0;
        for (Unit unit : units) {
            if (coveredUnitIds.contains(unit.getId())) {
                excludedCount++;
                continue;
            }
            remainingUnits.add(unit);
        }

        if (excludedCount > 0) {
            LOGGER.debug("[Deployment] Excluded {} loose unit(s) that already deploy with a staged formation",
                  excludedCount);
        }
        return remainingUnits;
    }

    /**
     * @param formations the formations to inspect
     *
     * @return the IDs of every unit under the given formations, including their sub-formations of any type
     */
    public static Set<UUID> unitIdsOfFormations(Collection<Formation> formations) {
        Set<UUID> unitIds = new HashSet<>();
        for (Formation formation : formations) {
            unitIds.addAll(formation.getAllUnits(false));
        }
        return unitIds;
    }

    /**
     * Finalizes a page's commit: assigns every deployed force to the track and, the first time forces are committed,
     * translates and scales the scenario's objectives. Safe to call once per page that commits (the objective work is
     * gated on scenario state).
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void finalizeForceDeployment(Campaign campaign, StratConTrackState track, StratConScenario scenario) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        for (int forceId : scenario.getAssignedForces()) {
            StratConGMs.forceDeployment(campaignOptions)
                  .processForceDeployment(scenario.getCoords(), forceId, campaign, track, false);
        }

        if (scenario.getCurrentState().ordinal() < REINFORCEMENTS_COMMITTED.ordinal()) {
            translateTemplateObjectives(scenario.getBackingScenario(), campaign);
            scaleObjectiveTimeLimits(scenario.getBackingScenario(), campaign);
        }
    }

    /**
     * Sets the scenario's conventional minefield count. Only the Utility page changes this, so it is separate from
     * {@link #finalizeForceDeployment} to keep the other pages from resetting it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void setMinefieldCount(StratConScenario scenario, int minefieldCount) {
        scenario.updateMinefieldCount(Minefield.TYPE_CONVENTIONAL, minefieldCount);
    }

    /**
     * Records the consequences of breaching an accepted batchall: the contract is marked no longer batchall-accepted
     * and, when faction standing is tracked, regard is adjusted and the resulting reports are logged.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processBatchallBreach(Campaign campaign, AbstractContract contract, String enemyCode) {
        contract.setEnemyData(new EnemyData(contract.getEnemyData(), false));

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (campaignOptions.get(CampaignOption.TRACK_FACTION_STANDING)) {
            FactionStandings factionStandings = campaign.getPlayerForce().getFactionStandings();
            // Breaching a batchall you accepted is worse than never accepting one, so the regard hit is doubled.
            double regardMultiplier = campaignOptions.get(CampaignOption.REGARD_MULTIPLIER) * 2;

            List<String> reports = factionStandings.processRefusedBatchall(campaign.getPlayerForce()
                                                                                 .getFaction()
                                                                                 .getShortName(),
                  enemyCode,
                  campaign.getGameYear(),
                  regardMultiplier);

            for (String report : reports) {
                campaign.addReport(POLITICS, report);
            }
        }
    }

    private static void collectDeployingUnits(Campaign campaign, StratConScenario scenario, Formation formation,
          List<UUID> reinforcements) {
        for (UUID unitId : formation.getAllUnits(true)) {
            Unit unit = campaign.getUnit(unitId);
            if ((unit != null) && !SupportCarrierDeployment.staysHome(unit, scenario.getBackingScenario())) {
                reinforcements.add(unitId);
            }
        }
    }
}
