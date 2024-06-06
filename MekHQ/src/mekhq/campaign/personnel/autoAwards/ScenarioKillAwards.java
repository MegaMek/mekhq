package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.personnel.Award;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScenarioKillAwards {
    /**
     * This function loops through Scenario Kill Awards, checking whether the person is eligible to receive each type of award.
     *
     * @param campaign the campaign to be processed
     * @param person the Person to check award eligibility for
     * @param kills a list of p's relevant kills
     * @param awards awards the awards to be processed (should only include awards where item == kill && ranges == scenario)
     */
    public static Map<Integer, List<Object>> ScenarioKillAwardsProcessor(Campaign campaign, UUID person, List<Award> awards, List<Kill> kills) {
        int killsNeeded;

        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> eligibleAwardsBestable = new ArrayList<>();
        Award bestAward = new Award();

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                try {
                    killsNeeded = award.getQty();
                } catch (Exception e) {
                    LogManager.getLogger().warn("Kill(Scenario) Award {} from the {} set has invalid range qty {}",
                            award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                if (kills.size() >= killsNeeded) {
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