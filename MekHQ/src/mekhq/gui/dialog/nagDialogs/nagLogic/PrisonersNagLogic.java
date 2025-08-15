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

public class PrisonersNagLogic {
    /**
     * Determines if the current campaign has prisoners of war (POWs).
     *
     * <p>This method checks if there are prisoners present in the campaign based on the following criteria:</p>
     * <ul>
     *     <li>If there are no active contracts in the campaign, it evaluates the campaign's prisoner list.</li>
     *     <li>If the list of prisoners is not empty, the method returns {@code true}.</li>
     *     <li>If there are active contracts, the method automatically returns {@code false} (prisoners are not considered).</li>
     * </ul>
     *
     * @param hasActiveContract A flag indicating whether the campaign has an active contract.
     * @param hasPrisoners      A flag indicating whether the campaign's prisoner list is non-empty.
     *
     * @return {@code true} if there are prisoners in the campaign and no active contract; {@code false} otherwise.
     */
    public static boolean hasPrisoners(boolean hasActiveContract, boolean hasPrisoners) {
        if (hasActiveContract) {
            return false;
        }

        return hasPrisoners;
    }
}
