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
package mekhq.campaign.unit.enums;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * The crew seat a {@link Person} holds in a {@link Unit}.
 *
 * <p>Each seat matches one of the unit's crew assignment methods, so a seat that was recorded when a character left
 * a unit can be given back to them on their return.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum CrewSeat {
    /** Both driver and gunner: a solo pilot, or a soldier in an infantry or battle armor unit. */
    PILOT_OR_SOLDIER,
    DRIVER,
    GUNNER,
    VESSEL_CREW,
    NAVIGATOR,
    /** The tech officer, which also holds a vehicle's command console commander. */
    TECH_OFFICER;

    /**
     * Parses a seat from its saved name.
     *
     * @param text the saved name
     *
     * @return the seat, or {@code null} if the text does not name one
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable CrewSeat parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception exception) {
            MMLogger.create(CrewSeat.class).error(exception, "Unable to parse {} into a CrewSeat. Returning null.",
                  text);
            return null;
        }
    }
}
