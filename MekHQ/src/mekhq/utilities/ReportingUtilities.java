/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.utilities;

/**
 * This class has a collection of values and methods to make writing out various
 * parts of reports and XML easier by using methods for common outputs.
 *
 * @author Richard J Hancock
 */
public class ReportingUtilities {
    /**
     * Just the closing part of the tag to prevent potential issues with strings in
     * Java files and the warnings about constants when using the same string in
     * multiple places.
     *
     */
    public static final String CLOSING_SPAN_TAG = "</span>";

    /**
     * Private constructor as we have no use to initialize this for anything. All
     * static methods to standardize output of various parts of reports. More need
     * to be added as code is looked at and refactored.
     */
    private ReportingUtilities() {
        // No public use for this class, Only static.
    }

    /**
     * Accepts a string of a color code to be used within an HTML span tag. Will
     * output the full opening tag.
     *
     * @param colorToUse What color to make the eventual text.
     * @return The formatted string for the opening tag.
     */
    public static String spanOpeningWithCustomColor(String colorToUse) {
        return String.format("<span color='%s'>", colorToUse);
    }

    /**
     * Takes the color and a message to create a full <span></span> message
     * for output to simplify the process. Uses
     * {@link #spanOpeningWithCustomColor(String)} and {@link CLOSING_SPAN_TAG} in
     * the process of formation.
     *
     * @param colorToUse Color for the text within the span tag.
     * @param message    Message to output.
     * @return Formatted string with color and message.
     */
    public static String messageSurroundedBySpanWithColor(String colorToUse, String message) {
        return String.format("%s%s%s", spanOpeningWithCustomColor(colorToUse), message, CLOSING_SPAN_TAG);
    }
}
