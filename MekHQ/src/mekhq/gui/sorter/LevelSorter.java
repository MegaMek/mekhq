/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.sorter;

import java.util.Comparator;

import megamek.common.enums.SkillLevel;

/**
 * A comparator for skills levels (e.g. Regular, Veteran, etc) * @author Jay Lawson
 */
public class LevelSorter implements Comparator<String> {

    /**
     * Compares two strings that may contain skill level descriptions.
     * <p>The comparison follows these rules:</p>
     *
     * <ul>
     *   <li>If both strings are "-", they are considered equal</li>
     *   <li>Strings containing "-" are sorted before any actual skill level</li>
     *   <li>Actual skill levels are compared based on their experience level values</li>
     * </ul>
     *
     * @param firstString  the first string to compare
     * @param secondString the second string to compare
     *
     * @return a negative {@link Integer} if {@code firstString} is less than {@code secondString}, {@code 0} if they
     *       are equal, or a positive {@link Integer} if {@code firstString} is greater
     *
     * @author Illiani
     * @since 0.50.05
     */
    @Override
    public int compare(String firstString, String secondString) {
        // Handle the special case of "-" values
        if (firstString.equals("-") && secondString.equals("-")) {
            return 0; // Equal values should return 0
        } else if (firstString.equals("-")) {
            return -1; // By convention, "-" sort before actual values
        } else if (secondString.equals("-")) {
            return 1;  // firstString is not "-", but secondString is, so firstString comes after
        }

        // Convert strings to SkillLevel enums and compare their experience levels
        SkillLevel firstStringLevel = parseSkillLevel(firstString);
        SkillLevel secondStringLevel = parseSkillLevel(secondString);

        return Integer.compare(firstStringLevel.getExperienceLevel(), secondStringLevel.getExperienceLevel());
    }

    /**
     * Parses a string to find the matching SkillLevel.
     *
     * <p>Checks if the input string contains the name of any skill level. The check is performed in the order
     * defined by {@link SkillLevel#values()} to avoid substring matching issues (e.g., ULTRA_GREEN vs. GREEN).</p>
     *
     * @param str String that may contain a skill level name
     *
     * @return The corresponding {@link SkillLevel}, or {@link SkillLevel#NONE} if no match is found
     *
     * @author Illiani
     * @since 0.50.05
     */
    private SkillLevel parseSkillLevel(String str) {
        for (SkillLevel level : SkillLevel.values()) {
            if (str.contains(level.toString())) {
                return level;
            }
        }
        return SkillLevel.NONE; // Default if no match found
    }
}
