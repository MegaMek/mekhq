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
    private final Campaign campaign;

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

        this.campaign = campaign;

        Money totalPaymentsDue = getTotalPaymentsDue(campaign);

        final String DIALOG_BODY = "UnableToAffordLoanPaymentNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false),
            totalPaymentsDue.toAmountAndSymbolString()));
    }

    /**
     * Determines whether the insufficient loan payment funds nag dialog should be displayed.
     *
     * <p>
     * The dialog is displayed if:
     * <ul>
     *     <li>The nag dialog for loan payments is not ignored in MekHQ options.</li>
     *     <li>The total loan payments due tomorrow exceed the campaign's available funds.</li>
     * </ul>
     * If both conditions are met, the user is shown the dialog to alert them about upcoming
     * loan payment issues.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT;

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && unableToAffordLoans(campaign)) {
            showDialog();
        }
    }
}
