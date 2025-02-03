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

public enum Intelligence {
    // region Enum Declarations
    BRAIN_DEAD,
    UNINTELLIGENT,
    FOOLISH,
    SIMPLE,
    SLOW,
    UNINSPIRED,
    DULL,
    DIMWITTED,
    OBTUSE,
    BELOW_AVERAGE,
    UNDER_PERFORMING,
    LIMITED_INSIGHT,
    AVERAGE,
    ABOVE_AVERAGE,
    STUDIOUS,
    DISCERNING,
    SHARP,
    QUICK_WITTED,
    PERCEPTIVE,
    BRIGHT,
    CLEVER,
    INTELLECTUAL,
    BRILLIANT,
    EXCEPTIONAL,
    GENIUS;
    // endregion Enum Declarations

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
     * Generates a description for a specified person based on their social description index,
     * pronoun, and other properties specific to the person and resource bundle.
     *
     * @param person the {@code Person} object for whom the description is being generated
     * @return a formatted description string tailored to the specified person
     */
    public String getDescription(Person person) {
        int descriptionIndex = person.getIntelligenceDescriptionIndex();
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

    // region Boolean Comparison Methods
    public boolean isAverage() {
        return this == AVERAGE;
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


        MMLogger logger = MMLogger.create(Greed.class);
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
