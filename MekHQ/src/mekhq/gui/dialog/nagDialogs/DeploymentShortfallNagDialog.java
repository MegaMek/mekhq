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
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import static mekhq.gui.dialog.nagDialogs.nagLogic.DeploymentShortfallNagLogic.hasDeploymentShortfall;

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
    private Campaign campaign;

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

        this.campaign = campaign;

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
     *     {@code hasDeploymentShortfall()}.</li>
     * </ul>
     * If all these conditions are satisfied, the dialog is shown to the user.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_SHORT_DEPLOYMENT;

        if (campaign.getCampaignOptions().isUseAtB()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasDeploymentShortfall(campaign)) {
            showDialog();
        }
    }
}
