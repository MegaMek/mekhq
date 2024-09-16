/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import javax.swing.*;
import java.time.DayOfWeek;

/**
 * This class represents a nag dialog displayed when the campaign does not meet the deployment
 * levels required by their active {@link AtBContract}
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class ShortDeploymentNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "ShortDeploymentNagDialog";
    private static String DIALOG_TITLE = "ShortDeploymentNagDialog.title";
    private static String DIALOG_BODY = "ShortDeploymentNagDialog.text";

    /**
     * Checks if the deployment requirements are met for a given campaign.
     *
     * @param campaign the campaign to check the deployment requirements for
     * @return {@code true} if the deployment requirements are met, {@code false} otherwise
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

    //region Constructors
    /**
     * Creates a new instance of the {@link ShortDeploymentNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public ShortDeploymentNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_SHORT_DEPLOYMENT);
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
                && checkDeploymentRequirementsMet(getCampaign());
    }
}
