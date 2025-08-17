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

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Handles the process of issuing Batchalls to the player when facing a Clan opponent.
 *
 * <p>This class manages the display of dialogue options, tracks player responses, and presents appropriate narrative
 * follow-ups based on player input and faction standing.</p>
 *
 * <p>The main logic randomly selects variations of the Batchall narrative and handles both acceptance and refusal
 * workflows, including confirmation and follow-up dialogs.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class PerformBatchall {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PerformBatchall";

    private static final int BATCHALL_OPTIONS_COUNT = 10;
    private static final int INTRO_RESPONSE_COUNT = 5;
    private static final int DIALOG_DECLINE_OPTION_START_INDEX = 3;
    private static final int DIALOG_DECLINE_OPTION_ARE_YOU_SURE_INDEX = 1;

    private final Campaign campaign;
    private final Person clanOpponent;
    private final String enemyFactionCode;
    private final FactionStandingLevel standingLevel;
    private final int batchallVersion;

    private boolean isBatchallAccepted = true;

    /**
     * Constructs a new {@code PerformBatchall} handler for the specified campaign and Clan opponent.
     *
     * <p>Initializes the dialog flow, beginning with the Batchall challenge and tracking the player's response.</p>
     *
     * @param campaign         The current campaign context.
     * @param clanOpponent     The opponent issuing the Batchall challenge.
     * @param enemyFactionCode The internal code of the opponent's faction.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public PerformBatchall(Campaign campaign, Person clanOpponent, String enemyFactionCode) {
        this.campaign = campaign;
        this.clanOpponent = clanOpponent;
        this.enemyFactionCode = enemyFactionCode;
        standingLevel = getFactionStandingLevel(campaign.getFactionStandings());
        batchallVersion = randomInt(BATCHALL_OPTIONS_COUNT);

        if (campaign.getCampaignOptions().isUseFactionStandingBatchallRestrictionsSafe()) {
            if (!standingLevel.isBatchallAllowed()) {
                getBatchallStandingTooLowDialog();
                isBatchallAccepted = false;
                return;
            }
        }

        if (getInitialChallengeDialog() < DIALOG_DECLINE_OPTION_START_INDEX) {
            getBatchallFollowUpDialog(false);
            return;
        }

        if (getAreYouSureDialog() == DIALOG_DECLINE_OPTION_ARE_YOU_SURE_INDEX) {
            getBatchallFollowUpDialog(true);
            isBatchallAccepted = false;
        } else {
            getBatchallFollowUpDialog(false);
        }
    }

    /**
     * Calculates the faction standing level toward the enemy Clan, used to determine narrative flavor and dialog
     * options in the Batchall process.
     *
     * @param factionStanding The {@link FactionStandings} object tracking campaign standing toward all factions.
     *
     * @return The {@link FactionStandingLevel} of the campaign toward the current Clan.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private FactionStandingLevel getFactionStandingLevel(FactionStandings factionStanding) {
        double regard = factionStanding.getRegardForFaction(enemyFactionCode, true);
        return FactionStandingUtilities.calculateFactionStandingLevel(regard);
    }

    /**
     * Returns whether the player accepted the Batchall challenge.
     *
     * @return {@code true} if the player accepted the Batchall; {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isBatchallAccepted() {
        return isBatchallAccepted;
    }

    /**
     * Displays a dialog informing the player that their standing is too low to issue a Batchall challenge.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void getBatchallStandingTooLowDialog() {
        new ImmersiveDialogSimple(campaign,
              clanOpponent,
              null,
              getBatchallForbiddenText(),
              null,
              getTextAt(RESOURCE_BUNDLE, "performBatchall.batchall.tooLowStanding.ooc"),
              null,
              true);
    }

    /**
     * Retrieves the narrative text displayed when the player's standing is too low to issue a Batchall challenge.
     *
     * @return A string containing the formatted message indicating that the player's standing is insufficient to issue
     *       a Batchall challenge.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getBatchallForbiddenText() {
        final String bundleKey = "performBatchall.batchall.tooLowStanding";

        Faction opponentClan = Factions.getInstance().getFaction(enemyFactionCode);
        String opponentClanName = opponentClan == null ? "" : opponentClan.getFullName(campaign.getGameYear());

        if (!opponentClanName.contains(getTextAt(RESOURCE_BUNDLE, "performBatchall.clanName.prefix.clan"))) {
            opponentClanName = String.format("%s %s",
                  getTextAt(RESOURCE_BUNDLE, "performBatchall.clanName.prefix.the"),
                  opponentClanName);
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, bundleKey, opponentClanName);
    }

    /**
     * Displays the initial Batchall challenge dialog to the player and returns their selection. The dialog options and
     * message use randomized and context-aware narrative variations.
     *
     * @return The index of the option chosen by the player in the dialog.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private int getInitialChallengeDialog() {
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

    /**
     * Generates the introduction text for the Batchall challenge based on standing, narrative template, and context
     * (such as campaign and opponent names).
     *
     * @return A formatted introduction text for the challenge.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getBatchallIntroText() {
        final String bundleKey = "performBatchall." + standingLevel.name() + ".batchall." + batchallVersion + ".intro";
        final String campaignName = campaign.getName();
        final String opponentName = clanOpponent == null ? "" : clanOpponent.getFullTitle();

        Faction opponentClan = Factions.getInstance().getFaction(enemyFactionCode);
        String opponentClanName = opponentClan == null ? "" : opponentClan.getFullName(campaign.getGameYear());

        if (!opponentClanName.contains(getTextAt(RESOURCE_BUNDLE, "performBatchall.clanName.prefix.empire"))) {
            opponentClanName = opponentClanName.replace(
                  getTextAt(RESOURCE_BUNDLE, "performBatchall.clanName.prefix.clan") + ' ', "");
            opponentClanName = getFormattedTextAt(RESOURCE_BUNDLE,
                  "performBatchall.clanName.formatted",
                  opponentClanName);
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, bundleKey, campaignName, opponentName, opponentClanName);
    }

    /**
     * Provides a list of possible initial responses to the Batchall challenge, typically rendered as selectable dialog
     * buttons.
     *
     * @return List of response strings for the player to choose from.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private List<String> getInitialChallengeDialogOptions() {
        List<String> responses = new ArrayList<>();

        for (int i = 0; i < INTRO_RESPONSE_COUNT; i++) {
            responses.add(getTextAt(RESOURCE_BUNDLE, "performBatchall.intro." + i));
        }

        return responses;
    }

    /**
     * Displays the follow-up dialog after the player chooses to accept or refuse the Batchall.
     *
     * <p>The content adapts to the player's action.</p>
     *
     * @param isRefuse {@code true} if the player refused the Batchall; {@code false} if accepted.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void getBatchallFollowUpDialog(boolean isRefuse) {
        String message = getBatchallPostIntroText(isRefuse);

        new ImmersiveDialogSimple(campaign, clanOpponent, null, message, null, null, null, true);
    }

    /**
     * Returns the follow-up text to present to the player, depending on whether the Batchall was accepted or refused.
     * The narrative varies by standing and context.
     *
     * @param isRefuse {@code true} for the refusal variant; {@code false} for acceptance.
     *
     * @return The appropriate narrative text.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getBatchallPostIntroText(boolean isRefuse) {
        final String keySuffix = isRefuse ? "refuse" : "accept";
        final String bundleKey = "performBatchall." +
                                       standingLevel.name() +
                                       ".batchall." +
                                       batchallVersion +
                                       "." +
                                       keySuffix;

        return getFormattedTextAt(RESOURCE_BUNDLE, bundleKey);
    }

    /**
     * Displays a confirmation ("Are you sure?") dialog if the player initially refuses the Batchall, offering them a
     * chance to confirm or reverse their decision.
     *
     * @return The index of the player's choice (e.g., confirm or cancel).
     *
     * @author Illiani
     * @since 0.50.07
     */
    private int getAreYouSureDialog() {
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

    /**
     * Retrieves the in-character message to be used in the "Are you sure?" confirmation dialog.
     *
     * @return The formatted confirmation dialog text, addressing the commander.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getAreYouSureDialogText() {
        String commanderAddress = campaign.getCommanderAddress();
        return getFormattedTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.inCharacter", commanderAddress);
    }

    /**
     * Returns the available button options for the "Are you sure?" confirmation dialog.
     *
     * @return A list of strings representing the confirmation choices.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private List<String> getAreYouSureDialogOptions() {
        return List.of(getTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.button.cancel"),
              getTextAt(RESOURCE_BUNDLE, "performBatchall.areYouSure.button.confirm"));
    }
}
