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
