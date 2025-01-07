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

import static mekhq.gui.dialog.nagDialogs.nagLogic.PrisonersNagLogic.hasPrisoners;

/**
 * A nag dialog that alerts the user about prisoners of war (POWs) in the campaign.
 *
 * <p>
 * This dialog checks whether there are prisoners of war in the campaign and displays a warning
 * if the user attempts to advance the day without addressing them. The purpose is to ensure
 * the player is aware of the prisoners and can take any necessary actions before proceeding.
 * </p>
 */
public class PrisonersNagDialog extends AbstractMHQNagDialog {
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
        showDialog();
    }

    /**
     * Checks if a nag dialog should be displayed for prisoners in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If the nag dialog for prisoners has not been ignored in the user options.</li>
     *     <li>If the campaign has prisoners.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_PRISONERS;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasPrisoners(campaign);
    }
}
