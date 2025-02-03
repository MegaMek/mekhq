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
import mekhq.campaign.personnel.Person;

import static mekhq.campaign.personnel.enums.GenderDescriptors.HE_SHE_THEY;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIM_HER_THEM;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIS_HER_THEIR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

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
     * Retrieves the description of the current enumeration value.
     *
     * <p>The label is determined based on the resource bundle for the application,
     * utilizing the enum name combined with a specific key suffix to fetch the
     * relevant localized string.</p>
     *
     * @return the description associated with this enumeration value
     */
    public String getDescription(Person person) {
        int descriptionIndex = person.getAggressionDescriptionIndex();
        final String RESOURCE_KEY = name() + ".description." + descriptionIndex + ".regexp";

        Gender gender = person.getGender();
        String subjectPronoun = HE_SHE_THEY.getDescriptorCapitalized(gender);
        String subjectPronounLowerCase = HE_SHE_THEY.getDescriptor(gender);
        String objectPronoun = HIM_HER_THEM.getDescriptorCapitalized(gender);
        String objectPronounLowerCase = HIM_HER_THEM.getDescriptor(gender);
        String possessivePronoun = HIS_HER_THEIR.getDescriptorCapitalized(gender);
        String possessivePronounLowerCase = HIS_HER_THEIR.getDescriptor(gender);

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY, person.getFirstName(),
            subjectPronoun, subjectPronounLowerCase, objectPronoun, objectPronounLowerCase,
            possessivePronoun, possessivePronounLowerCase);
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
