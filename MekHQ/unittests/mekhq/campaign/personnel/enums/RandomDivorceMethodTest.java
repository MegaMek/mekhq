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
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.divorce.DisabledRandomDivorce;
import mekhq.campaign.personnel.divorce.RandomDivorce;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RandomDivorceMethodTest {
    //region Variable Declarations
    private static final RandomDivorceMethod[] methods = RandomDivorceMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        String expected = wordWrap(resources.getString("RandomDivorceMethod.NONE.toolTipText")).replaceAll("\\s", "");
        String actual = wordWrap(RandomDivorceMethod.NONE.getToolTipText().trim()).replaceAll("\\s", "");

        assertEquals(expected, actual);
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final RandomDivorceMethod randomDivorceMethod : methods) {
            if (randomDivorceMethod == RandomDivorceMethod.NONE) {
                assertTrue(randomDivorceMethod.isNone());
            } else {
                assertFalse(randomDivorceMethod.isNone());
            }
        }
    }

    @Test
    public void testIsDiceRoll() {
        for (final RandomDivorceMethod randomDivorceMethod : methods) {
            if (randomDivorceMethod == RandomDivorceMethod.DICE_ROLL) {
                assertTrue(randomDivorceMethod.isDiceRoll());
            } else {
                assertFalse(randomDivorceMethod.isDiceRoll());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetMethod() {
        final CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockOptions.isUseClanPersonnelDivorce()).thenReturn(false);
        when(mockOptions.isUsePrisonerDivorce()).thenReturn(false);
        when(mockOptions.isUseRandomOppositeSexDivorce()).thenReturn(false);
        when(mockOptions.isUseRandomSameSexDivorce()).thenReturn(false);
        when(mockOptions.isUseRandomClanPersonnelDivorce()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerDivorce()).thenReturn(false);
        when(mockOptions.getRandomDivorceDiceSize()).thenReturn(5);

        assertInstanceOf(DisabledRandomDivorce.class, RandomDivorceMethod.NONE.getMethod(mockOptions));
        assertInstanceOf(RandomDivorce.class, RandomDivorceMethod.DICE_ROLL.getMethod(mockOptions));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomDivorceMethod.NONE.text"),
                RandomDivorceMethod.NONE.toString());
        assertEquals(resources.getString("RandomDivorceMethod.DICE_ROLL.text"),
                RandomDivorceMethod.DICE_ROLL.toString());
    }
}
