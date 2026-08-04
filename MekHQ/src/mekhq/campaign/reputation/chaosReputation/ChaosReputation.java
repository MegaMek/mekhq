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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import mekhq.campaign.mission.AbstractMissionTransition;
import mekhq.campaign.mission.enums.MissionStatus;
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
        // When tracking at the campaign level, the stored reputation is authoritative and is not derived from
        // personnel. The debt penalty is applied live at display time (see getCampaignLevelReputation), so there is
        // nothing to recalculate here.
        if (campaign.getCampaignOptions().get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)) {
            return;
        }

        PlayerForce playerForce = campaign.getPlayerForce();
        Collection<Person> personnel = playerForce.allPersonnel();
        String formationName = playerForce.getName();
        int total = getTotalReputation(campaign, personnel, playerForce, formationName);

        playerForce.setChaosCampaignReputation(total);
    }

    /**
     * Returns the force's campaign-level Chaos Reputation: the stored campaign reputation value plus the current debt
     * penalty. Used when {@link CampaignOption#CAMPAIGN_LEVEL_CHAOS_REPUTATION} is enabled, where the reputation is
     * tracked as a single campaign-wide value rather than being derived from personnel.
     *
     * @param campaign the campaign to report on
     *
     * @return the campaign-level reputation total, including the debt penalty
     */
    public static int getCampaignLevelReputation(Campaign campaign) {
        PlayerForce playerForce = campaign.getPlayerForce();
        int base = playerForce.getChaosCampaignReputation();
        int debtModifier = getDebtModifier(playerForce.getFinances().getLoans(),
              campaign.getLocalDate(),
              campaign.getCampaignOptions().get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK));
        int manualModifier = campaign.getCampaignOptions().get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
        return applyReputationCap(campaign, base + debtModifier + manualModifier);
    }

    /**
     * Applies the configured Chaos Reputation cap to a reputation value. When the cap
     * ({@link CampaignOption#CHAOS_REPUTATION_CAP}) is {@code 0}, the reputation is uncapped and returned unchanged;
     * otherwise the reputation is limited to at most the cap.
     *
     * @param campaign   the campaign whose cap option to read
     * @param reputation the reputation value to cap
     *
     * @return the capped reputation value
     */
    public static int applyReputationCap(Campaign campaign, int reputation) {
        int cap = campaign.getCampaignOptions().get(CampaignOption.CHAOS_REPUTATION_CAP);
        if (cap != 0) {
            return min(reputation, cap);
        }
        return reputation;
    }

    /**
     * Tabulates the base Chaos Reputation accrued from completed contracts that ended within a given window.
     *
     * <p>Contracts are processed in chronological order (by ending date). Only contracts whose ending date falls on or
     * between {@code windowStart} and {@code windowEnd} (inclusive) are counted; a {@code null} bound is treated as
     * open-ended, and a contract with no recorded ending date is always counted. This lets the caller restrict the
     * tally to a single character's tenure - from recruitment until death or retirement.</p>
     *
     * @param campaign    the campaign whose completed contracts to tabulate
     * @param windowStart the earliest ending date to count, or {@code null} for no lower bound
     * @param windowEnd   the latest ending date to count, or {@code null} for no upper bound
     *
     * @return the base reputation earned from contracts within the window
     */
    public static int tabulateReputationFromContracts(Campaign campaign, LocalDate windowStart, LocalDate windowEnd) {
        int reputation = STARTING_REPUTATION_SCORE;

        List<AbstractMissionTransition> completedMissions = new ArrayList<>(campaign.getCompletedMissions());
        completedMissions.sort(Comparator.comparing(AbstractMissionTransition::getEndingDate,
              Comparator.nullsFirst(Comparator.naturalOrder())));

        for (AbstractMissionTransition mission : completedMissions) {
            if (!isWithinWindow(mission.getEndingDate(), windowStart, windowEnd)) {
                continue;
            }

            MissionStatus status = mission.getStatus();
            if (status.isSuccess()) {
                reputation += CONTRACT_SUCCESS_DELTA;
            } else if (status.isBreach()) {
                int loss = clamp((int) round(reputation * BREAKING_CONTRACT_MULTIPLIER),
                      0,
                      -BREAKING_CONTRACT_MAX_DELTA);
                reputation -= loss;
            }
        }

        return reputation;
    }

    private static boolean isWithinWindow(LocalDate date, LocalDate windowStart, LocalDate windowEnd) {
        if (date == null) {
            return true;
        }
        if (windowStart != null && date.isBefore(windowStart)) {
            return false;
        }
        return windowEnd == null || !date.isAfter(windowEnd);
    }

    private static LocalDate earliestDate(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isBefore(second) ? first : second;
    }

    /**
     * Retroactively initializes the campaign's Chaos Reputation from its completed contracts.
     *
     * <p>Each character's base reputation is tabulated over their own tenure - only contracts that ended between their
     * recruitment date and their departure (the earlier of death or retirement) are counted - so late-joining or
     * long-departed personnel are not credited for contracts they were not present for. The force-wide campaign
     * reputation is tabulated over the full contract history. Both are written, so the result is correct whether
     * reputation is tracked per character or at the campaign level. This is a permanent change.</p>
     *
     * @param campaign the campaign to update
     *
     * @return the force-wide reputation value applied
     */
    public static int applyRetroactiveContractReputation(Campaign campaign) {
        for (Person person : campaign.getPlayerForce().getHumanResources().getPersonnel()) {
            LocalDate departure = earliestDate(person.getDateOfDeath(), person.getRetirement());
            person.setReputationDirect(tabulateReputationFromContracts(campaign,
                  person.getRecruitment(),
                  departure));
        }

        int forceReputation = tabulateReputationFromContracts(campaign, null, null);
        campaign.getPlayerForce().setChaosCampaignReputation(forceReputation);

        return forceReputation;
    }

    /**
     * Builds an HTML tooltip breaking the campaign-level Chaos Reputation down into its stored value, debt penalty, and
     * resulting total. This mirrors the per-character reputation tooltip shown in the personnel view.
     *
     * @param campaign the campaign to report on
     *
     * @return an HTML tooltip string
     */
    public static String getCampaignLevelTooltip(Campaign campaign) {
        PlayerForce playerForce = campaign.getPlayerForce();
        int base = playerForce.getChaosCampaignReputation();
        int debtModifier = getDebtModifier(playerForce.getFinances().getLoans(),
              campaign.getLocalDate(),
              campaign.getCampaignOptions().get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK));
        int manualModifier = campaign.getCampaignOptions().get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
        int total = applyReputationCap(campaign, base + debtModifier + manualModifier);

        return getFormattedTextAt(RESOURCE_BUNDLE, "campaignLevel.tooltip",
              Integer.toString(base),
              Integer.toString(debtModifier),
              Integer.toString(manualModifier),
              Integer.toString(total));
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
        int debtModifier = getDebtModifier(loans,
              currentDate,
              campaign.getCampaignOptions().get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK));
        int manualModifier = campaign.getCampaignOptions().get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);

        int total = applyReputationCap(campaign, modeReputation + debtModifier + manualModifier);

        String report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.update",
              spanOpeningWithCustomColor(getAmazingColor()),
              CLOSING_SPAN_TAG,
              formationName,
              modeReputation,
              debtModifier,
              manualModifier,
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

    public static int getDebtModifier(List<Loan> loans, LocalDate currentDate, boolean stackPenalties) {
        if (loans.isEmpty()) {
            return 0;
        }

        // When penalties stack, every outstanding loan contributes its own debt penalty; otherwise only the oldest
        // loan is penalized.
        if (stackPenalties) {
            int debtModifier = 0;
            for (Loan loan : loans) {
                debtModifier += getLoanDebtPenalty(loan.getAgeInMonths(currentDate));
            }
            return debtModifier;
        }

        long maxLoanAge = 0;
        for (Loan loan : loans) {
            long age = loan.getAgeInMonths(currentDate);
            if (age > maxLoanAge) {
                maxLoanAge = age;
            }
        }
        return getLoanDebtPenalty(maxLoanAge);
    }

    private static int getLoanDebtPenalty(long ageInMonths) {
        return ((int) floor(ageInMonths / GOING_INT_DEBT_MONTHLY_FREQUENCY) + 1) * GOING_INTO_DEBT_DELTA;
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

    public static int getExperienceLevel(Campaign campaign, Person person, boolean isPrimary) {
        PersonnelRole role = isPrimary ? person.getPrimaryRole() : person.getSecondaryRole();

        if (!role.isCivilian()) {
            return person.getExperienceLevel(campaign, !isPrimary, true);
        }

        return EXP_NONE;
    }
}
