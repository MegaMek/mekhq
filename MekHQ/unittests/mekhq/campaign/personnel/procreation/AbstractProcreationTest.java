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
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
public class AbstractProcreationTest {
    @Mock
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    @Mock
    private AbstractProcreation mockProcreation;

    @BeforeEach
    public void beforeEach() {
        lenient().when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
    }

    //region Determination Methods
    @Test
    public void testDetermineNumberOfBabies() {
        when(mockProcreation.determineNumberOfBabies(anyInt())).thenCallRealMethod();

        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(1);
            assertEquals(1, mockProcreation.determineNumberOfBabies(100));

            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);
            assertEquals(10, mockProcreation.determineNumberOfBabies(100));
        }
    }

    @Test
    public void testDeterminePregnancyWeek() {
        when(mockProcreation.determinePregnancyWeek(any(), any())).thenCallRealMethod();

        final Person mockPerson = mock(Person.class);

        // First Day means First Week
        when(mockPerson.getExpectedDueDate()).thenReturn(LocalDate.ofYearDay(3025, 281));
        assertEquals(1, mockProcreation.determinePregnancyWeek(LocalDate.ofYearDay(3025, 1), mockPerson));

        // Second Day means First Week
        when(mockPerson.getExpectedDueDate()).thenReturn(LocalDate.ofYearDay(3025, 281));
        assertEquals(1, mockProcreation.determinePregnancyWeek(LocalDate.ofYearDay(3025, 2), mockPerson));

        // Today is the expected due date, which is in the 40th Week
        when(mockPerson.getExpectedDueDate()).thenReturn(LocalDate.ofYearDay(3025, 1));
        assertEquals(40, mockProcreation.determinePregnancyWeek(LocalDate.ofYearDay(3025, 1), mockPerson));

        // The expected due date was yesterday, so it's now the 41st Week
        when(mockPerson.getExpectedDueDate()).thenReturn(LocalDate.ofYearDay(3025, 1));
        assertEquals(41, mockProcreation.determinePregnancyWeek(LocalDate.ofYearDay(3025, 2), mockPerson));
    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testDetermineFather() {

    }
    //endregion Determination Methods

    @Test
    public void testCanProcreate() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testAddPregnancy() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testRemovePregnancy() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testBirth() {

    }

    //region New Day
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testProcessNewDay() {

    }

    //region Random Procreation
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testRandomlyProcreates() {

    }
    //endregion Random Procreation
    //endregion New Day

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testProcessPregnancyComplications() {

    }
}
