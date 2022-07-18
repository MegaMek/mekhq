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
package mekhq.campaign.market.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnitMarketTypeTest {
    //region Variable Declarations
    private static final UnitMarketType[] types = UnitMarketType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsOpen() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.OPEN) {
                assertTrue(unitMarketType.isOpen());
            } else {
                assertFalse(unitMarketType.isOpen());
            }
        }
    }

    @Test
    public void testIsEmployer() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.EMPLOYER) {
                assertTrue(unitMarketType.isEmployer());
            } else {
                assertFalse(unitMarketType.isEmployer());
            }
        }
    }

    @Test
    public void testIsMercenary() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.MERCENARY) {
                assertTrue(unitMarketType.isMercenary());
            } else {
                assertFalse(unitMarketType.isMercenary());
            }
        }
    }

    @Test
    public void testIsFactory() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.FACTORY) {
                assertTrue(unitMarketType.isFactory());
            } else {
                assertFalse(unitMarketType.isFactory());
            }
        }
    }

    @Test
    public void testIsBlackMarket() {
        for (final UnitMarketType unitMarketType : types) {
            if (unitMarketType == UnitMarketType.BLACK_MARKET) {
                assertTrue(unitMarketType.isBlackMarket());
            } else {
                assertFalse(unitMarketType.isBlackMarket());
            }
        }
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(UnitMarketType.OPEN, UnitMarketType.parseFromString("OPEN"));
        assertEquals(UnitMarketType.FACTORY, UnitMarketType.parseFromString("FACTORY"));

        // Legacy Parsing
        assertEquals(UnitMarketType.OPEN, UnitMarketType.parseFromString("0"));
        assertEquals(UnitMarketType.EMPLOYER, UnitMarketType.parseFromString("1"));
        assertEquals(UnitMarketType.MERCENARY, UnitMarketType.parseFromString("2"));
        assertEquals(UnitMarketType.FACTORY, UnitMarketType.parseFromString("3"));
        assertEquals(UnitMarketType.BLACK_MARKET, UnitMarketType.parseFromString("4"));

        // Error Case
        assertEquals(UnitMarketType.OPEN, UnitMarketType.parseFromString("5"));
        assertEquals(UnitMarketType.OPEN, UnitMarketType.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("UnitMarketType.EMPLOYER.text"),
                UnitMarketType.EMPLOYER.toString());
        assertEquals(resources.getString("UnitMarketType.BLACK_MARKET.text"),
                UnitMarketType.BLACK_MARKET.toString());
    }
}
