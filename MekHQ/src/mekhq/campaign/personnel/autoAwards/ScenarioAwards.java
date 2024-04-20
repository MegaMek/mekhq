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

public class ScenarioAwards {
    Campaign campaign;
    Person person;

    /**
     * This function loops through Scenario Awards, checking whether the person is eligible to receive each type of award
     * @param c the campaign to be processed
     * @param awards the awards to be processed (should only include awards where item == Scenario)
     * @param p the person to check award eligibility for
     */
    public ScenarioAwards(Campaign c, List<Award> awards, Person p) {
        campaign = c;
        person = p;

        int logSize = person.getScenarioLog().size();
        int requiredScenarioCount;

        List<Award> eligibleAwards = new ArrayList<>();
        Award bestAward = new Award();

        for (Award award : awards) {
            if (award.canBeAwarded(person)) {
                try {
                    requiredScenarioCount = award.getQty();
                } catch (Exception e) {
                    LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}",
                            award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                if (logSize >= requiredScenarioCount) {
                    eligibleAwards.add(award);
                }
            }
        }

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
