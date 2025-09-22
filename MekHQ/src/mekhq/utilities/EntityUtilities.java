/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.utilities;

import java.util.UUID;

import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.unit.Unit;

/**
 * Utility class that provides helper methods for working with entities.
 */
public class EntityUtilities {
    /**
     * Retrieves an {@link Entity} associated with a specific unit ID.
     *
     * <p>This method attempts to find a {@link Unit} in the given {@link Campaign} using the provided
     * unit ID. If the unit exists, the method returns the associated {@link Entity}. If the unit does not exist or has
     * no associated entity, the method returns {@code null}.
     *
     * @param hangar The {@link Hangar} instance from which to retrieve the {@link Unit}.
     * @param unitID The {@link UUID} of the unit for which the associated {@link Entity} is requested.
     *
     * @return The {@link Entity} associated with the specified unit ID, or {@code null} if the unit is not found or has
     *       no associated entity.
     */
    public static @Nullable Entity getEntityFromUnitId(Hangar hangar, UUID unitID) {
        Unit unit = hangar.getUnit(unitID);

        if (unit == null) {
            return null;
        }

        return unit.getEntity();
    }

    /**
     * Checks if the given entity is unsupported in MekHQ.
     *
     * <p>This method evaluates whether the specified entity is considered unsupported.
     * Currently, it checks if the unit type matches {@link UnitType#GUN_EMPLACEMENT} or if the entity uses drone OS
     * {@link Entity#hasDroneOs()}.
     *
     * @param entity The entity to be checked
     *
     * @return {@code true} if the entity is unsupported (e.g., a {@link UnitType#GUN_EMPLACEMENT}), otherwise
     *       {@code false}.
     */
    public static boolean isUnsupportedEntity(Entity entity) {
        return entity.getUnitType() == UnitType.GUN_EMPLACEMENT
                     || entity.hasDroneOs();
    }
}
