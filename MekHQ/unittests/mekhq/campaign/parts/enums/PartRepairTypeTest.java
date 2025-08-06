/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

class PartRepairTypeTest {
    // region Variable Declarations
    private static final PartRepairType[] types = PartRepairType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Parts",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    void testIsValidForMRMS() {
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
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsArmour() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.ARMOUR) {
                assertTrue(partRepairType.isArmour());
            } else {
                assertFalse(partRepairType.isArmour());
            }
        }
    }

    @Test
    void testIsAmmunition() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.AMMUNITION) {
                assertTrue(partRepairType.isAmmunition());
            } else {
                assertFalse(partRepairType.isAmmunition());
            }
        }
    }

    @Test
    void testIsWeapon() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.WEAPON) {
                assertTrue(partRepairType.isWeapon());
            } else {
                assertFalse(partRepairType.isWeapon());
            }
        }
    }

    @Test
    void testIsGeneralLocation() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.GENERAL_LOCATION) {
                assertTrue(partRepairType.isGeneralLocation());
            } else {
                assertFalse(partRepairType.isGeneralLocation());
            }
        }
    }

    @Test
    void testIsEngine() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.ENGINE) {
                assertTrue(partRepairType.isEngine());
            } else {
                assertFalse(partRepairType.isEngine());
            }
        }
    }

    @Test
    void testIsGyro() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.GYRO) {
                assertTrue(partRepairType.isGyro());
            } else {
                assertFalse(partRepairType.isGyro());
            }
        }
    }

    @Test
    void testIsActuator() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.ACTUATOR) {
                assertTrue(partRepairType.isActuator());
            } else {
                assertFalse(partRepairType.isActuator());
            }
        }
    }

    @Test
    void testIsElectronics() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.ELECTRONICS) {
                assertTrue(partRepairType.isElectronics());
            } else {
                assertFalse(partRepairType.isElectronics());
            }
        }
    }

    @Test
    void testIsGeneral() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.GENERAL) {
                assertTrue(partRepairType.isGeneral());
            } else {
                assertFalse(partRepairType.isGeneral());
            }
        }
    }

    @Test
    void testIsHeatSink() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.HEAT_SINK) {
                assertTrue(partRepairType.isHeatSink());
            } else {
                assertFalse(partRepairType.isHeatSink());
            }
        }
    }

    @Test
    void testIsMekLocation() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.MEK_LOCATION) {
                assertTrue(partRepairType.isMekLocation());
            } else {
                assertFalse(partRepairType.isMekLocation());
            }
        }
    }

    @Test
    void testIsPhysicalWeapon() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.PHYSICAL_WEAPON) {
                assertTrue(partRepairType.isPhysicalWeapon());
            } else {
                assertFalse(partRepairType.isPhysicalWeapon());
            }
        }
    }

    @Test
    void testIsPodSpace() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.POD_SPACE) {
                assertTrue(partRepairType.isPodSpace());
            } else {
                assertFalse(partRepairType.isPodSpace());
            }
        }
    }

    @Test
    void testIsUnknownLocation() {
        for (final PartRepairType partRepairType : types) {
            if (partRepairType == PartRepairType.UNKNOWN_LOCATION) {
                assertTrue(partRepairType.isUnknownLocation());
            } else {
                assertFalse(partRepairType.isUnknownLocation());
            }
        }
    }

    // endregion Boolean Comparison Methods
    @Test
    void testGetMRMSValidTypes() {
        final List<PartRepairType> mrmsValidTypes = PartRepairType.getMRMSValidTypes();
        for (final PartRepairType partRepairType : types) {
            switch (partRepairType) {
                case HEAT_SINK:
                case MEK_LOCATION:
                case PHYSICAL_WEAPON:
                case UNKNOWN_LOCATION:
                    assertFalse(mrmsValidTypes.contains(partRepairType));
                    break;
                default:
                    assertTrue(mrmsValidTypes.contains(partRepairType));
                    break;
            }
        }
    }

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(PartRepairType.WEAPON, PartRepairType.parseFromString("WEAPON"));
        assertEquals(PartRepairType.GENERAL, PartRepairType.parseFromString("GENERAL"));

        // Error Case
        assertEquals(PartRepairType.GENERAL_LOCATION, PartRepairType.parseFromString("13"));
        assertEquals(PartRepairType.GENERAL_LOCATION, PartRepairType.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("PartRepairType.ARMOUR.text"),
              PartRepairType.ARMOUR.toString());
        assertEquals(resources.getString("PartRepairType.AMMUNITION.text"),
              PartRepairType.AMMUNITION.toString());
        assertEquals(resources.getString("PartRepairType.HEAT_SINK.text"),
              PartRepairType.HEAT_SINK.toString());
    }
}
