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

public class ROMDesignationTest {
    //region Variable Declarations
    private static final ROMDesignation[] designations = ROMDesignation.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.NONE) {
                assertTrue(designation.isNone());
            } else {
                assertFalse(designation.isNone());
            }
        }
    }

    @Test
    public void testIsEpsilon() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.EPSILON) {
                assertTrue(designation.isEpsilon());
            } else {
                assertFalse(designation.isEpsilon());
            }
        }
    }

    @Test
    public void testIsPi() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.PI) {
                assertTrue(designation.isPi());
            } else {
                assertFalse(designation.isPi());
            }
        }
    }

    @Test
    public void testIsIota() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.IOTA) {
                assertTrue(designation.isIota());
            } else {
                assertFalse(designation.isIota());
            }
        }
    }

    @Test
    public void testIsXi() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.XI) {
                assertTrue(designation.isXi());
            } else {
                assertFalse(designation.isXi());
            }
        }
    }

    @Test
    public void testIsTheta() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.THETA) {
                assertTrue(designation.isTheta());
            } else {
                assertFalse(designation.isTheta());
            }
        }
    }

    @Test
    public void testIsZeta() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.ZETA) {
                assertTrue(designation.isZeta());
            } else {
                assertFalse(designation.isZeta());
            }
        }
    }

    @Test
    public void testIsMu() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.MU) {
                assertTrue(designation.isMu());
            } else {
                assertFalse(designation.isMu());
            }
        }
    }

    @Test
    public void testIsRho() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.RHO) {
                assertTrue(designation.isRho());
            } else {
                assertFalse(designation.isRho());
            }
        }
    }

    @Test
    public void testIsLambda() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.LAMBDA) {
                assertTrue(designation.isLambda());
            } else {
                assertFalse(designation.isLambda());
            }
        }
    }

    @Test
    public void testIsPsi() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.PSI) {
                assertTrue(designation.isPsi());
            } else {
                assertFalse(designation.isPsi());
            }
        }
    }

    @Test
    public void testIsOmicron() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.OMICRON) {
                assertTrue(designation.isOmicron());
            } else {
                assertFalse(designation.isOmicron());
            }
        }
    }

    @Test
    public void testIsChi() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.CHI) {
                assertTrue(designation.isChi());
            } else {
                assertFalse(designation.isChi());
            }
        }
    }

    @Test
    public void testIsGamma() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.GAMMA) {
                assertTrue(designation.isGamma());
            } else {
                assertFalse(designation.isGamma());
            }
        }
    }

    @Test
    public void testIsKappa() {
        for (final ROMDesignation designation : designations) {
            if (designation == ROMDesignation.KAPPA) {
                assertTrue(designation.isKappa());
            } else {
                assertFalse(designation.isKappa());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetComStarBranchDesignation() {
        // FIXME : Windchild : ADD
    }

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(ROMDesignation.NONE, ROMDesignation.parseFromString("NONE"));
        assertEquals(ROMDesignation.LAMBDA, ROMDesignation.parseFromString("LAMBDA"));

        // Legacy Parsing
        assertEquals(ROMDesignation.NONE, ROMDesignation.parseFromString("0"));
        assertEquals(ROMDesignation.EPSILON, ROMDesignation.parseFromString("1"));

        // Error Case
        assertEquals(ROMDesignation.NONE, ROMDesignation.parseFromString("15"));
        assertEquals(ROMDesignation.NONE, ROMDesignation.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("ROMDesignation.NONE.text"), ROMDesignation.NONE.toString());
        assertEquals(resources.getString("ROMDesignation.KAPPA.text"), ROMDesignation.KAPPA.toString());
    }
}
