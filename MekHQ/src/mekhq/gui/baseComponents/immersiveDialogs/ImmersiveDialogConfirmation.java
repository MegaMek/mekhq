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
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import mekhq.MekHQ;
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
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ImmersiveDialogConfirmation";

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
     * @param nagKey   the nag identifier used to look up additional text and to track whether this confirmation should
     *                 be ignored in the future
     *
     * @author Illiani
     * @since 0.50.07
     */
    public ImmersiveDialogConfirmation(Campaign campaign, String nagKey) {
        super(campaign,
              null,
              null,
              getPrimaryText(nagKey),
              List.of(new ButtonLabelTooltipPair(getNoText(), null),
                    new ButtonLabelTooltipPair(getYesText(), null)),
              getSecondaryText(),
              ImmersiveDialogWidth.SMALL.getWidth(),
              false,
              getSupplementalPanel(nagKey),
              null,
              true);

        wasConfirmed = getDialogChoice() == DIALOG_CHOICE_INDEX_CONFIRM;
    }

    /**
     * Builds the primary message text for the confirmation dialog.
     *
     * <p>This combines a generic "Are you sure?" prompt with an optional, nag-specific follow-up text, if one is
     * defined for the given {@code nagKey}.</p>
     *
     * @param nagKey the nag identifier used to resolve additional primary text; if blank, only the generic message is
     *               returned
     *
     * @return the localized primary confirmation message
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getPrimaryText(String nagKey) {
        String initial = getText("AreYouSure.text");
        String followUp = nagKey.isBlank()
                                ? ""
                                : "<p>" + getTextAt(RESOURCE_BUNDLE,
              "ImmersiveDialogConfirmation." + nagKey + ".text.primary") + "</p>";

        return initial + followUp;
    }

    /**
     * Returns the secondary explanatory text displayed beneath the primary confirmation message.
     *
     * @return the localized secondary confirmation text
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getSecondaryText() {
        return getTextAt(RESOURCE_BUNDLE, "ImmersiveDialogConfirmation.text.secondary");
    }

    /**
     * Returns the label text for the "No" button.
     *
     * <p>The {@code nagKey} parameter is accepted for consistency with other helper methods and to allow future
     * customization, but is not currently used.</p>
     *
     * @return the localized label for the "No" button
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getNoText() {
        return getTextAt(RESOURCE_BUNDLE, "ImmersiveDialogConfirmation.button.no");
    }

    /**
     * Returns the label text for the "Yes" button.
     *
     * <p>The {@code nagKey} parameter is accepted for consistency with other helper methods and to allow future
     * customization, but is not currently used.</p>
     *
     * @return the localized label for the "Yes" button
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getYesText() {
        return getTextAt(RESOURCE_BUNDLE, "ImmersiveDialogConfirmation.button.yes");
    }

    /**
     * Creates the supplemental panel for this dialog, containing a checkbox that allows the user to ignore this nag in
     * the future.
     *
     * <p>When the checkbox state changes, the associated option on {@link MekHQ#getMHQOptions()} is updated using
     * the provided {@code nagKey}.</p>
     *
     * @param nagKey the nag identifier used to persist the "ignore" choice
     *
     * @return a panel containing the "ignore this nag" checkbox
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static JPanel getSupplementalPanel(String nagKey) {
        JPanel panel = new JPanel(new GridBagLayout());

        JCheckBox checkBox = new JCheckBox(getText("chkIgnore.text"));
        checkBox.addActionListener(
              e -> MekHQ.getMHQOptions()
                         .setNagDialogIgnore(nagKey, checkBox.isSelected()));
        panel.add(checkBox);

        return panel;
    }
}
