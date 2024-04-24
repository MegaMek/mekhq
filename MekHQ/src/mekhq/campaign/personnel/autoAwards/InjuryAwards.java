package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InjuryAwards {
    /**
     * This function processes Injury Awards and spits out eligibility into the Daily Report
     *
     * @param campaign the campaign to be processed
     * @param person the Person to check award eligibility for
     * @param awards awards the awards to be processed (should only include awards where item == Injury)
     * @param injuryCount the number of Hits sustained in the Scenario just concluded
     */
    public static Map<Integer, List<Object>> InjuryAwardsProcessor(Campaign campaign, UUID person, List<Award> awards, int injuryCount) {
        int injuriesNeeded;

        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> eligibleAwardsBestable = new ArrayList<>();
        Award bestAward = new Award();

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                try {
                    injuriesNeeded = award.getQty();
                } catch (Exception e) {
                    LogManager.getLogger().warn("Injury Award {} from the {} set has invalid range qty {}",
                            award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                if (injuryCount >= injuriesNeeded) {
                    eligibleAwardsBestable.add(award);
                }
            }
        }

        if (!eligibleAwardsBestable.isEmpty()) {
            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
                int rollingQty = 0;

                for (Award award : eligibleAwardsBestable) {
                    if (award.getQty() > rollingQty) {
                        rollingQty = award.getQty();
                        bestAward = award;
                    }
                }

                eligibleAwards.add(bestAward);
            } else {
                eligibleAwards.addAll(eligibleAwardsBestable);
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }
}
