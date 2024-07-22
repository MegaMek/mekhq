package mekhq.campaign.personnel.backgrounds;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public class BackgroundsController {
    public static void generateBackground(Campaign campaign, Person person) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.RandomBackgrounds",
                MekHQ.getMHQOptions().getLocale());

        if (campaign.getCampaignOptions().isUseToughness()) {
            Toughness.generateToughness(person, resources);
        }
    }
}
