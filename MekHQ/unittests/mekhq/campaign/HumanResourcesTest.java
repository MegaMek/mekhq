/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import megamek.Version;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import testUtilities.MHQTestUtilities;

public class HumanResourcesTest {

    private CampaignOptions campaignOptions;
    private LocalDate today;
    private Campaign campaign;

    @BeforeAll
    static void globalSetup() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @BeforeEach
    void setup() {
        campaignOptions = mock(CampaignOptions.class);
        today = LocalDate.of(3067, 1, 1);
        campaign = MHQTestUtilities.getTestCampaign();
    }

    /**
     * Tests for {@link HumanResources#getDoctors(Collection)}
     */
    @Nested
    class GetDoctors {

        @Test
        void emptyInputReturnsEmptyList() {
            // Arrange
            List<Person> people = List.of();

            // Act
            List<Person> result = HumanResources.getDoctors(people);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void doctorsAreIncluded() {
            // Arrange
            Person doctor = mock(Person.class);
            when(doctor.isDoctor()).thenReturn(true);

            // Act
            List<Person> result = HumanResources.getDoctors(List.of(doctor));

            // Assert
            assertEquals(List.of(doctor), result);
        }

        @Test
        void nonDoctorsAreExcluded() {
            // Arrange
            Person nonDoctor = mock(Person.class);
            when(nonDoctor.isDoctor()).thenReturn(false);

            // Act
            List<Person> result = HumanResources.getDoctors(List.of(nonDoctor));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void mixedInputReturnsOnlyDoctors() {
            // Arrange
            Person doctor = mock(Person.class);
            when(doctor.isDoctor()).thenReturn(true);

            Person mekwarrior = mock(Person.class);
            when(mekwarrior.isDoctor()).thenReturn(false);

            Person tech = mock(Person.class);
            when(tech.isDoctor()).thenReturn(false);

            // Act
            List<Person> result = HumanResources.getDoctors(List.of(doctor, mekwarrior, tech));

            // Assert
            assertEquals(List.of(doctor), result);
        }
    }

    /**
     * Tests for {@link HumanResources#getPatients(Collection)}
     */
    @Nested
    class GetPatients {

        @Test
        void emptyInputReturnsEmptyList() {
            // Arrange
            List<Person> people = List.of();

            // Act
            List<Person> result = HumanResources.getPatients(people);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void personNeedingFixingIsIncluded() {
            // Arrange
            Person injured = mock(Person.class);
            when(injured.needsFixing()).thenReturn(true);

            // Act
            List<Person> result = HumanResources.getPatients(List.of(injured));

            // Assert
            assertEquals(List.of(injured), result);
        }

        @Test
        void healthyPersonIsExcluded() {
            // Arrange
            Person healthy = mock(Person.class);
            when(healthy.needsFixing()).thenReturn(false);

            // Act
            List<Person> result = HumanResources.getPatients(List.of(healthy));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void mixedInputReturnsOnlyPatients() {
            // Arrange
            Person injured = mock(Person.class);
            when(injured.needsFixing()).thenReturn(true);

            Person healthy = mock(Person.class);
            when(healthy.needsFixing()).thenReturn(false);

            // Act
            List<Person> result = HumanResources.getPatients(List.of(injured, healthy));

            // Assert
            assertEquals(List.of(injured), result);
        }
    }

    /**
     * Tests for {@link HumanResources#getAdmins(Collection)}
     */
    @Nested
    class GetAdmins {

        @Test
        void emptyInputReturnsEmptyList() {
            // Arrange
            List<Person> people = List.of();

            // Act
            List<Person> result = HumanResources.getAdmins(people);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void administratorIsIncluded() {
            // Arrange
            Person admin = mock(Person.class);
            when(admin.isAdministrator()).thenReturn(true);

            // Act
            List<Person> result = HumanResources.getAdmins(List.of(admin));

            // Assert
            assertEquals(List.of(admin), result);
        }

        @Test
        void nonAdminIsExcluded() {
            // Arrange
            Person pilot = mock(Person.class);
            when(pilot.isAdministrator()).thenReturn(false);

            // Act
            List<Person> result = HumanResources.getAdmins(List.of(pilot));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void mixedInputReturnsOnlyAdmins() {
            // Arrange
            Person admin = mock(Person.class);
            when(admin.isAdministrator()).thenReturn(true);

            Person pilot = mock(Person.class);
            when(pilot.isAdministrator()).thenReturn(false);

            Person tech = mock(Person.class);
            when(tech.isAdministrator()).thenReturn(false);

            // Act
            List<Person> result = HumanResources.getAdmins(List.of(admin, pilot, tech));

            // Assert
            assertEquals(List.of(admin), result);
        }
    }

    /**
     * Tests for {@link HumanResources#getActiveDependents(Collection)}
     */
    @Nested
    class GetActiveDependents {

        @Test
        void emptyInputReturnsEmptyList() {
            // Arrange
            List<Person> people = List.of();

            // Act
            List<Person> result = HumanResources.getActiveDependents(people);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void activeDependentIsIncluded() {
            // Arrange
            PersonnelRole dependentRole = mock(PersonnelRole.class);
            when(dependentRole.isDependent()).thenReturn(true);

            PersonnelStatus activeStatus = mock(PersonnelStatus.class);
            when(activeStatus.isActiveFlexible()).thenReturn(true);

            Person dependent = mock(Person.class);
            when(dependent.getPrimaryRole()).thenReturn(dependentRole);
            when(dependent.getStatus()).thenReturn(activeStatus);

            // Act
            List<Person> result = HumanResources.getActiveDependents(List.of(dependent));

            // Assert
            assertEquals(List.of(dependent), result);
        }

        @Test
        void inactiveDependentIsExcluded() {
            // Arrange
            PersonnelRole dependentRole = mock(PersonnelRole.class);
            when(dependentRole.isDependent()).thenReturn(true);

            PersonnelStatus retiredStatus = mock(PersonnelStatus.class);
            when(retiredStatus.isActiveFlexible()).thenReturn(false);

            Person dependent = mock(Person.class);
            when(dependent.getPrimaryRole()).thenReturn(dependentRole);
            when(dependent.getStatus()).thenReturn(retiredStatus);

            // Act
            List<Person> result = HumanResources.getActiveDependents(List.of(dependent));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void activeNonDependentIsExcluded() {
            // Arrange
            PersonnelRole pilotRole = mock(PersonnelRole.class);
            when(pilotRole.isDependent()).thenReturn(false);

            PersonnelStatus activeStatus = mock(PersonnelStatus.class);
            when(activeStatus.isActiveFlexible()).thenReturn(true);

            Person pilot = mock(Person.class);
            when(pilot.getPrimaryRole()).thenReturn(pilotRole);
            when(pilot.getStatus()).thenReturn(activeStatus);

            // Act
            List<Person> result = HumanResources.getActiveDependents(List.of(pilot));

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    /**
     * Tests for {@link HumanResources#getCurrentPrisoners(Collection)}
     */
    @Nested
    class GetCurrentPrisoners {

        @Test
        void emptyInputReturnsEmptyList() {
            // Arrange
            List<Person> people = List.of();

            // Act
            List<Person> result = HumanResources.getCurrentPrisoners(people);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void currentPrisonerIsIncluded() {
            // Arrange
            Person prisoner = mock(Person.class);
            when(prisoner.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);

            // Act
            List<Person> result = HumanResources.getCurrentPrisoners(List.of(prisoner));

            // Assert
            assertEquals(List.of(prisoner), result);
        }

        @Test
        void prisonerDefectorIsIncluded() {
            // Arrange
            Person defector = mock(Person.class);
            when(defector.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER_DEFECTOR);

            // Act
            List<Person> result = HumanResources.getCurrentPrisoners(List.of(defector));

            // Assert
            assertEquals(List.of(defector), result);
        }

        @Test
        void freePersonIsExcluded() {
            // Arrange
            Person freePerson = mock(Person.class);
            when(freePerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);

            // Act
            List<Person> result = HumanResources.getCurrentPrisoners(List.of(freePerson));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void mixedInputReturnsOnlyPrisoners() {
            // Arrange
            Person prisoner = mock(Person.class);
            when(prisoner.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);

            Person freePerson = mock(Person.class);
            when(freePerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);

            // Act
            List<Person> result = HumanResources.getCurrentPrisoners(List.of(prisoner, freePerson));

            // Assert
            assertEquals(List.of(prisoner), result);
        }
    }

    /**
     * Tests for {@link HumanResources#getSalaryEligiblePersonnel(Collection)}
     */
    @Nested
    class GetSalaryEligiblePersonnel {

        @Test
        void emptyInputReturnsEmptyList() {
            // Arrange
            List<Person> people = List.of();

            // Act
            List<Person> result = HumanResources.getSalaryEligiblePersonnel(people);

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void salaryEligiblePersonIsIncluded() {
            // Arrange
            PersonnelStatus activeStatus = mock(PersonnelStatus.class);
            when(activeStatus.isSalaryEligible()).thenReturn(true);

            Person activePerson = mock(Person.class);
            when(activePerson.getStatus()).thenReturn(activeStatus);

            // Act
            List<Person> result = HumanResources.getSalaryEligiblePersonnel(List.of(activePerson));

            // Assert
            assertEquals(List.of(activePerson), result);
        }

        @Test
        void ineligiblePersonIsExcluded() {
            // Arrange
            PersonnelStatus retiredStatus = mock(PersonnelStatus.class);
            when(retiredStatus.isSalaryEligible()).thenReturn(false);

            Person retiree = mock(Person.class);
            when(retiree.getStatus()).thenReturn(retiredStatus);

            // Act
            List<Person> result = HumanResources.getSalaryEligiblePersonnel(List.of(retiree));

            // Assert
            assertTrue(result.isEmpty());
        }

        @Test
        void mixedInputReturnsOnlyEligible() {
            // Arrange
            PersonnelStatus activeStatus = mock(PersonnelStatus.class);
            when(activeStatus.isSalaryEligible()).thenReturn(true);

            PersonnelStatus retiredStatus = mock(PersonnelStatus.class);
            when(retiredStatus.isSalaryEligible()).thenReturn(false);

            Person active = mock(Person.class);
            when(active.getStatus()).thenReturn(activeStatus);

            Person retired = mock(Person.class);
            when(retired.getStatus()).thenReturn(retiredStatus);

            // Act
            List<Person> result = HumanResources.getSalaryEligiblePersonnel(List.of(active, retired));

            // Assert
            assertEquals(List.of(active), result);
        }
    }

    /**
     * Tests for
     * {@link HumanResources#getSeniorAdminPerson(Collection, AdministratorSpecialization, CampaignOptions, boolean,
     * LocalDate)}
     */
    @Nested
    class GetSeniorAdminPerson {

        @Test
        void emptyInputReturnsNull() {
            // Arrange
            List<Person> people = List.of();

            // Act
            Person result = HumanResources.getSeniorAdminPerson(people,
                  AdministratorSpecialization.COMMAND, campaignOptions, false, today);

            // Assert
            assertNull(result);
        }

        @Test
        void singleCommandAdminIsReturned() {
            // Arrange
            PersonnelRole commandRole = mock(PersonnelRole.class);
            when(commandRole.isAdministratorCommand()).thenReturn(true);

            PersonnelRole none = mock(PersonnelRole.class);
            when(none.isAdministratorCommand()).thenReturn(false);

            Person admin = mock(Person.class);
            when(admin.getPrimaryRole()).thenReturn(commandRole);
            when(admin.getSecondaryRole()).thenReturn(none);

            // Act
            Person result = HumanResources.getSeniorAdminPerson(List.of(admin),
                  AdministratorSpecialization.COMMAND, campaignOptions, false, today);

            // Assert
            assertEquals(admin, result);
        }

        @Test
        void higherRankingAdminWins() {
            // Arrange
            PersonnelRole hrRole = mock(PersonnelRole.class);
            when(hrRole.isAdministratorHR()).thenReturn(true);

            PersonnelRole none = mock(PersonnelRole.class);
            when(none.isAdministratorHR()).thenReturn(false);

            Person junior = mock(Person.class);
            when(junior.getPrimaryRole()).thenReturn(hrRole);
            when(junior.getSecondaryRole()).thenReturn(none);
            when(junior.outRanksUsingSkillTiebreaker(any(), anyBoolean(), any(), any())).thenReturn(false);

            Person senior = mock(Person.class);
            when(senior.getPrimaryRole()).thenReturn(hrRole);
            when(senior.getSecondaryRole()).thenReturn(none);
            when(senior.outRanksUsingSkillTiebreaker(any(), anyBoolean(), any(), any())).thenReturn(true);

            // Act
            Person result = HumanResources.getSeniorAdminPerson(List.of(junior, senior),
                  AdministratorSpecialization.HR, campaignOptions, false, today);

            // Assert
            assertEquals(senior, result);
        }

        @Test
        void nonMatchingSpecializationIsExcluded() {
            // Arrange — person is logistics admin, we ask for command admin
            PersonnelRole logisticsRole = mock(PersonnelRole.class);
            when(logisticsRole.isAdministratorCommand()).thenReturn(false);
            when(logisticsRole.isAdministratorLogistics()).thenReturn(true);

            PersonnelRole none = mock(PersonnelRole.class);
            when(none.isAdministratorCommand()).thenReturn(false);

            Person logisticsAdmin = mock(Person.class);
            when(logisticsAdmin.getPrimaryRole()).thenReturn(logisticsRole);
            when(logisticsAdmin.getSecondaryRole()).thenReturn(none);

            // Act
            Person result = HumanResources.getSeniorAdminPerson(List.of(logisticsAdmin),
                  AdministratorSpecialization.COMMAND, campaignOptions, false, today);

            // Assert
            assertNull(result);
        }
    }

    /**
     * Tests for {@link HumanResources#getSeniorPerson(Collection, CampaignOptions, boolean, LocalDate)}
     */
    @Nested
    class GetSeniorMedicalPerson {

        @Test
        void emptyInputReturnsNull() {
            // Arrange
            List<Person> people = List.of();

            // Act
            Person result = HumanResources.getSeniorPerson(people, campaignOptions, false, today);

            // Assert
            assertNull(result);
        }

        @Test
        void singleDoctorIsReturned() {
            // Arrange
            Person doctor = mock(Person.class);

            // Act
            Person result = HumanResources.getSeniorPerson(List.of(doctor), campaignOptions, false, today);

            // Assert
            assertEquals(doctor, result);
        }

        @Test
        void higherRankingDoctorWins() {
            // Arrange
            Person junior = mock(Person.class);
            when(junior.outRanksUsingSkillTiebreaker(any(), anyBoolean(), any(), any())).thenReturn(false);

            Person senior = mock(Person.class);
            when(senior.outRanksUsingSkillTiebreaker(any(), anyBoolean(), any(), any())).thenReturn(true);

            // Act
            Person result = HumanResources.getSeniorPerson(List.of(junior, senior),
                  campaignOptions, false, today);

            // Assert
            assertEquals(senior, result);
        }
    }

    /**
     * Tests for {@link HumanResources#findTopCommanders(Collection, CampaignOptions, boolean, LocalDate)}
     */
    @Nested
    class FindTopCommanders {

        @Test
        void emptyInputReturnsTwoNulls() {
            // Arrange
            List<Person> people = List.of();

            // Act
            Person[] result = HumanResources.findTopCommanders(people, campaignOptions, false, today);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.length);
            assertNull(result[0]);
            assertNull(result[1]);
        }

        @Test
        void flaggedCommanderAndSicAreReturnedWithoutRankCheck() {
            // Arrange
            Person commander = mock(Person.class);
            when(commander.isCommander()).thenReturn(true);
            when(commander.isSecondInCommand()).thenReturn(false);

            Person sic = mock(Person.class);
            when(sic.isCommander()).thenReturn(false);
            when(sic.isSecondInCommand()).thenReturn(true);

            // Act
            Person[] result = HumanResources.findTopCommanders(List.of(sic, commander),
                  campaignOptions, false, today);

            // Assert
            assertEquals(commander, result[0]);
            assertEquals(sic, result[1]);
        }

        @Test
        void unflaggedHighestRankerBecomesCommander() {
            // Arrange
            Person highRanker = mock(Person.class);
            when(highRanker.isCommander()).thenReturn(false);
            when(highRanker.isSecondInCommand()).thenReturn(false);
            when(highRanker.outRanksUsingSkillTiebreaker(any(), anyBoolean(), any(), any())).thenReturn(true);

            Person lowRanker = mock(Person.class);
            when(lowRanker.isCommander()).thenReturn(false);
            when(lowRanker.isSecondInCommand()).thenReturn(false);
            when(lowRanker.outRanksUsingSkillTiebreaker(any(), anyBoolean(), any(), any())).thenReturn(false);

            // Act
            Person[] result = HumanResources.findTopCommanders(List.of(lowRanker, highRanker),
                  campaignOptions, false, today);

            // Assert
            assertNotNull(result[0]);
            assertNotNull(result[1]);
        }

        @Test
        void singlePersonBecomesCommanderWithNullSic() {
            // Arrange
            Person only = mock(Person.class);
            when(only.isCommander()).thenReturn(false);
            when(only.isSecondInCommand()).thenReturn(false);

            // Act
            Person[] result = HumanResources.findTopCommanders(List.of(only), campaignOptions, false, today);

            // Assert
            assertEquals(only, result[0]);
            assertNull(result[1]);
        }
    }

    /**
     * Tests for {@link HumanResources#findBestAtSkill(Collection, String, CampaignOptions, boolean, LocalDate)}
     */
    @Nested
    class FindBestAtSkill {

        @Test
        void emptyInputReturnsNull() {
            // Arrange
            List<Person> people = List.of();
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            // Act
            Person result = HumanResources.findBestAtSkill(people, "Negotiation", campaignOptions, false, today);

            // Assert
            assertNull(result);
        }

        @Test
        void personWithSkillIsReturned() {
            // Arrange
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            Skill skill = mock(Skill.class);
            when(skill.getTotalSkillLevel(any())).thenReturn(5);

            Person person = mock(Person.class);
            when(person.getSkill("Negotiation")).thenReturn(skill);
            when(person.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            // Act
            Person result = HumanResources.findBestAtSkill(List.of(person), "Negotiation",
                  campaignOptions, false, today);

            // Assert
            assertEquals(person, result);
        }

        @Test
        void personWithoutSkillIsNotReturned() {
            // Arrange
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            Person person = mock(Person.class);
            when(person.getSkill(anyString())).thenReturn(null);

            // Act
            Person result = HumanResources.findBestAtSkill(List.of(person), "Negotiation",
                  campaignOptions, false, today);

            // Assert
            assertNull(result);
        }

        @Test
        void personWithHigherSkillWins() {
            // Arrange
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            Skill lowSkill = mock(Skill.class);
            when(lowSkill.getTotalSkillLevel(any())).thenReturn(3);

            Skill highSkill = mock(Skill.class);
            when(highSkill.getTotalSkillLevel(any())).thenReturn(7);

            Person weaker = mock(Person.class);
            when(weaker.getSkill("Negotiation")).thenReturn(lowSkill);
            when(weaker.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            Person stronger = mock(Person.class);
            when(stronger.getSkill("Negotiation")).thenReturn(highSkill);
            when(stronger.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            // Act
            Person result = HumanResources.findBestAtSkill(List.of(weaker, stronger), "Negotiation",
                  campaignOptions, false, today);

            // Assert
            assertEquals(stronger, result);
        }
    }

    /**
     * Tests for
     * {@link HumanResources#findBestInRole(Collection, PersonnelRole, String, String, CampaignOptions, boolean,
     * LocalDate)}
     */
    @Nested
    class FindBestInRole {

        @Test
        void emptyInputReturnsNull() {
            // Arrange
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);
            List<Person> people = List.of();

            // Act
            Person result = HumanResources.findBestInRole(people, PersonnelRole.DOCTOR,
                  "Surgery/Any", null, campaignOptions, false, today);

            // Assert
            assertNull(result);
        }

        @Test
        void personInRoleWithSkillIsReturned() {
            // Arrange
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            Skill skill = mock(Skill.class);
            when(skill.getTotalSkillLevel(any())).thenReturn(5);

            Person doctor = mock(Person.class);
            when(doctor.getPrimaryRole()).thenReturn(PersonnelRole.DOCTOR);
            when(doctor.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
            when(doctor.getSkill("Surgery/Any")).thenReturn(skill);
            when(doctor.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            // Act
            Person result = HumanResources.findBestInRole(List.of(doctor), PersonnelRole.DOCTOR,
                  "Surgery/Any", null, campaignOptions, false, today);

            // Assert
            assertEquals(doctor, result);
        }

        @Test
        void personNotInRoleIsExcluded() {
            // Arrange
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            Skill skill = mock(Skill.class);
            when(skill.getTotalSkillLevel(any())).thenReturn(5);

            Person pilot = mock(Person.class);
            when(pilot.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
            when(pilot.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
            when(pilot.getSkill(anyString())).thenReturn(skill);
            when(pilot.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            // Act
            Person result = HumanResources.findBestInRole(List.of(pilot), PersonnelRole.DOCTOR,
                  "Surgery/Any", null, campaignOptions, false, today);

            // Assert
            assertNull(result);
        }

        @Test
        void higherPrimarySkillWins() {
            // Arrange
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            Skill weakSkill = mock(Skill.class);
            when(weakSkill.getTotalSkillLevel(any())).thenReturn(3);

            Skill strongSkill = mock(Skill.class);
            when(strongSkill.getTotalSkillLevel(any())).thenReturn(8);

            Person weaker = mock(Person.class);
            when(weaker.getPrimaryRole()).thenReturn(PersonnelRole.DOCTOR);
            when(weaker.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
            when(weaker.getSkill("Surgery/Any")).thenReturn(weakSkill);
            when(weaker.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            Person stronger = mock(Person.class);
            when(stronger.getPrimaryRole()).thenReturn(PersonnelRole.DOCTOR);
            when(stronger.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
            when(stronger.getSkill("Surgery/Any")).thenReturn(strongSkill);
            when(stronger.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            // Act
            Person result = HumanResources.findBestInRole(List.of(weaker, stronger), PersonnelRole.DOCTOR,
                  "Surgery/Any", null, campaignOptions, false, today);

            // Assert
            assertEquals(stronger, result);
        }

        @Test
        void secondaryRoleMatchCounts() {
            // Arrange
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            Skill skill = mock(Skill.class);
            when(skill.getTotalSkillLevel(any())).thenReturn(5);

            Person secondaryDoctor = mock(Person.class);
            when(secondaryDoctor.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
            when(secondaryDoctor.getSecondaryRole()).thenReturn(PersonnelRole.DOCTOR);
            when(secondaryDoctor.getSkill("Surgery/Any")).thenReturn(skill);
            when(secondaryDoctor.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            // Act
            Person result = HumanResources.findBestInRole(List.of(secondaryDoctor), PersonnelRole.DOCTOR,
                  "Surgery/Any", null, campaignOptions, false, today);

            // Assert
            assertEquals(secondaryDoctor, result);
        }
    }

    /**
     * Tests for {@link HumanResources#getLogisticsPerson(Collection, CampaignOptions, boolean, LocalDate)}
     */
    @Nested
    class GetLogisticsPerson {

        @Test
        void automaticAcquisitionTypeReturnsNull() {
            // Arrange
            when(campaignOptions.getAcquisitionType()).thenReturn(AcquisitionsType.AUTOMATIC);

            Person admin = mock(Person.class);

            // Act
            Person result = HumanResources.getLogisticsPerson(List.of(admin), campaignOptions, false, today);

            // Assert
            assertNull(result);
        }

        @Test
        void emptyInputReturnsNull() {
            // Arrange
            when(campaignOptions.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);
            when(campaignOptions.getAcquisitionPersonnelCategory()).thenReturn(ProcurementPersonnelPick.ALL);
            when(campaignOptions.getMaxAcquisitions()).thenReturn(0);
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            // Act
            Person result = HumanResources.getLogisticsPerson(List.of(), campaignOptions, false, today);

            // Assert
            assertNull(result);
        }

        @Test
        void bestAdminSkillWinsForAdministrationMode() {
            // Arrange
            when(campaignOptions.getAcquisitionType()).thenReturn(AcquisitionsType.ADMINISTRATION);
            when(campaignOptions.getAcquisitionPersonnelCategory()).thenReturn(ProcurementPersonnelPick.ALL);
            when(campaignOptions.getMaxAcquisitions()).thenReturn(0);
            when(campaignOptions.isUseAgeEffects()).thenReturn(false);

            Skill weakAdmin = mock(Skill.class);
            when(weakAdmin.getTotalSkillLevel(any())).thenReturn(3);

            Skill strongAdmin = mock(Skill.class);
            when(strongAdmin.getTotalSkillLevel(any())).thenReturn(8);

            Person weaker = mock(Person.class);
            when(weaker.getSkill("Administration")).thenReturn(weakAdmin);
            when(weaker.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            Person stronger = mock(Person.class);
            when(stronger.getSkill("Administration")).thenReturn(strongAdmin);
            when(stronger.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(null);

            // Act
            Person result = HumanResources.getLogisticsPerson(List.of(weaker, stronger),
                  campaignOptions, false, today);

            // Assert
            assertEquals(stronger, result);
        }
    }

    /**
     * Tests for {@link HumanResources#writeToXML(PrintWriter, int, Campaign)}
     */
    @Nested
    class WriteToXML {

        @Test
        void outputWrapsContentInHumanResourcesTag() {
            // Arrange
            HumanResources hr = campaign.getHumanResources();
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);

            // Act
            hr.writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            // Assert
            assertTrue(xml.contains("<humanResources>"), "Output must open a <humanResources> tag");
            assertTrue(xml.contains("</humanResources>"), "Output must close a </humanResources> tag");
        }

        @Test
        void poolValuesAreWritten() {
            // Arrange
            HumanResources hr = campaign.getHumanResources();
            hr.setAsTechPool(3);
            hr.setMedicPool(2);

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);

            // Act
            hr.writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            // Assert
            assertTrue(xml.contains("<asTechPool>3</asTechPool>"), "asTechPool value must be written");
            assertTrue(xml.contains("<medicPool>2</medicPool>"), "medicPool value must be written");
        }

        @Test
        void personnelBlockIsNestedInsideHumanResources() {
            // Arrange
            HumanResources hr = campaign.getHumanResources();
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);

            // Act
            hr.writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            // Assert
            int hrOpen = xml.indexOf("<humanResources>");
            int personnelOpen = xml.indexOf("<personnel>");
            int personnelClose = xml.indexOf("</personnel>");
            int hrClose = xml.indexOf("</humanResources>");

            assertTrue(hrOpen < personnelOpen, "<personnel> must appear after <humanResources> opens");
            assertTrue(personnelClose < hrClose, "</personnel> must appear before </humanResources> closes");
        }
    }

    /**
     * Tests for {@link HumanResources#loadFromXML(Node, Campaign, Version)}
     */
    @Nested
    class LoadFromXML {

        @Test
        void roundTripPreservesAsTechPoolValue() throws Exception {
            // Arrange
            HumanResources hr = campaign.getHumanResources();
            hr.setAsTechPool(5);

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            hr.writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            Campaign fresh = MHQTestUtilities.getTestCampaign();
            Document doc = MHQXMLUtility.newSafeDocumentBuilder()
                                 .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Node hrNode = doc.getDocumentElement();

            // Act
            HumanResources loaded = HumanResources.loadFromXML(hrNode, fresh, new Version());

            // Assert
            assertNotNull(loaded);
            assertEquals(5, loaded.getTemporaryAsTechPool(), "asTechPool must round-trip through XML");
        }

        @Test
        void roundTripPreservesMedicPoolValue() throws Exception {
            // Arrange
            HumanResources hr = campaign.getHumanResources();
            hr.setMedicPool(4);

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            hr.writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            Campaign fresh = MHQTestUtilities.getTestCampaign();
            Document doc = MHQXMLUtility.newSafeDocumentBuilder()
                                 .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Node hrNode = doc.getDocumentElement();

            // Act
            HumanResources loaded = HumanResources.loadFromXML(hrNode, fresh, new Version());

            // Assert
            assertNotNull(loaded);
            assertEquals(4, loaded.getTemporaryMedicPool(), "medicPool must round-trip through XML");
        }

        @Test
        void roundTripPreservesPersonnelCount() throws Exception {
            // Arrange
            HumanResources hr = campaign.getHumanResources();
            Person mekwarrior = campaign.newPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE);
            Person doctor = campaign.newPerson(PersonnelRole.DOCTOR, PersonnelRole.NONE);
            hr.recruitPerson(campaign, mekwarrior);
            hr.recruitPerson(campaign, doctor);

            int originalCount = hr.getPersonnel().size();

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            hr.writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            Campaign fresh = MHQTestUtilities.getTestCampaign();
            Document doc = MHQXMLUtility.newSafeDocumentBuilder()
                                 .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Node hrNode = doc.getDocumentElement();

            // Act
            HumanResources.loadFromXML(hrNode, fresh, new Version());

            // Assert
            assertEquals(originalCount, fresh.getHumanResources().getPersonnel().size(),
                  "Personnel count must match after XML round-trip");
        }
    }

    /**
     * Tests for the backward-compatibility path in {@link HumanResources#loadFromXML(Node, Campaign, Version)} that
     * handles the pre-{@code <humanResources>} save format where pool values and personnel appeared at the campaign
     * level.
     */
    @Nested
    class BackwardCompatibility {

        @Test
        void legacyAsTechPoolNodeIsRead() throws Exception {
            // Arrange
            String legacyXml = "<humanResources>"
                                     + "<asTechPool>7</asTechPool>"
                                     + "<asTechPoolMinutes>3360</asTechPoolMinutes>"
                                     + "<asTechPoolOvertime>1680</asTechPoolOvertime>"
                                     + "<medicPool>0</medicPool>"
                                     + "<personnelWhoAdvancedInXP/>"
                                     + "<personnel/>"
                                     + "</humanResources>";

            Campaign fresh = MHQTestUtilities.getTestCampaign();
            Document doc = MHQXMLUtility.newSafeDocumentBuilder()
                                 .parse(new ByteArrayInputStream(legacyXml.getBytes(StandardCharsets.UTF_8)));
            Node hrNode = doc.getDocumentElement();

            // Act
            HumanResources loaded = HumanResources.loadFromXML(hrNode, fresh, new Version());

            // Assert
            assertNotNull(loaded);
            assertEquals(7, loaded.getTemporaryAsTechPool(),
                  "asTechPool from legacy node must be read correctly");
            assertEquals(3360, loaded.getAsTechPoolMinutes(),
                  "asTechPoolMinutes from legacy node must be read correctly");
        }

        @Test
        void unknownChildNodeDoesNotThrow() throws Exception {
            // Arrange
            String xmlWithUnknown = "<humanResources>"
                                          + "<asTechPool>0</asTechPool>"
                                          + "<asTechPoolMinutes>0</asTechPoolMinutes>"
                                          + "<asTechPoolOvertime>0</asTechPoolOvertime>"
                                          + "<medicPool>0</medicPool>"
                                          + "<unknownFutureElement>someValue</unknownFutureElement>"
                                          + "<personnelWhoAdvancedInXP/>"
                                          + "<personnel/>"
                                          + "</humanResources>";

            Campaign fresh = MHQTestUtilities.getTestCampaign();
            Document doc = MHQXMLUtility.newSafeDocumentBuilder()
                                 .parse(new ByteArrayInputStream(xmlWithUnknown.getBytes(StandardCharsets.UTF_8)));
            Node hrNode = doc.getDocumentElement();

            // Act — must not throw
            HumanResources loaded = HumanResources.loadFromXML(hrNode, fresh, new Version());

            // Assert
            assertNotNull(loaded, "Parser must return a valid HumanResources even with unknown elements");
        }

        @Test
        void emptyPersonnelNodeProducesEmptyRoster() throws Exception {
            // Arrange
            String xml = "<humanResources>"
                               + "<asTechPool>0</asTechPool>"
                               + "<asTechPoolMinutes>0</asTechPoolMinutes>"
                               + "<asTechPoolOvertime>0</asTechPoolOvertime>"
                               + "<medicPool>0</medicPool>"
                               + "<personnelWhoAdvancedInXP/>"
                               + "<personnel/>"
                               + "</humanResources>";

            Campaign fresh = MHQTestUtilities.getTestCampaign();
            Document doc = MHQXMLUtility.newSafeDocumentBuilder()
                                 .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Node hrNode = doc.getDocumentElement();

            // Act
            HumanResources.loadFromXML(hrNode, fresh, new Version());

            // Assert
            assertTrue(fresh.getHumanResources().getPersonnel().isEmpty(),
                  "Empty <personnel/> node must produce an empty roster");
        }
    }

    /**
     * Tests for
     * {@link HumanResources#getTechsExpanded(Collection, Collection, CampaignOptions, boolean, LocalDate, boolean,
     * boolean, boolean)}
     */
    @Nested
    class GetTechsExpanded {

        private Person makeTech(SkillLevel skillLevel, int dailyMinutes) {
            Person tech = mock(Person.class);
            PersonnelRole primary = mock(PersonnelRole.class);
            PersonnelRole secondary = mock(PersonnelRole.class);
            when(primary.isTech()).thenReturn(true);
            when(secondary.isTechSecondary()).thenReturn(false);
            when(tech.getPrimaryRole()).thenReturn(primary);
            when(tech.getSecondaryRole()).thenReturn(secondary);
            when(tech.isTech()).thenReturn(true);
            when(tech.isTechExpanded()).thenReturn(true);
            when(tech.getMinutesLeft()).thenReturn(dailyMinutes);
            when(tech.getSkillLevel(any(), anyBoolean(), any(), anyBoolean(), anyBoolean()))
                  .thenReturn(skillLevel);
            when(tech.getDailyAvailableTechTime(anyBoolean())).thenReturn(dailyMinutes);
            when(tech.outRanks(any())).thenReturn(false);
            return tech;
        }

        @Test
        void emptyInputReturnsEmptyList() {
            List<Person> result = HumanResources.getTechsExpanded(
                  List.of(), List.of(), campaignOptions, false, today, false, false, true);

            assertTrue(result.isEmpty());
        }

        @Test
        void nonTechsAreExcluded() {
            Person nonTech = mock(Person.class);
            when(nonTech.isTechExpanded()).thenReturn(false);

            List<Person> result = HumanResources.getTechsExpanded(
                  List.of(nonTech), List.of(), campaignOptions, false, today, false, false, true);

            assertTrue(result.isEmpty());
        }

        @Test
        void eliteFirstPlacesHigherSkillFirst() {
            Person veteran = makeTech(SkillLevel.VETERAN, 480);
            Person regular = makeTech(SkillLevel.REGULAR, 480);

            List<Person> result = HumanResources.getTechsExpanded(
                  List.of(regular, veteran), List.of(), campaignOptions, false, today,
                  false, true, true);

            assertEquals(veteran, result.get(0), "Veteran tech must precede Regular tech when eliteFirst=true");
            assertEquals(regular, result.get(1));
        }

        @Test
        void noZeroMinuteExcludesTechWithNoTime() {
            Person busy = makeTech(SkillLevel.REGULAR, 0);
            Person available = makeTech(SkillLevel.REGULAR, 480);

            List<Person> result = HumanResources.getTechsExpanded(
                  List.of(busy, available), List.of(), campaignOptions, false, today,
                  true, false, true);

            assertFalse(result.contains(busy), "Tech with 0 minutes must be excluded when noZeroMinute=true");
            assertTrue(result.contains(available));
        }

        @Test
        void selfCrewedEngineerIsIncluded() {
            Unit selfCrewedUnit = mock(Unit.class);
            Person engineer = makeTech(SkillLevel.REGULAR, 240);
            Entity entity = mock(Entity.class);

            when(selfCrewedUnit.isSelfCrewed()).thenReturn(true);
            when(selfCrewedUnit.getEntity()).thenReturn(entity);
            when(selfCrewedUnit.getEngineer()).thenReturn(engineer);

            List<Person> result = HumanResources.getTechsExpanded(
                  List.of(), List.of(selfCrewedUnit), campaignOptions, false, today,
                  false, false, true);

            assertTrue(result.contains(engineer), "Engineer from self-crewed unit must be included");
        }
    }
}
