/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum PlanetaryAcquisitionFactionLimit {
    //region Enum Declarations
    ALL("PlanetaryAcquisitionFactionLimit.ALL.text"),
    NEUTRAL("PlanetaryAcquisitionFactionLimit.NEUTRAL.text"),
    ALLIED("PlanetaryAcquisitionFactionLimit.ALLIED.text"),
    SELF("PlanetaryAcquisitionFactionLimit.SELF.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Campaign", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PlanetaryAcquisitionFactionLimit(final String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
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
    //endregion Boolean Comparison Methods

    //region File I/O
    public static PlanetaryAcquisitionFactionLimit parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return ALL;
                case 2:
                    return ALLIED;
                case 3:
                    return SELF;
                case 1:
                default:
                    return NEUTRAL;
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error("Unable to parse a PlanetaryAcquisitionFactionLimit from " + text
                + ". Returning NEUTRAL");

        return NEUTRAL;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
