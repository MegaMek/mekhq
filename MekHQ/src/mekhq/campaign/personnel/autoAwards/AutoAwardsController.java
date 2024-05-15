package mekhq.campaign.personnel.autoAwards;

import megamek.common.annotations.Nullable;
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
     * The controller for the manual-automatic processing of Awards
     *
     * @param c                    the campaign to be processed
     */
    public void ManualController(Campaign c) {
        LogManager.getLogger().info("autoAwards (Manual) has started");

        campaign = c;
        mission = null;

        buildAwardLists(0);

        Collection<UUID> personnel = getPersonnel();

        // we have to do multiple isEmpty() checks as, at any point in the removal process, we could end up with null personnel
        if (!personnel.isEmpty()) {
            // This is the main workhorse function
            ProcessAwards(personnel, false);
        } else {
            LogManager.getLogger().info("AutoAwards found no personnel, skipping the Award Ceremony");
        }

        LogManager.getLogger().info("autoAwards (Manual) has finished");
    }

    /**
     * The primary controller for the automatic processing of Awards
     *
     * @param c                    the campaign to be processed
     * @param m                    the mission just completed
     * @param missionWasSuccessful true if the Mission was a complete Success, otherwise false
     */
    public void PostMissionController(Campaign c, Mission m, Boolean missionWasSuccessful) {
        LogManager.getLogger().info("autoAwards (Mission Conclusion) has started");

        campaign = c;
        mission = m;

        buildAwardLists(1);

        Collection<UUID> personnel = getPersonnel();

        // we have to do multiple isEmpty() checks as, at any point in the removal process, we could end up with null personnel
        if (!personnel.isEmpty()) {
            // This is the main workhorse function
            ProcessAwards(personnel, missionWasSuccessful);
        } else {
            LogManager.getLogger().info("AutoAwards found no personnel, skipping the Award Ceremony");
        }

        LogManager.getLogger().info("autoAwards (Mission Conclusion) has finished");
    }

    /**
     * This function processes Kill (Scenario) and Injury Awards following the conclusion of a Scenario
     *
     * @param c           the campaign to be processed
     * @param scenarioId  id number for the Scenario just concluded
     * @param person      the person to check award eligibility for
     * @param injuryCount the number of Hits sustained in the Scenario just concluded
     */
    public void PostScenarioController(Campaign c, int scenarioId, UUID person, int injuryCount) {
        LogManager.getLogger().info("autoAwards (Scenario Conclusion) has started");

        campaign = c;

        // we convert a person into a Collection, as it allows us to use InjuryAwardsManager
        // outside of this function
        Collection<UUID> personnel = new ArrayList<>();
        personnel.add(person);

        buildAwardLists(2);

        Map<Integer, Map<Integer, List<Object>>> allAwardData = new HashMap<>();
        Map<Integer, List<Object>> processedData;
        int allAwardDataKey = 0;

        // beginning the processing of Injury Awards
        if ((injuryCount > 0) && (!injuryAwards.isEmpty())) {
            processedData = InjuryAwardsManager(personnel, injuryCount);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        // beginning the processing & filtering of Kill (Scenario) Awards
        List<Kill> kills = campaign.getKillsFor(campaign.getPerson(person).getId());
        kills.removeIf(kill -> kill.getScenarioId() != scenarioId);

        if ((!kills.isEmpty()) && (!killAwards.isEmpty())) {
            processedData = ScenarioKillAwardsManager(personnel, kills);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
            }
        }

        // beginning the processing & filtering of Misc Awards
        if ((injuryCount > 0) && (!kills.isEmpty()) && (!miscAwards.isEmpty())) {
            processedData = MiscAwardsManager(personnel, false, kills.size(), injuryCount);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
            }
        }

        if (!allAwardData.isEmpty()) {
            AutoAwardsDialog autoAwardsDialog = new AutoAwardsDialog(campaign, allAwardData, 0);
            autoAwardsDialog.setVisible(true);
        } else {
            LogManager.getLogger().info("Zero personnel were found eligible for Awards");
        }

        LogManager.getLogger().info("autoAwards (Scenario Conclusion) has finished");
    }

    /**
     * Builds a list of personnel autoAwards should process
     */
    private Collection<UUID> getPersonnel() {
        Collection<UUID> personnel = campaign.getActivePersonnel()
                .stream()
                .map(Person::getId)
                .collect(Collectors.toList());

        // if posthumous Awards are enabled, we add the relevant dead people
        if (campaign.getCampaignOptions().isIssuePosthumousAwards()) {
            Collection<UUID> deadPeople = campaign.getPersonnel()
                    .stream().filter(person -> person.getStatus().isDead())
                    .map(Person::getId)
                    .collect(Collectors.toList());

            LogManager.getLogger().info("deadPeople {}", deadPeople);

            if (!deadPeople.isEmpty()) {
                personnel.addAll(deadPeople);
            }
        }

        // Prisoners and Dependents are not eligible for Awards
        if (!personnel.isEmpty()) {
            removeDependentsAndPrisoners(personnel);
        }
        return personnel;
    }

    /**
     * Filters out anyone with the Prisoner status, or Dependent role
     *
     * @param personnel personnel to process
     */
    private void removeDependentsAndPrisoners(Collection<UUID> personnel) {
        if (!personnel.isEmpty()) {
            personnel.removeIf(person -> (campaign.getPerson(person).hasRole(PersonnelRole.DEPENDENT))
                    || (campaign.getPerson(person).getPrisonerStatus().isCurrentPrisoner()));
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

        if (campaign.getCampaignOptions().isIgnoreStandardSet()) {
            allSetNames.removeIf(setName -> setName.equalsIgnoreCase("standard"));
            LogManager.getLogger().info("Ignoring the Standard Set");
        }

        // we start by building a primary list of all awards
        if (!allSetNames.isEmpty()) {
            LogManager.getLogger().info("Getting all Award Sets");

            for (String setName : allSetNames) {
                if (!allSetNames.isEmpty()) {
                    LogManager.getLogger().info("Getting all awards from set: {}", setName);

                    awards.addAll(AwardsFactory.getInstance().getAllAwardsForSet(setName));

                    // next, we begin to filter the awards into discrete lists
                    switch (awardListCase) {
                        // Manual
                        case 0:
                            for (Award award : awards) {
                                switch (award.getItem().toLowerCase().replaceAll("\\s", "")) {
                                    case "group":
                                        break;
                                    case "ignore":
                                    case "contract":
                                    case "factionhunter":
                                    case "injury":
                                    case "theatreofwar":
                                        ignoredAwards.add(award);
                                        break;
                                    case "kill":
                                        if ((!award.getRange().equalsIgnoreCase("scenario"))
                                                && (!award.getRange().equalsIgnoreCase("mission"))) {
                                            if (campaign.getCampaignOptions().isEnableFormationKillAwards()) {
                                                killAwards.add(award);
                                            } else {
                                                ignoredAwards.add(award);
                                            }
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
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
                                    case "time":
                                        if (campaign.getCampaignOptions().isEnableTimeAwards()) {
                                            timeAwards.add(award);
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
                                    // trainingAwards are not currently supported.
                                    // We include them here for tracking purposes
                                    case "training":
                                        trainingAwards.add(award);
                                        break;
                                    default:
                                        // if autoAwards doesn't know what to do with an Award, it ignores it
                                        ignoredAwards.add(award);
                                }
                            }
                            // These logs help users double-check that the number of awards found matches their records
                            LogManager.getLogger().info("autoAwards found {} Kill Awards (excluding Mission & Scenario Kill Awards)", killAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Misc Awards", miscAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Rank Awards", rankAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Scenario Awards", scenarioAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Skill Awards", skillAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Time Awards", timeAwards.size());
                            LogManager.getLogger().info("autoAwards found {} Training Awards", trainingAwards.size());
                            LogManager.getLogger().info("autoAwards is ignoring {} Awards", ignoredAwards.size());

                            break;
                        // post-mission
                        case 1:
                            for (Award award : awards) {
                                switch (award.getItem().toLowerCase().replaceAll("\\s", "")) {
                                    case "ignore":
                                        ignoredAwards.add(award);
                                        break;
                                    case "divider":
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
                                        if (!award.getRange().equalsIgnoreCase("scenario")) {
                                            if ((campaign.getCampaignOptions().isEnableIndividualKillAwards())
                                                    || (campaign.getCampaignOptions().isEnableFormationKillAwards())) {
                                                killAwards.add(award);
                                            } else {
                                                ignoredAwards.add(award);
                                            }
                                        } else {
                                            ignoredAwards.add(award);
                                        }
                                        break;
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
                                    case "training":
                                        trainingAwards.add(award);
                                        break;
                                    default:
                                        // if autoAwards doesn't know what to do with an Award, it ignores it
                                        ignoredAwards.add(award);
                                }
                            }
                            // These logs help users double-check that the number of awards found matches their records
                            LogManager.getLogger().info("autoAwards found {} Kill Awards (excluding Scenario Kill Awards)", killAwards.size());
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
                                    case "divider":
                                        break;
                                    case "kill":
                                        if ((campaign.getCampaignOptions().isEnableIndividualKillAwards()) && (award.getRange().equalsIgnoreCase("scenario"))) {

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

                            LogManager.getLogger().info("autoAwards found {} Scenario Kill Awards (excluding Mission & Lifetime Kill Awards)", killAwards.size());
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
     * Process the awards for the given personnel.
     *
     * @param personnel               the collection of personnel to process awards for
     * @param missionWasSuccessful    true if the mission was successful, false otherwise
     */
    private void ProcessAwards(Collection<UUID> personnel, Boolean missionWasSuccessful) {
        Map<Integer, Map<Integer, List<Object>>> allAwardData = new HashMap<>();
        Map<Integer, List<Object>> processedData;
        int allAwardDataKey = 0;

        if ((!contractAwards.isEmpty()) && (mission instanceof Contract)) {
            processedData = ContractAwardsManager(personnel);

            // if processedData == null, nobody was eligible for this type of award, so they should be skipped
            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        if ((!factionHunterAwards.isEmpty()) && (campaign.getCampaignOptions().isUseAtB()) && (mission instanceof AtBContract)) {
            processedData = FactionHunterAwardsManager(personnel);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        // even if someone doesn't have a Combat Role, we still check combat-related Awards as we've no way to check
        // whether Person previously held a Combat Role earlier in the Mission
        if (!killAwards.isEmpty()) {
            processedData = KillAwardsManager(personnel);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        if (!miscAwards.isEmpty()) {
            processedData = MiscAwardsManager(personnel, missionWasSuccessful, null, null);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        if (!rankAwards.isEmpty()) {
            processedData = RankAwardsManager(personnel);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        if (!scenarioAwards.isEmpty()) {
            processedData = ScenarioAwardsManager(personnel);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        if (!skillAwards.isEmpty()) {
            processedData = SkillAwardsManager(personnel);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        if ((!theatreOfWarAwards.isEmpty()) && (mission instanceof Contract)) {
            processedData = TheatreOfWarAwardsManager(personnel);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        if (!timeAwards.isEmpty()) {
            processedData = TimeAwardsManager(personnel);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
            }
        }

        if (!allAwardData.isEmpty()) {
            AutoAwardsDialog autoAwardsDialog = new AutoAwardsDialog(campaign, allAwardData, 0);
            autoAwardsDialog.setVisible(true);
        } else {
            LogManager.getLogger().info("Zero personnel were found eligible for Awards");
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> ContractAwardsManager(Collection<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                // this gives us a map of unique identifier (int), containing a list of Person, and Award
                data = ContractAwards.ContractAwardsProcessor(campaign, mission, person, contractAwards);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Contract Awards.",
                        campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                // as the unique identifiers get doubled-up, as we loop through personnel, we extract the data from
                // data and insert it into awardData (now under a new, truly unique, int identifier).
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        // this gives us a map of unique identifier (int), containing a list of Person, and Award
        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> FactionHunterAwardsManager(Collection<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;

            try {
                data = FactionHunterAwards.FactionHunterAwardsProcessor(campaign, mission, person, factionHunterAwards);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Faction Hunter Awards.",
                        campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> InjuryAwardsManager(Collection<UUID> personnel, int injuryCount) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;

            try {
                data = InjuryAwards.InjuryAwardsProcessor(campaign, person, injuryAwards, injuryCount);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Injury Awards.", campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> KillAwardsManager(Collection<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = KillAwards.KillAwardProcessor(campaign, mission, person, killAwards);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Kill Awards.",
                        campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> ScenarioKillAwardsManager(Collection<UUID> personnel, List<Kill> kills) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = ScenarioKillAwards.ScenarioKillAwardsProcessor(campaign, person, killAwards, kills);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Scenario Kill Awards.",
                        campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This method is the manager for processing miscellaneous awards.
     *
     * @param personnel             the collection of personnel to be processed for awards
     * @param missionWasSuccessful  a boolean indicating if the mission was successful
     * @param killCount             an optional parameter specifying the kill count
     * @param injuryCount           an optional parameter specifying the injury count
     * @return a map containing the award data, or null if no awards are applicable
     */
    private Map<Integer, List<Object>> MiscAwardsManager(Collection<UUID> personnel, boolean missionWasSuccessful,
                                                         @Nullable Integer killCount, @Nullable Integer injuryCount) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = MiscAwards.MiscAwardsProcessor(campaign, mission, person, miscAwards, missionWasSuccessful, killCount, injuryCount);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Misc Awards.", campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> RankAwardsManager(Collection<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = RankAwards.RankAwardsProcessor(campaign, person, rankAwards);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Rank Awards.", campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> ScenarioAwardsManager(Collection<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = ScenarioAwards.ScenarioAwardsProcessor(campaign, person, scenarioAwards);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Scenario Awards.", campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> SkillAwardsManager(Collection<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = SkillAwards.SkillAwardsProcessor(campaign, person, skillAwards);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Skill Awards.", campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> TheatreOfWarAwardsManager(Collection<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = TheatreOfWarAwards.TheatreOfWarAwardsProcessor(campaign, mission, person, theatreOfWarAwards);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Theatre of War Awards.", campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            return null;
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> TimeAwardsManager(Collection<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = TimeAwards.TimeAwardsProcessor(campaign, person, timeAwards);
            } catch (Exception e) {
                data = null;
                LogManager.getLogger().info("{} is not eligible for any Time Awards.", campaign.getPerson(person).getFullName());
            }

            if (data != null) {
                for (Integer dataKey : data.keySet()) {
                    awardData.put(awardDataKey, data.get(dataKey));

                    awardDataKey++;
                }
            }
        }

        if (!awardData.isEmpty()) {
            return awardData;
        } else {
            LogManager.getLogger().info("Returning: null");
            return null;
        }
    }

    /**
     * This is called from within an Award Type module and prepares data for use by displayAwardCeremony()
     *
     * @param person the person being processed
     * @param eligibleAwards the Awards they are eligible for
     */
    public static Map<Integer, List<Object>> prepareAwardData(UUID person, List<Award> eligibleAwards) {
        Map<Integer, List<Object>> awardData = new HashMap<>();

        int awardDataKey = 0;

        for (Award award : eligibleAwards) {
            List<Object> personAwardList = new ArrayList<>();

            // this gives us a list containing [Person, Award, true]
            personAwardList.add(person);
            personAwardList.add(1, award);
            personAwardList.add(true);

            // we store that list under a unique key
            awardData.put(awardDataKey, personAwardList);

            // Increment the key for the next iteration
            awardDataKey++;
        }

        return awardData;
    }
}
