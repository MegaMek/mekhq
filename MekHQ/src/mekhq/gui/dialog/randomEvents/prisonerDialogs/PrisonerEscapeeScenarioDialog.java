/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.randomEvents.prisonerDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Represents a dialog triggered after prisoners have been tracked down, culminating in a special
 * scenario.
 *
 * <p>This dialog informs the player about the results of tracking down escaped prisoners and sets
 * the stage for a special scenario. It provides an immersive, in-character message describing the
 * situation, along with optional out-of-character context to clarify the mechanics of the
 * triggered scenario.</p>
 */
public class PrisonerEscapeeScenarioDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /**
     * Creates a dialog to communicate the outcome of a prisoner escapee scenario trigger.
     *
     * @param campaign The current campaign instance, which provides the context for the dialog.
     * @param track    The {@link StratconTrackState} associated with the prisoner escape scenario.
     * @param coords   The coordinates ({@link StratconCoords}) where the scenario is taking place.
     */
    public PrisonerEscapeeScenarioDialog(Campaign campaign, StratconTrackState track, StratconCoords coords) {
        super(campaign, campaign.getSeniorAdminPerson(COMMAND), null,
            createInCharacterMessage(campaign, track, coords), createButtons(),
            createOutOfCharacterMessage(), null);
    }

    /**
     * Generates the list of buttons for the dialog.
     *
     * <p>This dialog includes a single confirmation button for the player to acknowledge and
     * proceed with the triggered scenario. The button text is defined in the resource bundle and
     * indicates successful initiation of the scenario.</p>
     *
     * @return A list containing a single confirmation button.
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnConfirmation = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
            "successful.button"), null);

        return List.of(btnConfirmation);
    }

    /**
     * Creates the immersive in-character message to display in the dialog.
     *
     * <p>This message communicates the details of the prisoner escapee scenario to the player in
     * an immersive narrative style. It incorporates elements such as the player's title, the name
     * of the relevant StratCon track, and the sector coordinates where the scenario is taking
     * place.</p>
     *
     * @param campaign The current campaign context, including player information.
     * @param track    The {@link StratconTrackState} providing information about the track where
     *                 the scenario is occurring.
     * @param coords   The {@link StratconCoords} representing the location of the event in game.
     * @return A formatted string containing the in-character narrative message.
     */
    private static String createInCharacterMessage(Campaign campaign, StratconTrackState track,
                                                   StratconCoords coords) {
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, "escapeeScenario.report",
            commanderAddress, track.getDisplayableName(), coords.toBTString());
    }

    /**
     * Generates an optional out-of-character (OOC) message for the dialog.
     *
     * <p>The OOC message provides additional context for the player, explaining gameplay mechanics
     * or clarifying the next steps after this scenario has been triggered.</p>
     *
     * @return A formatted string containing the out-of-character message.
     */
    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "escapeeScenario.ooc");
    }
}
