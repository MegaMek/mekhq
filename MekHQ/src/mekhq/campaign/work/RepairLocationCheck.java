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
package mekhq.campaign.work;

import megamek.common.annotations.Nullable;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;

/**
 * Whether a tech is where a repair task is. A tech can only work on a unit at the tech's own base, ship or transit
 * group, and on a spare in the warehouse they are at. Both the repair itself and Mass Repair ask this question here,
 * so Mass Repair never picks a tech the repair would then refuse.
 */
public final class RepairLocationCheck {
    private RepairLocationCheck() {}

    /**
     * @param tech     the tech
     * @param partWork the task
     *
     * @return {@code true} if the tech is at the same place as the task's unit, or as the spare when it is not on a
     *       unit
     */
    public static boolean isTechAtTask(Person tech, IPartWork partWork) {
        return LocationUtils.areSameEffectiveLocation(tech, findWorkSite(partWork));
    }

    /**
     * @return the unit the task is on, or the task itself for a spare in a warehouse; {@code null} if the task has no
     *       place of its own
     */
    private static @Nullable ILocation findWorkSite(IPartWork partWork) {
        if ((partWork instanceof Part part) && (part.getUnit() != null)) {
            return part.getUnit();
        }
        return (partWork instanceof ILocation location) ? location : null;
    }
}
