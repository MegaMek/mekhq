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
package mekhq.campaign.randomEvents.randomEventsSystem;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.force.FormationType.SECURITY;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_POISON_RESISTANCE;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.skills.SkillType.SKILL_NONE;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventEffectedPersonnelType.CAMP_FOLLOWERS;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventEffectedPersonnelType.COMBAT_PERSONNEL;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventEffectedPersonnelType.PRISONERS;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventEffectedPersonnelType.SECURITY_GUARD;
import static mekhq.campaign.randomEvents.randomEventsSystem.RandomEventEffectedPersonnelType.SUPPORT_PERSONNEL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.InjurySPAUtility;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.selectors.factionSelectors.DefaultFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.DefaultPlanetSelector;
import mekhq.utilities.ReportingUtilities;
import org.jspecify.annotations.NonNull;

/**
 * Manages the resolution and effects of random events during a campaign.
 *
 * <p>This class applies the effects of prisoner-related events, such as injuries, capacity
 * changes, escapes, and loyalty adjustments. It processes event details and effects, manages affected campaign state or
 * personnel, and generates a comprehensive report summarizing the outcomes.</p>
 *
 * <p>The effects manager handles a wide variety of event consequences, including unique cases for
 * specific random events. It also tracks and exposes information like escapees for further processing in the
 * campaign.</p>
 */
public class RandomEventEffectsManager {
    private static final MMLogger LOGGER = MMLogger.create(RandomEventEffectsManager.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    private static final String SINGULAR_CHARACTER_RESOURCE_KEY = "context.character.singular";
    private static final String PLURAL_CHARACTER_RESOURCE_KEY = "context.character.plural";
    private static final String SINGULAR_PRISONER_RESOURCE_KEY = "context.prisoner.singular";
    private static final String PLURAL_PRISONER_RESOURCE_KEY = "context.prisoner.plural";

    private final Campaign campaign;

    private String eventReport = "";
    private final Set<Person> personHashSet = new HashSet<>();

    /**
     * Constructs an {@link RandomEventEffectsManager} object and processes the given event effects.
     *
     * <p>Based on the event data and the player's chosen response, this constructor processes the
     * potential effects of the event, applying their impact to the campaign. These effects are compiled into an event
     * report for further use.</p>
     *
     * @param campaign      The campaign in which the event occurs.
     * @param eventData     The data related to the random event being processed.
     * @param choiceIndex   The index of the user-selected choice for the event.
     * @param wasSuccessful Indicates whether the selected choice was successful.
     */
    public RandomEventEffectsManager(Campaign campaign, RandomEventData eventData, int choiceIndex,
          boolean wasSuccessful) {
        this.campaign = campaign;

        RandomEventResponseEntry responseEntry = eventData.responseEntries().get(choiceIndex);

        List<RandomEventResult> results = wasSuccessful ?
                                                responseEntry.effectsSuccess() :
                                                responseEntry.effectsFailure();

        StringBuilder report = buildMechanicalEffectsReport(eventData, results);
        eventReport = report.toString();
    }

    private @NonNull StringBuilder buildMechanicalEffectsReport(RandomEventData eventData,
          List<RandomEventResult> results) {
        StringBuilder report = new StringBuilder();
        for (RandomEventResult result : results) {
            RandomEventResultEffect effect = result.effect();

            // TODO change to Map-Lookup Pattern, Illiani Jun/22/26
            String mechanicalEffectsReport = switch (effect) {
                case NONE -> "";
                case PRISONER_CAPACITY -> eventEffectPrisonerCapacity(result);
                case INJURY -> eventEffectInjury(result);
                case INJURY_PERCENT -> eventEffectInjuryPercent(result);
                case DEATH -> eventEffectDeath(result);
                case DEATH_PERCENT -> eventEffectDeathPercent(result);
                case SKILL -> eventEffectSkill(result);
                case LOYALTY_ONE -> eventEffectLoyaltyOne(result);
                case LOYALTY_ALL -> eventEffectLoyaltyAll(result);
                case ESCAPE -> eventEffectEscape(result);
                case ESCAPE_PERCENT -> eventEffectEscapePercent(result);
                case FATIGUE_ONE -> eventEffectFatigueOne(result);
                case FATIGUE_ALL -> eventEffectFatigueAll(result);
                case SUPPORT_POINT -> eventEffectSupportPoint(result);
                case UNIQUE -> eventEffectUnique(eventData, result);
            };

            if (!mechanicalEffectsReport.isEmpty()) {
                report.append("- ").append(mechanicalEffectsReport).append("<br>");
            }
        }
        return report;
    }

    /**
     * Retrieves the generated event report summarizing the processed event's effects.
     *
     * <p>The report contains a textual summary of all effects that were applied during the event,
     * structured for easy display to the user or for logs.</p>
     *
     * @return The event report as a {@link String}.
     */
    public String getMechanicalEffectsReport() {
        return eventReport;
    }

    /**
     * Retrieves the set of personnel who have been returned by the processed event.
     *
     * <p>The returned set can be used to track and handle additional consequences, such as updating campaign
     * statistics or creating follow-up events.</p>
     *
     * <p><b>Example:</b> some events use this to return a set of escaped prisoners.</p>
     *
     * @return A {@link Set} of {@link Person} objects representing the prisoners who escaped, or an empty set if no
     *       escapes occurred.
     */
    public Set<Person> getPersonHashSet() {
        return personHashSet;
    }

    /**
     * Selects a random target for an event effect.
     *
     * @param effectedPersonnelTypes a list of {@link RandomEventEffectedPersonnelType} representing the types of
     *                               personnel to be considered for selection.
     *
     * @return The randomly selected {@link Person}, or {@code null} if no valid target exists.
     */
    private @Nullable Person getRandomTarget(final List<RandomEventEffectedPersonnelType> effectedPersonnelTypes) {
        Set<Person> potentialTargets = getAllPotentialTargets(effectedPersonnelTypes);

        if (potentialTargets.isEmpty()) {
            return null;
        } else {
            return getRandomItem(potentialTargets);
        }
    }

    /**
     * Retrieves all potential targets for an event effect.
     *
     * @param effectedPersonnelTypes a list of {@link RandomEventEffectedPersonnelType} representing the types of
     *                               personnel to be considered for selection.
     *
     * @return A {@link List} of {@link Person} objects representing the potential targets.
     */
    private Set<Person> getAllPotentialTargets(final List<RandomEventEffectedPersonnelType> effectedPersonnelTypes) {
        boolean includePrisoners = effectedPersonnelTypes.contains(PRISONERS);
        boolean includeCombatPersonnel = effectedPersonnelTypes.contains(COMBAT_PERSONNEL);
        boolean includeSupportPersonnel = effectedPersonnelTypes.contains(SUPPORT_PERSONNEL);
        boolean includeCampFollowers = effectedPersonnelTypes.contains(CAMP_FOLLOWERS);
        boolean includeSecurityGuards = effectedPersonnelTypes.contains(SECURITY_GUARD);

        Set<Person> potentialTargets = new HashSet<>();
        for (Person person : campaign.getAllPersonnel()) {
            PersonnelStatus personStatus = person.getStatus();
            if (personStatus.isDepartedUnit() || !personStatus.isActiveFlexible()) {
                continue;
            }

            // Prisoner early continue
            if (person.getPrisonerStatus().isCurrentPrisoner()) {
                if (includePrisoners) {
                    potentialTargets.add(person);
                }
                continue;
            }

            // Non-prisoners
            if (includeCombatPersonnel && person.isCombat()) {
                potentialTargets.add(person);
                continue;
            }

            if (includeSupportPersonnel && person.isSupport()) {
                potentialTargets.add(person);
                continue;
            }

            if (includeCampFollowers && personStatus.isCampFollower()) {
                potentialTargets.add(person);
                continue;
            }

            if (includeSecurityGuards) {
                Unit unit = person.getUnit();
                Formation formation = unit.getCampaign().getFormationFor(unit);
                if (formation != null && formation.isFormationType(SECURITY)) {
                    potentialTargets.add(person);
                }
            }
        }

        return potentialTargets;
    }

    /**
     * Applies an effect to adjust the temporary prisoner capacity and generates a report.
     *
     * <p>This effect changes the available capacity based on the magnitude, with positive values
     * increasing capacity and negative values decreasing it. The outcome is logged as part of the event report.</p>
     *
     * @param result The {@link RandomEventResult} detailing the effect and its magnitude.
     *
     * @return A {@link String} summarizing the effect of the operation.
     */
    String eventEffectPrisonerCapacity(RandomEventResult result) {
        final int magnitude = result.magnitude();
        campaign.changeTemporaryPrisonerCapacity(magnitude);

        String colorOpen = magnitude > 0 ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0 ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "PRISONER_CAPACITY.report",
              colorOpen,
              direction,
              CLOSING_SPAN_TAG,
              magnitude);
    }

    /**
     * Handles the effects of an "injury" event, causing injuries to a random target.
     *
     * <p>The injury amount is determined by the magnitude of the event effect. Injuries are
     * applied up to the maximum allowable level per target. The outcome is added to the event report.</p>
     *
     * @param result The {@link RandomEventResult} detailing the injury effect.
     *
     * @return A {@link String} summarizing the injury effect.
     */
    String eventEffectInjury(RandomEventResult result) {
        final List<RandomEventEffectedPersonnelType> affectedPersonnelTypes = result.affectedPersonnelTypes();
        final int magnitude = result.magnitude();

        Person target = getRandomTarget(affectedPersonnelTypes);

        if (target == null) {
            return "";
        }

        // We don't want to accidentally kill anyone. So while someone can get really mauled by
        // this event, they will never actually die.
        int wounds = max(magnitude, 1);

        int priorHits = target.getTotalInjurySeverity();

        wounds = InjurySPAUtility.adjustInjuriesAndFatigueForSPAs(target,
              campaign.getCampaignOptions().isUseInjuryFatigue(),
              campaign.getCampaignOptions().getFatigueRate(), wounds);

        if (priorHits + wounds > 5) {
            wounds = 5 - priorHits;
        }

        target.setHitsPrior(priorHits);
        target.setHits(priorHits + wounds);

        if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
            target.diagnose(campaign, wounds);
        }

        MekHQ.triggerEvent(new PersonChangedEvent(target));

        String colorOpen = target.getPrisonerStatus().isCurrentPrisoner() ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude > 0 ? SINGULAR_CHARACTER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);

        return getFormattedTextAt(RESOURCE_BUNDLE, "INJURY.report", colorOpen, context, CLOSING_SPAN_TAG);
    }

    /**
     * Handles the effects of a "death" event, removing personnel from the campaign due to fatalities.
     *
     * <p>The affected individuals are determined based on the event's magnitude, with normal characters being
     * marked as KIA and prisoners being removed completely. The outcome is added to the event report.</p>
     *
     * @param result The {@link RandomEventResult} detailing the death effect.
     *
     * @return A {@link String} summarizing the death effect.
     */
    String eventEffectInjuryPercent(RandomEventResult result) {
        final boolean isUseAdvancedMedical = campaign.getCampaignOptions().isUseAdvancedMedical();
        final List<RandomEventEffectedPersonnelType> affectedPersonnelTypes = result.affectedPersonnelTypes();
        final double magnitude = (double) result.magnitude() / 100;

        Set<Person> potentialTargets = getAllPotentialTargets(affectedPersonnelTypes);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = (int) max(1, potentialTargets.size() * magnitude);

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseInjuryFatigue = campaignOptions.isUseInjuryFatigue();
        int fatigueRate = campaignOptions.getFatigueRate();
        for (int i = 0; i < targetCount; i++) {
            Person target = getRandomItem(potentialTargets);

            int wounds = Math.clamp(d6(), 1, 5);

            int priorHits = target.getTotalInjurySeverity();

            wounds = InjurySPAUtility.adjustInjuriesAndFatigueForSPAs(target, isUseInjuryFatigue, fatigueRate, wounds);

            if (priorHits + wounds > 5) {
                wounds = 5 - priorHits;
            }

            target.setHitsPrior(priorHits);
            target.setHits(priorHits + wounds);

            if (isUseAdvancedMedical) {
                target.diagnose(campaign, wounds);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(target));

            potentialTargets.remove(target);
        }

        boolean isOnlyPrisonersAffected = affectedPersonnelTypes.size() == 1 &&
                                                affectedPersonnelTypes.contains(PRISONERS);
        String colorOpen = isOnlyPrisonersAffected ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String context;
        if (isOnlyPrisonersAffected) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targetCount != 1 ? PLURAL_PRISONER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targetCount != 1 ? PLURAL_CHARACTER_RESOURCE_KEY : SINGULAR_CHARACTER_RESOURCE_KEY);
        }

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1 ? "pluralizer.have" : "pluralizer.has");

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "INJURY_PERCENT.report",
              targetCount,
              colorOpen,
              context,
              CLOSING_SPAN_TAG,
              haveOrHas);
    }

    /**
     * Handles the escape effect, reducing the number of prisoners and tracking escapees.
     *
     * <p>The magnitude of the event determines how many prisoners escape, and these prisoners
     * are logged for further actions. An escape report is generated as part of the event handling.</p>
     *
     * @param result The {@link RandomEventResult} detailing the escape effect and its magnitude.
     *
     * @return A {@link String} summarizing the escape effect.
     */
    String eventEffectDeath(RandomEventResult result) {
        final List<RandomEventEffectedPersonnelType> affectedPersonnelTypes = result.affectedPersonnelTypes();
        int magnitude = result.magnitude();

        final LocalDate today = campaign.getLocalDate();

        Set<Person> potentialTargets = getAllPotentialTargets(affectedPersonnelTypes);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        magnitude = min(potentialTargets.size(), magnitude);

        for (int i = 0; i < magnitude; i++) {
            Person target = getRandomItem(potentialTargets);

            if (target.getPrisonerStatus().isCurrentPrisoner()) {
                campaign.removePerson(target, false);
            } else {
                target.changeStatus(campaign, today, PersonnelStatus.KIA);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(target));

            potentialTargets.remove(target);
        }

        boolean isOnlyPrisonersAffected = affectedPersonnelTypes.size() == 1 &&
                                                affectedPersonnelTypes.contains(PRISONERS);
        String colorOpen = isOnlyPrisonersAffected ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String context;
        if (isOnlyPrisonersAffected) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  magnitude != 1 ? PLURAL_PRISONER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  magnitude != 1 ? PLURAL_CHARACTER_RESOURCE_KEY : SINGULAR_CHARACTER_RESOURCE_KEY);
        }

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, magnitude != 1 ? "pluralizer.have" : "pluralizer.has");

        String bodyOrBodies = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude != 1 ? "DEATH.body.plural" : "DEATH.body.singular");

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "DEATH.report",
              magnitude,
              colorOpen,
              context,
              CLOSING_SPAN_TAG,
              haveOrHas,
              bodyOrBodies);
    }

    /**
     * Handles unique effects for specific random events and generates a report.
     *
     * <p>This method processes special cases that require custom logic, such as skill removal,
     * faction changes, or morale adjustments. The exact behavior depends on the event type.</p>
     *
     * @param result The {@link RandomEventResult} describing the unique effect.
     *
     * @return A {@link String} summarizing the unique effect.
     */
    private String eventEffectDeathPercent(RandomEventResult result) {
        final List<RandomEventEffectedPersonnelType> affectedPersonnelTypes = result.affectedPersonnelTypes();
        final double magnitude = (double) result.magnitude() / 100;

        final LocalDate today = campaign.getLocalDate();

        Set<Person> potentialTargets = getAllPotentialTargets(affectedPersonnelTypes);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = (int) max(1, potentialTargets.size() * magnitude);
        boolean isOnlyPrisonersAffected = affectedPersonnelTypes.size() == 1 &&
                                                affectedPersonnelTypes.contains(PRISONERS);

        for (int i = 0; i < targetCount; i++) {
            Person target = getRandomItem(potentialTargets);

            if (isOnlyPrisonersAffected) {
                campaign.removePerson(target, false);
            } else {
                target.changeStatus(campaign, today, PersonnelStatus.KIA);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(target));

            potentialTargets.remove(target);
        }

        String colorOpen = isOnlyPrisonersAffected ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String context;
        if (isOnlyPrisonersAffected) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targetCount != 1 ? PLURAL_PRISONER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targetCount != 1 ? PLURAL_CHARACTER_RESOURCE_KEY : SINGULAR_CHARACTER_RESOURCE_KEY);
        }

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1 ? "pluralizer.have" : "pluralizer.has");

        String bodyOrBodies = getFormattedTextAt(RESOURCE_BUNDLE,
              targetCount != 1 ? "DEATH.body.plural" : "DEATH.body.singular");

        // We can reuse the same report as the DEATH effect, here too
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "DEATH.report",
              targetCount,
              colorOpen,
              context,
              CLOSING_SPAN_TAG,
              haveOrHas,
              bodyOrBodies);
    }

    /**
     * Applies the effects of a random event to the skill of a target personnel, modifying their skill level as
     * specified by the event result. This method updates the target's skill and triggers an event indicating the
     * change, if applicable.
     *
     * @param result The {@code RandomEventResult} containing the details of the affected personnel types, the targeted
     *               skill, and the magnitude of the change to be applied.
     *
     * @return A formatted string representing the report of the change, or an empty string if no changes were made to
     *       the target's skills.
     */
    private String eventEffectSkill(RandomEventResult result) {
        final List<RandomEventEffectedPersonnelType> affectedPersonnelTypes = result.affectedPersonnelTypes();
        final int magnitude = result.magnitude();

        // Get skill
        String affectedSkill = result.affectedSkill();
        if (affectedSkill.equals(SKILL_NONE)) {
            return "";
        }


        SkillType skillType = SkillType.getType(affectedSkill);

        if (skillType == null) {
            return "";
        }

        String skillName = skillType.getName();

        // Get target
        Person target = getRandomTarget(affectedPersonnelTypes);

        if (target == null) {
            return "";
        }

        // Apply changes
        boolean hasSkill = target.hasSkill(skillName);
        boolean madeChange = false;
        if (hasSkill) {
            Skill skill = target.getSkill(skillName);
            int currentLevel = skill.getLevel();

            if (magnitude < currentLevel) {
                skill.setLevel(magnitude);
                madeChange = true;
            }
        } else {
            target.addSkill(skillName, magnitude, 0);
            madeChange = true;
        }

        // If we've not made any skill changes, we don't add a report.
        if (!madeChange) {
            return "";
        }

        MekHQ.triggerEvent(new PersonChangedEvent(target));

        String colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              target.getPrisonerStatus().isCurrentPrisoner() ?
                    SINGULAR_PRISONER_RESOURCE_KEY :
                    SINGULAR_CHARACTER_RESOURCE_KEY);

        // We can reuse the same report as the DEATH effect, here too
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "SKILL.report",
              colorOpen,
              context,
              CLOSING_SPAN_TAG,
              magnitude,
              skillName);
    }

    /**
     * Adjusts the loyalty level of a single person based on the event effect.
     *
     * <p>If loyalty modifiers are enabled, the loyalty of a random target is either increased or
     * decreased by the effect's magnitude.</p>
     *
     * @param result The {@link RandomEventResult} detailing the loyalty effect and its magnitude.
     *
     * @return A {@link String} summarizing the loyalty adjustment or an empty string if loyalty modifiers are disabled.
     */
    String eventEffectLoyaltyOne(RandomEventResult result) {
        boolean isUseLoyalty = campaign.getCampaignOptions().isUseLoyaltyModifiers();

        if (!isUseLoyalty) {
            return "";
        }

        final List<RandomEventEffectedPersonnelType> affectedPersonnelTypes = result.affectedPersonnelTypes();
        final int magnitude = result.magnitude();

        Person target = getRandomTarget(affectedPersonnelTypes);

        if (target == null) {
            return "";
        }

        target.changeLoyalty(magnitude);

        MekHQ.triggerEvent(new PersonChangedEvent(target));

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude > 0 ? SINGULAR_CHARACTER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);

        String colorOpen = magnitude > 0 ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0 ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "LOYALTY_ONE.report",
              context,
              colorOpen,
              direction,
              CLOSING_SPAN_TAG,
              magnitude);
    }

    /**
     * Adjusts the loyalty level of all eligible targets in the campaign.
     *
     * <p>If loyalty modifiers are enabled, this effect changes the loyalty of all affected characters, as determined
     * by the event effect's configuration.</p>
     *
     * @param result The {@link RandomEventResult} detailing the loyalty effect and its magnitude.
     *
     * @return A {@link String} summarizing the collective loyalty adjustment or an empty string if loyalty modifiers
     *       are disabled.
     */
    private String eventEffectLoyaltyAll(RandomEventResult result) {
        boolean isUseLoyalty = campaign.getCampaignOptions().isUseLoyaltyModifiers();

        if (!isUseLoyalty) {
            return "";
        }

        final List<RandomEventEffectedPersonnelType> affectedPersonnelTypes = result.affectedPersonnelTypes();
        final int magnitude = result.magnitude();

        Set<Person> targets = getAllPotentialTargets(affectedPersonnelTypes);

        if (targets.isEmpty()) {
            return "";
        }

        for (Person target : targets) {
            target.changeLoyalty(magnitude);

            MekHQ.triggerEvent(new PersonChangedEvent(target));
        }

        boolean isOnlyPrisonersAffected = affectedPersonnelTypes.size() == 1 &&
                                                affectedPersonnelTypes.contains(PRISONERS);

        String context;
        if (isOnlyPrisonersAffected) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targets.size() != 1 ? PLURAL_PRISONER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targets.size() != 1 ? PLURAL_CHARACTER_RESOURCE_KEY : SINGULAR_CHARACTER_RESOURCE_KEY);
        }

        String colorOpen = magnitude > 0 ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0 ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "LOYALTY_ALL.report",
              context,
              colorOpen,
              direction,
              CLOSING_SPAN_TAG,
              magnitude);
    }

    /**
     * Handles the escape effect, reducing the number of prisoners and tracking escapees.
     *
     * <p>The magnitude of the event determines how many prisoners escape. These prisoners
     * are logged for follow-up actions or reporting purposes.</p>
     *
     * @param result The {@link RandomEventResult} detailing the escape effect and its magnitude.
     *
     * @return A {@link String} summarizing the escape effect.
     */
    private String eventEffectEscape(RandomEventResult result) {
        int magnitude = result.magnitude();

        Set<Person> allPotentialTargets = getAllPotentialTargets(result.affectedPersonnelTypes());

        if (allPotentialTargets.isEmpty()) {
            return "";
        }

        magnitude = min(allPotentialTargets.size(), magnitude);

        for (int i = 0; i < magnitude; i++) {
            Person target = getRandomItem(allPotentialTargets);

            personHashSet.add(target);

            campaign.removePerson(target, false);

            allPotentialTargets.remove(target);
        }

        LOGGER.info(personHashSet.toString());

        String colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude != 1 ? PLURAL_PRISONER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, magnitude != 1 ? "pluralizer.have" : "pluralizer.has");

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "ESCAPE.report",
              magnitude,
              context,
              haveOrHas,
              colorOpen,
              CLOSING_SPAN_TAG);
    }

    /**
     * Applies a percentage-based escape effect, determining how many prisoners escape.
     *
     * <p>The number of escapees is calculated as a percentage of the total prisoner count.
     * Escapees are logged for tracking, and a summary report is generated.</p>
     *
     * @param result The {@link RandomEventResult} detailing the escape effect as a percentage.
     *
     * @return A {@link String} summarizing the escape effect.
     */
    private String eventEffectEscapePercent(RandomEventResult result) {
        final double magnitude = (double) result.magnitude() / 100;

        Set<Person> potentialTargets = getAllPotentialTargets(result.affectedPersonnelTypes());

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = (int) max(1, ceil(potentialTargets.size() * magnitude));

        for (int i = 0; i < targetCount; i++) {
            Person target = getRandomItem(potentialTargets);

            personHashSet.add(target);

            campaign.removePerson(target, false);

            potentialTargets.remove(target);
        }

        String colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              targetCount != 1 ? PLURAL_PRISONER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1 ? "pluralizer.have" : "pluralizer.has");

        // We can reuse the same report as the ESCAPE effect, here too
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "ESCAPE.report",
              targetCount,
              context,
              haveOrHas,
              colorOpen,
              CLOSING_SPAN_TAG);
    }

    /**
     * Applies a fatigue effect to a single target as part of the event effect.
     *
     * <p>If fatigue effects are enabled, the fatigue of a random target is adjusted based on the
     * event's magnitude. The outcome is logged in the event report.</p>
     *
     * @param result The {@link RandomEventResult} specifying the fatigue effect and its magnitude.
     *
     * @return A {@link String} summarizing the fatigue effect or an empty string if fatigue is disabled.
     */
    private String eventEffectFatigueOne(RandomEventResult result) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseFatigue = campaignOptions.isUseFatigue();
        int fatigueRate = campaignOptions.getFatigueRate();

        if (!isUseFatigue) {
            return "";
        }

        final List<RandomEventEffectedPersonnelType> affectedPersonnelTypes = result.affectedPersonnelTypes();
        int magnitude = result.magnitude();

        Person target = getRandomTarget(affectedPersonnelTypes);

        if (target == null) {
            return "";
        }

        target.changeFatigue(magnitude * fatigueRate);

        if (campaign.getCampaignOptions().isUseFatigue()) {
            Fatigue.processFatigueActions(campaign, target);

            MekHQ.triggerEvent(new PersonChangedEvent(target));
        }

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude > 0 ? SINGULAR_CHARACTER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);

        String colorOpen = magnitude > 0 ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0 ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "FATIGUE_ONE.report",
              context,
              colorOpen,
              direction,
              CLOSING_SPAN_TAG,
              magnitude);
    }

    /**
     * Applies a fatigue effect to all eligible targets in the campaign.
     *
     * <p>If fatigue effects are enabled, this effect adjusts the fatigue level of all affected characters, based on
     * the event effect's magnitude.</p>
     *
     * @param result The {@link RandomEventResult} specifying the fatigue effect and its magnitude.
     *
     * @return A {@link String} summarizing the collective fatigue effect or an empty string if fatigue is disabled.
     */
    private String eventEffectFatigueAll(RandomEventResult result) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseFatigue = campaignOptions.isUseFatigue();
        int fatigueRate = campaignOptions.getFatigueRate();

        if (!isUseFatigue) {
            return "";
        }

        final List<RandomEventEffectedPersonnelType> affectedPersonnelTypes = result.affectedPersonnelTypes();
        final int magnitude = result.magnitude();

        Set<Person> targets = getAllPotentialTargets(affectedPersonnelTypes);

        if (targets.isEmpty()) {
            return "";
        }

        for (Person target : targets) {
            target.changeFatigue(magnitude * fatigueRate);

            if (campaign.getCampaignOptions().isUseFatigue()) {
                Fatigue.processFatigueActions(campaign, target);

                MekHQ.triggerEvent(new PersonChangedEvent(target));
            }
        }

        boolean isOnlyPrisonersAffected = affectedPersonnelTypes.size() == 1 &&
                                                affectedPersonnelTypes.contains(PRISONERS);

        String context;
        if (isOnlyPrisonersAffected) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targets.size() != 1 ? PLURAL_PRISONER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targets.size() != 1 ? PLURAL_CHARACTER_RESOURCE_KEY : SINGULAR_CHARACTER_RESOURCE_KEY);
        }

        String colorOpen = magnitude > 0 ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0 ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "FATIGUE_ALL.report",
              context,
              colorOpen,
              direction,
              CLOSING_SPAN_TAG,
              magnitude);
    }

    /**
     * Adjusts support points for a strategic campaign operation.
     *
     * <p>If StratCon operations are enabled, this effect changes the support points of a random
     * active contract. The change is logged for reporting purposes.</p>
     *
     * @param result The {@link RandomEventResult} specifying the support point effect and its magnitude.
     *
     * @return A {@link String} summarizing the support point adjustment or an empty string if StratCon is disabled.
     */
    private String eventEffectSupportPoint(RandomEventResult result) {
        if (!campaign.getCampaignOptions().isUseStratCon()) {
            return "";
        }

        final int magnitude = result.magnitude();

        Map<AtBContract, StratConCampaignState> potentialTargets = new HashMap<>();

        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            StratConCampaignState campaignState = contract.getStratConCampaignState();

            if (campaignState != null) {
                potentialTargets.put(contract, campaignState);
            }
        }

        if (potentialTargets.isEmpty()) {
            return "";
        }

        AtBContract target = getRandomItem(potentialTargets.keySet());

        StratConCampaignState targetState = potentialTargets.get(target);
        targetState.changeSupportPoints(magnitude);

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude != 0 ? "SUPPORT_POINT.plural" : "SUPPORT_POINT.singular");

        String colorOpen = magnitude > 0 ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0 ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "SUPPORT_POINT.report",
              context,
              target.getHyperlinkedName(),
              colorOpen,
              direction,
              CLOSING_SPAN_TAG,
              magnitude);
    }

    /**
     * Applies unique effects for specific random events.
     *
     * <p>Unique event effects may perform complex operations depending on the event type, such as
     * changing factions, applying fatigue to personnel, or generating new prisoners.</p>
     *
     * @param eventData The {@link RandomEventData} providing context for the unique operation.
     * @param result    The {@link RandomEventResult} detailing the specific unique effect.
     *
     * @return A {@link String} summarizing the unique effect or an empty string for unsupported events.
     */
    private String eventEffectUnique(RandomEventData eventData, RandomEventResult result) {
        final RandomEventType event = eventData.randomEventType();

        return switch (event) {
            // The OpFor has their morale bumped by one level
            case BARTERING -> eventEffectUniqueBartering();
            // Remove all combat skills from a random Prisoner
            case MISTAKE -> eventEffectUniqueMistake(result);
            // Change the origin faction of one prisoner to match employer
            case UNDERCOVER -> eventEffectUniqueUndercover(result);
            // 'Poison' (xd6 Fatigue) 10% of personnel. x = magnitude
            case POISON -> eventEffectUniquePoison(result);
            // Generate 2d6 new prisoners & xd6 crime. x = magnitude
            case ABANDONED_TO_DIE -> eventEffectUniqueAbandonedToDie(result);
            default -> "";
        };
    }

    /**
     * Applies a morale boost to a random active contract's operation in the campaign.
     *
     * <p>If the morale level of an active contract is below the overwhelming threshold, it is
     * increased by one level. The effect is logged as part of the event report.</p>
     *
     * @return A {@link String} summarizing the outcome of the morale adjustment, or an empty string if no contract
     *       qualifies for the effect.
     */
    private String eventEffectUniqueBartering() {
        List<AtBContract> potentialTargets = new ArrayList<>();

        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            AtBMoraleLevel currentMorale = contract.getMoraleLevel();

            if (!currentMorale.isOverwhelming()) {
                potentialTargets.add(contract);
            }
        }

        if (potentialTargets.isEmpty()) {
            return "";
        }

        AtBContract target = getRandomItem(potentialTargets);

        int moraleOrdinal = target.getMoraleLevel().ordinal();
        target.setMoraleLevel(AtBMoraleLevel.values()[moraleOrdinal + 1]);

        String colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        return getFormattedTextAt(RESOURCE_BUNDLE, "BARTERING.report", colorOpen, CLOSING_SPAN_TAG);
    }

    /**
     * Removes all combat skills from a randomly selected prisoner as part of the event effect.
     *
     * <p>The selected prisoner has all their skills removed, and their role is changed to
     * {@code DEPENDENT}. The outcome is logged as part of the event report.</p>
     *
     * @return A {@link String} summarizing the changes to the prisoner's skills and roles.
     */
    private String eventEffectUniqueMistake(RandomEventResult result) {
        Person target = getRandomTarget(result.affectedPersonnelTypes());

        if (target == null) {
            return "";
        }

        target.removeAllSkills();
        target.setPrimaryRole(campaign, DEPENDENT);
        target.setSecondaryRole(NONE);

        MekHQ.triggerEvent(new PersonChangedEvent(target));

        return getFormattedTextAt(RESOURCE_BUNDLE, "MISTAKE.report", target.getFullTitle());
    }

    /**
     * Changes the origin faction of a random prisoner to match the employer's faction.
     *
     * <p>The prisoner is updated to match the employer's faction, and their "Clan Personnel" status
     * is adjusted accordingly. This event can represent undercover operations or defections.</p>
     *
     * @return A {@link String} summarizing the faction change for the affected prisoner, or an empty string if no
     *       prisoner qualifies.
     */
    private String eventEffectUniqueUndercover(RandomEventResult result) {
        Person targetCharacter = getRandomTarget(result.affectedPersonnelTypes());
        List<AtBContract> potentialContracts = campaign.getActiveAtBContracts();

        if (targetCharacter == null || potentialContracts.isEmpty()) {
            return "";
        }

        AtBContract targetContract = getRandomItem(potentialContracts);

        Faction newFaction = targetContract.getEmployerFaction();
        targetCharacter.setOriginFaction(newFaction);
        targetCharacter.setClanPersonnel(newFaction.isClan());

        MekHQ.triggerEvent(new PersonChangedEvent(targetCharacter));

        // We can reuse the MISTAKE report here, too.
        return getFormattedTextAt(RESOURCE_BUNDLE, "MISTAKE.report", targetCharacter.getFullTitle());
    }

    /**
     * Applies a unique poison effect to a subset of the active personnel in the campaign, adjusting their fatigue
     * levels based on the magnitude of the event result. The effect targets a percentage of personnel determined by a
     * skill value provided in the event result.
     *
     * @param result The event result containing data about the poison effect, including its magnitude and a factor
     *               influencing the percentage of targets affected.
     *
     * @return A formatted string summarizing the poison's effect, including a color-coded representation of severity,
     *       or an empty string if fatigue effects are disabled or there are no valid personnel to target.
     */
    private String eventEffectUniquePoison(RandomEventResult result) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseFatigue = campaignOptions.isUseFatigue();
        int fatigueRate = campaignOptions.getFatigueRate();

        if (!isUseFatigue) {
            return "";
        }

        final int magnitude = result.magnitude();

        Set<Person> potentialTargets = getAllPotentialTargets(result.affectedPersonnelTypes());

        if (potentialTargets.isEmpty()) {
            return "";
        }

        boolean madeChange = false;

        for (Person target : potentialTargets) {
            if (target.getOptions().booleanOption(ATOW_POISON_RESISTANCE)) {
                continue;
            }

            int fatigueChange = d6(magnitude) * fatigueRate;

            target.changeFatigue(fatigueChange);
            madeChange = true;

            if (campaign.getCampaignOptions().isUseFatigue()) {
                Fatigue.processFatigueActions(campaign, target);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(target));
        }

        if (!madeChange) {
            return "";
        }

        String colorOpen = magnitude > 1 ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        return getFormattedTextAt(RESOURCE_BUNDLE, "POISON.report", colorOpen, CLOSING_SPAN_TAG);
    }

    /**
     * Handles an event where abandoned personnel generate new prisoners while increasing the crime rating.
     *
     * <p>The magnitude determines the severity of the crime level increase and the number of new
     * prisoners generated. These prisoners are added to the campaign with random details and assigned the "prisoner"
     * status.</p>
     *
     * @param result The {@link RandomEventResult} specifying the magnitude of the crime and prisoner generation.
     *
     * @return A {@link String} summarizing the increase in crime and the generation of new prisoners.
     */
    private String eventEffectUniqueAbandonedToDie(RandomEventResult result) {
        int magnitude = result.magnitude();

        int crimeChange = 0;
        if (magnitude > 0) {
            crimeChange = d6(magnitude);
        }

        if (crimeChange > 0) {
            campaign.setDateOfLastCrime(campaign.getLocalDate());
            campaign.changeCrimeRating(crimeChange);
        }

        int prisonerCount = d6();

        List<AtBContract> potentialTargets = campaign.getActiveAtBContracts();
        AtBContract targetContract = getRandomItem(potentialTargets);
        Faction targetFaction = targetContract.getEnemy();

        RandomOriginOptions originOptions = campaign.getCampaignOptions().getRandomOriginOptions();

        if (potentialTargets.isEmpty()) {
            return "";
        }

        for (int i = 0; i < prisonerCount; i++) {
            Person newPerson = campaign.newPerson(PersonnelRole.MEKWARRIOR,
                  NONE,
                  new DefaultFactionSelector(originOptions, targetFaction),
                  new DefaultPlanetSelector(originOptions, targetContract.getSystem().getPrimaryPlanet()),
                  Gender.RANDOMIZE);
            campaign.recruitPerson(newPerson, PrisonerStatus.PRISONER, true, false, false);
        }

        String colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              prisonerCount != 1 ? PLURAL_PRISONER_RESOURCE_KEY : SINGULAR_PRISONER_RESOURCE_KEY);

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE,
              prisonerCount != 1 ? "pluralizer.have" : "pluralizer.has");

        String crimeReport = getFormattedTextAt(RESOURCE_BUNDLE,
              "ABANDONED_TO_DIE.report.crime",
              colorOpen,
              CLOSING_SPAN_TAG,
              crimeChange);

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "ABANDONED_TO_DIE.report.prisoners",
              prisonerCount,
              context,
              haveOrHas,
              crimeReport);
    }
}
