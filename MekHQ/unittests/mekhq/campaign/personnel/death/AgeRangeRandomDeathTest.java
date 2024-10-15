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
package mekhq.campaign.personnel.death;

import megamek.common.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.TenYearAgeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
public class AgeRangeRandomDeathTest {
    @Mock
    private CampaignOptions mockOptions;

    @BeforeEach
    public void beforeEach() {
        when(mockOptions.getEnabledRandomDeathAgeGroups()).thenReturn(new HashMap<>());
        when(mockOptions.isUseRandomClanPersonnelDeath()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerDeath()).thenReturn(false);
        when(mockOptions.isUseRandomDeathSuicideCause()).thenReturn(false);

        final Map<TenYearAgeRange, Double> maleAgeRangeMap = new HashMap<>();
        final Map<TenYearAgeRange, Double> femaleAgeRangeMap = new HashMap<>();
        for (final TenYearAgeRange range : TenYearAgeRange.values()) {
            maleAgeRangeMap.put(range, 18262500d);
            femaleAgeRangeMap.put(range, 14610000d);
        }
        when(mockOptions.getAgeRangeRandomDeathMaleValues()).thenReturn(maleAgeRangeMap);
        when(mockOptions.getAgeRangeRandomDeathFemaleValues()).thenReturn(femaleAgeRangeMap);
    }

    @Test
    public void testRandomlyDies() {
        final AgeRangeRandomDeath ageRangeRandomDeath = new AgeRangeRandomDeath(mockOptions, false);
        // We're using the same percentages for all age ranges, so we only need to test Genders
        // Testing Minimum (0f), Below Value (0.49f), At Value (0.5f), and Maximum (1f)
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(Compute::randomFloat).thenReturn(0f);
            assertTrue(ageRangeRandomDeath.randomlyDies(50, Gender.MALE));
            assertTrue(ageRangeRandomDeath.randomlyDies(50, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(0.39f);
            assertTrue(ageRangeRandomDeath.randomlyDies(50, Gender.MALE));
            assertTrue(ageRangeRandomDeath.randomlyDies(50, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(0.40f);
            assertTrue(ageRangeRandomDeath.randomlyDies(50, Gender.MALE));
            assertFalse(ageRangeRandomDeath.randomlyDies(50, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(0.49f);
            assertTrue(ageRangeRandomDeath.randomlyDies(50, Gender.MALE));
            assertFalse(ageRangeRandomDeath.randomlyDies(50, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(0.5f);
            assertFalse(ageRangeRandomDeath.randomlyDies(50, Gender.MALE));
            assertFalse(ageRangeRandomDeath.randomlyDies(50, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(1f);
            assertFalse(ageRangeRandomDeath.randomlyDies(50, Gender.MALE));
            assertFalse(ageRangeRandomDeath.randomlyDies(50, Gender.FEMALE));
        }
    }
}
