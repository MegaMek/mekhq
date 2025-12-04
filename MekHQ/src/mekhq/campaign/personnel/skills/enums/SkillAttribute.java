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

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

/**
 * Enum representing the primary attributes associated with skills in MekHQ. These attributes correspond to their ATOW
 * equivalents.
 *
 * <p>This enum also provides a utility method to parse values from strings or integers.</p>
 */
public enum SkillAttribute {
    /** Represents no specific attribute. */
    NONE("NONE"),
    /** Represents physical strength or power. */
    STRENGTH("STRENGTH"),
    /** Represents overall physical condition and health. */
    BODY("BODY"),
    /** Represents coordination and fine motor skills. */
    DEXTERITY("DEXTERITY"),
    /** Represents reflexes or reaction time. */
    REFLEXES("REFLEXES"),
    /** Represents cognitive ability and problem-solving skills. */
    INTELLIGENCE("INTELLIGENCE"),
    /** Represents mental willpower and determination. */
    WILLPOWER("WILLPOWER"),
    /** Represents social skills and personal magnetism. */
    CHARISMA("CHARISMA"),
    /** Represents exceptional destiny or skill. */
    EDGE("EDGE");

    public static final int NO_SKILL_ATTRIBUTE = -1;

    final private static String RESOURCE_BUNDLE = "mekhq.resources.SkillAttribute";

    private final String lookupName;
    private final String label;
    private final String shortName;
    private final String description;

    /**
     * Constructs a {@link SkillAttribute} with the specified lookup name.
     *
     * @param lookupName The localized or identifier name associated with this {@link SkillAttribute}.
     */
    SkillAttribute(String lookupName) {
        this.lookupName = lookupName;
        this.label = generateLabel();
        this.shortName = generateShortName();
        this.description = generateDescription();
    }

    /**
     * Retrieves the lookup name associated with this {@link SkillAttribute}.
     *
     * @return The lookup name as a {@link String}.
     */
    public String getLookupName() {
        return lookupName;
    }

    public String getLabel() {
        return label;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

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
     * @since 0.50.05
     */
    private String generateLabel() {
        final String RESOURCE_KEY = lookupName + ".label";

        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the short name associated with this {@link SkillAttribute}.
     *
     * <p>The short name is determined by looking up a resource bundle key associated with the enum's name in the
     * format <code>{name}.shortName</code>.</p>
     *
     * @return The localized short name for this {@link SkillAttribute}.
     *
     * @since 0.50.05
     */
    private String generateShortName() {
        final String RESOURCE_KEY = lookupName + ".shortName";

        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the description associated with this {@link SkillAttribute}.
     *
     * <p>The description is determined by looking up a resource bundle key associated with the enum's name in the
     * format <code>{name}.description</code>.</p>
     *
     * @return The localized description for this {@link SkillAttribute}.
     *
     * @since 0.50.05
     */
    private String generateDescription() {
        final String RESOURCE_KEY = lookupName + ".description";

        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
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
     *       fault is it is not.
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
