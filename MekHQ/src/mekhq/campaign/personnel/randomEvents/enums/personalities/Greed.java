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

    public boolean isAstute() {
        return this == ASTUTE;
    }

    public boolean isAdept() {
        return this == ADEPT;
    }

    public boolean isAvaricious() {
        return this == AVARICIOUS;
    }

    public boolean isCorrupt() {
        return this == CORRUPT;
    }

    public boolean isDynamic() {
        return this == DYNAMIC;
    }

    public boolean isEager() {
        return this == EAGER;
    }

    public boolean isEnterprising() {
        return this == ENTERPRISING;
    }

    public boolean isExploitative() {
        return this == EXPLOITATIVE;
    }

    public boolean isFraudulent() {
        return this == FRAUDULENT;
    }

    public boolean isGenerous() {
        return this == GENEROUS;
    }

    public boolean isGreedy() {
        return this == GREEDY;
    }

    public boolean isHoarding() {
        return this == HOARDING;
    }

    public boolean isInsatiable() {
        return this == INSATIABLE;
    }

    public boolean isInsightful() {
        return this == INSIGHTFUL;
    }

    public boolean isIntuitive() {
        return this == INTUITIVE;
    }

    public boolean isJudicious() {
        return this == JUDICIOUS;
    }

    public boolean isLustful() {
        return this == LUSTFUL;
    }

    public boolean isMercenary() {
        return this == MERCENARY;
    }

    public boolean isMeticulous() {
        return this == METICULOUS;
    }

    public boolean isNefarious() {
        return this == NEFARIOUS;
    }

    public boolean isOverreaching() {
        return this == OVERREACHING;
    }

    public boolean isProfitable() {
        return this == PROFITABLE;
    }

    public boolean isSavvy() {
        return this == SAVVY;
    }

    public boolean isSelfServing() {
        return this == SELF_SERVING;
    }

    public boolean isShameless() {
        return this == SHAMELESS;
    }

    public boolean isShrewd() {
        return this == SHREWD;
    }

    public boolean isTactical() {
        return this == TACTICAL;
    }

    public boolean isThief() {
        return this == THIEF;
    }

    public boolean isUnprincipled() {
        return this == UNPRINCIPLED;
    }

    public boolean isVoracious() {
        return this == VORACIOUS;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    /**
     * Parses a given string and returns the corresponding Greed enum.
     * Accepts either the ENUM ordinal value, or its name
     *
     * @param greed the string to be parsed
     * @return the Greed enum that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid
     *                               Greed
     */

    public static Greed parseFromString(final String greed) {
        return switch (greed) {
            case "0", "None" -> NONE;
            // Minor Characteristics
            case "1", "Astute" -> ASTUTE;
            case "2", "Adept" -> ADEPT;
            case "3", "Avaricious" -> AVARICIOUS;
            case "4", "Dynamic" -> DYNAMIC;
            case "5", "Eager" -> EAGER;
            case "6", "Exploitative" -> EXPLOITATIVE;
            case "7", "Fraudulent" -> FRAUDULENT;
            case "8", "Generous" -> GENEROUS;
            case "9", "Greedy" -> GREEDY;
            case "10", "Hoarding" -> HOARDING;
            case "11", "Insatiable" -> INSATIABLE;
            case "12", "Insightful" -> INSIGHTFUL;
            case "13", "Judicious" -> JUDICIOUS;
            case "14", "Lustful" -> LUSTFUL;
            case "15", "Mercenary" -> MERCENARY;
            case "16", "Overreaching" -> OVERREACHING;
            case "17", "Profitable" -> PROFITABLE;
            case "18", "Savvy" -> SAVVY;
            case "19", "Self-Serving" -> SELF_SERVING;
            case "20", "Shameless" -> SHAMELESS;
            case "21", "Shrewd" -> SHREWD;
            case "22", "Tactical" -> TACTICAL;
            case "23", "Unprincipled" -> UNPRINCIPLED;
            case "24", "Voracious" -> VORACIOUS;
            // Major Characteristics
            case "25", "Corrupt" -> CORRUPT;
            case "26", "Enterprising" -> ENTERPRISING;
            case "27", "Intuitive" -> INTUITIVE;
            case "28", "Meticulous" -> METICULOUS;
            case "29", "Nefarious" -> NEFARIOUS;
            case "30", "Thief" -> THIEF;
            default ->
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Greed.java/parseFromString: "
                                + greed);
        };
    }

    /**
     * Parses an integer value into an Greed enum.
     *
     * @param greed the integer value representing the Greed level
     * @return the corresponding Greed enum value
     * @throws IllegalStateException if the integer value does not correspond to any
     *                               valid Greed enum value
     */

    public static Greed parseFromInt(final int greed) {
        return switch (greed) {
            case 0 -> NONE;
            // Minor Characteristics
            case 1 -> ASTUTE;
            case 2 -> ADEPT;
            case 3 -> AVARICIOUS;
            case 4 -> DYNAMIC;
            case 5 -> EAGER;
            case 6 -> EXPLOITATIVE;
            case 7 -> FRAUDULENT;
            case 8 -> GENEROUS;
            case 9 -> GREEDY;
            case 10 -> HOARDING;
            case 11 -> INSATIABLE;
            case 12 -> INSIGHTFUL;
            case 13 -> JUDICIOUS;
            case 14 -> LUSTFUL;
            case 15 -> MERCENARY;
            case 16 -> OVERREACHING;
            case 17 -> PROFITABLE;
            case 18 -> SAVVY;
            case 19 -> SELF_SERVING;
            case 20 -> SHAMELESS;
            case 21 -> SHREWD;
            case 22 -> TACTICAL;
            case 23 -> UNPRINCIPLED;
            case 24 -> VORACIOUS;
            // Major Characteristics
            case 25 -> CORRUPT;
            case 26 -> ENTERPRISING;
            case 27 -> INTUITIVE;
            case 28 -> METICULOUS;
            case 29 -> NEFARIOUS;
            case 30 -> THIEF;
            default ->
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Greed.java/parseFromInt: "
                                + greed);
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
