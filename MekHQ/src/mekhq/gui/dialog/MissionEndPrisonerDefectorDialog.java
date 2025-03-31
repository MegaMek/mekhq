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
package mekhq.gui.dialog;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

/**
 * Represents a dialog triggered at the end of a mission when the player has Prisoner Defectors they can recruit.
 *
 * <p>
 * This dialog allows the player to handle the scenario of prisoners defecting to their forces. The player is presented
 * with an immersive, narrative-driven in-character message as well as optional out-of-character context. Action
 * buttons, such as canceling or continuing the mission conclusion process, are also provided.
 * </p>
 */
public class MissionEndPrisonerDefectorDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /**
     * Creates a dialog to remind the player of outstanding prisoner defectors.
     *
     * @param campaign The current campaign instance, which provides context for the dialog.
     */
    public MissionEndPrisonerDefectorDialog(Campaign campaign) {
        super(campaign,
              campaign.getSeniorAdminPerson(COMMAND),
              null,
              createInCharacterMessage(campaign),
              createButtons(),
              createOutOfCharacterMessage(),
              null,
              false,
              null,
              null,
              true);
    }

    /**
     * Builds the list of buttons displayed in the dialog.
     *
     * <p>
     * This method creates buttons allowing the player to either cancel the mission conclusion or continue.
     * </p>
     *
     * @return A list of button-label and tooltip pairs for this dialog.
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();

        ButtonLabelTooltipPair btnAccept = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "cancel.button"), null);
        buttons.add(btnAccept);

        ButtonLabelTooltipPair btnDecline = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "continue.button"), null);
        buttons.add(btnDecline);

        return buttons;
    }

    /**
     * Generates the in-character message for the dialog.
     *
     * <p>
     * This message is displayed narratively in the dialog. It addresses the player using their in-game commander
     * address while providing context about the prisoner defectors.
     * </p>
     *
     * @param campaign The current campaign instance, which provides data for the message.
     *
     * @return A formatted string containing the in-character message for the player.
     */
    private static String createInCharacterMessage(Campaign campaign) {
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, "prisonerDefectors.message", commanderAddress);
    }

    /**
     * Creates an out-of-character (OOC) message for the dialog.
     *
     * <p>
     * The OOC message provides additional context or instructions to the player about the recruiting defectors process.
     * It is shown for narrative or instructional purposes.
     * </p>
     *
     * @return A formatted string containing the OOC message.
     */
    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "prisonerDefectors.ooc");
    }
}
