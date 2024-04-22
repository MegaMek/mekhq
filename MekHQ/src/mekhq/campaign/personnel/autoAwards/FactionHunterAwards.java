package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mekhq.campaign.personnel.autoAwards.TheatreOfWarAwards.processFaction;

public class FactionHunterAwards {
    /**
     * This function loops through Faction Hunter Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param mission the mission just completed
     * @param person the person to check award eligibility for
     * @param awards the awards to be processed (should only include awards where item == TheatreOfWar)
     */
    public static Map<Integer, List<Object>> FactionHunterAwardsProcessor(Campaign campaign, Mission mission, Person person, List<Award> awards) {
        boolean isEligible;
        List<Award> eligibleAwards = new ArrayList<>();

        String missionFaction = ((AtBContract) mission).getEnemy().getFullName(campaign.getGameYear());

        for (Award award : awards) {
            List<String> targetFactions = List.of(award.getRange().split(","));

            if (!targetFactions.isEmpty()) {
                // returns true if missionFaction matches the requirements of the listed targetFactions
                isEligible = targetFactions.stream().anyMatch(targetFaction -> processFaction(missionFaction, targetFaction));

                if (isEligible) {
                    eligibleAwards.add(award);
                }
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }
}
