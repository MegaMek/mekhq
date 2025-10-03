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
package mekhq.campaign.personnel.skills;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.messageSurroundedBySpanWithColor;

import megamek.common.enums.SkillLevel;
import mekhq.MekHQ;

/**
 * Utility class providing static methods for working with experience levels and skill-related attributes.
 *
 * <p>This class offers methods for retrieving localized display names, formatting experience level names with color,
 * and obtaining color codes corresponding to specific experience levels. It supports both the raw experience level
 * integer values and {@code SkillLevel} objects.</p>
 *
 * <p>The methods in this class are intended for use throughout the application wherever experience levels or related
 * display information are needed.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class SkillUtilities {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SkillUtilities";

    public static final int SKILL_NONE = 0;

    public static final int EXP_NONE = -1;
    public static final int EXP_ULTRA_GREEN = 0;
    public static final int EXP_GREEN = 1;
    public static final int EXP_REGULAR = 2;
    public static final int EXP_VETERAN = 3;
    public static final int EXP_ELITE = 4;
    public static final int EXP_HEROIC = 5;
    public static final int EXP_LEGENDARY = 6;

    /**
     * Returns the localized display name for the specified experience level.
     *
     * <p>The method maps the given experience level integer constant to a key, retrieves the corresponding display
     * name from the resource bundle, and returns it. If the level does not match any predefined constant, a default
     * value is returned.</p>
     *
     * @param level the integer constant representing the experience level
     *
     * @return the localized display name for the specified experience level
     */
    public static String getExperienceLevelName(int level) {
        String key = switch (level) {
            case EXP_ULTRA_GREEN -> "ultraGreen";
            case EXP_GREEN -> "green";
            case EXP_REGULAR -> "regular";
            case EXP_VETERAN -> "veteran";
            case EXP_ELITE -> "elite";
            case EXP_HEROIC -> "heroic";
            case EXP_LEGENDARY -> "legendary";
            case -1 -> "none";
            default -> "default";
        };

        return getTextAt(RESOURCE_BUNDLE, "SkillUtilities.skillLevel." + key);
    }

    /**
     * Returns the hexadecimal color code associated with the specified {@link SkillLevel}.
     *
     * <p>Retrieves the adjusted experience level value from the {@link SkillLevel} object and returns the color code
     * associated with that level.
     *
     * @param skillLevel the {@link SkillLevel} object representing the experience level
     *
     * @return the hex color code as a {@code String} for the given skill level
     */
    public static String getExperienceLevelColor(SkillLevel skillLevel) {
        int level = skillLevel.getAdjustedValue();
        return getExperienceLevelColor(level);
    }

    /**
     * Returns the hexadecimal color code associated with the specified experience level.
     *
     * <p>The method selects the appropriate color code for the provided experience level, as defined in the current
     * options. If the level does not match any known experience level, an empty string is returned.</p>
     *
     * @param level the integer constant representing the experience level
     *
     * @return the hex color code as a {@code String} for the given experience level, or an empty string if unspecified
     */
    public static String getExperienceLevelColor(int level) {
        return switch (level) {
            case EXP_ULTRA_GREEN -> MekHQ.getMHQOptions().getFontColorSkillUltraGreenHexColor();
            case EXP_GREEN -> MekHQ.getMHQOptions().getFontColorSkillGreenHexColor();
            case EXP_REGULAR -> MekHQ.getMHQOptions().getFontColorSkillRegularHexColor();
            case EXP_VETERAN -> MekHQ.getMHQOptions().getFontColorSkillVeteranHexColor();
            case EXP_ELITE, EXP_HEROIC, EXP_LEGENDARY -> MekHQ.getMHQOptions().getFontColorSkillEliteHexColor();
            default -> "";
        };
    }

    /**
     * Returns the experience level name with associated color formatting.
     *
     * <p>If a color is defined for the specified experience level, the name is wrapped with a span tag using the
     * corresponding hex color code. Otherwise, returns the unformatted name.</p>
     *
     * @param level the integer constant representing the experience level
     *
     * @return the experience level name, colored with its associated hex code if available; otherwise, the plain name
     */
    public static String getColoredExperienceLevelName(int level) {
        if (getExperienceLevelColor(level).isEmpty()) {
            return getExperienceLevelName(level);
        }

        return messageSurroundedBySpanWithColor(getExperienceLevelColor(level), getExperienceLevelName(level));
    }

    /**
     * Returns the experience level name with associated color formatting for the specified {@link SkillLevel}.
     *
     * <p>Retrieves the adjusted experience level from the {@link SkillLevel} object and returns the colored
     * experience level name, if available.</p>
     *
     * @param skillLevel the {@link SkillLevel} object representing the experience level
     *
     * @return the experience level name, colored with its associated hex code if available; otherwise, the plain name
     */
    public static String getColoredExperienceLevelName(SkillLevel skillLevel) {
        int level = skillLevel.getAdjustedValue();
        return getColoredExperienceLevelName(level);
    }
}
