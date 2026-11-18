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

import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConDeploymentOrderBehavior.isDeploymentOrder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.compute.Compute;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConEnemyFacilityActivity;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityAttacks;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractGeneration;

/**
 * Rolls a StratCon contract's normal scenarios each week, and lays the contract's pre-rolled schedules out again when
 * the contract is edited or extended.
 *
 * <p>Each Monday, the contract rolls {@value StratConSpawnRates#NORMAL_SCENARIO_ROLLS_PER_SCALE} time per point of
 * Scale (rounded normally, never below once). Each roll picks a random sector and rolls against that sector's threat
 * (see {@link StratConRulesManager#calculateScenarioOdds}); a hit brings a normal scenario to that sector on a random day
 * of the week (see {@link #rollWeeklyScenarios}). A peaceful contract's threat is nothing, so it rolls no scenarios.</p>
 *
 * <p>Single Drop play keeps its own pace of one scenario a week. A contract accepted before normal scenarios were
 * rolled each week keeps the schedule it was given up front until it is edited or extended.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConScenarioTempo {
    private static final MMLogger LOGGER = MMLogger.create(StratConScenarioTempo.class);

    /**
     * How many points of a contract's Scale make one when battlefield support points are factored into scale, which
     * makes for smaller scales; used to size facilities (see {@link StratConContractInitializer}).
     */
    static final int SUPPORT_POINT_SCALE_ROLL_MULTIPLIER = 3;

    /** How many days of the week a rolled normal scenario may fall on, counting from the Monday it is rolled. */
    static final int DAYS_PER_WEEK = 7;

    /**
     * How many days late an ordinary scenario's date may be and still bring its scenario. Older dates are discarded, so
     * a long backlog - from a save made before overdue dates were caught up - does not arrive all at once.
     */
    static final int MAXIMUM_SCENARIO_CATCH_UP_DAYS = 7;

    private StratConScenarioTempo() {}

    /**
     * Takes every ordinary scenario date on or before today off the schedule and counts the scenarios now due.
     *
     * <p>A date is due if it falls today, or at most {@link #MAXIMUM_SCENARIO_CATCH_UP_DAYS} days before. That way a
     * date rolled for a day the new day has already processed - by editing or extending the contract - still brings
     * its scenario the next day, instead of sitting on the schedule forever. Older dates are discarded.</p>
     *
     * @param scheduledDates the contract's scheduled ordinary scenario dates; due and stale dates are removed from it
     * @param today          the current campaign date
     *
     * @return how many scenarios are due today
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int takeDueScenarioDates(List<LocalDate> scheduledDates, LocalDate today) {
        LocalDate oldestDueDate = today.minusDays(MAXIMUM_SCENARIO_CATCH_UP_DAYS);
        int dueCount = 0;
        int staleCount = 0;
        for (LocalDate scheduledDate : scheduledDates) {
            if (scheduledDate.isAfter(today)) {
                continue;
            }

            if (scheduledDate.isBefore(oldestDueDate)) {
                staleCount++;
            } else {
                dueCount++;
            }
        }

        if ((dueCount + staleCount) > 0) {
            scheduledDates.removeIf(scheduledDate -> !scheduledDate.isAfter(today));
        }

        if (staleCount > 0) {
            LOGGER.info("Discarded {} StratCon scenario date(s) more than {} days overdue.",
                  staleCount,
                  MAXIMUM_SCENARIO_CATCH_UP_DAYS);
        }

        return dueCount;
    }

    /**
     * Takes every scheduled normal scenario due on or before today off the schedule, as
     * {@link #takeDueScenarioDates} does for dates alone.
     *
     * @param scheduledScenarios the contract's scheduled normal scenarios; due and stale ones are removed from it
     * @param today              the current campaign date
     *
     * @return the scenarios due today, at most {@link #MAXIMUM_SCENARIO_CATCH_UP_DAYS} days late
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<StratConScheduledScenario> takeDueScheduledScenarios(
          List<StratConScheduledScenario> scheduledScenarios, LocalDate today) {
        LocalDate oldestDueDate = today.minusDays(MAXIMUM_SCENARIO_CATCH_UP_DAYS);
        List<StratConScheduledScenario> dueScenarios = new ArrayList<>();
        int staleCount = 0;
        for (StratConScheduledScenario scheduledScenario : scheduledScenarios) {
            LocalDate date = scheduledScenario.getDate();
            if ((date != null) && date.isAfter(today)) {
                continue;
            }

            if ((date == null) || date.isBefore(oldestDueDate)) {
                staleCount++;
            } else {
                dueScenarios.add(scheduledScenario);
            }
        }

        scheduledScenarios.removeIf(scheduledScenario -> (scheduledScenario.getDate() == null)
                                                               || !scheduledScenario.getDate().isAfter(today));

        if (staleCount > 0) {
            LOGGER.info("Discarded {} scheduled StratCon scenario(s) more than {} days overdue.",
                  staleCount,
                  MAXIMUM_SCENARIO_CATCH_UP_DAYS);
        }

        return dueScenarios;
    }

    /**
     * Makes the week's normal scenario rolls for a contract: {@value StratConSpawnRates#NORMAL_SCENARIO_ROLLS_PER_SCALE}
     * per point of Scale, rounded normally, never below one. Each roll picks a random sector and rolls d100 against its
     * threat - the chance shown on the sector's threat bar (see {@link StratConRulesManager#calculateScenarioOdds}). A
     * hit schedules a normal scenario in that sector on a random day of the coming week, starting today. One that would
     * fall on or after the contract's last day is not scheduled.
     *
     * @param campaign      the current campaign
     * @param contract      the contract
     * @param campaignState the contract's StratCon campaign state
     * @param today         the day of the rolls, a Monday
     *
     * @return how many normal scenarios were scheduled
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int rollWeeklyScenarios(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState, LocalDate today) {
        List<StratConTrackState> tracks = campaignState.getTracks();
        if (tracks.isEmpty()) {
            return 0;
        }

        LocalDate contractEnd = contract.getEndingDate();
        int rollCount = StratConSpawnRates.getCount(contract, StratConSpawnRates.NORMAL_SCENARIO_ROLLS_PER_SCALE);
        int scheduledCount = 0;
        for (int roll = 0; roll < rollCount; roll++) {
            int sectorIndex = Compute.randomInt(tracks.size());
            int threat = StratConRulesManager.calculateScenarioOdds(tracks.get(sectorIndex), contract, true);
            if (!isThreatRollHit(Compute.randomInt(100), threat)) {
                continue;
            }

            LocalDate date = today.plusDays(Compute.randomInt(DAYS_PER_WEEK));
            if ((contractEnd != null) && !date.isBefore(contractEnd)) {
                continue;
            }

            campaignState.addScheduledScenario(new StratConScheduledScenario(date, sectorIndex));
            scheduledCount++;
        }

        if (scheduledCount > 0) {
            LOGGER.info("Weekly rolls scheduled {} StratCon scenario(s) on contract {}.", scheduledCount,
                  contract.getName());
        }
        return scheduledCount;
    }

    /**
     * @param percentile a d100 roll, from 0 to 99
     * @param threat     the sector's threat, as a percentage; below zero (a peaceful contract) never hits
     *
     * @return {@code true} if the roll brings a scenario: exactly {@code threat} in 100 of them do
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isThreatRollHit(int percentile, int threat) {
        return percentile < threat;
    }

    /**
     * Lays a contract's pre-rolled schedules out again from today, after an edit to something they were laid out from -
     * the contract's Scale, length, or dates - or an emergency extension. Everything scheduled from today on is
     * replaced; what has already happened, and anything already overdue, is left alone.
     *
     * <ul>
     *     <li>A contract still on the up-front schedule of normal scenarios it was given before they were rolled each
     *     week drops what remains of it and moves onto the weekly rolls (see {@link #rollWeeklyScenarios}), except in
     *     Single Drop play, which keeps its own weekly pace.</li>
     *     <li>The contract's Essential scenarios are laid out again (see
     *     {@link AbstractContractGeneration#rollScenarioSchedule}), if it fights any (see
     *     {@link StratConContractInitializer#usesEssentialScenarios}).</li>
     *     <li>Points of interest are scheduled again (see
     *     {@link StratConContractInitializer#schedulePointsOfInterest(AbstractContract, StratConContractDefinition,
     *     StratConCampaignState, boolean, LocalDate, int, Map)}). Those already placed, or due and waiting for room,
     *     count against the contract's share, as do the marks they carry (see
     *     {@link StratConCampaignState#recordPlacedPointOfInterest}).</li>
     *     <li>Enemy engineers and facility attacks are scheduled again (see
     *     {@link StratConEnemyFacilityActivity#scheduleEnemyEngineers} and
     *     {@link StratConFacilityAttacks#scheduleFacilityAttacks}).</li>
     * </ul>
     *
     * <p>Mapless play has no Essential scenarios, points of interest, or facilities, so only its normal scenarios are
     * affected.</p>
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

        if (!campaignOptions.isUseStratConSinglesMode() && campaignState.isNormalTempoScheduled()) {
            campaignState.getScheduledScenarioDates().removeIf(scenarioDate -> !scenarioDate.isBefore(today));
            campaignState.setNormalTempoScheduled(false);
        }

        // Weekly rolls already made stand, unless the contract now ends before them.
        LocalDate contractEnd = contract.getEndingDate();
        if (contractEnd != null) {
            campaignState.getScheduledScenarios().removeIf(scheduledScenario ->
                                                                 (scheduledScenario.getDate() != null)
                                                                       && !scheduledScenario.getDate()
                                                                                 .isBefore(contractEnd));
        }

        if (campaignOptions.isUseStratConMaplessMode()) {
            return;
        }

        boolean isContractsUseSpecialMechanics = campaignState.isContractsUseSpecialMechanics();
        if (StratConContractInitializer.usesEssentialScenarios(contract, isContractsUseSpecialMechanics)) {
            contract.setScenarioSchedule(AbstractContractGeneration.rollScenarioSchedule(campaign,
                  contract,
                  contract.getLengthInMonths()));
            campaignState.getStrategicScenarioSpawnDates().removeIf(spawnDate -> !spawnDate.isBefore(today));
            StratConContractInitializer.scheduleStrategicScenarioSpawnDates(contract, campaignState, today);
        }

        regeneratePointsOfInterest(campaign, contract, campaignState, today);

        StratConEnemyFacilityActivity.scheduleEnemyEngineers(contract, campaignState, today);
        StratConFacilityAttacks.scheduleFacilityAttacks(contract, campaignState, today);
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

        // Only objectives are a fixed number, so only they count against the definition's share; ordinary points of
        // interest are rolled again. Those overdue - due, but still waiting for room in a sector - are kept, and count
        // as already placed.
        int committedObjectiveCount = countPlacedPointOfInterestObjectives(campaignState);
        Map<String, Integer> alreadyMarkedCounts = new HashMap<>(campaignState.getMarkedPointOfInterestCounts());
        List<StratConScheduledPointOfInterest> futurePointsOfInterest = new ArrayList<>();
        for (StratConScheduledPointOfInterest scheduledPointOfInterest : campaignState.getScheduledPointsOfInterest()) {
            LocalDate spawnDate = scheduledPointOfInterest.getSpawnDate();
            if ((spawnDate == null) || !spawnDate.isBefore(today)) {
                futurePointsOfInterest.add(scheduledPointOfInterest);
                continue;
            }

            // Deployment Orders are rolled again whole, so they never count against the definition's share.
            if (scheduledPointOfInterest.isStrategicObjective()
                      && !isDeploymentOrder(scheduledPointOfInterest.getTypeId())) {
                committedObjectiveCount++;
            }
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
              campaignState.isContractsUseSpecialMechanics(),
              today,
              committedObjectiveCount,
              alreadyMarkedCounts);
    }

    /**
     * Counts the point of interest objectives on the contract's map, met or not. One that was withdrawn has lost its
     * objective too, so it is not counted; nor is a Deployment Order, which is not one of the definition's.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static int countPlacedPointOfInterestObjectives(StratConCampaignState campaignState) {
        int count = 0;
        for (StratConTrackState track : campaignState.getTracks()) {
            for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
                if ((objective.getObjectiveType() == StrategicObjectiveType.PointOfInterest)
                          && !isDeploymentOrder(objective.getPointOfInterestTypeId())) {
                    count++;
                }
            }
        }
        return count;
    }
}
