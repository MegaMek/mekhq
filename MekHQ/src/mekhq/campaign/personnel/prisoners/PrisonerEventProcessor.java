package mekhq.campaign.personnel.prisoners;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.prisonerDialogs.PrisonerEventDialog;
import mekhq.gui.dialog.prisonerDialogs.PrisonerEventResultsDialog;
import mekhq.gui.dialog.prisonerDialogs.PrisonerWarningDialog;
import mekhq.gui.dialog.prisonerDialogs.PrisonerWarningResultsDialog;

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
import static mekhq.campaign.personnel.randomEvents.PersonalityController.getPersonalityValue;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

public class PrisonerEventProcessor {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerEventDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    private final Campaign campaign;

    // CamOps states that executing prisoners incurs a -50 reputation penalty.
    // However, that lacks nuance, so we've changed it to -1 per prisoner to a maximum of -50.
    private static final int MAX_CRIME_PENALTY = 50;
    private final int DEFAULT_EVENT_CHANCE = STALEMATE.ordinal();
    private final int MINIMUM_PRISONER_COUNT = 25;

    // Fixed Dialog Options
    private static int CHOICE_FREE = 1;
    private static int CHOICE_EXECUTE = 2;

    // Major Event Responses
    public enum ResponseType {
        RESPONSE_NEUTRAL, RESPONSE_POSITIVE, RESPONSE_NEGATIVE
    }

    private Map<Integer, List<ResponseType>> majorEventResponses = new HashMap<>();
    private final int RESPONSE_TARGET_NUMBER = 7;

    public PrisonerEventProcessor(Campaign campaign) {
        this.campaign = campaign;

        if (campaign.getCurrentPrisoners().isEmpty()) {
            return;
        }

        LocalDate today = campaign.getLocalDate();

        // Monthly events
        if (today.getDayOfMonth() == 1) {
            // reset temporary prisoner capacity
            campaign.setTemporaryPrisonerCapacity(0);

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

            Person speaker = getSpeaker(campaign);
            // If there is no event, throw up a warning and give the player an opportunity to do
            // something about the situation.
            if (!minorEvent) {
                processWarning(campaign, overflow, speaker);
                return;
            }

            // Major Event
            if (majorEvent) {
                processMajorEvent(campaign, speaker);
                return;
            }

            processMinorEvent(speaker, overflow);
        }
    }

    private void processWarning(Campaign campaign, int overflow, Person speaker) {
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
                campaign.addReport(String.format(resources.getString("free.report"),
                    prisoner.getFullName()));
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

    private void processMajorEvent(Campaign campaign, Person speaker) {
        buildMajorEventResponses();

        int event = randomInt(10);
        PrisonerEventDialog eventDialog =
            new PrisonerEventDialog(campaign, speaker, event, false);

        int choice = eventDialog.getDialogChoice();

        int responseModifier = 0;
        if (speaker != null) {
            responseModifier = getPersonalityValue(campaign, speaker);
        }

        ResponseType response = majorEventResponses.get(event).get(choice);

        switch (response) {
            case RESPONSE_NEUTRAL -> {}
            case RESPONSE_POSITIVE -> responseModifier += 3;
            case RESPONSE_NEGATIVE -> responseModifier -= 3;
        }

        int responseCheck = d6(2) + responseModifier;
        boolean isSuccessful = responseCheck >= RESPONSE_TARGET_NUMBER;

        new PrisonerEventResultsDialog(campaign, speaker, event, choice, false, isSuccessful);
    }

    private void buildMajorEventResponses() {
        // event 0
        majorEventResponses.put(0, List.of(
                ResponseType.RESPONSE_NEGATIVE,
                ResponseType.RESPONSE_NEGATIVE,
                ResponseType.RESPONSE_NEUTRAL));

        // event 1
        majorEventResponses.put(1, List.of(
                ResponseType.RESPONSE_POSITIVE,
                ResponseType.RESPONSE_NEGATIVE,
                ResponseType.RESPONSE_NEUTRAL));

        // event 2
        majorEventResponses.put(2, List.of(
            ResponseType.RESPONSE_POSITIVE,
            ResponseType.RESPONSE_NEUTRAL,
            ResponseType.RESPONSE_NEGATIVE));

        // event 3
        majorEventResponses.put(3, List.of(
            ResponseType.RESPONSE_POSITIVE,
            ResponseType.RESPONSE_NEUTRAL,
            ResponseType.RESPONSE_NEGATIVE));

        // event 4
        majorEventResponses.put(4, List.of(
            ResponseType.RESPONSE_NEUTRAL,
            ResponseType.RESPONSE_NEUTRAL,
            ResponseType.RESPONSE_NEUTRAL));

        // event 5
        majorEventResponses.put(5, List.of(
            ResponseType.RESPONSE_NEUTRAL,
            ResponseType.RESPONSE_NEGATIVE,
            ResponseType.RESPONSE_NEGATIVE));

        // event 6
        majorEventResponses.put(6, List.of(
            ResponseType.RESPONSE_POSITIVE,
            ResponseType.RESPONSE_NEGATIVE,
            ResponseType.RESPONSE_NEUTRAL));

        // event 7
        majorEventResponses.put(7, List.of(
            ResponseType.RESPONSE_NEGATIVE,
            ResponseType.RESPONSE_POSITIVE,
            ResponseType.RESPONSE_NEGATIVE));

        // event 8
        majorEventResponses.put(8, List.of(
            ResponseType.RESPONSE_POSITIVE,
            ResponseType.RESPONSE_NEUTRAL,
            ResponseType.RESPONSE_NEGATIVE));

        // event 9
        majorEventResponses.put(9, List.of(
            ResponseType.RESPONSE_POSITIVE,
            ResponseType.RESPONSE_NEGATIVE,
            ResponseType.RESPONSE_NEUTRAL));
    }

    private void processMinorEvent(@Nullable Person speaker, int overflow) {
        int event = randomInt(50);
        new PrisonerEventDialog(campaign, speaker, event, true);
    }

    private void processExecutions(int executions, List<Person> prisoners) {
        for (int i = 0; i < executions; i++) {
            Person prisoner = prisoners.get(i);
            campaign.addReport(String.format(resources.getString("execute.report"),
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
        String message = hasBackfired
            ? resources.getString("execute.backfired")
            : resources.getString("execute.successful");
        String color = hasBackfired
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor());

        String crimeMessage = crimeNoticed
            ? String.format(resources.getString("execute.crimeNoticed"), penalty)
            : resources.getString("execute.crimeUnnoticed");

        // Add the report
        campaign.addReport(String.format(message, color, CLOSING_SPAN_TAG, crimeMessage));
    }

    public static int calculatePrisonerCapacityUsage(Campaign campaign) {
        int prisonerCapacityUsage = 0;

        for (Person prisoner : campaign.getCurrentPrisoners()) {
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
        final int PRISONER_CAPACITY_CI = 4;
        final int PRISONER_CAPACITY_BA = 20;

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

                if (unit.isBattleArmor()) {
                    int crewSize = unit.getCrew().size();
                    for (int trooper = 0; trooper < crewSize; trooper++) {
                        if (unit.isBattleArmorSuitOperable(trooper)) {
                            prisonerCapacity += PRISONER_CAPACITY_BA;
                        }
                    }

                    prisonerCapacity += crewSize * PRISONER_CAPACITY_BA;
                    continue;
                }

                if (unit.isConventionalInfantry()) {
                    for (Person soldier : unit.getCrew()) {
                        if (!soldier.needsFixing() && !soldier.needsAMFixing()) {
                            prisonerCapacity += PRISONER_CAPACITY_CI;
                        }
                    }
                }
            }
        }

        return prisonerCapacity + campaign.getTemporaryPrisonerCapacity();
    }

    private static @Nullable Person getSpeaker(Campaign campaign) {
        List<Force> securityForces = new ArrayList<>();

        for (Force force : campaign.getAllForces()) {
            if (force.getForceType().isSecurity()) {
                securityForces.add(force);
            }
        }

        Collections.shuffle(securityForces);
        Force designatedForce = securityForces.get(0);

        Person speaker = null;
        UUID speakerId = designatedForce.getForceCommanderID();
        if (speakerId != null) {
            speaker = campaign.getPerson(speakerId);
        }

        if (speaker == null) {
            return campaign.getSeniorAdminPerson(TRANSPORT);
        } else {
            return speaker;
        }
    }
}
