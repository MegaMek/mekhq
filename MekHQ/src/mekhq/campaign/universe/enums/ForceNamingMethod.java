/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.universe.enums;

import mekhq.MekHQ;

import java.util.ResourceBundle;

/**
 * @author Justin "Windchild" Bowen
 */
public enum ForceNamingMethod {
    //region Enum Declarations
    CCB_1943("ForceNamingMethod.CCB_1943.text", "ForceNamingMethod.CCB_1943.toolTipText"),
    ICAO_1956("ForceNamingMethod.ICAO_1956.text", "ForceNamingMethod.ICAO_1956.toolTipText"),
    ENGLISH_ALPHABET("ForceNamingMethod.ENGLISH_ALPHABET.text", "ForceNamingMethod.ENGLISH_ALPHABET.toolTipText"),
    GREEK_ALPHABET("ForceNamingMethod.GREEK_ALPHABET.text", "ForceNamingMethod.GREEK_ALPHABET.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    ForceNamingMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isCCB1943() {
        return this == CCB_1943;
    }

    public boolean isICAO1956() {
        return this == ICAO_1956;
    }

    public boolean isEnglishAlphabet() {
        return this == ENGLISH_ALPHABET;
    }

    public boolean isGreekAlphabet() {
        return this == GREEK_ALPHABET;
    }
    //endregion Boolean Comparison Methods

    public String getValue(final Alphabet alphabet) {
        switch (this) {
            case ICAO_1956:
                return alphabet.getICAO1956();
            case ENGLISH_ALPHABET:
                return alphabet.getEnglish();
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
