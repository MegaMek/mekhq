package mekhq.campaign.reputation.chaosReputation;

import static java.lang.Math.ceil;
import static java.lang.Math.clamp;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
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
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.chaosCampaign.ChaosCampaignUtilities;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.AbstractMissionTransition;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class ChaosReputation {
    private static final MMLogger LOGGER = MMLogger.create(ChaosReputation.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosReputation";

    public static final int STARTING_REPUTATION_SCORE = 1; // Hot Spots Draconis Reach pg25 1st printing

    private static final int CONTRACT_SUCCESS_DELTA = 1; // Hot Spots Draconis Reach pg25 1st printing

    private static final double BREAKING_CONTRACT_MULTIPLIER = 0.5; // Hot Spots Draconis Reach pg25 1st printing
    private static final int BREAKING_CONTRACT_MIN_DELTA = 3; // Hot Spots Draconis Reach pg25 1st printing

    private static final int PIRACY_AVOIDANCE_TN_SUCCESS = 7; // Hot Spots Draconis Reach pg25 1st printing
    private static final int PIRACY_AVOIDANCE_TN_FAILURE = 9; // Hot Spots Draconis Reach pg25 1st printing
    private static final double PIRACY_PENALTY_PROFIT_DIVIDER = 500; // Hot Spots Draconis Reach pg25 1st printing

    private static final int GOING_INTO_DEBT_DELTA = -1; // Hot Spots Draconis Reach pg25 1st printing
    private static final double GOING_INT_DEBT_MONTHLY_FREQUENCY = 6.0; // Hot Spots Draconis Reach pg25 1st printing

    // When migrating a CamOps campaign to Chaos reputation, the CamOps crime rating is divided by this to seed each
    // character's Chaos criminal record.
    private static final double CRIME_RATING_TO_CRIMINAL_RECORD_DIVIDER = 10.0;

    public static void processChaosCampaignReputationChanges(CampaignOptions campaignOptions, PlayerForce playerForce,
          LocalDate today) {
        boolean debtPenaltiesStack = campaignOptions.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK);
        int manualModifier = campaignOptions.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
        int cap = campaignOptions.get(CampaignOption.CHAOS_REPUTATION_CAP);

        if (campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)) {
            ChaosReputation.calculateCampaignLevelReputation(playerForce,
                  today,
                  debtPenaltiesStack,
                  manualModifier,
                  cap);
        } else {
            ChaosReputation.calculatePersonnelLevelReputation(playerForce,
                  today,
                  debtPenaltiesStack,
                  manualModifier,
                  cap,
                  campaignOptions.get(CampaignOption.USE_AGE_EFFECTS));
        }
    }

    private static void calculatePersonnelLevelReputation(PlayerForce playerForce, LocalDate currentDate,
          boolean debtPenaltiesStack, int manualModifier, int cap, boolean useAgeEffects) {
        Collection<Person> personnel = playerForce.allPersonnel();
        boolean isClanForce = playerForce.isClanForce();
        List<Loan> loans = playerForce.getFinances().getLoans();

        int total = processReputation(personnel,
              currentDate,
              useAgeEffects,
              isClanForce,
              loans,
              debtPenaltiesStack,
              manualModifier,
              cap);

        playerForce.setChaosCampaignReputation(total);
    }

    private static int processReputation(Collection<Person> personnel, LocalDate currentDate, boolean isUseAgeEffects,
          boolean isClanForce, List<Loan> loans, boolean stackPenalties, int manualModifier, int cap) {
        int modeReputation = calculateAverageReputation(personnel,
              isUseAgeEffects,
              isClanForce,
              currentDate);

        int debtModifier = getDebtModifier(loans,
              currentDate,
              stackPenalties);

        return applyReputationCap(cap, modeReputation + debtModifier + manualModifier);
    }

    private static void calculateCampaignLevelReputation(PlayerForce playerForce, LocalDate currentDate,
          boolean debtPenaltiesStack, int manualModifier, int cap) {
        int base = playerForce.getChaosCampaignReputation();
        int debtModifier = getDebtModifier(playerForce.getFinances().getLoans(),
              currentDate,
              debtPenaltiesStack);

        int total = applyReputationCap(cap, base + debtModifier + manualModifier);

        playerForce.setChaosCampaignReputation(total);
    }

    public static int applyReputationCap(int cap, int reputation) {
        if (cap != 0) {
            return min(reputation, cap);
        }
        return reputation;
    }

    private static int tabulateReputationFromContracts(Campaign campaign, LocalDate windowStart, LocalDate windowEnd) {
        int reputation = STARTING_REPUTATION_SCORE;

        boolean noPartialSuccessReputation =
              campaign.getCampaignOptions().get(CampaignOption.CHAOS_NO_PARTIAL_SUCCESS_REPUTATION);

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
            } else if (status.isPartialSuccess() && !noPartialSuccessReputation) {
                reputation += CONTRACT_SUCCESS_DELTA;
            } else if (status.isBreach()) {
                int loss = max((int) round(reputation * BREAKING_CONTRACT_MULTIPLIER), BREAKING_CONTRACT_MIN_DELTA);
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
     * Reputation is tabulated over the full contract history. Both are written, so the result is correct whether
     * Reputation is tracked per character or at the campaign level. This is a permanent change.</p>
     *
     * @param campaign the campaign to update
     *
     * @return the force-wide reputation value applied
     */
    public static int applyRetroactiveContractReputation(Campaign campaign) {
        // Carry over any CamOps crime standing as a Chaos criminal record. The adjusted crime rating is negative, so
        // dividing by CRIME_RATING_TO_CRIMINAL_RECORD_DIVIDER yields a (negative) criminal record penalty.
        int adjustedCrimeRating = campaign.getPlayerForce().getAdjustedCrimeRating();
        int criminalRecordFromCrime = (int) floor(adjustedCrimeRating / CRIME_RATING_TO_CRIMINAL_RECORD_DIVIDER);

        for (Person person : campaign.getPlayerForce().getHumanResources().getPersonnel()) {
            LocalDate departure = earliestDate(person.getDateOfDeath(), person.getRetirement());
            person.setReputationDirect(tabulateReputationFromContracts(campaign,
                  person.getRecruitment(),
                  departure));

            if (adjustedCrimeRating != 0) {
                person.setCriminalRecord(criminalRecordFromCrime);
            }
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
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        int debtModifier = getDebtModifier(playerForce.getFinances().getLoans(),
              campaign.getLocalDate(),
              campaignOptions.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK));
        int manualModifier = campaignOptions.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
        int cap = campaignOptions.get(CampaignOption.CHAOS_REPUTATION_CAP);
        int total = applyReputationCap(cap, base + debtModifier + manualModifier);

        return getFormattedTextAt(RESOURCE_BUNDLE, "campaignLevel.tooltip",
              Integer.toString(base),
              Integer.toString(debtModifier),
              Integer.toString(manualModifier),
              Integer.toString(total));
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

    private static void updatePersonnelForContractSuccess(List<Person> personnel) {
        for (Person person : personnel) {
            if (person.isEmployed()) {
                person.changeReputation(CONTRACT_SUCCESS_DELTA);
            }
        }
    }

    private static void updatePersonnelForContractBreak(List<Person> personnel) {
        for (Person person : personnel) {
            if (person.isEmployed()) {
                int baseReputation = person.getReputationDirect();
                int delta = getContractBreakDelta(baseReputation);
                person.changeReputation(-delta);
            }
        }
    }

    private static int getContractBreakDelta(int baseReputation) {
        int delta = (int) round(baseReputation * BREAKING_CONTRACT_MULTIPLIER);
        delta = max(delta, BREAKING_CONTRACT_MIN_DELTA);
        return -delta;
    }

    public static void processContractCompletion(Campaign campaign, MissionStatus status, List<Person> personnel) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean useChaosReputation = campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION);
        boolean isCampaignLevelReputation = campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION);
        if (!useChaosReputation && !isCampaignLevelReputation) {
            return;
        }

        boolean rewardsReputation = !campaignOptions.get(CampaignOption.CHAOS_NO_PARTIAL_SUCCESS_REPUTATION) ?
                                          status.isOverallSuccess() :
                                          status.isSuccess();
        boolean penalizesReputation = status.isBreach();

        if (!rewardsReputation && !penalizesReputation) {
            return;
        }

        PlayerForce playerForce = campaign.getPlayerForce();
        int base = playerForce.getChaosCampaignReputation();
        if (rewardsReputation) {
            if (isCampaignLevelReputation) {
                playerForce.changeChaosCampaignReputation(CONTRACT_SUCCESS_DELTA);
            } else {
                updatePersonnelForContractSuccess(personnel);
            }

            String report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.contractSuccess",
                  spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG, CONTRACT_SUCCESS_DELTA);
            campaign.addReport(DailyReportType.GENERAL, report);
        } else {
            if (isCampaignLevelReputation) {
                playerForce.changeChaosCampaignReputation(getContractBreakDelta(base));
            } else {
                updatePersonnelForContractBreak(personnel);
            }

            String report = getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.brokenContract",
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG, BREAKING_CONTRACT_MIN_DELTA);
            campaign.addReport(DailyReportType.GENERAL, report);
        }

        processChaosCampaignReputationChanges(campaignOptions, playerForce, campaign.getLocalDate());
        campaign.getGUI().refreshAllTabs();
    }

    public static void resolveActOfPiracy(Campaign campaign, List<Person> personnel, int scale,
          List<Scenario> scenarios, boolean actWasSuccessful, String contractName) {
        int roll = d6(2);
        int targetNumber = actWasSuccessful ? PIRACY_AVOIDANCE_TN_SUCCESS : PIRACY_AVOIDANCE_TN_FAILURE;
        boolean gotCaught = roll < targetNumber;

        int supportPointsLoot = determinePiracySP(scale, scenarios);
        Money booty = ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(supportPointsLoot);
        creditFinancesForBooty(campaign, contractName, booty);

        int delta = (int) -ceil(supportPointsLoot / PIRACY_PENALTY_PROFIT_DIVIDER);

        if (gotCaught) {
            if (campaign.getCampaignOptions().get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)) {
                campaign.getPlayerForce().changeChaosCampaignReputation(delta);
            } else {
                updatePersonnelForActOfPiracy(personnel, delta);
            }
        }

        triggerPiracyDialog(campaign, roll, targetNumber, gotCaught, delta, booty);
    }

    private static void creditFinancesForBooty(Campaign campaign, String contractName, Money booty) {
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.THEFT,
                    campaign.getLocalDate(),
                    booty,
                    getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.piracy.booty", contractName));
    }

    private static void triggerPiracyDialog(Campaign campaign, int roll, int targetNumber, boolean gotCaught, int delta,
          Money booty) {
        PlayerForce playerForce = campaign.getPlayerForce();
        ForceHumanResources forceHumanResources = playerForce.getHumanResources();
        boolean isClanForce = playerForce.isClanForce();
        Person seniorAdmin = forceHumanResources.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND,
              campaign.getCampaignOptions(),
              isClanForce,
              campaign.getLocalDate());

        String captureKey = gotCaught ? "caught" : "notCaught";
        String reportKey = "ChaosReputation.piracy.dialog." + captureKey;
        String addendum = getTextAt(RESOURCE_BUNDLE, reportKey);

        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "ChaosReputation.piracy.dialog.ic",
              campaign.getCommanderAddress(),
              booty.toAmountString(),
              addendum);

        new ImmersiveDialogSimple(campaign,
              seniorAdmin,
              inCharacterMessage,
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.piracy.dialog.ooc", roll, targetNumber, -delta));
    }

    private static int determinePiracySP(int scale, List<Scenario> scenarios) {
        int roll = d6(1);
        int contractLootSP = determineContractLoot(roll, scale);
        int componentLootSP = determineComponentLoot(scenarios, scale);

        return contractLootSP + componentLootSP;
    }

    private static int determineContractLoot(int roll, int scale) {
        int base = switch (roll) {
            case 1 -> 0;
            case 2 -> 375;
            case 3 -> 750;
            case 4 -> 1250;
            case 5 -> 1500;
            case 6 -> 1650;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };

        return base * scale;
    }

    private static int determineComponentLoot(List<Scenario> scenarios, int scale) {
        int runningTotal = 0;
        for (Scenario scenario : scenarios) {
            if (scenario.getStatus().isVictory()) {
                runningTotal++;
            }
        }

        int value = switch (runningTotal) {
            case 0 -> 0;
            case 1 -> 250;
            case 2 -> 300;
            case 3 -> 400;
            case 4 -> 500;
            case 5 -> 550;
            case 6 -> 600;
            default -> 650;
        };

        return value * scale;
    }

    private static void updatePersonnelForActOfPiracy(List<Person> personnel, int delta) {
        for (Person person : personnel) {
            if (person.isEmployed()) {
                person.changeCriminalRecord(delta);
            }
        }
    }

    public static int getExperienceLevel(Campaign campaign, Person person, boolean isPrimary) {
        PersonnelRole role = isPrimary ? person.getPrimaryRole() : person.getSecondaryRole();

        if (!role.isCivilian()) {
            return person.getExperienceLevel(campaign, !isPrimary, true);
        }

        return EXP_NONE;
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
}
