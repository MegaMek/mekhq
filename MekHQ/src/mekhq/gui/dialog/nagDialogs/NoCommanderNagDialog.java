/*
 * Copyright (c) 2021-2024 The MegaMek Team. All Rights Reserved.
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
    private final Campaign campaign;

    /**
     * Checks if the campaign has no assigned commander.
     *
     * <p>
     * This method determines whether the campaign has a flagged commander assigned or not.
     * If {@code campaign.getFlaggedCommander()} returns {@code null}, it indicates
     * that no commander has been assigned.
     * </p>
     *
     * @return {@code true} if the campaign has no flagged commander
     * ({@code campaign.getFlaggedCommander()} is {@code null}); {@code false} otherwise.
     */
    boolean hasNoCommander() {
        return campaign.getFlaggedCommander() == null;
    }

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

        this.campaign = campaign;

        final String DIALOG_BODY = "NoCommanderNagDialog.text";
        setRightDescriptionMessage(resources.getString(DIALOG_BODY));
    }

    /**
     * Checks whether the "No Commander" nag dialog should be displayed.
     *
     * <p>
     * This method evaluates the following conditions:
     * <ul>
     *   <li>If the "No Commander" nag dialog is not flagged as ignored in the user settings.</li>
     *   <li>If the campaign currently does not have a flagged commander.</li>
     * </ul>
     * If both conditions are true, the dialog is displayed.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_NO_COMMANDER;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && hasNoCommander()) {
            showDialog();
        }
    }
}
