/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.skills.enums;

import megamek.logging.MMLogger;

/**
 * Enum representing the primary attributes associated with skills in MekHQ. These attributes correspond to their ATOW
 * equivalents.
 *
 * <p>This enum also provides a utility method to parse values from strings or integers.</p>
 */
public enum SkillAttribute {
    /**
     * Represents no specific attribute.
     */
    NONE,

    /**
     * Represents physical strength or power.
     */
    STRENGTH,

    /**
     * Represents overall physical condition and health.
     */
    BODY,

    /**
     * Represents coordination and fine motor skills.
     */
    DEXTERITY,

    /**
     * Represents reflexes or reaction time.
     */
    REFLEXES,

    /**
     * Represents cognitive ability and problem-solving skills.
     */
    INTELLIGENCE,

    /**
     * Represents mental willpower and determination.
     */
    WILLPOWER,

    /**
     * Represents social skills and personal magnetism.
     */
    CHARISMA;

    public static int NO_SKILL_ATTRIBUTE = -1;

    /**
     * Converts a string or integer input to its corresponding {@link SkillAttribute}.
     *
     * <p>This method attempts the following:</p>
     *
     * <ul>
     *     <li>Parses the input string to match a {@link SkillAttribute} value. The input is case-insensitive, and
     *     spaces are replaced with underscores ("_").</li>
     *     <li>If the above fails, converts the input string to an integer and matches it to an ordinal value
     *     of {@link SkillAttribute}.</li>
     *     <li>If both attempts fail, logs an error and returns {@link #NONE} as the default value.</li>
     * </ul>
     *
     * @param text The input string or integer representing the skill attribute.
     *
     * @return The corresponding {@link SkillAttribute} value if the input is valid, or {@link #NONE} as the default if
     *       fault if it is not.
     */
    public static SkillAttribute fromString(String text) {
        try {
            // Attempt to parse as string with case/space adjustments.
            return SkillAttribute.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {
        }

        try {
            // Attempt to parse as an integer and use as ordinal.
            // We're using Integer.parseInt() here and not MathUtility.parseInt as we want to have a log in the event
            // parsing fails, rather than just silently failing.
            return SkillAttribute.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {
        }

        // Log error if parsing fails and return default value.
        MMLogger logger = MMLogger.create(SkillAttribute.class);
        logger.error("Unknown SkillAttribute ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }
}
