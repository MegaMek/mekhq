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

public class AutoAwardsPostScenarioController {
    Campaign campaign;
    Person person;
    ArrayList<Award> killAwards = new ArrayList<>();
    ArrayList<Award> injuryAwards = new ArrayList<>();

    final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
            MekHQ.getMHQOptions().getLocale());


    /**
     * This creates a list of Kill Awards where item == scenario. It then loops through these Awards checking eligibility
     *
     * @param campaign    the campaign to be processed
     * @param scenarioId  the Id number for the scenario just completed
     * @param person      the person to check award eligibility for
     * @param injuryCount
     */
    public AutoAwardsPostScenarioController(Campaign c, int scenarioId, Person p, int injuryCount) {
        LogManager.getLogger().info("autoAwards (Mission Conclusion) has started");

        campaign = c;
        person = p;

        buildAwardLists();

        // beginning the processing of Injury Awards
        if ((injuryCount > 0) && (!injuryAwards.isEmpty())) {
            processInjuryAwards(injuryCount);
        } else if (injuryAwards.isEmpty()) {
            LogManager.getLogger().info("autoAwards failed to find any Injury Awards");
        }

        // beginning the processing of Kill(Scenario) Awards
        List<Kill> kills = campaign.getKillsFor(person.getId());

        kills.removeIf(kill -> kill.getScenarioId() != scenarioId);

        if ((!kills.isEmpty()) && (!killAwards.isEmpty())) {
            processKillAwards(kills);
        } else if (killAwards.isEmpty()) {
            LogManager.getLogger().info("autoAwards failed to find any Kill(Scenario) Awards");
        }

        LogManager.getLogger().info("autoAwards has finished");
    }

    /**
     * Builds the award list and filters it, so we're not processing the same medals multiple times.
     * This is a cut-down version of the identically named function in AutoAwardsPostMissionController.java
     */
    private void buildAwardLists() {
        ArrayList<Award> awards = new ArrayList<>();

        List<String> allSetNames = AwardsFactory.getInstance().getAllSetNames();

        // we start by building a master list of all awards
        if (!allSetNames.isEmpty()) {
            LogManager.getLogger().info("Getting all Award Sets");

            for (String setName : AwardsFactory.getInstance().getAllSetNames()) {
                if (!allSetNames.isEmpty()) {
                    LogManager.getLogger().info("Getting all awards from set: {}", setName);

                    awards.addAll(AwardsFactory.getInstance().getAllAwardsForSet(setName));

                    // next we separate out Kill(Scenario) and Injury Awards
                    for (Award award : awards) {
                        if (award.getItem().equalsIgnoreCase("kill")
                                && award.getRange().equalsIgnoreCase("scenario")) {
                            killAwards.add(award);
                        } else if (award.getItem().equalsIgnoreCase("injury")) {
                            injuryAwards.add(award);
                        }
                    }
                } else {
                    LogManager.getLogger().info("autoAwards failed to find any awards in set {}", setName);
                }
            }
        } else {
            LogManager.getLogger().info("AutoAwards failed to find any award sets");
        }
    }

    private void processInjuryAwards(int injuryCount) {
        int injuriesNeeded;

        for (Award award : injuryAwards) {
            try {
                injuriesNeeded = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Injury Award {} from the {} set has invalid range qty {}",
                        award.getName(), award.getSet(), award.getQty());
                continue;
            }

            if (injuryCount >= injuriesNeeded) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + MessageFormat
                        .format(resource.getString("EligibleForAwardReport.format"),
                                award.getName(), award.getSet()));
            }
        }
    }

    private void processKillAwards(List<Kill> kills) {
        int killsNeeded;

        for (Award award : killAwards) {
            try {
                killsNeeded = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Kill(Scenario) Award {} from the {} set has invalid range qty {}",
                        award.getName(), award.getSet(), award.getQty());
                continue;
            }

            if (kills.size() >= killsNeeded) {
                // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
                campaign.addReport(person.getHyperlinkedName() + ' ' + MessageFormat
                        .format(resource.getString("EligibleForAwardReport.format"),
                                award.getName(), award.getSet()));
            }
        }
    }
}
