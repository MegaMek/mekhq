/*
 * Copyright (C) 2024-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.randomEvents.personalities.enums;

import megamek.common.enums.Gender;
import megamek.logging.MMLogger;

import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HE_SHE_THEY;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIM_HER_THEM;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIS_HER_THEIR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Represents various levels and traits of aggression in a personality.
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
public enum Aggression {
    // region Enum Declarations
    NONE(false, false),
    AGGRESSIVE(false, false),
    ASSERTIVE(true, false),
    BELLIGERENT(false, false),
    BOLD(true, false),
    BRASH(false, false),
    CONFIDENT(true, false),
    COURAGEOUS(true, false),
    DARING(true, false),
    DECISIVE(true, false),
    DETERMINED(true, false),
    DOMINEERING(false, false),
    FEARLESS(true, false),
    HOSTILE(false, false),
    HOT_HEADED(false, false),
    IMPETUOUS(false, false),
    IMPULSIVE(false, false),
    INFLEXIBLE(false, false),
    INTREPID(true, false),
    OVERBEARING(false, false),
    RECKLESS(false, false),
    RESOLUTE(true, false),
    STUBBORN(false, false),
    TENACIOUS(true, false),
    VIGILANT(true, false),
    // Major Traits should always be last
    BLOODTHIRSTY(false, true),
    DIPLOMATIC(true, true),
    MURDEROUS(false, true),
    PACIFISTIC(true, true),
    SAVAGE(false, true),
    SADISTIC(false, true);

    // endregion Enum Declarations

    // region Variable Declarations
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Aggression(boolean isPositive, boolean isMajor) {
        this.isPositive = isPositive;
        this.isMajor = isMajor;
    }
    // endregion Constructors

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    /**
     * Defines the number of individual description variants available for each trait.
     */
    public final static int MAXIMUM_VARIATIONS = 3;

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
     * derived from the given aggression description index. The description is further customized
     * using the provided gender-specific pronouns, the individual's given name, and other localized
     * text from the resource bundle.
     * </p>
     *
     * @param aggressionDescriptionIndex an index representing the type/variation of the description.
     *                                   This value is clamped to ensure it falls within a valid range.
     * @param gender                     the {@link Gender} of the individual, used to determine
     *                                   appropriate pronouns for the description.
     * @param givenName                  the given name of the person. This <b>MUST</b> use
     *                                  'person.getGivenName()' and <b>NOT</b> 'person.getFirstName()'
     * @return                           a formatted description string based on the enum,
     *                                   the individual's gender, name, and aggression description index.
     *
     * @see Gender
     */
    public String getDescription(int aggressionDescriptionIndex, final Gender gender,
                                 final String givenName) {
        aggressionDescriptionIndex = clamp(aggressionDescriptionIndex, 0, MAXIMUM_VARIATIONS - 1);

        final String RESOURCE_KEY = name() + ".description." + aggressionDescriptionIndex;

        String subjectPronoun = HE_SHE_THEY.getDescriptorCapitalized(gender);
        String subjectPronounLowerCase = HE_SHE_THEY.getDescriptor(gender);
        String objectPronoun = HIM_HER_THEM.getDescriptorCapitalized(gender);
        String objectPronounLowerCase = HIM_HER_THEM.getDescriptor(gender);
        String possessivePronoun = HIS_HER_THEIR.getDescriptorCapitalized(gender);
        String possessivePronounLowerCase = HIS_HER_THEIR.getDescriptor(gender);

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY, givenName, subjectPronoun,
            subjectPronounLowerCase, objectPronoun, objectPronounLowerCase, possessivePronoun,
            possessivePronounLowerCase);
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

    // region File I/O
    /**
     * Converts the given string into an instance of the {@code Aggression} enum.
     * The method tries to interpret the string as both a name of an enumeration constant
     * and as an ordinal index. If neither interpretation succeeds, it logs an error
     * and returns {@code NONE}.
     *
     * @param text the string representation of the aggression; can be either
     *             the name of an enumeration constant or the ordinal string.
     * @return the corresponding {@code Aggression} enum instance if the string is a valid
     *         name or ordinal; otherwise, returns {@code NONE}.
     */
    public static Aggression fromString(String text) {
        try {
            return Aggression.valueOf(text);
        } catch (Exception ignored) {}

        try {
            return Aggression.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}


        MMLogger logger = MMLogger.create(Aggression.class);
        logger.error("Unknown Aggression ordinal: {} - returning {}.", text, NONE);

        return NONE;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
