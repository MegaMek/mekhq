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

/**
 *
 * @author Dylan Myers
 *         Comparator for comparing details in the warehouse and parts store
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
