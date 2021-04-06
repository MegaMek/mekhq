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

public enum FormerSpouseReason {
    //region Enum Declarations
    WIDOWED("FormerSpouseReason.WIDOWED.text"),
    DIVORCE("FormerSpouseReason.DIVORCE.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    FormerSpouseReason(String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region File I/O
    public static FormerSpouseReason parseFromText(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 1:
                    return DIVORCE;
                case 0:
                default:
                    return WIDOWED;
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error("Unable to parse the former spouse reason from string " + text
                + ". Returning FormerSpouseReason.WIDOWED");
        return WIDOWED;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
