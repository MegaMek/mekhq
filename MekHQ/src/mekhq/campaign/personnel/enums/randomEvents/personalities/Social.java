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
package mekhq.campaign.personnel.enums.randomEvents.personalities;

import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum Social {
    //region Enum Declarations
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
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String description;
    private final boolean isPositive;
    private final boolean isMajor;
    //endregion Variable Declarations

    //region Constructors
    Social(final String name, final String description, boolean isPositive, boolean isMajor) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.description = resources.getString(description);
        this.isPositive = isPositive;
        this.isMajor = isMajor;
    }
    //endregion Constructors

    //region Getters
    @SuppressWarnings(value = "unused")
    public String getDescription() {
        return description;
    }

    /**
     * @return {@code true} if the personality trait is considered positive, {@code false} otherwise.
     */
    @SuppressWarnings(value = "unused")
    public boolean isTraitPositive() {
        return isPositive;
    }

    /**
     * @return {@code true} if the personality trait is considered a major trait, {@code false} otherwise.
     */
    @SuppressWarnings(value = "unused")
    public boolean isTraitMajor() {
        return isMajor;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @SuppressWarnings(value = "unused")
    public boolean isNone() {
        return this == NONE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isAltruistic() {
        return this == ALTRUISTIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isApathetic() {
        return this == APATHETIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isAuthentic() {
        return this == AUTHENTIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isBlunt() {
        return this == BLUNT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCallous() {
        return this == CALLOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCompassionate() {
        return this == COMPASSIONATE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCondescending() {
        return this == CONDESCENDING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isConsiderate() {
        return this == CONSIDERATE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDisingenuous() {
        return this == DISINGENUOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDismissive() {
        return this == DISMISSIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isEncouraging() {
        return this == ENCOURAGING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isErratic() {
        return this == ERRATIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isEmpathetic() {
        return this == EMPATHETIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFriendly() {
        return this == FRIENDLY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isGregarious() {
        return this == GREGARIOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isInspiring() {
        return this == INSPIRING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isIndifferent() {
        return this == INDIFFERENT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isIntroverted() {
        return this == INTROVERTED;
    }

    @SuppressWarnings(value = "unused")
    public boolean isIrritable() {
        return this == IRRITABLE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNarcissistic() {
        return this == NARCISSISTIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNeglectful() {
        return this == NEGLECTFUL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPompous() {
        return this == POMPOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPetty() {
        return this == PETTY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPersuasive() {
        return this == PERSUASIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isReceptive() {
        return this == RECEPTIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isScheming() {
        return this == SCHEMING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSincere() {
        return this == SINCERE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSupportive() {
        return this == SUPPORTIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTactful() {
        return this == TACTFUL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUntrustworthy() {
        return this == UNTRUSTWORTHY;
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    /**
     * Parses a given string and returns the corresponding Social enum.
     * Accepts either the ENUM ordinal value, or its name
     *
     * @param social the string to be parsed
     * @return the Social enum that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid Social
     */
    @SuppressWarnings(value = "unused")
    public static Social parseFromString(final String social) {
        return switch (social.toLowerCase()) {
            case "0", "none" -> NONE;
            case "1", "astute" -> ALTRUISTIC;
            case "2", "adept" -> APATHETIC;
            case "3", "avaricious" -> AUTHENTIC;
            case "4", "corrupt" -> BLUNT;
            case "5", "dynamic" -> CALLOUS;
            case "6", "eager" -> COMPASSIONATE;
            case "7", "enterprising" -> CONDESCENDING;
            case "8", "exploitative" -> CONSIDERATE;
            case "9", "fraudulent" -> DISINGENUOUS;
            case "10", "generous" -> DISMISSIVE;
            case "11", "greedy" -> ENCOURAGING;
            case "12", "hoarding" -> ERRATIC;
            case "13", "insatiable" -> EMPATHETIC;
            case "14", "insightful" -> FRIENDLY;
            case "15", "intuitive" -> GREGARIOUS;
            case "16", "judicious" -> INSPIRING;
            case "17", "lustful" -> INDIFFERENT;
            case "18", "mercenary" -> INTROVERTED;
            case "19", "meticulous" -> IRRITABLE;
            case "20", "nefarious" -> NARCISSISTIC;
            case "21", "overreaching" -> NEGLECTFUL;
            case "22", "profitable" -> POMPOUS;
            case "23", "savvy" -> PETTY;
            case "24", "self-serving" -> PERSUASIVE;
            case "25", "shameless" -> RECEPTIVE;
            case "26", "shrewd" -> SCHEMING;
            case "27", "tactical" -> SINCERE;
            case "28", "thief" -> SUPPORTIVE;
            case "29", "unprincipled" -> TACTFUL;
            case "30", "voracious" -> UNTRUSTWORTHY;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Social.java/parseFromString: "
                            + social);
        };
    }

    /**
     * Parses an integer value into an Social enum.
     *
     * @param greed the integer value representing the Social level
     * @return the corresponding Social enum value
     * @throws IllegalStateException if the integer value does not correspond to any valid Social enum value
     */
    @SuppressWarnings(value = "unused")
    public static Social parseFromInt(final int social) {
        return switch (social) {
            case 0 -> NONE;
            case 1 -> ALTRUISTIC;
            case 2 -> APATHETIC;
            case 3 -> AUTHENTIC;
            case 4 -> BLUNT;
            case 5 -> CALLOUS;
            case 6 -> COMPASSIONATE;
            case 7 -> CONDESCENDING;
            case 8 -> CONSIDERATE;
            case 9 -> DISINGENUOUS;
            case 10 -> DISMISSIVE;
            case 11 -> ENCOURAGING;
            case 12 -> ERRATIC;
            case 13 -> EMPATHETIC;
            case 14 -> FRIENDLY;
            case 15 -> GREGARIOUS;
            case 16 -> INSPIRING;
            case 17 -> INDIFFERENT;
            case 18 -> INTROVERTED;
            case 19 -> IRRITABLE;
            case 20 -> NARCISSISTIC;
            case 21 -> NEGLECTFUL;
            case 22 -> POMPOUS;
            case 23 -> PETTY;
            case 24 -> PERSUASIVE;
            case 25 -> RECEPTIVE;
            case 26 -> SCHEMING;
            case 27 -> SINCERE;
            case 28 -> SUPPORTIVE;
            case 29 -> TACTFUL;
            case 30 -> UNTRUSTWORTHY;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Social.java/parseFromInt: "
                            + social);
        };
    }

    /**
     * Parses the given Social enum value to an integer.
     *
     * @param social the Social enum value to be parsed
     * @return the integer value representing the parsed Social
     */
    @SuppressWarnings(value = "unused")
    public static int parseToInt(final Social social) {
        return switch (social) {
            case NONE -> 0;
            case ALTRUISTIC -> 1;
            case APATHETIC -> 2;
            case AUTHENTIC -> 3;
            case BLUNT -> 4;
            case CALLOUS -> 5;
            case COMPASSIONATE -> 6;
            case CONDESCENDING -> 7;
            case CONSIDERATE -> 8;
            case DISINGENUOUS -> 9;
            case DISMISSIVE -> 10;
            case ENCOURAGING -> 11;
            case ERRATIC -> 12;
            case EMPATHETIC -> 13;
            case FRIENDLY -> 14;
            case GREGARIOUS -> 15;
            case INSPIRING -> 16;
            case INDIFFERENT -> 17;
            case INTROVERTED -> 18;
            case IRRITABLE -> 19;
            case NARCISSISTIC -> 20;
            case NEGLECTFUL -> 21;
            case POMPOUS -> 22;
            case PETTY -> 23;
            case PERSUASIVE -> 24;
            case RECEPTIVE -> 25;
            case SCHEMING -> 26;
            case SINCERE -> 27;
            case SUPPORTIVE -> 28;
            case TACTFUL -> 29;
            case UNTRUSTWORTHY -> 30;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
