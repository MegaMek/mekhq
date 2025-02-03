/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.personalities.enums;

import megamek.logging.MMLogger;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public enum Ambition {
    // region Enum Declarations
    NONE(false, false),
    AMBITIOUS(true, false),
    ARROGANT(false, false),
    ASPIRING(true, false),
    CALCULATING(true, false),
    CONNIVING(false, false),
    CONTROLLING(false, false),
    CUTTHROAT(false, false),
    DILIGENT(true, false),
    DRIVEN(true, false),
    ENERGETIC(true, false),
    EXCESSIVE(false, false),
    FOCUSED(true, false),
    GOAL_ORIENTED(true, false),
    MOTIVATED(true, false),
    OPPORTUNISTIC(true, false),
    OVERCONFIDENT(false, false),
    PERSISTENT(true, false),
    PROACTIVE(true, false),
    RESILIENT(true, false),
    RUTHLESS(false, false),
    SELFISH(false, false),
    STRATEGIC(true, false),
    UNAMBITIOUS(false, false),
    UNSCRUPULOUS(false, false),
    // Major Traits should always be last
    DISHONEST(false, true),
    INNOVATIVE(true, true),
    MANIPULATIVE(false, true),
    RESOURCEFUL(true, true),
    TYRANNICAL(false, true),
    VISIONARY(true, true);
    // endregion Enum Declarations

    // region Variable Declarations
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Ambition(boolean isPositive, boolean isMajor) {
        this.isPositive = isPositive;
        this.isMajor = isMajor;
    }
    // endregion Constructors

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    // region Getters
    /**
     * Retrieves the label associated with the current enumeration value.
     *
     * <p>The label is determined based on the resource bundle for the application,
     * utilizing the enum name combined with a specific key suffix to fetch the
     * relevant localized string.</p>
     *
     * @return the localized label string corresponding to the enumeration value.
     */
    public String getLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the description of the current enumeration value.
     *
     * <p>The label is determined based on the resource bundle for the application,
     * utilizing the enum name combined with a specific key suffix to fetch the
     * relevant localized string.</p>
     *
     * @return the description associated with this enumeration value
     */
    public String getDescription() {
        final String RESOURCE_KEY = name() + ".description";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * @return {@code true} if the personality trait is considered positive,
     *         {@code false} otherwise.
     */
    public boolean isTraitPositive() {
        return isPositive;
    }

    /**
     * @return {@code true} if the personality trait is considered a major trait,
     *         {@code false} otherwise.
     */
    public boolean isTraitMajor() {
        return isMajor;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }
    // endregion Boolean Comparison Methods

    /**
     * Converts the specified string into its corresponding Ambition enum value.
     * The method attempts to interpret the string as either the name of an enum constant
     * or an ordinal value of the enum. If the conversion fails, the method logs an error
     * and returns the default value {@code NONE}.
     *
     * @param text the string to be converted into an Ambition enum value. It can be the name
     *             of the enum constant or its ordinal value as a string.
     * @return the corresponding Ambition enum constant if the string matches a name or
     *         ordinal value, otherwise {@code NONE}.
     */
    // region File I/O
    public static Ambition fromString(String text) {
        try {
            return Ambition.valueOf(text);
        } catch (Exception ignored) {}

        try {
            return Ambition.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}


        MMLogger logger = MMLogger.create(Ambition.class);
        logger.error("Unknown Ambition ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
