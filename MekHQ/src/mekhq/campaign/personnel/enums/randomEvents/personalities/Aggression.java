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
    // Major Trait
    BLOODTHIRSTY("Aggression.BLOODTHIRSTY.text", "Aggression.BLOODTHIRSTY.description"),
    BOLD("Aggression.BOLD.text", "Aggression.BOLD.description"),
    AGGRESSIVE("Aggression.AGGRESSIVE.text", "Aggression.AGGRESSIVE.description"),
    ASSERTIVE("Aggression.ASSERTIVE.text", "Aggression.ASSERTIVE.description"),
    BELLIGERENT("Aggression.BELLIGERENT.text", "Aggression.BELLIGERENT.description"),
    BRASH("Aggression.BRASH.text", "Aggression.BRASH.description"),
    CONFIDENT("Aggression.CONFIDENT.text", "Aggression.CONFIDENT.description"),
    COURAGEOUS("Aggression.COURAGEOUS.text", "Aggression.COURAGEOUS.description"),
    DARING("Aggression.DARING.text", "Aggression.DARING.description"),
    DECISIVE("Aggression.DECISIVE.text", "Aggression.DECISIVE.description"),
    DETERMINED("Aggression.DETERMINED.text", "Aggression.DETERMINED.description"),
    // Major Trait
    DIPLOMATIC("Aggression.DIPLOMATIC.text", "Aggression.DIPLOMATIC.description"),
    DOMINEERING("Aggression.DOMINEERING.text", "Aggression.DOMINEERING.description"),
    FEARLESS("Aggression.FEARLESS.text", "Aggression.FEARLESS.description"),
    HOSTILE("Aggression.HOSTILE.text", "Aggression.HOSTILE.description"),
    HOT_HEADED("Aggression.HOT_HEADED.text", "Aggression.HOT_HEADED.description"),
    IMPETUOUS("Aggression.IMPETUOUS.text", "Aggression.IMPETUOUS.description"),
    IMPULSIVE("Aggression.IMPULSIVE.text", "Aggression.IMPULSIVE.description"),
    INFLEXIBLE("Aggression.INFLEXIBLE.text", "Aggression.INFLEXIBLE.description"),
    INTREPID("Aggression.INTREPID.text", "Aggression.INTREPID.description"),
    MURDEROUS("Aggression.MURDEROUS.text", "Aggression.MURDEROUS.description"),
    OVERBEARING("Aggression.OVERBEARING.text", "Aggression.OVERBEARING.description"),
    // Major Trait
    PACIFISTIC("Aggression.PACIFISTIC.text", "Aggression.PACIFISTIC.description"),
    RECKLESS("Aggression.RECKLESS.text", "Aggression.RECKLESS.description"),
    RESOLUTE("Aggression.RESOLUTE.text", "Aggression.RESOLUTE.description"),
    // Major Trait
    SADISTIC("Aggression.SADISTIC.text", "Aggression.SADISTIC.description"),
    // Major Trait
    SAVAGE("Aggression.SAVAGE.text", "Aggression.SAVAGE.description"),
    STUBBORN("Aggression.STUBBORN.text", "Aggression.STUBBORN.description"),
    TENACIOUS("Aggression.TENACIOUS.text", "Aggression.TENACIOUS.description"),
    VIGILANT("Aggression.VIGILANT.text", "Aggression.VIGILANT.description");

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

    public boolean isBloodthirsty() {
        return this == BLOODTHIRSTY;
    }

    public boolean isBold() {
        return this == BOLD;
    }

    public boolean isAggressive() {
        return this == AGGRESSIVE;
    }

    public boolean isAssertive() {
        return this == ASSERTIVE;
    }

    public boolean isBelligerent() {
        return this == BELLIGERENT;
    }

    public boolean isBrash() {
        return this == BRASH;
    }

    public boolean isConfident() {
        return this == CONFIDENT;
    }

    public boolean isCourageous() {
        return this == COURAGEOUS;
    }

    public boolean isDaring() {
        return this == DARING;
    }

    public boolean isDecisive() {
        return this == DECISIVE;
    }

    public boolean isDetermined() {
        return this == DETERMINED;
    }

    public boolean isDiplomatic() {
        return this == DIPLOMATIC;
    }

    public boolean isDomineering() {
        return this == DOMINEERING;
    }

    public boolean isFearless() {
        return this == FEARLESS;
    }

    public boolean isHostile() {
        return this == HOSTILE;
    }

    public boolean isHotHeaded() {
        return this == HOT_HEADED;
    }

    public boolean isImpetuous() {
        return this == IMPETUOUS;
    }

    public boolean isImpulsive() {
        return this == IMPULSIVE;
    }

    public boolean isInflexible() {
        return this == INFLEXIBLE;
    }

    public boolean isIntrepid() {
        return this == INTREPID;
    }

    public boolean isMurderous() {
        return this == MURDEROUS;
    }

    public boolean isOverbearing() {
        return this == OVERBEARING;
    }

    public boolean isPacifistic() {
        return this == PACIFISTIC;
    }

    public boolean isReckless() {
        return this == RECKLESS;
    }

    public boolean isResolute() {
        return this == RESOLUTE;
    }

    public boolean isSadistic() {
        return this == SADISTIC;
    }

    public boolean isSavage() {
        return this == SAVAGE;
    }

    public boolean isStubborn() {
        return this == STUBBORN;
    }

    public boolean isTenacious() {
        return this == TENACIOUS;
    }

    public boolean isVigilant() {
        return this == VIGILANT;
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
            case "1", "bloodthirsty" -> BLOODTHIRSTY;
            case "2", "bold" -> BOLD;
            case "3", "aggressive" -> AGGRESSIVE;
            case "4", "assertive" -> ASSERTIVE;
            case "5", "belligerent" -> BELLIGERENT;
            case "6", "brash" -> BRASH;
            case "7", "confident" -> CONFIDENT;
            case "8", "courageous" -> COURAGEOUS;
            case "9", "daring" -> DARING;
            case "10", "decisive" -> DECISIVE;
            case "11", "determined" -> DETERMINED;
            case "12", "diplomatic" -> DIPLOMATIC;
            case "13", "domineering" -> DOMINEERING;
            case "14", "fearless" -> FEARLESS;
            case "15", "hostile" -> HOSTILE;
            case "16", "hot-headed" -> HOT_HEADED;
            case "17", "impetuous" -> IMPETUOUS;
            case "18", "impulsive" -> IMPULSIVE;
            case "19", "inflexible" -> INFLEXIBLE;
            case "20", "intrepid" -> INTREPID;
            case "21", "murderous" -> MURDEROUS;
            case "22", "overbearing" -> OVERBEARING;
            case "23", "pacifistic" -> PACIFISTIC;
            case "24", "reckless" -> RECKLESS;
            case "25", "resolute" -> RESOLUTE;
            case "26", "sadistic" -> SADISTIC;
            case "27", "savage" -> SAVAGE;
            case "28", "stubborn" -> STUBBORN;
            case "29", "tenacious" -> TENACIOUS;
            case "30", "vigilant" -> VIGILANT;
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
            case 1 -> BLOODTHIRSTY;
            case 2 -> BOLD;
            case 3 -> AGGRESSIVE;
            case 4 -> ASSERTIVE;
            case 5 -> BELLIGERENT;
            case 6 -> BRASH;
            case 7 -> CONFIDENT;
            case 8 -> COURAGEOUS;
            case 9 -> DARING;
            case 10 -> DECISIVE;
            case 11 -> DETERMINED;
            case 12 -> DIPLOMATIC;
            case 13 -> DOMINEERING;
            case 14 -> FEARLESS;
            case 15 -> HOSTILE;
            case 16 -> HOT_HEADED;
            case 17 -> IMPETUOUS;
            case 18 -> IMPULSIVE;
            case 19 -> INFLEXIBLE;
            case 20 -> INTREPID;
            case 21 -> MURDEROUS;
            case 22 -> OVERBEARING;
            case 23 -> PACIFISTIC;
            case 24 -> RECKLESS;
            case 25 -> RESOLUTE;
            case 26 -> SADISTIC;
            case 27 -> SAVAGE;
            case 28 -> STUBBORN;
            case 29 -> TENACIOUS;
            case 30 -> VIGILANT;
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
            case BLOODTHIRSTY -> 1;
            case BOLD -> 2;
            case AGGRESSIVE -> 3;
            case ASSERTIVE -> 4;
            case BELLIGERENT -> 5;
            case BRASH -> 6;
            case CONFIDENT -> 7;
            case COURAGEOUS -> 8;
            case DARING -> 9;
            case DECISIVE -> 10;
            case DETERMINED -> 11;
            case DIPLOMATIC -> 12;
            case DOMINEERING -> 13;
            case FEARLESS -> 14;
            case HOSTILE -> 15;
            case HOT_HEADED -> 16;
            case IMPETUOUS -> 17;
            case IMPULSIVE -> 18;
            case INFLEXIBLE -> 19;
            case INTREPID -> 20;
            case MURDEROUS -> 21;
            case OVERBEARING -> 22;
            case PACIFISTIC -> 23;
            case RECKLESS -> 24;
            case RESOLUTE -> 25;
            case SADISTIC -> 26;
            case SAVAGE -> 27;
            case STUBBORN -> 28;
            case TENACIOUS -> 29;
            case VIGILANT -> 30;
        };
    }
    @Override
    public String toString() {
        return name;
    }
}
