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

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.MergingSurnameStyle;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.personnel.ranks.RankSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(value = MockitoExtension.class)
public class AbstractMarriageTest {
    @Mock
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    @Mock
    private AbstractMarriage mockMarriage;

    @BeforeEach
    public void beforeEach() {
        lenient().when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
    }

    //region Getters/Setters
    @Test
    public void testGettersAndSetters() {
        when(mockCampaignOptions.isUseClanPersonnelMarriages()).thenReturn(false);
        when(mockCampaignOptions.isUsePrisonerMarriages()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomSameSexMarriages()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomClanPersonnelMarriages()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomPrisonerMarriages()).thenReturn(false);

        final AbstractMarriage disabledMarriage = new DisabledRandomMarriage(mockCampaignOptions);

        assertEquals(RandomMarriageMethod.NONE, disabledMarriage.getMethod());
        assertFalse(disabledMarriage.isUseClanPersonnelMarriages());
        assertFalse(disabledMarriage.isUsePrisonerMarriages());
        assertFalse(disabledMarriage.isUseRandomSameSexMarriages());
        assertFalse(disabledMarriage.isUseRandomClanPersonnelMarriages());
        assertFalse(disabledMarriage.isUseRandomPrisonerMarriages());
    }
    //endregion Getters/Setters

    @Test
    public void testCanMarry() {
        doCallRealMethod().when(mockMarriage).canMarry(any(), any(), any(), anyBoolean());

        when(mockCampaignOptions.getMinimumMarriageAge()).thenReturn(16);

        final Genealogy mockGenealogy = mock(Genealogy.class);

        final Person mockPerson = mock(Person.class);
        when(mockPerson.getGenealogy()).thenReturn(mockGenealogy);

        // Have to be marriageable
        when(mockPerson.isMarriageable()).thenReturn(false);
        assertNotNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be married already
        when(mockPerson.isMarriageable()).thenReturn(true);
        when(mockGenealogy.hasSpouse()).thenReturn(true);
        assertNotNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Must be active
        when(mockGenealogy.hasSpouse()).thenReturn(false);
        when(mockPerson.getStatus()).thenReturn(PersonnelStatus.KIA);
        assertNotNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be deployed
        when(mockPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockPerson.isDeployed()).thenReturn(true);
        assertNotNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be younger than the minimum marriage age
        when(mockPerson.isDeployed()).thenReturn(false);
        when(mockPerson.getAge(any())).thenReturn(15);
        assertNotNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be Clan Personnel with Clan Marriage Disabled
        when(mockPerson.getAge(any())).thenReturn(16);
        when(mockPerson.isClanPersonnel()).thenReturn(true);
        when(mockMarriage.isUseClanPersonnelMarriages()).thenReturn(false);
        when(mockMarriage.isUsePrisonerMarriages()).thenReturn(true);
        assertNotNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can be Non-Clan Personnel with Clan Marriage Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(false);
        assertNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can be a Non-Prisoner with Prisoner Marriage Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockMarriage.isUsePrisonerMarriages()).thenReturn(false);
        assertNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be a Prisoner with Prisoner Marriage Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertNotNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can be a Non-Random Clan Prisoner with Clan and Prisoner Marriage Enabled and Random Marriage Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(true);
        when(mockMarriage.isUseClanPersonnelMarriages()).thenReturn(true);
        when(mockMarriage.isUsePrisonerMarriages()).thenReturn(true);
        assertNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false));

        // Can't be Clan Personnel with Random Clan Marriage Disabled
        when(mockMarriage.isUseRandomClanPersonnelMarriages()).thenReturn(false);
        when(mockMarriage.isUseRandomPrisonerMarriages()).thenReturn(true);
        assertNotNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can be Non-Clan Personnel with Random Clan Marriage Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(false);
        assertNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can be a Non-Prisoner with Random Prisoner Marriage Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockMarriage.isUseRandomPrisonerMarriages()).thenReturn(false);
        assertNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can't be a Prisoner with Random Prisoner Marriage Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertNotNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true));

        // Can be a Clan Prisoner with Random Clan and Random Prisoner Marriage Enabled
        lenient().when(mockPerson.isClanPersonnel()).thenReturn(true);
        when(mockMarriage.isUseRandomClanPersonnelMarriages()).thenReturn(true);
        when(mockMarriage.isUseRandomPrisonerMarriages()).thenReturn(true);
        assertNull(mockMarriage.canMarry(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true));
    }

    @Test
    public void testSafeSpouse() {
        doCallRealMethod().when(mockMarriage).safeSpouse(any(), any(), any(), any(), anyBoolean());

        final Genealogy mockGenealogy = mock(Genealogy.class);

        final Person mockPerson = mock(Person.class);
        when(mockPerson.getGenealogy()).thenReturn(mockGenealogy);

        final Person mockSpouse = mock(Person.class);

        // Can't marry yourself
        assertFalse(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockPerson, false));

        // Need to be able to marry
        when(mockMarriage.canMarry(any(), any(), any(), anyBoolean())).thenReturn("Married");
        assertFalse(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, false));

        // Can't be closely related
        when(mockMarriage.canMarry(any(), any(), any(), anyBoolean())).thenReturn(null);
        when(mockGenealogy.checkMutualAncestors(any(), anyInt())).thenReturn(true);
        assertFalse(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, false));

        when(mockGenealogy.checkMutualAncestors(any(), anyInt())).thenReturn(false);

        // Random Marriages require both to be current prisoners or both to not be current prisoners
        // Free - Free
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        assertTrue(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, true));

        // Prisoner - Free
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        assertFalse(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, true));

        // Free - Prisoner
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertFalse(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, true));

        // Prisoner - Prisoner
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER_DEFECTOR);
        assertTrue(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, true));

        // Otherwise, you can manually marry a prisoner to anyone, but a free person can't manually
        // marry a prisoner
        // Free - Free
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        assertTrue(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, false));

        // Free - Prisoner
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertFalse(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, false));

        // Prisoner - Free
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        assertTrue(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, false));

        // Prisoner - Prisoner
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER_DEFECTOR);
        when(mockSpouse.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertTrue(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, false));
    }

    @Test
    public void testMarry() {
        doCallRealMethod().when(mockMarriage).marry(any(), any(), any(), any(), any());

        when(mockCampaign.getRankSystem()).thenReturn(mock(RankSystem.class));
        doNothing().when(mockCampaign).addReport(any());

        final Person origin = new Person("Origin", "Origin", mockCampaign);
        final Person spouse = new Person("Spouse", "Spouse", mockCampaign);

        final MergingSurnameStyle mockMergingSurnameStyle = mock(MergingSurnameStyle.class);
        doNothing().when(mockMergingSurnameStyle).apply(any(), any(), any(), any());

        mockMarriage.marry(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, null, mockMergingSurnameStyle);
        assertNull(origin.getMaidenName());
        assertFalse(origin.getGenealogy().hasSpouse());

        mockMarriage.marry(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse, mockMergingSurnameStyle);
        assertEquals("Origin", origin.getMaidenName());
        assertEquals("Spouse", spouse.getMaidenName());
        assertEquals(origin, spouse.getGenealogy().getSpouse());
        assertEquals(spouse, origin.getGenealogy().getSpouse());
        verify(mockMergingSurnameStyle, times(1)).apply(any(), any(), any(), any());
    }

    //region New Day
    @Test
    public void testProcessNewWeek() {
        doCallRealMethod().when(mockMarriage).processNewWeek(any(), any(), any());
        doNothing().when(mockMarriage).marryRandomSpouse(any(), any(), any(), anyBoolean());

        final Person mockPerson = mock(Person.class);

        when(mockMarriage.canMarry(any(), any(), any(), anyBoolean())).thenReturn("Married");
        mockMarriage.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockMarriage, times(0)).randomOppositeSexMarriage(any());
        verify(mockMarriage, times(0)).randomSameSexMarriage(any());
        verify(mockMarriage, times(0)).marryRandomSpouse(any(), any(), any(), anyBoolean());

        when(mockMarriage.canMarry(any(), any(), any(), anyBoolean())).thenReturn(null);
        when(mockMarriage.randomOppositeSexMarriage(any())).thenReturn(true);
        mockMarriage.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockMarriage, times(1)).randomOppositeSexMarriage(any());
        verify(mockMarriage, times(0)).randomSameSexMarriage(any());
        verify(mockMarriage, times(1)).marryRandomSpouse(any(), any(), any(), anyBoolean());

        when(mockMarriage.randomOppositeSexMarriage(any())).thenReturn(false);
        when(mockMarriage.isUseRandomSameSexMarriages()).thenReturn(false);
        mockMarriage.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockMarriage, times(2)).randomOppositeSexMarriage(any());
        verify(mockMarriage, times(0)).randomSameSexMarriage(any());
        verify(mockMarriage, times(1)).marryRandomSpouse(any(), any(), any(), anyBoolean());

        when(mockMarriage.isUseRandomSameSexMarriages()).thenReturn(true);
        when(mockMarriage.randomSameSexMarriage(any())).thenReturn(false);
        mockMarriage.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockMarriage, times(3)).randomOppositeSexMarriage(any());
        verify(mockMarriage, times(1)).randomSameSexMarriage(any());
        verify(mockMarriage, times(1)).marryRandomSpouse(any(), any(), any(), anyBoolean());

        when(mockMarriage.randomSameSexMarriage(any())).thenReturn(true);
        mockMarriage.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockMarriage, times(4)).randomOppositeSexMarriage(any());
        verify(mockMarriage, times(2)).randomSameSexMarriage(any());
        verify(mockMarriage, times(2)).marryRandomSpouse(any(), any(), any(), anyBoolean());
    }

    //region Random Marriage
    @Test
    public void testMarryRandomSpouse() {
        doCallRealMethod().when(mockMarriage).marryRandomSpouse(any(), any(), any(), anyBoolean());

        final Person mockMale = mock(Person.class);
        when(mockMale.getGender()).thenReturn(Gender.MALE);

        final Person mockFemale = mock(Person.class);
        when(mockFemale.getGender()).thenReturn(Gender.FEMALE);

        final List<Person> mockPersonnel = new ArrayList<>();
        mockPersonnel.add(mockMale);
        mockPersonnel.add(mockFemale);
        when(mockCampaign.getActivePersonnel()).thenReturn(mockPersonnel);

        final Person mockPerson = mock(Person.class);
        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);

        // No Potential Spouses
        when(mockMarriage.isPotentialRandomSpouse(any(), any(), any(), any(), any())).thenReturn(false);
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true);
        verify(mockMarriage, times(0)).marry(any(), any(), any(), any(), any());

        // Replace AbstractMarriage::isPotentialRandomSpouse with this simple gender comparison
        doAnswer(invocation -> invocation.getArgument(3, Person.class).getGender() == invocation.getArgument(4))
                .when(mockMarriage).isPotentialRandomSpouse(any(), any(), any(), any(), any());

        // Same-sex, Female: Expect marry to be called with mockFemale as the spouse
        doAnswer(invocation -> {
            final Person spouse = invocation.getArgument(3);
            assertEquals(spouse, mockFemale);
            return null;
        }).when(mockMarriage).marry(any(), any(), any(), any(), any());
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true);

        // Opposite sex, Female: Expect marry to be called with mockMale as the spouse
        doAnswer(invocation -> {
            final Person spouse = invocation.getArgument(3);
            assertEquals(spouse, mockMale);
            return null;
        }).when(mockMarriage).marry(any(), any(), any(), any(), any());
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false);

        // Same-sex, Male: Expect marry to be called with mockMale as the spouse
        when(mockPerson.getGender()).thenReturn(Gender.MALE);
        doAnswer(invocation -> {
            final Person spouse = invocation.getArgument(3);
            assertEquals(spouse, mockMale);
            return null;
        }).when(mockMarriage).marry(any(), any(), any(), any(), any());
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true);

        // Opposite sex, Male: Expect marry to be called with mockFemale as the spouse
        doAnswer(invocation -> {
            final Person spouse = invocation.getArgument(3);
            assertEquals(spouse, mockFemale);
            return null;
        }).when(mockMarriage).marry(any(), any(), any(), any(), any());
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false);
    }

    @Test
    public void testIsPotentialRandomSpouse() {
        doCallRealMethod().when(mockMarriage).isPotentialRandomSpouse(any(), any(), any(), any(), any());

        when(mockCampaignOptions.getRandomMarriageAgeRange()).thenReturn(10);

        final Person mockPerson = mock(Person.class);
        when(mockPerson.getAge(any())).thenReturn(35);

        final Person mockPotentialSpouse = mock(Person.class);
        when(mockPotentialSpouse.getGender()).thenReturn(Gender.MALE);

        assertFalse(mockMarriage.isPotentialRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1),
                mockPerson, mockPotentialSpouse, Gender.FEMALE));

        when(mockMarriage.safeSpouse(any(), any(), any(), any(), anyBoolean())).thenReturn(false);
        assertFalse(mockMarriage.isPotentialRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1),
                mockPerson, mockPotentialSpouse, Gender.MALE));

        when(mockMarriage.safeSpouse(any(), any(), any(), any(), anyBoolean())).thenReturn(true);
        when(mockPotentialSpouse.getAge(any())).thenReturn(20);
        assertFalse(mockMarriage.isPotentialRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1),
                mockPerson, mockPotentialSpouse, Gender.MALE));

        when(mockPotentialSpouse.getAge(any())).thenReturn(25);
        assertTrue(mockMarriage.isPotentialRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1),
                mockPerson, mockPotentialSpouse, Gender.MALE));

        when(mockPotentialSpouse.getAge(any())).thenReturn(35);
        assertTrue(mockMarriage.isPotentialRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1),
                mockPerson, mockPotentialSpouse, Gender.MALE));

        when(mockPotentialSpouse.getAge(any())).thenReturn(45);
        assertTrue(mockMarriage.isPotentialRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1),
                mockPerson, mockPotentialSpouse, Gender.MALE));

        when(mockPotentialSpouse.getAge(any())).thenReturn(50);
        assertFalse(mockMarriage.isPotentialRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1),
                mockPerson, mockPotentialSpouse, Gender.MALE));
    }
    //endregion Random Marriage
    //endregion New Day
}
