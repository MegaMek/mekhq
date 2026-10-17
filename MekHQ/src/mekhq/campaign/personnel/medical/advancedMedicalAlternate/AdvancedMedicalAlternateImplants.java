/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import static megamek.common.options.OptionsConstants.MD_VDNI;
import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static mekhq.campaign.enums.DailyReportType.MEDICAL;
import static mekhq.campaign.enums.DailyReportType.SKILL_CHECKS;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static mekhq.campaign.personnel.medical.BodyLocation.BRAIN;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.ENHANCED_IMAGING_IMPLANT;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.ENHANCED_IMAGING;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.getProstheticTypeFromInjuryType;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.skills.ActionCheckResult;
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
     * @param campaign the current {@link Campaign}, used for game year, logging, and other campaign context
     * @param person   the {@link Person} to evaluate; must not be {@code null}
     */
    public static void performEnhancedImagingDegradationCheck(Campaign campaign, Person person) {
        PersonnelOptions options = person.getOptions();
        boolean hasEnhancedImaging = options.booleanOption(OptionsConstants.MD_EI_IMPLANT);
        boolean hasVDNI = options.booleanOption(MD_VDNI);
        boolean hasBufferedVDNI = options.booleanOption(MD_BVDNI);
        boolean hasTooManyProsthetics = isHasTooManyProsthetics(person);
        boolean hasAffectedImplant = hasEnhancedImaging || hasVDNI || hasBufferedVDNI;

        if (!hasAffectedImplant && !hasTooManyProsthetics) {
            return;
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean useFatigue = campaignOptions.get(CampaignOption.USE_FATIGUE);
        boolean useAbilities = campaignOptions.get(CampaignOption.USE_ABILITIES);

        if (!useFatigue && !useAbilities) { // We have nothing to process
            return;
        }

        int frequency = getFrequency(person.getPhenotype().isAerospace(), hasBufferedVDNI, hasVDNI, hasEnhancedImaging);

        int gameYear = campaign.getGameYear();
        if (hasAffectedImplant && (gameYear % frequency == 0)) {
            processDegradationEffects(campaign, person, useFatigue, useAbilities, false);
        }

        frequency = 3;
        if (hasTooManyProsthetics && (gameYear % frequency == 0)) {
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

            campaign.addReport(MEDICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  key,
                  spanOpeningWithCustomColor(getWarningColor()),
                  CLOSING_SPAN_TAG,
                  person.getHyperlinkedFullTitle()));
        }

        int resistanceModifier = person.getOptions().booleanOption(UNOFFICIAL_IMPLANT_RESISTANCE) ? -2 : 0;
        ActionCheckResult attributeCheckResult =
              person.checkAttributes(SkillAttribute.BODY, SkillAttribute.WILLPOWER)
                    .withMiscModifier(resistanceModifier)
                    .resolve(true, getTextAt(RESOURCE_BUNDLE, "AlternateInjuries.skillCheck.degradation"));
        campaign.addReport(SKILL_CHECKS, attributeCheckResult.getReport());

        if (!attributeCheckResult.isSuccess() && useAbilities) {
            String flaw = getAndApplyEIDegradationFlaw(person);
            if (!flaw.isBlank()) {
                String key = "AlternateInjuries.report." +
                                   (isTooManyProsthetics ? "prosthetics" : "implant") +
                                   ".degradation";

                campaign.addReport(MEDICAL, getFormattedTextAt(RESOURCE_BUNDLE,
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

        for (Injury injury : person.getProstheticInjuries()) {
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

        return eligibleProstheticsCount == prostheticThreshold;
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
     *     with the Aerospace phenotype. This follows ATOW p. 317, though MekHQ simplifies the “every 3rd full year"
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

    /**
     * An implant that Advanced Alternate Medical works out for itself each day from the prosthetics a person carries,
     * rather than granting it when a prosthetic is fitted.
     *
     * @param option        the implant's pilot option
     * @param prosthetics   the prosthetics that count towards it
     * @param requiredCount how many of those prosthetics must be present for the implant to take effect
     */
    private record DerivedImplant(String option, List<ProstheticType> prosthetics, int requiredCount) {
        private int countIn(List<Injury> injuries) {
            int count = 0;
            for (Injury injury : injuries) {
                for (ProstheticType prosthetic : prosthetics) {
                    if (injury.getType() == prosthetic.getInjuryType()) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

    /**
     * The implants that only take effect once enough of the body is covered. Dermal armour, dermal camouflage and
     * triple-strength myomer need all four limbs (IO:AE p. 74-75); prosthetic leg MASC needs both legs (ATOW Companion
     * p. 187).
     */
    private static final List<DerivedImplant> DERIVED_IMPLANTS = List.of(
          new DerivedImplant(OptionsConstants.MD_DERMAL_ARMOR,
                List.of(ProstheticType.DERMAL_MYOMER_ARM_ARMOR, ProstheticType.DERMAL_MYOMER_LEG_ARMOR), 4),
          new DerivedImplant(OptionsConstants.MD_DERMAL_CAMO_ARMOR,
                List.of(ProstheticType.DERMAL_MYOMER_ARM_CAMO, ProstheticType.DERMAL_MYOMER_LEG_CAMO), 4),
          new DerivedImplant(OptionsConstants.MD_TSM_IMPLANT,
                List.of(ProstheticType.DERMAL_MYOMER_ARM_TRIPLE, ProstheticType.DERMAL_MYOMER_LEG_TRIPLE), 4),
          new DerivedImplant(OptionsConstants.MD_PL_MASC, List.of(ProstheticType.PROSTHETIC_LEG_MASC), 2));

    /**
     * The prosthetics that stand behind each implant option, used to put an implant that was granted directly - by
     * the Command Generator, or before Advanced Alternate Medical was switched on - into the medical record.
     *
     * <p>Where the medical record splits one implant into several pieces of surgery, every piece is listed. The
     * triple-core processor is absent: it only has a prosthetic in combination with a buffered VDNI, which
     * {@link #getProstheticsFor(String, Collection)} handles.</p>
     */
    private static final Map<String, List<ProstheticType>> IMPLANT_PROSTHETICS = Map.ofEntries(
          Map.entry(OptionsConstants.MD_BOOST_COMM_IMPLANT,
                List.of(ProstheticType.CYBERNETIC_EAR_BOOSTED_COMMUNICATIONS)),
          Map.entry(OptionsConstants.MD_BVDNI, List.of(ProstheticType.BUFFERED_VDNI)),
          Map.entry(OptionsConstants.MD_COMM_IMPLANT, List.of(ProstheticType.CYBERNETIC_EAR_COMMUNICATIONS)),
          Map.entry(OptionsConstants.MD_CYBER_IMP_AUDIO, List.of(ProstheticType.CYBERNETIC_EAR_ENHANCED)),
          Map.entry(OptionsConstants.MD_CYBER_IMP_LASER, List.of(ProstheticType.CYBERNETIC_EYE_LASER)),
          Map.entry(OptionsConstants.MD_CYBER_IMP_TELE, List.of(ProstheticType.CYBERNETIC_EYE_TELESCOPE)),
          Map.entry(OptionsConstants.MD_CYBER_IMP_VISUAL, List.of(ProstheticType.CYBERNETIC_EYE_EM_IR)),
          Map.entry(OptionsConstants.MD_ENH_MM_IMPLANTS, List.of(ProstheticType.CYBERNETIC_EYE_MULTI_ENHANCED)),
          Map.entry(OptionsConstants.MD_MM_IMPLANTS,
                List.of(ProstheticType.CYBERNETIC_EAR_MULTI, ProstheticType.CYBERNETIC_EYE_MULTI)),
          Map.entry(OptionsConstants.MD_PAIN_SHUNT, List.of(ProstheticType.PAIN_SHUNT)),
          Map.entry(OptionsConstants.MD_VDNI, List.of(ProstheticType.VDNI)),
          Map.entry(OptionsConstants.MD_DERMAL_ARMOR,
                List.of(ProstheticType.DERMAL_MYOMER_ARM_ARMOR, ProstheticType.DERMAL_MYOMER_LEG_ARMOR)),
          Map.entry(OptionsConstants.MD_DERMAL_CAMO_ARMOR,
                List.of(ProstheticType.DERMAL_MYOMER_ARM_CAMO, ProstheticType.DERMAL_MYOMER_LEG_CAMO)),
          Map.entry(OptionsConstants.MD_TSM_IMPLANT,
                List.of(ProstheticType.DERMAL_MYOMER_ARM_TRIPLE, ProstheticType.DERMAL_MYOMER_LEG_TRIPLE)),
          Map.entry(OptionsConstants.MD_GAS_EFFUSER_PHEROMONE, List.of(ProstheticType.PHEROMONE_EFFUSER)),
          Map.entry(OptionsConstants.MD_GAS_EFFUSER_TOXIN, List.of(ProstheticType.TOXIN_EFFUSER)),
          Map.entry(OptionsConstants.MD_PL_ENHANCED, List.of(ProstheticType.ENHANCED_PROSTHETIC_LIMB)),
          Map.entry(OptionsConstants.MD_PL_I_ENHANCED, List.of(ProstheticType.IMPROVED_ENHANCED_PROSTHETIC_LIMB)),
          Map.entry(OptionsConstants.MD_PL_MASC, List.of(ProstheticType.PROSTHETIC_LEG_MASC)),
          Map.entry(OptionsConstants.MD_PL_EXTRA_LIMBS, List.of(ProstheticType.EXTRANEOUS_PROSTHETIC_LIMBS)),
          Map.entry(OptionsConstants.MD_PL_TAIL, List.of(ProstheticType.ENHANCED_TAIL_PROSTHETIC)),
          Map.entry(OptionsConstants.MD_PL_GLIDER, List.of(ProstheticType.GLIDER_WINGS)),
          Map.entry(OptionsConstants.MD_PL_FLIGHT, List.of(ProstheticType.POWERED_FLIGHT_WINGS)),
          Map.entry(OptionsConstants.MD_SUICIDE_IMPLANTS, List.of(ProstheticType.EXPLOSIVE_SUICIDE_IMPLANT)));

    /**
     * Works out each implant that depends on how much of the body its prosthetics cover, and writes the answer over
     * the implant option.
     *
     * <p>Despite the name, this covers triple-strength myomer and prosthetic leg MASC as well as the two dermal
     * implants.</p>
     *
     * @param person the person to check
     */
    public static void checkForDermalEligibility(Person person) {
        List<Injury> injuries = person.getProstheticInjuries();
        PersonnelOptions options = person.getOptions();
        for (DerivedImplant derivedImplant : DERIVED_IMPLANTS) {
            IOption option = options.getOption(derivedImplant.option());
            if (option != null) {
                option.setValue(derivedImplant.countIn(injuries) >= derivedImplant.requiredCount());
            }
        }
    }

    /**
     * Puts the prosthetics behind each of the given implants into a person's medical record, so the medical system
     * can see implants that were granted directly rather than through surgery.
     *
     * <p>Each prosthetic is recorded at every location it covers, skipping any location that already carries it, so
     * calling this twice records nothing new. Entries are made at severity zero, which is how a fitted prosthetic is
     * marked rather than a wound.</p>
     *
     * @param campaign the campaign the person belongs to
     * @param person   the person to record prosthetics for
     * @param implants the implant options the person carries
     *
     * @return the number of prosthetics newly recorded
     */
    public static int recordImplantProsthetics(Campaign campaign, Person person, Collection<String> implants) {
        int recorded = 0;
        for (String implant : implants) {
            for (ProstheticType prosthetic : getProstheticsFor(implant, implants)) {
                recorded += recordProsthetic(campaign, person, prosthetic);
            }
        }
        return recorded;
    }

    /**
     * Records the prosthetics behind any derived implant a person holds without them.
     *
     * <p>Derived implants are cleared each day unless enough of their prosthetics are present. A person granted one
     * before it was derived - or before Advanced Alternate Medical was switched on - carries none of them, and would
     * lose the implant on the next day. Someone with some but not all of the prosthetics is left alone: that is a
     * record the daily check already reads correctly.</p>
     *
     * @param campaign the campaign the person belongs to
     * @param person   the person to check
     *
     * @return the number of prosthetics newly recorded
     */
    public static int recordMissingDerivedImplantProsthetics(Campaign campaign, Person person) {
        List<Injury> injuries = person.getProstheticInjuries();
        PersonnelOptions options = person.getOptions();
        List<String> unsupported = new ArrayList<>();
        for (DerivedImplant derivedImplant : DERIVED_IMPLANTS) {
            if (options.booleanOption(derivedImplant.option()) && (derivedImplant.countIn(injuries) == 0)) {
                unsupported.add(derivedImplant.option());
            }
        }
        return recordImplantProsthetics(campaign, person, unsupported);
    }

    /**
     * @param implant  the implant option to look up
     * @param implants every implant option the person carries, as some prosthetics stand for a pair of implants
     *
     * @return the prosthetics behind that implant, empty if it has none
     */
    private static List<ProstheticType> getProstheticsFor(String implant, Collection<String> implants) {
        // A buffered VDNI with a triple-core processor is one piece of surgery. With a standard VDNI the processor has
        // no prosthetic of its own, so it goes unrecorded.
        if (implant.equals(OptionsConstants.MD_BVDNI) && implants.contains(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR)) {
            return List.of(ProstheticType.BUFFERED_VDNI_TRIPLE_CORE);
        }
        return IMPLANT_PROSTHETICS.getOrDefault(implant, List.of());
    }

    /**
     * Records one prosthetic at every location it covers, skipping any location that already carries it.
     *
     * @return the number of locations newly recorded
     */
    private static int recordProsthetic(Campaign campaign, Person person, ProstheticType prosthetic) {
        InjuryType injuryType = prosthetic.getInjuryType();
        int recorded = 0;
        for (BodyLocation location : prosthetic.getEligibleLocations()) {
            if (hasProsthetic(person, injuryType, location)) {
                continue;
            }
            person.addInjury(injuryType.newInjury(campaign, person, location, 0));
            recorded++;
        }
        return recorded;
    }

    /**
     * @return {@code true} where the person already carries that prosthetic at that location
     */
    private static boolean hasProsthetic(Person person, InjuryType injuryType, BodyLocation location) {
        for (Injury injury : person.getProstheticInjuries()) {
            if ((injury.getType() == injuryType) && (injury.getLocation() == location)) {
                return true;
            }
        }
        return false;
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

        if (campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS)) {
            for (String implant : ENHANCED_IMAGING.getAssociatedPilotOptions()) {
                person.getOptions().acquireAbility(LVL3_ADVANTAGES, implant, true);
            }
        }

        if (campaign.getCampaignOptions().get(CampaignOption.USE_ABILITIES)) {
            for (String option : ENHANCED_IMAGING.getAssociatedPersonnelOptions()) {
                person.getOptions().acquireAbility(LVL3_ADVANTAGES, option, true);
            }
        }
    }
}
