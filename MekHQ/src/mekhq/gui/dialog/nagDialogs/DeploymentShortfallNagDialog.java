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
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import java.time.DayOfWeek;

/**
 * A nag dialog that alerts the user if short deployments are detected in the campaign's active contracts.
 *
 * <p>
 * This dialog checks whether any active AtB (Against the Bot) contracts have a deployment deficit
 * and alerts the player to address the issue. The check is performed weekly (on Sundays) and only
 * when the campaign is currently located on a planet. If deployment requirements are not met,
 * the dialog is displayed to prompt the user to correct the situation.
 * </p>
 */
public class DeploymentShortfallNagDialog extends AbstractMHQNagDialog {
    /**
     * Checks if the campaign's active contracts have deployment deficits that need to be addressed.
     *
     * <p>
     * The following conditions are evaluated to determine whether the requirements for short
     * deployments are met:
     * <ul>
     *     <li>The campaign must currently be located on a planet. If it is not, the dialog is skipped.</li>
     *     <li>The check is performed weekly, only on Sundays, to avoid spamming the user daily.</li>
     *     <li>If any active AtB contract has a deployment deficit, the method returns {@code true}.</li>
     * </ul>
     * If none of these conditions are met, the method returns {@code false}.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     * @return {@code true} if there are unmet deployment requirements; otherwise, {@code false}.
     */
    static boolean checkDeploymentRequirementsMet(Campaign campaign) {
        if (!campaign.getLocation().isOnPlanet()) {
            return false;
        }

        // this prevents the nag from spamming daily
        if (campaign.getLocalDate().getDayOfWeek() != DayOfWeek.SUNDAY) {
            return false;
        }

        // There is no need to use a stream here, as the number of iterations doesn't warrant it.
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            if (campaign.getDeploymentDeficit(contract) > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Constructs the shortfall deployment nag dialog for a given campaign.
     *
     * <p>
     * This constructor initializes the dialog with the specified campaign and
     * formats the resource message to display information about deployment shortfalls.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public DeploymentShortfallNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_SHORT_DEPLOYMENT);

        final String DIALOG_BODY = "DeploymentShortfallNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    /**
     * Determines whether the nag dialog should be displayed, based on deployment requirements
     * in the campaign.
     *
     * <p>
     * The dialog is displayed if:
     * <ul>
     *     <li>AtB campaigns are enabled in the campaign options.</li>
     *     <li>The nag dialog for short deployments is not ignored in MekHQ options.</li>
     *     <li>There are unmet deployment requirements, as determined by
     *     {@link #checkDeploymentRequirementsMet(Campaign)}.</li>
     * </ul>
     * If all these conditions are satisfied, the dialog is shown to the user.
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public void checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_SHORT_DEPLOYMENT;

        if (campaign.getCampaignOptions().isUseAtB()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (checkDeploymentRequirementsMet(campaign))) {
            showDialog();
        }
    }
}
