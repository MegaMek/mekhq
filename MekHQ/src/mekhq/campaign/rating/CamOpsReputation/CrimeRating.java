package mekhq.campaign.rating.CamOpsReputation;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CrimeRating {
    private static final MMLogger logger = MMLogger.create(CrimeRating.class);

    /**
     * Calculates the crime rating for a given campaign.
     *
     * @param campaign the campaign for which to calculate the crime rating
     * @return the calculated crime rating
     */
    protected static Map<String, Integer> calculateCrimeRating(Campaign campaign) {
        Map<String, Integer> crimeRating = new HashMap<>();

        crimeRating.put("piracy", campaign.getCrimePirateModifier());
        crimeRating.put("other", campaign.getRawCrimeRating());

        int adjustedCrimeRating = campaign.getAdjustedCrimeRating();
        crimeRating.put("total", adjustedCrimeRating);

        logger.debug("Crime Rating = {}",
                crimeRating.entrySet().stream()
                        .map(entry -> String.format("%s: %d\n", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining()));

        return crimeRating;
    }
}
