package mekhq.campaign.rating.CamOpsRatingV2;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;

import java.util.HashMap;
import java.util.Map;

public class ReputationController {
    // average experience rating
    private SkillLevel averageSkillLevel = SkillLevel.NONE;
    private int averageExperienceRating = 0;

    // command rating
    private Map<String, Integer> commanderMap = new HashMap<>();
    private int commanderRating = 0;


    public void initializeReputation(Campaign campaign) {
        int reputation = 0;

        // step one: calculate average experience rating
        averageSkillLevel = AverageExperienceRating.getSkillLevel(campaign);
        averageExperienceRating = AverageExperienceRating.getReputationModifier(averageSkillLevel);

        // step two: calculate command rating
        // TODO add a campaign option to disable personality rating
        commanderMap = CommandRating.calculateCommanderRating(campaign, campaign.getFlaggedCommander());
        commanderRating = commanderMap.get("total");

    }
}
