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

import megamek.common.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import org.junit.jupiter.api.BeforeEach;
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

    //region Getters/Setters
    @Test
    public void testGettersAndSetters() {
        when(mockCampaignOptions.isUseRandomClanPersonnelProcreation()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomPrisonerProcreation()).thenReturn(false);

        final AbstractProcreation disabledProcreation = new DisabledRandomProcreation(mockCampaignOptions);

        assertEquals(RandomProcreationMethod.NONE, disabledProcreation.getMethod());
        assertFalse(disabledProcreation.isUseRandomClanPersonnelProcreation());
        assertFalse(disabledProcreation.isUseRandomPrisonerProcreation());
    }
    //endregion Getters/Setters

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


        assertNull(mockProcreation.determineFather(mockCampaign, mother));

        mother.getExtraData().set(AbstractProcreation.PREGNANCY_FATHER_DATA, father.getId().toString());
        assertEquals(father, mockProcreation.determineFather(mockCampaign, mother));


        assertEquals(father, mockProcreation.determineFather(mockCampaign, mother));

        mother.getGenealogy().setSpouse(father);
        assertEquals(father, mockProcreation.determineFather(mockCampaign, mother));
    }
    //endregion Determination Methods

    @Test
    void testCanProcreateReturnsNullForValidPerson() {
        Person personMock = mock(Person.class);
        CampaignOptions campaignOptionsMock = mock(CampaignOptions.class);

        AbstractProcreation procreation = new AbstractProcreation(null, campaignOptionsMock) {
            @Override
            protected boolean procreation(Person person) {
                return false; // No random chance for testing purposes
            }
        };
        LocalDate today = LocalDate.of(3151,1,1);


        // Setup conditions where `canProcreate` should return null
        when(personMock.getGender()).thenReturn(Gender.FEMALE);
        when(personMock.isTryingToConceive()).thenReturn(true);
        when(personMock.isPregnant()).thenReturn(false);
        when(personMock.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(personMock.isDeployed()).thenReturn(false);
        when(personMock.isChild(today)).thenReturn(false);
        when(personMock.getAge(today)).thenReturn(30);

        // Execute
        String result = procreation.canProcreate(today, personMock, false);

        // Assert
        assertNull(result, "Person should be able to procreate, but the method returned: " + result);
    }

    @Test
    void testCanProcreateFailsForMaleGender() {
        Person personMock = mock(Person.class);
        CampaignOptions campaignOptionsMock = mock(CampaignOptions.class);

        AbstractProcreation procreation = new AbstractProcreation(null, campaignOptionsMock) {
            @Override
            protected boolean procreation(Person person) {
                return false; // No random chance for testing purposes
            }
        };
        LocalDate today = LocalDate.of(3151,1,1);

        // Setup conditions for male gender
        when(personMock.getGender()).thenReturn(Gender.MALE);

        // Execute
        String result = procreation.canProcreate(today, personMock, false);

        // Assert
        assertNotNull(result, "Male gender cannot procreate and should return a reason");
    }

    @Test
    void testCanProcreateFailsForInactiveStatus() {
        Person personMock = mock(Person.class);
        CampaignOptions campaignOptionsMock = mock(CampaignOptions.class);

        AbstractProcreation procreation = new AbstractProcreation(null, campaignOptionsMock) {
            @Override
            protected boolean procreation(Person person) {
                return false; // No random chance for testing purposes
            }
        };
        LocalDate today = LocalDate.of(3151,1,1);

        // Setup conditions for inactive status
        when(personMock.getGender()).thenReturn(Gender.FEMALE);
        when(personMock.isTryingToConceive()).thenReturn(true);
        when(personMock.isPregnant()).thenReturn(false);
        when(personMock.getStatus()).thenReturn(PersonnelStatus.RETIRED);

        // Execute
        String result = procreation.canProcreate(today, personMock, false);

        // Assert
        assertNotNull(result, "Inactive status should prevent procreation");
    }

    @Test
    void testCanProcreateFailsForAlreadyPregnantPerson() {
        Person personMock = mock(Person.class);
        CampaignOptions campaignOptionsMock = mock(CampaignOptions.class);

        AbstractProcreation procreation = new AbstractProcreation(null, campaignOptionsMock) {
            @Override
            protected boolean procreation(Person person) {
                return false; // No random chance for testing purposes
            }
        };
        LocalDate today = LocalDate.of(3151,1,1);

        // Setup conditions for already pregnant
        when(personMock.getGender()).thenReturn(Gender.FEMALE);
        when(personMock.isTryingToConceive()).thenReturn(true);
        when(personMock.isPregnant()).thenReturn(true);

        // Execute
        String result = procreation.canProcreate(today, personMock, false);

        // Assert
        assertNotNull(result, "Pregnant person cannot procreate");
    }

    @Test
    void testCanProcreateFailsForPersonTooOld() {
        Person personMock = mock(Person.class);
        CampaignOptions campaignOptionsMock = mock(CampaignOptions.class);

        AbstractProcreation procreation = new AbstractProcreation(null, campaignOptionsMock) {
            @Override
            protected boolean procreation(Person person) {
                return false; // No random chance for testing purposes
            }
        };
        LocalDate today = LocalDate.of(3151,1,1);

        // Setup conditions for old age
        when(personMock.getGender()).thenReturn(Gender.FEMALE);
        when(personMock.isTryingToConceive()).thenReturn(true);
        when(personMock.isPregnant()).thenReturn(false);
        when(personMock.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(personMock.isChild(today)).thenReturn(false);
        when(personMock.getAge(today)).thenReturn(60); // Above the procreation limit

        // Execute
        String result = procreation.canProcreate(today, personMock, false);

        // Assert
        assertNotNull(result, "Person aged 51 or older cannot procreate");
    }

    @Test
    void testCanProcreateFailsForPersonDeployed() {
        Person personMock = mock(Person.class);
        CampaignOptions campaignOptionsMock = mock(CampaignOptions.class);

        AbstractProcreation procreation = new AbstractProcreation(null, campaignOptionsMock) {
            @Override
            protected boolean procreation(Person person) {
                return false; // No random chance for testing purposes
            }
        };
        LocalDate today = LocalDate.of(3151,1,1);

        // Setup conditions for deployed status
        when(personMock.getGender()).thenReturn(Gender.FEMALE);
        when(personMock.isTryingToConceive()).thenReturn(true);
        when(personMock.isPregnant()).thenReturn(false);
        when(personMock.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(personMock.isDeployed()).thenReturn(true); // Person is currently deployed

        // Execute
        String result = procreation.canProcreate(today, personMock, false);

        // Assert
        assertNotNull(result, "Deployed person cannot procreate");
    }

    @Test
    void testCanProcreateFailsForChild() {
        // Mock the Person class
        Person personMock = mock(Person.class);
        CampaignOptions campaignOptionsMock = mock(CampaignOptions.class);

        // Create an AbstractProcreation implementation for testing
        AbstractProcreation procreation = new AbstractProcreation(null, campaignOptionsMock) {
            @Override
            protected boolean procreation(Person person) {
                return false; // No random chance for testing purposes
            }
        };

        LocalDate today = LocalDate.of(3151, 1, 1);

        // Stub the behavior of the mocked methods
        when(personMock.getGender()).thenReturn(Gender.FEMALE);
        when(personMock.isTryingToConceive()).thenReturn(true);
        when(personMock.isPregnant()).thenReturn(false);
        when(personMock.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(personMock.isDeployed()).thenReturn(false);
        when(personMock.isChild(today)).thenReturn(true);

        // Execute
        String result = procreation.canProcreate(today, personMock, false);

        // Assert
        assertNotNull(result, "Child cannot procreate");
    }



    @Test
    public void testAddPregnancy() {
        doCallRealMethod().when(mockProcreation).addPregnancy(any(), any(), any(), eq(false));
        doCallRealMethod().when(mockProcreation).addPregnancy(any(), any(), any(), anyInt(), eq(false));

        final Person mother = new Person(mockCampaign);
        final Person father = new Person(mockCampaign);

        when(mockProcreation.determineNumberOfBabies(anyInt())).thenReturn(0);
        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, false);
        assertNull(mother.getExpectedDueDate());
        assertNull(mother.getDueDate());
        assertTrue(mother.getExtraData().isEmpty());


        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, 1, false);
        assertEquals(LocalDate.ofYearDay(3025, 281), mother.getExpectedDueDate());
        assertNotNull(mother.getDueDate());
        assertFalse(mother.getExtraData().isEmpty());
        assertNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA));
        assertNotNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
        assertEquals(1, mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));


        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, 2, false);
        assertEquals(LocalDate.ofYearDay(3025, 281), mother.getExpectedDueDate());
        assertNotNull(mother.getDueDate());
        assertFalse(mother.getExtraData().isEmpty());
        assertNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA));
        assertNotNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
        assertEquals(2, mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        mother.getGenealogy().setSpouse(father);
        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, 10, false);
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

    @Test
    public void testBirth() {
/*
        doCallRealMethod().when(mockProcreation).birth(any(), any(), any());

        when(mockCampaignOptions.getBabySurnameStyle()).thenReturn(BabySurnameStyle.MOTHERS);

        final Person father = new Person(mockCampaign);

        final Person mother = new Person(mockCampaign);
        mother.getGenealogy().setSpouse(father);

        final List<Person> activePersonnel = new ArrayList<>();
        activePersonnel.add(mother);
        activePersonnel.add(father);

        lenient().when(mockCampaign.getActivePersonnel()).thenReturn(activePersonnel);
        doAnswer(answer -> {
            final Person person = new Person(mockCampaign);
            activePersonnel.add(person);
            return person;
        }).when(mockCampaign).newDependent(anyBoolean());

        // Single Baby Tests
        when(mockCampaignOptions.getPrisonerBabyStatus()).thenReturn(false);
        when(mockProcreation.determineFather(any(), any())).thenReturn(null);
        mother.getExtraData().set(AbstractProcreation.PREGNANCY_CHILDREN_DATA, 1);
        mockProcreation.birth(mockCampaign, LocalDate.ofYearDay(3025, 1), mother);
        assertEquals(3, activePersonnel.size());
        assertEquals(activePersonnel.get(2).getGenealogy().getMothers().get(0), mother);
        assertTrue(activePersonnel.get(2).getGenealogy().getFathers().isEmpty());

        when(mockProcreation.determineFather(any(), any())).thenReturn(father);
        mockProcreation.birth(mockCampaign, LocalDate.ofYearDay(3025, 1), mother);
        assertEquals(4, activePersonnel.size());

        // Multiple Baby Test
        mother.getExtraData().set(AbstractProcreation.PREGNANCY_CHILDREN_DATA, 2);
        mother.setPrisonerStatusDirect(PrisonerStatus.PRISONER);
        when(mockCampaignOptions.getPrisonerBabyStatus()).thenReturn(true);
        mockProcreation.birth(mockCampaign, LocalDate.ofYearDay(3025, 1), mother);
*/
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

    //region Process New Week

    @Test
    public void testProcessNewWeek_ForNonPregnantMale() {
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any());

        final Person mockPerson = mock(Person.class);
        when(mockPerson.getGender()).thenReturn(Gender.MALE);
        mockProcreation.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockPerson, never()).isPregnant();
        verify(mockProcreation, never()).randomlyProcreates(any(), any());
    }

    @Test
    public void testProcessNewWeek_ForPregnantFemale() {
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any());

        final Person mockPerson = mock(Person.class);

        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);
        when(mockPerson.isPregnant()).thenReturn(true);
        when(mockPerson.getDueDate()).thenReturn(LocalDate.ofYearDay(3025, 2));

        mockProcreation.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);

        verify(mockProcreation, never()).birth(any(), any(), any());
        verify(mockProcreation, never()).randomlyProcreates(any(), any());
    }

    @Test
    public void testProcessNewWeek_ForPregnantFemaleWithDueDate() {
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any());
        doNothing().when(mockProcreation).birth(any(), any(), any());

        final Person mockPerson = mock(Person.class);

        // Ensure proper stubbing
        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);
        when(mockPerson.isPregnant()).thenReturn(true);
        when(mockPerson.getDueDate()).thenReturn(LocalDate.ofYearDay(3025, 1));

        mockProcreation.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockProcreation, times(1)).birth(any(), any(), any());
        verify(mockProcreation, never()).randomlyProcreates(any(), any());
    }

    //region Random Procreation
    @Test
    public void testRandomlyProcreates() {
        doCallRealMethod().when(mockProcreation).randomlyProcreates(any(), any());

        final Person person = new Person(mockCampaign);

        when(mockProcreation.canProcreate(any(), any(), anyBoolean())).thenReturn("Pregnant");
        assertFalse(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));

        reset(mockProcreation);
        doCallRealMethod().when(mockProcreation).randomlyProcreates(any(), any());

        when(mockProcreation.canProcreate(any(), any(), anyBoolean())).thenReturn(null);

        assertFalse(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));

        reset(mockProcreation);
        doCallRealMethod().when(mockProcreation).randomlyProcreates(any(), any());

        person.getGenealogy().setSpouse(mock(Person.class));
        when(mockProcreation.canProcreate(any(), any(), anyBoolean())).thenReturn(null);
        when(mockProcreation.procreation(any())).thenReturn(true);
        assertTrue(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));
    }
}
