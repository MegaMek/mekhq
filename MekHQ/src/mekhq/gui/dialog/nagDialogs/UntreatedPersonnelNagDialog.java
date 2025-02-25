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
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UntreatedPersonnelNagLogic.campaignHasUntreatedInjuries;

/**
 * A nag dialog that alerts the user about untreated injuries within the campaign's personnel.
 *
 * <p>
 * This dialog checks for active, injured personnel who have not been assigned to a doctor,
 * excluding those currently classified as prisoners. It provides a reminder to the player, ensuring
 * that injured personnel receive immediate treatment.
 * </p>
 */
public class UntreatedPersonnelNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs the nag dialog for untreated personnel injuries.
     *
     * <p>
     * This constructor initializes the dialog with relevant campaign details
     * and formats the displayed message to include context for the commander.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public UntreatedPersonnelNagDialog(Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNTREATED_PERSONNEL);

        final String DIALOG_BODY = "UntreatedPersonnelNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
        showDialog();
    }

    /**
     * Checks if a nag dialog should be displayed for untreated personnel injuries in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If the nag dialog for untreated personnel injuries has not been ignored in the user options.</li>
     *     <li>If the campaign has untreated injuries among its personnel.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_UNTREATED_PERSONNEL;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && campaignHasUntreatedInjuries(campaign);
    }
}
