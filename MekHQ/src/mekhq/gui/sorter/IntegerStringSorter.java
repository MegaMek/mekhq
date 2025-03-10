/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
import java.util.Objects;

public class IntegerStringSorter implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        if (Objects.equals(o1, o2)) {
            return 0;
        } else if ("-".equals(o1)) {
            return -1;
        } else if ("-".equals(o2)) {
            return 1;
        } else {
            int i1;
            try {
                i1 = Integer.parseInt(o1);
            } catch (Exception ignored) {
                return -1;
            }

            int i2;
            try {
                i2 = Integer.parseInt(o2);
            } catch (Exception ignored) {
                return 1;
            }

            return Integer.compare(i1, i2);
        }
    }
}
