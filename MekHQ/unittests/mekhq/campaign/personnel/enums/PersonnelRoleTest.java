/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.jupiter.api.Test;

import mekhq.MekHQ;

class PersonnelRoleTest {
    // region Variable Declarations
    private static final PersonnelRole[] roles = PersonnelRole.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    void testGetName() {
        assertEquals(resources.getString("PersonnelRole.MEKWARRIOR.text"),
                PersonnelRole.MEKWARRIOR.getName(false));
        assertEquals(resources.getString("PersonnelRole.MEKWARRIOR.text"),
                PersonnelRole.MEKWARRIOR.getName(true));
        assertEquals(resources.getString("PersonnelRole.BATTLE_ARMOUR.text"),
                PersonnelRole.BATTLE_ARMOUR.getName(false));
        assertEquals(resources.getString("PersonnelRole.BATTLE_ARMOUR.clan.text"),
                PersonnelRole.BATTLE_ARMOUR.getName(true));
        assertEquals(resources.getString("PersonnelRole.ADMINISTRATOR_LOGISTICS.text"),
                PersonnelRole.ADMINISTRATOR_LOGISTICS.getName(false));
    }

    @Test
    void testGetMnemonicEnsureUniqueness() {
        final Set<Integer> usedMnemonics = new HashSet<>();
        for (final PersonnelRole role : roles) {
            if (role.getMnemonic() == KeyEvent.VK_UNDEFINED) {
                // Allow duplicates if it isn't a mnemonic...
                continue;
            }
            assertFalse(usedMnemonics.contains(role.getMnemonic()),
                    String.format("%s: Using duplicate mnemonic of %d", role.name(), role.getMnemonic()));
            usedMnemonics.add(role.getMnemonic());
        }
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsMekWarrior() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.MEKWARRIOR) {
                assertTrue(personnelRole.isMekWarrior());
            } else {
                assertFalse(personnelRole.isMekWarrior());
            }
        }
    }

    @Test
    void testIsLAMPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.LAM_PILOT) {
                assertTrue(personnelRole.isLAMPilot());
            } else {
                assertFalse(personnelRole.isLAMPilot());
            }
        }
    }

    @Test
    void testIsGroundVehicleDriver() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.GROUND_VEHICLE_DRIVER) {
                assertTrue(personnelRole.isGroundVehicleDriver());
            } else {
                assertFalse(personnelRole.isGroundVehicleDriver());
            }
        }
    }

    @Test
    void testIsNavalVehicleDriver() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.NAVAL_VEHICLE_DRIVER) {
                assertTrue(personnelRole.isNavalVehicleDriver());
            } else {
                assertFalse(personnelRole.isNavalVehicleDriver());
            }
        }
    }

    @Test
    void testIsVTOLPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VTOL_PILOT) {
                assertTrue(personnelRole.isVTOLPilot());
            } else {
                assertFalse(personnelRole.isVTOLPilot());
            }
        }
    }

    @Test
    void testIsVehicleGunner() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VEHICLE_GUNNER) {
                assertTrue(personnelRole.isVehicleGunner());
            } else {
                assertFalse(personnelRole.isVehicleGunner());
            }
        }
    }

    @Test
    void testIsVehicleCrew() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VEHICLE_CREW) {
                assertTrue(personnelRole.isVehicleCrew());
            } else {
                assertFalse(personnelRole.isVehicleCrew());
            }
        }
    }

    @Test
    void testIsAerospacePilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.AEROSPACE_PILOT) {
                assertTrue(personnelRole.isAerospacePilot());
            } else {
                assertFalse(personnelRole.isAerospacePilot());
            }
        }
    }

    @Test
    void testIsConventionalAircraftPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT) {
                assertTrue(personnelRole.isConventionalAircraftPilot());
            } else {
                assertFalse(personnelRole.isConventionalAircraftPilot());
            }
        }
    }

    @Test
    void testIsProtoMekPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.PROTOMEK_PILOT) {
                assertTrue(personnelRole.isProtoMekPilot());
            } else {
                assertFalse(personnelRole.isProtoMekPilot());
            }
        }
    }

    @Test
    void testIsBattleArmour() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.BATTLE_ARMOUR) {
                assertTrue(personnelRole.isBattleArmour());
            } else {
                assertFalse(personnelRole.isBattleArmour());
            }
        }
    }

    @Test
    void testIsSoldier() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.SOLDIER) {
                assertTrue(personnelRole.isSoldier());
            } else {
                assertFalse(personnelRole.isSoldier());
            }
        }
    }

    @Test
    void testIsVesselPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VESSEL_PILOT) {
                assertTrue(personnelRole.isVesselPilot());
            } else {
                assertFalse(personnelRole.isVesselPilot());
            }
        }
    }

    @Test
    void testIsVesselGunner() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VESSEL_GUNNER) {
                assertTrue(personnelRole.isVesselGunner());
            } else {
                assertFalse(personnelRole.isVesselGunner());
            }
        }
    }

    @Test
    void testIsVesselCrew() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VESSEL_CREW) {
                assertTrue(personnelRole.isVesselCrew());
            } else {
                assertFalse(personnelRole.isVesselCrew());
            }
        }
    }

    @Test
    void testIsVesselNavigator() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VESSEL_NAVIGATOR) {
                assertTrue(personnelRole.isVesselNavigator());
            } else {
                assertFalse(personnelRole.isVesselNavigator());
            }
        }
    }

    @Test
    void testIsMekTech() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.MEK_TECH) {
                assertTrue(personnelRole.isMekTech());
            } else {
                assertFalse(personnelRole.isMekTech());
            }
        }
    }

    @Test
    void testIsMechanic() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.MECHANIC) {
                assertTrue(personnelRole.isMechanic());
            } else {
                assertFalse(personnelRole.isMechanic());
            }
        }
    }

    @Test
    void testIsAeroTek() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.AERO_TEK) {
                assertTrue(personnelRole.isAeroTek());
            } else {
                assertFalse(personnelRole.isAeroTek());
            }
        }
    }

    @Test
    void testIsBATech() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.BA_TECH) {
                assertTrue(personnelRole.isBATech());
            } else {
                assertFalse(personnelRole.isBATech());
            }
        }
    }

    @Test
    void testIsAstech() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ASTECH) {
                assertTrue(personnelRole.isAstech());
            } else {
                assertFalse(personnelRole.isAstech());
            }
        }
    }

    @Test
    void testIsDoctor() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.DOCTOR) {
                assertTrue(personnelRole.isDoctor());
            } else {
                assertFalse(personnelRole.isDoctor());
            }
        }
    }

    @Test
    void testIsMedic() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.MEDIC) {
                assertTrue(personnelRole.isMedic());
            } else {
                assertFalse(personnelRole.isMedic());
            }
        }
    }

    @Test
    void testIsAdministratorCommand() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ADMINISTRATOR_COMMAND) {
                assertTrue(personnelRole.isAdministratorCommand());
            } else {
                assertFalse(personnelRole.isAdministratorCommand());
            }
        }
    }

    @Test
    void testIsAdministratorLogistics() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ADMINISTRATOR_LOGISTICS) {
                assertTrue(personnelRole.isAdministratorLogistics());
            } else {
                assertFalse(personnelRole.isAdministratorLogistics());
            }
        }
    }

    @Test
    void testIsAdministratorTransport() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ADMINISTRATOR_TRANSPORT) {
                assertTrue(personnelRole.isAdministratorTransport());
            } else {
                assertFalse(personnelRole.isAdministratorTransport());
            }
        }
    }

    @Test
    void testIsAdministratorHR() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ADMINISTRATOR_HR) {
                assertTrue(personnelRole.isAdministratorHR());
            } else {
                assertFalse(personnelRole.isAdministratorHR());
            }
        }
    }

    @Test
    void testIsDependent() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.DEPENDENT) {
                assertTrue(personnelRole.isDependent());
            } else {
                assertFalse(personnelRole.isDependent());
            }
        }
    }

    @Test
    void testIsNone() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.NONE) {
                assertTrue(personnelRole.isNone());
            } else {
                assertFalse(personnelRole.isNone());
            }
        }
    }

    @Test
    void testIsCombat() {
        for (final PersonnelRole personnelRole : roles) {
            switch (personnelRole) {
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
                    assertTrue(personnelRole.isCombat());
                    break;
                default:
                    assertFalse(personnelRole.isCombat());
                    break;
            }
        }
    }

    @Test
    void testIsMekWarriorGrouping() {
        for (final PersonnelRole personnelRole : roles) {
            if ((personnelRole == PersonnelRole.MEKWARRIOR)
                    || (personnelRole == PersonnelRole.LAM_PILOT)) {
                assertTrue(personnelRole.isMekWarriorGrouping());
            } else {
                assertFalse(personnelRole.isMekWarriorGrouping());
            }
        }
    }

    @Test
    void testIsAerospaceGrouping() {
        for (final PersonnelRole personnelRole : roles) {
            if ((personnelRole == PersonnelRole.LAM_PILOT)
                    || (personnelRole == PersonnelRole.AEROSPACE_PILOT)) {
                assertTrue(personnelRole.isAerospaceGrouping());
            } else {
                assertFalse(personnelRole.isAerospaceGrouping());
            }
        }
    }

    @Test
    void testIsConventionalAirGrouping() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT) {
                assertTrue(personnelRole.isConventionalAirGrouping());
            } else {
                assertFalse(personnelRole.isConventionalAirGrouping());
            }
        }
    }

    @Test
    void testIsGroundVehicleCrew() {
        for (final PersonnelRole personnelRole : roles) {
            if ((personnelRole == PersonnelRole.GROUND_VEHICLE_DRIVER)
                    || (personnelRole == PersonnelRole.VEHICLE_GUNNER)
                    || (personnelRole == PersonnelRole.VEHICLE_CREW)) {
                assertTrue(personnelRole.isGroundVehicleCrew());
            } else {
                assertFalse(personnelRole.isGroundVehicleCrew());
            }
        }
    }

    @Test
    void testIsNavalVehicleCrew() {
        for (final PersonnelRole personnelRole : roles) {
            if ((personnelRole == PersonnelRole.NAVAL_VEHICLE_DRIVER)
                    || (personnelRole == PersonnelRole.VEHICLE_GUNNER)
                    || (personnelRole == PersonnelRole.VEHICLE_CREW)) {
                assertTrue(personnelRole.isNavalVehicleCrew());
            } else {
                assertFalse(personnelRole.isNavalVehicleCrew());
            }
        }
    }

    @Test
    void testIsVTOLCrew() {
        for (final PersonnelRole personnelRole : roles) {
            if ((personnelRole == PersonnelRole.VTOL_PILOT)
                    || (personnelRole == PersonnelRole.VEHICLE_GUNNER)
                    || (personnelRole == PersonnelRole.VEHICLE_CREW)) {
                assertTrue(personnelRole.isVTOLCrew());
            } else {
                assertFalse(personnelRole.isVTOLCrew());
            }
        }
    }

    @Test
    void testIsVehicleCrewMember() {
        for (final PersonnelRole personnelRole : roles) {
            switch (personnelRole) {
                case GROUND_VEHICLE_DRIVER:
                case NAVAL_VEHICLE_DRIVER:
                case VTOL_PILOT:
                case VEHICLE_GUNNER:
                case VEHICLE_CREW:
                    assertTrue(personnelRole.isVehicleCrewMember());
                    break;
                default:
                    assertFalse(personnelRole.isVehicleCrewMember());
                    break;
            }
        }
    }

    @Test
    void testIsSoldierOrBattleArmour() {
        for (final PersonnelRole personnelRole : roles) {
            if ((personnelRole == PersonnelRole.SOLDIER)
                    || (personnelRole == PersonnelRole.BATTLE_ARMOUR)) {
                assertTrue(personnelRole.isSoldierOrBattleArmour());
            } else {
                assertFalse(personnelRole.isSoldierOrBattleArmour());
            }
        }
    }

    @Test
    void testIsVesselCrewMember() {
        for (final PersonnelRole personnelRole : roles) {
            switch (personnelRole) {
                case VESSEL_PILOT:
                case VESSEL_GUNNER:
                case VESSEL_CREW:
                case VESSEL_NAVIGATOR:
                    assertTrue(personnelRole.isVesselCrewMember());
                    break;
                default:
                    assertFalse(personnelRole.isVesselCrewMember());
                    break;
            }
        }
    }

    @Test
    void testIsSupport() {
        assertFalse(PersonnelRole.MEKWARRIOR.isSupport());
        assertFalse(PersonnelRole.VESSEL_NAVIGATOR.isSupport());
        assertTrue(PersonnelRole.MEK_TECH.isSupport());
        assertTrue(PersonnelRole.ASTECH.isSupport());
        assertTrue(PersonnelRole.ADMINISTRATOR_COMMAND.isSupport());
        assertTrue(PersonnelRole.DEPENDENT.isSupport());
        assertTrue(PersonnelRole.NONE.isSupport());
        assertFalse(PersonnelRole.MEKWARRIOR.isSupport(true));
        assertFalse(PersonnelRole.VESSEL_NAVIGATOR.isSupport(true));
        assertTrue(PersonnelRole.MEK_TECH.isSupport(true));
        assertTrue(PersonnelRole.ASTECH.isSupport(true));
        assertTrue(PersonnelRole.ADMINISTRATOR_COMMAND.isSupport(true));
        assertFalse(PersonnelRole.DEPENDENT.isSupport(true));
        assertFalse(PersonnelRole.NONE.isSupport(true));
    }

    @Test
    void testIsTech() {
        for (final PersonnelRole personnelRole : roles) {
            switch (personnelRole) {
                case MEK_TECH:
                case MECHANIC:
                case AERO_TEK:
                case BA_TECH:
                case VESSEL_CREW:
                    assertTrue(personnelRole.isTech());
                    break;
                default:
                    assertFalse(personnelRole.isTech());
                    break;
            }
        }
    }

    @Test
    void testIsTechSecondary() {
        for (final PersonnelRole personnelRole : roles) {
            switch (personnelRole) {
                case MEK_TECH:
                case MECHANIC:
                case AERO_TEK:
                case BA_TECH:
                    assertTrue(personnelRole.isTechSecondary());
                    break;
                default:
                    assertFalse(personnelRole.isTechSecondary());
                    break;
            }
        }
    }

    @Test
    void testIsMedicalStaff() {
        for (final PersonnelRole personnelRole : roles) {
            if ((personnelRole == PersonnelRole.DOCTOR)
                    || (personnelRole == PersonnelRole.MEDIC)) {
                assertTrue(personnelRole.isMedicalStaff());
            } else {
                assertFalse(personnelRole.isMedicalStaff());
            }
        }
    }

    @Test
    void testIsAdministrator() {
        for (final PersonnelRole personnelRole : roles) {
            switch (personnelRole) {
                case ADMINISTRATOR_COMMAND:
                case ADMINISTRATOR_LOGISTICS:
                case ADMINISTRATOR_TRANSPORT:
                case ADMINISTRATOR_HR:
                    assertTrue(personnelRole.isAdministrator());
                    break;
                default:
                    assertFalse(personnelRole.isAdministrator());
                    break;
            }
        }
    }

    @Test
    void testIsCivilian() {
        for (final PersonnelRole personnelRole : roles) {
            if ((personnelRole == PersonnelRole.DEPENDENT)
                    || (personnelRole == PersonnelRole.NONE)) {
                assertTrue(personnelRole.isCivilian());
            } else {
                assertFalse(personnelRole.isCivilian());
            }
        }
    }
    // endregion Boolean Comparison Methods

    // region Static Methods
    @Test
    void testGetMilitaryRoles() {
        final List<PersonnelRole> militaryRoles = PersonnelRole.getMilitaryRoles();
        assertEquals(roles.length - 2, militaryRoles.size());
        assertFalse(militaryRoles.contains(PersonnelRole.DEPENDENT));
        assertFalse(militaryRoles.contains(PersonnelRole.NONE));
    }

    @Test
    void testGetPrimaryRoles() {
        // This should be all roles bar one, namely PersonnelRole.NONE
        final List<PersonnelRole> primaryRoles = PersonnelRole.getPrimaryRoles();
        assertEquals(roles.length - 1, primaryRoles.size());
        assertFalse(primaryRoles.contains(PersonnelRole.NONE));
    }

    @Test
    void testGetVesselRoles() {
        final List<PersonnelRole> expected = new ArrayList<>();
        expected.add(PersonnelRole.VESSEL_PILOT);
        expected.add(PersonnelRole.VESSEL_GUNNER);
        expected.add(PersonnelRole.VESSEL_CREW);
        expected.add(PersonnelRole.VESSEL_NAVIGATOR);
        assertEquals(expected, PersonnelRole.getVesselRoles());
    }

    @Test
    void testGetTechRoles() {
        final List<PersonnelRole> expected = new ArrayList<>();
        expected.add(PersonnelRole.VESSEL_CREW);
        expected.add(PersonnelRole.MEK_TECH);
        expected.add(PersonnelRole.MECHANIC);
        expected.add(PersonnelRole.AERO_TEK);
        expected.add(PersonnelRole.BA_TECH);
        assertEquals(expected, PersonnelRole.getTechRoles());
    }

    @Test
    void testGetAdministratorRoles() {
        final List<PersonnelRole> expected = new ArrayList<>();
        expected.add(PersonnelRole.ADMINISTRATOR_COMMAND);
        expected.add(PersonnelRole.ADMINISTRATOR_LOGISTICS);
        expected.add(PersonnelRole.ADMINISTRATOR_TRANSPORT);
        expected.add(PersonnelRole.ADMINISTRATOR_HR);
        assertEquals(expected, PersonnelRole.getAdministratorRoles());
    }

    @Test
    void testGetCivilianCount() {
        // Civilian Roles: Dependent and None
        assertEquals(2, PersonnelRole.getCivilianCount());
    }
    // endregion Static Methods

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(PersonnelRole.NONE, PersonnelRole.parseFromString("NONE"));
        assertEquals(PersonnelRole.BATTLE_ARMOUR, PersonnelRole.parseFromString("BATTLE_ARMOUR"));
        assertEquals(PersonnelRole.ADMINISTRATOR_LOGISTICS, PersonnelRole.parseFromString("ADMINISTRATOR_LOGISTICS"));

        // Error Case
        assertEquals(PersonnelRole.NONE, PersonnelRole.parseFromString("28"));
        assertEquals(PersonnelRole.NONE, PersonnelRole.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("PersonnelRole.MEKWARRIOR.text"),
                PersonnelRole.MEKWARRIOR.toString());
        assertEquals(resources.getString("PersonnelRole.ADMINISTRATOR_LOGISTICS.text"),
                PersonnelRole.ADMINISTRATOR_LOGISTICS.toString());
    }
}
