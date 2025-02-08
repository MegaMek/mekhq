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

public enum Intelligence {
    // region Enum Declarations
    BRAIN_DEAD("Intelligence.BRAIN_DEAD.text", "Intelligence.BRAIN_DEAD.description"),
    UNINTELLIGENT("Intelligence.UNINTELLIGENT.text", "Intelligence.UNINTELLIGENT.description"),
    FOOLISH("Intelligence.FOOLISH.text", "Intelligence.FOOLISH.description"),
    SIMPLE("Intelligence.SIMPLE.text", "Intelligence.SIMPLE.description"),
    SLOW("Intelligence.SLOW.text", "Intelligence.SLOW.description"),
    UNINSPIRED("Intelligence.UNINSPIRED.text", "Intelligence.UNINSPIRED.description"),
    DULL("Intelligence.DULL.text", "Intelligence.DULL.description"),
    DIMWITTED("Intelligence.DIMWITTED.text", "Intelligence.DIMWITTED.description"),
    OBTUSE("Intelligence.OBTUSE.text", "Intelligence.OBTUSE.description"),
    BELOW_AVERAGE("Intelligence.BELOW_AVERAGE.text", "Intelligence.BELOW_AVERAGE.description"),
    UNDER_PERFORMING("Intelligence.UNDER_PERFORMING.text", "Intelligence.UNDER_PERFORMING.description"),
    LIMITED_INSIGHT("Intelligence.LIMITED_INSIGHT.text", "Intelligence.LIMITED_INSIGHT.description"),
    AVERAGE("Intelligence.AVERAGE.text", "Intelligence.AVERAGE.description"),
    ABOVE_AVERAGE("Intelligence.ABOVE_AVERAGE.text", "Intelligence.ABOVE_AVERAGE.description"),
    STUDIOUS("Intelligence.STUDIOUS.text", "Intelligence.STUDIOUS.description"),
    DISCERNING("Intelligence.DISCERNING.text", "Intelligence.DISCERNING.description"),
    SHARP("Intelligence.SHARP.text", "Intelligence.SHARP.description"),
    QUICK_WITTED("Intelligence.QUICK_WITTED.text", "Intelligence.QUICK_WITTED.description"),
    PERCEPTIVE("Intelligence.PERCEPTIVE.text", "Intelligence.PERCEPTIVE.description"),
    BRIGHT("Intelligence.BRIGHT.text", "Intelligence.BRIGHT.description"),
    CLEVER("Intelligence.CLEVER.text", "Intelligence.CLEVER.description"),
    INTELLECTUAL("Intelligence.INTELLECTUAL.text", "Intelligence.INTELLECTUAL.description"),
    BRILLIANT("Intelligence.BRILLIANT.text", "Intelligence.BRILLIANT.description"),
    EXCEPTIONAL("Intelligence.EXCEPTIONAL.text", "Intelligence.EXCEPTIONAL.description"),
    GENIUS("Intelligence.GENIUS.text", "Intelligence.GENIUS.description");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String description;
    // endregion Variable Declarations

    // region Constructors
    Intelligence(final String name, final String description) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personalities",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.description = resources.getString(description);
    }
    // endregion Constructors

    // region Getters

    public String getDescription() {
        return description;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isAverage() {
        return this == AVERAGE;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    /**
     * Returns the {@link Intelligence} associated with the given ordinal.
     *
     * @param ordinal the ordinal value of the {@link Intelligence}
     * @return the {@link Intelligence} associated with the given ordinal, or default value
     * {@code AVERAGE} if not found
     */
    public static Intelligence fromOrdinal(int ordinal) {
        if ((ordinal >= 0) && (ordinal < values().length)) {
            return values()[ordinal];
        }

        MMLogger logger = MMLogger.create(Intelligence.class);
        logger.error(String.format("Unknown Intelligence ordinal: %s - returning AVERAGE.", ordinal));

        return AVERAGE;
    }

    @Override
    public String toString() {
        return name;
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
