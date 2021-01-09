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
package mekhq.campaign.parts.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public enum PartRepairType {
    //region Enum Declarations
    ARMOR("PartRepairType.ARMOR.text", true),
    AMMO("PartRepairType.AMMO.text", true),
    WEAPON("PartRepairType.WEAPON.text", true),
    GENERAL_LOCATION("PartRepairType.GENERAL_LOCATION.text", true),
    ENGINE("PartRepairType.ENGINE.text", true),
    GYRO("PartRepairType.GYRO.text", true),
    ACTUATOR("PartRepairType.ACTUATOR.text", true),
    ELECTRONICS("PartRepairType.ELECTRONICS.text", true),
    GENERAL("PartRepairType.GENERAL.text", true),
    HEAT_SINK("PartRepairType.HEAT_SINK.text", false),
    MEK_LOCATION("PartRepairType.MEK_LOCATION.text", false),
    PHYSICAL_WEAPON("PartRepairType.PHYSICAL_WEAPON.text", false),
    POD_SPACE("PartRepairType.POD_SPACE.text", true),
    UNKNOWN_LOCATION("PartRepairType.UNKNOWN_LOCATION.text", false);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final boolean validForMRMS;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Parts",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PartRepairType(String name, boolean validForMRMS) {
        this.name = resources.getString(name);
        this.validForMRMS = validForMRMS;
    }
    //endregion Constructors

    //region Getters
    public boolean isValidForMRMS() {
        return validForMRMS;
    }
    //endregion Getters

    public static List<PartRepairType> getMRMSValidTypes() {
        List<PartRepairType> partRepairTypes = new ArrayList<>();
        for (PartRepairType partRepairType : values()) {
            if (partRepairType.isValidForMRMS()) {
                partRepairTypes.add(partRepairType);
            }
        }

        return partRepairTypes;
    }

    public static PartRepairType parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return ARMOR;
                case 1:
                    return AMMO;
                case 2:
                    return WEAPON;
                case 3:
                    return GENERAL_LOCATION;
                case 4:
                    return ENGINE;
                case 5:
                    return GYRO;
                case 6:
                    return ACTUATOR;
                case 7:
                    return ELECTRONICS;
                case 8:
                    return GENERAL;
                case 9:
                    return HEAT_SINK;
                case 10:
                    return MEK_LOCATION;
                case 11:
                    return PHYSICAL_WEAPON;
                case 12:
                    return POD_SPACE;
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }

        MekHQ.getLogger().error("Unknown part repair type, returning GENERAL_LOCATION");

        return GENERAL_LOCATION;
    }

    @Override
    public String toString() {
        return name;
    }
}
