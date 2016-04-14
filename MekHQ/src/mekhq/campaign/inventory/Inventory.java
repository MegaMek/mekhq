package mekhq.campaign.inventory;

import java.util.Set;

/**
 * An inventory is a holder for items - ammo, physical body parts and so on.
 * <p>
 * For now (since we lack a better class) it holds Part instances.
 */
public abstract class Inventory {
    /** @return human-readable name of this inventory; can be <code>null</code> for temporary internal ones */
    public String getName() {
        return null;
    }
    
    /** @return a read-only set of item types inside this inventory */
    public abstract Set<Item> getItemTypes();
    
    /** @return amount of item types inside this inventory */
    public abstract int getItemTypesAmount();
    
    /** @return amount of specific item type inside this inventory */
    public abstract int getAmount(Item item);
    
    /**
     * Set the amount of items inside this inventory.
     * 
     * @return the amount of items set, or -1 if there was an error.
     */
    public abstract int setAmount(Item item, int amount);
    
    /**
     * Stores the amount of items inside this inventory.
     * 
     * @return the amount of items added, which can be less than the amount requested.
     */
    public abstract int storeItems(Item item, int amount);
    
    /**
     * Stores one item inside this inventory.
     * 
     * @return 1 is the storage was successful, 0 else
     */
    public final int storeItem(Item item) {
        return storeItems(item, 1);
    }
    
    /**
     * Removes the amount of items from this inventory.
     * 
     * @return the amount of items removed, which can be less than the amount requested.
     */
    public abstract int removeItems(Item item, int amount);
    
    /**
     * Removes one item from this inventory.
     * 
     * @return 1 is the removal was successful, 0 else
     */
    public final int removeItem(Item item) {
        return removeItems(item, 1);
    }
    
    /**
     * Transfer the items of the given type from the supplied inventory to this one.
     * <p>
     * This method is guaranteed to not "lose" any items. Essentially, the following is an invariant:
     * <code>this.getAmount(item) + from.getAmount(item)</code>
     * 
     * @return the amount of items transferred
     */
    public abstract int transferFrom(Inventory from, Item item, int amount);
    
    /**
     * Transfer all the possible items of the given type from the supplied inventory to this one.
     * <p>
     * This method is guaranteed to not "lose" any items. Essentially, the following is an invariant:
     * <code>this.getAmount(item) + from.getAmount(item)</code>
     * 
     * @return the amount of items transferred
     */
    public final int transferAllFrom(Inventory from, Item item) {
        return transferFrom(from, item, Integer.MAX_VALUE);
    }
    
    /**
     * Transfer everything that's possible to transfer from the supplied inventory to this one.
     */
    public final void transferAllFrom(Inventory from) {
        for(Item item : from.getItemTypes()) {
            transferFrom(from, item, Integer.MAX_VALUE);
        }
    }
    
    /**
     * The default implementation uses the Kahan summation algorithm, but subclasses are free
     * to override it.
     * 
     * @return the total tonnage of all items inside this inventory
     */
    public double getTonnage() {
        double sum = 0.0;
        double compensation = 0.0;
        for(Item item : getItemTypes()) {
            double adder = item.getTonnage() * getAmount(item) - compensation;
            double newSum = sum + adder;
            compensation = (newSum - sum) - adder;
            sum = newSum;
        }
        return sum;
    }
    
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("[INVENTORY]\n"); //$NON-NLS-1$
        for(Item item : getItemTypes()) {
            sb.append(String.format("* %4d %s\n", getAmount(item), item)); //$NON-NLS-1$
        }
        return sb.toString();
    }
}
