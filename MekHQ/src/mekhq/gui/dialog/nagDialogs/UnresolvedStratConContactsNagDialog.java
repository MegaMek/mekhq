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

/**
 * A nag dialog that warns the user about unresolved StratCon contacts within the campaign.
 *
 * <p>
 * This dialog identifies unresolved scenarios in StratCon tracks attached to active contracts
 * where the player can deploy forces. It provides a detailed report of unresolved contacts to
 * notify the player of critical actions required before advancing the campaign.
 * </p>
 */
public class UnresolvedStratConContactsNagDialog extends AbstractMHQNagDialog {
    private final Campaign campaign;

    private String unresolvedContactsReport = "";

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
    boolean hasUnresolvedContacts() {
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
    void determineUnresolvedContacts() {
        StringBuilder unresolvedContacts = new StringBuilder();

        // check every track attached to an active contract for unresolved scenarios
        // to which the player can deploy forces
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            if (contract.getStratconCampaignState() == null) {
                continue;
            }

            for (StratconTrackState track : contract.getStratconCampaignState().getTracks()) {
                for (StratconScenario scenario : track.getScenarios().values()) {
                    if ((scenario.getCurrentState() == ScenarioState.UNRESOLVED)
                        && (campaign.getLocalDate().equals(scenario.getDeploymentDate()))) {
                        unresolvedContacts.append(String.format("<br><b>- %s</b>, %s, %s-%s %s",
                            scenario.getName(), contract.getName(),
                            track.getDisplayableName(), scenario.getCoords().toBTString(),
                            scenario.isRequiredScenario() ? " (Critical)" : ""));
                    }
                }
            }
        }

        unresolvedContactsReport = unresolvedContacts.toString();
    }

    /**
     * Constructs the nag dialog for unresolved StratCon contacts.
     *
     * <p>
     * The dialog is initialized with information about unresolved StratCon scenarios and the
     * campaign's current state. The dynamic message is formatted to include the name or title
     * of the commander, providing context for the player.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public UnresolvedStratConContactsNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNRESOLVED_STRATCON_CONTACTS);

        this.campaign = campaign;

        String addendum = "";
        if (unresolvedContactsReport.isEmpty()) {
            addendum = resources.getString("UnresolvedStratConContactsNagDialog.stratcon");
        }

        final String DIALOG_BODY = "UnresolvedStratConContactsNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), addendum));
    }

    /**
     * Determines whether the unresolved StratCon contacts nag dialog should be displayed.
     *
     * <p>
     * The dialog is displayed if:
     * <ul>
     *     <li>StratCon is enabled in the campaign options.</li>
     *     <li>The nag dialog for unresolved StratCon contacts is not ignored in MekHQ options.</li>
     *     <li>There are unresolved StratCon contacts, as determined by
     *     {@link #determineUnresolvedContacts()}.</li>
     * </ul>
     * The dialog warns the player about unresolved scenarios requiring attention before
     * advancing the campaign.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_UNRESOLVED_STRATCON_CONTACTS;

        determineUnresolvedContacts();

        if (campaign.getCampaignOptions().isUseStratCon()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasUnresolvedContacts()) {
            showDialog();
        }
    }
}
