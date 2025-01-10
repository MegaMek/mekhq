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

public enum Aggression {
    // region Enum Declarations
    NONE("Personality.NONE.text", "Personality.NONE.description", false, false),
    AGGRESSIVE("Aggression.AGGRESSIVE.text", "Aggression.AGGRESSIVE.description", false, false),
    ASSERTIVE("Aggression.ASSERTIVE.text", "Aggression.ASSERTIVE.description", true, false),
    BELLIGERENT("Aggression.BELLIGERENT.text", "Aggression.BELLIGERENT.description", false, false),
    BLOODTHIRSTY("Aggression.BLOODTHIRSTY.text", "Aggression.BLOODTHIRSTY.description", false, true),
    BOLD("Aggression.BOLD.text", "Aggression.BOLD.description", true, false),
    BRASH("Aggression.BRASH.text", "Aggression.BRASH.description", false, false),
    CONFIDENT("Aggression.CONFIDENT.text", "Aggression.CONFIDENT.description", true, false),
    COURAGEOUS("Aggression.COURAGEOUS.text", "Aggression.COURAGEOUS.description", true, false),
    DARING("Aggression.DARING.text", "Aggression.DARING.description", true, false),
    DECISIVE("Aggression.DECISIVE.text", "Aggression.DECISIVE.description", true, false),
    DETERMINED("Aggression.DETERMINED.text", "Aggression.DETERMINED.description", true, false),
    DIPLOMATIC("Aggression.DIPLOMATIC.text", "Aggression.DIPLOMATIC.description", true, true),
    DOMINEERING("Aggression.DOMINEERING.text", "Aggression.DOMINEERING.description", false, false),
    FEARLESS("Aggression.FEARLESS.text", "Aggression.FEARLESS.description", true, false),
    HOSTILE("Aggression.HOSTILE.text", "Aggression.HOSTILE.description", false, false),
    HOT_HEADED("Aggression.HOT_HEADED.text", "Aggression.HOT_HEADED.description", false, false),
    IMPETUOUS("Aggression.IMPETUOUS.text", "Aggression.IMPETUOUS.description", false, false),
    IMPULSIVE("Aggression.IMPULSIVE.text", "Aggression.IMPULSIVE.description", false, false),
    INFLEXIBLE("Aggression.INFLEXIBLE.text", "Aggression.INFLEXIBLE.description", false, false),
    INTREPID("Aggression.INTREPID.text", "Aggression.INTREPID.description", true, false),
    MURDEROUS("Aggression.MURDEROUS.text", "Aggression.MURDEROUS.description", false, true),
    OVERBEARING("Aggression.OVERBEARING.text", "Aggression.OVERBEARING.description", false, false),
    PACIFISTIC("Aggression.PACIFISTIC.text", "Aggression.PACIFISTIC.description", true, true),
    RECKLESS("Aggression.RECKLESS.text", "Aggression.RECKLESS.description", false, false),
    RESOLUTE("Aggression.RESOLUTE.text", "Aggression.RESOLUTE.description", true, false),
    SADISTIC("Aggression.SADISTIC.text", "Aggression.SADISTIC.description", false, true),
    SAVAGE("Aggression.SAVAGE.text", "Aggression.SAVAGE.description", false, true),
    STUBBORN("Aggression.STUBBORN.text", "Aggression.STUBBORN.description", false, false),
    TENACIOUS("Aggression.TENACIOUS.text", "Aggression.TENACIOUS.description", true, false),
    VIGILANT("Aggression.VIGILANT.text", "Aggression.VIGILANT.description", true, false);

    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String description;
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Aggression(final String name, final String description, boolean isPositive, boolean isMajor) {
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
     * Returns the {@link Aggression} associated with the given ordinal.
     *
     * @param ordinal the ordinal value of the {@link Aggression}
     * @return the {@link Aggression} associated with the given ordinal, or default value
     * {@code NONE} if not found
     */
    public static Aggression fromOrdinal(int ordinal) {
        if ((ordinal >= 0) && (ordinal < values().length)) {
            return values()[ordinal];
        }

        MMLogger logger = MMLogger.create(Aggression.class);
        logger.error(String.format("Unknown Aggression ordinal: %s - returning NONE.", ordinal));

        return NONE;
    }

    @Override
    public String toString() {
        return name;
    }
}
