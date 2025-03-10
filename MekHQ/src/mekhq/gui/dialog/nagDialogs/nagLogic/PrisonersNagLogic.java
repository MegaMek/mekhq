/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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

public class PrisonersNagLogic {
    /**
     * Checks if the current campaign has prisoners of war (POWs).
     *
     * <p>
     * This method evaluates the state of the campaign to determine if there are prisoners present.
     * If the campaign does not have an active contract, the method checks the campaign's list of
     * current prisoners. If the list is not empty, the method returns {@code true}.
     * </p>
     *
     * @return {@code true} if there are prisoners in the campaign; otherwise, {@code false}.
     */
    public static boolean hasPrisoners(Campaign campaign) {
        if (!campaign.hasActiveContract()) {
            return !campaign.getCurrentPrisoners().isEmpty();
        }

        return false;
    }
}
