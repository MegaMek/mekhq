package mekhq.campaign.rating.CamOpsReputation;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

public class CrimeRating {
    private static final MMLogger logger = MMLogger.create(CrimeRating.class);

    /**
     * Calculates the crime rating for a given campaign.
     *
     * @param campaign the campaign for which to calculate the crime rating
     * @return the calculated crime rating
     */
    protected static int calculateCrimeRating(Campaign campaign) {
        int crimeRating = campaign.getAdjustedCrimeRating();

        logger.info("Crime Rating = {}", crimeRating);

        return crimeRating;
    }
}
