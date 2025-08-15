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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.enums.education;

import java.util.ResourceBundle;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import megamek.logging.MMLogger;
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
     * Converts a string to its corresponding {@link EducationLevel} enum constant, if possible.
     *
     * <p>This method attempts to map the input string to an {@link EducationLevel} in multiple ways:</p>
     * <ul>
     *   <li>First, it checks if the string matches an enum name exactly.</li>
     *   <li>Next, it attempts to parse the string as an integer and map it to the ordinal of the enum.</li>
     *   <li>Finally, it checks for a case-insensitive match against the enum names.</li>
     * </ul>
     *
     * <p>If no match is found, a default value of {@code EARLY_CHILDHOOD} is returned, and an error is logged to
     * indicate the invalid input.</p>
     *
     * @param text the input string to be converted to an {@link EducationLevel}.
     *
     * @return the {@link EducationLevel} corresponding to the input text, or {@link EducationLevel#EARLY_CHILDHOOD} if
     *       the input is invalid.
     */
    public static EducationLevel fromString(String text) {
        try {
            return EducationLevel.valueOf(text);
        } catch (Exception ignored) {
        }

        try {
            return EducationLevel.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {
        }

        try {
            for (EducationLevel educationLevel : EducationLevel.values()) {
                if (educationLevel.toString().equalsIgnoreCase(text)) {
                    return educationLevel;
                }
            }
        } catch (Exception ignored) {
        }


        MMLogger logger = MMLogger.create(EducationLevel.class);
        logger.error("Unknown EducationLevel ordinal: {} - returning {}.", text, EARLY_CHILDHOOD);

        return EARLY_CHILDHOOD;
    }

    /**
     * Parses the given EducationLevel enum value to an integer.
     *
     * @param educationLevel the EducationLevel enum value to be parsed
     *
     * @return the integer value representing the parsed EducationLevel
     *
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
            return EducationLevel.fromString(educationLevel);
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
