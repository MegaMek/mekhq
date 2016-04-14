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
public class AmmoWorkUnit extends WorkUnit {
    public static WorkUnit newLoad(AmmoType type, int shots) {
        if(shots < 0) {
            throw new IllegalArgumentException("Amount of shots has to be positive"); //$NON-NLS-1$
        }
        return new AmmoWorkUnit(Objects.requireNonNull(type), shots, true);
    }
    
    public static WorkUnit newUnload(AmmoType type, int shots) {
        if(shots < 0) {
            throw new IllegalArgumentException("Amount of shots has to be positive"); //$NON-NLS-1$
        }
        return new AmmoWorkUnit(Objects.requireNonNull(type), shots, false);
    }

    private final AmmoType type;
    private final int shots;
    private final boolean load;
    
    private AmmoWorkUnit(AmmoType type, int shots, boolean load) {
        this.type = type;
        this.shots = shots;
        this.load = load;
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
        for(AmmoBin bin : unit.getWorkingAmmoBins()) {
            if(load) {
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
            } else {
                // See if we have any non-empty bins with that ammo type
                if((bin.getMunitionType() == type.getMunitionType())
                    && (bin.getShotsNeeded() < bin.getFullShots())) {
                    return true;
                }
            }
        }
        // No place to load anything or unload anything from
        return false;
    }

    @Override
    protected int doWork(Unit unit, Inventory inv) throws WorkException {
        int movedShots = 0;
        for(AmmoBin bin : unit.getWorkingAmmoBins()) {
            if(movedShots >= shots) {
                // We're done, nothing more to move
                break;
            }
            if(load) {
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
            } else {
                if((bin.getMunitionType() == type.getMunitionType())
                    && (bin.getShotsNeeded() < bin.getFullShots())) {
                    int shotsToMove = Math.min(bin.getFullShots() - bin.getShotsNeeded(), shots - movedShots);
                    bin.setShotsNeeded(bin.getShotsNeeded() + shotsToMove);
                    movedShots += shotsToMove;
                    continue;
                }
            }
        }
        return 0; // TODO: Work time
    }

}
