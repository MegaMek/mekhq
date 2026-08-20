/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.reputation.chaosReputation;

import static java.lang.Math.ceil;
import static java.lang.Math.clamp;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.common.compute.Compute.d6;
import static megamek.common.options.OptionsConstants.ATOW_COMBAT_PARALYSIS;
import static megamek.common.options.OptionsConstants.ATOW_COMBAT_SENSE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_HEROIC;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ULTRA_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.MAX_CRIME_PENALTY;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
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

import jakarta.annotation.Nullable;
import megamek.common.enums.SkillLevel;
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
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Static helpers implementing the Chaos Campaign reputation system (Hot Spots: Draconis Reach).
 *
 * <p>Reputation can be tracked either per character - where the force's value is the average of its personnel - or as
 * a single campaign-wide value stored on the {@link PlayerForce}. These helpers cover recalculating the force value,
 * applying the configured cap, deriving the debt penalty from outstanding loans, applying reputation changes when a
 * contract is completed or an act of piracy is resolved, retroactively initializing reputation from past contracts, and
 * computing the force's average experience level.</p>
 */
public class ChaosReputation {
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

    /**
     * Recalculates and stores the force's Chaos Reputation according to the active tracking mode.
     *
     * <p>When {@link CampaignOption#CAMPAIGN_LEVEL_CHAOS_REPUTATION} is enabled this is a no-op: the campaign-wide
     * value is a pure base that only changes through events, and the debt, manual, and commander modifiers are applied
     * live when it is displayed (see {@link #getEffectiveCampaignLevelReputation}). Otherwise the value is re-derived
     * from the force's personnel, applying the configured debt-stacking, manual-modifier, and cap options.</p>
     *
     * @param campaignOptions the campaign options driving the calculation
     * @param playerForce     the force whose reputation is recalculated and stored
     * @param today           the current date, used for loan-age (debt) calculations
     */
    public static void processChaosCampaignReputationChanges(CampaignOptions campaignOptions, PlayerForce playerForce,
          LocalDate today) {
        if (campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)) {
            // The stored campaign-level value is a pure base that only changes through events (contract outcomes, acts
            // of piracy, prisoner executions, random events). The dynamic debt penalty, manual modifier, and commander
            // SPA modifiers are applied live wherever the value is displayed. Re-deriving and storing them here would
            // fold those modifiers back into the base and compound them on every daily update.
            return;
        }

        boolean debtPenaltiesStack = campaignOptions.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK);
        int manualModifier = campaignOptions.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
        int cap = campaignOptions.get(CampaignOption.CHAOS_REPUTATION_CAP);
        Person commander = playerForce.getHumanResources().getCommander(campaignOptions, playerForce.isClanForce(),
              today);

        boolean applyPersonality = campaignOptions.get(CampaignOption.CHAOS_PERSONALITY_AFFECTS_REPUTATION) &&
                                         campaignOptions.get(CampaignOption.USE_RANDOM_PERSONALITIES);
        ChaosReputation.calculatePersonnelLevelReputation(playerForce,
              today,
              debtPenaltiesStack,
              manualModifier,
              cap,
              campaignOptions.get(CampaignOption.USE_AGE_EFFECTS),
              commander,
              applyPersonality);
    }

    /**
     * Derives the force reputation from its personnel (the average adjusted reputation plus debt and manual modifiers,
     * capped) and stores it on the force.
     *
     * @param playerForce        the force whose personnel are averaged and whose reputation is stored
     * @param currentDate        the current date, used for age effects and loan-age calculations
     * @param debtPenaltiesStack whether each loan contributes its own debt penalty
     * @param manualModifier     the manual reputation modifier to add
     * @param cap                the reputation cap ({@code 0} for none)
     * @param useAgeEffects      whether age effects apply to adjusted reputation
     * @param commander          the campaign commander, whose SPAs may modify the debt penalty (may be {@code null})
     * @param applyPersonality   whether each character's personality value is added to their reputation
     */
    private static void calculatePersonnelLevelReputation(PlayerForce playerForce, LocalDate currentDate,
          boolean debtPenaltiesStack, int manualModifier, int cap, boolean useAgeEffects, Person commander,
          boolean applyPersonality) {
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
              cap,
              commander,
              applyPersonality);

        playerForce.setChaosCampaignReputation(total);
    }

    /**
     * Combines the personnel average reputation with the debt penalty and manual modifier, then applies the cap.
     *
     * @param personnel        the personnel to average
     * @param currentDate      the current date, used for age effects and loan-age calculations
     * @param isUseAgeEffects  whether age effects apply to adjusted reputation
     * @param isClanForce      whether the force is a Clan force
     * @param loans            the outstanding loans used to compute the debt penalty
     * @param stackPenalties   whether each loan contributes its own debt penalty
     * @param manualModifier   the manual reputation modifier to add
     * @param cap              the reputation cap ({@code 0} for none)
     * @param commander        the campaign commander, whose SPAs may modify the debt penalty (may be {@code null})
     * @param applyPersonality whether each character's personality value is added to their reputation
     *
     * @return the capped total reputation
     */
    private static int processReputation(Collection<Person> personnel, LocalDate currentDate, boolean isUseAgeEffects,
          boolean isClanForce, List<Loan> loans, boolean stackPenalties, int manualModifier, int cap,
          Person commander, boolean applyPersonality) {
        int modeReputation = calculateAverageReputation(personnel,
              isUseAgeEffects,
              isClanForce,
              currentDate,
              applyPersonality);

        int debtModifier = applyCommanderDebtModifier(getDebtModifier(loans, currentDate, stackPenalties), commander);
        int otherModifiers = getOtherModifiers(commander);

        return applyReputationCap(cap, modeReputation + debtModifier + otherModifiers + manualModifier);
    }

    public static int getOtherModifiers(Person commander) {
        if (commander == null) {
            return 0;
        }

        int otherModifiers = 0;
        PersonnelOptions options = commander.getOptions();
        boolean hasCombatSense = options.booleanOption(ATOW_COMBAT_SENSE);
        if (hasCombatSense) {
            otherModifiers++;
        }

        boolean hasCombatParalysis = options.booleanOption(ATOW_COMBAT_PARALYSIS);
        if (hasCombatParalysis) {
            otherModifiers--;
        }

        boolean isImpressiveLeader = options.booleanOption(PersonnelOptions.IMPRESSIVE_LEADER);
        if (isImpressiveLeader) {
            otherModifiers++;
        }

        boolean isDisappointingLeader = options.booleanOption(PersonnelOptions.DISAPPOINTING_LEADER);
        if (isDisappointingLeader) {
            otherModifiers--;
        }
        return otherModifiers;
    }

    /**
     * Computes the effective campaign-wide Chaos Reputation for display: the stored (pure) base value plus the current
     * debt penalty, commander SPA modifiers, and manual modifier, limited by the configured cap.
     *
     * <p>This is a read-only calculation. Unlike the personnel-level path, the stored base is never overwritten here,
     * so the dynamic modifiers are re-derived on every read rather than compounded into the stored value. It is the
     * campaign-level counterpart to {@link #processReputation} and is the single source of truth for the value shown in
     * the unit-rating text, the report header, and {@link #getCampaignLevelTooltip}.</p>
     *
     * @param playerForce     the force whose stored base reputation is read
     * @param campaignOptions the campaign options driving the modifiers and cap
     * @param currentDate     the current date, used for loan-age (debt) calculations
     *
     * @return the capped effective reputation
     */
    public static int getEffectiveCampaignLevelReputation(PlayerForce playerForce, CampaignOptions campaignOptions,
          LocalDate currentDate) {
        int base = playerForce.getChaosCampaignReputation();
        Person commander = playerForce.getHumanResources().getCommander(campaignOptions, playerForce.isClanForce(),
              currentDate);
        int debtModifier = applyCommanderDebtModifier(getDebtModifier(playerForce.getFinances().getLoans(),
              currentDate,
              campaignOptions.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK)), commander);
        int otherModifiers = getOtherModifiers(commander);
        int manualModifier = campaignOptions.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
        int cap = campaignOptions.get(CampaignOption.CHAOS_REPUTATION_CAP);

        return applyReputationCap(cap, base + debtModifier + otherModifiers + manualModifier);
    }

    /**
     * Applies the campaign commander's debt-related SPAs to a debt penalty.
     *
     * <p>"Silver Tongue" halves the (negative) debt penalty, while "Loan Shark Victim" doubles it. Only the campaign
     * commander's SPAs matter; other personnel have no effect.</p>
     *
     * @param debtModifier the base debt penalty (non-positive)
     * @param commander    the campaign commander, or {@code null} if there is none
     *
     * @return the adjusted debt penalty
     */
    public static int applyCommanderDebtModifier(int debtModifier, Person commander) {
        if (commander == null) {
            return debtModifier;
        }

        PersonnelOptions options = commander.getOptions();
        if (options.booleanOption(PersonnelOptions.SILVER_TONGUE)) {
            return (int) round(debtModifier * 0.5);
        }
        if (options.booleanOption(PersonnelOptions.LOAN_SHARK_VICTIM)) {
            return (int) round(debtModifier * 2.0);
        }
        return debtModifier;
    }

    /**
     * Limits a reputation value to the configured cap.
     *
     * @param cap        the maximum allowed reputation; {@code 0} disables the cap
     * @param reputation the reputation value to cap
     *
     * @return {@code reputation} unchanged when {@code cap} is {@code 0}, otherwise the lesser of the two
     */
    public static int applyReputationCap(int cap, int reputation) {
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
     * open-ended, and a contract with no recorded ending date is always counted. A success (and, unless
     * {@link CampaignOption#CHAOS_NO_PARTIAL_SUCCESS_REPUTATION} is set, a partial success) raises reputation; a breach
     * halves the running total, losing at least {@link #BREAKING_CONTRACT_MIN_DELTA}.</p>
     *
     * @param campaign    the campaign whose completed contracts to tabulate
     * @param windowStart the earliest ending date to count, or {@code null} for no lower bound
     * @param windowEnd   the latest ending date to count, or {@code null} for no upper bound
     *
     * @return the base reputation earned from contracts within the window
     */
    // Package-private for testing (see ChaosReputationTest); logically private to this class.
    static int tabulateReputationFromContracts(Campaign campaign, LocalDate windowStart, LocalDate windowEnd) {
        int reputation = STARTING_REPUTATION_SCORE;

        boolean noPartialSuccessReputation =
              campaign.getCampaignOptions().get(CampaignOption.CHAOS_NO_PARTIAL_SUCCESS_REPUTATION);

        List<AbstractContract> completedMissions = new ArrayList<>(campaign.getCompletedContracts());
        completedMissions.sort(Comparator.comparing(AbstractContract::getEndingDate,
              Comparator.nullsFirst(Comparator.naturalOrder())));

        for (AbstractContract mission : completedMissions) {
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

    /**
     * Tests whether a date falls within an inclusive window. A {@code null} bound is treated as open-ended, and a
     * {@code null} date is always considered within the window.
     *
     * @param date        the date to test, or {@code null}
     * @param windowStart the earliest allowed date, or {@code null} for no lower bound
     * @param windowEnd   the latest allowed date, or {@code null} for no upper bound
     *
     * @return {@code true} if the date is within the window
     */
    private static boolean isWithinWindow(LocalDate date, LocalDate windowStart, LocalDate windowEnd) {
        if (date == null) {
            return true;
        }
        if (windowStart != null && date.isBefore(windowStart)) {
            return false;
        }
        return windowEnd == null || !date.isAfter(windowEnd);
    }

    /**
     * Returns the earlier of two dates, treating {@code null} as "no date".
     *
     * @param first  the first date, or {@code null}
     * @param second the second date, or {@code null}
     *
     * @return the earlier non-null date, or {@code null} if both are {@code null}
     */
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
     * Reputation is tracked per character or at the campaign level. Any CamOps crime standing is also carried over as a
     * (negative) Chaos criminal record. This is a permanent change.</p>
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

        Collection<Person> personnel = campaign.getPlayerForce().getHumanResources().getPersonnel();
        for (Person person : personnel) {
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
        Person commander = playerForce.getHumanResources().getCommander(campaignOptions, playerForce.isClanForce(),
              campaign.getLocalDate());
        int debtModifier = applyCommanderDebtModifier(getDebtModifier(playerForce.getFinances().getLoans(),
              campaign.getLocalDate(),
              campaignOptions.get(CampaignOption.CHAOS_DEBT_PENALTIES_STACK)), commander);
        int otherModifiers = getOtherModifiers(commander);
        int manualModifier = campaignOptions.get(CampaignOption.MANUAL_UNIT_RATING_MODIFIER);
        int total = getEffectiveCampaignLevelReputation(playerForce, campaignOptions, campaign.getLocalDate());

        return getFormattedTextAt(RESOURCE_BUNDLE, "campaignLevel.tooltip",
              Integer.toString(base),
              Integer.toString(debtModifier),
              Integer.toString(manualModifier),
              Integer.toString(otherModifiers),
              Integer.toString(total));
    }

    /**
     * Computes the mean adjusted reputation across the force's active, employed personnel. Departed, inactive, or
     * unemployed characters are ignored.
     *
     * @param personnel         the personnel to average
     * @param isUseAgingEffects whether age effects apply to adjusted reputation
     * @param isClanCampaign    whether the force is a Clan force
     * @param currentDate       the current date, used for age effects
     * @param applyPersonality  whether each character's personality value is added to their reputation
     *
     * @return the rounded mean adjusted reputation
     */
    private static int calculateAverageReputation(Collection<Person> personnel, boolean isUseAgingEffects,
          boolean isClanCampaign, LocalDate currentDate, boolean applyPersonality) {
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
            if (status.isActive() &&
                      person.isEmployed() &&
                      !person.isCivilian() &&
                      !person.isChild(currentDate, false)) {
                int weight = getPersonnelReputationWeight(person);
                if (weight == 0) {
                    continue;
                }

                personCount += weight;
                int reputation = person.getAdjustedReputation(isUseAgingEffects,
                      isClanCampaign,
                      currentDate,
                      applyPersonality);
                totalReputation += reputation * weight;
            }
        }

        return personnel.isEmpty() ? 0 : (int) round(totalReputation / personCount);
    }

    /**
     * Computes the reputation debt penalty from outstanding loans.
     *
     * <p>When {@code stackPenalties} is {@code true} every outstanding loan contributes its own penalty and they are
     * summed; otherwise only the oldest loan is penalized. Each loan's penalty grows with its age. Returns {@code 0}
     * when there are no loans.</p>
     *
     * @param loans          the outstanding loans
     * @param currentDate    the current date, used to determine each loan's age
     * @param stackPenalties whether each loan contributes its own penalty
     *
     * @return the (non-positive) debt penalty
     */
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

    /**
     * Returns the debt penalty for a single loan of the given age: {@code -(floor(age / frequency) + 1)}.
     *
     * @param ageInMonths the loan's age in months
     *
     * @return the (negative) penalty contributed by the loan
     */
    private static int getLoanDebtPenalty(long ageInMonths) {
        return ((int) floor(ageInMonths / GOING_INT_DEBT_MONTHLY_FREQUENCY) + 1) * GOING_INTO_DEBT_DELTA;
    }

    /**
     * Raises the reputation of every employed character by the contract-success delta.
     *
     * @param personnel the personnel to reward
     */
    private static void updatePersonnelForContractSuccess(List<Person> personnel) {
        for (Person person : personnel) {
            if (person.isEmployed()) {
                person.changeReputation(CONTRACT_SUCCESS_DELTA);
            }
        }
    }

    /**
     * Lowers the reputation of every employed character by the contract-break penalty.
     *
     * @param personnel the personnel to penalize
     */
    private static void updatePersonnelForContractBreak(List<Person> personnel) {
        for (Person person : personnel) {
            if (person.isEmployed()) {
                int baseReputation = person.getReputationDirect();
                int delta = getContractBreakDelta(baseReputation);
                person.changeReputation(delta);
            }
        }
    }

    /**
     * Returns the reputation delta applied when a contract is broken: half the current reputation, but at least
     * {@link #BREAKING_CONTRACT_MIN_DELTA}, expressed as a negative value.
     *
     * @param baseReputation the current reputation value
     *
     * @return the (negative) reputation delta
     */
    private static int getContractBreakDelta(int baseReputation) {
        int delta = (int) round(baseReputation * BREAKING_CONTRACT_MULTIPLIER);
        delta = max(delta, BREAKING_CONTRACT_MIN_DELTA);
        return -delta;
    }

    /**
     * Applies the Chaos Reputation change for a completed contract, based on its outcome, and refreshes the force
     * reputation and UI.
     *
     * <p>Does nothing unless Chaos Reputation is in use. A success (or, unless
     * {@link CampaignOption#CHAOS_NO_PARTIAL_SUCCESS_REPUTATION} is set, a partial success) raises reputation; a breach
     * lowers it; other outcomes have no effect. In campaign-level mode the single stored value is adjusted; otherwise
     * the passed personnel are adjusted individually.</p>
     *
     * @param campaign  the campaign whose reputation to update
     * @param status    the completed contract's final status
     * @param personnel the personnel whose reputation is adjusted in per-character mode
     */
    public static void processContractCompletion(Campaign campaign, MissionStatus status, List<Person> personnel) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean useChaosReputation = campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION);
        if (!useChaosReputation) {
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
        boolean isCampaignLevelReputation = campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION);
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

    /**
     * Resolves an act of piracy: rolls to avoid getting caught, credits the looted booty to the force's finances, and -
     * if caught - applies a criminal-record penalty to the force or its personnel. Finally shows a summary dialog.
     *
     * @param campaign         the campaign performing the piracy
     * @param personnel        the personnel involved (penalized in per-character mode)
     * @param scale            the loot scale factor
     * @param scenarios        the contract's scenarios, used to value component loot
     * @param actWasSuccessful whether the underlying act succeeded (affects the avoidance target number)
     * @param contractName     the contract name, used in the finance and dialog text
     */
    public static void resolveActOfPiracy(Campaign campaign, List<Person> personnel, int scale,
          List<Scenario> scenarios, boolean actWasSuccessful, String contractName) {
        PlayerForce playerForce = campaign.getPlayerForce();
        Person commander = playerForce.getHumanResources().getCommander(campaign.getCampaignOptions(),
              playerForce.isClanForce(),
              campaign.getLocalDate());

        int roll = d6(2);
        int targetNumber = actWasSuccessful ? PIRACY_AVOIDANCE_TN_SUCCESS : PIRACY_AVOIDANCE_TN_FAILURE;
        // A "Slippery" commander makes the force harder to catch; a "Conspicuous" one makes it easier.
        if (commander != null) {
            if (commander.getOptions().booleanOption(PersonnelOptions.SLIPPERY)) {
                targetNumber -= 1;
            } else if (commander.getOptions().booleanOption(PersonnelOptions.CONSPICUOUS)) {
                targetNumber += 1;
            }
        }
        boolean gotCaught = roll < targetNumber;

        int supportPointsLoot = determinePiracySP(scale, scenarios, commander);
        Money booty = ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(supportPointsLoot);
        creditFinancesForBooty(campaign, contractName, booty);

        int delta = (int) -ceil(supportPointsLoot / PIRACY_PENALTY_PROFIT_DIVIDER);

        if (campaign.getCampaignOptions().get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION)) {
            if (gotCaught) {
                campaign.getPlayerForce().changeChaosCampaignReputation(delta);
            }
        } else {
            // Always process personnel: the "Loose Lips" SPA records a criminal record even when the detachment was
            // not caught, so the base delta is 0 for a clean getaway.
            updatePersonnelForActOfPiracy(personnel, gotCaught ? delta : 0);
        }

        triggerPiracyDialog(campaign, roll, targetNumber, gotCaught, delta, booty);
    }

    /**
     * Credits looted booty to the force's finances as a theft transaction.
     *
     * @param campaign     the campaign whose finances are credited
     * @param contractName the contract name shown on the transaction
     * @param booty        the amount to credit
     */
    private static void creditFinancesForBooty(Campaign campaign, String contractName, Money booty) {
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.THEFT,
                    campaign.getLocalDate(),
                    booty,
                    getFormattedTextAt(RESOURCE_BUNDLE, "ChaosReputation.piracy.booty", contractName));
    }

    /**
     * Shows the in-character summary dialog reporting the outcome of an act of piracy.
     *
     * @param campaign     the campaign the dialog belongs to
     * @param roll         the avoidance roll made
     * @param targetNumber the target number the roll was compared against
     * @param gotCaught    whether the force was caught
     * @param delta        the (negative) reputation/criminal-record delta applied when caught
     * @param booty        the looted amount
     */
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

    /**
     * Determines the total support-point value of piracy loot: a random contract-loot roll plus component loot earned
     * from victorious scenarios, each scaled by {@code scale}.
     *
     * @param scale             the loot scale factor
     * @param scenarios         the contract's scenarios
     * @param campaignCommander the campaign commander
     *
     * @return the total looted support points
     */
    private static int determinePiracySP(int scale, List<Scenario> scenarios, @Nullable Person campaignCommander) {
        int roll = d6(1);
        if (campaignCommander != null && campaignCommander.getOptions().booleanOption(PersonnelOptions.LOOT_GOBLIN)) {
            roll = min(6, roll + 1);
        }

        int contractLootSP = determineContractLoot(roll, scale);

        int componentLootSP = determineComponentLoot(scenarios, scale);

        return contractLootSP + componentLootSP;
    }

    /**
     * Returns the contract-loot support points for a d6 roll, scaled by {@code scale}.
     *
     * @param roll  the 1-6 roll
     * @param scale the loot scale factor
     *
     * @return the scaled contract-loot support points
     */
    private static int determineContractLoot(int roll, int scale) {
        // Hot Spots Draconis Reach, first printing, pg 126
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

    /**
     * Returns the component-loot support points earned from the number of victorious scenarios, scaled by
     * {@code scale}.
     *
     * @param scenarios the contract's scenarios
     * @param scale     the loot scale factor
     *
     * @return the scaled component-loot support points
     */
    private static int determineComponentLoot(List<Scenario> scenarios, int scale) {
        int runningTotal = 0;
        for (Scenario scenario : scenarios) {
            if (scenario.getStatus().isVictory()) {
                runningTotal++;
            }
        }

        // Hot Spots Draconis Reach, first printing, pg 126
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

    /**
     * Applies the per-character criminal-record change for an act of piracy.
     *
     * <p>Each employed character receives {@code baseDelta} (the negative penalty when the detachment was caught, or
     * {@code 0} for a clean getaway), adjusted by their SPAs: "Leaves Paper Trail" always records one extra point of
     * criminal record (even on a clean getaway), while "Leaves No Trail" reduces the criminal record gained by one, but
     * never turns a penalty into a bonus.</p>
     *
     * @param personnel the personnel involved in the act of piracy
     * @param baseDelta the base criminal-record change (negative when caught, {@code 0} otherwise)
     */
    private static void updatePersonnelForActOfPiracy(List<Person> personnel, int baseDelta) {
        for (Person person : personnel) {
            if (!person.isEmployed()) {
                continue;
            }

            int criminalRecordDelta = baseDelta;
            PersonnelOptions options = person.getOptions();
            if (options.booleanOption(PersonnelOptions.LOOSE_LIPS)) {
                criminalRecordDelta -= 1;
            } else if (options.booleanOption(PersonnelOptions.LEAVES_NO_TRAIL)) {
                criminalRecordDelta = min(criminalRecordDelta + 1, 0);
            }

            if (criminalRecordDelta != 0) {
                person.changeCriminalRecord(criminalRecordDelta);
            }
        }
    }

    /**
     * Updates the criminal record of employed personnel by a specified delta value. Iterates through the provided list
     * of personnel, and for each person that is currently employed, modifies their criminal record by the delta
     * amount.
     *
     * @param personnel the list of personnel to update
     * @param delta     the amount by which to adjust the criminal record for employed personnel
     */
    public static void updatePersonnelCriminalRecord(List<Person> personnel, int delta) {
        for (Person person : personnel) {
            if (!person.isEmployed()) {
                continue;
            }

            person.changeCriminalRecord(delta);
        }
    }

    /**
     * Calculates the experience level of a person based on various campaign parameters, role-specific adjustments, and
     * personnel options.
     *
     * @param campaignOptions The campaign options that affect experience calculation.
     * @param isClanForce     A boolean indicating if the force being evaluated is a Clan force.
     * @param currentDate     The current date used for calculating experience-related metrics.
     * @param person          The person whose experience level is to be calculated.
     * @param isPrimary       A boolean indicating whether the primary or secondary role of the person should be
     *                        evaluated.
     *
     * @return The calculated experience level of the person, adjusted for specific conditions and constraints.
     */
    public static int getExperienceLevel(CampaignOptions campaignOptions, boolean isClanForce, LocalDate currentDate,
          Person person, boolean isPrimary) {
        PersonnelRole role = isPrimary ? person.getPrimaryRole() : person.getSecondaryRole();

        if (role.isCivilian()) {
            return EXP_NONE;
        }

        int experienceLevel = person.getExperienceLevel(campaignOptions,
              isClanForce,
              currentDate,
              !isPrimary,
              true);

        // "Above Their Weight" / "Looks Good on Paper" shift how experienced this character is counted as, clamped so
        // they are never dropped from the tally (EXP_NONE).
        PersonnelOptions options = person.getOptions();
        if (options.booleanOption(PersonnelOptions.LOOKS_GOOD_ON_PAPER)) {
            experienceLevel = min(experienceLevel + 1, EXP_LEGENDARY);
        } else if (options.booleanOption(PersonnelOptions.UNASSUMING)) {
            experienceLevel = max(experienceLevel - 1, EXP_ULTRA_GREEN);
        }

        return experienceLevel;
    }

    /**
     * Returns how many times a character counts toward the force's average experience and Chaos Reputation.
     *
     * <p>The "Unrecorded Presence" SPA makes the character count {@code 0} times (they are ignored), while the "Big
     * Personality" SPA makes them count twice. Ordinary characters count once. Unrecorded Presence takes precedence if
     * both are somehow present.</p>
     *
     * @param person the character
     *
     * @return {@code 0}, {@code 1}, or {@code 2}
     */
    public static int getPersonnelReputationWeight(Person person) {
        PersonnelOptions options = person.getOptions();
        if (options.booleanOption(PersonnelOptions.REDACTED)) {
            return 0;
        }
        if (options.booleanOption(PersonnelOptions.BIG_PERSONALITY)) {
            return 2;
        }
        return 1;
    }

    /**
     * Calculates the average skill level of the given personnel, considering their employment status, activity, and
     * skill weights. The average is determined based on both primary and secondary experience levels, while accounting
     * for personnel who are departed or have no valid weight.
     *
     * @param campaignOptions The campaign options that provide configuration for determining skill levels.
     * @param isClanForce     A boolean indicating whether the current force is a Clan force.
     * @param currentDate     The current date used to evaluate skill levels and statuses.
     * @param personnel       A collection of Person objects representing the unit's members.
     *
     * @return The average {@link SkillLevel} for the personnel, or a default skill level if no valid personnel are
     *       eligible.
     */
    public static SkillLevel getAverageSkillLevel(CampaignOptions campaignOptions, boolean isClanForce,
          LocalDate currentDate, final Collection<Person> personnel) {
        double personCount = 0;
        int totalExperienceLevel = 0;

        for (Person person : personnel) {
            PersonnelStatus status = person.getStatus();
            if (status.isDepartedUnit()) {
                continue;
            }

            // Is the person active and employed?
            if (status.isActive() && person.isEmployed()) {
                int weight = getPersonnelReputationWeight(person);
                if (weight == 0) {
                    continue;
                }

                int primaryExperienceLevel = getExperienceLevel(campaignOptions,
                      isClanForce,
                      currentDate,
                      person,
                      true);
                if (primaryExperienceLevel != EXP_NONE) {
                    personCount += weight;
                    totalExperienceLevel += primaryExperienceLevel * weight;
                }

                int secondaryExperienceLevel = getExperienceLevel(campaignOptions,
                      isClanForce,
                      currentDate,
                      person,
                      false);
                if (secondaryExperienceLevel != EXP_NONE) {
                    personCount += weight;
                    totalExperienceLevel += secondaryExperienceLevel * weight;
                }
            }
        }

        int meanExperienceLevel = personCount == 0 ? EXP_NONE : (int) round(totalExperienceLevel / personCount);
        meanExperienceLevel = clamp(meanExperienceLevel, EXP_NONE, EXP_LEGENDARY);

        return SkillType.skillLevelFromExperienceLevel(meanExperienceLevel);
    }

    public static void applyStartingReputation(CampaignOptions campaignOptions, boolean isClanForce,
          LocalDate currentDate, Person person) {
        if (person.isCivilian() || person.isChild(currentDate, false)) {
            person.setReputationDirect(0);
            return;
        }

        int skillLevel = person.getExperienceLevel(campaignOptions,
              isClanForce,
              currentDate,
              false,
              true);

        int startingReputation = STARTING_REPUTATION_SCORE + switch (skillLevel) {
            case EXP_NONE, EXP_ULTRA_GREEN, EXP_GREEN -> -1;
            case EXP_VETERAN -> 1;
            case EXP_ELITE -> 2;
            case EXP_HEROIC -> 3;
            case EXP_LEGENDARY -> 4;
            default -> 0; // EXP_REGULAR
        };

        person.setReputationDirect(startingReputation);
    }

    public static void applyStartingCriminalRecord(LocalDate currentDate, Person person) {
        boolean isPirate = person.getOriginFaction().getShortName().equals(PIRATE_FACTION_CODE);
        boolean isChild = person.isChild(currentDate, false);
        boolean isCivilian = person.isCivilian();

        if (!isPirate || isChild || isCivilian) {
            return;
        }

        int roll = -d6(1);
        person.setCriminalRecord(roll);
    }

    public static void changeCrimePenalty(Collection<Person> personnel, int delta) {
        for (Person person : personnel) {
            if (person.isEmployed()) {
                if (delta != 0) {
                    person.changeCriminalRecord(delta);
                }
            }
        }
    }

    public static int getPrisonerExecutionPenalty(PlayerForce playerForce, int prisonerCount,
          boolean crimeNoticed, boolean isUseCampaignReputationTracking) {
        int penalty;
        penalty = (int) -ceil(min(MAX_CRIME_PENALTY, prisonerCount) / 10.0);
        if (crimeNoticed) {
            if (isUseCampaignReputationTracking) {
                playerForce.changeChaosCampaignReputation(penalty);
            } else {
                ChaosReputation.updatePersonnelCriminalRecord(
                      playerForce.getHumanResources().getPersonnelFilteringOutDepartedAndAbsent(),
                      penalty);
            }
        }
        return penalty;
    }
}
