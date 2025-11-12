/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version or (at your option) any later version,
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
import static megamek.common.options.OptionsConstants.UNOFFICIAL_EI_IMPLANT;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static mekhq.campaign.personnel.medical.BodyLocation.INTERNAL;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AlternateInjuries.ENHANCED_IMAGING_IMPLANT;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.ProstheticType.ENHANCED_IMAGING;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.skills.AttributeCheckUtility;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

public class AdvancedMedicalAlternate {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AlternateInjuries";
    private static final MMLogger LOGGER = MMLogger.create(AdvancedMedicalAlternate.class);

    static final int MAXIMUM_INJURY_DURATION_MULTIPLIER = 3;

    private static final Map<BodyLocation, BodyRegion> BODY_LOCATION_BODY_REGION_MAP = Map.ofEntries(
          Map.entry(BodyLocation.HEAD, BodyRegion.HEAD),
          Map.entry(BodyLocation.LEFT_HAND, BodyRegion.LEFT_HAND),
          Map.entry(BodyLocation.LEFT_ARM, BodyRegion.LEFT_ARM),
          Map.entry(BodyLocation.RIGHT_HAND, BodyRegion.RIGHT_HAND),
          Map.entry(BodyLocation.RIGHT_ARM, BodyRegion.RIGHT_ARM),
          Map.entry(BodyLocation.LEFT_FOOT, BodyRegion.LEFT_FOOT),
          Map.entry(BodyLocation.LEFT_LEG, BodyRegion.LEFT_LEG),
          Map.entry(BodyLocation.RIGHT_FOOT, BodyRegion.RIGHT_FOOT),
          Map.entry(BodyLocation.RIGHT_LEG, BodyRegion.RIGHT_LEG)
    );

    private enum BodyRegion {
        HEAD, LEFT_ARM, LEFT_HAND, RIGHT_ARM, RIGHT_HAND,
        LEFT_LEG, LEFT_FOOT, RIGHT_LEG, RIGHT_FOOT, OTHER
    }

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


    public static Collection<Injury> generateInjuriesFromHits(Campaign campaign, Person person, int hits) {
        List<Injury> newInjuries = new ArrayList<>();

        InjuryLocationService injuryLocationService = new InjuryLocationService(person);
        for (int i = 0; i < hits; i++) {
            InjuryLocationData injuryLocation = injuryLocationService.getInjuryLocation();
            Injury injury = pickInjury(campaign, person, injuryLocation);
            if (injury == null) {
                continue;
            }

            newInjuries.add(injury);
            // Update the limb presence map if we just removed a limb. From this point onwards we will no longer
            // generate injuries for that location
            if (injury.getType().impliesMissingLocation()) {
                injuryLocationService.setBodyLocationMissing(injury.getLocation());
            }
        }

        return newInjuries;
    }

    private static @Nullable Injury pickInjury(Campaign campaign, Person person,
          InjuryLocationData injuryLocationData) {
        InjuryType injuryType = injuryLocationData.injuryType();
        BodyLocation bodyLocation = injuryLocationData.bodyLocation();
        int injuryDurationMultiplier = injuryLocationData.injuryDurationMultiplier();

        Injury newInjury = injuryType.newInjury(campaign, person, bodyLocation, injuryDurationMultiplier);
        if (newInjury == null) {
            LOGGER.error("Failed to generate injury of type {} at body location {} with duration multiplier {}",
                  injuryType, bodyLocation, injuryDurationMultiplier);
        }

        return newInjury;
    }

    public static void purgeIllogicalInjuries(Person person) {
        List<Injury> injuries = person.getInjuries();

        // Collect all severed locations
        Set<BodyLocation> severedLocations = EnumSet.noneOf(BodyLocation.class);
        for (Injury injury : injuries) {
            if (injury.getType().impliesMissingLocation()) {
                severedLocations.add(injury.getLocation());
            }
        }

        if (severedLocations.isEmpty()) {
            return;
        }

        // Collect injuries to remove (can't remove while iterating)
        List<Injury> injuriesToRemove = new ArrayList<>();
        for (Injury injury : injuries) {
            if (!injury.getType().impliesMissingLocation() && isInSeveredLocation(injury, severedLocations)) {
                injuriesToRemove.add(injury);
            }
        }

        // Remove each injury
        for (Injury injury : injuriesToRemove) {
            person.removeInjury(injury);
        }
    }

    private static boolean isInSeveredLocation(Injury injury, Set<BodyLocation> severedLocations) {
        BodyLocation injuryLocation = injury.getLocation();

        for (BodyLocation severedLocation : severedLocations) {
            if (injuryLocation.isChildOf(severedLocation) || injuryLocation == severedLocation) {
                return true;
            }
        }

        return false;
    }

    public static List<InjuryEffect> getAllActiveInjuryEffects(boolean isAmbidextrous, List<Injury> currentInjuries) {
        if (currentInjuries.isEmpty()) {
            return List.of(InjuryEffect.NONE);
        }

        // Group effects by body region
        Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion = new EnumMap<>(BodyRegion.class);
        for (BodyRegion region : BodyRegion.values()) {
            effectsByRegion.put(region, EnumSet.noneOf(InjuryEffect.class));
        }

        collectEffectsByRegion(currentInjuries, effectsByRegion);
        processSeverance(effectsByRegion, isAmbidextrous);

        // If head is severed, nothing else matters
        if (effectsByRegion.get(BodyRegion.HEAD).contains(InjuryEffect.SEVERED)) {
            return List.of(InjuryEffect.SEVERED);
        }

        // Merge limb subsections and resolve fracture precedence
        mergeLimbEffects(effectsByRegion);

        // Combine all effects and sort
        List<InjuryEffect> allEffects = effectsByRegion.values().stream()
                                              .flatMap(Collection::stream)
                                              .sorted()
                                              .toList();

        return allEffects.isEmpty() ? List.of(InjuryEffect.NONE) : new ArrayList<>(allEffects);
    }

    private static void collectEffectsByRegion(List<Injury> injuries,
          Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion) {
        for (Injury injury : injuries) {
            InjuryEffect effect = injury.getInjuryEffect();
            if (effect == InjuryEffect.NONE) {
                continue;
            }

            BodyRegion region = determineBodyRegion(injury.getLocation());
            effectsByRegion.get(region).add(effect);
        }
    }

    private static BodyRegion determineBodyRegion(BodyLocation location) {
        for (Map.Entry<BodyLocation, BodyRegion> entry : BODY_LOCATION_BODY_REGION_MAP.entrySet()) {
            if (location.isChildOf(entry.getKey())) {
                return entry.getValue();
            }
        }
        return BodyRegion.OTHER;
    }

    private static void processSeverance(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion,
          boolean isAmbidextrous) {
        // Handle ambidextrous arm compensation
        processArmSeverance(effectsByRegion, isAmbidextrous);

        // Severed leg includes foot
        processSeveredLimb(effectsByRegion, BodyRegion.LEFT_LEG, BodyRegion.LEFT_FOOT);
        processSeveredLimb(effectsByRegion, BodyRegion.RIGHT_LEG, BodyRegion.RIGHT_FOOT);

        // Process individual severed hands/feet/head
        for (BodyRegion region : List.of(BodyRegion.HEAD, BodyRegion.LEFT_HAND, BodyRegion.RIGHT_HAND,
              BodyRegion.LEFT_FOOT, BodyRegion.RIGHT_FOOT)) {
            EnumSet<InjuryEffect> effects = effectsByRegion.get(region);
            if (effects.contains(InjuryEffect.SEVERED)) {
                replaceWithSevered(effects);
            }
        }
    }

    private static void processArmSeverance(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion,
          boolean isAmbidextrous) {
        boolean leftArmSevered = effectsByRegion.get(BodyRegion.LEFT_ARM).contains(InjuryEffect.SEVERED);
        boolean rightArmSevered = effectsByRegion.get(BodyRegion.RIGHT_ARM).contains(InjuryEffect.SEVERED);

        if (isAmbidextrous && leftArmSevered != rightArmSevered) {
            // Only one arm severed - ambidextrous can compensate, remove the effect
            if (leftArmSevered) {
                effectsByRegion.get(BodyRegion.LEFT_ARM).remove(InjuryEffect.SEVERED);
            } else {
                effectsByRegion.get(BodyRegion.RIGHT_ARM).remove(InjuryEffect.SEVERED);
            }
        } else {
            // Both arms severed, no arms severed, or not ambidextrous - process normally
            processSeveredLimb(effectsByRegion, BodyRegion.LEFT_ARM, BodyRegion.LEFT_HAND);
            processSeveredLimb(effectsByRegion, BodyRegion.RIGHT_ARM, BodyRegion.RIGHT_HAND);
        }
    }

    private static void processSeveredLimb(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion,
          BodyRegion mainLimb, BodyRegion subLimb) {
        if (effectsByRegion.get(mainLimb).contains(InjuryEffect.SEVERED)) {
            effectsByRegion.get(subLimb).clear();
            replaceWithSevered(effectsByRegion.get(mainLimb));
        }
    }

    private static void replaceWithSevered(EnumSet<InjuryEffect> effects) {
        effects.clear();
        effects.add(InjuryEffect.SEVERED);
    }

    private static void mergeLimbEffects(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion) {
        mergeLimbAndResolve(effectsByRegion, BodyRegion.LEFT_ARM, BodyRegion.LEFT_HAND);
        mergeLimbAndResolve(effectsByRegion, BodyRegion.RIGHT_ARM, BodyRegion.RIGHT_HAND);
        mergeLimbAndResolve(effectsByRegion, BodyRegion.LEFT_LEG, BodyRegion.LEFT_FOOT);
        mergeLimbAndResolve(effectsByRegion, BodyRegion.RIGHT_LEG, BodyRegion.RIGHT_FOOT);
    }

    private static void mergeLimbAndResolve(Map<BodyRegion, EnumSet<InjuryEffect>> effectsByRegion,
          BodyRegion mainLimb, BodyRegion subLimb) {
        EnumSet<InjuryEffect> mainEffects = effectsByRegion.get(mainLimb);
        mainEffects.addAll(effectsByRegion.get(subLimb));

        if (mainEffects.contains(InjuryEffect.COMPOUND_FRACTURE)) {
            mainEffects.remove(InjuryEffect.FRACTURE_LIMB);
        }
    }

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
        if (!person.getOptions().booleanOption(UNOFFICIAL_EI_IMPLANT)) {
            return;
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean useFatigue = campaignOptions.isUseFatigue();
        boolean useAbilities = campaignOptions.isUseAbilities();

        if (!useFatigue && !useAbilities) { // We have nothing to process
            return;
        }

        int gameYear = campaign.getGameYear();

        // Occurs every year for most phenotypes, but every 3rd year for the Aerospace phenotype (ATOW pg 317). ATOW
        // states that this occurs every 3rd full year, but I didn't want to add even more tracking to Person for
        // such a niche thing, so instead it hits every 3rd canonical year.
        boolean incrementPermanentFatigueDamage = !person.getPhenotype().isAerospace() || gameYear % 3 == 0;
        if (incrementPermanentFatigueDamage) {
            if (useFatigue) {
                person.changePermanentFatigue(1);
                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                      "AlternateInjuries.report.ei.fatigue",
                      spanOpeningWithCustomColor(getWarningColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle()));
            }

            AttributeCheckUtility attributeCheckUtility = new AttributeCheckUtility(person, SkillAttribute.BODY,
                  SkillAttribute.WILLPOWER, new ArrayList<>(), 0, true, false);
            campaign.addReport(attributeCheckUtility.getResultsText());

            if (!attributeCheckUtility.isSuccess() && useAbilities) {
                String flaw = getAndApplyEIDegradationFlaw(person);
                if (!flaw.isBlank()) {
                    campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                          "AlternateInjuries.report.ei.degradation",
                          spanOpeningWithCustomColor(getNegativeColor()),
                          CLOSING_SPAN_TAG,
                          person.getHyperlinkedFullTitle(), flaw));
                }
            }
        }
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
        Injury injury = ENHANCED_IMAGING_IMPLANT.newInjury(campaign, person, INTERNAL, 0);
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
