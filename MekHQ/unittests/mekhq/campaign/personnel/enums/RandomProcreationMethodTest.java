/*
 * Copyright (c) 2022-2024 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.procreation.DisabledRandomProcreation;
import mekhq.campaign.personnel.procreation.RandomProcreation;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RandomProcreationMethodTest {
    //region Variable Declarations
    private static final RandomProcreationMethod[] methods = RandomProcreationMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("RandomProcreationMethod.NONE.toolTipText").trim(),
                RandomProcreationMethod.NONE.getToolTipText().trim());
        assertEquals(resources.getString("RandomProcreationMethod.DICE_ROLL.toolTipText").trim(),
                RandomProcreationMethod.DICE_ROLL.getToolTipText().trim());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final RandomProcreationMethod randomProcreationMethod : methods) {
            if (randomProcreationMethod == RandomProcreationMethod.NONE) {
                assertTrue(randomProcreationMethod.isNone());
            } else {
                assertFalse(randomProcreationMethod.isNone());
            }
        }
    }

    @Test
    public void testIsPercentage() {
        for (final RandomProcreationMethod randomProcreationMethod : methods) {
            if (randomProcreationMethod == RandomProcreationMethod.DICE_ROLL) {
                assertTrue(randomProcreationMethod.isDiceRoll());
            } else {
                assertFalse(randomProcreationMethod.isDiceRoll());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetMethod() {
        final CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockOptions.isUseClanPersonnelProcreation()).thenReturn(false);
        when(mockOptions.isUsePrisonerProcreation()).thenReturn(false);
        when(mockOptions.isUseRelationshiplessRandomProcreation()).thenReturn(false);
        when(mockOptions.isUseRandomClanPersonnelProcreation()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerProcreation()).thenReturn(false);
        when(mockOptions.getRandomProcreationRelationshipDiceSize()).thenReturn(5);
        when(mockOptions.getRandomProcreationRelationshiplessDiceSize()).thenReturn(5);

        assertInstanceOf(DisabledRandomProcreation.class, RandomProcreationMethod.NONE.getMethod(mockOptions));
        assertInstanceOf(RandomProcreation.class, RandomProcreationMethod.DICE_ROLL.getMethod(mockOptions));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomProcreationMethod.NONE.text"),
                RandomProcreationMethod.NONE.toString());
        assertEquals(resources.getString("RandomProcreationMethod.DICE_ROLL.text"),
                RandomProcreationMethod.DICE_ROLL.toString());
    }
}
