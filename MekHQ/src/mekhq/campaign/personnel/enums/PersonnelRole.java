/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;

import mekhq.MekHQ;

public enum PersonnelRole {
    // region Enum Declarations
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
    PersonnelRole(final String name, final int mnemonic) {
        this(name, null, mnemonic);
    }

    PersonnelRole(final String name, final String clanName, final int mnemonic) {
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
    public boolean isMekWarrior() {
        return this == MEKWARRIOR;
    }

    public boolean isLAMPilot() {
        return this == LAM_PILOT;
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

    public boolean isBattleArmour() {
        return this == BATTLE_ARMOUR;
    }

    public boolean isSoldier() {
        return this == SOLDIER;
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

    public boolean isMekTech() {
        return this == MEK_TECH;
    }

    public boolean isMechanic() {
        return this == MECHANIC;
    }

    public boolean isAeroTek() {
        return this == AERO_TEK;
    }

    public boolean isBATech() {
        return this == BA_TECH;
    }

    public boolean isAstech() {
        return this == ASTECH;
    }

    public boolean isDoctor() {
        return this == DOCTOR;
    }

    public boolean isMedic() {
        return this == MEDIC;
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

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isCombat() {
        switch (this) {
            case MEKWARRIOR:
            case LAM_PILOT:
            case GROUND_VEHICLE_DRIVER:
            case NAVAL_VEHICLE_DRIVER:
            case VTOL_PILOT:
            case VEHICLE_GUNNER:
            case VEHICLE_CREW:
            case AEROSPACE_PILOT:
            case CONVENTIONAL_AIRCRAFT_PILOT:
            case PROTOMEK_PILOT:
            case BATTLE_ARMOUR:
            case SOLDIER:
            case VESSEL_PILOT:
            case VESSEL_GUNNER:
            case VESSEL_CREW:
            case VESSEL_NAVIGATOR:
                return true;
            case MEK_TECH:
            case MECHANIC:
            case AERO_TEK:
            case BA_TECH:
            case ASTECH:
            case DOCTOR:
            case MEDIC:
            case ADMINISTRATOR_COMMAND:
            case ADMINISTRATOR_LOGISTICS:
            case ADMINISTRATOR_TRANSPORT:
            case ADMINISTRATOR_HR:
            case DEPENDENT:
            case NONE:
            default:
                return false;
        }
    }

    public boolean isMekWarriorGrouping() {
        return isMekWarrior() || isLAMPilot();
    }

    public boolean isAerospaceGrouping() {
        return isLAMPilot() || isAerospacePilot();
    }

    public boolean isConventionalAirGrouping() {
        return isAerospaceGrouping() || isConventionalAircraftPilot();
    }

    public boolean isGroundVehicleCrew() {
        return isGroundVehicleDriver() || isVehicleGunner() || isVehicleCrew();
    }

    public boolean isNavalVehicleCrew() {
        return isNavalVehicleDriver() || isVehicleGunner() || isVehicleCrew();
    }

    public boolean isVTOLCrew() {
        return isVTOLPilot() || isVehicleGunner() || isVehicleCrew();
    }

    public boolean isVehicleCrewMember() {
        return isGroundVehicleCrew() || isNavalVehicleDriver() || isVTOLPilot();
    }

    public boolean isSoldierOrBattleArmour() {
        return isSoldier() || isBattleArmour();
    }

    public boolean isVesselCrewMember() {
        return isVesselPilot() || isVesselGunner() || isVesselCrew() || isVesselNavigator();
    }

    public boolean isSupport() {
        return isSupport(false);
    }

    public boolean isSupport(final boolean excludeCivilian) {
        return !isCombat() && (!excludeCivilian || !isCivilian());
    }

    public boolean isTech() {
        return isMekTech() || isMechanic() || isAeroTek() || isBATech() || isVesselCrew();
    }

    public boolean isTechSecondary() {
        return isMekTech() || isMechanic() || isAeroTek() || isBATech();
    }

    public boolean isMedicalStaff() {
        return isDoctor() || isMedic();
    }

    /**
     * @return true if the person is an Astech or Medic, false otherwise.
     */
    public boolean isAssistant() {
        return isAstech() || isMedic();
    }

    public boolean isAdministrator() {
        return isAdministratorCommand() || isAdministratorLogistics()
                || isAdministratorTransport() || isAdministratorHR();
    }

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

    // region File I/O
    public static PersonnelRole parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Magic Number Save Format
        try {
            switch (Integer.parseInt(text)) {
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
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Unable to parse " + text + " into a PersonnelRole. Returning NONE.");
        return NONE;
    }
    // endregion File I/O

    /**
     * This method is not recommend to be used in MekHQ, but is provided for
     * non-specified utilization
     *
     * @return the base name of this role, without applying any overrides
     */
    @Override
    public String toString() {
        return name;
    }
}
