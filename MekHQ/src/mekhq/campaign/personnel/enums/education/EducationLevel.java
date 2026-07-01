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

import java.util.Arrays;
import java.util.ResourceBundle;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum EducationLevel {
    // region Enum Declarations
    EARLY_CHILDHOOD("EducationLevel.EARLY_CHILDHOOD.text", "EducationLevel.EARLY_CHILDHOOD.toolTipText", 0),
    HIGH_SCHOOL("EducationLevel.HIGH_SCHOOL.text", "EducationLevel.HIGH_SCHOOL.toolTipText", 1),
    COLLEGE("EducationLevel.COLLEGE.text", "EducationLevel.COLLEGE.toolTipText", 2),
    POST_GRADUATE("EducationLevel.POST_GRADUATE.text", "EducationLevel.POST_GRADUATE.toolTipText", 3),
    DOCTORATE("EducationLevel.DOCTORATE.text", "EducationLevel.DOCTORATE.toolTipText", 4);
    // endregion Enum Declarations

    private static final MMLogger LOGGER = MMLogger.create(EducationLevel.class);

    public static final int MIN_LEVEL =
          Arrays.stream(EducationLevel.values()).mapToInt(EducationLevel::getLevel).min().getAsInt();
    public static final int MAX_LEVEL =
          Arrays.stream(EducationLevel.values()).mapToInt(EducationLevel::getLevel).max().getAsInt();

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final int level;
    // endregion Variable Declarations

    // region Constructors
    EducationLevel(String name, String toolTipText, int level) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.level = level;
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public int getLevel() {
        return level;
    }
    // endregion Getters

    // region Boolean Comparison Methods

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isEarlyChildhood() {
        return this == EARLY_CHILDHOOD;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
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
     *   <li>Next, it attempts to parse the string as an integer and map it to the level of the enum.</li>
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
            int level = Integer.parseInt(text);
            for (EducationLevel education : EducationLevel.values()) {
                if (education.level == level) {
                    return education;
                }
            }
        } catch (NumberFormatException ignored) {
        }

        try {
            for (EducationLevel educationLevel : EducationLevel.values()) {
                if (educationLevel.toString().equalsIgnoreCase(text)) {
                    return educationLevel;
                }
            }
        } catch (Exception ignored) {
        }

        LOGGER.error("Unknown education level: {} - returning {}.", text, EARLY_CHILDHOOD);
        return EARLY_CHILDHOOD;
    }

    public static EducationLevel fromLevel(int level) {
        for (EducationLevel education : EducationLevel.values()) {
            if (education.level == level) {
                return education;
            }
        }
        LOGGER.error("Unknown education level: {}", level, new IllegalArgumentException());
        return EARLY_CHILDHOOD;
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
