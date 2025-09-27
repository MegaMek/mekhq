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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekhq.campaign.handler;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.ResolveScenarioTracker.PersonStatus;
import mekhq.campaign.events.scenarios.ScenarioResolvedEvent;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.RetirementDefectionDialog;

/**
 * @author Luana Coppio
 */
public class PostScenarioDialogHandler {
    private static final MMLogger LOGGER = MMLogger.create(PostScenarioDialogHandler.class);

    /**
     * Handles post-game resolution checks, dialogs, and actions after a scenario is completed.
     *
     * <p>
     * This method is responsible for performing several post-combat processes, including retirement checks, automatic
     * application of awards, restarting campaign operations, and cleaning up temporary files. Additionally, it triggers
     * a {@link ScenarioResolvedEvent} to indicate the resolution of the current scenario.
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
     * @param campaignGUI     The {@link CampaignGUI} instance used to manage UI interactions and display any necessary
     *                        dialogs.
     * @param campaign        The {@link Campaign} instance containing the current state of the campaign and its
     *                        personnel.
     * @param currentScenario The {@link Scenario} that has just been resolved.
     * @param tracker         The {@link ResolveScenarioTracker} containing the results and details of the scenario
     *                        resolution.
     */
    public static void handle(CampaignGUI campaignGUI, Campaign campaign, Scenario currentScenario,
          ResolveScenarioTracker tracker) {
        postCombatRetirementCheck(campaignGUI, campaign, currentScenario);
        postCombatAutoApplyAward(campaign, tracker);
        restartRats(campaign);
        cleanupTempImageFiles();
        // we need to trigger ScenarioResolvedEvent before stopping the thread or
        // currentScenario may become null
        try {
            MekHQ.triggerEvent(new ScenarioResolvedEvent(currentScenario));
        } catch (Exception e) {
            LOGGER.error(e, "An error occurred during scenario resolution: {}", e.getMessage());
            LOGGER.errorDialog(
                  e,
                  """
                        A critical error has occurred during the scenario resolution. This issue is under investigation.\
                        
                        
                        Please open an issue report and include your MekHQ log file for further assessment.""",
                  "Critical Error"
            );
        }
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
            if (listFiles != null) {
                Stream.of(listFiles).filter(file -> file.getName().endsWith(".png"))
                      .forEach(File::delete);
            }
        }
    }

    private static void postCombatRetirementCheck(CampaignGUI campaignGUI, Campaign campaign,
          Scenario currentScenario) {
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
                isCivilianHelp = atbScenario.getScenarioType() == AtBScenario.CIVILIAN_HELP;
            }

            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.PostScenarioController(campaign, personnel, scenarioKills, isCivilianHelp);
        }
    }
}
