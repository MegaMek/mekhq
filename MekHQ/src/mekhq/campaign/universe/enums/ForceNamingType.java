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
package mekhq.campaign.universe.enums;

import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum ForceNamingType {
    //region Enum Declarations
    CCB_1943("ForceNamingType.CCB_1943.text"),
    ICAO_1956("ForceNamingType.ICAO_1956.text"),
    GREEK_ALPHABET("ForceNamingType.GREEK_ALPHABET.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    ForceNamingType(String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    public String getValue(Alphabet alphabet) {
        switch (this) {
            case ICAO_1956:
                return alphabet.getICAO1956();
            case GREEK_ALPHABET:
                return alphabet.getGreek();
            case CCB_1943:
            default:
                return alphabet.getCCB1943();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
