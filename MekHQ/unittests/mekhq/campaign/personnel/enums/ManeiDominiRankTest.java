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
package mekhq.campaign.personnel.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

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
