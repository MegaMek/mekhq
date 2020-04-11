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

import megamek.common.Crew;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public enum BabySurnameStyle {
    //region Enum Declaration
    FATHERS("BabySurnameStyle.FATHERS.text", "BabySurnameStyle.FATHERS.toolTipText"),
    MOTHERS("BabySurnameStyle.MOTHERS.text", "BabySurnameStyle.MOTHERS.toolTipText"),
    WELSH_PATRONYMICS("BabySurnameStyle.WELSH_PATRONYMICS.text", "BabySurnameStyle.WELSH_PATRONYMICS.toolTipText"),
    WELSH_MATRONYMICS("BabySurnameStyle.WELSH_MATRONYMICS.text", "BabySurnameStyle.WELSH_MATRONYMICS.toolTipText"),
    ICELANDIC_PATRONYMICS("BabySurnameStyle.ICELANDIC_PATRONYMICS.text", "BabySurnameStyle.ICELANDIC_PATRONYMICS.toolTipText"),
    ICELANDIC_MATRONYMICS("BabySurnameStyle.ICELANDIC_MATRONYMICS.text", "BabySurnameStyle.ICELANDIC_MATRONYMICS.toolTipText"),
    ICELANDIC_COMBINATION_NYMICS("BabySurnameStyle.ICELANDIC_COMBINATION_NYMICS.text", "BabySurnameStyle.ICELANDIC_COMBINATION_NYMICS.toolTipText");
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

    public String generateBabySurname(Person mother, Person father, int babyGender) {
        switch (this) {
            case WELSH_PATRONYMICS:
                if (father != null) {
                    return getWelshNymic(father.getGivenName(), babyGender);
                }
            case WELSH_MATRONYMICS:
                return getWelshNymic(mother.getGivenName(), babyGender);
            case ICELANDIC_COMBINATION_NYMICS:
                if (father != null) {
                    return getIcelandicNymic(mother.getGivenName(), babyGender)
                            + " " + getIcelandicNymic(father.getGivenName(), babyGender);
                }
            case ICELANDIC_PATRONYMICS:
                if (father != null) {
                    return getIcelandicNymic(father.getGivenName(), babyGender);
                }
            case ICELANDIC_MATRONYMICS:
                return getIcelandicNymic(mother.getGivenName(), babyGender);
            case FATHERS:
                if (father != null) {
                    return father.getSurname();
                }
            case MOTHERS:
            default:
                return mother.getSurname();
        }
    }

    /**
     * This creates a Welsh-style Surname based on the supplied given name and the gender of the baby
     * @param givenName the given name to create the surname from
     * @param babyGender the baby's gender
     * @return The Welsh-style surname
     */
    private String getWelshNymic(String givenName, int babyGender) {
        switch (babyGender) {
            case Crew.G_FEMALE:
                return "ferch " + givenName;
            case Crew.G_MALE:
            default:
                MekHQ.getLogger().error(getClass(), "getWelshNymic", String.valueOf(givenName.charAt(0)));
                switch (givenName.charAt(0)) {
                    case 'a':
                    case 'A':
                    case 'e':
                    case 'E':
                    case 'i':
                    case 'I':
                    case 'o':
                    case 'O':
                    case 'u':
                    case 'U':
                    case 'w':
                    case 'W':
                    case 'y':
                    case 'Y':
                        return "ab " + givenName;
                    default:
                        return "ap " + givenName;
                }
        }
    }

    /**
     * This creates an Icelandic-style surname based on the supplied given name and the gender of the baby
     * @param givenName the given name to create the surname from
     * @param babyGender the baby's gender
     * @return The Icelandic-style surname
     */
    private String getIcelandicNymic(String givenName, int babyGender) {
        switch (babyGender) {
            case Crew.G_MALE:
                return givenName + "sson";
            case Crew.G_FEMALE:
                return givenName + "sd\u00F3ttir";
            default:
                return givenName + "bur";
        }
    }

    @Override
    public String toString() {
        return getStyleName();
    }
}
