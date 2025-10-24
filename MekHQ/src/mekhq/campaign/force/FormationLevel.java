/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.force;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;

public enum FormationLevel {
    // region Enum Declarations
    REMOVE_OVERRIDE("FormationLevel.REMOVE_OVERRIDE.text", "FormationLevel.REMOVE_OVERRIDE.description", -1, true, true,
          true),
    NONE("FormationLevel.NONE.text", "FormationLevel.NONE.description", -1, true, true, true),
    INVALID("FormationLevel.INVALID.text", "FormationLevel.INVALID.description", -1, true, true, true),

    // Generic
    TEAM("FormationLevel.TEAM.text", "FormationLevel.TEAM.description", 0, true, true, true),

    // Inner Sphere
    LANCE("FormationLevel.LANCE.text", "FormationLevel.LANCE.description", 1, true, false, false),
    COMPANY("FormationLevel.COMPANY.text", "FormationLevel.COMPANY.description", 2, true, false, false),
    BATTALION("FormationLevel.BATTALION.text", "FormationLevel.BATTALION.description", 3, true, false, false),
    REGIMENT("FormationLevel.REGIMENT.text", "FormationLevel.REGIMENT.description", 4, true, false, false),
    BRIGADE("FormationLevel.BRIGADE.text", "FormationLevel.BRIGADE.description", 5, true, false, false),
    DIVISION("FormationLevel.DIVISION.text", "FormationLevel.DIVISION.description", 6, true, false, false),
    CORPS("FormationLevel.CORPS.text", "FormationLevel.CORPS.description", 7, true, false, false),
    ARMY("FormationLevel.ARMY.text", "FormationLevel.ARMY.description", 8, true, false, false),
    ARMY_GROUP("FormationLevel.ARMY_GROUP.text", "FormationLevel.ARMY_GROUP.description", 9, true, false, false),

    // Clan
    STAR_OR_NOVA("FormationLevel.STAR_OR_NOVA.text", "FormationLevel.STAR_OR_NOVA.description", 1, false, true, false),
    BINARY_OR_TRINARY("FormationLevel.BINARY_OR_TRINARY.text", "FormationLevel.BINARY_OR_TRINARY.description", 2, false,
          true, false),
    CLUSTER("FormationLevel.CLUSTER.text", "FormationLevel.CLUSTER.description", 3, false, true, false),
    GALAXY("FormationLevel.GALAXY.text", "FormationLevel.GALAXY.description", 4, false, true, false),
    TOUMAN("FormationLevel.TOUMAN.text", "FormationLevel.TOUMAN.description", 5, false, true, false),

    // ComStar
    LEVEL_II_OR_CHOIR("FormationLevel.LEVEL_II_OR_CHOIR.text", "FormationLevel.LEVEL_II_OR_CHOIR.description", 1, false,
          false, true),
    LEVEL_III("FormationLevel.LEVEL_III.text", "FormationLevel.LEVEL_III.description", 2, false, false, true),
    LEVEL_IV("FormationLevel.LEVEL_IV.text", "FormationLevel.LEVEL_IV.description", 3, false, false, true),
    LEVEL_V("FormationLevel.LEVEL_V.text", "FormationLevel.LEVEL_V.description", 4, false, false, true),
    LEVEL_VI("FormationLevel.LEVEL_VI.text", "FormationLevel.LEVEL_VI.description", 5, false, false, true);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String description;
    private final int depth;
    private final boolean isInnerSphere;
    private final boolean isClan;
    private final boolean isComStar;
    // endregion Variable Declarations

    // region Constructors
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
    // endregion Constructors

    // region Getters
    public String getDescription() {
        return description;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isInnerSphere() {
        return isInnerSphere;
    }

    public boolean isClan() {
        return isClan;
    }

    public boolean isComStar() {
        return isComStar;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isRemoveOverride() {
        return this == REMOVE_OVERRIDE;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isInvalid() {
        return this == INVALID;
    }

    public boolean isTeam() { return this == TEAM; }

    public boolean isLance() {
        return this == LANCE;
    }

    public boolean isCompany() {
        return this == COMPANY;
    }

    public boolean isBattalion() {
        return this == BATTALION;
    }

    public boolean isRegiment() {
        return this == REGIMENT;
    }

    public boolean isBrigade() {
        return this == BRIGADE;
    }

    public boolean isDivision() {
        return this == DIVISION;
    }

    public boolean isCorps() {
        return this == CORPS;
    }

    public boolean isArmy() {
        return this == ARMY;
    }

    public boolean isStarOrNova() {
        return this == STAR_OR_NOVA;
    }

    public boolean isBinaryOrTrinary() {
        return this == BINARY_OR_TRINARY;
    }

    public boolean isCluster() {
        return this == CLUSTER;
    }

    public boolean isGalaxy() {
        return this == GALAXY;
    }

    public boolean isTouman() {
        return this == TOUMAN;
    }

    public boolean isLevelTwoOrChoir() {
        return this == LEVEL_II_OR_CHOIR;
    }

    public boolean isLevelThree() {
        return this == LEVEL_III;
    }

    public boolean isLevelFour() {
        return this == LEVEL_IV;
    }

    public boolean isLevelFive() {
        return this == LEVEL_V;
    }

    public boolean isLevelSix() {
        return this == LEVEL_VI;
    }

    public boolean isLanceEquivalent() {
        return this == LANCE || this == STAR_OR_NOVA || this == LEVEL_II_OR_CHOIR;
    }

    public boolean isCompanyEquivalent() {
        return this == COMPANY || this == BINARY_OR_TRINARY || this == LEVEL_III;
    }

    public boolean isBattalionEquivalent() {
        return this == BATTALION || this == CLUSTER || this == LEVEL_IV;
    }
    // endregion Boolean Comparison Methods

    // region File I/O

    /**
     * Parses a string representation of a formation level and returns the corresponding FormationLevel enum value.
     *
     * @param formationLevel the string representation of the formation level
     *
     * @return the FormationLevel enum value corresponding to the given formation level string
     *
     * @throws IllegalStateException if the formation level string is not recognized
     */

    public static FormationLevel parseFromString(final String formationLevel) {
        return switch (formationLevel) {
            case "0", "Remove Override" -> REMOVE_OVERRIDE;
            case "1", "None" -> NONE;
            case "2", "Invalid", "Invalid Formation" -> INVALID;

            // Inner Sphere
            case "3", "Lance" -> LANCE;
            case "4", "Company" -> COMPANY;
            case "5", "Battalion" -> BATTALION;
            case "6", "Regiment" -> REGIMENT;
            case "7", "Brigade" -> BRIGADE;
            case "8", "Division" -> DIVISION;
            case "9", "Corps" -> CORPS;
            case "10", "Army" -> ARMY;
            case "11", "Army Group" -> ARMY_GROUP;

            // Clan
            case "12", "Star or Nova" -> STAR_OR_NOVA;
            case "13", "Binary or Trinary" -> BINARY_OR_TRINARY;
            case "14", "Cluster" -> CLUSTER;
            case "15", "Galaxy" -> GALAXY;
            case "16", "Touman" -> TOUMAN;

            // ComStar
            case "17", "Level II or Choir" -> LEVEL_II_OR_CHOIR;
            case "18", "Level III" -> LEVEL_III;
            case "19", "Level IV" -> LEVEL_IV;
            case "20", "Level V" -> LEVEL_V;
            case "21", "Level VI" -> LEVEL_VI;

            case "22", "Team" -> TEAM;

            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/force/FormationLevel.java/parseFromString: "
                        + formationLevel);
        };
    }

    /**
     * Parses an integer value representing a formation level and returns the corresponding FormationLevel enum value.
     *
     * @param formationLevel The integer value representing the formation level.
     *
     * @return The FormationLevel enum value corresponding to the given integer value.
     *
     * @throws IllegalStateException if the given formation level has no corresponding FormationLevel enum value.
     */

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
            case 11 -> ARMY_GROUP;

            // Clan
            case 12 -> STAR_OR_NOVA;
            case 13 -> BINARY_OR_TRINARY;
            case 14 -> CLUSTER;
            case 15 -> GALAXY;
            case 16 -> TOUMAN;

            // ComStar
            case 17 -> LEVEL_II_OR_CHOIR;
            case 18 -> LEVEL_III;
            case 19 -> LEVEL_IV;
            case 20 -> LEVEL_V;
            case 21 -> LEVEL_VI;

            case 22 -> TEAM;

            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/force/FormationLevel.java/parseFromInt: "
                        + formationLevel);
        };
    }

    /**
     * Parses a FormationLevel enum value to an integer representation.
     *
     * @return The integer representation of the FormationLevel enum value.
     *
     * @throws IllegalStateException If the given FormationLevel is unexpected.
     */

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
            case ARMY_GROUP -> 11;

            // Clan
            case STAR_OR_NOVA -> 12;
            case BINARY_OR_TRINARY -> 13;
            case CLUSTER -> 14;
            case GALAXY -> 15;
            case TOUMAN -> 16;

            // ComStar
            case LEVEL_II_OR_CHOIR -> 17;
            case LEVEL_III -> 18;
            case LEVEL_IV -> 19;
            case LEVEL_V -> 20;
            case LEVEL_VI -> 21;

            // Generic
            case TEAM -> 22;
        };
    }

    /**
     * Parses the formation level based on a given depth and campaign faction.
     *
     * @param campaign The current campaign
     * @param depth    The depth of the formation
     *
     * @return The corresponding formation level
     */
    public static FormationLevel parseFromDepth(Campaign campaign, int depth) {
        Faction faction = campaign.getFaction();

        if (faction.isClan()) {
            return switch (depth) {
                case 0 -> TEAM;
                case 1 -> STAR_OR_NOVA;
                case 2 -> BINARY_OR_TRINARY;
                case 3 -> CLUSTER;
                case 4 -> GALAXY;
                default -> TOUMAN;
            };
        } else if (faction.isComStarOrWoB()) {
            return switch (depth) {
                case 0 -> TEAM;
                case 1 -> LEVEL_II_OR_CHOIR;
                case 2 -> LEVEL_III;
                case 3 -> LEVEL_IV;
                case 4 -> LEVEL_V;
                default -> LEVEL_VI;
            };
        } else {
            return switch (depth) {
                case 0 -> TEAM;
                case 1 -> LANCE;
                case 2 -> COMPANY;
                case 3 -> BATTALION;
                case 4 -> REGIMENT;
                case 5 -> BRIGADE;
                case 6 -> DIVISION;
                case 7 -> CORPS;
                case 8 -> ARMY;
                default -> ARMY_GROUP;
            };
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
