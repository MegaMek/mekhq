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
package mekhq.gui.dialog.nagDialogs;

import static mekhq.MHQConstants.NAG_UNABLE_TO_AFFORD_EXPENSES;
import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordExpensesNagLogic.getMonthlyExpenses;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordExpensesNagLogic.unableToAffordExpenses;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.temporal.TemporalAdjusters;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players when campaign expenses cannot be afforded.
 *
 * <p>The {@code UnableToAffordExpensesNagDialog} extends {@link ImmersiveDialogNag} and is specifically designed
 * to alert players about financial issues in the campaign. It utilizes predefined constants, including the
 * {@code LOGISTICS} speaker and the {@code NAG_UNABLE_TO_AFFORD_EXPENSES} identifier, to configure the dialog's
 * behavior and content.</p>
 */
public class UnableToAffordExpensesNagDialog extends ImmersiveDialogNag {

    /**
     * Constructs a new {@code UnableToAffordExpensesNagDialog} to display a warning about unaffordable campaign
     * expenses.
     *
     * <p>This constructor initializes the dialog with preconfigured values, such as the
     * {@code NAG_UNABLE_TO_AFFORD_EXPENSES} constant for managing dialog suppression, the
     * {@code "UnableToAffordExpensesNagDialog"} localization key for retrieving dialog content, and the
     * {@code LOGISTICS} speaker for delivering the message.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data
     *                 required for constructing the nag dialog.
     */
    public UnableToAffordExpensesNagDialog(final Campaign campaign) {
        super(campaign, LOGISTICS, NAG_UNABLE_TO_AFFORD_EXPENSES, "UnableToAffordExpensesNagDialog");
    }

    @Override
    protected String getInCharacterMessage(Campaign campaign, String key, String commanderAddress) {
        final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

        Money monthlyExpenses = getMonthlyExpenses(campaign);
        Money currentFunds = campaign.getFunds();
        Money deficit = monthlyExpenses.minus(currentFunds);

        return getFormattedTextAt(RESOURCE_BUNDLE,
              key + ".ic",
              commanderAddress,
              monthlyExpenses.toAmountString(),
              currentFunds.toAmountString(),
              deficit.toAmountString());
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
     *
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise
     */
    public static boolean checkNag(Campaign campaign) {

        return campaign.getLocalDate().equals(campaign.getLocalDate().with(TemporalAdjusters.lastDayOfMonth())) &&
                     !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_UNABLE_TO_AFFORD_EXPENSES) &&
                     unableToAffordExpenses(campaign);
    }
}
