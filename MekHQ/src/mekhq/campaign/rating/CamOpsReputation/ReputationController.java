package mekhq.campaign.rating.CamOpsReputation;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReputationController {
    // average experience rating
    private SkillLevel averageSkillLevel = SkillLevel.NONE;
    private int averageExperienceRating = 0;

    // command rating
    private Map<String, Integer> commanderMap = new HashMap<>();
    private int commanderRating = 0;

    // combat record rating
    private Map<String, Integer> combatRecordMap = new HashMap<>();
    private int combatRecordRating = 0;

    // transportation rating
    private Map<String, Integer> transportationCapacities =  new HashMap<>();
    private Map<String, Integer> transportationRequirements =  new HashMap<>();
    private int transportationRating = 0;

    // financial rating
    private Map<String, Integer> financialRatingMap =  new HashMap<>();
    private int financialRating = 0;


    public void initializeReputation(Campaign campaign) {
        // step one: calculate average experience rating
        averageSkillLevel = AverageExperienceRating.getSkillLevel(campaign);
        averageExperienceRating = AverageExperienceRating.getReputationModifier(averageSkillLevel);

        // step two: calculate command rating
        // TODO add a campaign option to disable personality rating
        commanderMap = CommandRating.calculateCommanderRating(campaign, campaign.getFlaggedCommander());
        commanderRating = commanderMap.get("total");

        // step three: calculate combat record rating
        combatRecordMap = CombatRecordRating.calculateCombatRecordRating(campaign);
        combatRecordRating = combatRecordMap.get("total");

        // step four: calculate transportation rating
        List<Map<String, Integer>> rawTransportationData = TransportationRating.calculateTransportationRating(campaign);
        transportationCapacities = rawTransportationData.get(0);
        transportationRequirements = rawTransportationData.get(1);

        transportationRating = transportationCapacities.get("total");
        transportationCapacities.remove("total");

        // step five: calculate financial rating
        financialRatingMap = FinancialRating.calculateFinancialRating(campaign.getFinances());
        financialRating = financialRatingMap.get("total");

    }
}
