package mekhq.campaign.personnel.backgrounds;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

public class BackgroundsController {
    public static void generateBackground(Campaign campaign, Person person) {
        if (campaign.getCampaignOptions().isUseToughness()) {
            Toughness.generateToughness(person);
        }
    }
}
