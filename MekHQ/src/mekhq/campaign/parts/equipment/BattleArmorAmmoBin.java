/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts.equipment;

import megamek.common.CriticalSlot;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.PartInventory;

/**
 * Battle Armor ammo bins need to look for shots for all the remaining troopers in the squad.
 * TODO: Think about how to handle the case of understrength squads. Right now
 * they
 * pay for more ammo than they need, but this is easier than trying to track
 * ammo per suit
 * and adjust for different ammo types when suits are added and removed from
 * squads.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class BattleArmorAmmoBin extends AmmoBin {
    private static final MMLogger LOGGER = MMLogger.create(BattleArmorAmmoBin.class);

    public BattleArmorAmmoBin() {
        this(0, null, -1, 0, false, null);
    }

    public BattleArmorAmmoBin(int tonnage, @Nullable AmmoType et, int equipNum,
          int shots, boolean singleShot, @Nullable Campaign c) {
        super(tonnage, et, equipNum, shots, singleShot, false, c);
    }

    @Override
    public BattleArmorAmmoBin clone() {
        BattleArmorAmmoBin clone = new BattleArmorAmmoBin(getUnitTonnage(), getType(), getEquipmentNum(), shotsNeeded,
              isOneShot(),
              campaign);
        clone.copyBaseData(this);
        clone.shotsNeeded = this.shotsNeeded;
        return clone;
    }

    public int getNumTroopers() {
        if (null != unit && unit.getEntity() instanceof BattleArmor) {
            // we are going to base this on the full squad size, even though this makes
            // understrength
            // squads overpay for their ammo - that way suits can be moved around without
            // having to adjust
            // ammo - Tech: "oh you finally got here. Check in the back corner, we
            // stockpiled some ammo for
            // you."
            return ((BattleArmor) unit.getEntity()).getSquadSize();
        }
        return 0;
    }

    // No salvaging of BA parts
    @Override
    public boolean isSalvaging() {
        return false;
    }

    @Override
    protected int getCurrentShots() {
        Mounted<?> mounted = getMounted();
        if (mounted != null) {
            // Replace with actual entity values if entity not null because
            // the previous number will not be correct for ammo swaps
            return mounted.getBaseShotsLeft() * getNumTroopers();
        }

        return (getFullShots() * getNumTroopers()) - shotsNeeded;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        Mounted<?> mounted = getMounted();
        if ((mounted != null) && !ammoTypeChanged()) {
            // Same ammo type, just a reload
            shotsNeeded = (getFullShots() - mounted.getBaseShotsLeft()) * getNumTroopers();
        } else {
            // We have a change of munitions
            shotsNeeded = getFullShots() * getNumTroopers();
        }
    }

    @Override
    public int getBaseTime() {
        return ammoTypeChanged() ? 30 : 15;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        Mounted<?> mounted = getMounted();
        if (mounted != null) {
            mounted.setHit(false);
            mounted.setDestroyed(false);
            mounted.setRepairable(true);
            getUnit().repairSystem(CriticalSlot.TYPE_EQUIPMENT, equipmentNum);
            mounted.setShotsLeft(getFullShots() - (shotsNeeded / getNumTroopers()));
        }
    }

    /**
     * Requisition ammo for this bin and remove it from the warehouse. Only allow Battle Armor Ammo bins to be loaded
     * in
     * <code>getNumTroopers()</code> bins at a time.
     *
     * @see #getNumTroopers()
     */
    @Override
    public void loadBin() {
        AmmoMounted mounted = (AmmoMounted) getMounted();
        if (mounted != null) {

            // Calculate the actual shots needed
            int shotsPerTrooper = shotsNeeded / getNumTroopers();
            int shotsToReload = Math.min(shotsPerTrooper,
                  (int) Math.floor((double) getAmountAvailable() / getNumTroopers()));
            for (int shotsPerSuitLoaded = 0; shotsPerSuitLoaded < shotsToReload; shotsPerSuitLoaded++) {
                int shots = requisitionAmmo(getType(), getNumTroopers());
                shotsNeeded -= shots;
            }

            if (!ammoTypeChanged()) {
                // Just a simple reload
                mounted.setShotsLeft(mounted.getBaseShotsLeft() + shotsToReload);
            } else {
                // Loading a new type of ammo
                unload();
                mounted.changeAmmoType(getType());
                mounted.setShotsLeft(mounted.getBaseShotsLeft() + shotsToReload);
            }
        }
    }

    @Override
    public void unload() {
        int shots = 0;

        AmmoType curType = getType();
        Mounted<?> mounted = getMounted();
        if (mounted != null) {
            shots = mounted.getBaseShotsLeft() * getNumTroopers();
            mounted.setShotsLeft(0);
            curType = (AmmoType) mounted.getType();
        }

        shotsNeeded = getFullShots() * getNumTroopers();
        returnAmmo(curType, shots);
    }

    @Override
    public @Nullable String checkFixable() {
        int amountAvailable = getAmountAvailable();
        if ((amountAvailable > 0) && (amountAvailable < getNumTroopers())) {
            return "Cannot do a partial reload of Battle Armor ammo less than the number of troopers";
        }
        return super.checkFixable();
    }

    @Override
    public void remove(boolean salvage) {
        // shouldn't be here
    }

    @Override
    public AmmoStorage getNewPart() {
        return new AmmoStorage(1, getType(), calculateShots(), campaign);
    }

    @Override
    public String getAcquisitionDesc() {
        String toReturn = "<html><font";

        toReturn += ">";
        toReturn += "<b>" + getAcquisitionDisplayName() + "</b> " + getAcquisitionBonus() + "<br/>";
        toReturn += getAcquisitionExtraDesc() + "<br/>";
        PartInventory inventories = campaign.getPartInventory(getAcquisitionPart());
        toReturn += inventories.getTransitOrderedDetails() + "<br/>";
        toReturn += getBuyCost().toAmountAndSymbolString() + "<br/>";
        toReturn += "</font></html>";
        return toReturn;
    }

    protected int calculateShots() {
        int shots = (int) Math.floor(1000 / getType().getKgPerShot());
        if (shots <= 0) {
            // FIXME: no idea what to do here, these really should be fixed on the MM side because presumably this is
            //  happening because KgperShot is -1 or 0
            shots = 20;
        }

        return shots;
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return calculateShots() + " shots";
    }

    @Override
    public boolean canNeverScrap() {
        return true;
    }

    /**
     * Restores the equipment from the name
     */
    @Override
    public void restore() {
        if (typeName == null) {
            typeName = getType().getName();
        } else {
            type = EquipmentType.get(typeName);
        }

        // FIXME, this is a crappy hack, but we want something along these lines
        // to make sure that BA ammo gets removed from all parts - It might be better to
        // run
        // a check on the XML loading after restore - we also will need to to the same
        // for proto
        // ammo but we can only do this if we have all the correct ammo rack sizes for
        // the
        // generics (e.g. LRM1, LRM2, LRM3, etc)
        /*
         * if (typeName.contains("BA-")) {
         * String newTypeName = "IS" + typeName.split("BA-")[1];
         * EquipmentType newType = EquipmentType.get(newTypeName);
         * if (null != newType) {
         * typeName = newTypeName;
         * type = newType;
         * }
         * }
         */

        if (type == null) {
            LOGGER.error("Mounted.restore: could not restore equipment type \"{}\"", typeName);
            return;
        }
        try {
            equipTonnage = type.getTonnage(null);
        } catch (NullPointerException e) {
            LOGGER.error("", e);
        }
    }
}
