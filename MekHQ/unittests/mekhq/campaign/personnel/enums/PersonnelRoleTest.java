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

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonnelRoleTest {
    //region Variable Declarations
    private static final PersonnelRole[] roles = PersonnelRole.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
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
    //endregion Boolean Comparison Methods

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
