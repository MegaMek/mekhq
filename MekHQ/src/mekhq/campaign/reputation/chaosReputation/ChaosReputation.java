package mekhq.campaign.reputation.chaosReputation;

import static java.lang.Math.ceil;
import static java.lang.Math.clamp;
import static java.lang.Math.floor;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import megamek.common.enums.SkillLevel;
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
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.SkillType;

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
        String formationName = playerForce.getName();
        int total = getTotalReputation(campaign, personnel, playerForce, formationName);

        playerForce.setChaosCampaignReputation(total);
    }

    public static int getDetachmentReputation(Campaign campaign, Detachment detachment) {
        PlayerForce playerForce = campaign.getPlayerForce();
        Collection<Person> personnel = detachment.getPersonnel().values();
        String formationName = playerForce.getName(); // TODO replace with detachment name, once possible

        return getTotalReputation(campaign, personnel, playerForce, formationName);
    }

    private static int getTotalReputation(Campaign campaign, Collection<Person> personnel, PlayerForce playerForce,
          String formationName) {
        LocalDate currentDate = campaign.getLocalDate();

        int modeReputation = calculateAverageReputation(personnel,
              campaign.getCampaignOptions().get(CampaignOption.USE_AGE_EFFECTS),
              playerForce.isClanForce(),
              currentDate);

        List<Loan> loans = playerForce.getFinances().getLoans();
        int debtModifier = getDebtModifier(loans, currentDate);

        int total = modeReputation + debtModifier;

        String report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.update",
              spanOpeningWithCustomColor(getAmazingColor()),
              CLOSING_SPAN_TAG,
              formationName,
              modeReputation,
              debtModifier,
              total);
        campaign.addReport(DailyReportType.GENERAL, report);

        return total;
    }

    private static int calculateAverageReputation(Collection<Person> personnel, boolean isUseAgingEffects,
          boolean isClanCampaign, LocalDate currentDate) {
        // Tallies how often each adjusted reputation value appears among valid personnel.
        double personCount = 0;
        int totalReputation = 0;

        for (Person person : personnel) {
            // Is the person even here?
            PersonnelStatus status = person.getStatus();
            if (status.isDepartedUnit()) {
                continue;
            }

            // Is the person active and employed?
            if (status.isActive() && person.isEmployed()) {
                personCount++;
                int reputation = person.getAdjustedReputation(isUseAgingEffects, isClanCampaign, currentDate);
                totalReputation += reputation;
            }
        }

        int averageReputation = (int) round(totalReputation / personCount);

        LOGGER.info("Gathered all reputation from combat personnel in the force");
        LOGGER.info("People: {}, Reputation: {}, Avg: {}", personCount, totalReputation, averageReputation);

        return averageReputation;
    }

    private static int getDebtModifier(List<Loan> loans, LocalDate currentDate) {
        long maxLoanAge = 0;
        for (Loan loan : loans) {
            long age = loan.getAgeInMonths(currentDate);
            if (age > maxLoanAge) {
                maxLoanAge = age;
            }
        }

        int debtModifier = (int) floor(maxLoanAge / GOING_INT_DEBT_MONTHLY_FREQUENCY) * GOING_INTO_DEBT_DELTA;
        if (!loans.isEmpty()) {
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

    public static SkillLevel getAverageSkillLevel(final Campaign campaign, final Collection<Person> personnel) {
        double personCount = 0;
        int totalExperienceLevel = 0;

        for (Person person : personnel) {
            PersonnelStatus status = person.getStatus();
            if (status.isDepartedUnit()) {
                continue;
            }

            // Is the person active and employed?
            if (status.isActive() && person.isEmployed()) {
                int primaryExperienceLevel = getExperienceLevel(campaign, person, true);
                if (primaryExperienceLevel != EXP_NONE) {
                    personCount++;
                    totalExperienceLevel += primaryExperienceLevel;
                }

                int secondaryExperienceLevel = getExperienceLevel(campaign, person, false);
                if (secondaryExperienceLevel != EXP_NONE) {
                    personCount++;
                    totalExperienceLevel += secondaryExperienceLevel;
                }
            }
        }

        int meanExperienceLevel = personCount == 0 ? EXP_NONE : (int) round(totalExperienceLevel / personCount);
        meanExperienceLevel = clamp(meanExperienceLevel, EXP_NONE, EXP_LEGENDARY);

        return SkillType.skillLevelFromExperienceLevel(meanExperienceLevel);
    }

    private static int getExperienceLevel(Campaign campaign, Person person, boolean isPrimary) {
        PersonnelRole role = isPrimary ? person.getPrimaryRole() : person.getSecondaryRole();

        if (!role.isCivilian()) {
            return person.getExperienceLevel(campaign, !isPrimary, true);
        }

        return EXP_NONE;
    }
}
