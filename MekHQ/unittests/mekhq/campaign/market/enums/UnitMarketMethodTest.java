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
import mekhq.campaign.market.unitMarket.AtBMonthlyUnitMarket;
import mekhq.campaign.market.unitMarket.DisabledUnitMarket;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnitMarketMethodTest {
    //region Variable Declarations
    private static final UnitMarketMethod[] methods = UnitMarketMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("UnitMarketMethod.NONE.toolTipText"),
                UnitMarketMethod.NONE.getToolTipText());
        assertEquals(resources.getString("UnitMarketMethod.ATB_MONTHLY.toolTipText"),
                UnitMarketMethod.ATB_MONTHLY.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final UnitMarketMethod unitMarketMethod : methods) {
            if (unitMarketMethod == UnitMarketMethod.NONE) {
                assertTrue(unitMarketMethod.isNone());
            } else {
                assertFalse(unitMarketMethod.isNone());
            }
        }
    }

    @Test
    public void testIsAtBMonthly() {
        for (final UnitMarketMethod unitMarketMethod : methods) {
            if (unitMarketMethod == UnitMarketMethod.ATB_MONTHLY) {
                assertTrue(unitMarketMethod.isAtBMonthly());
            } else {
                assertFalse(unitMarketMethod.isAtBMonthly());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetUnitMarket() {
        assertTrue(UnitMarketMethod.NONE.getUnitMarket() instanceof DisabledUnitMarket);
        assertTrue(UnitMarketMethod.ATB_MONTHLY.getUnitMarket() instanceof AtBMonthlyUnitMarket);
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("UnitMarketMethod.NONE.text"),
                UnitMarketMethod.NONE.toString());
        assertEquals(resources.getString("UnitMarketMethod.ATB_MONTHLY.text"),
                UnitMarketMethod.ATB_MONTHLY.toString());
    }
}
