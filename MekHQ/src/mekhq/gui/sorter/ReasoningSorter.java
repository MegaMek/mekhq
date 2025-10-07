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

/**
 * A comparator for sorting strings that end with a numeric value in parentheses.
 *
 * <p>This is designed for entries in the form "{@code Some Value (N)}", where {@code N} is a non-negative
 * integer. Entries will be sorted in ascending order according to {@code N}, ignoring all other parts of the
 * string.</p>
 *
 * <p>Malformed strings that do not end with a {@code (N)} pattern will be placed last in the sort order.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class ReasoningSorter implements Comparator<String> {
    private static final Pattern NUM_PATTERN = Pattern.compile("\\((\\d+)\\)\\s*$");

    /**
     * Extracts the integer in trailing parentheses from the given string.
     *
     * <p>If the pattern is not found, {@link Integer#MAX_VALUE} is returned.</p>
     *
     * @param reasoningLabel the string to parse
     *
     * @return the integer within parentheses, or {@link Integer#MAX_VALUE} if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static int extractNumber(String reasoningLabel) {
        Matcher matcher = NUM_PATTERN.matcher(reasoningLabel);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        // Put malformed strings last
        return Integer.MAX_VALUE;
    }

    @Override
    public int compare(String firstString, String secondString) {
        return Integer.compare(extractNumber(firstString), extractNumber(secondString));
    }
}
