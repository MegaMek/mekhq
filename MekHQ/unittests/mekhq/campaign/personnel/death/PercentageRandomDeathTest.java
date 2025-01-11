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
import mekhq.campaign.CampaignOptions;
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
public class PercentageRandomDeathTest {
    @Mock
    private CampaignOptions mockOptions;

    @BeforeEach
    public void beforeEach() {
        when(mockOptions.getEnabledRandomDeathAgeGroups()).thenReturn(new HashMap<>());
        when(mockOptions.isUseRandomClanPersonnelDeath()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerDeath()).thenReturn(false);
        when(mockOptions.isUseRandomDeathSuicideCause()).thenReturn(false);
        when(mockOptions.getPercentageRandomDeathChance()).thenReturn(0.5);
    }

    @Test
    public void testRandomlyDies() {
        final PercentageRandomDeath percentageRandomDeath = new PercentageRandomDeath(mockOptions, false);
        // This ignores age and gender, so just using 50 and Male.
        // Testing Minimum (0f), Below Value (0.49f), At Value (0.5f), and Maximum (1f)
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(Compute::randomFloat).thenReturn(0f);
            assertTrue(percentageRandomDeath.randomlyDies(50, Gender.MALE));
            compute.when(Compute::randomFloat).thenReturn(0.49f);
            assertTrue(percentageRandomDeath.randomlyDies(50, Gender.MALE));
            compute.when(Compute::randomFloat).thenReturn(0.5f);
            assertFalse(percentageRandomDeath.randomlyDies(50, Gender.MALE));
            compute.when(Compute::randomFloat).thenReturn(1f);
            assertFalse(percentageRandomDeath.randomlyDies(50, Gender.MALE));
        }
    }
}
