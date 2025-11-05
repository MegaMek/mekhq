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
package mekhq.campaign.randomEvents.personalities.enums;

import static java.lang.Math.round;
import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType.REASONING;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.codeUtilities.MathUtility;
import megamek.common.enums.Gender;
import mekhq.campaign.personnel.PronounData;

/**
 * Represents various levels and traits of Reasoning in a personality.
 *
 * <p>
 * This enumeration defines a wide range of Reasoning-related traits, each categorized based on its comparison level.
 * Traits are associated with a broader classification through {@link ReasoningComparison}, allowing for simplified
 * grouping and weighting of Reasoning levels.
 * </p>
 *
 * <p>
 * The Reasoning levels range from significantly below average to vastly above average, with the ability to generate
 * user-facing descriptions and labels using internationalized resource bundles. Traits also integrate with
 * {@link Gender} to provide personalized and localized descriptions using gender-specific pronouns.
 * </p>
 */
public enum Reasoning {
    // region Enum Declarations
    // Although we no longer use the descriptive names for Reasoning traits, we've kept them
    // here as it avoids needing to create a handler for old characters
    BRAIN_DEAD(ReasoningComparison.SIGNIFICANTLY_BELOW_AVERAGE, 0),
    UNINTELLIGENT(ReasoningComparison.SIGNIFICANTLY_BELOW_AVERAGE, 1),
    FOOLISH(ReasoningComparison.SIGNIFICANTLY_BELOW_AVERAGE, 2),
    SIMPLE(ReasoningComparison.SIGNIFICANTLY_BELOW_AVERAGE, 3),
    SLOW(ReasoningComparison.SIGNIFICANTLY_BELOW_AVERAGE, 4),
    UNINSPIRED(ReasoningComparison.SIGNIFICANTLY_BELOW_AVERAGE, 5),
    DULL(ReasoningComparison.SIGNIFICANTLY_BELOW_AVERAGE, 6),
    DIMWITTED(ReasoningComparison.SIGNIFICANTLY_BELOW_AVERAGE, 7),
    OBTUSE(ReasoningComparison.BELOW_AVERAGE, 8),
    LIMITED_INSIGHT(ReasoningComparison.BELOW_AVERAGE, 9),
    UNDER_PERFORMING(ReasoningComparison.SLIGHTLY_BELOW_AVERAGE, 10),
    BELOW_AVERAGE(ReasoningComparison.AVERAGE, 11),
    AVERAGE(ReasoningComparison.AVERAGE, 12),
    ABOVE_AVERAGE(ReasoningComparison.AVERAGE, 13),
    STUDIOUS(ReasoningComparison.SLIGHTLY_ABOVE_AVERAGE, 14),
    DISCERNING(ReasoningComparison.ABOVE_AVERAGE, 15),
    SHARP(ReasoningComparison.SIGNIFICANTLY_ABOVE_AVERAGE, 16),
    QUICK_WITTED(ReasoningComparison.SIGNIFICANTLY_ABOVE_AVERAGE, 17),
    PERCEPTIVE(ReasoningComparison.SIGNIFICANTLY_ABOVE_AVERAGE, 18),
    BRIGHT(ReasoningComparison.VASTLY_ABOVE_AVERAGE, 19),
    CLEVER(ReasoningComparison.VASTLY_ABOVE_AVERAGE, 20),
    INTELLECTUAL(ReasoningComparison.VASTLY_ABOVE_AVERAGE, 21),
    BRILLIANT(ReasoningComparison.VASTLY_ABOVE_AVERAGE, 22),
    EXCEPTIONAL(ReasoningComparison.VASTLY_ABOVE_AVERAGE, 23),
    GENIUS(ReasoningComparison.VASTLY_ABOVE_AVERAGE, 24);

    /**
     * Enum representing different levels of Reasoning comparison. Used when fetching the user-facing description of any
     * given {@link Reasoning} enum.
     *
     * <p>We use this so that we can 'weight' descriptions without needing to create n descriptions
     * per individual Reasoning level. This way Reasoning levels with lower frequency can share descriptions reducing
     * the overall writing load.</p>
     */
    public enum ReasoningComparison {
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

    private final ReasoningComparison comparison;
    private final int level;

    /**
     * @deprecated only used in deprecated methods
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public final static int MAXIMUM_VARIATIONS = 25;

    /**
     * Constructs an instance of the {@link Reasoning} enum, with an associated {@link ReasoningComparison} value.
     *
     * @param comparison the {@link ReasoningComparison} enum value to associate with this {@link Reasoning} enum value
     * @param level      The integer score associated with this {@link Reasoning} enum value
     */
    Reasoning(ReasoningComparison comparison, int level) {
        this.comparison = comparison;
        this.level = level;
    }

    /**
     * Retrieves the {@link ReasoningComparison} associated with this {@link Reasoning} enum value.
     */
    public ReasoningComparison getComparison() {
        return comparison;
    }

    /**
     * Retrieves the Reasoning rating associated with this {@link Reasoning} enum.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Retrieves the label associated with the current enumeration value.
     *
     * <p>The label is determined based on the resource bundle for the application,
     * utilizing the enum name combined with a specific key suffix to fetch the relevant localized string.</p>
     *
     * @return the localized label string corresponding to the enumeration value.
     */
    // region Getters
    public String getLabel() {
        final String RESOURCE_KEY = name() + ".label";
        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY) + " (" + level + ")";
    }

    /**
     * @deprecated No longer used.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public String getDescription(int reasoningDescriptionIndex, final Gender gender, final String givenName) {
        reasoningDescriptionIndex = clamp(reasoningDescriptionIndex, 0, MAXIMUM_VARIATIONS - 1);

        final String RESOURCE_KEY = comparison + ".description." + reasoningDescriptionIndex;
        final PronounData pronounData = new PronounData(gender);

        // {0} = givenName
        // {1} = He/She/They
        // {2} = he/she/they
        // {3} = Him/Her/Them
        // {4} = him/her/them
        // {5} = His/Her/Their
        // {6} = his/her/their
        // {7} = Gender Neutral = 0, Otherwise 1 (used to determine whether to use a plural case)

        return getFormattedTextAt(RESOURCE_BUNDLE,
              RESOURCE_KEY,
              givenName,
              pronounData.subjectPronoun(),
              pronounData.subjectPronounLowerCase(),
              pronounData.objectPronoun(),
              pronounData.objectPronounLowerCase(),
              pronounData.possessivePronoun(),
              pronounData.possessivePronounLowerCase(),
              pronounData.pluralizer());
    }

    /**
     * Retrieves the formatted exam results text.
     *
     * <p>Calculates the result percentage based on the current {@code level} relative to {@code GENIUS.level},
     * and uses this value to format the exam results text from the resource bundle.</p>
     *
     * @return the formatted exam results string with the calculated percentage inserted.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getExamResults() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "examResults.text", getExamScore());
    }

    /**
     * Retrieves the formatted exam results text.
     *
     * <p>Calculates the result percentage based on the current {@code level} relative to {@code GENIUS.level}.</p>
     *
     * @return the exam results as a calculated percentage.
     *
     * @author Illiani
     * @since 0.50.010
     */
    public int getExamScore() {
        int results = (int) round(((double) this.level / GENIUS.level) * 100) - 5;
        results += randomInt(11);
        results = clamp(results, 0, 100);

        return results;
    }

    // region Boolean Comparison Methods

    /**
     * Check if the current instance belongs to the average type category.
     *
     * @return {@code true} if the instance is of average type, {@code false} otherwise.
     */
    public boolean isAverageType() {
        return this.comparison == ReasoningComparison.AVERAGE;
    }
    // endregion Boolean Comparison Methods

    /**
     * @deprecated replaced by {@link #getReasoningScore()}
     */
    @Deprecated(since = "0.50.5", forRemoval = true)
    public int getIntelligenceScore() {
        return getReasoningScore();
    }

    /**
     * Evaluates 'Reasoning score', an int representation of how intelligent a character is.
     *
     * @return The calculated Reasoning score.
     */
    public int getReasoningScore() {
        return this.level - (Reasoning.values().length / 2);
    }

    /**
     * @return the {@link PersonalityTraitType} representing reasoning
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonalityTraitType getPersonalityTraitType() {
        return REASONING;
    }

    /**
     * @return the label string for the reasoning personality trait type
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getPersonalityTraitTypeLabel() {
        return getPersonalityTraitType().getLabel();
    }

    /**
     * Converts a given string to its corresponding {@code Reasoning} enumeration value. The method first attempts to
     * parse the string as the name of an {@code Reasoning} enum value. If that fails, it attempts to parse the string
     * as an integer representing the ordinal of an {@code Reasoning} enum value. If neither succeeds, it logs an error
     * and defaults to returning {@code AVERAGE}.
     *
     * @param text the input string to parse, which represents either the name or the ordinal of an {@code Reasoning}
     *             enum value.
     *
     * @return the corresponding {@code Reasoning} enum instance for the given input string, or {@code AVERAGE} if no
     *       valid match is found.
     */
    // region File I/O
    public static Reasoning fromString(String text) {
        try {
            return Reasoning.valueOf(text);
        } catch (Exception ignored) {
        }

        return Reasoning.values()[MathUtility.parseInt(text, AVERAGE.level)];
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
