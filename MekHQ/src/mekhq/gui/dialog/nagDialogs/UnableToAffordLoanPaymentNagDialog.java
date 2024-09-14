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

public class UnableToAffordLoanPaymentNagDialog extends AbstractMHQNagDialog {
    /**
     * Checks if a campaign is unable to afford any loan payment due the next day.
     * It iterates over all loans of the campaign and sums up the loan payments with a due date of tomorrow.
     * If the campaign's current funds are less than the total due payments,
     * the function returns true, otherwise, false.
     *
     * @param campaign The ongoing campaign
     * @return A boolean value indicating whether the campaign cannot afford its loan payments.
     * Returns {@code true} if the current funds are less than payments due, {@code false} otherwise.
     */
    private static boolean isUnableToAffordLoanPayment (Campaign campaign) {
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

        // check if the campaign's funds are less than the total payments due tomorrow
        return campaign.getFunds().isLessThan(totalPaymentsDue);
    }

    //region Constructors
    public UnableToAffordLoanPaymentNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "UnableToAffordLoanPaymentNagDialog", "UnableToAffordLoanPaymentNagDialog.title",
                "UnableToAffordLoanPaymentNagDialog.text", campaign, MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT);
    }
    //endregion Constructors

    @Override
    protected boolean checkNag() {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && isUnableToAffordLoanPayment(getCampaign());
    }
}
