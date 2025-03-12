/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

public class UnresolvedStratConContactsNagLogic {
    final static String RESOURCE_BUNDLE = "mekhq.resources.GUI";

    /**
     * Checks if there are any unresolved contacts in the current report.
     *
     * <p>
     * This method inspects the {@code unresolvedContactsReport} and determines whether
     * it contains any unresolved contacts. If the report is not empty, it indicates
     * that there are unresolved contacts, and the method returns {@code true};
     * otherwise, it returns {@code false}.
     * </p>
     *
     * @return {@code true} if there are unresolved contacts in the report;
     *         {@code false} otherwise.
     */
    public static boolean hasUnresolvedContacts(Campaign campaign) {
        String unresolvedContactsReport = determineUnresolvedContacts(campaign);
        return !unresolvedContactsReport.isEmpty();
    }

    /**
     * Determines unresolved StratCon contacts for the campaign and generates a report.
     *
     * <p>
     * This method checks all active AtB contracts in the campaign and iterates over their
     * StratCon tracks to find unresolved scenarios. Scenarios are considered unresolved if:
     * <ul>
     *     <li>Their current state is {@link ScenarioState#UNRESOLVED}.</li>
     *     <li>Their deployment date matches the current campaign date.</li>
     * </ul>
     * A formatted report is created, summarizing all unresolved scenarios and marking critical ones.
     */
    public static String determineUnresolvedContacts(Campaign campaign) {
        StringBuilder unresolvedContacts = new StringBuilder();

        // check every track attached to an active contract for unresolved scenarios
        // to which the player can deploy forces
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            if (contract.getStratconCampaignState() == null) {
                continue; // Skip contracts without a Stratcon campaign state
            }

            for (StratconTrackState track : contract.getStratconCampaignState().getTracks()) {
                for (StratconScenario scenario : track.getScenarios().values()) {
                    // Check if the scenario is unresolved and the deployment date matches the local date
                    if (scenario.getCurrentState() == ScenarioState.UNRESOLVED
                          && campaign.getLocalDate().equals(scenario.getDeploymentDate())) {

                        AtBDynamicScenario backingScenario = scenario.getBackingScenario();

                        // Determine if the scenario is special or a turning point
                        boolean isCrisis = backingScenario != null
                              && backingScenario.getStratConScenarioType().isSpecial();
                        boolean isTurningPoint = scenario.isTurningPoint();

                        // Define the addendum text based on StratCon scenario type
                        String addendum;
                        if (isCrisis) {
                            addendum = getTextAt(RESOURCE_BUNDLE, "UnresolvedStratConContactsNagDialog.crisis");
                        } else if (isTurningPoint) {
                            addendum = getTextAt(RESOURCE_BUNDLE, "UnresolvedStratConContactsNagDialog.turningPoint");
                        } else {
                            addendum = ""; // No additional label if neither condition is true
                        }

                        // Append formatted unresolved scenario information
                        unresolvedContacts.append(getFormattedTextAt(
                              RESOURCE_BUNDLE,
                              "UnresolvedStratConContactsNagDialog.report",
                              scenario.getName(),
                              contract.getName(),
                              track.getDisplayableName(),
                              scenario.getCoords().toBTString(),
                              addendum
                        ));
                    }
                }
            }
        }

        return unresolvedContacts.toString();
    }
}
