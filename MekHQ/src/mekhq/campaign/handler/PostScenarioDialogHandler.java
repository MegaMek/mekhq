/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.enums.PersonnelStatus;
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
     * Post game resolution checks, dialogs and actions.
     * @param tracker The tracker that contains all the information about the scenario resolution.
     * @param control Whether the player controlled the battlefield at the end of the scenario.
     */
    public static void handle(
        CampaignGUI campaignGUI, Campaign campaign, AtBScenario currentScenario, ResolveScenarioTracker tracker, boolean control) {
        postCombatRetirementCheck(campaignGUI, campaign, currentScenario);
        postCombatAutoApplyAward(campaign, tracker);
        postCombatMissingInActionToPrisonerOfWarStatus(campaign, tracker, control);
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

    private static void postCombatRetirementCheck(CampaignGUI campaignGUI, Campaign campaign, AtBScenario currentScenario) {
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
                ResolveScenarioTracker.PersonStatus status = tracker.getPeopleStatus().get(personId);
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

    private static void postCombatMissingInActionToPrisonerOfWarStatus(Campaign campaign, ResolveScenarioTracker tracker, boolean control) {
        for (UUID personId : tracker.getPeopleStatus().keySet()) {
            Person person = campaign.getPerson(personId);

            if (person.getStatus() == PersonnelStatus.MIA && !control) {
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.POW);
            }
        }
    }
}
