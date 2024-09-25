/*
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The PersonnelRole enum represents various roles a person can have.
 * Each role is associated with a name, an optional clan name, and a mnemonic key event.
 * These roles can be used to classify personnel.
 */
public enum PersonnelRole {
    // region Enum Declarations
    /**
     * Individual roles with corresponding name texts and mnemonics.
     */
    MEKWARRIOR("PersonnelRole.MEKWARRIOR.text", KeyEvent.VK_M),
    LAM_PILOT("PersonnelRole.LAM_PILOT.text", KeyEvent.VK_UNDEFINED),
    GROUND_VEHICLE_DRIVER("PersonnelRole.GROUND_VEHICLE_DRIVER.text", KeyEvent.VK_V),
    NAVAL_VEHICLE_DRIVER("PersonnelRole.NAVAL_VEHICLE_DRIVER.text", KeyEvent.VK_N),
    VTOL_PILOT("PersonnelRole.VTOL_PILOT.text", KeyEvent.VK_UNDEFINED),
    VEHICLE_GUNNER("PersonnelRole.VEHICLE_GUNNER.text", KeyEvent.VK_G),
    VEHICLE_CREW("PersonnelRole.VEHICLE_CREW.text", KeyEvent.VK_UNDEFINED),
    AEROSPACE_PILOT("PersonnelRole.AEROSPACE_PILOT.text", KeyEvent.VK_A),
    CONVENTIONAL_AIRCRAFT_PILOT("PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT.text", KeyEvent.VK_C),
    PROTOMEK_PILOT("PersonnelRole.PROTOMEK_PILOT.text", KeyEvent.VK_P),
    BATTLE_ARMOUR("PersonnelRole.BATTLE_ARMOUR.text", "PersonnelRole.BATTLE_ARMOUR.clan.text", KeyEvent.VK_B),
    SOLDIER("PersonnelRole.SOLDIER.text", KeyEvent.VK_S),
    VESSEL_PILOT("PersonnelRole.VESSEL_PILOT.text", KeyEvent.VK_I),
    VESSEL_GUNNER("PersonnelRole.VESSEL_GUNNER.text", KeyEvent.VK_U),
    VESSEL_CREW("PersonnelRole.VESSEL_CREW.text", KeyEvent.VK_W),
    VESSEL_NAVIGATOR("PersonnelRole.VESSEL_NAVIGATOR.text", KeyEvent.VK_Y),
    MEK_TECH("PersonnelRole.MEK_TECH.text", KeyEvent.VK_T),
    MECHANIC("PersonnelRole.MECHANIC.text", KeyEvent.VK_E),
    AERO_TEK("PersonnelRole.AERO_TEK.text", KeyEvent.VK_O),
    BA_TECH("PersonnelRole.BA_TECH.text", KeyEvent.VK_UNDEFINED),
    ASTECH("PersonnelRole.ASTECH.text", KeyEvent.VK_UNDEFINED),
    DOCTOR("PersonnelRole.DOCTOR.text", KeyEvent.VK_D),
    MEDIC("PersonnelRole.MEDIC.text", KeyEvent.VK_UNDEFINED),
    ADMINISTRATOR_COMMAND("PersonnelRole.ADMINISTRATOR_COMMAND.text", KeyEvent.VK_UNDEFINED),
    ADMINISTRATOR_LOGISTICS("PersonnelRole.ADMINISTRATOR_LOGISTICS.text", KeyEvent.VK_L),
    ADMINISTRATOR_TRANSPORT("PersonnelRole.ADMINISTRATOR_TRANSPORT.text", KeyEvent.VK_R),
    ADMINISTRATOR_HR("PersonnelRole.ADMINISTRATOR_HR.text", KeyEvent.VK_H),
    DEPENDENT("PersonnelRole.DEPENDENT.text", KeyEvent.VK_UNDEFINED),
    NONE("PersonnelRole.NONE.text", KeyEvent.VK_UNDEFINED);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String clanName;
    private final int mnemonic; // Unused: J, K, Q, X, Z
    // endregion Variable Declarations

    // region Constructors
    /**
     * Constructs a new PersonnelRole with the given name and mnemonic.
     *
     * @param name     the name of the personnel role
     * @param mnemonic the mnemonic of the personnel role
     */
    PersonnelRole(final String name, final int mnemonic) {
        this(name, null, mnemonic);
    }

    /**
     * Main constructor that initializes the role with name, clanName, and mnemonic.
     * ClanName is optional and defaults to name if {@code null}.
     * @param name the name of the role.
     * @param clanName the clan name of the role can be {@code null}.
     * @param mnemonic the mnemonic associated with the role.
     */
    PersonnelRole(final String name, @Nullable final String clanName, final int mnemonic) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.clanName = (clanName == null) ? this.name : resources.getString(clanName);
        this.mnemonic = mnemonic;
    }
    // endregion Constructors

    // region Getters
    public String getName(final boolean isClan) {
        return isClan ? clanName : name;
    }

    public int getMnemonic() {
        return mnemonic;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    /**
     * @return {@code true} if the personnel has the Mek Warrior role, {@code false} otherwise.
     */
    public boolean isMekWarrior() {
        return this == MEKWARRIOR;
    }
    /**
     * @return {@code true} if the personnel has the LAM Pilot role, {@code false} otherwise.
     */
    public boolean isLAMPilot() {
        return this == LAM_PILOT;
    }
    /**
     * @return {@code true} if the personnel has the Ground Vehicle Driver role, {@code false}
     * otherwise.
     */
    public boolean isGroundVehicleDriver() {
        return this == GROUND_VEHICLE_DRIVER;
    }

    /**
     * @return {@code true} if the personnel has the Naval Vehicle Driver role, {@code false}
     * otherwise.
     */
    public boolean isNavalVehicleDriver() {
        return this == NAVAL_VEHICLE_DRIVER;
    }

    /**
     * @return {@code true} if the personnel has the VTOL Pilot role, {@code false} otherwise.
     */
    public boolean isVTOLPilot() {
        return this == VTOL_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the Vehicle Gunner role, {@code false} otherwise.
     */
    public boolean isVehicleGunner() {
        return this == VEHICLE_GUNNER;
    }

    /**
     * @return {@code true} if the personnel has the Vehicle Crew role, {@code false} otherwise.
     */
    public boolean isVehicleCrew() {
        return this == VEHICLE_CREW;
    }

    /**
     * @return {@code true} if the personnel has the Aerospace Pilot role, {@code false} otherwise.
     */
    public boolean isAerospacePilot() {
        return this == AEROSPACE_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the Conventional Aircraft Pilot role, {@code false}
     * otherwise.
     */
    public boolean isConventionalAircraftPilot() {
        return this == CONVENTIONAL_AIRCRAFT_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the ProtoMek Pilot role, {@code false} otherwise.
     */
    public boolean isProtoMekPilot() {
        return this == PROTOMEK_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the Battle Armor Pilot role, {@code false} otherwise.
     */
    public boolean isBattleArmour() {
        return this == BATTLE_ARMOUR;
    }

    /**
     * @return {@code true} if the personnel has the Soldier role, {@code false} otherwise.
     */
    public boolean isSoldier() {
        return this == SOLDIER;
    }

    /**
     * @return {@code true} if the personnel has the Vessel Pilot role, {@code false} otherwise.
     */
    public boolean isVesselPilot() {
        return this == VESSEL_PILOT;
    }

    /**
     * @return {@code true} if the personnel has the Vessel Gunner role, {@code false} otherwise.
     */
    public boolean isVesselGunner() {
        return this == VESSEL_GUNNER;
    }

    /**
     * @return {@code true} if the personnel has the Vessel Crew role, {@code false} otherwise.
     */
    public boolean isVesselCrew() {
        return this == VESSEL_CREW;
    }

    /**
     * @return {@code true} if the personnel has the Vessel Navigator role, {@code false} otherwise.
     */
    public boolean isVesselNavigator() {
        return this == VESSEL_NAVIGATOR;
    }

    /**
     * @return {@code true} if the personnel has the MekTech role, {@code false} otherwise.
     */
    public boolean isMekTech() {
        return this == MEK_TECH;
    }

    /**
     * @return {@code true} if the personnel has the Mechanic role, {@code false} otherwise.
     */
    public boolean isMechanic() {
        return this == MECHANIC;
    }

    /**
     * @return {@code true} if the personnel has the AeroTek role, {@code false} otherwise.
     */
    public boolean isAeroTek() {
        return this == AERO_TEK;
    }

    /**
     * @return {@code true} if the personnel has the Battle Armor Tech role, {@code false} otherwise.
     */
    public boolean isBATech() {
        return this == BA_TECH;
    }

    /**
     * @return {@code true} if the personnel has the Astech role, {@code false} otherwise.
     */
    public boolean isAstech() {
        return this == ASTECH;
    }

    /**
     * @return {@code true} if the personnel has the Doctor role, {@code false} otherwise.
     */
    public boolean isDoctor() {
        return this == DOCTOR;
    }

    /**
     * @return {@code true} if the personnel has the Medic role, {@code false} otherwise.
     */
    public boolean isMedic() {
        return this == MEDIC;
    }

    /**
     * @return {@code true} if the personnel has the Admin/Command role, {@code false} otherwise.
     */
    public boolean isAdministratorCommand() {
        return this == ADMINISTRATOR_COMMAND;
    }

    /**
     * @return {@code true} if the personnel has the Admin/Logistics role, {@code false} otherwise.
     */
    public boolean isAdministratorLogistics() {
        return this == ADMINISTRATOR_LOGISTICS;
    }

    /**
     * @return {@code true} if the personnel has the Admin/Transport role, {@code false} otherwise.
     */
    public boolean isAdministratorTransport() {
        return this == ADMINISTRATOR_TRANSPORT;
    }

    /**
     * @return {@code true} if the personnel has the Admin/HR role, {@code false} otherwise.
     */
    public boolean isAdministratorHR() {
        return this == ADMINISTRATOR_HR;
    }

    /**
     * @return {@code true} if the personnel has the Dependent role, {@code false} otherwise.
     */
    public boolean isDependent() {
        return this == DEPENDENT;
    }

    /**
     * @return {@code true} if the personnel has the None role, {@code false} otherwise.
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * @return {@code true} if the character has a combat role, {@code true} otherwise.
     */
    public boolean isCombat() {
        return switch (this) {
            case MEKWARRIOR, LAM_PILOT, GROUND_VEHICLE_DRIVER, NAVAL_VEHICLE_DRIVER, VTOL_PILOT,
                 VEHICLE_GUNNER, VEHICLE_CREW, AEROSPACE_PILOT, CONVENTIONAL_AIRCRAFT_PILOT,
                 PROTOMEK_PILOT, BATTLE_ARMOUR, SOLDIER, VESSEL_PILOT, VESSEL_GUNNER, VESSEL_CREW,
                 VESSEL_NAVIGATOR -> true;
            default -> false;
        };
    }

    /**
     * @return {@code true} if the character is a MekWarrior or a LAM Pilot, {@code false} otherwise.
     */
    public boolean isMekWarriorGrouping() {
        return isMekWarrior() || isLAMPilot();
    }

    /**
     * @return {@code true} if the character is an Aerospace Pilot or a LAM Pilot, {@code false}
     * otherwise.
     */
    public boolean isAerospaceGrouping() {
        return isLAMPilot() || isAerospacePilot();
    }

    /**
     * Deprecated in favor of {@code isConventionalAircraftPilot()}
     */
    @Deprecated
    public boolean isConventionalAirGrouping() {
        return isConventionalAircraftPilot();
    }

    /**
     * @return {@code true} if the character is assigned to the Ground Vehicle Driver, Vehicle Gunner,
     * or the Vehicle Crew role, {@code false} otherwise.
     */
    public boolean isGroundVehicleCrew() {
        return isGroundVehicleDriver() || isVehicleGunner() || isVehicleCrew();
    }

    /**
     * @return {@code true} if the character is assigned to the Naval Vehicle Driver, Vehicle Gunner,
     * or the Vehicle Crew role, {@code false} otherwise.
     */
    public boolean isNavalVehicleCrew() {
        return isNavalVehicleDriver() || isVehicleGunner() || isVehicleCrew();
    }

    /**
     * @return {@code true} if the character is assigned to the VTOL Pilot, Vehicle Gunner, or the
     * Vehicle Crew role, {@code false} otherwise.
     */
    public boolean isVTOLCrew() {
        return isVTOLPilot() || isVehicleGunner() || isVehicleCrew();
    }

    /**
     * @return {@code true} if the character is assigned to the Ground Vehicle Crew,
     * Naval Vehicle Crew, or the VTOL Pilot role, {@code false} otherwise.
     */
    public boolean isVehicleCrewMember() {
        return isGroundVehicleCrew() || isNavalVehicleDriver() || isVTOLPilot();
    }

    /**
     * @return {@code true} if the character is assigned to the Soldier, or the Battle Armor role,
     * {@code false} otherwise.
     */
    public boolean isSoldierOrBattleArmour() {
        return isSoldier() || isBattleArmour();
    }

    /**
     * @return {@code true} if the character is assigned to the Vessel Pilot, Vessel Gunner,
     * Vessel Crew, or the Vessel Navigator role, {@code false} otherwise.
     */
    public boolean isVesselCrewMember() {
        return isVesselPilot() || isVesselGunner() || isVesselCrew() || isVesselNavigator();
    }

    /**
     * @return {@code true} if the character is assigned to a support role, excluding civilian roles,
     * {@code false} otherwise.
     */
    public boolean isSupport() {
        return isSupport(false);
    }

    /**
     * @return {@code true} if the character is assigned to a support role, {@code false} otherwise.
     *
     * @param excludeCivilian whether to exclude civilian roles
     */
    public boolean isSupport(final boolean excludeCivilian) {
        return !isCombat() && (!excludeCivilian || !isCivilian());
    }

    /**
     * Checks whether a character is assigned to a technician role.
     * If checking secondary roles, {@code isTechSecondary} should be used.
     *
     * @return {@code true} if the character is assigned to a technician role, {@code false}
     * otherwise.
     */
    public boolean isTech() {
        return isMekTech() || isMechanic() || isAeroTek() || isBATech() || isVesselCrew();
    }

    /**
     * Checks whether a character is assigned to a technician role.
     * If checking primary roles, {@code isTech} should be used.
     *
     * @return {@code true} if the character is assigned to a technician role, {@code false}
     * otherwise.
     */
    public boolean isTechSecondary() {
        return isMekTech() || isMechanic() || isAeroTek() || isBATech();
    }

    /**
     * @return {@code true} if the character is assigned to a medical role, {@code false} otherwise.
     */
    public boolean isMedicalStaff() {
        return isDoctor() || isMedic();
    }

    /**
     * @return true if the person is an Astech or Medic, false otherwise.
     */
    public boolean isAssistant() {
        return isAstech() || isMedic();
    }

    /**
     * @return {@code true} if the character is assigned to an Administrative role, {@code false}
     * otherwise.
     */
    public boolean isAdministrator() {
        return isAdministratorCommand() || isAdministratorLogistics()
                || isAdministratorTransport() || isAdministratorHR();
    }

    /**
     * @return {@code true} if the character's assigned role is Dependent or None, {@code false}
     * otherwise.
     */
    public boolean isCivilian() {
        return isDependent() || isNone();
    }
    // endregion Boolean Comparison Methods

    // region Static Methods
    /**
     * @return a list of roles that can be included in the personnel market
     */
    public static List<PersonnelRole> getMilitaryRoles() {
        return Stream.of(values())
                .filter(personnelRole -> !personnelRole.isCivilian())
                .collect(Collectors.toList());
    }

    /**
     * @return a list of roles that are potential primary roles. Currently, this is
     *         all bar NONE
     */
    public static List<PersonnelRole> getPrimaryRoles() {
        return Stream.of(values())
                .filter(role -> !role.isNone())
                .collect(Collectors.toList());
    }

    /**
     * @return a list of roles that are considered to be vessel (as in spacecraft)
     *         crew members
     */
    public static List<PersonnelRole> getVesselRoles() {
        return Stream.of(values())
                .filter(PersonnelRole::isVesselCrewMember)
                .collect(Collectors.toList());
    }

    /**
     * @return a list of roles that are considered to be techs
     */
    public static List<PersonnelRole> getTechRoles() {
        return Stream.of(values())
                .filter(PersonnelRole::isTech)
                .collect(Collectors.toList());
    }

    /**
     * @return a list of all roles that are considered to be administrators
     */
    public static List<PersonnelRole> getAdministratorRoles() {
        return Stream.of(values())
                .filter(PersonnelRole::isAdministrator)
                .collect(Collectors.toList());
    }

    /**
     * @return the number of civilian roles
     */
    public static int getCivilianCount() {
        return Math.toIntExact(Stream.of(values())
                .filter(PersonnelRole::isCivilian)
                .count());
    }
    // endregion Static Methods

    /**
     * Parses a string representation of a {@link PersonnelRole} and returns the corresponding enum
     * value.
     *
     * @param personnelRole the string representation of the {@link PersonnelRole}
     * @return the corresponding {@link PersonnelRole} enum value, or {@code NONE} if parsing fails
     */
    // region File I/O
    public static PersonnelRole parseFromString(final String personnelRole) {
        try {
            return valueOf(personnelRole);
        } catch (Exception ignored) {}

        // Magic Number Save Format
        try {
            switch (Integer.parseInt(personnelRole)) {
                case 0:
                    return NONE;
                case 1:
                    return MEKWARRIOR;
                case 2:
                    return AEROSPACE_PILOT;
                case 3:
                    return GROUND_VEHICLE_DRIVER;
                case 4:
                    return NAVAL_VEHICLE_DRIVER;
                case 5:
                    return VTOL_PILOT;
                case 6:
                    return VEHICLE_GUNNER;
                case 7:
                    return BATTLE_ARMOUR;
                case 8:
                    return SOLDIER;
                case 9:
                    return PROTOMEK_PILOT;
                case 10:
                    return CONVENTIONAL_AIRCRAFT_PILOT;
                case 11:
                    return VESSEL_PILOT;
                case 12:
                    return VESSEL_CREW;
                case 13:
                    return VESSEL_GUNNER;
                case 14:
                    return VESSEL_NAVIGATOR;
                case 15:
                    return MEK_TECH;
                case 16:
                    return MECHANIC;
                case 17:
                    return AERO_TEK;
                case 18:
                    return BA_TECH;
                case 19:
                    return ASTECH;
                case 20:
                    return DOCTOR;
                case 21:
                    return MEDIC;
                case 22:
                    return ADMINISTRATOR_COMMAND;
                case 23:
                    return ADMINISTRATOR_LOGISTICS;
                case 24:
                    return ADMINISTRATOR_TRANSPORT;
                case 25:
                    return ADMINISTRATOR_HR;
                case 26:
                    return LAM_PILOT;
                case 27:
                    return VEHICLE_CREW;
                default:
                    break;
            }
        } catch (Exception ignored) {}

        // <50.1 compatibility
        switch (personnelRole) {
            case "MECHWARRIOR" -> {
                return MEKWARRIOR;
            }
            case "PROTOMECH_PILOT" -> {
                return PROTOMEK_PILOT;
            }
            case "MECH_TECH" -> {
                return MEK_TECH;
            }
            case "AERO_TECH" -> {
                return AERO_TEK;
            }
            default -> {}
        }

        // Error report, if parsing fails.
        // Ignore IDEA's suggestion of concatenating the error log, as this functionality doesn't
        // exist within MMLogger
        MMLogger.create(PersonnelRole.class)
                .error("Unable to parse " + personnelRole + " into a PersonnelRole. Returning NONE.");
        return NONE;
    }
    // endregion File I/O

    /**
     * This method is not recommended to be used in MekHQ, but is provided for non-specified
     * utilization
     *
     * @return the base name of this role, without applying any overrides
     */
    @Override
    public String toString() {
        return name;
    }
}
