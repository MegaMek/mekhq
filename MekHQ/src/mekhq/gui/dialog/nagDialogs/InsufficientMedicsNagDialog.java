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

/**
 * A dialog used to notify the user about insufficient medics required to meet the medical needs of the campaign.
 *
 * <p>
 * This nag dialog is triggered when the count of available medics in the campaign falls short of
 * the total number required for handling the current medical workload. It displays a localized
 * message for the user with specifics about the deficit, and optionally allows the user to dismiss
 * or ignore future warnings.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Calculates the number of medics required for a campaign using {@link Campaign#getMedicsNeed()}.</li>
 *   <li>Displays a dialog to warn the user if the required number of medics exceeds the available count.</li>
 *   <li>Extends {@link AbstractMHQNagDialog} to provide consistent behavior with other nag dialogs.</li>
 * </ul>
 */
public class InsufficientMedicsNagDialog extends AbstractMHQNagDialog {
    private int medicsRequired = 0;

    /**
     * Calculates the number of medics required for the campaign.
     *
     * <p>
     * This method retrieves the number of medics currently needed in the campaign by
     * calling {@link Campaign#getMedicsNeed()}. The result is assigned to
     * {@link #medicsRequired}, representing the deficit in the number of medics.
     * </p>
     *
     * @param campaign The {@link Campaign} from which to retrieve the required medic count.
     */
    private void checkMedicsNeededCount(Campaign campaign) {
        medicsRequired =  campaign.getMedicsNeed();
    }

    /**
     * Constructs an {@code InsufficientMedicsNagDialog} for the given campaign.
     *
     * <p>
     * This dialog calculates the number of medics required and uses a localized
     * message to notify the user about the shortage. The message includes the
     * commander's address, the medic deficit, and a pluralized suffix based on the deficit count.
     * </p>
     *
     * @param campaign The {@link Campaign} associated with this nag dialog.
     *                 The campaign provides the medical requirements for the calculation.
     */
    public InsufficientMedicsNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_INSUFFICIENT_MEDICS);

        checkMedicsNeededCount(campaign);

        String pluralizer = (medicsRequired > 1) ? "s" : "";

        final String DIALOG_BODY = "InsufficientMedicsNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), medicsRequired, pluralizer));
    }

    /**
     * Determines whether the "Insufficient Medics" nag dialog should be displayed.
     *
     * <p>
     * This method checks the following conditions:
     * <ul>
     *   <li>If the "Insufficient Medics" nag dialog is flagged as ignored in the user settings.</li>
     *   <li>If the campaign currently requires more medics than are available, as determined by
     *   {@link #checkMedicsNeededCount(Campaign)}.</li>
     * </ul>
     * If both conditions are met, the dialog is displayed to notify the user about
     * the medic shortage.
     * </p>
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_INSUFFICIENT_MEDICS;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (medicsRequired > 0)) {
            showDialog();
        }
    }
}
