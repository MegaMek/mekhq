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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonnelRoleTest {
    //region Variable Declarations
    private static final PersonnelRole[] roles = PersonnelRole.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetName() {
        assertEquals(resources.getString("PersonnelRole.MECHWARRIOR.text"),
                PersonnelRole.MECHWARRIOR.getName(false));
        assertEquals(resources.getString("PersonnelRole.MECHWARRIOR.text"),
                PersonnelRole.MECHWARRIOR.getName(true));
        assertEquals(resources.getString("PersonnelRole.BATTLE_ARMOUR.text"),
                PersonnelRole.BATTLE_ARMOUR.getName(false));
        assertEquals(resources.getString("PersonnelRole.BATTLE_ARMOUR.clan.text"),
                PersonnelRole.BATTLE_ARMOUR.getName(true));
        assertEquals(resources.getString("PersonnelRole.ADMINISTRATOR_LOGISTICS.text"),
                PersonnelRole.ADMINISTRATOR_LOGISTICS.getName(false));
    }

    @Test
    public void testGetMnemonicEnsureUniqueness() {
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
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsMechWarrior() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.MECHWARRIOR) {
                assertTrue(personnelRole.isMechWarrior());
            } else {
                assertFalse(personnelRole.isMechWarrior());
            }
        }
    }

    @Test
    public void testIsLAMPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.LAM_PILOT) {
                assertTrue(personnelRole.isLAMPilot());
            } else {
                assertFalse(personnelRole.isLAMPilot());
            }
        }
    }

    @Test
    public void testIsGroundVehicleDriver() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.GROUND_VEHICLE_DRIVER) {
                assertTrue(personnelRole.isGroundVehicleDriver());
            } else {
                assertFalse(personnelRole.isGroundVehicleDriver());
            }
        }
    }

    @Test
    public void testIsNavalVehicleDriver() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.NAVAL_VEHICLE_DRIVER) {
                assertTrue(personnelRole.isNavalVehicleDriver());
            } else {
                assertFalse(personnelRole.isNavalVehicleDriver());
            }
        }
    }

    @Test
    public void testIsVTOLPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VTOL_PILOT) {
                assertTrue(personnelRole.isVTOLPilot());
            } else {
                assertFalse(personnelRole.isVTOLPilot());
            }
        }
    }

    @Test
    public void testIsVehicleGunner() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VEHICLE_GUNNER) {
                assertTrue(personnelRole.isVehicleGunner());
            } else {
                assertFalse(personnelRole.isVehicleGunner());
            }
        }
    }

    @Test
    public void testIsVehicleCrew() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VEHICLE_CREW) {
                assertTrue(personnelRole.isVehicleCrew());
            } else {
                assertFalse(personnelRole.isVehicleCrew());
            }
        }
    }

    @Test
    public void testIsAerospacePilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.AEROSPACE_PILOT) {
                assertTrue(personnelRole.isAerospacePilot());
            } else {
                assertFalse(personnelRole.isAerospacePilot());
            }
        }
    }

    @Test
    public void testIsConventionalAircraftPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT) {
                assertTrue(personnelRole.isConventionalAircraftPilot());
            } else {
                assertFalse(personnelRole.isConventionalAircraftPilot());
            }
        }
    }

    @Test
    public void testIsProtoMechPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.PROTOMECH_PILOT) {
                assertTrue(personnelRole.isProtoMechPilot());
            } else {
                assertFalse(personnelRole.isProtoMechPilot());
            }
        }
    }

    @Test
    public void testIsBattleArmour() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.BATTLE_ARMOUR) {
                assertTrue(personnelRole.isBattleArmour());
            } else {
                assertFalse(personnelRole.isBattleArmour());
            }
        }
    }

    @Test
    public void testIsSoldier() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.SOLDIER) {
                assertTrue(personnelRole.isSoldier());
            } else {
                assertFalse(personnelRole.isSoldier());
            }
        }
    }

    @Test
    public void testIsVesselPilot() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VESSEL_PILOT) {
                assertTrue(personnelRole.isVesselPilot());
            } else {
                assertFalse(personnelRole.isVesselPilot());
            }
        }
    }

    @Test
    public void testIsVesselGunner() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VESSEL_GUNNER) {
                assertTrue(personnelRole.isVesselGunner());
            } else {
                assertFalse(personnelRole.isVesselGunner());
            }
        }
    }

    @Test
    public void testIsVesselCrew() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VESSEL_CREW) {
                assertTrue(personnelRole.isVesselCrew());
            } else {
                assertFalse(personnelRole.isVesselCrew());
            }
        }
    }

    @Test
    public void testIsVesselNavigator() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.VESSEL_NAVIGATOR) {
                assertTrue(personnelRole.isVesselNavigator());
            } else {
                assertFalse(personnelRole.isVesselNavigator());
            }
        }
    }

    @Test
    public void testIsMechTech() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.MECH_TECH) {
                assertTrue(personnelRole.isMechTech());
            } else {
                assertFalse(personnelRole.isMechTech());
            }
        }
    }

    @Test
    public void testIsMechanic() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.MECHANIC) {
                assertTrue(personnelRole.isMechanic());
            } else {
                assertFalse(personnelRole.isMechanic());
            }
        }
    }

    @Test
    public void testIsAeroTech() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.AERO_TECH) {
                assertTrue(personnelRole.isAeroTech());
            } else {
                assertFalse(personnelRole.isAeroTech());
            }
        }
    }

    @Test
    public void testIsBATech() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.BA_TECH) {
                assertTrue(personnelRole.isBATech());
            } else {
                assertFalse(personnelRole.isBATech());
            }
        }
    }

    @Test
    public void testIsAstech() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ASTECH) {
                assertTrue(personnelRole.isAstech());
            } else {
                assertFalse(personnelRole.isAstech());
            }
        }
    }

    @Test
    public void testIsDoctor() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.DOCTOR) {
                assertTrue(personnelRole.isDoctor());
            } else {
                assertFalse(personnelRole.isDoctor());
            }
        }
    }

    @Test
    public void testIsMedic() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.MEDIC) {
                assertTrue(personnelRole.isMedic());
            } else {
                assertFalse(personnelRole.isMedic());
            }
        }
    }

    @Test
    public void testIsAdministratorCommand() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ADMINISTRATOR_COMMAND) {
                assertTrue(personnelRole.isAdministratorCommand());
            } else {
                assertFalse(personnelRole.isAdministratorCommand());
            }
        }
    }

    @Test
    public void testIsAdministratorLogistics() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ADMINISTRATOR_LOGISTICS) {
                assertTrue(personnelRole.isAdministratorLogistics());
            } else {
                assertFalse(personnelRole.isAdministratorLogistics());
            }
        }
    }

    @Test
    public void testIsAdministratorTransport() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ADMINISTRATOR_TRANSPORT) {
                assertTrue(personnelRole.isAdministratorTransport());
            } else {
                assertFalse(personnelRole.isAdministratorTransport());
            }
        }
    }

    @Test
    public void testIsAdministratorHR() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.ADMINISTRATOR_HR) {
                assertTrue(personnelRole.isAdministratorHR());
            } else {
                assertFalse(personnelRole.isAdministratorHR());
            }
        }
    }

    @Test
    public void testIsDependent() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.DEPENDENT) {
                assertTrue(personnelRole.isDependent());
            } else {
                assertFalse(personnelRole.isDependent());
            }
        }
    }

    @Test
    public void testIsNone() {
        for (final PersonnelRole personnelRole : roles) {
            if (personnelRole == PersonnelRole.NONE) {
                assertTrue(personnelRole.isNone());
            } else {
                assertFalse(personnelRole.isNone());
            }
        }
    }

    @Test
    public void testIsCombat() {
        for (final PersonnelRole personnelRole : roles) {
            switch (personnelRole) {
                case MECHWARRIOR:
                case LAM_PILOT:
                case GROUND_VEHICLE_DRIVER:
                case NAVAL_VEHICLE_DRIVER:
                case VTOL_PILOT:
                case VEHICLE_GUNNER:
                case VEHICLE_CREW:
                case AEROSPACE_PILOT:
                case CONVENTIONAL_AIRCRAFT_PILOT:
                case PROTOMECH_PILOT:
                case BATTLE_ARMOUR:
                case SOLDIER:
                case VESSEL_PILOT:
                case VESSEL_GUNNER:
                case VESSEL_CREW:
                case VESSEL_NAVIGATOR:
                    assertTrue(personnelRole.isCombat());
                default:
                    assertFalse(personnelRole.isCombat());
            }
        }
    }

    @Test
    public void testIsMechWarriorGrouping() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsAerospaceGrouping() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsConventionalAirGrouping() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsGroundVehicleCrew() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsNavalVehicleCrew() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsVTOLCrew() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsVehicleCrewmember() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsSoldierOrBattleArmour() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsVesselCrewmember() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsSupport() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsTech() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsTechSecondary() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsMedicalStaff() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsAdministrator() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsCivilian() {
        // FIXME : Windchild : ADD
    }
    //endregion Boolean Comparison Methods

    //region Static Methods
    @Test
    public void testGetMilitaryRoles() {
        final List<PersonnelRole> militaryRoles = PersonnelRole.getMilitaryRoles();
        assertEquals(roles.length - 2, militaryRoles.size());
        assertFalse(militaryRoles.contains(PersonnelRole.DEPENDENT));
        assertFalse(militaryRoles.contains(PersonnelRole.NONE));
    }
    @Test
    public void testGetPrimaryRoles() {
        // This should be all roles bar one, namely PersonnelRole.NONE
        final List<PersonnelRole> primaryRoles = PersonnelRole.getPrimaryRoles();
        assertEquals(roles.length - 1, primaryRoles.size());
        assertFalse(primaryRoles.contains(PersonnelRole.NONE));
    }

    @Test
    public void testGetVesselRoles() {
        final List<PersonnelRole> expected = new ArrayList<>();
        expected.add(PersonnelRole.VESSEL_PILOT);
        expected.add(PersonnelRole.VESSEL_GUNNER);
        expected.add(PersonnelRole.VESSEL_CREW);
        expected.add(PersonnelRole.VESSEL_NAVIGATOR);
        assertEquals(expected, PersonnelRole.getVesselRoles());
    }

    @Test
    public void testGetTechRoles() {
        final List<PersonnelRole> expected = new ArrayList<>();
        expected.add(PersonnelRole.VESSEL_CREW);
        expected.add(PersonnelRole.MECH_TECH);
        expected.add(PersonnelRole.MECHANIC);
        expected.add(PersonnelRole.AERO_TECH);
        expected.add(PersonnelRole.BA_TECH);
        assertEquals(expected, PersonnelRole.getTechRoles());
    }

    @Test
    public void testGetAdministratorRoles() {
        final List<PersonnelRole> expected = new ArrayList<>();
        expected.add(PersonnelRole.ADMINISTRATOR_COMMAND);
        expected.add(PersonnelRole.ADMINISTRATOR_LOGISTICS);
        expected.add(PersonnelRole.ADMINISTRATOR_TRANSPORT);
        expected.add(PersonnelRole.ADMINISTRATOR_HR);
        assertEquals(expected, PersonnelRole.getAdministratorRoles());
    }

    @Test
    public void testGetCivilianCount() {
        // Civilian Roles: Dependent and None
        assertEquals(2, PersonnelRole.getCivilianCount());
    }
    //endregion Static Methods

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(PersonnelRole.NONE, PersonnelRole.parseFromString("NONE"));
        assertEquals(PersonnelRole.BATTLE_ARMOUR, PersonnelRole.parseFromString("BATTLE_ARMOUR"));
        assertEquals(PersonnelRole.ADMINISTRATOR_LOGISTICS, PersonnelRole.parseFromString("ADMINISTRATOR_LOGISTICS"));

        // Legacy Parsing
        assertEquals(PersonnelRole.NONE, PersonnelRole.parseFromString("0"));
        assertEquals(PersonnelRole.MECHWARRIOR, PersonnelRole.parseFromString("1"));
        assertEquals(PersonnelRole.AEROSPACE_PILOT, PersonnelRole.parseFromString("2"));
        assertEquals(PersonnelRole.GROUND_VEHICLE_DRIVER, PersonnelRole.parseFromString("3"));
        assertEquals(PersonnelRole.NAVAL_VEHICLE_DRIVER, PersonnelRole.parseFromString("4"));
        assertEquals(PersonnelRole.VTOL_PILOT, PersonnelRole.parseFromString("5"));
        assertEquals(PersonnelRole.VEHICLE_GUNNER, PersonnelRole.parseFromString("6"));
        assertEquals(PersonnelRole.BATTLE_ARMOUR, PersonnelRole.parseFromString("7"));
        assertEquals(PersonnelRole.SOLDIER, PersonnelRole.parseFromString("8"));
        assertEquals(PersonnelRole.PROTOMECH_PILOT, PersonnelRole.parseFromString("9"));
        assertEquals(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT, PersonnelRole.parseFromString("10"));
        assertEquals(PersonnelRole.VESSEL_PILOT, PersonnelRole.parseFromString("11"));
        assertEquals(PersonnelRole.VESSEL_CREW, PersonnelRole.parseFromString("12"));
        assertEquals(PersonnelRole.VESSEL_GUNNER, PersonnelRole.parseFromString("13"));
        assertEquals(PersonnelRole.VESSEL_NAVIGATOR, PersonnelRole.parseFromString("14"));
        assertEquals(PersonnelRole.MECH_TECH, PersonnelRole.parseFromString("15"));
        assertEquals(PersonnelRole.MECHANIC, PersonnelRole.parseFromString("16"));
        assertEquals(PersonnelRole.AERO_TECH, PersonnelRole.parseFromString("17"));
        assertEquals(PersonnelRole.BA_TECH, PersonnelRole.parseFromString("18"));
        assertEquals(PersonnelRole.ASTECH, PersonnelRole.parseFromString("19"));
        assertEquals(PersonnelRole.DOCTOR, PersonnelRole.parseFromString("20"));
        assertEquals(PersonnelRole.MEDIC, PersonnelRole.parseFromString("21"));
        assertEquals(PersonnelRole.ADMINISTRATOR_COMMAND, PersonnelRole.parseFromString("22"));
        assertEquals(PersonnelRole.ADMINISTRATOR_LOGISTICS, PersonnelRole.parseFromString("23"));
        assertEquals(PersonnelRole.ADMINISTRATOR_TRANSPORT, PersonnelRole.parseFromString("24"));
        assertEquals(PersonnelRole.ADMINISTRATOR_HR, PersonnelRole.parseFromString("25"));
        assertEquals(PersonnelRole.LAM_PILOT, PersonnelRole.parseFromString("26"));
        assertEquals(PersonnelRole.VEHICLE_CREW, PersonnelRole.parseFromString("27"));

        // Error Case
        assertEquals(PersonnelRole.NONE, PersonnelRole.parseFromString("28"));
        assertEquals(PersonnelRole.NONE, PersonnelRole.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("PersonnelRole.MECHWARRIOR.text"),
                PersonnelRole.MECHWARRIOR.toString());
        assertEquals(resources.getString("PersonnelRole.ADMINISTRATOR_LOGISTICS.text"),
                PersonnelRole.ADMINISTRATOR_LOGISTICS.toString());
    }
}
