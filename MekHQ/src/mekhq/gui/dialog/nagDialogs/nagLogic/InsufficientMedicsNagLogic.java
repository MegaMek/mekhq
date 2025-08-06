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

public class InsufficientMedicsNagLogic {
    /**
     * Determines whether additional medics are needed in the campaign.
     *
     * <p>This method checks if the number of required medics is greater than zero. If it is, this
     * indicates a need for additional medics to meet the campaign's requirements; otherwise, no additional medics are
     * required.</p>
     *
     * @param medicsRequired The number of medics currently required to meet the campaign's needs.
     *
     * @return {@code true} if the number of required medics ({@code medicsRequired}) is greater than zero,
     *       {@code false} otherwise.
     */
    public static boolean hasMedicsNeeded(int medicsRequired) {
        return medicsRequired > 0;
    }
}
