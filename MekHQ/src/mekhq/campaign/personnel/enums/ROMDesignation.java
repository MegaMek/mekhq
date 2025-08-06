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
package mekhq.campaign.personnel.enums;

import java.util.ResourceBundle;

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public enum ROMDesignation {
    // region Enum Declarations
    NONE("ROMDesignation.NONE.text"),
    EPSILON("ROMDesignation.EPSILON.text"),
    PI("ROMDesignation.PI.text"),
    IOTA("ROMDesignation.IOTA.text"),
    XI("ROMDesignation.XI.text"),
    THETA("ROMDesignation.THETA.text"),
    ZETA("ROMDesignation.ZETA.text"),
    MU("ROMDesignation.MU.text"),
    RHO("ROMDesignation.RHO.text"),
    LAMBDA("ROMDesignation.LAMBDA.text"),
    PSI("ROMDesignation.PSI.text"),
    OMICRON("ROMDesignation.OMICRON.text"),
    CHI("ROMDesignation.CHI.text"),
    GAMMA("ROMDesignation.GAMMA.text"),
    KAPPA("ROMDesignation.KAPPA.text");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    // endregion Variable Declarations

    // region Constructors
    ROMDesignation(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isEpsilon() {
        return this == EPSILON;
    }

    public boolean isPi() {
        return this == PI;
    }

    public boolean isIota() {
        return this == IOTA;
    }

    public boolean isXi() {
        return this == XI;
    }

    public boolean isTheta() {
        return this == THETA;
    }

    public boolean isZeta() {
        return this == ZETA;
    }

    public boolean isMu() {
        return this == MU;
    }

    public boolean isRho() {
        return this == RHO;
    }

    public boolean isLambda() {
        return this == LAMBDA;
    }

    public boolean isPsi() {
        return this == PSI;
    }

    public boolean isOmicron() {
        return this == OMICRON;
    }

    public boolean isChi() {
        return this == CHI;
    }

    public boolean isGamma() {
        return this == GAMMA;
    }

    public boolean isKappa() {
        return this == KAPPA;
    }
    // endregion Boolean Comparison Methods

    public static String getComStarBranchDesignation(final Person person) {
        final StringBuilder sb = new StringBuilder(" ");

        // Primary
        if (person.getPrimaryDesignator().isNone()) {
            sb.append(determineDesignationFromRole(person.getPrimaryRole(), person));
        } else {
            sb.append(person.getPrimaryDesignator());
        }

        // Secondary
        if (!person.getSecondaryDesignator().isNone()) {
            sb.append(' ').append(person.getSecondaryDesignator());
        } else if (!person.getSecondaryRole().isNone()) {
            sb.append(' ').append(determineDesignationFromRole(person.getSecondaryRole(), person));
        }

        return sb.toString();
    }

    private static String determineDesignationFromRole(final PersonnelRole role,
          final Person person) {
        switch (role) {
            case MEKWARRIOR:
            case LAM_PILOT:
                return EPSILON.toString();
            case GROUND_VEHICLE_DRIVER:
            case NAVAL_VEHICLE_DRIVER:
            case VTOL_PILOT:
            case VEHICLE_GUNNER:
            case VEHICLE_CREW:
            case CONVENTIONAL_AIRCRAFT_PILOT:
                return LAMBDA.toString();
            case AEROSPACE_PILOT:
                return PI.toString();
            case BATTLE_ARMOUR:
            case SOLDIER:
                return IOTA.toString();
            case VESSEL_PILOT:
            case VESSEL_GUNNER:
            case VESSEL_CREW:
            case VESSEL_NAVIGATOR:
                final Unit unit = person.getUnit();
                if (unit != null) {
                    final Entity entity = unit.getEntity();
                    if (entity instanceof Dropship) {
                        return XI.toString();
                    } else if (entity instanceof Jumpship) {
                        return THETA.toString();
                    }
                }
                break;
            case MEK_TECH:
            case MECHANIC:
            case AERO_TEK:
            case BA_TECH:
            case ASTECH:
                return ZETA.toString();
            case DOCTOR:
            case MEDIC:
                return KAPPA.toString();
            case ADMINISTRATOR_COMMAND:
            case ADMINISTRATOR_LOGISTICS:
            case ADMINISTRATOR_TRANSPORT:
            case ADMINISTRATOR_HR:
                return CHI.toString();
            default:
                break;
        }

        return "";
    }

    // region File I/O
    public static ROMDesignation parseFromString(final String text) {
        // Parse based on the enum name
        try {
            return valueOf(text);
        } catch (Exception ignored) {
            MMLogger.create(ROMDesignation.class)
                  .error(ignored, "Unable to parse " + text + " into a ROMDesignation. Returning NONE");
            return ROMDesignation.NONE;
        }
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
