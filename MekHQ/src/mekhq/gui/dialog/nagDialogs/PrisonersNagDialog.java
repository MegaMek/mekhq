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
import mekhq.gui.baseComponents.AbstractMHQNagDialog_NEW;

/**
 * A nag dialog that alerts the user about prisoners of war (POWs) in the campaign.
 *
 * <p>
 * This dialog checks whether there are prisoners of war in the campaign and displays a warning
 * if the user attempts to advance the day without addressing them. The purpose is to ensure
 * the player is aware of the prisoners and can take any necessary actions before proceeding.
 * </p>
 */
public class PrisonersNagDialog extends AbstractMHQNagDialog_NEW {
    /**
     * Checks if the current campaign has prisoners of war (POWs).
     *
     * <p>
     * This method evaluates the state of the campaign to determine if there are prisoners present.
     * If the campaign does not have an active contract, the method checks the campaign's list of
     * current prisoners. If the list is not empty, the method returns {@code true}.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     * @return {@code true} if there are prisoners in the campaign; otherwise, {@code false}.
     */
    static boolean hasPrisoners (Campaign campaign) {
        if (!campaign.hasActiveContract()) {
            return !campaign.getCurrentPrisoners().isEmpty();
        }

        return false;
    }

    /**
     * Constructs the prisoners nag dialog for the given campaign.
     *
     * <p>
     * This constructor initializes the dialog with the specified campaign and
     * formats the resource message to display information about prisoners in the campaign.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public PrisonersNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_PRISONERS);

        final String DIALOG_BODY = "PrisonersNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    /**
     * Determines whether the prisoners nag dialog should be displayed to the user.
     *
     * <p>
     * The dialog will be shown if the following conditions are met:
     * <ul>
     *     <li>AtB campaigns are enabled in the campaign options.</li>
     *     <li>The nag dialog for prisoners is not ignored in MekHQ options.</li>
     *     <li>The campaign contains prisoners, as determined by {@link #hasPrisoners(Campaign)}.</li>
     * </ul>
     * If all these conditions are satisfied, the dialog is displayed to notify the user about the prisoners.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public void checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_PRISONERS;

        if (campaign.getCampaignOptions().isUseAtB()
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (hasPrisoners(campaign))) {
            showDialog();
        }
    }
}
