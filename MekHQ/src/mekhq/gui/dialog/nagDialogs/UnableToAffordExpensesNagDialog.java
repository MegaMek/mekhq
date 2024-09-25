/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.finances.FinancialReport;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import javax.swing.*;

/**
 * This class represents a nag dialog displayed when the campaign does not have enough funds to
 * cover monthly expenses
 * It extends the {@link AbstractMHQNagDialog} class.
 */
public class UnableToAffordExpensesNagDialog extends AbstractMHQNagDialog {
    private static String DIALOG_NAME = "UnableToAffordExpensesNagDialog";
    private static String DIALOG_TITLE = "UnableToAffordExpensesNagDialog.title";
    private static String DIALOG_BODY = "UnableToAffordExpensesNagDialog.text";

    /**
     * Determines whether the given campaign is unable to afford its monthly expenses.
     *
     * @param campaign the ongoing campaign
     * @return {@code true} if the campaign's funds are less than the total deficit, {@code false} otherwise
     */
    static boolean isUnableToAffordExpenses (Campaign campaign) {
        Money deficit = getMonthlyExpenses(campaign);

        // check if the campaign's funds are less than the total deficit
        return campaign.getFunds().isLessThan(deficit);
    }

    /**
     * Calculates and returns the monthly expenses of a given campaign.
     *
     * @param campaign the campaign for which to calculate the monthly expenses
     * @return the monthly expenses as a {@link Money} object
     */
    static Money getMonthlyExpenses(Campaign campaign) {
        // calculate a financial report which includes the monthly expenses
        FinancialReport financialReport = FinancialReport.calculate(campaign);

        // get the total monthly expenses
        return financialReport.getMonthlyExpenses();
    }

    //region Constructors
    /**
     * Creates a new instance of the {@link ShortDeploymentNagDialog} class.
     *
     * @param frame the parent JFrame for the dialog
     * @param campaign the {@link Campaign} associated with the dialog
     */
    public UnableToAffordExpensesNagDialog(final JFrame frame, final Campaign campaign) {
        super(frame, DIALOG_NAME, DIALOG_TITLE, DIALOG_BODY, campaign, MHQConstants.NAG_UNABLE_TO_AFFORD_EXPENSES);
    }
    //endregion Constructors

    /**
     * Checks if the campaign is able to afford its monthly expenses.
     * If the campaign is unable to afford monthly expenses and the Nag dialog for the current key
     * is not ignored, it sets the description using the specified format and returns {@code true}.
     * Otherwise, it returns {@code false}.
     */
    @Override
    protected boolean checkNag() {
        if (!MekHQ.getMHQOptions().getNagDialogIgnore(getKey())
                && isUnableToAffordExpenses(getCampaign())) {
            setDescription(String.format(
                    resources.getString(DIALOG_BODY),
                    getMonthlyExpenses(getCampaign()).toAmountAndSymbolString()));
            return true;
        }

        return false;
    }
}
