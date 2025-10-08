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

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

/**
 * A comparator implementation for sorting strings representing numerical scores in the format "x / y". The sorting is
 * performed by the first number `x`, and in case of ties, by the second number `y`.
 *
 * <p>This class is typically used for sorting a character's
 * {@link mekhq.campaign.personnel.skills.enums.SkillAttribute} scores for display in
 * {@link mekhq.gui.enums.PersonnelTableModelColumn}.</p>
 */
public class AttributeScoreSorter implements Comparator<String> {
    private static final MMLogger LOGGER = MMLogger.create(AttributeScoreSorter.class);

    private final String DIVIDER_STRING = " / ";

    /**
     * Compares two strings in the format "x / y" for sorting.
     *
     * <p>The comparison is performed as follows:</p>
     * <ol>
     *     <li>The first numbers (`x` values) from both strings are compared numerically.</li>
     *     <li>If the `x` values are the same, the second numbers (`y` values) are compared numerically.</li>
     * </ol>
     *
     * @param firstString  the first string in the format "x / y".
     * @param secondString the second string in the format "x / y".
     *
     * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater
     *       than the second.
     */
    @Override
    public int compare(String firstString, String secondString) {
        try {
            // First String
            String[] firstStringParts = firstString.split(DIVIDER_STRING);
            int firstStringFirstNumber = MathUtility.parseInt(firstStringParts[0].trim());
            int firstStringSecondNumber = MathUtility.parseInt(firstStringParts[1].trim());

            // Second String
            String[] secondStringParts = secondString.split(DIVIDER_STRING);
            int secondStringFirstNumber = MathUtility.parseInt(secondStringParts[0].trim());
            int secondStringSecondNumber = MathUtility.parseInt(secondStringParts[1].trim());

            // Compare the first numbers
            int result = Integer.compare(firstStringFirstNumber, secondStringFirstNumber);

            // If the first numbers are the same, compare the second numbers
            if (result == 0) {
                result = Integer.compare(firstStringSecondNumber, secondStringSecondNumber);
            }

            return result;
            // This means the strings are malformed and can't be split into two parts using the divider
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Error parsing attribute score string: {} or {}", firstString, secondString);

            return 1; // By default, malformed strings go last
        }
    }
}
