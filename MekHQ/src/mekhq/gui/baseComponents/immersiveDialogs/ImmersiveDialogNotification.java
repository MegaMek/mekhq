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

import static mekhq.utilities.MHQInternationalization.getText;

import java.util.List;

import mekhq.campaign.Campaign;

/**
 * A specialized notification dialog that presents an immersive, modal, or non-modal message to the player.
 *
 * <p>This class extends {@link ImmersiveDialogCore} to provide a simple notification dialog featuring a customizable
 * central message and a single "Understood" button. This dialog can be used to inform the player of important events or
 * confirmations during gameplay. The dialog may optionally specify a custom width.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class ImmersiveDialogNotification extends ImmersiveDialogCore {
    /**
     * Constructs a notification dialog with the specified message, and modality.
     *
     * @param campaign      the current campaign context
     * @param centerMessage the main message to display in the center of the dialog
     * @param isModal       {@code true} if the dialog should be modal, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public ImmersiveDialogNotification(Campaign campaign, String centerMessage, boolean isModal) {
        super(campaign,
              null,
              null,
              centerMessage,
              createButtons(),
              null,
              null,
              false,
              null,
              null,
              isModal);
    }

    /**
     * Constructs a notification dialog with the specified campaign context, message, custom width, and modality.
     *
     * @param campaign      the current campaign context
     * @param centerMessage the main message to display in the center of the dialog
     * @param width         the {@link ImmersiveDialogWidth} to be used for the dialog
     * @param isModal       {@code true} if the dialog should be modal, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public ImmersiveDialogNotification(Campaign campaign, String centerMessage, ImmersiveDialogWidth width,
          boolean isModal) {
        super(campaign,
              null,
              null,
              centerMessage,
              createButtons(),
              null,
              width.getWidth(),
              false,
              null,
              null,
              isModal);
    }

    /**
     * Creates the list of button(s) to display in the dialog.
     *
     * <p>This implementation always returns a single "Understood" button with no tooltip.</p>
     *
     * @return a list containing one {@link ButtonLabelTooltipPair} for the "Understood" button
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        return List.of(new ButtonLabelTooltipPair(getText("Understood.text"), null));
    }
}
