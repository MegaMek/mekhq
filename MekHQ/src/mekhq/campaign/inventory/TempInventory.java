package mekhq.campaign.inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** A small, temporary inventory capable of holding an unlimited amount of items */
public class TempInventory extends Inventory {
    private Map<Item, Integer> items;
    
    public TempInventory() {
        items = new HashMap<>();
    }

    @Override
    public Set<Item> getItemTypes() {
        return Collections.unmodifiableSet(items.keySet());
    }

    @Override
    public int getItemTypesAmount() {
        return items.size();
    }

    @Override
    public int getAmount(Item item) {
        Integer amount = items.get(Objects.requireNonNull(item));
        return (null != amount) ? amount.intValue() : 0;
    }

    @Override
    public int setAmount(Item item, int amount) {
        items.put(Objects.requireNonNull(item), Integer.valueOf(Math.max(amount, 0)));
        return amount;
    }

    @Override
    public int storeItems(Item item, int amount) {
        setAmount(item, getAmount(item) + amount);
        return amount;
    }

    @Override
    public int removeItems(Item item, int amount) {
        int currentAmount = getAmount(item);
        setAmount(item, currentAmount - amount);
        return Math.min(amount, currentAmount);
    }

    @Override
    public int transferFrom(Inventory from, Item item, int amount) {
        int transfered = from.removeItems(Objects.requireNonNull(item), amount);
        return storeItems(item, transfered);
    }
}
