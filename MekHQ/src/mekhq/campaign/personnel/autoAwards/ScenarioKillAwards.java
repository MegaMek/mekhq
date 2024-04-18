package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.AwardsFactory;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ScenarioKillAwards {
    /**
     * This creates a list of Kill Awards where item == scenario. It then loops through these Awards checking eligibility
     * @param campaign the campaign to be processed
     * @param scenarioId the Id number for the scenario just completed
     * @param person the person to check award eligibility for
     */
    public ScenarioKillAwards(Campaign campaign, int scenarioId, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards", MekHQ.getMHQOptions().getLocale());

        int killsNeeded;
        List<Kill> kills;

        LogManager.getLogger().info("autoAwards (Mission Conclusion) has started");

        ArrayList<Award> awards = buildAwardLists();

        for (Kill kill : kills = campaign.getKillsFor(person.getId())) {
            if (kill.getScenarioId() != scenarioId) {
                kills.remove(kill);
            }
        }

        if ((!kills.isEmpty()) && (!awards.isEmpty())) {
            for (Award award : buildAwardLists()) {
                if (kills.isEmpty()) {
                    continue;
                }

                try {
                    killsNeeded = award.getQty();
                } catch (Exception e) {
                    LogManager.getLogger().warn("Award {} from the {} set has invalid range qty {}",
                            award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                kills.removeIf(kill -> kill.getScenarioId() != scenarioId);

                if (kills.size() >= killsNeeded) {
                    // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
                    campaign.addReport(person.getHyperlinkedName() + ' ' + MessageFormat
                            .format(resource.getString("EligibleForAwardReport.format"),
                                    award.getName(), award.getSet()));
                }
            }
        } else if (kills.isEmpty()) {
            LogManager.getLogger().info("autoAwards failed to find any valid kills for {}", person.getFullName());
        }

        LogManager.getLogger().info("autoAwards has finished");
    }

    /**
     * Builds the award list and filters it, so we're not processing the same medals multiple times.
     * This is a cut-down version of the identically named function in AutoAwardsController.java
     */
    private ArrayList<Award> buildAwardLists() {
        ArrayList<Award> awards = new ArrayList<>();
        List<String> allSetNames = AwardsFactory.getInstance().getAllSetNames();

        // we start by building a master list of all awards
        if (!allSetNames.isEmpty()) {
            LogManager.getLogger().info("Getting all Award Sets");

            for (String setName : AwardsFactory.getInstance().getAllSetNames()) {
                if (!allSetNames.isEmpty()) {
                    LogManager.getLogger().info("Getting all awards from set: {}", setName);

                    awards.addAll(AwardsFactory.getInstance().getAllAwardsForSet(setName));

                    // next we filter out non-scenario kill awards
                    awards.removeIf(award -> (!award.getRange().equalsIgnoreCase("scenario"))
                            || (!award.getItem().equalsIgnoreCase("kill")));
                } else {
                    LogManager.getLogger().info("autoAwards failed to find any awards in set {}", setName);
                }
            }
        } else {
            LogManager.getLogger().info("AutoAwards failed to find any award sets");
        }

        if (awards.isEmpty()) {
            LogManager.getLogger().info("autoAwards failed to find any Scenario Kill Awards, skipping the awards ceremony");
        }

        return awards;
    }
}
