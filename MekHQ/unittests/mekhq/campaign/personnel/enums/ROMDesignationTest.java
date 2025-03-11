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
 */
package mekhq.campaign.personnel.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ResourceBundle;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import megamek.common.BipedMek;
import megamek.common.Dropship;
import megamek.common.EquipmentType;
import megamek.common.Jumpship;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

class ROMDesignationTest {
    // region Variable Declarations
    private static final ROMDesignation[] designations = ROMDesignation.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    @BeforeAll
    public static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    // region Boolean Comparison Methods
    @Test
    void testIsNone() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.NONE) {
                assertTrue(designation.isNone());
            } else {
                assertFalse(designation.isNone());
            }
        }
    }

    @Test
    void testIsEpsilon() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.EPSILON) {
                assertTrue(designation.isEpsilon());
            } else {
                assertFalse(designation.isEpsilon());
            }
        }
    }

    @Test
    void testIsPi() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.PI) {
                assertTrue(designation.isPi());
            } else {
                assertFalse(designation.isPi());
            }
        }
    }

    @Test
    void testIsIota() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.IOTA) {
                assertTrue(designation.isIota());
            } else {
                assertFalse(designation.isIota());
            }
        }
    }

    @Test
    void testIsXi() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.XI) {
                assertTrue(designation.isXi());
            } else {
                assertFalse(designation.isXi());
            }
        }
    }

    @Test
    void testIsTheta() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.THETA) {
                assertTrue(designation.isTheta());
            } else {
                assertFalse(designation.isTheta());
            }
        }
    }

    @Test
    void testIsZeta() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.ZETA) {
                assertTrue(designation.isZeta());
            } else {
                assertFalse(designation.isZeta());
            }
        }
    }

    @Test
    void testIsMu() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.MU) {
                assertTrue(designation.isMu());
            } else {
                assertFalse(designation.isMu());
            }
        }
    }

    @Test
    void testIsRho() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.RHO) {
                assertTrue(designation.isRho());
            } else {
                assertFalse(designation.isRho());
            }
        }
    }

    @Test
    void testIsLambda() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.LAMBDA) {
                assertTrue(designation.isLambda());
            } else {
                assertFalse(designation.isLambda());
            }
        }
    }

    @Test
    void testIsPsi() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.PSI) {
                assertTrue(designation.isPsi());
            } else {
                assertFalse(designation.isPsi());
            }
        }
    }

    @Test
    void testIsOmicron() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.OMICRON) {
                assertTrue(designation.isOmicron());
            } else {
                assertFalse(designation.isOmicron());
            }
        }
    }

    @Test
    void testIsChi() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.CHI) {
                assertTrue(designation.isChi());
            } else {
                assertFalse(designation.isChi());
            }
        }
    }

    @Test
    void testIsGamma() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.GAMMA) {
                assertTrue(designation.isGamma());
            } else {
                assertFalse(designation.isGamma());
            }
        }
    }

    @Test
    void testIsKappa() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.KAPPA) {
                assertTrue(designation.isKappa());
            } else {
                assertFalse(designation.isKappa());
            }
        }
    }
    // endregion Boolean Comparison Methods

    @Test
    void testGetComStarBranchDesignation() {
        final Unit mockUnit = mock(Unit.class);

        final Person mockPerson = mock(Person.class);
        when(mockPerson.getPrimaryDesignator()).thenReturn(ROMDesignation.NONE);
        when(mockPerson.getSecondaryDesignator()).thenReturn(ROMDesignation.NONE);
        when(mockPerson.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(mockPerson.getUnit()).thenReturn(mockUnit);

        // No ROM Designations nor Secondary Role
        // MekWarrior- Expect " Epsilon"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        assertEquals(" " + ROMDesignation.EPSILON, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // LAM Pilot - Expect " Epsilon"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.LAM_PILOT);
        assertEquals(" " + ROMDesignation.EPSILON, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Ground Vehicle Driver - Expect " Lambda"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.GROUND_VEHICLE_DRIVER);
        assertEquals(" " + ROMDesignation.LAMBDA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Naval Vehicle Driver - Expect " Lambda"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.NAVAL_VEHICLE_DRIVER);
        assertEquals(" " + ROMDesignation.LAMBDA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // VTOL Pilot - Expect " Lambda"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.VTOL_PILOT);
        assertEquals(" " + ROMDesignation.LAMBDA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Vehicle Gunner - Expect " Lambda"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_GUNNER);
        assertEquals(" " + ROMDesignation.LAMBDA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Vehicle Crew - Expect " Lambda"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.VEHICLE_CREW);
        assertEquals(" " + ROMDesignation.LAMBDA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Conventional Aircraft Pilot - Expect " Lambda"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT);
        assertEquals(" " + ROMDesignation.LAMBDA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Aerospace Pilot - Expect " Pi"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.AEROSPACE_PILOT);
        assertEquals(" " + ROMDesignation.PI, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Battle Armour - Expect " Iota"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.BATTLE_ARMOUR);
        assertEquals(" " + ROMDesignation.IOTA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Soldier - Expect " Iota"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.SOLDIER);
        assertEquals(" " + ROMDesignation.IOTA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Vessel Pilot, DropShip - Expect " Xi"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.VESSEL_PILOT);
        when(mockUnit.getEntity()).thenReturn(new Dropship());
        assertEquals(" " + ROMDesignation.XI, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Vessel Gunner, JumpShip - Expect " Theta"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.VESSEL_GUNNER);
        when(mockUnit.getEntity()).thenReturn(new Jumpship());
        assertEquals(" " + ROMDesignation.THETA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Vessel Crew, Biped Mek - Expect " "
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.VESSEL_CREW);
        when(mockUnit.getEntity()).thenReturn(new BipedMek());
        assertEquals(" ", ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Vessel Navigator, Null Unit - Expect " "
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.VESSEL_NAVIGATOR);
        when(mockPerson.getUnit()).thenReturn(null);
        assertEquals(" ", ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Mek Tech - Expect " Zeta"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        assertEquals(" " + ROMDesignation.ZETA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Mechanic - Expect " Zeta"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.MECHANIC);
        assertEquals(" " + ROMDesignation.ZETA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Aero Tech - Expect " Zeta"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.AERO_TEK);
        assertEquals(" " + ROMDesignation.ZETA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // BA Tech - Expect " Zeta"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.BA_TECH);
        assertEquals(" " + ROMDesignation.ZETA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Astech - Expect " Zeta"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.ASTECH);
        assertEquals(" " + ROMDesignation.ZETA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Doctor - Expect " Kappa"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.DOCTOR);
        assertEquals(" " + ROMDesignation.KAPPA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Medic - Expect " Kappa"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.MEDIC);
        assertEquals(" " + ROMDesignation.KAPPA, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Administrator (Command) - Expect " Chi"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_COMMAND);
        assertEquals(" " + ROMDesignation.CHI, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Administrator (Logistics) - Expect " Chi"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_LOGISTICS);
        assertEquals(" " + ROMDesignation.CHI, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Administrator (Transport) - Expect " Chi"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_TRANSPORT);
        assertEquals(" " + ROMDesignation.CHI, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Administrator (HR) - Expect " Chi"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_HR);
        assertEquals(" " + ROMDesignation.CHI, ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Dependent - Expect " "
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.DEPENDENT);
        assertEquals(" ", ROMDesignation.getComStarBranchDesignation(mockPerson));

        // MekWarrior / Administrator (Command) - Expect " Epsilon Chi"
        when(mockPerson.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(mockPerson.getSecondaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_COMMAND);
        assertEquals(" " + ROMDesignation.EPSILON + ' ' + ROMDesignation.CHI,
                ROMDesignation.getComStarBranchDesignation(mockPerson));

        // Both Designators Set - Zetta Kappa - Expect " Zeta Kappa"
        when(mockPerson.getPrimaryDesignator()).thenReturn(ROMDesignation.ZETA);
        when(mockPerson.getSecondaryDesignator()).thenReturn(ROMDesignation.KAPPA);
        assertEquals(" " + ROMDesignation.ZETA + ' ' + ROMDesignation.KAPPA,
                ROMDesignation.getComStarBranchDesignation(mockPerson));
    }

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(ROMDesignation.NONE, ROMDesignation.parseFromString("NONE"));
        assertEquals(ROMDesignation.LAMBDA, ROMDesignation.parseFromString("LAMBDA"));

        // Error Case
        assertEquals(ROMDesignation.NONE, ROMDesignation.parseFromString("15"));
        assertEquals(ROMDesignation.NONE, ROMDesignation.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("ROMDesignation.NONE.text"), ROMDesignation.NONE.toString());
        assertEquals(resources.getString("ROMDesignation.KAPPA.text"), ROMDesignation.KAPPA.toString());
    }
}
