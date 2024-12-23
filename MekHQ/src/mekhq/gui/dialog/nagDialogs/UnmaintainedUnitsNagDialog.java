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
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;

/**
 * A dialog that displays a nag message if there are unmaintained units in the campaign's hangar.
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class UnmaintainedUnitsNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "UnmaintainedUnitsNagDialog";
    private static String DIALOG_TITLE = "UnmaintainedUnitsNagDialog.title";
    private static String DIALOG_BODY = "UnmaintainedUnitsNagDialog.text";

    /**
     * Checks if there are any unmaintained units in the given campaign's hangar.
     *
     * @param campaign the {@link Campaign} containing the hangar to check
     * @return {@code true} if there are unmaintained units in the hangar, {@code false} otherwise
     */
    static boolean checkHanger(Campaign campaign) {
        for (Unit u : campaign.getHangar().getUnits()) {
            if ((u.isUnmaintained()) && (!u.isSalvage())) {
                    return true;
            }
        }
        return false;
    }

    /**
     * Creates a new instance of the {@link UnmaintainedUnitsNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    //region Constructors
    public UnmaintainedUnitsNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_UNMAINTAINED_UNITS);
        pack();
    }
    //endregion Constructors

    /**
     * Checks if there is a nag message to display.
     *
     * @return {@code true} if there is a nag message to display, {@code false} otherwise
     */
    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && checkHanger(getCampaign());
    }
}
