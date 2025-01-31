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
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
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
import static megamek.common.Compute.d6;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.getPersonalityValue;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

public class PrisonerEventManager {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    private final Campaign campaign;
    private final Person speaker;

    // CamOps states that executing prisoners incurs a -50 reputation penalty.
    // However, that lacks nuance, so we've changed it to -1 per prisoner to a maximum of -50.
    public static final int MAX_CRIME_PENALTY = 50;
    private final int DEFAULT_EVENT_CHANCE = STALEMATE.ordinal();
    private final int MINIMUM_PRISONER_COUNT = 25;
    private final int RESPONSE_TARGET_NUMBER = 7;

    // Fixed Dialog Options
    private final int CHOICE_FREE = 1;
    private final int CHOICE_EXECUTE = 2;

    public PrisonerEventManager(Campaign campaign) {
        this.campaign = campaign;
        this.speaker = getSpeaker();

        if (campaign.getCurrentPrisoners().isEmpty()) {
            return;
        }

        LocalDate today = campaign.getLocalDate();

        // Monthly events
        if (today.getDayOfMonth() == 1) {
            // reset temporary prisoner capacity
            campaign.setTemporaryPrisonerCapacity(100);

            // Check for ransom events
            if (campaign.hasActiveContract()) {
                Contract contract = campaign.getActiveContracts().get(0);

                int ransomEventChance = DEFAULT_EVENT_CHANCE;
                if (contract instanceof AtBContract) {
                    ransomEventChance = ((AtBContract) contract).getMoraleLevel().ordinal();
                }

                int roll = d6(2);
                if (roll <= ransomEventChance) {
                    boolean isFriendlyPOWs = false;

                    if (!campaign.getFriendlyPrisoners().isEmpty()) {
                        isFriendlyPOWs = d6(1) <= 2;
                    }

                    new PrisonerRansomEvent(campaign, isFriendlyPOWs);
                }
            }
        }

        // Weekly events
        if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            int totalPrisoners = campaign.getCurrentPrisoners().size();
            int prisonerCapacityUsage = calculatePrisonerCapacityUsage(campaign);
            int prisonerCapacity = calculatePrisonerCapacity(campaign);

            int overflow = prisonerCapacityUsage - prisonerCapacity;

            if (overflow <= 0) {
                // No risk of event
                return;
            }

            boolean minorEvent = randomInt(100) < overflow;
            // Does the minor event escalate into a major event?
            boolean majorEvent = minorEvent
                && (totalPrisoners > MINIMUM_PRISONER_COUNT)
                && (randomInt(100) < overflow);

            // If there is no event, throw up a warning and give the player an opportunity to do
            // something about the situation.
            if (!minorEvent) {
                processWarning(overflow);
                return;
            }

            // Random Event
            processRandomEvent(majorEvent);
        }
    }

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

    private PrisonerEventData pickEvent(boolean isMajor) {
        List<PrisonerEventData> allMajorEvents = campaign.getRandomEventLibraries().getPrisonerEvents(isMajor);
        Collections.shuffle(allMajorEvents);
        return ObjectUtility.getRandomItem(allMajorEvents);
    }

    private boolean makeEventCheck(PrisonerEventData eventData, int choiceIndex) {
        int responseModifier = 0;
        if (speaker != null) {
            responseModifier = getPersonalityValue(campaign, speaker);
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

    private void processExecutions(int executions, List<Person> prisoners) {
        for (int i = 0; i < executions; i++) {
            Person prisoner = prisoners.get(i);
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "execute.report",
                    prisoner.getFullName()));
            campaign.removePerson(prisoner, false);
        }

        processAdHocExecution(campaign, executions);
    }

    public static void processAdHocExecution(Campaign campaign, int victims) {
        // Did the execution backfire?
        int backfireRoll = d6(1);
        boolean hasBackfired = backfireRoll == 1;

        if (hasBackfired) {
            campaign.setTemporaryPrisonerCapacity(-(victims * 2));
        } else {
            campaign.setTemporaryPrisonerCapacity(victims * 2);
        }

        // Was the crime noticed?
        int crimeNoticeRoll = randomInt(100);
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

    public static int calculatePrisonerCapacityUsage(Campaign campaign) {
        int prisonerCapacityUsage = 0;

        for (Person prisoner : campaign.getCurrentPrisoners()) {
            // TODO this needs to be optional
            if (prisoner.needsFixing()) {
                if (prisoner.getDoctorId() != null) {
                    // Injured prisoners without doctors increase prisoner unhappiness, increasing capacity usage.
                    prisonerCapacityUsage++;
                }
            }

            prisonerCapacityUsage++;
        }

        return prisonerCapacityUsage;
    }

    public static int calculatePrisonerCapacity(Campaign campaign) {
        // These values are based on CamOps. CamOps states a platoon of CI or one squad of BA can
        // handle 100 prisoners. As there are usually around 28 soldiers in a CI platoon and 5 BA
        // in a BA Squad, we extrapolated from there to more easily handle different platoon and
        // squad sizes.
        final int PRISONER_CAPACITY_CONVENTIONAL_INFANTRY = 4;
        final int PRISONER_CAPACITY_BATTLE_ARMOR = 20;
        final int PRISONER_CAPACITY_OTHER = 2;

        int prisonerCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (!force.getForceType().isSecurity()) {
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

                if (unit.isBattleArmor()) {
                    int crewSize = unit.getCrew().size();
                    for (int trooper = 0; trooper < crewSize; trooper++) {
                        if (unit.isBattleArmorSuitOperable(trooper)) {
                            prisonerCapacity += PRISONER_CAPACITY_BATTLE_ARMOR;
                        }
                    }

                    prisonerCapacity += crewSize * PRISONER_CAPACITY_BATTLE_ARMOR;
                    continue;
                }

                if (unit.isConventionalInfantry()) {
                    for (Person soldier : unit.getCrew()) {
                        if (!soldier.needsFixing()) {
                            prisonerCapacity += PRISONER_CAPACITY_CONVENTIONAL_INFANTRY;
                        }
                    }
                }

                // TODO This needs to be an optional rule
                if (!unit.isDamaged()) {
                    for (Person crewMember : unit.getCrew()) {
                        if (!crewMember.needsFixing()) {
                            prisonerCapacity += PRISONER_CAPACITY_OTHER;
                        }
                    }
                }
            }
        }

        double modifier = (double) campaign.getTemporaryPrisonerCapacity() / 100;

        return max(0, (int) round(prisonerCapacity * modifier));
    }

    private @Nullable Person getSpeaker() {
        List<Force> securityForces = new ArrayList<>();

        for (Force force : campaign.getAllForces()) {
            if (force.getForceType().isSecurity()) {
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
}
