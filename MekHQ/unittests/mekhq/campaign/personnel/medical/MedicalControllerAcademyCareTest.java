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
package mekhq.campaign.personnel.medical;

import static mekhq.campaign.personnel.medical.MedicalController.ACADEMY_MEDICAL_CARE_TARGET_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import megamek.common.TargetRollModifier;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.AcademyFactory;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternateHealing;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.ActionCheckRoll;
import mekhq.campaign.personnel.skills.ActionCheckRoll.RollType;
import mekhq.campaign.personnel.skills.SkillCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Tests that students away at an academy are healed by its medical staff, against a flat target number, while
 * home-schooled students stay under the care of the campaign's infirmary.
 */
class MedicalControllerAcademyCareTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 6, 1);
    private static final String ACADEMY_SET = "TestSet";
    private static final String ACADEMY_NAME = "Test Academy";

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private ForceHumanResources humanResources;

    @BeforeEach
    void beforeEach() {
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.USE_ADVANCED_MEDICAL, false);
        campaignOptions.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, false);

        campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        humanResources = mock(ForceHumanResources.class);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
    }

    @Test
    void academyStudent_successfulCheckHealsAgainstFlatTargetNumber() {
        Person student = injuredStudent(0);

        try (MockedStatic<AcademyFactory> mockFactory = mockAcademyFactory(false);
              MockedConstruction<SkillCheck> skillChecks = mockSkillChecks(1)) {
            new MedicalController(campaign).processMedicalEvents(student, false, false, TODAY);

            assertEquals(1, skillChecks.constructed().size());
            SkillCheck skillCheck = skillChecks.constructed().get(0);
            verify(skillCheck).withRollType(RollType.NORMAL);
        }

        verify(student).heal();
        verify(student).setDaysToWaitForHealing(campaignOptions.get(CampaignOption.HEAL_WAITING_PERIOD));
        verify(humanResources, never()).getPerson(any());
    }

    @Test
    void academyStudent_skillCheckUsesFlatTargetNumber() {
        Person student = injuredStudent(0);
        int[] targetNumber = new int[1];

        try (MockedStatic<AcademyFactory> mockFactory = mockAcademyFactory(false);
              MockedConstruction<SkillCheck> skillChecks = mockConstruction(SkillCheck.class,
                    withSettings().defaultAnswer(RETURNS_SELF),
                    (skillCheck, context) -> {
                        targetNumber[0] = ((TargetRoll) context.arguments().get(2)).getValue();
                        when(skillCheck.resolve(anyBoolean(), any())).thenReturn(checkResult(1));
                    })) {
            new MedicalController(campaign).processMedicalEvents(student, false, false, TODAY);
        }

        assertEquals(ACADEMY_MEDICAL_CARE_TARGET_NUMBER, targetNumber[0]);
    }

    @Test
    void academyStudent_failedCheckDoesNotHealButResetsWaitingPeriod() {
        Person student = injuredStudent(0);

        try (MockedStatic<AcademyFactory> mockFactory = mockAcademyFactory(false);
              MockedConstruction<SkillCheck> skillChecks = mockSkillChecks(-1)) {
            new MedicalController(campaign).processMedicalEvents(student, false, false, TODAY);
        }

        verify(student, never()).heal();
        verify(student).setDaysToWaitForHealing(campaignOptions.get(CampaignOption.HEAL_WAITING_PERIOD));
    }

    @Test
    void academyStudent_noCheckWhileWaitingPeriodRuns() {
        Person student = injuredStudent(2);

        try (MockedStatic<AcademyFactory> mockFactory = mockAcademyFactory(false);
              MockedConstruction<SkillCheck> skillChecks = mockSkillChecks(1)) {
            new MedicalController(campaign).processMedicalEvents(student, false, false, TODAY);

            assertTrue(skillChecks.constructed().isEmpty());
        }

        verify(student).decrementDaysToWaitForHealing();
        verify(student, never()).heal();
    }

    @Test
    void academyStudent_advancedMedicalUsesAcademyCare() {
        campaignOptions.set(CampaignOption.USE_ADVANCED_MEDICAL, true);
        Person student = injuredStudent(0);

        try (MockedStatic<AcademyFactory> mockFactory = mockAcademyFactory(false);
              MockedStatic<InjuryUtil> injuryUtil = mockStatic(InjuryUtil.class)) {
            new MedicalController(campaign).processMedicalEvents(student, false, false, TODAY);

            injuryUtil.verify(() -> InjuryUtil.resolveDailyHealingUnderAcademyCare(campaign, student));
            injuryUtil.verify(() -> InjuryUtil.resolveDailyHealing(any(), any()), never());
        }
    }

    @Test
    void academyStudent_alternateAdvancedMedicalUsesAcademyCare() {
        campaignOptions.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, true);
        Person student = injuredStudent(0);

        try (MockedStatic<AcademyFactory> mockFactory = mockAcademyFactory(false);
              MockedStatic<AdvancedMedicalAlternateHealing> healing =
                    mockStatic(AdvancedMedicalAlternateHealing.class)) {
            new MedicalController(campaign).processMedicalEvents(student, false, false, TODAY);

            healing.verify(() -> AdvancedMedicalAlternateHealing.processNewDayUnderAcademyCare(campaign, student));
            healing.verify(() -> AdvancedMedicalAlternateHealing.processNewDay(any(), any(), any()), never());
        }
    }

    @Test
    void homeSchooledStudent_staysWithInfirmaryDoctor() {
        Person student = injuredStudent(0);
        UUID doctorId = UUID.randomUUID();
        when(student.getDoctorId()).thenReturn(doctorId);

        try (MockedStatic<AcademyFactory> mockFactory = mockAcademyFactory(true);
              MockedConstruction<SkillCheck> skillChecks = mockSkillChecks(1)) {
            new MedicalController(campaign).processMedicalEvents(student, false, false, TODAY);

            assertTrue(skillChecks.constructed().isEmpty());
        }

        verify(humanResources).getPerson(doctorId);
    }

    /**
     * @param daysToWaitForHealing the days left before the student's next healing check
     *
     * @return an injured student enrolled at the test academy
     */
    private Person injuredStudent(int daysToWaitForHealing) {
        Person student = mock(Person.class);
        when(student.getStatus()).thenReturn(PersonnelStatus.STUDENT);
        when(student.needsFixing()).thenReturn(true);
        when(student.getEduAcademySet()).thenReturn(ACADEMY_SET);
        when(student.getEduAcademyNameInSet()).thenReturn(ACADEMY_NAME);
        when(student.getDaysToWaitForHealing()).thenReturn(daysToWaitForHealing);
        when(student.getHealingMods(campaign)).thenReturn(new TargetRollModifier(0, "difficulty"));
        when(student.getHyperlinkedFullTitle()).thenReturn("Student");
        return student;
    }

    private static MockedStatic<AcademyFactory> mockAcademyFactory(boolean isHomeSchool) {
        Academy academy = new Academy(ACADEMY_SET, ACADEMY_NAME, "College",
              false, false, false,
              "Test Academy Description",
              0, false,
              List.of("TestSystem"),
              false, isHomeSchool,
              3025, null, null,
              1000, 365, 12,
              EducationLevel.HIGH_SCHOOL, EducationLevel.COLLEGE,
              18, 35,
              List.of("Test Course"),
              List.of("Test Curriculum"),
              List.of(3025),
              0, 0);

        MockedStatic<AcademyFactory> mockFactory = mockStatic(AcademyFactory.class);
        AcademyFactory factory = mock(AcademyFactory.class);
        mockFactory.when(AcademyFactory::getInstance).thenReturn(factory);
        when(factory.getAllSetNames()).thenReturn(List.of(ACADEMY_SET));
        when(factory.getAllAcademiesForSet(ACADEMY_SET)).thenReturn(List.of(academy));
        return mockFactory;
    }

    private static MockedConstruction<SkillCheck> mockSkillChecks(int marginOfSuccess) {
        return mockConstruction(SkillCheck.class, withSettings().defaultAnswer(RETURNS_SELF),
              (skillCheck, context) -> when(skillCheck.resolve(anyBoolean(), any()))
                                             .thenReturn(checkResult(marginOfSuccess)));
    }

    private static ActionCheckResult checkResult(int marginOfSuccess) {
        return new ActionCheckResult(new ActionCheckRoll(8 + marginOfSuccess, List.of(4, 4 + marginOfSuccess)),
              marginOfSuccess, false, "report");
    }
}
