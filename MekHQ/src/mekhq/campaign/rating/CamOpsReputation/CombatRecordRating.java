/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.rating.CamOpsReputation;

import static mekhq.campaign.campaignOptions.CampaignOptions.REPUTATION_PERFORMANCE_CUT_OFF_YEARS;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;

public class CombatRecordRating {
    private static final MMLogger logger = MMLogger.create(CombatRecordRating.class);

    /**
     * Calculates the combat record rating for the provided campaign.
     *
     * @param campaign the campaign for which to calculate the combat record rating
     *
     * @return a map containing the combat record ratings: - "partialSuccesses": the number of missions with status
     *       "PARTIAL" - "successes": the number of missions with status "SUCCESS" - "failures": the number of missions
     *       with status "FAILED" - "contractsBreached": the number of missions with status "BREACH" -
     *       "retainerDuration": the duration of the campaign's retainer (in years), zero if there is no retainer -
     *       "total": the total combat record rating calculated using the formula: (successes * 5) - (failures * 10) -
     *       (contractBreaches * 25)
     */
    protected static Map<String, Integer> calculateCombatRecordRating(Campaign campaign) {
        Map<String, Integer> combatRecord = new HashMap<>();

        // If the faction is pirate, set all values to zero and return the map
        // immediately,
        // CamOps says pirates don't track combat record rating, but we still want these
        // values for use elsewhere
        if (campaign.getFaction().isPirate()) {
            combatRecord.put("partialSuccesses", 0);
            combatRecord.put("successes", 0);
            combatRecord.put("failures", 0);
            combatRecord.put("contractsBreached", 0);
            combatRecord.put("retainerDuration", 0);
            combatRecord.put("total", 0);
            return combatRecord;
        }

        // Construct a map with mission statuses and their counts
        boolean usePerformanceCutOff = campaign.getCampaignOptions().isReputationPerformanceModifierCutOff();
        LocalDate cutOffDate = campaign.getLocalDate().minusYears(REPUTATION_PERFORMANCE_CUT_OFF_YEARS);
        Map<MissionStatus, Long> missionCountsByStatus = new HashMap<>();
        for (Mission mission : campaign.getCompletedMissions()) {
            if (mission.getStatus() == MissionStatus.ACTIVE) {
                continue;
            }

            if (usePerformanceCutOff) {
                if (mission instanceof AtBContract) {
                    if (((AtBContract) mission).getEndingDate().isBefore(cutOffDate)) {
                        continue;
                    }
                }
            }

            missionCountsByStatus.put(mission.getStatus(),
                  missionCountsByStatus.getOrDefault(mission.getStatus(), 0L) + 1);
        }

        // Assign mission counts to each category
        int successes = missionCountsByStatus.getOrDefault(MissionStatus.SUCCESS, 0L).intValue();
        int partialSuccesses = missionCountsByStatus.getOrDefault(MissionStatus.PARTIAL, 0L).intValue();
        int failures = missionCountsByStatus.getOrDefault(MissionStatus.FAILED, 0L).intValue();
        int contractBreaches = missionCountsByStatus.getOrDefault(MissionStatus.BREACH, 0L).intValue();

        // place the values into the map
        combatRecord.put("partialSuccesses", partialSuccesses);
        combatRecord.put("successes", successes);
        combatRecord.put("failures", failures);
        combatRecord.put("contractsBreached", contractBreaches);

        // Calculate combat record rating
        boolean usePerformanceModifierReduction = campaign.getCampaignOptions().isReduceReputationPerformanceModifier();
        int successMultiplier = usePerformanceModifierReduction ? 1 : 5;
        int failureMultiplier = usePerformanceModifierReduction ? 2 : 10;
        int breachMultiplier = usePerformanceModifierReduction ? 5 : 25;

        int combatRecordRating = (successes * successMultiplier)
                                       - (failures * failureMultiplier)
                                       - (contractBreaches * breachMultiplier);

        // if the campaign has a retainer, check retainer duration
        if (campaign.getRetainerStartDate() != null) {
            int retainerDuration = (int) ChronoUnit.YEARS.between(campaign.getRetainerStartDate(),
                  campaign.getLocalDate());
            combatRecord.put("retainerDuration", retainerDuration);
            combatRecordRating += retainerDuration * 5;
        } else {
            combatRecord.put("retainerDuration", 0);
        }

        // add the total rating to the map
        combatRecord.put("total", combatRecordRating);

        // post a log to aid debugging
        logger.debug("Combat Record Rating = {}",
              combatRecord.keySet().stream()
                    .map(key -> String.format("%s: %d", key, combatRecord.get(key)))
                    .collect(Collectors.joining("\n")));

        // return the completed map
        return combatRecord;
    }
}
