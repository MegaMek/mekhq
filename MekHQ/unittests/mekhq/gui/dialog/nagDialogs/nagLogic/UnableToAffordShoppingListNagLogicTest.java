package mekhq.gui.dialog.nagDialogs.nagLogic;

import static mekhq.gui.dialog.nagDialogs.nagLogic.UnableToAffordShoppingListNag.unableToAffordShoppingList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;

public class UnableToAffordShoppingListNagLogicTest {

    @Test
    void canAfford() {
        Money currentFunds = Money.of(10);
        Money totalBuyCost = Money.of(5);
        assertFalse(unableToAffordShoppingList(totalBuyCost, currentFunds));
    }

    @Test
    void canNotAfford() {
        Money currentFunds = Money.of(5);
        Money totalBuyCost = Money.of(10);
        assertTrue(unableToAffordShoppingList(totalBuyCost, currentFunds));
    }

    @Test
    void bothZero() {
        Money currentFunds = Money.zero();
        Money totalBuyCost = Money.zero();
        assertFalse(unableToAffordShoppingList(totalBuyCost, currentFunds));
    }
    
    @Test
    void bothSame() {
        Money currentFunds = Money.of(10);
        Money totalBuyCost = Money.of(10);
        assertFalse(unableToAffordShoppingList(totalBuyCost, currentFunds));
    }
}
