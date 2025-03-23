/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

public class AdminStrainNagLogic {
    /**
     * Checks whether a campaign is experiencing administrative strain.
     *
     * <p>Administrative strain occurs when the administrative strain level is greater than zero.
     * This method serves as a simple utility check to determine if administrative strain
     * exists in the campaign based on the provided strain level.</p>
     *
     * @param adminStrain The administrative strain level of the campaign. A value greater than 0
     *                    indicates the presence of administrative strain.
     *
     * @return {@code true} if administrative strain is present (i.e., {@code adminStrain > 0}).
     *         {@code false} otherwise.
     */
    public static boolean hasAdminStrain(int adminStrain) {
        return adminStrain > 0;
    }
}
