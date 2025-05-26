/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

import java.util.ArrayList;
import java.util.List;

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * Handles the process of issuing Batchalls to the player when facing a Clan opponent.
 *
 * <p>This class manages the display of dialogue options, tracks player responses, and presents appropriate narrative
 * follow-ups based on player input and faction standing.</p>
 *
 * <p>The main logic randomly selects variations of the Batchall narrative and handles both acceptance and refusal
 * workflows, including confirmation and follow-up dialogs.</p>
 */
public class PerformBatchall {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PerformBatchall";

    private static final int BATCHALL_OPTIONS_COUNT = 10;
    private static final int INTRO_RESPONSE_COUNT = 5;
    private static final int DIALOG_DECLINE_OPTION_START_INDEX = 3;

    private final Campaign campaign;
    private final Person clanOpponent;
    private final String enemyFactionCode;
    private final FactionStandingLevel standingLevel;
    private final int batchallVersion;

    private boolean isBatchallAccepted = true;

    public PerformBatchall(Campaign campaign, Person clanOpponent, String enemyFactionCode) {
        this.campaign = campaign;
        this.clanOpponent = clanOpponent;
        this.enemyFactionCode = enemyFactionCode;
        standingLevel = getFactionStandingLevel(campaign.getFactionStandings());
        batchallVersion = randomInt(BATCHALL_OPTIONS_COUNT);

        if (getInitialChallengeDialog() < DIALOG_DECLINE_OPTION_START_INDEX) {
            getBatchallFollowUpDialog(false);
            return;
        }

        if (getAreYouSureDialog() >= DIALOG_DECLINE_OPTION_START_INDEX) {
            getBatchallFollowUpDialog(true);
            isBatchallAccepted = false;
        } else {
            getBatchallFollowUpDialog(false);
        }
    }

    private FactionStandingLevel getFactionStandingLevel(FactionStandings factionStanding) {
        double regard = factionStanding.getRegardForFaction(enemyFactionCode, true);
        return FactionStandingUtilities.calculateFactionStandingLevel(regard);
    }

    public boolean isBatchallAccepted() {
        return isBatchallAccepted;
    }

    public int getInitialChallengeDialog() {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              clanOpponent,
              null,
              getBatchallIntroText(),
              getInitialChallengeDialogOptions(),
              getTextAt(RESOURCE_BUNDLE, "performBatchall.intro.ooc"),
              null,
              true);

        return dialog.getDialogChoice();
    }

    public String getBatchallIntroText() {
        final String bundleKey = "performBatchall." + standingLevel.name() + ".batchall." + batchallVersion + ".intro";
        final String campaignName = campaign.getName();
        final String opponentName = clanOpponent == null ? "" : clanOpponent.getFullName();

        Faction opponentClan = Factions.getInstance().getFaction(enemyFactionCode);
        String opponentClanName = opponentClan == null ? "" : opponentClan.getFullName(campaign.getGameYear());
        opponentClanName = opponentClanName.replace(getTextAt(RESOURCE_BUNDLE, "performBatchall.clanName.prefix") + ' ',
              "");
        opponentClanName = getFormattedTextAt(RESOURCE_BUNDLE, "performBatchall.clanName.formatted", opponentClanName);

        return getFormattedTextAt(RESOURCE_BUNDLE, bundleKey, campaignName, opponentName, opponentClanName);
    }

    public List<String> getInitialChallengeDialogOptions() {
        List<String> responses = new ArrayList<>();

        for (int i = 0; i < INTRO_RESPONSE_COUNT; i++) {
            responses.add(getTextAt(RESOURCE_BUNDLE, "performBatchall.intro." + i));
        }

        return responses;
    }

    public void getBatchallFollowUpDialog(boolean isRefuse) {
        String message = getBatchallPostIntroText(isRefuse);

        new ImmersiveDialogSimple(campaign, clanOpponent, null, message, null, null, null, true);
    }

    public String getBatchallPostIntroText(boolean isRefuse) {
        final String keySuffix = isRefuse ? "refuse" : "accept";
        final String bundleKey = "performBatchall." +
                                       standingLevel.name() +
                                       ".batchall." +
                                       batchallVersion +
                                       "." +
                                       keySuffix;

        return getFormattedTextAt(RESOURCE_BUNDLE, bundleKey);
    }

    public int getAreYouSureDialog() {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(COMMAND),
              null,
              getAreYouSureDialogText(),
              getAreYouSureDialogOptions(),
              getTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.outOfCharacter"),
              null,
              true);

        return dialog.getDialogChoice();
    }

    public String getAreYouSureDialogText() {
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.inCharacter", commanderAddress);
    }

    public List<String> getAreYouSureDialogOptions() {
        return List.of(getTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.button.confirm"),
              getTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.button.cancel"));
    }
}
