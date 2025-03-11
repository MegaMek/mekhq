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

        Money monthlyExpenses = getMonthlyExpenses(campaign);

        final String DIALOG_BODY = "UnableToAffordExpensesNagDialog.text";
        setRightDescriptionMessage(String.format(resources.getString(DIALOG_BODY),
            campaign.getCommanderAddress(false),
            monthlyExpenses.toAmountAndSymbolString()));
        showDialog();
    }

    /**
     * Checks if a nag dialog should be displayed for the inability to afford expenses in the given campaign.
     *
     * <p>The method evaluates the following conditions to determine if the nag dialog should appear:</p>
     * <ul>
     *     <li>If it is the last day of the month in the campaign.</li>
     *     <li>If the nag dialog for the inability to afford expenses has not been ignored in the user options.</li>
     *     <li>If the campaign is unable to afford its expenses.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} to check for nagging conditions
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_UNABLE_TO_AFFORD_EXPENSES;

        return campaign.getLocalDate().equals(campaign.getLocalDate().with(TemporalAdjusters.lastDayOfMonth()))
            && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY)
            && unableToAffordExpenses(campaign);
    }
}
