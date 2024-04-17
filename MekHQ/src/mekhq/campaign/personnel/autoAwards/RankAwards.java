package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class RankAwards {
    /**
     * This function loops through Rank Awards, checking whether the person is eligible to receive each type of award.
     * All Misc awards need to be hardcoded
     * @param campaign the campaign to be processed
     * @param awards the awards to be processed (should only include awards where item == Kill)
     * @param person the person to check award eligibility for
     */
    public RankAwards(Campaign campaign, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        int requiredRankNumeric;
        boolean isInclusive;
        boolean isEligible;

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
                LogManager.getLogger().warn("Award {} from the {} set has the invalid range {}",
                        award.getName(), award.getSet(), award.getRange());
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
                // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
                campaign.addReport(person.getHyperlinkedName() + ' ' +
                        MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                award.getName(), award.getSet()));
            }
        }
    }
}
