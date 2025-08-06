/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;

public class UnableToAffordLoanPaymentNag {
    /**
     * Checks if the campaign's current funds are insufficient to cover the total loan payments due.
     *
     * <p>This method calculates the total loan payments due for tomorrow and compares it to the current
     * available funds. If the available funds are less than the total payment amount, the method returns {@code true},
     * indicating that the campaign cannot afford the payments. Otherwise, it returns {@code false}.</p>
     *
     * @param loans        A {@link List} of {@link Loan} objects representing the campaign's active loans.
     * @param today        The current date, used to calculate tomorrow's date for loan payments.
     * @param currentFunds The current available funds in the campaign as a {@link Money} object.
     *
     * @return {@code true} if the campaign's funds are less than the total loan payments due, {@code false} otherwise.
     */
    public static boolean unableToAffordLoans(List<Loan> loans, LocalDate today, Money currentFunds) {
        Money totalPaymentsDue = getTotalPaymentsDue(loans, today);
        return currentFunds.isLessThan(totalPaymentsDue) && totalPaymentsDue.isGreaterThan(Money.zero());
    }

    /**
     * Calculates the total loan payments due for the following day.
     *
     * <p>This method iterates through a list of loans to determine which payments are due tomorrow.
     * For each loan with a due date matching tomorrow's date, its payment amount is added to the cumulative total. The
     * final total is returned as a {@link Money} object.</p>
     *
     * @param loans A {@link List} of {@link Loan} objects associated with the campaign.
     * @param today The current date, used to calculate tomorrow's date.
     *
     * @return The total payments due for tomorrow as a {@link Money} object.
     */
    public static Money getTotalPaymentsDue(List<Loan> loans, LocalDate today) {
        Money totalPaymentsDue = Money.zero();

        // gets tomorrow's date
        LocalDate tomorrow = today.plusDays(1);

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
