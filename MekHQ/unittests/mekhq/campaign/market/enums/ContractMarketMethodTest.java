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
package mekhq.campaign.market.enums;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContractMarketMethodTest {
    //region Variable Declarations
    private static final ContractMarketMethod[] methods = ContractMarketMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("ContractMarketMethod.NONE.toolTipText"),
                ContractMarketMethod.NONE.getToolTipText());
        assertEquals(resources.getString("ContractMarketMethod.ATB_MONTHLY.toolTipText"),
                ContractMarketMethod.ATB_MONTHLY.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final ContractMarketMethod contractMarketMethod : methods) {
            if (contractMarketMethod == ContractMarketMethod.NONE) {
                assertTrue(contractMarketMethod.isNone());
            } else {
                assertFalse(contractMarketMethod.isNone());
            }
        }
    }

    @Test
    public void testIsAtBMonthly() {
        for (final ContractMarketMethod contractMarketMethod : methods) {
            if (contractMarketMethod == ContractMarketMethod.ATB_MONTHLY) {
                assertTrue(contractMarketMethod.isAtBMonthly());
            } else {
                assertFalse(contractMarketMethod.isAtBMonthly());
            }
        }
    }
    //endregion Boolean Comparison Methods

/*
    @Test
    public void testGetContractMarket() {
        assertInstanceOf(DisabledContractMarket.class, ContractMarketMethod.NONE.getContractMarket());
        assertInstanceOf(AtBMonthlyContractMarket.class, ContractMarketMethod.ATB_MONTHLY.getContractMarket());
    }
*/

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("ContractMarketMethod.NONE.text"),
                ContractMarketMethod.NONE.toString());
        assertEquals(resources.getString("ContractMarketMethod.ATB_MONTHLY.text"),
                ContractMarketMethod.ATB_MONTHLY.toString());
    }
}
