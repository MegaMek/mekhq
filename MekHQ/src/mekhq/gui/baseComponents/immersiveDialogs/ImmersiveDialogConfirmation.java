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
 * A confirmation dialog that prompts the user with a "Yes/No" choice, typically used for critical actions such as
 * deletions or major changes in a campaign.
 *
 * <p>The dialog displays a localized "Are you sure?" message and tracks whether the user selected "Yes" (confirm).</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class ImmersiveDialogConfirmation extends ImmersiveDialogCore {
    /** The index position in the dialog button list that corresponds to user confirmation ("Yes"). */
    private static final int DIALOG_CHOICE_INDEX_CONFIRM = 1;

    /** Whether the user confirmed the dialog (selected "Yes"). */
    private final boolean wasConfirmed;

    /**
     * Returns whether the user confirmed the action (clicked "Yes").
     *
     * @return {@code true} if the user confirmed the dialog, {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean wasConfirmed() {
        return wasConfirmed;
    }

    /**
     * Constructs a confirmation dialog for the provided campaign, presenting a localized "Are you sure?" message with
     * "No" and "Yes" button choices.
     *
     * <p>After construction, the result of the user's choice may be queried via {@link #wasConfirmed()}.</p>
     *
     * @param campaign the current {@link Campaign} context this dialog is associated with
     *
     * @author Illiani
     * @since 0.50.07
     */
    public ImmersiveDialogConfirmation(Campaign campaign) {
        super(campaign,
              null,
              null,
              getText("AreYouSure.text"),
              List.of(new ButtonLabelTooltipPair(getText("No.text"), null),
                    new ButtonLabelTooltipPair(getText("Yes.text"), null)),
              null,
              ImmersiveDialogWidth.SMALL.getWidth(),
              false,
              null,
              null,
              true);

        wasConfirmed = getDialogChoice() == DIALOG_CHOICE_INDEX_CONFIRM;
    }
}
