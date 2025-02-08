/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.randomEvents.enums.personalities;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum Social {
    // region Enum Declarations
    NONE("Personality.NONE.text", "Personality.NONE.description", false, false),
    ALTRUISTIC("Social.ALTRUISTIC.text", "Social.ALTRUISTIC.description", true, true),
    APATHETIC("Social.APATHETIC.text", "Social.APATHETIC.description", false, false),
    AUTHENTIC("Social.AUTHENTIC.text", "Social.AUTHENTIC.description", true, false),
    BLUNT("Social.BLUNT.text", "Social.BLUNT.description", false, false),
    CALLOUS("Social.CALLOUS.text", "Social.CALLOUS.description", false, false),
    COMPASSIONATE("Social.COMPASSIONATE.text", "Social.COMPASSIONATE.description", true, true),
    CONDESCENDING("Social.CONDESCENDING.text", "Social.CONDESCENDING.description", false, false),
    CONSIDERATE("Social.CONSIDERATE.text", "Social.CONSIDERATE.description", true, false),
    DISINGENUOUS("Social.DISINGENUOUS.text", "Social.DISINGENUOUS.description", false, false),
    DISMISSIVE("Social.DISMISSIVE.text", "Social.DISMISSIVE.description", false, false),
    ENCOURAGING("Social.ENCOURAGING.text", "Social.ENCOURAGING.description", true, false),
    ERRATIC("Social.ERRATIC.text", "Social.ERRATIC.description", false, false),
    EMPATHETIC("Social.EMPATHETIC.text", "Social.EMPATHETIC.description", true, false),
    FRIENDLY("Social.FRIENDLY.text", "Social.FRIENDLY.description", true, false),
    GREGARIOUS("Social.GREGARIOUS.text", "Social.GREGARIOUS.description", true, true),
    INSPIRING("Social.INSPIRING.text", "Social.INSPIRING.description", true, false),
    INDIFFERENT("Social.INDIFFERENT.text", "Social.INDIFFERENT.description", false, false),
    INTROVERTED("Social.INTROVERTED.text", "Social.INTROVERTED.description", true, false),
    IRRITABLE("Social.IRRITABLE.text", "Social.IRRITABLE.description", false, false),
    NARCISSISTIC("Social.NARCISSISTIC.text", "Social.NARCISSISTIC.description", false, true),
    NEGLECTFUL("Social.NEGLECTFUL.text", "Social.NEGLECTFUL.description", false, false),
    POMPOUS("Social.POMPOUS.text", "Social.POMPOUS.description", false, true),
    PETTY("Social.PETTY.text", "Social.PETTY.description", false, false),
    PERSUASIVE("Social.PERSUASIVE.text", "Social.PERSUASIVE.description", true, false),
    RECEPTIVE("Social.RECEPTIVE.text", "Social.RECEPTIVE.description", true, false),
    SCHEMING("Social.SCHEMING.text", "Social.SCHEMING.description", false, true),
    SINCERE("Social.SINCERE.text", "Social.SINCERE.description", true, false),
    SUPPORTIVE("Social.SUPPORTIVE.text", "Social.SUPPORTIVE.description", true, false),
    TACTFUL("Social.TACTFUL.text", "Social.TACTFUL.description", true, false),
    UNTRUSTWORTHY("Social.UNTRUSTWORTHY.text", "Social.UNTRUSTWORTHY.description", false, false);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String description;
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Social(final String name, final String description, boolean isPositive, boolean isMajor) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personalities",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.description = resources.getString(description);
        this.isPositive = isPositive;
        this.isMajor = isMajor;
    }
    // endregion Constructors

    // region Getters

    public String getDescription() {
        return description;
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
     * Returns the {@link Social} associated with the given ordinal.
     *
     * @param ordinal the ordinal value of the {@link Social}
     * @return the {@link Social} associated with the given ordinal, or default value
     * {@code NONE} if not found
     */
    public static Social fromOrdinal(int ordinal) {
        if ((ordinal >= 0) && (ordinal < values().length)) {
            return values()[ordinal];
        }

        MMLogger logger = MMLogger.create(Social.class);
        logger.error(String.format("Unknown Social ordinal: %s - returning NONE.", ordinal));

        return NONE;
    }

    @Override
    public String toString() {
        return name;
    }
}
