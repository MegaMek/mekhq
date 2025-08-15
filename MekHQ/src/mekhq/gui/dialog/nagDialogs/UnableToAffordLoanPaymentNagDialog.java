/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT;
import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordLoanPaymentNag.getTotalPaymentsDue;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordLoanPaymentNag.unableToAffordLoans;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players when they are unable to afford a loan payment within the campaign.
 *
 * <p>The {@code UnableToAffordLoanPaymentNagDialog} extends {@link ImmersiveDialogNag} and is specifically
 * designed to alert players about financial constraints preventing them from making a loan payment. It uses predefined
 * constants, including the {@code LOGISTICS} speaker and the {@code NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT} identifier, to
 * configure the dialog's behavior and content.</p>
 */
public class UnableToAffordLoanPaymentNagDialog extends ImmersiveDialogNag {
    /**
     * Constructs a new {@code UnableToAffordLoanPaymentNagDialog} to display a warning about an unaffordable loan
     * payment.
     *
     * <p>This constructor initializes the dialog with preconfigured values, such as the
     * {@code NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT} constant for managing dialog suppression, the
     * {@code "UnableToAffordLoanPaymentNagDialog"} localization key for retrieving dialog content, and the
     * {@code LOGISTICS} speaker for delivering the message.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data
     *                 required for constructing the nag dialog.
     */
    public UnableToAffordLoanPaymentNagDialog(final Campaign campaign) {
        super(campaign, LOGISTICS, NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT, "UnableToAffordLoanPaymentNagDialog");
    }

    @Override
    protected String getInCharacterMessage(Campaign campaign, String key, String commanderAddress) {
        final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

        Finances finances = campaign.getFinances();
        Money totalPaymentsDue = getTotalPaymentsDue(finances.getLoans(), campaign.getLocalDate());
        Money currentFunds = campaign.getFunds();
        Money deficit = totalPaymentsDue.minus(currentFunds);

        return getFormattedTextAt(RESOURCE_BUNDLE,
              key + ".ic",
              commanderAddress,
              totalPaymentsDue.toAmountString(),
              currentFunds.toAmountString(),
              deficit.toAmountString());
    }

    /**
     * Determines whether a nag dialog should be displayed for the inability to afford loan payments.
     *
     * <p>This method evaluates two conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for the inability to afford loan payments in their options.</li>
     *     <li>The campaign does not have sufficient funds to cover its loan payments.</li>
     * </ul>
     *
     * @param loans        A {@link List} of {@link Loan} objects representing the campaign's active loans.
     * @param today        The current date, used to calculate tomorrow's date for loan payments.
     * @param currentFunds The current available funds in the campaign as a {@link Money} object.
     *
     * @return {@code true} if the nag dialog should be displayed due to insufficient funds for loan payments,
     *       {@code false} otherwise.
     */
    public static boolean checkNag(List<Loan> loans, LocalDate today, Money currentFunds) {
        final String NAG_KEY = NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) && unableToAffordLoans(loans, today, currentFunds);
    }
}
