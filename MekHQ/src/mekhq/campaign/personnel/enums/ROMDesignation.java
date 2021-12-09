/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

import java.util.ResourceBundle;

public enum ROMDesignation {
    //region Enum Declarations
    NONE("ROMDesignations.NONE.text"),
    EPSILON("ROMDesignations.EPSILON.text"),
    PI("ROMDesignations.PI.text"),
    IOTA("ROMDesignations.IOTA.text"),
    XI("ROMDesignations.XI.text"),
    THETA("ROMDesignations.THETA.text"),
    ZETA("ROMDesignations.ZETA.text"),
    MU("ROMDesignations.MU.text"),
    RHO("ROMDesignations.RHO.text"),
    LAMBDA("ROMDesignations.LAMBDA.text"),
    PSI("ROMDesignations.PSI.text"),
    OMICRON("ROMDesignations.OMICRON.text"),
    CHI("ROMDesignations.CHI.text"),
    GAMMA("ROMDesignations.GAMMA.text"),
    KAPPA("ROMDesignations.KAPPA.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String designation;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel", new EncodeControl());
    //endregion Variable Declarations

    ROMDesignation(String designation) {
        this.designation = resources.getString(designation);
    }

    public static String getComStarBranchDesignation(Person person) {
        StringBuilder sb = new StringBuilder(" ");

        // Primary
        if (person.getPrimaryDesignator() != NONE) {
            sb.append(person.getPrimaryDesignator());
        } else if (person.getPrimaryRole().isTech()) {
            sb.append(ZETA);
        } else if (person.getPrimaryRole().isAdministrator()) {
            sb.append(CHI);
        } else {
            sb.append(determineDesignationFromRole(person.getPrimaryRole(), person));
        }

        // Secondary
        if (person.getSecondaryDesignator() != NONE) {
            sb.append(" ").append(person.getSecondaryDesignator());
        } else if (person.getSecondaryRole().isTechSecondary()) {
            sb.append(" ").append(ZETA);
        } else if (person.getSecondaryRole().isAdministrator()) {
            sb.append(" ").append(CHI);
        } else if (!person.getSecondaryRole().isNone()) {
            sb.append(" ").append(determineDesignationFromRole(person.getSecondaryRole(), person));
        }

        return sb.toString();
    }

    private static String determineDesignationFromRole(PersonnelRole role, Person person) {
        switch (role) {
            case MECHWARRIOR:
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
                Unit u = person.getUnit();
                if (u != null) {
                    Entity en = u.getEntity();
                    if (en instanceof Dropship) {
                        return XI.toString();
                    } else if (en instanceof Jumpship) {
                        return THETA.toString();
                    }
                }
                break;
            case DOCTOR:
            case MEDIC:
                return KAPPA.toString();
            default:
                break;
        }

        return "";
    }

    @Override
    public String toString() {
        return this.designation;
    }

    public static ROMDesignation parseFromString(String information) {
        // Parse based on the enum name
        try {
            return valueOf(information);
        } catch (Exception ignored) {

        }

        // Parse from Ordinal Int - Legacy save method
        ROMDesignation[] values = values();
        try {
            int designation = Integer.parseInt(information);
            if (values.length > designation) {
                return values[designation];
            }
        } catch (Exception ignored) {

        }

        // Could not parse based on either method, so return NONE
        MekHQ.getLogger().error("Unable to parse " + information + " into a ROMDesignation. Returning NONE");

        return ROMDesignation.NONE;
    }
}
