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

import megamek.common.EntityWeightClass;

/**
 * A comparator for unit weight classes
 * 
 * @author Jay Lawson
 */
public class WeightClassSorter implements Comparator<String> {
    @Override
    public int compare(String s0, String s1) {
        // lets find the weight class integer for each name
        int l0 = 0;
        int l1 = 0;
        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            if (EntityWeightClass.getClassName(i).equals(s0)) {
                l0 = i;
            }
            if (EntityWeightClass.getClassName(i).equals(s1)) {
                l1 = i;
            }
        }
        return ((Comparable<Integer>) l0).compareTo(l1);
    }
}
