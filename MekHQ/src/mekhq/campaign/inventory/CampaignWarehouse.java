package mekhq.campaign.inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;

/**
 * A class wrapping the campaign's warehouse as an inventory. Badly.
 */
public class CampaignWarehouse extends Inventory {
    private Campaign c;
    
    public CampaignWarehouse(Campaign campaign) {
        this.c = Objects.requireNonNull(campaign);
    }

    private Map<Item, Integer> getItemMap() {
        Map<Item, Integer> result = new HashMap<Item, Integer>();
        for(Part part : c.getSpareParts()) {
            Item item = Item.newItem(part);
            if(null != item) {
                Integer currentAmount = result.get(item);
                int partQuantity = part.getQuantity();
                // Ammo and armour break the Part contract. Obviously.
                if(part instanceof AmmoStorage) {
                    partQuantity = ((AmmoStorage) part).getShots();
                }
                if(part instanceof Armor) {
                    partQuantity = ((Armor) part).getAmount();
                }
                if(null == currentAmount) {
                    result.put(item, Integer.valueOf(partQuantity));
                } else {
                    result.put(item, Integer.valueOf(currentAmount.intValue() + partQuantity));
                }
            }
        }
        return result;
    }
    
    @Override
    public Set<Item> getItemTypes() {
        return Collections.unmodifiableSet(getItemMap().keySet());
    }

    @Override
    public int getItemTypesAmount() {
        return getItemMap().size();
    }

    @Override
    public int getAmount(Item item) {
        Integer amount = getItemMap().get(Objects.requireNonNull(item));
        return (null != amount) ? amount.intValue() : 0;
    }

    @Override
    public int setAmount(Item item, int amount) {
        // TODO Implement me
        return 0;
    }

    @Override
    public int storeItems(Item item, int amount) {
        // TODO Implement me
        return 0;
    }

    @Override
    public int removeItems(Item item, int amount) {
        // TODO Implement me
        return 0;
    }

    @Override
    public int transferFrom(Inventory from, Item item, int amount) {
        // TODO Implement me
        return 0;
    }
}
