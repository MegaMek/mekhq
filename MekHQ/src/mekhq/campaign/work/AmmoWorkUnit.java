package mekhq.campaign.work;

import java.util.Objects;

import megamek.common.AmmoType;
import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.inventory.Inventory;
import mekhq.campaign.inventory.Item;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;

/**
 * A work unit representing loading or unloading of ammunition. This can load or unload
 * less then the required amount if there is not enough space or not enough loaded ammo,
 * but it will never move more. Thus, asking for <code>Integer.MAX_VALUE</code> ammo
 * count is valid and means "as much as possible".
 */
public abstract class AmmoWorkUnit extends WorkUnit {
    public static WorkUnit newLoad(AmmoType type, int shots) {
        if(shots < 0) {
            throw new IllegalArgumentException("Amount of shots has to be positive"); //$NON-NLS-1$
        }
        return new Load(Objects.requireNonNull(type), shots);
    }
    
    public static WorkUnit newUnload(AmmoType type, int shots) {
        if(shots < 0) {
            throw new IllegalArgumentException("Amount of shots has to be positive"); //$NON-NLS-1$
        }
        return new Unload(Objects.requireNonNull(type), shots);
    }

    protected final AmmoType type;
    protected final int shots;
    
    private AmmoWorkUnit(AmmoType type, int shots) {
        this.type = type;
        this.shots = shots;
    }
    
    @Override
    protected boolean canWork(Unit unit, Inventory inv) {
        if((null == unit) || (null == inv)) {
            return false;
        }
        if(shots == 0) {
            // We can always load or unload zero shots
            return true;
        }
        Item item = Item.newItem(new AmmoStorage(0, type, 1, null));
        int storedAmount = inv.getAmount(item);
        if(storedAmount < shots) {
            // Not enough items in inventory
            return false;
        }
        // Let the subclasses deal with any other requirements
        return true;
    }

    private static class Load extends AmmoWorkUnit {
        private Load(AmmoType ammoType, int shots) {
            super(ammoType, shots);
        }
        
        @Override
        protected boolean canWork(Unit unit, Inventory inv) {
            if(super.canWork(unit, inv)) {
                for(AmmoBin bin : unit.getWorkingAmmoBins()) {
                    // See if we have any bins which could accept this ammo type
                    if(bin.getShotsNeeded() > 0) {
                        if(bin.getMunitionType() == type.getMunitionType()) {
                            // Same munition
                            return true;
                        }
                        if(bin.getShotsNeeded() == bin.getFullShots()) {
                            // Bin is empty, see if we can use this munition in there
                            for (AmmoType atype :
                                Utilities.getMunitionsFor(unit.getEntity(),
                                    (AmmoType) bin.getType(), CampaignOptions.TECH_EXPERIMENTAL)) {
                                if (atype.getMunitionType() == type.getMunitionType()) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
        
        @Override
        protected int doWork(Unit unit, Inventory inv) throws WorkException {
            if(shots == 0) {
                // Easy "work"
                return 0;
            }
            int movedShots = 0;
            for(AmmoBin bin : unit.getWorkingAmmoBins()) {
                if(movedShots >= shots) {
                    // We're done, nothing more to move
                    break;
                }
                if(bin.getShotsNeeded() > 0) {
                    if(bin.getMunitionType() == type.getMunitionType()) {
                        int shotsToMove = Math.min(bin.getShotsNeeded(), shots - movedShots);
                        bin.setShotsNeeded(bin.getShotsNeeded() - shotsToMove);
                        movedShots += shotsToMove;
                        continue;
                    }
                    if(bin.getShotsNeeded() == bin.getFullShots()) {
                        boolean munitionValid = false;
                        for (AmmoType atype :
                            Utilities.getMunitionsFor(unit.getEntity(),
                                (AmmoType) bin.getType(), CampaignOptions.TECH_EXPERIMENTAL)) {
                            if (atype.getMunitionType() == type.getMunitionType()) {
                                munitionValid = true;
                            }
                        }
                        if(munitionValid) {
                            bin.changeMunition(type.getMunitionType());
                            int shotsToMove = Math.min(bin.getShotsNeeded(), shots - movedShots);
                            bin.setShotsNeeded(bin.getShotsNeeded() - shotsToMove);
                            movedShots += shotsToMove;
                            continue;
                        }
                    }
                }
            }
            return 0;
        }
    }
    
    private static class Unload extends AmmoWorkUnit {
        private Unload(AmmoType ammoType, int shots) {
            super(ammoType, shots);
        }
        
        @Override
        protected boolean canWork(Unit unit, Inventory inv) {
            if(super.canWork(unit, inv)) {
                for(AmmoBin bin : unit.getWorkingAmmoBins()) {
                    // See if we have any non-empty bins with that ammo type
                    if((bin.getMunitionType() == type.getMunitionType())
                        && (bin.getShotsNeeded() < bin.getFullShots())) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        @Override
        protected int doWork(Unit unit, Inventory inv) throws WorkException {
            if(shots == 0) {
                // Easy "work"
                return 0;
            }
            int movedShots = 0;
            for(AmmoBin bin : unit.getWorkingAmmoBins()) {
                if(movedShots >= shots) {
                    // We're done, nothing more to move
                    break;
                }
                if((bin.getMunitionType() == type.getMunitionType())
                    && (bin.getShotsNeeded() < bin.getFullShots())) {
                    int shotsToMove = Math.min(bin.getFullShots() - bin.getShotsNeeded(), shots - movedShots);
                    bin.setShotsNeeded(bin.getShotsNeeded() + shotsToMove);
                    movedShots += shotsToMove;
                    continue;
                }
            }
            return 0; // TODO: Work time
        }
    }
}
