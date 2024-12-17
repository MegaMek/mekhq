/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.utilities;

import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

import java.util.UUID;

/**
 * Utility class that provides helper methods for working with entities.
 */
public class EntityUtilities {
    /**
     * Retrieves an {@link Entity} associated with a specific unit ID.
     *
     * <p>This method attempts to find a {@link Unit} in the given {@link Campaign} using the provided
     * unit ID. If the unit exists, the method returns the associated {@link Entity}. If the unit
     * does not exist or has no associated entity, the method returns {@code null}.
     *
     * @param campaign The {@link Campaign} instance from which to retrieve the {@link Unit}.
     * @param unitID The {@link UUID} of the unit for which the associated {@link Entity} is requested.
     * @return The {@link Entity} associated with the specified unit ID, or {@code null} if the unit
     *         is not found or has no associated entity.
     */
    public static @Nullable Entity getEntityFromUnitId(Campaign campaign, UUID unitID) {
        Unit unit = campaign.getUnit(unitID);

        if (unit == null) {
            return null;
        }

        return unit.getEntity();
    }
}
