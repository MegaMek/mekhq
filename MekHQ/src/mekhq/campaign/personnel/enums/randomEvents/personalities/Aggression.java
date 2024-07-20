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

public enum Aggression {
    //region Enum Declarations
    NONE("Personality.NONE.text", "Personality.NONE.description"),
    PEACEFUL("Aggression.PEACEFUL.text", "Aggression.PEACEFUL.description"),
    PROFESSIONAL("Aggression.PROFESSIONAL.text", "Aggression.PROFESSIONAL.description"),
    STUBBORN("Aggression.STUBBORN.text", "Aggression.STUBBORN.description"),
    AGGRESSIVE("Aggression.AGGRESSIVE.text", "Aggression.AGGRESSIVE.description"),
    BRUTAL("Aggression.BRUTAL.text", "Aggression.BRUTAL.description"),
    BLOODTHIRSTY("Aggression.BLOODTHIRSTY.text", "Aggression.BLOODTHIRSTY.description"),
    MURDEROUS("Aggression.MURDEROUS.text", "Aggression.MURDEROUS.description");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String description;
    //endregion Variable Declarations

    //region Constructors
    Aggression(final String name, final String description) {
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

    public boolean isPeaceful() {
        return this == PEACEFUL;
    }

    public boolean isProfessional() {
        return this == PROFESSIONAL;
    }

    public boolean isAggressive() {
        return this == AGGRESSIVE;
    }

    public boolean isStubborn() {
        return this == STUBBORN;
    }

    public boolean isBrutal() {
        return this == BRUTAL;
    }

    public boolean isBloodthirsty() {
        return this == BLOODTHIRSTY;
    }

    public boolean isMurderous() {
        return this == MURDEROUS;
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    /**
     * Parses a given string and returns the corresponding Aggression enum.
     * Accepts either the ENUM ordinal value, or its name
     *
     * @param aggression the string to be parsed
     * @return the Aggression enum that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid Aggression
     */
    public static Aggression parseFromString(final String aggression) {
        return switch (aggression.toLowerCase()) {
            case "0", "none" -> NONE;
            case "1", "peaceful" -> PEACEFUL;
            case "2", "professional" -> PROFESSIONAL;
            case "3", "stubborn" -> STUBBORN;
            case "4", "aggressive" -> AGGRESSIVE;
            case "5", "brutal" -> BRUTAL;
            case "6", "bloodthirsty" -> BLOODTHIRSTY;
            case "7", "murderous" -> MURDEROUS;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Aggression.java/parseFromString: "
                            + aggression);
        };
    }

    /**
     * Parses an integer value into an Aggression enum.
     *
     * @param aggression the integer value representing the Aggression level
     * @return the corresponding Aggression enum value
     * @throws IllegalStateException if the integer value does not correspond to any valid Aggression enum value
     */
    public static Aggression parseFromInt(final int aggression) {
        return switch (aggression) {
            case 0 -> NONE;
            case 1 -> PEACEFUL;
            case 2 -> PROFESSIONAL;
            case 3 -> STUBBORN;
            case 4 -> AGGRESSIVE;
            case 5 -> BRUTAL;
            case 6 -> BLOODTHIRSTY;
            case 7 -> MURDEROUS;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Aggression.java/parseFromInt: "
                            + aggression);
        };
    }

    /**
     * Parses the given Aggression enum value to an integer.
     *
     * @param aggression the Aggression enum value to be parsed
     * @return the integer value representing the parsed Aggression
     */
    public static int parseToInt(final Aggression aggression) {
        return switch (aggression) {
            case NONE -> 0;
            case PEACEFUL -> 1;
            case PROFESSIONAL -> 2;
            case STUBBORN -> 3;
            case AGGRESSIVE -> 4;
            case BRUTAL -> 5;
            case BLOODTHIRSTY -> 6;
            case MURDEROUS -> 7;
        };
    }
    @Override
    public String toString() {
        return name;
    }
}
