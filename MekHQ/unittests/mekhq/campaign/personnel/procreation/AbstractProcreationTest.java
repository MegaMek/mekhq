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
import megamek.common.enums.Gender;
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

import static mekhq.campaign.personnel.PersonnelTestUtilities.matchPersonUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @Test
    public void testDetermineFather() {
        when(mockProcreation.determineFather(any(), any())).thenCallRealMethod();

        final Person mother = new Person(mockCampaign);
        final Person father = new Person(mockCampaign);

        given(mockCampaign.getPerson(argThat(matchPersonUUID(father.getId())))).willReturn(father);

        when(mockCampaignOptions.isDetermineFatherAtBirth()).thenReturn(false);
        assertNull(mockProcreation.determineFather(mockCampaign, mother));

        mother.getExtraData().set(AbstractProcreation.PREGNANCY_FATHER_DATA, father.getId().toString());
        assertEquals(father, mockProcreation.determineFather(mockCampaign, mother));

        when(mockCampaignOptions.isDetermineFatherAtBirth()).thenReturn(true);
        assertEquals(father, mockProcreation.determineFather(mockCampaign, mother));

        mother.getGenealogy().setSpouse(father);
        assertEquals(father, mockProcreation.determineFather(mockCampaign, mother));
    }
    //endregion Determination Methods

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testCanProcreate() {

    }

    @Test
    public void testAddPregnancy() {
        doCallRealMethod().when(mockProcreation).addPregnancy(any(), any(), any());
        doCallRealMethod().when(mockProcreation).addPregnancy(any(), any(), any(), anyInt());

        final Person mother = new Person(mockCampaign);
        final Person father = new Person(mockCampaign);

        when(mockProcreation.determineNumberOfBabies(anyInt())).thenReturn(0);
        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother);
        assertNull(mother.getExpectedDueDate());
        assertNull(mother.getDueDate());
        assertTrue(mother.getExtraData().isEmpty());

        when(mockCampaignOptions.isLogProcreation()).thenReturn(false);
        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, 1);
        assertEquals(LocalDate.ofYearDay(3025, 281), mother.getExpectedDueDate());
        assertNotNull(mother.getDueDate());
        assertFalse(mother.getExtraData().isEmpty());
        assertNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA));
        assertNotNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
        assertEquals(1, mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        when(mockCampaignOptions.isLogProcreation()).thenReturn(true);
        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, 2);
        assertEquals(LocalDate.ofYearDay(3025, 281), mother.getExpectedDueDate());
        assertNotNull(mother.getDueDate());
        assertFalse(mother.getExtraData().isEmpty());
        assertNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA));
        assertNotNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
        assertEquals(2, mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        mother.getGenealogy().setSpouse(father);
        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, 10);
        assertEquals(LocalDate.ofYearDay(3025, 281), mother.getExpectedDueDate());
        assertNotNull(mother.getDueDate());
        assertFalse(mother.getExtraData().isEmpty());
        assertNotNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA));
        assertEquals(father.getId().toString(), mother.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA));
        assertNotNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
        assertEquals(10, mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
    }

    @Test
    public void testRemovePregnancy() {
        doCallRealMethod().when(mockProcreation).removePregnancy(any());

        final Person mother = new Person(mockCampaign);
        mother.setDueDate(LocalDate.ofYearDay(3025, 1));
        mother.setExpectedDueDate(LocalDate.ofYearDay(3025, 1));
        mother.getExtraData().set(AbstractProcreation.PREGNANCY_CHILDREN_DATA, 2);

        mockProcreation.removePregnancy(mother);
        assertNull(mother.getDueDate());
        assertNull(mother.getExpectedDueDate());
        assertNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testBirth() {

    }

    //region Pregnancy Complications
    @Test
    public void testProcessPregnancyComplications() {
        doCallRealMethod().when(mockProcreation).processPregnancyComplications(any(), any(), any());
        doNothing().when(mockProcreation).birth(any(), any(), any());

        final Person mockPerson = mock(Person.class);
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(Compute::randomFloat).thenReturn(0.24f);

            when(mockPerson.isPregnant()).thenReturn(false);
            mockProcreation.processPregnancyComplications(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
            verify(mockProcreation, never()).birth(any(), any(), any());

            when(mockPerson.isPregnant()).thenReturn(true);
            when(mockProcreation.determinePregnancyWeek(any(), any())).thenReturn(22);
            mockProcreation.processPregnancyComplications(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
            verify(mockProcreation, never()).birth(any(), any(), any());

            when(mockProcreation.determinePregnancyWeek(any(), any())).thenReturn(23);
            mockProcreation.processPregnancyComplications(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
            verify(mockProcreation, times(1)).birth(any(), any(), any());

            when(mockProcreation.determinePregnancyWeek(any(), any())).thenReturn(24);
            mockProcreation.processPregnancyComplications(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
            verify(mockProcreation, times(2)).birth(any(), any(), any());

            when(mockProcreation.determinePregnancyWeek(any(), any())).thenReturn(25);
            mockProcreation.processPregnancyComplications(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
            verify(mockProcreation, times(3)).birth(any(), any(), any());

            when(mockProcreation.determinePregnancyWeek(any(), any())).thenReturn(26);
            mockProcreation.processPregnancyComplications(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
            verify(mockProcreation, times(4)).birth(any(), any(), any());

            when(mockProcreation.determinePregnancyWeek(any(), any())).thenReturn(30);
            mockProcreation.processPregnancyComplications(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
            verify(mockProcreation, times(5)).birth(any(), any(), any());

            when(mockProcreation.determinePregnancyWeek(any(), any())).thenReturn(36);
            mockProcreation.processPregnancyComplications(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
            verify(mockProcreation, times(6)).birth(any(), any(), any());
        }
    }
    //endregion Pregnancy Complications

    //region New Day
    @Test
    public void testProcessNewDay() {
        doCallRealMethod().when(mockProcreation).processNewDay(any(), any(), any());
        doNothing().when(mockProcreation).birth(any(), any(), any());
        doNothing().when(mockProcreation).addPregnancy(any(), any(), any());

        final Person mockPerson = mock(Person.class);

        when(mockPerson.getGender()).thenReturn(Gender.MALE);
        mockProcreation.processNewDay(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockPerson, never()).isPregnant();
        verify(mockProcreation, never()).randomlyProcreates(any(), any());

        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);
        when(mockPerson.isPregnant()).thenReturn(true);
        when(mockPerson.getDueDate()).thenReturn(LocalDate.ofYearDay(3025, 2));
        mockProcreation.processNewDay(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockProcreation, never()).birth(any(), any(), any());
        verify(mockProcreation, never()).randomlyProcreates(any(), any());

        when(mockPerson.getDueDate()).thenReturn(LocalDate.ofYearDay(3025, 1));
        mockProcreation.processNewDay(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockProcreation, times(1)).birth(any(), any(), any());
        verify(mockProcreation, never()).randomlyProcreates(any(), any());

        when(mockPerson.isPregnant()).thenReturn(false);

        when(mockProcreation.randomlyProcreates(any(), any())).thenReturn(false);
        mockProcreation.processNewDay(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockProcreation, times(1)).birth(any(), any(), any());
        verify(mockProcreation, times(1)).randomlyProcreates(any(), any());

        when(mockProcreation.randomlyProcreates(any(), any())).thenReturn(true);
        mockProcreation.processNewDay(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockProcreation, times(2)).randomlyProcreates(any(), any());
        verify(mockProcreation, times(1)).addPregnancy(any(), any(), any());
    }

    //region Random Procreation
    @Test
    public void testRandomlyProcreates() {
        doCallRealMethod().when(mockProcreation).randomlyProcreates(any(), any());

        final Person person = new Person(mockCampaign);

        when(mockProcreation.canProcreate(any(), any(), anyBoolean())).thenReturn("Pregnant");
        assertFalse(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));

        when(mockProcreation.canProcreate(any(), any(), anyBoolean())).thenReturn(null);
        when(mockProcreation.isUseRelationshiplessProcreation()).thenReturn(false);
        assertFalse(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));


        when(mockProcreation.isUseRelationshiplessProcreation()).thenReturn(true);
        when(mockProcreation.relationshiplessProcreation(any())).thenReturn(true);
        assertTrue(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));

        person.getGenealogy().setSpouse(mock(Person.class));
        when(mockProcreation.relationshipProcreation(any())).thenReturn(true);
        assertTrue(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));
    }
    //endregion Random Procreation
    //endregion New Day
}
