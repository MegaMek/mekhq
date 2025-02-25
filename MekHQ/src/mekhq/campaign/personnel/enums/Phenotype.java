/*
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * The {@link Phenotype} Enum represents various phenotypes a Clan character can have.
 * Each {@link Phenotype} is associated with a name, short name, grouping name and a tooltip text.
 * Each {@link Phenotype} can be classified as either {@code external} or {@code internal}.
 */
public enum Phenotype {
    // region Enum Declarations
    /**
     * Individual external phenotypes.
     */
    MEKWARRIOR("Phenotype.MEKWARRIOR.text", "Trueborn.text",
            "Phenotype.MEKWARRIOR.text", "Phenotype.MEKWARRIOR.toolTipText"),
    ELEMENTAL("Phenotype.ELEMENTAL.text", "Trueborn.text",
            "Phenotype.ELEMENTAL.text", "Phenotype.ELEMENTAL.toolTipText"),
    AEROSPACE("Phenotype.AEROSPACE.text", "Trueborn.text",
            "Phenotype.AEROSPACE.groupingNameText", "Phenotype.AEROSPACE.toolTipText"),
    VEHICLE("Phenotype.VEHICLE.text", "Trueborn.text",
            "Phenotype.VEHICLE.groupingNameText", "Phenotype.VEHICLE.toolTipText"),
    PROTOMEK("Phenotype.PROTOMEK.text", "Trueborn.text",
            "Phenotype.PROTOMEK.groupingNameText", "Phenotype.PROTOMEK.toolTipText"),
    NAVAL("Phenotype.NAVAL.text", "Trueborn.text",
            "Phenotype.NAVAL.groupingNameText", "Phenotype.NAVAL.toolTipText"),

    /**
     * Individual internal phenotypes.
     */
    // Internal Phenotypes
    NONE("Phenotype.NONE.text", "Freeborn.text",
            "Phenotype.NONE.text", "Phenotype.NONE.toolTipText", false),
    GENERAL("Phenotype.GENERAL.text", "Trueborn.text",
            "Phenotype.GENERAL.text", "Phenotype.GENERAL.toolTipText", false);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String shortName;
    private final String groupingName;
    private final String toolTipText;
    private final boolean external;
    // endregion Variable Declarations

    // region Constructors
    /**
     * Overloaded constructor to create an external {@link Phenotype}.
     *
     * @param name the name of the phenotype.
     * @param shortName the short name for phenotype.
     * @param groupingName the group the phenotype belongs to.
     * @param toolTipText tooltip text for the phenotype.
     */
    Phenotype(final String name, final String shortName, final String groupingName,
            final String toolTipText) {
        this(name, shortName, groupingName, toolTipText, true);
    }

    /**
     * Overloaded constructor to create a {@link Phenotype}, either external or internal.
     *
     * @param name the name of the phenotype.
     * @param shortName the short name for phenotype.
     * @param groupingName the group the phenotype belongs to.
     * @param toolTipText tooltip text for the phenotype.
     * @param external a boolean denoting whether the phenotype is internal ({@code false}) or
     *                external ({@code true}).
     */
    Phenotype(final String name, final String shortName, final String groupingName,
            final String toolTipText, final boolean external) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.shortName = resources.getString(shortName);
        this.groupingName = resources.getString(groupingName);
        this.toolTipText = resources.getString(toolTipText);
        this.external = external;
    }
    // endregion Constructors

    // region Getters
    /**
     * Retrieves the name of the phenotype.
     *
     * @return a {@link String} representing the name of the phenotype.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the short name of the phenotype.
     *
     * @return a {@link String} representing the short name of the phenotype.
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Retrieves the grouping name of the phenotype.
     *
     * @return a {@link String} representing the grouping name of the phenotype.
     */
    public String getGroupingName() {
        return groupingName;
    }

    /**
     * Retrieves the tooltip text associated with the phenotype.
     *
     * @return a {@link String} representing the tooltip text of the phenotype.
     */
    public String getToolTipText() {
        return toolTipText;
    }

    /**
     * Checks whether the phenotype is marked as external.
     *
     * @return a boolean, {@code true} if the phenotype is external, otherwise {@code false}.
     */
    public boolean isExternal() {
        return external;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    /**
     * Checks if the phenotype is MekWarrior.
     *
     * @return {@code true} if the phenotype is a Mek Warrior, {@code false} otherwise.
     */
    public boolean isMekWarrior() {
        return this == MEKWARRIOR;
    }

    /**
     * Checks if the phenotype is Elemental.
     *
     * @return {@code true} if the phenotype is an Elemental, {@code false} otherwise.
     */
    public boolean isElemental() {
        return this == ELEMENTAL;
    }

    /**
     * Checks if the phenotype is Aerospace.
     *
     * @return {@code true} if the phenotype is Aerospace, {@code false} otherwise.
     */
    public boolean isAerospace() {
        return this == AEROSPACE;
    }

    /**
     * Checks if the phenotype is Vehicle.
     *
     * @return {@code true} if the phenotype is a Vehicle, {@code false} otherwise.
     */
    public boolean isVehicle() {
        return this == VEHICLE;
    }

    /**
     * Checks if the phenotype is ProtoMek.
     *
     * @return {@code true} if the phenotype is a ProtoMek, {@code false} otherwise.
     */
    public boolean isProtoMek() {
        return this == PROTOMEK;
    }

    /**
     * Checks if the phenotype is Naval.
     *
     * @return {@code true} if the phenotype is Naval, {@code false} otherwise.
     */
    public boolean isNaval() {
        return this == NAVAL;
    }

    /**
     * Checks if the phenotype is None.
     *
     * @return {@code true} if the phenotype is None, {@code false} otherwise.
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * Checks if the phenotype is General.
     *
     * @return {@code true} if the phenotype is General, {@code false} otherwise.
     */
    public boolean isGeneral() {
        return this == GENERAL;
    }
    // endregion Boolean Comparison Methods

    /**
     * Retrieves a list of external phenotypes.
     *
     * @return a {@link List} of {@link Phenotype} objects where {@code isExternal()} returns
     * {@code true}.
     */
    public static List<Phenotype> getExternalPhenotypes() {
        return Arrays.stream(values())
                .filter(Phenotype::isExternal)
                .collect(Collectors.toList());
    }

    /**
     * Parses a string representation of a {@link Phenotype} and returns the corresponding phenotype
     * object.
     * If the string cannot be parsed into a valid phenotype, returns {@code Phenotype.NONE}.
     *
     * @param phenotype the string representation of the phenotype to parse
     * @return the parsed phenotype
     */
    // region File I/O
    public static Phenotype parseFromString(final String phenotype) {
        try {
            return valueOf(phenotype);
        } catch (Exception ignored) {}

        try {
            switch (Integer.parseInt(phenotype)) {
                case 0:
                    return NONE;
                case 1:
                    return MEKWARRIOR;
                case 2:
                    return ELEMENTAL;
                case 3:
                    return AEROSPACE;
                case 4:
                    return VEHICLE;
                default:
                    break;
            }
        } catch (Exception ignored) {}

        // <50.1 compatibility
        switch (phenotype) {
            case "MECHWARRIOR" -> {
                return MEKWARRIOR;
            }
            case "PROTOMECH" -> {
                return PROTOMEK;
            }
            default -> {}
        }

        MMLogger.create(Phenotype.class).error(
                String.format("Unable to parse %s into a Phenotype. Returning NONE.", phenotype));
        return NONE;
    }
    // endregion File I/O

    /**
     * @return The string representation of a {@link Phenotype}.
     *         If the phenotype is {@code None} or {@code General}, it returns short name.
     *         Otherwise, it returns the short name followed by a space and group name.
     */
    @Override
    public String toString() {
        return (isNone() || isGeneral()) ? getShortName() : getShortName() + ' ' + getGroupingName();
    }
}
