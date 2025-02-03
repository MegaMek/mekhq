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
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Greed(boolean isPositive, boolean isMajor) {
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
     * Generates a description for a specified person based on their social description index,
     * pronoun, and other properties specific to the person and resource bundle.
     *
     * @param person the {@code Person} object for whom the description is being generated
     * @return a formatted description string tailored to the specified person
     */
    public String getDescription(Person person) {
        int descriptionIndex = person.getGreedDescriptionIndex();
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

    /**
     * Parses a string to return the corresponding {@code Greed} enum instance.
     * It attempts to match the string either to a valid enum constant name or
     * an integer representing the ordinal of the desired enum value. If neither
     * interpretation is valid, it defaults to returning {@code NONE}.
     *
     * @param text the input string to parse, representing either the name or
     *             the ordinal of the {@code Greed} enum.
     * @return the corresponding {@code Greed} enum instance for the given input
     *         string, or {@code NONE} if no valid match is found.
     */
    // region File I/O
    public static Greed fromString(String text) {
        try {
            return Greed.valueOf(text);
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
