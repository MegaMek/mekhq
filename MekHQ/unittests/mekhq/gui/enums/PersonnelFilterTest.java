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

import mekhq.MekHQ;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonnelFilterTest {
    //region Variable Declarations
    private static final PersonnelFilter[] filters = PersonnelFilter.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("PersonnelFilter.ALL.toolTipText"),
                PersonnelFilter.ALL.getToolTipText());
        assertEquals(resources.getString("PersonnelFilter.PROTOMEK_PILOT.toolTipText"),
                PersonnelFilter.PROTOMEK_PILOT.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsAll() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ALL) {
                assertTrue(personnelFilter.isAll());
            } else {
                assertFalse(personnelFilter.isAll());
            }
        }
    }

    @Test
    public void testIsActive() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ACTIVE) {
                assertTrue(personnelFilter.isActive());
            } else {
                assertFalse(personnelFilter.isActive());
            }
        }
    }

    @Test
    public void testIsCombat() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.COMBAT) {
                assertTrue(personnelFilter.isCombat());
            } else {
                assertFalse(personnelFilter.isCombat());
            }
        }
    }

    @Test
    public void testIsSupport() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.SUPPORT) {
                assertTrue(personnelFilter.isSupport());
            } else {
                assertFalse(personnelFilter.isSupport());
            }
        }
    }

    @Test
    public void testIsMekWarriors() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEKWARRIORS) {
                assertTrue(personnelFilter.isMekWarriors());
            } else {
                assertFalse(personnelFilter.isMekWarriors());
            }
        }
    }

    @Test
    public void testIsMekWarrior() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEKWARRIOR) {
                assertTrue(personnelFilter.isMekWarrior());
            } else {
                assertFalse(personnelFilter.isMekWarrior());
            }
        }
    }

    @Test
    public void testIsLAMPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.LAM_PILOT) {
                assertTrue(personnelFilter.isLAMPilot());
            } else {
                assertFalse(personnelFilter.isLAMPilot());
            }
        }
    }

    @Test
    public void testIsVehicleCrewmember() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VEHICLE_CREWMEMBER) {
                assertTrue(personnelFilter.isVehicleCrewmember());
            } else {
                assertFalse(personnelFilter.isVehicleCrewmember());
            }
        }
    }

    @Test
    public void testIsGroundVehicleDriver() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.GROUND_VEHICLE_DRIVER) {
                assertTrue(personnelFilter.isGroundVehicleDriver());
            } else {
                assertFalse(personnelFilter.isGroundVehicleDriver());
            }
        }
    }

    @Test
    public void testIsNavalVehicleDriver() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.NAVAL_VEHICLE_DRIVER) {
                assertTrue(personnelFilter.isNavalVehicleDriver());
            } else {
                assertFalse(personnelFilter.isNavalVehicleDriver());
            }
        }
    }

    @Test
    public void testIsVTOLPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VTOL_PILOT) {
                assertTrue(personnelFilter.isVTOLPilot());
            } else {
                assertFalse(personnelFilter.isVTOLPilot());
            }
        }
    }

    @Test
    public void testIsVehicleGunner() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VEHICLE_GUNNER) {
                assertTrue(personnelFilter.isVehicleGunner());
            } else {
                assertFalse(personnelFilter.isVehicleGunner());
            }
        }
    }

    @Test
    public void testIsVehicleCrew() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VEHICLE_CREW) {
                assertTrue(personnelFilter.isVehicleCrew());
            } else {
                assertFalse(personnelFilter.isVehicleCrew());
            }
        }
    }

    @Test
    public void testIsAerospacePilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.AEROSPACE_PILOT) {
                assertTrue(personnelFilter.isAerospacePilot());
            } else {
                assertFalse(personnelFilter.isAerospacePilot());
            }
        }
    }

    @Test
    public void testIsConventionalAircraftPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.CONVENTIONAL_AIRCRAFT_PILOT) {
                assertTrue(personnelFilter.isConventionalAircraftPilot());
            } else {
                assertFalse(personnelFilter.isConventionalAircraftPilot());
            }
        }
    }

    @Test
    public void testIsProtoMekPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.PROTOMEK_PILOT) {
                assertTrue(personnelFilter.isProtoMekPilot());
            } else {
                assertFalse(personnelFilter.isProtoMekPilot());
            }
        }
    }

    @Test
    public void testIsBattleArmour() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.BATTLE_ARMOUR) {
                assertTrue(personnelFilter.isBattleArmor());
            } else {
                assertFalse(personnelFilter.isBattleArmor());
            }
        }
    }

    @Test
    public void testIsSoldier() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.SOLDIER) {
                assertTrue(personnelFilter.isSoldier());
            } else {
                assertFalse(personnelFilter.isSoldier());
            }
        }
    }

    @Test
    public void testIsVesselCrewmember() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_CREWMEMBER) {
                assertTrue(personnelFilter.isVesselCrewmember());
            } else {
                assertFalse(personnelFilter.isVesselCrewmember());
            }
        }
    }

    @Test
    public void testIsVesselPilot() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_PILOT) {
                assertTrue(personnelFilter.isVesselPilot());
            } else {
                assertFalse(personnelFilter.isVesselPilot());
            }
        }
    }

    @Test
    public void testIsVesselGunner() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_GUNNER) {
                assertTrue(personnelFilter.isVesselGunner());
            } else {
                assertFalse(personnelFilter.isVesselGunner());
            }
        }
    }

    @Test
    public void testIsVesselCrew() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_CREW) {
                assertTrue(personnelFilter.isVesselCrew());
            } else {
                assertFalse(personnelFilter.isVesselCrew());
            }
        }
    }

    @Test
    public void testIsVesselNavigator() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.VESSEL_NAVIGATOR) {
                assertTrue(personnelFilter.isVesselNavigator());
            } else {
                assertFalse(personnelFilter.isVesselNavigator());
            }
        }
    }

    @Test
    public void testIsTech() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.TECH) {
                assertTrue(personnelFilter.isTech());
            } else {
                assertFalse(personnelFilter.isTech());
            }
        }
    }

    @Test
    public void testIsMekTech() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEK_TECH) {
                assertTrue(personnelFilter.isMekTech());
            } else {
                assertFalse(personnelFilter.isMekTech());
            }
        }
    }

    @Test
    public void testIsMechanic() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MECHANIC) {
                assertTrue(personnelFilter.isMechanic());
            } else {
                assertFalse(personnelFilter.isMechanic());
            }
        }
    }

    @Test
    public void testIsAeroTech() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.AERO_TECH) {
                assertTrue(personnelFilter.isAeroTech());
            } else {
                assertFalse(personnelFilter.isAeroTech());
            }
        }
    }

    @Test
    public void testIsBATech() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.BA_TECH) {
                assertTrue(personnelFilter.isBATech());
            } else {
                assertFalse(personnelFilter.isBATech());
            }
        }
    }

    @Test
    public void testIsAstech() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ASTECH) {
                assertTrue(personnelFilter.isAstech());
            } else {
                assertFalse(personnelFilter.isAstech());
            }
        }
    }

    @Test
    public void testIsMedical() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEDICAL) {
                assertTrue(personnelFilter.isMedical());
            } else {
                assertFalse(personnelFilter.isMedical());
            }
        }
    }

    @Test
    public void testIsDoctor() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.DOCTOR) {
                assertTrue(personnelFilter.isDoctor());
            } else {
                assertFalse(personnelFilter.isDoctor());
            }
        }
    }

    @Test
    public void testIsMedic() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MEDIC) {
                assertTrue(personnelFilter.isMedic());
            } else {
                assertFalse(personnelFilter.isMedic());
            }
        }
    }

    @Test
    public void testIsAdministrator() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR) {
                assertTrue(personnelFilter.isAdministrator());
            } else {
                assertFalse(personnelFilter.isAdministrator());
            }
        }
    }

    @Test
    public void testIsAdministratorCommand() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR_COMMAND) {
                assertTrue(personnelFilter.isAdministratorCommand());
            } else {
                assertFalse(personnelFilter.isAdministratorCommand());
            }
        }
    }

    @Test
    public void testIsAdministratorLogistics() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR_LOGISTICS) {
                assertTrue(personnelFilter.isAdministratorLogistics());
            } else {
                assertFalse(personnelFilter.isAdministratorLogistics());
            }
        }
    }

    @Test
    public void testIsAdministratorTransport() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR_TRANSPORT) {
                assertTrue(personnelFilter.isAdministratorTransport());
            } else {
                assertFalse(personnelFilter.isAdministratorTransport());
            }
        }
    }

    @Test
    public void testIsAdministratorHR() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.ADMINISTRATOR_HR) {
                assertTrue(personnelFilter.isAdministratorHR());
            } else {
                assertFalse(personnelFilter.isAdministratorHR());
            }
        }
    }

    @Test
    public void testIsDependent() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.DEPENDENT) {
                assertTrue(personnelFilter.isDependent());
            } else {
                assertFalse(personnelFilter.isDependent());
            }
        }
    }

    @Test
    public void testIsFounder() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.FOUNDER) {
                assertTrue(personnelFilter.isFounder());
            } else {
                assertFalse(personnelFilter.isFounder());
            }
        }
    }

    @Test
    public void testIsPrisoner() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.PRISONER) {
                assertTrue(personnelFilter.isPrisoner());
            } else {
                assertFalse(personnelFilter.isPrisoner());
            }
        }
    }

    @Test
    public void testIsInactive() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.INACTIVE) {
                assertTrue(personnelFilter.isInactive());
            } else {
                assertFalse(personnelFilter.isInactive());
            }
        }
    }

    @Test
    public void testIsMIA() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.MIA) {
                assertTrue(personnelFilter.isMIA());
            } else {
                assertFalse(personnelFilter.isMIA());
            }
        }
    }

    @Test
    public void testIsRetired() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.RETIRED) {
                assertTrue(personnelFilter.isRetired());
            } else {
                assertFalse(personnelFilter.isRetired());
            }
        }
    }

    @Test
    public void testIsDeserted() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.DESERTED) {
                assertTrue(personnelFilter.isDeserted());
            } else {
                assertFalse(personnelFilter.isDeserted());
            }
        }
    }

    @Test
    public void testIsStudent() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.STUDENT) {
                assertTrue(personnelFilter.isStudent());
            } else {
                assertFalse(personnelFilter.isStudent());
            }
        }
    }

    @Test
    public void testIsKIA() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.KIA) {
                assertTrue(personnelFilter.isKIA());
            } else {
                assertFalse(personnelFilter.isKIA());
            }
        }
    }

    @Test
    public void testIsDead() {
        for (final PersonnelFilter personnelFilter : filters) {
            if (personnelFilter == PersonnelFilter.DEAD) {
                assertTrue(personnelFilter.isDead());
            } else {
                assertFalse(personnelFilter.isDead());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetStandardPersonnelFilters() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetExpandedPersonnelFilters() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetIndividualRolesStandardPersonnelFilters() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetIndividualRolesExpandedPersonnelFilters() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetAllStandardFilters() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetAllIndividualRoleFilters() {
        assertEquals(filters.length, PersonnelFilter.getAllIndividualRoleFilters().size());
    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetFilteredInformation() {

    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("PersonnelFilter.ALL.text"), PersonnelFilter.ALL.toString());
        assertEquals(resources.getString("PersonnelFilter.SOLDIER.text"), PersonnelFilter.SOLDIER.toString());
        assertEquals(resources.getString("PersonnelFilter.PRISONER.text"), PersonnelFilter.PRISONER.toString());
    }
}
