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

import static mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType.AMBITION;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.logging.MMLogger;

/**
 * Represents various levels and traits of ambition in a personality.
 *
 * <p>This enumeration defines a wide range of traits that can be associated with a person's
 * personality. Traits are characterized as either "positive" or not and can optionally be "major" traits. The
 * enumeration also handles metadata such as retrieving localized labels and descriptions.</p>
 *
 * <p>Some traits, referred to as "Major Traits," denote stronger personality attributes
 * and are to be handled distinctly. These traits are always listed at the end of the enumeration.</p>
 */
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
    private final String label;
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Ambition(boolean isPositive, boolean isMajor) {
        this.label = this.generateLabel();
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

    // region Getters

    /**
     * @return the {@link PersonalityTraitType} representing ambition
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonalityTraitType getPersonalityTraitType() {
        return AMBITION;
    }

    /**
     * @return the label string for the ambition personality trait type
     *
     * @author Illiani
     * @since 0.50.06
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public String getPersonalityTraitTypeLabel() {
        return getPersonalityTraitType().getLabel();
    }

    public String getLabel() {
        return label;
    }

    /**
     * Retrieves the label associated with the current enumeration value.
     *
     * <p>The label is determined based on the resource bundle for the application,
     * utilizing the enum name combined with a specific key suffix to fetch the relevant localized string.</p>
     *
     * @return the localized label string corresponding to the enumeration value.
     */
    private String generateLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the message displayed when a Ronin warrior expresses interest in joining the campaign.
     *
     * @return the formatted Ronin message as a {@link String}.
     */
    public String getRoninMessage() {
        final String RESOURCE_KEY = name() + ".ronin";

        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the formatted interviewer notes for a specific ambition description index.
     *
     * <p>Constructs a resource key by combining the enum name, "interviewerNote", and the provided index,
     * then fetches the formatted text for that key from the resource bundle.</p>
     *
     * @param ambitionDescriptionIndex the index of the ambition description to retrieve notes for
     *
     * @return the formatted interviewer notes text corresponding to the specified index.
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getInterviewersNotes(int ambitionDescriptionIndex) {
        final String RESOURCE_KEY = name() + ".interviewerNote." + ambitionDescriptionIndex;

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
     * Converts the specified string into its corresponding Ambition enum value. The method attempts to interpret the
     * string as either the name of an enum constant or an ordinal value of the enum. If the conversion fails, the
     * method logs an error and returns the default value {@code NONE}.
     *
     * @param text the string to be converted into an Ambition enum value. It can be the name of the enum constant or
     *             its ordinal value as a string.
     *
     * @return the corresponding Ambition enum constant if the string matches a name or ordinal value, otherwise
     *       {@code NONE}.
     */
    // region File I/O
    public static Ambition fromString(String text) {
        try {
            return Ambition.valueOf(text.toUpperCase().replace(" ", "_"));
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
