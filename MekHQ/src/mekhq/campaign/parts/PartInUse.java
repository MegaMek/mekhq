package mekhq.campaign.parts;

import java.util.Objects;
import java.util.regex.Pattern;

import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;

public class PartInUse {
    private static final Pattern cleanUp1 = Pattern.compile("\\d+\\shit\\(s\\),\\s"); //$NON-NLS-1$
    private static final Pattern cleanUp2 = Pattern.compile("\\d+\\shit\\(s\\)"); //$NON-NLS-1$

    private String description;
    private IAcquisitionWork partToBuy;
    private int useCount;
    private int storeCount;
    private int transferCount;
    private int plannedCount;
    private long cost;
    
    public PartInUse(Part part) {
        StringBuilder sb = new StringBuilder(part.getName());
        Unit u = part.getUnit();
        if(!(part instanceof MissingBattleArmorSuit)) {
            part.setUnit(null);
        }
        if(!(part instanceof Armor)) {
            String details = part.getDetails();
            details = cleanUp1.matcher(details).replaceFirst(""); //$NON-NLS-1$
            details = cleanUp2.matcher(details).replaceFirst(""); //$NON-NLS-1$
            if(details.length() > 0) {
                sb.append(" (").append(details).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        part.setUnit(u);
        this.description = sb.toString();
        this.partToBuy = part.getAcquisitionWork();
        this.cost = partToBuy.getBuyCost();
    }
    
    public PartInUse(String description, IAcquisitionWork partToBuy, long cost) {
        this.description = Objects.requireNonNull(description);
        this.partToBuy = Objects.requireNonNull(partToBuy);
        this.cost = cost;
    }
    
    public PartInUse(String description, IAcquisitionWork partToBuy) {
        this(description, partToBuy, partToBuy.getBuyCost());
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
