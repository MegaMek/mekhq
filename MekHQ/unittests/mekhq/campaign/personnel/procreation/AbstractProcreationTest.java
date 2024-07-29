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
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import mekhq.campaign.personnel.familyTree.Genealogy;
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
        when(mockCampaignOptions.isUseClanPersonnelProcreation()).thenReturn(false);
        when(mockCampaignOptions.isUsePrisonerProcreation()).thenReturn(false);
        when(mockCampaignOptions.isUseRelationshiplessRandomProcreation()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomClanPersonnelProcreation()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomPrisonerProcreation()).thenReturn(false);

        final AbstractProcreation disabledProcreation = new DisabledRandomProcreation(mockCampaignOptions);

        assertEquals(RandomProcreationMethod.NONE, disabledProcreation.getMethod());
        assertFalse(disabledProcreation.isUseClanPersonnelProcreation());
        assertFalse(disabledProcreation.isUsePrisonerProcreation());
        assertFalse(disabledProcreation.isUseRelationshiplessProcreation());
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

    @Test
    public void testCanProcreate() {
        doCallRealMethod().when(mockProcreation).canProcreate(any(), any(), anyBoolean());

        final Person mockPerson = mock(Person.class);
        final Person mockSpouse = mock(Person.class);
        final Genealogy mockGenealogy = mock(Genealogy.class);

        when(mockPerson.getGenealogy()).thenReturn(mockGenealogy);

        // Males can't procreate
        when(mockPerson.getGender()).thenReturn(Gender.MALE);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Have to be trying to conceive
        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);
        when(mockPerson.isTryingToConceive()).thenReturn(false);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't already be pregnant
        when(mockPerson.isTryingToConceive()).thenReturn(true);
        when(mockPerson.isPregnant()).thenReturn(true);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Must be active
        when(mockPerson.isPregnant()).thenReturn(false);
        when(mockPerson.getStatus()).thenReturn(PersonnelStatus.RETIRED);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be deployed
        when(mockPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockPerson.isDeployed()).thenReturn(true);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be a child
        when(mockPerson.isDeployed()).thenReturn(false);
        when(mockPerson.isChild(any())).thenReturn(true);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Must be younger than 51
        when(mockPerson.isChild(any())).thenReturn(false);
        when(mockPerson.getAge(any())).thenReturn(51);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be Clan Personnel with Clan Procreation Disabled
        when(mockPerson.getAge(any())).thenReturn(25);
        when(mockPerson.isClanPersonnel()).thenReturn(true);
        when(mockProcreation.isUseClanPersonnelProcreation()).thenReturn(false);
        when(mockProcreation.isUsePrisonerProcreation()).thenReturn(true);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can be Non-Clan Personnel with Clan Procreation Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(false);
        assertNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can be a Non-Prisoner with Prisoner Procreation Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockProcreation.isUsePrisonerProcreation()).thenReturn(false);
        assertNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be a Prisoner with Prisoner Procreation Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can be a Non-Random Clan Prisoner with Clan and Prisoner Procreation Enabled and Random Procreation Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(true);
        when(mockProcreation.isUseClanPersonnelProcreation()).thenReturn(true);
        when(mockProcreation.isUsePrisonerProcreation()).thenReturn(true);
        assertNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be Single with Relationshipless Random Procreation Disabled
        when(mockGenealogy.hasSpouse()).thenReturn(false);
        when(mockProcreation.isUseRelationshiplessProcreation()).thenReturn(false);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can't be Clan Personnel with Random Clan Procreation Disabled
        when(mockProcreation.isUseRelationshiplessProcreation()).thenReturn(true);
        when(mockProcreation.isUseRandomClanPersonnelProcreation()).thenReturn(false);
        when(mockProcreation.isUseRandomPrisonerProcreation()).thenReturn(true);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can be Non-Clan Personnel with Random Clan Procreation Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(false);
        assertNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can be a Non-Prisoner with Random Prisoner Procreation Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockProcreation.isUseRandomPrisonerProcreation()).thenReturn(false);
        assertNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can't be a Prisoner with Random Prisoner Procreation Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can be a Clan Prisoner with no Spouse with Random Relationshipless, Random Clan, and
        // Random Prisoner Procreation Enabled
        when(mockPerson.isClanPersonnel()).thenReturn(true);
        when(mockProcreation.isUseRandomClanPersonnelProcreation()).thenReturn(true);
        when(mockProcreation.isUseRandomPrisonerProcreation()).thenReturn(true);
        assertNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can't have a Same-sex Spouse
        when(mockSpouse.getGender()).thenReturn(Gender.FEMALE);
        when(mockGenealogy.hasSpouse()).thenReturn(true);
        when(mockGenealogy.getSpouse()).thenReturn(mockSpouse);
        when(mockProcreation.isUseRelationshiplessProcreation()).thenReturn(false);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Spouse must also be trying to conceive
        when(mockSpouse.getGender()).thenReturn(Gender.MALE);
        when(mockSpouse.isTryingToConceive()).thenReturn(false);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Spouse must be active
        when(mockSpouse.getStatus()).thenReturn(PersonnelStatus.RETIRED);
        when(mockSpouse.isTryingToConceive()).thenReturn(true);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Spouse can't be deployed
        when(mockSpouse.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockSpouse.isDeployed()).thenReturn(true);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Spouse can't be a child
        when(mockSpouse.isDeployed()).thenReturn(false);
        when(mockSpouse.isChild(any())).thenReturn(true);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Spouse can't be Clan Personnel with Random Clan Procreation Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(false);
        when(mockSpouse.isClanPersonnel()).thenReturn(true);
        when(mockSpouse.isChild(any())).thenReturn(false);
        when(mockProcreation.isUseRandomClanPersonnelProcreation()).thenReturn(false);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Spouse can be Non-Clan Personnel with Random Clan Procreation Disabled
        when(mockSpouse.isClanPersonnel()).thenReturn(false);
        assertNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Spouse can be a Non-Prisoner with Random Prisoner Procreation Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockProcreation.isUseRandomPrisonerProcreation()).thenReturn(false);
        assertNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Spouse can't be a Prisoner with Prisoner Procreation Disabled
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertNotNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Spouse can be a Prisoner Clan Personnel with Random Clan and Prisoner Procreation Enabled
        lenient().when(mockSpouse.isClanPersonnel()).thenReturn(true);
        when(mockProcreation.isUseRandomClanPersonnelProcreation()).thenReturn(true);
        when(mockProcreation.isUseRandomPrisonerProcreation()).thenReturn(true);
        assertNull(mockProcreation.canProcreate(LocalDate.ofYearDay(3025, 1), mockPerson, true));
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

        when(mockCampaignOptions.isLogProcreation()).thenReturn(false);
        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, 1, false);
        assertEquals(LocalDate.ofYearDay(3025, 281), mother.getExpectedDueDate());
        assertNotNull(mother.getDueDate());
        assertFalse(mother.getExtraData().isEmpty());
        assertNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA));
        assertNotNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
        assertEquals(1, mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        when(mockCampaignOptions.isLogProcreation()).thenReturn(true);
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

    //region New Day
    @Test
    public void testProcessNewDay() {
        doCallRealMethod().when(mockProcreation).processNewDay(any(), any(), any());
        doNothing().when(mockProcreation).birth(any(), any(), any());
        doNothing().when(mockProcreation).addPregnancy(any(), any(), any(), eq(false));

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
        verify(mockProcreation, times(1)).addPregnancy(any(), any(), any(), eq(false));
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
