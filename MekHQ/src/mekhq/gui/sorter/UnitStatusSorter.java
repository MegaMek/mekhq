/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
 * A comparator for unit status strings
 *
 * @author Jay Lawson
 */
public class UnitStatusSorter implements Comparator<String> {

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
