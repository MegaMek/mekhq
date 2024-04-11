package mekhq.campaign.personnel.autoMedals;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class MissionAccomplishedAwards {
    public MissionAccomplishedAwards(Campaign campaign, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        for (Award award : awards) {
            if (award.canBeAwarded(person)) {
                campaign.addReport(MessageFormat.format(resource.getString("EligibleForAwardReportAll.format"),
                        award.getName(), award.getSet()));
            }
        }
    }
}
