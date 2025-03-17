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

public class InsufficientAstechsNagLogic {
    /**
     * Determines whether additional astechs are needed in the campaign.
     *
     * <p>This method checks if the number of astechs required is greater than zero. If so, this
     * indicates a need for additional astechs to meet the campaign's requirements. Otherwise, no
     * additional astechs are required.</p>
     *
     * @param asTechsNeeded The number of astechs currently required to meet the campaign's needs.
     * @return {@code true} if the number of required astechs ({@code asTechsNeeded}) is greater
     *         than zero; {@code false} otherwise.
     */
    public static boolean hasAsTechsNeeded(int asTechsNeeded) {
        return asTechsNeeded > 0;
    }
}
