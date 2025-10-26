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
package mekhq.campaign.personnel.medical;

import java.util.List;

import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import mekhq.campaign.unit.Unit;

public class MASHCapacity {
    /**
     * Calculates the total patient capacity of all MASH theatres present in the given list of units.
     *
     * <p>For each unit that is eligible (not deployed, not damaged, and fully crewed), the method counts the number
     * of installed MASH theatre components and multiplies the total by the specified capacity per theatre. The returned
     * value represents the aggregate patient capacity available from all operational MASH theatres in the provided
     * units.</p>
     *
     * @param units              the list of units to scan for MASH theatres
     * @param capacityPerTheatre the patient capacity provided by a single MASH theatre
     *
     * @return the total patient capacity across all MASH theatres in the given units
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int checkMASHCapacity(List<Unit> units, int capacityPerTheatre) {
        int mashTheatreCount = 0;

        for (Unit unit : units) {
            if ((unit.isDeployed())
                      || (unit.isDamaged())
                      || (unit.getCrewState().isUncrewed())
                      || (unit.getCrewState().isPartiallyCrewed())) {
                continue;
            }

            for (MiscMounted item : unit.getEntity().getMisc()) {
                if (item.getType().hasFlag(MiscType.F_MASH)) {
                    mashTheatreCount++;
                }
            }
        }

        return mashTheatreCount * capacityPerTheatre;
    }
}
