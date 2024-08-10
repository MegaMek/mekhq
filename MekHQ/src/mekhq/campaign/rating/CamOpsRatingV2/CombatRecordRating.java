package mekhq.campaign.rating.CamOpsRatingV2;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CombatRecordRating {
    private static final MMLogger logger = MMLogger.create(CombatRecordRating.class);

    /**
     * Calculates the combat record rating for the provided campaign.
     *
     * @param campaign the campaign for which to calculate the combat record rating
     * @return a map containing the combat record ratings:
     *         - "partialSuccesses": the number of missions with status "PARTIAL"
     *         - "successes": the number of missions with status "SUCCESS"
     *         - "failures": the number of missions with status "FAILED"
     *         - "contractsBreached": the number of missions with status "BREACH"
     *         - "retainerDuration": the duration of the campaign's retainer (in years),
     *                               zero if there is no retainer
     *         - "total": the total combat record rating calculated using the formula:
     *                    (successes * 5) - (failures * 10) - (contractBreaches * 25)
     */
    protected static Map<String, Integer> calculateCombatRecordRating(Campaign campaign) {
        Map<String, Integer> combatRecord = new HashMap<>();

        // If the faction is pirate, set all values to zero and return the map immediately,
        // CamOps says pirates don't track combat record rating, but we still want these values for use elsewhere
        if (campaign.getFaction().isPirate()) {
            combatRecord.put("partialSuccesses", 0);
            combatRecord.put("successes", 0);
            combatRecord.put("failures", 0);
            combatRecord.put("contractsBreached", 0);
            combatRecord.put("retainerDuration", 0);
            return combatRecord;
        }

        // Construct a map with mission statuses and their counts
        Map<MissionStatus, Long> missionCountsByStatus = campaign.getCompletedMissions().stream()
                .filter(mission -> mission.getStatus() != MissionStatus.ACTIVE)
                .collect(Collectors.groupingBy(Mission::getStatus, Collectors.counting()));

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
        int combatRecordRating = (successes * 5) - (failures * 10) - (contractBreaches * 25);

        // if the campaign has a retainer, check retainer duration
        if (campaign.getRetainerStartDate() != null) {
            int retainerDuration = (int) ChronoUnit.YEARS.between(campaign.getRetainerStartDate(), campaign.getLocalDate());
            combatRecord.put("retainerDuration", retainerDuration);
            combatRecordRating += retainerDuration * 5;
        } else {
            combatRecord.put("retainerDuration", 0);
        }

        // add the total rating to the map
        combatRecord.put("total", combatRecordRating);

        // post a log to aid debugging
        logger.info("Combat Record Rating = {}",
                combatRecord.keySet().stream()
                        .map(key -> String.format("%s: %d", key.hashCode(), combatRecord.get(key)))
                        .collect(Collectors.joining("\n")));

        // return the completed map
        return combatRecord;
    }
}
