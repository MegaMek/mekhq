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
        Faction enemy;

        for (Award award : awards) {
            isEligible = false;
            enemy = null;

            List<String> wartime = List.of(award.getSize()
                    .replaceAll("\\s","")
                    .split(","));

            if (wartime.size() != 2) {
                LogManager.getLogger().warn("Award {} from the {} set has invalid start/end date {}",
                        award.getName(), award.getSet(), award.getSize());
                break;
            }

            int currentYear = campaign.getGameYear();

            List<String> belligerents = List.of(award.getRange()
                    .toLowerCase()
                    .replaceAll("\\s","")
                    .split(","));

            if (belligerents.size() != 2) {
                LogManager.getLogger().warn("Award {} from the {} set has invalid belligerents {}",
                        award.getName(), award.getSet(), award.getSize());
                break;
            }

            Faction employer = Factions.getInstance().getFaction(((Contract) mission).getEmployer());

            if ((!campaign.getCampaignOptions().isUseAtB()) && (mission instanceof AtBContract)) {
                enemy = Factions.getInstance().getFaction(((AtBContract) mission).getEnemyName(campaign.getGameYear()));
            }

            if (award.canBeAwarded(person)) {
                if ((belligerents.get(0).equals(belligerents.get(1))) || (!campaign.getCampaignOptions().isUseAtB())) {
                    if (processFaction(belligerents.get(0), employer, mission)) {
                        isEligible = true;
                    }
                } else {
                    if ((processFaction(belligerents.get(0), employer, mission))
                            && (processFaction(belligerents.get(1), employer, mission))) {
                        isEligible = true;
                    }
                }

                if (isEligible) {
                    isEligible = (currentYear >= Integer.parseInt(wartime.get(0)))
                            && (currentYear <= Integer.parseInt(wartime.get(1)));
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

    private boolean processFaction(String belligerent, Faction faction, Mission mission) {
        switch (belligerent) {
            case "innersphere":
                return faction.isInnerSphere();
            case "clan":
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
                return ((Contract) mission).getEmployer().replaceAll("\\s", "")
                        .equalsIgnoreCase(belligerent);
        }
    }
}
