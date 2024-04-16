package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.AwardsFactory;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AutoAwardsController {
    private Campaign campaign;

    private List<Award> contractAwards = new ArrayList<>();
    private List<Award> injuryAwards = new ArrayList<>();
    private List<Award> killAwards = new ArrayList<>();
    private List<Award> miscAwards = new ArrayList<>();
    private List<Award> rankAwards = new ArrayList<>();
    private List<Award> scenarioAwards = new ArrayList<>();
    private List<Award> skillAwards = new ArrayList<>();
    private List<Award> theatreOfWarAwards = new ArrayList<>();
    private List<Award> timeAwards = new ArrayList<>();
    private List<Award> trainingAwards = new ArrayList<>();

    /**
     * The primary controller for the automatic processing of Awards
     * @param campaign the campaign to be processed
     * @param missionWasSuccessful @Nullable true if Mission was a complete Success, otherwise false, should also be false if not run at the end of a Mission
     */
    public AutoAwardsController(Campaign c, Mission mission, Boolean missionWasSuccessful) {
        LogManager.getLogger().info("autoAwards has started");

        campaign = c;

        buildAwardLists();

        Collection<Person> personnel = campaign.getActivePersonnel();

        if (!personnel.isEmpty()) {
            // Prisoners and Dependents are not eligible for Awards
            removeDependentsAndPrisoners(personnel);

            // we have to do multiple isEmpty() checks as any point in the removal process could result in null personnel
            if(!personnel.isEmpty()) {
                // This is the main workhorse function
                ProcessAwards(personnel, mission, missionWasSuccessful);
            }

            // TODO add Posthumous Awards to Campaign Options
            boolean fakeCampaignOptionPosthumousAwardsIsEnabled = false;

            if (fakeCampaignOptionPosthumousAwardsIsEnabled) {
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
                    ProcessAwards(deceasedPersonnel, mission, missionWasSuccessful);
                } else {
                    LogManager.getLogger().info("AutoAwards found no deceased personnel, skipping this step");
                }
            }
        } else {
            LogManager.getLogger().info("AutoAwards found no active personnel, skipping the award ceremony");
        }

        LogManager.getLogger().info("autoAwards has finished");
    }

    /**
     * Loops through provided personnel, checking whether each is eligible for an award
     * @param personnel all personnel that should be checked for award eligibility
     * @param missionWasSuccessful whether the Mission ended in a Success
     */
    private void ProcessAwards(Collection<Person> personnel, Mission mission, Boolean missionWasSuccessful) {
        for (Person person : personnel) {
            if ((!contractAwards.isEmpty()) && (mission instanceof Contract)) {
                new ContractAwards(campaign, mission, contractAwards, person);
            }

            // even if someone doesn't have a Combat Role, we still check combat-related Awards as we've no way to check
            // whether Person previously held a Combat Role earlier in the Mission
            if (!killAwards.isEmpty()) {
                new KillAwards(campaign, killAwards, person);
            }

            if (!miscAwards.isEmpty()) {
                new MiscAwards(campaign, miscAwards, person, missionWasSuccessful);
            }

            if (!rankAwards.isEmpty()) {
                new RankAwards(campaign, rankAwards, person);
            }

            if (!scenarioAwards.isEmpty()) {
                new ScenarioAwards(campaign, scenarioAwards, person);
            }

            if (!skillAwards.isEmpty()) {
                new SkillAwards(campaign, skillAwards, person);
            }

            if (!theatreOfWarAwards.isEmpty()) {
                new TheatreOfWarAwards(campaign, theatreOfWarAwards, person);
            }

            if (!timeAwards.isEmpty()) {
                new TimeAwards(campaign, timeAwards, person);
            }
        }
    }

    /**
     * Builds the award list and filters it, so we're not processing the same medals multiple times
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

                    // next we begin to filter the awards into discrete lists
                    for (Award award : awards) {
                        switch (award.getItem().toLowerCase().replaceAll("\\s","")) {
                            // TODO track InjuryAwards at the end of a scenario
                            // InjuryAwards are issued immediately after a scenario
                            // We include them here, for tracking purposes
                            case "injury":
                                injuryAwards.add(award);
                                break;
                            // TODO kill awards
                            case "kill":
                                killAwards.add(award);
                                break;
                            case "scenario":
                                scenarioAwards.add(award);
                                break;
                            case "rank":
                                rankAwards.add(award);
                                break;
                            case "contract":
                                contractAwards.add(award);
                                break;
                            // TODO theatre of war awards
                            case "theatreofwar":
                                theatreOfWarAwards.add(award);
                                break;
                            case "skill":
                                skillAwards.add(award);
                                break;
                            case "time":
                                timeAwards.add(award);
                                break;
                            case "training":
                                trainingAwards.add(award);
                                break;
                            // TODO hardcode more misc awards
                            case "misc":
                                miscAwards.add(award);
                                break;
                            default:
                                // TODO add file directory for documentation
                                LogManager.getLogger().info("AutoAwards failed to find a valid award type for {} from the {} set. Please see DOCUMENT ADDRESS", award.getName(), setName);
                        }
                    }

                    // These logs help users double-check that the number of awards found matches their records
                    LogManager.getLogger().info("autoAwards found {} Injury Awards", injuryAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Kill Awards", killAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Misc Awards", miscAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Contract Awards", contractAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Rank Awards", rankAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Scenario Awards", scenarioAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Skill Awards", skillAwards.size());
                    LogManager.getLogger().info("autoAwards found {} TheatreOfWar Awards", theatreOfWarAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Time Awards", timeAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Training Awards", trainingAwards.size());
                } else {
                    LogManager.getLogger().info("autoAwards failed to find any awards in set {}", setName);
                }
            }
        } else {
            LogManager.getLogger().info("AutoAwards failed to find any award sets");
        }
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
}
