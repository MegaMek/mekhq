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

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled // FIXME : Windchild : All Tests Missing
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

    }

    //region Determination Methods
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testDetermineNumberOfBabies() {
//        when(mockProcreation.determineNumberOfBabies(any())).thenCallRealMethod();
//        assertEquals(10, mockProcreation.determineNumberOfBabies());
    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testDeterminePregnancyDuration() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testDeterminePregnancyWeek() {
        final LocalDate today = LocalDate.of(3025, 1, 1);
        final Person mockPerson = mock(Person.class);

        when(mockProcreation.determinePregnancyWeek(any(), any())).thenCallRealMethod();

        // First Day means First Week
        // Jan 1st Start, Oct 8th Expected
        when(mockPerson.getExpectedDueDate()).thenReturn(LocalDate.of(3025, 10, 8));
        assertEquals(1, mockProcreation.determinePregnancyWeek(today, mockPerson));

        // Today means 40th Week
        when(mockPerson.getExpectedDueDate()).thenReturn(today);
        assertEquals(40, mockProcreation.determinePregnancyWeek(today, mockPerson));
    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testDetermineFather() {
        //        when(mockProcreation.determineNumberOfBabies(any())).thenCallRealMethod();
        //        assertEquals(10, mockProcreation.determineNumberOfBabies());
    }
    //endregion Determination Methods

    @Disabled // FIXME : Windchild : Test Missing
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
