package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class TimeAwards {
    /**
     * This function loops through Time Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param person the person to check award eligibility for
     * @param awards the awards to be processed (should only include awards where item == Time)
     */
    public static Map<Integer, List<Object>> TimeAwardsProcessor(Campaign campaign, UUID person, List<Award> awards) {
        int requiredYearsOfService;
        boolean isCumulative;
        int yearsOfService;

        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> eligibleAwardsBestable = new ArrayList<>();
        Award bestAward = new Award();

        for (Award award : awards) {
            try {
                requiredYearsOfService = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}",
                        award.getName(), award.getSet(), award.getQty());
                continue;
            }

            try {
                isCumulative = award.isStackable();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid stackable value {}",
                        award.getName(), award.getSet(), award.getQty());
                continue;
            }

            if (award.canBeAwarded(campaign.getPerson(person))) {
                try {
                    yearsOfService = Integer.parseInt(Pattern.compile("\\s+")
                            .splitAsStream(campaign.getPerson(person).getTimeInService(campaign))
                            .findFirst()
                            .orElseThrow());
                } catch (Exception e) {
                    LogManager.getLogger().error("Unable to parse yearsOfService for {} while processing Award {} from the [{}] set." +
                                    " This can be ignored if {} has 0 years Time in Service.",
                            campaign.getPerson(person).getFullName(), award.getName(), award.getSet(), campaign.getPerson(person).getFullName());
                    continue;
                }

                if (isCumulative) {
                    requiredYearsOfService *= campaign.getPerson(person).getAwardController().getNumberOfAwards(award) + 1;
                }

                if (yearsOfService >= requiredYearsOfService) {
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
