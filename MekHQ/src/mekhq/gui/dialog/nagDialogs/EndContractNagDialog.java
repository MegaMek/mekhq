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
    private final Campaign campaign;

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

        this.campaign = campaign;

        final String DIALOG_BODY = "EndContractNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    /**
     * Checks if the "Contract End" nag dialog should be displayed.
     *
     * <p>
     * This method evaluates whether the nag dialog for contract end dates should be shown.
     * It checks the following conditions:
     * <ul>
     *   <li>If the "Contract Ended" nag dialog is flagged as ignored in the user’s settings.</li>
     *   <li>If any active contracts in the campaign are ending on the current date.</li>
     * </ul>
     * If these conditions are met, the dialog is displayed.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_CONTRACT_ENDED;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && isContractEnded(campaign)) {
            showDialog();
        }
    }
}
