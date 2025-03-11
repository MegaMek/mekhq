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
