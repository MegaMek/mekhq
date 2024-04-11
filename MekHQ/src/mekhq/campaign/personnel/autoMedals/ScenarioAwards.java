package mekhq.campaign.personnel.autoMedals;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class ScenarioAwards {
    public ScenarioAwards(Campaign campaign, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        for (Award award : awards) {
            if (award.canBeAwarded(person)) {
                // TODO we need to find the hardest award person qualities for and only report that award

                if (person.getScenarioLog().size() >= award.getQty()) {
                    campaign.addReport(person.getHyperlinkedName() + ' ' +
                            MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                    award.getName(), award.getSet()));
                }
            }
        }
    }
}
