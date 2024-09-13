/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum Phenotype {
    // region Enum Declarations
    // External Phenotypes
    MEKWARRIOR("Phenotype.MEKWARRIOR.text", "Trueborn.text", "Phenotype.MEKWARRIOR.text",
            "Phenotype.MEKWARRIOR.toolTipText"),
    ELEMENTAL("Phenotype.ELEMENTAL.text", "Trueborn.text", "Phenotype.ELEMENTAL.text",
            "Phenotype.ELEMENTAL.toolTipText"),
    AEROSPACE("Phenotype.AEROSPACE.text", "Trueborn.text", "Phenotype.AEROSPACE.groupingNameText",
            "Phenotype.AEROSPACE.toolTipText"),
    VEHICLE("Phenotype.VEHICLE.text", "Trueborn.text", "Phenotype.VEHICLE.groupingNameText",
            "Phenotype.VEHICLE.toolTipText"),
    PROTOMEK("Phenotype.PROTOMEK.text", "Trueborn.text", "Phenotype.PROTOMEK.groupingNameText",
            "Phenotype.PROTOMEK.toolTipText"),
    NAVAL("Phenotype.NAVAL.text", "Trueborn.text", "Phenotype.NAVAL.groupingNameText", "Phenotype.NAVAL.toolTipText"),

    // Remove Milestone past 0.49.19
    MECHWARRIOR("Phenotype.MEKWARRIOR.text", "Trueborn.text", "Phenotype.MEKWARRIOR.text",
            "Phenotype.MEKWARRIOR.toolTipText"),
    PROTOMECH("Phenotype.PROTOMEK.text", "Trueborn.text", "Phenotype.PROTOMEK.groupingNameText",
            "Phenotype.PROTOMEK.toolTipText"),

    // Internal Phenotypes
    NONE("Phenotype.NONE.text", "Freeborn.text", "Phenotype.NONE.text", "Phenotype.NONE.toolTipText", false),
    GENERAL("Phenotype.GENERAL.text", "Trueborn.text", "Phenotype.GENERAL.text", "Phenotype.GENERAL.toolTipText",
            false);

    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String shortName;
    private final String groupingName;
    private final String toolTipText;
    private final boolean external;
    // endregion Variable Declarations

    // region Constructors
    Phenotype(final String name, final String shortName, final String groupingName,
            final String toolTipText) {
        this(name, shortName, groupingName, toolTipText, true);
    }

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
    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getGroupingName() {
        return groupingName;
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public boolean isExternal() {
        return external;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isMekWarrior() {
        return this == MEKWARRIOR;
    }

    public boolean isElemental() {
        return this == ELEMENTAL;
    }

    public boolean isAerospace() {
        return this == AEROSPACE;
    }

    public boolean isVehicle() {
        return this == VEHICLE;
    }

    public boolean isProtoMek() {
        return this == PROTOMEK;
    }

    public boolean isNaval() {
        return this == NAVAL;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isGeneral() {
        return this == GENERAL;
    }
    // endregion Boolean Comparison Methods

    public static List<Phenotype> getExternalPhenotypes() {
        return Arrays.stream(values())
                .filter(Phenotype::isExternal)
                .collect(Collectors.toList());
    }

    // region File I/O
    public static Phenotype parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
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
        } catch (Exception ignored) {

        }

        String message = String.format("Unable to parse %s into a Phenotype. Returning NONE.", text);
        MMLogger.create(Phenotype.class).error(message);
        return NONE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return (isNone() || isGeneral()) ? getShortName() : getShortName() + ' ' + getGroupingName();
    }
}
