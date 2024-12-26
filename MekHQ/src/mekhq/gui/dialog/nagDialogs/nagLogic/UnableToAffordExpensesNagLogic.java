package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.FinancialReport;
import mekhq.campaign.finances.Money;

public class UnableToAffordExpensesNagLogic {
    /**
     * Determines whether the campaign's current funds are insufficient to cover
     * the monthly expenses.
     *
     * <p>
     * This method compares the campaign's available funds with the {@code monthlyExpenses}
     * amount. If the available funds are less than the monthly expenses, it returns {@code true},
     * indicating that the campaign cannot afford its expenses; otherwise, it returns {@code false}.
     * </p>
     *
     * @return {@code true} if the campaign's funds are less than the monthly expenses;
     *         {@code false} otherwise.
     */
    public static boolean unableToAffordExpenses(Campaign campaign) {
        Money monthlyExpenses = getMonthlyExpenses(campaign);
        return campaign.getFunds().isLessThan(monthlyExpenses);
    }

    /**
     * Retrieves and calculates the campaign's total monthly expenses.
     *
     * <p>
     * This method generates a {@link FinancialReport} for the campaign to compute the
     * total monthly expenses, which are then stored in the {@code monthlyExpenses} field.
     * The expenses include operational costs, unit upkeep, payroll, and other recurring items.
     * </p>
     */
    public static Money getMonthlyExpenses(Campaign campaign) {
        // calculate a financial report which includes the monthly expenses
        FinancialReport financialReport = FinancialReport.calculate(campaign);

        // get the total monthly expenses
        return financialReport.getMonthlyExpenses();
    }
}
