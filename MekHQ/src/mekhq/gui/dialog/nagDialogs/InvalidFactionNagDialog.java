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
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.AbstractMHQNagDialog_NEW;

import java.time.LocalDate;

/**
 * A dialog used to notify the user about an invalid faction in the current campaign.
 *
 * <p>
 * This nag dialog is triggered when the campaign's selected faction is determined to be invalid
 * for the current campaign date. It evaluates the validity of the faction based on the campaign
 * date and displays a localized message warning the user about the issue.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Checks whether the campaign's faction is valid based on the current in-game date.</li>
 *   <li>Displays a warning dialog to alert the user when an invalid faction is detected.</li>
 *   <li>Extends {@link AbstractMHQNagDialog_NEW} to ensure consistent behavior with other nag dialogs.</li>
 * </ul>
 */
public class InvalidFactionNagDialog extends AbstractMHQNagDialog_NEW {
    /**
     * Checks whether the campaign's faction is invalid for the current in-game date.
     *
     * <p>
     * This method retrieves the campaign's faction using {@link Campaign#getFaction()} and evaluates
     * its validity for the current date using {@link Faction#validIn(LocalDate)}. A faction is considered
     * invalid if it is not valid for the campaign's local date.
     * </p>
     *
     * @param campaign The {@link Campaign} whose faction validity is to be checked.
     * @return {@code true} if the faction is invalid for the campaign's current date; otherwise,
     * {@code false}.
     */
    static boolean isFactionInvalid(Campaign campaign) {
        Faction campaignFaction = campaign.getFaction();
        LocalDate today = campaign.getLocalDate();

        return !campaignFaction.validIn(today);
    }

    /**
     * Constructs an {@code InvalidFactionNagDialog} for the given campaign.
     *
     * <p>
     * This dialog initializes with the campaign information and sets a localized
     * message to notify the user about the potential issue involving an invalid faction.
     * The message includes the commander's address for better clarity.
     * </p>
     *
     * @param campaign The {@link Campaign} associated with this nag dialog.
     *                 The campaign provides the faction and other details for evaluation.
     */
    public InvalidFactionNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_INVALID_FACTION);

        final String DIALOG_BODY = "InvalidFactionNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    /**
     * Determines whether the "Invalid Faction" nag dialog should be displayed.
     *
     * <p>
     * This method checks the following conditions:
     * <ul>
     *   <li>If the "Invalid Faction" nag dialog is flagged as ignored in user settings.</li>
     *   <li>If the campaign's faction is invalid for the current in-game date,
     *   as determined by {@link #isFactionInvalid(Campaign)}.</li>
     * </ul>
     * If both conditions are met, the dialog is displayed to notify the user of the issue.
     * </p>
     *
     * @param campaign The {@link Campaign} being evaluated for faction validity.
     */
    public void checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_INVALID_FACTION;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (isFactionInvalid(campaign))) {
            showDialog();
        }
    }
}
