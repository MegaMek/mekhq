package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.universe.Faction;
import org.apache.logging.log4j.LogManager;

import java.util.*;

public class FactionHunterAwards {
    /**
     * This function loops through Faction Hunter Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param mission the mission just completed
     * @param person the person to check award eligibility for
     * @param awards the awards to be processed (should only include awards where item == TheatreOfWar)
     */
    public static Map<Integer, List<Object>> FactionHunterAwardsProcessor(Campaign campaign, Mission mission, UUID person, List<Award> awards) {
        boolean isEligible = false;
        List<Award> eligibleAwards = new ArrayList<>();

        Faction missionFaction = ((AtBContract) mission).getEnemy();

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                List<String> targetFactions = List.of(award.getRange().split(","));
                LogManager.getLogger().info(targetFactions);

                if (!targetFactions.isEmpty()) {
                    // returns true if missionFaction matches the requirements of the listed targetFactions
                    for (String awardFaction : targetFactions) {
                        // does the awardFaction equal missionFaction? if so, break the loop
                        if ((Objects.equals(awardFaction, missionFaction.getShortName()))) {
                            isEligible = true;
                            break;
                        }

                        // does awardFaction match one of the special super-factions?
                        switch (awardFaction.toLowerCase()) {
                            case "major powers":
                                if (missionFaction.isMajorOrSuperPower()) {
                                    isEligible = true;
                                }
                                break;
                            case "inner sphere":
                                if (missionFaction.isInnerSphere()) {
                                    isEligible = true;
                                }
                                break;
                            case "clans":
                                if (missionFaction.isClan()) {
                                    isEligible = true;
                                }
                                break;
                            case "periphery":
                                if (missionFaction.isPeriphery()) {
                                    isEligible = true;
                                }
                                break;
                            case "pirate":
                                if (missionFaction.isPirate()) {
                                    isEligible = true;
                                }
                                break;
                            case "mercenary":
                                if (missionFaction.isMercenary()) {
                                    isEligible = true;
                                }
                                break;
                            case "independent":
                                if (missionFaction.isIndependent()) {
                                    isEligible = true;
                                }
                                break;
                            case "deep periphery":
                                if (missionFaction.isDeepPeriphery()) {
                                    isEligible = true;
                                }
                                break;
                            case "wob":
                                if (missionFaction.isWoB()) {
                                    isEligible = true;
                                }
                                break;
                            case "comstar or wob":
                                if (missionFaction.isComStarOrWoB()) {
                                    isEligible = true;
                                }
                                break;
                            default:
                                break;
                        }

                        // once we have one positive, there is no need to continue cycling through factions
                        if (isEligible) {
                            break;
                        }
                    }

                    if (isEligible) {
                        eligibleAwards.add(award);
                    }
                }
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }
}
