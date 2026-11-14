/*
 * Copyright (C) 2022-2026 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.BabySurnameStyle;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.personnel.lifeEvents.BirthAnnouncement;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(value = MockitoExtension.class)
public class AbstractProcreationTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    @Mock
    private AbstractProcreation mockProcreation;

    @BeforeEach
    public void beforeEach() {
        lenient().when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        lenient().when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_PRISONER_PROCREATION)).thenReturn(false);
        lenient().when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION)).thenReturn(false);
        lenient().when(mockCampaignOptions.get(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION)).thenReturn(false);
        lenient().when(mockCampaignOptions.get(CampaignOption.USE_PRISONER_PROCREATION)).thenReturn(false);
        lenient().when(mockCampaignOptions.get(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION)).thenReturn(false);
        lenient().when(mockCampaignOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE)).thenReturn(0);
        lenient().when(mockCampaignOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE)).thenReturn(0);
        lenient().when(mockCampaignOptions.get(CampaignOption.MULTIPLE_PREGNANCY_OCCURRENCES)).thenReturn(0);
        lenient().when(mockCampaignOptions.get(CampaignOption.IS_ENABLE_SALVAGE_FLAG_BY_DEFAULT)).thenReturn(false);
        lenient().when(mockCampaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION)).thenReturn(false);
        lenient().when(mockCampaignOptions.get(CampaignOption.USE_MATERNITY_LEAVE)).thenReturn(false);
    }

    //region Getters/Setters
    @Test
    public void testGettersAndSetters() {
        when(mockCampaignOptions.get(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.USE_PRISONER_PROCREATION)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_PRISONER_PROCREATION)).thenReturn(false);

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
        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        final Person mother = new Person(mockCampaign);
        final Person father = new Person(mockCampaign);

        given(mockCampaign.getPlayerForce().getHumanResources().getPerson(argThat(matchPersonUUID(father.getId())))).willReturn(father);

        when(mockCampaignOptions.get(CampaignOption.DETERMINE_FATHER_AT_BIRTH)).thenReturn(false);
        assertNull(mockProcreation.determineFather(mockCampaign, mother));

        mother.getExtraData().set(AbstractProcreation.PREGNANCY_FATHER_DATA, father.getId().toString());
        assertEquals(father, mockProcreation.determineFather(mockCampaign, mother));

        when(mockCampaignOptions.get(CampaignOption.DETERMINE_FATHER_AT_BIRTH)).thenReturn(true);
        assertEquals(father, mockProcreation.determineFather(mockCampaign, mother));

        mother.getGenealogy().setSpouse(father);
        assertEquals(father, mockProcreation.determineFather(mockCampaign, mother));
    }
    //endregion Determination Methods

    @Test
    public void testNotInterestInChildrenMale() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.MALE);
        when(person.isWantsChildren()).thenReturn(false);

        // Act
        String result = procreation.canProcreate(date, person, false);

        // Assert
        assertNotNull(result);
    }

    @Test
    public void testNotInterestInChildrenFemale() {
        // Arrange
        AbstractProcreation procreation = new RandomProcreation(mockCampaignOptions);

        Person person = mock(Person.class);
        LocalDate date = LocalDate.of(3025, 1, 1);

        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(person.isWantsChildren()).thenReturn(false);

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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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
        when(spouse.isWantsChildren()).thenReturn(false);

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

        when(person.isWantsChildren()).thenReturn(true);
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
        when(spouse.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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
        when(spouse.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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
        when(spouse.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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
        when(spouse.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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
        when(spouse.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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
        when(spouse.isWantsChildren()).thenReturn(true);
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

        when(person.isWantsChildren()).thenReturn(true);
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
        when(spouse.isWantsChildren()).thenReturn(true);
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
        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");

        final Person mother = new Person(mockCampaign);
        final Person father = new Person(mockCampaign);

        when(mockProcreation.determineNumberOfBabies(anyInt())).thenReturn(0);
        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, false);
        assertNull(mother.getExpectedDueDate());
        assertNull(mother.getDueDate());
        assertTrue(mother.getExtraData().isEmpty());

        when(mockCampaignOptions.get(CampaignOption.LOG_PROCREATION)).thenReturn(false);
        mockProcreation.addPregnancy(mockCampaign, LocalDate.ofYearDay(3025, 1), mother, 1, false);
        assertEquals(LocalDate.ofYearDay(3025, 281), mother.getExpectedDueDate());
        assertNotNull(mother.getDueDate());
        assertFalse(mother.getExtraData().isEmpty());
        assertNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA));
        assertNotNull(mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
        assertEquals(1, mother.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        when(mockCampaignOptions.get(CampaignOption.LOG_PROCREATION)).thenReturn(true);
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
        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
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
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any(), anyBoolean());

        final Person mockPerson = mock(Person.class);
        when(mockPerson.getGender()).thenReturn(Gender.MALE);
        mockProcreation.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockPerson, never()).isPregnant();
        verify(mockProcreation, never()).randomlyProcreates(any(), any());
        verify(mockProcreation, times(1)).processRandomPaternityCheck(mockCampaign,
              LocalDate.ofYearDay(3025, 1),
              mockPerson,
              false);
        verify(mockProcreation, times(1)).processRandomAdoptionCheck(mockCampaign,
              LocalDate.ofYearDay(3025, 1),
              mockPerson,
              false);
    }

    @Test
    public void testProcessNewWeek_ForPregnantFemale() {
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any());
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any(), anyBoolean());

        final Person mockPerson = mock(Person.class);

        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);
        when(mockPerson.isPregnant()).thenReturn(true);
        when(mockPerson.getDueDate()).thenReturn(LocalDate.ofYearDay(3025, 2));

        mockProcreation.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);

        verify(mockProcreation, never()).birth(any(), any(), any(), anyBoolean());
        verify(mockProcreation, never()).randomlyProcreates(any(), any());
    }

    @Test
    public void testProcessNewWeek_ForPregnantFemaleWithDueDate() {
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any());
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any(), anyBoolean());
        doNothing().when(mockProcreation).birth(any(), any(), any(), anyBoolean());

        final Person mockPerson = mock(Person.class);

        // Ensure proper stubbing
        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);
        when(mockPerson.isPregnant()).thenReturn(true);
        when(mockPerson.getDueDate()).thenReturn(LocalDate.ofYearDay(3025, 1));

        mockProcreation.processNewWeek(mockCampaign, LocalDate.ofYearDay(3025, 1), mockPerson);
        verify(mockProcreation, times(1)).birth(any(), any(), any(), anyBoolean());
        verify(mockProcreation, never()).randomlyProcreates(any(), any());
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testProcessNewWeek_PregnantActivePersonGoesOnMaternityLeave() {
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any());
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any(), anyBoolean());
        when(mockCampaignOptions.get(CampaignOption.USE_MATERNITY_LEAVE)).thenReturn(true);

        final LocalDate today = LocalDate.ofYearDay(3025, 1);
        final Person mockPerson = mock(Person.class);
        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);
        when(mockPerson.isPregnant()).thenReturn(true);
        when(mockPerson.getDueDate()).thenReturn(today.plusWeeks(10));
        when(mockPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);

        mockProcreation.processNewWeek(mockCampaign, today, mockPerson);

        verify(mockPerson, times(1)).changeStatus(mockCampaign, today, PersonnelStatus.ON_MATERNITY_LEAVE);
    }

    /**
     * Regression test: only {@link PersonnelStatus#ACTIVE} personnel should be moved onto maternity leave. Anyone in
     * another status (e.g. {@link PersonnelStatus#STUDENT}) must keep that status.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @ParameterizedTest
    @EnumSource(value = PersonnelStatus.class, names = { "ACTIVE" }, mode = EnumSource.Mode.EXCLUDE)
    public void testProcessNewWeek_PregnantNonActivePersonDoesNotGoOnMaternityLeave(PersonnelStatus status) {
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any());
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any(), anyBoolean());
        when(mockCampaignOptions.get(CampaignOption.USE_MATERNITY_LEAVE)).thenReturn(true);

        final LocalDate today = LocalDate.ofYearDay(3025, 1);
        final Person mockPerson = mock(Person.class);
        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);
        when(mockPerson.isPregnant()).thenReturn(true);
        when(mockPerson.getDueDate()).thenReturn(today.plusWeeks(10));
        when(mockPerson.getStatus()).thenReturn(status);

        mockProcreation.processNewWeek(mockCampaign, today, mockPerson);

        verify(mockPerson, never()).changeStatus(any(), any(), any());
    }

    /**
     * Regression test for issue 10851: during a new command's starting simulation, a mother due on or before the
     * campaign's date gives birth within the simulated history, so her leave would be over before play begins. Sending
     * her on leave anyway stripped her unit and tech assignments, which the birth never restored.
     *
     * @param weeksFromCampaignStart the due date relative to the campaign's date: before it, or on it
     */
    @ParameterizedTest
    @ValueSource(ints = { -10, 0 })
    public void testProcessNewWeek_StartingSimulationSkipsLeaveEndingBeforeCampaignStart(int weeksFromCampaignStart) {
        doCallRealMethod().when(mockProcreation).processNewWeek(any(), any(), any(), anyBoolean());
        when(mockCampaignOptions.get(CampaignOption.USE_MATERNITY_LEAVE)).thenReturn(true);

        final LocalDate campaignStart = LocalDate.ofYearDay(3025, 1);
        when(mockCampaign.getLocalDate()).thenReturn(campaignStart);
        final LocalDate dueDate = campaignStart.plusWeeks(weeksFromCampaignStart);
        final LocalDate simulatedWeek = dueDate.minusWeeks(10);
        final Person mockPerson = mock(Person.class);
        when(mockPerson.getGender()).thenReturn(Gender.FEMALE);
        when(mockPerson.isPregnant()).thenReturn(true);
        when(mockPerson.getDueDate()).thenReturn(dueDate);
        lenient().when(mockPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);

        mockProcreation.processNewWeek(mockCampaign, simulatedWeek, mockPerson, true);

        verify(mockPerson, never()).changeStatus(any(), any(), any());
    }

    //region Random Procreation
    @Test
    public void testRandomlyProcreates() {
        doCallRealMethod().when(mockProcreation).randomlyProcreates(any(), any());
        Faction campaignFaction = mock(Faction.class);
        when(mockCampaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
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

    /**
     * Regression test for 'Dr Baby': background (historic) births used to keep whatever education and titles the
     * personnel generator rolled for the dependent's random adult age, so newborns could arrive with doctorates.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testBirthHistoric_babiesHaveNoEducationOrTitles() {
        when(mockCampaignOptions.get(CampaignOption.BABY_SURNAME_STYLE)).thenReturn(BabySurnameStyle.MOTHERS);
        when(mockCampaignOptions.get(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.LOG_PROCREATION)).thenReturn(false);

        // Simulate the generator handing back a dependent that was rolled as a highly educated adult
        when(mockCampaign.getPlayerForce().getHumanResources().newDependent(any(), any(), any(), any()))
              .thenAnswer(invocation -> {
                  Person generatedDependent = new Person(mockCampaign);
                  generatedDependent.setDateOfBirth(LocalDate.of(2990, 1, 1));
                  generatedDependent.setEduHighestEducation(EducationLevel.DOCTORATE);
                  generatedDependent.setPreNominal("Dr");
                  generatedDependent.setPostNominal("PhD");
                  return generatedDependent;
              });

        final LocalDate dueDate = LocalDate.of(3025, 6, 1);
        final Person mother = new Person(mockCampaign);
        mother.setDueDate(dueDate);
        mother.getExtraData().set(AbstractProcreation.PREGNANCY_CHILDREN_DATA, 3);

        final AbstractProcreation procreation = new DisabledRandomProcreation(mockCampaignOptions);
        final List<Person> babies = procreation.birthHistoric(mockCampaign, dueDate, mother, null);

        assertEquals(3, babies.size());
        for (Person baby : babies) {
            assertEquals(dueDate, baby.getDateOfBirth());
            assertEquals(EducationLevel.EARLY_CHILDHOOD, baby.getEduHighestEducation());
            assertEquals("", baby.getPreNominal());
            assertEquals("", baby.getPostNominal());
        }
    }

    /**
     * A normal birth leaves the mother in postpartum recovery and shows the birth announcement.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testBirth_appliesPostpartumAndAnnouncesOutsideStartingSimulation() {
        final Person mother = prepareBirth();

        try (MockedConstruction<BirthAnnouncement> announcements = mockConstruction(BirthAnnouncement.class)) {
            new DisabledRandomProcreation(mockCampaignOptions).birth(mockCampaign, LocalDate.of(3025, 6, 1), mother,
                  false);

            assertEquals(1, mother.getHits());
            assertEquals(1, announcements.constructed().size());
        }
    }

    /**
     * A birth during a new command's starting simulation spares the mother postpartum recovery and shows no birth
     * announcement.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testBirth_skipsPostpartumAndAnnouncementDuringStartingSimulation() {
        final Person mother = prepareBirth();

        try (MockedConstruction<BirthAnnouncement> announcements = mockConstruction(BirthAnnouncement.class)) {
            new DisabledRandomProcreation(mockCampaignOptions).birth(mockCampaign, LocalDate.of(3025, 6, 1), mother,
                  true);

            assertEquals(0, mother.getHits());
            assertTrue(announcements.constructed().isEmpty());
        }
    }

    /**
     * A child with only a known father takes their surname from him, as there is no mother to fall back on.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testCreateChild_fatherOnlyUsesFathersSurname() {
        when(mockCampaignOptions.get(CampaignOption.BABY_SURNAME_STYLE)).thenReturn(BabySurnameStyle.MOTHERS);
        when(mockCampaignOptions.get(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN)).thenReturn(false);
        when(mockCampaign.getPlayerForce().getHumanResources().newDependent(any(), any(), any(), any()))
              .thenAnswer(invocation -> new Person(mockCampaign));

        final Person father = new Person(mockCampaign);
        father.setGender(Gender.MALE);
        father.setSurname("Kerensky");

        final LocalDate dateOfBirth = LocalDate.of(3020, 3, 14);
        final Person child = new DisabledRandomProcreation(mockCampaignOptions).createChild(mockCampaign,
              dateOfBirth, null, father, null, null);

        assertEquals("Kerensky", child.getSurname());
        assertEquals(dateOfBirth, child.getDateOfBirth());
        assertEquals(EducationLevel.EARLY_CHILDHOOD, child.getEduHighestEducation());
        // Creating the child must not link them; that is left to the caller
        assertTrue(child.getGenealogy().getParents().isEmpty());
    }

    /**
     * A child can't be created without at least one known parent.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testCreateChild_noParentsThrows() {
        final AbstractProcreation procreation = new DisabledRandomProcreation(mockCampaignOptions);

        assertThrows(IllegalArgumentException.class,
              () -> procreation.createChild(mockCampaign, LocalDate.of(3020, 3, 14), null, null, null, null));
    }

    /**
     * Linking records the relationship on both sides, and an unknown parent is ignored.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testLinkParentAndChild() {
        final Person parent = new Person(mockCampaign);
        final Person child = new Person(mockCampaign);

        AbstractProcreation.linkParentAndChild(null, child);
        assertTrue(child.getGenealogy().getParents().isEmpty());

        AbstractProcreation.linkParentAndChild(parent, child);
        assertEquals(List.of(parent), child.getGenealogy().getParents());
        assertEquals(List.of(child), parent.getGenealogy().getChildren());
    }

    /**
     * Changing the number of babies only affects pregnancies, and stays between a single baby and decuplets.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testSetPregnancySize() {
        final AbstractProcreation procreation = new DisabledRandomProcreation(mockCampaignOptions);
        final Person person = new Person(mockCampaign);

        procreation.setPregnancySize(person, 2);
        assertNull(person.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        person.setDueDate(LocalDate.of(3025, 6, 1));
        procreation.setPregnancySize(person, 3);
        assertEquals(3, person.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        procreation.setPregnancySize(person, 0);
        assertEquals(1, person.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));

        procreation.setPregnancySize(person, AbstractProcreation.MAXIMUM_PREGNANCY_SIZE + 5);
        assertEquals(AbstractProcreation.MAXIMUM_PREGNANCY_SIZE,
              person.getExtraData().get(AbstractProcreation.PREGNANCY_CHILDREN_DATA));
    }

    //region Children Joining Later in Life

    /**
     * A parent's first possible biological child is born a full pregnancy after their eighteenth birthday.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testGetEarliestChildDateOfBirth() {
        final Person parent = mock(Person.class);
        when(parent.getDateOfBirth()).thenReturn(LocalDate.of(3000, 1, 1));

        assertEquals(LocalDate.of(3018, 1, 1).plusDays(MHQConstants.PREGNANCY_STANDARD_DURATION),
              AbstractProcreation.getEarliestChildDateOfBirth(parent));
    }

    /**
     * The oldest biological child is limited by the parent's adult life, and never older than a minor.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testGetMaximumBiologicalChildAge() {
        final LocalDate today = LocalDate.of(3025, 1, 1);
        final Person parent = mock(Person.class);

        // Eighteen today, so too young for any child
        when(parent.getDateOfBirth()).thenReturn(LocalDate.of(3007, 1, 1));
        assertEquals(-1, AbstractProcreation.getMaximumBiologicalChildAge(today, parent));

        // Twenty-five today, so their first child could be born a little over six years ago
        when(parent.getDateOfBirth()).thenReturn(LocalDate.of(3000, 1, 1));
        assertEquals(6, AbstractProcreation.getMaximumBiologicalChildAge(today, parent));

        // Sixty today, which would allow a child far older than a minor
        when(parent.getDateOfBirth()).thenReturn(LocalDate.of(2965, 1, 1));
        assertEquals(AbstractProcreation.MAXIMUM_ARRIVING_CHILD_AGE,
              AbstractProcreation.getMaximumBiologicalChildAge(today, parent));
    }

    /**
     * A random date of birth gives the requested age, and never falls before the earliest permitted date.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testGetRandomDateOfBirth() {
        final LocalDate today = LocalDate.of(3025, 6, 15);

        for (int age = 0; age <= AbstractProcreation.MAXIMUM_ARRIVING_CHILD_AGE; age++) {
            for (int attempt = 0; attempt < 20; attempt++) {
                final LocalDate dateOfBirth = AbstractProcreation.getRandomDateOfBirth(today, age, null);
                assertEquals(age, ChronoUnit.YEARS.between(dateOfBirth, today));
            }
        }

        final LocalDate earliestDateOfBirth = LocalDate.of(3021, 3, 1);
        for (int attempt = 0; attempt < 20; attempt++) {
            final LocalDate dateOfBirth = AbstractProcreation.getRandomDateOfBirth(today, 4, earliestDateOfBirth);
            assertFalse(dateOfBirth.isBefore(earliestDateOfBirth));
            assertEquals(4, ChronoUnit.YEARS.between(dateOfBirth, today));
        }
    }

    /**
     * Random paternity does nothing unless the campaign enables it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testIsEligibleForRandomPaternity_disabled() {
        when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_PATERNITY)).thenReturn(false);

        final AbstractProcreation procreation = new DisabledRandomProcreation(mockCampaignOptions);

        assertFalse(procreation.isEligibleForRandomPaternity(mockCampaign,
              LocalDate.of(3025, 1, 1),
              mock(Person.class)));
    }

    /**
     * A married man's children arrive through his spouse's pregnancy, not random paternity.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testIsEligibleForRandomPaternity_married() {
        when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_PATERNITY)).thenReturn(true);

        final Genealogy genealogy = mock(Genealogy.class);
        when(genealogy.hasSpouse()).thenReturn(true);
        final Person person = mock(Person.class);
        when(person.getGender()).thenReturn(Gender.MALE);
        when(person.getGenealogy()).thenReturn(genealogy);

        final AbstractProcreation procreation = new DisabledRandomProcreation(mockCampaignOptions);

        assertFalse(procreation.isEligibleForRandomPaternity(mockCampaign, LocalDate.of(3025, 1, 1), person));
    }

    /**
     * An unmarried man who could otherwise procreate is eligible, unless he is too young to have any child.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testIsEligibleForRandomPaternity_unmarried() {
        when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_PATERNITY)).thenReturn(true);

        final LocalDate today = LocalDate.of(3025, 1, 1);
        final Genealogy genealogy = mock(Genealogy.class);
        when(genealogy.hasSpouse()).thenReturn(false);
        final Person person = mock(Person.class);
        when(person.getGender()).thenReturn(Gender.MALE);
        when(person.getGenealogy()).thenReturn(genealogy);

        final AbstractProcreation procreation = spy(new DisabledRandomProcreation(mockCampaignOptions));
        doReturn(null).when(procreation).canProcreate(today, person, true);

        when(person.getDateOfBirth()).thenReturn(LocalDate.of(3000, 1, 1));
        assertTrue(procreation.isEligibleForRandomPaternity(mockCampaign, today, person));

        // Just turned eighteen, so no child of his could have been born yet
        when(person.getDateOfBirth()).thenReturn(LocalDate.of(3007, 1, 1));
        assertFalse(procreation.isEligibleForRandomPaternity(mockCampaign, today, person));
    }

    /**
     * Random adoption does nothing unless the campaign enables it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testIsEligibleForRandomAdoption_disabled() {
        when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_SAME_SEX_ADOPTION)).thenReturn(false);

        final AbstractProcreation procreation = new DisabledRandomProcreation(mockCampaignOptions);

        assertFalse(procreation.isEligibleForRandomAdoption(mockCampaign,
              LocalDate.of(3025, 1, 1),
              mock(Person.class)));
    }

    /**
     * Unmarried people and opposite-sex couples aren't eligible for random adoption.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testIsEligibleForRandomAdoption_unmarriedOrOppositeSex() {
        when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_SAME_SEX_ADOPTION)).thenReturn(true);

        final LocalDate today = LocalDate.of(3025, 1, 1);
        final AbstractProcreation procreation = new DisabledRandomProcreation(mockCampaignOptions);

        final Person person = mock(Person.class);
        final Genealogy genealogy = mock(Genealogy.class);
        when(person.getGenealogy()).thenReturn(genealogy);

        when(genealogy.getSpouse()).thenReturn(null);
        assertFalse(procreation.isEligibleForRandomAdoption(mockCampaign, today, person));

        final Person spouse = mock(Person.class);
        when(person.getGender()).thenReturn(Gender.FEMALE);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(genealogy.getSpouse()).thenReturn(spouse);
        assertFalse(procreation.isEligibleForRandomAdoption(mockCampaign, today, person));
    }

    /**
     * A same-sex couple who could otherwise procreate is eligible, but only checked from one spouse's side, so they
     * don't get two rolls a week.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @ParameterizedTest
    @EnumSource(value = Gender.class, names = { "MALE", "FEMALE" })
    public void testIsEligibleForRandomAdoption_sameSex(final Gender gender) {
        when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_SAME_SEX_ADOPTION)).thenReturn(true);

        final LocalDate today = LocalDate.of(3025, 1, 1);
        final Person lowerIdSpouse = mockSpouseForAdoption(gender, new UUID(0, 1));
        final Person higherIdSpouse = mockSpouseForAdoption(gender, new UUID(0, 2));
        when(lowerIdSpouse.getGenealogy().getSpouse()).thenReturn(higherIdSpouse);
        when(higherIdSpouse.getGenealogy().getSpouse()).thenReturn(lowerIdSpouse);

        final AbstractProcreation procreation = spy(new DisabledRandomProcreation(mockCampaignOptions));
        doReturn(null).when(procreation).canProcreate(today, lowerIdSpouse, false);
        doReturn(null).when(procreation).canProcreate(today, higherIdSpouse, false);

        assertTrue(procreation.isEligibleForRandomAdoption(mockCampaign, today, lowerIdSpouse));
        assertFalse(procreation.isEligibleForRandomAdoption(mockCampaign, today, higherIdSpouse));
    }

    /**
     * A same-sex couple can't adopt if either spouse is unable to procreate, such as being deployed.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    public void testIsEligibleForRandomAdoption_spouseCannotProcreate() {
        when(mockCampaignOptions.get(CampaignOption.USE_RANDOM_SAME_SEX_ADOPTION)).thenReturn(true);

        final LocalDate today = LocalDate.of(3025, 1, 1);
        final Person person = mock(Person.class);
        final Genealogy genealogy = mock(Genealogy.class);
        final Person spouse = mock(Person.class);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(genealogy.getSpouse()).thenReturn(spouse);
        when(person.getGender()).thenReturn(Gender.MALE);
        when(spouse.getGender()).thenReturn(Gender.MALE);
        when(person.getId()).thenReturn(new UUID(0, 1));
        when(spouse.getId()).thenReturn(new UUID(0, 2));

        final AbstractProcreation procreation = spy(new DisabledRandomProcreation(mockCampaignOptions));
        doReturn(null).when(procreation).canProcreate(today, person, false);
        doReturn("Deployed").when(procreation).canProcreate(today, spouse, false);

        assertFalse(procreation.isEligibleForRandomAdoption(mockCampaign, today, person));
    }

    /**
     * Mocks a married person for the random adoption tests, who is free and not clan personnel.
     *
     * @param gender the person's gender
     * @param id     the person's ID
     *
     * @return the person, whose genealogy is a mock with no spouse set
     */
    private static Person mockSpouseForAdoption(final Gender gender, final UUID id) {
        final Person person = mock(Person.class);
        final Genealogy genealogy = mock(Genealogy.class);
        when(person.getGenealogy()).thenReturn(genealogy);
        when(person.getGender()).thenReturn(gender);
        when(person.getId()).thenReturn(id);
        lenient().when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        return person;
    }
    //endregion Children Joining Later in Life

    /**
     * Stubs the options a single, fatherless birth reads, with birth announcements enabled.
     *
     * @return the mother, uninjured
     */
    private Person prepareBirth() {
        when(mockCampaignOptions.get(CampaignOption.DETERMINE_FATHER_AT_BIRTH)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.USE_FOUNDER_PLOT_ARMOR)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.BABY_SURNAME_STYLE)).thenReturn(BabySurnameStyle.MOTHERS);
        when(mockCampaignOptions.get(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.LOG_PROCREATION)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.ASSIGN_NON_PRISONER_BABIES_FOUNDER_TAG)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.ASSIGN_CHILDREN_OF_FOUNDERS_FOUNDER_TAG)).thenReturn(false);
        when(mockCampaignOptions.get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_BIRTHS)).thenReturn(true);
        // Only read if the random loyalty change actually moves someone's loyalty
        lenient().when(mockCampaignOptions.get(CampaignOption.USE_LOYALTY_MODIFIERS)).thenReturn(false);

        when(mockCampaign.getPlayerForce().getHumanResources().newDependent(any(), any(), any(), any()))
              .thenAnswer(invocation -> new Person(mockCampaign));

        final Person mother = new Person(mockCampaign);
        mother.setGender(Gender.FEMALE);
        assertEquals(0, mother.getHits());
        return mother;
    }
}
