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
package mekhq.campaign.personnel.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

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
              MekHQ.getMHQOptions().getLocale());
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
