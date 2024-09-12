package mekhq.campaign.personnel.autoAwards;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;

public class ContractAwards {
    private static final MMLogger logger = MMLogger.create(ContractAwards.class);

    /**
     * This function loops through Contract Awards, checking whether the person is
     * eligible to receive each type of award
     *
     * @param campaign the campaign to be processed
     * @param mission  the mission that just concluded
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where
     *                 item == Kill)
     */
    public static Map<Integer, List<Object>> ContractAwardsProcessor(Campaign campaign, Mission mission,
            UUID person, List<Award> awards) {
        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> eligibleAwardsBestable = new ArrayList<>();
        Award bestAward = new Award();

        long contractDuration = ChronoUnit.MONTHS.between(
                ((Contract) mission).getStartDate(),
                campaign.getLocalDate());

        // these entries should always be in lower case
        List<String> validTypes = Arrays.asList("months", "duty", "garrison duty", "cadre duty", "security duty",
                "riot duty", "planetary assault", "relief duty", "guerrilla warfare", "pirate hunting", "raid",
                "diversionary raid", "objective raid", "recon raid", "extraction raid");

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                if (award.getRange().equalsIgnoreCase("months")) {
                    try {
                        int requiredDuration = award.getQty();

                        if (contractDuration >= requiredDuration) {
                            eligibleAwardsBestable.add(award);
                        }
                    } catch (Exception e) {
                        logger.warn("Award {} from the {} set has an invalid qty value {}",
                                award.getName(), award.getSet(), award.getQty());
                    }
                } else if (validTypes.contains(award.getRange().toLowerCase())) {
                    switch (award.getRange().toLowerCase()) {
                        case "duty":
                            if (mission.getType().toLowerCase().contains("duty")) {
                                eligibleAwards.add(award);
                            }
                            break;
                        case "raid":
                            if (mission.getType().toLowerCase().contains("raid")) {
                                eligibleAwards.add(award);
                            }
                            break;
                        default:
                            if (mission.getType().equalsIgnoreCase(award.getRange())) {
                                eligibleAwards.add(award);
                            }
                    }
                } else {
                    logger.warn("Award {} from the {} set has an invalid range value {}",
                            award.getName(), award.getSet(), award.getRange());
                }
            }
        }

        if (!eligibleAwardsBestable.isEmpty()) {
            int rollingQty = 0;

            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
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
