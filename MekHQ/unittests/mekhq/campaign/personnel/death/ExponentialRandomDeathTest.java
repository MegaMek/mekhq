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
package mekhq.campaign.personnel.death;

import megamek.common.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.CampaignOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(value = MockitoExtension.class)
public class ExponentialRandomDeathTest {
    @Mock
    private CampaignOptions mockOptions;

    private ExponentialRandomDeath exponentialRandomDeath;

    @BeforeEach
    public void setUp() {
        exponentialRandomDeath = new ExponentialRandomDeath(mockOptions, false);
    }

    @Test
    public void testRandomlyDiesForYoungMale() {
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            // Mock random float to always return 0 (smallest possible value, ensuring "true")
            compute.when(Compute::randomFloat).thenReturn(0f);

            // A male age 0 should "die" since the random value is always less than the death chance
            assertTrue(exponentialRandomDeath.randomlyDies(0, Gender.MALE));
        }
    }

    @Test
    public void testRandomlyDiesForYoungFemale() {
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(Compute::randomFloat).thenReturn(0f);

            // A female age 0 should "die" since the random value is 0
            assertTrue(exponentialRandomDeath.randomlyDies(0, Gender.FEMALE));
        }
    }

    @Test
    public void testRandomlyDiesForHigherAgeMale() {
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            // Mock random float to return a value slightly higher than expected for age 50 male
            compute.when(Compute::randomFloat).thenReturn(0.00001f);

            // A male age 50 (relatively high chance) should still die based on the random value
            assertTrue(exponentialRandomDeath.randomlyDies(50, Gender.MALE));
        }
    }

    @Test
    public void testRandomlyDiesForHigherAgeFemale() {
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            // Mock random float to return a value slightly higher than expected for age 75 female
            compute.when(Compute::randomFloat).thenReturn(0.0001f);

            // A female age 75 should die since the random value is lower than her chance of death
            assertTrue(exponentialRandomDeath.randomlyDies(75, Gender.FEMALE));
        }
    }

    @Test
    public void testNoRandomDeathForLowChanceMale() {
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            // Mock random float to return a large value, higher than any plausible death chance
            compute.when(Compute::randomFloat).thenReturn(5.0f);

            // A male at any age will not die since the random value is very high
            assertFalse(exponentialRandomDeath.randomlyDies(30, Gender.MALE));
        }
    }

    @Test
    public void testNoRandomDeathForLowChanceFemale() {
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(Compute::randomFloat).thenReturn(5.0f);

            // A female at any age will also not die since the random value is high
            assertFalse(exponentialRandomDeath.randomlyDies(30, Gender.FEMALE));
        }
    }

    @Test
    public void testEdgeCaseForExtremelyOldMale() {
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            // Mock random float to return a value close to 0, ensuring death for very old males
            compute.when(Compute::randomFloat).thenReturn(0f);

            // A male age 200+ has an extremely high chance of death
            assertTrue(exponentialRandomDeath.randomlyDies(200, Gender.MALE));
        }
    }

    @Test
    public void testEdgeCaseForExtremelyOldFemale() {
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(Compute::randomFloat).thenReturn(0f);

            // A female age 200+ has an extremely high chance of death
            assertTrue(exponentialRandomDeath.randomlyDies(200, Gender.FEMALE));
        }
    }
}
