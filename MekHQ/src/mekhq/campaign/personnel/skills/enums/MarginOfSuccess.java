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
import mekhq.utilities.ReportingUtilities;

/**
 * Represents margins of success (mos) that define ranges of roll results with an associated integer margin, lower
 * bound, and upper bound.
 *
 * <p>It is used to categorize results such as skill checks into predefined ranges and retrieve associated values or
 * labels for those results.</p>
 *
 * <p>Each enum constant represents a performance level and is associated with:</p>
 * <ul>
 *     <li>A lower bound (inclusive),</li>
 *     <li>An upper bound (inclusive),</li>
 *     <li>A margin of success value.</li>
 *     <li>A reporting color.</li>
 * </ul>
 *
 * <p>For example, {@link #SPECTACULAR} represents a margin of success in the range of 7 to {@link Integer#MAX_VALUE},
 * while {@link #DISASTROUS} represents a margin of success in the range of {@link Integer#MIN_VALUE} to -7.</p>
 *
 * @author Illiani
 * @since 0.50.05
 */
public enum MarginOfSuccess {
    SPECTACULAR(7, Integer.MAX_VALUE, 4, ReportingUtilities.getAmazingColor()),
    EXTRAORDINARY(5, 6, 3, ReportingUtilities.getPositiveColor()),
    GOOD(3, 4, 2, ReportingUtilities.getPositiveColor()),
    IT_WILL_DO(1, 2, 1, ReportingUtilities.getWarningColor()),
    BARELY_MADE_IT(0, 0, 0, ReportingUtilities.getWarningColor()),
    ALMOST(-2, -1, -1, ReportingUtilities.getWarningColor()),
    BAD(-4, -3, -2, ReportingUtilities.getNegativeColor()),
    TERRIBLE(-6, -5, -3, ReportingUtilities.getNegativeColor()),
    DISASTROUS(Integer.MIN_VALUE, -7, -4, ReportingUtilities.getNegativeColor());

    private static final MMLogger LOGGER = MMLogger.create(MarginOfSuccess.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.MarginOfSuccess";

    private final String label;
    private final int lowerBound;
    private final int upperBound;
    private final int margin;
    private final String color;

    /**
     * Constructs a {@link MarginOfSuccess} enum constant with the specified bounds and margin value.
     *
     * @param lowerBound the lower inclusive bound for this margin of success
     * @param upperBound the upper inclusive bound for this margin of success
     * @param margin     the margin value associated with this range
     * @param color      the color of reporting text
     *
     * @author Illiani
     * @since 0.50.05
     */
    MarginOfSuccess(int lowerBound, int upperBound, int margin, String color) {
        this.label = generateMarginOfSuccessString();
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.margin = margin;
        this.color = color;
    }

    /**
     * Retrieves the margin value associated with the specified {@link MarginOfSuccess}.
     *
     * <p>The margin value represents the numerical value tied to a specific margin of success, typically used to
     * measure the degree of success or failure of a skill check.</p>
     *
     * @param marginOfSuccess the {@link MarginOfSuccess} whose margin value is to be retrieved
     *
     * @return the margin value associated with the given {@link MarginOfSuccess}
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static int getMarginValue(MarginOfSuccess marginOfSuccess) {
        return marginOfSuccess.margin;
    }

    /**
     * Determines the margin of success as an integer based on the difference between the roll and the target.
     *
     * <p>This method calculates the margin of success using the given difference and returns the associated
     * margin as an integer. Internally, it utilizes {@link #getMarginOfSuccessObject(int)} to determine the relevant
     * margin category.</p>
     *
     * @param differenceBetweenRollAndTarget The difference between the roll result and the target value.
     *
     * @return The margin of success as an integer.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static int getMarginOfSuccess(int differenceBetweenRollAndTarget) {
        return getMarginOfSuccessObject(differenceBetweenRollAndTarget).margin;
    }

    /**
     * Returns the color associated with the specified {@link MarginOfSuccess} value.
     *
     * <p>This method allows retrieval of a string representing a display color associated with a given margin of
     * success outcome, which is typically used for formatting or UI rendering purposes.</p>
     *
     * @param marginOfSuccess The {@link MarginOfSuccess} for which to retrieve the associated color string.
     *
     * @return The color string defined for the given margin of success.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static String getMarginOfSuccessColor(MarginOfSuccess marginOfSuccess) {
        return marginOfSuccess.color;
    }

    /**
     * Determines the {@link MarginOfSuccess} category based on the difference between the roll and the target.
     *
     * <p>This method iterates through all possible {@link MarginOfSuccess} values and compares the provided
     * difference to their defined bounds ({@code lowerBound} and {@code upperBound}). If a matching range is found, it
     * returns the corresponding {@link MarginOfSuccess} object.</p>
     *
     * <p>If no matching category is found, an error message is logged, and the method returns the
     * {@link MarginOfSuccess#DISASTROUS} category as a fallback.</p>
     *
     * @param differenceBetweenRollAndTarget The difference between the roll result and the target value.
     *
     * @return The {@link MarginOfSuccess} object that corresponds to the provided difference.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static MarginOfSuccess getMarginOfSuccessObject(int differenceBetweenRollAndTarget) {
        for (MarginOfSuccess margin : MarginOfSuccess.values()) {
            if ((differenceBetweenRollAndTarget >= margin.lowerBound) &&
                      (differenceBetweenRollAndTarget <= margin.upperBound)) {
                return margin;
            }
        }
        LOGGER.error("No valid MarginOfSuccess found for roll: {}. Returning DISASTROUS",
              differenceBetweenRollAndTarget);
        return DISASTROUS;
    }

    /**
     * Retrieves the {@link MarginOfSuccess} object corresponding to the specified margin value.
     *
     * <p>This method iterates through all possible {@link MarginOfSuccess} values and returns the one
     * whose associated margin matches the provided {@code marginValue}.</p>
     *
     * <p>If no matching {@link MarginOfSuccess} is found, an error is logged, and the method
     * defaults to returning {@link MarginOfSuccess#DISASTROUS}.</p>
     *
     * @param marginValue The integer margin value to look up.
     *
     * @return The {@link MarginOfSuccess} object corresponding to the given margin value, or
     *       {@link MarginOfSuccess#DISASTROUS} if no match is found.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static MarginOfSuccess getMarginOfSuccessObjectFromMarginValue(int marginValue) {
        for (MarginOfSuccess marginOfSuccess : MarginOfSuccess.values()) {
            if (marginOfSuccess.margin == marginValue) {
                return marginOfSuccess;
            }
        }

        LOGGER.error("No valid MarginOfSuccess found for marginValue: {}. Returning DISASTROUS", marginValue);
        return DISASTROUS;
    }

    /**
     * Retrieves the localized string label for a given margin of success.
     *
     * <p>This method looks up the label from the associated resource bundle, using the specified margin of success
     * as a key, suffixed with {@code .label}.</p>
     *
     * @param marginOfSuccess the margin of success for which to retrieve the label
     *
     * @return the localized string representing the given margin of success
     *
     * @author Illiani
     * @since 0.50.05
     */
    public static String getMarginOfSuccessString(MarginOfSuccess marginOfSuccess) {
        return marginOfSuccess.label;
    }

    private String generateMarginOfSuccessString() {
        return getTextAt(RESOURCE_BUNDLE, margin + ".label");
    }
}
