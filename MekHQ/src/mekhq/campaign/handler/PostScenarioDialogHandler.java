/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.handler;

import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomUnitGenerator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.ResolveScenarioTracker.PersonStatus;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.RetirementDefectionDialog;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @author Luana Coppio
 */
public class PostScenarioDialogHandler {

    /**
     * Handles post-game resolution checks, dialogs, and actions after a scenario is completed.
     *
     * <p>
     * This method is responsible for performing several post-combat processes, including retirement checks,
     * automatic application of awards, restarting campaign operations, and cleaning up temporary files.
     * Additionally, it triggers a {@link ScenarioResolvedEvent} to indicate the resolution of the current scenario.
     * </p>
     *
     * <b>Steps Performed:</b>
     * <ul>
     *   <li>Performs post-combat retirement checks on units and personnel within the campaign.</li>
     *   <li>Automatically applies any awards or bonuses based on the scenario results using the tracker.</li>
     *   <li>Restarts "rats" (any required campaign activities or background processes).</li>
     *   <li>Cleans up temporary image files generated during the scenario.</li>
     *   <li>Triggers a {@link ScenarioResolvedEvent} to notify the system about the scenario resolution.</li>
     * </ul>
     *
     * <p>
     * It is important to note that the {@code ScenarioResolvedEvent} is triggered before stopping any
     * background threads to ensure that the {@code currentScenario} is still accessible at the time of the event.
     * </p>
     *
     * @param campaignGUI The {@link CampaignGUI} instance used to manage UI interactions and
     *                   display any necessary dialogs.
     * @param campaign The {@link Campaign} instance containing the current state of the campaign
     *                and its personnel.
     * @param currentScenario The {@link Scenario} that has just been resolved.
     * @param tracker The {@link ResolveScenarioTracker} containing the results and details of the
     *               scenario resolution.
     */
    public static void handle(CampaignGUI campaignGUI, Campaign campaign, Scenario currentScenario,
                              ResolveScenarioTracker tracker) {
        postCombatRetirementCheck(campaignGUI, campaign, currentScenario);
        postCombatAutoApplyAward(campaign, tracker);
        restartRats(campaign);
        cleanupTempImageFiles();
        // we need to trigger ScenarioResolvedEvent before stopping the thread or
        // currentScenario may become null
        MekHQ.triggerEvent(new ScenarioResolvedEvent(currentScenario));
    }

    private static void restartRats(Campaign campaign) {
        if (campaign.getCampaignOptions().isUseAtB()) {
            RandomUnitGenerator.getInstance();
            RandomNameGenerator.getInstance();
        }
    }

    private static void cleanupTempImageFiles() {
        final File tempImageDirectory = new File("data/images/temp");
        if (tempImageDirectory.isDirectory()) {
            var listFiles = tempImageDirectory.listFiles();
            if (listFiles == null) {
                // This may happen if the directory is not accessible or if someone creates a file which the name collides
                // with the folder
                return;
            }
            // This can't be null because of the above
            Stream.of(listFiles).filter(file -> file.getName().endsWith(".png"))
                .forEach(File::delete);
        }
    }

    private static void postCombatRetirementCheck(CampaignGUI campaignGUI, Campaign campaign, Scenario currentScenario) {
        if (!campaign.getRetirementDefectionTracker().getRetirees().isEmpty()) {
            RetirementDefectionDialog rdd = new RetirementDefectionDialog(campaignGUI,
                campaign.getMission(currentScenario.getMissionId()), false);

            if (!rdd.wasAborted()) {
                campaign.applyRetirement(rdd.totalPayout(), rdd.getUnitAssignments());
            }
        }
    }

    private static void postCombatAutoApplyAward(Campaign campaign, ResolveScenarioTracker tracker) {
        if (campaign.getCampaignOptions().isEnableAutoAwards()) {
            HashMap<UUID, Integer> personnel = new HashMap<>();
            HashMap<UUID, List<Kill>> scenarioKills = new HashMap<>();

            for (UUID personId : tracker.getPeopleStatus().keySet()) {
                Person person = campaign.getPerson(personId);
                PersonStatus status = tracker.getPeopleStatus().get(personId);
                int injuryCount = 0;

                if (!person.getStatus().isDead() || campaign.getCampaignOptions().isIssuePosthumousAwards()) {
                    if (status.getHits() > person.getHitsPrior()) {
                        injuryCount = status.getHits() - person.getHitsPrior();
                    }
                }

                personnel.put(personId, injuryCount);
                scenarioKills.put(personId, tracker.getPeopleStatus().get(personId).getKills());
            }

            boolean isCivilianHelp = false;

            if (tracker.getScenario() instanceof AtBScenario atbScenario) {
                isCivilianHelp = atbScenario.getScenarioType() == AtBScenario.CIVILIANHELP;
            }

            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.PostScenarioController(campaign, personnel, scenarioKills, isCivilianHelp);
        }
    }
}
