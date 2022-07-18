/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

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
    private final String name;
    //endregion Variable Declarations

    //region Constructors
    FamilialConnectionType(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
    public boolean isMarried() {
        return this == MARRIED;
    }

    public boolean isDivorced() {
        return this == DIVORCED;
    }

    public boolean isWidowed() {
        return this == WIDOWED;
    }

    public boolean isPartner() {
        return this == PARTNER;
    }

    public boolean isSingleParent() {
        return this == SINGLE_PARENT;
    }

    public boolean isAdopted() {
        return this == ADOPTED;
    }

    public boolean isUndefined() {
        return this == UNDEFINED;
    }
    //endregion Boolean Comparison Methods

    @Override
    public String toString() {
        return name;
    }
}
