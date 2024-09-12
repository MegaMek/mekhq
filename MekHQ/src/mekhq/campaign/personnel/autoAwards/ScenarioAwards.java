package mekhq.campaign.personnel.autoAwards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;

public class ScenarioAwards {
    private static final MMLogger logger = MMLogger.create(ScenarioAwards.class);

    /**
     * This function loops through Scenario Awards, checking whether the person is
     * eligible to receive each type of award.
     *
     * @param campaign the campaign to be processed
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where
     *                 item == Scenario)
     */
    public static Map<Integer, List<Object>> ScenarioAwardsProcessor(Campaign campaign, UUID person,
            List<Award> awards) {
        int logSize = campaign.getPerson(person).getScenarioLog().size();
        int requiredScenarioCount;

        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> eligibleAwardsBestable = new ArrayList<>();
        Award bestAward = new Award();

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                try {
                    requiredScenarioCount = award.getQty();
                } catch (Exception e) {
                    logger.warn("Award {} from the {} set has an invalid qty value {}",
                            award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                if (logSize >= requiredScenarioCount) {
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
