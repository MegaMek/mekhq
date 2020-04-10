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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public enum BabySurnameStyle {
    //region Enum Declaration
    FATHERS("BabySurnameStyle.FATHERS.text", "BabySurnameStyle.FATHERS.toolTipText"),
    MOTHERS("BabySurnameStyle.MOTHERS.text", "BabySurnameStyle.MOTHERS.toolTipText"),
    WELSH_PATRONYMICS("BabySurnameStyle.WELSH_PATRONYMICS.text", "BabySurnameStyle.WELSH_PATRONYMICS.toolTipText"),
    WELSH_MATRONYMICS("BabySurnameStyle.WELSH_MATRONYMICS.text", "BabySurnameStyle.WELSH_MATRONYMICS.toolTipText");
    //endregion Enum Declaration

    //region Variable Declarations
    private String styleName;
    private String styleToolTip;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    BabySurnameStyle(String styleName, String styleToolTip) {
        this.styleName = resources.getString(styleName);
        this.styleToolTip = resources.getString(styleToolTip);
    }
    //endregion Constructors

    public String getStyleName() {
        return styleName;
    }

    public String getStyleToolTip() {
        return styleToolTip;
    }

    public String generateBabySurname(Person mother, Person father) {
        switch (this) {
            case WELSH_PATRONYMICS:
                if (father != null) {
                    return getWelshPatronymic(father.getGivenName());
                }
            case WELSH_MATRONYMICS:
                return "ferch " + mother.getGivenName();
            case FATHERS:
                if (father != null) {
                    return father.getSurname();
                }
            case MOTHERS:
            default:
                return mother.getSurname();
        }
    }

    private String getWelshPatronymic(String fatherName) {
        switch (fatherName.charAt(0)) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
            case 'w':
            case 'y':
                return "ab " + fatherName;
            default:
                return "ap " + fatherName;
        }
    }

    @Override
    public String toString() {
        return getStyleName();
    }
}
