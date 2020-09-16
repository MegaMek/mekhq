/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
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
package mekhq.gui.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public enum PersonnelFilter {
    //region Personnel Filters
    //region Standard Personnel Filters
    ALL("PersonnelFilter.ALL", true, true, true),
    ACTIVE("PersonnelFilter.ACTIVE", true, true, true),
    COMBAT("PersonnelFilter.COMBAT", true, true, true),
    SUPPORT("PersonnelFilter.SUPPORT", true, true, true),
    MECHWARRIOR("PersonnelFilter.MECHWARRIOR", true, true, true),
    VEHICLE_CREWMEMBER("PersonnelFilter.VEHICLE_CREWMEMBER", true, true, false),
    GROUND_VEHICLE_DRIVER("PersonnelFilter.GROUND_VEHICLE_DRIVER", false, false, true),
    NAVAL_VEHICLE_DRIVER("PersonnelFilter.NAVAL_VEHICLE_DRIVER", false, false, true),
    VEHICLE_GUNNER("PersonnelFilter.VEHICLE_GUNNER", false, false, true),
    VEHICLE_CREW("PersonnelFilter.VEHICLE_CREW", false, false, true),
    VTOL_PILOT("PersonnelFilter.VTOL_PILOT", false, false, true),
    AEROSPACE_PILOT("PersonnelFilter.AEROSPACE_PILOT", true, true, true),
    CONVENTIONAL_AIRCRAFT_PILOT("PersonnelFilter.CONVENTIONAL_AIRCRAFT_PILOT", true, true,true),
    PROTOMECH_PILOT("PersonnelFilter.PROTOMECH_PILOT", true, true, true),
    BATTLE_ARMOUR("PersonnelFilter.BATTLE_ARMOUR", true, true, true),
    SOLDIER("PersonnelFilter.SOLDIER", true, true, true),
    VESSEL_CREWMEMBER("PersonnelFilter.VESSEL_CREWMEMBER", true, true, false),
    VESSEL_PILOT("PersonnelFilter.VESSEL_PILOT", false, false, true),
    VESSEL_CREW("PersonnelFilter.VESSEL_CREW", false, false, true),
    VESSEL_GUNNER("PersonnelFilter.VESSEL_GUNNER", false, false, true),
    VESSEL_NAVIGATOR("PersonnelFilter.VESSEL_NAVIGATOR", false, false, true),
    TECH("PersonnelFilter.TECH", true, true, false),
    MECH_TECH("PersonnelFilter.MECH_TECH", false, false, true),
    MECHANIC("PersonnelFilter.MECHANIC", false, false, true),
    AERO_TECH("PersonnelFilter.AERO_TECH", false, false, true),
    BA_TECH("PersonnelFilter.BA_TECH", false, false, true),
    ASTECH("PersonnelFilter.ASTECH", false, false, true),
    MEDICAL("PersonnelFilter.MEDICAL", true, true, false),
    DOCTOR("PersonnelFilter.DOCTOR", false, false, true),
    MEDIC("PersonnelFilter.MEDIC",  false, false, true),
    ADMINISTRATOR("PersonnelFilter.ADMINISTRATOR", true, true, false),
    ADMINISTRATOR_COMMAND("PersonnelFilter.ADMINISTRATOR_COMMAND", false, false, true),
    ADMINISTRATOR_LOGISTICS("PersonnelFilter.ADMINISTRATOR_LOGISTICS", false, false, true),
    ADMINISTRATOR_TRANSPORT("PersonnelFilter.ADMINISTRATOR_TRANSPORT", false, false, true),
    ADMINISTRATOR_HR("PersonnelFilter.ADMINISTRATOR_HR", false, false, true),
    //endregion Standard Personnel Filters

    //region Expanded Personnel Tab Filters
    DEPENDENT("PersonnelFilter.DEPENDENT", false, true, true),
    FOUNDER("PersonnelFilter.FOUNDER", false, true, true),
    PRISONER("PersonnelFilter.PRISONER", false, true, true),
    INACTIVE("PersonnelFilter.INACTIVE", false, true, true),
    RETIRED("PersonnelFilter.RETIRED", false, true, true),
    MIA("PersonnelFilter.MIA", false, true, true),
    KIA("PersonnelFilter.KIA", false, true, true),
    DEAD("PersonnelFilter.DEAD", false,  true,true);
    //endregion Expanded Personnel Tab Filters
    //endregion Personnel Filters

    private final String name;
    private final boolean standard;
    private final boolean expanded;
    private final boolean individualRolesExpanded;

    PersonnelFilter(String name, boolean standard, boolean expanded, boolean individualRolesExpanded) {
        this.name = ResourceBundle.getBundle("mekhq.resources.GUIEnums", new EncodeControl())
                .getString(name);
        this.standard = standard;
        this.expanded = expanded;
        this.individualRolesExpanded = individualRolesExpanded;
    }

    public boolean isStandard() {
        return standard;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isIndividualRolesExpanded() {
        return individualRolesExpanded;
    }

    public static List<PersonnelFilter> getStandardPersonnelFilters() {
        List<PersonnelFilter> standardFilters = new ArrayList<>();
        for (PersonnelFilter filter : values()) {
            if (filter.isStandard()) {
                standardFilters.add(filter);
            }
        }
        return standardFilters;
    }

    public static List<PersonnelFilter> getExpandedPersonnelFilters() {
        List<PersonnelFilter> expandedFilters = new ArrayList<>();
        for (PersonnelFilter filter : values()) {
            if (filter.isExpanded()) {
                expandedFilters.add(filter);
            }
        }
        return expandedFilters;
    }

    public static List<PersonnelFilter> getIndividualRolesExpandedPersonnelFilters() {
        List<PersonnelFilter> individualRolesExpandedFilters = new ArrayList<>();
        for (PersonnelFilter filter : values()) {
            if (filter.isIndividualRolesExpanded()) {
                individualRolesExpandedFilters.add(filter);
            }
        }
        return individualRolesExpandedFilters;
    }

    public boolean getFilteredInformation(Person person) {
        final boolean active = person.getStatus().isActive() && !person.getPrisonerStatus().isPrisoner();
        switch (this) {
            case ALL:
                return true;
            case ACTIVE:
                return active;
            case COMBAT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryCombatRole() : person.hasCombatRole());
            case SUPPORT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimarySupportRole(true) : person.hasSupportRole(true));
            case MECHWARRIOR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_MECHWARRIOR) : person.hasRole(Person.T_MECHWARRIOR));
            case VEHICLE_CREWMEMBER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? (person.hasPrimaryRoleWithin(Person.T_GVEE_DRIVER, Person.T_VEE_GUNNER) || person.hasPrimaryRole(Person.T_VEHICLE_CREW))
                        : (person.hasRoleWithin(Person.T_GVEE_DRIVER, Person.T_VEE_GUNNER) || person.hasRole(Person.T_VEHICLE_CREW)));
            case GROUND_VEHICLE_DRIVER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_GVEE_DRIVER) : person.hasRole(Person.T_GVEE_DRIVER));
            case NAVAL_VEHICLE_DRIVER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_NVEE_DRIVER) : person.hasRole(Person.T_NVEE_DRIVER));
            case VEHICLE_GUNNER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_VEE_GUNNER) : person.hasRole(Person.T_VEE_GUNNER));
            case VEHICLE_CREW:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_VEHICLE_CREW) : person.hasRole(Person.T_VEHICLE_CREW));
            case VTOL_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_VTOL_PILOT) : person.hasRole(Person.T_VTOL_PILOT));
            case AEROSPACE_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_AERO_PILOT) : person.hasRole(Person.T_AERO_PILOT));
            case CONVENTIONAL_AIRCRAFT_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_CONV_PILOT) : person.hasRole(Person.T_CONV_PILOT));
            case PROTOMECH_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_PROTO_PILOT) : person.hasRole(Person.T_PROTO_PILOT));
            case BATTLE_ARMOUR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_BA) : person.hasRole(Person.T_BA));
            case SOLDIER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_INFANTRY) : person.hasRole(Person.T_INFANTRY));
            case VESSEL_CREWMEMBER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRoleWithin(Person.T_SPACE_PILOT, Person.T_NAVIGATOR)
                        : person.hasRoleWithin(Person.T_SPACE_PILOT, Person.T_NAVIGATOR));
            case VESSEL_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_SPACE_PILOT) : person.hasRole(Person.T_SPACE_PILOT));
            case VESSEL_CREW:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_SPACE_CREW) : person.hasRole(Person.T_SPACE_CREW));
            case VESSEL_GUNNER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_SPACE_GUNNER) : person.hasRole(Person.T_SPACE_GUNNER));
            case VESSEL_NAVIGATOR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_NAVIGATOR) : person.hasRole(Person.T_NAVIGATOR));
            case TECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.isTechPrimary() : person.isTech());
            case MECH_TECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_MECH_TECH) : person.hasRole(Person.T_MECH_TECH));
            case MECHANIC:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_MECHANIC) : person.hasRole(Person.T_MECHANIC));
            case AERO_TECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_AERO_TECH) : person.hasRole(Person.T_AERO_TECH));
            case BA_TECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_BA_TECH) : person.hasRole(Person.T_BA_TECH));
            case ASTECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_ASTECH) : person.hasRole(Person.T_ASTECH));
            case MEDICAL:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRoleWithin(Person.T_DOCTOR, Person.T_MEDIC)
                        : person.hasRoleWithin(Person.T_DOCTOR, Person.T_MEDIC));
            case DOCTOR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_DOCTOR) : person.hasRole(Person.T_DOCTOR));
            case MEDIC:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_MEDIC) : person.hasRole(Person.T_MEDIC));
            case ADMINISTRATOR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRoleWithin(Person.T_ADMIN_COM, Person.T_ADMIN_HR)
                        : person.hasRoleWithin(Person.T_ADMIN_COM, Person.T_ADMIN_HR));
            case ADMINISTRATOR_COMMAND:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_ADMIN_COM) : person.hasRole(Person.T_ADMIN_COM));
            case ADMINISTRATOR_LOGISTICS:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_ADMIN_LOG) : person.hasRole(Person.T_ADMIN_LOG));
            case ADMINISTRATOR_TRANSPORT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_ADMIN_TRA) : person.hasRole(Person.T_ADMIN_TRA));
            case ADMINISTRATOR_HR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.hasPrimaryRole(Person.T_ADMIN_HR) : person.hasRole(Person.T_ADMIN_HR));
            case DEPENDENT:
                return active && person.isDependent();
            case FOUNDER:
                return person.isFounder();
            case PRISONER:
                return person.getPrisonerStatus().isPrisoner();
            case INACTIVE:
                return !person.getStatus().isActive();
            case RETIRED:
                return person.getStatus().isRetired();
            case MIA:
                return person.getStatus().isMIA();
            case KIA:
                return person.getStatus().isKIA();
            case DEAD:
                return person.getStatus().isDead();
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
