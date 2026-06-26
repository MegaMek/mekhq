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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static mekhq.campaign.personnel.PersonnelOptions.ATOW_FIT;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.EDGE_MEDICAL;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_HOLISTIC_CARE;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_HYPOCHONDRIAC;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_PATHOLOGIC_INSIGHT;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_PROTHESIS_TECHNICIAN;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_TRAUMA_SURGEON;
import static mekhq.campaign.personnel.medical.BodyLocation.GENERIC;
import static mekhq.campaign.personnel.medical.BodyLocation.RIGHT_FOREARM;
import static mekhq.campaign.personnel.medical.BodyLocation.RIGHT_HAND;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType.DISEASE_GENERIC;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType.NORMAL;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjurySubType.PROSTHETIC_GENERIC;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import megamek.common.TargetRollModifier;
import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.log.PatientLogger;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.SkillCheck;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.utilities.MHQInternationalization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class AdvancedMedicalAlternateHealingTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 22);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 6, 23);

    @Test
    @DisplayName("getSPAModifiers returns one modifier for each enabled healing SPA")
    void getSPAModifiers_returnsEnabledSPAModifiers() {
        Person patient = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);
        when(patient.getOptions()).thenReturn(options);
        when(options.booleanOption(ATOW_FIT)).thenReturn(true);
        when(options.booleanOption(ATOW_TOUGHNESS)).thenReturn(true);

        List<TargetRollModifier> modifiers = AdvancedMedicalAlternateHealing.getSPAModifiers(patient);

        assertEquals(2, modifiers.size());
        assertEquals(-1, modifiers.get(0).value());
        assertEquals("Fit SPA", modifiers.get(0).getDesc());
        assertEquals(-1, modifiers.get(1).value());
        assertEquals("Toughness SPA", modifiers.get(1).getDesc());
    }

    @Test
    @DisplayName("HealingSPAOptions.from reads doctor and patient flags from the correct source")
    void healingSPAOptions_fromReadsDoctorAndPatientOptions() {
        Person doctor = mock(Person.class);
        Person patient = mock(Person.class);
        PersonnelOptions doctorOptions = mock(PersonnelOptions.class);
        PersonnelOptions patientOptions = mock(PersonnelOptions.class);
        when(doctor.getOptions()).thenReturn(doctorOptions);
        when(patient.getOptions()).thenReturn(patientOptions);

        when(doctorOptions.booleanOption(EDGE_MEDICAL)).thenReturn(true);
        when(doctorOptions.booleanOption(UNOFFICIAL_HOLISTIC_CARE)).thenReturn(true);
        when(doctorOptions.booleanOption(UNOFFICIAL_TRAUMA_SURGEON)).thenReturn(false);
        when(doctorOptions.booleanOption(UNOFFICIAL_PROTHESIS_TECHNICIAN)).thenReturn(true);
        when(doctorOptions.booleanOption(UNOFFICIAL_PATHOLOGIC_INSIGHT)).thenReturn(false);
        when(patientOptions.booleanOption(UNOFFICIAL_HYPOCHONDRIAC)).thenReturn(true);

        Object options = invokeHealingSPAOptionsFrom(doctor, patient);

        assertTrue(invokeBooleanRecordAccessor(options, "hasMedicalEdge"));
        assertTrue(invokeBooleanRecordAccessor(options, "hasHolisticCareSPA"));
        assertFalse(invokeBooleanRecordAccessor(options, "hasTraumaSurgeon"));
        assertTrue(invokeBooleanRecordAccessor(options, "hasProthesisTechnician"));
        assertFalse(invokeBooleanRecordAccessor(options, "hasPathologicInsight"));
        assertTrue(invokeBooleanRecordAccessor(options, "hasHypochondriac"));
    }

    @Test
    @DisplayName("getProstheticPenalties records permanent modifications on their primary body location")
    void getProstheticPenalties_collectsPrimaryLocationsOnly() throws Exception {
        Injury prosthetic = mock(Injury.class);
        when(prosthetic.getSubType()).thenReturn(PROSTHETIC_GENERIC);
        when(prosthetic.getLocation()).thenReturn(RIGHT_FOREARM);

        Injury ignored = mock(Injury.class);
        when(ignored.getSubType()).thenReturn(NORMAL);
        when(ignored.getLocation()).thenReturn(GENERIC);

        Injury skipped = mock(Injury.class);
        when(skipped.getSubType()).thenReturn(PROSTHETIC_GENERIC);
        when(skipped.getLocation()).thenReturn(GENERIC);

        Person patient = mock(Person.class);
        when(patient.getPermanentInjuries()).thenReturn(List.of(prosthetic, ignored, skipped));

        Set<BodyLocation> penalties = invokePrivateStatic("getProstheticPenalties",
              new Class<?>[] { Person.class }, patient);

        assertEquals(Set.of(RIGHT_HAND), penalties);
    }

    @Test
    @DisplayName("getMiscPenalty applies trauma surgeon, prosthetic, and prosthetic technician modifiers")
    void getMiscPenalty_appliesAllRelevantModifiers() throws Exception {
        Set<BodyLocation> prostheticPenalties = Set.of(RIGHT_HAND);

        int plainPenalty = invokePrivateStatic("getMiscPenalty",
              new Class<?>[] { int.class, Set.class, BodyLocation.class, boolean.class, boolean.class },
              2, prostheticPenalties, RIGHT_FOREARM, false, false);

        int traumaPenalty = invokePrivateStatic("getMiscPenalty",
              new Class<?>[] { int.class, Set.class, BodyLocation.class, boolean.class, boolean.class },
              2, prostheticPenalties, RIGHT_FOREARM, true, false);

        int technicanPenalty = invokePrivateStatic("getMiscPenalty",
              new Class<?>[] { int.class, Set.class, BodyLocation.class, boolean.class, boolean.class },
              0, prostheticPenalties, RIGHT_FOREARM, false, true);

        assertEquals(6, plainPenalty);
        assertEquals(5, traumaPenalty);
        assertEquals(3, technicanPenalty);
    }

    @Test
    @DisplayName("processNewDay uses the unassisted healing modifier and processes an injury on Monday")
    void processNewDay_unassistedHealingAddsModifierAndHeals() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        Person patient = mock(Person.class);
        Injury injury = mock(Injury.class);
        SkillCheck skillCheck = mock(SkillCheck.class);
        ActionCheckResult result = new ActionCheckResult(8, 3, false, "success");
        PersonnelOptions options = mock(PersonnelOptions.class);

        when(campaign.getLocalDate()).thenReturn(MONDAY);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseEdge()).thenReturn(false);
        when(campaignOptions.isUseFatigue()).thenReturn(false);
        when(patient.getOptions()).thenReturn(options);
        when(options.booleanOption(ATOW_FIT)).thenReturn(false);
        when(options.booleanOption(ATOW_TOUGHNESS)).thenReturn(false);
        when(patient.getPermanentInjuries()).thenReturn(List.of());
        when(patient.getTotalInjurySeverity()).thenReturn(0);
        when(patient.getAdjustedToughness()).thenReturn(0);
        when(injury.isPermanent()).thenReturn(false);
        when(injury.isDisease()).thenReturn(false);
        when(injury.getSubType()).thenReturn(NORMAL);
        when(injury.getLocation()).thenReturn(GENERIC);
        when(injury.getTime()).thenReturn(0); // Timer already elapsed; ready to resolve
        when(injury.getOriginalTime()).thenReturn(1);
        when(injury.getName()).thenReturn("Broken arm");
        when(patient.checkSkill(S_SURGERY, campaign)).thenReturn(skillCheck);
        when(skillCheck.withMiscModifier(anyInt())).thenReturn(skillCheck);
        when(skillCheck.withExternalModifiers(any())).thenReturn(skillCheck);
        when(skillCheck.resolve(anyBoolean(), any(), anyBoolean())).thenReturn(result);

        AtomicBoolean removed = new AtomicBoolean(false);
        doAnswer(invocation -> {
            removed.set(true);
            return null;
        }).when(patient).removeInjury(eq(injury), eq(MONDAY));
        when(patient.getInjuries()).thenAnswer(invocation -> removed.get() ? List.of() : List.of(injury));

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedStatic<MHQInternationalization> i18n = mockStatic(MHQInternationalization.class);
              MockedStatic<PatientLogger> patientLogger = mockStatic(PatientLogger.class);
              MockedStatic<MedicalLogger> medicalLogger = mockStatic(MedicalLogger.class)) {
            compute.when(() -> Compute.randomInt(20)).thenReturn(1);
            i18n.when(() -> MHQInternationalization.getTextAt(anyString(), anyString())).thenAnswer(invocation ->
                                                                                                          invocation.getArgument(
                                                                                                                1));

            AdvancedMedicalAlternateHealing.processNewDay(campaign, patient, null);

            ArgumentCaptor<List<TargetRollModifier>> modifiersCaptor = ArgumentCaptor.forClass(List.class);
            verify(skillCheck).withMiscModifier(0);
            verify(skillCheck).withExternalModifiers(modifiersCaptor.capture());
            assertEquals(List.of(new TargetRollModifier(-3, "Unassisted Healing")), modifiersCaptor.getValue());
            verify(patient).removeInjury(eq(injury), eq(MONDAY));
            verify(patient).setDoctorId(null, 1);
            verify(patient).changeNTasks(1);
            patientLogger.verify(() ->
                                       PatientLogger.successfullyTreatedOwnInjuryAltAdvancedMedical(patient,
                                             MONDAY,
                                             "Broken arm"));
        }
    }

    @Test
    @DisplayName("processNewDay skips unassisted healing on non-Mondays")
    void processNewDay_unassistedHealingSkipsNonMonday() {
        Campaign campaign = mock(Campaign.class);
        Person patient = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);

        when(campaign.getLocalDate()).thenReturn(TUESDAY);
        when(patient.getOptions()).thenReturn(options);
        when(options.booleanOption(ATOW_FIT)).thenReturn(false);
        when(options.booleanOption(ATOW_TOUGHNESS)).thenReturn(false);
        when(patient.getPermanentInjuries()).thenReturn(List.of());
        when(patient.getInjuries()).thenReturn(List.of(mock(Injury.class)));

        AdvancedMedicalAlternateHealing.processNewDay(campaign, patient, null);

        verify(patient, never()).checkSkill(anyString(), any(Campaign.class));
    }

    @Test
    @DisplayName("processNewDay with a doctor applies pathologic insight and assisted healing logs")
    void processNewDay_assistedHealingAppliesPathologicInsight() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        Person doctor = mock(Person.class);
        Person patient = mock(Person.class);
        Injury injury = mock(Injury.class);
        Injury permanentMod = mock(Injury.class);
        SkillCheck skillCheck = mock(SkillCheck.class);
        ActionCheckResult result = new ActionCheckResult(10, 2, false, "success");
        PersonnelOptions doctorOptions = mock(PersonnelOptions.class);
        PersonnelOptions patientOptions = mock(PersonnelOptions.class);

        when(campaign.getLocalDate()).thenReturn(MONDAY);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseEdge()).thenReturn(false);
        when(campaignOptions.isUseFatigue()).thenReturn(false);
        when(doctor.getOptions()).thenReturn(doctorOptions);
        when(patient.getOptions()).thenReturn(patientOptions);
        when(doctorOptions.booleanOption(EDGE_MEDICAL)).thenReturn(false);
        when(doctorOptions.booleanOption(UNOFFICIAL_HOLISTIC_CARE)).thenReturn(false);
        when(doctorOptions.booleanOption(UNOFFICIAL_TRAUMA_SURGEON)).thenReturn(false);
        when(doctorOptions.booleanOption(UNOFFICIAL_PROTHESIS_TECHNICIAN)).thenReturn(false);
        when(doctorOptions.booleanOption(UNOFFICIAL_PATHOLOGIC_INSIGHT)).thenReturn(true);
        when(patientOptions.booleanOption(UNOFFICIAL_HYPOCHONDRIAC)).thenReturn(false);
        when(patient.getPermanentInjuries()).thenReturn(List.of(permanentMod));
        when(permanentMod.getSubType()).thenReturn(PROSTHETIC_GENERIC);
        when(permanentMod.getLocation()).thenReturn(RIGHT_FOREARM);
        when(patient.getTotalInjurySeverity()).thenReturn(0);
        when(patient.getAdjustedToughness()).thenReturn(0);
        when(injury.isPermanent()).thenReturn(false);
        when(injury.isDisease()).thenReturn(true);
        when(injury.getSubType()).thenReturn(DISEASE_GENERIC);
        when(injury.getLocation()).thenReturn(RIGHT_FOREARM);
        when(injury.getOriginalTime()).thenReturn(1);
        when(injury.getOriginalTime()).thenReturn(1);
        when(injury.getName()).thenReturn("Illness");
        when(doctor.checkSkill(S_SURGERY, campaign)).thenReturn(skillCheck);
        when(skillCheck.withMiscModifier(anyInt())).thenReturn(skillCheck);
        when(skillCheck.withExternalModifiers(any())).thenReturn(skillCheck);
        when(skillCheck.resolve(anyBoolean(), any(), anyBoolean())).thenReturn(result);
        when(patient.getDoctorId()).thenReturn(null);

        AtomicBoolean removed = new AtomicBoolean(false);
        doAnswer(invocation -> {
            removed.set(true);
            return null;
        }).when(patient).removeInjury(eq(injury), eq(MONDAY));
        when(patient.getInjuries()).thenAnswer(invocation -> removed.get() ? List.of() : List.of(injury));

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedStatic<MHQInternationalization> i18n = mockStatic(MHQInternationalization.class);
              MockedStatic<PatientLogger> patientLogger = mockStatic(PatientLogger.class);
              MockedStatic<MedicalLogger> medicalLogger = mockStatic(MedicalLogger.class)) {
            compute.when(() -> Compute.randomInt(20)).thenReturn(1);
            i18n.when(() -> MHQInternationalization.getTextAt(anyString(), anyString())).thenAnswer(invocation ->
                                                                                                          invocation.getArgument(
                                                                                                                1));

            AdvancedMedicalAlternateHealing.processNewDay(campaign, patient, doctor);

            ArgumentCaptor<List<TargetRollModifier>> modifiersCaptor = ArgumentCaptor.forClass(List.class);
            verify(skillCheck).withMiscModifier(4);
            verify(skillCheck).withExternalModifiers(modifiersCaptor.capture());
            assertTrue(modifiersCaptor.getValue().stream()
                             .anyMatch(modifier -> modifier.equals(new TargetRollModifier(-2, "Pathologic Insight"))));
            verify(doctor).changeNTasks(1);
            verify(patient).setDoctorId(null, 1);
            patientLogger.verify(() ->
                                       PatientLogger.successfullyTreatedAltAdvancedMedical(doctor,
                                             patient,
                                             MONDAY,
                                             "Illness"));
        }
    }

    @Test
    @DisplayName("getMarginOfSuccessForHealing rerolls with edge when the first result would cause a permanent injury")
    void getMarginOfSuccessForHealing_usesEdgeReroll() throws Exception {
        Campaign campaign = mock(Campaign.class);
        Person doctor = mock(Person.class);
        SkillCheck skillCheck = mock(SkillCheck.class);
        ActionCheckResult first = new ActionCheckResult(2, -4, false, "first");
        ActionCheckResult second = new ActionCheckResult(11, 1, true, "second");

        when(doctor.checkSkill(S_SURGERY, campaign)).thenReturn(skillCheck);
        when(skillCheck.withMiscModifier(6)).thenReturn(skillCheck);
        when(skillCheck.withExternalModifiers(any())).thenReturn(skillCheck);
        when(skillCheck.resolve(anyBoolean(), anyString(), anyBoolean())).thenReturn(first, second);
        when(doctor.getCurrentEdge()).thenReturn(1);

        try (MockedStatic<MHQInternationalization> i18n = mockStatic(MHQInternationalization.class)) {
            i18n.when(() -> MHQInternationalization.getTextAt(anyString(), anyString())).thenAnswer(invocation ->
                                                                                                          invocation.getArgument(
                                                                                                                1));

            int margin = invokePrivateStatic("getMarginOfSuccessForHealing",
                  new Class<?>[] { Person.class, Campaign.class, List.class, int.class, boolean.class },
                  doctor, campaign, List.of(), 6, true);

            assertEquals(1, margin);
            verify(doctor).spendEdge();
            verify(skillCheck, times(2)).resolve(anyBoolean(), anyString(), anyBoolean());
        }
    }

    @Test
    @DisplayName("processHealingEffects heals, clears the infirmary assignment, and applies fatigue")
    void processHealingEffects_healsAndDismissesWhenLastInjury() throws Exception {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        Person patient = mock(Person.class);
        Injury injury = mock(Injury.class);

        when(campaign.getLocalDate()).thenReturn(MONDAY);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseFatigue()).thenReturn(true);
        when(campaignOptions.getFatigueRate()).thenReturn(2);
        when(patient.getInjuries()).thenAnswer(invocation -> List.of());
        when(patient.getDoctorId()).thenReturn(UUID.randomUUID());
        when(patient.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(injury.getName()).thenReturn("Broken arm");

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedStatic<MedicalLogger> medicalLogger = mockStatic(MedicalLogger.class)) {
            compute.when(() -> Compute.d6(1)).thenReturn(4);
            HealingMarginOfSuccessEffects effect = invokePrivateStatic("processHealingEffects",
                  new Class<?>[] { Campaign.class, Person.class, Injury.class, int.class },
                  campaign, patient, injury, 3);

            assertSame(HealingMarginOfSuccessEffects.RECOVERY, effect);
            verify(patient).removeInjury(injury, MONDAY);
            verify(patient).setDoctorId(null, 1);
            verify(patient).changeFatigue(0);
            medicalLogger.verify(() -> MedicalLogger.dismissedFromInfirmary(patient, campaign));
        }
    }

    @Test
    @DisplayName("processHealingEffects delays non-healed injuries and applies fatigue")
    void processHealingEffects_delaysAndAppliesFatigue() throws Exception {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        Person patient = mock(Person.class);
        Injury injury = mock(Injury.class);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseFatigue()).thenReturn(true);
        when(campaignOptions.getFatigueRate()).thenReturn(3);
        when(patient.getInjuries()).thenReturn(List.of(injury));
        when(injury.getOriginalTime()).thenReturn(7);
        when(injury.getName()).thenReturn("Burn");

        try (MockedStatic<Compute> compute = mockStatic(Compute.class);
              MockedStatic<MedicalLogger> medicalLogger = mockStatic(MedicalLogger.class)) {
            compute.when(() -> Compute.d6(1)).thenReturn(5);
            HealingMarginOfSuccessEffects effect = invokePrivateStatic("processHealingEffects",
                  new Class<?>[] { Campaign.class, Person.class, Injury.class, int.class },
                  campaign, patient, injury, -1);

            assertSame(HealingMarginOfSuccessEffects.RECOVERY_DELAYED, effect);
            verify(injury).changeTime(5);
            verify(patient).changeFatigue(15);
            medicalLogger.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("processHealingEffects marks an injury permanent and adds a complication injury")
    void processHealingEffects_marksPermanentAndAddsComplication() throws Exception {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        Person patient = mock(Person.class);
        Injury injury = mock(Injury.class);
        PersonnelOptions patientOptions = mock(PersonnelOptions.class);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseFatigue()).thenReturn(false);
        when(campaign.getLocalDate()).thenReturn(MONDAY);
        when(patient.getOptions()).thenReturn(patientOptions);
        when(patientOptions.booleanOption(anyString())).thenReturn(false);
        when(patient.getGender()).thenReturn(Gender.MALE);
        when(patient.getInjuries()).thenReturn(List.of(injury));
        when(injury.getName()).thenReturn("Crush injury");

        try (MockedStatic<InjuryUtil> injuryUtil = mockStatic(InjuryUtil.class);
              MockedStatic<MedicalLogger> medicalLogger = mockStatic(MedicalLogger.class)) {
            Object medicalComplication = Class.forName(
                        "mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries")
                                               .getField("MEDICAL_COMPLICATION")
                                               .get(null);
            injuryUtil.when(() -> InjuryUtil.genHealingTime(campaign, patient, (InjuryType) medicalComplication, 1))
                  .thenReturn(1);
            HealingMarginOfSuccessEffects effect = invokePrivateStatic("processHealingEffects",
                  new Class<?>[] { Campaign.class, Person.class, Injury.class, int.class },
                  campaign, patient, injury, -6);

            assertTrue(effect.isPermanent());
            verify(injury).setPermanent(true);
            verify(patient).addInjury(any(Injury.class));
            medicalLogger.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("processTaskAwardsAndPersonnelLogUpdates increments the doctor task count and logs success")
    void processTaskAwardsAndPersonnelLogUpdates_awardsDoctorTask() throws Exception {
        LocalDate today = MONDAY;
        Person patient = mock(Person.class);
        Person doctor = mock(Person.class);
        Injury injury = mock(Injury.class);
        when(injury.getName()).thenReturn("Broken arm");

        try (MockedStatic<PatientLogger> patientLogger = mockStatic(PatientLogger.class);
              MockedStatic<MedicalLogger> medicalLogger = mockStatic(MedicalLogger.class)) {
            invokePrivateStatic("processTaskAwardsAndPersonnelLogUpdates",
                  new Class<?>[] { LocalDate.class, Person.class, Person.class, Injury.class,
                                   HealingMarginOfSuccessEffects.class },
                  today, patient, doctor, injury, HealingMarginOfSuccessEffects.RECOVERY);

            verify(doctor).changeNTasks(1);
            patientLogger.verify(() ->
                                       PatientLogger.successfullyTreatedAltAdvancedMedical(doctor,
                                             patient,
                                             today,
                                             "Broken arm"));
            medicalLogger.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("processTaskAwardsAndPersonnelLogUpdates logs permanent injuries and complications")
    void processTaskAwardsAndPersonnelLogUpdates_logsPermanentAndComplication() throws Exception {
        LocalDate today = MONDAY;
        Person patient = mock(Person.class);
        Injury injury = mock(Injury.class);
        when(injury.getName()).thenReturn("Broken arm");

        try (MockedStatic<PatientLogger> patientLogger = mockStatic(PatientLogger.class);
              MockedStatic<MedicalLogger> medicalLogger = mockStatic(MedicalLogger.class)) {
            invokePrivateStatic("processTaskAwardsAndPersonnelLogUpdates",
                  new Class<?>[] { LocalDate.class, Person.class, Person.class, Injury.class,
                                   HealingMarginOfSuccessEffects.class },
                  today, patient, null, injury, HealingMarginOfSuccessEffects.getEffectFromHealingAttempt(-6, false));

            verify(patient, never()).changeNTasks(anyInt());
            patientLogger.verifyNoInteractions();
            medicalLogger.verify(() -> MedicalLogger.permanentInjuryAltAdvancedMedical(patient, today, "Broken arm"));
            medicalLogger.verify(() -> {
                try {
                    Method method = MedicalLogger.class.getMethod("medicalComplicationAltAdvancedMedical",
                          Person.class, LocalDate.class, String.class);
                    method.invoke(null, patient, today, "Broken arm");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static <T> T invokePrivateStatic(String methodName, Class<?>[] parameterTypes, Object... args)
          throws Exception {
        Method method = AdvancedMedicalAlternateHealing.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        T result = (T) method.invoke(null, args);
        return result;
    }

    private static Object invokeHealingSPAOptionsFrom(Person doctor, Person patient) {
        try {
            Class<?> clazz = Class.forName(
                  "mekhq.campaign.personnel.medical.advancedMedicalAlternate.HealingSPAOptions");
            Method from = clazz.getDeclaredMethod("from", Person.class, Person.class);
            from.setAccessible(true);
            return from.invoke(null, doctor, patient);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static boolean invokeBooleanRecordAccessor(Object record, String accessor) {
        try {
            return (boolean) record.getClass().getMethod(accessor).invoke(record);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
