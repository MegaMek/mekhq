package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class ScenarioAwards {
    /**
     * This function loops through Scenario Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param awards the awards to be processed (should only include awards where item == Scenario)
     * @param person the person to check award eligibility for
     */
    public ScenarioAwards(Campaign campaign, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        int requiredScenarioCount;

        for (Award award : awards) {
            // we do this here to more reliably catch Awards with malformed qty values
            try {
                requiredScenarioCount = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}",
                        award.getName(), award.getSet(), award.getQty());
                break;
            }

            if (award.canBeAwarded(person)) {
                if (person.getScenarioLog().size() >= requiredScenarioCount) {
                    // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString
                    campaign.addReport(person.getHyperlinkedName() + ' ' +
                            MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                    award.getName(), award.getSet()));
                }
            }
        }
    }
}
