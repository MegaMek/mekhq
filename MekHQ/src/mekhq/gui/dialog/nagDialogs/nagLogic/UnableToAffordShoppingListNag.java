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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.finances.Money;

public class UnableToAffordShoppingListNag {
    /**
     * Checks if the campaign's current funds are insufficient to cover the total loan payments due.
     *
     * <p>This method calculates the total loan payments due for tomorrow and compares it to the current
     * available funds. If the available funds are less than the total payment amount, the method returns {@code true},
     * indicating that the campaign cannot afford the payments. Otherwise, it returns {@code false}.</p>
     *
     * @param totalBuyCost A {@link Money} object representing the total cost to buy all items on the shopping list
     * @param currentFunds The current available funds in the campaign as a {@link Money} object.
     *
     * @return {@code true} if the campaign's funds are less than the total cost to buy oll items on the shopping list,
     *       {@code false} otherwise.
     */
    public static boolean unableToAffordShoppingList(Money totalBuyCost, Money currentFunds) {
        return currentFunds.isLessThan(totalBuyCost);
    }
}
