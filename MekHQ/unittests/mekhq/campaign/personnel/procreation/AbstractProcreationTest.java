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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.procreation;

import static mekhq.campaign.personnel.PersonnelTestUtilities.matchPersonUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

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
    public void testIsMale() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.MALE);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testNotInterestInChildren() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(false);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testIsAlreadyPregnant() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(true);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testIsInactive() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.STUDENT);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testIsDeployed() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(true);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testIsChild() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(true);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testIsTooOld() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(356);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testIsClanAndClanProcreationDisabled() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(true);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testIsPrisonerAndPrisonerProcreationDisabled() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testIsPrisonerAndPrisonerProcreationDisabledBondsman() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.BONDSMAN);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNull(result);
    }

    @Test
    public void testRandomProcreationRelationshiplessProcreationDisabled() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(false);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomProcreationSpouseIsFemale() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Person spouse = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(spouse.getGender()).thenReturn(Gender.FEMALE);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomProcreationSpouseHasNoInterestInChildren() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Person spouse = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(spouse.isTryingToConceive()).thenReturn(false);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomProcreationSpouseIsInactive() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Person spouse = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(spouse.isTryingToConceive()).thenReturn(true);
        when(spouse.getStatus()).thenReturn(PersonnelStatus.STUDENT);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomProcreationSpouseIsDeployed() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Person spouse = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(spouse.isTryingToConceive()).thenReturn(true);
        when(spouse.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(spouse.isDeployed()).thenReturn(true);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomProcreationSpouseIsChild() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Person spouse = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(spouse.isTryingToConceive()).thenReturn(true);
        when(spouse.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(spouse.isDeployed()).thenReturn(false);
        when(spouse.isChild(date, true)).thenReturn(true);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomProcreationSpouseIsClanAndClanRandomProcreationDisabled() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Person spouse = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(spouse.isTryingToConceive()).thenReturn(true);
        when(spouse.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(spouse.isDeployed()).thenReturn(false);
        when(spouse.isChild(date, true)).thenReturn(false);
        when(spouse.isClanPersonnel()).thenReturn(true);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomProcreationSpouseIsPrisonAndPrisonerRandomProcreationDisabled() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Person spouse = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(spouse.isTryingToConceive()).thenReturn(true);
        when(spouse.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(spouse.isDeployed()).thenReturn(false);
        when(spouse.isChild(date, true)).thenReturn(false);
        when(spouse.isClanPersonnel()).thenReturn(false);
        when(spouse.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testRandomProcreationSpouseIsPrisonAndPrisonerRandomProcreationDisabledBondsman() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Person spouse = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(spouse.isTryingToConceive()).thenReturn(true);
        when(spouse.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(spouse.isDeployed()).thenReturn(false);
        when(spouse.isChild(date, true)).thenReturn(false);
        when(spouse.isClanPersonnel()).thenReturn(false);
        when(spouse.getPrisonerStatus()).thenReturn(PrisonerStatus.BONDSMAN);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNull(result);
    }

    @Test
    public void testProcreationNoBlockers() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        Person spouse = mock(Person.class);
        Genealogy genealogy = mock(Genealogy.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isTryingToConceive()).thenReturn(true);
        when(person.isPregnant()).thenReturn(false);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.isDeployed()).thenReturn(false);
        when(person.isChild(date, true)).thenReturn(false);
        when(person.getAge(date)).thenReturn(21);
        when(person.isClanPersonnel()).thenReturn(false);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.hasSpouse()).thenReturn(true);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(spouse.isTryingToConceive()).thenReturn(true);
        when(spouse.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(spouse.isDeployed()).thenReturn(false);
        when(spouse.isChild(date, true)).thenReturn(false);
        when(spouse.isClanPersonnel()).thenReturn(false);
        when(spouse.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);

        // Act
        String result = procreation.canProcreate(date, person, true);

        // Assert
        assertNull(result);
    }

    @Test
    public void testAddPregnancy() {
        doCallRealMethod().when(mockProcreation).addPregnancy(any(), any(), any(), eq(false));
        doCallRealMethod().when(mockProcreation).addPregnancy(any(), any(), any(), anyInt(), eq(false));
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

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
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

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
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        final Person person = new Person(mockCampaign);

        when(mockProcreation.canProcreate(any(), any(), anyBoolean())).thenReturn("Pregnant");
        assertFalse(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));

        reset(mockProcreation);
        doCallRealMethod().when(mockProcreation).randomlyProcreates(any(), any());

        when(mockProcreation.canProcreate(any(), any(), anyBoolean())).thenReturn(null);
        when(mockProcreation.isUseRelationshiplessProcreation()).thenReturn(false);
        assertFalse(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));

        reset(mockProcreation);
        doCallRealMethod().when(mockProcreation).randomlyProcreates(any(), any());

        person.getGenealogy().setSpouse(mock(Person.class));
        when(mockProcreation.canProcreate(any(), any(), anyBoolean())).thenReturn(null);
        when(mockProcreation.procreation(any())).thenReturn(true);
        assertTrue(mockProcreation.randomlyProcreates(LocalDate.ofYearDay(3025, 1), person));
    }
}
