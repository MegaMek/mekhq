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

import java.util.ResourceBundle;

public enum ManeiDominiRank {
    //region Enum Declarations
    NONE("ManeiDominiRank.NONE.text"),
    ALPHA("ManeiDominiRank.ALPHA.text"),
    BETA("ManeiDominiRank.BETA.text"),
    OMEGA("ManeiDominiRank.OMEGA.text"),
    TAU("ManeiDominiRank.TAU.text"),
    DELTA("ManeiDominiRank.DELTA.text"),
    SIGMA("ManeiDominiRank.SIGMA.text"),
    OMICRON("ManeiDominiRank.OMICRON.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String rankName;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    ManeiDominiRank(String rankName) {
        this.rankName = resources.getString(rankName);
    }
    //endregion Constructors

    @Override
    public String toString() {
        return rankName;
    }

    public static ManeiDominiRank parseFromString(String information) {
        // Parse based on the enum name
        try {
            return valueOf(information);
        } catch (Exception ignored) {

        }

        // Parse from Ordinal Int - Legacy save method
        ManeiDominiRank[] values = values();
        try {
            int mdRank = Integer.parseInt(information) + 1;
            if (values.length > mdRank) {
                return values[mdRank];
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error(ManeiDominiRank.class, "parseFromString",
                "Unable to parse " + information + "into a ManeiDominiRank. Returning NONE.");

        return ManeiDominiRank.NONE;
    }
}
