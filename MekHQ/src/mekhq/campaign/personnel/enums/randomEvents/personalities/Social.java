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
    NONE("Personality.NONE.text", "Personality.NONE.description"),
    RECLUSIVE("Social.RECLUSIVE.text", "Social.RECLUSIVE.description"),
    RESILIENT("Social.RESILIENT.text", "Social.RESILIENT.description"),
    TEMPERATE("Social.TEMPERATE.text", "Social.TEMPERATE.description"),
    WISE("Social.WISE.text", "Social.WISE.description"),
    LOVING("Social.LOVING.text", "Social.LOVING.description"),
    IMPARTIAL("Social.IMPARTIAL.text", "Social.IMPARTIAL.description"),
    HONORABLE("Social.HONORABLE.text", "Social.HONORABLE.description");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String description;
    //endregion Variable Declarations

    //region Constructors
    Social(final String name, final String description) {
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

    public boolean isReclusive() {
        return this == RECLUSIVE;
    }

    public boolean isResilient() {
        return this == RESILIENT;
    }

    public boolean isTemperate() {
        return this == TEMPERATE;
    }

    public boolean isWise() {
        return this == WISE;
    }

    public boolean isLoving() {
        return this == LOVING;
    }

    public boolean isImpartial() {
        return this == IMPARTIAL;
    }

    public boolean isHonorable() {
        return this == HONORABLE;
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
    public static Social parseFromString(final String social) {
        return switch (social.toLowerCase()) {
            case "0", "none" -> NONE;
            case "1", "reclusive" -> RECLUSIVE;
            case "2", "resilient" -> RESILIENT;
            case "3", "temperate" -> TEMPERATE;
            case "4", "wise" -> WISE;
            case "5", "loving" -> LOVING;
            case "6", "impartial" -> IMPARTIAL;
            case "7", "honorable" -> HONORABLE;
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
    public static Social parseFromInt(final int social) {
        return switch (social) {
            case 0 -> NONE;
            case 1 -> RECLUSIVE;
            case 2 -> RESILIENT;
            case 3 -> TEMPERATE;
            case 4 -> WISE;
            case 5 -> LOVING;
            case 6 -> IMPARTIAL;
            case 7 -> HONORABLE;
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
    public static int parseToInt(final Social social) {
        return switch (social) {
            case NONE -> 0;
            case RECLUSIVE -> 1;
            case RESILIENT -> 2;
            case TEMPERATE -> 3;
            case WISE -> 4;
            case LOVING -> 5;
            case IMPARTIAL -> 6;
            case HONORABLE -> 7;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
