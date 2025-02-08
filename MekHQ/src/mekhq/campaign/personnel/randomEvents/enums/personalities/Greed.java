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

public enum Greed {
    // region Enum Declarations
    NONE("Personality.NONE.text", "Personality.NONE.description", false, false),
    ASTUTE("Greed.ASTUTE.text", "Greed.ASTUTE.description", true, false),
    ADEPT("Greed.ADEPT.text", "Greed.ADEPT.description", true, false),
    AVARICIOUS("Greed.AVARICIOUS.text", "Greed.AVARICIOUS.description", false, false),
    CORRUPT("Greed.CORRUPT.text", "Greed.CORRUPT.description", false, true),
    DYNAMIC("Greed.DYNAMIC.text", "Greed.DYNAMIC.description", true, false),
    EAGER("Greed.EAGER.text", "Greed.EAGER.description", true, false),
    ENTERPRISING("Greed.ENTERPRISING.text", "Greed.ENTERPRISING.description", true, true),
    EXPLOITATIVE("Greed.EXPLOITATIVE.text", "Greed.EXPLOITATIVE.description", false, false),
    FRAUDULENT("Greed.FRAUDULENT.text", "Greed.FRAUDULENT.description", false, false),
    GENEROUS("Greed.GENEROUS.text", "Greed.GENEROUS.description", true, false),
    GREEDY("Greed.GREEDY.text", "Greed.GREEDY.description", false, false),
    HOARDING("Greed.HOARDING.text", "Greed.HOARDING.description", false, false),
    INSATIABLE("Greed.INSATIABLE.text", "Greed.INSATIABLE.description", false, false),
    INSIGHTFUL("Greed.INSIGHTFUL.text", "Greed.INSIGHTFUL.description", true, false),
    INTUITIVE("Greed.INTUITIVE.text", "Greed.INTUITIVE.description", true, true),
    JUDICIOUS("Greed.JUDICIOUS.text", "Greed.JUDICIOUS.description", true, false),
    LUSTFUL("Greed.LUSTFUL.text", "Greed.LUSTFUL.description", false, false),
    MERCENARY("Greed.MERCENARY.text", "Greed.MERCENARY.description", false, false),
    METICULOUS("Greed.METICULOUS.text", "Greed.METICULOUS.description", true, true),
    NEFARIOUS("Greed.NEFARIOUS.text", "Greed.NEFARIOUS.description", false, true),
    OVERREACHING("Greed.OVERREACHING.text", "Greed.OVERREACHING.description", false, false),
    PROFITABLE("Greed.PROFITABLE.text", "Greed.PROFITABLE.description", true, false),
    SAVVY("Greed.SAVVY.text", "Greed.SAVVY.description", true, false),
    SELF_SERVING("Greed.SELF_SERVING.text", "Greed.SELF_SERVING.description", false, false),
    SHAMELESS("Greed.SHAMELESS.text", "Greed.SHAMELESS.description", false, false),
    SHREWD("Greed.SHREWD.text", "Greed.SHREWD.description", true, false),
    TACTICAL("Greed.TACTICAL.text", "Greed.TACTICAL.description", true, false),
    THIEF("Greed.THIEF.text", "Greed.THIEF.description", false, true),
    UNPRINCIPLED("Greed.UNPRINCIPLED.text", "Greed.UNPRINCIPLED.description", false, false),
    VORACIOUS("Greed.VORACIOUS.text", "Greed.VORACIOUS.description", true, false);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String description;
    private final boolean isPositive;
    private final boolean isMajor;
    // endregion Variable Declarations

    // region Constructors
    Greed(final String name, final String description, boolean isPositive, boolean isMajor) {
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
     * Returns the {@link Greed} associated with the given ordinal.
     *
     * @param ordinal the ordinal value of the {@link Greed}
     * @return the {@link Greed} associated with the given ordinal, or default value
     * {@code NONE} if not found
     */
    public static Greed fromOrdinal(int ordinal) {
        if ((ordinal >= 0) && (ordinal < values().length)) {
            return values()[ordinal];
        }

        MMLogger logger = MMLogger.create(Greed.class);
        logger.error(String.format("Unknown Greed ordinal: %s - returning NONE.", ordinal));

        return NONE;
    }

    @Override
    public String toString() {
        return name;
    }
}
