package mekhq.campaign.personnel.prisoners;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceType;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.prisoners.enums.EventResultEffect;
import mekhq.campaign.personnel.prisoners.enums.PrisonerEvent;
import mekhq.campaign.personnel.prisoners.enums.PrisonerStatus;
import mekhq.campaign.personnel.prisoners.records.EventResult;
import mekhq.campaign.personnel.prisoners.records.PrisonerEventData;
import mekhq.campaign.personnel.prisoners.records.PrisonerResponseEntry;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.selectors.factionSelectors.DefaultFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.DefaultPlanetSelector;

import java.time.LocalDate;
import java.util.*;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.Compute.d6;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

/**
 * Manages the resolution and effects of prisoner events.
 *
 * <p>This class is responsible for applying effects based on prisoner events. It processes the
 * event details, applies changes like injuries, loyalty modifications, and other event-related
 * effects, and generates a report summarizing the results.</p>
 */
public class EventEffectsManager {
    private final Campaign campaign;

    private String eventReport = "";

    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEventDialog";

    /**
     * Constructs an {@code EventEffectsManager} object and processes the given event effects.
     *
     * @param campaign the campaign in which the event occurs
     * @param eventData the data related to the prisoner event being processed
     * @param choiceIndex the index of the user-selected choice for the event
     * @param wasSuccessful whether the event's selected choice was successful
     */
    public EventEffectsManager(Campaign campaign, PrisonerEventData eventData, int choiceIndex,
                               boolean wasSuccessful) {
        this.campaign = campaign;

        PrisonerResponseEntry responseEntry = eventData.responseEntries().get(choiceIndex);

        List<EventResult> results = wasSuccessful
            ? responseEntry.effectsSuccess()
            : responseEntry.effectsFailure();

        StringBuilder report = new StringBuilder();
        for (EventResult result : results) {
            EventResultEffect effect = result.effect();

            switch (effect) {
                case NONE -> {}
                case PRISONER_CAPACITY -> report.append(eventEffectPrisonerCapacity(result));
                case INJURY -> report.append(eventEffectInjury(result));
                case INJURY_PERCENT -> report.append(eventEffectInjuryPercent(result));
                case DEATH -> report.append(eventEffectDeath(result));
                case DEATH_PERCENT -> report.append(eventEffectDeathPercent(result));
                case SKILL -> report.append(eventEffectSkill(result));
                case LOYALTY_ONE -> report.append(eventEffectLoyaltyOne(result));
                case LOYALTY_ALL -> report.append(eventEffectLoyaltyAll(result));
                case ESCAPE -> report.append(eventEffectEscape(result));
                case ESCAPE_PERCENT -> report.append(eventEffectEscapePercent(result));
                case FATIGUE_ONE -> report.append(eventEffectFatigueOne(result));
                case FATIGUE_ALL -> report.append(eventEffectFatigueAll(result));
                case SUPPORT_POINT -> report.append(eventEffectSupportPoint(result));
                case UNIQUE -> report.append(eventEffectUnique(eventData, result));
            }

            report.append(' ');
        }

        eventReport = report.toString();
    }

    /**
     * Gets the event report summarizing the effects of the processed event.
     *
     * @return the event report as a {@link String}
     */
    public String getEventReport() {
        return eventReport;
    }

    /**
     * Selects a random target from the campaign's personnel based on whether the
     * target is a guard or prisoner.
     *
     * @param isGuard whether to target a guard ({@code true}) or prisoner ({@code false})
     * @return the randomly selected {@link Person}, or {@code null} if no valid target exists
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
     * Retrieves all potential targets for an event effect based on their role (guards or prisoners).
     *
     * @param isGuard whether to retrieve guards ({@code true}) or prisoners ({@code false})
     * @return a {@link List} of {@link Person} objects representing potential targets
     */
    private List<Person> getAllPotentialTargets(final boolean isGuard) {
        List<Person> potentialTargets = new ArrayList<>();
        if (isGuard) {
            for (Force force : campaign.getAllForces()) {
                ForceType forceType = force.getForceType();

                if (forceType.isSecurity()) {
                    for (UUID unitId : force.getAllUnits(false)) {
                        Unit unit = campaign.getUnit(unitId);

                        if (unit != null) {
                            potentialTargets.addAll(unit.getActiveCrew());
                        }
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
     * Applies an event effect to adjust the temporary prisoner capacity in the campaign and
     * generates a report.
     *
     * @param result the {@link EventResult} detailing the effect
     * @return a {@link String} summarizing the effect of the operation
     */
    private String eventEffectPrisonerCapacity(EventResult result) {
        final int magnitude = result.magnitude();
        int currentTemporarilyPrisonerCapacity = campaign.getTemporaryPrisonerCapacity();

        campaign.setTemporaryPrisonerCapacity(currentTemporarilyPrisonerCapacity + magnitude);

        String colorOpen = magnitude > 0
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0
            ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE, "PRISONER_CAPACITY.report",
            colorOpen, direction, CLOSING_SPAN_TAG, magnitude);
    }

    /**
     * Handles the effects of an "injury" event, applying injuries to a random target and
     * generating a report.
     *
     * @param result the {@link EventResult} detailing the injury effect
     * @return a {@link String} summarizing the injury effect
     */
    private String eventEffectInjury(EventResult result) {
        final boolean isGuard = result.isGuard();
        final int magnitude = result.magnitude();

        Person target = getRandomTarget(isGuard);

        if (target == null) {
            return "";
        }

        int wounds = clamp(magnitude, 1, 5);

        int priorHits = max(target.getHits(), target.getInjuries().size());

        if (priorHits + wounds > 5) {
            wounds = 5 - priorHits;
        }

        target.setHitsPrior(priorHits);
        target.setHits(priorHits + wounds);

        if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
            target.diagnose(campaign, wounds);
        }

        String colorOpen = isGuard
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0
            ? "context.guard.singular"
            : "context.prisoner.singular");

        return getFormattedTextAt(RESOURCE_BUNDLE, "INJURY.report",
            colorOpen, context, CLOSING_SPAN_TAG);
    }

    /**
     * Handles the effects of an "injury percent" event, which applies injuries to a percentage of
     * selected personnel and generates a report.
     *
     * @param result the {@link EventResult} detailing the injury percentage effect
     * @return a {@link String} summarizing the injury percentage effect
     */
    private String eventEffectInjuryPercent(EventResult result) {
        final boolean isUseAdvancedMedical = campaign.getCampaignOptions().isUseAdvancedMedical();
        final boolean isGuard = result.isGuard();
        final double magnitude = (double) result.magnitude() / 100;

        List<Person> potentialTargets = getAllPotentialTargets(isGuard);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = (int) max(1, ceil(potentialTargets.size() * magnitude));

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

            potentialTargets.remove(target);
        }

        String colorOpen = isGuard
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1
                ? "context.guard.plural"
                : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1
                ? "context.prisoner.plural"
                : "context.prisoner.singular");
        }

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1
            ? "pluralizer.have"
            : "pluralizer.has");

        return getFormattedTextAt(RESOURCE_BUNDLE, "INJURY_PERCENT.report",
            targetCount, colorOpen, context, CLOSING_SPAN_TAG, haveOrHas);
    }

    /**
     * Handles the effects of a "death" event, removing personnel from the campaign, or changing
     * their status based on the event magnitude and generating a report.
     *
     * @param result the {@link EventResult} detailing the death effect
     * @return a {@link String} summarizing the death effect
     */
    private String eventEffectDeath(EventResult result) {
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

            potentialTargets.remove(target);
        }

        String colorOpen = isGuard
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE, magnitude != 1
                ? "context.guard.plural"
                : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE, magnitude != 1
                ? "context.prisoner.plural"
                : "context.prisoner.singular");
        }

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, magnitude != 1
            ? "pluralizer.have"
            : "pluralizer.has");

        String bodyOrBodies = getFormattedTextAt(RESOURCE_BUNDLE, magnitude != 1
            ? "DEATH.body.plural"
            : "DEATH.body.singular");

        return getFormattedTextAt(RESOURCE_BUNDLE, "DEATH.report",
            magnitude, colorOpen, context, CLOSING_SPAN_TAG, haveOrHas, bodyOrBodies);
    }

    /**
     * Handles the effects of a "death percent" event, applying death to a percentage of selected
     * personnel and generating a report.
     *
     * @param result the {@link EventResult} detailing the death percentage effect
     * @return a {@link String} summarizing the death percentage effect
     */
    private String eventEffectDeathPercent(EventResult result) {
        final boolean isGuard = result.isGuard();
        final double magnitude = (double) result.magnitude() / 100;

        final LocalDate today = campaign.getLocalDate();

        List<Person> potentialTargets = getAllPotentialTargets(isGuard);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = (int) max(1, ceil(potentialTargets.size() * magnitude));

        for (int i = 0; i < targetCount; i++) {
            Person target = getRandomItem(potentialTargets);

            if (isGuard) {
                target.changeStatus(campaign, today, PersonnelStatus.KIA);
            } else {
                campaign.removePerson(target, false);
            }

            potentialTargets.remove(target);
        }

        String colorOpen = isGuard
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1
                ? "context.guard.plural"
                : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1
                ? "context.prisoner.plural"
                : "context.prisoner.singular");
        }

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1
            ? "pluralizer.have"
            : "pluralizer.has");

        String bodyOrBodies = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1
            ? "DEATH.body.plural"
            : "DEATH.body.singular");

        // We can reuse the same report as the DEATH effect, here too
        return getFormattedTextAt(RESOURCE_BUNDLE, "DEATH.report",
            targetCount, colorOpen, context, CLOSING_SPAN_TAG, haveOrHas, bodyOrBodies);
    }

    /**
     * Handles the "loyalty one" effect, altering the loyalty of a single target
     * and generating a report.
     *
     * @param result the {@link EventResult} detailing the loyalty effect
     * @return a {@link String} summarizing the loyalty change for one target
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

        String colorOpen = spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE, isGuard
            ? "context.guard.singular"
            : "context.prisoner.singular");

        // We can reuse the same report as the DEATH effect, here too
        return getFormattedTextAt(RESOURCE_BUNDLE, "SKILL.report",
            colorOpen, context, CLOSING_SPAN_TAG, magnitude, skillName);
    }

    /**
     * Handles the "loyalty all" effect, altering the loyalty of all potential targets
     * and generating a report.
     *
     * @param result the {@link EventResult} detailing the loyalty effect
     * @return a {@link String} summarizing the loyalty change for all targeted personnel
     */
    private String eventEffectLoyaltyOne(EventResult result) {
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

        String context = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0
            ? "context.guard.singular"
            : "context.prisoner.singular");

        String colorOpen = magnitude > 0
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0
            ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE, "LOYALTY_ONE.report",
            context, colorOpen, direction, CLOSING_SPAN_TAG, magnitude);
    }


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
        }

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE, targets.size() != 1
                ? "context.guard.plural"
                : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE, targets.size() != 1
                ? "context.prisoner.plural"
                : "context.prisoner.singular");
        }

        String colorOpen = magnitude > 0
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0
            ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE, "LOYALTY_ALL.report",
            context, colorOpen, direction, CLOSING_SPAN_TAG, magnitude);
    }

    /**
     * Handles the effects of a prisoner escape event, reducing the number of prisoners
     * and generating a report.
     *
     * @param result the {@link EventResult} detailing the escape effect
     * @return a {@link String} summarizing the prisoner escape
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
            campaign.removePerson(target, false);
        }

        String colorOpen = spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE, magnitude != 1 ?
            "context.prisoner.plural" : "context.prisoner.singular");

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, magnitude != 1
            ? "pluralizer.have"
            : "pluralizer.has");

        return getFormattedTextAt(RESOURCE_BUNDLE, "ESCAPE.report",
            magnitude, context, haveOrHas, colorOpen, CLOSING_SPAN_TAG);
    }

    /**
     * Handles the "escape percent" effect, determining the percentage of escaping prisoners
     * based on magnitude and generating a report.
     *
     * @param result the {@link EventResult} detailing the percentage of prisoners escaping
     * @return a {@link String} summarizing the escape percentage effect
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

            campaign.removePerson(target, false);

            potentialTargets.remove(target);
        }

        String colorOpen = spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1 ?
            "context.prisoner.plural" : "context.prisoner.singular");

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, targetCount != 1
            ? "pluralizer.have"
            : "pluralizer.has");

        // We can reuse the same report as the ESCAPE effect, here too
        return getFormattedTextAt(RESOURCE_BUNDLE, "ESCAPE.report",
            targetCount, context, haveOrHas, colorOpen, CLOSING_SPAN_TAG);
    }


    /**
     * Handles the "fatigue one" effect, which modifies the fatigue level of one target and
     * generates a report.
     *
     * @param result the {@link EventResult} detailing the fatigue effect
     * @return a {@link String} summarizing the fatigue effect for one target
     */
    private String eventEffectFatigueOne(EventResult result) {
        boolean isUseFatigue = campaign.getCampaignOptions().isUseFatigue();

        if (!isUseFatigue) {
            return "";
        }

        final boolean isGuard = result.isGuard();
        final int magnitude = result.magnitude();

        Person target = getRandomTarget(isGuard);

        if (target == null) {
            return "";
        }

        target.changeFatigue(magnitude);

        String context = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0
            ? "context.guard.singular"
            : "context.prisoner.singular");

        String colorOpen = magnitude > 0
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0
            ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE, "FATIGUE_ONE.report",
            context, colorOpen, direction, CLOSING_SPAN_TAG, magnitude);
    }

    /**
     * Handles the "fatigue all" effect, which applies fatigue to all relevant personnel
     * and generates a report.
     *
     * @param result the {@link EventResult} detailing the fatigue effect
     * @return a {@link String} summarizing the fatigue effect for all targets
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
        }

        String context;
        if (isGuard) {
            context = getFormattedTextAt(RESOURCE_BUNDLE, targets.size() != 1
                ? "context.guard.plural"
                : "context.guard.singular");
        } else {
            context = getFormattedTextAt(RESOURCE_BUNDLE, targets.size() != 1
                ? "context.prisoner.plural"
                : "context.prisoner.singular");
        }

        String colorOpen = magnitude > 0
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0
            ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE, "FATIGUE_ALL.report",
            context, colorOpen, direction, CLOSING_SPAN_TAG, magnitude);
    }

    /**
     * Handles the "support point" effect, which adjusts support points within StratCon
     * campaign states and generates a report.
     *
     * @param result the {@link EventResult} detailing the support point effect
     * @return a {@link String} summarizing the support point adjustment
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

        String context = getFormattedTextAt(RESOURCE_BUNDLE, magnitude != 0
            ? "SUPPORT_POINT.plural"
            : "SUPPORT_POINT.singular");

        String colorOpen = magnitude > 0
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        String direction = getFormattedTextAt(RESOURCE_BUNDLE, magnitude > 0
            ? "change.increased" : "change.decreased");

        return getFormattedTextAt(RESOURCE_BUNDLE, "SUPPORT_POINT.report",
            context, target.getName(), colorOpen, direction, CLOSING_SPAN_TAG, magnitude);
    }

    /**
     * Handles unique effects for specific events and generates a report based on the
     * implemented unique event behavior.
     *
     * @param eventData the {@link PrisonerEventData} containing event details
     * @param result the {@link EventResult} detailing the unique effect
     * @return a {@link String} summarizing the unique event effect
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
     * Handles the "bartering" effect where the OpFor gains morale and generates a report.
     *
     * @return a {@link String} summarizing the morale increase effect for the OpFor
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

        String colorOpen = spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor());

        return getFormattedTextAt(RESOURCE_BUNDLE, "BARTERING.report",
            colorOpen, CLOSING_SPAN_TAG);
    }

    /**
     * Handles the "mistake" effect, removing all combat skills from a random prisoner and
     * generating a report.
     *
     * @return a {@link String} summarizing the skills removal effect
     */
    private String eventEffectUniqueMistake() {
        Person target = getRandomTarget(false);

        if (target == null) {
            return "";
        }

        target.removeAllSkills();
        target.setPrimaryRole(campaign, PersonnelRole.DEPENDENT);
        target.setSecondaryRole(PersonnelRole.DEPENDENT);

        return getFormattedTextAt(RESOURCE_BUNDLE, "MISTAKE.report",
            target.getFullTitle());
    }

    /**
     * Handles the "undercover" effect, assigning one prisoner to the employer faction and
     * generating a report.
     *
     * @return a {@link String} summarizing the undercover effect
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

        // We can reuse the MISTAKE report here, too.
        return getFormattedTextAt(RESOURCE_BUNDLE, "MISTAKE.report",
            targetCharacter.getFullTitle());
    }

    /**
     * Handles the "poison" effect, applying fatigue to 10% of personnel and generating a report.
     *
     * @param result the {@link EventResult} detailing the poison's fatigue effect
     * @return a {@link String} summarizing the poison effect
     */
    private String eventEffectUniquePoison(EventResult result) {
        if (!campaign.getCampaignOptions().isUseFatigue()) {
            return "";
        }

        final int magnitude = result.magnitude();

        List<Person> potentialTargets = campaign.getActivePersonnel(true);

        if (potentialTargets.isEmpty()) {
            return "";
        }

        int targetCount = (int) max(1, ceil(potentialTargets.size() * 0.1));

        for (int i = 0; i < targetCount; i++) {
            Person target = getRandomItem(potentialTargets);

            int fatigueChange = d6(magnitude);
            target.changeFatigue(fatigueChange);

            potentialTargets.remove(target);
        }

        String colorOpen = magnitude > 1
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor());

        return getFormattedTextAt(RESOURCE_BUNDLE, "POISON.report",
            colorOpen, CLOSING_SPAN_TAG);
    }

    /**
     * Handles the "abandoned to die" effect, recruiting new prisoners, altering crime levels,
     * and generating a report.
     *
     * @param result the {@link EventResult} detailing the abandoned-to-die effect
     * @return a {@link String} summarizing the effect
     */
    private String eventEffectUniqueAbandonedToDie(EventResult result) {
        int magnitude = result.magnitude();
        int crimeChange = d6(magnitude);

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
                PersonnelRole.NONE,
                new DefaultFactionSelector(originOptions, targetFaction),
                new DefaultPlanetSelector(originOptions, targetContract.getSystem().getPrimaryPlanet()),
                Gender.RANDOMIZE);
            campaign.recruitPerson(newPerson, PrisonerStatus.PRISONER, true, false);
        }

        String colorOpen = spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor());

        String context = getFormattedTextAt(RESOURCE_BUNDLE, prisonerCount != 1
            ? "context.prisoner.plural"
            : "context.prisoner.singular");

        String haveOrHas = getFormattedTextAt(RESOURCE_BUNDLE, prisonerCount != 1
            ? "pluralizer.have"
            : "pluralizer.has");

        String crimeReport = getFormattedTextAt(RESOURCE_BUNDLE, "ABANDONED_TO_DIE.report.crime",
            colorOpen, CLOSING_SPAN_TAG, crimeChange);

        return getFormattedTextAt(RESOURCE_BUNDLE, "ABANDONED_TO_DIE.report.prisoners",
            prisonerCount, context, haveOrHas, crimeReport);
    }
}
