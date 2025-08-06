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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import java.time.LocalDate;

import mekhq.campaign.universe.Faction;

public class InvalidFactionNagLogic {
    /**
     * Determines whether the specified faction is invalid for the given date.
     *
     * <p>This method evaluates the validity of a faction by checking if it is applicable
     * for the specified date using {@link Faction#validIn(LocalDate)}. A faction is deemed invalid if it is not valid
     * for the provided date.</p>
     *
     * @param campaignFaction The {@link Faction} associated with the campaign to be validated.
     * @param today           The {@link LocalDate} representing the current in-game date.
     *
     * @return {@code true} if the faction is invalid for the specified date, {@code false} otherwise.
     */
    public static boolean isFactionInvalid(Faction campaignFaction, LocalDate today) {
        return !campaignFaction.validIn(today);
    }
}
