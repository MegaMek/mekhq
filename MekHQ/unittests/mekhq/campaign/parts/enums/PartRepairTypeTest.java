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
package mekhq.campaign.parts.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PartRepairTypeTest {
    //region Variable Declarations
    private static final PartRepairType[] types = PartRepairType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Parts",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testIsValidForMRMS() {
        for (final PartRepairType partRepairType : types) {
            switch (partRepairType) {
                case HEAT_SINK:
                case MEK_LOCATION:
                case PHYSICAL_WEAPON:
                case UNKNOWN_LOCATION:
                    assertFalse(partRepairType.isValidForMRMS());
                    break;
                default:
                    assertTrue(partRepairType.isValidForMRMS());
                    break;
            }
        }
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsArmour() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.ARMOUR) {
                assertTrue(partRepairType.isArmour());
            } else {
                assertFalse(partRepairType.isArmour());
            }
        }
    }

    @Test
    public void testIsAmmunition() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.AMMUNITION) {
                assertTrue(partRepairType.isAmmunition());
            } else {
                assertFalse(partRepairType.isAmmunition());
            }
        }
    }

    @Test
    public void testIsWeapon() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.WEAPON) {
                assertTrue(partRepairType.isWeapon());
            } else {
                assertFalse(partRepairType.isWeapon());
            }
        }
    }

    @Test
    public void testIsGeneralLocation() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.GENERAL_LOCATION) {
                assertTrue(partRepairType.isGeneralLocation());
            } else {
                assertFalse(partRepairType.isGeneralLocation());
            }
        }
    }

    @Test
    public void testIsEngine() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.ENGINE) {
                assertTrue(partRepairType.isEngine());
            } else {
                assertFalse(partRepairType.isEngine());
            }
        }
    }

    @Test
    public void testIsGyro() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.GYRO) {
                assertTrue(partRepairType.isGyro());
            } else {
                assertFalse(partRepairType.isGyro());
            }
        }
    }

    @Test
    public void testIsActuator() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.ACTUATOR) {
                assertTrue(partRepairType.isActuator());
            } else {
                assertFalse(partRepairType.isActuator());
            }
        }
    }

    @Test
    public void testIsElectronics() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.ELECTRONICS) {
                assertTrue(partRepairType.isElectronics());
            } else {
                assertFalse(partRepairType.isElectronics());
            }
        }
    }

    @Test
    public void testIsGeneral() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.GENERAL) {
                assertTrue(partRepairType.isGeneral());
            } else {
                assertFalse(partRepairType.isGeneral());
            }
        }
    }

    @Test
    public void testIsHeatSink() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.HEAT_SINK) {
                assertTrue(partRepairType.isHeatSink());
            } else {
                assertFalse(partRepairType.isHeatSink());
            }
        }
    }

    @Test
    public void testIsMekLocation() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.MEK_LOCATION) {
                assertTrue(partRepairType.isMekLocation());
            } else {
                assertFalse(partRepairType.isMekLocation());
            }
        }
    }

    @Test
    public void testIsPhysicalWeapon() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.PHYSICAL_WEAPON) {
                assertTrue(partRepairType.isPhysicalWeapon());
            } else {
                assertFalse(partRepairType.isPhysicalWeapon());
            }
        }
    }

    @Test
    public void testIsPodSpace() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.POD_SPACE) {
                assertTrue(partRepairType.isPodSpace());
            } else {
                assertFalse(partRepairType.isPodSpace());
            }
        }
    }

    @Test
    public void testIsUnknownLocation() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.UNKNOWN_LOCATION) {
                assertTrue(partRepairType.isUnknownLocation());
            } else {
                assertFalse(partRepairType.isUnknownLocation());
            }
        }
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(PartRepairType.WEAPON, PartRepairType.parseFromString("WEAPON"));
        assertEquals(PartRepairType.GENERAL, PartRepairType.parseFromString("GENERAL"));

        // Legacy Parsing - Enum Renames
        assertEquals(PartRepairType.ARMOUR, PartRepairType.parseFromString("0"));
        assertEquals(PartRepairType.AMMUNITION, PartRepairType.parseFromString("1"));
        assertEquals(PartRepairType.WEAPON, PartRepairType.parseFromString("2"));
        assertEquals(PartRepairType.GENERAL_LOCATION, PartRepairType.parseFromString("3"));
        assertEquals(PartRepairType.ENGINE, PartRepairType.parseFromString("4"));
        assertEquals(PartRepairType.GYRO, PartRepairType.parseFromString("5"));
        assertEquals(PartRepairType.ACTUATOR, PartRepairType.parseFromString("6"));
        assertEquals(PartRepairType.ELECTRONICS, PartRepairType.parseFromString("7"));
        assertEquals(PartRepairType.GENERAL, PartRepairType.parseFromString("8"));
        assertEquals(PartRepairType.HEAT_SINK, PartRepairType.parseFromString("9"));
        assertEquals(PartRepairType.MEK_LOCATION, PartRepairType.parseFromString("10"));
        assertEquals(PartRepairType.PHYSICAL_WEAPON, PartRepairType.parseFromString("11"));
        assertEquals(PartRepairType.POD_SPACE, PartRepairType.parseFromString("12"));

        // Error Case
        assertEquals(PartRepairType.GENERAL_LOCATION, PartRepairType.parseFromString("13"));
        assertEquals(PartRepairType.GENERAL_LOCATION, PartRepairType.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("PartRepairType.ARMOUR.text"),
                PartRepairType.ARMOUR.toString());
        assertEquals(resources.getString("PartRepairType.AMMUNITION.text"),
                PartRepairType.AMMUNITION.toString());
        assertEquals(resources.getString("PartRepairType.HEAT_SINK.text"),
                PartRepairType.HEAT_SINK.toString());
    }
}
