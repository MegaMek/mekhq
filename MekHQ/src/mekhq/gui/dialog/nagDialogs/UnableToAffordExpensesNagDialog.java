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
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;

import java.time.temporal.TemporalAdjusters;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordExpensesNagLogic.getMonthlyExpenses;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordExpensesNagLogic.unableToAffordExpenses;

/**
 * A nag dialog that warns the user if the campaign's available funds are insufficient to cover monthly expenses.
 *
 * <p>
 * This dialog is designed to notify players when their campaign is at financial risk due to
 * a shortage of funds relative to required monthly expenses. It calculates the total expenses
 * for the current financial report and compares it with the campaign's available funds. If
 * the funds are insufficient, the dialog is displayed to alert the player.
 * </p>
 */
public class UnableToAffordExpensesNagDialog extends AbstractMHQNagDialog {
    private final Campaign campaign;

    /**
     * Constructs the nag dialog for insufficient campaign funds.
     *
     * <p>
     * This constructor initializes the dialog with the necessary campaign information
     * and formats the displayed message to include the total monthly expenses and the
     * commander's name or title.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public UnableToAffordExpensesNagDialog(final Campaign campaign) {
        super(campaign, MHQConstants.NAG_UNABLE_TO_AFFORD_EXPENSES);

        this.campaign = campaign;

        Money monthlyExpenses = getMonthlyExpenses(campaign);

        final String DIALOG_BODY = "UnableToAffordExpensesNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false),
            monthlyExpenses.toAmountAndSymbolString()));
    }

    /**
     * Determines whether the insufficient funds nag dialog should be displayed to the user.
     *
     * <p>
     * The dialog is displayed if:
     * <ul>
     *     <li>It's the last day of the month.</li>
     *     <li>The nag dialog for insufficient funds is not ignored in MekHQ options.</li>
     *     <li>The campaign's available funds are less than the calculated monthly expenses.</li>
     * </ul>
     * If the conditions are met, the dialog is displayed to alert the player.
     */
    public void checkNag() {
        final String NAG_KEY = MHQConstants.NAG_UNABLE_TO_AFFORD_EXPENSES;

        if (campaign.getLocalDate().equals(campaign.getLocalDate().with(TemporalAdjusters.lastDayOfMonth()))
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && unableToAffordExpenses(campaign)) {
            showDialog();
        }
    }
}
