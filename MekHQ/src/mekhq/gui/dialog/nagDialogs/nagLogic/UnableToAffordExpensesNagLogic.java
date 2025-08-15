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

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.FinancialReport;
import mekhq.campaign.finances.Money;

public class UnableToAffordExpensesNagLogic {
    /**
     * Determines whether the campaign's current funds are insufficient to cover the monthly expenses.
     *
     * <p>
     * This method compares the campaign's available funds with the {@code monthlyExpenses} amount. If the available
     * funds are less than the monthly expenses, it returns {@code true}, indicating that the campaign cannot afford its
     * expenses; otherwise, it returns {@code false}.
     * </p>
     *
     * @return {@code true} if the campaign's funds are less than the monthly expenses; {@code false} otherwise.
     */
    public static boolean unableToAffordExpenses(Campaign campaign) {
        Money monthlyExpenses = getMonthlyExpenses(campaign);
        return campaign.getFunds().isLessThan(monthlyExpenses);
    }

    /**
     * Retrieves and calculates the campaign's total monthly expenses.
     *
     * <p>
     * This method generates a {@link FinancialReport} for the campaign to compute the total monthly expenses, which are
     * then stored in the {@code monthlyExpenses} field. The expenses include operational costs, unit upkeep, payroll,
     * and other recurring items.
     * </p>
     */
    public static Money getMonthlyExpenses(Campaign campaign) {
        // calculate a financial report which includes the monthly expenses
        FinancialReport financialReport = FinancialReport.calculate(campaign);

        // get the total monthly expenses
        return financialReport.getMonthlyExpenses();
    }
}
