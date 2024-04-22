package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RankAwards {
    /**
     * This function loops through Rank Awards, checking whether the person is eligible to receive each type of award.
     * All Misc awards need to be hardcoded
     * @param person the person to check award eligibility for
     * @param awards the awards to be processed (should only include awards where item == Kill)
     */
    public static Map<Integer, List<Object>> RankAwardsProcessor(Person person, List<Award> awards) {
        int requiredRankNumeric;
        boolean isInclusive;
        boolean isEligible;

        List<Award> eligibleAwards = new ArrayList<>();

        for (Award award : awards) {
            isEligible = false;

            try {
                requiredRankNumeric = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}", award.getName(), award.getSet(), award.getQty());
                continue;
            }

            if (award.getRange().equalsIgnoreCase("inclusive")) {
                isInclusive = true;
            } else if (award.getRange().equalsIgnoreCase("exclusive")) {
                isInclusive = false;
            } else {
                LogManager.getLogger().warn("Award {} from the {} set has the invalid range {}", award.getName(), award.getSet(), award.getRange());
                continue;
            }

            if (award.canBeAwarded(person)) {
                if (isInclusive) {
                    if (person.getRankNumeric() >= requiredRankNumeric) {
                        isEligible = true;
                    }
                } else {
                    if ((requiredRankNumeric <= 20) && (person.getRankNumeric() <= 20)) {
                        isEligible = true;
                    } else if ((requiredRankNumeric <= 30) && (person.getRankNumeric() <= 30)) {
                        isEligible = true;
                    } else if ((requiredRankNumeric >= 31) && (person.getRankNumeric() >= 31)) {
                        isEligible = true;
                    }

                    if (isEligible) {
                        isEligible = person.getRankNumeric() >= requiredRankNumeric;
                    }
                }
            }

            if (isEligible) {
                eligibleAwards.add(award);
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }
}
