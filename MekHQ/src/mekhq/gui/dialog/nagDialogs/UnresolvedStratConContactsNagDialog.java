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
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;

/**
 * This class represents a nag dialog displayed when the campaign has outstanding StratCon contacts
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class UnresolvedStratConContactsNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "UnresolvedStratConContactsNagDialog";
    private static String DIALOG_TITLE = "UnresolvedStratConContactsNagDialog.title";
    private static String DIALOG_BODY = "UnresolvedStratConContactsNagDialog.text";

    /**
     * Checks if the given campaign has unresolved contact nags.
     *
     * @param campaign the campaign to check for unresolved contacts
     * @return a string indicating whether the campaign has unresolved contacts or not
     */
    boolean hasUnresolvedContacts(Campaign campaign) {
        String unresolvedContacts = nagUnresolvedContacts(campaign);

        if (unresolvedContacts.isEmpty()) {
            return false;
        } else {
            setDescription(String.format(resources.getString(DIALOG_BODY), unresolvedContacts));
            return true;
        }
    }

    /**
     * Determine whether the user should be nagged about unresolved scenarios on AtB
     * StratCon tracks.
     *
     * @param campaign Campaign to check.
     * @return An informative string containing the reasons the user was nagged.
     */
    static String nagUnresolvedContacts(Campaign campaign) {
        if (!campaign.getCampaignOptions().isUseStratCon()) {
            return "";
        }

        StringBuilder unresolvedContacts = new StringBuilder();

        // check every track attached to an active contract for unresolved scenarios
        // to which the player must deploy forces today
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            if (contract.getStratconCampaignState() == null) {
                continue;
            }

            for (StratconTrackState track : contract.getStratconCampaignState().getTracks()) {
                // "scenario name, track name"
                for (StratconScenario scenario : track.getScenarios().values()) {
                    if ((scenario.getCurrentState() == ScenarioState.UNRESOLVED)
                            && (campaign.getLocalDate().equals(scenario.getDeploymentDate()))) {
                        String resolvedScenario = String.format("%s, %s-%s %s\n",
                            scenario.getName(),
                            track.getDisplayableName(),
                            scenario.getCoords().toBTString(),
                            scenario.isRequiredScenario() ? " (Critical)" : ""
                        );

                        unresolvedContacts.append(resolvedScenario);
                    }
                }
            }
        }

        return unresolvedContacts.toString();
    }

    //region Constructors
    /**
     * Creates a new instance of the {@link UnresolvedStratConContactsNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public UnresolvedStratConContactsNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_UNRESOLVED_STRATCON_CONTACTS);
        pack();
    }
    //endregion Constructors

    /**
     * Checks if there is a nag message to display.
     *
     * @return {@code true} if there is a nag message to display, {@code false} otherwise
     */
    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && hasUnresolvedContacts(getCampaign());
    }
}
