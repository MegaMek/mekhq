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

import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientMedicsNagLogic.hasMedicsNeeded;

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
    private final Campaign campaign;

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

        this.campaign = campaign;

        int medicsRequired =  campaign.getMedicsNeed();

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
     *   {@link #checkMedicsNeededCount()}.</li>
     * </ul>
     * If both conditions are met, the dialog is displayed to notify the user about
     * the medic shortage.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_INSUFFICIENT_MEDICS;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasMedicsNeeded(campaign)) {
            showDialog();
        }
    }
}
