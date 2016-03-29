package mekhq.campaign.parts;

import java.util.Objects;
import java.util.regex.Pattern;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;

public class PartInUse {
    private static final Pattern cleanUp1 = Pattern.compile("\\d+\\shit\\(s\\),\\s"); //$NON-NLS-1$
    private static final Pattern cleanUp2 = Pattern.compile("\\d+\\shit\\(s\\)"); //$NON-NLS-1$

    private String description;
    private IAcquisitionWork partToBuy;
    private int useCount;
    private int storeCount;
    private double tonnagePerItem;
    private int transferCount;
    private int plannedCount;
    private long cost;
    
    private void appendDetails(StringBuilder sb, Part part) {
        String details = part.getDetails();
        details = cleanUp1.matcher(details).replaceFirst(""); //$NON-NLS-1$
        details = cleanUp2.matcher(details).replaceFirst(""); //$NON-NLS-1$
        if(details.length() > 0) {
            sb.append(" (").append(details).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
    
    public PartInUse(Part part) {
        StringBuilder sb = new StringBuilder(part.getName());
        Unit u = part.getUnit();
        if(!(part instanceof MissingBattleArmorSuit)) {
            part.setUnit(null);
        }
        if(!(part instanceof Armor) && !(part instanceof AmmoStorage)) {
            appendDetails(sb, part);
        }
        part.setUnit(u);
        this.description = sb.toString();
        this.partToBuy = part.getAcquisitionWork();
        this.tonnagePerItem = part.getTonnage();
        // AmmoBin are special: They aren't buyable (yet?), but instead buy you the ammo inside
        // We redo the description based on that
        if(partToBuy instanceof AmmoStorage) {
            sb.setLength(0);
            sb.append(((AmmoStorage) partToBuy).getName());
            appendDetails(sb, (Part) ((AmmoStorage) partToBuy).getAcquisitionWork());
            this.description = sb.toString();
            AmmoType ammoType = (AmmoType) ((AmmoStorage) partToBuy).getType();
            if(ammoType.getKgPerShot() > 0) {
                this.tonnagePerItem = ammoType.getKgPerShot() / 1000.0;
            } else {
                this.tonnagePerItem = 1.0 / ammoType.getShots();
            }
        }
        if(part instanceof Armor) {
            // Armor needs different tonnage values
            this.tonnagePerItem = 1.0 / ((Armor) part).getArmorPointsPerTon();
        }
        if(null == partToBuy) {
            System.err.println(String.format("Registeing part without a corresponding acquisition work: %s", part.getPartName())); //$NON-NLS-1$
        } else {
            this.cost = partToBuy.getBuyCost();
        }
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
