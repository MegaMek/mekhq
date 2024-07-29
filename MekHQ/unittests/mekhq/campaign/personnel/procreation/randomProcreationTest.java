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
package mekhq.campaign.personnel.procreation;

import megamek.common.Compute;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familyTree.Genealogy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
public class randomProcreationTest {
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
        when(mockOptions.getRandomProcreationRelationshipDiceSize()).thenReturn(5);
        when(mockOptions.getRandomProcreationRelationshiplessDiceSize()).thenReturn(5);
    }

    @Test
    public void testProcreation() {
        final randomProcreation randomProcreation = new randomProcreation(mockOptions);
        Genealogy mockGenealogy = mock(Genealogy.class);

        when(mockGenealogy.hasSpouse()).thenReturn(true);
        when(mockGenealogy.getChildren()).thenReturn(new ArrayList<>(Collections.singleton(mock(Person.class))));

        // Make sure getGenealogy returns mocked Genealogy
        when(mockPerson.getGenealogy()).thenReturn(mockGenealogy);

        // Assuming diceSize and multiplier are defined...
        int diceSize = 5;
        int multiplier = 1;

        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(diceSize * multiplier)).thenReturn(0);
            assertTrue(randomProcreation.procreation(mockPerson));
            compute.when(() -> Compute.randomInt(diceSize * multiplier)).thenReturn(1);
            assertFalse(randomProcreation.procreation(mockPerson));
        }
    }
}
