/*
 * Copyright (c) 2022-2025 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.personnel.death.DisabledRandomDeath;
import mekhq.campaign.personnel.death.ExponentialRandomDeath;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RandomDeathMethodTest {
    //region Variable Declarations
    private static final RandomDeathMethod[] methods = RandomDeathMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("RandomDeathMethod.NONE.toolTipText"),
                RandomDeathMethod.NONE.getToolTipText());
        assertEquals(resources.getString("RandomDeathMethod.RANDOM.toolTipText"),
                RandomDeathMethod.RANDOM.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final RandomDeathMethod randomDeathMethod : methods) {
            if (randomDeathMethod == RandomDeathMethod.NONE) {
                assertTrue(randomDeathMethod.isNone());
            } else {
                assertFalse(randomDeathMethod.isNone());
            }
        }
    }

    @Test
    public void testIsDiceRoll() {
        for (final RandomDeathMethod randomDeathMethod : methods) {
            if (randomDeathMethod == RandomDeathMethod.RANDOM) {
                assertTrue(randomDeathMethod.isRandom());
            } else {
                assertFalse(randomDeathMethod.isRandom());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetMethod() {
        final CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockOptions.getEnabledRandomDeathAgeGroups()).thenReturn(new HashMap<>());
        when(mockOptions.isUseRandomDeathSuicideCause()).thenReturn(false);
        when(mockOptions.getExponentialRandomDeathMaleValues()).thenReturn(new double[] { 1d });
        when(mockOptions.getExponentialRandomDeathFemaleValues()).thenReturn(new double[] { 1d });

        final Map<TenYearAgeRange, Double> ageRangeMap = new HashMap<>();
        for (final TenYearAgeRange range : TenYearAgeRange.values()) {
            ageRangeMap.put(range, 1d);
        }

        assertInstanceOf(DisabledRandomDeath.class, RandomDeathMethod.NONE.getMethod(mockOptions));
        assertInstanceOf(ExponentialRandomDeath.class, RandomDeathMethod.RANDOM.getMethod(mockOptions, false));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomDeathMethod.NONE.text"),
                RandomDeathMethod.NONE.toString());
        assertEquals(resources.getString("RandomDeathMethod.RANDOM.text"),
                RandomDeathMethod.RANDOM.toString());
    }
}
