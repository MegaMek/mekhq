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
import mekhq.campaign.personnel.death.AgeRangeRandomDeath;
import mekhq.campaign.personnel.death.DisabledRandomDeath;
import mekhq.campaign.personnel.death.ExponentialRandomDeath;
import mekhq.campaign.personnel.death.PercentageRandomDeath;
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
        assertEquals(resources.getString("RandomDeathMethod.AGE_RANGE.toolTipText"),
                RandomDeathMethod.AGE_RANGE.getToolTipText());
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
    public void testIsPercentage() {
        for (final RandomDeathMethod randomDeathMethod : methods) {
            if (randomDeathMethod == RandomDeathMethod.PERCENTAGE) {
                assertTrue(randomDeathMethod.isPercentage());
            } else {
                assertFalse(randomDeathMethod.isPercentage());
            }
        }
    }

    @Test
    public void testIsExponential() {
        for (final RandomDeathMethod randomDeathMethod : methods) {
            if (randomDeathMethod == RandomDeathMethod.EXPONENTIAL) {
                assertTrue(randomDeathMethod.isExponential());
            } else {
                assertFalse(randomDeathMethod.isExponential());
            }
        }
    }

    @Test
    public void testIsAgeRange() {
        for (final RandomDeathMethod randomDeathMethod : methods) {
            if (randomDeathMethod == RandomDeathMethod.AGE_RANGE) {
                assertTrue(randomDeathMethod.isAgeRange());
            } else {
                assertFalse(randomDeathMethod.isAgeRange());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetMethod() {
        final CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockOptions.getEnabledRandomDeathAgeGroups()).thenReturn(new HashMap<>());
        when(mockOptions.isUseRandomClanPersonnelDeath()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerDeath()).thenReturn(false);
        when(mockOptions.isUseRandomDeathSuicideCause()).thenReturn(false);
        when(mockOptions.getPercentageRandomDeathChance()).thenReturn(0.5);
        when(mockOptions.getExponentialRandomDeathMaleValues()).thenReturn(new double[] { 1d });
        when(mockOptions.getExponentialRandomDeathFemaleValues()).thenReturn(new double[] { 1d });

        final Map<TenYearAgeRange, Double> ageRangeMap = new HashMap<>();
        for (final TenYearAgeRange range : TenYearAgeRange.values()) {
            ageRangeMap.put(range, 1d);
        }
        when(mockOptions.getAgeRangeRandomDeathMaleValues()).thenReturn(ageRangeMap);
        when(mockOptions.getAgeRangeRandomDeathFemaleValues()).thenReturn(ageRangeMap);

        assertInstanceOf(DisabledRandomDeath.class, RandomDeathMethod.NONE.getMethod(mockOptions));
        assertInstanceOf(PercentageRandomDeath.class, RandomDeathMethod.PERCENTAGE.getMethod(mockOptions, false));
        assertInstanceOf(ExponentialRandomDeath.class, RandomDeathMethod.EXPONENTIAL.getMethod(mockOptions, false));
        assertInstanceOf(AgeRangeRandomDeath.class, RandomDeathMethod.AGE_RANGE.getMethod(mockOptions, false));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomDeathMethod.NONE.text"),
                RandomDeathMethod.NONE.toString());
        assertEquals(resources.getString("RandomDeathMethod.EXPONENTIAL.text"),
                RandomDeathMethod.EXPONENTIAL.toString());
    }
}
