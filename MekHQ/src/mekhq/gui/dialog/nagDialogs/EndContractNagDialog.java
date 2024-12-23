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
import mekhq.campaign.mission.Contract;
import mekhq.gui.baseComponents.AbstractMHQNagDialog_NEW;

import java.time.LocalDate;

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
 *   <li>Extends the {@link AbstractMHQNagDialog_NEW} to reuse base nag dialog functionality.</li>
 * </ul>
 */
public class EndContractNagDialog extends AbstractMHQNagDialog_NEW {
    public EndContractNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_CONTRACT_ENDED);

        final String DIALOG_BODY = "EndContractNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false)));
    }

    static boolean isContractEnded(Campaign campaign) {
        LocalDate today = campaign.getLocalDate();

        // we can't use 'is date y after x', as once the end date has been passed,
        // the contract is removed from the list of active contracts

        // there is no reason to use a stream here, as there won't be enough iterations to warrant it
        for (Contract contract : campaign.getActiveContracts()) {
            if (contract.getEndingDate().equals(today)) {
                return true;
            }
        }

        return false;
    }

    public boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_CONTRACT_ENDED;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && (campaign.getFlaggedCommander() == null)) {
            showDialog();
        }
        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
                && isContractEnded(campaign);
    }
}
