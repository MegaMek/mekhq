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
package mekhq.gui.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import mekhq.MekHQ;

class PersonnelFilterTest {
    // region Variable Declarations
    private static final PersonnelFilter[] filters = PersonnelFilter.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    void testGetToolTipText() {
        assertEquals(resources.getString("PersonnelFilter.ALL.toolTipText"),
                PersonnelFilter.ALL.getToolTipText());
        assertEquals(resources.getString("PersonnelFilter.PROTOMEK_PILOT.toolTipText"),
                PersonnelFilter.PROTOMEK_PILOT.getToolTipText());
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsAll() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ALL) {
                assertTrue(personnelFilter.isAll());
            } else {
                assertFalse(personnelFilter.isAll());
            }
        }
    }

    @Test
    void testIsActive() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ACTIVE) {
                assertTrue(personnelFilter.isActive());
            } else {
                assertFalse(personnelFilter.isActive());
            }
        }
    }

    @Test
    void testIsCombat() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.COMBAT) {
                assertTrue(personnelFilter.isCombat());
            } else {
                assertFalse(personnelFilter.isCombat());
            }
        }
    }

    @Test
    void testIsSupport() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.SUPPORT) {
                assertTrue(personnelFilter.isSupport());
            } else {
                assertFalse(personnelFilter.isSupport());
            }
        }
    }

    @Test
    void testIsMekWarriors() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEKWARRIORS) {
                assertTrue(personnelFilter.isMekWarriors());
            } else {
                assertFalse(personnelFilter.isMekWarriors());
            }
        }
    }

    @Test
    void testIsMekWarrior() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEKWARRIOR) {
                assertTrue(personnelFilter.isMekWarrior());
            } else {
                assertFalse(personnelFilter.isMekWarrior());
            }
        }
    }

    @Test
    void testIsLAMPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.LAM_PILOT) {
                assertTrue(personnelFilter.isLAMPilot());
            } else {
                assertFalse(personnelFilter.isLAMPilot());
            }
        }
    }

    @Test
    void testIsVehicleCrewMember() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VEHICLE_CREWMEMBER) {
                assertTrue(personnelFilter.isVehicleCrewMember());
            } else {
                assertFalse(personnelFilter.isVehicleCrewMember());
            }
        }
    }

    @Test
    void testIsGroundVehicleDriver() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.GROUND_VEHICLE_DRIVER) {
                assertTrue(personnelFilter.isGroundVehicleDriver());
            } else {
                assertFalse(personnelFilter.isGroundVehicleDriver());
            }
        }
    }

    @Test
    void testIsNavalVehicleDriver() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.NAVAL_VEHICLE_DRIVER) {
                assertTrue(personnelFilter.isNavalVehicleDriver());
            } else {
                assertFalse(personnelFilter.isNavalVehicleDriver());
            }
        }
    }

    @Test
    void testIsVTOLPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VTOL_PILOT) {
                assertTrue(personnelFilter.isVTOLPilot());
            } else {
                assertFalse(personnelFilter.isVTOLPilot());
            }
        }
    }

    @Test
    void testIsVehicleGunner() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VEHICLE_GUNNER) {
                assertTrue(personnelFilter.isVehicleGunner());
            } else {
                assertFalse(personnelFilter.isVehicleGunner());
            }
        }
    }

    @Test
    void testIsVehicleCrew() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VEHICLE_CREW) {
                assertTrue(personnelFilter.isVehicleCrew());
            } else {
                assertFalse(personnelFilter.isVehicleCrew());
            }
        }
    }

    @Test
    void testIsAerospacePilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.AEROSPACE_PILOT) {
                assertTrue(personnelFilter.isAerospacePilot());
            } else {
                assertFalse(personnelFilter.isAerospacePilot());
            }
        }
    }

    @Test
    void testIsConventionalAircraftPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.CONVENTIONAL_AIRCRAFT_PILOT) {
                assertTrue(personnelFilter.isConventionalAircraftPilot());
            } else {
                assertFalse(personnelFilter.isConventionalAircraftPilot());
            }
        }
    }

    @Test
    void testIsProtoMekPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.PROTOMEK_PILOT) {
                assertTrue(personnelFilter.isProtoMekPilot());
            } else {
                assertFalse(personnelFilter.isProtoMekPilot());
            }
        }
    }

    @Test
    void testIsBattleArmour() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.BATTLE_ARMOUR) {
                assertTrue(personnelFilter.isBattleArmor());
            } else {
                assertFalse(personnelFilter.isBattleArmor());
            }
        }
    }

    @Test
    void testIsSoldier() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.SOLDIER) {
                assertTrue(personnelFilter.isSoldier());
            } else {
                assertFalse(personnelFilter.isSoldier());
            }
        }
    }

    @Test
    void testIsVesselCrewMember() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_CREWMEMBER) {
                assertTrue(personnelFilter.isVesselCrewMember());
            } else {
                assertFalse(personnelFilter.isVesselCrewMember());
            }
        }
    }

    @Test
    void testIsVesselPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_PILOT) {
                assertTrue(personnelFilter.isVesselPilot());
            } else {
                assertFalse(personnelFilter.isVesselPilot());
            }
        }
    }

    @Test
    void testIsVesselGunner() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_GUNNER) {
                assertTrue(personnelFilter.isVesselGunner());
            } else {
                assertFalse(personnelFilter.isVesselGunner());
            }
        }
    }

    @Test
    void testIsVesselCrew() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_CREW) {
                assertTrue(personnelFilter.isVesselCrew());
            } else {
                assertFalse(personnelFilter.isVesselCrew());
            }
        }
    }

    @Test
    void testIsVesselNavigator() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_NAVIGATOR) {
                assertTrue(personnelFilter.isVesselNavigator());
            } else {
                assertFalse(personnelFilter.isVesselNavigator());
            }
        }
    }

    @Test
    void testIsTech() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.TECH) {
                assertTrue(personnelFilter.isTech());
            } else {
                assertFalse(personnelFilter.isTech());
            }
        }
    }

    @Test
    void testIsMekTech() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEK_TECH) {
                assertTrue(personnelFilter.isMekTech());
            } else {
                assertFalse(personnelFilter.isMekTech());
            }
        }
    }

    @Test
    void testIsMechanic() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MECHANIC) {
                assertTrue(personnelFilter.isMechanic());
            } else {
                assertFalse(personnelFilter.isMechanic());
            }
        }
    }

    @Test
    void testIsAeroTek() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.AERO_TECH) {
                assertTrue(personnelFilter.isAeroTek());
            } else {
                assertFalse(personnelFilter.isAeroTek());
            }
        }
    }

    @Test
    void testIsBATech() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.BA_TECH) {
                assertTrue(personnelFilter.isBATech());
            } else {
                assertFalse(personnelFilter.isBATech());
            }
        }
    }

    @Test
    void testIsAstech() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ASTECH) {
                assertTrue(personnelFilter.isAstech());
            } else {
                assertFalse(personnelFilter.isAstech());
            }
        }
    }

    @Test
    void testIsMedical() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEDICAL) {
                assertTrue(personnelFilter.isMedical());
            } else {
                assertFalse(personnelFilter.isMedical());
            }
        }
    }

    @Test
    void testIsDoctor() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.DOCTOR) {
                assertTrue(personnelFilter.isDoctor());
            } else {
                assertFalse(personnelFilter.isDoctor());
            }
        }
    }

    @Test
    void testIsMedic() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEDIC) {
                assertTrue(personnelFilter.isMedic());
            } else {
                assertFalse(personnelFilter.isMedic());
            }
        }
    }

    @Test
    void testIsAdministrator() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR) {
                assertTrue(personnelFilter.isAdministrator());
            } else {
                assertFalse(personnelFilter.isAdministrator());
            }
        }
    }

    @Test
    void testIsAdministratorCommand() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR_COMMAND) {
                assertTrue(personnelFilter.isAdministratorCommand());
            } else {
                assertFalse(personnelFilter.isAdministratorCommand());
            }
        }
    }

    @Test
    void testIsAdministratorLogistics() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR_LOGISTICS) {
                assertTrue(personnelFilter.isAdministratorLogistics());
            } else {
                assertFalse(personnelFilter.isAdministratorLogistics());
            }
        }
    }

    @Test
    void testIsAdministratorTransport() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR_TRANSPORT) {
                assertTrue(personnelFilter.isAdministratorTransport());
            } else {
                assertFalse(personnelFilter.isAdministratorTransport());
            }
        }
    }

    @Test
    void testIsAdministratorHR() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR_HR) {
                assertTrue(personnelFilter.isAdministratorHR());
            } else {
                assertFalse(personnelFilter.isAdministratorHR());
            }
        }
    }

    @Test
    void testIsDependent() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.DEPENDENT) {
                assertTrue(personnelFilter.isDependent());
            } else {
                assertFalse(personnelFilter.isDependent());
            }
        }
    }

    @Test
    void testIsFounder() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.FOUNDER) {
                assertTrue(personnelFilter.isFounder());
            } else {
                assertFalse(personnelFilter.isFounder());
            }
        }
    }

    @Test
    void testIsPrisoner() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.PRISONER) {
                assertTrue(personnelFilter.isPrisoner());
            } else {
                assertFalse(personnelFilter.isPrisoner());
            }
        }
    }

    @Test
    void testIsInactive() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.INACTIVE) {
                assertTrue(personnelFilter.isInactive());
            } else {
                assertFalse(personnelFilter.isInactive());
            }
        }
    }

    @Test
    void testIsMIA() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MIA) {
                assertTrue(personnelFilter.isMIA());
            } else {
                assertFalse(personnelFilter.isMIA());
            }
        }
    }

    @Test
    void testIsRetired() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.RETIRED) {
                assertTrue(personnelFilter.isRetired());
            } else {
                assertFalse(personnelFilter.isRetired());
            }
        }
    }

    @Test
    void testIsDeserted() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.DESERTED) {
                assertTrue(personnelFilter.isDeserted());
            } else {
                assertFalse(personnelFilter.isDeserted());
            }
        }
    }

    @Test
    void testIsStudent() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.STUDENT) {
                assertTrue(personnelFilter.isStudent());
            } else {
                assertFalse(personnelFilter.isStudent());
            }
        }
    }

    @Test
    void testIsKIA() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.KIA) {
                assertTrue(personnelFilter.isKIA());
            } else {
                assertFalse(personnelFilter.isKIA());
            }
        }
    }

    @Test
    void testIsDead() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.DEAD) {
                assertTrue(personnelFilter.isDead());
            } else {
                assertFalse(personnelFilter.isDead());
            }
        }
    }
    // endregion Boolean Comparison Methods

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("PersonnelFilter.ALL.text"), PersonnelFilter.ALL.toString());
        assertEquals(resources.getString("PersonnelFilter.SOLDIER.text"), PersonnelFilter.SOLDIER.toString());
        assertEquals(resources.getString("PersonnelFilter.PRISONER.text"), PersonnelFilter.PRISONER.toString());
    }
}
