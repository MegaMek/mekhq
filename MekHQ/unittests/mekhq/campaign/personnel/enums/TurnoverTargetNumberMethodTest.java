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

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TurnoverTargetNumberMethodTest {
    //region Variable Declarations
    private static final TurnoverTargetNumberMethod[] methods = TurnoverTargetNumberMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("TurnoverTargetNumberMethod.FIXED.toolTipText"),
                TurnoverTargetNumberMethod.FIXED.getToolTipText());
        assertEquals(resources.getString("TurnoverTargetNumberMethod.ADMINISTRATION.toolTipText"),
                TurnoverTargetNumberMethod.ADMINISTRATION.getToolTipText());
        assertEquals(resources.getString("TurnoverTargetNumberMethod.NEGOTIATION.toolTipText"),
                TurnoverTargetNumberMethod.NEGOTIATION.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsFixed() {
        for (final TurnoverTargetNumberMethod TurnoverTargetNumberMethod : methods) {
            if (TurnoverTargetNumberMethod == mekhq.campaign.personnel.enums.TurnoverTargetNumberMethod.FIXED) {
                assertTrue(TurnoverTargetNumberMethod.isFixed());
            } else {
                assertFalse(TurnoverTargetNumberMethod.isFixed());
            }
        }
    }

    @Test
    public void testIsAdministration() {
        for (final TurnoverTargetNumberMethod TurnoverTargetNumberMethod : methods) {
            if (TurnoverTargetNumberMethod == mekhq.campaign.personnel.enums.TurnoverTargetNumberMethod.ADMINISTRATION) {
                assertTrue(TurnoverTargetNumberMethod.isAdministration());
            } else {
                assertFalse(TurnoverTargetNumberMethod.isAdministration());
            }
        }
    }

    @Test
    public void testIsNegotiation() {
        for (final TurnoverTargetNumberMethod TurnoverTargetNumberMethod : methods) {
            if (TurnoverTargetNumberMethod == mekhq.campaign.personnel.enums.TurnoverTargetNumberMethod.NEGOTIATION) {
                assertTrue(TurnoverTargetNumberMethod.isNegotiation());
            } else {
                assertFalse(TurnoverTargetNumberMethod.isNegotiation());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("TurnoverTargetNumberMethod.FIXED.text"),
                TurnoverTargetNumberMethod.FIXED.toString());
        assertEquals(resources.getString("TurnoverTargetNumberMethod.ADMINISTRATION.text"),
                TurnoverTargetNumberMethod.ADMINISTRATION.toString());
        assertEquals(resources.getString("TurnoverTargetNumberMethod.NEGOTIATION.text"),
                TurnoverTargetNumberMethod.NEGOTIATION.toString());
    }
}
