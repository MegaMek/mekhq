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

import static megamek.common.options.OptionsConstants.MD_DERMAL_ARMOR;
import static megamek.common.options.OptionsConstants.MD_DERMAL_CAMO_ARMOR;
import static megamek.common.options.OptionsConstants.MD_EI_IMPLANT;
import static megamek.common.options.OptionsConstants.UNOFFICIAL_EI_IMPLANT;
import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static mekhq.campaign.enums.DailyReportType.MEDICAL;
import static mekhq.campaign.enums.DailyReportType.SKILL_CHECKS;
import static mekhq.campaign.personnel.PersonnelOptions.COMPULSION_PAINKILLER_ADDICTION;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_UNFIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.enums.Gender;
import megamek.common.options.IOption;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.personnel.skills.ActionCheckResult;
import mekhq.campaign.personnel.skills.AttributeCheck;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ReportingUtilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AdvancedMedicalAlternateImplantsTest {
    // region getFrequency
    @Test
    @DisplayName("getFrequency returns 3 for buffered VDNI, which takes priority over everything else")
    void getFrequency_bufferedVDNITakesPriority() throws Exception {
        int frequency = invokePrivateStatic("getFrequency",
              new Class<?>[] { boolean.class, boolean.class, boolean.class, boolean.class },
              true, true, true, true);

        assertEquals(3, frequency);
    }

    @Test
    @DisplayName("getFrequency returns 2 for standard VDNI when buffered VDNI is absent")
    void getFrequency_standardVDNI() throws Exception {
        int frequency = invokePrivateStatic("getFrequency",
              new Class<?>[] { boolean.class, boolean.class, boolean.class, boolean.class },
              false, false, true, true);

        assertEquals(2, frequency);
    }

    @Test
    @DisplayName("getFrequency returns 1 for Enhanced Imaging on a non-Aerospace phenotype")
    void getFrequency_enhancedImagingNonAerospace() throws Exception {
        int frequency = invokePrivateStatic("getFrequency",
              new Class<?>[] { boolean.class, boolean.class, boolean.class, boolean.class },
              false, false, false, true);

        assertEquals(1, frequency);
    }

    @Test
    @DisplayName("getFrequency returns 3 for Enhanced Imaging on an Aerospace phenotype")
    void getFrequency_enhancedImagingAerospace() throws Exception {
        int frequency = invokePrivateStatic("getFrequency",
              new Class<?>[] { boolean.class, boolean.class, boolean.class, boolean.class },
              true, false, false, true);

        assertEquals(3, frequency);
    }

    @Test
    @DisplayName("getFrequency returns the default of 1 when no relevant implants are present")
    void getFrequency_defaultsToOne() throws Exception {
        int frequency = invokePrivateStatic("getFrequency",
              new Class<?>[] { boolean.class, boolean.class, boolean.class, boolean.class },
              false, false, false, false);

        assertEquals(1, frequency);
    }
    // endregion getFrequency

    // region isHasTooManyProsthetics
    @Test
    @DisplayName("isHasTooManyProsthetics returns true once three high-impact prosthetics are present")
    void isHasTooManyProsthetics_threeQualifyingReturnsTrue() throws Exception {
        Person person = mock(Person.class);
        List<Injury> prostheticInjuries = List.of(
              prostheticInjury(AlternateInjuries.ADVANCED_PROSTHETIC_ARM),
              prostheticInjury(AlternateInjuries.ADVANCED_PROSTHETIC_HAND),
              prostheticInjury(AlternateInjuries.ADVANCED_PROSTHETIC_LEG));
        when(person.getProstheticInjuries()).thenReturn(prostheticInjuries);

        boolean result = invokePrivateStatic("isHasTooManyProsthetics",
              new Class<?>[] { Person.class }, person);

        assertTrue(result);
    }

    @Test
    @DisplayName("isHasTooManyProsthetics returns false when only two high-impact prosthetics are present")
    void isHasTooManyProsthetics_twoQualifyingReturnsFalse() throws Exception {
        Person person = mock(Person.class);
        List<Injury> prostheticInjuries = List.of(
              prostheticInjury(AlternateInjuries.ADVANCED_PROSTHETIC_ARM),
              prostheticInjury(AlternateInjuries.ADVANCED_PROSTHETIC_HAND));
        when(person.getProstheticInjuries()).thenReturn(prostheticInjuries);

        boolean result = invokePrivateStatic("isHasTooManyProsthetics",
              new Class<?>[] { Person.class }, person);

        assertFalse(result);
    }

    @Test
    @DisplayName("isHasTooManyProsthetics ignores low-impact prosthetics below the complexity threshold")
    void isHasTooManyProsthetics_lowImpactProstheticsIgnored() throws Exception {
        Person person = mock(Person.class);
        // BIONIC_EAR is STANDARD (tier 3) and therefore must not count towards the threshold.
        List<Injury> prostheticInjuries = List.of(
              prostheticInjury(AlternateInjuries.BIONIC_EAR),
              prostheticInjury(AlternateInjuries.BIONIC_EAR),
              prostheticInjury(AlternateInjuries.BIONIC_EAR));
        when(person.getProstheticInjuries()).thenReturn(prostheticInjuries);

        boolean result = invokePrivateStatic("isHasTooManyProsthetics",
              new Class<?>[] { Person.class }, person);

        assertFalse(result);
    }

    @Test
    @DisplayName("isHasTooManyProsthetics ignores injuries that do not map to a ProstheticType")
    void isHasTooManyProsthetics_nonProstheticInjuriesIgnored() throws Exception {
        Person person = mock(Person.class);
        List<Injury> prostheticInjuries = List.of(
              prostheticInjury(AlternateInjuries.FRACTURED_RIB),
              prostheticInjury(AlternateInjuries.ADVANCED_PROSTHETIC_ARM));
        when(person.getProstheticInjuries()).thenReturn(prostheticInjuries);

        boolean result = invokePrivateStatic("isHasTooManyProsthetics",
              new Class<?>[] { Person.class }, person);

        assertFalse(result);
    }
    // endregion isHasTooManyProsthetics

    // region getAndApplyEIDegradationFlaw
    @Test
    @DisplayName("getAndApplyEIDegradationFlaw applies a newly rolled Flaw and returns its display name")
    void getAndApplyEIDegradationFlaw_appliesNewFlaw() {
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);
        SpecialAbility ability = mock(SpecialAbility.class);

        when(person.getOptions()).thenReturn(options);
        when(options.booleanOption(FLAW_UNFIT)).thenReturn(false);
        when(ability.getDisplayName()).thenReturn("Unfit");

        try (MockedStatic<ObjectUtility> objectUtility = mockStatic(ObjectUtility.class);
              MockedStatic<SpecialAbility> specialAbility = mockStatic(SpecialAbility.class)) {
            objectUtility.when(() -> ObjectUtility.getRandomItem(anyList())).thenReturn(FLAW_UNFIT);
            specialAbility.when(() -> SpecialAbility.getAbility(FLAW_UNFIT)).thenReturn(ability);

            String applied = AdvancedMedicalAlternateImplants.getAndApplyEIDegradationFlaw(person);

            assertEquals("Unfit", applied);
            verify(options).acquireAbility(LVL3_ADVANTAGES, FLAW_UNFIT, true);
        }
    }

    @Test
    @DisplayName("getAndApplyEIDegradationFlaw returns an empty string and applies nothing when the Flaw is already owned")
    void getAndApplyEIDegradationFlaw_alreadyOwnedGetsFreePass() {
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);

        when(person.getOptions()).thenReturn(options);
        when(options.booleanOption(FLAW_UNFIT)).thenReturn(true);

        try (MockedStatic<ObjectUtility> objectUtility = mockStatic(ObjectUtility.class);
              MockedStatic<SpecialAbility> specialAbility = mockStatic(SpecialAbility.class)) {
            objectUtility.when(() -> ObjectUtility.getRandomItem(anyList())).thenReturn(FLAW_UNFIT);

            String applied = AdvancedMedicalAlternateImplants.getAndApplyEIDegradationFlaw(person);

            assertEquals("", applied);
            verify(options, never()).acquireAbility(anyString(), anyString(), any());
            specialAbility.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("getAndApplyEIDegradationFlaw resolves the compulsion placeholder to a concrete compulsion Flaw")
    void getAndApplyEIDegradationFlaw_resolvesCompulsionPlaceholder() {
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);
        SpecialAbility ability = mock(SpecialAbility.class);

        when(person.getOptions()).thenReturn(options);
        when(options.booleanOption(COMPULSION_PAINKILLER_ADDICTION)).thenReturn(false);
        when(ability.getDisplayName()).thenReturn("Painkiller Addiction");

        try (MockedStatic<ObjectUtility> objectUtility = mockStatic(ObjectUtility.class);
              MockedStatic<SpecialAbility> specialAbility = mockStatic(SpecialAbility.class)) {
            // First draw hits the placeholder, second draw selects a concrete compulsion.
            objectUtility.when(() -> ObjectUtility.getRandomItem(anyList()))
                  .thenReturn(AdvancedMedicalAlternateImplants.COMPULSION_PLACEHOLDER,
                        COMPULSION_PAINKILLER_ADDICTION);
            specialAbility.when(() -> SpecialAbility.getAbility(COMPULSION_PAINKILLER_ADDICTION)).thenReturn(ability);

            String applied = AdvancedMedicalAlternateImplants.getAndApplyEIDegradationFlaw(person);

            assertEquals("Painkiller Addiction", applied);
            verify(options).acquireAbility(LVL3_ADVANTAGES, COMPULSION_PAINKILLER_ADDICTION, true);
        }
    }

    @Test
    @DisplayName("getAndApplyEIDegradationFlaw returns an empty string when the Flaw is disabled in the campaign")
    void getAndApplyEIDegradationFlaw_disabledAbilityReturnsEmpty() {
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);

        when(person.getOptions()).thenReturn(options);
        when(options.booleanOption(FLAW_UNFIT)).thenReturn(false);

        try (MockedStatic<ObjectUtility> objectUtility = mockStatic(ObjectUtility.class);
              MockedStatic<SpecialAbility> specialAbility = mockStatic(SpecialAbility.class)) {
            objectUtility.when(() -> ObjectUtility.getRandomItem(anyList())).thenReturn(FLAW_UNFIT);
            specialAbility.when(() -> SpecialAbility.getAbility(FLAW_UNFIT)).thenReturn(null);

            String applied = AdvancedMedicalAlternateImplants.getAndApplyEIDegradationFlaw(person);

            assertEquals("", applied);
            verify(options, never()).acquireAbility(anyString(), anyString(), any());
        }
    }
    // endregion getAndApplyEIDegradationFlaw

    // region checkForDermalEligibility
    @Test
    @DisplayName("checkForDermalEligibility enables the dermal options when all four limbs are covered")
    void checkForDermalEligibility_enablesWhenAllLimbsCovered() {
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);
        IOption armorOption = mock(IOption.class);
        IOption camoOption = mock(IOption.class);

        List<Injury> prostheticInjuries = List.of(
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_ARM_ARMOR),
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_ARM_ARMOR),
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_LEG_ARMOR),
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_LEG_ARMOR),
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_ARM_CAMO),
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_ARM_CAMO),
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_LEG_CAMO),
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_LEG_CAMO));
        when(person.getProstheticInjuries()).thenReturn(prostheticInjuries);
        when(person.getOptions()).thenReturn(options);
        when(options.getOption(MD_DERMAL_ARMOR)).thenReturn(armorOption);
        when(options.getOption(MD_DERMAL_CAMO_ARMOR)).thenReturn(camoOption);

        AdvancedMedicalAlternateImplants.checkForDermalEligibility(person);

        verify(armorOption).setValue(true);
        verify(camoOption).setValue(true);
    }

    @Test
    @DisplayName("checkForDermalEligibility disables the dermal options when fewer than four limbs are covered")
    void checkForDermalEligibility_disablesWhenNotEnoughLimbs() {
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);
        IOption armorOption = mock(IOption.class);
        IOption camoOption = mock(IOption.class);

        List<Injury> prostheticInjuries = List.of(
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_ARM_ARMOR),
              prostheticInjury(AlternateInjuries.DERMAL_MYOMER_ARM_CAMO));
        when(person.getProstheticInjuries()).thenReturn(prostheticInjuries);
        when(person.getOptions()).thenReturn(options);
        when(options.getOption(MD_DERMAL_ARMOR)).thenReturn(armorOption);
        when(options.getOption(MD_DERMAL_CAMO_ARMOR)).thenReturn(camoOption);

        AdvancedMedicalAlternateImplants.checkForDermalEligibility(person);

        verify(armorOption).setValue(false);
        verify(camoOption).setValue(false);
    }
    // endregion checkForDermalEligibility

    // region giveEIImplant
    @Test
    @DisplayName("giveEIImplant acquires the associated pilot and personnel options when both are enabled")
    void giveEIImplant_acquiresOptionsWhenEnabled() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(person.getOptions()).thenReturn(options);
        when(person.getGender()).thenReturn(Gender.MALE);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
        lenient().when(campaignOptions.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL)).thenReturn(false);
        when(campaignOptions.get(CampaignOption.USE_IMPLANTS)).thenReturn(true);
        when(campaignOptions.get(CampaignOption.USE_ABILITIES)).thenReturn(true);

        try (MockedStatic<InjuryUtil> injuryUtil = mockStatic(InjuryUtil.class)) {
            injuryUtil.when(() -> InjuryUtil.genHealingTime(any(), any(), any(), anyInt())).thenReturn(0);

            AdvancedMedicalAlternateImplants.giveEIImplant(campaign, person);

            verify(person).addInjury(any());
            verify(options).acquireAbility(LVL3_ADVANTAGES, UNOFFICIAL_EI_IMPLANT, true);
            verify(options).acquireAbility(LVL3_ADVANTAGES, COMPULSION_PAINKILLER_ADDICTION, true);
        }
    }

    @Test
    @DisplayName("giveEIImplant only adds the injury and acquires no options when both toggles are disabled")
    void giveEIImplant_skipsOptionsWhenDisabled() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(person.getOptions()).thenReturn(options);
        when(person.getGender()).thenReturn(Gender.MALE);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
        lenient().when(campaignOptions.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL)).thenReturn(false);
        when(campaignOptions.get(CampaignOption.USE_IMPLANTS)).thenReturn(false);
        when(campaignOptions.get(CampaignOption.USE_ABILITIES)).thenReturn(false);

        try (MockedStatic<InjuryUtil> injuryUtil = mockStatic(InjuryUtil.class)) {
            injuryUtil.when(() -> InjuryUtil.genHealingTime(any(), any(), any(), anyInt())).thenReturn(0);

            AdvancedMedicalAlternateImplants.giveEIImplant(campaign, person);

            verify(person).addInjury(any());
            verify(options, never()).acquireAbility(anyString(), anyString(), any());
        }
    }
    // endregion giveEIImplant

    // region performEnhancedImagingDegradationCheck
    @Test
    @DisplayName("performEnhancedImagingDegradationCheck returns early with no affected implant and no prosthetic overload")
    void performEnhancedImagingDegradationCheck_earlyReturnWhenNothingAffected() {
        Campaign campaign = mock(Campaign.class);
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);

        when(person.getOptions()).thenReturn(options);
        // hasAffectedImplant is false only when none of the relevant implants are present.
        when(options.booleanOption(anyString())).thenReturn(false);
        when(person.getProstheticInjuries()).thenReturn(List.of());

        AdvancedMedicalAlternateImplants.performEnhancedImagingDegradationCheck(campaign, person);

        verify(person, never()).changePermanentFatigue(anyInt());
        verify(campaign, never()).getCampaignOptions();
    }

    @Test
    @DisplayName("performEnhancedImagingDegradationCheck returns early when neither fatigue nor abilities are in use")
    void performEnhancedImagingDegradationCheck_earlyReturnWhenNoSystemsInUse() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);

        when(person.getOptions()).thenReturn(options);
        when(options.booleanOption(anyString())).thenReturn(false);
        when(options.booleanOption(MD_EI_IMPLANT)).thenReturn(true); // hasAffectedImplant becomes true
        when(person.getProstheticInjuries()).thenReturn(List.of());
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(false);
        when(campaignOptions.get(CampaignOption.USE_ABILITIES)).thenReturn(false);

        AdvancedMedicalAlternateImplants.performEnhancedImagingDegradationCheck(campaign, person);

        verify(person, never()).changePermanentFatigue(anyInt());
        verify(person, never()).getPhenotype();
    }

    @Test
    @DisplayName("performEnhancedImagingDegradationCheck applies fatigue and logs reports on a triggering year")
    void performEnhancedImagingDegradationCheck_appliesFatigueAndReports() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        Person person = mock(Person.class);
        PersonnelOptions options = mock(PersonnelOptions.class);
        AttributeCheck attributeCheck = mock(AttributeCheck.class);
        ActionCheckResult attributeCheckResult = mock(ActionCheckResult.class);

        when(person.getOptions()).thenReturn(options);
        when(options.booleanOption(anyString())).thenReturn(false);
        // Enhanced Imaging on a non-Aerospace phenotype gives a frequency of 1, so every year triggers.
        when(options.booleanOption(MD_EI_IMPLANT)).thenReturn(true);
        when(person.getProstheticInjuries()).thenReturn(List.of());
        when(person.getPhenotype()).thenReturn(Phenotype.GENERAL);
        when(person.getHyperlinkedFullTitle()).thenReturn("Trooper");
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(true);
        when(campaignOptions.get(CampaignOption.USE_ABILITIES)).thenReturn(false);
        when(campaign.getGameYear()).thenReturn(3025);
        when(person.checkAttributes(SkillAttribute.BODY, SkillAttribute.WILLPOWER)).thenReturn(attributeCheck);
        when(attributeCheck.withMiscModifier(anyInt())).thenReturn(attributeCheck);
        when(attributeCheck.resolve(anyBoolean(), any())).thenReturn(attributeCheckResult);
        when(attributeCheckResult.isSuccess()).thenReturn(true);
        when(attributeCheckResult.getReport()).thenReturn("check report");

        try (MockedStatic<MHQInternationalization> i18n = mockStatic(MHQInternationalization.class);
              MockedStatic<ReportingUtilities> reporting = mockStatic(ReportingUtilities.class)) {
            i18n.when(() -> MHQInternationalization.getTextAt(anyString(), anyString())).thenReturn("skill check");
            i18n.when(() -> MHQInternationalization.getFormattedTextAt(anyString(), anyString(),
                  any(), any(), any())).thenReturn("fatigue report");

            AdvancedMedicalAlternateImplants.performEnhancedImagingDegradationCheck(campaign, person);

            verify(person).changePermanentFatigue(1);
            verify(attributeCheck).withMiscModifier(0);
            verify(campaign).addReport(eq(MEDICAL), eq("fatigue report"));
            verify(campaign).addReport(eq(SKILL_CHECKS), eq("check report"));
        }
    }
    // endregion performEnhancedImagingDegradationCheck

    private static Injury prostheticInjury(InjuryType injuryType) {
        Injury injury = mock(Injury.class);
        lenient().when(injury.getType()).thenReturn(injuryType);
        return injury;
    }

    private static <T> T invokePrivateStatic(String methodName, Class<?>[] parameterTypes, Object... args)
          throws Exception {
        Method method = AdvancedMedicalAlternateImplants.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        T result = (T) method.invoke(null, args);
        return result;
    }
}
