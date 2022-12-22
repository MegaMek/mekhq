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

public class RandomRetirementMethodTest {
    //region Variable Declarations
    private static final RandomRetirementMethod[] methods = RandomRetirementMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("RandomRetirementMethod.NONE.toolTipText"),
                RandomRetirementMethod.NONE.getToolTipText());
        assertEquals(resources.getString("RandomRetirementMethod.AGAINST_THE_BOT.toolTipText"),
                RandomRetirementMethod.AGAINST_THE_BOT.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final RandomRetirementMethod randomRetirementMethod : methods) {
            if (randomRetirementMethod == RandomRetirementMethod.NONE) {
                assertTrue(randomRetirementMethod.isNone());
            } else {
                assertFalse(randomRetirementMethod.isNone());
            }
        }
    }

    @Test
    public void testIsAgainstTheBot() {
        for (final RandomRetirementMethod randomRetirementMethod : methods) {
            if (randomRetirementMethod == RandomRetirementMethod.AGAINST_THE_BOT) {
                assertTrue(randomRetirementMethod.isAgainstTheBot());
            } else {
                assertFalse(randomRetirementMethod.isAgainstTheBot());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomRetirementMethod.NONE.text"),
                RandomRetirementMethod.NONE.toString());
        assertEquals(resources.getString("RandomRetirementMethod.AGAINST_THE_BOT.text"),
                RandomRetirementMethod.AGAINST_THE_BOT.toString());
    }
}
