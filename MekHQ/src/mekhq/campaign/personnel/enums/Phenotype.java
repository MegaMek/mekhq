/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public enum Phenotype {
    //region Enum Declarations
    // External Phenotypes
    MECHWARRIOR(0, "Phenotype.MECHWARRIOR.text", "Phenotype.TRUEBORN", "Phenotype.MECHWARRIOR.text", "Phenotype.MECHWARRIOR.toolTipText"),
    ELEMENTAL(1, "Phenotype.ELEMENTAL.text", "Phenotype.TRUEBORN", "Phenotype.ELEMENTAL.text", "Phenotype.ELEMENTAL.toolTipText"),
    AEROSPACE(2, "Phenotype.AEROSPACE.text", "Phenotype.TRUEBORN", "Phenotype.AEROSPACE.groupingNameText", "Phenotype.AEROSPACE.toolTipText"),
    VEHICLE(3, "Phenotype.VEHICLE.text", "Phenotype.TRUEBORN", "Phenotype.VEHICLE.groupingNameText", "Phenotype.VEHICLE.toolTipText"),
    PROTOMECH(4, "Phenotype.PROTOMECH.text", "Phenotype.TRUEBORN", "Phenotype.PROTOMECH.groupingNameText", "Phenotype.PROTOMECH.toolTipText"),
    NAVAL(5, "Phenotype.NAVAL.text", "Phenotype.TRUEBORN", "Phenotype.NAVAL.groupingNameText", "Phenotype.NAVAL.toolTipText"),
    // Internal Phenotypes - Must have an index of -1 or they will be displayed in external facing methods
    NONE(-1, "Phenotype.NONE.text", "Phenotype.FREEBORN", "Phenotype.NONE.text",  "Phenotype.NONE.toolTipText"),
    GENERAL(-1, "Phenotype.GENERAL.text", "Phenotype.TRUEBORN","Phenotype.GENERAL.text", "Phenotype.GENERAL.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String shortName;
    private final String groupingName;
    private final String toolTip;
    private final int index;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    Phenotype(int index, String name, String shortName, String groupingName, String toolTip) {
        this.name = resources.getString(name);
        this.shortName = resources.getString(shortName);
        this.groupingName = resources.getString(groupingName);
        this.toolTip = resources.getString(toolTip);
        this.index = index;
    }
    //endregion Constructors

    //region Getters
    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getGroupingName() {
        return groupingName;
    }

    public String getToolTip() {
        return toolTip;
    }

    public int getIndex() {
        return index;
    }
    //endregion Getters

    public static List<Phenotype> getExternalPhenotypes() {
        List<Phenotype> phenotypeList = new ArrayList<>();
        for (Phenotype phenotype : values()) {
            if (phenotype.getIndex() != -1) {
                phenotypeList.add(phenotype);
            }
        }
        return phenotypeList;
    }

    public static Phenotype parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 1:
                    return MECHWARRIOR;
                case 2:
                    return ELEMENTAL;
                case 3:
                    return AEROSPACE;
                case 4:
                    return VEHICLE;
                case 0:
                default:
                    return NONE;
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error(Phenotype.class, "parseFromString",
                "Unable to parse the phenotype from string " + text + ". Returning Phenotype.NONE");

        return NONE;
    }

    @Override
    public String toString() {
        if ((this == NONE) || (this == GENERAL)) {
            return getShortName();
        } else {
            return getShortName() + " " + getGroupingName();
        }
    }
}
