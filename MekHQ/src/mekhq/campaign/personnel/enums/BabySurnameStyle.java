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

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;

public enum BabySurnameStyle {
    // region Enum Declaration
    FATHERS("BabySurnameStyle.FATHERS.text", "BabySurnameStyle.FATHERS.toolTipText"),
    MOTHERS("BabySurnameStyle.MOTHERS.text", "BabySurnameStyle.MOTHERS.toolTipText"),
    MOTHERS_FATHERS("BabySurnameStyle.MOTHERS_FATHERS.text", "BabySurnameStyle.MOTHERS_FATHERS.toolTipText"),
    MOTHERS_HYPHEN_FATHERS("BabySurnameStyle.MOTHERS_HYPHEN_FATHERS.text",
            "BabySurnameStyle.MOTHERS_HYPHEN_FATHERS.toolTipText"),
    FATHERS_MOTHERS("BabySurnameStyle.FATHERS_MOTHERS.text", "BabySurnameStyle.FATHERS_MOTHERS.toolTipText"),
    FATHERS_HYPHEN_MOTHERS("BabySurnameStyle.FATHERS_HYPHEN_MOTHERS.text",
            "BabySurnameStyle.FATHERS_HYPHEN_MOTHERS.toolTipText"),
    WELSH_PATRONYMICS("BabySurnameStyle.WELSH_PATRONYMICS.text", "BabySurnameStyle.WELSH_PATRONYMICS.toolTipText"),
    WELSH_MATRONYMICS("BabySurnameStyle.WELSH_MATRONYMICS.text", "BabySurnameStyle.WELSH_MATRONYMICS.toolTipText"),
    ICELANDIC_PATRONYMICS("BabySurnameStyle.ICELANDIC_PATRONYMICS.text",
            "BabySurnameStyle.ICELANDIC_PATRONYMICS.toolTipText"),
    ICELANDIC_MATRONYMICS("BabySurnameStyle.ICELANDIC_MATRONYMICS.text",
            "BabySurnameStyle.ICELANDIC_MATRONYMICS.toolTipText"),
    ICELANDIC_COMBINATION_NYMICS("BabySurnameStyle.ICELANDIC_COMBINATION_NYMICS.text",
            "BabySurnameStyle.ICELANDIC_COMBINATION_NYMICS.toolTipText"),
    RUSSIAN_PATRONYMICS("BabySurnameStyle.RUSSIAN_PATRONYMICS.text",
            "BabySurnameStyle.RUSSIAN_PATRONYMICS.toolTipText");
    // endregion Enum Declaration

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    BabySurnameStyle(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isFathers() {
        return this == FATHERS;
    }

    public boolean isMothers() {
        return this == MOTHERS;
    }

    public boolean isMothersFathers() {
        return this == MOTHERS_FATHERS;
    }

    public boolean isMothersHyphenFathers() {
        return this == MOTHERS_HYPHEN_FATHERS;
    }

    public boolean isFathersMothers() {
        return this == FATHERS_MOTHERS;
    }

    public boolean isFathersHyphenMothers() {
        return this == FATHERS_HYPHEN_MOTHERS;
    }

    public boolean isWelshPatronymics() {
        return this == WELSH_PATRONYMICS;
    }

    public boolean isWelshMatronymics() {
        return this == WELSH_MATRONYMICS;
    }

    public boolean isIcelandicPatronymics() {
        return this == ICELANDIC_PATRONYMICS;
    }

    public boolean isIcelandicMatronymics() {
        return this == ICELANDIC_MATRONYMICS;
    }

    public boolean isIcelandicCombinationNymics() {
        return this == ICELANDIC_COMBINATION_NYMICS;
    }

    public boolean isRussianPatronymics() {
        return this == RUSSIAN_PATRONYMICS;
    }
    // endregion Boolean Comparison Methods

    public String generateBabySurname(final Person mother, final @Nullable Person father,
            final Gender babyGender) {
        final boolean hasFather = father != null;
        switch (this) {
            case WELSH_PATRONYMICS:
                if (hasFather) {
                    return getWelshNymic(father.getGivenName(), babyGender);
                }
            case WELSH_MATRONYMICS:
                return getWelshNymic(mother.getGivenName(), babyGender);
            case ICELANDIC_COMBINATION_NYMICS:
                if (hasFather) {
                    return getIcelandicNymic(mother.getGivenName(), babyGender)
                            + ' ' + getIcelandicNymic(father.getGivenName(), babyGender);
                }
            case ICELANDIC_PATRONYMICS:
                if (hasFather) {
                    return getIcelandicNymic(father.getGivenName(), babyGender);
                }
            case ICELANDIC_MATRONYMICS:
                return getIcelandicNymic(mother.getGivenName(), babyGender);
            case RUSSIAN_PATRONYMICS:
                if (hasFather) {
                    return getRussianNymic(father.getGivenName().trim(), babyGender);
                }
            case MOTHERS_FATHERS:
                if (hasFather) {
                    return mother.getSurname() + ' ' + father.getSurname();
                }
            case FATHERS_MOTHERS:
                if (hasFather) {
                    return father.getSurname() + ' ' + mother.getSurname();
                }
            case MOTHERS_HYPHEN_FATHERS:
                if (hasFather) {
                    return mother.getSurname() + '-' + father.getSurname();
                }
            case FATHERS_HYPHEN_MOTHERS:
                if (hasFather) {
                    return father.getSurname() + '-' + mother.getSurname();
                }
            case FATHERS:
                if (hasFather) {
                    return father.getSurname();
                }
            case MOTHERS:
            default:
                return mother.getSurname();
        }
    }

    /**
     * This creates a Welsh-style Surname based on the supplied given name and the
     * gender of the baby
     * 
     * @param givenName  the given name to create the surname from
     * @param babyGender the baby's gender
     * @return The Welsh-style surname
     */
    private String getWelshNymic(final String givenName, final Gender babyGender) {
        switch (babyGender) {
            case FEMALE:
                return "ferch " + givenName;
            case MALE:
            default:
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
     * This creates an Icelandic-style surname based on the supplied given name and
     * the gender of the baby
     * 
     * @param givenName  the given name to create the surname from
     * @param babyGender the baby's gender
     * @return The Icelandic-style surname
     */
    private String getIcelandicNymic(final String givenName, final Gender babyGender) {
        switch (babyGender) {
            case MALE:
                return givenName + "sson";
            case FEMALE:
                return givenName + "sd\u00F3ttir";
            default:
                return givenName + "sbur";
        }
    }

    /**
     * This creates an Russian-style surname based on the supplied given name and
     * the gender of the baby
     * 
     * @param givenName  the given name to create the surname from
     * @param babyGender the baby's gender
     * @return The Russian-style surname
     */
    private String getRussianNymic(final String givenName, final Gender babyGender) {
        switch (givenName.charAt(givenName.length() - 1)) {
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
                return givenName.substring(0, givenName.length() - 1) + (babyGender.isMale() ? "evich" : "evna");
            default:
                return givenName + (babyGender.isMale() ? "ovich" : "ovna");

        }
    }

    // region File I/O
    /**
     * @param text containing the BabySurnameStyle
     * @return the saved BabySurnameStyle
     */
    public static BabySurnameStyle parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Migration Occurred in 0.49.9
        switch (text) {
            case "MOTHERS_HYP_FATHERS":
                return MOTHERS_HYPHEN_FATHERS;
            case "FATHERS_HYP_MOTHERS":
                return FATHERS_HYPHEN_MOTHERS;
            default:
                break;
        }

        // 0.47.* Migration
        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return MOTHERS;
                case 1:
                    return FATHERS;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(BabySurnameStyle.class)
                .error("Unable to parse " + text + " into a BabySurnameStyle. Returning MOTHERS.");
        return MOTHERS;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
