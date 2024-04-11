package mekhq.campaign.personnel.autoMedals;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.AwardsFactory;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AutoAwardsController {
    private Campaign campaign;

    private List<Award> killAwards = new ArrayList<>();
    private List<Award> miscAwards = new ArrayList<>();
    private List<Award> missionAccomplishedAwards = new ArrayList<>();
    // MissionAwards are disabled until a way to track how many Missions a person has completed is introduced
//    private List<Award> missionAwards = new ArrayList<>();
    private List<Award> scenarioAwards = new ArrayList<>();
    private List<Award> skillAwards = new ArrayList<>();
    private List<Award> theatreOfWarAwards = new ArrayList<>();
    private List<Award> timeAwards = new ArrayList<>();

    public AutoAwardsController(Campaign c, Boolean missionWasSuccessful) {
        campaign = c;

        LogManager.getLogger().info("autoAwards has started");

        buildAwardLists();

        Collection<Person> personnel = campaign.getActivePersonnel();

        if (!personnel.isEmpty()) {
            ProcessAwards(personnel, missionWasSuccessful);

            // TODO add Posthumous Awards to Campaign Options
            boolean fakeCampaignOptionPosthumousAwardsIsEnabled = false;
            if (fakeCampaignOptionPosthumousAwardsIsEnabled) {
                Collection<Person> deadPersonnel = campaign.getDeceasedPersonnel();

                if (deadPersonnel.isEmpty()) {
                    // even dead Dependants don't get awards
                    personnel.removeIf(person -> person.hasRole(PersonnelRole.DEPENDENT));

                    ProcessAwards(personnel, missionWasSuccessful);
                } else {
                    LogManager.getLogger().info("AutoAwards found no deceased personnel, skipping this step");
                }
            }
        } else {
            LogManager.getLogger().info("AutoAwards found no active personnel, skipping the award ceremony");
        }
    }

    /**
     * Loops through provided personnel, checking whether each is eligible for an award
     * @param personnel all personnel that should be checked for award eligibility
     * @param missionWasSuccessful whether the Mission ended in a Success
     */
    private void ProcessAwards(Collection<Person> personnel, Boolean missionWasSuccessful) {
        for (Person person : personnel) {
            // If Mission was unsuccessful, there is no point in checking eligibility for Mission Accomplished awards
            if ((missionWasSuccessful) && (!missionAccomplishedAwards.isEmpty())) {
                new MissionAccomplishedAwards(campaign, missionAccomplishedAwards, person);
            }

            if (!theatreOfWarAwards.isEmpty()) {
                new TheatreOfWarAwards(campaign, theatreOfWarAwards, person);
            }

            if (!timeAwards.isEmpty()) {
                new TimeAwards(campaign, timeAwards, person);
            }

            if (!skillAwards.isEmpty()) {
                new SkillAwards(campaign, skillAwards, person);
            }

            if (!miscAwards.isEmpty()) {
                new MiscAwards(campaign, miscAwards, person);
            }

            // is person has no combat role, we don't need to check combat related medals
            if (person.hasCombatRole()) {
                if (!killAwards.isEmpty()) {
                    new KillAwards(campaign, killAwards, person);
                }

                if (!scenarioAwards.isEmpty()) {
                    new ScenarioAwards(campaign, scenarioAwards, person);
                }

//                if (!missionAwards.isEmpty()) {
//                    new MissionAwards(campaign, missionAwards, person);
//                }
            }
        }
        LogManager.getLogger().info("autoAwards has finished");
    }
    /**
     * Builds the award list and filters it, so we're not processing the same medals multiple times
     */
    private void buildAwardLists() {
        ArrayList<Award> awards = new ArrayList<>();
        List<String> allSetNames = AwardsFactory.getInstance().getAllSetNames();

        // we start by building a master list of all awards
        if (!allSetNames.isEmpty()) {
            LogManager.getLogger().info("Getting all set names");

            for (String setName : AwardsFactory.getInstance().getAllSetNames()) {
                if (!allSetNames.isEmpty()) {
                    LogManager.getLogger().info("Getting all awards from set: {}", setName);

                    awards.addAll(AwardsFactory.getInstance().getAllAwardsForSet(setName));

                    // next we begin to filter the awards into discrete lists
                    for (Award award : awards) {
                        switch (award.getItem()) {
                            // TODO track InjuryAwards at the end of a scenario
                            // InjuryAwards are missing because we issue those immediately after a scenario
                            case "Kill":
                                killAwards.add(award);
                                break;
                            case "Scenario":
                                scenarioAwards.add(award);
                                break;
                            case "MissionAccomplished":
                                missionAccomplishedAwards.add(award);
                                break;
//                            case "Mission":
//                                missionAwards.add(award);
//                                break;
                            case "TheatreOfWar":
                                theatreOfWarAwards.add(award);
                                break;
                            case "Skill":
                                skillAwards.add(award);
                                break;
                            case "Time":
                                timeAwards.add(award);
                                break;
                            case "Misc":
                                miscAwards.add(award);
                                break;
                            default:
                                // TODO add file directory for documentation
                                LogManager.getLogger().info("AutoAwards failed to find a valid award type for {} from the {} set. Please see DOCUMENT ADDRESS", award.getName(), setName);
                        }
                    }

                    // These logs help users double-check that the number of awards found matches their records
                    LogManager.getLogger().info("autoAwards found {} Kill awards", killAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Misc awards", miscAwards.size());
                    LogManager.getLogger().info("autoAwards found {} MissionAccomplished awards", missionAccomplishedAwards.size());
//                    LogManager.getLogger().info("autoAwards found {} Mission awards", missionAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Scenario awards", scenarioAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Skill awards", skillAwards.size());
                    LogManager.getLogger().info("autoAwards found {} TheatreOfWar awards", theatreOfWarAwards.size());
                    LogManager.getLogger().info("autoAwards found {} Time awards", timeAwards.size());
                } else {
                    LogManager.getLogger().info("AutoAwards failed to find any awards in set {}", setName);
                }
            }
        } else {
            LogManager.getLogger().info("AutoAwards failed to find any award sets");
        }
    }
}
