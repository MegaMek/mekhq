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

import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.campaign.randomEvents.personalities.PersonalityController.PronounData;

import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Represents various levels and traits of social skills in a personality.
 *
 * <p>This enumeration defines a wide range of traits that can be associated with a person's
 * personality. Traits are characterized as either "positive" or not and can optionally be "major"
 * traits. The enumeration also handles metadata such as retrieving localized labels and
 * descriptions.</p>
 *
 * <p>Some traits, referred to as "Major Traits," denote stronger personality attributes
 * and are to be handled distinctly. These traits are always listed at the end of the
 * enumeration.</p>
 */
public enum Social {
    // region Enum Declarations
    NONE(false, false),
    APATHETIC(false, false),
    AUTHENTIC(true, false),
    BLUNT(false, false),
    CALLOUS(false, false),
    CONDESCENDING(false, false),
    CONSIDERATE(true, false),
    DISINGENUOUS(false, false),
    DISMISSIVE(false, false),
    ENCOURAGING(true, false),
    ERRATIC(false, false),
    EMPATHETIC(true, false),
    FRIENDLY(true, false),
    INSPIRING(true, false),
    INDIFFERENT(false, false),
    INTROVERTED(true, false),
    IRRITABLE(false, false),
    NEGLECTFUL(false, false),
    PETTY(false, false),
    PERSUASIVE(true, false),
    RECEPTIVE(true, false),
    SINCERE(true, false),
    SUPPORTIVE(true, false),
    TACTFUL(true, false),
    UNTRUSTWORTHY(false, false),
    // Major Traits should always be last
    SCHEMING(false, true),
    ALTRUISTIC(true, true),
    COMPASSIONATE(true, true),
    GREGARIOUS(true, true),
    NARCISSISTIC(false, true),
    POMPOUS(false, true);
    // endregion Enum Declarations

    // region Variable Declarations
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Social(boolean isPositive, boolean isMajor) {
        this.isPositive = isPositive;
        this.isMajor = isMajor;
    }
    // endregion Constructors

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    /**
     * Defines the number of individual description variants available for each trait.
     */
    public final static int MAXIMUM_VARIATIONS = 6;

    /**
     * The index at which major traits begin within the enumeration.
     */
    public final static int MAJOR_TRAITS_START_INDEX = 25;

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
     * @param socialDescriptionIndex     an index representing the type/variation of the description.
     *                                   This value is clamped to ensure it falls within a valid range.
     * @param gender                     the {@link Gender} of the individual, used to determine
     *                                   appropriate pronouns for the description.
     * @param givenName                  the given name of the person. This <b>MUST</b> use
     *                                  'person.getGivenName()' and <b>NOT</b> 'person.getFirstName()'
     * @return                           a formatted description string based on the enum,
     *                                   the individual's gender, name, and aggression description index.
     */
    public String getDescription(int socialDescriptionIndex, final Gender gender,
                                 final String givenName) {
        socialDescriptionIndex = clamp(socialDescriptionIndex, 0, MAXIMUM_VARIATIONS - 1);

        final String RESOURCE_KEY = name() + ".description." + socialDescriptionIndex;
        final PronounData pronounDate = new PronounData(gender);

        // {0} = givenName
        // {1} = He/She/They
        // {2} = he/she/they
        // {3} = Him/Her/Them
        // {4} = him/her/them
        // {5} = His/Her/Their
        // {6} = his/her/their
        // {7} = Gender Neutral = 0, Otherwise 1 (used to determine whether to use plural case)

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY, givenName, pronounDate.subjectPronoun(),
            pronounDate.subjectPronounLowerCase(), pronounDate.objectPronoun(), pronounDate.objectPronounLowerCase(),
            pronounDate.possessivePronoun(), pronounDate.possessivePronounLowerCase(), pronounDate.pluralizer());
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
     * Converts the given string into an instance of the {@code Social} enum.
     * The method tries to interpret the string as both a name of an enumeration constant
     * and as an ordinal index. If neither interpretation succeeds, it logs an error
     * and returns {@code NONE}.
     *
     * @param text the string representation of the social; can be either
     *             the name of an enumeration constant or the ordinal string.
     * @return the corresponding {@code Social} enum instance if the string is a valid
     *         name or ordinal; otherwise, returns {@code NONE}.
     */
    // region File I/O
    public static Social fromString(String text) {
        try {
            return Social.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}

        try {
            return Social.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}


        MMLogger logger = MMLogger.create(Social.class);
        logger.error("Unknown Social ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
