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

public enum Ambition {
    //region Enum Declarations
    NONE("Personality.NONE.text", "Personality.NONE.toolTipText"),
    UNAMBITIOUS("Ambition.UNAMBITIOUS.text", "Ambition.UNAMBITIOUS.toolTipText"),
    DRIVEN("Ambition.DRIVEN.text", "Ambition.DRIVEN.toolTipText"),
    ASSERTIVE("Ambition.ASSERTIVE.text", "Ambition.ASSERTIVE.toolTipText"),
    ARROGANT("Ambition.ARROGANT.text", "Ambition.ARROGANT.toolTipText"),
    CONTROLLING("Ambition.CONTROLLING.text", "Ambition.CONTROLLING.toolTipText"),
    RUTHLESS("Ambition.RUTHLESS.text", "Ambition.RUTHLESS.toolTipText"),
    DECEITFUL("Ambition.DECEITFUL.text", "Ambition.DECEITFUL.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    Ambition(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isUnambitious() {
        return this == UNAMBITIOUS;
    }

    public boolean isDriven() {
        return this == DRIVEN;
    }

    public boolean isAssertive() {
        return this == ASSERTIVE;
    }

    public boolean isArrogant() {
        return this == ARROGANT;
    }

    public boolean isControlling() {
        return this == CONTROLLING;
    }

    public boolean isRuthless() {
        return this == RUTHLESS;
    }

    public boolean isDeceitful() {
        return this == DECEITFUL;
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    /**
     * Parses a given string and returns the corresponding Ambition enum.
     * Accepts either the ENUM ordinal value, or its name
     *
     * @param ambition the string to be parsed
     * @return the Ambition enum that corresponds to the given string
     * @throws IllegalStateException if the given string does not match any valid Ambition
     */
    public static Ambition parseFromString(final String ambition) {
        return switch (ambition.toLowerCase()) {
            case "0", "none" -> NONE;
            case "1", "peaceful" -> UNAMBITIOUS;
            case "2", "professional" -> DRIVEN;
            case "3", "aggressive" -> ASSERTIVE;
            case "4", "stubborn" -> ARROGANT;
            case "5", "brutal" -> CONTROLLING;
            case "6", "bloodthirsty" -> RUTHLESS;
            case "7", "murderous" -> DECEITFUL;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Ambition.java/parseFromString: "
                            + ambition);
        };
    }

    /**
     * Parses an integer value into an Aggression enum.
     *
     * @param ambition the integer value representing the Ambition level
     * @return the corresponding Ambition enum value
     * @throws IllegalStateException if the integer value does not correspond to any valid Ambition enum value
     */
    public static Ambition parseFromInt(final int ambition) {
        return switch (ambition) {
            case 0 -> NONE;
            case 1 -> UNAMBITIOUS;
            case 2 -> DRIVEN;
            case 3 -> ASSERTIVE;
            case 4 -> ARROGANT;
            case 5 -> CONTROLLING;
            case 6 -> RUTHLESS;
            case 7 -> DECEITFUL;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/randomEvents/personalities/Ambition.java/parseFromInt: "
                            + ambition);
        };
    }

    /**
     * Parses the given Ambition enum value to an integer.
     *
     * @param ambition the Ambition enum value to be parsed
     * @return the integer value representing the parsed Ambition
     */
    public static int parseToInt(final Ambition ambition) {
        return switch (ambition) {
            case NONE -> 0;
            case UNAMBITIOUS -> 1;
            case DRIVEN -> 2;
            case ASSERTIVE -> 3;
            case ARROGANT -> 4;
            case CONTROLLING -> 5;
            case RUTHLESS -> 6;
            case DECEITFUL -> 7;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
