package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.util.List;

public class TimeAwards {
    /**
     * This function loops through Time Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param awards the awards to be processed (should only include awards where item == Time)
     * @param person the person to check award eligibility for
     */
    public TimeAwards(Campaign campaign, List<Award> awards, Person person) {
        for (Award award : awards) {
            if (award.canBeAwarded(person)) {

            }
        }
    }
}
