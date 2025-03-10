/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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

import static mekhq.gui.dialog.nagDialogs.nagLogic.NoCommanderNagLogic.hasNoCommander;

/**
 * A dialog used to notify the user that their campaign lacks a designated commander.
 *
 * <p>
 * This nag dialog is displayed when the campaign does not currently have a commander,
 * and checks whether the user has opted to ignore such notifications in the future.
 * If shown, the user has the option to dismiss the dialog or address the issue.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Handles the "No Commander" notification for campaigns.</li>
 *   <li>Localized message fetched using resource bundles.</li>
 *   <li>Extends the {@link AbstractMHQNagDialog} for reusable dialog behavior.</li>
 * </ul>
 *
 * @see AbstractMHQNagDialog
 */
public class NoCommanderNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs a {@code NoCommanderNagDialog} for a campaign.
     *
     * <p>
     * This dialog uses the localization key {@code "NoCommanderNagDialog.text"}
     * to display a message informing the user about the absence of a commander in their campaign.
     * </p>
     *
     * @param campaign The {@link Campaign} for which the nag dialog is being triggered.
     */
    public NoCommanderNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_NO_COMMANDER);

        final String DIALOG_BODY = "NoCommanderNagDialog.text";
        setRightDescriptionMessage(resources.getString(DIALOG_BODY));
        showDialog();
    }

    /**
     * Checks if a nag dialog should be displayed for the absence of a commander in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If the nag dialog for the absence of a commander has not been ignored in the user options.</li>
     *     <li>If the campaign does not have a commander assigned.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    static public boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_NO_COMMANDER;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasNoCommander(campaign);
    }
}
