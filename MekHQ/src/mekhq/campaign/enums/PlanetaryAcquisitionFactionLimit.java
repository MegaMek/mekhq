/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.enums;

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum PlanetaryAcquisitionFactionLimit {
    // region Enum Declarations
    ALL("PlanetaryAcquisitionFactionLimit.ALL.text"),
    NEUTRAL("PlanetaryAcquisitionFactionLimit.NEUTRAL.text"),
    ALLIED("PlanetaryAcquisitionFactionLimit.ALLIED.text"),
    SELF("PlanetaryAcquisitionFactionLimit.SELF.text");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    // endregion Variable Declarations

    // region Constructors
    PlanetaryAcquisitionFactionLimit(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Boolean Comparison Methods
    public boolean isAll() {
        return this == ALL;
    }

    public boolean isNeutral() {
        return this == NEUTRAL;
    }

    public boolean isAllied() {
        return this == ALLIED;
    }

    public boolean isSelf() {
        return this == SELF;
    }

    public boolean generateOnEnemyPlanets() {
        return isAll();
    }

    public boolean generateOnNeutralPlanets() {
        return generateOnEnemyPlanets() || isNeutral();
    }

    public boolean generateOnAlliedPlanets() {
        return generateOnNeutralPlanets() || isAllied();
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    public static PlanetaryAcquisitionFactionLimit parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            return switch (Integer.parseInt(text)) {
                case 0 -> ALL;
                case 2 -> ALLIED;
                case 3 -> SELF;
                default -> NEUTRAL;
            };
        } catch (Exception ignored) {

        }

        MMLogger.create(PlanetaryAcquisitionFactionLimit.class)
              .error("Unable to parse {} into a PlanetaryAcquisitionFactionLimit. Returning NEUTRAL.", text);
        return NEUTRAL;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
