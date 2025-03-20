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

import java.util.ArrayList;
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
     * Constructs a {@code GenericImmersiveMessageDialog} with the specified campaign and message
     * details.
     *
     * <p>This dialog represents an immersive interaction, typically involving the appearance
     * of one or two characters "speaking" to the player. The dialog is modal by default, and
     * it provides customizable button labels for user interaction, with the commonly used
     * button being "Understood" for closing the dialog.</p>
     *
     * @param campaign              The current game state, providing relevant campaign data.
     * @param leftSpeaker           The {@link Person} appearing as the left speaker, or {@code null}
     *                              if no speaker is present on the left side.
     * @param rightSpeaker          The {@link Person} appearing as the right speaker, or {@code null}
     *                              if no speaker is present on the right side.
     * @param centerMessage         The primary message to be displayed in the center of the dialog.
     * @param buttonLabels          A {@link List} of custom button labels to display in the dialog.
     *                              If the list is empty, a default "Understood" button is displayed.
     * @param outOfCharacterMessage An optional out-of-character (OOC) message, or {@code null} if
     *                              not applicable. This message is displayed in a non-character
     *                              context, usually to provide additional information or context
     *                              to the player.
     */
    public GenericImmersiveMessageDialog(Campaign campaign, @Nullable Person leftSpeaker,
                                         @Nullable Person rightSpeaker, String centerMessage,
                                         List<String> buttonLabels, @Nullable String outOfCharacterMessage) {
        super(campaign, leftSpeaker, rightSpeaker, centerMessage, createButtons(buttonLabels), outOfCharacterMessage,
              null, false, null, true);
    }

    /**
     * Creates a list of buttons to be displayed in the dialog based on the provided labels.
     *
     * <p>If the list of button labels is empty, a default "Understood" button is created for
     * acknowledging and closing the dialog. Otherwise, buttons are generated with the given
     * labels without additional tooltips.</p>
     *
     * @param buttonLabels A {@link List} of button label strings to generate. If empty, a default
     *                    "Understood" button is created.
     * @return A {@link List} of {@link ButtonLabelTooltipPair} objects, one for each button to be
     * displayed.
     */
    private static List<ButtonLabelTooltipPair> createButtons(List<String> buttonLabels) {
        if (buttonLabels.isEmpty()) {
            return List.of(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                  "Understood.text"), null));
        }

        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();

        for (String buttonLabel : buttonLabels) {
            buttons.add(new ButtonLabelTooltipPair(buttonLabel, null));
        }

        return buttons;
    }
}
