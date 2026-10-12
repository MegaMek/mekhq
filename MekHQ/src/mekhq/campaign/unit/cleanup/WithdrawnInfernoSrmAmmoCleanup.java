/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.cleanup;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.parts.InfantryAmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.InfantryAmmoBin;
import mekhq.campaign.parts.equipment.MissingInfantryAmmoBin;
import mekhq.campaign.unit.Unit;

/**
 * Removes Inferno ammo for infantry SRM launchers from campaigns saved before MegaMek withdrew it.
 *
 * <p>The TechManual pp. 350-352 errata deletes the SRM Launcher (Inferno Ammo) rows, so a small support vehicle with
 * an infantry SRM launcher no longer carries a second, Inferno ammo bin. MegaMek used to add that bin's mount right
 * after the standard one; without it every later equipment number on the vehicle moves down by one. Left in place,
 * the old Inferno bin has no mount of its own, and the equipment unscrambler can pair it with another weapon's
 * ammo.</p>
 *
 * <p>This runs on campaign load, before the unscrambler. Each old Inferno bin is removed, any ammo in it goes back to
 * the warehouse as standard ammo for the same launcher (it is priced per launcher, so its value is unchanged), and
 * its clips go back to the standard bin it was split from. Inferno ammo for these launchers in a warehouse becomes
 * standard ammo the same way.</p>
 */
public final class WithdrawnInfernoSrmAmmoCleanup {
    private static final MMLogger LOGGER = MMLogger.create(WithdrawnInfernoSrmAmmoCleanup.class);

    private WithdrawnInfernoSrmAmmoCleanup() {
    }

    /**
     * @param ammoType   the munition in a bin or warehouse stack
     * @param weaponType the launcher the ammo is for
     *
     * @return {@code true} if this is Inferno ammo for an infantry SRM launcher, which no longer exists
     */
    static boolean isWithdrawnInfernoAmmo(@Nullable AmmoType ammoType, @Nullable InfantryWeapon weaponType) {
        return (ammoType != null)
              && (weaponType != null)
              && weaponType.hasFlag(WeaponType.F_SRM)
              && !weaponType.hasInfernoAmmo()
              && ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO);
    }

    /**
     * Removes the unit's withdrawn Inferno SRM ammo bins. Must run before the equipment unscrambler, while each bin
     * still holds the equipment number it was saved with.
     *
     * @param unit the unit being loaded
     */
    public static void cleanUnit(Unit unit) {
        List<Part> withdrawnBins = new ArrayList<>();
        for (Part part : unit.getParts()) {
            if ((part instanceof InfantryAmmoBin bin) && isWithdrawnInfernoAmmo(bin.getType(), bin.getWeaponType())) {
                withdrawnBins.add(part);
            } else if ((part instanceof MissingInfantryAmmoBin missingBin)
                  && isWithdrawnInfernoAmmo(missingBin.getType(), missingBin.getWeaponType())) {
                withdrawnBins.add(part);
            }
        }

        for (Part withdrawnBin : withdrawnBins) {
            int equipmentNumber;
            int clips;
            if (withdrawnBin instanceof InfantryAmmoBin infernoBin) {
                returnAsStandardAmmo(infernoBin);
                equipmentNumber = infernoBin.getEquipmentNum();
                clips = infernoBin.getClips();
            } else {
                MissingInfantryAmmoBin missingInfernoBin = (MissingInfantryAmmoBin) withdrawnBin;
                equipmentNumber = missingInfernoBin.getEquipmentNum();
                clips = missingInfernoBin.getClips();
            }
            restoreStandardBinCapacity(unit, equipmentNumber, clips);

            unit.removePart(withdrawnBin);
            LocalWarehouse warehouse = withdrawnBin.getWarehouse();
            if (warehouse != null) {
                warehouse.removePart(withdrawnBin);
            }
            LOGGER.info("[WithdrawnInfernoSrm] Removed Inferno SRM ammo bin (equipment {}, {} clips) from {}",
                  equipmentNumber, clips, unit.getName());
        }
    }

    /**
     * Turns any Inferno ammo for infantry SRM launchers held in a warehouse into standard ammo for the same launcher.
     *
     * @param warehouse the warehouse to check
     */
    public static void cleanWarehouse(LocalWarehouse warehouse) {
        List<InfantryAmmoStorage> withdrawnStock = new ArrayList<>();
        for (Part part : warehouse.getParts()) {
            if ((part instanceof InfantryAmmoStorage storage)
                  && isWithdrawnInfernoAmmo(storage.getType(), storage.getWeaponType())) {
                withdrawnStock.add(storage);
            }
        }

        for (InfantryAmmoStorage storage : withdrawnStock) {
            int shots = storage.getShots();
            InfantryWeapon weaponType = storage.getWeaponType();
            warehouse.removePart(storage);
            storage.getCampaign().getQuartermaster().addAmmo(warehouse, standardAmmo(), weaponType, shots);
            LOGGER.info("[WithdrawnInfernoSrm] Converted {} Inferno shots for {} to standard ammo",
                  shots, weaponType.getName());
        }
    }

    private static void returnAsStandardAmmo(InfantryAmmoBin infernoBin) {
        // Not getShotsNeeded(): that compares against the bin's mount, and after the renumbering the old equipment
        // number may point at another weapon's ammo, which would make the loaded shots read as zero.
        int loadedShots = Math.max(0, infernoBin.getLoadedShots());
        LocalWarehouse warehouse = infernoBin.getWarehouse();
        if ((loadedShots > 0) && (warehouse != null)) {
            infernoBin.getCampaign().getQuartermaster().addAmmo(warehouse, standardAmmo(), infernoBin.getWeaponType(),
                  loadedShots);
            LOGGER.info("[WithdrawnInfernoSrm] Returned {} loaded Inferno shots for {} as standard ammo",
                  loadedShots, infernoBin.getWeaponType().getName());
        }
    }

    /**
     * MegaMek added the Inferno mount directly after its standard mount, so the standard bin is the one saved with the
     * equipment number just before the Inferno bin.
     */
    private static void restoreStandardBinCapacity(Unit unit, int infernoEquipmentNumber, int infernoClips) {
        for (Part part : unit.getParts()) {
            if ((part instanceof InfantryAmmoBin standardBin)
                  && (standardBin.getEquipmentNum() == (infernoEquipmentNumber - 1))
                  && !isWithdrawnInfernoAmmo(standardBin.getType(), standardBin.getWeaponType())) {
                standardBin.changeCapacity(standardBin.getClips() + infernoClips);
                LOGGER.info("[WithdrawnInfernoSrm] Standard bin for {} on {} restored to {} clips",
                      standardBin.getWeaponType().getName(), unit.getName(), standardBin.getClips());
                return;
            }
        }
        LOGGER.warn("[WithdrawnInfernoSrm] No standard bin at equipment {} on {}; {} clips not restored",
              infernoEquipmentNumber - 1, unit.getName(), infernoClips);
    }

    private static AmmoType standardAmmo() {
        return (AmmoType) EquipmentType.get(EquipmentTypeLookup.INFANTRY_AMMO);
    }
}
