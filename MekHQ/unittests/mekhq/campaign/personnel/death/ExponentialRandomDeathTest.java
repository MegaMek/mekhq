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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
public class ExponentialRandomDeathTest {
    @Mock
    private CampaignOptions mockOptions;

    @BeforeEach
    public void beforeEach() {
        when(mockOptions.getEnabledRandomDeathAgeGroups()).thenReturn(new HashMap<>());
        when(mockOptions.isUseRandomClanPersonnelDeath()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerDeath()).thenReturn(false);
        when(mockOptions.isUseRandomDeathSuicideCause()).thenReturn(false);
        when(mockOptions.getExponentialRandomDeathMaleValues()).thenReturn(new double[] { 5.4757, -7.0, 0.0709 });
        when(mockOptions.getExponentialRandomDeathFemaleValues()).thenReturn(new double[] { 2.4641, -7.0, 0.0752 });
    }

    @Test
    public void testRandomlyDies() {
        final ExponentialRandomDeath exponentialRandomDeath = new ExponentialRandomDeath(mockOptions, false);
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(Compute::randomFloat).thenReturn(0f);
            assertTrue(exponentialRandomDeath.randomlyDies(0, Gender.MALE));
            assertTrue(exponentialRandomDeath.randomlyDies(0, Gender.FEMALE));
            assertTrue(exponentialRandomDeath.randomlyDies(50, Gender.MALE));
            assertTrue(exponentialRandomDeath.randomlyDies(50, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(0.000001f);
            assertFalse(exponentialRandomDeath.randomlyDies(0, Gender.MALE));
            assertFalse(exponentialRandomDeath.randomlyDies(0, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(0.0000692f);
            assertTrue(exponentialRandomDeath.randomlyDies(75, Gender.MALE));
            assertTrue(exponentialRandomDeath.randomlyDies(75, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(0.0000694f);
            assertTrue(exponentialRandomDeath.randomlyDies(75, Gender.MALE));
            assertFalse(exponentialRandomDeath.randomlyDies(75, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(0.000111f);
            assertTrue(exponentialRandomDeath.randomlyDies(75, Gender.MALE));
            assertFalse(exponentialRandomDeath.randomlyDies(75, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(0.000112f);
            assertFalse(exponentialRandomDeath.randomlyDies(75, Gender.MALE));
            assertFalse(exponentialRandomDeath.randomlyDies(75, Gender.FEMALE));
            compute.when(Compute::randomFloat).thenReturn(1f);
            assertFalse(exponentialRandomDeath.randomlyDies(0, Gender.MALE));
            assertFalse(exponentialRandomDeath.randomlyDies(0, Gender.FEMALE));
            assertFalse(exponentialRandomDeath.randomlyDies(50, Gender.MALE));
            assertFalse(exponentialRandomDeath.randomlyDies(50, Gender.FEMALE));
            assertFalse(exponentialRandomDeath.randomlyDies(100, Gender.MALE));
            assertFalse(exponentialRandomDeath.randomlyDies(100, Gender.FEMALE));
            assertFalse(exponentialRandomDeath.randomlyDies(200, Gender.MALE));
            assertFalse(exponentialRandomDeath.randomlyDies(200, Gender.FEMALE));
            assertTrue(exponentialRandomDeath.randomlyDies(205, Gender.MALE));
            assertTrue(exponentialRandomDeath.randomlyDies(205, Gender.FEMALE));
        }
    }
}
