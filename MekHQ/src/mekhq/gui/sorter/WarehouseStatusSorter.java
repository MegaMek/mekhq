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
package mekhq.gui.sorter;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

/**
 * Custom comparator to sort strings alphabetically, unless the string contains a phrase indicating a number of "day
 * (s)" (e.g., "In Transit (5 days)"). Strings with "day(s)" are sorted numerically by the number of days first, while
 * those without are sorted alphabetically.
 *
 * <p>Example List Before Sorting:</p>
 * <pre>["Functional", "Broken", "Damaged", "In Transit (5 days)", "In Transit (1 day)", "Transit (101 days)"]</pre>
 *
 * <p>Example List After Sorting:</p>
 * <pre>["Broken", "Damaged", "Functional", "In Transit (1 day)", "In Transit (5 days)", "Transit (101 days)"]</pre>
 *
 * @author Illiani
 * @since 0.50.05
 */
public class WarehouseStatusSorter implements Comparator<String> {
    private static final MMLogger LOGGER = MMLogger.create(WarehouseStatusSorter.class);

    /**
     * Regular expression pattern to locate the number of "days" in a string.
     *
     * <p>Matches strings like "(5 days)" or "(1 day)" and captures the numeric part.</p>
     */
    private static final Pattern DAYS_PATTERN = Pattern.compile("\\((\\d+)\\s*day(s)?\\)");

    /**
     * Compares two strings and determines their sorting order.
     *
     * <ul>
     *     <li>If both strings contain "day(s)", they are compared numerically based on the number of days.</li>
     *     <li>If only one string contains "day(s)", it is considered greater (sorted after) strings without "day(s)".</li>
     *     <li>If neither string contains "day(s)", they are compared alphabetically (case-insensitive).</li>
     * </ul>
     *
     * @param firstString  the first string to be compared
     * @param secondString the second string to be compared
     *
     * @return a negative integer, zero, or a positive integer as the first string is less than, equal to, or greater
     *       than the second string respectively
     *
     * @author Illiani
     * @since 0.50.05
     */
    @Override
    public int compare(String firstString, String secondString) {
        // Check if either string contains "day" or "days"
        boolean firstStringContainsDays = DAYS_PATTERN.matcher(firstString).find();
        boolean secondStringContainsDays = DAYS_PATTERN.matcher(secondString).find();

        // If both strings contain "day(s)", compare numerically
        if (firstStringContainsDays && secondStringContainsDays) {
            int firstStringDayCount = extractDays(firstString);
            int secondStringDayCount = extractDays(secondString);
            return Integer.compare(firstStringDayCount, secondStringDayCount);
        }
        // If only the first string contains "day(s)", it is greater
        else if (firstStringContainsDays) {
            return 1;
        }
        // If only the second string contains "day(s)", it is greater
        else if (secondStringContainsDays) {
            return -1;
        }
        // If neither contains "day(s)", fallback to alphabetical comparison
        else {
            return firstString.compareToIgnoreCase(secondString);
        }
    }

    /**
     * Extracts the numeric value representing "days" from a string.
     *
     * <p>The method searches for a pattern matching a number followed by "day(s)" (e.g., "(5 days)"). If found, it
     * parses and returns the numeric value.</p>
     *
     * <p>If no numeric value is found, or parsing fails, it defaults to {@link Integer#MAX_VALUE}.</p>
     *
     * @param string the input string to search
     *
     * @return the numeric representation of "days", or {@link Integer#MAX_VALUE} if no match is found
     *
     * @author Illiani
     * @since 0.50.05
     */
    private int extractDays(String string) {
        Matcher matcher = DAYS_PATTERN.matcher(string);
        if (matcher.find()) {
            // We're using the group value here, as we want to compare the entire number not just the first integer
            // we hit.
            return MathUtility.parseInt(matcher.group(1), Integer.MAX_VALUE);
        }

        // This is a fallback value, in the event the Regex picks up that the String contains numbers, but for some
        // reason can't parse them. I don't expect this to ever be used, but if it is, we'll want to address the error.
        LOGGER.error("Matcher failed to extract Integer from String: {}", string);

        return Integer.MAX_VALUE;
    }
}
