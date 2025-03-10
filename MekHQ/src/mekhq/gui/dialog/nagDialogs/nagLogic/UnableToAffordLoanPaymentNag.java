/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;

import java.time.LocalDate;
import java.util.List;

public class UnableToAffordLoanPaymentNag {
    /**
     * Determines whether the campaign's current funds are insufficient to cover
     * the total loan payments due.
     *
     * <p>
     * This method compares the campaign's available funds with the {@code totalPaymentsDue}
     * amount. If the available funds are less than the total loan payments due, it returns {@code true},
     * indicating that the campaign cannot afford the loan payments; otherwise, it returns {@code false}.
     * </p>
     *
     * @return {@code true} if the campaign's funds are less than the total loan payments due;
     *         {@code false} otherwise.
     */
    public static boolean unableToAffordLoans(Campaign campaign) {
        Money totalPaymentsDue = getTotalPaymentsDue(campaign);
        return campaign.getFunds().isLessThan(totalPaymentsDue);
    }

    /**
     * Calculates the total loan payments due for tomorrow.
     *
     * <p>
     * This method retrieves the list of loans associated with the campaign and checks if their
     * next payment date matches tomorrow's date. If a payment is due, the amount is added to the
     * cumulative total, which is stored in the {@code totalPaymentsDue} field.
     * </p>
     */
    public static Money getTotalPaymentsDue(Campaign campaign) {
        Money totalPaymentsDue = Money.zero();

        // gets the list of the campaign's current loans
        List<Loan> loans = campaign.getFinances().getLoans();

        // gets tomorrow's date
        LocalDate tomorrow = campaign.getLocalDate().plusDays(1);

        // iterate over all loans
        for (Loan loan : loans) {
            // if a loan payment is due tomorrow, add its payment amount to the total payments due
            if (loan.getNextPayment().equals(tomorrow)) {
                totalPaymentsDue = totalPaymentsDue.plus(loan.getPaymentAmount());
            }
        }

        return totalPaymentsDue;
    }
}
