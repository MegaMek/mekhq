package mekhq.campaign.randomEvents.prisoners;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.gui.dialog.MissionEndPrisonerDefectorDialog;
import mekhq.gui.dialog.MissionEndPrisonerDialog;

import java.time.LocalDate;
import java.util.List;

import static java.lang.Math.min;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.finances.enums.TransactionType.RANSOM;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ACTIVE;
import static mekhq.campaign.personnel.enums.PersonnelStatus.HOMICIDE;
import static mekhq.campaign.personnel.enums.PersonnelStatus.LEFT;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.MAX_CRIME_PENALTY;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

public class PrisonerMissionEndEvent {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    private final Campaign campaign;
    private final AtBContract contract;
    private boolean isSuccess;
    private boolean isAllied;

    private final int GOOD_EVENT_CHANCE = 20;

    private final int CHOICE_ACCEPTED = 0;
    private final int CHOICE_RELEASE_THEM = 1;
    private final int CHOICE_EXECUTE_THEM = 2;

    public PrisonerMissionEndEvent(Campaign campaign, AtBContract contract) {
        this.campaign = campaign;
        this.contract = contract;
    }

    public int handlePrisonerDefectors() {
        MissionEndPrisonerDefectorDialog dialog = new MissionEndPrisonerDefectorDialog(campaign);
        return dialog.getDialogChoice();
    }

    public void handlePrisoners(boolean isSuccess, boolean isAllied) {
        this.isAllied = isAllied;
        this.isSuccess = isSuccess;

        List<Person> prisoners = isAllied ? campaign.getFriendlyPrisoners() : campaign.getCurrentPrisoners();
        Money ransom = getRansom(prisoners);
        boolean isGoodEvent = determineGoodEvent(isAllied);

        MissionEndPrisonerDialog dialog = new MissionEndPrisonerDialog(campaign, ransom, isAllied,
            isSuccess, isGoodEvent);

        processPlayerResponse(ransom, isGoodEvent, dialog.getDialogChoice(), prisoners);
    }

    private boolean determineGoodEvent(boolean isAllied) {
        if (isAllied) {
            LocalDate lastCrime = campaign.getDateOfLastCrime();
            if (lastCrime != null && lastCrime.isAfter(contract.getStartDate().minusDays(1))) {
                // Adjust the chance of a good event based on crime rating
                int adjustedChance = GOOD_EVENT_CHANCE - campaign.getAdjustedCrimeRating();

                return adjustedChance >= 1 && randomInt(adjustedChance) != 0;
            }
        }
        // Default chance for goodEvent (used for both non-allied and no recent crimes in allied)
        return randomInt(GOOD_EVENT_CHANCE) != 0;
    }

    private Money getRansom(List<Person> alliedPoWs) {
        Money alliedRansom = Money.zero();
        for (Person person : alliedPoWs) {
            Money ransomValue = person.getRansomValue(campaign);
            alliedRansom = alliedRansom.plus(ransomValue);
        }
        return alliedRansom;
    }

    private void processPlayerResponse(Money ransom, boolean isGoodEvent, int choiceIndex,
                                       List<Person> prisoners) {
        if (choiceIndex == CHOICE_RELEASE_THEM) {
            removeAllPrisoners(prisoners);
        }

        if (choiceIndex == CHOICE_EXECUTE_THEM) {
            removeAllPrisoners(prisoners);
            executePrisoners(prisoners);
        }

        final LocalDate today = campaign.getLocalDate();

        if (isAllied && isSuccess && isGoodEvent) {
            changeStatusOfAllPrisoners(prisoners, today, ACTIVE);
            return;
        }

        // Your IDE is going to tell you that some of these conditions can be removed as they're
        // always true. That's correct, but they should be left in place as that makes this
        // sequence much easier to follow.
        if (isAllied && isSuccess && !isGoodEvent) {
            changeStatusOfAllPrisoners(prisoners, today, HOMICIDE);
            return;
        }

        if (isAllied && !isSuccess && isGoodEvent) {
            boolean isAccepted = choiceIndex == CHOICE_ACCEPTED;
            if (isAccepted) {
                performRansom(false, ransom, today);
            }

            changeStatusOfAllPrisoners(prisoners, today, isAccepted ? ACTIVE : LEFT);
            return;
        }

        if (isAllied && !isSuccess && !isGoodEvent) {
            changeStatusOfAllPrisoners(prisoners, today, HOMICIDE);
            return;
        }

        if (!isAllied && isSuccess && isGoodEvent) {
            if (choiceIndex == CHOICE_ACCEPTED) {
                performRansom(true, ransom, today);
                removeAllPrisoners(prisoners);
            }
            return;
        }

        if (!isAllied && isSuccess && !isGoodEvent) {
            if (choiceIndex == CHOICE_ACCEPTED) {
                removeAllPrisoners(prisoners);
            }
            return;
        }

        if (!isAllied && !isSuccess && isGoodEvent) {
            if (choiceIndex == CHOICE_ACCEPTED) {
                performRansom(true, ransom, today);
                removeAllPrisoners(prisoners);
            }
            return;
        }

        if (!isAllied && !isSuccess && !isGoodEvent) {
            if (choiceIndex == CHOICE_ACCEPTED) {
                removeAllPrisoners(prisoners);
            }
        }
    }

    private void changeStatusOfAllPrisoners(List<Person> prisoners, LocalDate today, PersonnelStatus newStatus) {
        for (Person prisoner : prisoners) {
            prisoner.changeStatus(campaign, today, newStatus);
        }
    }

    private void performRansom(boolean isCredit, Money ransom, LocalDate today) {
        if (isCredit) {
            campaign.getFinances().credit(RANSOM, today, ransom, getFormattedTextAt(RESOURCE_BUNDLE,
                "transaction.ransom"));
        } else {
            campaign.getFinances().debit(RANSOM, today, ransom, getFormattedTextAt(RESOURCE_BUNDLE,
                "transaction.ransom"));
        }
    }

    private void removeAllPrisoners(List<Person> prisoners) {
        for (Person prisoner : prisoners) {
            campaign.removePerson(prisoner);
        }
    }

    private void executePrisoners(List<Person> prisoners) {
        // Was the crime noticed?
        int crimeNoticeRoll = randomInt(100);
        boolean crimeNoticed = crimeNoticeRoll < prisoners.size();

        int penalty = min(MAX_CRIME_PENALTY, prisoners.size() * 2);
        if (crimeNoticed && campaign.getCampaignOptions().getUnitRatingMethod().isCampaignOperations()) {
            campaign.changeCrimeRating(-penalty);
            campaign.setDateOfLastCrime(campaign.getLocalDate());
        }

        // Build the report
        String crimeColor = crimeNoticed
            ? spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())
            : spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor());

        String crimeMessage = crimeNoticed
            ? getFormattedTextAt(RESOURCE_BUNDLE, "execute.crimeNoticed",
            crimeColor, CLOSING_SPAN_TAG, penalty)
            : getFormattedTextAt(RESOURCE_BUNDLE, "execute.crimeUnnoticed",
            crimeColor, CLOSING_SPAN_TAG);

        // Add the report
        campaign.addReport(crimeMessage);
    }
}
