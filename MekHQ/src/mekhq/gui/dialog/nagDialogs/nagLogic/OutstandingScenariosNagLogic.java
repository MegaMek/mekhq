/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;

import java.time.LocalDate;
import java.util.List;

import static mekhq.campaign.stratcon.StratconCampaignState.getStratconScenarioFromAtBScenario;
import static mekhq.campaign.stratcon.StratconScenario.ScenarioState.UNRESOLVED;

public class OutstandingScenariosNagLogic {
    /**
     * Checks if there are any outstanding scenarios in the campaign.
     *
     * <p>
     * This method evaluates whether the {@code outstandingScenarios} string is blank or not.
     * If the string is not blank, it indicates that there are outstanding scenarios
     * that need to be addressed.
     * </p>
     *
     * @return {@code true} if {@code outstandingScenarios} is not blank, indicating there are
     * outstanding scenarios; {@code false} otherwise.
     */
    public static boolean hasOutStandingScenarios(Campaign campaign) {
        String outstandingScenarios = getOutstandingScenarios(campaign);
        return !outstandingScenarios.isBlank();
    }

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
     */
    public static String getOutstandingScenarios(Campaign campaign) {
        List<AtBContract> activeContracts = campaign.getActiveAtBContracts(true);
        LocalDate today = campaign.getLocalDate();
        StringBuilder activeScenarios = new StringBuilder();

        for (AtBContract contract : activeContracts) {
            for (AtBScenario scenario : contract.getCurrentAtBScenarios()) {
                LocalDate scenarioDate = scenario.getDate();

                if (scenario.getDate() == null) {
                    continue;
                }

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

                            if (stratconScenario.isTurningPoint()) {
                                activeScenarios.append(" (Turning Point)");
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

        return activeScenarios.toString();
    }
}
