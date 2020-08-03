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

public enum FamilialConnectionType {
    //region Enum Declarations
    MARRIED("FamilialConnectionType.MARRIED.text"),
    DIVORCED("FamilialConnectionType.DIVORCED.text"),
    WIDOWED("FamilialConnectionType.WIDOWED.text"),
    PARTNER("FamilialConnectionType.PARTNER.text"),
    SINGLE_PARENT("FamilialConnectionType.SINGLE_PARENT.text"),
    ADOPTED("FamilialConnectionType.ADOPTED.text"),
    UNDEFINED("FamilialConnectionType.UNDEFINED.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String typeName;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    FamilialConnectionType(String typeName) {
        this.typeName = resources.getString(typeName);
    }
    //endregion Constructors

    public String getTypeName() {
        return typeName;
    }
}
