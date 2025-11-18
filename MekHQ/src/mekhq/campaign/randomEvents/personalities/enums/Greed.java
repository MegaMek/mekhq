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

import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType.GREED;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.PronounData;

/**
 * Represents various levels and traits of greed in a personality.
 *
 * <p>This enumeration defines a wide range of traits that can be associated with a person's
 * personality. Traits are characterized as either "positive" or not and can optionally be "major" traits. The
 * enumeration also handles metadata such as retrieving localized labels and descriptions.</p>
 *
 * <p>Some traits, referred to as "Major Traits," denote stronger personality attributes
 * and are to be handled distinctly. These traits are always listed at the end of the enumeration.</p>
 */
public enum Greed {
    // region Enum Declarations
    NONE(false, false),
    ASTUTE(true, false),
    ADEPT(true, false),
    AVARICIOUS(false, false),
    DYNAMIC(true, false),
    EAGER(true, false),
    EXPLOITATIVE(false, false),
    FRAUDULENT(false, false),
    GENEROUS(true, false),
    GREEDY(false, false),
    HOARDING(false, false),
    INSATIABLE(false, false),
    INSIGHTFUL(true, false),
    JUDICIOUS(true, false),
    LUSTFUL(false, false),
    MERCENARY(false, false),
    OVERREACHING(false, false),
    PROFITABLE(true, false),
    SAVVY(true, false),
    SELF_SERVING(false, false),
    SHAMELESS(false, false),
    SHREWD(true, false),
    TACTICAL(true, false),
    UNPRINCIPLED(false, false),
    VORACIOUS(true, false),
    // Major Traits should always be last
    INTUITIVE(true, true),
    ENTERPRISING(true, true),
    CORRUPT(false, true),
    METICULOUS(true, true),
    NEFARIOUS(false, true),
    THIEF(false, true);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String label;
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Greed(boolean isPositive, boolean isMajor) {
        this.label = generateLabel();
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

    public String getLabel() {
        return label;
    }

    /**
     * @return the {@link PersonalityTraitType} representing greed
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonalityTraitType getPersonalityTraitType() {
        return GREED;
    }

    /**
     * @return the label string for the greed personality trait type
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getPersonalityTraitTypeLabel() {
        return getPersonalityTraitType().getLabel();
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

        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Generates a localized and personalized description for the current enumeration value.
     * <p>
     * This method retrieves a description using the enumeration's name and a specific key suffix derived from the given
     * ambition description index. The description is further customized using the provided gender-specific pronouns,
     * the individual's given name, and other localized text from the resource bundle.
     * </p>
     *
     * @param greedDescriptionIndex an index representing the type/variation of the description. This value is clamped
     *                              to ensure it falls within a valid range.
     * @param gender                the {@link Gender} of the individual, used to determine appropriate pronouns for the
     *                              description.
     * @param givenName             the given name of the person. This <b>MUST</b> use 'person.getGivenName()' and
     *                              <b>NOT</b> 'person.getFirstName()'
     *
     * @return a formatted description string based on the enum, the individual's gender, name, and aggression
     *       description index.
     */
    public String getDescription(int greedDescriptionIndex, final Gender gender,
          final String givenName) {
        greedDescriptionIndex = clamp(greedDescriptionIndex, 0, MAXIMUM_VARIATIONS - 1);

        final String RESOURCE_KEY = name() + ".description." + greedDescriptionIndex;
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

    /**
     * Retrieves the message displayed when a Ronin warrior expresses interest in joining the campaign.
     *
     * <p>This method formats a message using a resource key derived from the current object and
     * includes the commander's address as part of the message formatting.</p>
     *
     * @param commanderAddress the address or name of the commander to include in the message.
     *
     * @return the formatted Ronin message as a {@link String}.
     */
    public String getRoninMessage(String commanderAddress) {
        final String RESOURCE_KEY = name() + ".ronin";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY, commanderAddress);
    }

    /**
     * Retrieves the formatted interviewer notes for a specific greed description index.
     *
     * <p>Constructs a resource key by combining the enum name, "interviewerNote", and the provided index,
     * then fetches the formatted text for that key from the resource bundle.</p>
     *
     * @param greedDescriptionIndex the index of the greed description to retrieve notes for
     *
     * @return the formatted interviewer notes text corresponding to the specified index.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getInterviewersNotes(int greedDescriptionIndex) {
        final String RESOURCE_KEY = name() + ".interviewerNote." + greedDescriptionIndex;

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * @return {@code true} if the personality trait is considered positive, {@code false} otherwise.
     */
    public boolean isTraitPositive() {
        return isPositive;
    }

    /**
     * @return {@code true} if the personality trait is considered a major trait, {@code false} otherwise.
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
     * Parses a string to return the corresponding {@code Greed} enum instance. It attempts to match the string either
     * to a valid enum constant name or an integer representing the ordinal of the desired enum value. If neither
     * interpretation is valid, it defaults to returning {@code NONE}.
     *
     * @param text the input string to parse, representing either the name or the ordinal of the {@code Greed} enum.
     *
     * @return the corresponding {@code Greed} enum instance for the given input string, or {@code NONE} if no valid
     *       match is found.
     */
    // region File I/O
    public static Greed fromString(String text) {
        try {
            return Greed.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}

        try {
            return Greed.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}


        MMLogger logger = MMLogger.create(Greed.class);
        logger.error("Unknown Greed ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
