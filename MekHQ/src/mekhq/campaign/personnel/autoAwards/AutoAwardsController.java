/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.personnel.autoAwards;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.AwardsFactory;
import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.AutoAwardsDialog;

import javax.swing.*;
import java.awt.Dialog.ModalityType;
import java.time.LocalDate;
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

    private static final MMLogger logger = MMLogger.create(AutoAwardsController.class);

    /**
     * The controller for the manual-automatic processing of Awards
     *
     * @param campaign                    the campaign to be processed
     */
    public void ManualController(Campaign campaign, boolean isManualPrompt) {
        logger.info("autoAwards (Manual) has started");

        this.campaign = campaign;
        mission = null;

        buildAwardLists(0);

        List<UUID> personnel = getPersonnel();

        // we have to do multiple isEmpty() checks as, at any point in the removal process, we could end up with null personnel
        if (!personnel.isEmpty()) {
            // This is the main workhorse function
            ProcessAwards(personnel, false, null, isManualPrompt);
        } else {
            logger.info("AutoAwards found no personnel, skipping the Award Ceremony");

            final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AutoAwardsDialog",
                    MekHQ.getMHQOptions().getLocale());

            if (isManualPrompt) {
                JOptionPane.showMessageDialog(null,
                        resources.getString("txtNoneEligible.text"),
                        resources.getString("AutoAwardsDialog.title"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }

        logger.info("autoAwards (Manual) has finished");
    }

    /**
     * The controller for the processing of awards prompted by a change in rank
     *
     * @param campaign                    the campaign to be processed
     */
    public void PromotionController(Campaign campaign, boolean isManualPrompt) {
        logger.info("autoAwards (Promotion) has started");

        this.campaign = campaign;
        mission = null;

        buildAwardLists(4);

        List<UUID> personnel = getPersonnel();

        // we have to do multiple isEmpty() checks as, at any point in the removal process, we could end up with null personnel
        if (!personnel.isEmpty()) {
            // This is the main workhorse function
            ProcessAwards(personnel, false, null, isManualPrompt);
        } else {
            logger.info("AutoAwards found no personnel, skipping the Award Ceremony");
        }

        logger.info("autoAwards (Promotion) has finished");
    }

    /**
     * The primary controller for the automatic processing of Awards
     *
     * @param campaign                    the campaign to be processed
     * @param mission                    the mission just completed
     * @param missionWasSuccessful true if the Mission was a complete Success, otherwise false
     */
    public void PostMissionController(Campaign campaign, Mission mission, Boolean missionWasSuccessful) {
        logger.info("autoAwards (Mission Conclusion) has started");

        this.campaign = campaign;
        this.mission = mission;

        buildAwardLists(1);

        List<UUID> personnel = getPersonnel();

        // we have to do multiple isEmpty() checks as, at any point in the removal process, we could end up with null personnel
        if (!personnel.isEmpty()) {
            // This is the main workhorse function
            ProcessAwards(personnel, missionWasSuccessful, null, false);
        } else {
            logger.info("AutoAwards found no personnel, skipping the Award Ceremony");
        }

        logger.info("autoAwards (Mission Conclusion) has finished");
    }

    /**
     * Processes awards after a scenario is concluded.
     *
     * @param campaign the campaign
     * @param personnel the personnel involved in the scenario, mapped by their UUID
     * @param scenarioKills the kills made during the scenario, mapped by personnel UUID
     * @param wasCivilianHelp whether the scenario (if any) was AtB Scenario CIVILIANHELP
     */
    public void PostScenarioController(Campaign campaign, HashMap<UUID, Integer> personnel, HashMap<UUID, List<Kill>>scenarioKills, boolean wasCivilianHelp) {
        logger.info("autoAwards (Scenario Conclusion) has started");

        this.campaign = campaign;

        buildAwardLists(2);

        Map<Integer, Map<Integer, List<Object>>> allAwardData = new HashMap<>();
        Map<Integer, List<Object>> processedData;
        int allAwardDataKey = 0;

        // beginning the processing of Injury Awards
        if (!injuryAwards.isEmpty()) {
            processedData = InjuryAwardsManager(personnel);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        // beginning the processing of Kill (Scenario) Awards
        if (!killAwards.isEmpty()) {
            processedData = ScenarioKillAwardsManager(new ArrayList<>(personnel.keySet()), scenarioKills);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        // beginning the processing & filtering of Misc Awards
        if (!miscAwards.isEmpty()) {
            processedData = MiscAwardsManager(personnel, false, true, wasCivilianHelp, scenarioKills);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
                allAwardDataKey++;
            }
        }

        // beginning the processing & filtering of scenario awards
        if (!scenarioAwards.isEmpty()) {
            processedData = ScenarioAwardsManager(new ArrayList<>(personnel.keySet()));

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
            }
        }

        if (!allAwardData.isEmpty()) {
            AutoAwardsDialog autoAwardsDialog = new AutoAwardsDialog(this.campaign, allAwardData, 0);
            autoAwardsDialog.setModalityType(ModalityType.APPLICATION_MODAL);
            autoAwardsDialog.setLocation(autoAwardsDialog.getLocation().x, 0);
            autoAwardsDialog.setVisible(true);
        } else {
            logger.info("Zero personnel were found eligible for Awards");
        }

        logger.info("autoAwards (Scenario Conclusion) has finished");
    }

    /**
     * The primary controller for the automatic processing of Training Awards
     *
     * @param campaign                    the campaign to be processed
     */
    public void PostGraduationController(Campaign campaign, List<UUID> personnel, HashMap<UUID, List<Object>> academyAttributes) {
        logger.info("autoAwards (Education Conclusion) has started");

        this.campaign = campaign;

        buildAwardLists(3);

        // we have to do multiple isEmpty() checks as, at any point in the removal process, we could end up with null personnel
        if (!personnel.isEmpty()) {
            // This is the main workhorse function
            ProcessAwards(personnel, false, academyAttributes, false);
        } else {
            logger.info("AutoAwards found no personnel, skipping the Award Ceremony");
        }

        logger.info("autoAwards (Education Conclusion) has finished");
    }

    /**
     * Builds a list of personnel autoAwards should process
     */
    private List<UUID> getPersonnel() {
        // Get whether to issue posthumous awards from campaign options.
        boolean issuePosthumous = campaign.getCampaignOptions().isIssuePosthumousAwards();

        // Get the last mission end date.
        LocalDate lastMissionEndDate = getLastMissionEndDate();

        // Fetch all personnel in the campaign.
        Collection<Person> rawPersonnel = campaign.getPersonnel();
        // Create a list to store Ids of qualifying personnel.
        List<UUID> personnel = new ArrayList<>();

        // Iterate through each person in the rawPersonnel collection.
        for (Person person : rawPersonnel) {
            // Check conditions of the person being active, not currently a prisoner, and not a civilian.
            // If all conditions are met, add their ID to the list.
            if ((person.getStatus().isActive())
                    && (!person.getPrisonerStatus().isCurrentPrisoner())
                    && (!person.getPrimaryRole().isCivilian())) {
                personnel.add(person.getId());
            }

            // Check the conditions of the person being dead, the issuePosthumous flag being true,
            // the person not currently being a prisoner, and not a civilian.
            // If all conditions are met, further check if the person's date of death
            // is after or equal to the last mission's end date.
            // If all these conditions are met, add their ID to the list.
            if (person.getStatus().isDead()
                    && (issuePosthumous)
                    && (!person.getPrisonerStatus().isCurrentPrisoner())
                    && (!person.getPrimaryRole().isCivilian())) {
                if (person.getDateOfDeath().isAfter(lastMissionEndDate) || person.getDateOfDeath().equals(lastMissionEndDate)) {
                    personnel.add(person.getId());
                }
            }
        }

        // Log the details of the personnel list.
        logger.debug("Personnel {}", personnel);

        // Return the list of qualifying personnel IDs.
        return personnel;
    }

    /**
     * Returns the last mission end date based on the campaign's completed AtBContracts.
     *
     * @return the date of the last mission end, or today's date if no completed contracts exist
     */
    private LocalDate getLastMissionEndDate() {
        // Get the current date from the campaign object.
        LocalDate today = campaign.getLocalDate();

        // Get the list of completed contracts from the campaign object.
        List<AtBContract> completedContracts = campaign.getCompletedAtBContracts();

        // If there are no completed contracts, return the current date.
        if (completedContracts.isEmpty()) {
            return today;
        }

        // Initialize a variable to hold the ending date of the last contract.
        LocalDate lastContractEndingDate = null;

        // Loop through each contract in the list of completed contracts.
        for (AtBContract contract : completedContracts) {
            // Get the ending date of the current contract.
            LocalDate endingDate = contract.getEndingDate();

            // If this is the first iteration, assign the ending date of the current contract to lastContractEndingDate and continue to the next iteration.
            if (lastContractEndingDate == null) {
                lastContractEndingDate = endingDate;
                continue;
            }

            // If the ending date of the current contract is after the lastContractEndingDate, update lastContractEndingDate.
            if (endingDate.isAfter(lastContractEndingDate)) {
                lastContractEndingDate = endingDate;
            }
        }

        // Return the ending date of the last contract.
        return lastContractEndingDate;
    }

    /**
     * Builds the award list and filters it, so we're not processing the same awards multiple times
     *
     * @param awardListCase when the award controller was called: 0 manual (or monthly), 1 post-mission, 2 post-scenario, 3 rank
     */
    private void buildAwardLists(int awardListCase) {
        ArrayList<Award> awards = new ArrayList<>();
        List<String> allSetNames = AwardsFactory.getInstance().getAllSetNames();

        if (campaign.getCampaignOptions().isIgnoreStandardSet()) {
            allSetNames.removeIf(setName -> setName.equalsIgnoreCase("standard"));
            logger.info("Ignoring the Standard Set");
        }

        String[] filterList = campaign.getCampaignOptions().getAwardSetFilterList().split(",");

        // we start by building a primary list of all awards
        logger.info("Getting all Award Sets");

        for (String setName : allSetNames) {
            if (Arrays.asList(filterList).contains(setName)) {
                logger.info("'{}' was found in the list of sets to be ignored. Ignoring it.", setName);
                continue;
            }

            logger.info("Getting all awards from set: {}", setName);

            awards.addAll(AwardsFactory.getInstance().getAllAwardsForSet(setName));
        }

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
                        case "training":
                            trainingAwards.add(award);
                            break;
                        default:
                            // if autoAwards doesn't know what to do with an Award, it ignores it
                            ignoredAwards.add(award);
                    }
                }
                // These logs help users double-check that the number of awards found matches their records
                logger.info("autoAwards found {} Kill Awards (excluding Mission & Scenario Kill Awards)", killAwards.size());
                logger.info("autoAwards found {} Rank Awards", rankAwards.size());
                logger.info("autoAwards found {} Scenario Awards", scenarioAwards.size());
                logger.info("autoAwards found {} Skill Awards", skillAwards.size());
                logger.info("autoAwards found {} Time Awards", timeAwards.size());
                logger.info("autoAwards found {} Training Awards", trainingAwards.size());
                logger.info("autoAwards found {} Misc Awards", miscAwards.size());
                logger.info("autoAwards is ignoring {} Awards", ignoredAwards.size());

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
                        case "training":
                            trainingAwards.add(award);
                            break;
                        default:
                            // if autoAwards doesn't know what to do with an Award, it ignores it
                            ignoredAwards.add(award);
                    }
                }
                // These logs help users double-check that the number of awards found matches their records
                logger.info("autoAwards found {} Kill Awards (excluding Scenario Kill Awards)", killAwards.size());
                logger.info("autoAwards found {} Contract Awards", contractAwards.size());
                logger.info("autoAwards found {} Rank Awards", rankAwards.size());
                logger.info("autoAwards found {} Scenario Awards", scenarioAwards.size());
                logger.info("autoAwards found {} Skill Awards", skillAwards.size());
                logger.info("autoAwards found {} TheatreOfWar Awards", theatreOfWarAwards.size());
                logger.info("autoAwards found {} Time Awards", timeAwards.size());
                logger.info("autoAwards found {} Misc Awards", miscAwards.size());
                logger.info("autoAwards is ignoring {} Awards", ignoredAwards.size());

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
                        case "misc":
                            if (campaign.getCampaignOptions().isEnableMiscAwards()) {
                                miscAwards.add(award);
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
                        default:
                            ignoredAwards.add(award);
                    }
                }

                logger.info("autoAwards found {} Scenario Kill Awards (excluding Mission & Lifetime Kill Awards)", killAwards.size());
                logger.info("autoAwards found {} Injury Awards", injuryAwards.size());
                logger.info("autoAwards found {} Misc Awards", miscAwards.size());
                logger.info("autoAwards found {} Scenario Awards", scenarioAwards.size());
                logger.info("autoAwards is ignoring {} Awards", ignoredAwards.size());

                break;
            // post-graduation
            case 3:
                for (Award award : awards) {
                    switch (award.getItem().toLowerCase().replaceAll("\\s", "")) {
                        case "divider":
                            break;
                        case "training":
                            if (campaign.getCampaignOptions().isEnableTrainingAwards()) {
                                trainingAwards.add(award);
                            }
                            break;
                        default:
                            ignoredAwards.add(award);
                    }
                }

                logger.info("autoAwards found {} Training Awards", trainingAwards.size());
                logger.info("autoAwards is ignoring {} Awards", ignoredAwards.size());

                break;
            // post-promotion
            case 4:
                for (Award award : awards) {
                    switch (award.getItem().toLowerCase().replaceAll("\\s", "")) {
                        case "divider":
                            break;
                        case "rank":
                            if (campaign.getCampaignOptions().isEnableRankAwards()) {
                                rankAwards.add(award);
                            }
                            break;
                        default:
                            ignoredAwards.add(award);
                    }
                }

                logger.info("autoAwards found {} Training Awards", rankAwards.size());
                logger.info("autoAwards is ignoring {} Awards", ignoredAwards.size());

                break;
            default:
                throw new IllegalStateException("Unexpected awardListCase: " + awardListCase);
        }
    }

    /**
     * Process the awards for the given personnel.
     *
     * @param personnel               the List of personnel to process awards for
     * @param missionWasSuccessful    true if the mission was successful, false otherwise
     * @param academyAttributes       a map of academy attributes, null if not processing graduation awards
     * @param isManualPrompt          whether autoAwards was triggered manually
     */
    private void ProcessAwards(List<UUID> personnel, Boolean missionWasSuccessful, @Nullable HashMap<UUID, List<Object>> academyAttributes, boolean isManualPrompt) {
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
            // we need to use a map when accessing post-scenario,
            // so we need to convert to a map here
            HashMap<UUID, Integer> personnelMap = new HashMap<>();

            for (UUID person : personnel) {
                personnelMap.put(person, 0);
            }

            processedData = MiscAwardsManager(personnelMap, missionWasSuccessful, false, false, null);

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

        if ((!trainingAwards.isEmpty()) && (academyAttributes != null)) {
            processedData = TrainingAwardsManager(personnel, academyAttributes);

            if (processedData != null) {
                allAwardData.put(allAwardDataKey, processedData);
            }
        }

        if (!allAwardData.isEmpty()) {
            AutoAwardsDialog autoAwardsDialog = new AutoAwardsDialog(campaign, allAwardData, 0);
            autoAwardsDialog.setModalityType(ModalityType.APPLICATION_MODAL);
            autoAwardsDialog.setLocation(autoAwardsDialog.getLocation().x, 0);
            autoAwardsDialog.setVisible(true);
        } else {
            logger.info("Zero personnel were found eligible for Awards");

            final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AutoAwardsDialog",
                    MekHQ.getMHQOptions().getLocale());

            if (isManualPrompt) {
                JOptionPane.showMessageDialog(null,
                        resources.getString("txtNoneEligible.text"),
                        resources.getString("AutoAwardsDialog.title"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * This is the manager for this type of award, processing eligibility and preparing awardData
     *
     * @param personnel the personnel to be processed
     */
    private Map<Integer, List<Object>> ContractAwardsManager(List<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                // this gives us a map of unique identifier (int), containing a list of Person, and Award
                data = ContractAwards.ContractAwardsProcessor(campaign, mission, person, contractAwards);
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Contract Awards.",
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
    private Map<Integer, List<Object>> FactionHunterAwardsManager(List<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;

            try {
                data = FactionHunterAwards.FactionHunterAwardsProcessor(campaign, mission, person, factionHunterAwards);
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Faction Hunter Awards.",
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
    private Map<Integer, List<Object>> InjuryAwardsManager(HashMap<UUID, Integer> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel.keySet()) {
            Map<Integer, List<Object>> data;

            try {
                data = InjuryAwards.InjuryAwardsProcessor(campaign, person, injuryAwards, personnel.get(person));
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Injury Awards.", campaign.getPerson(person).getFullName());
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
    private Map<Integer, List<Object>> KillAwardsManager(List<UUID> personnel) {
        // prep the kill award data so that we only have to process it once
        Map<Integer, List<Kill>> missionKillData = personnel.stream()
                .flatMap(person -> campaign.getKillsFor(person).stream())
                .filter(kill -> mission != null && (kill.getMissionId() == mission.getId()))
                .collect(Collectors.groupingBy(Kill::getForceId));

        // process the award data, checking for award eligibility
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = KillAwards.KillAwardProcessor(campaign, mission, person, killAwards, missionKillData);
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Kill Awards.",
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
     * This method is the manager for processing Scenario Kill Awards.
     *
     * @param personnel     the List of personnel to be processed for awards
     * @param scenarioKills a map of personnel and their corresponding list of Kills
     * @return a map containing the award data, or null if no awards are applicable
     */
    private Map<Integer, List<Object>> ScenarioKillAwardsManager(List<UUID> personnel, HashMap<UUID, List<Kill>> scenarioKills) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            List<Kill> personalKills = scenarioKills.get(person);

            Map<Integer, List<Object>> data;

            try {
                data = ScenarioKillAwards.ScenarioKillAwardsProcessor(campaign, person, killAwards, personalKills.size());
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Scenario Kill Awards.",
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
     * This method is the manager for processing Miscellaneous Awards.
     *
     * @param personnel        the personnel to be processed
     * @param missionWasSuccessful    true if the mission was successful, false otherwise
     * @param wasCivilianHelp  true if the scenario (if relevant) was AtB Scenario type CIVILIANHELP
     * @param wasScenario      true if the award is for a scenario, false otherwise
     * @param scenarioKills    a map of personnel and their corresponding list of Kills
     * @return a map containing the award data, or null if no awards are applicable
     */
    private Map<Integer, List<Object>> MiscAwardsManager(HashMap<UUID, Integer> personnel, boolean missionWasSuccessful, boolean wasScenario,
                                                         boolean wasCivilianHelp, HashMap<UUID, List<Kill>> scenarioKills) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        UUID supportPersonOfTheYear = null;

        if (campaign.getLocalDate().getDayOfYear() == 1) {
            int supportPoints = 0;


            // we duplicate and shuffle the list to avoid giving personnel advantage based on name
            List<UUID> temporaryPersonnelList = new ArrayList<>(personnel.keySet());
            Collections.shuffle(temporaryPersonnelList);

            // we do everybody here, as we want to capture personnel who were support personnel,
            // even if they're not current support personnel
            for (UUID person : temporaryPersonnelList) {
                Person p = campaign.getPerson(person);

                if (p.getAutoAwardSupportPoints() > supportPoints) {
                    supportPersonOfTheYear = person;
                    supportPoints = p.getAutoAwardSupportPoints();
                }

                // we reset them for next year
                p.setAutoAwardSupportPoints(0);
            }
        }

        for (UUID person : personnel.keySet()) {
            Map<Integer, List<Object>> data;

            List<Kill> personalKills = new ArrayList<>();

            if (wasScenario) {
                // This should only throw an exception if the person we're trying to get wasn't in the scenario.
                // I'm not sure if that can even happen, but insurance never hurts.
                try {
                    personalKills = scenarioKills.get(person);
                } catch (Exception ignored) {}
            }

            try {
                data = MiscAwards.MiscAwardsProcessor(
                        campaign,
                        mission,
                        person,
                        miscAwards,
                        missionWasSuccessful,
                        wasCivilianHelp,
                        personalKills.size(),
                        personnel.get(person),
                        supportPersonOfTheYear
                );
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Misc Awards.", campaign.getPerson(person).getFullName());
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
    private Map<Integer, List<Object>> RankAwardsManager(List<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = RankAwards.RankAwardsProcessor(campaign, person, rankAwards);
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Rank Awards.", campaign.getPerson(person).getFullName());
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
    private Map<Integer, List<Object>> ScenarioAwardsManager(List<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = ScenarioAwards.ScenarioAwardsProcessor(campaign, person, scenarioAwards);
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Scenario Awards.", campaign.getPerson(person).getFullName());
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
    private Map<Integer, List<Object>> SkillAwardsManager(List<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = SkillAwards.SkillAwardsProcessor(campaign, person, skillAwards);
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Skill Awards.", campaign.getPerson(person).getFullName());
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
    private Map<Integer, List<Object>> TheatreOfWarAwardsManager(List<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = TheatreOfWarAwards.TheatreOfWarAwardsProcessor(campaign, mission, person, theatreOfWarAwards);
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Theatre of War Awards.", campaign.getPerson(person).getFullName());
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
    private Map<Integer, List<Object>> TimeAwardsManager(List<UUID> personnel) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = TimeAwards.TimeAwardsProcessor(campaign, person, timeAwards);
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Time Awards.", campaign.getPerson(person).getFullName());
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
     * @param academyAttributes the academy attributes mapped to the personnel being processed
     */
    private Map<Integer, List<Object>> TrainingAwardsManager(List<UUID> personnel, HashMap<UUID, List<Object>> academyAttributes) {
        Map<Integer, List<Object>> awardData = new HashMap<>();
        int awardDataKey = 0;

        for (UUID person : personnel) {
            Map<Integer, List<Object>> data;
            try {
                data = TrainingAwards.TrainingAwardsProcessor(campaign, person, academyAttributes.get(person), trainingAwards);
            } catch (Exception e) {
                data = null;
                logger.debug("{} is not eligible for any Training Awards.", campaign.getPerson(person).getFullName());
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
