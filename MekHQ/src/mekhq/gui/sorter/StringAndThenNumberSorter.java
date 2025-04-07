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
package mekhq.gui.sorter;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A comparator for sorting strings based on their natural lexicographic order, while properly handling embedded numeric
 * values in the strings.
 *
 * <p>The comparator compares strings in chunks of numeric and non-numeric parts:</p>
 * <ul>
 *     <li>Non-numeric parts are compared lexicographically (case-insensitive).</li>
 *     <li>Numeric parts are compared numerically, ensuring that numbers are sorted based on their numerical value
 *     rather than their string representation (e.g., "2" is less than "106").</li>
 * </ul>
 *
 * <p>This sorter is useful for cases where strings have mixed content (letters and numbers), and the numeric ordering
 * needs to take precedence where applicable.</p>
 *
 * <p>For example, the following list:</p>
 * <pre>["Destroyed", "Functional", "In Transit (2 days)", "In Transit (106 days)", "Transit (7 days)"]</pre>
 *
 * <p>Will be sorted as:</p>
 * <pre>["Destroyed", "Functional", "In Transit (2 days)", ""Transit (7 days)", In Transit (106 days)"]</pre>
 *
 * @author Illiani
 * @since 0.50.05
 */
public class StringAndThenNumberSorter implements Comparator<String> {
    /**
     * A compiled RegEx pattern used to match and separate numeric and non-numeric parts of a string in the order they
     * appear.
     *
     * <p>The pattern alternates between sequences of digits (numeric parts) and non-digit characters (non-numeric
     * parts). This pattern is useful for parsing strings that need to be interpreted in a manner where numeric and
     * non-numeric parts are processed differently.</p>
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+|\\D+)");

    /**
     * Compares two strings by splitting them into alternating numeric and non-numeric parts, and comparing these parts
     * accordingly.
     *
     * <ul>
     *     <li>If both parts are numeric, they will be compared numerically (e.g., "2" is less than "106").</li>
     *     <li>If the parts are non-numeric, they will be compared lexicographically (case-insensitive).</li>
     *     <li>If one string contains more parts after all the common parts are compared, the string with additional
     *     parts is considered greater.</li>
     * </ul>
     *
     * @param inputString     The first string to compare.
     * @param stringToCompare The second string to compare.
     *
     * @return A negative integer, zero, or a positive integer as the first string is less than, equal to, or greater
     *       than the second string.
     *
     * @author Illiani
     * @since 0.50.05
     */
    @Override
    public int compare(String inputString, String stringToCompare) {
        // Split strings into alternating chunks of numbers and non-numbers
        Matcher inputStringMatcher = NUMBER_PATTERN.matcher(inputString);
        Matcher compareStringMatcher = NUMBER_PATTERN.matcher(stringToCompare);

        while (inputStringMatcher.find() && compareStringMatcher.find()) {
            String part1 = inputStringMatcher.group();
            String part2 = compareStringMatcher.group();

            int result;
            if (isNumber(part1) && isNumber(part2)) {
                // Compare as integers if both parts are numeric
                result = Integer.compare(Integer.parseInt(part1), Integer.parseInt(part2));
            } else {
                // Compare lexicographically if parts are non-numeric
                result = part1.compareToIgnoreCase(part2);
            }

            if (result != 0) {
                return result; // Return if parts differ
            }
        }

        // Input string is greater
        if (inputStringMatcher.find()) {
            return 1;
        }

        // Compare string is greater
        if (compareStringMatcher.find()) {
            return -1;
        }

        // Strings are equal
        return 0;
    }

    /**
     * Checks if a string consists entirely of numeric digits.
     *
     * @param string The string to check.
     *
     * @return {@code true} if the string represents a number (consists of only numeric digits), {@code false}
     *       otherwise.
     *
     * @author Illiani
     * @since 0.50.05
     */
    private boolean isNumber(String string) {
        return string.chars().allMatch(Character::isDigit);
    }
}
