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

public class NoCommanderNagLogic {
    /**
     * Checks if the campaign has no assigned commander.
     *
     * <p>
     * This method determines whether the campaign has a flagged commander assigned or not.
     * If {@code campaign.getFlaggedCommander()} returns {@code null}, it indicates
     * that no commander has been assigned.
     * </p>
     *
     * @return {@code true} if the campaign has no flagged commander
     * ({@code campaign.getFlaggedCommander()} is {@code null}); {@code false} otherwise.
     */
    public static boolean hasNoCommander(Campaign campaign) {
        return campaign.getFlaggedCommander() == null;
    }
}
