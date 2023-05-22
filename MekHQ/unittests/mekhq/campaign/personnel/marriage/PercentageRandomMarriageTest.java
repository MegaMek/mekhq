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
package mekhq.campaign.personnel.marriage;

import megamek.common.Compute;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
public class PercentageRandomMarriageTest {
    @Mock
    private CampaignOptions mockOptions;

    @Mock
    private Person mockPerson;

    @BeforeEach
    public void beforeEach() {
        when(mockOptions.isUseClanPersonnelMarriages()).thenReturn(false);
        when(mockOptions.isUsePrisonerMarriages()).thenReturn(false);
        when(mockOptions.isUseRandomSameSexMarriages()).thenReturn(false);
        when(mockOptions.isUseRandomClanPersonnelMarriages()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerMarriages()).thenReturn(false);
        when(mockOptions.getPercentageRandomMarriageOppositeSexChance()).thenReturn(0.5);
        when(mockOptions.getPercentageRandomMarriageSameSexChance()).thenReturn(0.5);
    }

    @Test
    public void testRandomOppositeSexMarriage() {
        final PercentageRandomMarriage percentageRandomMarriage = new PercentageRandomMarriage(mockOptions);
        // This ignores the person, so just using a mocked person
        // Testing Minimum (0f), Below Value (0.49f), At Value (0.5f), and Maximum (1f)
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(Compute::randomFloat).thenReturn(0f);
            assertTrue(percentageRandomMarriage.randomOppositeSexMarriage(mockPerson));
            compute.when(Compute::randomFloat).thenReturn(0.49f);
            assertTrue(percentageRandomMarriage.randomOppositeSexMarriage(mockPerson));
            compute.when(Compute::randomFloat).thenReturn(0.5f);
            assertFalse(percentageRandomMarriage.randomOppositeSexMarriage(mockPerson));
            compute.when(Compute::randomFloat).thenReturn(1f);
            assertFalse(percentageRandomMarriage.randomOppositeSexMarriage(mockPerson));
        }
    }

    @Test
    public void testRandomSameSexMarriage() {
        final PercentageRandomMarriage percentageRandomMarriage = new PercentageRandomMarriage(mockOptions);
        // This ignores the person, so just using a mocked person
        // Testing Minimum (0f), Below Value (0.49f), At Value (0.5f), and Maximum (1f)
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(Compute::randomFloat).thenReturn(0f);
            assertTrue(percentageRandomMarriage.randomSameSexMarriage(mockPerson));
            compute.when(Compute::randomFloat).thenReturn(0.49f);
            assertTrue(percentageRandomMarriage.randomSameSexMarriage(mockPerson));
            compute.when(Compute::randomFloat).thenReturn(0.5f);
            assertFalse(percentageRandomMarriage.randomSameSexMarriage(mockPerson));
            compute.when(Compute::randomFloat).thenReturn(1f);
            assertFalse(percentageRandomMarriage.randomSameSexMarriage(mockPerson));
        }
    }
}
