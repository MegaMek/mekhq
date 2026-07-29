package mekhq.campaign.reputation.chaosReputation;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.common.compute.Compute.d6;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.chaosCampaign.ChaosCampaignUtilities;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;

public class ChaosReputation {
    private static final MMLogger LOGGER = MMLogger.create(ChaosReputation.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosReputation";

    public static final int STARTING_REPUTATION_SCORE = 1; // Hot Spots Draconis Reach pg25 1st printing

    private static final int CONTRACT_SUCCESS_DELTA = 1; // Hot Spots Draconis Reach pg25 1st printing

    private static final double BREAKING_CONTRACT_MULTIPLIER = 0.5; // Hot Spots Draconis Reach pg25 1st printing
    private static final int BREAKING_CONTRACT_MAX_DELTA = -3; // Hot Spots Draconis Reach pg25 1st printing

    private static final int PIRACY_AVOIDANCE_TN_SUCCESS = 7; // Hot Spots Draconis Reach pg25 1st printing
    private static final int PIRACY_AVOIDANCE_TN_FAILURE = 9; // Hot Spots Draconis Reach pg25 1st printing
    private static final double PIRACY_PENALTY_PROFIT_DIVIDER = 500; // Hot Spots Draconis Reach pg25 1st printing

    private static final int GOING_INTO_DEBT_DELTA = -1; // Hot Spots Draconis Reach pg25 1st printing
    private static final double GOING_INT_DEBT_MONTHLY_FREQUENCY = 6.0; // Hot Spots Draconis Reach pg25 1st printing

    public static void calculateForceReputation(Campaign campaign) {
        PlayerForce playerForce = campaign.getPlayerForce();
        Collection<Person> personnel = playerForce.allPersonnel();
        LocalDate currentDate = campaign.getLocalDate();

        int modeReputation = calculateMostCommonReputation(personnel,
              campaign.getCampaignOptions().get(CampaignOption.USE_AGE_EFFECTS),
              playerForce.isClanForce(),
              currentDate);

        int debtModifier = getDebtModifier(playerForce, currentDate);

        int total = modeReputation - debtModifier;

        String report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.update",
              spanOpeningWithCustomColor(getAmazingColor()),
              CLOSING_SPAN_TAG,
              playerForce.getName(),
              modeReputation,
              debtModifier,
              total);
        campaign.addReport(DailyReportType.GENERAL, report);

        playerForce.setChaosCampaignReputation(total);
    }

    public static int getDetachmentReputation(Campaign campaign, Detachment detachment, boolean isUseAgingEffects) {
        PlayerForce playerForce = campaign.getPlayerForce();
        Collection<Person> personnel = detachment.getPersonnel().values();
        LocalDate currentDate = campaign.getLocalDate();

        int modeReputation = calculateMostCommonReputation(personnel,
              isUseAgingEffects,
              playerForce.isClanForce(),
              currentDate);

        int debtModifier = getDebtModifier(playerForce, currentDate);

        int total = modeReputation - debtModifier;

        String report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.update",
              spanOpeningWithCustomColor(getAmazingColor()),
              CLOSING_SPAN_TAG,
              playerForce.getName(), // TODO detachment name (this functionality doesn't yet exist)
              modeReputation,
              debtModifier,
              total);
        campaign.addReport(DailyReportType.GENERAL, report);

        return modeReputation - debtModifier;
    }

    private static int calculateMostCommonReputation(Collection<Person> personnel, boolean isUseAgingEffects,
          boolean isClanCampaign, LocalDate currentDate) {
        // Tallies how often each adjusted reputation value appears among valid personnel.
        Map<Integer, Integer> reputationCounts = new HashMap<>();

        for (Person person : personnel) {
            // Is the person even here?
            if (person.getStatus().isDepartedUnit()) {
                continue;
            }

            // Is the person active and employed?
            if (person.getStatus().isActive() && person.isEmployed()) {
                int reputation = person.getAdjustedReputation(isUseAgingEffects, isClanCampaign, currentDate);
                reputationCounts.merge(reputation, 1, Integer::sum);
            }
        }

        LOGGER.info("Gathered all reputation from combat personnel in the force");
        LOGGER.info(reputationCounts);

        return getModeReputation(reputationCounts);
    }

    private static int getModeReputation(Map<Integer, Integer> reputationCounts) {
        int bestReputation = STARTING_REPUTATION_SCORE;
        int bestCount = 0;

        for (Map.Entry<Integer, Integer> entry : reputationCounts.entrySet()) {
            int reputation = entry.getKey();
            int count = entry.getValue();

            // Higher count wins; on a tie, prefer the higher reputation value.
            boolean beatsBestCount = count > bestCount;
            boolean beatsOnADraw = count == bestCount && reputation > bestReputation;

            if (beatsBestCount || beatsOnADraw) {
                bestCount = count;
                bestReputation = reputation;
            }
        }

        LOGGER.info("Mode Reputation: {}", bestReputation);

        return bestReputation;
    }

    private static int getDebtModifier(PlayerForce playerForce, LocalDate currentDate) {
        List<Loan> activeLoans = playerForce.getFinances().getLoans();
        long maxLoanAge = 0;
        for (Loan loan : activeLoans) {
            long age = loan.getAgeInMonths(currentDate);
            if (age > maxLoanAge) {
                maxLoanAge = age;
            }
        }

        int debtModifier = (int) floor(maxLoanAge / GOING_INT_DEBT_MONTHLY_FREQUENCY);
        if (!activeLoans.isEmpty()) {
            debtModifier++;
        }
        return debtModifier;
    }

    public static void updatePersonnelForContractSuccess(Campaign campaign, Collection<Person> personnel) {
        for (Person person : personnel) {
            person.changeReputation(CONTRACT_SUCCESS_DELTA);
        }

        String report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.contractSuccess",
              spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG, CONTRACT_SUCCESS_DELTA);
        campaign.addReport(DailyReportType.GENERAL, report);
    }

    public static void updatePersonnelForContractBreak(Campaign campaign, Collection<Person> personnel) {
        for (Person person : personnel) {
            int baseReputation = person.getReputationDirect();
            int delta = (int) round(baseReputation * BREAKING_CONTRACT_MULTIPLIER);
            delta = min(delta, BREAKING_CONTRACT_MAX_DELTA);
            person.changeReputation(-delta);
        }

        String report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.brokenContract",
              spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG, BREAKING_CONTRACT_MAX_DELTA);
        campaign.addReport(DailyReportType.GENERAL, report);
    }

    public static void updatePersonnelForActOfPiracy(Campaign campaign, Collection<Person> personnel,
          boolean actWasSuccessful, Money lootValue) {
        int roll = d6(2);
        int targetNumber = actWasSuccessful ? PIRACY_AVOIDANCE_TN_SUCCESS : PIRACY_AVOIDANCE_TN_FAILURE;

        int supperPointsFromMoney = ChaosCampaignUtilities.getChaosSupportPointsFromMoney(lootValue);
        int delta = (int) ceil(supperPointsFromMoney / PIRACY_PENALTY_PROFIT_DIVIDER);

        String report;
        boolean gotCaught = roll < targetNumber;
        if (gotCaught) {
            for (Person person : personnel) {
                person.changeCriminalRecord(-delta);
            }

            report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.piracy.caught",
                  spanOpeningWithCustomColor(getNegativeColor()),
                  CLOSING_SPAN_TAG,
                  roll,
                  targetNumber,
                  delta);
        } else {
            report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.piracy.gotAway",
                  spanOpeningWithCustomColor(getPositiveColor()),
                  CLOSING_SPAN_TAG,
                  roll,
                  targetNumber);
        }

        campaign.addReport(DailyReportType.GENERAL, report);
    }
}
