/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.stratCon.StratConScenario.ScenarioState.PRIMARY_FORCES_COMMITTED;
import static mekhq.campaign.stratCon.StratConScenario.ScenarioState.UNRESOLVED;

import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.gui.StratConPanel;
import mekhq.gui.stratCon.StratConScenarioWizard;
import mekhq.gui.stratCon.TrackForceAssignmentUI;


/**
 * Utility class for managing mapless StratCon scenario deployment operations.
 *
 * <p>This class provides functionality for deploying forces to StratCon scenarios without using the traditional
 * map-based interface. It handles the workflow of locating scenarios within campaign tracks, presenting appropriate
 * dialogs for force assignment, and managing the deployment state transitions.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class MaplessStratCon {
    private static final MMLogger LOGGER = MMLogger.create(MaplessStratCon.class);

    /**
     * Represents the complete context required for deploying forces to a StratCon scenario.
     *
     * <p>This record encapsulates all the state information needed to identify and interact with a specific scenario
     * within the StratCon campaign structure.</p>
     *
     * @param campaignState    the overall state of the StratCon campaign
     * @param trackState       the state of the specific track containing the scenario
     * @param stratConScenario the StratCon scenario being deployed to
     * @param scenarioCoords   the coordinates of the scenario within its track
     *
     * @author Illiani
     * @since 0.50.10
     */
    private record StratConDeploymentContext(StratConCampaignState campaignState, StratConTrackState trackState,
          StratConScenario stratConScenario, StratConCoords scenarioCoords) {}

    /**
     * Initiates a mapless deployment workflow for the specified scenario.
     *
     * <p>This method serves as the main entry point for deploying forces to a StratCon scenario without using the
     * map interface. It validates the scenario, retrieves the necessary context information, and triggers the
     * appropriate assignment dialogs.</p>
     *
     * @param stratConPanel the UI panel managing StratCon operations
     * @param campaign      the current campaign
     * @param scenario      the scenario to deploy forces to
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void deployWithoutMap(StratConPanel stratConPanel, Campaign campaign, Scenario scenario) {
        StratConDeploymentContext deploymentContext = buildScenarioData(campaign, scenario);
        if (deploymentContext == null) {
            return;
        }

        triggerAssignmentDialog(stratConPanel, campaign, deploymentContext);
    }


    /**
     * Builds the deployment context for a given scenario by locating it within the campaign structure.
     *
     * <p>This method validates that the scenario belongs to an AtB contract with an active StratCon campaign state,
     * then searches through all tracks to find the scenario and construct the necessary context information.</p>
     *
     * @param campaign the current campaign
     * @param scenario the scenario to build context for
     *
     * @return a {@link StratConDeploymentContext} containing all necessary scenario information, or {@code null} if the
     *       scenario cannot be located or is invalid
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static @Nullable StratConDeploymentContext buildScenarioData(Campaign campaign, Scenario scenario) {
        Mission mission = campaign.getMission(scenario.getMissionId());
        if (!(mission instanceof AtBContract atbContract)) {
            // We should have obstructed the user before they get to this point
            LOGGER.error("Mission is not an AtBContract: {}", mission);
            return null;
        }

        StratConCampaignState campaignState = atbContract.getStratconCampaignState();
        if (campaignState == null) {
            LOGGER.warn("CampaignState is null for contract: {}", atbContract);
            return null;
        }

        int scenarioId = scenario.getId();

        for (StratConTrackState track : campaignState.getTracks()) {
            for (Map.Entry<StratConCoords, StratConScenario> scenarioInTrack : track.getScenarios().entrySet()) {
                if (scenarioInTrack.getValue().getBackingScenarioID() == scenarioId) {
                    return new StratConDeploymentContext(campaignState, track, scenarioInTrack.getValue(),
                          scenarioInTrack.getKey());
                }
            }
        }

        LOGGER.warn("Unable to find scenario {} in any tracks", scenarioId);
        return null;
    }

    /**
     * Triggers the appropriate force assignment dialog based on the scenario's current state.
     *
     * <p>This method manages the deployment workflow by:</p>
     *
     * <ul>
     *   <li>Setting the active track and selected coordinates in the StratCon panel</li>
     *   <li>Displaying the force assignment UI for unresolved scenarios (primary force deployment)</li>
     *   <li>Displaying the scenario wizard for scenarios with committed primary forces (reinforcement deployment)</li>
     *   <li>Handling cancellation and cleanup of the deployment process</li>
     * </ul>
     *
     * @param stratConPanel     the UI panel managing StratCon operations
     * @param campaign          the current campaign
     * @param deploymentContext the context information for the scenario being deployed to
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void triggerAssignmentDialog(StratConPanel stratConPanel, Campaign campaign,
          StratConDeploymentContext deploymentContext) {
        if (deploymentContext == null) {
            return;
        }

        // We're going to use these values a lot, so we're going to unpack them from the deploymentContext Record
        StratConCampaignState campaignState = deploymentContext.campaignState;
        StratConTrackState trackState = deploymentContext.trackState;
        StratConScenario stratConScenario = deploymentContext.stratConScenario;
        StratConCoords scenarioCoords = deploymentContext.scenarioCoords;

        stratConPanel.setCurrentTrack(deploymentContext.trackState);
        stratConPanel.setSelectedCoords(deploymentContext.scenarioCoords);

        TrackForceAssignmentUI assignmentUI = stratConPanel.getAssignmentUI();
        StratConScenarioWizard scenarioWizard = stratConPanel.getStratConScenarioWizard();

        boolean isPrimaryForce = false;
        StratConScenario.ScenarioState currentState = stratConScenario.getCurrentState();
        AtBDynamicScenario backingScenario = stratConScenario.getBackingScenario();
        boolean restrictToSingleForce = backingScenario != null &&
                                              backingScenario.getStratConScenarioType().isOfficialChallenge();
        if (currentState.equals(UNRESOLVED)) {
            assignmentUI.display(campaign, campaignState, scenarioCoords, restrictToSingleForce);
            assignmentUI.setVisible(true);
            isPrimaryForce = true;
        }

        // Let's reload the scenario in case it updated
        stratConScenario = trackState.getScenario(scenarioCoords);
        if (stratConScenario == null) {
            LOGGER.error("StratConScenario is null for scenarioCoords: {}", scenarioCoords);
            return;
        }


        if (stratConScenario.getCurrentState() == PRIMARY_FORCES_COMMITTED) {
            scenarioWizard.setCurrentScenario(stratConScenario,
                  trackState,
                  campaignState,
                  isPrimaryForce);

            scenarioWizard.toFront();
            scenarioWizard.setVisible(true);
        }

        stratConPanel.setCommitForces(false);
        stratConPanel.repaint();
    }
}
