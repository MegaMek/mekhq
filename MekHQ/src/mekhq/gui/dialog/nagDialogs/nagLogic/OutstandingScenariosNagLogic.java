/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import static mekhq.campaign.stratCon.StratConCampaignState.getStratConScenarioFromAtBScenario;
import static mekhq.campaign.stratCon.StratConScenario.ScenarioState.UNRESOLVED;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.stratCon.StratConScenario;
import mekhq.campaign.stratCon.StratConTrackState;

public class OutstandingScenariosNagLogic {
    final static String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    /**
     * Checks if there are any outstanding scenarios in the campaign.
     *
     * <p>
     * This method evaluates whether the {@code outstandingScenarios} string is blank or not. If the string is not
     * blank, it indicates that there are outstanding scenarios that need to be addressed.
     * </p>
     *
     * @return {@code true} if {@code outstandingScenarios} is not blank, indicating there are outstanding scenarios;
     *       {@code false} otherwise.
     */
    public static boolean hasOutStandingScenarios(Campaign campaign) {
        String outstandingScenarios = getOutstandingScenarios(campaign);
        return !outstandingScenarios.isBlank();
    }

    /**
     * Retrieves and processes the list of outstanding scenarios for the current campaign.
     *
     * <p>
     * This method iterates through all active contracts and their associated AtB scenarios, identifying scenarios that
     * are outstanding based on the following conditions:
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
                    StratConScenario stratconScenario = getStratConScenarioFromAtBScenario(campaign, scenario);

                    if (stratconScenario != null) {
                        // Skip if the scenario is unresolved
                        if (stratconScenario.getCurrentState() == UNRESOLVED) {
                            continue;
                        }

                        AtBDynamicScenario backingScenario = stratconScenario.getBackingScenario();

                        // Determine if the scenario is special or a turning point
                        boolean isCrisis = backingScenario != null &&
                                                 (backingScenario.getStratConScenarioType().isSpecial() ||
                                                        backingScenario.isCrisis());
                        boolean isTurningPoint = stratconScenario.isTurningPoint();

                        // Define the addendum text based on StratCon scenario type
                        String addendum;
                        if (isCrisis) {
                            addendum = getTextAt(RESOURCE_BUNDLE, "UnresolvedStratConContactsNagDialog.crisis");
                        } else if (isTurningPoint) {
                            addendum = getTextAt(RESOURCE_BUNDLE, "UnresolvedStratConContactsNagDialog.turningPoint");
                        } else {
                            addendum = ""; // No additional label if neither condition is true
                        }

                        StratConTrackState track = stratconScenario.getTrackForScenario(campaign, null);

                        // Append formatted unresolved scenario information
                        activeScenarios.append(getFormattedTextAt(RESOURCE_BUNDLE,
                              "UnresolvedStratConContactsNagDialog.report",
                              scenario.getHyperlinkedName(),
                              contract.getHyperlinkedName(),
                              track == null ? "" : track.getDisplayableName(),
                              stratconScenario.getCoords().toBTString(),
                              addendum));
                    }
                } else {
                    // Add non-track scenarios
                    activeScenarios.append("<p>- ")
                          .append("<b>")
                          .append(scenario.getHyperlinkedName())
                          .append("</b>, ")
                          .append(contract.getHyperlinkedName());
                }
            }
        }

        return activeScenarios.toString();
    }
}
