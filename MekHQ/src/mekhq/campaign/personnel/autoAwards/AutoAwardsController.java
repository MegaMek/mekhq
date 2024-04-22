package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.AwardsFactory;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.dialog.AutoAwardsDialog;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;

public class AutoAwardsController {
    private Campaign campaign;
    private Mission mission;

    final private List<Award> contractAwards = new ArrayList<>();
    final private List<Award> factionHunterAwards = new ArrayList<>();
    final private List<Award> injuryAwards = new ArrayList<>();
    final private List<Award> killAwards = new ArrayList<>();
    final private List<Award> miscAwards = new ArrayList<>();
    final private List<Award> rankAwards = new ArrayList<>();
    final private List<Award> scenarioAwards = new ArrayList<>();
    final private List<Award> skillAwards = new ArrayList<>();
    final private List<Award> theatreOfWarAwards = new ArrayList<>();
    final private List<Award> timeAwards = new ArrayList<>();
    final private List<Award> trainingAwards = new ArrayList<>();
    final private List<Award> ignoredAwards = new ArrayList<>();

    /**
     * The primary controller for the automatic processing of Awards
     * @param c the campaign to be processed
     * @param m the mission just completed
     * @param missionWasSuccessful true if Mission was a complete Success, otherwise false
     */
    public void PostMissionController(Campaign c, Mission m, Boolean missionWasSuccessful) {
        LogManager.getLogger().info("autoAwards (Mission Conclusion) has started");

        campaign = c;
        mission = m;

        buildAwardLists(1);

        Collection<Person> personnel = campaign.getActivePersonnel();

        if (!personnel.isEmpty()) {
            // Prisoners and Dependents are not eligible for Awards
            removeDependentsAndPrisoners(personnel);

            // we have to do multiple isEmpty() checks as any point in the removal process could result in null personnel
            if(!personnel.isEmpty()) {
                // This is the main workhorse function
                ProcessAwards(personnel, missionWasSuccessful);
            }
        } else {
            LogManager.getLogger().info("AutoAwards found no active personnel");
        }

        // if posthumous Awards are enabled, we process them here

        if (campaign.getCampaignOptions().isIssuePosthumousAwards()) {
            LogManager.getLogger().info("AutoAwards is beginning to process Posthumous Awards");

            personnel = campaign.getPersonnel();
            ArrayList<Person> deceasedPersonnel = new ArrayList<>();

            if (!personnel.isEmpty()) {
                // even dead Dependents and Prisoners aren't eligible for Awards
                removeDependentsAndPrisoners(personnel);
            }

            if(!personnel.isEmpty()) {
                deceasedPersonnel = personnel.stream().filter(person
                        -> person.getStatus().isDead()).collect(Collectors.toCollection(ArrayList::new));
            }

            if (!deceasedPersonnel.isEmpty()) {
                ProcessAwards(deceasedPersonnel, missionWasSuccessful);
            } else {
                LogManager.getLogger().info("AutoAwards found no deceased personnel, skipping this step");
            }
        }

        LogManager.getLogger().info("autoAwards (Mission Conclusion) has finished");
    }

    /**
     * This function processes Kill(Scenario) and Injury Awards following the conclusion of a Scenario
     *
     * @param c the campaign to be processed
     * @param scenarioId id number for the Scenario just concluded
     * @param person the person to check award eligibility for
     * @param injuryCount the number of Hits sustained in the Scenario just concluded
     */
    public void PostScenarioController(Campaign c, int scenarioId, Person person, int injuryCount) {
        LogManager.getLogger().info("autoAwards (Scenario Conclusion) has started");

        campaign = c;

        buildAwardLists(2);

        // beginning the processing of Injury Awards
        if ((injuryCount > 0) && (!injuryAwards.isEmpty())) {
            new InjuryAwards(campaign, person, injuryAwards, injuryCount);
        } else if (injuryAwards.isEmpty()) {
            LogManager.getLogger().info("autoAwards failed to find any Injury Awards");
        }

        // beginning the processing of Kill(Scenario) Awards
        List<Kill> kills = campaign.getKillsFor(person.getId());

        kills.removeIf(kill -> kill.getScenarioId() != scenarioId);

        if ((!kills.isEmpty()) && (!killAwards.isEmpty())) {
            new ScenarioKillAwards(campaign, person, killAwards, kills);
        } else if (killAwards.isEmpty()) {
            LogManager.getLogger().info("autoAwards failed to find any Kill(Scenario) Awards");
        }

        LogManager.getLogger().info("autoAwards (Scenario Conclusion) has finished");
    }

    /**
     * Filters out anyone with the Prisoner status, or Dependent role
     * @param personnel personnel to process
     */
    private void removeDependentsAndPrisoners (Collection<Person> personnel) {
        if (!personnel.isEmpty()) {
            personnel.removeIf(person -> (person.hasRole(PersonnelRole.DEPENDENT)) || (person.getPrisonerStatus().isPrisoner()));
        }
    }

    /**
     * Builds the award list and filters it, so we're not processing the same awards multiple times
     *
     * @param awardListCase when the award controller was called: 0 manual, 1 post-mission, 2 post-scenario
     */
    private void buildAwardLists(int awardListCase) {
        ArrayList<Award> awards = new ArrayList<>();
        List<String> allSetNames = AwardsFactory.getInstance().getAllSetNames();

        // we start by building a master list of all awards
        if (!allSetNames.isEmpty()) {
            LogManager.getLogger().info("Getting all Award Sets");

            for (String setName : AwardsFactory.getInstance().getAllSetNames()) {
                if (!allSetNames.isEmpty()) {
                    LogManager.getLogger().info("Getting all awards from set: {}", setName);

                    awards.addAll(AwardsFactory.getInstance().getAllAwardsForSet(setName));

                    // next we begin to filter the awards into discrete lists
                    switch (awardListCase) {
                        // manual
                        case 0:
                            break;
                        // post-mission
                        case 1:
                            for (Award award : awards) {
                                switch (award.getItem().toLowerCase().replaceAll("\\s", "")) {
                                    case "ignore":
                                        ignoredAwards.add(award);
                                        break;
                                    case "contract":
                                        if (campaign.getCampaignOptions().isEnableContractAwards()) {
                                            contractAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    case "factionhunter":
                                        if (campaign.getCampaignOptions().isEnableFactionHunterAwards()) {
                                            factionHunterAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    // Injury Awards are handled by the post-scenario controller
                                    case "injury":
                                        ignoredAwards.add(award);
                                        break;
                                    case "kill":
                                        // Scenario Kill Awards are handled by the post-scenario controller
                                        if ((campaign.getCampaignOptions().isEnableKillAwards())
                                                && (!award.getRange().equalsIgnoreCase("scenario"))) {
                                            killAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    // TODO hardcode more misc awards
                                    case "misc":
                                        if (campaign.getCampaignOptions().isEnableMiscAwards()) {
                                            miscAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    case "rank":
                                        if (campaign.getCampaignOptions().isEnableRankAwards()) {
                                            rankAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    case "scenario":
                                        if (campaign.getCampaignOptions().isEnableScenarioAwards()) {
                                            scenarioAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    case "skill":
                                        if (campaign.getCampaignOptions().isEnableSkillAwards()) {
                                            skillAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    case "theatreofwar":
                                        if (campaign.getCampaignOptions().isEnableTheatreOfWarAwards()) {
                                            theatreOfWarAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    case "time":
                                        if (campaign.getCampaignOptions().isEnableTimeAwards()) {
                                            timeAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    // trainingAwards are not currently supported.
                                    // We include them here for tracking purposes
                                    // TODO add training functionality
                                    case "training":
                                        trainingAwards.add(award);
                                        break;
                                    default:
                                        // if autoAwards doesn't know what to do with an Award, it ignores it
                                        ignoredAwards.add(award);
                                }
                            }
                            // These logs help users double-check that the number of awards found matches their records
                            LogManager.getLogger().info("autoAwards found {} Kill Awards (excluding Scenario Kill Awards)",
                                    killAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Misc Awards", miscAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Contract Awards", contractAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Rank Awards", rankAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Scenario Awards", scenarioAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Skill Awards", skillAwards.size());
                            LogManager.getLogger().info("autoAwards found {} TheatreOfWar Awards", theatreOfWarAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Time Awards", timeAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Training Awards", trainingAwards.size());
                            LogManager.getLogger().info("autoAwards is ignoring {} Awards", ignoredAwards.size());

                            break;
                        // post-scenario
                        case 2:
                            for (Award award : awards) {
                                switch (award.getItem().toLowerCase().replaceAll("\\s", "")) {
                                    case "kill":
                                        if ((campaign.getCampaignOptions().isEnableKillAwards())
                                                && (award.getRange().equalsIgnoreCase("scenario"))) {

                                            killAwards.add(award);
                                        }
                                        break;
                                    case "injury":
                                        if (campaign.getCampaignOptions().isEnableInjuryAwards()) {

                                            injuryAwards.add(award);
                                        }
                                        break;
                                    default:
                                        ignoredAwards.add(award);
                                }
                            }

                            LogManager.getLogger().info("autoAwards found {} Scenario Kill Awards (excluding Mission & Lifetime Kill Awards)",
                                    killAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Injury Awards", injuryAwards.size());

                            break;
                        default:
                            throw new IllegalStateException("Unexpected awardListCase: " + awardListCase);
                    }
                } else {
                    LogManager.getLogger().info("autoAwards failed to find any awards in set {}", setName);
                }
            }
        } else {
            LogManager.getLogger().info("AutoAwards failed to find any award sets");
        }
    }

    /**
     * Loops through provided personnel, checking whether each is eligible for an award
     * @param personnel all personnel that should be checked for award eligibility
     * @param missionWasSuccessful whether the Mission ended in a Success
     */
    private void ProcessAwards(Collection<Person> personnel, Boolean missionWasSuccessful) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        Map<Integer, List<Object>> data;

        // this gives us an incremental int that we can use as a key for awardData
        int awardDataKey = 0;

        boolean skipAll = false;

        // Ideally, we'd not be doing multiple passes of Person or Award, but we don't have that luxury. By processing
        // Award Eligibility in the manner below, we can stagger processing of Award Types, which reduces the likelihood
        // that autoAwards will lock up mhq. Really, this isn't an ideal solution, but it's the best I could muster at
        // time of writing.
        if ((!contractAwards.isEmpty()) && (mission instanceof Contract)) {
            for (Person person: personnel) {
                new ContractAwards(campaign, mission, contractAwards, person);
            }
        }

        if ((!skipAll) && (!factionHunterAwards.isEmpty())
                && (campaign.getCampaignOptions().isUseAtB())
                && (mission instanceof AtBContract)) {
            for (Person person: personnel) {
                new FactionHunterAwards(campaign, mission, factionHunterAwards, person);
            }
        }

        // even if someone doesn't have a Combat Role, we still check combat-related Awards as we've no way to check
        // whether Person previously held a Combat Role earlier in the Mission
        if ((!skipAll) && (!killAwards.isEmpty())) {
            for (Person person: personnel) {
                try {
                    data = KillAwards.KillAwardProcessor(campaign, mission, killAwards, person);
                } catch (Exception e) {
                    data = null;
                    LogManager.getLogger().info("{} is not eligible for any Kill Awards.", person.getFullName());
                }

                if (data != null) {
                    for (Integer dataKey : data.keySet()) {
                        awardData.put(awardDataKey, data.get(dataKey));

                        awardDataKey++;
                    }
                }
            }

            // if awardData is empty, we just skip the Dialog for this type of Award. This ensures we don't
            // end up with a bunch of empty Dialogs.
            if (!awardData.isEmpty()) {
                AutoAwardsDialog autoAwardsDialog = new AutoAwardsDialog(campaign, awardData);
                autoAwardsDialog.setVisible(true);

                skipAll = autoAwardsDialog.wasSkipAll();
            } else {
                LogManager.getLogger().info("Zero personnel were found eligible for Kill Awards");
            }
        }

        if ((!skipAll) && (!miscAwards.isEmpty())) {
            for (Person person: personnel) {
                new MiscAwards(campaign, miscAwards, person, missionWasSuccessful);
            }
        }

        if ((!skipAll) && (!rankAwards.isEmpty())) {
            for (Person person: personnel) {
                new RankAwards(campaign, rankAwards, person);
            }
        }

        if ((!skipAll) && (!scenarioAwards.isEmpty())) {
            for (Person person: personnel) {
                new ScenarioAwards(campaign, scenarioAwards, person);
            }
        }

        if ((!skipAll) && (!skillAwards.isEmpty())) {
            for (Person person: personnel) {
                new SkillAwards(campaign, skillAwards, person);
            }
        }

        // theatre of war awards are based on employer, only Contracts have employers
        if ((!skipAll) && (!theatreOfWarAwards.isEmpty()) && (mission instanceof Contract)) {
            for (Person person: personnel) {
                new TheatreOfWarAwards(campaign, mission, theatreOfWarAwards, person);
            }
        }

        if ((!skipAll) && (!timeAwards.isEmpty())) {
            for (Person person: personnel) {
                new TimeAwards(campaign, person, timeAwards);
            }
        }
    }
}
