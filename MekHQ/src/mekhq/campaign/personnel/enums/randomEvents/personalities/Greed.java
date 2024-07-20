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
    NONE("Personality.NONE.text", "Personality.NONE.description"),
    GENEROUS("Greed.GENEROUS.text", "Greed.GENEROUS.description"),
    FRUGAL("Greed.FRUGAL.text", "Greed.FRUGAL.description"),
    GREEDY("Greed.GREEDY.text", "Greed.GREEDY.description"),
    SELFISH("Greed.SELFISH.text", "Greed.SELFISH.description"),
    INSATIABLE("Greed.INSATIABLE.text", "Greed.INSATIABLE.description"),
    LUSTFUL("Greed.LUSTFUL.text", "Greed.LUSTFUL.description"),
    THIEF("Greed.THIEF.text", "Greed.THIEF.description");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String description;
    //endregion Variable Declarations

    //region Constructors
    Greed(final String name, final String description) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.description = resources.getString(description);
    }
    //endregion Constructors

    //region Getters
    public String getDescription() {
        return description;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isGenerous() {
        return this == GENEROUS;
    }

    public boolean isFrugal() {
        return this == FRUGAL;
    }

    public boolean isGreedy() {
        return this == GREEDY;
    }

    public boolean isSelfish() {
        return this == SELFISH;
    }

    public boolean isInsatiable() {
        return this == INSATIABLE;
    }

    public boolean isLustful() {
        return this == LUSTFUL;
    }

    public boolean isThief() {
        return this == THIEF;
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
    public static Greed parseFromString(final String greed) {
        return switch (greed.toLowerCase()) {
            case "0", "none" -> NONE;
            case "1", "generous" -> GENEROUS;
            case "2", "frugal" -> FRUGAL;
            case "3", "greedy" -> GREEDY;
            case "4", "selfish" -> SELFISH;
            case "5", "insatiable" -> INSATIABLE;
            case "6", "lustful" -> LUSTFUL;
            case "7", "thief" -> THIEF;
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
    public static Greed parseFromInt(final int greed) {
        return switch (greed) {
            case 0 -> NONE;
            case 1 -> GENEROUS;
            case 2 -> FRUGAL;
            case 3 -> GREEDY;
            case 4 -> SELFISH;
            case 5 -> INSATIABLE;
            case 6 -> LUSTFUL;
            case 7 -> THIEF;
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
    public static int parseToInt(final Greed greed) {
        return switch (greed) {
            case NONE -> 0;
            case GENEROUS -> 1;
            case FRUGAL -> 2;
            case GREEDY -> 3;
            case SELFISH -> 4;
            case INSATIABLE -> 5;
            case LUSTFUL -> 6;
            case THIEF -> 7;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
