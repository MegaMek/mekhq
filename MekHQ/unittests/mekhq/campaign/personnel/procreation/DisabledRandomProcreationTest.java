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
package mekhq.campaign.personnel.procreation;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
public class DisabledRandomProcreationTest {
    @Mock
    private CampaignOptions mockOptions;

    @Mock
    private Person mockPerson;

    @BeforeEach
    public void beforeEach() {
        when(mockOptions.isUseClanPersonnelProcreation()).thenReturn(false);
        when(mockOptions.isUsePrisonerProcreation()).thenReturn(false);
        when(mockOptions.isUseRelationshiplessRandomProcreation()).thenReturn(false);
        when(mockOptions.isUseRandomClanPersonnelProcreation()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerProcreation()).thenReturn(false);
    }

    @Test
    public void testRandomlyProcreates() {
        assertFalse(new DisabledRandomProcreation(mockOptions).randomlyProcreates(LocalDate.now(), mockPerson));
    }

    @Test
    public void testRandomlyDies() {
        assertFalse(new DisabledRandomProcreation(mockOptions).procreation(mockPerson));
    }

    @Test
    public void testRelationshiplessProcreation() {
        assertFalse(new DisabledRandomProcreation(mockOptions).procreation(mockPerson));
    }
}
