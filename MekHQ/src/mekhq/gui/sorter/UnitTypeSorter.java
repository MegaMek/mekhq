/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
