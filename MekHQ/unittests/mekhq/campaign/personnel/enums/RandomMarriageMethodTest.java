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
import mekhq.campaign.personnel.marriage.DisabledRandomMarriage;
import mekhq.campaign.personnel.marriage.PercentageRandomMarriage;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RandomMarriageMethodTest {
    //region Variable Declarations
    private static final RandomMarriageMethod[] methods = RandomMarriageMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("RandomMarriageMethod.NONE.toolTipText"),
                RandomMarriageMethod.NONE.getToolTipText());
        assertEquals(resources.getString("RandomMarriageMethod.PERCENTAGE.toolTipText"),
                RandomMarriageMethod.PERCENTAGE.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final RandomMarriageMethod randomMarriageMethod : methods) {
            if (randomMarriageMethod == RandomMarriageMethod.NONE) {
                assertTrue(randomMarriageMethod.isNone());
            } else {
                assertFalse(randomMarriageMethod.isNone());
            }
        }
    }

    @Test
    public void testIsPercentage() {
        for (final RandomMarriageMethod randomMarriageMethod : methods) {
            if (randomMarriageMethod == RandomMarriageMethod.PERCENTAGE) {
                assertTrue(randomMarriageMethod.isPercentage());
            } else {
                assertFalse(randomMarriageMethod.isPercentage());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetMethod() {
        final CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockOptions.isUseClannerMarriages()).thenReturn(false);
        when(mockOptions.isUsePrisonerMarriages()).thenReturn(false);
        when(mockOptions.isUseRandomSameSexMarriages()).thenReturn(false);
        when(mockOptions.isUseRandomClannerMarriages()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerMarriages()).thenReturn(false);
        when(mockOptions.getPercentageRandomMarriageOppositeSexChance()).thenReturn(0.5);
        when(mockOptions.getPercentageRandomMarriageSameSexChance()).thenReturn(0.5);

        assertInstanceOf(DisabledRandomMarriage.class, RandomMarriageMethod.NONE.getMethod(mockOptions));
        assertInstanceOf(PercentageRandomMarriage.class, RandomMarriageMethod.PERCENTAGE.getMethod(mockOptions));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomMarriageMethod.NONE.text"),
                RandomMarriageMethod.NONE.toString());
        assertEquals(resources.getString("RandomMarriageMethod.PERCENTAGE.text"),
                RandomMarriageMethod.PERCENTAGE.toString());
    }
}
