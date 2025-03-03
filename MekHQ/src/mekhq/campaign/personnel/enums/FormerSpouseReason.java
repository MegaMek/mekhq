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

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum FormerSpouseReason {
    // region Enum Declarations
    WIDOWED("FormerSpouseReason.WIDOWED.text"),
    DIVORCE("FormerSpouseReason.DIVORCE.text");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    // endregion Variable Declarations

    // region Constructors
    FormerSpouseReason(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Boolean Comparison Methods
    public boolean isWidowed() {
        return this == WIDOWED;
    }

    public boolean isDivorce() {
        return this == DIVORCE;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    public static FormerSpouseReason parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return WIDOWED;
                case 1:
                    return DIVORCE;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(FormerSpouseReason.class)
                .error("Unable to parse " + text + " into a FormerSpouseReason. Returning WIDOWED.");
        return WIDOWED;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
