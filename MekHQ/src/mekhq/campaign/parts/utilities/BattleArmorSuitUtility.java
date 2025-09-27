/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.utilities;

import java.util.List;

import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.battleValue.BVCalculator;
import megamek.common.battleValue.BattleArmorBVCalculator;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.parts.missing.MissingBattleArmorSuit;

/**
 * Battle Armor Suits and Missing Battle Armor Suits do not track enough information to determine if two suits with same
 * chassis but different model names are actually the same - for example, an Elemental [Flamer](Sqd5) suit being used
 * for Elemental [Flamer](Sqd3). This utility class will look up a BA part's corresponding entity and can be used to get
 * the information needed for the part/missing part to make the comparison.
 *
 * @see MissingBattleArmorSuit
 * @see mekhq.campaign.parts.BattleArmorSuit
 */
public class BattleArmorSuitUtility {
    private static final MMLogger LOGGER = MMLogger.create(BattleArmorSuitUtility.class);

    String chassis;
    String model;

    Entity entity;

    public BattleArmorSuitUtility(String chassis, String model) {
        this.chassis = chassis;
        this.model = model;

        entity = getEntity(this.chassis, this.model);
    }

    /**
     * The entity might be null if there was an exception.
     *
     * @return true if the entity exists, false if the entity is null
     */
    public boolean hasEntity() {
        return entity != null;
    }

    /**
     * The same BA chassis in different sizes should have the same suit BV
     *
     * @return int BV of the individual BA suit
     */
    public int getBattleArmorSuitBV() {
        return getBattleArmorSuitBV(entity);
    }

    /**
     * The same BA chassis in different sizes should have the same weapon type list hash. It's hashed because we don't
     * actually care about the details, we just need to compare if two BA entities have the same weapons. This should do
     * that.
     *
     * @return int the list of weapon types this BA entity has, hashed
     */
    public int getWeaponTypeListHash() {
        return getWeaponTypeListHash(entity);
    }

    private static int getWeaponTypeListHash(Entity entity) {
        List<WeaponMounted> entityWeaponList = entity.getIndividualWeaponList();
        List<WeaponType> entityWeaponTypeList = entityWeaponList.stream().map(WeaponMounted::getType).toList();
        return entityWeaponTypeList.hashCode();
    }

    private static int getBattleArmorSuitBV(Entity entity) {
        if (entity instanceof BattleArmor ba) {
            BVCalculator calc = ba.getBvCalculator();
            if (calc instanceof BattleArmorBVCalculator bvCalc) {
                return bvCalc.singleTrooperBattleValue();
            }
        }
        return 0;
    }

    /**
     * Parts don't store their entity. We can look it up in the same way that the MUL parser does, using the chassis and
     * model. This is based on the MULParser's implementation.
     */
    private static Entity getEntity(String chassis, @Nullable String model) {
        StringBuilder key = new StringBuilder(chassis);
        MekSummary ms = MekSummaryCache.getInstance().getMek(key.toString());
        if (!StringUtility.isNullOrBlank(model)) {
            key.append(" ").append(model);
            ms = MekSummaryCache.getInstance().getMek(key.toString());
            // That didn't work. Try swapping model and chassis.
            if (ms == null) {
                key = new StringBuilder(model);
                key.append(" ").append(chassis);
                ms = MekSummaryCache.getInstance().getMek(key.toString());
            }
        }
        Entity newEntity = null;
        if (ms != null) {
            try {
                newEntity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
        return newEntity;
    }
}
