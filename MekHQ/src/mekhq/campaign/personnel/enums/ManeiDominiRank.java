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

public enum ManeiDominiRank {
    // region Enum Declarations
    NONE("ManeiDominiRank.NONE.text"),
    ALPHA("ManeiDominiRank.ALPHA.text"),
    BETA("ManeiDominiRank.BETA.text"),
    OMEGA("ManeiDominiRank.OMEGA.text"),
    TAU("ManeiDominiRank.TAU.text"),
    DELTA("ManeiDominiRank.DELTA.text"),
    SIGMA("ManeiDominiRank.SIGMA.text"),
    OMICRON("ManeiDominiRank.OMICRON.text");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    // endregion Variable Declarations

    // region Constructors
    ManeiDominiRank(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isAlpha() {
        return this == ALPHA;
    }

    public boolean isBeta() {
        return this == BETA;
    }

    public boolean isOmega() {
        return this == OMEGA;
    }

    public boolean isTau() {
        return this == TAU;
    }

    public boolean isDelta() {
        return this == DELTA;
    }

    public boolean isSigma() {
        return this == SIGMA;
    }

    public boolean isOmicron() {
        return this == OMICRON;
    }
    // endregion Boolean Comparison Methods

    public static ManeiDominiRank parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {
            MMLogger.create(ManeiDominiRank.class)
                  .error(ignored, "Unable to parse " + text + "into a ManeiDominiRank. Returning NONE.");
            return NONE;

        }

    }

    @Override
    public String toString() {
        return name;
    }
}
