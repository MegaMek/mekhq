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
package mekhq.gui.sorter;

import java.util.Comparator;

import megamek.common.UnitType;

/**
 * A comparator for unit types
 *
 * @author Jay Lawson
 */
public class UnitTypeSorter implements Comparator<String> {
    @Override
    public int compare(String compare0, String compare1) {
        // lets find the weight class integer for each name
        int sort0 = 0;
        int sort1 = 0;

        // We ONLY get the strings so sorting Omni units (in this case to be after the same
        // unit type but otherwise in the same order as this) is going to be a little silly

        boolean omni0 = false;
        boolean omni1 = false;

        if (compare0.startsWith("Omni ")) {
            omni0 = true;
            compare0 = compare0.substring(5);
        } else if (compare0.startsWith("Omni")) {
            omni0 = true;
            compare0 = compare0.substring(4);
        }

        if (compare1.startsWith("Omni ")) {
            omni1 = true;
            compare1 = compare1.substring(5);
        } else if (compare1.startsWith("Omni")) {
            omni1 = true;
            compare1 = compare1.substring(4);
        }

        for (int i = 0; i <= UnitType.SPACE_STATION; i++) {
            if (UnitType.getTypeDisplayableName(i).equals(compare0)) {
                sort0 = i * 2;
                if (omni0) {
                    sort0++;
                }
            }
            if (UnitType.getTypeDisplayableName(i).equals(compare1)) {
                sort1 = i * 2;
                if (omni1) {
                    sort1++;
                }
            }
        }
        return ((Comparable<Integer>) sort1).compareTo(sort0);
    }
}
