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
package mekhq.campaign.randomEvents.prisoners;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.force.ForceType.SECURITY;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_POISON_RESISTANCE;
import static mekhq.campaign.personnel.enums.PersonnelRole.DEPENDENT;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
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
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.randomEvents.prisoners.enums.EventResultEffect;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.randomEvents.prisoners.records.EventResult;
import mekhq.campaign.randomEvents.prisoners.records.PrisonerEventData;
import mekhq.campaign.randomEvents.prisoners.records.PrisonerResponseEntry;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.selectors.factionSelectors.DefaultFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.DefaultPlanetSelector;
import mekhq.utilities.ReportingUtilities;

/**
 * Manages the resolution and effects of prisoner events during a campaign.
 *
 * <p>This class applies the effects of prisoner-related events, such as injuries, capacity
 * changes, escapes, and loyalty adjustments. It processes event details and effects, manages affected campaign state or
 * personnel, and generates a comprehensive report summarizing the outcomes.</p>
 *
 * <p>The effects manager handles a wide variety of event consequences, including unique cases for
 * specific prisoner events. It also tracks and exposes information like escapees for further processing in the
 * campaign.</p>
 */
public class EventEffectsManager {
    private static final MMLogger logger = MMLogger.create(EventEffectsManager.class);

    private final Campaign campaign;

    private String eventReport = "";
    private Set<Person> escapees = new HashSet<>();

    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /**
     * Constructs an {@link EventEffectsManager} object and processes the given event effects.
     *
     * <p>Based on the event data and the player's chosen response, this constructor processes the
     * potential effects of the event, applying their impact to the campaign. These effects are compiled into an event
     * report for further use.</p>
     *
     * @param campaign      The campaign in which the event occurs.
     * @param eventData     The data related to the prisoner event being processed.
     * @param choiceIndex   The index of the user-selected choice for the event.
     * @param wasSuccessful Indicates whether the selected choice was successful.
     */
    public EventEffectsManager(Campaign campaign, PrisonerEventData eventData, int choiceIndex, boolean wasSuccessful) {
        this.campaign = campaign;

        PrisonerResponseEntry responseEntry = eventData.responseEntries().get(choiceIndex);

        List<EventResult> results = wasSuccessful ? responseEntry.effectsSuccess() : responseEntry.effectsFailure();

        StringBuilder report = new StringBuilder();
        for (EventResult result : results) {
            EventResultEffect effect = result.effect();

            String incidentReport = switch (effect) {
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

            if (!incidentReport.isEmpty()) {
                report.append("- ").append(incidentReport).append("<br>");
            }
        }

        eventReport = report.toString();
    }

    /**
     * Retrieves the generated event report summarizing the processed event's effects.
     *
     * <p>The report contains a textual summary of all effects that were applied during the event,
     * structured for easy display to the user or for logs.</p>
     *
     * @return The event report as a {@link String}.
     */
    public String getEventReport() {
        return eventReport;
    }

    /**
     * Retrieves the set of prisoners who escaped as a result of the processed event.
     *
     * <p>The returned set can be used to track and handle additional consequences of the escapes,
     * such as updating campaign statistics or creating follow-up events.</p>
     *
     * @return A {@link Set} of {@link Person} objects representing the prisoners who escaped, or an empty set if no
     *       escapes occurred.
     */
    public Set<Person> getEscapees() {
        return escapees;
    }

    /**
     * Selects a random target for an event effect based on whether the target is a guard or a prisoner.
     *
     * <p>Guards are selected from security forces, while prisoners are selected from the current
     * prisoner list. The selection excludes any invalid targets.</p>
     *
     * @param isGuard {@code true} to select a guard, {@code false} to select a prisoner.
     *
     * @return The randomly selected {@link Person}, or {@code null} if no valid target exists.
     */
    private @Nullable Person getRandomTarget(final boolean isGuard) {
        List<Person> potentialTargets = getAllPotentialTargets(isGuard);

        if (potentialTargets.isEmpty()) {
            return null;
        } else {
            return getRandomItem(potentialTargets);
        }
    }

    /**
     * Retrieves all potential targets for an event effect, either guards or prisoners.
     *
     * @param isGuard {@code true} to retrieve guards, {@code false} to retrieve prisoners.
     *
     * @return A {@link List} of {@link Person} objects representing the potential targets.
     */
    private List<Person> getAllPotentialTargets(final boolean isGuard) {
        List<Person> potentialTargets = new ArrayList<>();
        if (isGuard) {
            for (Force force : campaign.getAllForces()) {
                if (!force.isForceType(SECURITY)) {
                    continue;
                }

                for (UUID unitId : force.getAllUnits(false)) {
                    Unit unit = campaign.getUnit(unitId);

                    if (unit != null) {
                        potentialTargets.addAll(unit.getActiveCrew());
                    }
                }
            }
        } else {
            for (Person prisoner : campaign.getCurrentPrisoners()) {
                // This allows us to injury multiple people in the same event without risking the
                // same person being hit twice.
                if (!prisoner.needsFixing()) {
                    potentialTargets.add(prisoner);
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
     * @param result The {@link EventResult} detailing the effect and its magnitude.
     *
     * @return A {@link String} summarizing the effect of the operation.
     */
    String eventEffectPrisonerCapacity(EventResult result) {
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
     * @param result The {@link EventResult} detailing the injury effect.
     *
     * @return A {@link String} summarizing the injury effect.
     */
    String eventEffectInjury(EventResult result) {
        final boolean isGuard = result.isGuard();
        final int magnitude = result.magnitude();

        Person target = getRandomTarget(isGuard);

        if (target == null) {
            return "";
        }

        // We don't want to accidentally kill anyone. So while someone can get really mauled by
        // this event, they will never actually die.
        int wounds = max(magnitude, 1);

        int priorHits = max(target.getHits(), target.getInjuries().size());

        if (priorHits + wounds > 5) {
            wounds = 5 - priorHits;
        }

        target.setHitsPrior(priorHits);
        target.setHits(priorHits + wounds);

        if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
            target.diagnose(campaign, wounds);
        }

        MekHQ.triggerEvent(new PersonChangedEvent(target));

        String colorOpen = isGuard ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude > 0 ? "context.guard.singular" : "context.prisoner.singular");

        return getFormattedTextAt(RESOURCE_BUNDLE, "INJURY.report", colorOpen, context, CLOSING_SPAN_TAG);
    }

    /**
     * Handles the effects of a "death" event, removing personnel from the campaign due to fatalities.
     *
     * <p>The affected individuals are determined based on the event's magnitude, with guards being
     * marked as KIA and prisoners being removed completely. The outcome is added to the event report.</p>
     *
     * @param result The {@link EventResult} detailing the death effect.
     *
     * @return A {@link String} summarizing the death effect.
     */
    String eventEffectInjuryPercent(EventResult result) {
        final boolean isUseAdvancedMedical = campaign.getCampaignOptions().isUseAdvancedMedical();
        final boolean isGuard = result.isGuard();
        final double magnitude = (double) result.magnitude() / 100;

        List<Person> potentialTargets = getAllPotentialTargets(isGuard);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = (int) max(1, potentialTargets.size() * magnitude);

        for (int i = 0; i < targetCount; i++) {
            Person target = getRandomItem(potentialTargets);

            int wounds = clamp(d6(), 1, 5);

            int priorHits = max(target.getHits(), target.getInjuries().size());

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

        String colorOpen = isGuard ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targetCount != 1 ? "context.guard.plural" : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targetCount != 1 ? "context.prisoner.plural" : "context.prisoner.singular");
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
     * @param result The {@link EventResult} detailing the escape effect and its magnitude.
     *
     * @return A {@link String} summarizing the escape effect.
     */
    String eventEffectDeath(EventResult result) {
        final boolean isGuard = result.isGuard();
        int magnitude = result.magnitude();

        final LocalDate today = campaign.getLocalDate();

        List<Person> potentialTargets = getAllPotentialTargets(isGuard);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        magnitude = min(potentialTargets.size(), magnitude);

        for (int i = 0; i < magnitude; i++) {
            Person target = getRandomItem(potentialTargets);

            if (isGuard) {
                target.changeStatus(campaign, today, PersonnelStatus.KIA);
            } else {
                campaign.removePerson(target, false);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(target));

            potentialTargets.remove(target);
        }

        String colorOpen = isGuard ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  magnitude != 1 ? "context.guard.plural" : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  magnitude != 1 ? "context.prisoner.plural" : "context.prisoner.singular");
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
     * Handles unique effects for specific prisoner events and generates a report.
     *
     * <p>This method processes special cases that require custom logic, such as skill removal,
     * faction changes, or morale adjustments. The exact behavior depends on the event type.</p>
     *
     * @param result The {@link EventResult} describing the unique effect.
     *
     * @return A {@link String} summarizing the unique effect.
     */
    private String eventEffectDeathPercent(EventResult result) {
        final boolean isGuard = result.isGuard();
        final double magnitude = (double) result.magnitude() / 100;

        final LocalDate today = campaign.getLocalDate();

        List<Person> potentialTargets = getAllPotentialTargets(isGuard);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = (int) max(1, potentialTargets.size() * magnitude);

        for (int i = 0; i < targetCount; i++) {
            Person target = getRandomItem(potentialTargets);

            if (isGuard) {
                target.changeStatus(campaign, today, PersonnelStatus.KIA);
            } else {
                campaign.removePerson(target, false);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(target));

            potentialTargets.remove(target);
        }

        String colorOpen = isGuard ?
                                 spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()) :
                                 spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targetCount != 1 ? "context.guard.plural" : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targetCount != 1 ? "context.prisoner.plural" : "context.prisoner.singular");
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
     * Handles the "fatigue all" effect, applying fatigue to all targeted personnel.
     *
     * <p>The magnitude of the event determines the level of fatigue applied. The outcome is
     * added to the event report.</p>
     *
     * @param result The {@link EventResult} specifying the fatigue effect and its magnitude.
     *
     * @return A {@link String} summarizing the fatigue effect.
     */
    private String eventEffectSkill(EventResult result) {
        final boolean isGuard = result.isGuard();
        final int magnitude = result.magnitude();

        // Get skill
        SkillType skillType = SkillType.getType(result.skillType());

        if (skillType == null) {
            return "";
        }

        String skillName = skillType.getName();

        // Get target
        Person target = getRandomTarget(isGuard);

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
              isGuard ? "context.guard.singular" : "context.prisoner.singular");

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
     * @param result The {@link EventResult} detailing the loyalty effect and its magnitude.
     *
     * @return A {@link String} summarizing the loyalty adjustment or an empty string if loyalty modifiers are disabled.
     */
    String eventEffectLoyaltyOne(EventResult result) {
        boolean isUseLoyalty = campaign.getCampaignOptions().isUseLoyaltyModifiers();

        if (!isUseLoyalty) {
            return "";
        }

        final boolean isGuard = result.isGuard();
        final int magnitude = result.magnitude();

        Person target = getRandomTarget(isGuard);

        if (target == null) {
            return "";
        }

        target.changeLoyalty(magnitude);

        MekHQ.triggerEvent(new PersonChangedEvent(target));

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude > 0 ? "context.guard.singular" : "context.prisoner.singular");

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
     * <p>If loyalty modifiers are enabled, this effect changes the loyalty of all guards or
     * prisoners, as determined by the event effect's configuration.</p>
     *
     * @param result The {@link EventResult} detailing the loyalty effect and its magnitude.
     *
     * @return A {@link String} summarizing the collective loyalty adjustment or an empty string if loyalty modifiers
     *       are disabled.
     */
    private String eventEffectLoyaltyAll(EventResult result) {
        boolean isUseLoyalty = campaign.getCampaignOptions().isUseLoyaltyModifiers();

        if (!isUseLoyalty) {
            return "";
        }

        final boolean isGuard = result.isGuard();
        final int magnitude = result.magnitude();

        List<Person> targets = getAllPotentialTargets(isGuard);

        if (targets.isEmpty()) {
            return "";
        }

        for (Person target : targets) {
            target.changeLoyalty(magnitude);

            MekHQ.triggerEvent(new PersonChangedEvent(target));
        }

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targets.size() != 1 ? "context.guard.plural" : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targets.size() != 1 ? "context.prisoner.plural" : "context.prisoner.singular");
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
     * @param result The {@link EventResult} detailing the escape effect and its magnitude.
     *
     * @return A {@link String} summarizing the escape effect.
     */
    private String eventEffectEscape(EventResult result) {
        int magnitude = result.magnitude();

        List<Person> allPotentialTargets = getAllPotentialTargets(false);

        if (allPotentialTargets.isEmpty()) {
            return "";
        }

        magnitude = min(allPotentialTargets.size(), magnitude);

        for (int i = 0; i < magnitude; i++) {
            Person target = getRandomItem(allPotentialTargets);

            escapees.add(target);

            campaign.removePerson(target, false);

            allPotentialTargets.remove(target);
        }

        logger.info(escapees.toString());

        String colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude != 1 ? "context.prisoner.plural" : "context.prisoner.singular");

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
     * @param result The {@link EventResult} detailing the escape effect as a percentage.
     *
     * @return A {@link String} summarizing the escape effect.
     */
    private String eventEffectEscapePercent(EventResult result) {
        final double magnitude = (double) result.magnitude() / 100;

        List<Person> potentialTargets = getAllPotentialTargets(false);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = (int) max(1, ceil(potentialTargets.size() * magnitude));

        for (int i = 0; i < targetCount; i++) {
            Person target = getRandomItem(potentialTargets);

            escapees.add(target);

            campaign.removePerson(target, false);

            potentialTargets.remove(target);
        }

        MMLogger logger = MMLogger.create(EventEffectsManager.class);
        logger.info(escapees.toString());

        String colorOpen = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              targetCount != 1 ? "context.prisoner.plural" : "context.prisoner.singular");

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
     * @param result The {@link EventResult} specifying the fatigue effect and its magnitude.
     *
     * @return A {@link String} summarizing the fatigue effect or an empty string if fatigue is disabled.
     */
    private String eventEffectFatigueOne(EventResult result) {
        boolean isUseFatigue = campaign.getCampaignOptions().isUseFatigue();

        if (!isUseFatigue) {
            return "";
        }

        final boolean isGuard = result.isGuard();
        int magnitude = result.magnitude();

        Person target = getRandomTarget(isGuard);

        if (target == null) {
            return "";
        }

        target.changeFatigue(magnitude);

        if (campaign.getCampaignOptions().isUseFatigue()) {
            Fatigue.processFatigueActions(campaign, target);

            MekHQ.triggerEvent(new PersonChangedEvent(target));
        }

        String context = getFormattedTextAt(RESOURCE_BUNDLE,
              magnitude > 0 ? "context.guard.singular" : "context.prisoner.singular");

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
     * <p>If fatigue effects are enabled, this effect adjusts the fatigue level of all guards or
     * prisoners, based on the event effect's magnitude.</p>
     *
     * @param result The {@link EventResult} specifying the fatigue effect and its magnitude.
     *
     * @return A {@link String} summarizing the collective fatigue effect or an empty string if fatigue is disabled.
     */
    private String eventEffectFatigueAll(EventResult result) {
        boolean isUseFatigue = campaign.getCampaignOptions().isUseFatigue();

        if (!isUseFatigue) {
            return "";
        }

        final boolean isGuard = result.isGuard();
        final int magnitude = result.magnitude();

        List<Person> targets = getAllPotentialTargets(isGuard);

        if (targets.isEmpty()) {
            return "";
        }

        for (Person target : targets) {
            target.changeFatigue(magnitude);

            if (campaign.getCampaignOptions().isUseFatigue()) {
                Fatigue.processFatigueActions(campaign, target);

                MekHQ.triggerEvent(new PersonChangedEvent(target));
            }
        }

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targets.size() != 1 ? "context.guard.plural" : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE,
                  targets.size() != 1 ? "context.prisoner.plural" : "context.prisoner.singular");
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
     * @param result The {@link EventResult} specifying the support point effect and its magnitude.
     *
     * @return A {@link String} summarizing the support point adjustment or an empty string if StratCon is disabled.
     */
    private String eventEffectSupportPoint(EventResult result) {
        if (!campaign.getCampaignOptions().isUseStratCon()) {
            return "";
        }

        final int magnitude = result.magnitude();

        Map<AtBContract, StratconCampaignState> potentialTargets = new HashMap<>();

        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            StratconCampaignState campaignState = contract.getStratconCampaignState();

            if (campaignState != null) {
                potentialTargets.put(contract, campaignState);
            }
        }

        if (potentialTargets.isEmpty()) {
            return "";
        }

        AtBContract target = getRandomItem(potentialTargets.keySet());

        StratconCampaignState targetState = potentialTargets.get(target);
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
     * Applies unique effects for specific prisoner events.
     *
     * <p>Unique event effects may perform complex operations depending on the event type, such as
     * changing factions, applying fatigue to personnel, or generating new prisoners.</p>
     *
     * @param eventData The {@link PrisonerEventData} providing context for the unique operation.
     * @param result    The {@link EventResult} detailing the specific unique effect.
     *
     * @return A {@link String} summarizing the unique effect or an empty string for unsupported events.
     */
    private String eventEffectUnique(PrisonerEventData eventData, EventResult result) {
        final PrisonerEvent event = eventData.prisonerEvent();

        return switch (event) {
            // The OpFor has their morale bumped by one level
            case BARTERING -> eventEffectUniqueBartering();
            // Remove all combat skills from a random Prisoner
            case MISTAKE -> eventEffectUniqueMistake();
            // Change the origin faction of one prisoner to match employer
            case UNDERCOVER -> eventEffectUniqueUndercover();
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
    private String eventEffectUniqueMistake() {
        Person target = getRandomTarget(false);

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
    private String eventEffectUniqueUndercover() {
        Person targetCharacter = getRandomTarget(false);
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
    private String eventEffectUniquePoison(EventResult result) {
        if (!campaign.getCampaignOptions().isUseFatigue()) {
            return "";
        }

        final int magnitude = result.magnitude();

        List<Person> potentialTargets = campaign.getActivePersonnel(false);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = potentialTargets.size();

        for (int i = 0; i < targetCount; i++) {
            Person target = getRandomItem(potentialTargets);

            int fatigueChange = d6(magnitude);

            if (target.getOptions().booleanOption(ATOW_POISON_RESISTANCE)) {
                continue;
            }

            target.changeFatigue(fatigueChange);

            if (campaign.getCampaignOptions().isUseFatigue()) {
                Fatigue.processFatigueActions(campaign, target);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(target));

            potentialTargets.remove(target);
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
     * @param result The {@link EventResult} specifying the magnitude of the crime and prisoner generation.
     *
     * @return A {@link String} summarizing the increase in crime and the generation of new prisoners.
     */
    private String eventEffectUniqueAbandonedToDie(EventResult result) {
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
              prisonerCount != 1 ? "context.prisoner.plural" : "context.prisoner.singular");

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
