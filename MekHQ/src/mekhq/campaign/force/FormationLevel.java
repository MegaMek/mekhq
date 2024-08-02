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
package mekhq.campaign.force;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;

import java.util.ResourceBundle;

public enum FormationLevel {
    //region Enum Declarations
    REMOVE_OVERRIDE("FormationLevel.REMOVE_OVERRIDE.text", "FormationLevel.REMOVE_OVERRIDE.description", -1, true, true, true),
    NONE("FormationLevel.NONE.text", "FormationLevel.NONE.description", -1, true, true, true),
    INVALID("FormationLevel.INVALID.text", "FormationLevel.INVALID.description", -1, true, true, true),

    // Inner Sphere
    LANCE("FormationLevel.LANCE.text", "FormationLevel.LANCE.description", 0, true, false, false),
    COMPANY("FormationLevel.COMPANY.text", "FormationLevel.COMPANY.description", 1, true, false, false),
    BATTALION("FormationLevel.BATTALION.text", "FormationLevel.BATTALION.description", 2, true, false, false),
    REGIMENT("FormationLevel.REGIMENT.text", "FormationLevel.REGIMENT.description", 3, true, false, false),
    BRIGADE("FormationLevel.BRIGADE.text", "FormationLevel.BRIGADE.description", 4, true, false, false),
    DIVISION("FormationLevel.DIVISION.text", "FormationLevel.DIVISION.description", 5, true, false, false),
    CORPS("FormationLevel.CORPS.text", "FormationLevel.CORPS.description", 6, true, false, false),
    ARMY("FormationLevel.ARMY.text", "FormationLevel.ARMY.description", 7, true, false, false),

    // Clan
    STAR_OR_NOVA("FormationLevel.STAR_OR_NOVA.text", "FormationLevel.STAR_OR_NOVA.description", 0, false, true, false),
    BINARY_OR_TRINARY("FormationLevel.BINARY_OR_TRINARY.text", "FormationLevel.BINARY_OR_TRINARY.description", 1, false, true, false),
    CLUSTER("FormationLevel.CLUSTER.text", "FormationLevel.CLUSTER.description", 2, false, true, false),
    GALAXY("FormationLevel.GALAXY.text", "FormationLevel.GALAXY.description", 3, false, true, false),
    TOUMAN("FormationLevel.TOUMAN.text", "FormationLevel.TOUMAN.description", 4, false, true, false),

    // ComStar
    LEVEL_II_OR_CHOIR("FormationLevel.LEVEL_II_OR_CHOIR.text", "FormationLevel.LEVEL_II_OR_CHOIR.description", 0, false, false, true),
    LEVEL_III("FormationLevel.LEVEL_III.text", "FormationLevel.LEVEL_III.description", 1, false, false, true),
    LEVEL_IV("FormationLevel.LEVEL_IV.text", "FormationLevel.LEVEL_IV.description", 2, false, false, true),
    LEVEL_V("FormationLevel.LEVEL_V.text", "FormationLevel.LEVEL_V.description", 3, false, false, true),
    LEVEL_VI("FormationLevel.LEVEL_VI.text", "FormationLevel.LEVEL_VI.description", 4, false, false, true);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String description;
    private final int depth;
    private final boolean isInnerSphere;
    private final boolean isClan;
    private final boolean isComStar;
    //endregion Variable Declarations

    //region Constructors
    FormationLevel(final String name, final String description, int depth, boolean isIS, boolean isClan, boolean isCS) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.FormationLevel",
                MekHQ.getMHQOptions().getLocale());

        this.name = resources.getString(name);
        this.description = resources.getString(description);
        this.depth = depth;
        this.isInnerSphere = isIS;
        this.isClan = isClan;
        this.isComStar = isCS;
    }
    //endregion Constructors

    //region Getters
    @SuppressWarnings(value = "unused")
    public String getDescription() {
        return description;
    }

    @SuppressWarnings(value = "unused")
    public int getDepth() {
        return depth;
    }

    @SuppressWarnings(value = "unused")
    public boolean isInnerSphere() {
        return isInnerSphere;
    }

    @SuppressWarnings(value = "unused")
    public boolean isClan() {
        return isClan;
    }

    @SuppressWarnings(value = "unused")
    public boolean isComStar() {
        return isComStar;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @SuppressWarnings(value = "unused")
    public boolean isRemoveOverride() {
        return this == REMOVE_OVERRIDE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isNone() {
        return this == NONE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isInvalid() {
        return this == INVALID;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLance() {
        return this == LANCE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCompany() {
        return this == COMPANY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isBattalion() {
        return this == BATTALION;
    }

    @SuppressWarnings(value = "unused")
    public boolean isRegiment() {
        return this == REGIMENT;
    }

    @SuppressWarnings(value = "unused")
    public boolean isBrigade() {
        return this == BRIGADE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isDivision() {
        return this == DIVISION;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCorps() {
        return this == CORPS;
    }

    @SuppressWarnings(value = "unused")
    public boolean isArmy() {
        return this == ARMY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isStarOrNova() {
        return this == STAR_OR_NOVA;
    }

    @SuppressWarnings(value = "unused")
    public boolean isBinaryOrTrinary() {
        return this == BINARY_OR_TRINARY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCluster() {
        return this == CLUSTER;
    }

    @SuppressWarnings(value = "unused")
    public boolean isGalaxy() {
        return this == GALAXY;
    }

    @SuppressWarnings(value = "unused")
    public boolean isTouman() {
        return this == TOUMAN;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLevelTwoOrChoir() {
        return this == LEVEL_II_OR_CHOIR;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLevelThree() {
        return this == LEVEL_III;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLevelFour() {
        return this == LEVEL_IV;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLevelFive() {
        return this == LEVEL_V;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLevelSix() {
        return this == LEVEL_VI;
    }

    @SuppressWarnings(value = "unused")
    public boolean isLanceEquivalent() {
        return this == LANCE || this == STAR_OR_NOVA || this == LEVEL_II_OR_CHOIR;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCompanyEquivalent() {
        return this == COMPANY || this == BINARY_OR_TRINARY || this == LEVEL_III;
    }

    @SuppressWarnings(value = "unused")
    public boolean isBattalionEquivalent() {
        return this == BATTALION || this == CLUSTER || this == LEVEL_IV;
    }
    //endregion Boolean Comparison Methods

    //region File I/O

    /**
     * Parses a string representation of a formation level and returns the corresponding FormationLevel enum value.
     *
     * @param formationLevel the string representation of the formation level
     * @return the FormationLevel enum value corresponding to the given formation level string
     * @throws IllegalStateException if the formation level string is not recognized
     */
    @SuppressWarnings(value = "unused")
    public static FormationLevel parseFromString(final String formationLevel) {
        return switch (formationLevel) {
            case "0", "Remove Override" -> REMOVE_OVERRIDE;
            case "1", "None" -> NONE;
            case "2", "Invalid" -> INVALID;

            // Inner Sphere
            case "3", "Lance" -> LANCE;
            case "4", "Company" -> COMPANY;
            case "5", "Battalion" -> BATTALION;
            case "6", "Regiment" -> REGIMENT;
            case "7", "Brigade" -> BRIGADE;
            case "8", "Division" -> DIVISION;
            case "9", "Corps" -> CORPS;
            case "10", "Army" -> ARMY;

            // Clan
            case "11", "Star or Nova" -> STAR_OR_NOVA;
            case "12", "Binary or Trinary" -> BINARY_OR_TRINARY;
            case "13", "Cluster" -> CLUSTER;
            case "14", "Galaxy" -> GALAXY;
            case "15", "Touman" -> TOUMAN;

            // ComStar
            case "16", "Level II or Choir" -> LEVEL_II_OR_CHOIR;
            case "17", "Level III" -> LEVEL_III;
            case "18", "Level IV" -> LEVEL_IV;
            case "19", "Level V" -> LEVEL_V;
            case "20", "Level VI" -> LEVEL_VI;

            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/force/FormationLevel.java/parseFromString: "
                            + formationLevel);
        };
    }

    /**
     * Parses an integer value representing a formation level and returns the corresponding FormationLevel enum value.
     *
     * @param formationLevel The integer value representing the formation level.
     * @return The FormationLevel enum value corresponding to the given integer value.
     * @throws IllegalStateException if the given formation level has no corresponding FormationLevel enum value.
     */
    @SuppressWarnings(value = "unused")
    public static FormationLevel parseFromInt(final int formationLevel) {
        return switch (formationLevel) {
            case 0 -> REMOVE_OVERRIDE;
            case 1 -> NONE;
            case 2 -> INVALID;

            // Inner Sphere
            case 3 -> LANCE;
            case 4 -> COMPANY;
            case 5 -> BATTALION;
            case 6 -> REGIMENT;
            case 7 -> BRIGADE;
            case 8 -> DIVISION;
            case 9 -> CORPS;
            case 10 -> ARMY;

            // Clan
            case 11 -> STAR_OR_NOVA;
            case 12 -> BINARY_OR_TRINARY;
            case 13 -> CLUSTER;
            case 14 -> GALAXY;
            case 15 -> TOUMAN;

            // ComStar
            case 16 -> LEVEL_II_OR_CHOIR;
            case 17 -> LEVEL_III;
            case 18 -> LEVEL_IV;
            case 19 -> LEVEL_V;
            case 20 -> LEVEL_VI;

            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/force/FormationLevel.java/parseFromInt: "
                            + formationLevel);
        };
    }

    /**
     * Parses a FormationLevel enum value to an integer representation.
     *
     * @return The integer representation of the FormationLevel enum value.
     * @throws IllegalStateException If the given FormationLevel is unexpected.
     */
    @SuppressWarnings(value = "unused")
    public int parseToInt() {
        return switch (this) {
            case REMOVE_OVERRIDE -> 0;
            case NONE -> 1;
            case INVALID -> 2;

            // Inner Sphere
            case LANCE -> 3;
            case COMPANY -> 4;
            case BATTALION -> 5;
            case REGIMENT -> 6;
            case BRIGADE -> 7;
            case DIVISION -> 8;
            case CORPS -> 9;
            case ARMY -> 10;

            // Clan
            case STAR_OR_NOVA -> 11;
            case BINARY_OR_TRINARY -> 12;
            case CLUSTER -> 13;
            case GALAXY -> 14;
            case TOUMAN -> 15;

            // ComStar
            case LEVEL_II_OR_CHOIR -> 16;
            case LEVEL_III -> 17;
            case LEVEL_IV -> 18;
            case LEVEL_V -> 19;
            case LEVEL_VI -> 20;
        };
    }

    /**
     * Parses the formation level based on a given depth and campaign faction.
     *
     * @param campaign The current campaign
     * @param depth The depth of the formation
     * @return The corresponding formation level
     */
    public static FormationLevel parseFromDepth(Campaign campaign, int depth) {
        Faction faction = campaign.getFaction();

        if (faction.isClan()) {
            return switch (depth) {
                case 0 -> STAR_OR_NOVA;
                case 1 -> BINARY_OR_TRINARY;
                case 2 -> CLUSTER;
                case 3 -> GALAXY;
                default -> TOUMAN;
            };
        } else if (faction.isComStarOrWoB()) {
            return switch (depth) {
                case 0 -> LEVEL_II_OR_CHOIR;
                case 1 -> LEVEL_III;
                case 2 -> LEVEL_IV;
                case 3 -> LEVEL_V;
                default -> LEVEL_VI;
            };
        } else {
            return switch (depth) {
                case 0 -> LANCE;
                case 1 -> COMPANY;
                case 2 -> BATTALION;
                case 3 -> REGIMENT;
                case 4 -> BRIGADE;
                case 5 -> DIVISION;
                case 6 -> CORPS;
                default -> ARMY;
            };
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
