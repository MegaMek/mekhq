package mekhq.campaign.personnel.autoMedals;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.util.List;

public class TheatreOfWarAwards {
    public TheatreOfWarAwards(Campaign campaign, List<Award> awards, Person person) {
        for (Award award : awards) {
            if (award.canBeAwarded(person)) {

            }
        }
    }
}
