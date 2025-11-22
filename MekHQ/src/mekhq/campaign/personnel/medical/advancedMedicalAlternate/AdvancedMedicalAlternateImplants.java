/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.options.OptionsConstants.ATOW_COMBAT_PARALYSIS;
import static megamek.common.options.OptionsConstants.MD_BVDNI;
import static megamek.common.options.OptionsConstants.MD_DERMAL_ARMOR;
import static megamek.common.options.OptionsConstants.MD_DERMAL_CAMO_ARMOR;
import static megamek.common.options.OptionsConstants.MD_VDNI;
import static megamek.common.options.OptionsConstants.UNOFFICIAL_EI_IMPLANT;
import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static mekhq.campaign.personnel.medical.BodyLocation.BRAIN;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.DERMAL_MYOMER_ARM_ARMOR;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.DERMAL_MYOMER_ARM_CAMO;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.DERMAL_MYOMER_LEG_ARMOR;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.DERMAL_MYOMER_LEG_CAMO;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.ENHANCED_IMAGING_IMPLANT;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.DERMAL_MYOMER_ARM_ARMOR;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.ENHANCED_IMAGING;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.getProstheticTypeFromInjuryType;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.List;

import megamek.codeUtilities.ObjectUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.AttributeCheckUtility;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

public class AdvancedMedicalAlternateImplants {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AlternateInjuries";

    // This next section is for possible Flaws that can be gained from EI Implant degradation
    static final String COMPULSION_PLACEHOLDER = "COMPULSION";
    // This 'table' is stacked based on the frequency we want each to occur
    private static final List<String> POSSIBLE_FLAWS = List.of(
          ATOW_COMBAT_PARALYSIS,
          ATOW_COMBAT_PARALYSIS,
          ATOW_COMBAT_PARALYSIS,
          FLAW_UNFIT, // We don't have the Handicap Flaw, so we're using this instead
          FLAW_UNFIT,
          FLAW_UNFIT,
          FLAW_UNFIT,
          FLAW_UNFIT,
          FLAW_UNFIT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_IMPATIENT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          FLAW_INTROVERT,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          COMPULSION_PLACEHOLDER,
          FLAW_SLOW_LEARNER,
          FLAW_SLOW_LEARNER,
          FLAW_SLOW_LEARNER,
          FLAW_SLOW_LEARNER
    );

    // We're only including the 100 xp Flaws here as some of the more expensive ones get really nasty
    private static final List<String> POSSIBLE_COMPULSIONS = List.of(
          COMPULSION_UNPLEASANT_PERSONALITY,
          COMPULSION_MILD_PARANOIA,
          COMPULSION_RACISM,
          COMPULSION_RELIGIOUS_FANATICISM,
          COMPULSION_FACTION_PRIDE,
          COMPULSION_GAMBLING,
          COMPULSION_ANARCHIST
    );

    /**
     * Performs the periodic Enhanced Imaging degradation check for a character.
     *
     * <p>This method only runs for characters that have the {@code UNOFFICIAL_EI_IMPLANT} personnel option enabled.
     * For most phenotypes, the degradation check occurs every year; for Aerospace phenotypes it only occurs every third
     * canonical game year (ATOW p. 317).</p>
     *
     * <p>When the check triggers, this method:</p>
     * <ol>
     *   <li>Increases the character's permanent fatigue by 1.</li>
     *   <li>Logs a warning report to the campaign.</li>
     *   <li>Performs a BODY-WILLPOWER attribute check.</li>
     *   <li>On a failed check, randomly applies a new Flaw representing EI degradation and logs an additional
     *   report.</li>
     * </ol>
     *
     * @param campaign the current {@link Campaign}, used for game year, logging and other campaign context
     * @param person   the {@link Person} to evaluate; must not be {@code null}
     */
    public static void performEnhancedImagingDegradationCheck(Campaign campaign, Person person) {
        PersonnelOptions options = person.getOptions();
        boolean hasEnhancedImaging = options.booleanOption(UNOFFICIAL_EI_IMPLANT);
        boolean hasVDNI = options.booleanOption(MD_VDNI);
        boolean hasBufferedVDNI = options.booleanOption(MD_BVDNI);
        boolean hasTooManyProsthetics = isHasTooManyProsthetics(person);

        if (!hasEnhancedImaging && !hasVDNI && !hasBufferedVDNI && !hasTooManyProsthetics) {
            return;
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean useFatigue = campaignOptions.isUseFatigue();
        boolean useAbilities = campaignOptions.isUseAbilities();

        if (!useFatigue && !useAbilities) { // We have nothing to process
            return;
        }

        int frequency = getFrequency(person.getPhenotype().isAerospace(), hasBufferedVDNI, hasVDNI, hasEnhancedImaging);

        int gameYear = campaign.getGameYear();
        if (gameYear % frequency == 0) {
            processDegradationEffects(campaign, person, useFatigue, useAbilities, false);
        }

        frequency = 3;
        if (hasTooManyProsthetics && gameYear % frequency == 0) {
            processDegradationEffects(campaign, person, useFatigue, useAbilities, true);
        }
    }

    private static void processDegradationEffects(Campaign campaign, Person person, boolean useFatigue,
          boolean useAbilities, boolean isTooManyProsthetics) {
        if (useFatigue) {
            person.changePermanentFatigue(1);

            String key = "AlternateInjuries.report." +
                               (isTooManyProsthetics ? "prosthetics" : "implant") +
                               ".fatigue";

            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                  key,
                  spanOpeningWithCustomColor(getWarningColor()),
                  CLOSING_SPAN_TAG,
                  person.getHyperlinkedFullTitle()));
        }

        int resistanceModifier = person.getOptions().booleanOption(UNOFFICIAL_IMPLANT_RESISTANCE) ? -2 : 0;
        AttributeCheckUtility attributeCheckUtility = new AttributeCheckUtility(person, SkillAttribute.BODY,
              SkillAttribute.WILLPOWER, new ArrayList<>(), resistanceModifier, true, false);
        campaign.addReport(attributeCheckUtility.getResultsText());

        if (!attributeCheckUtility.isSuccess() && useAbilities) {
            String flaw = getAndApplyEIDegradationFlaw(person);
            if (!flaw.isBlank()) {
                String key = "AlternateInjuries.report." +
                                   (isTooManyProsthetics ? "prosthetics" : "implant") +
                                   ".degradation";

                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                      key,
                      spanOpeningWithCustomColor(getNegativeColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle(), flaw));
            }
        }
    }

    /**
     * Determines whether the given person has exceeded the allowed number of high-impact prosthetics.
     *
     * <p>This method evaluates all active injuries for the person and counts how many of them map to a
     * {@link ProstheticType} whose prosthetic category value is greater than {@code 3}. These represent major or
     * invasive prosthetics that meaningfully affect the individual's physiology.</p>
     *
     * <p>Once the count reaches three, the method stops early and reports {@code true}. Otherwise, it returns {@code
     * false}, indicating the person does not yet meet the threshold.</p>
     *
     * @param person the person whose prosthetic load is being checked
     *
     * @return {@code true} if the person has at least three qualifying prosthetics; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static boolean isHasTooManyProsthetics(Person person) {
        int prostheticThreshold = 3;
        int eligibleProstheticsCount = 0;

        for (Injury injury : person.getInjuries()) {
            ProstheticType prostheticType = getProstheticTypeFromInjuryType(injury.getType());

            if (prostheticType != null) {
                int type = prostheticType.getProstheticType();
                if (type == 4 || type == 5) {
                    eligibleProstheticsCount++;
                }

                if (eligibleProstheticsCount >= prostheticThreshold) {
                    break;
                }
            }
        }

        return eligibleProstheticsCount >= prostheticThreshold;
    }

    /**
     * Computes how often a character must perform their neurological stability check, based on phenotype and implanted
     * systems.
     *
     * <p>The returned value represents how frequently (in years) the check should occur. A base frequency of
     * {@code 1} year is used unless modified by specific augmentations:</p>
     *
     * <ul>
     *     <li><b>Buffered VDNI</b> — Increases the interval to every 3 years.</li>
     *     <li><b>Standard VDNI</b> — Sets the interval to every 2 years.</li>
     *     <li><b>Enhanced Imaging (EI)</b> — Occurs every year for most characters, but every 3rd year for those
     *     with the Aerospace phenotype. This follows ATOW p. 317, though MekHQ simplifies the “every 3rd full year”
     *     rule to every 3 canonical years to avoid additional state tracking.</li>
     * </ul>
     *
     * <p>Priority is applied in the order listed: EI overrides VDNI, which overrides buffered VDNI and prosthetic
     * overload.</p>
     *
     * @param isAerospacePhenotype whether the character uses an aerospace phenotype body
     * @param hasBufferedVDNI      whether the character possesses buffered VDNI
     * @param hasVDNI              whether the character has standard VDNI
     * @param hasEnhancedImaging   whether the character has Enhanced Imaging installed
     *
     * @return the number of years between required neurological stability checks
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getFrequency(boolean isAerospacePhenotype, boolean hasBufferedVDNI, boolean hasVDNI,
          boolean hasEnhancedImaging) {
        if (hasBufferedVDNI) {
            return 3;
        }

        if (hasVDNI) {
            return 2;
        }

        if (hasEnhancedImaging) {
            // Occurs every year for most characters, but every 3rd year for the Aerospace phenotype (ATOW pg 317). ATOW
            // states that this occurs every 3rd full year, but I didn't want to add even more tracking to Person for
            // such a niche thing, so instead it hits every 3rd canonical year.
            return isAerospacePhenotype ? 3 : 1;
        }

        return 1;
    }

    /**
     * Randomly selects and applies a Flaw representing long-term EI degradation.
     *
     * <p>The selection is weighted by duplicating entries in an internal "table" of candidate Flaws. Some entries
     * represent a generic compulsion placeholder; in that case a specific compulsion Flaw is chosen from a second
     * weighted list of possible compulsions.</p>
     *
     * <p>If the selected Flaw is already present on the character, no changes are made and an empty string is
     * returned. Otherwise, the Flaw is acquired via the {@code LVL3_ADVANTAGES} personnel options and the display name
     * of the applied Flaw is returned.</p>
     *
     * @param person the {@link Person} receiving the EI degradation Flaw
     *
     * @return the display name of the applied Flaw, or an empty string if the character already possessed the randomly
     *       selected Flaw
     */
    public static String getAndApplyEIDegradationFlaw(Person person) {
        String flaw = ObjectUtility.getRandomItem(POSSIBLE_FLAWS);
        if (flaw.equals(COMPULSION_PLACEHOLDER)) {
            flaw = ObjectUtility.getRandomItem(POSSIBLE_COMPULSIONS);
        }

        PersonnelOptions options = person.getOptions();
        if (options.booleanOption(flaw)) { // If they already have the Flaw, they get a free pass
            return "";
        }

        SpecialAbility ability = SpecialAbility.getAbility(flaw);
        if (ability != null) { // This will return null if the ability has been disabled in the player's campaign
            options.acquireAbility(LVL3_ADVANTAGES, flaw, true);

            return ability.getDisplayName();
        } else {
            return "";
        }
    }

    public static void checkForDermalEligibility(Person person) {
        List<Injury> injuries = person.getInjuries();

        int dermalArmorCount = 0;
        int dermalCamoCount = 0;
        for (Injury injury : injuries) {
            InjuryType injuryType = injury.getType();
            if (injuryType == DERMAL_MYOMER_ARM_ARMOR || injuryType == DERMAL_MYOMER_LEG_ARMOR) {
                dermalArmorCount++;
            }

            if (injuryType == DERMAL_MYOMER_ARM_CAMO || injuryType == DERMAL_MYOMER_LEG_CAMO) {
                dermalCamoCount++;
            }
        }

        PersonnelOptions options = person.getOptions();
        int requiredLimbCount = 4;
        options.getOption(MD_DERMAL_ARMOR).setValue(dermalArmorCount >= requiredLimbCount);
        options.getOption(MD_DERMAL_CAMO_ARMOR).setValue(dermalCamoCount >= requiredLimbCount);
    }

    /**
     * Applies the Enhanced Imaging (EI) implant to the specified person.
     *
     * <p><b>Usage:</b> This is predominantly aimed at grandfathering in existing ProtoMek pilots and granting
     * NPC pilots the implant.</p>
     *
     * @param campaign the current {@link Campaign} context; used for injury creation and to check applicable campaign
     *                 options
     * @param person   the {@link Person} receiving the Enhanced Imaging implant
     */
    public static void giveEIImplant(Campaign campaign, Person person) {
        Injury injury = ENHANCED_IMAGING_IMPLANT.newInjury(campaign, person, BRAIN, 0);
        person.addInjury(injury);

        if (campaign.getCampaignOptions().isUseImplants()) {
            for (String implant : ENHANCED_IMAGING.getAssociatedPilotOptions()) {
                person.getOptions().acquireAbility(LVL3_ADVANTAGES, implant, true);
            }
        }

        if (campaign.getCampaignOptions().isUseAbilities()) {
            for (String option : ENHANCED_IMAGING.getAssociatedPersonnelOptions()) {
                person.getOptions().acquireAbility(LVL3_ADVANTAGES, option, true);
            }
        }
    }
}
