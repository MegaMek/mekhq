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

public enum Phenotype {
    //region Enum Declarations
    // All External Phenotypes must be listed before any Internal Phenotypes
    // External Phenotypes
    MECHWARRIOR(0, "Phenotype.MECHWARRIOR.text", "Phenotype.MECHWARRIOR.toolTipText"),
    ELEMENTAL(1, "Phenotype.ELEMENTAL.text", "Phenotype.ELEMENTAL.toolTipText"),
    AEROSPACE(2, "Phenotype.AEROSPACE.text", "Phenotype.AEROSPACE.toolTipText"),
    VEHICLE(3, "Phenotype.VEHICLE.text", "Phenotype.VEHICLE.toolTipText"),
    PROTOMECH(4, "Phenotype.PROTOMECH.text", "Phenotype.PROTOMECH.toolTipText"),
    NAVAL(5, "Phenotype.NAVAL.text", "Phenotype.NAVAL.toolTipText"),
    // Internal Phenotypes
    NONE(-1, "Phenotype.NONE.text", "Phenotype.NONE.toolTipText"),
    GENERAL(-1, "Phenotype.GENERAL.text", "Phenotype.GENERAL.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTip;
    private final int index;
    //endregion Variable Declarations

    //region Constructors
    Phenotype(int index, String name, String toolTip) {
        this.name = name;
        this.toolTip = toolTip;
        this.index = index;
    }
    //endregion Constructors

    public String getToolTip() {
        return toolTip;
    }

    public int getIndex() {
        return index;
    }

    public static int getExternalPhenotypeCount() {
        int count = 0;
        for (Phenotype phenotype : values()) {
            if (phenotype.getIndex() != -1) {
                count++;
            }
        }
        return count;
    }

    public Phenotype parseFromString(String text) {
        return NONE;
    }

    @Override
    public String toString() {
        return name;
    }
}
