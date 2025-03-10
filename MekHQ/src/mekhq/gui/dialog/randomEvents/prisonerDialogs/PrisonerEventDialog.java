/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.ArrayList;
import java.util.List;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Represents a dialog triggered when a random prisoner event occurs, asking the player how they
 * would like to resolve it.
 *
 * <p>This dialog communicates the nature of a prisoner-related random event and presents the player
 * with three resolution options to choose from. The event is described with immersive, in-character
 * messaging, and each option provides a narrative justification and contextual tooltip to guide
 * the player's decision.</p>
 */
public class PrisonerEventDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    static final String FORWARD_EVENT = "event.";
    static final String SUFFIX_MESSAGE = ".message";

    static final String FORWARD_RESPONSE = "response.";
    static final String OPTION_INDEX_0 = "0.";
    static final String OPTION_INDEX_1 = "1.";
    static final String OPTION_INDEX_2 = "2.";
    static final String SUFFIX_BUTTON = ".button";

    /**
     * Creates a dialog to ask the player how they would like to respond to a prisoner event.
     *
     * @param campaign The current campaign instance, which provides additional context for the dialog.
     * @param speaker  The character delivering the message in the dialog, or {@code null} if no
     *                speaker is defined.
     * @param event    The type of prisoner event that triggered the dialog.
     */
    public PrisonerEventDialog(Campaign campaign, @Nullable Person speaker, PrisonerEvent event) {
        super(campaign, speaker, null, createInCharacterMessage(campaign, event),
            createButtons(event), createOutOfCharacterMessage(), null, true);
    }

    /**
     * Generates the list of buttons for the dialog, representing the player's resolution options.
     *
     * <p>The dialog provides three resolution options for the prisoner event, each labeled to
     * indicate the choice and accompanied by a tooltip for additional context. The available
     * options and their corresponding labels and tooltips are retrieved based on the specific
     * event type.</p>
     *
     * @param event The type of prisoner event, used to determine the specific options available.
     * @return A list of three buttons, each labeled and tooltip-equipped to guide the player's decision.
     */
    private static List<ButtonLabelTooltipPair> createButtons(PrisonerEvent event) {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();

        ButtonLabelTooltipPair btnResponseA = new ButtonLabelTooltipPair(
            null,
            getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + OPTION_INDEX_0 + event.name() + SUFFIX_BUTTON));
        buttons.add(btnResponseA);

        ButtonLabelTooltipPair btnResponseB = new ButtonLabelTooltipPair(
            null,
            getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + OPTION_INDEX_1 + event.name() + SUFFIX_BUTTON));
        buttons.add(btnResponseB);

        ButtonLabelTooltipPair btnResponseC = new ButtonLabelTooltipPair(
            null,
            getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + OPTION_INDEX_2 + event.name() + SUFFIX_BUTTON));
        buttons.add(btnResponseC);

        return buttons;
    }

    /**
     * Generates the immersive in-character message to display in the dialog.
     *
     * <p>The message provides a narrative description of the prisoner event, addressing the
     * player using their in-game title or designation. The content is tailored to the specific
     * type of prisoner event that has occurred.</p>
     *
     * @param campaign The current campaign context, providing player and game-state information.
     * @param event    The type of prisoner event, used to determine the message text.
     * @return A formatted string containing the in-character message describing the event.
     */
    private static String createInCharacterMessage(Campaign campaign, PrisonerEvent event) {
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_EVENT + event.name() + SUFFIX_MESSAGE,
            commanderAddress);
    }

    /**
     * Generates the optional out-of-character (OOC) message for the dialog.
     *
     * <p>The OOC message provides additional gameplay context or clarification. This message is
     * intended to help the player understand why the event occurred.</p>
     *
     * @return A formatted string containing the out-of-character message for the event.
     */
    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "result.ooc");
    }
}
