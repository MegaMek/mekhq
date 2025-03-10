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
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;

import java.time.LocalDate;

public class InvalidFactionNagLogic {
    /**
     * Checks whether the campaign's faction is invalid for the current in-game date.
     *
     * <p>
     * This method retrieves the campaign's faction using {@link Campaign#getFaction()} and evaluates
     * its validity for the current date using {@link Faction#validIn(LocalDate)}. A faction is considered
     * invalid if it is not valid for the campaign's local date.
     * </p>
     *
     * @return {@code true} if the faction is invalid for the campaign's current date; otherwise,
     * {@code false}.
     */
    public static boolean isFactionInvalid(Campaign campaign) {
        Faction campaignFaction = campaign.getFaction();
        LocalDate today = campaign.getLocalDate();

        return !campaignFaction.validIn(today);
    }
}
