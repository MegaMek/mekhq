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

public class RandomDependentMethodTest {
    //region Variable Declarations
    private static final RandomDependentMethod[] methods = RandomDependentMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("RandomDependentMethod.NONE.toolTipText"),
                RandomDependentMethod.NONE.getToolTipText());
        assertEquals(resources.getString("RandomDependentMethod.AGAINST_THE_BOT.toolTipText"),
                RandomDependentMethod.AGAINST_THE_BOT.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final RandomDependentMethod randomDependentMethod : methods) {
            if (randomDependentMethod == RandomDependentMethod.NONE) {
                assertTrue(randomDependentMethod.isNone());
            } else {
                assertFalse(randomDependentMethod.isNone());
            }
        }
    }

    @Test
    public void testIsAgainstTheBot() {
        for (final RandomDependentMethod randomDependentMethod : methods) {
            if (randomDependentMethod == RandomDependentMethod.AGAINST_THE_BOT) {
                assertTrue(randomDependentMethod.isAgainstTheBot());
            } else {
                assertFalse(randomDependentMethod.isAgainstTheBot());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomDependentMethod.NONE.text"),
                RandomDependentMethod.NONE.toString());
        assertEquals(resources.getString("RandomDependentMethod.AGAINST_THE_BOT.text"),
                RandomDependentMethod.AGAINST_THE_BOT.toString());
    }
}
