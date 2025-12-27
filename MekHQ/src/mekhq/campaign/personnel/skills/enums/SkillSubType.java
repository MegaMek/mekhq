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

import megamek.logging.MMLogger;

/**
 * Enum representing the subtypes of skills used in MekHQ.
 *
 * <p>The available skill subtypes are:</p>
 *
 * <p>This enum also includes a utility method for parsing {@link SkillSubType} values from strings
 * or integers.</p>
 */
public enum SkillSubType {
    /**
     * Represents a default value, it normally means the SubType is missing, or hasn't been set.
     */
    NONE("NONE"),

    /**
     * Represents gunnery-related combat skills.
     */
    COMBAT_GUNNERY("COMBAT_GUNNERY"),

    /**
     * Represents piloting-related combat skills.
     */
    COMBAT_PILOTING("COMBAT_PILOTING"),

    /**
     * Represents the primary skills used by non-combat professions.
     */
    SUPPORT("SUPPORT"),

    /**
     * Represents technician skills.
     */
    SUPPORT_TECHNICIAN("SUPPORT_TECHNICIAN"),

    /**
     * Use {@link #UTILITY_COMMAND} instead
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    SUPPORT_COMMAND("SUPPORT_COMMAND"),

    /**
     * Represents skills with mechanical effects that are not specifically profession-based skills
     */
    UTILITY("UTILITY"),

    /**
     * Represents command-related utility skills.
     */
    UTILITY_COMMAND("UTILITY_COMMAND"),

    /**
     * Represents roleplay or narrative-based skills.
     */
    ROLEPLAY_GENERAL("ROLEPLAY_GENERAL"),

    /**
     * Represents skills related to art or artistic pursuits within the roleplay or narrative context.
     */
    ROLEPLAY_ART("ROLEPLAY_ART"),

    /**
     * Represents roleplay or narrative-based skills related to special interests.
     */
    ROLEPLAY_INTEREST("ROLEPLAY_INTEREST"),

    /**
     * Represents roleplay or narrative-based skills related to science or scientific disciplines.
     */
    ROLEPLAY_SCIENCE("ROLEPLAY_SCIENCE"),

    /**
     * Represents roleplay or narrative-based skills related to security.
     */
    ROLEPLAY_SECURITY("ROLEPLAY_SECURITY");


    private static final String RESOURCE_BUNDLE = "mekhq.resources.SkillSubType";

    private final String lookupName;

    SkillSubType(String lookupName) {
        this.lookupName = lookupName;
    }

    public String getLookupName() {
        return lookupName;
    }

    public String getDisplayName() {
        return getTextAt(RESOURCE_BUNDLE, "SkillSubType." + lookupName + ".label");
    }

    /**
     * Converts a string or integer input to its corresponding {@link SkillSubType}.
     *
     * <p>This method attempts the following:</p>
     * <ul>
     *     <li>Parses the input string as a {@link SkillSubType} value. The input is case-insensitive and can contain spaces,
     *     which will be replaced by underscores ("_").</li>
     *     <li>If the above fails, converts the input string to an integer and attempts to match it to an ordinal value
     *     of {@link SkillSubType}.</li>
     *     <li>If both attempts fail, logs an error and returns {@link #COMBAT_GUNNERY} as the default value.</li>
     * </ul>
     *
     * @param text The input string or integer representing the skill subtype.
     *
     * @return The corresponding {@link SkillSubType} value if the input is valid, or {@link #COMBAT_GUNNERY} as the
     *       default if it is not.
     */
    public static SkillSubType fromString(String text) {
        try {
            // Attempt to parse as string with case/space adjustments.
            return SkillSubType.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {
        }

        try {
            // Attempt to parse as an integer and use as ordinal.
            // We're using Integer.parseInt() here and not MathUtility.parseInt as we want to have a log in the event
            // parsing fails, rather than just silently failing.
            return SkillSubType.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {
        }

        // Log error if parsing fails and return default value.
        MMLogger logger = MMLogger.create(SkillSubType.class);
        logger.error("Unknown SkillSubType ordinal: {} - returning {}.", text, COMBAT_GUNNERY);

        return COMBAT_GUNNERY;
    }
}
