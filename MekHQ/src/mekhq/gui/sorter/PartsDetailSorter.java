/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

/**
 *
 * @author Dylan Myers Comparator for comparing details in the warehouse and parts store
 */
public class PartsDetailSorter implements Comparator<String> {

    @Override
    public int compare(String s0, String s1) {
        double l0 = -1;
        double l1 = -1;
        String[] ss0 = s0.replace("<html>", "").replace("</html>", "").replace("<nobr>", "").replace("</nobr>", "")
                             .split(" ");
        String[] ss1 = s1.replace("<html>", "").replace("</html>", "").replace("<nobr>", "").replace("</nobr>", "")
                             .split(" ");
        if (!ss0[0].isEmpty()) {
            try {
                l0 = Double.parseDouble(ss0[0]);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        if (!ss1[0].isEmpty()) {
            try {
                l1 = Double.parseDouble(ss1[0]);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        s0 = "";
        s1 = "";
        if (ss0.length > 1) {
            s0 = ss0[1];
        }
        if (ss1.length > 1) {
            s1 = ss1[1];
        }
        int sComp = s0.compareTo(s1);
        if (sComp == 0) {
            return ((Comparable<Double>) l0).compareTo(l1);
        } else {
            return sComp;
        }
    }
}
