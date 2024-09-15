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
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;
import java.time.LocalDate;

/**
 * This class represents a nag dialog displayed on the day a contract ends
 * It extends the AbstractMHQNagDialog class.
 */
public class EndContractNagDialog extends AbstractMHQNagDialog {
    static String DIALOG_NAME = "EndContractNagDialog";
    static String DIALOG_TITLE = "EndContractNagDialog.title";
    static String DIALOG_BODY = "EndContractNagDialog.text";

    /**
     * Checks if a contract within a campaign has ended on the current date.
     *
     * @param campaign the campaign containing the contracts to be checked
     * @return {@code true} if a contract within the campaign has ended on the current date,
     *         {@code false} otherwise
     */
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

    /**
     * Creates a new instance of the EndContractNagDialog class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the Campaign associated with the dialog
     */
    public EndContractNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_CONTRACT_ENDED);
    }

    /**
     * Checks if there is a nag message to display.
     *
     * @return {@code true} if there is a nag message to display, {@code false} otherwise
     */
    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && isContractEnded(getCampaign());
    }
}
