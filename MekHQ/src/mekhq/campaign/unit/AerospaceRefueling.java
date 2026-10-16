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

import jakarta.annotation.Nullable;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.logging.MMLogger;

/**
 * Fills the fuel tanks of aerospace units, LAMs included, when they come back from a battle. The fuel itself is paid
 * for by the monthly fuel cost, so refuelling is free.
 */
public final class AerospaceRefueling {
    private static final MMLogger LOGGER = MMLogger.create(AerospaceRefueling.class);

    private AerospaceRefueling() {
    }

    /**
     * Fills the unit's tanks to its design capacity. Without this, fuel burned in a battle is never put back, and once
     * the campaign is saved the lower amount also becomes the unit's capacity.
     *
     * @param entity       the unit's entity; a unit that carries no fuel is left alone
     * @param designEntity a fresh copy of the unit read from its unit file, or {@code null} when the unit cache has no
     *                     file for it, in which case the unit's own capacity is used
     */
    public static void refuel(Entity entity, @Nullable Entity designEntity) {
        if (!(entity instanceof IAero aero)) {
            return;
        }
        int fullFuel = (designEntity instanceof IAero designAero) ? designAero.getFuel() : aero.getFuel();
        int fuelBefore = aero.getCurrentFuel();
        aero.setFuel(fullFuel);
        LOGGER.debug("[Refuel] {}: refuelled from {} to {} fuel points{}", entity.getShortName(), fuelBefore, fullFuel,
              (designEntity == null) ? " (no unit file, kept its own capacity)" : "");
    }
}
