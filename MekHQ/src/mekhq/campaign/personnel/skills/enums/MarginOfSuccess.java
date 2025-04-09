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
package mekhq.campaign.personnel.skills.enums;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.logging.MMLogger;

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
 * </ul>
 *
 * <p>For example, {@link #SPECTACULAR} represents a margin of success in the range of 7 to {@link Integer#MAX_VALUE},
 * while {@link #DISASTROUS} represents a margin of success in the range of {@link Integer#MIN_VALUE} to -7.</p>
 *
 * @author Illiani
 * @since 0.50.5
 */
public enum MarginOfSuccess {
    SPECTACULAR(7, Integer.MAX_VALUE, 4),
    EXTRAORDINARY(5, 6, 3),
    GOOD(3, 4, 2),
    IT_WILL_DO(1, 2, 1),
    BARELY_MADE_IT(0, 0, 0),
    ALMOST(-2, -1, -1),
    BAD(-4, -3, -2),
    TERRIBLE(-6, -5, -3),
    DISASTROUS(Integer.MIN_VALUE, -7, -4);

    private static final MMLogger logger = MMLogger.create(MarginOfSuccess.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources." + MarginOfSuccess.class.getSimpleName();

    private final int lowerBound;
    private final int upperBound;
    private final int margin;

    /**
     * Constructs a {@link MarginOfSuccess} enum constant with the specified bounds and margin value.
     *
     * @param lowerBound the lower inclusive bound for this margin of success
     * @param upperBound the upper inclusive bound for this margin of success
     * @param margin     the margin value associated with this range
     *
     * @author Illiani
     * @since 0.50.5
     */
    MarginOfSuccess(int lowerBound, int upperBound, int margin) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.margin = margin;
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
     * @since 0.50.5
     */
    public static int getMarginValue(MarginOfSuccess marginOfSuccess) {
        return marginOfSuccess.margin;
    }

    /**
     * Retrieves the margin of success for the specified roll value.
     *
     * <p>This method matches the provided roll value against the bounds of each {@link MarginOfSuccess} constant to
     * determine the appropriate range and calculate the roll's margin.</p>
     *
     * @param roll the roll value to evaluate
     *
     * @return the margin (calculated as {@code roll - lowerBound}) corresponding to the matching
     *       {@link MarginOfSuccess} range, or the margin for {@link #DISASTROUS} if no matching range is found
     *
     * @author Illiani
     * @since 0.50.5
     */
    public static int getMarginOfSuccess(int roll) {
        for (MarginOfSuccess mos : MarginOfSuccess.values()) {
            if (roll >= mos.lowerBound && roll <= mos.upperBound) {
                return roll - mos.lowerBound;
            }
        }

        logger.error("Unknown MarginOfSuccess value: {} - returning {}.", roll, DISASTROUS);

        return DISASTROUS.margin;
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
     * @since 0.50.5
     */
    public static String getMarginOfSuccessString(int marginOfSuccess) {
        return getFormattedTextAt(RESOURCE_BUNDLE, marginOfSuccess + ".label");
    }
}
