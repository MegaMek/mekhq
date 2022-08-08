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

public class ContractMarketMethodTest {
    //region Variable Declarations
    private static final ContractMarketMethod[] methods = ContractMarketMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
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
