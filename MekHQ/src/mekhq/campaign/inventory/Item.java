package mekhq.campaign.inventory;

import java.util.Objects;

import megamek.common.AmmoType;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.StructuralIntegrity;
import mekhq.campaign.parts.equipment.AmmoBin;

/** A class representing a physical item */
public class Item {
    private Part partClone;
    private double tonnage;
    
    /** @return an Item corresponding to the given {@link Part}, or <code>null</code> if that's not possible */
    public static Item newItem(Part part) {
        if(null == part) {
            return null;
        }
        // Lots of specific case code follows, sadly.
        if(part instanceof MissingPart) {
            // "Missing" parts are simply empty spaces with a purpose
            return null;
        }
        if(part instanceof AmmoBin) {
            // Ammo bin is simply empty space waiting for ammo (see above)
            return null;
        }
        if(part instanceof StructuralIntegrity) {
            // This simply represents the whole internal structure of a unit
            return null;
        }
        if(part instanceof Refit) {
            // Why is this even a "part?
            return null;
        }
        double tonnage = part.getTonnage();
        // Most things play nicely with getTonnage(). Some don't. *sigh*
        if(part instanceof Armor) {
            tonnage = 1.0 / ((Armor) part).getArmorPointsPerTon();
        }
        if(part instanceof AmmoStorage) {
            AmmoType ammoType = (AmmoType) ((AmmoStorage) part).getType();
            if(ammoType.getKgPerShot() > 0.0) {
                tonnage = ammoType.getKgPerShot() / 1000.0;
            } else {
                tonnage = 1.0 / ammoType.getShots();
            }
        }
        return new Item(part, tonnage);
    }
    
    private Item(Part part, double tonnage) {
        this.partClone = part.clone();
        this.tonnage = tonnage;
    }
    
    public Part getPart() {
        return partClone.clone();
    }
    
    public double getTonnage() {
        return tonnage;
    }

    @Override
    public int hashCode() {
        // TODO: Add hashCode() to Part
        return Objects.hash(partClone, tonnage);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final Item other = (Item) obj;
        // TODO: Add equals() to Part
        return tonnage == other.tonnage && Objects.equals(partClone, other.partClone);
    }
    
    @Override
    public String toString() {
        return "[ITEM:" + partClone.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
