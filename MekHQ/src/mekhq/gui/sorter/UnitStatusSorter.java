/*
 * Copyright (c) 2013-2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.sorter;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator for unit status strings
 * @author Jay Lawson
 */
public class UnitStatusSorter implements Comparator<String>, Serializable {
    private static final long serialVersionUID = 7404736496063859617L;

    @Override
    public int compare(String s0, String s1) {
        //probably easiest to turn into numbers and then sort that way
        int l0 = getDamageStateIndex(s0);
        int l1 = getDamageStateIndex(s1);

        return ((Comparable<Integer>) l0).compareTo(l1);
    }

    public static int getDamageStateIndex(String damageState) {
        int idx = 0;

        if (damageState.contains("Mothballed")) {
            idx = 1;
        } else if (damageState.contains("Mothballing")) {
            idx = 2;
        } else if (damageState.contains("Activating")) {
            idx = 3;
        } else if (damageState.contains("In Transit")) {
            idx = 4;
        } else if (damageState.contains("Refitting")) {
            idx = 5;
        } else if (damageState.contains("Deployed")) {
            idx = 6;
        } else if (damageState.contains("Salvage")) {
            idx = 7;
        } else if (damageState.contains("Inoperable")) {
            idx = 8;
        } else if (damageState.contains("Crippled")) {
            idx = 9;
        } else if (damageState.contains("Heavy")) {
            idx = 10;
        } else if (damageState.contains("Moderate")) {
            idx = 11;
        } else if (damageState.contains("Light")) {
            idx = 12;
        } else if (damageState.contains("Undamaged")) {
            idx = 13;
        }

        return idx;
    }
}
