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
 */
package mekhq.gui.dialog.randomEvents.prisonerDialogs;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

/**
 * Represents a dialog triggered after a random prisoner event to communicate its results to the player.
 *
 * <p>This dialog displays the outcome of a prisoner-related random event, based on the player’s choices
 * and whether the event was successful or not. The message provides an immersive, in-character narrative describing the
 * results and optionally includes an event report for further context.</p>
 */
public class PrisonerEventResultsDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    static final String FORWARD_RESPONSE = "response.";
    static final String SUFFIX_SUCCESS = ".success";
    static final String SUFFIX_FAILURE = ".failure";

    /**
     * Creates a dialog to present the results of a random prisoner event.
     *
     * @param campaign     The current campaign instance, which provides the context for the dialog.
     * @param speaker      The in-game character acting as the speaker in the dialog, or {@code null} if no speaker is
     *                     defined.
     * @param event        The type of random prisoner event that occurred.
     * @param choiceIndex  The index of the player's selected choice during the event, used to generate the results.
     * @param isSuccessful {@code true} if the event concluded successfully, {@code false} if the event failed.
     * @param eventReport  A detailed report of the event's outcome, presented out-of-character for additional clarity.
     */
    public PrisonerEventResultsDialog(Campaign campaign, @Nullable Person speaker, PrisonerEvent event, int choiceIndex,
                                      boolean isSuccessful, String eventReport) {
        super(campaign,
              speaker,
              null,
              createInCharacterMessage(campaign, event, choiceIndex, isSuccessful),
              createButtons(isSuccessful),
              eventReport,
              null,
              false,
              null,
              null,
              true);
    }

    /**
     * Generates the buttons to display in the dialog.
     *
     * <p>This dialog includes a single confirmation button that acknowledges the outcome of the
     * prisoner event. The text of the button reflects whether the event was successful or a failure.</p>
     *
     * @param isSuccessful {@code true} if the event was successful, {@code false} if it failed.
     *
     * @return A list containing a single button, labeled appropriately based on the success or failure of the event.
     */
    private static List<ButtonLabelTooltipPair> createButtons(boolean isSuccessful) {
        String resourceKey = isSuccessful ? "successful.button" : "failure.button";

        ButtonLabelTooltipPair btnConfirmation = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              resourceKey), null);

        return List.of(btnConfirmation);
    }

    /**
     * Generates the immersive in-character message to display in the dialog.
     *
     * <p>The content of the message reflects the type of prisoner event,
     * the player's choice during the event, and the outcome (success or failure). The player's in-game title is
     * incorporated into the message for added narrative immersion.</p>
     *
     * @param campaign     The current campaign context, which provides player-specific details like their title.
     * @param event        The type of random prisoner event that occurred.
     * @param choiceIndex  The player's selected choice during the event, determining the specific response text.
     * @param isSuccessful {@code true} if the event concluded successfully, {@code false} otherwise.
     *
     * @return A formatted string containing the in-character message describing the event results.
     */
    private static String createInCharacterMessage(Campaign campaign, PrisonerEvent event, int choiceIndex,
                                                   boolean isSuccessful) {
        String suffix = isSuccessful ? SUFFIX_SUCCESS : SUFFIX_FAILURE;
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE,
              FORWARD_RESPONSE + choiceIndex + '.' + event.name() + suffix,
              commanderAddress);
    }
}
