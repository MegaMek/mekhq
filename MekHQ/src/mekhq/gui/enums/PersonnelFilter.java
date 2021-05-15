/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved
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
import mekhq.campaign.personnel.enums.PersonnelRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public enum PersonnelFilter {
    //region Personnel Filters
    //region Standard Personnel Filters
    ALL("PersonnelFilter.ALL.text"),
    ACTIVE("PersonnelFilter.ACTIVE.text"),
    COMBAT("PersonnelFilter.COMBAT.text"),
    SUPPORT("PersonnelFilter.SUPPORT.text"),
    MECHWARRIORS("PersonnelFilter.MECHWARRIORS.text"),
    MECHWARRIOR("PersonnelFilter.MECHWARRIOR.text", false, true),
    LAM_PILOT("PersonnelFilter.LAM_PILOT.text", false, true),
    VEHICLE_CREWMEMBER("PersonnelFilter.VEHICLE_CREWMEMBER.text", true, false),
    GROUND_VEHICLE_DRIVER("PersonnelFilter.GROUND_VEHICLE_DRIVER.text", false, true),
    NAVAL_VEHICLE_DRIVER("PersonnelFilter.NAVAL_VEHICLE_DRIVER.text", false, true),
    VTOL_PILOT("PersonnelFilter.VTOL_PILOT.text", false, true),
    VEHICLE_GUNNER("PersonnelFilter.VEHICLE_GUNNER.text", false, true),
    VEHICLE_CREW("PersonnelFilter.VEHICLE_CREW.text", false, true),
    AEROSPACE_PILOT("PersonnelFilter.AEROSPACE_PILOT.text"),
    CONVENTIONAL_AIRCRAFT_PILOT("PersonnelFilter.CONVENTIONAL_AIRCRAFT_PILOT.text"),
    PROTOMECH_PILOT("PersonnelFilter.PROTOMECH_PILOT.text"),
    BATTLE_ARMOUR("PersonnelFilter.BATTLE_ARMOUR.text"),
    SOLDIER("PersonnelFilter.SOLDIER.text"),
    VESSEL_CREWMEMBER("PersonnelFilter.VESSEL_CREWMEMBER.text", true, false),
    VESSEL_PILOT("PersonnelFilter.VESSEL_PILOT.text", false, true),
    VESSEL_GUNNER("PersonnelFilter.VESSEL_GUNNER.text", false, true),
    VESSEL_CREW("PersonnelFilter.VESSEL_CREW.text", false, true),
    VESSEL_NAVIGATOR("PersonnelFilter.VESSEL_NAVIGATOR.text", false, true),
    TECH("PersonnelFilter.TECH.text", true, false),
    MECH_TECH("PersonnelFilter.MECH_TECH.text", false, true),
    MECHANIC("PersonnelFilter.MECHANIC.text", false, true),
    AERO_TECH("PersonnelFilter.AERO_TECH.text", false, true),
    BA_TECH("PersonnelFilter.BA_TECH.text", false, true),
    ASTECH("PersonnelFilter.ASTECH.text", false, true),
    MEDICAL("PersonnelFilter.MEDICAL.text", true, false),
    DOCTOR("PersonnelFilter.DOCTOR.text", false, true),
    MEDIC("PersonnelFilter.MEDIC.text",  false, true),
    ADMINISTRATOR("PersonnelFilter.ADMINISTRATOR.text", true, false),
    ADMINISTRATOR_COMMAND("PersonnelFilter.ADMINISTRATOR_COMMAND.text", false, true),
    ADMINISTRATOR_LOGISTICS("PersonnelFilter.ADMINISTRATOR_LOGISTICS.text", false, true),
    ADMINISTRATOR_TRANSPORT("PersonnelFilter.ADMINISTRATOR_TRANSPORT.text", false, true),
    ADMINISTRATOR_HR("PersonnelFilter.ADMINISTRATOR_HR.text", false, true),
    //endregion Standard Personnel Filters

    //region Expanded Personnel Tab Filters
    DEPENDENT("PersonnelFilter.DEPENDENT.text", false, false),
    FOUNDER("PersonnelFilter.FOUNDER.text", false, false),
    PRISONER("PersonnelFilter.PRISONER.text", false, false),
    INACTIVE("PersonnelFilter.INACTIVE.text", false, false),
    RETIRED("PersonnelFilter.RETIRED.text", false, false),
    MIA("PersonnelFilter.MIA.text", false, false),
    KIA("PersonnelFilter.KIA.text", false, false),
    DEAD("PersonnelFilter.DEAD.text", false, false);
    //endregion Expanded Personnel Tab Filters
    //endregion Personnel Filters

    //region Variable Declarations
    private final String name;
    private final boolean baseline;
    private final boolean standard;
    private final boolean individualRole;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUIEnums", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PersonnelFilter(String name) {
        this(name, true, true, true);
    }

    PersonnelFilter(String name, boolean standard, boolean individualRole) {
        this(name, false, standard, individualRole);
    }
    PersonnelFilter(String name, boolean baseline, boolean standard, boolean individualRole) {
        this.name = resources.getString(name);
        this.baseline = baseline;
        this.standard = standard;
        this.individualRole = individualRole;
    }
    //endregion Constructors

    //region Getters
    public boolean isBaseline() {
        return baseline;
    }

    public boolean isStandard() {
        return standard;
    }

    public boolean isIndividualRole() {
        return individualRole;
    }
    //endregion Getters

    public static List<PersonnelFilter> getStandardPersonnelFilters() {
        List<PersonnelFilter> standardFilters = new ArrayList<>();
        for (PersonnelFilter filter : values()) {
            if (filter.isBaseline() || filter.isStandard()) {
                standardFilters.add(filter);
            }
        }
        return standardFilters;
    }

    public static List<PersonnelFilter> getExpandedPersonnelFilters() {
        List<PersonnelFilter> expandedFilters = new ArrayList<>();
        for (PersonnelFilter filter : values()) {
            if (filter.isBaseline() || !filter.isIndividualRole()) {
                expandedFilters.add(filter);
            }
        }
        return expandedFilters;
    }

    public static List<PersonnelFilter> getIndividualRolesStandardPersonnelFilters() {
        List<PersonnelFilter> individualRolesStandardFilters = new ArrayList<>();
        for (PersonnelFilter filter : values()) {
            if (filter.isBaseline() || filter.isIndividualRole()) {
                individualRolesStandardFilters.add(filter);
            }
        }
        return individualRolesStandardFilters;
    }

    public static List<PersonnelFilter> getIndividualRolesExpandedPersonnelFilters() {
        List<PersonnelFilter> individualRolesExpandedFilters = new ArrayList<>();
        for (PersonnelFilter filter : values()) {
            if (filter.isBaseline() || !filter.isStandard() || filter.isIndividualRole()) {
                individualRolesExpandedFilters.add(filter);
            }
        }
        return individualRolesExpandedFilters;
    }

    public static List<PersonnelFilter> getAllStandardFilters() {
        List<PersonnelFilter> allStandardFilters = new ArrayList<>();
        for (PersonnelFilter filter : values()) {
            if (filter.isBaseline() || filter.isStandard() || filter.isIndividualRole()) {
                allStandardFilters.add(filter);
            }
        }
        return allStandardFilters;
    }

    public static List<PersonnelFilter> getAllIndividualRoleFilters() {
        return new ArrayList<>(Arrays.asList(values()));
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
                        ? person.getPrimaryRole().isCombat() : person.hasCombatRole());
            case SUPPORT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? !person.getPrimaryRole().isCombat() : person.hasSupportRole(true));
            case MECHWARRIORS:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMechWarriorGrouping()
                        : (person.getPrimaryRole().isMechWarriorGrouping() || person.getSecondaryRole().isMechWarriorGrouping()));
            case MECHWARRIOR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMechWarrior() : person.hasRole(PersonnelRole.MECHWARRIOR));
            case LAM_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isLAMPilot() : person.hasRole(PersonnelRole.LAM_PILOT));
            case VEHICLE_CREWMEMBER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVehicleCrewmember()
                        : (person.getPrimaryRole().isVehicleCrewmember() || person.getSecondaryRole().isVehicleCrewmember()));
            case GROUND_VEHICLE_DRIVER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isGroundVehicleDriver() : person.hasRole(PersonnelRole.GROUND_VEHICLE_DRIVER));
            case NAVAL_VEHICLE_DRIVER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isNavalVehicleDriver() : person.hasRole(PersonnelRole.NAVAL_VEHICLE_DRIVER));
            case VEHICLE_GUNNER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVehicleGunner() : person.hasRole(PersonnelRole.VEHICLE_GUNNER));
            case VEHICLE_CREW:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVehicleCrew() : person.hasRole(PersonnelRole.VEHICLE_CREW));
            case VTOL_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVTOLPilot() : person.hasRole(PersonnelRole.VTOL_PILOT));
            case AEROSPACE_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAerospacePilot() : person.hasRole(PersonnelRole.AEROSPACE_PILOT));
            case CONVENTIONAL_AIRCRAFT_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isConventionalAircraftPilot() : person.hasRole(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT));
            case PROTOMECH_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isProtoMechPilot() : person.hasRole(PersonnelRole.PROTOMECH_PILOT));
            case BATTLE_ARMOUR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isBattleArmour() : person.hasRole(PersonnelRole.BATTLE_ARMOUR));
            case SOLDIER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isSoldier() : person.hasRole(PersonnelRole.SOLDIER));
            case VESSEL_CREWMEMBER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselCrewmember()
                        : (person.getPrimaryRole().isVesselCrewmember() || person.getSecondaryRole().isVesselCrewmember()));
            case VESSEL_PILOT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselPilot() : person.hasRole(PersonnelRole.VESSEL_PILOT));
            case VESSEL_CREW:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselCrew() : person.hasRole(PersonnelRole.VESSEL_CREW));
            case VESSEL_GUNNER:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselGunner() : person.hasRole(PersonnelRole.VESSEL_GUNNER));
            case VESSEL_NAVIGATOR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselNavigator() : person.hasRole(PersonnelRole.VESSEL_NAVIGATOR));
            case TECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isTech() : person.isTech());
            case MECH_TECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMechTech() : person.hasRole(PersonnelRole.MECH_TECH));
            case MECHANIC:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMechanic() : person.hasRole(PersonnelRole.MECHANIC));
            case AERO_TECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAeroTech() : person.hasRole(PersonnelRole.AERO_TECH));
            case BA_TECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isBATech() : person.hasRole(PersonnelRole.BA_TECH));
            case ASTECH:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAstech() : person.hasRole(PersonnelRole.ASTECH));
            case MEDICAL:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMedicalStaff()
                        : (person.getPrimaryRole().isMedicalStaff() || person.getSecondaryRole().isMedicalStaff()));
            case DOCTOR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isDoctor() : person.hasRole(PersonnelRole.DOCTOR));
            case MEDIC:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMedic() : person.hasRole(PersonnelRole.MEDIC));
            case ADMINISTRATOR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministrator() : person.isAdministrator());
            case ADMINISTRATOR_COMMAND:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministratorCommand() : person.hasRole(PersonnelRole.ADMINISTRATOR_COMMAND));
            case ADMINISTRATOR_LOGISTICS:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministratorLogistics() : person.hasRole(PersonnelRole.ADMINISTRATOR_LOGISTICS));
            case ADMINISTRATOR_TRANSPORT:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministratorTransport() : person.hasRole(PersonnelRole.ADMINISTRATOR_TRANSPORT));
            case ADMINISTRATOR_HR:
                return active && (MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministratorHR() : person.hasRole(PersonnelRole.ADMINISTRATOR_HR));
            case DEPENDENT:
                return active && person.getPrimaryRole().isDependent();
            case FOUNDER:
                return person.isFounder();
            case PRISONER:
                return person.getPrisonerStatus().isPrisoner() || person.getPrisonerStatus().isBondsman();
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
