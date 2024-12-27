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
    private final Campaign campaign;

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

        this.campaign = campaign;

        int asTechsTimeDeficit = getAsTechTimeDeficit(campaign);

        String pluralizer = (asTechsTimeDeficit > 1) ? "s" : "";

        final String DIALOG_BODY = "InsufficientAstechTimeNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), asTechsTimeDeficit, pluralizer));
    }

    /**
     * Determines whether the "Insufficient Astech Time" nag dialog should be displayed.
     *
     * <p>
     * This method checks the following conditions:
     * <ul>
     *   <li>If the "Insufficient Astech Time" nag dialog is flagged as ignored in the user settings.</li>
     *   <li>If there is a time deficit in the astech pool based on the campaign's
     *   maintenance requirements.</li>
     * </ul>
     * If both conditions are met, the dialog is displayed to alert the user about
     * insufficient available time for astechs.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasAsTechTimeDeficit(campaign)) {
            showDialog();
        }
    }
}
