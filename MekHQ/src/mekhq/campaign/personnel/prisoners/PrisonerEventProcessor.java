package mekhq.campaign.personnel.prisoners;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.prisonerDialogs.PrisonerMinorEventDialog;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Calendar.MONDAY;
import static megamek.common.Compute.d6;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

public class PrisonerEventProcessor {
    private static final String BUNDLE_KEY = "mekhq.resources.PrisonerMinorEventDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    private final Campaign campaign;

    // CamOps states that executing prisoners incurs a -50 reputation penalty.
    // However, that lacks nuance, so we've changed it to -1 per prisoner to a maximum of -50.
    private static final int MAX_CRIME_PENALTY = 50;
    private final int DEFAULT_EVENT_CHANCE = STALEMATE.ordinal();

    // Dialog Options
    private static int CHOICE_FREE = 3;
    private static int CHOICE_EXECUTE = 4;

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
        if (today.getDayOfWeek().getValue() == MONDAY) {
            int prisonerCapacityUsage = calculatePrisonerCapacityUsage(campaign);
            int prisonerCapacity = calculatePrisonerCapacity(campaign);

            boolean majorEvent =  prisonerCapacityUsage > prisonerCapacity;
            boolean minorEvent = !majorEvent && prisonerCapacityUsage > (prisonerCapacity * 0.75);

            if (minorEvent) {
                int escalation = d6(1);
                if (escalation == 1) {
                    majorEvent = true;
                } else{
                    processMinorEvent();
                }
            }

            if (majorEvent) {}
        }
    }

    private void processMinorEvent() {
        List<Person> prisoners = campaign.getCurrentPrisoners();
        Collections.shuffle(prisoners);

        int prisonerPortion = max(1, (int) round(prisoners.size() * 0.1));

        int event = randomInt(50);
        PrisonerMinorEventDialog minorEventDialog = new PrisonerMinorEventDialog(campaign, prisonerPortion, event);
        int choice = minorEventDialog.getDialogChoice();

        if (choice == CHOICE_FREE) {
            for (int i = 0; i < prisonerPortion; i++) {
                Person prisoner = prisoners.get(i);
                campaign.addReport(String.format(resources.getString("free.report"),
                    prisoner.getFullName()));
                campaign.removePerson(prisoner, false);
            }
            return;
        }

        if (choice == CHOICE_EXECUTE) {
            processExecutions(prisonerPortion, prisoners);
        }
    }

    private void processExecutions(int prisonerPortion, List<Person> prisoners) {
        for (int i = 0; i < prisonerPortion; i++) {
            Person prisoner = prisoners.get(i);
            campaign.addReport(String.format(resources.getString("execute.report"),
                prisoner.getFullName()));
            campaign.removePerson(prisoner, false);
        }

        processAdHocExecution(campaign, prisonerPortion);
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
}
