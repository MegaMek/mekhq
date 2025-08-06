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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import mekhq.campaign.market.unitMarket.AtBMonthlyUnitMarket;
import mekhq.campaign.market.unitMarket.DisabledUnitMarket;
import org.junit.jupiter.api.Test;

public class UnitMarketMethodTest {
    //region Variable Declarations
    private static final UnitMarketMethod[] methods = UnitMarketMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
          MekHQ.getMHQOptions().getLocale());
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
        assertInstanceOf(DisabledUnitMarket.class, UnitMarketMethod.NONE.getUnitMarket());
        assertInstanceOf(AtBMonthlyUnitMarket.class, UnitMarketMethod.ATB_MONTHLY.getUnitMarket());
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("UnitMarketMethod.NONE.text"),
              UnitMarketMethod.NONE.toString());
        assertEquals(resources.getString("UnitMarketMethod.ATB_MONTHLY.text"),
              UnitMarketMethod.ATB_MONTHLY.toString());
    }
}
