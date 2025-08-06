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
package mekhq.gui.baseComponents.immersiveDialogs;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

/**
 * A generic immersive message dialog for providing information to the player in the campaign.
 *
 * <p>This dialog supports in-character (IC) and optional out-of-character (OOC) messages, enhancing
 * the immersion of the game experience. It includes speaker information, a central message, and configurable buttons to
 * interact with the dialog.</p>
 *
 * <p>The use case for this dialog is any time you want to present information to the player, in an
 * immersive manner, but don't need them to make any decisions and don't need access to any of the more advanced
 * functionality offered by {@link ImmersiveDialogCore}</p>
 */
public class ImmersiveDialogSimple extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.GUI";

    /**
     * Constructs a {@code GenericImmersiveMessageDialog} with the specified campaign, message details, and optional
     * image and layout configuration.
     *
     * <p>This dialog represents an immersive interaction, typically involving one or two characters
     * "speaking" to the player. It provides a central message area, optional image display above the content, and
     * customizable buttons for user interaction. The dialog is modal by default, blocking other interactions until it
     * is closed.</p>
     *
     * @param campaign              The current game state, providing relevant campaign data.
     * @param leftSpeaker           The {@link Person} appearing as the left speaker, or {@code null} if no speaker is
     *                              displayed on the left side.
     * @param rightSpeaker          The {@link Person} appearing as the right speaker, or {@code null} if no speaker is
     *                              displayed on the right side.
     * @param centerMessage         The primary message to be displayed in the center of the dialog. This typically
     *                              conveys the main information or narrative of the dialog.
     * @param buttonLabels          A {@link List} of custom button labels to display in the dialog. If the list is
     *                              {@code null}, a default "Understood" button is displayed.
     * @param outOfCharacterMessage An optional out-of-character (OOC) message, or {@code null} if not applicable. This
     *                              message is displayed outside the dialog's in-character context, usually to provide
     *                              additional explanation or game-related information to the player.
     * @param imageIcon             An optional {@link ImageIcon}, or {@code null} if not applicable. If specified, the
     *                              image will appear above the center message to highlight or visually support the
     *                              dialog's content. For example, it can represent a symbol, character, or important
     *                              visual cue.
     * @param useVerticalLayout     A boolean flag indicating whether to use a vertical layout. If {@code true}, the
     *                              buttons are stacked vertically; otherwise, they are arranged side-by-side.
     */
    public ImmersiveDialogSimple(Campaign campaign, @Nullable Person leftSpeaker, @Nullable Person rightSpeaker,
          String centerMessage, @Nullable List<String> buttonLabels, @Nullable String outOfCharacterMessage,
          @Nullable ImageIcon imageIcon, boolean useVerticalLayout) {
        super(campaign,
              leftSpeaker,
              rightSpeaker,
              centerMessage,
              createButtons(buttonLabels),
              outOfCharacterMessage,
              null,
              useVerticalLayout,
              null,
              imageIcon,
              true);
    }

    /**
     * Constructs a {@code GenericImmersiveMessageDialog} with the specified campaign, message details, and optional
     * image and layout configuration.
     *
     * <p>This dialog represents an immersive interaction, typically involving one or two characters
     * "speaking" to the player. It provides a central message area, optional image display above the content, and
     * customizable buttons for user interaction. The dialog is modal by default, blocking other interactions until it
     * is closed.</p>
     *
     * @param campaign              The current game state, providing relevant campaign data.
     * @param leftSpeaker           The {@link Person} appearing as the left speaker, or {@code null} if no speaker is
     *                              displayed on the left side.
     * @param rightSpeaker          The {@link Person} appearing as the right speaker, or {@code null} if no speaker is
     *                              displayed on the right side.
     * @param centerMessage         The primary message to be displayed in the center of the dialog. This typically
     *                              conveys the main information or narrative of the dialog.
     * @param buttonLabels          A {@link List} of custom button labels to display in the dialog. If the list is
     *                              {@code null}, a default "Understood" button is displayed.
     * @param outOfCharacterMessage An optional out-of-character (OOC) message, or {@code null} if not applicable. This
     *                              message is displayed outside the dialog's in-character context, usually to provide
     *                              additional explanation or game-related information to the player.
     * @param imageIcon             An optional {@link ImageIcon}, or {@code null} if not applicable. If specified, the
     *                              image will appear above the center message to highlight or visually support the
     *                              dialog's content. For example, it can represent a symbol, character, or important
     *                              visual cue.
     * @param useVerticalLayout     A boolean flag indicating whether to use a vertical layout. If {@code true}, the
     *                              buttons are stacked vertically; otherwise, they are arranged side-by-side.
     * @param width                 A {@link ImmersiveDialogWidth} object used to dictate non-default widths
     */
    public ImmersiveDialogSimple(Campaign campaign, @Nullable Person leftSpeaker, @Nullable Person rightSpeaker,
          String centerMessage, @Nullable List<String> buttonLabels, @Nullable String outOfCharacterMessage,
          @Nullable ImageIcon imageIcon, boolean useVerticalLayout, ImmersiveDialogWidth width) {
        super(campaign,
              leftSpeaker,
              rightSpeaker,
              centerMessage,
              createButtons(buttonLabels),
              outOfCharacterMessage,
              width.getWidth(),
              useVerticalLayout,
              null,
              imageIcon,
              true);
    }

    /**
     * Creates a list of buttons to be displayed in the dialog based on the provided labels.
     *
     * <p>If the list of button labels is {@code null}, a default "Understood" button is created for
     * acknowledging and closing the dialog. Otherwise, buttons are generated with the given labels without additional
     * tooltips.</p>
     *
     * @param buttonLabels A {@link List} of button label strings to generate. If {@code null}, a default "Understood"
     *                     button is created.
     *
     * @return A {@link List} of {@link ButtonLabelTooltipPair} objects, one for each button to be displayed.
     */
    private static List<ButtonLabelTooltipPair> createButtons(@Nullable List<String> buttonLabels) {
        if (buttonLabels == null || buttonLabels.isEmpty()) {
            return List.of(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, "Understood.text"), null));
        }

        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();

        for (String buttonLabel : buttonLabels) {
            buttons.add(new ButtonLabelTooltipPair(buttonLabel, null));
        }

        return buttons;
    }
}
