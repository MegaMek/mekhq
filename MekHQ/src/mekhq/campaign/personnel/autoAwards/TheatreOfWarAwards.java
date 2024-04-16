package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class TheatreOfWarAwards {
    /**
     * This function loops through Theatre of War Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param awards the awards to be processed (should only include awards where item == TheatreOfWar)
     * @param person the person to check award eligibility for
     */
    public TheatreOfWarAwards(Campaign campaign, Mission mission, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        boolean isEligible;

        String employer = ((Contract) mission).getEmployer();

        int currentYear = campaign.getGameYear();

        for (Award award : awards) {
            List<String> attackers = new ArrayList<>();
            List<String> defenders = new ArrayList<>();

            List<String> wartime = List.of(award.getSize()
                    .replaceAll("\\s","")
                    .split(","));

            if (wartime.size() != 2) {
                LogManager.getLogger().warn("Award {} from the {} set has invalid start/end date {}",
                        award.getName(), award.getSet(), award.getSize());
                continue;
            }

            List<String> belligerents = List.of(award.getRange().split(","));

            if (!belligerents.isEmpty()) {
                if (belligerents.size() > 1) {
                    for (String belligerent : belligerents) {
                        if (belligerent.replaceAll("[()]", "").contains("1")) {
                            attackers.add(belligerent.replaceAll("[^. A-Za-z]", ""));
                        } else if (belligerent.replaceAll("[()]", "").contains("2")) {
                            defenders.add(belligerent.replaceAll("[^. A-Za-z]", ""));
                        }
                    }

                    if ((attackers.isEmpty()) || (defenders.isEmpty())) {
                        LogManager.getLogger().warn("Award {} from the {} set has incorrectly formated belligerents {}",
                                award.getName(), award.getSet(), award.getRange());
                        continue;
                    }
                }
            } else {
                LogManager.getLogger().warn("Award {} from the {} set has no belligerents",
                        award.getName(), award.getSet());
                continue;
            }

            if (award.canBeAwarded(person)) {
                if ((currentYear >= Integer.parseInt(wartime.get(0))) && (currentYear <= Integer.parseInt(wartime.get(1)))) {
                    isEligible = true;
                } else {
                    continue;
                }

                if (belligerents.size() == 1) {
                    if(!processFaction(belligerents.get(0), employer)) {
                        continue;
                    }
                } else if ((campaign.getCampaignOptions().isUseAtB()) && (mission instanceof AtBContract)) {
                    String enemy = ((AtBContract) mission).getEnemyName(campaign.getGameYear());

                    if (isLoyalty(employer, attackers)) {
                        isEligible = isLoyalty(enemy, defenders);
                    } else if (isLoyalty(employer, defenders)) {
                        isEligible = isLoyalty(enemy, attackers);
                    } else {
                        continue;
                    }
                }

                if (isEligible) {
                    // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
                    campaign.addReport(person.getHyperlinkedName() + ' ' +
                            MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                    award.getName(), award.getSet()));
                }
            }
        }
    }

    private boolean isLoyalty (String missionFaction, List<String> factions) {
        return factions.contains(missionFaction);
    }

    private boolean processFaction(String missionFaction, String belligerent) {
        Faction faction = Factions.getInstance().getFaction(missionFaction);

        missionFaction = missionFaction.toLowerCase().replaceAll("\\s","");
        belligerent = belligerent.toLowerCase().replaceAll("\\s","");

        switch (belligerent) {
            case "majorpowers":
                return faction.isMajorOrSuperPower();
            case "innersphere":
                return faction.isInnerSphere();
            case "clans":
                return faction.isClan();
            case "periphery":
                return faction.isPeriphery();
            case "independent":
                return faction.isIndependent();
            case "deepperiphery":
                return faction.isDeepPeriphery();
            case "comstar":
                return faction.isComStar();
            case "wob":
                return faction.isWoB();
            case "comstarorwob":
                return faction.isComStarOrWoB();
            default:
                return belligerent.equals(missionFaction);
        }
    }
}
