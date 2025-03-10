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

import static mekhq.gui.dialog.nagDialogs.nagLogic.EndContractNagLogic.isContractEnded;

/**
 * A dialog used to notify the user about the end date of a contract in the campaign.
 *
 * <p>
 * This nag dialog is triggered when a contract in the campaign is flagged as ending on the current date
 * and the user has not opted to ignore such notifications. It shows relevant details about the situation
 * and allows the user to take action or dismiss the dialog.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Handles notifications for contract end dates in the campaign.</li>
 *   <li>Uses a localized message with context-specific details from the campaign.</li>
 *   <li>Extends the {@link AbstractMHQNagDialog} to reuse base nag dialog functionality.</li>
 * </ul>
 */
public class EndContractNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs an {@code EndContractNagDialog} for the given campaign.
     *
     * <p>
     * This dialog uses the localization key {@code "EndContractNagDialog.text"} to provide
     * a message that includes additional information, such as the commander's address.
     * It is specifically tailored to show when a contract is reaching its end date.
     * </p>
     *
     * @param campaign The {@link Campaign} that the nag dialog is tied to.
     */
    public EndContractNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_CONTRACT_ENDED);

        final String DIALOG_BODY = "EndContractNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
        showDialog();
    }

    /**
     * Checks if a nag dialog should be displayed for an ended contract within the given campaign.
     *
     * <p>The method evaluates the following conditions:</p>
     * <ul>
     *     <li>If the nag dialog for an ended contract has not been ignored in the user options.</li>
     *     <li>If the contract associated with the provided campaign has ended.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_CONTRACT_ENDED;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && isContractEnded(campaign);
    }
}
