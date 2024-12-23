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

import javax.swing.*;

/**
 * This class represents a nag dialog displayed when a campaign's Medic deficit is greater than 0.
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class InsufficientMedicsNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "InsufficientMedicsNagDialog";
    private static String DIALOG_TITLE = "InsufficientMedicsNagDialog.title";
    private static String DIALOG_BODY = "InsufficientMedicsNagDialog.text";

    /**
     * Checks if the count of Medics needed is greater than zero.
     * If so, it sets the description using the specified format and returns {@code true}.
     * Otherwise, it returns {@code false}.
     */
    static boolean checkMedicsNeededCount(Campaign campaign) {
        return campaign.getMedicsNeed() > 0;
    }

    //region Constructors
    /**
     * Creates a new instance of the {@link InsufficientAstechsNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public InsufficientMedicsNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_INSUFFICIENT_MEDICS);
        pack();
    }
    //endregion Constructors

    /**
     * Checks if the count of Medics needed is greater than zero.
     * If the count is greater than zero and the Nag dialog for the current key is not ignored,
     * it sets the description using the specified format and returns {@code true}.
     * Otherwise, it returns {@code false}.
     */
    @Override
    protected boolean checkNag() {
        if (!MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && checkMedicsNeededCount(getCampaign())) {
            setDescription(String.format(
                    resources.getString(DIALOG_BODY),
                    getCampaign().getAstechNeed()));
            return true;
        }

        return false;
    }
}
