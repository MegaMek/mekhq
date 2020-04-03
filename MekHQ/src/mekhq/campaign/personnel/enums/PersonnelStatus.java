/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum PersonnelStatus {
    //region Enum Declarations
    ACTIVE("PersonnelStatus.ACTIVE.text"),
    RETIRED("PersonnelStatus.RETIRED.text"),
    KIA("PersonnelStatus.KIA.text"),
    MIA("PersonnelStatus.MIA.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String statusName;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PersonnelStatus(String status) {
        this.statusName = resources.getString(status);
    }
    //endregion Constructors

    /**
     * @return the name of the status
     */
    public String getStatusName() {
        return statusName;
    }
}
