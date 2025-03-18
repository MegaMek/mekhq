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

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * A generic immersive message dialog for providing information to the player in the campaign.
 *
 * <p>This dialog supports in-character (IC) and optional out-of-character (OOC) messages, enhancing
 * the immersion of the game experience. It includes speaker information, a central message, and
 * configurable buttons to interact with the dialog.</p>
 *
 * <p>The use case for this dialog is any time you want to present information to the player, in an
 * immersive manner, but don't need them to make any decisions and don't need access to any of the
 * more advanced functionality offered by {@link MHQDialogImmersive}</p>
 */
public class GenericImmersiveMessageDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.GUI";

    /**
     * Constructs a {@code GenericImmersiveMessageDialog} with the given campaign and message details.
     *
     * <p>This dialog is structured to represent an immersive interaction, typically with the appearance
     * of one or two characters "speaking" to the player. The dialog is modal by default and displays
     * a single "Understood" button for closing the dialog.</p>
     *
     * @param campaign              The current game state.
     * @param leftSpeaker           The left {@link Person} speaker, or {@code null} for no speaker.
     * @param rightSpeaker          The right {@link Person} speaker, or {@code null} for no speaker.
     * @param centerMessage         The primary message to be displayed in the center of the dialog.
     * @param outOfCharacterMessage An optional out-of-character (OOC) message, or {@code null} if
     *                             not applicable.
     */
    public GenericImmersiveMessageDialog(Campaign campaign, @Nullable Person leftSpeaker,
                                         @Nullable Person rightSpeaker, String centerMessage,
                                         @Nullable String outOfCharacterMessage) {
        super(campaign, leftSpeaker, rightSpeaker, centerMessage, createButtons(), outOfCharacterMessage,
              null, false, null, true);
    }

    /**
     * Creates the list of buttons for the dialog.
     *
     * <p>This method generates a single "Understood" button for acknowledging and closing the dialog.</p>
     *
     * @return A {@link List} containing a {@link ButtonLabelTooltipPair} for the "Understood" button.
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnConfirm = new ButtonLabelTooltipPair(
              getFormattedTextAt(RESOURCE_BUNDLE, "Understood.text"), null);

        return List.of(btnConfirm);
    }
}
