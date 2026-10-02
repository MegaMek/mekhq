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
package mekhq.campaign.unit;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.units.IBomber;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

/**
 * Takes every bomb off an aircraft and puts it back in the warehouse in one step, so a flight can be made "clean"
 * after a scenario without setting each bomb type to zero in the bomb dialog (issue #2604).
 */
public final class BombUnloader {
    private static final MMLogger LOGGER = MMLogger.create(BombUnloader.class);

    private BombUnloader() {}

    /**
     * @param unit the unit
     *
     * @return {@code true} if the unit can carry bombs and carries at least one
     */
    public static boolean hasBombsToUnload(Unit unit) {
        if (!(unit.getEntity() instanceof IBomber bomber)) {
            return false;
        }
        return bomber.getBombChoices().getTotalBombs() > 0;
    }

    /**
     * Returns every bomb the unit carries, internal and external, to the warehouse and leaves the unit carrying none.
     * A bomb type with no matching ammunition in the warehouse stays on the unit instead of being lost.
     *
     * @param campaign the campaign whose warehouse takes the bombs
     * @param unit     the unit to unload
     *
     * @return how many bombs were returned
     */
    public static int unloadAll(Campaign campaign, Unit unit) {
        if (!(unit.getEntity() instanceof IBomber bomber)) {
            LOGGER.debug("[BombUnloader] {} carries no bombs", unit.getName());
            return 0;
        }
        BombLoadout loadout = bomber.getBombChoices();
        // Bombs that cannot go back to the warehouse stay on the aircraft, so nothing is lost
        BombLoadout bombsKeptOnBoard = new BombLoadout();
        int bombsReturned = 0;
        for (BombTypeEnum bombType : BombTypeEnum.values()) {
            int count = loadout.getCount(bombType);
            if ((bombType == BombTypeEnum.NONE) || (count <= 0)) {
                continue;
            }
            if (EquipmentType.get(bombType.getInternalName()) instanceof AmmoType bombAmmo) {
                campaign.getQuartermaster().addAmmo(bombAmmo, count);
                bombsReturned += count;
            } else {
                LOGGER.error("[BombUnloader] no ammunition type for bomb {}; {} left on {}",
                      bombType.getInternalName(), count, unit.getName());
                bombsKeptOnBoard.put(bombType, count);
            }
        }
        bomber.clearBombChoices();
        if (!bombsKeptOnBoard.isEmpty()) {
            bomber.setBombChoices(bombsKeptOnBoard);
        }
        LOGGER.debug("[BombUnloader] {}: {} bombs returned to the warehouse", unit.getName(), bombsReturned);
        return bombsReturned;
    }
}
