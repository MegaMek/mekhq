package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class InjuryAwards {
    final Campaign campaign;
    final Person person;

    /**
     * This function processes Injury Awards and spits out eligibility into the Daily Report
     *
     * @param c the campaign to be processed
     * @param p the Person to check award eligibility for
     * @param awards awards the awards to be processed (should only include awards where item == Injury)
     * @param injuryCount the number of Hits sustained in the Scenario just concluded
     */
    public InjuryAwards(Campaign c, Person p, List<Award> awards, int injuryCount) {
        campaign = c;
        person = p;
        int injuriesNeeded;

        List<Award> eligibleAwards = new ArrayList<>();

        for (Award award : awards) {
            try {
                injuriesNeeded = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Injury Award {} from the {} set has invalid range qty {}",
                        award.getName(), award.getSet(), award.getQty());
                continue;
            }

            if (injuryCount >= injuriesNeeded) {
                eligibleAwards.add(award);
            }
        }

        // this finds the best Award (if applicable) and announces Award eligibility
        Award bestAward = new Award();

        if (!eligibleAwards.isEmpty()) {
            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
                int rollingQty = 0;

                for (Award award : eligibleAwards) {
                    if (award.getQty() > rollingQty) {
                        rollingQty = award.getQty();
                        bestAward = award;
                    }
                }

                announceEligibility(bestAward);
            } else {
                for (Award award : eligibleAwards) {
                    announceEligibility(award);
                }
            }
        }
    }

    /**
     * This function announced Award eligibility to the Daily Report pane
     * @param award the award to be announced
     */
    private void announceEligibility (Award award){
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
        campaign.addReport(person.getHyperlinkedName() + ' ' + MessageFormat.format(resource.getString(
                "EligibleForAwardReport.format"), award.getName(), award.getSet()));
    }
}
