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
 */
package mekhq.campaign.randomEvents.personalities.enums;

import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.campaign.randomEvents.personalities.PersonalityController.PronounData;

import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Represents various levels and traits of intelligence in a personality.
 *
 * <p>
 * This enumeration defines a wide range of intelligence-related traits, each categorized
 * based on its comparison level. Traits are associated with a broader classification
 * through {@link IntelligenceComparison}, allowing for simplified grouping and weighting
 * of intelligence levels.
 * </p>
 *
 * <p>
 * The intelligence levels range from significantly below average to vastly above average,
 * with the ability to generate user-facing descriptions and labels using internationalized
 * resource bundles. Traits also integrate with {@link Gender} to provide personalized and
 * localized descriptions using gender-specific pronouns.
 * </p>
 */
public enum Intelligence {
    // region Enum Declarations
    // Although we no longer use the descriptive names for intelligence traits, we've kept them
    // here as it avoids needing to create a handler for old characters
    BRAIN_DEAD(IntelligenceComparison.SIGNIFICANTLY_BELOW_AVERAGE),
    UNINTELLIGENT(IntelligenceComparison.SIGNIFICANTLY_BELOW_AVERAGE),
    FOOLISH(IntelligenceComparison.SIGNIFICANTLY_BELOW_AVERAGE),
    SIMPLE(IntelligenceComparison.SIGNIFICANTLY_BELOW_AVERAGE),
    SLOW(IntelligenceComparison.SIGNIFICANTLY_BELOW_AVERAGE),
    UNINSPIRED(IntelligenceComparison.SIGNIFICANTLY_BELOW_AVERAGE),
    DULL(IntelligenceComparison.SIGNIFICANTLY_BELOW_AVERAGE),
    DIMWITTED(IntelligenceComparison.SIGNIFICANTLY_BELOW_AVERAGE),
    OBTUSE(IntelligenceComparison.BELOW_AVERAGE),
    LIMITED_INSIGHT(IntelligenceComparison.BELOW_AVERAGE),
    UNDER_PERFORMING(IntelligenceComparison.SLIGHTLY_BELOW_AVERAGE),
    BELOW_AVERAGE(IntelligenceComparison.AVERAGE),
    AVERAGE(IntelligenceComparison.AVERAGE),
    ABOVE_AVERAGE(IntelligenceComparison.AVERAGE),
    STUDIOUS(IntelligenceComparison.SLIGHTLY_ABOVE_AVERAGE),
    DISCERNING(IntelligenceComparison.ABOVE_AVERAGE),
    SHARP(IntelligenceComparison.SIGNIFICANTLY_ABOVE_AVERAGE),
    QUICK_WITTED(IntelligenceComparison.SIGNIFICANTLY_ABOVE_AVERAGE),
    PERCEPTIVE(IntelligenceComparison.SIGNIFICANTLY_ABOVE_AVERAGE),
    BRIGHT(IntelligenceComparison.VASTLY_ABOVE_AVERAGE),
    CLEVER(IntelligenceComparison.VASTLY_ABOVE_AVERAGE),
    INTELLECTUAL(IntelligenceComparison.VASTLY_ABOVE_AVERAGE),
    BRILLIANT(IntelligenceComparison.VASTLY_ABOVE_AVERAGE),
    EXCEPTIONAL(IntelligenceComparison.VASTLY_ABOVE_AVERAGE),
    GENIUS(IntelligenceComparison.VASTLY_ABOVE_AVERAGE);

    /**
     * Enum representing different levels of intelligence comparison. Used when fetching the
     * user-facing description of any given {@link Intelligence} enum.
     *
     * <p>We use this so that we can 'weight' descriptions without needing to create n descriptions
     * per individual Intelligence level. This way intelligence levels with lower frequency can
     * share descriptions reducing the overall writing load.</p>
     */
    public enum IntelligenceComparison {
        SIGNIFICANTLY_BELOW_AVERAGE,
        BELOW_AVERAGE,
        SLIGHTLY_BELOW_AVERAGE,
        AVERAGE,
        SLIGHTLY_ABOVE_AVERAGE,
        ABOVE_AVERAGE,
        SIGNIFICANTLY_ABOVE_AVERAGE,
        VASTLY_ABOVE_AVERAGE,
    }
    // endregion Enum Declarations

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    private final IntelligenceComparison comparison;

    /**
     * Defines the number of individual description variants available for each trait.
     */
    public final static int MAXIMUM_VARIATIONS = 25;

    /**
     * Constructs an instance of the {@link Intelligence} enum, with an associated
     * {@link IntelligenceComparison} value.
     *
     * @param comparison the {@link IntelligenceComparison} enum value to associate with this
     *                  {@link Intelligence} enum value
     */
    Intelligence(IntelligenceComparison comparison) {
        this.comparison = comparison;
    }

    /**
     * Retrieves the {@link IntelligenceComparison} associated with this {@link Intelligence} enum
     * value.
     */
    public IntelligenceComparison getComparison() {
        return comparison;
    }

    /**
     * Retrieves the label associated with the current enumeration value.
     *
     * <p>The label is determined based on the resource bundle for the application,
     * utilizing the enum name combined with a specific key suffix to fetch the
     * relevant localized string.</p>
     *
     * @return the localized label string corresponding to the enumeration value.
     */
    // region Getters
    public String getLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Generates a localized and personalized description for the current enumeration value.
     * <p>
     * This method retrieves a description using the enumeration's name and a specific key suffix
     * derived from the given ambition description index. The description is further customized
     * using the provided gender-specific pronouns, the individual's given name, and other localized
     * text from the resource bundle.
     * </p>
     *
     * @param intelligenceDescriptionIndex an index representing the type/variation of the description.
     *                                   This value is clamped to ensure it falls within a valid range.
     * @param gender                     the {@link Gender} of the individual, used to determine
     *                                   appropriate pronouns for the description.
     * @param givenName                  the given name of the person. This <b>MUST</b> use
     *                                  'person.getGivenName()' and <b>NOT</b> 'person.getFirstName()'
     * @return                           a formatted description string based on the enum,
     *                                   the individual's gender, name, and aggression description index.
     */
    public String getDescription(int intelligenceDescriptionIndex, final Gender gender,
                                 final String givenName) {
        intelligenceDescriptionIndex = clamp(intelligenceDescriptionIndex, 0, MAXIMUM_VARIATIONS - 1);

        final String RESOURCE_KEY = comparison + ".description." + intelligenceDescriptionIndex;
        final PronounData pronounData = new PronounData(gender);

        // {0} = givenName
        // {1} = He/She/They
        // {2} = he/she/they
        // {3} = Him/Her/Them
        // {4} = him/her/them
        // {5} = His/Her/Their
        // {6} = his/her/their
        // {7} = Gender Neutral = 0, Otherwise 1 (used to determine whether to use plural case)

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY, givenName, pronounData.subjectPronoun(),
            pronounData.subjectPronounLowerCase(), pronounData.objectPronoun(), pronounData.objectPronounLowerCase(),
            pronounData.possessivePronoun(), pronounData.possessivePronounLowerCase(), pronounData.pluralizer());
    }

    // region Boolean Comparison Methods
    /**
     * Check if the current instance belongs to the average type category.
     *
     * @return {@code true} if the instance is of average type, {@code false} otherwise.
     */
    public boolean isAverageType() {
        return this.comparison == IntelligenceComparison.AVERAGE;
    }
    // endregion Boolean Comparison Methods

    /**
     * Converts a given string to its corresponding {@code Intelligence} enumeration value.
     * The method first attempts to parse the string as the name of an {@code Intelligence} enum value.
     * If that fails, it attempts to parse the string as an integer representing the ordinal of an
     * {@code Intelligence} enum value. If neither succeeds, it logs an error and defaults to
     * returning {@code AVERAGE}.
     *
     * @param text the input string to parse, which represents either the name or the ordinal
     *             of an {@code Intelligence} enum value.
     * @return the corresponding {@code Intelligence} enum instance for the given input string,
     *         or {@code AVERAGE} if no valid match is found.
     */
    // region File I/O
    public static Intelligence fromString(String text) {
        try {
            return Intelligence.valueOf(text);
        } catch (Exception ignored) {}

        try {
            return Intelligence.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}


        MMLogger logger = MMLogger.create(Intelligence.class);
        logger.error("Unknown Intelligence ordinal: {} - returning {}.", text, AVERAGE);

        return AVERAGE;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    /**
     * Evaluates 'intelligence score', an int representation of how intelligent a
     * character is.
     *
     * @return The calculated intelligence score.
     */
    public int getIntelligenceScore() {
        return this.ordinal() - (Intelligence.values().length / 2);
    }
}
