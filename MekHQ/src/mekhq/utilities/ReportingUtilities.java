/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.utilities;

import mekhq.MekHQ;

/**
 * This class has a collection of values and methods to make writing out various parts of reports and XML easier by
 * using methods for common outputs.
 *
 * @author Richard J Hancock
 */
public class ReportingUtilities {
    /**
     * Just the closing part of the tag to prevent potential issues with strings in Java files and the warnings about
     * constants when using the same string in multiple places.
     *
     */
    public static final String CLOSING_SPAN_TAG = "</span>";

    /**
     * Private constructor as we have no use to initialize this for anything. All static methods to standardize output
     * of various parts of reports. More need to be added as code is looked at and refactored.
     */
    private ReportingUtilities() {
        // No public use for this class, Only static.
    }

    /**
     * Accepts a string of a color code to be used within an HTML span tag. Will output the full opening tag.
     *
     * @param colorToUse What color to make the eventual text.
     *
     * @return The formatted string for the opening tag.
     */
    public static String spanOpeningWithCustomColor(String colorToUse) {
        return String.format("<span color='%s'>", colorToUse);
    }

    /**
     * Takes the color and a message to create a full <span></span> message for output to simplify the process. Uses
     * {@link #spanOpeningWithCustomColor(String)} and {@link #CLOSING_SPAN_TAG} in the process of formation.
     *
     * @param colorToUse Color for the text within the span tag.
     * @param message    Message to output.
     *
     * @return Formatted string with color and message.
     */
    public static String messageSurroundedBySpanWithColor(String colorToUse, String message) {
        return String.format("%s%s%s", spanOpeningWithCustomColor(colorToUse), message, CLOSING_SPAN_TAG);
    }

    /**
     * Wraps the center argument with the start and end arguments if the center argument is not blank or null. For your
     * optional parenthetical's and such.
     *
     * @param start String to begin with
     * @param main  String to contain, if it exists
     * @param end   String to end with
     *
     * @return String start + main + end if main else ""
     */
    public static String surroundIf(String start, String main, String end) {
        if (null == main || main.isEmpty()) {
            return "";
        }
        return String.format("%s%s%s", start, main, end);
    }

    /**
     * Connects the first string with the second using separator, if both strings are non-null and non-empty. If only
     * one string is valid, return that string. If neither string is valid, return "". For when using a StringJoiner is
     * just overkill.
     *
     * @param first     String to begin with
     * @param separator String to separate with
     * @param second    String to end with
     *
     * @return String first + separator + second or first or second or ""
     */
    public static String separateIf(String first, String separator, String second) {
        boolean isFirst = (null != first) && (!first.isEmpty());
        boolean isSecond = (null != second) && (!second.isEmpty());

        if (isFirst && isSecond) {
            return String.format("%s%s%s", first, separator, second);
        } else if (isFirst) {
            return first;
        } else if (isSecond) {
            return second;
        } else {
            return "";
        }
    }

    /**
     * Returns the hex color code used for an amazing status or messages.
     *
     * @return the hex color string representing a amazing color
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static String getAmazingColor() {
        return MekHQ.getMHQOptions().getFontColorAmazingHexColor();
    }

    /**
     * Returns the hex color code used for positive status or messages.
     *
     * @return the hex color string representing a positive color
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static String getPositiveColor() {
        return MekHQ.getMHQOptions().getFontColorPositiveHexColor();
    }

    /**
     * Returns the hex color code used for warning status or messages.
     *
     * @return the hex color string representing a warning color
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static String getWarningColor() {
        return MekHQ.getMHQOptions().getFontColorWarningHexColor();
    }

    /**
     * Returns the hex color code used for negative status or messages.
     *
     * @return the hex color string representing a negative color
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static String getNegativeColor() {
        return MekHQ.getMHQOptions().getFontColorNegativeHexColor();
    }
}
