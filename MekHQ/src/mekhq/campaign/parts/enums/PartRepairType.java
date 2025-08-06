/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.enums;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum PartRepairType {
    // region Enum Declarations
    ARMOUR("PartRepairType.ARMOUR.text", true),
    AMMUNITION("PartRepairType.AMMUNITION.text", true),
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
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final boolean validForMRMS;
    // endregion Variable Declarations

    // region Constructors
    PartRepairType(final String name, final boolean validForMRMS) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Parts",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.validForMRMS = validForMRMS;
    }
    // endregion Constructors

    // region Getters
    public boolean isValidForMRMS() {
        return validForMRMS;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isArmour() {
        return this == ARMOUR;
    }

    public boolean isAmmunition() {
        return this == AMMUNITION;
    }

    public boolean isWeapon() {
        return this == WEAPON;
    }

    public boolean isGeneralLocation() {
        return this == GENERAL_LOCATION;
    }

    public boolean isEngine() {
        return this == ENGINE;
    }

    public boolean isGyro() {
        return this == GYRO;
    }

    public boolean isActuator() {
        return this == ACTUATOR;
    }

    public boolean isElectronics() {
        return this == ELECTRONICS;
    }

    public boolean isGeneral() {
        return this == GENERAL;
    }

    public boolean isHeatSink() {
        return this == HEAT_SINK;
    }

    public boolean isMekLocation() {
        return this == MEK_LOCATION;
    }

    public boolean isPhysicalWeapon() {
        return this == PHYSICAL_WEAPON;
    }

    public boolean isPodSpace() {
        return this == POD_SPACE;
    }

    public boolean isUnknownLocation() {
        return this == UNKNOWN_LOCATION;
    }
    // endregion Boolean Comparison Methods

    public static List<PartRepairType> getMRMSValidTypes() {
        return Arrays.stream(values()).filter(PartRepairType::isValidForMRMS).collect(Collectors.toList());
    }

    // region File I/O
    public static PartRepairType parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return ARMOUR;
                case 1:
                    return AMMUNITION;
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
                default:
                    break;
            }
        } catch (Exception ex) {
            MMLogger.create(PartRepairType.class).error("", ex);
        }

        MMLogger.create(PartRepairType.class)
              .error("Unable to parse " + text + " into a PartRepairType. Returning GENERAL_LOCATION.");
        return GENERAL_LOCATION;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
