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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.skills.enums;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.codeUtilities.MathUtility;
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

    final private static String RESOURCE_BUNDLE = "mekhq.resources.SkillAttribute";

    /**
     * Checks if the current instance is {@link #NONE}.
     *
     * @return {@code true} if the current instance is {@code NONE}, {@code false} otherwise.
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * Retrieves the label associated with this {@link SkillAttribute}.
     *
     * <p>The label is determined by looking up a resource bundle key associated with the enum's name in the format
     * <code>{name}.label</code>.</p>
     *
     * @return The localized label for this {@link SkillAttribute}.
     *
     * @see #getLabel(SkillAttribute)
     * @since 0.50.05
     */
    public String getLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the label associated with the given {@link SkillAttribute}.
     *
     * <p>The label is determined by looking up a resource bundle key associated with the attribute's name in the
     * format <code>{name}.label</code>.</p>
     *
     * @param attribute The {@link SkillAttribute} whose label is to be retrieved.
     *
     * @return The localized label for the provided {@link SkillAttribute}.
     *
     * @see #getLabel()
     * @since 0.50.05
     */
    public static String getLabel(SkillAttribute attribute) {
        final String RESOURCE_KEY = attribute.name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the short name associated with this {@link SkillAttribute}.
     *
     * <p>The short name is determined by looking up a resource bundle key associated with the enum's name in the
     * format <code>{name}.shortName</code>.</p>
     *
     * @return The localized short name for this {@link SkillAttribute}.
     *
     * @see #getShortName(SkillAttribute)
     * @since 0.50.05
     */
    public String getShortName() {
        final String RESOURCE_KEY = name() + ".shortName";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the short name associated with the given {@link SkillAttribute}.
     *
     * <p>The short name is determined by looking up a resource bundle key associated with the attribute's name in
     * the format <code>{name}.shortName</code>.</p>
     *
     * @param attribute The {@link SkillAttribute} whose short name is to be retrieved.
     *
     * @return The localized short name for the provided {@link SkillAttribute}.
     *
     * @see #getShortName()
     * @since 0.50.05
     */
    public static String getShortName(SkillAttribute attribute) {
        final String RESOURCE_KEY = attribute.name() + ".shortName";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the description associated with this {@link SkillAttribute}.
     *
     * <p>The description is determined by looking up a resource bundle key associated with the enum's name in the
     * format <code>{name}.description</code>.</p>
     *
     * @return The localized description for this {@link SkillAttribute}.
     *
     * @see #getDescription(SkillAttribute)
     * @since 0.50.05
     */
    public String getDescription() {
        final String RESOURCE_KEY = name() + ".description";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the description associated with the given {@link SkillAttribute}.
     *
     * <p>The description is determined by looking up a resource bundle key associated with the attribute's name in
     * the format <code>{name}.description</code>.</p>
     *
     * @param attribute The {@link SkillAttribute} whose description is to be retrieved.
     *
     * @return The localized description for the provided {@link SkillAttribute}.
     *
     * @see #getDescription()
     * @since 0.50.05
     */
    public static String getDescription(SkillAttribute attribute) {
        final String RESOURCE_KEY = attribute.name() + ".description";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

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
            return SkillAttribute.values()[MathUtility.parseInt(text)];
        } catch (Exception ignored) {
        }

        // Log error if parsing fails and return default value.
        MMLogger logger = MMLogger.create(SkillAttribute.class);
        logger.error("Unknown SkillAttribute ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }
}
