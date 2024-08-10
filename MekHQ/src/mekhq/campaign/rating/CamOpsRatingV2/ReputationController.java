package mekhq.campaign.rating.CamOpsRatingV2;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;

public class ReputationController {
    private SkillLevel averageSkillLevel = SkillLevel.NONE;
    private int averageExperienceRating = 0;

    public void initializeReputation(Campaign campaign) {
        int reputation = 0;

        // step one: calculate average experience rating
        averageSkillLevel = AverageExperienceRating.getSkillLevel(campaign);
        averageExperienceRating = AverageExperienceRating.getReputationModifier(averageSkillLevel);

        // step two:

    }
}
