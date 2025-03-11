/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.personnel.marriage;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.MergingSurnameStyle;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
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
        when(mockCampaignOptions.isUseRandomClanPersonnelMarriages()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomPrisonerMarriages()).thenReturn(false);

        final AbstractMarriage disabledMarriage = new DisabledRandomMarriage(mockCampaignOptions);

        assertEquals(RandomMarriageMethod.NONE, disabledMarriage.getMethod());
        assertFalse(disabledMarriage.isUseClanPersonnelMarriages());
        assertFalse(disabledMarriage.isUsePrisonerMarriages());
        assertFalse(disabledMarriage.isUseRandomClanPersonnelMarriages());
        assertFalse(disabledMarriage.isUseRandomPrisonerMarriages());
    }
    //endregion Getters/Setters

    @Test
    public void testNotMarriageable() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.isMarriageable()).thenReturn(false);

        // Act
        String result = marriage.canMarry(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testAlreadyMarried() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);

        // Act
        String result = marriage.canMarry(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testInactiveStatus() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.STUDENT);

        // Act
        String result = marriage.canMarry(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testDeployed() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(true);

        // Act
        String result = marriage.canMarry(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testUnderage() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(true);

        // Act
        String result = marriage.canMarry(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testClanPersonnelMarriageDisabled() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);
        marriage.setUseClanPersonnelMarriages(false);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.isClanPersonnel()).thenReturn(true);

        // Act
        String result = marriage.canMarry(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testPrisonerMarriageDisabled() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);
        marriage.setUsePrisonerMarriages(false);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);

        // Act
        String result = marriage.canMarry(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testPrisonerMarriageDisabledBondsman() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);
        marriage.setUsePrisonerMarriages(false);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.BONDSMAN);

        // Act
        String result = marriage.canMarry(date, person, false);

        // Assert
        assertNull(result);
    }

    @Test
    public void testRandomClanMarriageDisabled() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);
        marriage.setUseClanPersonnelMarriages(true);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.isClanPersonnel()).thenReturn(true);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);

        // Act
        String result = marriage.canMarry(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomPrisonerMarriageDisabled() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);
        marriage.setUsePrisonerMarriages(true);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);

        // Act
        String result = marriage.canMarry(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomPrisonerMarriageDisabledBondsman() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);
        marriage.setUsePrisonerMarriages(true);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.BONDSMAN);

        // Act
        String result = marriage.canMarry(date, person, true);

        // Assert
        assertNull(result);
    }

    @Test
    public void testNoBlockers() {
        // Arrange
        AbstractMarriage marriage = new RandomMarriage(mockCampaignOptions);
        marriage.setUseClanPersonnelMarriages(true);
        marriage.setUseRandomClanPersonnelMarriages(true);
        marriage.setUsePrisonerMarriages(true);
        marriage.setUseRandomPrisonerMarriages(true);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);
        Genealogy genealogy = mock(Genealogy.class);

        when(person.isMarriageable()).thenReturn(true);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);

        // Act
        String result = marriage.canMarry(date, person, true);

        // Assert
        assertNull(result);
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
        when(mockMarriage.canMarry(any(), any(), anyBoolean())).thenReturn("Married");
        assertFalse(mockMarriage.safeSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, mockSpouse, false));

        // Can't be closely related
        when(mockMarriage.canMarry(any(), any(), anyBoolean())).thenReturn(null);
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
        doCallRealMethod().when(mockMarriage).marry(any(), any(), any(), any(), any(), anyBoolean());

        when(mockCampaign.getRankSystem()).thenReturn(mock(RankSystem.class));
        doNothing().when(mockCampaign).addReport(any());

        final Person origin = new Person("Origin", "Origin", mockCampaign);
        origin.setJoinedCampaign(LocalDate.ofYearDay(3025, 1));

        final Person spouse = new Person("Spouse", "Spouse", mockCampaign);
        spouse.setJoinedCampaign(LocalDate.ofYearDay(3025, 1));

        final MergingSurnameStyle mockMergingSurnameStyle = mock(MergingSurnameStyle.class);
        doNothing().when(mockMergingSurnameStyle).apply(any(), any(), any(), any());

        mockMarriage.marry(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, null, mockMergingSurnameStyle, false);
        assertNull(origin.getMaidenName());
        assertFalse(origin.getGenealogy().hasSpouse());

        mockMarriage.marry(mockCampaign, LocalDate.ofYearDay(3025, 1), origin, spouse, mockMergingSurnameStyle, false);
        assertEquals("Origin", origin.getMaidenName());
        assertEquals("Spouse", spouse.getMaidenName());
        assertEquals(origin, spouse.getGenealogy().getSpouse());
        assertEquals(spouse, origin.getGenealogy().getSpouse());
        verify(mockMergingSurnameStyle, times(1)).apply(any(), any(), any(), any());
    }

    //region New Week
    @Test
    public void testProcessNewWeek() {
        doCallRealMethod().when(mockMarriage).processNewWeek(any(), any(), any(), anyBoolean());
        doNothing().when(mockMarriage).marryRandomSpouse(any(), any(), any(), anyBoolean(), anyBoolean(), eq(true));

        final Person mockPerson = mock(Person.class);

        when(mockMarriage.canMarry(any(), any(), anyBoolean())).thenReturn("Married");
        mockMarriage.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true);
        verify(mockMarriage, times(0)).randomMarriage();
        verify(mockMarriage, times(0)).marryRandomSpouse(any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean());

        when(mockMarriage.canMarry(any(), any(), anyBoolean())).thenReturn(null);
        when(mockMarriage.randomMarriage()).thenReturn(true);
        mockMarriage.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true);
        verify(mockMarriage, times(1)).randomMarriage();
        verify(mockMarriage, times(1)).marryRandomSpouse(any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean());
    }


    //region Random Marriage
    @Test
    public void testMarryRandomSpouse() {
        doCallRealMethod().when(mockMarriage).marryRandomSpouse(any(), any(), any(), anyBoolean(), eq(true), eq(true));

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
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true, true, true);

        // Replace AbstractMarriage::isPotentialRandomSpouse with this simple gender comparison
        doAnswer(invocation -> invocation.getArgument(3, Person.class).getGender() == invocation.getArgument(4))
                .when(mockMarriage).isPotentialRandomSpouse(any(), any(), any(), any(), any());

        // Same-sex, Female: Expect marry to be called with mockFemale as the spouse
        doAnswer(invocation -> {
            final Person spouse = invocation.getArgument(3);
            assertEquals(spouse, mockFemale);
            return null;
        }).when(mockMarriage).marry(any(), any(), any(), any(), any(), anyBoolean());
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true, true, true);

        // Opposite sex, Female: Expect marry to be called with mockMale as the spouse
        doAnswer(invocation -> {
            final Person spouse = invocation.getArgument(3);
            assertEquals(spouse, mockMale);
            return null;
        }).when(mockMarriage).marry(any(), any(), any(), any(), any(), anyBoolean());
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false, true, true);

        // Same-sex, Male: Expect marry to be called with mockMale as the spouse
        when(mockPerson.getGender()).thenReturn(Gender.MALE);
        doAnswer(invocation -> {
            final Person spouse = invocation.getArgument(3);
            assertEquals(spouse, mockMale);
            return null;
        }).when(mockMarriage).marry(any(), any(), any(), any(), any(), anyBoolean());
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, true, true, true);

        // Opposite sex, Male: Expect marry to be called with mockFemale as the spouse
        doAnswer(invocation -> {
            final Person spouse = invocation.getArgument(3);
            assertEquals(spouse, mockFemale);
            return null;
        }).when(mockMarriage).marry(any(), any(), any(), any(), any(), anyBoolean());
        mockMarriage.marryRandomSpouse(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson, false, true, true);
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
