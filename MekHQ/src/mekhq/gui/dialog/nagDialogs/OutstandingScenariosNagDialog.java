/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.baseComponents.AbstractMHQNagDialog_NEW;

import java.time.LocalDate;
import java.util.List;

import static mekhq.campaign.stratcon.StratconCampaignState.getStratconScenarioFromAtBScenario;
import static mekhq.campaign.stratcon.StratconScenario.ScenarioState.UNRESOLVED;

/**
 * Represents a nag dialog for displaying the list of outstanding scenarios in a campaign.
 *
 * <p>
 * This dialog checks for active scenarios within the campaign, categorizes them by
 * their state (e.g., unresolved or requiring a track), and displays a list of these
 * scenarios to the user. Scenarios are considered "outstanding" if they are unresolved or
 * require attention on the current campaign date.
 * </p>
 *
 * <p>
 * The dialog includes logic to account for both AtB contracts and StratCon-enabled campaigns,
 * formatting the outstanding scenarios with additional details when appropriate.
 * </p>
 */
public class OutstandingScenariosNagDialog extends AbstractMHQNagDialog_NEW {
    String outstandingScenarios = "";

    /**
     * Retrieves and processes the list of outstanding scenarios for the current campaign.
     *
     * <p>
     * This method iterates through all active contracts and their associated AtB scenarios,
     * identifying scenarios that are outstanding based on the following conditions:
     * <ul>
     *     <li>Whether the scenario's date matches the current campaign date.</li>
     *     <li>If the scenario is part of StratCon and is unresolved or critical.</li>
     *     <li>If it's associated with a track and includes detailed information about that track.</li>
     * </ul>
     * Scenarios are categorized into "critical" scenarios (e.g., required StratCon scenarios)
     * and others, with additional formatting for StratCon-specific scenarios where applicable.
     * </p>
     *
     * @param campaign The {@link Campaign} from which to retrieve outstanding scenarios.
     */
    private void getOutstandingScenarios(Campaign campaign) {
        List<AtBContract> activeContracts = campaign.getActiveAtBContracts(true);
        LocalDate today = campaign.getLocalDate();
        StringBuilder activeScenarios = new StringBuilder();

        for (AtBContract contract : activeContracts) {
            for (AtBScenario scenario : contract.getCurrentAtBScenarios()) {
                LocalDate scenarioDate = scenario.getDate();

                // Skip scenarios not matching today's date
                if (!scenarioDate.equals(today)) {
                    continue;
                }

                if (scenario.getHasTrack()) {
                    StratconScenario stratconScenario = getStratconScenarioFromAtBScenario(campaign, scenario);

                    if (stratconScenario != null) {
                        // Skip if the scenario is unresolved
                        if (stratconScenario.getCurrentState() == UNRESOLVED) {
                            continue;
                        }

                        StratconTrackState track = stratconScenario.getTrackForScenario(campaign, null);

                        if (track != null) {
                            activeScenarios.append("<br>- ")
                                .append(scenario.getName())
                                .append(", ").append(contract.getName())
                                .append(", ").append(track.getDisplayableName())
                                .append('-').append(stratconScenario.getCoords().toBTString());

                            if (stratconScenario.isRequiredScenario()) {
                                activeScenarios.append(" (Critical)");
                            }

                            continue;
                        }
                    }
                }

                // Add non-track scenarios
                activeScenarios.append("<br>- ")
                    .append(scenario.getName())
                    .append(", ").append(contract.getName());
            }
        }

        if (campaign.getCampaignOptions().isUseStratCon()) {
            activeScenarios.append(resources.getString("OutstandingScenariosNagDialog.stratCon"));
        }

        outstandingScenarios = activeScenarios.toString();
    }

    /**
     * Constructs the OutstandingScenariosNagDialog for the given campaign.
     *
     * <p>
     * Upon initialization, this dialog prepares a formatted string of outstanding
     * scenarios (if any) and sets up the dialog UI for display.
     * </p>
     *
     * @param campaign The {@link Campaign} associated with this nag dialog.
     */
    public OutstandingScenariosNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_OUTSTANDING_SCENARIOS);

        final String DIALOG_BODY = "OutstandingScenariosNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), outstandingScenarios));
    }

    /**
     * Checks if the nag dialog should be displayed, based on the current campaign state.
     *
     * <p>
     * The dialog is displayed if the following conditions are met:
     * <ul>
     *     <li>AtB campaigns are enabled in the campaign options.</li>
     *     <li>The nag dialog for outstanding scenarios is not ignored in MekHQ options.</li>
     *     <li>Outstanding scenarios exist in the campaign.</li>
     * </ul>
     * If all these conditions are satisfied, the nag dialog is displayed to the user.
     * </p>
     *
     * @param campaign The {@link Campaign} to check for outstanding scenarios.
     */
    public void checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_OUTSTANDING_SCENARIOS;

        getOutstandingScenarios(campaign);

        if (campaign.getCampaignOptions().isUseAtB()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && !outstandingScenarios.isBlank()) {
            showDialog();
        }
    }
}
