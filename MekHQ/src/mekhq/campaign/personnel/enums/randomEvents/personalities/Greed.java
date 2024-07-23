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

public enum Greed {
    //region Enum Declarations
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
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String description;
    private final boolean isPositive;
    private final boolean isMajor;
    //endregion Variable Declarations

    //region Constructors
    Greed(final String name, final String description, boolean isPositive, boolean isMajor) {
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
    public boolean isAstute() {
        return this == ASTUTE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isAdept() {
        return this == ADEPT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isAvaricious() {
        return this == AVARICIOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCorrupt() {
        return this == CORRUPT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDynamic() {
        return this == DYNAMIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isEager() {
        return this == EAGER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isEnterprising() {
        return this == ENTERPRISING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isExploitative() {
        return this == EXPLOITATIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFraudulent() {
        return this == FRAUDULENT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isGenerous() {
        return this == GENEROUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isGreedy() {
        return this == GREEDY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isHoarding() {
        return this == HOARDING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isInsatiable() {
        return this == INSATIABLE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isInsightful() {
        return this == INSIGHTFUL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isIntuitive() {
        return this == INTUITIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isJudicious() {
        return this == JUDICIOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLustful() {
        return this == LUSTFUL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isMercenary() {
        return this == MERCENARY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isMeticulous() {
        return this == METICULOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNefarious() {
        return this == NEFARIOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isOverreaching() {
        return this == OVERREACHING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isProfitable() {
        return this == PROFITABLE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSavvy() {
        return this == SAVVY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSelfServing() {
        return this == SELF_SERVING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isShameless() {
        return this == SHAMELESS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isShrewd() {
        return this == SHREWD;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTactical() {
        return this == TACTICAL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isThief() {
        return this == THIEF;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUnprincipled() {
        return this == UNPRINCIPLED;
    }

    @SuppressWarnings(value = "unused")
    public boolean isVoracious() {
        return this == VORACIOUS;
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    /**
     * Parses a given string and returns the corresponding Greed enum.
     * Accepts either the ENUM ordinal value, or its name
     *
     * @param greed the string to be parsed
     * @return the Greed enum that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid Greed
     */
    @SuppressWarnings(value = "unused")
    public static Greed parseFromString(final String greed) {
        return switch (greed.toLowerCase()) {
            case "0", "none" -> NONE;
            case "1", "astute" -> ASTUTE;
            case "2", "adept" -> ADEPT;
            case "3", "avaricious" -> AVARICIOUS;
            case "4", "corrupt" -> CORRUPT;
            case "5", "dynamic" -> DYNAMIC;
            case "6", "eager" -> EAGER;
            case "7", "enterprising" -> ENTERPRISING;
            case "8", "exploitative" -> EXPLOITATIVE;
            case "9", "fraudulent" -> FRAUDULENT;
            case "10", "generous" -> GENEROUS;
            case "11", "greedy" -> GREEDY;
            case "12", "hoarding" -> HOARDING;
            case "13", "insatiable" -> INSATIABLE;
            case "14", "insightful" -> INSIGHTFUL;
            case "15", "intuitive" -> INTUITIVE;
            case "16", "judicious" -> JUDICIOUS;
            case "17", "lustful" -> LUSTFUL;
            case "18", "mercenary" -> MERCENARY;
            case "19", "meticulous" -> METICULOUS;
            case "20", "nefarious" -> NEFARIOUS;
            case "21", "overreaching" -> OVERREACHING;
            case "22", "profitable" -> PROFITABLE;
            case "23", "savvy" -> SAVVY;
            case "24", "self-serving" -> SELF_SERVING;
            case "25", "shameless" -> SHAMELESS;
            case "26", "shrewd" -> SHREWD;
            case "27", "tactical" -> TACTICAL;
            case "28", "thief" -> THIEF;
            case "29", "unprincipled" -> UNPRINCIPLED;
            case "30", "voracious" -> VORACIOUS;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Greed.java/parseFromString: "
                            + greed);
        };
    }

    /**
     * Parses an integer value into an Greed enum.
     *
     * @param greed the integer value representing the Greed level
     * @return the corresponding Greed enum value
     * @throws IllegalStateException if the integer value does not correspond to any valid Greed enum value
     */
    @SuppressWarnings(value = "unused")
    public static Greed parseFromInt(final int greed) {
        return switch (greed) {
            case 0 -> NONE;
            case 1 -> ASTUTE;
            case 2 -> ADEPT;
            case 3 -> AVARICIOUS;
            case 4 -> CORRUPT;
            case 5 -> DYNAMIC;
            case 6 -> EAGER;
            case 7 -> ENTERPRISING;
            case 8 -> EXPLOITATIVE;
            case 9 -> FRAUDULENT;
            case 10 -> GENEROUS;
            case 11 -> GREEDY;
            case 12 -> HOARDING;
            case 13 -> INSATIABLE;
            case 14 -> INSIGHTFUL;
            case 15 -> INTUITIVE;
            case 16 -> JUDICIOUS;
            case 17 -> LUSTFUL;
            case 18 -> MERCENARY;
            case 19 -> METICULOUS;
            case 20 -> NEFARIOUS;
            case 21 -> OVERREACHING;
            case 22 -> PROFITABLE;
            case 23 -> SAVVY;
            case 24 -> SELF_SERVING;
            case 25 -> SHAMELESS;
            case 26 -> SHREWD;
            case 27 -> TACTICAL;
            case 28 -> THIEF;
            case 29 -> UNPRINCIPLED;
            case 30 -> VORACIOUS;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Greed.java/parseFromInt: "
                            + greed);
        };
    }

    /**
     * Parses the given Greed enum value to an integer.
     *
     * @param greed the Greed enum value to be parsed
     * @return the integer value representing the parsed Greed
     */
    @SuppressWarnings(value = "unused")
    public static int parseToInt(final Greed greed) {
        return switch (greed) {
            case NONE -> 0;
            case ASTUTE -> 1;
            case ADEPT -> 2;
            case AVARICIOUS -> 3;
            case CORRUPT -> 4;
            case DYNAMIC -> 5;
            case EAGER -> 6;
            case ENTERPRISING -> 7;
            case EXPLOITATIVE -> 8;
            case FRAUDULENT -> 9;
            case GENEROUS -> 10;
            case GREEDY -> 11;
            case HOARDING -> 12;
            case INSATIABLE -> 13;
            case INSIGHTFUL -> 14;
            case INTUITIVE -> 15;
            case JUDICIOUS -> 16;
            case LUSTFUL -> 17;
            case MERCENARY -> 18;
            case METICULOUS -> 19;
            case NEFARIOUS -> 20;
            case OVERREACHING -> 21;
            case PROFITABLE -> 22;
            case SAVVY -> 23;
            case SELF_SERVING -> 24;
            case SHAMELESS -> 25;
            case SHREWD -> 26;
            case TACTICAL -> 27;
            case THIEF -> 28;
            case UNPRINCIPLED -> 29;
            case VORACIOUS -> 30;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
