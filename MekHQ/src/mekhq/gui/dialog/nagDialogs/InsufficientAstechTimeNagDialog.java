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

import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientAstechTimeNagLogic.getAsTechTimeDeficit;
import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientAstechTimeNagLogic.hasAsTechTimeDeficit;

/**
 * A dialog used to notify the user about insufficient available time for astechs to complete the
 * required maintenance tasks. Not to be confused with {@link InsufficientAstechsNagDialog}.
 *
 * <p>
 * This nag dialog is triggered when the available work time for the astech pool is inadequate to meet
 * the maintenance time requirements for the current campaign's hangar units. It provides a localized
 * message detailing the time deficit and allows the user to take necessary action or dismiss the dialog.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Calculates the time deficit for the astech pool based on hangar unit maintenance requirements.</li>
 *   <li>Notifies the user when there is inadequate time available to maintain all units.</li>
 *   <li>Extends {@link AbstractMHQNagDialog} for consistent nag dialog behavior.</li>
 * </ul>
 */
public class InsufficientAstechTimeNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs an {@code InsufficientAstechTimeNagDialog} for the given campaign.
     *
     * <p>
     * This dialog calculates the astech time deficit and uses a localized message
     * to notify the user about the shortage of available time. The message provides
     * the commander's address, the time deficit, and a pluralized suffix for correctness.
     * </p>
     *
     * @param campaign The {@link Campaign} tied to this nag dialog.
     *                 The campaign provides data about hangar units and astech availability.
     */
    public InsufficientAstechTimeNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME);

        int asTechsTimeDeficit = getAsTechTimeDeficit(campaign);

        String pluralizer = (asTechsTimeDeficit > 1) ? "s" : "";

        final String DIALOG_BODY = "InsufficientAstechTimeNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), asTechsTimeDeficit, pluralizer));
        showDialog();
    }

    /**
     * Checks if a nag dialog should be displayed for insufficient AsTech time in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If the nag dialog for insufficient AsTech time has not been ignored in the user options.</li>
     *     <li>If there is a deficit in the available AsTech time for the given campaign.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasAsTechTimeDeficit(campaign);
    }
}
