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
package mekhq.campaign.digitalGM.stratCon;

import static java.lang.Math.max;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractGeneration;
import mekhq.campaign.mission.contract.contractGeneration.TrackIntensityTable;

/**
 * Schedules a StratCon contract's ordinary scenarios, and rolls the contract's whole pre-rolled schedule again when the
 * contract is edited.
 *
 * <p>Ordinary scenarios are scheduled the way Essential scenarios are: up front, by rolling the Track Intensity Tables
 * for the contract's track count (see {@link TrackIntensityTable#rollSchedule}). The table is rolled once per point of
 * scale when "Multiply Track Intensity by Scale" is on and once otherwise, and three times as often when battlefield
 * support points are factored into scale, since that makes for smaller scales. The rolls are then multiplied by the
 * contract type's operations tempo (see {@link ContractObjectiveType#getOperationsTempoMultiplier()}). Where
 * Essential scenarios read the table's columns as months, ordinary scenarios read them as weeks, rolling the
 * six-column table afresh for each six weeks of the contract. The "Fewer Weekly Scenarios" option reads them as months
 * instead, as Essential scenarios do.</p>
 *
 * <p>Single Drop play keeps its own pace of one scenario a week, and schedules nothing here.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConScenarioTempo {
    private static final MMLogger LOGGER = MMLogger.create(StratConScenarioTempo.class);

    /** How much more often the table is rolled when battlefield support points are factored into scale. */
    static final int SUPPORT_POINT_SCALE_ROLL_MULTIPLIER = 3;

    /**
     * A contract length long enough to pick the six-column table (see {@link TrackIntensityTable#rollSchedule}), whose
     * columns weekly play reads as weeks, whatever the contract's real length.
     */
    static final int WEEKLY_TABLE_LENGTH = 6;

    private static final int DAYS_PER_WEEK = 7;

    private StratConScenarioTempo() {}

    /**
     * Works out how many times the Track Intensity Tables are rolled for a contract's ordinary scenarios.
     *
     * @param isMultiplyTrackIntensityByScale whether the "Multiply Track Intensity by Scale" option is on
     * @param isFactorSupportPointsIntoScale  whether battlefield support points are factored into scale
     * @param scale                           the contract's scale; a scale below one still rolls as one
     *
     * @return the number of rolls
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getRollCount(boolean isMultiplyTrackIntensityByScale, boolean isFactorSupportPointsIntoScale,
          int scale) {
        int rollCount = isMultiplyTrackIntensityByScale ? max(1, scale) : 1;
        return isFactorSupportPointsIntoScale ? rollCount * SUPPORT_POINT_SCALE_ROLL_MULTIPLIER : rollCount;
    }

    /**
     * Multiplies a roll count by a contract type's operations tempo. The fraction left over becomes one more roll with
     * that chance, so the tempo holds on average even when it is applied to a single roll.
     *
     * @param rollCount       the roll count before the tempo (see {@link #getRollCount})
     * @param tempoMultiplier the contract type's operations tempo multiplier
     *
     * @return the roll count after the tempo; never negative
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int applyTempoMultiplier(int rollCount, double tempoMultiplier) {
        double scaledRolls = max(0.0, rollCount * tempoMultiplier);
        int wholeRolls = (int) Math.floor(scaledRolls);
        double remainder = scaledRolls - wholeRolls;
        return ((remainder > 0) && (Compute.randomFloat() < remainder)) ? wholeRolls + 1 : wholeRolls;
    }

    /**
     * Rolls the days on which a contract's ordinary scenarios appear over its whole run.
     *
     * <p>In weekly play the six-column table is rolled afresh for each six weeks, its columns read as weeks; the
     * contract's final, partial block keeps only the weeks the contract still runs. With {@code isFewerWeeklyScenarios}
     * the table for the contract's length is rolled instead, its columns read as months, and rolled afresh for however
     * many months the table falls short of the contract. Each scenario gets a random day within its week or month, cut
     * off at the contract's end.</p>
     *
     * @param startDate              the contract's start date
     * @param endDate                the contract's end date
     * @param lengthInMonths         the contract's length in months
     * @param trackCount             the contract's track count, choosing the table column
     * @param rollCount              how many times to roll the table for each block (see {@link #getRollCount})
     * @param tempoMultiplier        the contract type's operations tempo, applied to each block's roll count (see
     *                               {@link #applyTempoMultiplier})
     * @param isFewerWeeklyScenarios whether the "Fewer Weekly Scenarios" option is on
     *
     * @return one day per scenario, in calendar order of their windows
     *
     * @author Illiani
     * @since 0.51.01
     */
    static List<LocalDate> rollScenarioDates(LocalDate startDate, LocalDate endDate, int lengthInMonths,
          int trackCount, int rollCount, double tempoMultiplier, boolean isFewerWeeklyScenarios) {
        List<LocalDate> scenarioDates = new ArrayList<>();
        if (!endDate.isAfter(startDate)) {
            return scenarioDates;
        }

        int windowCount = isFewerWeeklyScenarios ?
                                max(1, lengthInMonths) :
                                (int) Math.ceil(ChronoUnit.DAYS.between(startDate, endDate) / (double) DAYS_PER_WEEK);
        int tableLength = isFewerWeeklyScenarios ? lengthInMonths : WEEKLY_TABLE_LENGTH;

        int window = 0;
        while (window < windowCount) {
            List<Integer> schedule = TrackIntensityTable.rollSchedule(tableLength,
                  trackCount,
                  applyTempoMultiplier(rollCount, tempoMultiplier));
            if (schedule.isEmpty()) {
                break;
            }

            for (int scenarioCount : schedule) {
                if (window >= windowCount) {
                    break;
                }

                LocalDate windowStart = isFewerWeeklyScenarios ?
                                              startDate.plusMonths(window) :
                                              startDate.plusDays((long) window * DAYS_PER_WEEK);
                LocalDate windowEnd = isFewerWeeklyScenarios ?
                                            startDate.plusMonths(window + 1L) :
                                            windowStart.plusDays(DAYS_PER_WEEK);
                if (windowEnd.isAfter(endDate)) {
                    windowEnd = endDate;
                }
                int windowDays = max(1, (int) ChronoUnit.DAYS.between(windowStart, windowEnd));

                for (int scenario = 0; scenario < scenarioCount; scenario++) {
                    scenarioDates.add(windowStart.plusDays(Compute.randomInt(windowDays)));
                }
                window++;
            }
        }

        return scenarioDates;
    }

    /**
     * Schedules a contract's ordinary scenarios from the given day to the contract's end, replacing any scheduled from
     * that day on. The days are stored on the campaign state, where the daily StratCon lifecycle generates the
     * scenarios due each day. Does nothing without a settled start date.
     *
     * @param campaign      the current campaign
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state
     * @param fromDate      the first day to schedule on; the contract's start date when it is accepted
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void scheduleNormalScenarios(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState, @Nullable LocalDate fromDate) {
        LocalDate startDate = contract.getStartDate();
        if (startDate == null) {
            LOGGER.warn("Contract {} has no start date, so its scenarios cannot be scheduled.", contract.getName());
            return;
        }

        LocalDate endDate = contract.getEndingDate();
        if (endDate == null) {
            endDate = startDate.plusMonths(max(1, contract.getLengthInMonths()));
        }
        LocalDate firstDate = ((fromDate == null) || fromDate.isBefore(startDate)) ? startDate : fromDate;

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        int rollCount = getRollCount(campaignOptions.get(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE),
              campaignOptions.get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION),
              contract.getScale());
        ContractObjectiveType objectiveType = contract.getObjectiveType();
        double tempoMultiplier = (objectiveType == null) ? 1.0 : objectiveType.getOperationsTempoMultiplier();
        List<LocalDate> scenarioDates = rollScenarioDates(startDate, endDate, contract.getLengthInMonths(),
              contract.getTrackCount(), rollCount, tempoMultiplier,
              campaignOptions.get(CampaignOption.FEWER_WEEKLY_SCENARIOS));

        List<LocalDate> scheduledDates = campaignState.getWeeklyScenarios();
        scheduledDates.removeIf(scenarioDate -> !scenarioDate.isBefore(firstDate));
        int scheduledCount = 0;
        for (LocalDate scenarioDate : scenarioDates) {
            if (!scenarioDate.isBefore(firstDate)) {
                campaignState.addWeeklyScenario(scenarioDate);
                scheduledCount++;
            }
        }
        campaignState.setNormalTempoScheduled(true);

        LOGGER.info("Scheduled {} StratCon scenarios on contract {} from {} ({} rolls per block, tempo x{}).",
              scheduledCount,
              contract.getName(),
              firstDate,
              rollCount,
              tempoMultiplier);
    }

    /**
     * Rolls a contract's pre-rolled schedule again from today, after an edit to something it was rolled from - the
     * contract's scale, track count, or dates. Everything scheduled from today on is replaced; what has already
     * happened, and anything already overdue, is left alone.
     *
     * <ul>
     *     <li>Ordinary scenarios are scheduled again (see {@link #scheduleNormalScenarios}), except in Single Drop
     *     play, which keeps its own weekly pace.</li>
     *     <li>The contract's Essential scenario schedule is rolled again, as when the contract was generated (see
     *     {@link AbstractContractGeneration#rollScenarioSchedule}), unless the contract's special points of interest
     *     replace its Essential scenarios.</li>
     *     <li>Points of interest are scheduled again (see
     *     {@link StratConContractInitializer#schedulePointsOfInterest(AbstractContract, StratConContractDefinition,
     *     StratConCampaignState, boolean, boolean, LocalDate, int, Map)}). Those already placed, or due and waiting
     *     for room, count against the contract's share, as do the marks they carry (see
     *     {@link StratConCampaignState#recordPlacedPointOfInterest}).</li>
     * </ul>
     *
     * <p>Mapless play has no Essential scenarios or points of interest, so only its ordinary scenarios are rolled
     * again.</p>
     *
     * @param campaign      the current campaign
     * @param contract      the edited contract
     * @param campaignState the contract's StratCon campaign state
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void regenerateSchedules(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState) {
        LocalDate today = campaign.getLocalDate();
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        if (!campaignOptions.isUseStratConSinglesMode()) {
            scheduleNormalScenarios(campaign, contract, campaignState, today);
        }

        if (campaignOptions.isUseStratConMaplessMode()) {
            return;
        }

        boolean isContractsUseSpecialMechanics = campaignState.isContractsUseSpecialMechanics();
        if (!StratConContractInitializer.isReplacingEssentialScenarios(contract, isContractsUseSpecialMechanics)) {
            contract.setScenarioSchedule(AbstractContractGeneration.rollScenarioSchedule(campaign,
                  contract,
                  contract.getLengthInMonths()));
            campaignState.getStrategicScenarioSpawnDates().removeIf(spawnDate -> !spawnDate.isBefore(today));
            StratConContractInitializer.scheduleStrategicScenarioSpawnDates(contract, campaignState, today);
        }

        regeneratePointsOfInterest(campaign, contract, campaignState, today);
    }

    /**
     * Schedules a contract's points of interest again from today (see {@link #regenerateSchedules}).
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void regeneratePointsOfInterest(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState, LocalDate today) {
        StratConContractDefinition contractDefinition =
              StratConContractDefinition.getContractDefinition(contract.getObjectiveType());
        if (contractDefinition == null) {
            LOGGER.warn("Contract {} has no StratCon contract definition, so its points of interest cannot be "
                              + "scheduled again.", contract.getName());
            return;
        }

        campaignState.seedPointOfInterestLedger();

        // Those overdue - due, but still waiting for room in a sector - are kept, and count as already placed.
        int committedCount = campaignState.getPlacedPointOfInterestCount();
        Map<String, Integer> alreadyMarkedCounts = new HashMap<>(campaignState.getMarkedPointOfInterestCounts());
        List<StratConScheduledPointOfInterest> futurePointsOfInterest = new ArrayList<>();
        for (StratConScheduledPointOfInterest scheduledPointOfInterest : campaignState.getScheduledPointsOfInterest()) {
            LocalDate spawnDate = scheduledPointOfInterest.getSpawnDate();
            if ((spawnDate == null) || !spawnDate.isBefore(today)) {
                futurePointsOfInterest.add(scheduledPointOfInterest);
                continue;
            }

            committedCount++;
            for (Map.Entry<String, String> entry : scheduledPointOfInterest.getInitialState().entrySet()) {
                if (Boolean.parseBoolean(entry.getValue())) {
                    alreadyMarkedCounts.merge(entry.getKey(), 1, Integer::sum);
                }
            }
        }
        campaignState.getScheduledPointsOfInterest().removeAll(futurePointsOfInterest);

        StratConContractInitializer.schedulePointsOfInterest(contract,
              contractDefinition,
              campaignState,
              campaign.getCampaignOptions().get(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE),
              campaignState.isContractsUseSpecialMechanics(),
              today,
              committedCount,
              alreadyMarkedCounts);
    }
}
