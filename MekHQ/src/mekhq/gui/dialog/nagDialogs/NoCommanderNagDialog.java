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

import javax.swing.*;

/**
 * This class represents a nag dialog displayed when the campaign has no assigned commander
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class NoCommanderNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "NoCommanderNagDialog";
    private static String DIALOG_TITLE = "NoCommanderNagDialog.title";
    private static String DIALOG_BODY = "NoCommanderNagDialog.text";


    /**
     * Checks if the given {@link Campaign} is missing a commander.
     *
     * @param campaign the campaign to check for a missing commander
     * @return {@code true} if the campaign is missing a commander, otherwise {@code false}
     */
    static boolean isCommanderMissing (Campaign campaign) {
        return (campaign.getFlaggedCommander() == null);
    }

    /**
     * Creates a new instance of the {@link EndContractNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public NoCommanderNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_NO_COMMANDER);
        pack();
    }

    /**
     * Checks if there is a nag message to display.
     *
     * @return {@code true} if there is a nag message to display, {@code false} otherwise
     */
    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && isCommanderMissing(getCampaign());
    }
}
