/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums.education;

import java.util.ResourceBundle;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import mekhq.MekHQ;

public enum EducationLevel {
    // region Enum Declarations
    EARLY_CHILDHOOD("EducationLevel.EARLY_CHILDHOOD.text", "EducationLevel.EARLY_CHILDHOOD.toolTipText"),
    HIGH_SCHOOL("EducationLevel.HIGH_SCHOOL.text", "EducationLevel.HIGH_SCHOOL.toolTipText"),
    COLLEGE("EducationLevel.COLLEGE.text", "EducationLevel.COLLEGE.toolTipText"),
    POST_GRADUATE("EducationLevel.POST_GRADUATE.text", "EducationLevel.POST_GRADUATE.toolTipText"),
    DOCTORATE("EducationLevel.DOCTORATE.text", "EducationLevel.DOCTORATE.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    EducationLevel(final String name, final String toolTipText) {
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

    public boolean isEarlyChildhood() {
        return this == EARLY_CHILDHOOD;
    }

    public boolean isHighSchool() {
        return this == HIGH_SCHOOL;
    }

    public boolean isCollege() {
        return this == COLLEGE;
    }

    public boolean isPostGraduate() {
        return this == POST_GRADUATE;
    }

    public boolean isDoctorate() {
        return this == DOCTORATE;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    /**
     * Parses a given string and returns the corresponding AcademyType.
     * Accepts either the ENUM ordinal value or its name
     *
     * @param educationLevel the string to be parsed
     * @return the AcademyType object that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid
     *                               AcademyType
     */
    public static EducationLevel parseFromString(final String educationLevel) {
        return switch (educationLevel) {
            case "None", "Early Childhood" -> EARLY_CHILDHOOD;
            case "High School" -> HIGH_SCHOOL;
            case "College" -> COLLEGE;
            case "Post-Graduate" -> POST_GRADUATE;
            case "Doctorate" -> DOCTORATE;
            default ->
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/personnel/enums/education/EducationLevel.java/parseFromString: "
                                + educationLevel);
        };
    }

    /**
     * Parses an integer value into an EducationLevel enum.
     *
     * @param educationLevel the integer value representing the education level
     * @return the corresponding EducationLevel enum value
     * @throws IllegalStateException if the integer value does not correspond to any
     *                               valid EducationLevel enum value
     */
    public static EducationLevel parseFromInt(final int educationLevel) {
        return switch (educationLevel) {
            case 0 -> EARLY_CHILDHOOD;
            case 1 -> HIGH_SCHOOL;
            case 2 -> COLLEGE;
            case 3 -> POST_GRADUATE;
            case 4 -> DOCTORATE;
            default ->
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/personnel/enums/education/EducationLevel.java/parseFromInt: "
                                + educationLevel);
        };
    }

    /**
     * Parses the given EducationLevel enum value to an integer.
     *
     * @param educationLevel the EducationLevel enum value to be parsed
     * @return the integer value representing the parsed EducationLevel
     * @throws IllegalStateException if the given EducationLevel is unexpected
     */
    public static int parseToInt(final EducationLevel educationLevel) {
        return switch (educationLevel) {
            case EARLY_CHILDHOOD -> 0;
            case HIGH_SCHOOL -> 1;
            case COLLEGE -> 2;
            case POST_GRADUATE -> 3;
            case DOCTORATE -> 4;
        };
    }
    // endregion File I/O

    // region adaptors
    public static class Adapter extends XmlAdapter<String, EducationLevel> {
        @Override
        public EducationLevel unmarshal(String educationLevel) {
            return EducationLevel.parseFromString(educationLevel);
        }

        @Override
        public String marshal(EducationLevel educationLevel) {
            return educationLevel.toString();
        }
    }
    // endregion adaptors

    @Override
    public String toString() {
        return name;
    }
}
