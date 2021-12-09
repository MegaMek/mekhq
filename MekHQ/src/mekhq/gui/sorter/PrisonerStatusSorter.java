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

import mekhq.campaign.personnel.enums.PrisonerStatus;

import java.io.Serializable;
import java.util.Comparator;

public class PrisonerStatusSorter implements Comparator<PrisonerStatus>, Serializable {
    private static final long serialVersionUID = -6785638981414529553L;

    /**
     * Order:
     * 1) Free
     * 2) Prisoners willing to Defect
     * 3) Prisoners not willing to Defect
     * 4) Bondsmen
     *
     * @param o1    the first PrisonerStatus
     * @param o2    the second PrisonerStatus
     * @return the sort order
     */
    @Override
    public int compare(PrisonerStatus o1, PrisonerStatus o2) {
        if (o1 == o2) {
            return 0;
        }

        int o1Order, o2Order;

        switch (o1) {
            case FREE:
                o1Order = 0;
                break;
            case PRISONER_DEFECTOR:
                o1Order = 1;
                break;
            case PRISONER:
                o1Order = 2;
                break;
            case BONDSMAN:
            default:
                o1Order = 3;
                break;
        }

        switch (o2) {
            case FREE:
                o2Order = 0;
                break;
            case PRISONER_DEFECTOR:
                o2Order = 1;
                break;
            case PRISONER:
                o2Order = 2;
                break;
            case BONDSMAN:
            default:
                o2Order = 3;
                break;
        }

        return o2Order - o1Order;
    }
}
