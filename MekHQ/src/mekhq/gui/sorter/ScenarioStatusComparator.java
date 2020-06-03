/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import mekhq.campaign.mission.Scenario;

import java.io.Serializable;
import java.util.Comparator;

public class ScenarioStatusComparator implements Comparator<String>, Serializable {
    private static final long serialVersionUID = -6287998488809978029L;

    @Override
    public int compare(String o1, String o2) {
        int a = -1;
        int b = -1;

        // First we need to determine the numbers based on the name
        for (int i = 0; i < Scenario.S_NUM; i++) {
            if (Scenario.getStatusName(i).equals(o1)) {
                a = i;
            } else if (Scenario.getStatusName(i).equals(o2)) {
                b = i;
            }
        }

        // Now we need to fix the references to Defeat and Draw so they sort nicely
        switch (a) {
            case 3:
                a = 5;
                break;
            case 5:
                a = 3;
                break;
        }

        switch (b) {
            case 3:
                b = 5;
                break;
            case 5:
                b = 3;
                break;
        }

        // Then we just subtract b from a to determine the sort order
        return a - b;
    }
}
