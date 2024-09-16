/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

/**
 * This class represents a nag dialog displayed when the campaign cannot afford its next loan payment
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class UnableToAffordLoanPaymentNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "UnableToAffordLoanPaymentNagDialog";
    private static String DIALOG_TITLE = "UnableToAffordLoanPaymentNagDialog.title";
    private static String DIALOG_BODY = "UnableToAffordLoanPaymentNagDialog.text";

    /**
     * Determines if the campaign is unable to afford its due loan payments.
     *
     * @param campaign the campaign for which the loan payment affordability needs to be checked
     * @return {@code true} if the campaign is unable to afford the loan payment, {@code false} otherwise
     */
    static boolean isUnableToAffordLoanPayment(Campaign campaign) {
        Money totalPaymentsDue = getTotalPaymentsDue(campaign);

        // check if the campaign's funds are less than the total payments due tomorrow
        return campaign.getFunds().isLessThan(totalPaymentsDue);
    }

    /**
     * Calculates the total payments due tomorrow, across all current loans
     *
     * @param campaign the campaign for which to calculate the total payments due
     * @return the total payments due as a {@link Money} object
     */
    static Money getTotalPaymentsDue(Campaign campaign) {
        // gets the list of the campaign's current loans
        List<Loan> loans = campaign.getFinances().getLoans();

        // gets tomorrow's date
        LocalDate tomorrow = campaign.getLocalDate().plusDays(1);

        // initialize the total loan payment due tomorrow as zero
        Money totalPaymentsDue = Money.zero();

        // iterate over all loans
        for (Loan loan : loans) {
            // if a loan payment is due tomorrow, add its payment amount to the total payments due
            if (loan.getNextPayment().equals(tomorrow)) {
                totalPaymentsDue = totalPaymentsDue.plus(loan.getPaymentAmount());
            }
        }
        return totalPaymentsDue;
    }

    //region Constructors
    /**
     * Creates a new instance of the {@link UnableToAffordLoanPaymentNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public UnableToAffordLoanPaymentNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT);
    }
    //endregion Constructors

    /**
     * Checks if the campaign is able to afford its next loan payment.
     * If the campaign is unable to afford its next loan payment and the Nag dialog for the current
     * key is not ignored, it sets the description using the specified format and returns {@code true}.
     * Otherwise, it returns {@code false}.
     */
    @Override
    protected boolean checkNag() {
        if (!MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && isUnableToAffordLoanPayment(getCampaign())) {
            setDescription(String.format(
                    resources.getString(DIALOG_BODY),
                    getTotalPaymentsDue(getCampaign()).toAmountAndSymbolString()));
            return true;
        }

        return false;
    }
}
