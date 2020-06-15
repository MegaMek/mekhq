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
import mekhq.campaign.parts.Part;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * This class compares two {@link Scenario} status integers
 */
public class ScenarioStatusComparator implements Comparator<String>, Serializable {
    private static final long serialVersionUID = -6287998488809978029L;

    @Override
    public int compare(String o1, String o2) {
        if (Objects.equals(o1, o2)) {
            return 0;
        }

        int a = getStatusInt(o1);
        int b = getStatusInt(o2);

        // Then we just subtract b from a to determine the sort order
        return a - b;
    }

    private int getStatusInt(String status) {
        int statusInt = -1;

        for (int i = 0; i < Scenario.S_NUM; i++) {
            if (Scenario.getStatusName(i).equals(status)) {
                statusInt = i;
                break;
            }
        }

        // Now we need to fix the references to Defeat (3) and Draw (5) so they sort nicely
        switch (statusInt) {
            case 3: // Defeat is listed as 3, but is best sorted as 5
                statusInt = 5;
                break;
            case 5: // Draw is listed as 5, but is best sorted as 3 (in the middle between Victory and Defeat)
                statusInt = 3;
                break;
        }

        return statusInt;
    }
}
