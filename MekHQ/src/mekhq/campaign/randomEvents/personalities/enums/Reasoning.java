/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType.REASONING;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.codeUtilities.MathUtility;

/**
 * Represents various levels and traits of Reasoning in a personality.
 *
 * <p>
 * This enumeration defines a wide range of Reasoning-related traits, each categorized based on its comparison level.
 * </p>
 */
public enum Reasoning {
    // region Enum Declarations
    // Although we no longer use the descriptive names for Reasoning traits, we've kept them
    // here as it avoids needing to create a handler for old characters
    BRAIN_DEAD(0),
    UNINTELLIGENT(1),
    FOOLISH(2),
    SIMPLE(3),
    SLOW(4),
    UNINSPIRED(5),
    DULL(6),
    DIMWITTED(7),
    OBTUSE(8),
    LIMITED_INSIGHT(9),
    UNDER_PERFORMING(10),
    BELOW_AVERAGE(11),
    AVERAGE(12),
    ABOVE_AVERAGE(13),
    STUDIOUS(14),
    DISCERNING(15),
    SHARP(16),
    QUICK_WITTED(17),
    PERCEPTIVE(18),
    BRIGHT(19),
    CLEVER(20),
    INTELLECTUAL(21),
    BRILLIANT(22),
    EXCEPTIONAL(23),
    GENIUS(24);
    // endregion Enum Declarations

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    private final String label;
    private final int level;

    /**
     * Constructs an instance of the {@link Reasoning} enum
     *
     * @param level The integer score associated with this {@link Reasoning} enum value
     */
    Reasoning(int level) {
        this.level = level;
        this.label = generateLabel();
    }

    public String getLabel() {
        return label;
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
    private String generateLabel() {
        final String RESOURCE_KEY = name() + ".label";
        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY) + " (" + level + ")";
    }

    /**
     * Retrieves the formatted exam results text.
     *
     * <p>Uses the supplied result percentage (that was calculated earlier using {@link #getExamScore()} ) to format
     * the exam results text from the resource bundle.</p>
     *
     * @return the formatted exam results string with the supplied result percentage inserted.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getExamResults(final int examScore) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "examResults.text", examScore);
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
        results = Math.clamp(results, 0, 100);

        return results;
    }

    // region Boolean Comparison Methods

    /**
     * Check if the current instance belongs to the average type category.
     *
     * @return {@code true} if the instance is of average type, {@code false} otherwise.
     */
    public boolean isAverage() {
        return this == AVERAGE;
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
    @Deprecated(since = "0.51.0", forRemoval = true)
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
