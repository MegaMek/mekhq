/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum PersonnelFilter {
    //region Enum Declarations
    //region Standard Personnel Filters
    ALL("PersonnelFilter.ALL.text", "PersonnelFilter.ALL.toolTipText"),
    ACTIVE("PersonnelFilter.ACTIVE.text", "PersonnelFilter.ACTIVE.toolTipText"),
    COMBAT("PersonnelFilter.COMBAT.text", "PersonnelFilter.COMBAT.toolTipText"),
    SUPPORT("PersonnelFilter.SUPPORT.text", "PersonnelFilter.SUPPORT.toolTipText"),
    MEKWARRIORS("PersonnelFilter.MEKWARRIORS.text", "PersonnelFilter.MEKWARRIORS.toolTipText", true, false),
    MEKWARRIOR("PersonnelFilter.MEKWARRIOR.text", "PersonnelFilter.MEKWARRIOR.toolTipText", false, true),
    LAM_PILOT("PersonnelFilter.LAM_PILOT.text", "PersonnelFilter.LAM_PILOT.toolTipText", false, true),
    VEHICLE_CREWMEMBER("PersonnelFilter.VEHICLE_CREWMEMBER.text", "PersonnelFilter.VEHICLE_CREWMEMBER.toolTipText", true, false),
    GROUND_VEHICLE_DRIVER("PersonnelFilter.GROUND_VEHICLE_DRIVER.text", "PersonnelFilter.GROUND_VEHICLE_DRIVER.toolTipText", false, true),
    NAVAL_VEHICLE_DRIVER("PersonnelFilter.NAVAL_VEHICLE_DRIVER.text", "PersonnelFilter.NAVAL_VEHICLE_DRIVER.toolTipText", false, true),
    VTOL_PILOT("PersonnelFilter.VTOL_PILOT.text", "PersonnelFilter.VTOL_PILOT.toolTipText", false, true),
    VEHICLE_GUNNER("PersonnelFilter.VEHICLE_GUNNER.text", "PersonnelFilter.VEHICLE_GUNNER.toolTipText", false, true),
    VEHICLE_CREW("PersonnelFilter.VEHICLE_CREW.text", "PersonnelFilter.VEHICLE_CREW.toolTipText", false, true),
    AEROSPACE_PILOT("PersonnelFilter.AEROSPACE_PILOT.text", "PersonnelFilter.AEROSPACE_PILOT.toolTipText"),
    CONVENTIONAL_AIRCRAFT_PILOT("PersonnelFilter.CONVENTIONAL_AIRCRAFT_PILOT.text", "PersonnelFilter.CONVENTIONAL_AIRCRAFT_PILOT.toolTipText"),
    PROTOMEK_PILOT("PersonnelFilter.PROTOMEK_PILOT.text", "PersonnelFilter.PROTOMEK_PILOT.toolTipText"),
    BATTLE_ARMOUR("PersonnelFilter.BATTLE_ARMOUR.text", "PersonnelFilter.BATTLE_ARMOUR.toolTipText"),
    SOLDIER("PersonnelFilter.SOLDIER.text", "PersonnelFilter.SOLDIER.toolTipText"),
    VESSEL_CREWMEMBER("PersonnelFilter.VESSEL_CREWMEMBER.text", "PersonnelFilter.VESSEL_CREWMEMBER.toolTipText", true, false),
    VESSEL_PILOT("PersonnelFilter.VESSEL_PILOT.text", "PersonnelFilter.VESSEL_PILOT.toolTipText", false, true),
    VESSEL_GUNNER("PersonnelFilter.VESSEL_GUNNER.text", "PersonnelFilter.VESSEL_GUNNER.toolTipText", false, true),
    VESSEL_CREW("PersonnelFilter.VESSEL_CREW.text", "PersonnelFilter.VESSEL_CREW.toolTipText", false, true),
    VESSEL_NAVIGATOR("PersonnelFilter.VESSEL_NAVIGATOR.text", "PersonnelFilter.VESSEL_NAVIGATOR.toolTipText", false, true),
    TECH("PersonnelFilter.TECH.text", "PersonnelFilter.TECH.toolTipText", true, false),
    MEK_TECH("PersonnelFilter.MEK_TECH.text", "PersonnelFilter.MEK_TECH.toolTipText", false, true),
    MECHANIC("PersonnelFilter.MECHANIC.text", "PersonnelFilter.MECHANIC.toolTipText", false, true),
    AERO_TECH("PersonnelFilter.AERO_TECH.text", "PersonnelFilter.AERO_TECH.toolTipText", false, true),
    BA_TECH("PersonnelFilter.BA_TECH.text", "PersonnelFilter.BA_TECH.toolTipText", false, true),
    ASTECH("PersonnelFilter.ASTECH.text", "PersonnelFilter.ASTECH.toolTipText", false, true),
    MEDICAL("PersonnelFilter.MEDICAL.text", "PersonnelFilter.MEDICAL.toolTipText", true, false),
    DOCTOR("PersonnelFilter.DOCTOR.text", "PersonnelFilter.DOCTOR.toolTipText", false, true),
    MEDIC("PersonnelFilter.MEDIC.text", "PersonnelFilter.MEDIC.toolTipText", false, true),
    ADMINISTRATOR("PersonnelFilter.ADMINISTRATOR.text", "PersonnelFilter.ADMINISTRATOR.toolTipText", true, false),
    ADMINISTRATOR_COMMAND("PersonnelFilter.ADMINISTRATOR_COMMAND.text", "PersonnelFilter.ADMINISTRATOR_COMMAND.toolTipText", false, true),
    ADMINISTRATOR_LOGISTICS("PersonnelFilter.ADMINISTRATOR_LOGISTICS.text", "PersonnelFilter.ADMINISTRATOR_LOGISTICS.toolTipText", false, true),
    ADMINISTRATOR_TRANSPORT("PersonnelFilter.ADMINISTRATOR_TRANSPORT.text", "PersonnelFilter.ADMINISTRATOR_TRANSPORT.toolTipText", false, true),
    ADMINISTRATOR_HR("PersonnelFilter.ADMINISTRATOR_HR.text", "PersonnelFilter.ADMINISTRATOR_HR.toolTipText", false, true),
    DEPENDENT("PersonnelFilter.DEPENDENT.text", "PersonnelFilter.DEPENDENT.toolTipText"),
    //endregion Standard Personnel Filters

    //region Expanded Personnel Tab Filters
    FOUNDER("PersonnelFilter.FOUNDER.text", "PersonnelFilter.FOUNDER.toolTipText", false, false),
    KIDS("PersonnelFilter.KIDS.text", "PersonnelFilter.KIDS.toolTipText"),
    PRISONER("PersonnelFilter.PRISONER.text", "PersonnelFilter.PRISONER.toolTipText", false, false),
    INACTIVE("PersonnelFilter.INACTIVE.text", "PersonnelFilter.INACTIVE.toolTipText", false, false),
    ON_LEAVE("PersonnelFilter.ON_LEAVE.text", "PersonnelFilter.ON_LEAVE.toolTipText", false, false),
    MIA("PersonnelFilter.MIA.text", "PersonnelFilter.MIA.toolTipText", false, false),
    RETIRED("PersonnelFilter.RETIRED.text", "PersonnelFilter.RETIRED.toolTipText", false, false),
    RESIGNED("PersonnelFilter.RESIGNED.text", "PersonnelFilter.RESIGNED.toolTipText", false, false),
    AWOL("PersonnelFilter.AWOL.text", "PersonnelFilter.AWOL.toolTipText", false, false),
    DESERTED("PersonnelFilter.DESERTED.text", "PersonnelFilter.DESERTED.toolTipText", false, false),
    STUDENT("PersonnelFilter.STUDENT.text", "PersonnelFilter.STUDENT.toolTipText", false, false),
    MISSING("PersonnelFilter.MISSING.text", "PersonnelFilter.MISSING.toolTipText", false, false),
    KIA("PersonnelFilter.KIA.text", "PersonnelFilter.KIA.toolTipText", false, false),
    DEAD("PersonnelFilter.DEAD.text", "PersonnelFilter.DEAD.toolTipText", false, false);
    //endregion Expanded Personnel Tab Filters
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final boolean baseline;
    private final boolean standard;
    private final boolean individualRole;
    //endregion Variable Declarations

    //region Constructors
    PersonnelFilter(final String name, final String toolTipText) {
        this(name, toolTipText, true, true, true);
    }

    PersonnelFilter(final String name, final String toolTipText, final boolean standard,
                    final boolean individualRole) {
        this(name, toolTipText, false, standard, individualRole);
    }

    PersonnelFilter(final String name, final String toolTipText, final boolean baseline,
                    final boolean standard, final boolean individualRole) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.baseline = baseline;
        this.standard = standard;
        this.individualRole = individualRole;
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }

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

    //region Boolean Comparison Methods
    public boolean isAll() {
        return this == ALL;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isCombat() {
        return this == COMBAT;
    }

    public boolean isSupport() {
        return this == SUPPORT;
    }

    public boolean isMekWarriors() {
        return this == MEKWARRIORS;
    }

    public boolean isMekWarrior() {
        return this == MEKWARRIOR;
    }

    public boolean isLAMPilot() {
        return this == LAM_PILOT;
    }

    public boolean isVehicleCrewmember() {
        return this == VEHICLE_CREWMEMBER;
    }

    public boolean isGroundVehicleDriver() {
        return this == GROUND_VEHICLE_DRIVER;
    }

    public boolean isNavalVehicleDriver() {
        return this == NAVAL_VEHICLE_DRIVER;
    }

    public boolean isVTOLPilot() {
        return this == VTOL_PILOT;
    }

    public boolean isVehicleGunner() {
        return this == VEHICLE_GUNNER;
    }

    public boolean isVehicleCrew() {
        return this == VEHICLE_CREW;
    }

    public boolean isAerospacePilot() {
        return this == AEROSPACE_PILOT;
    }

    public boolean isConventionalAircraftPilot() {
        return this == CONVENTIONAL_AIRCRAFT_PILOT;
    }

    public boolean isProtoMekPilot() {
        return this == PROTOMEK_PILOT;
    }

    public boolean isBattleArmor() {
        return this == BATTLE_ARMOUR;
    }

    public boolean isSoldier() {
        return this == SOLDIER;
    }

    public boolean isVesselCrewmember() {
        return this == VESSEL_CREWMEMBER;
    }

    public boolean isVesselPilot() {
        return this == VESSEL_PILOT;
    }

    public boolean isVesselGunner() {
        return this == VESSEL_GUNNER;
    }

    public boolean isVesselCrew() {
        return this == VESSEL_CREW;
    }

    public boolean isVesselNavigator() {
        return this == VESSEL_NAVIGATOR;
    }

    public boolean isTech() {
        return this == TECH;
    }

    public boolean isMekTech() {
        return this == MEK_TECH;
    }

    public boolean isMechanic() {
        return this == MECHANIC;
    }

    public boolean isAeroTech() {
        return this == AERO_TECH;
    }

    public boolean isBATech() {
        return this == BA_TECH;
    }

    public boolean isAstech() {
        return this == ASTECH;
    }

    public boolean isMedical() {
        return this == MEDICAL;
    }

    public boolean isDoctor() {
        return this == DOCTOR;
    }

    public boolean isMedic() {
        return this == MEDIC;
    }

    public boolean isAdministrator() {
        return this == ADMINISTRATOR;
    }

    public boolean isAdministratorCommand() {
        return this == ADMINISTRATOR_COMMAND;
    }

    public boolean isAdministratorLogistics() {
        return this == ADMINISTRATOR_LOGISTICS;
    }

    public boolean isAdministratorTransport() {
        return this == ADMINISTRATOR_TRANSPORT;
    }

    public boolean isAdministratorHR() {
        return this == ADMINISTRATOR_HR;
    }

    public boolean isDependent() {
        return this == DEPENDENT;
    }

    public boolean isFounder() {
        return this == FOUNDER;
    }

    public boolean isPrisoner() {
        return this == PRISONER;
    }

    public boolean isInactive() {
        return this == INACTIVE;
    }

    public boolean isMIA() {
        return this == MIA;
    }

    public boolean isRetired() {
        return this == RETIRED;
    }

    public boolean isDeserted() {
        return this == DESERTED;
    }

    public boolean isStudent() {
        return this == STUDENT;
    }

    public boolean isMissing() {
        return this == MISSING;
    }

    public boolean isKIA() {
        return this == KIA;
    }

    public boolean isDead() {
        return this == DEAD;
    }
    //endregion Boolean Comparison Methods

    public static List<PersonnelFilter> getStandardPersonnelFilters() {
        return Stream.of(values()).filter(filter -> filter.isBaseline() || filter.isStandard())
                .collect(Collectors.toList());
    }

    public static List<PersonnelFilter> getExpandedPersonnelFilters() {
        return Stream.of(values()).filter(filter -> filter.isBaseline() || !filter.isIndividualRole())
                .collect(Collectors.toList());
    }

    public static List<PersonnelFilter> getIndividualRolesStandardPersonnelFilters() {
        return Stream.of(values()).filter(filter -> filter.isBaseline() || filter.isIndividualRole())
                .collect(Collectors.toList());
    }

    public static List<PersonnelFilter> getIndividualRolesExpandedPersonnelFilters() {
        return Stream.of(values())
                .filter(filter -> filter.isBaseline() || !filter.isStandard() || filter.isIndividualRole())
                .collect(Collectors.toList());
    }

    public static List<PersonnelFilter> getAllStandardFilters() {
        return Stream.of(values())
                .filter(filter -> filter.isBaseline() || filter.isStandard() || filter.isIndividualRole())
                .collect(Collectors.toList());
    }

    public static List<PersonnelFilter> getAllIndividualRoleFilters() {
        return Arrays.asList(values());
    }

    public boolean getFilteredInformation(final Person person, LocalDate currentDate) {
        final boolean active = person.getStatus().isActive() && !person.getPrisonerStatus().isCurrentPrisoner();
        final boolean dead = person.getStatus().isDead();

        switch (this) {
            case ALL:
                return true;
            case ACTIVE:
                return active;
            case COMBAT:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isCombat() : person.hasCombatRole());
            case SUPPORT:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? !person.getPrimaryRole().isCombat() : person.hasSupportRole(true));
            case MEKWARRIORS:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMekWarriorGrouping()
                        : (person.getPrimaryRole().isMekWarriorGrouping() || person.getSecondaryRole().isMekWarriorGrouping()));
            case MEKWARRIOR:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMekWarrior() : person.hasRole(PersonnelRole.MEKWARRIOR));
            case LAM_PILOT:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isLAMPilot() : person.hasRole(PersonnelRole.LAM_PILOT));
            case VEHICLE_CREWMEMBER:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVehicleCrewmember()
                        : (person.getPrimaryRole().isVehicleCrewmember() || person.getSecondaryRole().isVehicleCrewmember()));
            case GROUND_VEHICLE_DRIVER:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isGroundVehicleDriver() : person.hasRole(PersonnelRole.GROUND_VEHICLE_DRIVER));
            case NAVAL_VEHICLE_DRIVER:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isNavalVehicleDriver() : person.hasRole(PersonnelRole.NAVAL_VEHICLE_DRIVER));
            case VEHICLE_GUNNER:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVehicleGunner() : person.hasRole(PersonnelRole.VEHICLE_GUNNER));
            case VEHICLE_CREW:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVehicleCrew() : person.hasRole(PersonnelRole.VEHICLE_CREW));
            case VTOL_PILOT:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVTOLPilot() : person.hasRole(PersonnelRole.VTOL_PILOT));
            case AEROSPACE_PILOT:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAerospacePilot() : person.hasRole(PersonnelRole.AEROSPACE_PILOT));
            case CONVENTIONAL_AIRCRAFT_PILOT:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isConventionalAircraftPilot() : person.hasRole(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT));
            case PROTOMEK_PILOT:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isProtoMekPilot() : person.hasRole(PersonnelRole.PROTOMEK_PILOT));
            case BATTLE_ARMOUR:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isBattleArmour() : person.hasRole(PersonnelRole.BATTLE_ARMOUR));
            case SOLDIER:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isSoldier() : person.hasRole(PersonnelRole.SOLDIER));
            case VESSEL_CREWMEMBER:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselCrewmember()
                        : (person.getPrimaryRole().isVesselCrewmember() || person.getSecondaryRole().isVesselCrewmember()));
            case VESSEL_PILOT:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselPilot() : person.hasRole(PersonnelRole.VESSEL_PILOT));
            case VESSEL_CREW:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselCrew() : person.hasRole(PersonnelRole.VESSEL_CREW));
            case VESSEL_GUNNER:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselGunner() : person.hasRole(PersonnelRole.VESSEL_GUNNER));
            case VESSEL_NAVIGATOR:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isVesselNavigator() : person.hasRole(PersonnelRole.VESSEL_NAVIGATOR));
            case TECH:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isTech() : person.isTech());
            case MEK_TECH:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMekTech() : person.hasRole(PersonnelRole.MEK_TECH));
            case MECHANIC:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMechanic() : person.hasRole(PersonnelRole.MECHANIC));
            case AERO_TECH:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAeroTech() : person.hasRole(PersonnelRole.AERO_TECH));
            case BA_TECH:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isBATech() : person.hasRole(PersonnelRole.BA_TECH));
            case ASTECH:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAstech() : person.hasRole(PersonnelRole.ASTECH));
            case MEDICAL:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMedicalStaff()
                        : (person.getPrimaryRole().isMedicalStaff() || person.getSecondaryRole().isMedicalStaff()));
            case DOCTOR:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isDoctor() : person.hasRole(PersonnelRole.DOCTOR));
            case MEDIC:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isMedic() : person.hasRole(PersonnelRole.MEDIC));
            case ADMINISTRATOR:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministrator() : person.isAdministrator());
            case ADMINISTRATOR_COMMAND:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministratorCommand() : person.hasRole(PersonnelRole.ADMINISTRATOR_COMMAND));
            case ADMINISTRATOR_LOGISTICS:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministratorLogistics() : person.hasRole(PersonnelRole.ADMINISTRATOR_LOGISTICS));
            case ADMINISTRATOR_TRANSPORT:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministratorTransport() : person.hasRole(PersonnelRole.ADMINISTRATOR_TRANSPORT));
            case ADMINISTRATOR_HR:
                return active && (MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole()
                        ? person.getPrimaryRole().isAdministratorHR() : person.hasRole(PersonnelRole.ADMINISTRATOR_HR));
            case DEPENDENT:
                return ((!dead) && (active && person.getPrimaryRole().isDependent()));
            case FOUNDER:
                return ((!dead) && (person.isFounder()));
            case KIDS:
                return ((!dead) && (!person.getStatus().isLeft()) && (person.isChild(currentDate)));
            case PRISONER:
                return ((!dead) && ((person.getPrisonerStatus().isCurrentPrisoner()) || (person.getPrisonerStatus().isBondsman())));
            case INACTIVE:
                return ((!dead) && (!person.getStatus().isActive()));
            case ON_LEAVE:
                return person.getStatus().isOnLeave();
            case MIA:
                return person.getStatus().isMIA() || person.getStatus().isPoW();
            case RETIRED:
                return person.getStatus().isRetired();
            case RESIGNED:
                return ((person.getStatus().isResigned()) || (person.getStatus().isLeft()));
            case AWOL:
                return person.getStatus().isAwol();
            case DESERTED:
                return person.getStatus().isDeserted();
            case STUDENT:
                return person.getStatus().isStudent();
            case MISSING:
                return person.getStatus().isMissing();
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
