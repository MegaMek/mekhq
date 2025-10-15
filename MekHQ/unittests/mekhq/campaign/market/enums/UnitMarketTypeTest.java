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
package mekhq.campaign.market.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

class UnitMarketTypeTest {
    // region Variable Declarations
    private static final UnitMarketType[] types = UnitMarketType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Boolean Comparison Methods
    @Test
    void testIsOpen() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.OPEN) {
                assertTrue(unitMarketType.isOpen());
            } else {
                assertFalse(unitMarketType.isOpen());
            }
        }
    }

    @Test
    void testIsEmployer() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.EMPLOYER) {
                assertTrue(unitMarketType.isEmployer());
            } else {
                assertFalse(unitMarketType.isEmployer());
            }
        }
    }

    @Test
    void testIsMercenary() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.MERCENARY) {
                assertTrue(unitMarketType.isMercenary());
            } else {
                assertFalse(unitMarketType.isMercenary());
            }
        }
    }

    @Test
    void testIsFactory() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.FACTORY) {
                assertTrue(unitMarketType.isFactory());
            } else {
                assertFalse(unitMarketType.isFactory());
            }
        }
    }

    @Test
    void testIsBlackMarket() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.BLACK_MARKET) {
                assertTrue(unitMarketType.isBlackMarket());
            } else {
                assertFalse(unitMarketType.isBlackMarket());
            }
        }
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(UnitMarketType.OPEN, UnitMarketType.parseFromString("OPEN"));
        assertEquals(UnitMarketType.FACTORY, UnitMarketType.parseFromString("FACTORY"));

        // Error Case
        assertEquals(UnitMarketType.OPEN, UnitMarketType.parseFromString("7"));
        assertEquals(UnitMarketType.OPEN, UnitMarketType.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("UnitMarketType.EMPLOYER.text"),
              UnitMarketType.EMPLOYER.toString());
        assertEquals(resources.getString("UnitMarketType.BLACK_MARKET.text"),
              UnitMarketType.BLACK_MARKET.toString());
    }
}
