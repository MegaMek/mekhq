/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.randomEvents.prisoners;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent;
import mekhq.campaign.randomEvents.prisoners.enums.ResponseQuality;
import mekhq.campaign.randomEvents.prisoners.records.PrisonerEventData;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.randomEvents.prisonerDialogs.PrisonerEventDialog;
import mekhq.gui.dialog.randomEvents.prisonerDialogs.PrisonerEventResultsDialog;
import mekhq.gui.dialog.randomEvents.prisonerDialogs.PrisonerWarningDialog;
import mekhq.gui.dialog.randomEvents.prisonerDialogs.PrisonerWarningResultsDialog;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR;
import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.campaign.force.ForceType.SECURITY;
import static mekhq.campaign.rating.CamOpsReputation.CommandRating.getPersonalityValue;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

/**
 * Manages prisoner-related events and warnings during a campaign.
 *
 * <p>Handles both weekly and monthly events associated with prisoners in the campaign, including
 * ransom opportunities, warnings for prisoner overflow, and random events that affect the campaign
 * state. It also calculates prisoner capacity, processes executions, and dynamically generates
 * prisoner-related scenarios based on campaign conditions.</p>
 *
 * <p>The manager adjusts campaign parameters such as temporary prisoner capacity and handles
 * interactions with the player via dialogs, providing options to resolve prisoner-related issues.</p>
 */
public class PrisonerEventManager {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    private final Campaign campaign;
    private final Person speaker;

    // CamOps states that executing prisoners incurs a -50 reputation penalty.
    // However, that lacks nuance, so we've changed it to -1 per prisoner to a maximum of -50.
    public static final int MAX_CRIME_PENALTY = 50;
    static final int RANSOM_EVENT_CHANCE = 10;
    private final int MINIMUM_PRISONER_COUNT = 25;
    private final int RESPONSE_TARGET_NUMBER = 7;

    public static final int DEFAULT_TEMPORARY_CAPACITY = 100;
    public static final double TEMPORARY_CAPACITY_DEGRADE_RATE = 0.1;

    // These values are based on CamOps. CamOps states a squad of CI or one squad of BA can
    // handle 100 prisoners. As there are usually around 21 soldiers in a CI platoon and 5 BA
    // in a Clan BA Squad, we extrapolated from there to more easily handle different platoon and
    // squad sizes. However, the idea that a single squad of 7 soldiers can manage 100 prisoners is
    // a stretch, so we suggested errata-ing it to 100 per platoon, and these numbers reflect that.
    public static final int PRISONER_CAPACITY_CONVENTIONAL_INFANTRY = 5;
    public static final int PRISONER_CAPACITY_BATTLE_ARMOR = 20;
    public static final int PRISONER_CAPACITY_OTHER = 2;
    public static final int PRISONER_CAPACITY_CAM_OPS_MULTIPLIER = 3;

    // Fixed Dialog Options
    private final int CHOICE_FREE = 1;
    private final int CHOICE_EXECUTE = 2;

    /**
     * Constructs a new {@link PrisonerEventManager} and handles the initialization of
     * prisoner-related events.
     *
     * <p>Performs the following during initialization:</p>
     * <ul>
     *     <li>Adjusts temporary prisoner capacity for the campaign.</li>
     *     <li>Triggers monthly events such as ransom opportunities if applicable.</li>
     *     <li>Handles weekly events related to prisoner overflow or random events.</li>
     * </ul>
     *
     * @param campaign The current campaign instance, providing context and state for prisoner
     *                management.
     */
    public PrisonerEventManager(Campaign campaign) {
        this.campaign = campaign;
        this.speaker = getSpeaker();

        LocalDate today = campaign.getLocalDate();
        boolean isFirstOfMonth = today.getDayOfMonth() == 1;
        boolean isMonday = today.getDayOfWeek() == DayOfWeek.MONDAY;
        boolean isFortnight = isMonday && (today.get(WEEK_OF_WEEK_BASED_YEAR) % 2 == 0);

        // we have this here, as we still want Temporary Capacity to degrade even if the MeKHQ
        // capture style isn't being used.
        if (isFirstOfMonth) {
            degradeTemporaryCapacity();
        }

        if (campaign.getCurrentPrisoners().isEmpty()) {
            return;
        }

        if (!campaign.getCampaignOptions().getPrisonerCaptureStyle().isMekHQ()) {
            return;
        }

        if (!campaign.hasActiveAtBContract(false)) {
            return;
        }

        // Monthly events
        if (isFirstOfMonth) {
            checkForRansomEvents();
        }

        // Fortnightly events
        if (isMonday && isFortnight) {
            checkForPrisonerEvents(false);
        }
    }

    /**
     * Adjusts the temporary prisoner capacity for the given campaign by degrading it.
     *
     * <p>This method modifies the campaign's temporary prisoner capacity based on a percentage of
     * the current value. It ensures that the capacity moves closer to a default value, either
     * increasing or decreasing depending on the current capacity modifier's position relative to
     * the default.</p>
     *
     * @return The updated temporary capacity modifier after the adjustment has been applied.
     */
    int degradeTemporaryCapacity() {
        int temporaryCapacityModifier = campaign.getTemporaryPrisonerCapacity();
        int newCapacity = 0;

        if (temporaryCapacityModifier != DEFAULT_TEMPORARY_CAPACITY) {
            int degreeOfChange = (int) round(temporaryCapacityModifier * TEMPORARY_CAPACITY_DEGRADE_RATE);

            if (temporaryCapacityModifier < DEFAULT_TEMPORARY_CAPACITY) {
                temporaryCapacityModifier += degreeOfChange;
                newCapacity = min(DEFAULT_TEMPORARY_CAPACITY, temporaryCapacityModifier);

                campaign.setTemporaryPrisonerCapacity(newCapacity);
            } else {
                temporaryCapacityModifier -= degreeOfChange;
                newCapacity = max(DEFAULT_TEMPORARY_CAPACITY, temporaryCapacityModifier);

                campaign.setTemporaryPrisonerCapacity(newCapacity);
            }
        }

        return newCapacity;
    }

    /**
     * Checks for ransom-related events in the given campaign. This method determines if a ransom
     * event is triggered and whether friendly prisoners of war (POWs) are involved.
     *
     * @return A list of two boolean values where the first element indicates if an event
     *         was triggered, and the second element specifies if the event involves friendly POWs.
     */
    List<Boolean> checkForRansomEvents() {
        boolean eventTriggered = false;
        boolean isFriendlyPOWs = false;

        // Check for ransom events
        if (campaign.hasActiveContract()) {
            int roll = d6(2);
            if (roll >= RANSOM_EVENT_CHANCE) {
                if (!campaign.getFriendlyPrisoners().isEmpty()) {
                    // We use randomInt here as it allows us better control over the return values
                    // when testing.
                    isFriendlyPOWs = randomInt(6) == 1;
                }

                eventTriggered = true;
                new PrisonerRansomEvent(campaign, isFriendlyPOWs);
            }
        }

        return List.of(eventTriggered, isFriendlyPOWs);
    }

    /**
     * Checks for events related to prisoner overflow in the campaign.
     *
     * <p>This method determines whether a minor or major prisoner-related event occurs based on
     * the current prisoner capacity, usage, and overflow. If there is no event, a warning may be
     * processed.
     *
     * @param isHeadless A boolean value indicating whether the process is running without a user
     *                  interface. Allows Unit Tests to bypass the GUI prompts created by this method.
     * @return A list of two boolean values: The first element indicates whether a minor event
     * occurred. The second element indicates whether a major event occurred.
     */
    List<Boolean> checkForPrisonerEvents(boolean isHeadless) {
        int totalPrisoners = campaign.getCurrentPrisoners().size();
        int prisonerCapacityUsage = calculatePrisonerCapacityUsage(campaign);
        int prisonerCapacity = calculatePrisonerCapacity(campaign);

        int overflow = prisonerCapacityUsage - prisonerCapacity;

        if (overflow <= 0 && totalPrisoners < MINIMUM_PRISONER_COUNT) {
            // No risk of event
            return List.of(false, false);
        }

        int eventRoll = randomInt(100);
        boolean minorEvent = eventRoll < overflow;

        if (eventRoll == 0) {
            minorEvent = true;
        }

        // Does the minor event escalate into a major event?
        eventRoll = randomInt(100);
        boolean majorEvent = minorEvent
            && (totalPrisoners > MINIMUM_PRISONER_COUNT)
            && (eventRoll < overflow || eventRoll == 0);

        // If there is no event, throw up a warning and give the player an opportunity to do
        // something about the situation.
        if (!minorEvent && overflow > 0) {
            if (!isHeadless) {
                processWarning(overflow);
            }
            return List.of(false, false);
        }

        // Random Event
        if (!isHeadless) {
            processRandomEvent(majorEvent);
        }
        return List.of(true, majorEvent);
    }

    /**
     * Processes a random event involving prisoners.
     *
     * <p>Handles both minor and major prisoner events. A dialog is presented to the player,
     * allowing them to decide how to respond. Based on the outcome, the event's effects are
     * applied, which may include generating escapee scenarios or other consequences.</p>
     *
     * @param majorEvent {@code true} if the event is classified as a major event, {@code false}
     *                              for a minor event.
     */
    private void processRandomEvent(boolean majorEvent) {
        PrisonerEventData eventData;
        if (majorEvent) {
            eventData = pickEvent(true);
        } else {
            eventData = pickEvent(false);
        }
        PrisonerEvent event = eventData.prisonerEvent();

        PrisonerEventDialog eventDialog =
            new PrisonerEventDialog(campaign, speaker, event);

        int choiceIndex = eventDialog.getDialogChoice();

        boolean isSuccessful = makeEventCheck(eventData, choiceIndex);

        EventEffectsManager effectsManager = new EventEffectsManager(campaign, eventData, choiceIndex, isSuccessful);
        String eventReport = effectsManager.getEventReport();

        new PrisonerEventResultsDialog(campaign, speaker, event, choiceIndex, isSuccessful, eventReport);

        Set<Person> escapees = effectsManager.getEscapees();

        if (!escapees.isEmpty() && campaign.hasActiveAtBContract(false)) {
            if (randomInt(100) < escapees.size()) {
                List<AtBContract> contracts = campaign.getActiveAtBContracts();
                Collections.shuffle(contracts);

                new PrisonEscapeScenario(campaign, contracts.get(0), escapees);
            }
        }
    }

    /**
     * Processes a warning event when the prisoner overflow exceeds acceptable limits.
     *
     * <p>Presents a dialog to the player, allowing them to take corrective actions by choosing to
     * either release or execute prisoners to address the overflow. Results in the removal or
     * execution of prisoners based on the player's choice.</p>
     *
     * @param overflow The calculated overflow value indicating prisoners exceeding capacity.
     */
    private void processWarning(int overflow) {
        List<Person> prisoners = campaign.getCurrentPrisoners();
        Collections.shuffle(prisoners);

        int setFree = max(1, (int) round(overflow * 1.1));
        setFree = min(setFree, prisoners.size());
        int executions = max(1, (int) round(prisoners.size() * 0.1));
        executions = min(executions, prisoners.size());

        PrisonerWarningDialog warningDialog = new PrisonerWarningDialog(campaign, speaker, executions, setFree);
        int choice = warningDialog.getDialogChoice();

        if (choice == CHOICE_FREE) {
            for (int i = 0; i < setFree; i++) {
                Person prisoner = prisoners.get(i);
                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "free.report", prisoner.getFullName()));
                campaign.removePerson(prisoner, false);
            }

            new PrisonerWarningResultsDialog(campaign, speaker, false);
            return;
        }

        if (choice == CHOICE_EXECUTE) {
            processExecutions(executions, prisoners);
            new PrisonerWarningResultsDialog(campaign, speaker, true);
        }
    }

    /**
     * Selects a random event from the available prisoner events.
     *
     * @param isMajor {@code true} to select a major event, {@code false} to select a minor event.
     * @return A randomly selected {@link PrisonerEventData} object representing the event.
     */
    private PrisonerEventData pickEvent(boolean isMajor) {
        List<PrisonerEventData> allMajorEvents = campaign.getRandomEventLibraries().getPrisonerEvents(isMajor);
        Collections.shuffle(allMajorEvents);
        return ObjectUtility.getRandomItem(allMajorEvents);
    }

    /**
     * Performs a check to determine if the player's response to an event is successful.
     *
     * <p>The success of the check depends on the attributes of the event, the chosen response
     * option, and modifiers such as the speaker's personality.</p>
     *
     * @param eventData   The data for the prisoner event being processed.
     * @param choiceIndex The index of the choice made by the player in the event dialog.
     * @return {@code true} if the player's response is deemed successful, {@code false} otherwise.
     */
    private boolean makeEventCheck(PrisonerEventData eventData, int choiceIndex) {
        int responseModifier = 0;
        if (speaker != null) {
            responseModifier = getPersonalityValue(campaign, speaker);
        }

        if (speaker == null) {
            responseModifier = -12; // this deliberately renders the check impossible
        }

        ResponseQuality responseQuality = eventData.responseEntries().get(choiceIndex).quality();
        switch (responseQuality) {
            case RESPONSE_NEUTRAL -> {} // No modifier
            case RESPONSE_POSITIVE -> responseModifier += 3;
            case RESPONSE_NEGATIVE -> responseModifier -= 3;
        }

        int responseCheck = d6(2) + responseModifier;

        return responseCheck >= RESPONSE_TARGET_NUMBER;
    }

    /**
     * Processes the execution of a given number of prisoners.
     *
     * <p>Removes prisoners from the campaign while generating appropriate reports of their
     * execution. Triggers additional logic to handle campaign state updates, such as potential
     * backfires or penalties from the executions.</p>
     *
     * @param executions The number of prisoners to be executed.
     * @param prisoners  The list of prisoners involved in the execution.
     */
    private void processExecutions(int executions, List<Person> prisoners) {
        for (int i = 0; i < executions; i++) {
            Person prisoner = prisoners.get(i);
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "execute.report",
                    prisoner.getFullName()));
            campaign.removePerson(prisoner, false);
        }

        processAdHocExecution(campaign, executions);
    }

    /**
     * Handles ad-hoc executions and applies their effects on the campaign state.
     *
     * <p>This method can affect the campaign's temporary prisoner capacity and crime rating, and
     * it generates reports based on whether the executions were noticed or backfired.</p>
     *
     * @param campaign The current campaign instance.
     * @param victims  The number of victims executed.
     */
    public static void processAdHocExecution(Campaign campaign, int victims) {
        // Did the execution backfire?
        int backfireRoll = Compute.d6(1);
        boolean hasBackfired = backfireRoll == 1;

        if (hasBackfired) {
            campaign.setTemporaryPrisonerCapacity(-(victims * 2));
        } else {
            campaign.setTemporaryPrisonerCapacity(victims * 2);
        }

        // Was the crime noticed?
        int crimeNoticeRoll = Compute.randomInt(100);
        boolean crimeNoticed = crimeNoticeRoll < victims;

        int penalty = min(MAX_CRIME_PENALTY, victims * 2);
        if (crimeNoticed && campaign.getCampaignOptions().getUnitRatingMethod().isCampaignOperations()) {
            campaign.changeCrimeRating(-penalty);
            campaign.setDateOfLastCrime(campaign.getLocalDate());
        }

        // Build the report
        String key = getFormattedTextAt(RESOURCE_BUNDLE, hasBackfired ? "execute.backfired" : "execute.successful");

        String messageColor = hasBackfired
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor());

        String crimeColor = crimeNoticed
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor());

        String crimeMessage = crimeNoticed
            ? getFormattedTextAt(RESOURCE_BUNDLE, "execute.crimeNoticed",
                crimeColor, CLOSING_SPAN_TAG, penalty)
            : getFormattedTextAt(RESOURCE_BUNDLE, "execute.crimeUnnoticed",
                crimeColor, CLOSING_SPAN_TAG);

        // Add the report
        campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, key), messageColor, CLOSING_SPAN_TAG,
                crimeMessage);
    }

    /**
     * Calculates the total capacity usage for holding prisoners in the campaign.
     *
     * <p>Includes adjustments for capture styles and considers the needs of injured prisoners.
     * This value represents the total number of prisoners consuming capacity resources.</p>
     *
     * @param campaign The current campaign instance.
     * @return The total prisoner capacity usage.
     */
    public static int calculatePrisonerCapacityUsage(Campaign campaign) {
        PrisonerCaptureStyle captureStyle = campaign.getCampaignOptions().getPrisonerCaptureStyle();
        boolean isMekHQCaptureStyle = captureStyle.isMekHQ();

        int prisonerCapacityUsage = 0;

        for (Person prisoner : campaign.getCurrentPrisoners()) {
            if (prisoner.needsFixing() && isMekHQCaptureStyle) {
                if (prisoner.getDoctorId() != null) {
                    // Injured prisoners without doctors increase prisoner unhappiness, increasing
                    // capacity usage.
                    prisonerCapacityUsage++;
                }
            }

            prisonerCapacityUsage++;
        }

        return prisonerCapacityUsage;
    }

    /**
     * Calculates the total available capacity for holding prisoners in the campaign.
     *
     * <p>This calculation accounts for forces capable of handling prisoners, such as security
     * units, and factors in adjustments based on the MekHQ capture style and temporary capacity
     * modifiers.</p>
     *
     * @param campaign The current campaign instance.
     * @return The total prisoner capacity.
     */
    public static int calculatePrisonerCapacity(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        PrisonerCaptureStyle captureStyle = campaignOptions.getPrisonerCaptureStyle();
        boolean isMekHQCaptureStyle = captureStyle.isMekHQ();

        int prisonerCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (!force.isForceType(SECURITY)) {
                continue;
            }

            for (UUID unitId : force.getUnits()) {
                Unit unit = campaign.getUnit(unitId);
                if (unit == null) {
                    continue;
                }

                if (!unit.isAvailable()) {
                    continue;
                }

                if (isProhibitedUnitType(unit)) {
                    continue;
                }

                if (unit.isBattleArmor()) {
                    int crewSize = unit.getCrew().size();
                    for (int trooper = 0; trooper < crewSize; trooper++) {
                        if (unit.isBattleArmorSuitOperable(trooper)) {
                            prisonerCapacity += isMekHQCaptureStyle
                                ? PRISONER_CAPACITY_BATTLE_ARMOR
                                : PRISONER_CAPACITY_BATTLE_ARMOR * PRISONER_CAPACITY_CAM_OPS_MULTIPLIER;
                        }
                    }

                    continue;
                }

                if (unit.isConventionalInfantry()) {
                    for (Person soldier : unit.getCrew()) {
                        if (!soldier.needsFixing()) {
                            prisonerCapacity += isMekHQCaptureStyle
                                ? PRISONER_CAPACITY_CONVENTIONAL_INFANTRY
                                : PRISONER_CAPACITY_CONVENTIONAL_INFANTRY * PRISONER_CAPACITY_CAM_OPS_MULTIPLIER;
                        }
                    }
                    continue;
                }

                if (!unit.isDamaged() && isMekHQCaptureStyle) {
                    prisonerCapacity += unit.getCrew().size() * PRISONER_CAPACITY_OTHER;
                }
            }
        }

        double modifier = (double) campaign.getTemporaryPrisonerCapacity() / 100;

        if (isMekHQCaptureStyle) {
            return max(0, (int) round(prisonerCapacity * modifier));
        } else {
            return max(0, prisonerCapacity);
        }
    }

    /**
     * Determines whether the specified unit is of a prohibited type. A unit is considered
     * prohibited if its associated entity is an aerospace entity.
     *
     * @param unit The unit to be checked for prohibition. Must not be null.
     * @return true if the unit's entity is an aerospace entity, false otherwise.
     */
    private static boolean isProhibitedUnitType(Unit unit) {
        Entity entity = unit.getEntity();

        if (entity == null) {
            return false;
        }

        return entity.isAerospace();
    }

    /**
     * Retrieves the speaker for a prisoner-related dialog or event.
     *
     * <p>The speaker is typically selected from security forces within the campaign. If no suitable
     * speaker is found, a senior administrator with the transport specialization is returned as a
     * fallback.</p>
     *
     * @return The selected {@link Person} who acts as the speaker, or {@code null} if none is found.
     */
    private @Nullable Person getSpeaker() {
        List<Force> securityForces = new ArrayList<>();

        for (Force force : campaign.getAllForces()) {
            if (force.isForceType(SECURITY)) {
                securityForces.add(force);
            }
        }

        Person speaker = null;

        if (!securityForces.isEmpty()) {
            Collections.shuffle(securityForces);
            Force designatedForce = securityForces.get(0);
            UUID speakerId = designatedForce.getForceCommanderID();
            if (speakerId != null) {
                speaker = campaign.getPerson(speakerId);
            }
        }

        if (speaker == null) {
            return campaign.getSeniorAdminPerson(TRANSPORT);
        } else {
            return speaker;
        }
    }

    /**
     * Generates a random integer between 0 (inclusive) and the specified maximum value (exclusive).
     *
     * <p>This method exists to assist testing. As it allows us to easily override the return
     * value.</p>
     *
     * @param maxValue The upper bound (exclusive) for the random integer. Must be greater than 0.
     * @return A randomly generated integer in the range [0, (maxValue - 1)].
     */
    protected int randomInt(int maxValue) {
        return Compute.randomInt(maxValue);
    }

    /**
     * Rolls a specified number of six-sided dice and computes the total.
     *
     * <p>This method exists to assist testing. As it allows us to easily override the return
     * value.</p>
     *
     * @param dice The number of six-sided dice to roll. Must be a non-negative integer.
     * @return The total result of the rolled dice.
     */
    protected int d6(int dice) {
        return Compute.d6(dice);
    }
}
