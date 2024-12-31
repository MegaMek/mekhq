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

import static mekhq.gui.dialog.nagDialogs.nagLogic.InvalidFactionNagLogic.isFactionInvalid;

/**
 * A dialog used to notify the user about an invalid faction in the current campaign.
 *
 * <p>
 * This nag dialog is triggered when the campaign's selected faction is determined to be invalid
 * for the current campaign date. It evaluates the validity of the faction based on the campaign
 * date and displays a localized message warning the user about the issue.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Checks whether the campaign's faction is valid based on the current in-game date.</li>
 *   <li>Displays a warning dialog to alert the user when an invalid faction is detected.</li>
 *   <li>Extends {@link AbstractMHQNagDialog} to ensure consistent behavior with other nag dialogs.</li>
 * </ul>
 */
public class InvalidFactionNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs an {@code InvalidFactionNagDialog} for the given campaign.
     *
     * <p>
     * This dialog initializes with the campaign information and sets a localized
     * message to notify the user about the potential issue involving an invalid faction.
     * The message includes the commander's address for better clarity.
     * </p>
     *
     * @param campaign The {@link Campaign} associated with this nag dialog.
     *                 The campaign provides the faction and other details for evaluation.
     */
    public InvalidFactionNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_INVALID_FACTION);

        final String DIALOG_BODY = "InvalidFactionNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
        showDialog();
    }

    /**
     * Checks if a nag dialog should be displayed for an invalid faction in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If the nag dialog for an invalid faction has not been ignored in the user options.</li>
     *     <li>If the faction associated with the campaign is considered invalid.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_INVALID_FACTION;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (isFactionInvalid(campaign));
    }
}
