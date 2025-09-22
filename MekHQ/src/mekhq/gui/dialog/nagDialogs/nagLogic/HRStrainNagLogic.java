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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

public class HRStrainNagLogic {
    /**
     * use {@link #hasHRStrain(int)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static boolean hasAdminStrain(int hrStrain) {
        return hrStrain > 0;
    }

    /**
     * Checks whether a campaign is experiencing HR Strain.
     *
     * <p>HR Strain occurs when the HR Strain level is greater than zero.
     * This method serves as a simple utility check to determine if HR Strain exists in the campaign based on the
     * provided strain level.</p>
     *
     * @param hrStrain The HR Strain level of the campaign. A value greater than 0 indicates the presence of HR Strain.
     *
     * @return {@code true} if HR Strain is present (i.e., {@code hrStrain > 0}). {@code false} otherwise.
     */
    public static boolean hasHRStrain(int hrStrain) {
        return hrStrain > 0;
    }

    @Deprecated(since = "0.50.07", forRemoval = true)
    public static class AdminStrainNagLogic extends HRStrainNagLogic {
        public AdminStrainNagLogic() {}
    }
}
