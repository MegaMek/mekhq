package mekhq.campaign.parts;

import java.util.Objects;

import megamek.common.AmmoType;
import mekhq.campaign.Campaign;
import mekhq.campaign.work.IAcquisitionWork;

public class PartInUse {
    private String description;
    private IAcquisitionWork partToBuy;
    private int useCount;
    private int storeCount;
    private double tonnagePerItem;
    private int transferCount;
    private int plannedCount;
    private long cost;
    
    public PartInUse(Part part, Campaign c) {
        this.description = part.generateLongDescription(c);
        this.partToBuy = part.getAcquisitionWork();
        this.tonnagePerItem = part.getTonnage();

        if(partToBuy instanceof AmmoStorage) {
	        AmmoType ammoType = (AmmoType) ((AmmoStorage) partToBuy).getType();
	        
	        if(ammoType.getKgPerShot() > 0) {
	            this.tonnagePerItem = ammoType.getKgPerShot() / 1000.0;
	        } else {
	            this.tonnagePerItem = 1.0 / ammoType.getShots();
	        }
        } else if(part instanceof Armor) {
            // Armor needs different tonnage values
            this.tonnagePerItem = 1.0 / ((Armor) part).getArmorPointsPerTon();
        }
        
        if(null != partToBuy) {
            this.cost = partToBuy.getBuyCost();
        }
    }
    
    public String getDescription() {
        return description;
    }
    
    public IAcquisitionWork getPartToBuy() {
        return partToBuy;
    }
    
    public int getUseCount() {
        return useCount;
    }
    
    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }
    
    public void incUseCount() {
        ++ useCount;
    }
    
    public int getStoreCount() {
        return storeCount;
    }
    
    public double getStoreTonnage() {
        return storeCount * tonnagePerItem;
    }
    
    public void setStoreCount(int storeCount) {
        this.storeCount = storeCount;
    }
    
    public void incStoreCount() {
        ++ storeCount;
    }
    
    public int getTransferCount() {
        return transferCount;
    }
    
    public void incTransferCount() {
        ++ transferCount;
    }
    
    public void setTransferCount(int transferCount) {
        this.transferCount = transferCount;
    }
    
    public int getPlannedCount() {
        return plannedCount;
    }
    
    public void setPlannedCount(int plannedCount) {
        this.plannedCount = plannedCount;
    }
    
    public void incPlannedCount() {
        ++ plannedCount;
    }
    
    public long getCost() {
        return cost;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(description);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final PartInUse other = (PartInUse) obj;
        return Objects.equals(description, other.description);
    }
}
