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

public class ManeiDominiRankTest {
    //region Variable Declarations
    private static final ManeiDominiRank[] maneiDominiRanks = ManeiDominiRank.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.NONE) {
                assertTrue(maneiDominiRank.isNone());
            } else {
                assertFalse(maneiDominiRank.isNone());
            }
        }
    }

    @Test
    public void testIsAlpha() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.ALPHA) {
                assertTrue(maneiDominiRank.isAlpha());
            } else {
                assertFalse(maneiDominiRank.isAlpha());
            }
        }
    }

    @Test
    public void testIsBeta() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.BETA) {
                assertTrue(maneiDominiRank.isBeta());
            } else {
                assertFalse(maneiDominiRank.isBeta());
            }
        }
    }

    @Test
    public void testIsOmega() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.OMEGA) {
                assertTrue(maneiDominiRank.isOmega());
            } else {
                assertFalse(maneiDominiRank.isOmega());
            }
        }
    }

    @Test
    public void testIsTau() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.TAU) {
                assertTrue(maneiDominiRank.isTau());
            } else {
                assertFalse(maneiDominiRank.isTau());
            }
        }
    }

    @Test
    public void testIsDelta() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.DELTA) {
                assertTrue(maneiDominiRank.isDelta());
            } else {
                assertFalse(maneiDominiRank.isDelta());
            }
        }
    }

    @Test
    public void testIsSigma() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.SIGMA) {
                assertTrue(maneiDominiRank.isSigma());
            } else {
                assertFalse(maneiDominiRank.isSigma());
            }
        }
    }

    @Test
    public void testIsOmicron() {
        for (final ManeiDominiRank maneiDominiRank : maneiDominiRanks) {
            if (maneiDominiRank == ManeiDominiRank.OMICRON) {
                assertTrue(maneiDominiRank.isOmicron());
            } else {
                assertFalse(maneiDominiRank.isOmicron());
            }
        }
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(ManeiDominiRank.NONE, ManeiDominiRank.parseFromString("NONE"));
        assertEquals(ManeiDominiRank.DELTA, ManeiDominiRank.parseFromString("DELTA"));

        // Legacy Parsing
        assertEquals(ManeiDominiRank.ALPHA, ManeiDominiRank.parseFromString("0"));
        assertEquals(ManeiDominiRank.OMICRON, ManeiDominiRank.parseFromString("6"));

        // Error Case
        assertEquals(ManeiDominiRank.NONE, ManeiDominiRank.parseFromString("7"));
        assertEquals(ManeiDominiRank.NONE, ManeiDominiRank.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("ManeiDominiRank.NONE.text"), ManeiDominiRank.NONE.toString());
        assertEquals(resources.getString("ManeiDominiRank.OMICRON.text"), ManeiDominiRank.OMICRON.toString());
    }
}
