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

import java.util.Collection;

import mekhq.campaign.unit.Unit;

public class UnmaintainedUnitsNagLogic {
    /**
     * Checks if the campaign has any unmaintained units in the hangar.
     *
     * <p>This method iterates through a collection of units and identifies those that:</p>
     * <ul>
     *     <li>Are classified as unmaintained (see {@link Unit#isUnmaintained()}).</li>
     *     <li>Are not marked as salvage (see {@link Unit#isSalvage()}).</li>
     * </ul>
     * <p>If any unit matches these criteria, the method returns {@code true}; otherwise, it returns
     * {@code false}.</p>
     *
     * @param units A {@link Collection} of {@link Unit} objects representing the campaign's hangar units.
     *
     * @return {@code true} if unmaintained, non-salvage units are found in the collection, {@code false} otherwise.
     */
    public static boolean campaignHasUnmaintainedUnits(Collection<Unit> units) {
        for (Unit unit : units) {
            if ((unit.isUnmaintained()) && (!unit.isSalvage())) {
                return true;
            }
        }
        return false;
    }
}
