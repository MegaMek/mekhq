package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

import static mekhq.campaign.personnel.autoAwards.TheatreOfWarAwards.processFaction;

public class FactionHunterAwards {
    /**
     * This function loops through Faction Hunter Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param mission the mission just completed
     * @param awards the awards to be processed (should only include awards where item == TheatreOfWar)
     * @param person the person to check award eligibility for
     */
    public FactionHunterAwards(Campaign campaign, Mission mission, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        boolean isEligible;

        String missionFaction = ((AtBContract) mission).getEnemy().getFullName(campaign.getGameYear());

        for (Award award : awards) {
            List<String> targetFactions = List.of(award.getRange().split(","));

            if (!targetFactions.isEmpty()) {
                // returns true if missionFaction matches the requirements of the listed targetFactions
                isEligible = targetFactions.stream().anyMatch(targetFaction -> processFaction(missionFaction, targetFaction));

                if (isEligible) {
                    // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
                    campaign.addReport(person.getHyperlinkedName() + ' ' +
                            MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                    award.getName(), award.getSet()));
                }
            }
        }
    }
}
