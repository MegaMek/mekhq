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
    NONE("Personality.NONE.text", "Personality.NONE.description"),
    AMBITIOUS("Ambition.AMBITIOUS.text", "Ambition.AMBITIOUS.description"),
    ARROGANT("Ambition.ARROGANT.text", "Ambition.ARROGANT.description"),
    ASPIRING("Ambition.ASPIRING.text", "Ambition.ASPIRING.description"),
    CALCULATING("Ambition.CALCULATING.text", "Ambition.CALCULATING.description"),
    CONNIVING("Ambition.CONNIVING.text", "Ambition.CONNIVING.description"),
    CONTROLLING("Ambition.CONTROLLING.text", "Ambition.CONTROLLING.description"),
    CUTTHROAT("Ambition.CUTTHROAT.text", "Ambition.CUTTHROAT.description"),
    // Major Trait
    DISHONEST("Ambition.DISHONEST.text", "Ambition.DISHONEST.description"),
    DILIGENT("Ambition.DILIGENT.text", "Ambition.DILIGENT.description"),
    DRIVEN("Ambition.DRIVEN.text", "Ambition.DRIVEN.description"),
    ENERGETIC("Ambition.ENERGETIC.text", "Ambition.ENERGETIC.description"),
    EXCESSIVE("Ambition.EXCESSIVE.text", "Ambition.EXCESSIVE.description"),
    FOCUSED("Ambition.FOCUSED.text", "Ambition.FOCUSED.description"),
    GOAL_ORIENTED("Ambition.GOAL_ORIENTED.text", "Ambition.GOAL_ORIENTED.description"),
    // Major Trait
    INNOVATIVE("Ambition.INNOVATIVE.text", "Ambition.INNOVATIVE.description"),
    // Major Trait
    MANIPULATIVE("Ambition.MANIPULATIVE.text", "Ambition.MANIPULATIVE.description"),
    MOTIVATED("Ambition.MOTIVATED.text", "Ambition.MOTIVATED.description"),
    OPPORTUNISTIC("Ambition.OPPORTUNISTIC.text", "Ambition.OPPORTUNISTIC.description"),
    OVERCONFIDENT("Ambition.OVERCONFIDENT.text", "Ambition.OVERCONFIDENT.description"),
    PERSISTENT("Ambition.PERSISTENT.text", "Ambition.PERSISTENT.description"),
    PROACTIVE("Ambition.PROACTIVE.text", "Ambition.PROACTIVE.description"),
    RESILIENT("Ambition.RESILIENT.text", "Ambition.RESILIENT.description"),
    // Major Trait
    RESOURCEFUL("Ambition.RESOURCEFUL.text", "Ambition.RESOURCEFUL.description"),
    RUTHLESS("Ambition.RUTHLESS.text", "Ambition.RUTHLESS.description"),
    SELFISH("Ambition.SELFISH.text", "Ambition.SELFISH.description"),
    STRATEGIC("Ambition.STRATEGIC.text", "Ambition.STRATEGIC.description"),
    // Major Trait
    TYRANNICAL("Ambition.TYRANNICAL.text", "Ambition.TYRANNICAL.description"),
    UNAMBITIOUS("Ambition.UNAMBITIOUS.text", "Ambition.UNAMBITIOUS.description"),
    UNSCRUPULOUS("Ambition.UNSCRUPULOUS.text", "Ambition.UNSCRUPULOUS.description"),
    // Major Trait
    VISIONARY("Ambition.VISIONARY.text", "Ambition.VISIONARY.description");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String description;
    //endregion Variable Declarations

    //region Constructors
    Ambition(final String name, final String description) {
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
    @SuppressWarnings(value = "unused")
    public boolean isNone() {
        return this == NONE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isAmbitious() {
        return this == AMBITIOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isArrogant() {
        return this == ARROGANT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isAspiring() {
        return this == ASPIRING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCalculating() {
        return this == CALCULATING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isConniving() {
        return this == CONNIVING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isControlling() {
        return this == CONTROLLING;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCutthroat() {
        return this == CUTTHROAT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDishonest() {
        return this == DISHONEST;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDiligent() {
        return this == DILIGENT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDriven() {
        return this == DRIVEN;
    }

    @SuppressWarnings(value = "unused")
    public boolean isEnergetic() {
        return this == ENERGETIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isExcessive() {
        return this == EXCESSIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isFocused() {
        return this == FOCUSED;
    }

    @SuppressWarnings(value = "unused")
    public boolean isGoalOriented() {
        return this == GOAL_ORIENTED;
    }

    @SuppressWarnings(value = "unused")
    public boolean isInnovative() {
        return this == INNOVATIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isManipulative() {
        return this == MANIPULATIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isMotivated() {
        return this == MOTIVATED;
    }

    @SuppressWarnings(value = "unused")
    public boolean isOpportunistic() {
        return this == OPPORTUNISTIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isOverconfident() {
        return this == OVERCONFIDENT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isPersistent() {
        return this == PERSISTENT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isProactive() {
        return this == PROACTIVE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isResilient() {
        return this == RESILIENT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isResourceful() {
        return this == RESOURCEFUL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isRuthless() {
        return this == RUTHLESS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isSelfish() {
        return this == SELFISH;
    }

    @SuppressWarnings(value = "unused")
    public boolean isStrategic() {
        return this == STRATEGIC;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTyrannical() {
        return this == TYRANNICAL;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUnambitious() {
        return this == UNAMBITIOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUnscrupulous() {
        return this == UNSCRUPULOUS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isVisionary() {
        return this == VISIONARY;
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
    @SuppressWarnings(value = "unused")
    public static Ambition parseFromString(final String ambition) {
        return switch (ambition.toLowerCase()) {
            case "0", "none" -> NONE;
            case "1", "ambitious" -> AMBITIOUS;
            case "2", "arrogant" -> ARROGANT;
            case "3", "aspiring" -> ASPIRING;
            case "4", "calculating" -> CALCULATING;
            case "5", "conniving" -> CONNIVING;
            case "6", "controlling" -> CONTROLLING;
            case "7", "cutthroat" -> CUTTHROAT;
            case "8", "dishonest" -> DISHONEST;
            case "9", "diligent" -> DILIGENT;
            case "10", "driven" -> DRIVEN;
            case "11", "energetic" -> ENERGETIC;
            case "12", "excessive" -> EXCESSIVE;
            case "13", "focused" -> FOCUSED;
            case "14", "goal-oriented" -> GOAL_ORIENTED;
            case "15", "innovative" -> INNOVATIVE;
            case "16", "manipulative" -> MANIPULATIVE;
            case "17", "motivated" -> MOTIVATED;
            case "18", "opportunistic" -> OPPORTUNISTIC;
            case "19", "overconfident" -> OVERCONFIDENT;
            case "20", "persistent" -> PERSISTENT;
            case "21", "proactive" -> PROACTIVE;
            case "22", "resilient" -> RESILIENT;
            case "23", "resourceful" -> RESOURCEFUL;
            case "24", "ruthless" -> RUTHLESS;
            case "25", "selfish" -> SELFISH;
            case "26", "strategic" -> STRATEGIC;
            case "27", "tyrannical" -> TYRANNICAL;
            case "28", "unambitious" -> UNAMBITIOUS;
            case "29", "unscrupulous" -> UNSCRUPULOUS;
            case "30", "visionary" -> VISIONARY;
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
    @SuppressWarnings(value = "unused")
    public static Ambition parseFromInt(final int ambition) {
        return switch (ambition) {
            case 0 -> NONE;
            case 1 -> AMBITIOUS;
            case 2 -> ARROGANT;
            case 3 -> ASPIRING;
            case 4 -> CALCULATING;
            case 5 -> CONNIVING;
            case 6 -> CONTROLLING;
            case 7 -> CUTTHROAT;
            case 8 -> DISHONEST;
            case 9 -> DILIGENT;
            case 10 -> DRIVEN;
            case 11 -> ENERGETIC;
            case 12 -> EXCESSIVE;
            case 13 -> FOCUSED;
            case 14 -> GOAL_ORIENTED;
            case 15 -> INNOVATIVE;
            case 16 -> MANIPULATIVE;
            case 17 -> MOTIVATED;
            case 18 -> OPPORTUNISTIC;
            case 19 -> OVERCONFIDENT;
            case 20 -> PERSISTENT;
            case 21 -> PROACTIVE;
            case 22 -> RESILIENT;
            case 23 -> RESOURCEFUL;
            case 24 -> RUTHLESS;
            case 25 -> SELFISH;
            case 26 -> STRATEGIC;
            case 27 -> TYRANNICAL;
            case 28 -> UNAMBITIOUS;
            case 29 -> UNSCRUPULOUS;
            case 30 -> VISIONARY;
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
    @SuppressWarnings(value = "unused")
    public static int parseToInt(final Ambition ambition) {
        return switch (ambition) {
            case NONE -> 0;
            case AMBITIOUS -> 1;
            case ARROGANT -> 2;
            case ASPIRING -> 3;
            case CALCULATING -> 4;
            case CONNIVING -> 5;
            case CONTROLLING -> 6;
            case CUTTHROAT -> 7;
            case DISHONEST -> 8;
            case DILIGENT -> 9;
            case DRIVEN -> 10;
            case ENERGETIC -> 11;
            case EXCESSIVE -> 12;
            case FOCUSED -> 13;
            case GOAL_ORIENTED -> 14;
            case INNOVATIVE -> 15;
            case MANIPULATIVE -> 16;
            case MOTIVATED -> 17;
            case OPPORTUNISTIC -> 18;
            case OVERCONFIDENT -> 19;
            case PERSISTENT -> 20;
            case PROACTIVE -> 21;
            case RESILIENT -> 22;
            case RESOURCEFUL -> 23;
            case RUTHLESS -> 24;
            case SELFISH -> 25;
            case STRATEGIC -> 26;
            case TYRANNICAL -> 27;
            case UNAMBITIOUS -> 28;
            case UNSCRUPULOUS -> 29;
            case VISIONARY -> 30;
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
