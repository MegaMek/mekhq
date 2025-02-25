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

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import mekhq.MekHQ;

class ManeiDominiRankTest {
    // region Variable Declarations
    private static final ManeiDominiRank[] maneiDominiRanks = ManeiDominiRank.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Boolean Comparison Methods
    @Test
    void testIsNone() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.NONE) {
                assertTrue(maneiDominiRank.isNone());
            } else {
                assertFalse(maneiDominiRank.isNone());
            }
        }
    }

    @Test
    void testIsAlpha() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.ALPHA) {
                assertTrue(maneiDominiRank.isAlpha());
            } else {
                assertFalse(maneiDominiRank.isAlpha());
            }
        }
    }

    @Test
    void testIsBeta() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.BETA) {
                assertTrue(maneiDominiRank.isBeta());
            } else {
                assertFalse(maneiDominiRank.isBeta());
            }
        }
    }

    @Test
    void testIsOmega() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.OMEGA) {
                assertTrue(maneiDominiRank.isOmega());
            } else {
                assertFalse(maneiDominiRank.isOmega());
            }
        }
    }

    @Test
    void testIsTau() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.TAU) {
                assertTrue(maneiDominiRank.isTau());
            } else {
                assertFalse(maneiDominiRank.isTau());
            }
        }
    }

    @Test
    void testIsDelta() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.DELTA) {
                assertTrue(maneiDominiRank.isDelta());
            } else {
                assertFalse(maneiDominiRank.isDelta());
            }
        }
    }

    @Test
    void testIsSigma() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.SIGMA) {
                assertTrue(maneiDominiRank.isSigma());
            } else {
                assertFalse(maneiDominiRank.isSigma());
            }
        }
    }

    @Test
    void testIsOmicron() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.OMICRON) {
                assertTrue(maneiDominiRank.isOmicron());
            } else {
                assertFalse(maneiDominiRank.isOmicron());
            }
        }
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(ManeiDominiRank.NONE, ManeiDominiRank.parseFromString("NONE"));
        assertEquals(ManeiDominiRank.DELTA, ManeiDominiRank.parseFromString("DELTA"));

        // Error Case
        assertEquals(ManeiDominiRank.NONE, ManeiDominiRank.parseFromString("7"));
        assertEquals(ManeiDominiRank.NONE, ManeiDominiRank.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("ManeiDominiRank.NONE.text"), ManeiDominiRank.NONE.toString());
        assertEquals(resources.getString("ManeiDominiRank.OMICRON.text"), ManeiDominiRank.OMICRON.toString());
    }
}
