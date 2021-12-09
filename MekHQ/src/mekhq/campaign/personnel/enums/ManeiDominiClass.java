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
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public enum ManeiDominiClass {
    //region Enum Declarations
    NONE("ManeiDominiClass.NONE.text"),
    GHOST("ManeiDominiClass.GHOST.text"),
    WRAITH("ManeiDominiClass.WRAITH.text"),
    BANSHEE("ManeiDominiClass.BANSHEE.text"),
    ZOMBIE("ManeiDominiClass.ZOMBIE.text"),
    PHANTOM("ManeiDominiClass.PHANTOM.text"),
    SPECTER("ManeiDominiClass.SPECTER.text"),
    POLTERGEIST("ManeiDominiClass.POLTERGEIST.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    ManeiDominiClass(final String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    public static ManeiDominiClass parseFromString(final String text) {
        // Parse based on the enum name
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Parse from Ordinal Int - Legacy save method
        ManeiDominiClass[] values = values();
        try {
            int mdClass = Integer.parseInt(text);
            if (values.length > mdClass) {
                return values[mdClass];
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error("Unable to parse " + text + "into a ManeiDominiClass. Returning NONE.");

        return NONE;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
