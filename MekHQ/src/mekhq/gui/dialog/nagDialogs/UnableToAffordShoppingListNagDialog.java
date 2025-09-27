/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.MHQConstants.NAG_UNABLE_TO_AFFORD_SHOPPING_LIST;
import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordShoppingListNag.unableToAffordShoppingList;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players when they are unable to afford all items on the shopping list within the
 * campaign.
 *
 * <p>The {@code UnableToAffordShoppingListNagDialog} extends {@link ImmersiveDialogNag} and is specifically
 * designed to alert players about financial constraints preventing them from procuring all items on the shopping list
 * It uses predefined constants, including the {@code LOGISTICS} speaker and the
 * {@code NAG_UNABLE_TO_AFFORD_SHOPPING_LIST} identifier, to configure the dialog's behavior and content.</p>
 */


public class UnableToAffordShoppingListNagDialog extends ImmersiveDialogNag {
    /**
     * Constructs a new {@code UnableToAffordShoppingListNagDialog} to display a warning about insufficient funds for
     * all items on the shopping list.
     *
     * <p>This constructor initializes the dialog with preconfigured values, such as the
     * {@code NAG_UNABLE_TO_AFFORD_SHOPPING_LIST} constant for managing dialog suppression, the
     * {@code "UnableToAffordShoppingListNagDialog"} localization key for retrieving dialog content, and the
     * {@code LOGISTICS} speaker for delivering the message.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data
     *                 required for constructing the nag dialog.
     */
    public UnableToAffordShoppingListNagDialog(final Campaign campaign) {
        super(campaign, LOGISTICS, NAG_UNABLE_TO_AFFORD_SHOPPING_LIST, "UnableToAffordShoppingListNagDialog");
    }

    @Override
    protected String getInCharacterMessage(Campaign campaign, String key, String commanderAddress) {
        final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

        Money totalBuyCost = campaign.getShoppingList().getTotalBuyCost();
        Money currentFunds = campaign.getFunds();
        Money deficit = totalBuyCost.minus(currentFunds);

        return getFormattedTextAt(RESOURCE_BUNDLE,
              key + ".ic",
              commanderAddress,
              totalBuyCost.toAmountString(),
              currentFunds.toAmountString(),
              deficit.toAmountString());
    }

    /**
     * Determines whether a nag dialog should be displayed for the inability to afford all items on the shopping list
     *
     * <p>This method evaluates two conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for the inability to afford all items on the shopping list.</li>
     *     <li>The campaign does not have sufficient funds to pay for all items on the shopping list.</li>
     * </ul>
     *
     * @param totalBuyCost A {@link Money} object representing the total cost to buy all items on the shopping list
     * @param currentFunds The current available funds in the campaign as a {@link Money} object.
     *
     * @return {@code true} if the nag dialog should be displayed due to insufficient funds for loan payments,
     *       {@code false} otherwise.
     */
    public static boolean checkNag(Money totalBuyCost, Money currentFunds) {

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_UNABLE_TO_AFFORD_SHOPPING_LIST) &&
                     unableToAffordShoppingList(totalBuyCost, currentFunds);
    }

}
