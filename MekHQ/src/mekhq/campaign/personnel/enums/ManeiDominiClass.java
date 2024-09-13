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

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum ManeiDominiClass {
    // region Enum Declarations
    NONE("ManeiDominiClass.NONE.text"),
    GHOST("ManeiDominiClass.GHOST.text"),
    WRAITH("ManeiDominiClass.WRAITH.text"),
    BANSHEE("ManeiDominiClass.BANSHEE.text"),
    ZOMBIE("ManeiDominiClass.ZOMBIE.text"),
    PHANTOM("ManeiDominiClass.PHANTOM.text"),
    SPECTER("ManeiDominiClass.SPECTER.text"),
    POLTERGEIST("ManeiDominiClass.POLTERGEIST.text");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    // endregion Variable Declarations

    // region Constructors
    ManeiDominiClass(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isGhost() {
        return this == GHOST;
    }

    public boolean isWraith() {
        return this == WRAITH;
    }

    public boolean isBanshee() {
        return this == BANSHEE;
    }

    public boolean isZombie() {
        return this == ZOMBIE;
    }

    public boolean isPhantom() {
        return this == PHANTOM;
    }

    public boolean isSpecter() {
        return this == SPECTER;
    }

    public boolean isPoltergeist() {
        return this == POLTERGEIST;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    public static ManeiDominiClass parseFromString(final String text) {
        // Parse based on the enum name
        try {
            return valueOf(text);
        } catch (Exception ignored) {
            MMLogger.create(ManeiDominiClass.class)
                    .error(ignored, "Unable to parse " + text + "into a ManeiDominiClass. Returning NONE.");
            return NONE;

        }

    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
