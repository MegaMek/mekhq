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
 * A dialog used to notify the user that their campaign has insufficient astechs. Not to be
 * confused with {@link InsufficientAstechTimeNagDialog}.
 *
 * <p>
 * This nag dialog is triggered when the campaign does not have enough astechs to handle
 * the current maintenance and repair workload. Users are notified via a localized
 * message that provides relevant details about the issue.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Notifies users about the shortage of astechs in the current campaign.</li>
 *   <li>Allows users to address the issue or dismiss the dialog while optionally ignoring future warnings.</li>
 *   <li>Extends {@link AbstractMHQNagDialog} for consistent functionality with other nag dialogs.</li>
 * </ul>
 */
public class InsufficientAstechsNagDialog extends AbstractMHQNagDialog {
    private int asTechsNeeded = 0;

    /**
     * Verifies if the campaign has an unmet astech requirement.
     *
     * <p>
     * This method checks whether the campaign has any pending maintenance or
     * repair tasks that require astechs. It evaluates the current astech
     * need in the campaign and determines if it exceeds zero.
     * </p>
     *
     * @param campaign The {@link Campaign} to check for astech needs.
     */
    void checkAsTechsNeededCount(Campaign campaign) {
        asTechsNeeded = campaign.getAstechNeed();
    }

    /**
     * Checks if there is a need for AsTechs in the campaign.
     *
     * <p>
     * This method determines whether the number of required AsTechs
     * is greater than zero. If {@code asTechsNeeded} is greater than zero,
     * it indicates that there is a need for AsTechs to meet the campaign's requirements.
     * </p>
     *
     * @return {@code true} if the number of required AsTechs ({@code asTechsNeeded}) is greater than zero;
     *         {@code false} otherwise.
     */
    boolean hasAsTechsNeeded() {
        return asTechsNeeded > 0;
    }

    /**
     * Constructs an {@code InsufficientAstechsNagDialog} for the given campaign.
     *
     * <p>
     * This dialog uses a localized message identified by the key
     * {@code "InsufficientAstechsNagDialog.text"} to inform the user of the insufficient
     * astechs in their campaign.
     * </p>
     *
     * @param campaign The {@link Campaign} associated with this nag dialog.
     */
    public InsufficientAstechsNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_INSUFFICIENT_ASTECHS);

        checkAsTechsNeededCount(campaign);

        String pluralizer = (asTechsNeeded > 1) ? "s" : "";

        final String DIALOG_BODY = "InsufficientAstechsNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), asTechsNeeded, pluralizer));
    }

    /**
     * Evaluates whether the "Insufficient Astechs" nag dialog should be displayed.
     *
     * <p>
     * This method checks the following conditions:
     * <ul>
     *   <li>If the "Insufficient Astechs" nag dialog has not been flagged as ignored
     *       in the user options.</li>
     *   <li>If the campaign has an unmet asTech requirement as determined by
     *       {@link #checkAsTechsNeededCount(Campaign)}.</li>
     * </ul>
     * If both conditions are met, the dialog is displayed.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_INSUFFICIENT_ASTECHS;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasAsTechsNeeded()) {
            showDialog();
        }
    }
}
