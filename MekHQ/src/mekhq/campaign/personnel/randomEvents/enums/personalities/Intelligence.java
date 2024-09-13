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

import java.util.ResourceBundle;

import mekhq.MekHQ;

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

    public boolean isBrainDead() {
        return this == BRAIN_DEAD;
    }

    public boolean isUnintelligent() {
        return this == UNINTELLIGENT;
    }

    public boolean isFeebleMinded() {
        return this == FOOLISH;
    }

    public boolean isSimple() {
        return this == SIMPLE;
    }

    public boolean isSlow() {
        return this == SLOW;
    }

    public boolean isUninspired() {
        return this == UNINSPIRED;
    }

    public boolean isDull() {
        return this == DULL;
    }

    public boolean isDimwitted() {
        return this == DIMWITTED;
    }

    public boolean isObtuse() {
        return this == OBTUSE;
    }

    public boolean isBelowAverage() {
        return this == BELOW_AVERAGE;
    }

    public boolean isUnderPerforming() {
        return this == UNDER_PERFORMING;
    }

    public boolean isLimitedInsight() {
        return this == LIMITED_INSIGHT;
    }

    public boolean isAverage() {
        return this == AVERAGE;
    }

    public boolean isAboveAverage() {
        return this == ABOVE_AVERAGE;
    }

    public boolean isSTUDIOUS() {
        return this == STUDIOUS;
    }

    public boolean isDiscerning() {
        return this == DISCERNING;
    }

    public boolean isSharp() {
        return this == SHARP;
    }

    public boolean isQuickWitted() {
        return this == QUICK_WITTED;
    }

    public boolean isPerceptive() {
        return this == PERCEPTIVE;
    }

    public boolean isBright() {
        return this == BRIGHT;
    }

    public boolean isClever() {
        return this == CLEVER;
    }

    public boolean isIntellectual() {
        return this == INTELLECTUAL;
    }

    public boolean isBrilliant() {
        return this == BRILLIANT;
    }

    public boolean isExceptional() {
        return this == EXCEPTIONAL;
    }

    public boolean isGenius() {
        return this == GENIUS;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    /**
     * Parses a given string and returns the corresponding Quirk enum.
     * Accepts either the ENUM ordinal value, or its name
     *
     * @param quirk the string to be parsed
     * @return the Greed enum that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid
     *                               Quirk
     */
    public static Intelligence parseFromString(final String quirk) {
        return switch (quirk) {
            case "0", "Brain Dead" -> BRAIN_DEAD;
            case "1", "Unintelligent" -> UNINTELLIGENT;
            case "2", "Feeble Minded", "Foolish" -> FOOLISH;
            case "3", "Simple" -> SIMPLE;
            case "4", "Slow to Comprehend", "Slow" -> SLOW;
            case "5", "Uninspired" -> UNINSPIRED;
            case "6", "Dull" -> DULL;
            case "7", "Dimwitted" -> DIMWITTED;
            case "8", "Obtuse" -> OBTUSE;
            case "9", "Below Average" -> BELOW_AVERAGE;
            case "10", "Under Performing" -> UNDER_PERFORMING;
            case "11", "Limited Insight" -> LIMITED_INSIGHT;
            case "12", "Average" -> AVERAGE;
            case "13", "Above Average" -> ABOVE_AVERAGE;
            case "14", "Studious" -> STUDIOUS;
            case "15", "Discerning" -> DISCERNING;
            case "16", "Sharp" -> SHARP;
            case "17", "Quick-Witted" -> QUICK_WITTED;
            case "18", "Perceptive" -> PERCEPTIVE;
            case "19", "Bright" -> BRIGHT;
            case "20", "Clever" -> CLEVER;
            case "21", "Intellectual" -> INTELLECTUAL;
            case "22", "Brilliant" -> BRILLIANT;
            case "23", "Exceptional" -> EXCEPTIONAL;
            case "24", "Genius" -> GENIUS;
            default ->
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/PersonalityQuirk.java/parseFromString: "
                                + quirk);
        };
    }

    /**
     * Parses the given Intelligence enum value to an integer.
     *
     * @param intelligence the Intelligence enum value to be parsed
     * @return the integer value representing the parsed Intelligence
     */

    public static int parseToInt(final Intelligence intelligence) {
        return switch (intelligence) {
            case BRAIN_DEAD -> 0;
            case UNINTELLIGENT -> 1;
            case FOOLISH -> 2;
            case SIMPLE -> 3;
            case SLOW -> 4;
            case UNINSPIRED -> 5;
            case DULL -> 6;
            case DIMWITTED -> 7;
            case OBTUSE -> 8;
            case BELOW_AVERAGE -> 9;
            case UNDER_PERFORMING -> 10;
            case LIMITED_INSIGHT -> 11;
            case AVERAGE -> 12;
            case ABOVE_AVERAGE -> 13;
            case STUDIOUS -> 14;
            case DISCERNING -> 15;
            case SHARP -> 16;
            case QUICK_WITTED -> 17;
            case PERCEPTIVE -> 18;
            case BRIGHT -> 19;
            case CLEVER -> 20;
            case INTELLECTUAL -> 21;
            case BRILLIANT -> 22;
            case EXCEPTIONAL -> 23;
            case GENIUS -> 24;
        };
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
