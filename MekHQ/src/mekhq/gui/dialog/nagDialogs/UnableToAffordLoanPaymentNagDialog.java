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
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordLoanPaymentNag.getTotalPaymentsDue;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordLoanPaymentNag.unableToAffordLoans;

/**
 * A nag dialog that warns the user if the campaign's available funds are not enough to cover
 * upcoming loan payments.
 *
 * <p>
 * This dialog calculates the total amount due for loan payments scheduled for the next day
 * and compares it against the campaign's available funds. If the funds are not enough to cover
 * the payments, the dialog is displayed to alert the user and prompt corrective action.
 * </p>
 */
public class UnableToAffordLoanPaymentNagDialog extends AbstractMHQNagDialog {
    /**
     * Constructs the nag dialog for insufficient funds to cover loan payments.
     *
     * <p>
     * This constructor initializes the dialog with relevant information about the campaign.
     * The displayed message includes the commander's name or title and the total amount due
     * for loans that must be paid the next day.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public UnableToAffordLoanPaymentNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT);

        Money totalPaymentsDue = getTotalPaymentsDue(campaign);

        final String DIALOG_BODY = "UnableToAffordLoanPaymentNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false),
            totalPaymentsDue.toAmountAndSymbolString()));
        showDialog();
    }

    /**
     * Checks if a nag dialog should be displayed for the inability to afford loan payments in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If the nag dialog for the inability to afford loan payments has not been ignored in the user options.</li>
     *     <li>If the campaign is unable to afford its loan payments.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && unableToAffordLoans(campaign);
    }
}
