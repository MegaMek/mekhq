/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.gui.dialog.nagDialogs;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientAstechsNagLogic.hasAsTechsNeeded;

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
        int asTechsNeeded = campaign.getAstechNeed();

        String pluralizer = (asTechsNeeded > 1) ? "s" : "";

        final String DIALOG_BODY = "InsufficientAstechsNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false), asTechsNeeded, pluralizer));
        showDialog();
    }

    /**
     * Determines whether a nag dialog should be displayed for insufficient AsTechs in the campaign.
     *
     * <p>This method evaluates the following conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for insufficient AsTechs in their options.</li>
     *     <li>The campaign requires additional AsTechs ({@code asTechsNeeded} is greater than zero).</li>
     * </ul>
     *
     * @param asTechsNeeded The number of additional AsTechs required to meet the campaign's needs.
     * @return {@code true} if the nag dialog should be displayed due to insufficient AsTechs,
     *         {@code false} otherwise.
     */
    public static boolean checkNag(int asTechsNeeded) {
        final String NAG_KEY = MHQConstants.NAG_INSUFFICIENT_ASTECHS;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
              && hasAsTechsNeeded(asTechsNeeded);
    }
}
