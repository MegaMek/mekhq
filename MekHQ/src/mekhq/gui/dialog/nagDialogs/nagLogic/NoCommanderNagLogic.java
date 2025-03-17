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

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;

public class NoCommanderNagLogic {
    /**
     * Determines whether a campaign does not have a flagged commander assigned.
     *
     * <p>This method checks if the provided flagged commander is {@code null}, which indicates
     * that no commander has been assigned to the campaign.</p>
     *
     * @param flaggedCommander The {@link Person} designated as the flagged commander, or {@code null}
     *                         if no commander is assigned.
     * @return {@code true} if no flagged commander is assigned ({@code flaggedCommander} is {@code null}),
     *         {@code false} otherwise.
     */
    public static boolean hasNoCommander(@Nullable Person flaggedCommander) {
        return flaggedCommander == null;
    }
}
