/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
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
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import java.time.LocalDate;
import java.util.List;

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

        Finances finances = campaign.getFinances();
        Money totalPaymentsDue = getTotalPaymentsDue(finances.getLoans(), campaign.getLocalDate());

        final String DIALOG_BODY = "UnableToAffordLoanPaymentNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false),
            totalPaymentsDue.toAmountAndSymbolString()));
        showDialog();
    }

    /**
     * Determines whether a nag dialog should be displayed for the inability to afford loan payments.
     *
     * <p>This method evaluates two conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for the inability to afford loan payments in their options.</li>
     *     <li>The campaign does not have sufficient funds to cover its loan payments.</li>
     * </ul>
     *
     * @param loans A {@link List} of {@link Loan} objects representing the campaign's active loans.
     * @param today The current date, used to calculate tomorrow's date for loan payments.
     * @param currentFunds The current available funds in the campaign as a {@link Money} object.
     * @return {@code true} if the nag dialog should be displayed due to insufficient funds for loan payments,
     *         {@code false} otherwise.
     */
    public static boolean checkNag(List<Loan> loans, LocalDate today, Money currentFunds) {
        final String NAG_KEY = MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
              && unableToAffordLoans(loans, today, currentFunds);
    }
}
