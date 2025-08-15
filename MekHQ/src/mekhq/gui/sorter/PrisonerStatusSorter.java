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

import java.util.Comparator;

import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;

public class PrisonerStatusSorter implements Comparator<PrisonerStatus> {

    /**
     * Order: 1) Free 2) Prisoners willing to Defect 3) Prisoners not willing to Defect 4) Bondsmen
     *
     * @param o1 the first PrisonerStatus
     * @param o2 the second PrisonerStatus
     *
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
