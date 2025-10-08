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
        } catch (Exception ex) {
            MMLogger.create(ManeiDominiClass.class)
                  .error(ex, "Unable to parse {} into a ManeiDominiClass. Returning NONE.", text);
            return NONE;

        }

    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
