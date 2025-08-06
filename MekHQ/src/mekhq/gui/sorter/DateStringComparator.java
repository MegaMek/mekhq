/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

import mekhq.MekHQ;

public class DateStringComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        if (Objects.equals(o1, o2)) {
            return 0;
        } else if ("-".equals(o1)) {
            return -1;
        } else if ("-".equals(o2)) {
            return 1;
        }

        LocalDate dateA;
        LocalDate dateB;
        try {
            dateA = MekHQ.getMHQOptions().parseDisplayFormattedDate(o1);
        } catch (Exception ignored) {
            return -1;
        }

        try {
            dateB = MekHQ.getMHQOptions().parseDisplayFormattedDate(o2);
        } catch (Exception ignored) {
            return 1;
        }

        return dateA.compareTo(dateB);
    }
}
