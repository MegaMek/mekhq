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
